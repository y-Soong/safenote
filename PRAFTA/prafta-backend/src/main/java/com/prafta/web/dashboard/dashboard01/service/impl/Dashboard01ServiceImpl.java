package com.prafta.web.dashboard.dashboard01.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.dashboard.dashboard01.application.param.DashAttdPlanRegRateParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashAttdStatusRateParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashSafetyAcctParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashSafetyParam;
import com.prafta.web.dashboard.dashboard01.application.param.LeaveUsageParam;
import com.prafta.web.dashboard.dashboard01.application.param.OvertimeTrendParam;
import com.prafta.web.dashboard.dashboard01.dto.response.DashAttdPlanRegRateResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.DashAttdStatusRateResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.DashSafetyAcctResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.LeaveUsageResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.OvertimeTrendResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.SafetyPatrolResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.SafetyRiskResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.SafetyTbmTrendResponse;
import com.prafta.web.dashboard.dashboard01.mapper.Dashboard01Mapper;
import com.prafta.web.dashboard.dashboard01.result.DashAcctGradeCountResult;
import com.prafta.web.dashboard.dashboard01.result.DashAttdPlanRegRateRowResult;
import com.prafta.web.dashboard.dashboard01.result.DashAttdStatusCountResult;
import com.prafta.web.dashboard.dashboard01.result.DashHalfLeaveWindowRow;
import com.prafta.web.dashboard.dashboard01.result.DashPartialLeaveAttdRow;
import com.prafta.web.dashboard.dashboard01.result.DashRecentAcctResult;
import com.prafta.web.dashboard.dashboard01.result.DashSiteBaselineResult;
import com.prafta.web.dashboard.dashboard01.result.LeaveUseSplitResult;
import com.prafta.web.dashboard.dashboard01.result.OvertimeMonthlyResult;
import com.prafta.web.dashboard.dashboard01.result.PatrolTodayResult;
import com.prafta.web.dashboard.dashboard01.result.RiskStatusCountResult;
import com.prafta.web.dashboard.dashboard01.result.TbmMonthCntResult;
import com.prafta.web.dashboard.dashboard01.service.Dashboard01Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 웹 관리자 대시보드 서비스 구현 (PRAFTA-DASHBOARD-T1 골격 / T4·T5 안전 탭 추가).
 */
@Slf4j
@Service
public class Dashboard01ServiceImpl implements Dashboard01Service {

	// 무사고 기산일 성격 구분 (FE 라벨 분기용)
	private static final String BASELINE_ACCT = "ACCT";               // 최근 사고 발생일
	private static final String BASELINE_SITE_STR = "SITE_STR";       // 사업개시일
	private static final String BASELINE_SITE_INSERT = "SITE_INSERT"; // 사업장 등록일

	private final Dashboard01Mapper dashboard01Mapper;

	public Dashboard01ServiceImpl(Dashboard01Mapper dashboard01Mapper) {
		this.dashboard01Mapper = dashboard01Mapper;
	}

	// ── T4: 안전 탭 무사고 배너(S1) + 사고 summary(S5) ──────────────

	@Override
	public DashSafetyAcctResponse selectSafetyAcct(DashSafetyAcctParam param) {
		log.info("대시보드 안전 사고 summary 조회 진입 - cmpnyCd={}, siteCd={}, ym={}",
			param.gvCmpnyCd(), param.siteCd(), param.ym());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 1) 무사고 기산일: 최근 사고 발생일 → 없으면 사업개시일 → 없으면 사업장 등록일
		String baselineYmd = null;
		String baselineType = null;

		String latestYmd = dashboard01Mapper.selectDashLatestAcctYmd(param.gvCmpnyCd(), param.siteCd());
		if (StringUtils.hasText(latestYmd)) {
			baselineYmd = latestYmd;
			baselineType = BASELINE_ACCT;
		} else {
			DashSiteBaselineResult baseline =
				dashboard01Mapper.selectDashSiteBaseline(param.gvCmpnyCd(), param.siteCd());
			if (baseline != null && StringUtils.hasText(baseline.strDate())) {
				baselineYmd = baseline.strDate();
				baselineType = BASELINE_SITE_STR;
			} else if (baseline != null && StringUtils.hasText(baseline.insertYmd())) {
				baselineYmd = baseline.insertYmd();
				baselineType = BASELINE_SITE_INSERT;
			}
			// 둘 다 null(사이트 미존재 방어)이면 baseline 계열 전부 null 유지
		}

		// 2) 무사고 경과일 = 오늘 − 기산일 (당일 사고 0일, 미래일 방어 max 0)
		Integer noAcctDays = calcNoAcctDays(baselineYmd);
		if (noAcctDays == null) {
			// 기산일 파싱 불가(비정상 데이터) 시 기산 표시도 함께 내리지 않는다
			baselineYmd = null;
			baselineType = null;
		}

		// 3) 조회월(ym) → YYYYMMDD 범위 변환 후 등급별 카운트
		YearMonth yearMonth = YearMonth.parse(param.ym());
		String fromYmd = yearMonth.atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE);
		String toYmd = yearMonth.atEndOfMonth().format(DateTimeFormatter.BASIC_ISO_DATE);

		DashAcctGradeCountResult gradeCounts =
			dashboard01Mapper.selectDashAcctGradeCounts(param.gvCmpnyCd(), param.siteCd(), fromYmd, toYmd);

		// 4) 전체 기간 최근 사고 3건 (PII 미포함)
		List<DashRecentAcctResult> recentAcctList =
			dashboard01Mapper.selectDashRecentAcctList(param.gvCmpnyCd(), param.siteCd());

		return DashSafetyAcctResponse.builder()
			.noAcctDays(noAcctDays)
			.baselineYmd(baselineYmd)
			.baselineType(baselineType)
			.monthTotalCnt(gradeCounts.monthTotalCnt())
			.grade100Cnt(gradeCounts.grade100Cnt())
			.grade200Cnt(gradeCounts.grade200Cnt())
			.grade300Cnt(gradeCounts.grade300Cnt())
			.recentAcctList(recentAcctList)
			.build();
	}

	// ── T5: 안전 탭 순회점검(S2) / 위험성평가(S3) / TBM 추이(S4) ─────

	@Override
	public SafetyPatrolResponse selectSafetyPatrol(DashSafetyParam param) {
		log.info("대시보드 순회점검 위젯 조회 진입 - cmpnyCd={}, siteCd={}, ym={}",
			param.gvCmpnyCd(), param.siteCd(), param.ym());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 1) 당일 카드: 조회월(ym)과 무관하게 항상 오늘 기준 (사용자 확정 2026-07-07)
		LocalDate today = LocalDate.now();
		PatrolTodayResult todayResult = dashboard01Mapper.selectPatrolToday(
			param.gvCmpnyCd(), param.siteCd(), today.format(DateTimeFormatter.BASIC_ISO_DATE));

		// 2) 월 미이행: 기간 경계는 service 선판정 (미래월/당월 1일 → 쿼리 스킵 후 0)
		//    - 과거월: 월초 ~ 월말 / - 당월: 월초 ~ 어제 / - 미래월: 0
		YearMonth searchYm = YearMonth.parse(param.ym());
		YearMonth currentYm = YearMonth.from(today);

		int monthMissCnt = 0;
		if (searchYm.isBefore(currentYm)) {
			monthMissCnt = dashboard01Mapper.selectPatrolMonthMiss(
				param.gvCmpnyCd()
				, param.siteCd()
				, searchYm.atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE)
				, searchYm.atEndOfMonth().format(DateTimeFormatter.BASIC_ISO_DATE));
		} else if (searchYm.equals(currentYm)) {
			LocalDate yesterday = today.minusDays(1);
			// 오늘이 1일이면 어제가 전월 — 판정 대상일 없음 → 스킵
			if (!yesterday.isBefore(searchYm.atDay(1))) {
				monthMissCnt = dashboard01Mapper.selectPatrolMonthMiss(
					param.gvCmpnyCd()
					, param.siteCd()
					, searchYm.atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE)
					, yesterday.format(DateTimeFormatter.BASIC_ISO_DATE));
			}
		}

		return SafetyPatrolResponse.builder()
			.todayInspectCnt(todayResult.todayInspectCnt())
			.todayTotalCnt(todayResult.todayTotalCnt())
			.monthMissCnt(monthMissCnt)
			.build();
	}

	@Override
	public SafetyRiskResponse selectSafetyRisk(DashSafetyParam param) {
		log.info("대시보드 위험성평가 위젯 조회 진입 - cmpnyCd={}, siteCd={}, ym={}",
			param.gvCmpnyCd(), param.siteCd(), param.ym());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 1) 상태 카운트 2종: 월 조건 없음 (Risk_03 목록 건수 일치 축 — T1 확정)
		RiskStatusCountResult statusCounts =
			dashboard01Mapper.selectRiskStatusCounts(param.gvCmpnyCd(), param.siteCd());

		// 2) 아차사고: 발생일시(OCCUR_DTIME) 기준 조회월 1일~말일 (NearMiss_01 기간필터 축)
		YearMonth searchYm = YearMonth.parse(param.ym());
		int nearMissCnt = dashboard01Mapper.selectNearMissMonthCount(
			param.gvCmpnyCd()
			, param.siteCd()
			, searchYm.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
			, searchYm.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE));

		return SafetyRiskResponse.builder()
			.reviewRequestCnt(statusCounts.reviewRequestCnt())
			.improvePlanCnt(statusCounts.improvePlanCnt())
			.nearMissCnt(nearMissCnt)
			.build();
	}

	@Override
	public SafetyTbmTrendResponse selectSafetyTbmTrend(DashSafetyParam param) {
		log.info("대시보드 TBM 추이 위젯 조회 진입 - cmpnyCd={}, siteCd={}, ym={}",
			param.gvCmpnyCd(), param.siteCd(), param.ym());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 조회월 포함 과거 12개월 범위 (예: ym=2026-07 → 2025-08-01 ~ 2026-07-31)
		YearMonth endYm = YearMonth.parse(param.ym());
		YearMonth startYm = endYm.minusMonths(11);

		List<TbmMonthCntResult> sparseCounts = dashboard01Mapper.selectTbmMonthlyCounts(
			param.gvCmpnyCd()
			, param.siteCd()
			, startYm.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
			, endYm.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE));

		// 희소 결과 → 12포인트 0채움 (과거→조회월 오름차순, FE는 받은 그대로 그린다)
		Map<String, Integer> cntByYm = sparseCounts.stream()
			.collect(Collectors.toMap(TbmMonthCntResult::ym, TbmMonthCntResult::cnt));

		List<TbmMonthCntResult> trend = new ArrayList<>(12);
		for (YearMonth m = startYm; !m.isAfter(endYm); m = m.plusMonths(1)) {
			String ymKey = m.format(DateTimeFormatter.ofPattern("yyyy-MM"));
			trend.add(new TbmMonthCntResult(ymKey, cntByYm.getOrDefault(ymKey, 0)));
		}

		return SafetyTbmTrendResponse.builder()
			.trend(trend)
			.build();
	}

	// ── T2: 근태 탭 A1 근무계획 등록율 + A2 정상/비정상 근무율 ──────

	@Override
	public DashAttdPlanRegRateResponse selectAttdPlanRegRate(DashAttdPlanRegRateParam param) {
		log.info("대시보드 근무계획 등록율 조회 진입 - cmpnyCd={}, siteCd={}, nodeCd={}, workYm={}",
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.workYm());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 부서별 대상/등록 사용자 수 (사용자 모수 = Attd_05 selectUserList 술어 미러)
		List<DashAttdPlanRegRateRowResult> rows = dashboard01Mapper.selectDashAttdPlanRegRate(
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.incSubNodeYn(), param.workYm());

		int totalUserCnt = 0;
		int regUserCnt = 0;
		List<DashAttdPlanRegRateResponse.DeptRate> deptList = new ArrayList<>(rows.size());
		for (DashAttdPlanRegRateRowResult row : rows) {
			totalUserCnt += row.totalUserCnt();
			regUserCnt += row.regUserCnt();
			deptList.add(new DashAttdPlanRegRateResponse.DeptRate(
				row.nodeCd()
				, row.nodeNm()
				, row.totalUserCnt()
				, row.regUserCnt()
				, calcRate(row.regUserCnt(), row.totalUserCnt())));
		}

		// 저조 부서 상단 노출 — 등록율 오름차순, 동률이면 부서명 오름차순
		deptList.sort(Comparator
			.comparingDouble(DashAttdPlanRegRateResponse.DeptRate::regRate)
			.thenComparing(d -> d.nodeNm() == null ? "" : d.nodeNm()));

		return DashAttdPlanRegRateResponse.builder()
			.totalUserCnt(totalUserCnt)
			.regUserCnt(regUserCnt)
			.regRate(calcRate(regUserCnt, totalUserCnt))
			.deptList(deptList)
			.build();
	}

	@Override
	public DashAttdStatusRateResponse selectAttdStatusRate(DashAttdStatusRateParam param) {
		log.info("대시보드 정상/비정상 근무율 조회 진입 - cmpnyCd={}, siteCd={}, nodeCd={}, workYm={}",
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.workYm());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 일 단위 롤업 카운트 (ABSENT > LATE > EARLY_LEAVE > NORMAL — Attd08 판정식 이식)
		//   ★ NF-2b: 확정 부분연차(반차) 보유일은 이 집계에서 제외되어 온다(쿼리의 NOT EXISTS).
		DashAttdStatusCountResult counts = dashboard01Mapper.selectDashAttdStatusCount(
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.incSubNodeYn(), param.workYm());

		// NF-2b: 제외된 반차일을 PartialLeaveWindowUtils 단일 출처로 재판정해 더한다.
		DashAttdStatusCountResult merged = addPartialLeaveDayCounts(param, counts);

		// 합산 정합 방어 (CASE 가 상호배타라 어긋나면 쿼리/데이터 결함 — 경고만 남기고 그대로 응답)
		int sum = merged.normalCnt() + merged.lateCnt() + merged.earlyLeaveCnt() + merged.absentCnt();
		if (sum != merged.targetDayCnt()) {
			log.warn("근태 상태 합산 정합 불일치 - targetDayCnt={}, sum={}", merged.targetDayCnt(), sum);
		}

		// 판정 대상 계획일이 없으면 rate null (FE "판정 대상 근무계획이 없습니다" 표시)
		Double normalRate = merged.targetDayCnt() > 0
			? Double.valueOf(calcRate(merged.normalCnt(), merged.targetDayCnt()))
			: null;

		return DashAttdStatusRateResponse.builder()
			.targetDayCnt(merged.targetDayCnt())
			.normalCnt(merged.normalCnt())
			.lateCnt(merged.lateCnt())
			.earlyLeaveCnt(merged.earlyLeaveCnt())
			.absentCnt(merged.absentCnt())
			.normalRate(normalRate)
			.build();
	}

	/**
	 * NF-2b(2026-08-07): 확정 부분연차(반차) 보유 계획일의 상태를 재판정해 A2 카운트에 합산한다.
	 *
	 * <p><b>왜 SQL 이 아니라 Java 인가</b> — 반차 반영 판정은 연차 시각을 그날 <b>원 스케줄 프레임</b>으로
	 * 정렬해야 하는데(야간 스케줄에서 스케줄 시작보다 이른 시각은 익일), SQL 의 문자열 CONCAT 비교로는
	 * 이 구분이 불가능하다. 산식을 SQL 에 재구현하면 웹 Attd_08/Attd_11·앱과 답이 갈린다(2차 D-1 재발).
	 *
	 * <p>집계 쿼리가 {@code NOT EXISTS} 로 제외한 집합을 원시행 쿼리가 {@code EXISTS} 로 되받으므로
	 * (같은 CTE·같은 조각) 두 집합은 정확히 상보다 — 이중 계상도 누락도 발생하지 않는다.
	 * 일 단위 롤업 규칙(미출근 &gt; 지각 &gt; 조퇴 &gt; 정상)은 쿼리와 동일하다.
	 */
	private DashAttdStatusCountResult addPartialLeaveDayCounts(DashAttdStatusRateParam param,
			DashAttdStatusCountResult base) {

		List<DashPartialLeaveAttdRow> rows = dashboard01Mapper.selectDashPartialLeaveAttdRows(
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.incSubNodeYn(), param.workYm());
		if (rows == null || rows.isEmpty()) {
			return base;
		}

		Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd = new HashMap<>();
		for (DashHalfLeaveWindowRow w : dashboard01Mapper.selectDashPartialLeaveWindows(
				param.gvCmpnyCd(), param.siteCd(), param.workYm())) {
			if (w.userCd() == null || w.workYmd() == null) {
				continue;
			}
			leaveByUserYmd.computeIfAbsent(w.userCd() + "|" + w.workYmd(), k -> new ArrayList<>())
				.add(new PartialLeaveWindowUtils.LeaveWindow(w.startTime(), w.endTime()));
		}

		// (사용자, 근무일) 단위로 접어 차수 상태를 롤업한다(쿼리의 MAX 집계와 동일 의미).
		Map<String, List<DashPartialLeaveAttdRow>> byDay = new LinkedHashMap<>();
		for (DashPartialLeaveAttdRow r : rows) {
			byDay.computeIfAbsent(r.userCd() + "|" + r.workYmd(), k -> new ArrayList<>()).add(r);
		}

		int targetDayCnt = base.targetDayCnt();
		int normalCnt = base.normalCnt();
		int lateCnt = base.lateCnt();
		int earlyLeaveCnt = base.earlyLeaveCnt();
		int absentCnt = base.absentCnt();

		for (Map.Entry<String, List<DashPartialLeaveAttdRow>> e : byDay.entrySet()) {
			List<PartialLeaveWindowUtils.LeaveWindow> leaves = leaveByUserYmd.get(e.getKey());
			if (leaves == null || leaves.isEmpty()) {
				// EXISTS 로 걸러온 날이므로 정상 경로에서는 도달하지 않는다(사업장 이동 등 이례 데이터 방어).
				log.warn("대시보드 A2 반차 구간 미조회(원 스케줄로 판정) - key={}", e.getKey());
			}
			boolean hasCheckIn = false;
			boolean late = false;
			boolean early = false;
			for (DashPartialLeaveAttdRow r : e.getValue()) {
				String status = PartialLeaveWindowUtils.resolveAttdStatus(
					r.workYmd(), r.planStart(), r.planEnd(), leaves,
					r.checkInDate(), r.checkInTime(), r.checkOutDate(), r.checkOutTime());
				if (status == null || PartialLeaveWindowUtils.STATUS_ABSENT.equals(status)) {
					continue; // 그 차수는 출근기록 없음 — 다른 차수가 있으면 그쪽이 판정한다
				}
				hasCheckIn = true;
				if (PartialLeaveWindowUtils.STATUS_LATE.equals(status)) {
					late = true;
				} else if (PartialLeaveWindowUtils.STATUS_EARLY_LEAVE.equals(status)) {
					early = true;
				}
			}
			targetDayCnt++;
			if (!hasCheckIn) {
				absentCnt++;
			} else if (late) {
				lateCnt++;
			} else if (early) {
				earlyLeaveCnt++;
			} else {
				normalCnt++;
			}
		}

		log.info("대시보드 A2 반차일 재판정 - 대상일={}일, targetDayCnt {}→{}",
			byDay.size(), base.targetDayCnt(), targetDayCnt);

		return new DashAttdStatusCountResult(targetDayCnt, normalCnt, lateCnt, earlyLeaveCnt, absentCnt);
	}

	// ── T3: 근태 탭 A3 초과근무 6개월 추이 + A4 법정연차 3분할 ──────

	@Override
	public OvertimeTrendResponse selectOvertimeTrend(OvertimeTrendParam param) {
		log.info("대시보드 초과근무 6개월 추이 조회 진입 - cmpnyCd={}, siteCd={}, nodeCd={}, baseYm={}",
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.baseYm());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 조회월 포함 과거 6개월 범위 (예: baseYm=2026-07 → 2026-02-01 ~ 2026-07-31)
		YearMonth endYm = YearMonth.parse(param.baseYm());
		YearMonth startYm = endYm.minusMonths(5);

		List<OvertimeMonthlyResult> sparseTotals = dashboard01Mapper.selectOvertimeMonthlyTotals(
			param.gvCmpnyCd()
			, param.siteCd()
			, param.nodeCd()
			, param.incSubNodeYn()
			, startYm.atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE)
			, endYm.atEndOfMonth().format(DateTimeFormatter.BASIC_ISO_DATE));

		// 희소 결과(ym='YYYYMM') → 6포인트 0채움 + 'YYYYMM'→'YYYY-MM' 변환 (과거→조회월 오름차순)
		Map<String, Long> minutesByYm = sparseTotals.stream()
			.collect(Collectors.toMap(OvertimeMonthlyResult::ym, OvertimeMonthlyResult::totalMinutes));

		DateTimeFormatter ymBasic = DateTimeFormatter.ofPattern("yyyyMM");
		DateTimeFormatter ymDash = DateTimeFormatter.ofPattern("yyyy-MM");

		List<OvertimeMonthlyResult> monthlyList = new ArrayList<>(6);
		for (YearMonth m = startYm; !m.isAfter(endYm); m = m.plusMonths(1)) {
			long minutes = minutesByYm.getOrDefault(m.format(ymBasic), 0L);
			monthlyList.add(new OvertimeMonthlyResult(m.format(ymDash), minutes));
		}

		return OvertimeTrendResponse.builder()
			.monthlyList(monthlyList)
			.build();
	}

	@Override
	public LeaveUsageResponse selectLeaveUsage(LeaveUsageParam param) {
		log.info("대시보드 법정연차 3분할 조회 진입 - cmpnyCd={}, siteCd={}, nodeCd={}",
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd());

		assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

		// 1) 부여 분모: STATUTORY_% · STATUS IN ('ACTIVE','EXHAUSTED') 합계 (사용기록 없는 부여도 포함 → use 조인 불가, 별도 쿼리)
		BigDecimal grantDays = dashboard01Mapper.selectLeaveGrantTotal(
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

		// 2) 사용/사용예정: GRANT_ID 경유로 동일 부여 집합에 연결된 CONFIRMED 사용분을 오늘 기준 분리
		LeaveUseSplitResult split = dashboard01Mapper.selectLeaveUseSplit(
			param.gvCmpnyCd(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

		// 미사용(부여−사용−예정)은 응답에 넣지 않는다 — FE 파생 계산 (§2-2 확정)
		return LeaveUsageResponse.builder()
			.grantDays(grantDays != null ? grantDays : BigDecimal.ZERO)
			.usedDays(split != null && split.usedDays() != null ? split.usedDays() : BigDecimal.ZERO)
			.plannedDays(split != null && split.plannedDays() != null ? split.plannedDays() : BigDecimal.ZERO)
			.build();
	}

	/** 백분율 계산 (소수 1자리 반올림). 분모 0 이면 0.0 (0 나눗셈 가드). */
	private double calcRate(int numerator, int denominator) {
		if (denominator <= 0) {
			return 0.0;
		}
		return Math.round(numerator * 1000.0 / denominator) / 10.0;
	}

	/**
	 * 무사고 경과일 계산. 기산일 null/파싱 불가 시 null (FE '-' 표시).
	 * 당일 사고면 0일, STR_DATE 미래(비정상 데이터) 방어로 max(0, n).
	 */
	private Integer calcNoAcctDays(String baselineYmd) {
		if (!StringUtils.hasText(baselineYmd)) {
			return null;
		}
		try {
			LocalDate baseline = LocalDate.parse(baselineYmd, DateTimeFormatter.BASIC_ISO_DATE);
			long days = ChronoUnit.DAYS.between(baseline, LocalDate.now());
			return (int) Math.max(0L, days);
		} catch (DateTimeParseException e) {
			// varchar(8) 기산일이 비정상 형식인 경우 방어 — 배너는 '-' 처리
			log.warn("무사고 기산일 파싱 실패 - baselineYmd={}", baselineYmd);
			return null;
		}
	}

	/**
	 * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단). acct01 선례와 동일.
	 * 전사 권한(master/hr/safe)은 전체 허용, 그 외는 tb_user_site_auth 매핑 보유 시 허용.
	 */
	private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
		// 전사 권한(master/hr/safe): 모든 사업장 접근 허용 (prafta-042 전사 스코프 정책)
		if (AuthRoleUtils.canManageAllNodes(authCd)) {
			return;
		}
		// 사업장 미지정이면 사업장 단위 검증 불가 → 차단
		if (!StringUtils.hasText(siteCd)) {
			log.warn("대시보드 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
			throw new ApiException(CommonErrorCode.COMMON_403_001);
		}
		// 그 외: tb_user_site_auth 매핑 보유 시에만 허용
		if (dashboard01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
			log.warn("대시보드 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
			throw new ApiException(CommonErrorCode.COMMON_403_001);
		}
	}
}
