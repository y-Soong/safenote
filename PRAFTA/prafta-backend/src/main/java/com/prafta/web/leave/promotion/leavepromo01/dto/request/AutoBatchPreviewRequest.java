package com.prafta.web.leave.promotion.leavepromo01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-5: 자동배치 프리뷰 요청(전략/기간 + 조회조건 스냅샷).
 *
 * <p>프론트(LeavePromotionAutoBatchPop) body = { strategy, windowFrom, windowTo, siteCd, nodeCd,
 * incSubNodeYn, userNm, tenureType }. siteCd 는 세션 고정 사업장과 일치해야 한다(Param 에서 IDOR 가드).
 */
@Getter
@Setter
public class AutoBatchPreviewRequest {

    /** 'YEAR_END' | 'MIN_OVERLAP' */
    private String strategy;

    /** 배치 윈도 시작 (YYYYMMDD) */
    private String windowFrom;

    /** 배치 윈도 종료 (YYYYMMDD) */
    private String windowTo;

    /** 조회 사업장(세션 고정 사업장과 일치 필수) */
    private String siteCd;

    /** 조회 노드(null=루트) */
    private String nodeCd;

    /** 하위노드 포함('Y'/'N') */
    private String incSubNodeYn;

    /** 사용자명 LIKE 필터 */
    private String userNm;

    /** 1년차 필터('ALL'/'OVER1'/'UNDER1'). 프론트는 tenureType 으로도 전달 가능. */
    private String tenureFilter;

    /** 프론트 호환 별칭(tenureType). tenureFilter 가 비면 본 값을 사용. */
    private String tenureType;
}
