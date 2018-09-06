package com.zzjz.zzpro.task;

import com.zzjz.zzpro.entity.Switches;
import com.zzjz.zzpro.service.SnmpService;
import com.zzjz.zzpro.util.Constant;
import com.zzjz.zzpro.util.SnmpData;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 房桂堂
 * @description AllTask
 * @date 2018/8/31 16:20
 */
@Component
public class AllTask {

    private final static Logger LOGGER = LoggerFactory.getLogger(AllTask.class);

    @Autowired
    SnmpData snmpData;

    @Autowired
    SnmpService snmpService;

    @Value("${switches}")
    String switches;

    private static ConcurrentHashMap<String, List> IN_MAP = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, List> OUT_MAP = new ConcurrentHashMap<>();

    /**
     * 每过30秒获取交换机流量并计算速率(单位 kbps)
     */
    @Scheduled(cron = "0/30 * * * * *")
    public void snmpSpeed() {
        //先从switch表中获取所有交换机 之前是读的配置文件
        //JsonArray jsonArray = new JsonParser().parse(switches).getAsJsonArray();
        List<Switches> switches = getEnabledSwitches();
        for (Switches switchesOne : switches) {
            System.out.println(switchesOne);
            String switchIp = switchesOne.getIp();
            String community = switchesOne.getCommunity();
            List<String> portList = switchesOne.getPorts();

            RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
            BulkRequest request = new BulkRequest();
            SnmpData snmpData = new SnmpData();
            Map<String, String> ipDescMap = new HashMap<>();
            String sysName = snmpService.getSysName(switchIp, community);
            Map<String, String> ipDescMapLong = snmpService.getPortMap(switchIp, community);
            Map<String, String> inDataMap = snmpData.snmpWalk(switchIp, community, Constant.Oid.ifHCInOctets.getValue());
            Map<String, String> outDataMap = snmpData.snmpWalk(switchIp, community, Constant.Oid.ifHCOutOctets.getValue());
            long sec = System.currentTimeMillis();
            ipDescMapLong.forEach((k, v) -> ipDescMap.put(k.substring(k.lastIndexOf(".") + 1), v));
            Map<String, Object> inJsonMap = new HashMap<>();
            String dayStr = new DateTime().toString("yyyy.MM.dd");
            //输入流量
            inDataMap.forEach((k, v) -> {
                String key = k.substring(k.lastIndexOf(".") + 1);
                String portName = ipDescMap.get(key);
                //只存储ports中指定的端口
                if (!portList.contains(portName)) {
                    return;
                }
                if (IN_MAP.get(portName) != null) {
                    //没有缓存的计算不了
                    long oldTime = (long) IN_MAP.get(portName).get(0);
                    BigDecimal oldValue = (BigDecimal) IN_MAP.get(portName).get(1);
                    BigDecimal nowValue = new BigDecimal(v);
                    long newTime = System.currentTimeMillis();
                    long interval = (newTime - oldTime) / 1000;
                    //计算速率 单位为bps
                    System.out.println("输入interval:" + interval);
                    Long speed = nowValue.subtract(oldValue).multiply(new BigDecimal(8))
                                         .divide(new BigDecimal(interval), BigDecimal.ROUND_HALF_UP)
                                         .divide(BigDecimal.valueOf(1024), BigDecimal.ROUND_HALF_UP).longValue();
                    inJsonMap.put("sys_name", sysName);
                    inJsonMap.put("ip", switchIp);
                    inJsonMap.put("port_index", key);
                    inJsonMap.put("port_name", portName);
                    inJsonMap.put("speed", speed);
                    inJsonMap.put("oid", k);
                    inJsonMap.put("flow_type", "in");
                    inJsonMap.put("insert_time", new Date());
                    request.add(new IndexRequest("snmp_speed-" + dayStr, "doc").source(inJsonMap));
                }
                //将此次值缓存
                IN_MAP.put(portName, Arrays.asList(sec, new BigDecimal(v)));
            });
            //输出流量
            Map<String, Object> outJsonMap = new HashMap<>();
            outDataMap.forEach((k, v) -> {
                String key = k.substring(k.lastIndexOf(".") + 1);
                String portName = ipDescMap.get(key);
                //只存储ports中指定的端口
                if (!portList.contains(portName)) {
                    return;
                }
                if (OUT_MAP.get(portName) != null) {
                    //没有缓存的计算不了
                    long oldTime = (long) OUT_MAP.get(portName).get(0);
                    BigDecimal oldValue = (BigDecimal) OUT_MAP.get(portName).get(1);
                    BigDecimal nowValue = new BigDecimal(v);
                    long newTime = System.currentTimeMillis();
                    long interval = (newTime - oldTime)/1000;
                    System.out.println("输出interval:" + interval);
                    //计算速率 单位为bps
                    Long speed = nowValue.subtract(oldValue).multiply(new BigDecimal(8))
                            .divide(new BigDecimal(interval), BigDecimal.ROUND_HALF_UP)
                            .divide(BigDecimal.valueOf(1024), BigDecimal.ROUND_HALF_UP).longValue();
                    outJsonMap.put("sys_name", sysName);
                    outJsonMap.put("ip", switchIp);
                    outJsonMap.put("port_index", key);
                    outJsonMap.put("port_name", portName);
                    outJsonMap.put("speed", speed);
                    outJsonMap.put("oid", k);
                    outJsonMap.put("flow_type", "out");
                    outJsonMap.put("insert_time", new Date());
                    request.add(new IndexRequest("snmp_speed-" + dayStr, "doc").source(outJsonMap));
                }
                //将此次值缓存
                OUT_MAP.put(portName, Arrays.asList(sec, new BigDecimal(v)));
            });
            BulkResponse bulkResponse;
            try {
                if (request.requests().size() > 0) {
                    bulkResponse = client.bulk(request);
                    LOGGER.info("snmp插入执行结果:" + (bulkResponse.hasFailures() ? "有错误" : "成功"));
                    LOGGER.info("snmp插入执行用时:" + bulkResponse.getTook().getMillis() + "毫秒");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取启用的交换机列表
     * @return 列表
     */
    public List<Switches> getEnabledSwitches() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.SWITCHES);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("enabled", true));
        searchSourceBuilder.size(100);
        searchRequest.source(searchSourceBuilder);
        List<Switches> switches = new ArrayList<>();
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            while (it.hasNext()) {
                Switches switchOne = new Switches();
                SearchHit hit = (SearchHit) it.next();
                switchOne.setIp(hit.getId());
                switchOne.setCommunity(hit.getSourceAsMap().get("community").toString());
                switchOne.setPorts((List<String>) hit.getSourceAsMap().get("ports"));
                switches.add(switchOne);
            }
            //获取最近的一次数量统计
            return switches;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SnmpData snmpData = new SnmpData();
        Map<String, String> ipDescMapLong = snmpData.snmpWalk("192.168.1.1", "123qweASD", Constant.Oid.ipDescr.getValue());
        System.out.println(ipDescMapLong);
    }
}
