package com.prafta.web.user.user01.result;

/**
 * 소속이동 대상 사용자 기본 정보(회사 스코프 조회 결과) — PRAFTA-WEB_001-1.
 *
 * <p>대상자의 현재 소속(사업장/부서)과 고용형태를 담는다. 현재 SITE_CD 는
 * 불가케이스 ①(사업장 관리자)/④(교대조) 판정과 예약 스냅샷(FROM_*) 에 사용한다.
 */
public record UserTransferBasicResult(
    String userCd
    , String userId
    , String userNm
    , String siteCd
    , String nodeCd
    , String employmentType
) {}
