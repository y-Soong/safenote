package com.prafta.platform.location.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대상 회사 사업장 목록 조회 요청 DTO (GET /platformApi/location/site-lists).
 */
@Getter
@Setter
@NoArgsConstructor
public class LocationSiteListRequest {

    /** 열람 대상 회사코드(필수). */
    private String cmpnyCd;
}
