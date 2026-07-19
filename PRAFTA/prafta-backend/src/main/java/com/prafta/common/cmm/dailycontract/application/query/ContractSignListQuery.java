package com.prafta.common.cmm.dailycontract.application.query;

/**
 * 서명 이력 목록 조회 쿼리 (웹 User_08 탭2).
 *
 * <p>siteCd 는 필수(사업장 스코프 강제). fromDate/toDate(YYYYMMDD)·userNm 은 선택 필터.
 * limitCnt 는 전수조회 방지 상한(서비스에서 고정 주입).
 * 계정 만료/탈퇴 후에도 서명본 레코드는 조회 대상이다(근로기준법 §42 3년 보존 — §6-2).
 */
public record ContractSignListQuery(
    String cmpnyCd
    , String siteCd
    , String fromDate    // YYYYMMDD (SIGN_DTIME 시작일, 선택)
    , String toDate      // YYYYMMDD (SIGN_DTIME 종료일, 선택)
    , String userNm      // 이름 스냅샷 부분 검색(선택)
    , int limitCnt
) {
}
