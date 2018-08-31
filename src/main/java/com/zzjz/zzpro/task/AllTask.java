package com.zzjz.zzpro.task;

import com.zzjz.zzpro.util.Constant;
import com.zzjz.zzpro.util.SnmpData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 房桂堂
 * @description AllTask
 * @date 2018/8/31 16:20
 */
public class AllTask {

    @Autowired
    SnmpData snmpData;

    private static ConcurrentHashMap<String, BigDecimal> map = new ConcurrentHashMap<>();

    /**
     * 没过10秒获取交换机流量
     */
    @Scheduled(cron = "0/10 * * * * *")
    public void timer() {
        Map<String, String> ipDescMap = new HashMap<>();
        Map<String, String> ipDescMapLong = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ipDescr.getValue());
        Map<String, String> inDataMap = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ifHCInOctets.getValue());
        Map<String, String> outDataMap = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ifHCOutOctets.getValue());
        ipDescMapLong.forEach((k, v) -> ipDescMap.put(k.substring(k.lastIndexOf(".") + 1), v));
        System.out.println(1);
    }

    public static void main(String[] args) {
        SnmpData snmpData = new SnmpData();
        Map<String, String> ipDescMap = new HashMap<>();
        String sysName = snmpData.snmpGet(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.sysName.getValue());
        Map<String, String> ipDescMapLong = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ipDescr.getValue());
        Map<String, String> inDataMap = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ifHCInOctets.getValue());
        Map<String, String> outDataMap = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ifHCOutOctets.getValue());
        ipDescMapLong.forEach((k, v) -> ipDescMap.put(k.substring(k.lastIndexOf(".") + 1), v));
        Map<String, Object> inJsonMap = new HashMap<>();
        //输入流量
        inDataMap.forEach((k, v) -> {
            String key = k.substring(k.lastIndexOf(".") + 1);
            String portName = ipDescMap.get(key);
            if (map.get(portName) == null) {
                map.put(portName, new BigDecimal(v));
            } else {
                BigDecimal oldValue = map.get(portName);
                BigDecimal nowValue = new BigDecimal(v);
                //计算速率 单位为bps
                Long speed = nowValue.subtract(oldValue).multiply(new BigDecimal(8)).divide(new BigDecimal(10)).longValue();
                inJsonMap.put("sys_name", sysName);
                inJsonMap.put("port_index", key);
                inJsonMap.put("port_name", portName);
                inJsonMap.put("speed", speed);
                inJsonMap.put("oid", k);
                inJsonMap.put("flow_type", "in");
                inJsonMap.put("insert_time", new Date());
                System.out.println(inJsonMap);
            }
        });



    }
}
