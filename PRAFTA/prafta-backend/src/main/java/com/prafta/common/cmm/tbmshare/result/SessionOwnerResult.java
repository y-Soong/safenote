package com.prafta.common.cmm.tbmshare.result;

/**
 * TBM 세션 소유/개설 정보(PRAFTA-SUBCON-T5).
 *
 * <p>SESSION_CD 단독으로 조회한다(회사 필터 없음). 이 단독 조회는 마이그레이션
 * {@code prafta-subcon-t5-1} 의 {@code UX_TBM_SESSION_CD}(전역 유일키)로 보장된다.
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record SessionOwnerResult(
    String sessionCd
    , String hostCmpnyCd    // 개설사(TB_TBM_SESSION.CMPNY_CD)
    , String siteCd         // 개설사 사업장
    , String statusCd
    , String delYn
) {
}
