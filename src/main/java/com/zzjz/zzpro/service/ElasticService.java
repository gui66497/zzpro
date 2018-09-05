package com.zzjz.zzpro.service;

import com.google.gson.JsonObject;

/**
 * @author 房桂堂
 * @description ElasticService
 * @date 2018/8/13 9:45
 */
public interface ElasticService {

    /**
     * 获取指定端口指定时间段的输入输出流量.
     * @param hours 小时数
     * @param portName 端口
     * @return 流量
     */
    JsonObject snmpData(String ip, String portName, int hours);

}
