package com.zzjz.zzpro.service;

import java.util.Map;

/**
 * @author 房桂堂
 * @description SnmpService
 * @date 2018/9/6 16:59
 */
public interface SnmpService {

    /**
     * 获取交换机的系统名
     * @param switchIp ip
     * @param community 协议
     * @return str
     */
    String getSysName(String switchIp, String community);

    /**
     * 获取指定交换机的端口map
     * @param switchIp ip
     * @param community 协议
     * @return map
     */
    Map<String, String> getPortMap(String switchIp, String community);
}
