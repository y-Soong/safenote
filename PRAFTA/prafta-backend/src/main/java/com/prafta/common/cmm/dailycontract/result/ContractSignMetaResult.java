package com.prafta.common.cmm.dailycontract.result;

/**
 * 계약서 서명본 메타 단건 (게이트 판정 / 본인 서명 조회 / 관리자 열람 공용).
 *
 * <p>파일 경로(FILE_PATH)는 조회하지 않는다 — 열람은 서버 스트림 응답만 허용(경로 미노출).
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record ContractSignMetaResult(
    String signId
    , String siteCd
    , String userCd
    , String userNmSnapshot
    , int contractVer
    , String reqId              // 승인 사이클 연결(레거시 서명은 NULL)
    , String mergedFileMgmtCd
    , String mergedSha256
    , String firstWorkDate      // YYYYMMDD
    , String signDtime          // YYYY-MM-DD HH:mm:ss
) {
}
