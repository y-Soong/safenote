package com.prafta.web.leave.promotion.leavepromo01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 사용촉진 1차 현황 조회 요청(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §5-1).
 *
 * <p>{@code PromotionTargetSearchRequest}(2차 조회)와 조건 처리를 미러하되 <b>1년차 구분
 * (tenureType/tenureFilter)은 두지 않는다</b> — 1차 대상은 구조적으로 1년차 이상(ACTIVE
 * STATUTORY_ANNUAL 보유)뿐이라 필터가 무의미하다. 죽은 필드를 남기면 후속 개발자가 필터가 있다고
 * 오해하므로 Request 를 별도로 둔다.
 *
 * <p>siteCd 는 요청값이지만 서비스에서 사업장 인가 원장(SiteAccessService)으로 검증한 뒤 사용한다
 * (cross-site IDOR 가드). cmpnyCd/userCd 는 토큰 도출값만 사용한다.
 */
@Getter
@Setter
public class PromotionFirstTargetSearchRequest {

    /** 사업장 코드(사업장 인가 통과 필수). */
    private String siteCd;

    /** 소속부서 노드 코드(없으면 사업장 전체 루트 기준). */
    private String nodeCd;

    /** 하위부서 포함 여부 Y/N. */
    private String incSubNodeYn;

    /** 사용자명 LIKE 검색어. */
    private String userNm;
}
