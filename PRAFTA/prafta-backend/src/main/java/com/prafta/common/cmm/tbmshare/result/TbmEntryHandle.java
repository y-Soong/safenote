package com.prafta.common.cmm.tbmshare.result;

/**
 * 대리입실 후보 핸들의 평문 페이로드(PRAFTA-SUBCON-T5 M1).
 *
 * <p>후보 목록 각 행에 서버가 발급한 <b>불투명 핸들</b>(AES-GCM 암호문)이 실려 나가고, 대리입실
 * 요청은 그 핸들만 키로 받는다. 클라이언트는 참석자의 회사코드/사용자코드를 알지도, 조작하지도 못한다.
 *
 * <p><b>왜 USER_CD 로는 안 되는가</b>: USER_CD 는 회사별 채번
 * ({@code CONCAT(YYYYMM, FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'USER_CD'))}) 이라 서로 다른 회사가 같은 달에
 * 만든 n번째 사용자는 USER_CD 가 완전히 동일하다(TB_USER PK 가 (CMPNY_CD, USER_CD) 인 이유이며,
 * 실 DB 에도 이미 중복이 존재한다). 재지정 체인(B→C)이 있으면 USER_CD 단독 매칭은 상시 모호해진다.
 * TB_DAILY_USER.USER_ID 는 UNIQUE 도 아니라 공통 대체키로 쓸 수 없다.
 */
public record TbmEntryHandle(
    String sessionCd    // 발급 시점의 세션(다른 세션으로의 재사용 차단)
    , String cmpnyCd    // 참석자 소속 회사(출결행 CMPNY_CD 의 권위 출처)
    , String userTypeCd // REGULAR / DAILY
    , String userCd
) {
}
