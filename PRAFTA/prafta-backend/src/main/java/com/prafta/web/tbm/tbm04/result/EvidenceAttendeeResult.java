package com.prafta.web.tbm.tbm04.result;

/**
 * TBM 증빙 교육일지(건별) 참석자 1행 (시트3+ 참석자 명단).
 *
 * <p>자사 개설 세션은 참석자 전원(타사 포함 — 소속 회사명 표기), 타사 개설(공유) 세션은
 * 자사 참석자만 내려간다(타사 명단 비노출 — 쿼리 술어로 강제).
 * 서명은 이미지 대신 존재 여부만(signedYn — 필요 시 Tbm_04 상세 화면에서 열람).
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record EvidenceAttendeeResult(
    String sessionCd
    , String userNm
    , String userTypeCd        // REGULAR | DAILY
    , String cmpnyNm           // 참석자 소속 회사명
    , String entryAt           // yyyy-MM-dd HH:mm (KST)
    , String exitAt
    , String completionStatusCd // COMPLETED | NOT_COMPLETED | null(미완료)
    , String signedYn          // 입실/종료 서명 파일 존재 여부
){
}
