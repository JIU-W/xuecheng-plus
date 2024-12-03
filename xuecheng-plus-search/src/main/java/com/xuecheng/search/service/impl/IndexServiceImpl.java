package com.xuecheng.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程索引管理接口实现
 * @date 2024-12-01
 */
@Slf4j
@Service
public class IndexServiceImpl implements IndexService {


    @Autowired
    private RestHighLevelClient client;

    /**
     * 给course_public索引库添加一条文档数据(添加索引)
     * @param indexName 索引库名称
     * @param id        主键(课程id)
     * @param object    索引对象(索引库具体的文档信息)
     * @return
     */
    public Boolean addCourseIndex(String indexName, String id, Object object) {

        //将索引库的文档数据序列化为json (这里的索引库实体用Object接收，将这部分逻辑抽象成通用的逻辑)
        String jsonString = JSON.toJSONString(object);

        //1.创建Request对象，这里是IndexRequest，因为添加文档就是创建倒排索引的过程。
        IndexRequest indexRequest = new IndexRequest(indexName).id(id);
        //2.准备json文档(指定索引文档内容)
        indexRequest.source(jsonString, XContentType.JSON);
        //索引响应对象
        IndexResponse indexResponse = null;
        try {
            //3.发送请求
            indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("添加索引出错:{}", e.getMessage());
            e.printStackTrace();
            XueChengPlusException.cast("添加索引出错");
        }
        String name = indexResponse.getResult().name();
        System.out.println(name);
        //新增文档 和 全量修改文档 的API完全一致，判断依据是ID：
        //- 如果新增时，ID已经存在，则修改
        //- 如果新增时，ID不存在，则新增
        return name.equalsIgnoreCase("created") || name.equalsIgnoreCase("updated");

    }

    /**
     * 修改course_public索引库的一条文档信息(这里指的是局部修改，因为全量修改的API和新增文档的API一样)
     *                                     局部修改：修改文档中的指定字段值。
     * @param indexName 索引库名称
     * @param id        主键
     * @param object    索引对象
     * @return
     */
    public Boolean updateCourseIndex(String indexName, String id, Object object) {
        //准备json数据：里面指定文档中要修改的字段。
        String jsonString = JSON.toJSONString(object);
        //准备request对象
        UpdateRequest updateRequest = new UpdateRequest(indexName, id);
        //填入json参数
        updateRequest.doc(jsonString, XContentType.JSON);

        UpdateResponse updateResponse = null;
        try {
            //更新稳定
            updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("更新索引出错:{}", e.getMessage());
            e.printStackTrace();
            XueChengPlusException.cast("更新索引出错");
        }
        DocWriteResponse.Result result = updateResponse.getResult();
        return result.name().equalsIgnoreCase("updated");
    }

    /**
     * 删除course_public索引库的一条文档数据
     * @param indexName 索引库名称
     * @param id        主键
     * @return
     */
    public Boolean deleteCourseIndex(String indexName, String id) {

        //准备Request对象
        DeleteRequest deleteRequest = new DeleteRequest(indexName, id);
        //响应对象
        DeleteResponse deleteResponse = null;
        try {
            //发送请求
            deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("删除索引出错:{}", e.getMessage());
            e.printStackTrace();
            XueChengPlusException.cast("删除索引出错");
        }
        //获取响应结果
        DocWriteResponse.Result result = deleteResponse.getResult();
        return result.name().equalsIgnoreCase("deleted");
    }
}
