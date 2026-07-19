package com.prafta.platform.location.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 위치정보 조회 요청 DTO (GET /platformApi/location/gps-lists).
 *
 * <p>3개 파라미터 전부 필수(누락 시 PLATFORM_400_011).
 */
@Getter
@Setter
@NoArgsConstructor
public class GpsListRequest {

    /** 열람 대상 회사코드(필수). */
    private String cmpnyCd;

    /** 열람 대상 사업장코드(필수). */
    private String siteCd;

    /** 열람 대상 일자 YYYYMMDD(필수, 하이픈 입력 허용). */
    private String date;
}
