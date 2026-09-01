package com.prafta.common.cmm.consent.service;

/**
 * 약관 동의/철회 이력 기록기 — PRAFTA-SUBCON-T4-02.
 *
 * <p>동의 현재상태(TB_TERMS_USER_AGR_MGMT) upsert 와 전이 이력(TB_TERMS_USER_AGR_HIST) INSERT 를
 *    한 묶음으로 수행하는 <b>유일한 동의 변경 경로</b>다. 매퍼 upsert 를 직접 호출하는 코드는 없어야 한다
 *    (이력 우회 경로가 남으면 동의 이력의 무결성이 깨진다 — 요청서 §3 "전 이력 기록").
 *
 * <p>트랜잭션: 별도 트랜잭션을 열지 않고 <b>호출자 트랜잭션에 참여</b>한다
 *    (REQUIRES_NEW 금지 — 본체 롤백 시 이력만 남으면 허위 기록이 된다).
 */
public interface ConsentHistoryRecorder {

    /**
     * 동의 상태 전이 기록(멱등).
     *
     * <ol>
     *   <li>현재값(before) 조회</li>
     *   <li>before == after 면 upsert/이력 모두 생략하고 0 반환(완전 멱등 — 동일 값 재저장은 이력을 남기지 않는다)</li>
     *   <li>다르면 현재상태 upsert + 전이 이력 INSERT</li>
     * </ol>
     *
     * @param cmpnyCd      동의 주체 회사코드(JWT)
     * @param userCd       동의 주체 사용자코드(JWT)
     * @param termsId      약관ID(서버 상수 또는 서버 검증값)
     * @param termsVersion 약관 현재버전(서버 resolve 값 — 클라 입력 금지)
     * @param afterAgrYn   전이 후 값('Y'|'N')
     * @param source       응답 경로(ConsentConst.SOURCE_*)
     * @param actorCmpnyCd 변경 주체 회사코드(현재는 본인)
     * @param actorUserCd  변경 주체 사용자코드(현재는 본인)
     * @return upsert 영향행(전이 없음=0)
     */
    int recordAndUpsert(
            String cmpnyCd
            , String userCd
            , String termsId
            , String termsVersion
            , String afterAgrYn
            , String source
            , String actorCmpnyCd
            , String actorUserCd);

    /**
     * 상태 관리 약관(위치기반서비스 005)의 4-state 전이 기록 — 위치정보 동의철회·중지 S2.
     *
     * <p>{@link #recordAndUpsert} 와 동일한 경로(FOR UPDATE 직렬화 + upsert + 이력)를 쓰되
     * {@code CONSENT_STATE} 를 함께 전이시킨다.
     *
     * <p>★멱등 판정에 <b>상태까지 포함</b>한다. AGR_YN 만 비교하면 {@code SUSPENDED → WITHDRAWN}
     * 처럼 둘 다 {@code 'N'} 인 전이가 "변화 없음"으로 걸러져 <b>철회 이력이 남지 않는다</b>.
     *
     * @param afterState {@code AGREED / SUSPENDED / PENDING_REAGREE / WITHDRAWN}
     * @return 전이가 일어났으면 upsert 영향행(1 이상), 이미 같은 상태면 0
     */
    int recordAndUpsertState(
            String cmpnyCd
            , String userCd
            , String termsId
            , String termsVersion
            , String afterAgrYn
            , String afterState
            , String source
            , String actorCmpnyCd
            , String actorUserCd);
}
