package com.prafta.web.leave.promotion.leavepromo01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-4: 2차 회사직권 지정 요청.
 *
 * <p>대상 사용자(targetUserCd)와 지정 날짜 목록만 받는다. 대상자의 사업장/부서는 서버에서 재조회하여
 * 권한(canManageUser)·스코프를 강제한다(클라이언트 siteCd/nodeCd 불신뢰 — IDOR 차단).
 */
@Getter
@Setter
public class PromotionDesignateRequest {

    /** 지정 대상 근로자 코드. */
    private String targetUserCd;

    /** 직권 지정할 연차일 목록 (YYYYMMDD, 1일 단위). */
    private List<String> dates;
}
