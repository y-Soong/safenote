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
import com.prafta.common.cmm.consent.service.ConsentTermsService;

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
    private final ConsentHistoryRecorder consentHistoryRecorder;
    // ★ 선택약관 목록/토글 · 제3자 제공 동의(006) 게이트/응답은 공용 서비스에 위임한다.
    //   웹 내 정보 팝업/웹 로그인 게이트가 같은 경로를 타야 채널 간 판정이 갈리지 않는다.
    private final ConsentTermsService consentTermsService;

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
        // 공용 서비스 위임(응답 스펙 불변 — terms[] 각 항목의 필드명/의미 동일).
        return OptionalTermsResponse.of(consentTermsService.listOptionalTerms(cmpnyCd, userCd));
    }

    @Override
    public TermsAgreeResponse toggleOptionalTerms(OptionalTermsAgreeParam param) {
        // 선택약관 검증/현재버전 resolve/이력 기록은 공용 서비스가 수행한다(@Transactional 도 그쪽).
        int affected = consentTermsService.toggleOptionalTerms(
                param.cmpnyCd(), param.userCd(), param.termsId(), param.agrYn(), ConsentConst.SOURCE_MYPAGE);
        return TermsAgreeResponse.success(affected);
    }

    @Override
    public SubconConsentGateResponse selectSubconConsentGate(String cmpnyCd, String userCd) {
        // 공용 서비스 위임 — 판정 결과가 null 이면 게이트 불필요(약관 미배포/비연동 사업장/응답 완료).
        ConsentTermsResult terms = consentTermsService.resolveSubconConsentGate(cmpnyCd, userCd);
        return terms == null ? SubconConsentGateResponse.notRequired() : SubconConsentGateResponse.required(terms);
    }

    @Override
    public SubconConsentRespondResponse respondSubconConsent(SubconConsentRespondParam param) {
        // 공용 서비스 위임(@Transactional 도 그쪽). 동의/미동의 모두 저장 = 게이트 해제.
        consentTermsService.respondSubconConsent(
                param.cmpnyCd(), param.userCd(), param.agrYn(), ConsentConst.SOURCE_GATE);
        return SubconConsentRespondResponse.success(param.agrYn());
    }
}
