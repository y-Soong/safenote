package com.prafta.common.cmm.push.result;

/**
 * TBM 세션 입실 참석자 1행(PUSH 대상, PRAFTA-SUBCON-T5 F6).
 *
 * <p>연동 회사 지정으로 <b>타사 소속 참석자</b>가 섞이므로 USER_CD 만으로는 대상을 특정할 수 없다
 * (USER_CD 는 회사별 채번). PUSH 토큰 조회/outbox 적재가 회사별이므로 회사코드를 동반한다.
 *
 * <p>resultType record: SELECT 컬럼 순서 = 아래 필드 순서(위치기반 매핑)와 반드시 일치.
 */
public record TbmPushTargetRow(
    String cmpnyCd
    , String userCd
) {
}
