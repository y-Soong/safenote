package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * E10 정규직 관리자 대리입실 요청 바디(prafta-051 R-B).
 *
 * <p>대상 정규직 USER_CD 만 받는다. 회사/사업장/관리자 식별자는 JWT 에서 도출하며, 대상이 토큰
 * 스코프(세션 사업장/노드) 내인지는 서버가 재검증한다(IDOR 차단). 비번/GPS 는 받지 않는다(D-4).
 */
@Getter
@Setter
public class AdminManagerDirectRequest {
    private String userCd;
}
