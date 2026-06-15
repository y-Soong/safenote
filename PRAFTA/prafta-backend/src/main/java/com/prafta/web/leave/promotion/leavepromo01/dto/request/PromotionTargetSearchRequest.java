package com.prafta.web.leave.promotion.leavepromo01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-4: 2차 회사직권 대상자 조회 요청.
 *
 * <p>siteCd 는 세션 고정 사업장(JWT gv_siteCd)과 일치해야 하며(cross-site IDOR 가드 — Param 에서 검증),
 * 노드/하위 cascade·1년차 필터는 서버 스코프로 강제한다.
 */
@Getter
@Setter
public class PromotionTargetSearchRequest {

    /** 사업장 코드(세션 고정 사업장과 일치 필수). */
    private String siteCd;

    /** 소속부서 노드 코드(없으면 사업장 전체 루트 기준). */
    private String nodeCd;

    /** 하위부서 포함 여부 Y/N. */
    private String incSubNodeYn;

    /** 사용자명 LIKE 검색어. */
    private String userNm;

    /** 1년차 구분 필터: ALL / UNDER1(1년차 미만) / OVER1(1년차 이상). */
    private String tenureFilter;

    /** 프론트 호환 별칭(tenureType). tenureFilter 가 비면 본 값을 사용(A-8 화면 파라미터명). */
    private String tenureType;

    /** tenureFilter 우선, 비면 tenureType. 둘 다 비면 null. */
    public String resolveTenure() {
        if (tenureFilter != null && !tenureFilter.isBlank()) {
            return tenureFilter;
        }
        return (tenureType != null && !tenureType.isBlank()) ? tenureType : null;
    }
}
