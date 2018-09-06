package com.zzjz.zzpro.service.impl;

import com.zzjz.zzpro.service.SnmpService;
import com.zzjz.zzpro.util.Constant;
import com.zzjz.zzpro.util.SnmpData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 房桂堂
 * @description SnmpServiceImpl
 * @date 2018/9/6 16:59
 */
@Service
public class SnmpServiceImpl implements SnmpService {

    @Autowired
    SnmpData snmpData;

    /**
     * 系统名称缓存
     */
    private static ConcurrentHashMap<String, String> SYSNAMEMAP = new ConcurrentHashMap<>();

    /**
     * 交换机端口缓存
     */
    private static ConcurrentHashMap<String, Map<String, String>> PORTMAP = new ConcurrentHashMap<>();

    @Override
    public String getSysName(String switchIp, String community) {
        if (SYSNAMEMAP.get(switchIp) == null) {
            String sysName = snmpData.snmpGet(switchIp, community, Constant.Oid.sysName.getValue());
            SYSNAMEMAP.put(switchIp, sysName);
            return sysName;
        } else {
            return SYSNAMEMAP.get(switchIp);
        }
    }

    @Override
    public Map<String, String> getPortMap(String switchIp, String community) {
        if (PORTMAP.get(switchIp) == null) {
            Map<String, String> ipDescMap = snmpData.snmpWalk(switchIp, community, Constant.Oid.ipDescr.getValue());
            PORTMAP.put(switchIp, ipDescMap);
            return ipDescMap;
        } else {
            return PORTMAP.get(switchIp);
        }
    }
}
