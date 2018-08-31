package com.zzjz.zzpro.controller;

import com.zzjz.zzpro.util.Constant;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author 房桂堂
 * @description ElasticController
 * @date 2018/8/31 15:02
 */
@Component
@Path("es")
public class ElasticController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticController.class);

    /**
     * 通过nmap获取当前在线主机
     * @return
     */
    @GET
    @Path("online_hosts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String isReachAble() {
        //查询过去十分钟的所有ping得通的nmap数据
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = DateTime.now().minusMinutes(Constant.NMAP_INTERVAL).toString(format);
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.NMAP);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(500);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchPhraseQuery("status.state", "up"))
                .must(QueryBuilders.rangeQuery("@timestamp")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchRequest.source(searchSourceBuilder);
        Set<String> onlineHosts = new HashSet<>();
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            while (it.hasNext()) {
                SearchHit hit = (SearchHit) it.next();
                String ipv4 = (String) hit.getSourceAsMap().get("ipv4");
                onlineHosts.add(ipv4);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "执行出错:" + e.getMessage();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return onlineHosts.toString();
    }
}
