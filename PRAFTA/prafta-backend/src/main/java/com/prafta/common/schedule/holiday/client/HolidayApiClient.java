package com.prafta.common.schedule.holiday.client;

import java.net.URI;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.prafta.common.schedule.holiday.dto.HolidayApiResponse;

@Component
public class HolidayApiClient {

    private final RestTemplate holidayRestTemplate;
    private final HolidayApiProperties props;

    public HolidayApiClient(@Qualifier("holidayRestTemplate") RestTemplate holidayRestTemplate, HolidayApiProperties props) {
        this.holidayRestTemplate = holidayRestTemplate;
        this.props = props;
        validateProps();
    }

    private void validateProps() {
    	String key = System.getenv("HOLIDAY_API_SERVICE_KEY");
    	
        if (!StringUtils.hasText(props.getServiceKey())) {
            // 키 없으면 "조용히 실패"하지 말고 앱 시작부터 막는 게 안전
            throw new IllegalStateException("HOLIDAY_API_SERVICE_KEY is empty. Set env var HOLIDAY_API_SERVICE_KEY.");
        }
        if (!StringUtils.hasText(props.getBaseUrl())) {
            throw new IllegalStateException("HOLIDAY_API_BASE_URL is empty.");
        }
    }

    public HolidayApiResponse fetchHolidays(int solYear) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(props.getBaseUrl())
                .queryParam("solYear", solYear)
                .queryParam("ServiceKey", props.getServiceKey()) // 민감정보: 로그에 절대 남기지 말 것
                .queryParam("_type", props.getType())
                .queryParam("numOfRows", props.getNumOfRows())
                .build(true)     // 인코딩 이슈 방지
                .toUri();
        
        var res = holidayRestTemplate.getForEntity(uri, String.class);

        // 이 uri를 로그로 찍으면 ServiceKey 유출됩니다. 절대 금지.
        return holidayRestTemplate.getForObject(uri, HolidayApiResponse.class);
    }
}