package com.prafta.app.terms.terms01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.terms.terms01.application.param.OptionalTermsAgreeParam;
import com.prafta.app.terms.terms01.application.param.SubconConsentRespondParam;
import com.prafta.app.terms.terms01.dto.response.OptionalTermsResponse;
import com.prafta.app.terms.terms01.dto.response.PendingTermsResponse;
import com.prafta.app.terms.terms01.dto.response.SubconConsentGateResponse;
import com.prafta.app.terms.terms01.dto.response.SubconConsentRespondResponse;
import com.prafta.app.terms.terms01.dto.response.TermsAgreeResponse;
import com.prafta.app.terms.terms01.mapper.Terms01Mapper;
import com.prafta.app.terms.terms01.mapper.result.PendingTermsResult;
import com.prafta.app.terms.terms01.service.Terms01Service;
import com.prafta.common.cmm.consent.ConsentConst;
import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;
import com.prafta.common.cmm.consent.service.ConsentHistoryRecorder;
import com.prafta.common.cmm.consent.service.ConsentQueryService;
import com.prafta.common.error.terms.TermsErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 약관(Terms) 서비스 구현.
 *
 * <p>USER_CD 는 컨트롤러에서 JWT 로만 확정해 전달받는다(본문/쿼리 식별값 미사용 → IDOR 차단).
 * <p>필수약관 일괄 동의는 클라가 보낸 목록을 신뢰하지 않고 서버가 미동의 목록을 재산출하여 전부 동의 처리한다.
 * <p>선택약관 토글은 대상이 선택약관(REQUIRED_YN='N' AND USE_YN='Y')인지 서버가 검증(현재버전 resolve 동반)하여
 *    필수약관을 'N' 으로 토글해 게이트를 우회하려는 시도를 차단한다(TERMS_403_001).
 * <p>★ SUBCON-T4: 모든 동의 변경(필수 일괄동의 / 선택 토글 / 제3자 제공 동의 응답)은
 *    ConsentHistoryRecorder.recordAndUpsert 단일 경로를 경유하여 전이 이력을 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Terms01ServiceImpl implements Terms01Service {

    private final Terms01Mapper terms01Mapper;
    private final ConsentQueryService consentQueryService;
    private final ConsentHistoryRecorder consentHistoryRecorder;

    @Override
    public PendingTermsResponse selectPendingRequiredTerms(String cmpnyCd, String userCd) {
        List<PendingTermsResult> pending = terms01Mapper.selectPendingRequiredTerms(cmpnyCd, userCd);
        log.info("미동의 필수약관 조회 - cmpnyCd={}, userCd={}, 미동의건수={}", cmpnyCd, userCd, pending == null ? 0 : pending.size());
        return PendingTermsResponse.of(pending);
    }

    @Override
    @Transactional
    public TermsAgreeResponse agreeRequiredTerms(String cmpnyCd, String userCd) {
        // 서버가 미동의 필수약관을 직접 재산출(클라 목록 불신). 빈 목록이면 멱등 성공(영향행 0).
        List<PendingTermsResult> pending = terms01Mapper.selectPendingRequiredTerms(cmpnyCd, userCd);
        log.info("필수약관 일괄 동의 진입 - cmpnyCd={}, userCd={}, 대상건수={}", cmpnyCd, userCd, pending == null ? 0 : pending.size());

        int affected = 0;
        if (pending != null) {
            for (PendingTermsResult t : pending) {
                // 각 약관 현재버전에 AGR_YN='Y' 전이 기록 + upsert. CMPNY_CD/TERMS_VERSION 은 서버 산출값만 사용.
                affected += consentHistoryRecorder.recordAndUpsert(
                        cmpnyCd, userCd, t.termsId(), t.termsVersion(), "Y"
                        , ConsentConst.SOURCE_GATE, cmpnyCd, userCd);
            }
        }

        log.info("필수약관 일괄 동의 완료 - cmpnyCd={}, userCd={}, 영향행={}", cmpnyCd, userCd, affected);
        return TermsAgreeResponse.success(affected);
    }

    @Override
    public OptionalTermsResponse selectOptionalTerms(String cmpnyCd, String userCd) {
        OptionalTermsResponse response = OptionalTermsResponse.of(terms01Mapper.selectOptionalTerms(cmpnyCd, userCd));
        log.info("선택약관 목록 조회 - cmpnyCd={}, userCd={}, 건수={}", cmpnyCd, userCd, response.getTerms().size());
        return response;
    }

    @Override
    @Transactional
    public TermsAgreeResponse toggleOptionalTerms(OptionalTermsAgreeParam param) {
        // 1) 대상이 선택약관(REQUIRED_YN='N' AND USE_YN='Y')인지 검증 + 현재버전 resolve(클라 버전 위조 차단).
        //    필수약관/미사용약관/미존재면 null → 게이트 우회 시도로 간주하여 차단.
        String currentVersion = terms01Mapper.selectOptionalTermsCurrentVersion(param.termsId());
        if (currentVersion == null || currentVersion.isBlank()) {
            log.warn("선택약관 토글 거부(선택약관 아님) - userCd={}, termsId={}", param.userCd(), param.termsId());
            throw new ApiException(TermsErrorCode.TERMS_403_001);
        }

        // 2) (CMPNY_CD, USER_CD, TERMS_ID, 현재버전) AGR_YN 전이 기록 + upsert(멱등).
        //    CMPNY_CD 는 Param(=JWT) 회사 스코프. 006 철회(Y→N)도 여기로 들어오며 이력이 남는다.
        int affected = consentHistoryRecorder.recordAndUpsert(
                param.cmpnyCd(), param.userCd(), param.termsId(), currentVersion, param.agrYn()
                , ConsentConst.SOURCE_MYPAGE, param.cmpnyCd(), param.userCd());

        log.info("선택약관 토글 완료 - userCd={}, termsId={}, agrYn={}, 영향행={}"
                , param.userCd(), param.termsId(), param.agrYn(), affected);
        return TermsAgreeResponse.success(affected);
    }

    @Override
    public SubconConsentGateResponse selectSubconConsentGate(String cmpnyCd, String userCd) {
        // 1) 006 약관 배포 여부. 미배포(USE_YN='N' 또는 행 부재) = 제도 미가동 → 게이트 미노출.
        ConsentTermsResult terms = consentQueryService.resolveActiveTerms(ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID);
        if (terms == null) {
            log.debug("제3자 제공 동의 게이트 - 약관 미배포(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return SubconConsentGateResponse.notRequired();
        }

        // 1-2) 006 이 '선택약관'인지 응답 저장 경로와 동일한 술어로 확인한다(REQUIRED_YN='N' AND USE_YN='Y').
        //      게이트는 노출되는데 응답 저장이 TERMS_403_001 로 막히면, 사용자는 매 로그인 게이트를 만나고
        //      어떤 버튼으로도 해소할 수 없다(동의의 자유의사도 훼손). 술어를 일치시켜 그 상태를 원천 차단한다.
        String optionalVersion = terms01Mapper.selectOptionalTermsCurrentVersion(terms.termsId());
        if (optionalVersion == null || optionalVersion.isBlank()) {
            log.warn("제3자 제공 동의 게이트 - 006 이 선택약관이 아님(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return SubconConsentGateResponse.notRequired();
        }

        // 2) 소속 사업장이 활성 연동 링크에 참여(SRC 또는 DST) 중인지 DB 실측 판정(토큰 gv_siteCd 불신).
        if (!consentQueryService.isLinkedSiteMember(cmpnyCd, userCd)) {
            log.debug("제3자 제공 동의 게이트 - 비연동 사업장(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return SubconConsentGateResponse.notRequired();
        }

        // 3) 현재버전 응답 존재 여부. 'Y'(동의)든 'N'(미동의)든 응답이 있으면 게이트 해제(재노출 없음).
        String agrYn = consentQueryService.selectUserAgrYn(
                cmpnyCd, userCd, terms.termsId(), terms.termsVersion());
        if (agrYn != null) {
            log.debug("제3자 제공 동의 게이트 - 응답 완료(게이트 미노출) cmpnyCd={}, userCd={}, agrYn={}"
                    , cmpnyCd, userCd, agrYn);
            return SubconConsentGateResponse.notRequired();
        }

        log.info("제3자 제공 동의 게이트 노출 - cmpnyCd={}, userCd={}, termsId={}, ver={}"
                , cmpnyCd, userCd, terms.termsId(), terms.termsVersion());
        return SubconConsentGateResponse.required(terms);
    }

    @Override
    @Transactional
    public SubconConsentRespondResponse respondSubconConsent(SubconConsentRespondParam param) {
        // 1) 약관 상수(006) 기준 현재버전 resolve. 미배포/미사용이면 응답 저장 불가.
        String termsId = ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID;
        ConsentTermsResult terms = consentQueryService.resolveActiveTerms(termsId);
        if (terms == null) {
            log.warn("제3자 제공 동의 응답 거부(약관 미배포) - userCd={}, termsId={}", param.userCd(), termsId);
            throw new ApiException(TermsErrorCode.TERMS_404_001);
        }

        // 2) 선택약관 검증(REQUIRED_YN='N' AND USE_YN='Y') — 본 경로로 필수약관을 토글할 수 없게 재확인.
        String optionalVersion = terms01Mapper.selectOptionalTermsCurrentVersion(termsId);
        if (optionalVersion == null || optionalVersion.isBlank()) {
            log.warn("제3자 제공 동의 응답 거부(선택약관 아님) - userCd={}, termsId={}", param.userCd(), termsId);
            throw new ApiException(TermsErrorCode.TERMS_403_001);
        }

        // 3) 동의('Y')/미동의('N') 모두 저장 = 응답 완료 = 게이트 해제. 전이 시에만 이력 1행.
        //    ★ 철회 소급 없음: 기존 스냅샷(tb_cmpny_share_snapshot*)은 조회조차 하지 않는다.
        int affected = consentHistoryRecorder.recordAndUpsert(
                param.cmpnyCd(), param.userCd(), termsId, optionalVersion, param.agrYn()
                , ConsentConst.SOURCE_GATE, param.cmpnyCd(), param.userCd());

        log.info("제3자 제공 동의 응답 - cmpnyCd={}, userCd={}, ver={}, agrYn={}, 경로=GATE, 영향행={}"
                , param.cmpnyCd(), param.userCd(), optionalVersion, param.agrYn(), affected);
        return SubconConsentRespondResponse.success(param.agrYn());
    }
}
