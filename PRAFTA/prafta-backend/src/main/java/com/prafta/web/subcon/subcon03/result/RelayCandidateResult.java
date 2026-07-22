package com.prafta.web.subcon.subcon03.result;

/**
 * 릴레이 후보 1건 — 제공측이 하위로부터 수신 보유 중인 스냅샷(PRAFTA-SUBCON-T3 §5-7).
 *
 * <p><b>하위 제공사 회사코드/회사명은 절대 포함하지 않는다</b>(마스터 §1-5 — 상위는 하위의 하위를 모른다).
 * 화면 라벨은 "연동사 수신자료" 수준으로만 표시된다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record RelayCandidateResult(
    Long snapshotId
    , String periodStr
    , String periodEnd
    , String periodLabel
    , Integer version
    , Integer rowCnt
    , String unclosedIncludedYn
    , String createDtime
    // [PS-05] 부분 포함(마감분만 필터) 표식 — record 끝 append. 구본 NULL 은 SQL IFNULL 로 'N'(D-4).
    , String closedPartialYn
){
}
