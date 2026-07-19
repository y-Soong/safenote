package com.prafta.common.cmm.tbmshare.result;

/**
 * TBM 세션 조회 접근 판정 결과(PRAFTA-SUBCON-T5).
 *
 * @param session 세션 소유 정보(개설사/사업장/상태)
 * @param owner   조회자가 개설사 본인이면 true, SHARE 체인 소속(타사)이면 false
 */
public record TbmSessionAccess(
    SessionOwnerResult session
    , boolean owner
) {
    /** 세션 개설사 회사코드(콘텐츠/위험성/자료 조회 키). */
    public String hostCmpnyCd() {
        return session.hostCmpnyCd();
    }
}
