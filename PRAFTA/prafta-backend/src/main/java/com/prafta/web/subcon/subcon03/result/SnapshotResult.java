package com.prafta.web.subcon.subcon03.result;

/**
 * 수신 보유 스냅샷 목록 1행(PRAFTA-SUBCON-T3 §5-8).
 *
 * <p>OWNER_CMPNY_CD = 자사인 스냅샷만 조회된다(테넌트 스코프 1차 WHERE).
 * srcCmpnyNm 은 <b>직상위 제공사</b>까지만이다(릴레이로 묶여 온 하위 회사 정보는 존재하지 않는다).
 * relationActiveYn='N' = 연동이 종료된 회사의 자료(열람은 정상 — 결정 3 존속).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SnapshotResult(
    Long snapshotId
    , Long shareReqId
    , String dataType
    , String srcCmpnyNm
    , String siteNm
    , String periodStr
    , String periodEnd
    , Integer version
    , Integer rowCnt
    , String unclosedIncludedYn
    , Integer consentExcludedCnt
    , String createDtime
    , String relationActiveYn
    , String purpose
    , String reqUserNm      // 요청자 성명 - 자사 소속 요청자만 해석(상대사 요청자는 null), 성명 공란 시 USER_CD 폴백
    , String reqDtime       // 요청일시(TB_CMPNY_SHARE_REQ.INSERT_DATE)
    , String processDtime   // 승인일시(PROCESS_DTIME) - 승인 주체는 마스킹 정책상 미제공, nullable
    // [PS-07] 부분 포함 가이드 필드 — record 끝 append(하위호환).
    , String closedOnlyYn   // 요청 옵션(IFNULL 'Y') — 마감분만/미마감 포함 구분
    , String closedPartialYn // 부분 포함 표식. NULL=구본(메타 없음 — 전체 포함 간주, D-4). IFNULL 금지
    , String coverageMeta   // 커버리지 요약 JSON 문자열(월·부서명 단위까지 — PII 없음). NULL=구본
){
}
