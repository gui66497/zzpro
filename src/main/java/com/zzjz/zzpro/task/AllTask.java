package com.zzjz.zzpro.task;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zzjz.zzpro.util.Constant;
import com.zzjz.zzpro.util.SnmpData;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
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

    @Value("${switches}")
    String switches;

    private static ConcurrentHashMap<String, List> IN_MAP = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, List> OUT_MAP = new ConcurrentHashMap<>();

    /**
     * 每过20秒获取交换机流量并计算速率
     */
    @Scheduled(cron = "0/20 * * * * *")
    public void snmpSpeed() {
        System.out.println(switches);
        JsonArray jsonArray = new JsonParser().parse(switches).getAsJsonArray();
        for (JsonElement element : jsonArray) {
            System.out.println(element);
            String switchIp = ((JsonObject) element).get("ip").getAsString();
            String community = ((JsonObject) element).get("community").getAsString();
            JsonArray ports = ((JsonObject) element).getAsJsonArray("ports");
            Gson gson = new Gson();
            ArrayList portList = gson.fromJson(ports, ArrayList.class);

            RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
            BulkRequest request = new BulkRequest();
            SnmpData snmpData = new SnmpData();
            Map<String, String> ipDescMap = new HashMap<>();
            String sysName = snmpData.snmpGet(switchIp, community, Constant.Oid.sysName.getValue());
            Map<String, String> ipDescMapLong = snmpData.snmpWalk(switchIp, community, Constant.Oid.ipDescr.getValue());
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
                    Long speed = nowValue.subtract(oldValue).multiply(new BigDecimal(8)).divide(new BigDecimal(interval), BigDecimal.ROUND_HALF_UP).longValue();
                    inJsonMap.put("sys_name", sysName);
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
                    Long speed = nowValue.subtract(oldValue).multiply(new BigDecimal(8)).divide(new BigDecimal(interval), BigDecimal.ROUND_HALF_UP).longValue();
                    outJsonMap.put("sys_name", sysName);
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

    public static void main(String[] args) {

    }
}
