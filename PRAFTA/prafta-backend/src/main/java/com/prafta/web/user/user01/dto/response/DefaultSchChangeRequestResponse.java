package com.prafta.web.user.user01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * PRAFTA-001(기본근무타입-승인제, 웹): 기본 근무타입 변경 요청 등록 응답.
 *
 * <p>"승인 전 미반영" 설계 원칙에 따라 요청 등록 직후에는 {@code TB_USER.DEFAULT_SCH_CD} 가
 * 바뀌지 않으므로, 변경된 근무타입 값 대신 등록된 요청 식별값/상태만 돌려준다.
 */
@Getter
@Builder
public class DefaultSchChangeRequestResponse {

    /** 등록된 요청 ID(TB_USER_ATTD_REQ.REQ_ID). */
    private String reqId;

    /** 등록 직후 상태 — 항상 '01'(신청). */
    private String reqStatus;
}
