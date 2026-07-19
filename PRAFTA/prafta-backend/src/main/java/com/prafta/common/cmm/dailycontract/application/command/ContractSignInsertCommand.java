package com.prafta.common.cmm.dailycontract.application.command;

/**
 * 계약서 서명본 INSERT 커맨드 (TB_DAILY_CONTRACT_SIGN — append-only, 3년 보존).
 *
 * <p>SIGN_DTIME 은 서버 NOW() 로 SQL 에서 기록한다(클라이언트 시각 불신 — 요청서 §6-3).
 * reqId 는 현재 승인 사이클(최근 소진 '05' 요청) 연결값이며, 배포 전 기존 활성 일용직은 NULL 허용.
 */
public record ContractSignInsertCommand(
    String cmpnyCd
    , String signId
    , String siteCd
    , String userCd
    , String userNmSnapshot     // 서명 시점 사용자명 스냅샷(계정 만료 후 조회 대비)
    , int contractVer
    , String reqId              // 승인요청ID(레거시 서명은 NULL)
    , String signFileMgmtCd     // 서명 PNG 원본 파일코드
    , String mergedFileMgmtCd   // 합성본 파일코드
    , String mergedSha256       // 합성본 SHA-256 hex(증적 무결성)
    , String firstWorkDate      // 최초 근로일 YYYYMMDD (=서명일, D1)
) {
}
