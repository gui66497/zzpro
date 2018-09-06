package com.zzjz.zzpro.util;

import java.math.BigDecimal;

/**
 * @author 房桂堂
 * @description Constant
 * @date 2018/8/31 15:09
 */
public class Constant {

    /**
     * Elasticsearch的ip
     */
    public static final String ES_HOST = "192.168.1.188";

    /**
     * Elasticsearch的rest端口
     */
    public static final int ES_PORT = 9200;

    /**
     * Elasticsearch的rest端口
     */
    public static final String ES_METHOD = "http";

    public static final Integer NMAP_INTERVAL = 10;

    public static final String NMAP = "nmap-*";

    //todo 改造支持多交换机和多接口
    /**
     * 交换机ip1.3.6.1.2.1.2.2.1.10
     */
    public static final String SWITCH_IP = "192.168.1.1";

    /**
     * 交换机协议
     */
    public static final String COMMUNITY = "123qweASD";

    /**
     * 需要监控的交换机端口
     */
    public static final String SWITCH_PORT = "GigabitEthernet1/0/1";

    /**
     * 32位交换机流量存储最大值 2^32
     */
    public static final long SNMP_MAX_DATA = 4294967296L;

    /**
     * oid的枚举
     */
    public enum Oid {

        /**
         * 交换机名称
         */
        sysName(".1.3.6.1.2.1.1.5.0"),

        /**
         * 接口的对应的文字描述
         */
        ipDescr(".1.3.6.1.2.1.2.2.1.2 "),

        /**
         * 接口输入值(64位)
         */
        ifHCInOctets(".1.3.6.1.2.1.31.1.1.1.6"),

        /**
         * 接口输出值(64位)
         */
        ifHCOutOctets(".1.3.6.1.2.1.31.1.1.1.10");

        /**
         * 对应的oid值
         */
        private String value;

        /**
         * 构造方法
         * @param value
         */
        private Oid(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * bit(位)根据长度转成kb(千字节)和mb(兆字节)或gb
     * @param bytes 字节
     * @return String
     */
    public static String bit2kb(long bytes, int scale) {
        BigDecimal filesize = new BigDecimal(bytes);
        //8 * 1024 * 1024 * 2024 * 1024
        BigDecimal petabyte = new BigDecimal( 8796093022208L);
        BigDecimal returnValue = filesize.divide(petabyte, scale, BigDecimal.ROUND_HALF_UP);
        if (returnValue.floatValue() >= 1) {
            return (returnValue + "PB");
        }
        //8 * 1024 * 1024 * 2024
        BigDecimal gigabyte = new BigDecimal( 8589934592L);
        returnValue = filesize.divide(gigabyte, scale, BigDecimal.ROUND_HALF_UP);
        if (returnValue.floatValue() >= 1) {
            return (returnValue + "GB");
        }
        BigDecimal megabyte = new BigDecimal(8 * 1024 * 1024);
        returnValue = filesize.divide(megabyte, scale, BigDecimal.ROUND_HALF_UP);
        if (returnValue.floatValue() >= 1) {
            return (returnValue + "MB");
        }
        BigDecimal kilobyte = new BigDecimal(8 * 1024);
        returnValue = filesize.divide(kilobyte, scale, BigDecimal.ROUND_HALF_UP);
        return (returnValue + "KB");
    }

    /**
     * 交换机实时流量表
     */
    public static final String SNMP_SPEED = "snmp_speed-*";

    /**
     * 交换机表
     */
    public static final String SWITCHES = "switch";

}
