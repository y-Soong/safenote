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
 * <p><b>도래 판정은 구간 판정이다</b>(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §4, 확정 D5·D6·D8).
 *   배치가 특정 하루에 돌지 못하면 촉진 절차가 영구 누락되어 미사용 연차 수당 지급 의무로 직결되므로,
 *   기존 "1차 = 10일 창 / 2차 = 만료 3개월 전 당일" 단일일 판정을 아래 구간으로 완화했다.
 *   1차 촉구 선행을 전제로 한다(법 구조: 촉구 없는 직권지정은 무효 소지).
 *
 * <ul>
 *   <li>FIRST = 해당 회차 FIRST 마스터 <b>부재</b> AND today 가 [availTo-6개월, availTo-2개월) 구간</li>
 *   <li>SECOND = 해당 회차 FIRST 마스터 <b>존재</b> AND today &ge; max(availTo-3개월, 통지일+10일)
 *       AND today 가 availTo-2개월 미만</li>
 *   <li>둘 다 아니면 미해당(null)</li>
 * </ul>
 *
 * <p>availTo-2개월(=법정 2차 통보 기한) 당일 이후는 촉진 절차 자체가 불가하므로 전면 제외한다(D8).
 *   잔여 r_i &le; 0 은 매퍼에서 이미 제외. 회차 동일성은 BASE_AVAIL_TO_DATE 동등 비교로 판별한다
 *   (= DEDUP_KEY 규약과 동일 의미). 중복 통지 차단의 단일 진실은 여전히
 *   UNIQUE(CMPNY_CD, DEDUP_KEY) + DuplicateKeyException 흡수다(구간으로 넓혀도 멱등 유지).
 *
 * <p>결정성: {@code today} 를 호출부(스케줄러)에서 1회 산출해 주입받는다. 본 구현은
 *   {@code LocalDate.now()} 를 호출하지 않는다(테스트·감사 재현성).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeavePromotionServiceImpl implements LeavePromotionService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 1차 도래 구간 시작: 만료 6개월 전(이후 D8 상한까지 계속 유효 — 소급 통지 D6). */
    private static final int STAGE1_MONTHS_BEFORE = 6;

    /**
     * 구(舊) 1차 통지 창 길이(일). <b>본 클래스의 판정에서는 더 이상 사용하지 않는다</b>(D6 구간화).
     * 다만 "지연 통지"(NOTICED_DATE 가 availTo-6개월+9일 초과) 파생 판별의 기준값으로 남긴다
     * (§4 파생 규칙). 웹 1차 현황 조회는 같은 값을 자체 상수로 둔다
     * ({@code WebLeavePromo01ServiceImpl.LATE_NOTICE_WINDOW_DAYS}) — 변경 시 양쪽 동시 수정.
     */
    private static final int STAGE1_WINDOW_DAYS = 10;

    /** 2차 도래 구간 시작 하한: 만료 3개월 전(상향 고정, 작업지시서 §1 C1). */
    private static final int STAGE2_MONTHS_BEFORE = 3;

    /**
     * 촉진 절차 상한: 만료 2개월 전(D8). 법정 2차 통보 기한이므로 이 날 <b>포함</b> 이후는
     * 1차·2차 모두 도래 대상에서 제외한다(별건: 미사용 연차 수당 정산 대상).
     */
    private static final int PROMOTION_HARD_STOP_MONTHS_BEFORE = 2;

    /**
     * 1차 촉구 후 근로자 계획 제출 기한(역일, D2 — 근로기준법 제61조 "촉구받은 때부터 10일 이내").
     * 2차 판정은 이 기한이 지난 뒤에만 성립한다.
     */
    private static final int PLAN_SUBMIT_DEADLINE_DAYS = 10;

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

        // 해당 회차 FIRST 마스터 존재/최초 통지일(후보 쿼리 LEFT JOIN 1패스 산출값).
        boolean firstMasterExists = "Y".equals(c.getFirstMasterYn());
        LocalDate noticedDate = null;
        if (firstMasterExists) {
            noticedDate = parseNoticedDate(c.getFirstNoticedDate());
            if (noticedDate == null) {
                // 비정상 데이터(통지 사실은 있으나 통지일 미기록) — 법정 10일 기한 산출 불가.
                // availTo-3개월 하한만 적용해 2차 판정을 이어간다(통지 누락보다 지연 통지가 낫다 — D6).
                log.warn("[leavePromotion] FIRST 마스터 NOTICED_DATE 없음 — userCd={}, availTo={}",
                        c.getUserCd(), availTo);
            }
        }

        PromotionStage stage = determineStage(availToDate, today, firstMasterExists, noticedDate);
        if (stage == null) {
            return null;
        }
        return new PromotionTargetResult(
                c.getCmpnyCd(), c.getSiteCd(), c.getUserCd(),
                c.getBaseGrantId(), availTo, remaining, stage);
    }

    /** FIRST 마스터 통지일(YYYYMMDD) 파싱. 형식 오류/미기록이면 null. */
    private LocalDate parseNoticedDate(String noticedYmd) {
        if (!StringUtils.hasText(noticedYmd) || noticedYmd.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(noticedYmd, YMD);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 만료일 역산으로 today 의 촉진 단계를 <b>구간</b> 판정한다. 1차/2차 미해당이면 null(D5·D6·D8).
     *
     * <p>월 오프셋 연산은 전부 Java 로 통일한다({@code minusMonths} 말일 클램핑 규칙 = 8/31 -6M → 2/28).
     * SQL 에서 날짜를 다시 계산하지 않는다(경계 불일치 방지).
     *
     * <pre>
     * hardStop   = availTo - 2개월   (이 날 포함 이후 촉진 불가 — D8)
     * stage1From = availTo - 6개월
     * stage2From = availTo - 3개월
     *
     * today &ge; hardStop                         → null
     * 마스터 부재 : today &ge; stage1From         → FIRST (그 전이면 null)
     * 마스터 존재 : today &ge; max(stage2From,
     *                            통지일+10일)     → SECOND (그 전이면 null)
     * </pre>
     *
     * @param firstMasterExists 해당 회차 FIRST 마스터 존재 여부(=1차 촉구 선행 사실)
     * @param noticedDate       FIRST 마스터 최초 통지일. null(비정상)이면 stage2From 하한만 적용
     */
    private PromotionStage determineStage(LocalDate availToDate, LocalDate today,
                                          boolean firstMasterExists, LocalDate noticedDate) {
        // D8 — 만료 2개월 전 당일부터는 촉진 절차 불가(경계 = 제외).
        LocalDate hardStop = availToDate.minusMonths(PROMOTION_HARD_STOP_MONTHS_BEFORE);
        if (!today.isBefore(hardStop)) {
            return null;
        }

        LocalDate stage1From = availToDate.minusMonths(STAGE1_MONTHS_BEFORE);
        if (!firstMasterExists) {
            // 1차 미통지 — 구간 [availTo-6개월, availTo-2개월) 안이면 (소급) 통지 대상.
            return today.isBefore(stage1From) ? null : PromotionStage.FIRST;
        }

        // 1차 통지 완료 — 법정 계획 제출 기한(통지일+10일)과 availTo-3개월 중 늦은 쪽부터 2차 대상.
        LocalDate stage2From = availToDate.minusMonths(STAGE2_MONTHS_BEFORE);
        LocalDate planDeadline = (noticedDate == null)
                ? stage2From
                : noticedDate.plusDays(PLAN_SUBMIT_DEADLINE_DAYS);
        LocalDate secondFrom = stage2From.isAfter(planDeadline) ? stage2From : planDeadline;
        return today.isBefore(secondFrom) ? null : PromotionStage.SECOND;
    }
}
