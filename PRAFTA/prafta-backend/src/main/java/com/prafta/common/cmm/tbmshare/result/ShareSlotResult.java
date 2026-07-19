package com.prafta.common.cmm.tbmshare.result;

/**
 * 지정 슬롯(UK(SESSION_CD, SHARE_CMPNY_CD)) 점유 행(PRAFTA-SUBCON-T5).
 *
 * <p>없으면 null(→INSERT), 있으면 DEL_YN 으로 분기한다(N=이미 지정 / Y=RESTORE 대상).
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record ShareSlotResult(
    long shareId
    , String delYn
) {
}
