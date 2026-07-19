package com.prafta.common.cmm.tbmshare.command;

/**
 * 연동 회사 지정 INSERT/RESTORE 커맨드(PRAFTA-SUBCON-T5).
 *
 * <p>{@code hostCmpnyCd} 는 반드시 <b>세션에서 읽은 값</b>만 사용한다(클라 입력 불신 — plan §9-1 6).
 * <p>UK(SESSION_CD, SHARE_CMPNY_CD) 가 DEL_YN 을 포함하지 않으므로, 해제된 동일 행이 있으면
 * INSERT 가 아니라 RESTORE(UPDATE) 로 처리한다(중복키 함정).
 */
public record ShareDesignateCommand(
    String sessionCd
    , String hostCmpnyCd
    , String shareCmpnyCd
    , String designatedByCmpnyCd
    , String designatedByUserCd
) {
}
