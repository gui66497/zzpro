package com.zzjz.zzpro.entity;

import java.util.List;

/**
 * @author 房桂堂
 * @description 交换机实体类
 * @date 2018/9/6 16:33
 */
public class Switches {

    /**
     * ip
     */
    private String ip;

    /**
     * 协议
     */
    private String community;

    /**
     * 端口
     */
    private List<String> ports;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }
}
