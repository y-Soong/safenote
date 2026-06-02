package com.prafta.common.schedule.holiday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidayApiResponse {
    public Response response;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        public Body body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        public Items items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        public HolidayItem[] item;
    }
}