package com.zzjz.zzpro.service.impl;

import com.google.gson.JsonObject;
import com.zzjz.zzpro.service.ElasticService;
import com.zzjz.zzpro.util.Constant;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author 房桂堂
 * @description ElasticServiceImpl
 * @date 2018/8/13 9:46
 */
@Service
public class ElasticServiceImpl implements ElasticService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticServiceImpl.class);

    @Override
    public JsonObject snmpData(String ip, String portName, int hours) {
        //得到in和out各自的平均速率
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = new DateTime().minusHours(hours).toString(format);
        LOGGER.info("查询的起始时间为" + oldTime);
        long inAverage = calAverageSpeed("in", ip, portName, oldTime);
        long outAverage = calAverageSpeed("out", ip, portName, oldTime);
        long inData = inAverage * hours * 60 * 60;
        long outData = outAverage * hours * 60 * 60;
        long allData =inData + outData;
        //将数据可读化
        String inDataRead = Constant.bit2kb(inData, 1);
        String outDataRead = Constant.bit2kb(outData, 1);
        String allDataRead = Constant.bit2kb(allData, 1);
        JsonObject json = new JsonObject();
        json.addProperty("in", inDataRead);
        json.addProperty("out", outDataRead);
        json.addProperty("all", allDataRead);
        return json;
    }

    public long calAverageSpeed(String flowType, String ip, String portName, String oldTime) {
        String format = "yyyy-MM-dd HH:mm:ss";
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.SNMP_SPEED);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchPhraseQuery("ip", ip))
                .must(QueryBuilders.matchPhraseQuery("flow_type", flowType))
                .must(QueryBuilders.matchPhraseQuery("port_name", portName))
                .must(QueryBuilders.rangeQuery("insert_time")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchSourceBuilder.sort(SortBuilders.fieldSort("insert_time").order(SortOrder.ASC));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            long allNum = 0L;
            while (it.hasNext()) {
                SearchHit hit = (SearchHit) it.next();
                allNum += Long.parseLong(hit.getSourceAsMap().get("speed").toString());
            }
            return allNum * 1024 / searchResponse.getHits().totalHits;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static void main(String[] args) {
    }


}
