package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prafta.common.cmm.leave.command.ManualGrantCommand;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDashboardService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.util.FiscalYearUtils;
import com.prafta.common.cmm.leave.vo.AppliedLeaveTypeVO;
import com.prafta.common.cmm.leave.vo.HireDateGrantResultVO;
import com.prafta.common.cmm.leave.vo.LeaveBalanceVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardItemVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardMetricsResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardMetricsVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardRowVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailUserHeaderVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailUserVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantHistoryRowVO;
import com.prafta.common.cmm.leave.vo.LeaveGrantInsertVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallResultVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallTargetVO;
import com.prafta.common.cmm.leave.vo.LeaveSummaryVO;
import com.prafta.common.cmm.leave.vo.LeaveTypeAvailTermVO;
import com.prafta.common.cmm.leave.vo.LeaveTypeOptionVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.ManualGrantResultVO;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.leave.vo.PagingMetaVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveDashboardService} 구현체 (PRAFTA-017-2 연차 현황, 정책서 §8.5).
 *
 * <p>법정/법정외 구분은 {@code GRANT_TYPE} prefix(STATUTORY_ / MANUAL_), 활성 부여 정의는
 * STATUS=ACTIVE AND DEL_YN=N. 모든 조회/INSERT는 CMPNY_CD 스코프로 격리한다.
 *
 * <p>근사 한계(D-4): "법적 근속", "다음 부여 예정일"은 정책 계산 의존이라 정밀 부여엔진 미구현
 * 상태에서는 근사로만 산출한다. 산출 불가 시 "-"를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveDashboardServiceImpl implements LeaveDashboardService {

    private static final int MAX_PAGE_SIZE = 100;

    // ===== 수동 부여 고정값 (정책서 §8.5.8) =====
    private static final String MANUAL_GRANT_TYPE = "MANUAL_OTHER";
    /**
     * 관리자 수동 부여 방식 코드 (GRANT_BY_TYPE). 공통코드 SYS043 '02'(관리자 수동 부여)를 사용한다.
     * GRANT_BY_TYPE 컬럼이 varchar(2)이므로 코드값 2자를 저장한다.
     * SYS043: '01'=자동 부여(AUTO) / '02'=관리자 수동 부여(ADMIN) — prafta-017-2 시드.
     */
    private static final String GRANT_BY_TYPE_ADMIN = "02";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_VALIDITY_MONTHS = 12;

    // ===== 타입 "사용 가능 기간"(SYS026) 코드 — 수동부여 AVAIL_TO_DATE 산출 (prafta-045, §8.1.1) =====
    /** 01:설정안함(무기한). AVAIL_TO_DATE 를 sentinel 먼 미래로 적재해 소비창/만료/대시보드 SQL 무변경 유지. */
    private static final String AVAIL_TERM_NONE = "01";
    /** 02:해당 연도 내. AVAIL_TO_DATE = (폼 사용가능일 연도)1231. */
    private static final String AVAIL_TERM_YEAR = "02";
    /** 03:기간 설정. AVAIL_TO_DATE = 타입 ADMIN_AVAIL_TO_DT(YYYYMMDD 절대일). */
    private static final String AVAIL_TERM_PERIOD = "03";
    /**
     * 무기한(01) sentinel 종료일. {@code AVAIL_TO_DATE >= workYmd} 소비창과
     * {@code AVAIL_TO_DATE < today} 만료 판정을 코드/ SQL 변경 없이 안전하게 통과시킨다.
     * (null 대신 sentinel 채택 — planner §3-2 (A)안, 소비 SQL 무변경으로 회귀선 보존.)
     */
    private static final String AVAIL_TO_DATE_FOREVER = "99991231";

    /** 부여 사유 최대 길이 (GRANT_REASON varchar(500)). */
    private static final int MAX_REASON_LENGTH = 500;
    /** 1회 부여 일수 상한(일). 정책 명시값 없어 합리적 기본값 365 적용(보고서 플래그). */
    private static final BigDecimal MAX_GRANT_DAYS = BigDecimal.valueOf(365);
    /**
     * 수동 부여 더블클릭/재전송 중복 차단 시간창(초) — com-013-08-5.
     * 같은 페이로드의 재요청이 이 창 안이면 같은 멱등키가 되어 중복 차단,
     * 창을 넘기면 새 키라 의도적 재부여가 통과한다. 30초는 더블클릭/네트워크 재시도를 충분히 덮는 보수값.
     */
    private static final long MANUAL_DEDUP_WINDOW_SEC = 30L;

    private static final String AXIS1_HIRE_DATE = "HIRE_DATE";
    private static final String AXIS1_FISCAL_YEAR = "FISCAL_YEAR";

    // ===== 수동 부여 연차 회수 (soft cancel, PRAFTA-031) =====
    /** 관리자 수동 부여 식별 prefix(GRANT_TYPE). 회수 대상은 MANUAL_* 만. */
    private static final String MANUAL_GRANT_PREFIX = "MANUAL_";
    private static final String STATUS_CANCELED = "CANCELED";
    /** 알림 유형 [SYS045] — 부여 연차 회수. */
    private static final String NOTI_TYPE_RECALLED = "LEAVE_GRANT_RECALLED";
    private static final String NOTI_CHANNEL_PUSH = "PUSH";
    private static final String NOTI_SEND_STATUS_PENDING = "PENDING";
    /** 회수 알림 제목(한국어). */
    private static final String NOTI_RECALL_TITLE = "부여 연차 회수 안내";

    private final LeaveDashboardMapper leaveDashboardMapper;
    private final LeavePolicyService leavePolicyService;
    private final LeaveGrantEngineService leaveGrantEngineService;
    private final ObjectMapper objectMapper;
    /** LC-07(표기): 현재 기준 1일 환산시간(분) 조회 — FE "N일 H시간 M분" 조립 분모 단일 출처. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;

    // ============================================================
    // 대시보드 목록
    // ============================================================

    @Override
    public LeaveDashboardResultVO getDashboard(String cmpnyCd, String authCd, String siteCd, String nodeCd,
                                               String incSubNodeYn, String userNm, int page, int size) {
        requireCmpnyCd(cmpnyCd);
        // 권한 가드 (정책서 §8.5.7): 대시보드 조회 = MASTER/HR + 사업장 스코프. 전 직원 PII 노출 차단.
        ensureManager(cmpnyCd, authCd, "연차 현황 대시보드 조회");

        int safePage = (page < 1) ? 1 : page;
        int safeSize = (size < 1) ? 20 : Math.min(size, MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        String siteFilter = blankToNull(siteCd);
        String nodeFilter = blankToNull(nodeCd);
        // 하위부서 포함은 nodeCd가 있을 때만 의미 있다. Y만 허용, 그 외는 N (attd08 패턴).
        String incSub = ("Y".equals(incSubNodeYn) && nodeFilter != null) ? "Y" : "N";
        String keyword = blankToNull(userNm);

        long total = leaveDashboardMapper.countDashboardList(cmpnyCd, siteFilter, nodeFilter, incSub, keyword);
        List<LeaveDashboardRowVO> rows = total == 0
                ? new ArrayList<>()
                : leaveDashboardMapper.selectDashboardList(
                        cmpnyCd, siteFilter, nodeFilter, incSub, keyword, offset, safeSize);

        // PC-07(N8): 행별 conv = 대상 사용자의 오늘 기준 개인 분모(480 캡, 미산출 null → FE 480 폴백).
        //   페이지당 최대 100행이라 서비스 루프로 산출(SQL 조인안 대신 — effective-dating 서브쿼리 재사용).
        String todayYmd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        List<LeaveDashboardItemVO> items = new ArrayList<>(rows.size());
        for (LeaveDashboardRowVO row : rows) {
            Integer rowConv = leaveConversionPolicyService.resolvePersonalConvMinutes(
                    cmpnyCd, row.getUserCd(), todayYmd);
            items.add(toItem(row, rowConv));
        }

        LeaveDashboardMetricsResultVO metrics = buildMetrics(
                leaveDashboardMapper.selectDashboardMetrics(cmpnyCd));

        log.info("연차 현황 대시보드 조회. cmpnyCd={}, siteCd={}, nodeCd={}, incSub={}, page={}, size={}, total={}",
                cmpnyCd, siteFilter, nodeFilter, incSub, safePage, safeSize, total);

        return LeaveDashboardResultVO.builder()
                .metrics(metrics)
                .list(items)
                .paging(PagingMetaVO.builder().page(safePage).size(safeSize).totalCount(total).build())
                // PC-03(N8): 목록은 사용자별 분모가 서로 달라 단일 값이 무의미 — 표기 전용 480 폴백
                //   (기존 480 고정과 동일 값, 회귀 0). 행별 convMinutes 는 PC-07 에서 제공한다.
                .convMinutes(LeaveConversionPolicyService.DEFAULT_CONV_MINUTES)
                .build();
    }

    private LeaveDashboardItemVO toItem(LeaveDashboardRowVO row, Integer rowConvMinutes) {
        // usedTotal = 캐시 USED_DAYS 합계(도래+미도래). scheduled = 그 중 미도래분(START_DATE > 오늘).
        // used(도래분) = usedTotal - scheduled. remaining = granted - usedTotal (= granted - (used+scheduled), 기존과 동일).
        BigDecimal legalGranted = nz(row.getLegalGranted());
        BigDecimal legalUsedTotal = nz(row.getLegalUsed());
        BigDecimal legalScheduled = clampScheduled(nz(row.getLegalScheduled()), legalUsedTotal);
        BigDecimal legalUsedArrived = legalUsedTotal.subtract(legalScheduled);

        BigDecimal nonLegalGranted = nz(row.getNonLegalGranted());
        BigDecimal nonLegalUsedTotal = nz(row.getNonLegalUsed());
        BigDecimal nonLegalScheduled = clampScheduled(nz(row.getNonLegalScheduled()), nonLegalUsedTotal);
        BigDecimal nonLegalUsedArrived = nonLegalUsedTotal.subtract(nonLegalScheduled);

        LeaveBalanceVO legal = LeaveBalanceVO.builder()
                .granted(legalGranted)
                .used(legalUsedArrived)
                .scheduled(legalScheduled)
                .remaining(legalGranted.subtract(legalUsedTotal))
                .build();
        LeaveBalanceVO nonLegal = LeaveBalanceVO.builder()
                .granted(nonLegalGranted)
                .used(nonLegalUsedArrived)
                .scheduled(nonLegalScheduled)
                .remaining(nonLegalGranted.subtract(nonLegalUsedTotal))
                .build();

        BigDecimal totalGranted = legalGranted.add(nonLegalGranted);
        BigDecimal totalUsedTotal = legalUsedTotal.add(nonLegalUsedTotal);
        BigDecimal totalScheduled = legalScheduled.add(nonLegalScheduled);
        BigDecimal totalUsedArrived = totalUsedTotal.subtract(totalScheduled);

        LeaveBalanceVO total = LeaveBalanceVO.builder()
                .granted(totalGranted)
                .used(totalUsedArrived)
                .scheduled(totalScheduled)
                .remaining(totalGranted.subtract(totalUsedTotal))
                .build();

        return LeaveDashboardItemVO.builder()
                .userCd(row.getUserCd())
                .userNm(row.getUserNm())
                .deptNm(row.getDeptNm())
                .hireDate(row.getHireDate())
                .employmentType(row.getEmploymentType())
                .tenureText(buildTenureText(row.getHireDate()))
                .creditMonths(row.getCreditMonths() == null ? 0 : row.getCreditMonths())
                .legal(legal)
                .nonLegal(nonLegal)
                .total(total)
                .usageRate(usageRate(totalGranted, totalUsedTotal))
                // 가불 사용분(prafta-com-011-7, 표시 전용 §5). 매퍼 IFNULL로 0 보장이나 방어적 nz.
                .borrowedDays(nz(row.getBorrowedDays()))
                // PC-07(N8): 행별 개인 분모(오늘 기준, 미산출 null — FE 480 폴백).
                .convMinutes(rowConvMinutes)
                .build();
    }

    /**
     * 사용예정(미도래)을 캐시 USED_DAYS 합계 범위로 제한한다(정합성 방어).
     * 사용예정은 USED_DAYS를 구성하는 CONFIRMED 사용분의 부분집합이라 usedTotal을 넘을 수 없으나,
     * 캐시 컬럼 일시 불일치로 음수 사용분이 표시되는 일을 막기 위해 [0, usedTotal] 로 클램프한다.
     */
    private static BigDecimal clampScheduled(BigDecimal scheduled, BigDecimal usedTotal) {
        if (scheduled.signum() < 0) {
            return BigDecimal.ZERO;
        }
        if (scheduled.compareTo(usedTotal) > 0) {
            return usedTotal;
        }
        return scheduled;
    }

    private LeaveDashboardMetricsResultVO buildMetrics(LeaveDashboardMetricsVO m) {
        if (m == null) {
            return LeaveDashboardMetricsResultVO.builder()
                    .totalEmployees(0).avgUsageRate(0).expiringSoon30(0).newGrantThisMonth(0).build();
        }
        return LeaveDashboardMetricsResultVO.builder()
                .totalEmployees(m.getTotalEmployees() == null ? 0 : m.getTotalEmployees())
                .avgUsageRate(usageRate(nz(m.getTotalGranted()), nz(m.getTotalUsed())))
                .expiringSoon30(m.getExpiringSoon30() == null ? 0 : m.getExpiringSoon30())
                .newGrantThisMonth(m.getNewGrantThisMonth() == null ? 0 : m.getNewGrantThisMonth())
                .build();
    }

    // ============================================================
    // 상세
    // ============================================================

    @Override
    public LeaveDetailResultVO getDetail(String cmpnyCd, String authCd, String userCd) {
        requireCmpnyCd(cmpnyCd);
        // 권한 가드 (정책서 §8.5.7): 상세 조회 = MASTER/HR + 사업장 스코프. 특정 직원 PII 노출 차단.
        ensureManager(cmpnyCd, authCd, "연차 상세 조회");
        if (userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        LeaveDetailUserVO u = leaveDashboardMapper.selectDetailUser(cmpnyCd, userCd);
        if (u == null) {
            // 스코프 밖/미존재: 존재 여부를 노출하지 않는 일반 메시지
            log.warn("연차 상세 - 대상 직원 없음/스코프 밖. cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            throw new ApiException(AttdErrorCode.ATTD_404_020);
        }

        BigDecimal legalGranted = nz(u.getLegalGranted());
        BigDecimal legalUsed = nz(u.getLegalUsed());
        BigDecimal nonLegalGranted = nz(u.getNonLegalGranted());
        BigDecimal nonLegalUsed = nz(u.getNonLegalUsed());

        LeaveSummaryVO legalSummary = LeaveSummaryVO.builder()
                .granted(legalGranted).used(legalUsed)
                .remaining(legalGranted.subtract(legalUsed))
                .expiresAt(u.getLegalNearestExpire())
                .build();
        LeaveSummaryVO nonLegalSummary = LeaveSummaryVO.builder()
                .granted(nonLegalGranted).used(nonLegalUsed)
                .remaining(nonLegalGranted.subtract(nonLegalUsed))
                .expiresAt(null)
                .build();

        // D-4: 활성 정책 기반 부여 정책 라벨 + 다음 부여 예정일(근사)
        LeavePolicyVO activePolicy = leavePolicyService.findActivePolicy(cmpnyCd);

        LeaveDetailUserHeaderVO header = LeaveDetailUserHeaderVO.builder()
                .userCd(u.getUserCd())
                .userNm(u.getUserNm())
                .deptNm(u.getDeptNm())
                .employmentType(u.getEmploymentType())
                .hireDate(u.getHireDate())
                .tenureText(buildTenureText(u.getHireDate()))
                .grantPolicyText(buildGrantPolicyText(activePolicy))
                .nextGrantDateText(buildNextGrantDateText(activePolicy, u.getHireDate()))
                .build();

        List<LeaveGrantHistoryRowVO> history = leaveDashboardMapper.selectGrantHistory(cmpnyCd, userCd);

        // 신청형 휴가(LEAVE_TYPE='01') 타입별 잔여 — 법정/법정외와 합산하지 않는 별도 섹션.
        // 당해 회계연도 경계는 활성 정책 AXIS2(시작 월/일)로 산출(NULL이면 01/01 폴백 — 유틸 처리).
        List<AppliedLeaveTypeVO> appliedLeaveTypes = buildAppliedLeaveTypes(cmpnyCd, userCd, activePolicy);

        log.info("연차 상세 조회. cmpnyCd={}, userCd={}, history건수={}, 신청형타입건수={}",
                cmpnyCd, userCd, history.size(), appliedLeaveTypes.size());

        // PC-03(N7·N8): convMinutes = 오늘 기준 "대상 사용자" 개인 분모(480 캡). 산출 불가(교대 등)면
        //   표기 전용 480 폴백(FE formatLeaveDays 폴백과 정합) + 시간차 사용 분 합계(전 기간, additive).
        Integer personalConv = leaveConversionPolicyService.resolvePersonalConvMinutes(
                cmpnyCd, userCd, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        int convMinutes = (personalConv != null) ? personalConv : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;
        Integer hourlyUsedMinutes = leaveDashboardMapper.selectHourlyUsedMinutes(cmpnyCd, userCd);

        return LeaveDetailResultVO.builder()
                .user(header)
                .legalSummary(legalSummary)
                .nonLegalSummary(nonLegalSummary)
                .appliedLeaveTypes(appliedLeaveTypes)
                .grantHistory(history)
                .convMinutes(convMinutes)
                .hourlyUsedMinutes(hourlyUsedMinutes == null ? 0 : hourlyUsedMinutes)
                .build();
    }

    /**
     * 신청형 휴가(사용자 신청 LEAVE_TYPE='01') 타입별 잔여 현황 산출.
     *
     * <p>법정연차(STATUTORY_*)/관리자부여(02) 그룹과 절대 합산하지 않고 타입별 별도 항목으로 반환한다.
     * 각 타입:
     * <ul>
     *   <li>한도 = MAX_APLY_DAYS (NULL이면 한도 0 = 잔여 0, fail-closed).</li>
     *   <li>사용 = 당해 회계연도 CONFIRMED 사용 합계(SQL 산출).</li>
     *   <li>잔여 = 한도 − 사용 (음수면 0 클램프 — 표시 안정성).</li>
     * </ul>
     * 당해 회계연도 경계는 활성 정책 AXIS2 시작 월/일로 {@link FiscalYearUtils}가 산출한다(NULL/공백 → 01/01).
     */
    private List<AppliedLeaveTypeVO> buildAppliedLeaveTypes(String cmpnyCd, String userCd, LeavePolicyVO activePolicy) {
        String startMm = (activePolicy == null) ? null : activePolicy.getAxis2FiscalStartMm();
        String startDd = (activePolicy == null) ? null : activePolicy.getAxis2FiscalStartDd();
        FiscalYearUtils.FiscalWindow window = FiscalYearUtils.fiscalWindow(LocalDate.now(), startMm, startDd);

        List<AppliedLeaveTypeVO> types = leaveDashboardMapper.selectAppliedLeaveTypes(
                cmpnyCd, userCd, window.fiscalStartYmd(), window.fiscalEndYmdExclusive());

        for (AppliedLeaveTypeVO t : types) {
            // 한도 NULL → 0 (fail-closed). 사용은 SQL IFNULL로 0 보장이나 방어적으로 0 처리.
            int limit = (t.getMaxAplyDays() == null) ? 0 : t.getMaxAplyDays();
            int used = (t.getUsedDays() == null) ? 0 : t.getUsedDays();
            int remain = limit - used;
            t.setRemainDays(Math.max(remain, 0));
        }
        return types;
    }

    // ============================================================
    // 수동 부여 가능 휴가 종류
    // ============================================================

    @Override
    public List<LeaveTypeOptionVO> getManualGrantTypes(String cmpnyCd, String authCd) {
        requireCmpnyCd(cmpnyCd);
        // 권한 가드 (정책서 §8.5.7): 수동 부여 화면 보조 조회 = MASTER/HR + 사업장 스코프.
        ensureManager(cmpnyCd, authCd, "수동 부여 가능 휴가 종류 조회");
        return leaveDashboardMapper.selectManualGrantTypes(cmpnyCd);
    }

    // ============================================================
    // 수동 부여 (단일/일괄 공통)
    // ============================================================

    @Override
    @Transactional
    public ManualGrantResultVO manualGrant(String cmpnyCd, ManualGrantCommand command, String authCd, String userCd) {
        requireCmpnyCd(cmpnyCd);
        if (command == null || userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 1. 권한 가드 (정책서 §8.5.7) — 진입부 강제
        ensureManager(cmpnyCd, authCd, "연차 수동 부여");

        // 2. 입력 검증 (서버 권위 — 프론트 1차 검증과 별개)
        List<String> userCds = command.userCds();
        if (userCds == null || userCds.isEmpty()) {
            throw new ApiException(AttdErrorCode.ATTD_400_033);
        }
        String leaveCd = blankToNull(command.leaveCd());
        if (leaveCd == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_030);
        }
        BigDecimal grantDays = command.grantDays();
        if (!isValidGrantDays(grantDays)) {
            throw new ApiException(AttdErrorCode.ATTD_400_031);
        }
        // 부여 일수 상한 (GRANT_DAYS decimal(5,1), 비현실적 대량 부여 방지). 정책 명시값 없어 365일 기본.
        if (grantDays.compareTo(MAX_GRANT_DAYS) > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_035);
        }
        String availFromDate = blankToNull(command.availFromDate());
        if (!isValidYyyymmdd(availFromDate)) {
            throw new ApiException(AttdErrorCode.ATTD_400_032);
        }
        // 부여 사유 길이 검증 (GRANT_REASON varchar(500) — 초과 시 raw 500 방지)
        if (command.reason() != null && command.reason().length() > MAX_REASON_LENGTH) {
            throw new ApiException(AttdErrorCode.ATTD_400_034);
        }

        // 3. leaveCd 화이트리스트 서버 재검증 (수동 부여 가능 휴가 종류인지)
        if (leaveDashboardMapper.countManualGrantType(cmpnyCd, leaveCd) < 1) {
            log.warn("연차 수동 부여 - 허용되지 않은 휴가 종류. cmpnyCd={}, leaveCd={}", cmpnyCd, leaveCd);
            throw new ApiException(AttdErrorCode.ATTD_400_030);
        }

        // 4. AVAIL_TO_DATE 산출 (prafta-045, §8.1.1) — MANUAL_ 전용:
        //    회사 공통 AXIS6 가 아니라 부여 대상 타입의 사용가능기간(SYS026)으로 산출한다.
        //    법정(STATUTORY_*) 경로(LeaveGrantEngineServiceImpl)는 본 변경과 무관하다(§2 회귀선).
        //    AVAIL_FROM_DATE 는 폼 입력(availFromDate)을 그대로 유지한다(§3-3).
        String availToDate = resolveManualAvailToDate(cmpnyCd, leaveCd, availFromDate);

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        BigDecimal grantDaysScaled = grantDays.setScale(1, RoundingMode.HALF_UP);

        // com-013-08-5: 더블클릭/재전송 중복 부여 방어용 결정적 시간창 산출.
        //   같은 제출(동일 페이로드)이 같은 시간창(MANUAL_DEDUP_WINDOW_SEC) 안에 다시 들어오면 멱등키가 동일해진다.
        //   윈도우가 지나거나 페이로드가 다르면 키가 달라져 정상적인 별개 부여는 그대로 통과한다.
        long dedupWindow = System.currentTimeMillis() / 1000L / MANUAL_DEDUP_WINDOW_SEC;

        // 5. 직원당 1건 INSERT (전건 성공 or 전건 롤백 — @Transactional)
        List<String> grantedUserCds = new ArrayList<>(userCds.size());
        for (String targetUserCd : userCds) {
            String tu = blankToNull(targetUserCd);
            if (tu == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_033);
            }
            // 대상 직원이 본 회사 활성 사용자인지 검증 (스코프 격리)
            if (leaveDashboardMapper.countActiveUser(cmpnyCd, tu) < 1) {
                log.warn("연차 수동 부여 - 대상 직원 스코프 밖/미존재. cmpnyCd={}, userCd={}", cmpnyCd, tu);
                throw new ApiException(AttdErrorCode.ATTD_404_020);
            }

            // com-013-08-5: 결정적 멱등키 = {userCd}_{payloadHash}_{window}_MANUAL.
            //   - payloadHash : 부여 페이로드(유형/일수/사용가능일/사유) 해시 → 다른 부여는 다른 키.
            //   - window      : 단기 시간창 버킷 → 동일 페이로드 재전송은 같은 키(중복 차단), 윈도우 경과 후 재부여는 통과.
            String idempotencyKey = buildIdempotencyKey(tu, leaveCd, grantDaysScaled, availFromDate,
                    command.reason(), dedupWindow);

            // 동일 키 live 부여가 이미 있으면 더블클릭/재전송으로 판단하고 차단(별개 부여는 키가 달라 영향 없음).
            if (leaveDashboardMapper.countLiveByIdempotencyKey(cmpnyCd, idempotencyKey) > 0) {
                log.warn("연차 수동 부여 - 동일 제출 단시간 중복 감지(차단). cmpnyCd={}, userCd={}, key={}",
                        cmpnyCd, tu, idempotencyKey);
                throw new ApiException(AttdErrorCode.ATTD_409_030);
            }

            LeaveGrantInsertVO vo = new LeaveGrantInsertVO();
            vo.setGrantId(leaveDashboardMapper.selectNextGrantId(cmpnyCd));
            vo.setCmpnyCd(cmpnyCd);
            vo.setUserCd(tu);
            vo.setLeaveCd(leaveCd);
            vo.setGrantType(MANUAL_GRANT_TYPE);
            vo.setGrantDays(grantDaysScaled);
            vo.setUsedDays(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            vo.setGrantReason(command.reason());
            vo.setGrantByType(GRANT_BY_TYPE_ADMIN);
            vo.setPolicySeq(null);
            vo.setGrantDate(today);
            vo.setAvailFromDate(availFromDate);
            vo.setAvailToDate(availToDate);
            vo.setIdempotencyKey(idempotencyKey);
            vo.setStatus(STATUS_ACTIVE);
            vo.setInsertNo(userCd);

            try {
                leaveDashboardMapper.insertManualGrant(vo);
            } catch (DuplicateKeyException e) {
                // 동시 더블클릭 경합: 위 카운트 검사를 통과한 두 트랜잭션이 같은 키로 동시에 INSERT한 경우.
                //   UNIQUE(CMPNY_CD, IDEMPOTENCY_KEY)가 최종 방어선 → 한쪽만 성공하고 본 트랜잭션은 중복으로 차단.
                log.warn("연차 수동 부여 - 동일 제출 동시 INSERT 경합(차단). cmpnyCd={}, userCd={}, key={}",
                        cmpnyCd, tu, idempotencyKey);
                throw new ApiException(AttdErrorCode.ATTD_409_030);
            }
            grantedUserCds.add(tu);
        }

        log.info("연차 수동 부여 완료. cmpnyCd={}, leaveCd={}, 일수={}, 대상건수={}, 수행자={}",
                cmpnyCd, leaveCd, grantDaysScaled, grantedUserCds.size(), userCd);

        return ManualGrantResultVO.builder()
                .grantedCount(grantedUserCds.size())
                .grantedUserCds(grantedUserCds)
                .build();
    }

    // ============================================================
    // 수동 부여 연차 회수 (soft cancel, PRAFTA-031)
    // ============================================================

    @Override
    @Transactional
    public LeaveRecallResultVO recallGrant(String cmpnyCd, String grantId, String reason, String authCd,
                                           String operatorUserCd) {
        requireCmpnyCd(cmpnyCd);

        // 1. 권한 가드 (정책서 §8.5.7) — 진입부 강제 (MASTER/HR)
        ensureManager(cmpnyCd, authCd, "연차 회수");

        // 2. grantId / 사유 검증
        String safeGrantId = blankToNull(grantId);
        if (safeGrantId == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String safeReason = blankToNull(reason);
        if (safeReason == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_070);
        }
        if (safeReason.length() > MAX_REASON_LENGTH) {
            throw new ApiException(AttdErrorCode.ATTD_400_034);
        }

        // 3. 회수 대상 단건 조회(회사 스코프)
        LeaveRecallTargetVO target = leaveDashboardMapper.selectRecallTarget(cmpnyCd, safeGrantId);
        if (target == null) {
            log.warn("연차 회수 - 대상 부여 없음/스코프 밖. cmpnyCd={}, grantId={}", cmpnyCd, safeGrantId);
            throw new ApiException(AttdErrorCode.ATTD_404_070);
        }

        // 4. 서버 재검증 (정책서 §8.5.8 확정 결정 1/2)
        //    4-1. 관리자 수동 부여건만 회수 가능: GRANT_TYPE LIKE 'MANUAL_%' AND GRANT_BY_TYPE='02'
        boolean isManualType = target.getGrantType() != null && target.getGrantType().startsWith(MANUAL_GRANT_PREFIX);
        if (!isManualType || !GRANT_BY_TYPE_ADMIN.equals(target.getGrantByType())) {
            log.warn("연차 회수 - 수동 부여건 아님. cmpnyCd={}, grantId={}, grantType={}, grantByType={}",
                    cmpnyCd, safeGrantId, target.getGrantType(), target.getGrantByType());
            throw new ApiException(AttdErrorCode.ATTD_400_071);
        }
        //    4-2. ACTIVE 상태만 회수 가능 (이미 취소/만료/소진이면 차단)
        if (!STATUS_ACTIVE.equals(target.getStatus())) {
            log.warn("연차 회수 - ACTIVE 아님. cmpnyCd={}, grantId={}, status={}",
                    cmpnyCd, safeGrantId, target.getStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_070);
        }
        //    4-3. 미사용분만 회수 가능 (USED_DAYS=0 — 부분 회수 없음)
        BigDecimal usedDays = nz(target.getUsedDays());
        if (usedDays.compareTo(BigDecimal.ZERO) != 0) {
            log.warn("연차 회수 - 이미 사용됨. cmpnyCd={}, grantId={}, usedDays={}",
                    cmpnyCd, safeGrantId, usedDays);
            throw new ApiException(AttdErrorCode.ATTD_400_072);
        }

        // 5. soft cancel UPDATE (WHERE에 STATUS='ACTIVE' + DEL_YN='N' 못박아 경합 재확인)
        //    USED_DAYS는 절대 갱신하지 않는다(§8.5.8 #2). row=0이면 경합으로 본다.
        int updated = leaveDashboardMapper.recallGrant(cmpnyCd, safeGrantId, safeReason, operatorUserCd);
        if (updated == 0) {
            log.warn("연차 회수 - 경합으로 갱신 0건(상태 변경 추정). cmpnyCd={}, grantId={}", cmpnyCd, safeGrantId);
            throw new ApiException(AttdErrorCode.ATTD_409_071);
        }

        // 6. 알림 outbox 적재 (발송은 추후 모바일 push). 중복 발송 방지 키 = 'RECALL_'+grantId.
        insertRecallNotiOutbox(cmpnyCd, target, safeReason, operatorUserCd);

        log.info("연차 회수 완료. cmpnyCd={}, grantId={}, 대상직원={}, 수행자={}",
                cmpnyCd, safeGrantId, target.getUserCd(), operatorUserCd);

        return LeaveRecallResultVO.builder()
                .grantId(safeGrantId)
                .status(STATUS_CANCELED)
                .build();
    }

    /**
     * 회수 알림 outbox 1건 적재(PRAFTA-031, 공통 정책서 §10).
     * DATA_PAYLOAD는 Jackson으로 안전하게 직렬화(사유 내 따옴표 이스케이프 위임).
     */
    private void insertRecallNotiOutbox(String cmpnyCd, LeaveRecallTargetVO target, String reason,
                                        String operatorUserCd) {
        NotiOutboxInsertVO vo = new NotiOutboxInsertVO();
        vo.setNotiId(leaveDashboardMapper.selectNextNotiId(cmpnyCd));
        vo.setCmpnyCd(cmpnyCd);
        vo.setSiteCd(null);
        vo.setTargetUserCd(target.getUserCd());
        vo.setNotiType(NOTI_TYPE_RECALLED);
        vo.setChannel(NOTI_CHANNEL_PUSH);
        vo.setTitle(NOTI_RECALL_TITLE);
        vo.setBody(buildRecallNotiBody(target));
        vo.setDataPayload(buildRecallPayload(target, reason));
        vo.setSendStatus(NOTI_SEND_STATUS_PENDING);
        vo.setDedupKey("RECALL_" + target.getGrantId());
        vo.setInsertNo(operatorUserCd);

        leaveDashboardMapper.insertNotiOutbox(vo);
    }

    /** 회수 알림 본문(한국어). 부여 일수를 포함한다. */
    private String buildRecallNotiBody(LeaveRecallTargetVO target) {
        BigDecimal grantDays = nz(target.getGrantDays()).stripTrailingZeros();
        return "관리자가 부여한 연차 " + grantDays.toPlainString() + "일이 회수되었습니다.";
    }

    /** 회수 알림 DATA_PAYLOAD(JSON 문자열). Jackson 직렬화로 사유 내 특수문자 이스케이프. */
    private String buildRecallPayload(LeaveRecallTargetVO target, String reason) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("grantId", target.getGrantId());
        node.put("leaveCd", target.getLeaveCd());
        node.put("grantDays", nz(target.getGrantDays()));
        node.put("reason", reason);
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            // 직렬화 실패는 알림 부가 데이터 손실일 뿐 회수 자체를 막지 않는다. payload는 null로 둔다.
            log.warn("연차 회수 알림 payload 직렬화 실패(무시). grantId={}", target.getGrantId());
            return null;
        }
    }

    // ============================================================
    // 입사일 기준 연차 부여 (테스트/검증용) — 부여 엔진 위임 (prafta-022 작업 A)
    // ============================================================

    @Override
    public HireDateGrantResultVO hireDateGrant(String cmpnyCd, List<String> userCds, String authCd, String operatorUserCd) {
        // 부여 핵심 로직은 공용 부여 엔진으로 이관됨. @Transactional은 엔진 메서드가 보유한다.
        return leaveGrantEngineService.hireDateGrant(cmpnyCd, userCds, authCd, operatorUserCd);
    }

    // ============================================================
    // 근속/정책 텍스트 산출 (D-4 근사)
    // ============================================================

    /** HIRE_DATE(YYYYMMDD) 기준 실제 근속 텍스트("N년 M개월"). 입사일 없거나 미래면 "-". */
    private String buildTenureText(String hireDate) {
        Integer months = monthsBetweenNow(hireDate);
        if (months == null) {
            return "-";
        }
        int years = months / 12;
        int remMonths = months % 12;
        if (years <= 0 && remMonths <= 0) {
            return "0개월";
        }
        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append("년");
        }
        if (remMonths > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(remMonths).append("개월");
        }
        return sb.toString();
    }

    /** 활성 정책 AXIS1 기반 부여 정책 라벨. 정책 없으면 "-". */
    private String buildGrantPolicyText(LeavePolicyVO policy) {
        if (policy == null || policy.getAxis1GrantBase() == null) {
            return "-";
        }
        if (AXIS1_HIRE_DATE.equals(policy.getAxis1GrantBase())) {
            return "입사일 기준";
        }
        if (AXIS1_FISCAL_YEAR.equals(policy.getAxis1GrantBase())) {
            return "회계연도 기준";
        }
        return "-";
    }

    /**
     * 다음 부여 예정일(근사, D-4).
     * <ul>
     *   <li>입사일 기준: 입사일의 다음 anniversary(올해 지났으면 내년).</li>
     *   <li>회계연도 기준: 활성 정책 AXIS2(시작 월/일)의 다음 회계연도 시작일.</li>
     * </ul>
     * 산출 불가(정책 없음/입사일 없음/회계연도 시작값 없음) 시 "-".
     */
    private String buildNextGrantDateText(LeavePolicyVO policy, String hireDate) {
        if (policy == null || policy.getAxis1GrantBase() == null) {
            return "-";
        }
        LocalDate today = LocalDate.now();
        try {
            if (AXIS1_HIRE_DATE.equals(policy.getAxis1GrantBase())) {
                LocalDate hire = parseYyyymmdd(hireDate);
                if (hire == null) {
                    return "-";
                }
                LocalDate next = hire.withYear(today.getYear());
                if (!next.isAfter(today)) {
                    next = next.plusYears(1);
                }
                return formatYmdDash(next);
            }
            if (AXIS1_FISCAL_YEAR.equals(policy.getAxis1GrantBase())) {
                int mm = parseIntSafe(policy.getAxis2FiscalStartMm());
                int dd = parseIntSafe(policy.getAxis2FiscalStartDd());
                if (mm < 1 || mm > 12 || dd < 1 || dd > 31) {
                    return "-";
                }
                LocalDate next = safeDate(today.getYear(), mm, dd);
                if (next == null) {
                    return "-";
                }
                if (!next.isAfter(today)) {
                    LocalDate nextYear = safeDate(today.getYear() + 1, mm, dd);
                    next = (nextYear == null) ? next : nextYear;
                }
                return formatYmdDash(next);
            }
        } catch (Exception e) {
            log.warn("다음 부여 예정일 산출 실패(근사). axis1={}", policy.getAxis1GrantBase());
            return "-";
        }
        return "-";
    }

    // ============================================================
    // 수동 부여 보조
    // ============================================================

    /**
     * 수동 부여(MANUAL_*) 부여건의 AVAIL_TO_DATE 산출 (prafta-045, §8.1.1).
     *
     * <p>부여 대상 타입의 사용가능기간(SYS026 {@code ADMIN_AVAIL_TERM_TYPE})에 따라 산출한다:
     * <ul>
     *   <li>{@code 01} 설정안함(무기한) → sentinel {@code 99991231}(소비창/만료/대시보드 SQL 무변경).</li>
     *   <li>{@code 02} 해당 연도 내 → 폼 사용가능일(availFromDate) 연도의 {@code YYYY1231}.</li>
     *   <li>{@code 03} 기간 설정 → 부여일(availFromDate) + 타입 {@code ADMIN_AVAIL_MONTHS} 개월(prafta-com-016-B).
     *       존재 안 하는 날은 말일 보정. 개월수 미설정/범위밖이면 AXIS6 폴백.</li>
     *   <li>미설정(null)/조회 불가 → 안전 폴백 = 기존 회사 공통 AXIS6(폼 from + validityMonths).</li>
     * </ul>
     * AVAIL_FROM_DATE 는 폼 입력(availFromDate)을 그대로 유지하며, {@code from > to} 모순이면 거부한다.
     * 법정(STATUTORY_*) 부여엔진은 본 산출과 전혀 무관하다(§2 회귀선).
     *
     * @param cmpnyCd       회사 코드(CMPNY_CD 스코프)
     * @param leaveCd       부여 대상 타입 코드(수동 부여 화이트리스트 통과 후)
     * @param availFromDate 폼 입력 사용가능일(YYYYMMDD, 검증 완료)
     * @return 산출된 AVAIL_TO_DATE(YYYYMMDD)
     */
    private String resolveManualAvailToDate(String cmpnyCd, String leaveCd, String availFromDate) {
        LeaveTypeAvailTermVO term = leaveDashboardMapper.selectAdminAvailTerm(cmpnyCd, leaveCd);
        String termType = (term == null) ? null : blankToNull(term.getAdminAvailTermType());

        String availToDate;
        if (AVAIL_TERM_NONE.equals(termType)) {
            // 무기한: sentinel 적재(§3-2 (A)안). from > sentinel 은 자연 성립하지 않으므로 모순검증 생략.
            log.info("수동 부여 사용가능기간 - 설정안함(무기한). cmpnyCd={}, leaveCd={}, availTo={}",
                    cmpnyCd, leaveCd, AVAIL_TO_DATE_FOREVER);
            return AVAIL_TO_DATE_FOREVER;
        } else if (AVAIL_TERM_YEAR.equals(termType)) {
            // 해당 연도 내: 폼 사용가능일 연도의 1231 (availFromDate 는 검증 완료 8자).
            availToDate = availFromDate.substring(0, 4) + "1231";
        } else if (AVAIL_TERM_PERIOD.equals(termType)) {
            // prafta-com-016-B(3-2): 기간 설정 = "부여일로부터 N개월"(상대기간).
            //   만료 = availFromDate(부여일) + ADMIN_AVAIL_MONTHS 개월의 해당일.
            //   존재 안 하는 날(예: 1/31 + 1개월)은 LocalDate.plusMonths 기본 동작(말일 보정)으로 처리.
            //   개월수가 없거나 1~99 범위 밖이면 기존 AXIS6 폴백 유지(하위호환).
            Integer months = (term == null) ? null : term.getAdminAvailMonths();
            if (months == null || months < 1 || months > 99) {
                log.warn("수동 부여 사용가능기간 - '03' 기간설정인데 ADMIN_AVAIL_MONTHS 부적합(1~99 아님), AXIS6 폴백. "
                        + "cmpnyCd={}, leaveCd={}, months={}", cmpnyCd, leaveCd, months);
                return fallbackAxis6AvailToDate(cmpnyCd, availFromDate);
            }
            availToDate = addMonthsYyyymmdd(availFromDate, months);
        } else {
            // 미설정(null)/알 수 없는 코드: 하위호환 폴백 = 기존 AXIS6 산출.
            log.info("수동 부여 사용가능기간 - 미설정/미인식 코드, AXIS6 폴백. cmpnyCd={}, leaveCd={}, termType={}",
                    cmpnyCd, leaveCd, termType);
            return fallbackAxis6AvailToDate(cmpnyCd, availFromDate);
        }

        // from > to 모순 방어 (02/03 한정). YYYYMMDD 8자 문자열 사전식 비교 == 날짜 비교.
        if (availFromDate.compareTo(availToDate) > 0) {
            log.warn("수동 부여 사용가능기간 - from > to 모순. cmpnyCd={}, leaveCd={}, from={}, to={}",
                    cmpnyCd, leaveCd, availFromDate, availToDate);
            throw new ApiException(AttdErrorCode.ATTD_400_032);
        }
        return availToDate;
    }

    /** 하위호환 폴백: 기존 회사 공통 AXIS6(폼 availFromDate + validityMonths) 산출(prafta-045). */
    private String fallbackAxis6AvailToDate(String cmpnyCd, String availFromDate) {
        int validityMonths = resolveValidityMonths(cmpnyCd);
        return addMonthsYyyymmdd(availFromDate, validityMonths);
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

    /**
     * 수동 부여 멱등키 (정책서 §8.5.8, com-013-08-5 결정적 키).
     *
     * <p>형식: {@code {USER_CD}_{PAYLOAD_HASH8}_{WINDOW}_MANUAL}
     * <ul>
     *   <li>PAYLOAD_HASH8 : (leaveCd|grantDays|availFromDate|reason) SHA-256 의 앞 8 hex.
     *       부여 내용이 다르면 키가 달라져 같은 날 서로 다른 부여는 정상 통과한다.</li>
     *   <li>WINDOW : {@link #MANUAL_DEDUP_WINDOW_SEC} 단위 시간 버킷. 동일 페이로드가 같은 윈도우 안에 재전송되면
     *       같은 키가 되어 중복으로 차단되고, 윈도우 경과 후 의도적 재부여는 새 키라 통과한다.</li>
     * </ul>
     * IDEMPOTENCY_KEY varchar(100) 안에 안전히 들어간다(userCd≤20 + hash 8 + window ≤ 약20 + 구분자/접미사).
     */
    private String buildIdempotencyKey(String userCd, String leaveCd, BigDecimal grantDays,
                                       String availFromDate, String reason, long window) {
        String payload = leaveCd + "|" + grantDays.toPlainString() + "|" + availFromDate
                + "|" + (reason == null ? "" : reason);
        String hash8 = sha256Hex8(payload);
        return userCd + "_" + hash8 + "_" + window + "_MANUAL";
    }

    /** 페이로드 문자열의 SHA-256 앞 8 hex (멱등키 식별자용 — 충돌 가능성 무시 가능 수준의 짧은 지문). */
    private String sha256Hex8(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i] & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 JDK 표준 — 사실상 발생 불가. 발생 시 멱등 보장 불가하므로 처리 중단.
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }

    private boolean isValidGrantDays(BigDecimal days) {
        if (days == null) {
            return false;
        }
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        // 1일 단위: 정수만 허용 (소수부가 없어야 함)
        return days.stripTrailingZeros().scale() <= 0;
    }

    private String addMonthsYyyymmdd(String yyyymmdd, int months) {
        LocalDate d = parseYyyymmdd(yyyymmdd);
        if (d == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_032);
        }
        return d.plusMonths(months).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    // ============================================================
    // 공용 유틸
    // ============================================================

    private void requireCmpnyCd(String cmpnyCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /**
     * 관리자(MASTER/HR) 권한 가드 (정책서 §8.5.7). 위반 시 {@link AttdErrorCode#ATTD_403_020}.
     * 대시보드/상세/수동부여 종류 조회 및 수동 부여가 동일 권한 기준을 공유한다.
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

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 사용률(%). granted<=0이면 0. 0~100 clamp + 정수 반올림. */
    private int usageRate(BigDecimal granted, BigDecimal used) {
        if (granted == null || granted.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal rate = nz(used)
                .multiply(BigDecimal.valueOf(100))
                .divide(granted, 0, RoundingMode.HALF_UP);
        int r = rate.intValue();
        if (r < 0) {
            return 0;
        }
        return Math.min(r, 100);
    }

    /** HIRE_DATE(YYYYMMDD) ~ 오늘 경과 개월수. 입사일 없거나 미래면 null. */
    private Integer monthsBetweenNow(String hireDate) {
        LocalDate hire = parseYyyymmdd(hireDate);
        if (hire == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (hire.isAfter(today)) {
            return null;
        }
        long months = ChronoUnit.MONTHS.between(hire, today);
        return (int) Math.max(0, months);
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

    private String formatYmdDash(LocalDate d) {
        return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private int parseIntSafe(String s) {
        if (s == null || s.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }
}
