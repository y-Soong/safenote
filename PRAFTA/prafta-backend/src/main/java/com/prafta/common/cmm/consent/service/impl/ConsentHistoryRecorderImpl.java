package com.prafta.common.cmm.consent.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.consent.application.command.ConsentAgrUpsertCommand;
import com.prafta.common.cmm.consent.application.command.ConsentHistInsertCommand;
import com.prafta.common.cmm.consent.mapper.ConsentMapper;
import com.prafta.common.cmm.consent.mapper.result.ConsentStateResult;
import com.prafta.common.cmm.consent.service.ConsentHistoryRecorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 약관 동의/철회 이력 기록기 구현 — PRAFTA-SUBCON-T4-02.
 *
 * <p>동의 변경의 유일한 경로. 현재상태 upsert 와 전이 이력 INSERT 를 항상 함께 수행한다.
 * <p>트랜잭션 어노테이션을 두지 않는다 — 호출 서비스(@Transactional)의 트랜잭션에 참여해야
 *    본체 롤백 시 이력도 함께 롤백된다(REQUIRES_NEW 금지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentHistoryRecorderImpl implements ConsentHistoryRecorder {

    private final ConsentMapper consentMapper;

    @Override
    public int recordAndUpsert(
            String cmpnyCd
            , String userCd
            , String termsId
            , String termsVersion
            , String afterAgrYn
            , String source
            , String actorCmpnyCd
            , String actorUserCd) {

        // 1) 현재값(before) — 행이 없으면 null(최초 응답). ★ FOR UPDATE 로 잠근다:
        //    잠금 없이 읽으면 동시 응답 2건이 모두 before=null 을 보고 "최초 동의" 이력을 각각 남긴다
        //    (MGMT 는 ON DUPLICATE, HIST 는 AUTO_INCREMENT 라 제약 위반 없이 중복 이력이 조용히 쌓인다).
        //    이력은 PII 반출의 법적 근거이므로 전이 기록은 직렬화되어야 한다.
        String beforeAgrYn = consentMapper.selectUserAgrYnForUpdate(cmpnyCd, userCd, termsId, termsVersion);

        // 2) 값이 같으면 전이가 아니다 → upsert/이력 모두 생략(완전 멱등, 허위 이력 방지).
        if (beforeAgrYn != null && beforeAgrYn.equals(afterAgrYn)) {
            log.debug("약관 동의 전이 없음(멱등) - cmpnyCd={}, userCd={}, termsId={}, ver={}, agrYn={}"
                    , cmpnyCd, userCd, termsId, termsVersion, afterAgrYn);
            return 0;
        }

        // 3) 현재상태 upsert.
        int affected = consentMapper.upsertTermsAgr(
                ConsentAgrUpsertCommand.of(cmpnyCd, userCd, termsId, termsVersion, afterAgrYn));

        // 4) 전이 이력 INSERT(append-only, ACTION_DTIME=서버 NOW()).
        consentMapper.insertTermsAgrHist(ConsentHistInsertCommand.of(
                cmpnyCd, userCd, termsId, termsVersion, beforeAgrYn, afterAgrYn, source, actorCmpnyCd, actorUserCd));

        log.info("약관 동의 전이 기록 - cmpnyCd={}, userCd={}, termsId={}, ver={}, {}→{}, 경로={}, 영향행={}"
                , cmpnyCd, userCd, termsId, termsVersion, beforeAgrYn, afterAgrYn, source, affected);
        return affected;
    }

    @Override
    public int recordAndUpsertState(
            String cmpnyCd
            , String userCd
            , String termsId
            , String termsVersion
            , String afterAgrYn
            , String afterState
            , String source
            , String actorCmpnyCd
            , String actorUserCd) {

        // 1) 현재값(before) + 잠금. 행이 없으면 null(최초 응답). 잠금 이유는 recordAndUpsert 와 동일하다.
        ConsentStateResult before = consentMapper.selectUserAgrStateForUpdate(cmpnyCd, userCd, termsId, termsVersion);
        String beforeAgrYn = before == null ? null : before.agrYn();
        String beforeState = before == null ? null : before.consentState();

        // 2) ★동의값과 상태가 <b>둘 다</b> 같을 때만 전이가 아니다.
        //    AGR_YN 만 비교하면 SUSPENDED→WITHDRAWN('N'→'N')이 걸러져 철회 이력이 남지 않는다.
        if (beforeAgrYn != null && beforeAgrYn.equals(afterAgrYn)
                && beforeState != null && beforeState.equals(afterState)) {
            log.debug("위치정보 동의 전이 없음(멱등) - cmpnyCd={}, userCd={}, ver={}, state={}"
                    , cmpnyCd, userCd, termsVersion, afterState);
            return 0;
        }

        // 3) 현재상태 upsert(AGR_YN + CONSENT_STATE).
        int affected = consentMapper.upsertTermsAgr(
                ConsentAgrUpsertCommand.of(cmpnyCd, userCd, termsId, termsVersion, afterAgrYn, afterState));

        // 4) 전이 이력 INSERT(append-only).
        consentMapper.insertTermsAgrHist(ConsentHistInsertCommand.of(
                cmpnyCd, userCd, termsId, termsVersion, beforeAgrYn, afterAgrYn, source, actorCmpnyCd, actorUserCd
                , beforeState, afterState));

        log.info("위치정보 동의 전이 기록 - cmpnyCd={}, userCd={}, ver={}, 상태 {}→{}, 동의 {}→{}, 경로={}, 영향행={}"
                , cmpnyCd, userCd, termsVersion, beforeState, afterState, beforeAgrYn, afterAgrYn, source, affected);
        return affected;
    }
}
