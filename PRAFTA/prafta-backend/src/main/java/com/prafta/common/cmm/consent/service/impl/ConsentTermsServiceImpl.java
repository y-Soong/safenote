package com.prafta.common.cmm.consent.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.consent.ConsentConst;
import com.prafta.common.cmm.consent.mapper.ConsentMapper;
import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;
import com.prafta.common.cmm.consent.mapper.result.OptionalTermsResult;
import com.prafta.common.cmm.consent.service.ConsentHistoryRecorder;
import com.prafta.common.cmm.consent.service.ConsentQueryService;
import com.prafta.common.cmm.consent.service.ConsentTermsService;
import com.prafta.common.error.terms.TermsErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선택약관/제3자 제공 동의(006) 공용 서비스 구현.
 *
 * <p>Terms01ServiceImpl 에 있던 판정/저장 로직을 그대로 이관했다(동작 동일). 앱은 본 서비스에 위임하고,
 *    웹(내 정보 팝업 토글 / 로그인 게이트)은 ConsentController 를 통해 같은 경로를 탄다.
 * <p>로그에는 userCd 까지만 남긴다(성명/휴대폰 등 PII 금지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentTermsServiceImpl implements ConsentTermsService {

    private final ConsentMapper consentMapper;
    private final ConsentQueryService consentQueryService;
    private final ConsentHistoryRecorder consentHistoryRecorder;

    @Override
    public List<OptionalTermsResult> listOptionalTerms(String cmpnyCd, String userCd) {
        List<OptionalTermsResult> terms = consentMapper.selectOptionalTerms(cmpnyCd, userCd);
        log.info("선택약관 목록 조회 - cmpnyCd={}, userCd={}, 건수={}"
                , cmpnyCd, userCd, terms == null ? 0 : terms.size());
        return terms == null ? List.of() : terms;
    }

    @Override
    @Transactional
    public int toggleOptionalTerms(String cmpnyCd, String userCd, String termsId, String agrYn, String source) {
        // 1) 대상이 선택약관(REQUIRED_YN='N' AND USE_YN='Y')인지 검증 + 현재버전 resolve(클라 버전 위조 차단).
        //    필수약관/미사용약관/미존재면 null → 게이트 우회 시도로 간주하여 차단.
        String currentVersion = consentMapper.selectOptionalTermsCurrentVersion(termsId);
        if (currentVersion == null || currentVersion.isBlank()) {
            log.warn("선택약관 토글 거부(선택약관 아님) - userCd={}, termsId={}", userCd, termsId);
            throw new ApiException(TermsErrorCode.TERMS_403_001);
        }

        // 2) (CMPNY_CD, USER_CD, TERMS_ID, 현재버전) AGR_YN 전이 기록 + upsert(멱등).
        //    006 철회(Y→N)도 여기로 들어오며 이력이 남는다.
        int affected = consentHistoryRecorder.recordAndUpsert(
                cmpnyCd, userCd, termsId, currentVersion, agrYn
                , source, cmpnyCd, userCd);

        log.info("선택약관 토글 완료 - userCd={}, termsId={}, agrYn={}, 경로={}, 영향행={}"
                , userCd, termsId, agrYn, source, affected);
        return affected;
    }

    @Override
    public ConsentTermsResult resolveSubconConsentGate(String cmpnyCd, String userCd) {
        // 1) 006 약관 배포 여부. 미배포(USE_YN='N' 또는 행 부재) = 제도 미가동 → 게이트 미노출.
        ConsentTermsResult terms = consentQueryService.resolveActiveTerms(ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID);
        if (terms == null) {
            log.debug("제3자 제공 동의 게이트 - 약관 미배포(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return null;
        }

        // 1-2) 006 이 '선택약관'인지 응답 저장 경로와 동일한 술어로 확인한다(REQUIRED_YN='N' AND USE_YN='Y').
        //      게이트는 노출되는데 응답 저장이 TERMS_403_001 로 막히면, 사용자는 매 로그인 게이트를 만나고
        //      어떤 버튼으로도 해소할 수 없다(동의의 자유의사도 훼손). 술어를 일치시켜 그 상태를 원천 차단한다.
        String optionalVersion = consentMapper.selectOptionalTermsCurrentVersion(terms.termsId());
        if (optionalVersion == null || optionalVersion.isBlank()) {
            log.warn("제3자 제공 동의 게이트 - 006 이 선택약관이 아님(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return null;
        }

        // 2) 소속 사업장이 활성 연동 링크에 참여(SRC 또는 DST) 중인지 DB 실측 판정(토큰 gv_siteCd 불신).
        if (!consentQueryService.isLinkedSiteMember(cmpnyCd, userCd)) {
            log.debug("제3자 제공 동의 게이트 - 비연동 사업장(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return null;
        }

        // 3) 현재버전 응답 존재 여부. 'Y'(동의)든 'N'(미동의)든 응답이 있으면 게이트 해제(재노출 없음).
        String agrYn = consentQueryService.selectUserAgrYn(
                cmpnyCd, userCd, terms.termsId(), terms.termsVersion());
        if (agrYn != null) {
            log.debug("제3자 제공 동의 게이트 - 응답 완료(게이트 미노출) cmpnyCd={}, userCd={}, agrYn={}"
                    , cmpnyCd, userCd, agrYn);
            return null;
        }

        log.info("제3자 제공 동의 게이트 노출 - cmpnyCd={}, userCd={}, termsId={}, ver={}"
                , cmpnyCd, userCd, terms.termsId(), terms.termsVersion());
        return terms;
    }

    @Override
    @Transactional
    public int respondSubconConsent(String cmpnyCd, String userCd, String agrYn, String source) {
        // 1) 약관 상수(006) 기준 현재버전 resolve. 미배포/미사용이면 응답 저장 불가.
        String termsId = ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID;
        ConsentTermsResult terms = consentQueryService.resolveActiveTerms(termsId);
        if (terms == null) {
            log.warn("제3자 제공 동의 응답 거부(약관 미배포) - userCd={}, termsId={}", userCd, termsId);
            throw new ApiException(TermsErrorCode.TERMS_404_001);
        }

        // 2) 선택약관 검증(REQUIRED_YN='N' AND USE_YN='Y') — 본 경로로 필수약관을 토글할 수 없게 재확인.
        String optionalVersion = consentMapper.selectOptionalTermsCurrentVersion(termsId);
        if (optionalVersion == null || optionalVersion.isBlank()) {
            log.warn("제3자 제공 동의 응답 거부(선택약관 아님) - userCd={}, termsId={}", userCd, termsId);
            throw new ApiException(TermsErrorCode.TERMS_403_001);
        }

        // 3) 동의('Y')/미동의('N') 모두 저장 = 응답 완료 = 게이트 해제. 전이 시에만 이력 1행.
        //    ★ 철회 소급 없음: 기존 스냅샷(tb_cmpny_share_snapshot*)은 조회조차 하지 않는다.
        int affected = consentHistoryRecorder.recordAndUpsert(
                cmpnyCd, userCd, termsId, optionalVersion, agrYn
                , source, cmpnyCd, userCd);

        log.info("제3자 제공 동의 응답 - cmpnyCd={}, userCd={}, ver={}, agrYn={}, 경로={}, 영향행={}"
                , cmpnyCd, userCd, optionalVersion, agrYn, source, affected);
        return affected;
    }
}
