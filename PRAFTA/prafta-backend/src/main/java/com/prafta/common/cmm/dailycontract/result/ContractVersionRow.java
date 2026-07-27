package com.prafta.common.cmm.dailycontract.result;

/**
 * 계약서 버전 이력 행 (웹 User_07 버전 이력 테이블).
 *
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record ContractVersionRow(
    int contractVer
    , String contractNm
    , String useYn         // Y: 활성 / N: 교체·중지됨
    , String insertNo      // 등록자 USER_CD
    , String insertNm      // 등록자명(TB_USER 조인 — 탈퇴/부재 시 USER_CD 폴백, qa L-2)
    , String insertDate    // YYYY-MM-DD HH:mm
    , String formatType    // 'PDF' | 'IMG' (TB_FILE_INFO.FILE_EXT 조인 도출 — 멀티페이지 지원 T4)
) {
}
