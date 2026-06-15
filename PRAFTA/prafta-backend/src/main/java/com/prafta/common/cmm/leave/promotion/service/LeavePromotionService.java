package com.prafta.common.cmm.leave.promotion.service;

import java.time.LocalDate;
import java.util.List;

import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult;
import com.prafta.common.cmm.leave.promotion.vo.PromotionCandidateVO;

/**
 * 연차 사용촉진 시점 판정·잔여 산정 공용 도메인 서비스 (PRAFTA-COM-008-A-1).
 *
 * <p>매일 1회 배치({@code LeavePromotionScheduler})가 전 사용자를 스캔해 개인별 1차(만료 6개월 전)/
 * 2차(만료 3개월 전) 촉진 도래를 판정한다. 도래 시점 비교는 결정성을 위해 {@code today} 인자를
 * 받아 수행한다(서비스 내부에서 {@code LocalDate.now()} 직접 호출 금지 — 테스트 결정성).
 *
 * <p>정책 출처: attd/08-leave §8.5.2(AXIS7), §8.5.8(멱등). 작업지시서 §1/§1-1.
 */
public interface LeavePromotionService {

    /**
     * 기준일 {@code today} 에 1차 또는 2차 촉진이 도래한 전 회사·사용자 목록을 산출한다.
     *
     * <p>대상 = AXIS7='Y' 회사 + 1년차 이상(ACTIVE STATUTORY_ANNUAL 보유) + 잔여(r_i)&gt;0.
     * 1차 = today 가 (BASE_AVAIL_TO_DATE - 6개월) 부터 10일 이내. 2차 = today == (BASE_AVAIL_TO_DATE - 3개월).
     *
     * @param today 기준일(배치가 1회 산출해 전달 — 결정성)
     * @return 도래자 목록(1차/2차 stage 포함). 없으면 빈 리스트.
     */
    List<PromotionTargetResult> resolveDueTargets(LocalDate today);

    /**
     * 단건 사용자에 대해 {@code today} 기준 촉진 도래 단계를 재산정한다(통지/지정 직전 교차체크).
     *
     * <p>입사일/부여기준 변경으로 BASE_AVAIL_TO_DATE 가 바뀌었을 수 있어, 마스터 기록 직전에
     * 동일 산식으로 다시 확인한다(§3-3). 미도래/미해당이면 null.
     *
     * @param cmpnyCd 회사 코드
     * @param userCd  사용자 코드
     * @param today   기준일
     * @return 재산정 도래 결과(미도래/미해당이면 null)
     */
    PromotionTargetResult recomputeForUser(String cmpnyCd, String userCd, LocalDate today);

    /**
     * 역산 기준 본연차 grant 해석(정책 변경 대비 단일 격리 지점).
     *
     * <p>현재 정책(확정-1): 본연차(STATUTORY_ANNUAL) ACTIVE grant 중 가장 임박한 AVAIL_TO_DATE.
     * 후보 VO 는 이미 그 기준으로 산출되어 오므로 본 메서드는 후보→기준 매핑을 캡슐화한다.
     *
     * @param candidate 도래 후보(매퍼 산출)
     * @return 역산 기준 grant 의 BASE_AVAIL_TO_DATE(YYYYMMDD). 후보가 null/무효면 null.
     */
    String resolvePromotionBaseGrant(PromotionCandidateVO candidate);
}
