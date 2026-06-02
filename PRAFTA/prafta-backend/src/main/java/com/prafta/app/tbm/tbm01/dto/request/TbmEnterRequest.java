package com.prafta.app.tbm.tbm01.dto.request;

import lombok.Data;

/**
 * prafta-app-004-C1: TBM 입실 요청(JSON 바디).
 * <p>정규직(REGULAR) MVP — 본인 디바이스 자가 입실(ENTRY_TYPE_CD='SELF_DEVICE') 경로.
 * <p>USER_CD/CMPNY_CD/SITE_CD 는 바디로 받지 않는다(JWT 출처, IDOR 차단). USER_TYPE_CD='REGULAR' 고정.
 * <p>lat/lon 은 GPS 검증/거리 기록용이며 응답·로그에는 좌표 원본을 노출하지 않는다(거리 m만).
 */
@Data
public class TbmEnterRequest {
    private String sessionCd;
    private String entryPwd;
    private Double lat;
    private Double lon;
}
