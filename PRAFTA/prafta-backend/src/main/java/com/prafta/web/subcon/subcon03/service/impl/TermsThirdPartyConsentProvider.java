package com.prafta.web.subcon.subcon03.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.consent.ConsentConst;
import com.prafta.common.cmm.consent.service.ConsentQueryService;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon03.service.ThirdPartyConsentProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 제3자 제공 동의 필터 구현체 — PRAFTA-SUBCON-T4-04.
 *
 * <p>T3(subcon03)는 인터페이스만 정의하고 ObjectProvider 로 optional 주입한다. 본 @Component 가 등록되면
 *    <b>T3 코드 수정 없이</b> 스냅샷 생성 시 미동의자 전 행 제외 + CONSENT_EXCLUDED_CNT 산출이 연결된다.
 *
 * <p>동의 단위는 (CMPNY_CD, USER_CD) 다 — siteCd/workerType 은 쿼리 조건이 아니라 <b>로그 컨텍스트</b>로만 쓴다
 *    (정규·일용직 USER_CD 가 동일 회사 스코프에서 채번되므로 같은 쿼리로 커버된다. 일용직도 동의 대상 — T3 결정 2).
 *
 * <p><b>fail-closed</b>(plan D4): 조회 예외를 잡아 "전원 포함"으로 fallback 하지 않는다.
 *    예외를 그대로 전파해 승인 트랜잭션 전체를 롤백시킨다(무동의 반출 방지).
 *
 * <p><b>약관 미배포 = 제도 미가동</b>(plan D5): 006(USE_YN='Y') 행이 없으면 동의 제도가 아직 가동되지 않은 것이므로
 *    전원 포함 + WARN(훅 부재 시 T3 기본 동작과 동일). 미배포 상태에서 필터를 걸면 전 스냅샷이 0행이 되어 T3 가 마비된다.
 *    이는 "조회 실패(장애) → fail-closed" 와 충돌하지 않는다(장애가 아니라 정상적인 미도입 상태).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TermsThirdPartyConsentProvider implements ThirdPartyConsentProvider {

    private final ConsentQueryService consentQueryService;

    @Override
    public Set<String> filterConsented(String cmpnyCd, String siteCd, String workerType, Collection<String> userCds) {
        if (userCds == null || userCds.isEmpty()) return Collections.emptySet();

        if (consentQueryService.resolveActiveTerms(ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID) == null) {
            // 활성 약관이 없다. 두 상태를 반드시 구분한다(security M-1):
            //   ① 약관 행 자체가 없음  = 제도 미도입 → 전원 포함(필터를 걸면 전 스냅샷이 0행이 되어 T3 마비).
            //   ② 행은 있으나 USE_YN='N' = 배포 후 비활성화 → 동의 필터를 적용할 수 없는 상태다.
            //      이때 전원 포함으로 진행하면 '명시적으로 미동의를 선택한' 근로자의 PII 까지 반출된다.
            //      약관 토글 하나가 동의 제도 전체의 kill-switch 가 되므로 승인을 차단한다(fail-closed).
            if (consentQueryService.existsTerms(ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID)) {
                log.error("제3자 제공 동의 약관(006) 비활성(USE_YN='N') — 동의 필터 적용 불가로 승인 차단."
                        + " cmpnyCd={}, siteCd={}, workerType={}, 후보={}건"
                        , cmpnyCd, siteCd, workerType, userCds.size());
                throw new ApiException(SubconErrorCode.SUBCON_409_009);
            }

            log.warn("제3자 제공 동의 약관(006) 미배포 — 동의 필터 미적용(전원 포함). cmpnyCd={}, siteCd={}, workerType={}, 후보={}건"
                    , cmpnyCd, siteCd, workerType, userCds.size());
            return new HashSet<>(userCds);
        }

        // 동의자만 반환(예외는 삼키지 않는다 = fail-closed).
        Set<String> consented = consentQueryService.filterConsentedUserCds(cmpnyCd, userCds);

        log.info("제3자 제공 동의 필터 적용 - cmpnyCd={}, siteCd={}, workerType={}, 후보={}건, 동의={}건, 제외={}건"
                , cmpnyCd, siteCd, workerType, userCds.size(), consented.size(), userCds.size() - consented.size());
        return consented;
    }
}
