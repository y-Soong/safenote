package com.prafta.app.terms.terms01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.terms.terms01.application.command.TermsAgrUpsertCommand;
import com.prafta.app.terms.terms01.application.param.OptionalTermsAgreeParam;
import com.prafta.app.terms.terms01.dto.response.OptionalTermsResponse;
import com.prafta.app.terms.terms01.dto.response.PendingTermsResponse;
import com.prafta.app.terms.terms01.dto.response.TermsAgreeResponse;
import com.prafta.app.terms.terms01.mapper.Terms01Mapper;
import com.prafta.app.terms.terms01.mapper.result.PendingTermsResult;
import com.prafta.app.terms.terms01.service.Terms01Service;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Terms01ServiceImpl implements Terms01Service {

    private final Terms01Mapper terms01Mapper;

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
                // 각 약관 현재버전에 AGR_YN='Y' upsert. CMPNY_CD/TERMS_VERSION 은 서버 산출값만 사용.
                affected += terms01Mapper.upsertTermsAgr(
                        TermsAgrUpsertCommand.of(cmpnyCd, userCd, t.termsId(), t.termsVersion(), "Y"));
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

        // 2) (CMPNY_CD, USER_CD, TERMS_ID, 현재버전) AGR_YN upsert(멱등). CMPNY_CD 는 Param(=JWT) 회사 스코프.
        int affected = terms01Mapper.upsertTermsAgr(
                TermsAgrUpsertCommand.of(param.cmpnyCd(), param.userCd(), param.termsId(), currentVersion, param.agrYn()));

        log.info("선택약관 토글 완료 - userCd={}, termsId={}, agrYn={}, 영향행={}"
                , param.userCd(), param.termsId(), param.agrYn(), affected);
        return TermsAgreeResponse.success(affected);
    }
}
