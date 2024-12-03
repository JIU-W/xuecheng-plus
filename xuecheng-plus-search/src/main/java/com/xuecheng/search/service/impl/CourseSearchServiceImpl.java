package com.xuecheng.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.search.dto.SearchCourseParamDto;
import com.xuecheng.search.dto.SearchPageResultDto;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.CourseSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程搜索service实现类
 * @date 2024-12-01
 */
@Slf4j
@Service
public class CourseSearchServiceImpl implements CourseSearchService {

    @Value("${elasticsearch.course.index}")
    private String courseIndexStore;

    @Value("${elasticsearch.course.source_fields}")
    private String sourceFields;

    @Autowired
    private RestHighLevelClient client;


    public SearchPageResultDto<CourseIndex> queryCoursePubIndex(PageParams pageParams,
                                                                SearchCourseParamDto courseSearchParam) {

        //1.创建Request，设置索引库
        SearchRequest searchRequest = new SearchRequest(courseIndexStore);

        //2.组织请求参数
        //准备bool查询
        BoolQueryBuilder bool = QueryBuilders.boolQuery();


        //source源字段过滤
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        String[] sourceFieldsArray = sourceFields.split(",");
        searchSourceBuilder.fetchSource(sourceFieldsArray, new String[]{});


        if (courseSearchParam == null) {
            courseSearchParam = new SearchCourseParamDto();
        }
        //关键字搜索：  采用multi_match全文检索查询
        if (StringUtils.isNotEmpty(courseSearchParam.getKeywords())) {
            //匹配关键字     关键字需要匹配课程的名称、 课程内容：name  description
            MultiMatchQueryBuilder multiMatchQueryBuilder =
                    QueryBuilders.multiMatchQuery(courseSearchParam.getKeywords(), "name", "description");
            //设置匹配的最小占比
            multiMatchQueryBuilder.minimumShouldMatch("70%");
            //提升另个字段的Boost值
            multiMatchQueryBuilder.field("name", 10);
            bool.must(multiMatchQueryBuilder);
        }

        //filter过滤：不会计算相关度得分，效率更高。
        //课程大分类过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            //term查询：属于精确查询。词条级别的查询。
            //也就是说不会对用户输入的搜索条件再分词，而是作为一个词条，与搜索的字段内容精确值匹配。
            bool.filter(QueryBuilders.termQuery("mtName", courseSearchParam.getMt()));
        }
        //课程小分类过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            bool.filter(QueryBuilders.termQuery("stName", courseSearchParam.getSt()));
        }
        //难度等级过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            bool.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }

        //布尔查询
        searchSourceBuilder.query(bool);

        //分页
        Long pageNo = pageParams.getPageNo();
        Long pageSize = pageParams.getPageSize();
        int start = (int) ((pageNo - 1) * pageSize);
        //- from：从第几个文档开始
        //- size：总共查询几个文档

        //"from": 分页开始的位置，默认为0
        //"size": 每页文档数量，默认10
        searchSourceBuilder.from(start).size(Math.toIntExact(pageSize));

        //排序
        Integer sortType = courseSearchParam.getSortType();
        if(sortType != null){
            if(sortType == 1){
                //按价格升序
                searchSourceBuilder.sort("price", SortOrder.ASC);
            } else if (sortType == 2) {
                //按价格降序
                searchSourceBuilder.sort("price", SortOrder.DESC);
            }
        }

        //高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        //设置高亮字段
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);

        //请求搜索
        searchRequest.source(searchSourceBuilder);

        //聚合设置
        buildAggregation(searchRequest);

        SearchResponse searchResponse = null;
        try {
            //发送请求
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("课程搜索异常：{}", e.getMessage());
            return new SearchPageResultDto<CourseIndex>(new ArrayList(), 0, 0, 0);
        }

        //结果集处理
        SearchHits hits = searchResponse.getHits();
        //记录总数
        TotalHits totalHits = hits.getTotalHits();
        long tatal = totalHits.value;
        //搜索结果
        SearchHit[] searchHits = hits.getHits();

        //数据列表
        List<CourseIndex> list = new ArrayList<>();

        for (SearchHit hit : searchHits) {

            String sourceAsString = hit.getSourceAsString();
            CourseIndex courseIndex = JSON.parseObject(sourceAsString, CourseIndex.class);

            //取出source
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            //课程id
            Long id = courseIndex.getId();
            //取出名称
            String name = courseIndex.getName();

            //取出高亮字段内容
            //Map集合：key是高亮字段名称，值是HighlightField对象，代表高亮值
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();

            if (highlightFields != null) {
                HighlightField nameField = highlightFields.get("name");
                if (nameField != null) {
                    Text[] fragments = nameField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (Text str : fragments) {
                        stringBuffer.append(str.string());
                    }
                    name = stringBuffer.toString();
                }
            }
            courseIndex.setId(id);
            //高亮的结果替换CourseIndex中的非高亮结果
            courseIndex.setName(name);

            list.add(courseIndex);

        }

        //封装搜索结果
        SearchPageResultDto<CourseIndex> pageResult =
                new SearchPageResultDto<>(list, tatal, pageNo, pageSize);

        //获取聚合结果
        List<String> mtList = getAggregation(searchResponse.getAggregations(), "mtAgg");
        List<String> stList = getAggregation(searchResponse.getAggregations(), "stAgg");

        pageResult.setMtList(mtList);
        pageResult.setStList(stList);
        return pageResult;
    }

    /**
     * 聚合设置
     * @param request
     */
    private void buildAggregation(SearchRequest request) {
        //这个聚合会根据mtName字段的值进行分组，并返回每个分组的文档数量。
        //size(100)表示最多返回100个分组。
        request.source().aggregation(AggregationBuilders
                .terms("mtAgg")  //terms聚合：按分类聚合
                .field("mtName")
                .size(100)
        );
        //这个聚合会根据stName字段的值进行分组，并返回每个分组的文档数量。
        //size(100)表示最多返回100个分组。
        request.source().aggregation(AggregationBuilders
                .terms("stAgg")
                .field("stName")
                .size(100)
        );
    }

    /**
     * 解析聚合结果
     * @param aggregations
     * @param aggName
     * @return
     */
    private List<String> getAggregation(Aggregations aggregations, String aggName) {
        //4.1.根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        //4.2.获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.3.遍历
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            // 4.4.获取key
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }


}
