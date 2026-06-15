package com.prafta.common.cmm.leave.promotion.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult;
import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult.PromotionStage;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionService;
import com.prafta.common.cmm.leave.promotion.vo.PromotionCandidateVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 사용촉진 시점 판정·잔여 산정 구현 (PRAFTA-COM-008-A-1).
 *
 * <p>도래 판정은 후보(매퍼 산출)의 BASE_AVAIL_TO_DATE 에서 역산한다.
 *   1차 = today 가 [availTo - 6개월, availTo - 6개월 + 9일] 구간(=만료 6개월 전부터 10일 이내).
 *   2차 = today == availTo - 3개월(상향 고정, 작업지시서 §1 C1).
 *   두 시점이 동시에 성립할 일은 없다(6개월 전 vs 3개월 전). 잔여 r_i &le; 0 은 매퍼에서 이미 제외.
 *
 * <p>결정성: {@code today} 를 호출부(스케줄러)에서 1회 산출해 주입받는다. 본 구현은
 *   {@code LocalDate.now()} 를 호출하지 않는다(테스트·감사 재현성).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeavePromotionServiceImpl implements LeavePromotionService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 1차 도래 창: 만료 6개월 전부터 10일 이내(시작일 포함 10일 = +0..+9). */
    private static final int STAGE1_MONTHS_BEFORE = 6;
    private static final int STAGE1_WINDOW_DAYS = 10;

    /** 2차 도래: 만료 3개월 전 정확히 그날(상향 고정). */
    private static final int STAGE2_MONTHS_BEFORE = 3;

    private final LeavePromotionMapper leavePromotionMapper;

    @Override
    public List<PromotionTargetResult> resolveDueTargets(LocalDate today) {
        List<PromotionTargetResult> due = new ArrayList<>();
        if (today == null) {
            return due;
        }
        List<PromotionCandidateVO> candidates = leavePromotionMapper.selectPromotionCandidates();
        if (candidates == null || candidates.isEmpty()) {
            return due;
        }
        for (PromotionCandidateVO c : candidates) {
            PromotionTargetResult r = toDueResult(c, today);
            if (r != null) {
                due.add(r);
            }
        }
        log.info("[leavePromotion] 촉진 도래 판정 — 기준일={}, 후보 {}명, 도래 {}건",
                today.format(YMD), candidates.size(), due.size());
        return due;
    }

    @Override
    public PromotionTargetResult recomputeForUser(String cmpnyCd, String userCd, LocalDate today) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || today == null) {
            return null;
        }
        PromotionCandidateVO c = leavePromotionMapper.selectPromotionCandidate(cmpnyCd, userCd);
        return toDueResult(c, today);
    }

    @Override
    public String resolvePromotionBaseGrant(PromotionCandidateVO candidate) {
        if (candidate == null) {
            return null;
        }
        // 현재 정책(확정-1): 후보는 이미 "가장 임박한 STATUTORY_ANNUAL ACTIVE grant" 기준으로 산출됨.
        // 정책 변경 시(예: 보유 grant 최임박/ grant별 독립) 본 메서드만 교체한다.
        return candidate.getBaseAvailToDate();
    }

    /**
     * 후보를 today 기준 도래 결과로 변환. 도래(1차/2차) 아니면 null.
     * 잔여 r_i &le; 0 은 매퍼에서 이미 제외되었으나 방어적으로 한 번 더 확인한다.
     */
    private PromotionTargetResult toDueResult(PromotionCandidateVO c, LocalDate today) {
        if (c == null) {
            return null;
        }
        String availTo = resolvePromotionBaseGrant(c);
        if (!StringUtils.hasText(availTo) || availTo.length() != 8) {
            return null;
        }
        BigDecimal remaining = c.getRemainingDays();
        if (remaining == null || remaining.signum() <= 0) {
            return null;
        }
        LocalDate availToDate;
        try {
            availToDate = LocalDate.parse(availTo, YMD);
        } catch (Exception e) {
            log.warn("[leavePromotion] BASE_AVAIL_TO_DATE 파싱 실패 — userCd={}, availTo={}",
                    c.getUserCd(), availTo);
            return null;
        }

        PromotionStage stage = determineStage(availToDate, today);
        if (stage == null) {
            return null;
        }
        return new PromotionTargetResult(
                c.getCmpnyCd(), c.getSiteCd(), c.getUserCd(),
                c.getBaseGrantId(), availTo, remaining, stage);
    }

    /**
     * 만료일 역산으로 today 의 촉진 단계를 판정. 1차/2차 미해당이면 null.
     *
     * <p>1차 창 시작 = availTo.minusMonths(6). today 가 [시작, 시작+9일] 이면 1차.
     * 2차 = today == availTo.minusMonths(3). (2차가 1차 창보다 항상 늦으므로 동시성립 없음)
     */
    private PromotionStage determineStage(LocalDate availToDate, LocalDate today) {
        LocalDate stage2Date = availToDate.minusMonths(STAGE2_MONTHS_BEFORE);
        if (today.isEqual(stage2Date)) {
            return PromotionStage.SECOND;
        }
        LocalDate stage1Start = availToDate.minusMonths(STAGE1_MONTHS_BEFORE);
        LocalDate stage1End = stage1Start.plusDays(STAGE1_WINDOW_DAYS - 1L);
        if (!today.isBefore(stage1Start) && !today.isAfter(stage1End)) {
            return PromotionStage.FIRST;
        }
        return null;
    }
}
