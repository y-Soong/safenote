package com.prafta.web.user.user07.dto.response;

/**
 * 계약서 버전 항목 (User_07 활성 요약 카드 / 버전 이력 테이블 공용).
 * 파일 경로는 노출하지 않는다 — 미리보기는 GET contract-image 스트림만 사용.
 */
public record ContractVersionItem(
    int contractVer
    , String contractNm
    , String useYn         // Y: 활성 / N: 교체·중지됨
    , String insertNo      // 등록자 USER_CD
    , String insertNm      // 등록자명(탈퇴/부재 시 USER_CD 폴백, qa L-2)
    , String insertDate    // YYYY-MM-DD HH:mm
    , String formatType    // 'PDF' | 'IMG' (TB_FILE_INFO.FILE_EXT 조인 도출 — 멀티페이지 지원 T4)
) {
}
