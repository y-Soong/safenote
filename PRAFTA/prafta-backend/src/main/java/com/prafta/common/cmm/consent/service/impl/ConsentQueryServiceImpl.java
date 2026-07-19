package com.prafta.common.cmm.consent.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.consent.ConsentConst;
import com.prafta.common.cmm.consent.mapper.ConsentMapper;
import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;
import com.prafta.common.cmm.consent.service.ConsentQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 약관 동의 조회 서비스 구현 — PRAFTA-SUBCON-T4-02.
 *
 * <p>읽기 전용(트랜잭션 어노테이션 없음 — 프로젝트 컨벤션).
 * <p>로그에는 userCd 까지만 남긴다(성명/휴대폰 등 PII 금지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentQueryServiceImpl implements ConsentQueryService {

    private final ConsentMapper consentMapper;

    @Override
    public ConsentTermsResult resolveActiveTerms(String termsId) {
        if (termsId == null || termsId.isBlank()) return null;
        return consentMapper.selectActiveTerms(termsId);
    }

    @Override
    public boolean existsTerms(String termsId) {
        if (termsId == null || termsId.isBlank()) return false;
        return consentMapper.countTermsExists(termsId) > 0;
    }

    @Override
    public boolean isLinkedSiteMember(String cmpnyCd, String userCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || userCd == null || userCd.isBlank()) return false;

        // 소속 사업장은 DB 실측(정규 + 일용직). 토큰 gv_siteCd 는 로그인 시점 스냅샷이라 사용하지 않는다.
        String siteCd = consentMapper.selectUserSiteCd(cmpnyCd, userCd);
        if (siteCd == null || siteCd.isBlank()) {
            log.warn("동의 게이트 판정 - 소속 사업장 미확인(게이트 미노출) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return false;
        }

        // 활성 링크 참여 = 내 사업장이 제공측(SRC) 이거나 수신측(DST) — 한쪽만 보면 제공사 근로자가 누락된다.
        int linked = consentMapper.countActiveSiteLinkMember(cmpnyCd, siteCd);
        return linked > 0;
    }

    @Override
    public String selectUserAgrYn(String cmpnyCd, String userCd, String termsId, String termsVersion) {
        return consentMapper.selectUserAgrYn(cmpnyCd, userCd, termsId, termsVersion);
    }

    @Override
    public Set<String> filterConsentedUserCds(String cmpnyCd, Collection<String> userCds) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || userCds == null || userCds.isEmpty())
            return Collections.emptySet();

        // 중복 제거 + null/공백 제외(IN 절 파라미터 위생).
        List<String> targets = new ArrayList<>(new HashSet<>(userCds));
        targets.removeIf(u -> u == null || u.isBlank());
        if (targets.isEmpty()) return Collections.emptySet();

        Set<String> consented = new HashSet<>();
        // 대량 후보(기간 요청) 대비 청크 분할 — 바인딩 파라미터 폭주 방지.
        for (int i = 0; i < targets.size(); i += ConsentConst.USER_CD_CHUNK_SIZE) {
            List<String> chunk = targets.subList(i, Math.min(i + ConsentConst.USER_CD_CHUNK_SIZE, targets.size()));
            // 예외를 삼키지 않는다(fail-closed) — 조회 실패 시 전원 포함으로 fallback 하면 무동의 반출이 된다.
            List<String> rows = consentMapper.selectConsentedUserCds(
                    cmpnyCd, ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID, chunk);
            if (rows != null) consented.addAll(rows);
        }

        log.info("제3자 제공 동의 필터 - cmpnyCd={}, 후보={}건, 동의={}건", cmpnyCd, targets.size(), consented.size());
        return consented;
    }
}
