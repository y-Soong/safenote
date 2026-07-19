package com.prafta.common.cmm.tbmshare.result;

/**
 * 연동 회사 지정 후보 1행(PRAFTA-SUBCON-T5).
 *
 * <p>행위자 회사와 관계 ACCEPTED 인 회사 중, 개설사·이미 이 세션 체인에 있는 회사를 제외한 목록.
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record ShareCandidateResult(
    String cmpnyCd
    , String cmpnyNm
) {
}
