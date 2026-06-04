package com.prafta.web.leaverefusal.leaverefusal01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 노무수령거부 통지 발송 대상 1건 요청 (PRAFTA-COM-001 기능1).
 *
 * <p>관리자(master/hr)가 사용지정일에 대해 통지를 발송할 대상을 List 로 전달한다.
 * 식별자(cmpnyCd) 는 본 요청이 아니라 JWT(TokenInfo)에서만 도출한다(IDOR 가드).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveRefusalNoticeRequest {

    /** 사업장 코드 */
    private String siteCd;

    /** 대상 근로자 코드 */
    private String userCd;

    /** 노무수령거부 대상일 (YYYYMMDD, =연차촉진 사용지정일) */
    private String targetYmd;
}
