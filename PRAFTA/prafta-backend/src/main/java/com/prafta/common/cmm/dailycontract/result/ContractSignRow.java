package com.prafta.common.cmm.dailycontract.result;

/**
 * 서명 이력 목록 행 (웹 User_08 탭2).
 *
 * <p>이름은 서명 시점 스냅샷(USER_NM_SNAPSHOT)만 사용한다(계정 만료/탈퇴 후 조회 대비 — §6-2).
 * 휴대폰은 TB_DAILY_USER.MBL_NO_ENC 를 조회해 응답 계층에서 복호화·평문 노출한다
 * (2026-07-17 결정 — User_05 관리 화면 전례 미러. 평문 자체는 조회/로그 금지).
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record ContractSignRow(
    String signId
    , String siteCd
    , String userCd
    , String userNmSnapshot
    , String mblNoLast4         // 계정 물리 삭제 시 NULL 가능(레코드는 보존)
    , String mblNoEnc           // AES-GCM 암호문(v1.base64url) — 웹 평문 표시용, 계정 삭제 시 NULL
    , int contractVer
    , String firstWorkDate      // YYYYMMDD
    , String signDtime          // YYYY-MM-DD HH:mm
    , String mergedSha256
) {
}
