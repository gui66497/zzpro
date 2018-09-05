package com.zzjz.zzpro.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zzjz.zzpro.service.ElasticService;
import com.zzjz.zzpro.util.Constant;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * @author 房桂堂
 * @description SnmpController
 * @date 2018/9/5 10:17
 */
@Component
@Path("snmp")
public class SnmpController {

    private final static Logger LOGGER = LoggerFactory.getLogger(SnmpController.class);

    @Autowired
    ElasticService elasticService;

    /**
     * 获取指定端口指定时间段的输入输出流量.
     * @return 流量
     */
    @POST
    @Path("snmpData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String snmpData(@Context UriInfo ui) {
        String ip = ui.getQueryParameters().getFirst("ip");
        String portName = ui.getQueryParameters().getFirst("portName");
        int hours = Integer.parseInt(ui.getQueryParameters().getFirst("hours"));
        JsonObject json = elasticService.snmpData(ip, portName, hours);
        return json.toString();
    }

    /**
     * 获取指定时间到现在的流量趋势(包括输入,输出,总计)
     * @return 趋势
     */
    @POST
    @Path("snmpSpeedTrend")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String snmpDataTrend(@Context UriInfo ui) {
        String ip = ui.getQueryParameters().getFirst("ip");
        String portName = ui.getQueryParameters().getFirst("portName");
        int hours = Integer.parseInt(ui.getQueryParameters().getFirst("hours"));
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = DateTime.now().minusHours(hours).toString(format);
        //计算出分组时间间隔
        int interval= (hours * 60) / 10;
        LOGGER.info("查询的起始时间为" + oldTime);
        SearchRequest searchRequest = new SearchRequest(Constant.SNMP_SPEED);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(0);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchPhraseQuery("ip", ip))
                .should(QueryBuilders.matchPhraseQuery("port_name", portName))
                .must(QueryBuilders.rangeQuery("insert_time")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai"))
                .minimumShouldMatch(1));
        searchSourceBuilder.aggregation(AggregationBuilders.dateHistogram("时间")
                .field("insert_time")
                .timeZone(DateTimeZone.forID("Asia/Shanghai"))
                .dateHistogramInterval(DateHistogramInterval.minutes(interval))
                .minDocCount(1)
                .subAggregation(AggregationBuilders.terms("类型").field("flow_type").size(2)
                        .subAggregation(AggregationBuilders.avg("速率").field("speed"))));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            ParsedDateHistogram histogram1 = searchResponse.getAggregations().get("时间");
            JsonArray timeArray = new JsonArray();
            JsonArray inSpeedArray = new JsonArray();
            JsonArray outSpeedArray = new JsonArray();
            JsonArray allSpeedArray  = new JsonArray();
            for(Histogram.Bucket bt : histogram1.getBuckets())
            {
                timeArray.add(new DateTime(bt.getKeyAsString()).toString("yyyy-MM-dd HH:mm:ss"));
                Terms terms = bt.getAggregations().get("类型");
                ParsedAvg inAvg = terms.getBucketByKey("in").getAggregations().get("速率");
                ParsedAvg outAvg = terms.getBucketByKey("out").getAggregations().get("速率");
                inSpeedArray.add((long) inAvg.getValue());
                outSpeedArray.add((long) outAvg.getValue());
                allSpeedArray.add((long) inAvg.getValue() + (long) outAvg.getValue());
                System.out.println(bt.getKey() + " " + bt.getDocCount());
            }
            System.out.println("分组个数"+timeArray.size());
            //组织为json形式
            //时间点
            JsonObject oJson = new JsonObject();
            //输入
            JsonObject inJson = new JsonObject();
            inJson.addProperty("type", "in");
            inJson.add("datas", inSpeedArray);
            //输出
            JsonObject outJson = new JsonObject();
            outJson.addProperty("type", "out");
            outJson.add("datas", outSpeedArray);
            //总计
            JsonObject allJson = new JsonObject();
            allJson.addProperty("type", "all");
            allJson.add("datas", allSpeedArray);

            JsonArray dataArray = new JsonArray();
            dataArray.add(inJson);
            dataArray.add(outJson);
            dataArray.add(allJson);
            oJson.add("timeArr", timeArray);
            oJson.add("timeData", dataArray);
            System.out.println(oJson.toString());
            return oJson.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
