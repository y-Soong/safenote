package com.prafta.web.leave.promotion.leavepromo01.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-com-008-A-4: 직권지정 대상 사용자 메타 운반체.
 *
 * <p>대상자의 사업장/부서(서버 재조회 — 클라 불신뢰)·입사일·SECOND 회차 기준 만료일을 싣는다.
 * baseAvailToDate 가 null 이면 2차 미도래(대상 아님)로 판정한다.
 */
@Getter
@Setter
public class DesignateTargetMetaVO {

    /** 대상 사용자 사업장 코드(서버 조회). */
    private String siteCd;

    /** 대상 사용자 소속 부서 코드(서버 조회, 권한 검증용). */
    private String nodeCd;

    /** 대상 사용자 입사일 (YYYYMMDD). */
    private String hireDate;

    /** SECOND 회차 기준 본연차 만료일 (YYYYMMDD). null=2차 미도래. */
    private String baseAvailToDate;
}
