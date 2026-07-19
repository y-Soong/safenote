package com.prafta.common.cmm.tbmshare.result;

/**
 * 관계 해지 훅(T1)에서 사용하는 지정 참조 행(PRAFTA-SUBCON-T5).
 *
 * <p>두 회사 사이의 유효(DEL_YN='N') 지정 1건 = (세션, 지정된 회사).
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record ShareRefRow(
    String sessionCd
    , String shareCmpnyCd
) {
}
