package com.zzjz.zzpro.app;

import com.zzjz.zzpro.controller.ElasticController;
import com.zzjz.zzpro.controller.SnmpController;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;
import javax.ws.rs.ApplicationPath;

/**
 * @author 房桂堂
 * @description JerseyConfig
 * @date 2018/7/27 12:52
 */
@Component
@ApplicationPath("zzpro")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        //构造函数，在这里注册需要使用的内容，（过滤器，拦截器，API等）
        register(ElasticController.class);
        register(SnmpController.class);
    }
}
