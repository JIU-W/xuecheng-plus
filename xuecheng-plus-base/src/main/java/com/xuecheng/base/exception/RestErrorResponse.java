package com.xuecheng.base.exception;

import java.io.Serializable;

/**
 * @author JIU-W
 * @version 1.0
 * @description 错误响应参数包装
 * @date 2024-11-12
 */
public class RestErrorResponse implements Serializable {

    private String errMessage;

    public RestErrorResponse(String errMessage) {
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}
