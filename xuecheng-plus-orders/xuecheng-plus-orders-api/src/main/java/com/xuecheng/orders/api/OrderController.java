package com.xuecheng.orders.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Api(value = "订单支付接口", tags = "订单支付接口")
@Slf4j
@Controller
public class OrderController {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    OrderService orderService;

    /**
     * 生成支付二维码：前端一点击“支付宝支付”按钮就会请求这个接口，从而插入订单、订单明细、支付记录、
     *                      最后生成支付二维码并返回给前端展示。
     * @param addOrderDto
     * @return
     */
    @ApiOperation("生成支付二维码")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto) {
        //登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        /*if(user == null){
            XueChengPlusException.cast("请登录后继续选课");
        }*/
        return orderService.createOrder(user.getId(), addOrderDto);
    }

    //扫码下单:因为二维码就是根据这个接口的URL生成的，所以支付宝(沙箱)一扫码就会请求这个接口
    @ApiOperation("扫码下单接口")
    @GetMapping("/requestpay")
    public void requestpay(String payNo, HttpServletResponse httpResponse) throws IOException {
        //根据支付记录号查询支付记录
        //如果payNo不存在则提示重新发起支付
        XcPayRecord payRecord = orderService.getPayRecordByPayno(payNo);
        if(payRecord == null){
            XueChengPlusException.cast("请重新点击支付获取二维码");
        }
        //支付状态
        String status = payRecord.getStatus();
        if("601002".equals(status)){
            XueChengPlusException.cast("订单已支付，请勿重复支付。");
        }
        //构造sdk的客户端对象
        AlipayClient client = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);//获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request

        //对于手机网站支付产生的交易，支付宝会通知商户支付结果，有两种通知方式，
        //通过return_url、notify_url进行通知，使用return_url不能保证通知到位，推荐使用notify_url完成支付结构通知。
        //alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        //在公共参数中设置回跳和通知地址
        //下单时指定NotifyUrl:
        alipayRequest.setNotifyUrl("http://tjxt-user-t.itheima.net/xuecheng/orders/receivenotify");
        alipayRequest.setBizContent("{" +
                " \"out_trade_no\":\""+payRecord.getPayNo()+"\"," +
                " \"total_amount\":\""+payRecord.getTotalPrice()+"\"," +
                " \"subject\":\""+payRecord.getOrderName()+"\"," +
                " \"product_code\":\"QUICK_WAP_PAY\"" +
                " }");//填充业务参数
        String form = "";//调用SDK生成表单
        //支付宝会根据这个请求，将我们请求的参数，生成一个支付宝的表单，
        //这个表单的作用就是唤起扫码的手机的支付宝客户端(测试)的支付页面，从而跳转到支付宝的支付页面。
        try {
            //请求支付宝下单接口,发起http请求
            form = client.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }


    //主动查询支付结果(一般在主动查询之前支付宝已经将支付宝的结果返回给我们学成在线系统(详情看下一个接口"/receivenotify")了，
    //从而进行了订单表和支付记录表的状态修改。但是支付很重要，必须要做好万全之策，
    //万一支付宝出问题“没有返回结果”，那就要靠我们自己主动查询并改变订单表和支付记录表的状态：就是如下接口("/payresult")。
    //触发这个接口需要用户点击“支付完成”按钮，然后请求这个接口，从而查询支付结果。
    //在支付宝返回支付结果没有出问题的前提下：这个接口我们只是做了主动查询，没有做任何修改，只是查询一下，然后返回给前端。
    //而在支付宝返回支付结果出现问题的话,我们要先主动查询从而修改订单表和支付记录表的状态，然后再把修改后的结果返回给前端。
    @ApiOperation("查询支付结果")
    @GetMapping("/payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) throws IOException {
        //调用支付宝接口“根据支付交易号”查询支付结果
        PayRecordDto payRecordDto = orderService.queryPayResult(payNo);
        return payRecordDto;
    }

    //被动接受支付宝的支付结果通知：支付完成后第三方支付系统会主动通知支付结果，
    //要实现主动通知需要在“请求支付系统下单时”传入NotifyUrl，
    //这里有两个url：NotifyUrl和ReturnUrl，ReturnUrl是支付完成后支付系统携带支付结果重定向到ReturnUrl地址，
    //NotifyUrl是支付完成后支付系统在后台定时去通知，使用NotifyUrl比使用ReturnUrl有保证。
    @ApiOperation("接收支付结果通知")
    @PostMapping("/receivenotify")
    public void receivenotify(HttpServletRequest request, HttpServletResponse out) throws UnsupportedEncodingException, AlipayApiException {
        Map<String,String> params = new HashMap<String,String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        //验签
        boolean verify_result = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, AlipayConfig.CHARSET, "RSA2");

        if(verify_result) {//验证成功

            //商户订单号
            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");
            //支付宝交易号
            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");
            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"),"UTF-8");
            //appid
            String app_id = new String(request.getParameter("app_id").getBytes("ISO-8859-1"),"UTF-8");
            //total_amount
            String total_amount = new String(request.getParameter("total_amount").getBytes("ISO-8859-1"),"UTF-8");

            //交易成功处理
            if (trade_status.equals("TRADE_SUCCESS")) {
                PayStatusDto payStatusDto = new PayStatusDto();
                payStatusDto.setOut_trade_no(out_trade_no);
                payStatusDto.setTrade_status(trade_status);
                payStatusDto.setApp_id(app_id);
                payStatusDto.setTrade_no(trade_no);
                payStatusDto.setTotal_amount(total_amount);
                //处理逻辑
                orderService.saveAliPayStatus(payStatusDto);
            }

        }


    }



}
