package com.jeju.ormicamp.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmap")
public class TmapProperties {

    private String appKey;


    public String getAppKey() {
        return appKey;
    }


    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}
