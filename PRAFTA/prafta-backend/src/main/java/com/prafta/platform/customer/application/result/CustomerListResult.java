package com.prafta.platform.customer.application.result;

/**
 * 고객 리스트 조회 결과 1행(TB_CMPNY + AI 토큰 쿼터/사용량 LEFT JOIN).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서에 의존한다 — 본 컴포넌트 순서와
 * PlatformCustomerMapper.xml selectCustomerList 의 SELECT 순서를 항상 일치시킬 것.
 *
 * @param usedTokens     당월 AI 토큰 사용량(입력+출력 합). 미사용 회사는 0(SQL IFNULL).
 * @param tokenLimit     유효 한도(행 미존재 → 800,000 을 SQL IFNULL 로 해소. -1 무제한/0 차단은 원값 유지).
 * @param quotaCustomYn  'Y'=개별 설정 행 존재 / 'N'=기본값 적용.
 * @param weekStdMinutes 회사 통상근로자 주 소정근로 분(TB_CMPNY_STD_WORK_POLICY COMPANY 스코프).
 *                       <b>null = 직접 지정 없음</b> → 코드 폴백 2400분(주 40시간) 적용.
 *                       ★IFNULL 로 2400 을 채우지 않는다 — "직접 지정 40시간"과 "미지정(기본 40시간)"을
 *                       화면이 구분해야 하고, 팝업이 '기본값 사용/직접 지정' 라디오를 프리필해야 하기 때문.
 */
public record CustomerListResult(
    String cmpnyCd
    , String cmpnyNm
    , String bsnsLcnNo
    , String addr1
    , String addr2
    , String zipCode
    , String contractYn
    , String contractEndDate
    , String useYn
    , Long usedTokens
    , Long tokenLimit
    , String quotaCustomYn
    , Integer weekStdMinutes
) {
}
