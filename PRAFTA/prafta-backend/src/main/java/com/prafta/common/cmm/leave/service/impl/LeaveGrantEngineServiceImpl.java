package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveGrantEngineMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.BorrowGrantCapacityVO;
import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO;
import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO.BorrowGrantSlotVO;
import com.prafta.common.cmm.leave.vo.BorrowProjectionVO;
import com.prafta.common.cmm.leave.vo.HireDateAdjustResultVO;
import com.prafta.common.cmm.leave.vo.HireDateGrantResultVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantRecallRowVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewRowVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveGrantEngineService} 구현체 (prafta-022 작업 A 추출 → 작업 B·C 확장).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2(7-axis)·§8.5.3(교차 매트릭스)·
 * §8.5.4(1년 미만 월차)·§8.5.6(입사일 변경 처리 매트릭스)·§8.5.8(멱등·기부여보호 절대규칙).
 *
 * <p>작업 B(문제2): 입사일 anniversary 하드코딩을 제거하고 활성 정책의 AXIS1(HIRE_DATE/FISCAL_YEAR)에
 * 따라 부여 기준연도·본연차 부여 시점을 분기한다. PRORATE는 prafta-023로 분리되어 본 엔진에서는
 * NEXT_YEAR_BULK로 폴백한다(INFO 로그).
 *
 * <p>작업 C(문제1) 폐기(prafta-032 D1/D6/010): 입사일 변경 처리방식(SYS039 KEEP_AND_BACKFILL/
 * KEEP_AND_APPLY_NEW/RESET_ALL) 자동계산을 폐기하고 그 죽은 분기 코드를 물리 제거했다(prafta-032 009).
 * "정책 기준 부여"(Attd_09)는 이제 단일 동작만 한다 — <b>기존 부여가 있으면 변경 없음(멱등 skip), 없으면
 * 정책+입사일+경력인정 기준으로 신규 부여</b>(월차 D2-B 만1년 일괄소멸 포함). 입사일 변경에 따른 법정 연차
 * 조정(목표 부여량 차액의 추가 소급/회수)은 입사일 변경 화면의 수동 경로
 * ({@link #adjustStatutoryGrantsByHireDateChange})로 이관됐다. 부여 성공 시 미적용 이력을 APPLIED_YN='Y'로 마킹한다.
 *
 * <p>prafta-029 옵션 A(CANCELED 재활성화): 기부여 판정을 live-only(STATUS!='CANCELED' AND DEL_YN='N')로 하고,
 * 같은 표준 멱등키에 CANCELED 단건이 남아 있으면 INSERT(UNIQUE 충돌) 대신 그 단건을 ACTIVE로 부활시킨다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveGrantEngineServiceImpl implements LeaveGrantEngineService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_VALIDITY_MONTHS = 12;

    // ===== 입사일/정책 기준 부여 =====
    /** 자동(규칙) 부여 방식 코드 (GRANT_BY_TYPE, SYS043 '01'=자동 부여). */
    private static final String GRANT_BY_TYPE_AUTO = "01";
    /** 본연차 기준 일수(§8.5.4). */
    private static final int BASE_ANNUAL_DAYS = 15;
    /** 1년 미만 법정 월차 최대치(§8.5.4). */
    private static final int MONTHLY_MAX = 11;
    /** AXIS5 근속가산 법정 기본값(시작 3년차 / 2년 간격 / 최대 25일). */
    private static final int AXIS5_DEFAULT_START_YEAR = 3;
    private static final int AXIS5_DEFAULT_INTERVAL = 2;
    private static final int AXIS5_DEFAULT_MAX_DAYS = 25;
    /** 시스템 연차 종류 코드 (TB_LEAVE_TYPE_MGMT SYSTEM_YN='Y' 시드). */
    private static final String LEAVE_CD_ANNUAL = "SYS_ANNUAL";
    private static final String LEAVE_CD_MONTHLY = "SYS_MONTHLY";
    private static final String LEAVE_CD_TENURE = "SYS_TENURE_BONUS";
    /** 부여 행 분류(TB_USER_LEAVE_GRANT.GRANT_TYPE, 법정=STATUTORY_ prefix). */
    private static final String GRANT_TYPE_ANNUAL = "STATUTORY_ANNUAL";
    private static final String GRANT_TYPE_MONTHLY = "STATUTORY_MONTHLY";
    private static final String GRANT_TYPE_TENURE = "STATUTORY_TENURE_BONUS";
    private static final String HIRE_GRANT_REASON = "정책 기준 연차 부여";
    /** 1회 부여 대상 인원 상한(장시간 트랜잭션/대량 부여 방지). */
    private static final int MAX_GRANT_USER_COUNT = 500;
    /** 자동 정기부여(prafta-023 E) 수행자(INSERT_NO) 시스템 식별자. */
    private static final String SYSTEM_OPERATOR = "SYSTEM";

    // ===== AXIS1 (SYS036) =====
    private static final String AXIS1_HIRE_DATE = "HIRE_DATE";
    private static final String AXIS1_FISCAL_YEAR = "FISCAL_YEAR";
    // ===== AXIS3 (SYS037) =====
    private static final String AXIS3_PRORATE = "PRORATE";
    private static final String AXIS3_NEXT_YEAR_BULK = "NEXT_YEAR_BULK";
    private static final String AXIS3_MONTHLY_ONLY = "MONTHLY_ONLY";

    // ===== AXIS4 (SYS038) — PRORATE 비례부여 반올림 =====
    private static final String AXIS4_ROUND = "ROUND";
    private static final String AXIS4_FLOOR = "FLOOR";
    private static final String AXIS4_HALF_DAY = "HALF_DAY";
    // CEIL 은 기본값(그 외/널 포함)이라 별도 상수 분기 불필요.

    // ===== prafta-032 입사일 변경 수동 연차 조정 =====
    /** 입사일 변경 추가/회수 전용 멱등키 접미사 네임스페이스(_BF/_R 와 충돌 금지, prafta-032 D4). */
    private static final String HIRE_ADJUST_KEY_PREFIX = "_HD";
    /** 소급 추가 부여 사유(D4): 한국어 설명 + 영문 식별 코드 병기. */
    private static final String HIRE_BACKFILL_GRANT_REASON = "입사일 변경 소급(INSADAY_CHANGE_BACKFILL)";
    /** 오늘 폴백 추가 부여 사유(D4). */
    private static final String HIRE_OVERAGE_GRANT_REASON = "입사일 변경 초과 부여(MANUAL_OVERAGE)";

    // ===== prafta-com-011 연차 가불(마이너스/이월) =====
    /** 가불 GRANT 식별 마커(GRANT_REASON 프리픽스, 멱등키 밖 — plan §0-1). LIKE '[가불]%' 로 집계/회수 매칭. */
    private static final String BORROW_GRANT_REASON_PREFIX = "[가불] ";
    /** 본연차 가불 사유(차기 부여 예정 본연차를 당겨씀, 결정 §1). */
    private static final String BORROW_ANNUAL_GRANT_REASON = BORROW_GRANT_REASON_PREFIX + "차기 부여 예정 본연차 가불";
    /** 월차 가불 사유(미래 월차분을 당겨씀, 결정 §1). */
    private static final String BORROW_MONTHLY_GRANT_REASON = BORROW_GRANT_REASON_PREFIX + "미래 월차 가불";

    /**
     * 입사일 변경 조정 스냅샷 직렬화 전용 ObjectMapper (prafta-032).
     * ⚠️ @RequiredArgsConstructor 의존성으로 넣지 않는다 — 기존 단위테스트의 4-arg 생성자 호출
     *    (new LeaveGrantEngineServiceImpl(dash, eng, policySvc, statusSvc))을 깨지 않기 위함. JSON 직렬화 전용으로 충분.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LeaveDashboardMapper leaveDashboardMapper;
    private final LeaveGrantEngineMapper leaveGrantEngineMapper;
    private final LeavePolicyService leavePolicyService;
    private final LeaveGrantStatusService leaveGrantStatusService;

    // ============================================================
    // 정책 기준 연차 부여 (Attd_09 "정책 기준 부여" 버튼 경로)
    // ============================================================

    @Override
    @Transactional
    public HireDateGrantResultVO hireDateGrant(String cmpnyCd, List<String> userCds, String authCd, String operatorUserCd) {
        // 1. 진입 가드(권한/입력/연차종류) + 부여 공통값(정책/유효기간/오늘) 산정
        GrantContext ctx = prepareGrantContext(cmpnyCd, userCds, authCd);

        // 2. 대상 직원 검증 — apply는 입사일 미입력자가 1명이라도 있으면 전건 거부(기존 동작 유지)
        List<String[]> targets = resolveTargets(cmpnyCd, userCds, true); // [userCd, hireDate]

        // 3. 직원별: (a) read-only 부여 계획 산정 → (b) 계획대로 부여/마킹 실행
        //    prafta-032 D1/D6/009: 처리방식 자동계산(KEEP_*/RESET_ALL) 폐기로 취소(cancelGrant)·차액보전(c-1b)·
        //    소급재발급(c-2) 죽은 분기를 물리 제거했다. 단일 동작 = 표준 멱등키 컴포넌트(c) + 월차(c-3) 신규 부여만.
        List<String> grantedUserCds = new ArrayList<>();
        List<String> skippedUserCds = new ArrayList<>();
        BigDecimal grantedDaysTotal = BigDecimal.ZERO;

        for (String[] t : targets) {
            String tu = t[0];
            String hireDate = t[1];

            // (a) 부여 계획 산정 (DB 쓰기 없음, preview와 동일 로직 공유)
            UserGrantPlan plan = buildUserPlan(cmpnyCd, tu, hireDate, ctx);

            // (b) 부여 실행 — 계획의 당기 컴포넌트를 표준 멱등키로 INSERT(또는 옵션 A reactivate).
            //     옵션 A: 직전 CANCELED 표준키 기간(예: 종전 RESET_ALL 잔재)을 grantComponent 내부에서 reactivate로
            //     재부여한다(같은 표준키 → UNIQUE 충돌 회피).
            BigDecimal grantedDaysForUser = BigDecimal.ZERO;
            boolean grantedAny = false;
            for (PlanComponent pc : plan.components) {
                // 전환가드(prafta-028 C): buildUserPlan에서 달력연도 dual-read로 기부여 판정된 당기분은 건너뜀.
                //   (월차 루프와 동일하게 newInsert를 1차 판정으로 사용 — grantComponent는 동시성 2차 가드)
                if (!pc.newInsert) {
                    continue;
                }
                boolean inserted = grantComponent(cmpnyCd, tu, pc.leaveCd, pc.grantType, pc.days,
                        ctx.policySeq, ctx.today, plan.availFromDate, ctx.availToDate, plan.yearLabel, plan.keySuffix,
                        HIRE_GRANT_REASON, operatorUserCd);
                if (inserted) {
                    grantedAny = true;
                    grantedDaysForUser = grantedDaysForUser.add(pc.days);
                }
            }

            // (c) 월차 per-월 누적 (prafta-023 #1) — 법정 의무(§8.5.4). 레거시 ACTIVE 집계 연도는 상호배타 제외.
            //     ⚠️ prafta-030 BE-2(D2): 본연차가 실제 발생하는 직원이면 computeMonthlyPeriods가 빈 목록을 반환(월차 게이트).
            for (PeriodComponent mp : computeMonthlyPeriods(cmpnyCd, tu, hireDate, ctx, plan.keySuffix)) {
                if (!mp.newInsert) {
                    continue;
                }
                boolean insertedMonthly = grantComponent(cmpnyCd, tu, mp.leaveCd, mp.grantType, mp.days,
                        ctx.policySeq, ctx.today, mp.availFromDate, mp.availToDate, mp.periodLabel, plan.keySuffix,
                        HIRE_GRANT_REASON, operatorUserCd);
                if (insertedMonthly) {
                    grantedAny = true;
                    grantedDaysForUser = grantedDaysForUser.add(mp.days);
                }
            }

            // (d) 부여 성공 시 미적용 이력 일괄 적용 마킹 (재클릭 멱등화)
            if (grantedAny) {
                leaveGrantEngineMapper.markHireDateHistoryApplied(cmpnyCd, tu, operatorUserCd);
                grantedUserCds.add(tu);
                grantedDaysTotal = grantedDaysTotal.add(grantedDaysForUser);
            } else {
                // 부여 대상 아님(입사일 미래/0개월) 또는 이미 동일 키로 부여됨(멱등)
                skippedUserCds.add(tu);
            }
        }

        log.info("정책 기준 연차 부여 완료. cmpnyCd={}, 부여={}명, 건너뜀={}명, 총일수={}, 수행자={}",
                cmpnyCd, grantedUserCds.size(), skippedUserCds.size(), grantedDaysTotal, operatorUserCd);

        return HireDateGrantResultVO.builder()
                .grantedCount(grantedUserCds.size())
                .grantedUserCds(grantedUserCds)
                .skippedCount(skippedUserCds.size())
                .skippedUserCds(skippedUserCds)
                .grantedDays(grantedDaysTotal.setScale(1, RoundingMode.HALF_UP))
                .canceledCount(0)
                .build();
    }

    @Override
    public PolicyGrantPreviewVO previewPolicyGrant(String cmpnyCd, List<String> userCds, String authCd) {
        // 1. 진입 가드(권한/입력/연차종류) + 부여 공통값 산정 (apply와 동일 기준)
        //    ※ DB 쓰기 없음 — INSERT/UPDATE/cancelGrant 호출 금지, 조회만.
        GrantContext ctx = prepareGrantContext(cmpnyCd, userCds, authCd);

        // 2. 대상 직원 — preview는 입사일 미입력자를 전건 거부 대신 행 note로 안내(rejectIfNoHire=false)
        List<String[]> targets = resolveTargets(cmpnyCd, userCds, false); // [userCd, hireDate(null 가능)]

        // 3. 직원별 read-only 부여 계획 산정 → 집계
        //    prafta-032 D6: 처리방식 자동계산(KEEP_*/RESET_ALL) 폐기로 "재발급(reissueCount)" 집계 제거.
        //    단일 동작 = 기존 부여 있으면 변경 없음, 없으면 신규 부여(addDays>0).
        List<PolicyGrantPreviewRowVO> rows = new ArrayList<>(targets.size());
        int newGrantCount = 0;
        int noChangeCount = 0;

        for (String[] t : targets) {
            String tu = t[0];
            String hireDate = t[1];

            // 입사일 미입력 — 적용 시 전건 거부 사유. preview는 행 note로만 안내.
            if (!isValidYyyymmdd(hireDate)) {
                rows.add(PolicyGrantPreviewRowVO.builder()
                        .userCd(tu)
                        .addDays(0)
                        .note("입사일 미입력 — 적용 시 제외(전건 거부 사유)")
                        .build());
                noChangeCount++;
                continue;
            }

            UserGrantPlan plan = buildUserPlan(cmpnyCd, tu, hireDate, ctx);

            // addDays = 실제 신규 INSERT 될 일수 합(멱등 skip 분 제외). 비례부여 0.5일 가능 → BigDecimal 합산 후 표시용 반올림(prafta-023 #3).
            //   prafta-032 D1/D6/009: 처리방식(backfill/reset) 폐기로 당기 컴포넌트는 항상 합산(분기 제거).
            BigDecimal addDays = BigDecimal.ZERO;
            for (PlanComponent pc : plan.components) {
                if (pc.newInsert) {
                    addDays = addDays.add(pc.days);
                }
            }
            // 월차 per-월 누적분도 합산 (prafta-023 #1). 소급(backfill/reset) 분기는 prafta-032로 폐기됨.
            BigDecimal monthlyDays = BigDecimal.ZERO;
            for (PeriodComponent mp : computeMonthlyPeriods(cmpnyCd, tu, hireDate, ctx, plan.keySuffix)) {
                if (mp.newInsert) {
                    monthlyDays = monthlyDays.add(mp.days);
                }
            }
            addDays = addDays.add(monthlyDays);
            int addDaysInt = addDays.setScale(0, RoundingMode.HALF_UP).intValue(); // 프리뷰 표시용(0.5는 반올림)

            // 당기가 멱등(변경 없음)이라도 월차 추가분이 있으면 "변경 없음" 노트는 오해 → 보정
            String note = (monthlyDays.signum() > 0)
                    ? ("추가 예정 " + addDaysInt + "일(월차 " + monthlyDays.stripTrailingZeros().toPlainString() + ")")
                    : plan.note;

            rows.add(PolicyGrantPreviewRowVO.builder()
                    .userCd(tu)
                    .addDays(addDaysInt)
                    .note(note)
                    .build());

            if (addDaysInt > 0) {
                newGrantCount++;
            } else {
                noChangeCount++;
            }
        }

        log.info("정책 기준 부여 프리뷰 완료. cmpnyCd={}, 선택={}명, 신규부여={}명, 변경없음={}명",
                cmpnyCd, targets.size(), newGrantCount, noChangeCount);

        return PolicyGrantPreviewVO.builder()
                .selectedCount(targets.size())
                .newGrantCount(newGrantCount)
                .noChangeCount(noChangeCount)
                .rows(rows)
                .build();
    }

    @Override
    public int estimateBackfillDays(String cmpnyCd, String userCd, String hireDate) {
        // 입사일 변경 영향분석용 read-only 추정 (prafta-023 F → prafta-030 BE-1 차액 산식으로 정합). DB 쓰기 없음.
        //   권한 가드는 호출부(영향분석)가 수행. 옵션1(KEEP_AND_BACKFILL) 추가 부여 예정량 = 차액 보전 산식과 동일.
        //   차액 = (새 기준 본연차+가산 누적) − (기존 live 본연차+가산 누적). >0만 추가(미래변경/근속감소면 0).
        if (cmpnyCd == null || userCd == null || !isValidYyyymmdd(hireDate)) {
            return 0;
        }
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 추정용 경량 컨텍스트(정책 + 오늘만 사용; policySeq/availToDate는 차액 산정에 불필요)
        GrantContext ctx = new GrantContext(policy, null, today, null);
        BigDecimal shortfall = computeBackfillShortfall(cmpnyCd, userCd, hireDate, ctx);
        return shortfall.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * FISCAL 다음 회계연도 발생예정 안내 텍스트 (prafta-030 BE-3 / D3·D4, read-only).
     *
     * <p>활성 정책 AXIS1=FISCAL_YEAR 이면 "본연차 다음 회계연도(YYYY-MM-DD) 발생 예정"을, 그 외(HIRE_DATE/정책 없음)는
     * 빈 문자열을 반환한다. 다음 회계연도 시작일 = 현재 회계연도 시작일 + 1년(AXIS2 시작 MM/DD 기준, 말일 보정).
     */
    private String buildFiscalNextGrantText(LeavePolicyVO policy, String today) {
        String axis1 = (policy == null) ? AXIS1_HIRE_DATE : nvl(policy.getAxis1GrantBase(), AXIS1_HIRE_DATE);
        if (!AXIS1_FISCAL_YEAR.equals(axis1)) {
            return "";
        }
        LocalDate todayDate = parseYyyymmdd(today);
        if (todayDate == null) {
            return "";
        }
        int startMm = parseMm(policy.getAxis2FiscalStartMm(), 1);
        int startDd = parseDd(policy.getAxis2FiscalStartDd(), 1);
        LocalDate currentFiscalStart = currentFiscalStart(todayDate, startMm, startDd);
        LocalDate nextFiscalStart = safeMonthDay(currentFiscalStart.getYear() + 1, startMm, startDd);
        String ymd = String.format("%04d-%02d-%02d",
                nextFiscalStart.getYear(), nextFiscalStart.getMonthValue(), nextFiscalStart.getDayOfMonth());
        return "본연차 다음 회계연도(" + ymd + ") 발생 예정";
    }

    /** BigDecimal 일수를 "N일" 라벨로(소수 끝자리 0 제거: 1.0 → "1일", 0.5 → "0.5일"). */
    private String dayLabel(BigDecimal days) {
        BigDecimal v = (days == null) ? BigDecimal.ZERO : days.stripTrailingZeros();
        if (v.scale() < 0) {
            v = v.setScale(0);
        }
        return v.toPlainString() + "일";
    }

    @Override
    public String fiscalNextGrantText(String cmpnyCd) {
        // prafta-032 D1: 옵션 시뮬은 폐기하되 FISCAL 다음 회계연도 발생예정 텍스트는 영향분석에 유지. read-only.
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return buildFiscalNextGrantText(policy, today);
    }

    @Override
    public int runScheduledAutoGrant() {
        // 자동 정기부여 1회 실행 (prafta-023 E). 스케줄러(LeaveGrantScheduler)가 게이트 ON일 때만 호출.
        // 활성 정책 회사별 → 입사일 보유 활성 직원 → 청크(≤500) 단위로 hireDateGrant 위임(청크별 트랜잭션).
        int total = 0;
        List<String> companies = leaveGrantEngineMapper.selectAutoGrantCompanyCds();
        for (String cmpnyCd : companies) {
            List<String> users = leaveGrantEngineMapper.selectActiveUserCdsForAutoGrant(cmpnyCd);
            for (int i = 0; i < users.size(); i += MAX_GRANT_USER_COUNT) {
                List<String> chunk = new ArrayList<>(
                        users.subList(i, Math.min(i + MAX_GRANT_USER_COUNT, users.size())));
                try {
                    // 시스템 컨텍스트: 권한은 MASTER로 통과(무인), 수행자는 SYSTEM. 입사일 보유자만 추렸으므로 전건거부 미발생.
                    HireDateGrantResultVO r = hireDateGrant(
                            cmpnyCd, chunk, AuthRoleUtils.AUTH_MASTER, SYSTEM_OPERATOR);
                    total += r.getGrantedCount();
                } catch (Exception e) {
                    // 한 청크 실패가 다른 회사/청크를 막지 않도록 격리(청크 트랜잭션은 롤백).
                    log.error("자동 정기부여 청크 실패 — cmpnyCd={}, fromIdx={}", cmpnyCd, i, e);
                }
            }
        }
        log.info("자동 정기부여 실행 완료 — 회사 {}곳, 부여 {}명", companies.size(), total);
        return total;
    }

    // ============================================================
    // 입사일 변경 수동 연차 조정 (prafta-032 D3/D4/D5)
    //   호출부(User01ServiceImpl.updateUserHireDate)의 단일 트랜잭션에 합류한다(D8, REQUIRED).
    //   처리방식 자동계산(KEEP_*/RESET_ALL)을 대체하는 명시 경로 — 법정(STATUTORY_*)만 다룬다.
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HireDateAdjustResultVO adjustStatutoryGrantsByHireDateChange(String cmpnyCd, String userCd,
                                                                        String newHireDate, BigDecimal target,
                                                                        String withdrawReason, String histId,
                                                                        String operatorUserCd) {
        requireCmpnyCd(cmpnyCd);

        // 현재 법정 부여량(ACTIVE STATUTORY GRANT_DAYS 합) — 차액 기준선
        BigDecimal currentTotal = nvlZero(leaveGrantEngineMapper.selectActiveStatutoryGrantedTotal(cmpnyCd, userCd))
                .setScale(1, RoundingMode.HALF_UP);

        // 목표 미입력 → 조정 없음(차액 0). 현재값만 스냅샷으로 반환.
        if (target == null) {
            return HireDateAdjustResultVO.builder()
                    .oldGrantTotal(currentTotal)
                    .newGrantTotal(currentTotal)
                    .diff(BigDecimal.ZERO.setScale(1))
                    .addedDays(BigDecimal.ZERO.setScale(1))
                    .withdrawnDays(BigDecimal.ZERO.setScale(1))
                    .recallableDays(nvlZero(leaveGrantEngineMapper.selectRecallableStatutoryTotal(cmpnyCd, userCd))
                            .setScale(1, RoundingMode.HALF_UP))
                    .canceledGrantCount(0)
                    .reducedGrantCount(0)
                    .addedGrantCount(0)
                    .affectedSnapshotJson(null)
                    .build();
        }

        // 목표는 0 이상이어야 한다(음수 목표는 입력 오류).
        if (target.signum() < 0) {
            throw new ApiException(UserErrorCode.USER_400_032);
        }

        BigDecimal newTotal = target.setScale(1, RoundingMode.HALF_UP);
        BigDecimal diff = newTotal.subtract(currentTotal); // 목표 − 현재

        BigDecimal recallable = nvlZero(leaveGrantEngineMapper.selectRecallableStatutoryTotal(cmpnyCd, userCd))
                .setScale(1, RoundingMode.HALF_UP);

        if (diff.signum() == 0) {
            // 무처리(차액 0) — 현재값/회수가능량만 스냅샷.
            return HireDateAdjustResultVO.builder()
                    .oldGrantTotal(currentTotal).newGrantTotal(newTotal).diff(diff.setScale(1))
                    .addedDays(BigDecimal.ZERO.setScale(1)).withdrawnDays(BigDecimal.ZERO.setScale(1))
                    .recallableDays(recallable)
                    .canceledGrantCount(0).reducedGrantCount(0).addedGrantCount(0)
                    .affectedSnapshotJson(null)
                    .build();
        }

        if (diff.signum() > 0) {
            // 차액 > 0: 추가 부여 (D4)
            return applyHireChangeAdd(cmpnyCd, userCd, newHireDate, currentTotal, newTotal, diff, recallable,
                    histId, operatorUserCd);
        }

        // 차액 < 0: 회수 (D3 검증 + D5 차감)
        BigDecimal recallAmount = diff.abs(); // (현재 − 목표)
        return applyHireChangeRecall(cmpnyCd, userCd, currentTotal, newTotal, diff, recallAmount, recallable,
                withdrawReason, histId, operatorUserCd);
    }

    // ============================================================
    // 연차 가불(마이너스/이월) 코어 (prafta-com-011-1)
    //   출처: prafta-com-011-decisions.md §1·§2·§3·§6 / attd/08-leave.md §8.5.4·§8.5.8.
    //   read-only projection(한도/차기예정/만료검증) + 가불 GRANT 생성/회수. 기존 산식/채번/CANCEL 패턴 재사용.
    // ============================================================

    @Override
    public BigDecimal computeBorrowQuota(String cmpnyCd, String userCd, String hireDate, BorrowFamily family) {
        // read-only. 입력/입사일 유효성 방어 — 산정 불가면 가불 한도 0.
        if (cmpnyCd == null || userCd == null || family == null || !isValidYyyymmdd(hireDate)) {
            return BigDecimal.ZERO;
        }
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = LocalDate.now();
        if (hire == null || hire.isAfter(todayDate)) {
            return BigDecimal.ZERO;
        }

        if (family == BorrowFamily.MONTHLY) {
            // 월차 가불 한도 = 11 − min(actualMonths, 11) − 이미 가불한 월차 일수 (결정 §2).
            //   1년(만1년 도래일) 경과 → 월차 일괄소멸이라 한도 0. 경력인정 더블딥(월차 게이트)도 0.
            //   만료(만1년 도래일) 경과 판정: 입사+1년−1일 < 오늘 → 0.
            String monthlyExpiry = hire.plusYears(1).minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
            String today = todayDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            if (monthlyExpiry.compareTo(today) < 0) {
                return BigDecimal.ZERO; // 만1년 경과(소멸)
            }
            LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
            int creditMonths = leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd);
            if (isCreditDoubleDip(policy, hire, todayDate, creditMonths, cmpnyCd, userCd)) {
                return BigDecimal.ZERO; // 더블딥(월차 비대상)
            }
            int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
            int accrued = Math.min(actualMonths, MONTHLY_MAX);
            // 통일 모델(§6-2): 이미 당겨쓴 일수 = 미발생(AVAIL_FROM>today) 가불 GRANT 의 USED 합.
            BigDecimal alreadyBorrowed = nvlZero(
                    leaveDashboardMapper.selectBorrowedDaysTotal(cmpnyCd, userCd, GRANT_TYPE_MONTHLY, today));
            BigDecimal quota = BigDecimal.valueOf(MONTHLY_MAX - accrued).subtract(alreadyBorrowed);
            return (quota.signum() > 0) ? quota.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }

        // 본연차 가불 한도 = 차기 부여 예정 본연차(+근속가산) − 이미 가불한 본연차 일수 (결정 §2).
        BorrowProjectionVO proj = projectNextAnnualGrant(cmpnyCd, userCd, hireDate);
        BigDecimal projected = nvlZero(proj.getDays());
        if (projected.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        // 차기 만료일이 이미 경과(소멸)면 가불 의미 없음 → 0.
        String today = todayDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        if (proj.getAvailToYmd() != null && proj.getAvailToYmd().compareTo(today) < 0) {
            return BigDecimal.ZERO;
        }
        // 통일 모델(§6-2): 이미 당겨쓴 일수 = 미발생(AVAIL_FROM>today) 가불 본연차 GRANT 의 USED 합.
        BigDecimal alreadyBorrowed = nvlZero(
                leaveDashboardMapper.selectBorrowedDaysTotal(cmpnyCd, userCd, GRANT_TYPE_ANNUAL, today));
        BigDecimal quota = projected.subtract(alreadyBorrowed);
        return (quota.signum() > 0) ? quota.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public BorrowProjectionVO projectNextAnnualGrant(String cmpnyCd, String userCd, String hireDate) {
        // read-only. 차기 회차 시점(AXIS1=HIRE_DATE → 다음 입사 기념일 / FISCAL_YEAR → 다음 회계연도 시작)을
        //   AVAIL_FROM 기준으로, resolveEntitlement 를 "차기 시점 가상 오늘"로 호출해 STATUTORY_ANNUAL +
        //   STATUTORY_TENURE_BONUS 예정 일수를 합산하고, 발생일 + AXIS6 유효개월로 만료일을 산정한다(Q2).
        BorrowProjectionVO empty = BorrowProjectionVO.builder()
                .days(BigDecimal.ZERO).availFromYmd(null).availToYmd(null).build();
        if (cmpnyCd == null || userCd == null || !isValidYyyymmdd(hireDate)) {
            return empty;
        }
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = LocalDate.now();
        if (hire == null || hire.isAfter(todayDate)) {
            return empty;
        }
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
        String axis1 = (policy == null) ? AXIS1_HIRE_DATE : nvl(policy.getAxis1GrantBase(), AXIS1_HIRE_DATE);
        int validityMonths = resolveValidityMonths(cmpnyCd);

        // 차기 부여 발생일 산정.
        LocalDate nextAccrual;
        if (AXIS1_FISCAL_YEAR.equals(axis1)) {
            int startMm = parseMm(policy == null ? null : policy.getAxis2FiscalStartMm(), 1);
            int startDd = parseDd(policy == null ? null : policy.getAxis2FiscalStartDd(), 1);
            LocalDate currentFiscalStart = currentFiscalStart(todayDate, startMm, startDd);
            nextAccrual = safeMonthDay(currentFiscalStart.getYear() + 1, startMm, startDd);
        } else {
            // HIRE_DATE: 다음 입사 기념일(가장 가까운 미래 anniversary).
            int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
            int completedYears = actualMonths / 12;
            nextAccrual = hire.plusMonths(12L * (completedYears + 1));
        }

        // 차기 발생일 시점의 entitlement(본연차+근속가산) 산정 — 기존 resolveEntitlement/tenureBonusDays 재사용.
        //   "그 시점의 오늘"을 차기 발생일로 본 산정근속으로 본연차/가산 예정 일수를 얻는다.
        BigDecimal days = projectAnnualEntitlementAt(policy, hire, nextAccrual, cmpnyCd, userCd);
        String availFrom = nextAccrual.format(DateTimeFormatter.BASIC_ISO_DATE);
        String availTo = addMonthsYyyymmdd(availFrom, validityMonths);

        return BorrowProjectionVO.builder()
                .days(days.setScale(1, RoundingMode.HALF_UP))
                .availFromYmd(availFrom)
                .availToYmd(availTo)
                .build();
    }

    /**
     * 차기 발생일(atAccrual) 시점의 본연차(STATUTORY_ANNUAL) + 근속가산(STATUTORY_TENURE_BONUS) 예정 일수 합 (read-only).
     *
     * <p>{@link #resolveEntitlement}/{@link #tenureBonusDays} 를 재사용한다. AXIS1 분기/경력인정 가산은 엔진
     * 산식을 그대로 따른다(중복 산식 금지). 차기 발생일 시점에 본연차가 발생하지 않으면(예: FISCAL crossed==0
     * 직후 등) 0 을 반환할 수 있다. 월차(STATUTORY_MONTHLY)는 본연차 가불 대상이 아니므로 합산에서 제외한다.
     */
    private BigDecimal projectAnnualEntitlementAt(LeavePolicyVO policy, LocalDate hire, LocalDate atAccrual,
                                                  String cmpnyCd, String userCd) {
        // resolveEntitlement 는 내부적으로 LocalDate.now() 를 "오늘"로 쓴다. 차기 시점 산정을 위해 동일 산식을
        //   재현하되 "오늘"을 atAccrual 로 본다(엔진 산식 재사용: actualMonths/creditedMonths → 본연차/가산).
        int creditMonths = Math.max(0, leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd));
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, atAccrual));
        int creditedMonths = actualMonths + creditMonths;
        int creditedYears = creditedMonths / 12;

        BigDecimal sum = BigDecimal.ZERO;
        if (creditedMonths >= 12) {
            // 본연차 15일 + 근속가산(차기 시점 근속연차 기준). HIRE_DATE/FISCAL 공통으로 12개월 이상이면 본연차 발생.
            sum = sum.add(BigDecimal.valueOf(BASE_ANNUAL_DAYS));
            int bonus = tenureBonusDays(policy, Math.max(1, creditedYears));
            if (bonus > 0) {
                sum = sum.add(BigDecimal.valueOf(bonus));
            }
        }
        return sum;
    }

    @Override
    public void assertBorrowWorkYmdWithinExpiry(String cmpnyCd, String userCd, String hireDate, String workYmd,
                                                BorrowFamily family) {
        // fail-closed. 만료(소멸)일을 지난 workYmd 면 ATTD_400_181 (결정 §3). 입력 불량도 차단.
        if (family == null || !isValidYyyymmdd(hireDate) || !isValidYyyymmdd(workYmd)) {
            throw new ApiException(AttdErrorCode.ATTD_400_181);
        }
        LocalDate hire = parseYyyymmdd(hireDate);
        if (hire == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_181);
        }
        String expiry;
        if (family == BorrowFamily.MONTHLY) {
            // 월차 만료 = 입사 + 1년 − 1일(첫해 월차 일괄소멸일, §8.5.4 / D2-B).
            expiry = hire.plusYears(1).minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        } else {
            BorrowProjectionVO proj = projectNextAnnualGrant(cmpnyCd, userCd, hireDate);
            expiry = proj.getAvailToYmd();
            if (expiry == null) {
                // 차기 만료 산정 불가 → fail-closed 차단.
                throw new ApiException(AttdErrorCode.ATTD_400_181);
            }
        }
        if (workYmd.compareTo(expiry) > 0) {
            log.warn("가불 만료 경과 차단 — cmpnyCd={}, userCd={}, family={}, workYmd={}, expiry={}",
                    cmpnyCd, userCd, family, workYmd, expiry);
            throw new ApiException(AttdErrorCode.ATTD_400_181);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowGrantResultVO createBorrowGrant(String cmpnyCd, String userCd, String hireDate, BorrowFamily family,
                                                 BigDecimal days, String workYmd, String operatorUserCd) {
        requireCmpnyCd(cmpnyCd);
        if (userCd == null || family == null || !isValidYyyymmdd(hireDate) || !isValidYyyymmdd(workYmd)) {
            throw new ApiException(AttdErrorCode.ATTD_400_180);
        }
        BigDecimal need = (days == null) ? BigDecimal.ZERO : days.setScale(1, RoundingMode.HALF_UP);
        if (need.signum() <= 0) {
            // 충당할 부족분이 없으면 생성 0건(잔여 충분 — 결정 §6-1 Q1=b).
            return BorrowGrantResultVO.builder()
                    .slots(new ArrayList<>())
                    .createdDays(BigDecimal.ZERO.setScale(1))
                    .skippedDays(BigDecimal.ZERO.setScale(1))
                    .build();
        }

        LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
        Long policySeq = (policy == null) ? null : policy.getPolicySeq();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        if (family == BorrowFamily.MONTHLY) {
            return createMonthlyBorrowGrant(cmpnyCd, userCd, hireDate, need, workYmd, policySeq, today, operatorUserCd);
        }
        return createAnnualBorrowGrant(cmpnyCd, userCd, hireDate, need, workYmd, policySeq, today, operatorUserCd);
    }

    /**
     * 월차 가불 GRANT 생성 (결정 §6-2 통일 모델, Q3). "다음 미발생 월차 슬롯(입사+m개월, m=actualMonths+1..11)"부터
     * 순서대로 점유한다. 슬롯(미발생 월)당 GRANT 는 <b>전량 1.0</b>으로 생성하고(AVAIL_FROM=그 슬롯 발생일=입사+m개월,
     * AVAIL_TO=입사+1년−1일), 가불 사용분만큼만 leave_use 로 차감한다. 슬롯은 부분 사용 가능(반차면 USED=0.5, 잔여
     * 0.5 는 발생일까지 잠금). 누적 가불(§6-2): 같은 슬롯 멱등키로 이미 생성된 가불 GRANT 에 잔여 capacity 가
     * 있으면 신규 INSERT 대신 그 GRANT 를 재사용해 충당한다. 멱등키 라벨 = 슬롯 YYYYMM(정기 부여 배치 키 동일).
     * 정기 부여로 이미 발생(non-borrow live)한 슬롯은 skip(가불 불필요). 한도(11−발생분) 내에서만 생성한다.
     */
    private BorrowGrantResultVO createMonthlyBorrowGrant(String cmpnyCd, String userCd, String hireDate,
                                                         BigDecimal need, String workYmd, Long policySeq,
                                                         String today, String operatorUserCd) {
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = LocalDate.now();
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
        // 월차 만료 = 입사+1년−1일(첫해 월차 일괄소멸일) — AVAIL_TO 로 설정(결정 §3 / §8.5.4).
        String monthlyAvailTo = hire.plusYears(1).minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);

        List<BorrowGrantSlotVO> slots = new ArrayList<>();
        BigDecimal created = BigDecimal.ZERO; // 이번 호출로 충당(차감 예정)한 일수 합
        BigDecimal skipped = BigDecimal.ZERO; // 이미 정기 발생되어 가불 불필요로 건너뛴 슬롯 일수 합
        BigDecimal remaining = need;

        // 다음 미발생 월차 슬롯 = m = actualMonths+1 .. MONTHLY_MAX(11). 슬롯당 전량 1.0, 부분 차감 가능.
        for (int m = actualMonths + 1; m <= MONTHLY_MAX && remaining.signum() > 0; m++) {
            LocalDate accrual = hire.plusMonths(m); // m번째 월차 발생일(미래)
            String yyyymm = accrual.format(DateTimeFormatter.ofPattern("yyyyMM"));
            String availFrom = accrual.format(DateTimeFormatter.BASIC_ISO_DATE); // 발생일(미래) — 통일 모델 §6-2
            BorrowGrantSlotVO slot = resolveBorrowSlot(cmpnyCd, userCd, LEAVE_CD_MONTHLY, GRANT_TYPE_MONTHLY,
                    BigDecimal.ONE, yyyymm, policySeq, today, availFrom, monthlyAvailTo,
                    BORROW_MONTHLY_GRANT_REASON, operatorUserCd, remaining);
            if (slot == null) {
                // 이미 정기 발생(non-borrow live)된 슬롯 → 가불 대상 아님(skip). 다음 슬롯으로.
                skipped = skipped.add(BigDecimal.ONE);
                continue;
            }
            slots.add(slot);
            BigDecimal take = slot.getDays();
            created = created.add(take);
            remaining = remaining.subtract(take);
        }

        if (remaining.signum() > 0) {
            // 미래 월차 슬롯이 부족분을 못 채움 → 한도 초과(결정 §2). 트랜잭션 롤백.
            log.warn("월차 가불 한도 초과 — cmpnyCd={}, userCd={}, need={}, 잔여미충당={}",
                    cmpnyCd, userCd, need.toPlainString(), remaining.toPlainString());
            throw new ApiException(AttdErrorCode.ATTD_400_182);
        }

        log.info("월차 가불 생성 — cmpnyCd={}, userCd={}, 충당={}일, skip(이미발생)={}일",
                cmpnyCd, userCd, created.toPlainString(), skipped.toPlainString());
        return BorrowGrantResultVO.builder()
                .slots(slots)
                .createdDays(created.setScale(1, RoundingMode.HALF_UP))
                .skippedDays(skipped.setScale(1, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 본연차 가불 GRANT 생성 (결정 §6-2 통일 모델). 차기 부여 예정 본연차 1슬롯(STATUTORY_ANNUAL)을 <b>전량</b>
     * (projectNextAnnualGrant().days)으로 생성하고(AVAIL_FROM=차기 부여일, AVAIL_TO=차기 만료일), 가불 사용분만
     * leave_use 로 차감한다. 누적 가불(§6-2): 같은 차기연도 멱등키로 이미 생성된 가불 GRANT 에 잔여 capacity 가
     * 있으면 재사용. 멱등키 라벨 = 차기 발생연도 YYYY(정기 부여 배치 키 동일). 정기 부여로 이미 발생(non-borrow live)
     * 한 경우 skip(이미 부여 = 가불 불필요).
     */
    private BorrowGrantResultVO createAnnualBorrowGrant(String cmpnyCd, String userCd, String hireDate,
                                                        BigDecimal need, String workYmd, Long policySeq,
                                                        String today, String operatorUserCd) {
        BorrowProjectionVO proj = projectNextAnnualGrant(cmpnyCd, userCd, hireDate);
        BigDecimal fullDays = nvlZero(proj.getDays());
        if (proj.getAvailFromYmd() == null || fullDays.signum() <= 0) {
            // 차기 본연차 발생 자체가 없음 → 가불 불가(한도 0인데 호출됨). 한도 초과로 차단.
            log.warn("본연차 가불 불가(차기 발생 없음) — cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            throw new ApiException(AttdErrorCode.ATTD_400_182);
        }
        String yearLabel = String.valueOf(parseYyyymmdd(proj.getAvailFromYmd()).getYear());

        // 차기 부여일이 미래라면 AVAIL_FROM=차기 부여일(미발생 잠금), 본연차 GRANT_DAYS=차기 전량.
        BorrowGrantSlotVO slot = resolveBorrowSlot(cmpnyCd, userCd, LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL,
                fullDays, yearLabel, policySeq, today, proj.getAvailFromYmd(), proj.getAvailToYmd(),
                BORROW_ANNUAL_GRANT_REASON, operatorUserCd, need);

        List<BorrowGrantSlotVO> slots = new ArrayList<>();
        BigDecimal created;
        BigDecimal skipped;
        if (slot == null) {
            // 이미 차기 본연차가 정기 부여됨(non-borrow live) → 가불 불필요인데 잔여 부족 경로로 진입한 것.
            //   이 GRANT 로 충당할 grantId 가 없어 신청흐름이 충당 불가 → 한도 초과로 차단(롤백).
            log.warn("본연차 가불 불가(이미 정기 부여됨) — cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            throw new ApiException(AttdErrorCode.ATTD_400_182);
        } else {
            slots.add(slot);
            created = slot.getDays();
            skipped = BigDecimal.ZERO;
        }

        log.info("본연차 가불 생성 — cmpnyCd={}, userCd={}, 차기전량={}, 충당={}",
                cmpnyCd, userCd, fullDays.toPlainString(), created.toPlainString());
        return BorrowGrantResultVO.builder()
                .slots(slots)
                .createdDays(created.setScale(1, RoundingMode.HALF_UP))
                .skippedDays(skipped.setScale(1, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 가불 슬롯 1건 충당 (결정 §6-2 통일 모델). 멱등키 = 정기 부여 배치와 동일({@code {userCd}_{periodLabel}_{grantType}}).
     *
     * <ol>
     *   <li><b>이미 정기 발생(non-borrow live)</b> 슬롯: 가불 마커가 아닌 live GRANT 가 멱등키를 점유 → 가불 불필요로
     *       {@code null} 반환(호출부가 skip/차단 판단).</li>
     *   <li><b>기존 가불 GRANT 재사용</b>(누적 가불 §6-2): 같은 멱등키로 가불 GRANT 가 있으면 잔여 capacity
     *       (GRANT_DAYS−USED_DAYS) 내에서 {@code remaining} 만큼 충당 slot(grantId=기존, days=take, created=false) 반환.
     *       capacity 0 이면 충당 불가(null) — 한도 초과로 이어짐.</li>
     *   <li><b>신규 생성</b>: 멱등키 미점유면 전량({@code fullDays})으로 INSERT(AVAIL_FROM=발생일(미래)) 후
     *       {@code remaining} 만큼(≤fullDays) 충당 slot(grantId=신규, days=take, created=true) 반환.</li>
     * </ol>
     *
     * <p>가불 GRANT 는 정기 부여와 같은 멱등키를 점유하므로, 추후 정기 부여 배치가 같은 키로 돌면 멱등 skip 되어
     * 이중 부여가 자동 회피된다(자동 상계, 결정 §6). 마커가 멱등키 밖(GRANT_REASON)에 있어 키는 배치와 동일하다.
     * AVAIL_FROM 을 발생일(미래)로 두어 가불 안 한 잔여분이 발생일 전까지 일반 신청으로 새지 않게 잠근다(§6-2).
     *
     * @param fullDays  슬롯 전량(월차 1.0 / 본연차 차기 전량) — 신규 GRANT_DAYS
     * @param remaining 아직 충당해야 할 부족분 — 이 슬롯에서 take = min(capacity, remaining)
     * @return 충당 slot(grantId/days=take). 이미 정기 발생이면 null.
     */
    private BorrowGrantSlotVO resolveBorrowSlot(String cmpnyCd, String userCd, String leaveCd, String grantType,
                                                BigDecimal fullDays, String periodLabel, Long policySeq, String today,
                                                String availFrom, String availTo, String grantReason,
                                                String operatorUserCd, BigDecimal remaining) {
        String idempotencyKey = buildIdempotencyKey(userCd, periodLabel, grantType, "");

        // (2) 누적 가불: 같은 멱등키로 기존 live 가불 GRANT 가 있으면 그 잔여 capacity 로 충당(신규 INSERT 안 함).
        BorrowGrantCapacityVO existing = leaveDashboardMapper.selectBorrowGrantByKey(cmpnyCd, idempotencyKey);
        if (existing != null) {
            BigDecimal capacity = nvlZero(existing.getGrantDays()).subtract(nvlZero(existing.getUsedDays()));
            if (capacity.signum() <= 0) {
                // 기존 가불 GRANT 가 이미 전량 소진 → 이 슬롯으로는 더 충당 불가(null). 한도 초과로 이어짐.
                log.info("가불 슬롯 capacity 소진 — cmpnyCd={}, key={}", cmpnyCd, idempotencyKey);
                return null;
            }
            BigDecimal take = capacity.min(remaining).setScale(1, RoundingMode.HALF_UP);
            return BorrowGrantSlotVO.builder()
                    .grantId(existing.getGrantId()).days(take).periodLabel(periodLabel)
                    .grantType(grantType).created(false).build();
        }

        // (1) 이미 정기 발생(non-borrow live)된 슬롯이면 가불 불필요 → null(호출부 skip/차단).
        //     existing(가불 GRANT)이 없는데 멱등키가 live 점유돼 있으면 그건 정기 부여분이다.
        if (alreadyGranted(cmpnyCd, userCd, periodLabel, grantType, "")) {
            log.info("가불 슬롯 이미 정기 발생 — cmpnyCd={}, key={}", cmpnyCd, idempotencyKey);
            return null;
        }

        // (3) 신규 생성: 전량(fullDays)으로 INSERT, AVAIL_FROM=발생일(미래). 그 뒤 remaining 만큼 충당.
        LeaveGrantInsertVO vo = new LeaveGrantInsertVO();
        String grantId = leaveDashboardMapper.selectNextGrantId(cmpnyCd);
        vo.setGrantId(grantId);
        vo.setCmpnyCd(cmpnyCd);
        vo.setUserCd(userCd);
        vo.setLeaveCd(leaveCd);
        vo.setGrantType(grantType);
        vo.setGrantDays(fullDays.setScale(1, RoundingMode.HALF_UP)); // 통일 모델 §6-2: 전량 부여
        vo.setUsedDays(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)); // 차감은 호출부 leave_use 가 USED 증액
        vo.setGrantReason(grantReason);
        vo.setGrantByType(GRANT_BY_TYPE_AUTO); // '01' — prafta-031 회수 대상(MANUAL_%/'02')에 미해당
        vo.setPolicySeq(policySeq);
        vo.setGrantDate(today);
        vo.setAvailFromDate(availFrom); // 통일 모델 §6-2: 발생일(미래) — 미발생 잔여 잠금
        vo.setAvailToDate(availTo);
        vo.setIdempotencyKey(idempotencyKey);
        vo.setStatus(STATUS_ACTIVE);
        vo.setInsertNo(operatorUserCd);

        try {
            leaveDashboardMapper.insertManualGrant(vo);
        } catch (DuplicateKeyException e) {
            // 동시 호출 경합(TOCTOU): UNIQUE(CMPNY_CD, IDEMPOTENCY_KEY)가 최종 차단.
            //   경합 상대가 가불이면 그 GRANT 재사용, 정기면 발생분이라 skip 으로 본다(재조회).
            BorrowGrantCapacityVO raced = leaveDashboardMapper.selectBorrowGrantByKey(cmpnyCd, idempotencyKey);
            if (raced != null) {
                BigDecimal capacity = nvlZero(raced.getGrantDays()).subtract(nvlZero(raced.getUsedDays()));
                if (capacity.signum() > 0) {
                    BigDecimal take = capacity.min(remaining).setScale(1, RoundingMode.HALF_UP);
                    return BorrowGrantSlotVO.builder()
                            .grantId(raced.getGrantId()).days(take).periodLabel(periodLabel)
                            .grantType(grantType).created(false).build();
                }
            }
            log.info("가불 멱등키 경합으로 충당 불가 — cmpnyCd={}, key={}", cmpnyCd, idempotencyKey);
            return null;
        }
        BigDecimal take = fullDays.min(remaining).setScale(1, RoundingMode.HALF_UP);
        return BorrowGrantSlotVO.builder()
                .grantId(grantId).days(take).periodLabel(periodLabel).grantType(grantType).created(true).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelBorrowGrantByReqId(String cmpnyCd, String reqId, String operatorUserCd) {
        requireCmpnyCd(cmpnyCd);
        if (reqId == null || reqId.isBlank()) {
            return 0;
        }
        // 호출부가 leave_use 차감 해제(recompute → USED_DAYS=0)를 먼저 수행한 뒤 호출한다(결정 §6, plan §0-4).
        List<String> grantIds = leaveDashboardMapper.selectBorrowGrantIdsForCancel(cmpnyCd, reqId);
        int canceled = 0;
        for (String grantId : grantIds) {
            int updated = leaveDashboardMapper.cancelBorrowGrant(cmpnyCd, grantId, operatorUserCd);
            if (updated == 1) {
                canceled++;
            }
        }
        if (canceled > 0) {
            log.info("가불 GRANT 회수 — cmpnyCd={}, reqId={}, 회수={}건", cmpnyCd, reqId, canceled);
        }
        return canceled;
    }

    /**
     * 차액 &gt; 0 추가 부여 (prafta-032 D4). 새 입사일 기준 미부여 발생일(오늘 이전) 빠른순 소급 부여 + 오늘 폴백.
     * 발생일 시점 산정근속으로 GRANT_TYPE 자동판단. GRANT_BY_TYPE='01', 멱등키 접미사 _HD{histId}.
     */
    private HireDateAdjustResultVO applyHireChangeAdd(String cmpnyCd, String userCd, String newHireDate,
                                                      BigDecimal currentTotal, BigDecimal newTotal, BigDecimal diff,
                                                      BigDecimal recallable, String histId, String operatorUserCd) {
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
        Long policySeq = (policy == null) ? null : policy.getPolicySeq();
        int validityMonths = resolveValidityMonths(cmpnyCd);
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // 1) 새 입사일 기준 발생했어야 할 "미부여 발생일"(오늘 이전, 기존 표준 부여 미커버) 빠른순 산출
        List<AccrualEvent> events = computeUngrantedAccrualEvents(cmpnyCd, userCd, newHireDate, policy,
                validityMonths, today);

        BigDecimal remaining = diff;
        BigDecimal addedTotal = BigDecimal.ZERO;
        int addedCount = 0;
        List<Map<String, Object>> snapshot = new ArrayList<>();

        // 2) 미부여 발생일에 빠른 순으로 소급 부여(발생일당 최대 그 발생일 일수만큼)
        for (AccrualEvent ev : events) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal give = ev.days.min(remaining);
            if (give.signum() <= 0) {
                continue;
            }
            String availTo = addMonthsYyyymmdd(ev.accrualYmd, validityMonths);
            String grantId = insertHireAdjustGrant(cmpnyCd, userCd, ev.leaveCd, ev.grantType, give, policySeq,
                    today, ev.accrualYmd, availTo, HIRE_BACKFILL_GRANT_REASON, operatorUserCd, histId);
            addedTotal = addedTotal.add(give);
            addedCount++;
            remaining = remaining.subtract(give);
            snapshot.add(addSnapshotRow("ADD_BACKFILL", grantId, ev.grantType, give, ev.accrualYmd, availTo));
        }

        // 3) 미부여 발생일 소진 후 잔여 일수는 오늘 폴백(초과 부여). GRANT_TYPE은 오늘 시점 산정근속 기준.
        if (remaining.signum() > 0) {
            String grantType = resolveGrantTypeAt(cmpnyCd, userCd, newHireDate, today);
            String leaveCd = leaveCdForGrantType(grantType);
            String availTo = addMonthsYyyymmdd(today, validityMonths);
            String grantId = insertHireAdjustGrant(cmpnyCd, userCd, leaveCd, grantType, remaining, policySeq,
                    today, today, availTo, HIRE_OVERAGE_GRANT_REASON, operatorUserCd, histId);
            addedTotal = addedTotal.add(remaining);
            addedCount++;
            snapshot.add(addSnapshotRow("ADD_OVERAGE", grantId, grantType, remaining, today, availTo));
            remaining = BigDecimal.ZERO;
        }

        log.info("입사일 변경 추가 부여 완료. cmpnyCd={}, userCd={}, 차액={}, 추가={}건/{}일, histId={}",
                cmpnyCd, userCd, diff.stripTrailingZeros().toPlainString(), addedCount,
                addedTotal.stripTrailingZeros().toPlainString(), histId);

        return HireDateAdjustResultVO.builder()
                .oldGrantTotal(currentTotal).newGrantTotal(newTotal).diff(diff.setScale(1))
                .addedDays(addedTotal.setScale(1, RoundingMode.HALF_UP))
                .withdrawnDays(BigDecimal.ZERO.setScale(1))
                .recallableDays(recallable)
                .canceledGrantCount(0).reducedGrantCount(0).addedGrantCount(addedCount)
                .affectedSnapshotJson(serializeSnapshot(snapshot))
                .build();
    }

    /**
     * 차액 &lt; 0 회수 (prafta-032 D3 검증 + D5 차감). 회수가능량 초과면 차단(예외). 회수 사유 필수.
     * 대상 ACTIVE 법정 부여를 (소멸임박→최근부여→GRANT_ID큰순)으로 순회하며 회수량을 차감한다.
     */
    private HireDateAdjustResultVO applyHireChangeRecall(String cmpnyCd, String userCd, BigDecimal currentTotal,
                                                         BigDecimal newTotal, BigDecimal diff, BigDecimal recallAmount,
                                                         BigDecimal recallable, String withdrawReason, String histId,
                                                         String operatorUserCd) {
        // D3 검증 1: 회수 사유 필수
        String safeReason = blankToNull(withdrawReason);
        if (safeReason == null) {
            throw new ApiException(UserErrorCode.USER_400_031);
        }
        // D3 검증 2: 회수 시도량(현재−목표)이 회수 가능량(ACTIVE 법정 잔여)을 초과하면 차단(저장 불가)
        if (recallAmount.compareTo(recallable) > 0) {
            String msg = "회수 가능한 연차는 " + dayLabel(recallable) + "입니다. (잔여 " + dayLabel(recallable) + ")"
                    + " 이미 사용했거나 사용 예정인 연차는 회수할 수 없습니다.";
            log.warn("입사일 변경 회수 차단 — 회수가능 초과. cmpnyCd={}, userCd={}, 시도={}, 가능={}",
                    cmpnyCd, userCd, recallAmount.stripTrailingZeros().toPlainString(),
                    recallable.stripTrailingZeros().toPlainString());
            throw new ApiException(UserErrorCode.USER_400_030, msg);
        }

        // D5: 회수 우선순위 정렬된 ACTIVE 법정 부여를 순회하며 차감
        List<LeaveGrantRecallRowVO> targets = leaveGrantEngineMapper.selectActiveStatutoryGrantsForRecall(cmpnyCd, userCd);
        BigDecimal remaining = recallAmount;
        BigDecimal withdrawnTotal = BigDecimal.ZERO;
        int canceledCount = 0;
        int reducedCount = 0;
        List<Map<String, Object>> snapshot = new ArrayList<>();
        // GRANT_REASON varchar(500) 상한 보호: 태깅 접두 + 회수 사유가 500자를 넘으면 절단.
        String recallTagRaw = "[입사일변경 회수 " + histId + "] " + safeReason;
        String recallTag = (recallTagRaw.length() > 500) ? recallTagRaw.substring(0, 500) : recallTagRaw;

        for (LeaveGrantRecallRowVO g : targets) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal grantDays = nvlZero(g.getGrantDays());
            BigDecimal usedDays = nvlZero(g.getUsedDays());
            BigDecimal rowRemain = grantDays.subtract(usedDays); // 행 잔여
            if (rowRemain.signum() <= 0) {
                continue; // 잔여 없는 행(소진)은 회수 대상 아님
            }
            BigDecimal take = rowRemain.min(remaining); // 이 행에서 회수할 양

            boolean fullRowRecall = take.compareTo(rowRemain) == 0; // 행 잔여 전체 회수
            if (fullRowRecall && usedDays.signum() == 0) {
                // 잔여 전체 회수 + USED_DAYS=0 → STATUS='CANCELED'(prafta-031 패턴)
                int updated = leaveGrantEngineMapper.cancelStatutoryGrantForHireChange(
                        cmpnyCd, g.getGrantId(), safeReason, recallTag, operatorUserCd);
                if (updated == 1) {
                    canceledCount++;
                    withdrawnTotal = withdrawnTotal.add(take);
                    remaining = remaining.subtract(take);
                    snapshot.add(recallSnapshotRow("CANCELED", g.getGrantId(), g.getGrantType(), take, grantDays,
                            usedDays, g.getAvailToDate()));
                }
                // updated==0(경합)이면 다음 행으로 — 트랜잭션 일관성은 최종 합계 검증에서 방어.
            } else {
                // 부분 회수이거나 USED_DAYS>0 → GRANT_DAYS 직접 차감(USED_DAYS·FK 불변)
                int updated = leaveGrantEngineMapper.reduceStatutoryGrantDaysForHireChange(
                        cmpnyCd, g.getGrantId(), take, recallTag, operatorUserCd);
                if (updated == 1) {
                    reducedCount++;
                    withdrawnTotal = withdrawnTotal.add(take);
                    remaining = remaining.subtract(take);
                    snapshot.add(recallSnapshotRow("REDUCED", g.getGrantId(), g.getGrantType(), take, grantDays,
                            usedDays, g.getAvailToDate()));
                }
            }
        }

        // 회수 부족(경합 등으로 목표 미달) — 데이터 무결성 위해 롤백.
        if (remaining.signum() > 0) {
            log.warn("입사일 변경 회수 미완(잔여 {}) — 경합 추정, 롤백. cmpnyCd={}, userCd={}",
                    remaining.stripTrailingZeros().toPlainString(), cmpnyCd, userCd);
            throw new ApiException(AttdErrorCode.ATTD_409_071);
        }

        log.info("입사일 변경 회수 완료. cmpnyCd={}, userCd={}, 차액={}, 회수={}일(취소 {}건/차감 {}건), histId={}",
                cmpnyCd, userCd, diff.stripTrailingZeros().toPlainString(),
                withdrawnTotal.stripTrailingZeros().toPlainString(), canceledCount, reducedCount, histId);

        return HireDateAdjustResultVO.builder()
                .oldGrantTotal(currentTotal).newGrantTotal(newTotal).diff(diff.setScale(1))
                .addedDays(BigDecimal.ZERO.setScale(1))
                .withdrawnDays(withdrawnTotal.setScale(1, RoundingMode.HALF_UP))
                .recallableDays(recallable)
                .canceledGrantCount(canceledCount).reducedGrantCount(reducedCount).addedGrantCount(0)
                .affectedSnapshotJson(serializeSnapshot(snapshot))
                .build();
    }

    /**
     * 새 입사일 기준 "미부여 발생일(오늘 이전, 소멸 전)" 빠른순 목록 (prafta-032 D4, read-only 산정).
     *
     * <p>월차(per-월, D2-B 만1년 일괄소멸)·본연차(입사 기념일)·근속가산을 발생일 기준으로 만든 뒤,
     * (a) 발생일이 오늘 이전이고 (b) 유효기간(AXIS6) 내(소멸 전)이며 (c) <b>기존 표준 부여</b>(standard 멱등키,
     * 접미사 빈값 + 레거시 _HIRE, live)로 아직 커버되지 않은 발생일만 남겨 발생일 오름차순으로 정렬한다.
     * "미부여 발생일 비교"의 기준은 정책 기준 부여가 만든 표준 부여이므로 detection 은 표준 키({@code ""})로 한다.
     * 실제 추가 INSERT 는 충돌 회피를 위해 _HD{histId}+GRANT_ID 전용 멱등키를 쓴다(insertHireAdjustGrant).
     * {@code computeMonthlyPeriods}와 입사 기념일 산식을 재사용한다(prafta-030 D2-B 월차 일괄소멸 유지).
     */
    private List<AccrualEvent> computeUngrantedAccrualEvents(String cmpnyCd, String userCd, String hireDate,
                                                             LeavePolicyVO policy, int validityMonths, String today) {
        // 미부여 판정 기준 = 기존 표준 부여(접미사 빈값) live 커버 여부. (추가 INSERT 키와 다름 — 의도된 분리)
        final String detectSuffix = "";
        List<AccrualEvent> out = new ArrayList<>();
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = parseYyyymmdd(today);
        if (hire == null || todayDate == null || hire.isAfter(todayDate)) {
            return out;
        }

        GrantContext ctx = new GrantContext(policy, null, today, null);
        // (1) 월차(per-월) — computeMonthlyPeriods가 D2-B 만1년 일괄소멸/더블딥 게이트 + 표준키 미부여 판정을 적용.
        for (PeriodComponent mp : computeMonthlyPeriods(cmpnyCd, userCd, hireDate, ctx, detectSuffix)) {
            if (mp.newInsert && mp.days.signum() > 0) {
                out.add(new AccrualEvent(mp.availFromDate, mp.leaveCd, mp.grantType, mp.days));
            }
        }

        // (2) 본연차 + 근속가산 — 입사 기념일(입사+12*y개월) 발생일 기준. 오늘 이전 + 유효기간 내 + 표준키 미부여만.
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
        int actualYears = actualMonths / 12;
        int creditYears = Math.max(0, leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd)) / 12;
        for (int y = 1; y <= actualYears; y++) {
            LocalDate accrual = hire.plusMonths(12L * y);
            if (accrual.isAfter(todayDate)) {
                break;
            }
            String accrualYmd = accrual.format(DateTimeFormatter.BASIC_ISO_DATE);
            String availTo = addMonthsYyyymmdd(accrualYmd, validityMonths);
            if (availTo.compareTo(today) < 0) {
                continue; // 소멸분 제외
            }
            String label = String.valueOf(accrual.getYear());
            if (!alreadyGranted(cmpnyCd, userCd, label, GRANT_TYPE_ANNUAL, detectSuffix)) {
                out.add(new AccrualEvent(accrualYmd, LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL,
                        BigDecimal.valueOf(BASE_ANNUAL_DAYS)));
            }
            int bonus = tenureBonusDays(policy, y + creditYears);
            if (bonus > 0 && !alreadyGranted(cmpnyCd, userCd, label, GRANT_TYPE_TENURE, detectSuffix)) {
                out.add(new AccrualEvent(accrualYmd, LEAVE_CD_TENURE, GRANT_TYPE_TENURE, BigDecimal.valueOf(bonus)));
            }
        }

        // 발생일 오름차순(빠른 순). 동일 발생일이면 월차→본연차→가산 순으로 안정 정렬되도록 grantType 보조키.
        out.sort((a, b) -> {
            int c = a.accrualYmd.compareTo(b.accrualYmd);
            return (c != 0) ? c : a.grantType.compareTo(b.grantType);
        });
        return out;
    }

    /**
     * 추가 부여 GRANT_TYPE 자동판단 (prafta-032 D4): 발생일(grantDate) 시점 산정근속(경력 인정 포함).
     * &lt;12m → STATUTORY_MONTHLY / 12~36m → STATUTORY_ANNUAL / &ge;36m → STATUTORY_TENURE_BONUS.
     */
    private String resolveGrantTypeAt(String cmpnyCd, String userCd, String hireDate, String atYmd) {
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate at = parseYyyymmdd(atYmd);
        if (hire == null || at == null) {
            return GRANT_TYPE_ANNUAL;
        }
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, at));
        int creditMonths = Math.max(0, leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd));
        int creditedMonths = actualMonths + creditMonths;
        if (creditedMonths < 12) {
            return GRANT_TYPE_MONTHLY;
        }
        if (creditedMonths < 36) {
            return GRANT_TYPE_ANNUAL;
        }
        return GRANT_TYPE_TENURE;
    }

    /** GRANT_TYPE → 시스템 연차 코드(LEAVE_CD) 매핑. */
    private String leaveCdForGrantType(String grantType) {
        if (GRANT_TYPE_MONTHLY.equals(grantType)) {
            return LEAVE_CD_MONTHLY;
        }
        if (GRANT_TYPE_TENURE.equals(grantType)) {
            return LEAVE_CD_TENURE;
        }
        return LEAVE_CD_ANNUAL;
    }

    /**
     * 입사일 변경 추가 부여 1건 INSERT (prafta-032 D4). GRANT_BY_TYPE='01'(정책 기반 산정).
     * 멱등키 전용 네임스페이스 = {@code _HD{histId}}(prafta-030 _BF / prafta-029 _R 과 구분). 동일 변경 트랜잭션 내
     * 다건(소급 + 오늘 폴백)이 UNIQUE(CMPNY_CD,IDEMPOTENCY_KEY) 충돌하지 않도록 발생일·종류 + GRANT_ID 꼬리를 붙인다.
     *
     * <p><b>GRANT_DATE 정합(prafta-032 #3 정정):</b> GRANT_DATE 는 <b>부여 실행일(오늘)</b>로 둔다. 이는 기존 정책
     * 기준 부여({@link #grantComponent})·소급 백필({@link #computeBackfillPeriods})이 모두 GRANT_DATE=오늘 / 발생일은
     * AVAIL_FROM_DATE 에 담는 컨벤션과 일관시키기 위함이다. "해당 미부여 발생일"은 {@code availFrom} 인자(=AVAIL_FROM_DATE)로
     * 전달되며, 소멸일(AVAIL_TO_DATE)은 발생일+AXIS6 로 산정된다. (결정문서 D4의 "GRANT_DATE=발생일" 표기는 본 컨벤션에
     * 맞춰 정정됨 — 발생일은 GRANT_DATE 가 아니라 AVAIL_FROM_DATE 에 기록한다.)
     *
     * @return 채번된 GRANT_ID
     */
    private String insertHireAdjustGrant(String cmpnyCd, String userCd, String leaveCd, String grantType,
                                         BigDecimal days, Long policySeq, String today, String availFrom,
                                         String availTo, String grantReason, String operatorUserCd, String histId) {
        String grantId = leaveDashboardMapper.selectNextGrantId(cmpnyCd);
        // 멱등키: {userCd}_{발생일}_{grantType}_HD{histId}_{grantId} — _HD{histId} 입사일변경 전용 네임스페이스.
        //   발생일로 다건 구분 + GRANT_ID 꼬리로 동일 변경 트랜잭션 내 UNIQUE 충돌 방지.
        String idempotencyKey = userCd + "_" + availFrom + "_" + grantType
                + HIRE_ADJUST_KEY_PREFIX + histId + "_" + grantId;

        LeaveGrantInsertVO vo = new LeaveGrantInsertVO();
        vo.setGrantId(grantId);
        vo.setCmpnyCd(cmpnyCd);
        vo.setUserCd(userCd);
        vo.setLeaveCd(leaveCd);
        vo.setGrantType(grantType);
        vo.setGrantDays(days.setScale(1, RoundingMode.HALF_UP));
        vo.setUsedDays(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        vo.setGrantReason(grantReason);
        vo.setGrantByType(GRANT_BY_TYPE_AUTO); // '01' — 정책 기반 산정(prafta-031 회수 대상 MANUAL_%+'02'에 미해당)
        vo.setPolicySeq(policySeq);
        vo.setGrantDate(today);
        vo.setAvailFromDate(availFrom);
        vo.setAvailToDate(availTo);
        vo.setIdempotencyKey(idempotencyKey);
        vo.setStatus(STATUS_ACTIVE);
        vo.setInsertNo(operatorUserCd);

        leaveDashboardMapper.insertManualGrant(vo);
        return grantId;
    }

    private Map<String, Object> addSnapshotRow(String action, String grantId, String grantType, BigDecimal days,
                                               String availFrom, String availTo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("action", action);
        m.put("grantId", grantId);
        m.put("grantType", grantType);
        m.put("days", days.stripTrailingZeros().toPlainString());
        m.put("availFrom", availFrom);
        m.put("availTo", availTo);
        return m;
    }

    private Map<String, Object> recallSnapshotRow(String action, String grantId, String grantType, BigDecimal take,
                                                  BigDecimal beforeGrantDays, BigDecimal usedDays, String availTo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("action", action);
        m.put("grantId", grantId);
        m.put("grantType", grantType);
        m.put("recall", take.stripTrailingZeros().toPlainString());
        m.put("beforeGrantDays", beforeGrantDays.stripTrailingZeros().toPlainString());
        m.put("afterGrantDays", "CANCELED".equals(action)
                ? beforeGrantDays.stripTrailingZeros().toPlainString()
                : beforeGrantDays.subtract(take).stripTrailingZeros().toPlainString());
        m.put("usedDays", usedDays.stripTrailingZeros().toPlainString());
        m.put("availTo", availTo);
        return m;
    }

    private String serializeSnapshot(List<Map<String, Object>> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("입사일 변경 조정 스냅샷 직렬화 실패(무시). cnt={}", snapshot.size());
            return null;
        }
    }

    /** 추가 부여 발생일 후보 1건(발생일/연차코드/부여분류/일수). prafta-032 D4 내부 운반체. */
    private static final class AccrualEvent {
        private final String accrualYmd;
        private final String leaveCd;
        private final String grantType;
        private final BigDecimal days;

        private AccrualEvent(String accrualYmd, String leaveCd, String grantType, BigDecimal days) {
            this.accrualYmd = accrualYmd;
            this.leaveCd = leaveCd;
            this.grantType = grantType;
            this.days = days;
        }
    }

    // ============================================================
    // 공유 진입 가드 / 대상 해석 / 부여 계획 빌더 (apply ↔ preview 공유)
    // ============================================================

    /**
     * 부여/프리뷰 공통 진입 가드(권한·입력·시스템 연차종류) + 부여 공통값(정책/유효기간/오늘) 산정.
     * 조회만 수행하며 DB 쓰기는 없다. apply·preview 모두 동일 기준을 적용한다(§8.5.7).
     */
    private GrantContext prepareGrantContext(String cmpnyCd, List<String> userCds, String authCd) {
        requireCmpnyCd(cmpnyCd);

        // 권한 가드 (정책서 §8.5.7) — 진입부 강제 (수동 부여와 동일)
        ensureManager(cmpnyCd, authCd, "정책 기준 연차 부여");

        if (userCds == null || userCds.isEmpty()) {
            throw new ApiException(AttdErrorCode.ATTD_400_033);
        }
        // 대량 부여/장시간 트랜잭션 방지 — 1회 대상 인원 상한
        if (userCds.size() > MAX_GRANT_USER_COUNT) {
            log.warn("정책 기준 부여 - 대상 인원 상한 초과. cmpnyCd={}, count={}", cmpnyCd, userCds.size());
            throw new ApiException(AttdErrorCode.ATTD_400_060);
        }

        // 부여에 쓸 시스템 연차 종류가 회사에 설정돼 있는지 진입부 1회 검증
        ensureLeaveTypeExists(cmpnyCd, LEAVE_CD_ANNUAL);
        ensureLeaveTypeExists(cmpnyCd, LEAVE_CD_MONTHLY);
        ensureLeaveTypeExists(cmpnyCd, LEAVE_CD_TENURE);

        // 활성 정책(AXIS1/AXIS2/AXIS3/근속가산/유효기간) + 부여 공통값
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
        Long policySeq = (policy == null) ? null : policy.getPolicySeq();
        int validityMonths = resolveValidityMonths(cmpnyCd);
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String availToDate = addMonthsYyyymmdd(today, validityMonths);

        return new GrantContext(policy, policySeq, today, availToDate);
    }

    /**
     * 대상 직원 검증 + 입사일 조회. 회사 활성 사용자(스코프 격리)가 아니면 ATTD_404_020.
     *
     * @param rejectIfNoHire true면 입사일 미입력자가 1명이라도 있으면 전건 거부(apply 동작 유지),
     *                       false면 입사일을 null로 담아 반환(preview는 행 note로 안내)
     * @return [userCd, hireDate(미입력이면 null)] 목록
     */
    private List<String[]> resolveTargets(String cmpnyCd, List<String> userCds, boolean rejectIfNoHire) {
        List<String[]> targets = new ArrayList<>(userCds.size());
        for (String raw : userCds) {
            String tu = blankToNull(raw);
            if (tu == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_033);
            }
            if (leaveDashboardMapper.countActiveUser(cmpnyCd, tu) < 1) {
                log.warn("정책 기준 부여 - 대상 직원 스코프 밖/미존재. cmpnyCd={}, userCd={}", cmpnyCd, tu);
                throw new ApiException(AttdErrorCode.ATTD_404_020);
            }
            String hireDate = leaveDashboardMapper.selectUserHireDate(cmpnyCd, tu);
            if (!isValidYyyymmdd(hireDate)) {
                if (rejectIfNoHire) {
                    log.warn("정책 기준 부여 - 입사일 미입력. cmpnyCd={}, userCd={}", cmpnyCd, tu);
                    throw new ApiException(AttdErrorCode.ATTD_400_058);
                }
                targets.add(new String[] {tu, null});
                continue;
            }
            targets.add(new String[] {tu, hireDate});
        }
        return targets;
    }

    /**
     * 직원 1명의 부여 계획(read-only)을 산정한다. <b>DB 쓰기 없음</b> — 조회만 수행한다.
     *
     * <p>apply(hireDateGrant)와 preview가 공유하는 단일 진실원이다. 정책 기준 entitlement 산정, 멱등키 산출 +
     * {@code alreadyGranted}(live-only dual-read)로 실제 신규부여 여부를 판별하되, 실제 부여/마킹은 호출부가 담당한다.
     *
     * <p>prafta-032 (D1/D6/009): the handling-type auto-calc (KEEP_* / RESET_ALL), HANDLING_TYPE lookup,
     * RESET round key (_R+histId) and cancel-target lookup were removed. The idempotency suffix is always
     * the standard key (empty string); "policy-based grant" performs new grants only.
     *
     * <p>The hire date is assumed valid (YYYYMMDD); the no-hire-date case is branched in the caller.
     */
    private UserGrantPlan buildUserPlan(String cmpnyCd, String userCd, String hireDate, GrantContext ctx) {
        // (a) 정책 기준 entitlement 산정 (AXIS1 분기, 경력 인정 개월 가산)
        int creditMonths = leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd);
        Entitlement ent = resolveEntitlement(ctx.policy, hireDate, creditMonths, cmpnyCd, userCd);

        // (b) 멱등키 접미사 = 항상 표준키(빈 문자열). prafta-032로 RESET 회차키(_R{histId}) 폐기.
        String keySuffix = "";

        // (c) 컴포넌트별 멱등키 산출 + 실제 신규부여 여부(alreadyGranted: live-only dual-read == false) 판별
        // C 라벨 전환가드(prafta-028): 당기부여 라벨을 '기념일연도'로 바꾸면서, prafta-023까지 '달력연도'로
        //   부여된 기존 당기분도 인식하도록 달력연도 키를 dual-read로 함께 확인한다(HIRE_DATE 한정 —
        //   FISCAL_YEAR는 회계연도 키로 일관되어 전환가드 불필요). 재키잉 마이그레이션 없이 이중부여 차단.
        boolean hireDateAxis = (ctx.policy == null)
                || AXIS1_HIRE_DATE.equals(nvl(ctx.policy.getAxis1GrantBase(), AXIS1_HIRE_DATE));
        String legacyCalendarLabel = String.valueOf(LocalDate.now().getYear());
        List<PlanComponent> planComponents = new ArrayList<>(ent.components.size());
        for (GrantComponent gc : ent.components) {
            // 정식 키 + 레거시(_HIRE) dual-read (prafta-023 A) + 달력연도 전환가드(prafta-028 C)
            boolean newInsert = gc.days.signum() > 0
                    && !alreadyGranted(cmpnyCd, userCd, ent.yearLabel, gc.grantType, keySuffix)
                    && !(hireDateAxis
                            && !legacyCalendarLabel.equals(ent.yearLabel)
                            && alreadyGranted(cmpnyCd, userCd, legacyCalendarLabel, gc.grantType, keySuffix));
            planComponents.add(new PlanComponent(gc.leaveCd, gc.grantType, gc.days, newInsert));
        }

        String note = buildPlanNote(hireDate, ent, planComponents);

        return new UserGrantPlan(keySuffix, ent.yearLabel, ent.availFromDate(ctx.today), planComponents, note);
    }

    /** 프리뷰/안내용 비고 산출. 부여 대상 없음/멱등 전부 skip/입사일 미래 등을 사람이 읽을 사유로. */
    private String buildPlanNote(String hireDate, Entitlement ent, List<PlanComponent> planComponents) {
        LocalDate hire = parseYyyymmdd(hireDate);
        if (hire != null && hire.isAfter(LocalDate.now())) {
            return "입사일 미래 — 부여 대상 아님";
        }
        if (ent.components.isEmpty()) {
            return "부여 대상 없음(근속 0개월)";
        }
        boolean anyNew = planComponents.stream().anyMatch(pc -> pc.newInsert);
        if (!anyNew) {
            return "이미 부여됨(멱등 — 변경 없음)";
        }
        return null;
    }

    // ============================================================
    // 정책 기준 entitlement 산정 (작업 B — AXIS1 분기)
    // ============================================================

    /**
     * 직원 1명의 정책 기준 부여 컴포넌트(월차/본연차/근속가산)와 연도식별자(멱등키용)를 산정한다.
     *
     * <p>정책서 §8.5.2(7-axis)·§8.5.3(교차 매트릭스)·§8.5.4(1년 미만 월차).
     * <ul>
     *   <li>월차(STATUTORY_MONTHLY)는 §8.5.4 법정 의무 — AXIS 조합과 무관하게 실제 근속 12개월 미만이면
     *       {@code min(근속개월, 11)}일 부여. 경력 인정 개월은 본연차/근속가산에는 가산하되 월차 판정에는
     *       실제 근속(creditMonths 미포함)을 쓴다(1년 미만 법정 월차는 "실제 재직 기간" 기준).</li>
     *   <li>AXIS1=HIRE_DATE: 근속개월(creditMonths 가산) 기준. 12개월 이상이면 본연차15 + 근속가산.
     *       연도식별자 = 현재 달력연도 YYYY.</li>
     *   <li>AXIS1=FISCAL_YEAR: AXIS2(시작 MM/DD)로 현재 회계연도 라벨/시작일 산정 후, 입사 이후 회계연도
     *       시작을 몇 번 넘겼는지(crossedFiscalStarts)로 본연차/근속가산 산정. 연도식별자 = fiscalYear 라벨.</li>
     *   <li>AXIS3=PRORATE는 prafta-023로 분리 → NEXT_YEAR_BULK로 폴백(INFO 로그). MONTHLY_ONLY와
     *       NEXT_YEAR_BULK는 022에서 "본연차는 첫 회계연도 시작 이후부터" 동일 처리(부분기간 비례 없음).</li>
     *   <li>정책 없음(policy==null): HIRE_DATE 기준 법정 기본으로 폴백.</li>
     * </ul>
     */
    private Entitlement resolveEntitlement(LeavePolicyVO policy, String hireDate, int creditMonths,
                                           String cmpnyCd, String userCd) {
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = LocalDate.now();

        // 입사일 미래면 부여 대상 아님(컴포넌트 없음). 연도식별자는 달력연도로 둔다(미사용).
        if (hire == null || hire.isAfter(todayDate)) {
            return Entitlement.empty(String.valueOf(todayDate.getYear()));
        }

        // 실제 근속 개월(경력 인정 제외) — 1년 미만 법정 월차 판정용
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
        // 부여 산정용 근속 개월(경력 인정 가산) — 본연차/근속가산용
        int creditedMonths = actualMonths + Math.max(0, creditMonths);

        String axis1 = (policy == null) ? AXIS1_HIRE_DATE : nvl(policy.getAxis1GrantBase(), AXIS1_HIRE_DATE);

        if (AXIS1_FISCAL_YEAR.equals(axis1)) {
            return resolveFiscalEntitlement(policy, hire, todayDate, actualMonths, creditedMonths, cmpnyCd, userCd);
        }
        // 기본/폴백: HIRE_DATE
        return resolveHireDateEntitlement(policy, hire, actualMonths, creditedMonths);
    }

    /** AXIS1=HIRE_DATE: 근속개월 기준(현행 로직 유지). 연도식별자 = 달력연도 YYYY. */
    private Entitlement resolveHireDateEntitlement(LeavePolicyVO policy, LocalDate hire,
                                                   int actualMonths, int creditedMonths) {
        // C 라벨 통일(prafta-028): 당기부여 라벨을 '가장 최근 도래한 입사 기념일 연도'로 둔다(기존엔 달력연도).
        //   백필(computeBackfillPeriods)이 기념일연도(accrual.getYear())로 키를 매기므로, 당기부여도 같은
        //   기준을 써야 동일 근속연차가 '달력연도' 키와 '기념일연도' 키로 갈라져 이중부여되지 않는다.
        String yearLabel = latestAnniversaryYearLabel(hire, LocalDate.now());
        List<GrantComponent> comps = new ArrayList<>();

        // 1년 미만 법정 월차(§8.5.4)는 per-월 누적(computeMonthlyPeriods, prafta-023 #1)으로 분리 부여 → 여기선 본연차/근속만 산정.
        // 12개월 이상: 본연차 + 근속가산 (경력 인정 가산 근속개월 기준)
        if (creditedMonths >= 12) {
            comps.add(new GrantComponent(LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL, BigDecimal.valueOf(BASE_ANNUAL_DAYS)));
            int bonus = tenureBonusDays(policy, creditedMonths / 12);
            if (bonus > 0) {
                comps.add(new GrantComponent(LEAVE_CD_TENURE, GRANT_TYPE_TENURE, BigDecimal.valueOf(bonus)));
            }
        }
        return new Entitlement(comps, yearLabel);
    }

    /**
     * AXIS1=FISCAL_YEAR: AXIS2(시작 MM/DD) 기준 현재 회계연도 라벨/시작일 산정 후,
     * 입사 이후 회계연도 시작을 몇 번 넘겼는지(crossedFiscalStarts)로 본연차/근속가산 산정.
     * 연도식별자 = fiscalYear 라벨(새 회계연도엔 새 키 → 다음 회계연도 재부여 가능).
     */
    private Entitlement resolveFiscalEntitlement(LeavePolicyVO policy, LocalDate hire, LocalDate today,
                                                 int actualMonths, int creditedMonths,
                                                 String cmpnyCd, String userCd) {
        int startMm = parseMm(policy == null ? null : policy.getAxis2FiscalStartMm(), 1);
        int startDd = parseDd(policy == null ? null : policy.getAxis2FiscalStartDd(), 1);

        // 현재 회계연도 시작일 = 오늘 이전(또는 오늘)의 가장 최근 (startMm/startDd)
        LocalDate currentFiscalStart = currentFiscalStart(today, startMm, startDd);
        String yearLabel = String.valueOf(currentFiscalStart.getYear());

        // 입사 이후 오늘까지 회계연도 시작일을 넘긴 횟수(=입사 후 도래한 회계연도 시작 개수)
        int crossedFiscalStarts = countFiscalStartsCrossed(hire, today, startMm, startDd);

        List<GrantComponent> comps = new ArrayList<>();
        int creditedYears = creditedMonths / 12;
        String axis3 = (policy == null) ? null : policy.getAxis3FirstYearMethod();

        // 1년 미만 법정 월차(§8.5.4, AXIS 무관)는 per-월 누적(computeMonthlyPeriods, prafta-023 #1)으로 분리 부여.
        // 표준 모델(고용노동부 기준, prafta-029): crossed = 입사 이후 도래한 회계연도 시작 횟수.
        //   crossed==0 → 본연차 없음(월차만). crossed==1 → AXIS3로 분기(PRORATE=비례 / 그 외=일괄 15).
        //   crossed>=2 → 본연차 15 + 근속가산. PRORATE와 NEXT_YEAR_BULK는 crossed==1에서만 다르다.
        // ⚠️ 유효기간 12개월 고정 전제로 과거 회계연도 백필은 항상 만료(availTo<today) 제외됨; 유효기간 연장 시 첫 회계연도분 비례 일관화 필요.

        if (crossedFiscalStarts >= 2) {
            // 회계연도 시작 2회 이상 도래: 본연차 15 + 근속가산 (PRORATE/NEXT_YEAR_BULK 동일)
            comps.add(new GrantComponent(LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL, BigDecimal.valueOf(BASE_ANNUAL_DAYS)));
            // 경력 인정 개월이 있으면 근속 연차에 가산(개월→연차). 회계연도 도래 횟수와 경력 인정 연차 중 큰 값을 근속연차로.
            int tenureYear = Math.max(crossedFiscalStarts, creditedYears);
            int bonus = tenureBonusDays(policy, tenureYear);
            if (bonus > 0) {
                comps.add(new GrantComponent(LEAVE_CD_TENURE, GRANT_TYPE_TENURE, BigDecimal.valueOf(bonus)));
            }
        } else if (crossedFiscalStarts == 1) {
            if (AXIS3_PRORATE.equals(axis3)) {
                // crossed==1 + AXIS3=PRORATE: 전년 부분기 비례 본연차 = (입사~현재 회계연도 시작 일수 ÷ 365) × 본연차, AXIS4 반올림.
                //   근속가산은 첫 회계연도엔 없음. 월차(§8.5.4)는 위에서 별도 부여(법정 의무).
                BigDecimal prorated = computeProratedAnnualDays(hire, currentFiscalStart, policy == null ? null : policy.getAxis4ProrateRounding());
                if (prorated.signum() > 0) {
                    comps.add(new GrantComponent(LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL, prorated));
                }
            } else {
                // crossed==1 + NEXT_YEAR_BULK(표준) / MONTHLY_ONLY(잔존, 폴백) / null:
                //   본연차 15 일괄 부여. 근속가산은 crossed 기준이라 보통 0(tenureYear=max(1, creditedYears)).
                if (policy != null && AXIS3_MONTHLY_ONLY.equals(axis3)) {
                    // 작업 3(백워드호환): DB에 잔존하는 비표준 조합(FISCAL+MONTHLY_ONLY)은 NEXT_YEAR_BULK로 폴백.
                    //   FE 정규화 + 신규저장 차단으로 자연 교정되며 DB 마이그레이션은 하지 않는다.
                    log.info("비표준 조합(FISCAL_YEAR+MONTHLY_ONLY)이라 NEXT_YEAR_BULK로 처리. cmpnyCd={}, userCd={}", cmpnyCd, userCd);
                }
                comps.add(new GrantComponent(LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL, BigDecimal.valueOf(BASE_ANNUAL_DAYS)));
                int tenureYear = Math.max(1, creditedYears);
                int bonus = tenureBonusDays(policy, tenureYear);
                if (bonus > 0) {
                    comps.add(new GrantComponent(LEAVE_CD_TENURE, GRANT_TYPE_TENURE, BigDecimal.valueOf(bonus)));
                }
            }
        }
        // crossed==0: 본연차 미부여(월차만) — 표준 모델
        return new Entitlement(comps, yearLabel);
    }

    /**
     * AXIS3=PRORATE 첫 회계연도 비례 본연차 일수 (prafta-029 표준 모델). FISCAL_YEAR crossed==1 전용.
     * 비례 = (입사가 속한 첫 부분기 일수 ÷ 365) × 본연차일수, AXIS4 반올림.
     * 부분기 일수 = DAYS.between(입사일, currentFiscalStart) (= 입사~도래한 회계연도 시작까지의 일수).
     * 0 이하/365 초과(비정상)는 0 또는 1년분 상한으로 방어.
     * 예: 입사 2025-07-21, currentFiscalStart 2026-01-01 → 164일 → 15×164/365=6.74 → CEIL 7.
     */
    // package-private: prafta-029 표준 모델 단위테스트(LeaveGrantEngineProrationTest)에서 직접 검증
    BigDecimal computeProratedAnnualDays(LocalDate hire, LocalDate currentFiscalStart, String axis4) {
        long partialDays = ChronoUnit.DAYS.between(hire, currentFiscalStart);
        if (partialDays <= 0) {
            return BigDecimal.ZERO;
        }
        if (partialDays > 365) {
            partialDays = 365; // 입사~회계연도 시작이 1년 초과인 비정상치 방어(상한 1년분)
        }
        double raw = (partialDays / 365.0) * BASE_ANNUAL_DAYS;
        return applyAxis4Rounding(raw, axis4);
    }

    /**
     * AXIS4 반올림 (SYS038). 비례부여 일수에 적용. 결과는 BigDecimal(HALF_DAY는 0.5 단위 가능, prafta-023 #3).
     * CEIL 올림 / ROUND 반올림 / FLOOR 내림 / HALF_DAY 0.5일 절사(0.5 배수로 내림).
     * CEIL 및 그 외/널은 올림(§8.5.3: AXIS3≠PRORATE면 CEIL 기본 — 단 본 메서드는 PRORATE 분기에서만 호출됨).
     */
    // package-private: prafta-023 D/#3 단위테스트에서 직접 검증
    BigDecimal applyAxis4Rounding(double raw, String axis4) {
        if (AXIS4_FLOOR.equals(axis4)) {
            return BigDecimal.valueOf(Math.floor(raw));
        }
        if (AXIS4_ROUND.equals(axis4)) {
            return BigDecimal.valueOf(Math.round(raw));
        }
        if (AXIS4_HALF_DAY.equals(axis4)) {
            // 0.5일 단위 절사(내림): raw 를 0.5 배수로 내림. 예) 3.78 → 3.5, 7.2 → 7.0
            return BigDecimal.valueOf(Math.floor(raw * 2.0) / 2.0);
        }
        // CEIL 및 그 외/널 = 올림
        return BigDecimal.valueOf(Math.ceil(raw));
    }

    // ============================================================
    // 입사일 변경 차액 보전 (prafta-030 BE-1 / D1) — KEEP_AND_BACKFILL 전용
    // ============================================================

    /**
     * 옵션1(KEEP_AND_BACKFILL) 차액 보전 산정 (read-only — DB 쓰기 없음). prafta-030 BE-1 / 결정문서 D1.
     *
     * <p><b>차액 = (새 기준 부여누적) − (기존 부여누적)</b>, 본연차/가산만 대상(월차 제외).
     * <ul>
     *   <li><b>새 기준 부여누적</b>: 새 입사일 기준 "오늘 시점 유효(소멸 제외)해야 할" 본연차+가산 누적.
     *       = 현행 entitlement 당기분(ANNUAL/TENURE) + {@code computeBackfillPeriods}의 유효분(ANNUAL/TENURE)을
     *       read-only 합산. 유효기간(AXIS6 12개월) 고정이라 과거분이 소멸돼도 "오늘 보유했어야 할 양"은
     *       최신 발생분이 된다(SHEET3/SHEET8).</li>
     *   <li><b>기존 부여누적</b>: live(STATUS!='CANCELED' AND DEL_YN='N') 전 STATUTORY 유형(월차 포함)에 대해
     *       <b>"소멸 제외 + 사용 포함"</b> = {@code USED_DAYS + (AVAIL_TO_DATE >= today ? GRANT_DAYS-USED_DAYS : 0)}의
     *       합(정정 2026-05-26 / 결정문서 D1). 사용한 연차도 혜택 제공분이므로 누적에 포함하고, 이미 소멸한
     *       미사용분은 제외한다. 월차(STATUTORY_MONTHLY)도 포함해야 경계B(월차 누적 차감 → 차액 +8)가 맞다.
     *       {@link LeaveDashboardMapper#selectStatutoryGrantAccrual}.</li>
     * </ul>
     * <p>차액 &gt; 0이면 그만큼 STATUTORY_ANNUAL 단건을 보전한다. &le; 0이면 0(미래변경/근속감소 = 옵션2 동치,
     * 절대 줄이지 않음 — §8.5.8 기부여보호).
     *
     * @return 보전할 차액 일수(&ge; 0). 0이면 보전 없음.
     */
    private BigDecimal computeBackfillShortfall(String cmpnyCd, String userCd, String hireDate, GrantContext ctx) {
        BigDecimal newBasisCumulative = computeNewBasisAnnualCumulative(cmpnyCd, userCd, hireDate, ctx);
        BigDecimal existingCumulative = nvlZero(
                leaveDashboardMapper.selectStatutoryGrantAccrual(cmpnyCd, userCd, ctx.today));
        BigDecimal diff = newBasisCumulative.subtract(existingCumulative);
        return (diff.signum() > 0) ? diff : BigDecimal.ZERO;
    }

    /**
     * 새 입사일 기준 "오늘 보유했어야 할" 본연차+가산 누적 산정 (read-only). prafta-030 BE-1.
     *
     * <p>현행 entitlement 당기분(ANNUAL/TENURE) + {@code computeBackfillPeriods}의 유효분(ANNUAL/TENURE)을
     * 합산한다(월차 제외). {@code newInsert} 여부와 무관하게 "목표 누적"이므로 모든 유효 컴포넌트를 합산한다
     * (멱등 skip 분도 보유량에 포함). 유효기간 12개월 고정 전제로 과거 소멸분은 백필 후보에서 이미 제외된다.
     */
    private BigDecimal computeNewBasisAnnualCumulative(String cmpnyCd, String userCd, String hireDate, GrantContext ctx) {
        int creditMonths = leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd);
        Entitlement ent = resolveEntitlement(ctx.policy, hireDate, creditMonths, cmpnyCd, userCd);

        BigDecimal sum = BigDecimal.ZERO;
        for (GrantComponent gc : ent.components) {
            if (isAnnualOrTenure(gc.grantType)) {
                sum = sum.add(gc.days);
            }
        }
        // 과거 유효분(소멸 전) ANNUAL/TENURE 백필 후보 합산 — 멱등 skip 여부와 무관(목표 누적이므로 전부 합산)
        for (PeriodComponent bp : computeBackfillPeriods(cmpnyCd, userCd, hireDate, ctx, "")) {
            if (isAnnualOrTenure(bp.grantType)) {
                sum = sum.add(bp.days);
            }
        }
        return sum;
    }

    /** GRANT_TYPE이 본연차/근속가산(차액 대상)인지. 월차(STATUTORY_MONTHLY)는 false. */
    private boolean isAnnualOrTenure(String grantType) {
        return GRANT_TYPE_ANNUAL.equals(grantType) || GRANT_TYPE_TENURE.equals(grantType);
    }

    private BigDecimal nvlZero(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    /**
     * 과거 본연차/근속가산 백필 후보 산정 (read-only — DB 쓰기 없음). prafta-023 C.
     *
     * <p>prafta-032 009 이후 호출처: {@link #computeNewBasisAnnualCumulative}(영향분석 누락부여 추정의 새 기준 누적
     * 산정) 및 {@link #estimateBackfillDays}. 과거 처리방식(BACKFILL/RESET_ALL) 부여 분기는 폐기됐고, 본 메서드는
     * "새 입사일 기준 오늘 보유했어야 할 본연차+가산 누적"의 read-only 합산에만 쓰인다.
     *
     * <p>입사 이후 <b>완성한 각 근속연차</b>의 본연차 15일(+AXIS5 근속가산)을 후보로 만들되, 다음을 적용한다.
     * <ul>
     *   <li><b>유효기간 제한 (prafta-023 결정: (i)+유효기간)</b>: 각 컴포넌트의 발생일 기준 사용기간
     *       (availFrom~availTo, AXIS6 유효개월)이 이미 지난(availTo &lt; today) 과거분은 제외 — 소멸된 연차를 휴가로 되살리지 않는다.</li>
     *   <li><b>당해(현재 달력연도) 분 제외</b>: 현재분은 {@code buildUserPlan}의 당기 부여가 담당하므로 중복을 피해 제외한다.</li>
     *   <li>경력인정(creditMonths)은 근속가산 tier에만 가산(본연차 발생 연차 수에는 미반영).</li>
     * </ul>
     * <p>한계(후속): 월차 per-월 누적과 AXIS1=FISCAL_YEAR 과거 백필은 본 단계 범위 밖이다(작업 E/후속).
     *    FISCAL_YEAR 정책이면 빈 목록을 반환한다(당기 부여는 buildUserPlan이 처리).
     */
    private List<PeriodComponent> computeBackfillPeriods(String cmpnyCd, String userCd, String hireDate,
                                                         GrantContext ctx, String keySuffix) {
        List<PeriodComponent> out = new ArrayList<>();

        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = parseYyyymmdd(ctx.today);
        if (hire == null || todayDate == null || hire.isAfter(todayDate)) {
            return out;
        }

        int validityMonths = resolveValidityMonths(cmpnyCd);
        int creditYears = Math.max(0, leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd)) / 12;
        String axis1 = (ctx.policy == null) ? AXIS1_HIRE_DATE : nvl(ctx.policy.getAxis1GrantBase(), AXIS1_HIRE_DATE);

        if (AXIS1_FISCAL_YEAR.equals(axis1)) {
            // prafta-023 #2: FISCAL_YEAR 과거 회계연도 백필(발생일 = 회계연도 시작일). 당해 회계연도는 당기 부여가 담당.
            addFiscalBackfillPeriods(out, cmpnyCd, userCd, hire, todayDate, ctx.policy,
                    validityMonths, ctx.today, keySuffix, creditYears);
            return out;
        }

        // AXIS1=HIRE_DATE: 입사 anniversary(입사+12*y개월) 기준 과거 본연차·근속 백필
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
        int actualYears = actualMonths / 12;
        // 당해(최근 기념일연도) 분은 당기부여(resolveHireDateEntitlement)가 담당 → 백필에서 제외.
        //   당기부여와 동일 기준(기념일연도)이어야 같은 근속연차가 중복 부여되지 않는다(prafta-028 C).
        String currentYearLabel = latestAnniversaryYearLabel(hire, todayDate);

        for (int y = 1; y <= actualYears; y++) {
            LocalDate accrual = hire.plusMonths(12L * y);
            if (accrual.isAfter(todayDate)) {
                break;
            }
            String label = String.valueOf(accrual.getYear());
            // 당해(현재 달력연도) 분은 당기 부여(buildUserPlan)가 담당 → 백필에서는 제외(중복 방지)
            if (label.equals(currentYearLabel)) {
                continue;
            }
            String availFrom = accrual.format(DateTimeFormatter.BASIC_ISO_DATE);
            String availTo = addMonthsYyyymmdd(availFrom, validityMonths);
            // 유효기간 경과(소멸) 분은 제외 — 휴가로 되살리지 않음
            if (availTo.compareTo(ctx.today) < 0) {
                continue;
            }
            addBackfillComponent(out, cmpnyCd, userCd, keySuffix, label, availFrom, availTo,
                    LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL, BigDecimal.valueOf(BASE_ANNUAL_DAYS));
            int bonus = tenureBonusDays(ctx.policy, y + creditYears);
            if (bonus > 0) {
                addBackfillComponent(out, cmpnyCd, userCd, keySuffix, label, availFrom, availTo,
                        LEAVE_CD_TENURE, GRANT_TYPE_TENURE, BigDecimal.valueOf(bonus));
            }
        }
        return out;
    }

    /**
     * FISCAL_YEAR 과거 회계연도 백필 (prafta-023 #2). 입사 이후 오늘까지 도래한 회계연도 시작(들) 중
     * <b>당해 회계연도를 제외</b>한 과거 회계연도마다 본연차 15 + 근속가산을, 발생일(회계연도 시작) 기준
     * 사용기간(AXIS6) 유효분만 후보로 만든다. 당해 회계연도분은 buildUserPlan(당기 부여)이 담당.
     */
    private void addFiscalBackfillPeriods(List<PeriodComponent> out, String cmpnyCd, String userCd,
                                          LocalDate hire, LocalDate todayDate, LeavePolicyVO policy,
                                          int validityMonths, String today, String keySuffix, int creditYears) {
        int startMm = parseMm(policy == null ? null : policy.getAxis2FiscalStartMm(), 1);
        int startDd = parseDd(policy == null ? null : policy.getAxis2FiscalStartDd(), 1);
        String currentFiscalYearLabel = String.valueOf(currentFiscalStart(todayDate, startMm, startDd).getYear());

        int k = 0; // 입사 이후 도래한 회계연도 시작 순번(1-based, 근속가산 tier 산정용)
        for (int y = hire.getYear(); y <= todayDate.getYear(); y++) {
            LocalDate fs = safeMonthDay(y, startMm, startDd);
            // 입사 이후(같은 날 포함) ~ 오늘(같은 날 포함) 사이에 도래한 회계연도 시작만
            if (fs.isBefore(hire) || fs.isAfter(todayDate)) {
                continue;
            }
            k++;
            String label = String.valueOf(fs.getYear());
            if (label.equals(currentFiscalYearLabel)) {
                continue; // 당해 회계연도는 당기 부여가 담당 → 백필 제외(중복 방지)
            }
            String availFrom = fs.format(DateTimeFormatter.BASIC_ISO_DATE);
            String availTo = addMonthsYyyymmdd(availFrom, validityMonths);
            if (availTo.compareTo(today) < 0) {
                continue; // 유효기간 경과(소멸) 분 제외
            }
            addBackfillComponent(out, cmpnyCd, userCd, keySuffix, label, availFrom, availTo,
                    LEAVE_CD_ANNUAL, GRANT_TYPE_ANNUAL, BigDecimal.valueOf(BASE_ANNUAL_DAYS));
            int bonus = tenureBonusDays(policy, k + creditYears);
            if (bonus > 0) {
                addBackfillComponent(out, cmpnyCd, userCd, keySuffix, label, availFrom, availTo,
                        LEAVE_CD_TENURE, GRANT_TYPE_TENURE, BigDecimal.valueOf(bonus));
            }
        }
    }

    /** 백필 후보 1건 생성 + 신규부여 여부(dual-read 멱등) 표시. */
    private void addBackfillComponent(List<PeriodComponent> out, String cmpnyCd, String userCd, String keySuffix,
                                      String periodLabel, String availFrom, String availTo,
                                      String leaveCd, String grantType, BigDecimal days) {
        boolean newInsert = days.signum() > 0 && !alreadyGranted(cmpnyCd, userCd, periodLabel, grantType, keySuffix);
        out.add(new PeriodComponent(periodLabel, leaveCd, grantType, days, availFrom, availTo, newInsert));
    }

    /**
     * 1년 미만 법정 월차(§60②, §8.5.4)를 per-월 누적 후보로 산정 (prafta-023 #1, read-only).
     *
     * <p>입사 후 완성한 각 개월(최대 11)마다 1일, periodLabel=YYYYMM(멱등키 {@code {userCd}_{YYYYMM}_STATUTORY_MONTHLY}),
     * 발생일=입사+m개월. 월차는 법정 의무라 처리방식·AXIS 무관하게 부여한다.
     *
     * <p><b>소멸(AVAIL_TO_DATE) = 만 1년 도래일 일괄</b>(prafta-030 D2-B, 근기법 §60⑦, 정답표 §1.4). 1년 미만 월차는
     * 발생일에 무관하게 "입사일 + 1년 − 1일"(입사 1주년 직전)에 모두 소멸한다(예: 2023-08-15 입사 → 2024-08-14 소멸).
     * 만 1년 도래일이 오늘 이전인(만1년 경과) 직원은 전 월차가 skip되어 0이 되고, 1년 미만 직원은 월차가 유지된다.
     * AXIS6(발생일+유효개월)은 본연차/가산 유효기간 전용이며 월차에는 적용하지 않는다.
     *
     * <p><b>레거시 연 단위 집계 월차가 ACTIVE인 해는 중복 방지를 위해 per-월을 건너뛴다</b>(집계↔per-월 상호배타).
     * RESET_ALL은 집계분을 먼저 취소(CANCELED)하므로 ACTIVE가 아니게 되어 per-월로 재발급된다.
     * (한계: 레거시 ACTIVE 집계 보유 연도는 만료/RESET 전까지 집계 유지 — 완전 전환은 일회성 마이그레이션 필요, 후속)
     */
    private List<PeriodComponent> computeMonthlyPeriods(String cmpnyCd, String userCd, String hireDate,
                                                        GrantContext ctx, String keySuffix) {
        List<PeriodComponent> out = new ArrayList<>();
        LocalDate hire = parseYyyymmdd(hireDate);
        LocalDate todayDate = parseYyyymmdd(ctx.today);
        if (hire == null || todayDate == null || hire.isAfter(todayDate)) {
            return out;
        }
        // prafta-030 BE-2(D2, 2026-05-26 정정) 월차 게이트: "고용승계 더블딥"에만 한정해 월차를 차단한다(빈 목록).
        //   차단 = (실근속<12) AND (경력인정 포함 산정근속>=12) AND (이번 부여 entitlement에 full 본연차 15 발생).
        //   ⚠️ 이전 초안("본연차 발생 시 차단")은 과잉 — 경력인정 없는 정상 FISCAL 비례부여 중도입사자의 법정
        //      월차(§8.5.4)까지 차단했다. 좁힌 결과: 정상 근로자(경력인정 0)·FISCAL 비례(crossed==1 PRORATE,<15)·
        //      FISCAL 첫 부분기(crossed==0)는 모두 월차 보존. 오직 경력인정으로 실근속<1년인데 full 15를 받는
        //      더블딥만 차단(본연차 15 >= 월차 상한 11 + 즉시 사용 가능 → "더 유리한 처우"라 §60② 위반 아님).
        //   ※ 기존 부여(이미 INSERT된 월차)는 건드리지 않는다(미래 부여 산정만 게이트, §8.5.8 기부여보호).
        int creditMonths = leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd);
        if (isCreditDoubleDip(ctx.policy, hire, todayDate, creditMonths, cmpnyCd, userCd)) {
            return out;
        }
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, todayDate));
        int monthlyCount = Math.min(actualMonths, MONTHLY_MAX); // 완성한 개월(최대 11)
        if (monthlyCount <= 0) {
            return out;
        }
        java.util.Map<String, Boolean> aggActiveByYear = new java.util.HashMap<>(); // 연도별 ACTIVE 집계 월차 보유 캐시
        // prafta-030 D2-B(2026-05-26): 1년 미만 월차 소멸 = 만 1년 도래일 일괄(근기법 §60⑦, 정답표 §1.4).
        //   첫해 월차는 발생일(AVAIL_FROM)에 무관하게 "입사일 + 1년 − 1일"(입사 1주년 직전)에 일괄 소멸한다.
        //   예: 2023-08-15 입사 → 2024-08-14 소멸. AXIS6(발생일+유효개월)은 본연차/가산에만 적용하고 월차에는 쓰지 않는다.
        String monthlyAvailTo = hire.plusYears(1).minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);

        for (int m = 1; m <= monthlyCount; m++) {
            LocalDate accrual = hire.plusMonths(m); // m번째 만근 완성일(= 월차 발생일)
            if (accrual.isAfter(todayDate)) {
                break;
            }
            String year = String.valueOf(accrual.getYear());
            Boolean hasAgg = aggActiveByYear.get(year);
            if (hasAgg == null) {
                hasAgg = hasActiveAggregateMonthly(cmpnyCd, userCd, year);
                aggActiveByYear.put(year, hasAgg);
            }
            if (hasAgg) {
                continue; // 레거시 ACTIVE 집계 월차 보유 연도 → per-월 미부여(중복 방지)
            }
            String yyyymm = accrual.format(DateTimeFormatter.ofPattern("yyyyMM"));
            String availFrom = accrual.format(DateTimeFormatter.BASIC_ISO_DATE);
            // 월차 소멸일 = 만 1년 도래일(모든 첫해 월차 동일 — 일괄 소멸). AXIS6 발생일+유효개월 산식을 쓰지 않는다.
            String availTo = monthlyAvailTo;
            if (availTo.compareTo(ctx.today) < 0) {
                continue; // 만 1년 도래(소멸)분 제외 — 만1년 경과 직원은 전 월차 skip, 1년 미만 직원은 유지
            }
            addBackfillComponent(out, cmpnyCd, userCd, keySuffix, yyyymm, availFrom, availTo,
                    LEAVE_CD_MONTHLY, GRANT_TYPE_MONTHLY, BigDecimal.ONE);
        }
        return out;
    }

    /**
     * "경력인정으로 인한 고용승계 더블딥"인지 판정 (prafta-030 BE-2 / D2 월차 게이트용, read-only).
     *
     * <p>월차 차단을 "더블딥"으로 좁힌다(결정문서 D2 정정 2026-05-26). 세 조건을 모두 만족할 때만 true.
     * <ul>
     *   <li>(1) 실근속 {@code actualMonths < 12} — 실제 재직 1년 미만.</li>
     *   <li>(2) 경력 인정 포함 산정근속 {@code creditedMonths >= 12} — 경력인정으로 1년 이상으로 산정됨.</li>
     *   <li>(3) 이번 부여 entitlement에 full 본연차(STATUTORY_ANNUAL, days {@code >= 15}) 발생.</li>
     * </ul>
     * <p>좁힌 결과:
     * <ul>
     *   <li>정상 근로자(경력인정 0): 산정=실근속 → (2) 거짓 → 월차 보존.</li>
     *   <li>FISCAL 비례(crossed==1 PRORATE, 비례&lt;15): (3) 거짓 → 월차+비례 보존.</li>
     *   <li>FISCAL 첫 부분기(crossed==0): 본연차 미발생 → (3) 거짓 → 월차 보존(공백 방지).</li>
     *   <li>경력인정으로 실근속&lt;1년인데 full 15(고용승계 더블딥): 세 조건 충족 → 차단(중복 월차 제거).</li>
     * </ul>
     * <p>full 15일 때만 차단이 정당(본연차 15 ≥ 월차 상한 11 + 즉시 사용 가능 → "더 유리한 처우"). 비례 7 등
     * {@code < 11}이면 차단 시 법정 미달이므로 (3)에서 걸러 차단하지 않는다.
     */
    private boolean isCreditDoubleDip(LeavePolicyVO policy, LocalDate hire, LocalDate today,
                                      int creditMonths, String cmpnyCd, String userCd) {
        if (hire == null || today == null || hire.isAfter(today)) {
            return false;
        }
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, today));
        // (1) 실근속 1년 미만 + (2) 경력인정 포함 산정근속 1년 이상 — 둘 중 하나라도 아니면 더블딥 아님.
        if (actualMonths >= 12) {
            return false;
        }
        int creditedMonths = actualMonths + Math.max(0, creditMonths);
        if (creditedMonths < 12) {
            return false;
        }
        // (3) 이번 부여 entitlement에 full 본연차(STATUTORY_ANNUAL, days >= 15)가 발생하는지 확인(read-only 재호출).
        String hireYmd = hire.format(DateTimeFormatter.BASIC_ISO_DATE);
        Entitlement ent = resolveEntitlement(policy, hireYmd, creditMonths, cmpnyCd, userCd);
        for (GrantComponent gc : ent.components) {
            if (GRANT_TYPE_ANNUAL.equals(gc.grantType)
                    && gc.days != null
                    && gc.days.compareTo(BigDecimal.valueOf(BASE_ANNUAL_DAYS)) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** 해당 연도에 연 단위 집계 월차(레거시 {@code {YYYY}_STATUTORY_MONTHLY} 또는 {@code ..._HIRE})가 ACTIVE로 존재하는지. */
    private boolean hasActiveAggregateMonthly(String cmpnyCd, String userCd, String year) {
        return leaveDashboardMapper.countActiveByIdempotencyKey(cmpnyCd,
                       buildIdempotencyKey(userCd, year, GRANT_TYPE_MONTHLY, "")) > 0
            || leaveDashboardMapper.countActiveByIdempotencyKey(cmpnyCd,
                       legacyHireIdempotencyKey(userCd, year, GRANT_TYPE_MONTHLY, "")) > 0;
    }

    /**
     * HIRE_DATE 당기부여 멱등키 라벨 = 가장 최근 도래한 입사 기념일의 연도(YYYY). (prafta-028 C)
     *
     * <p>백필(computeBackfillPeriods)이 과거 기념일별로 {@code accrual.getYear()} 라벨을 쓰므로, 당기부여도
     * 동일하게 기념일 기준 연도를 써야 같은 근속연차가 '달력연도' 당기 키와 '기념일연도' 백필 키로 갈라져
     * 이중부여되는 문제(prafta-028 C)가 사라진다. 근속 1년 미만(기념일 미도래)이면 입사 연도를 반환하나,
     * 그 경우 당기 본연차 컴포넌트가 없어(월차만) 이 라벨은 부여에 사용되지 않는다.
     */
    private String latestAnniversaryYearLabel(LocalDate hire, LocalDate today) {
        int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, today));
        int actualYears = actualMonths / 12;
        return String.valueOf(hire.plusMonths(12L * actualYears).getYear());
    }

    /** 오늘 기준 현재 회계연도 시작일(오늘 포함 이전의 가장 최근 startMm/startDd). */
    private LocalDate currentFiscalStart(LocalDate today, int startMm, int startDd) {
        LocalDate thisYearStart = safeMonthDay(today.getYear(), startMm, startDd);
        if (!thisYearStart.isAfter(today)) {
            return thisYearStart;
        }
        return safeMonthDay(today.getYear() - 1, startMm, startDd);
    }

    /**
     * 입사일 이후 오늘까지 회계연도 시작일을 넘긴 횟수.
     * = (입사 직후 첫 회계연도 시작 ~ 현재 회계연도 시작) 사이의 시작 발생 개수.
     * 입사일이 회계연도 시작일과 같은 날이면 그날을 1회로 센다(이미 도래).
     */
    private int countFiscalStartsCrossed(LocalDate hire, LocalDate today, int startMm, int startDd) {
        int count = 0;
        // 입사 연도부터 오늘 연도까지 각 연도의 회계연도 시작일을 검사
        for (int y = hire.getYear(); y <= today.getYear(); y++) {
            LocalDate fs = safeMonthDay(y, startMm, startDd);
            // 입사일 이후(같은 날 포함)이고 오늘 이전(같은 날 포함)인 회계연도 시작만 카운트
            if (!fs.isBefore(hire) && !fs.isAfter(today)) {
                count++;
            }
        }
        return count;
    }

    /** 윤년/말일 보정(02/29 등): 해당 연·월의 마지막 일을 넘으면 말일로 클램프. */
    private LocalDate safeMonthDay(int year, int mm, int dd) {
        int m = Math.min(Math.max(mm, 1), 12);
        LocalDate first = LocalDate.of(year, m, 1);
        int last = first.lengthOfMonth();
        int d = Math.min(Math.max(dd, 1), last);
        return LocalDate.of(year, m, d);
    }

    /**
     * 컴포넌트(월차/본연차/근속가산) 1건 부여. 멱등성 키로 중복(동일 직원·연도식별자·종류) 차단.
     *
     * <p>멱등키 = {@code {userCd}_{periodLabel}_{grantType}{keySuffix}} (§8.5.8, prafta-023 A).
     * 존재 확인은 {@link #alreadyGranted}로 정식 키 + 레거시({@code _HIRE}) 키를 <b>live-only</b> dual-read 한다.
     * keySuffix는 RESET_ALL 재발급 시 {@code _R{HIST_ID}}(회차마다 유니크), 그 외 빈 문자열.
     *
     * <p><b>prafta-029 옵션 A(CANCELED→reactivate)</b>: live 기부여가 없더라도 같은 멱등키에 CANCELED 단건이
     * 남아 있을 수 있다(RESET_ALL이 표준키를 CANCELED로 만든 경우). UNIQUE(CMPNY_CD, IDEMPOTENCY_KEY)가
     * INSERT를 막으므로, CANCELED가 있으면 INSERT 대신 {@link LeaveDashboardMapper#reactivateCanceledGrant}로
     * 그 단건을 ACTIVE로 부활(재부여 값 갱신, USED_DAYS 보존)시킨다. CANCELED가 없으면 기존 INSERT 경로.
     *
     * <p>TOCTOU: reactivate가 0행(경합으로 그 사이 부활/소멸)이면 INSERT로 폴백하고, 그래도 Duplicate면
     * 이미 부여된 것으로 보고 false로 건너뛴다(이상 재시도 루프 없음).
     *
     * @return 실제 INSERT 또는 reactivate 발생 시 true, 중복(이미 부여)이라 건너뛰면 false
     */
    private boolean grantComponent(String cmpnyCd, String userCd, String leaveCd, String grantType,
                                   BigDecimal days, Long policySeq, String today, String availFromDate,
                                   String availToDate, String yearLabel, String keySuffix,
                                   String grantReason, String operatorUserCd) {
        if (days == null || days.signum() <= 0) {
            return false;
        }
        // 멱등성: 동일 직원·기간식별자·종류로 이미 live 부여됐으면 건너뜀 (재실행 시 중복 부여 차단 — §8.5.8).
        // 정식 키 + 레거시(_HIRE) 키 live-only dual-read로 prafta-022 기존 부여까지 인식(마이그레이션 불요 — prafta-023 A).
        // 시스템 연차 종류 존재는 진입부에서 1회 검증함(ensureLeaveTypeExists).
        if (alreadyGranted(cmpnyCd, userCd, yearLabel, grantType, keySuffix)) {
            return false;
        }
        String idempotencyKey = buildIdempotencyKey(userCd, yearLabel, grantType, keySuffix);

        LeaveGrantInsertVO vo = new LeaveGrantInsertVO();
        vo.setGrantId(leaveDashboardMapper.selectNextGrantId(cmpnyCd));
        vo.setCmpnyCd(cmpnyCd);
        vo.setUserCd(userCd);
        vo.setLeaveCd(leaveCd);
        vo.setGrantType(grantType);
        vo.setGrantDays(days.setScale(1, RoundingMode.HALF_UP));
        vo.setUsedDays(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        vo.setGrantReason(grantReason);
        vo.setGrantByType(GRANT_BY_TYPE_AUTO);
        vo.setPolicySeq(policySeq);
        vo.setGrantDate(today);
        vo.setAvailFromDate(availFromDate);
        vo.setAvailToDate(availToDate);
        vo.setIdempotencyKey(idempotencyKey);
        vo.setStatus(STATUS_ACTIVE);
        vo.setInsertNo(operatorUserCd);

        // 옵션 A: 같은 멱등키에 CANCELED 단건이 있으면 INSERT 대신 부활(reactivate). UNIQUE 충돌 회피 + 사용분(USED_DAYS) 보존.
        //   _HIRE CANCELED는 reactivate 비대상 — 신규 부여는 항상 정식키(idempotencyKey)로만 하고, _HIRE는 dual-read 인식 전용이라
        //   여기서 조회하는 키도 정식키이다(따라서 _HIRE CANCELED는 부활시키지 않음).
        String canceledGrantId = leaveDashboardMapper.selectCanceledGrantIdByKey(cmpnyCd, idempotencyKey);
        if (canceledGrantId != null) {
            int reactivated = leaveDashboardMapper.reactivateCanceledGrant(vo);
            if (reactivated == 1) {
                log.info("CANCELED 표준키 재활성화. cmpnyCd={}, userCd={}, key={}", cmpnyCd, userCd, idempotencyKey);
                return true;
            }
            // reactivated==0: 경합으로 그 사이 CANCELED가 사라짐 → 아래 INSERT로 폴백.
        }

        try {
            leaveDashboardMapper.insertManualGrant(vo);
        } catch (DuplicateKeyException e) {
            // 동시 호출 경합(TOCTOU): UNIQUE(CMPNY_CD, IDEMPOTENCY_KEY)가 최종 차단.
            // 이미 부여된 것으로 보고 건너뜀 처리(이중 부여 방지).
            log.info("정책 기준 부여 - 멱등키 경합으로 건너뜀. cmpnyCd={}, key={}", cmpnyCd, idempotencyKey);
            return false;
        }
        return true;
    }

    /**
     * 멱등키 산출 (prafta-023 작업 A). 정식 키 = {@code {userCd}_{periodLabel}_{grantType}{keySuffix}} (§8.5.8).
     * keySuffix는 RESET_ALL 재발급 시 {@code _R{HIST_ID}}(회차마다 유니크), 그 외 빈 문자열.
     * apply(부여 실행)와 preview(신규부여 판별)가 동일 키 규칙을 공유하도록 단일 메서드로 둔다.
     *
     * <p>prafta-022까지는 임시로 {@code _HIRE} 접미사를 붙였다(수동 버튼 한정). 본 작업에서 정식 키로 전환하되,
     * 기존 부여를 재키잉하지 않고 {@link #alreadyGranted}의 dual-read로 이중부여를 차단한다(운영 마이그레이션 불요).
     */
    private String buildIdempotencyKey(String userCd, String periodLabel, String grantType, String keySuffix) {
        return userCd + "_" + periodLabel + "_" + grantType + keySuffix;
    }

    /** prafta-022까지 발급된 임시 키({@code _HIRE} 접미사). 신규 발급엔 쓰지 않고 dual-read 존재확인용으로만 사용. */
    private String legacyHireIdempotencyKey(String userCd, String periodLabel, String grantType, String keySuffix) {
        return userCd + "_" + periodLabel + "_" + grantType + "_HIRE" + keySuffix;
    }

    /**
     * 정식 키 또는 레거시({@code _HIRE}) 키 중 하나라도 이미 <b>live</b> 부여돼 있으면 true (prafta-023 A, dual-read).
     * prafta-022에 {@code _HIRE} 키로 부여된 기존 건을 마이그레이션 없이 인식해 이중부여를 차단한다.
     *
     * <p><b>prafta-029 옵션 A(live-only)</b>: 두 dual-read를 {@code countByIdempotencyKey}(CANCELED 포함)에서
     * {@link LeaveDashboardMapper#countLiveByIdempotencyKey}(STATUS!='CANCELED' AND DEL_YN='N')로 교체했다.
     * CANCELED 표준키는 멱등키를 점유하지 않은 것으로 보므로, RESET_ALL이 표준키를 CANCELED로 만든 뒤
     * KEEP_AND_BACKFILL/APPLY_NEW이 같은 기간을 reactivate(또는 INSERT)로 재부여할 수 있다(주 버그 수정).
     *
     * <p>prafta-029 변형키 가드(유지): <b>표준키 클릭(keySuffix 비어 있음)</b>일 때만, 같은 (기간·종류)에 회차 접미사
     * ({@code _R{HIST_ID}}) 등 '변형 키'로 이미 <b>ACTIVE</b> 부여가 있으면 기부여로 간주한다(이중부여 방지).
     * RESET_ALL이 회차키로 재발급한 기간을 직후 APPLY_NEW 클릭이 표준키로 다시 부여(특히 월차)하던
     * 분할/이중부여 누수를 차단한다. 리셋 클릭(keySuffix={@code _R...})은 cancel→재발급 경로라 이 검사를
     * 적용하지 않는다(계획 산정 시점이 cancel 이전이라 적용 시 재발급이 막힘).
     */
    private boolean alreadyGranted(String cmpnyCd, String userCd, String periodLabel, String grantType, String keySuffix) {
        if (leaveDashboardMapper.countLiveByIdempotencyKey(cmpnyCd,
                buildIdempotencyKey(userCd, periodLabel, grantType, keySuffix)) > 0) {
            return true;
        }
        if (leaveDashboardMapper.countLiveByIdempotencyKey(cmpnyCd,
                legacyHireIdempotencyKey(userCd, periodLabel, grantType, keySuffix)) > 0) {
            return true;
        }
        // prafta-029: 표준키 클릭 한정 — 회차키(_R{HIST_ID}) 등 변형 키로 이미 ACTIVE 부여가 있으면 기부여.
        if (keySuffix == null || keySuffix.isEmpty()) {
            String baseKey = buildIdempotencyKey(userCd, periodLabel, grantType, "");
            return leaveDashboardMapper.countActiveBySuffixVariant(cmpnyCd, baseKey) > 0;
        }
        return false;
    }

    /** 부여에 쓸 시스템 연차 종류(LEAVE_CD)가 회사에 설정돼 있는지 검증. 없으면 ATTD_400_059. */
    private void ensureLeaveTypeExists(String cmpnyCd, String leaveCd) {
        if (leaveDashboardMapper.countLeaveTypeExists(cmpnyCd, leaveCd) < 1) {
            log.warn("정책 기준 부여 - 시스템 연차 종류 미설정. cmpnyCd={}, leaveCd={}", cmpnyCd, leaveCd);
            throw new ApiException(AttdErrorCode.ATTD_400_059);
        }
    }

    /**
     * 근속 가산 일수(§8.5.3 AXIS5). {@code year} = 근속 연차(1-based).
     * bonus = year &gt;= start ? floor((year-start)/interval)+1 : 0, 단 (본연차+bonus) &le; maxDays.
     * 활성 정책의 AXIS5 값을 쓰되 없으면 법정 기본(3/2/25).
     */
    private int tenureBonusDays(LeavePolicyVO policy, int year) {
        int start = AXIS5_DEFAULT_START_YEAR;
        int interval = AXIS5_DEFAULT_INTERVAL;
        int maxDays = AXIS5_DEFAULT_MAX_DAYS;
        if (policy != null) {
            if (policy.getAxis5StartYear() != null) {
                start = policy.getAxis5StartYear();
            }
            if (policy.getAxis5Interval() != null && policy.getAxis5Interval() >= 1) {
                interval = policy.getAxis5Interval();
            }
            if (policy.getAxis5MaxDays() != null) {
                maxDays = policy.getAxis5MaxDays();
            }
        }
        int bonus = (year >= start) ? ((year - start) / interval) + 1 : 0;
        int total = Math.min(BASE_ANNUAL_DAYS + bonus, maxDays);
        return Math.max(0, total - BASE_ANNUAL_DAYS);
    }

    // ============================================================
    // 내부 운반체
    // ============================================================

    /** 부여/프리뷰 공통 컨텍스트(활성 정책·정책SEQ·오늘·유효종료일). 진입부 1회 산정 후 직원 루프에서 공유. */
    private static final class GrantContext {
        private final LeavePolicyVO policy;
        private final Long policySeq;
        private final String today;
        private final String availToDate;

        private GrantContext(LeavePolicyVO policy, Long policySeq, String today, String availToDate) {
            this.policy = policy;
            this.policySeq = policySeq;
            this.today = today;
            this.availToDate = availToDate;
        }
    }

    /**
     * 직원 1명의 read-only 부여 계획. apply는 이 계획대로 INSERT/마킹을 실행하고, preview는 집계만 한다.
     * 계획 산정 자체는 DB 쓰기를 하지 않는다.
     *
     * <p>prafta-032 009: 처리방식(handlingType)·isReset/isBackfill·histId·cancelGrantIds 폐기로 제거했다.
     * 멱등키 접미사는 항상 표준키(빈 문자열)이다.
     */
    private static final class UserGrantPlan {
        private final String keySuffix;
        private final String yearLabel;
        private final String availFromDate;
        private final List<PlanComponent> components;
        /** 프리뷰/안내용 비고(없으면 null). */
        private final String note;

        private UserGrantPlan(String keySuffix, String yearLabel, String availFromDate,
                              List<PlanComponent> components, String note) {
            this.keySuffix = keySuffix;
            this.yearLabel = yearLabel;
            this.availFromDate = availFromDate;
            this.components = components;
            this.note = note;
        }
    }

    /** 계획 컴포넌트(연차코드/부여분류/일수 + 실제 신규 INSERT 여부). newInsert=false면 멱등 skip 예정. */
    private static final class PlanComponent {
        private final String leaveCd;
        private final String grantType;
        private final BigDecimal days;
        private final boolean newInsert;

        private PlanComponent(String leaveCd, String grantType, BigDecimal days, boolean newInsert) {
            this.leaveCd = leaveCd;
            this.grantType = grantType;
            this.days = days;
            this.newInsert = newInsert;
        }
    }

    /**
     * 과거 백필 1기간 컴포넌트 (발생연도 라벨 + 자기 발생일 기준 사용기간 + 신규부여 여부). prafta-023 C.
     * 당기 부여(PlanComponent)와 달리 컴포넌트마다 발생일 기준 availFrom/availTo를 따로 갖는다.
     */
    private static final class PeriodComponent {
        private final String periodLabel;
        private final String leaveCd;
        private final String grantType;
        private final BigDecimal days;
        private final String availFromDate;
        private final String availToDate;
        private final boolean newInsert;

        private PeriodComponent(String periodLabel, String leaveCd, String grantType, BigDecimal days,
                                String availFromDate, String availToDate, boolean newInsert) {
            this.periodLabel = periodLabel;
            this.leaveCd = leaveCd;
            this.grantType = grantType;
            this.days = days;
            this.availFromDate = availFromDate;
            this.availToDate = availToDate;
            this.newInsert = newInsert;
        }
    }

    /** 직원 1명의 정책 기준 부여 컴포넌트 + 연도식별자(멱등키용). */
    private static final class Entitlement {
        private final List<GrantComponent> components;
        private final String yearLabel;

        private Entitlement(List<GrantComponent> components, String yearLabel) {
            this.components = components;
            this.yearLabel = yearLabel;
        }

        private static Entitlement empty(String yearLabel) {
            return new Entitlement(new ArrayList<>(), yearLabel);
        }

        /** 사용 시작일 = 부여일(today). 별도 비례/소급 시작일 산정은 023 범위. */
        private String availFromDate(String today) {
            return today;
        }
    }

    /** 부여 1건 컴포넌트(연차코드/부여분류/일수). */
    private static final class GrantComponent {
        private final String leaveCd;
        private final String grantType;
        private final BigDecimal days;

        private GrantComponent(String leaveCd, String grantType, BigDecimal days) {
            this.leaveCd = leaveCd;
            this.grantType = grantType;
            this.days = days;
        }
    }

    // ============================================================
    // 공용 유틸 (대시보드 원본에서 복제 — 다른 메서드와 공유되어 원본은 유지)
    // ============================================================

    private void requireCmpnyCd(String cmpnyCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /**
     * 관리자(MASTER/HR) 권한 가드 (정책서 §8.5.7). 위반 시 {@link AttdErrorCode#ATTD_403_020}.
     *
     * @param action 로그용 행위명(보안 민감 정보는 사용자에게 노출하지 않고 서버 로그에만 기록)
     */
    private void ensureManager(String cmpnyCd, String authCd, String action) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("{} 권한 없음. cmpnyCd={}, authCd={}", action, cmpnyCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_020);
        }
    }

    private String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    /** MM(01~12) 파싱. 잘못된 값이면 기본값. */
    private int parseMm(String mm, int def) {
        Integer v = parseIntSafe(mm);
        if (v == null || v < 1 || v > 12) {
            return def;
        }
        return v;
    }

    /** DD(01~31) 파싱. 잘못된 값이면 기본값(말일 보정은 safeMonthDay에서 수행). */
    private int parseDd(String dd, int def) {
        Integer v = parseIntSafe(dd);
        if (v == null || v < 1 || v > 31) {
            return def;
        }
        return v;
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int resolveValidityMonths(String cmpnyCd) {
        try {
            LeavePolicyVO policy = leavePolicyService.findActivePolicy(cmpnyCd);
            if (policy != null && policy.getAxis6ValidityMonths() != null
                    && policy.getAxis6ValidityMonths() > 0) {
                return policy.getAxis6ValidityMonths();
            }
        } catch (Exception e) {
            log.warn("활성 정책 유효기간 조회 실패, 기본값 적용. cmpnyCd={}", cmpnyCd);
        }
        return DEFAULT_VALIDITY_MONTHS;
    }

    private String addMonthsYyyymmdd(String yyyymmdd, int months) {
        LocalDate d = parseYyyymmdd(yyyymmdd);
        if (d == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_032);
        }
        return d.plusMonths(months).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private LocalDate parseYyyymmdd(String ymd) {
        if (ymd == null || ymd.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(ymd, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidYyyymmdd(String ymd) {
        return parseYyyymmdd(ymd) != null;
    }
}
