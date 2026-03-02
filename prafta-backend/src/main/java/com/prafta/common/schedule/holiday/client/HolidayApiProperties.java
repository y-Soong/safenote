package com.prafta.common.schedule.holiday.client;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "holiday.api")
public class HolidayApiProperties {

    private String baseUrl;
    private String serviceKey;
    private int numOfRows = 100;
    private String type = "json";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 7000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }

    public int getNumOfRows() { return numOfRows; }
    public void setNumOfRows(int numOfRows) { this.numOfRows = numOfRows; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}