package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.command.LeavePolicyCommand;
import com.prafta.common.cmm.leave.mapper.LeavePolicyMapper;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.AffectedEmployeeBaseVO;
import com.prafta.common.cmm.leave.vo.AffectedEmployeeVO;
import com.prafta.common.cmm.leave.vo.AnalyzeImpactSummaryVO;
import com.prafta.common.cmm.leave.vo.AnalyzeImpactVO;
import com.prafta.common.cmm.leave.vo.ImpactDiffVO;
import com.prafta.common.cmm.leave.vo.ImpactSummaryVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyHistoryVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.PagedResult;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeavePolicyService} 구현체.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md}
 * <ul>
 *   <li>§8.5.2 — 7개 axis 정의 + 회사당 활성 1건 보장</li>
 *   <li>§8.5.3 — Cross-axis 활성 매트릭스 (위반 시 ApiException)</li>
 *   <li>§8.5.7 — 권한 매핑 (AUTH_MASTER OR AUTH_HR_MANAGER)</li>
 *   <li>§8.5.8 — 절대 규칙: 기 부여 보호, 과거 소급 금지</li>
 *   <li>§8.5.9 — 사용 단위 정책 (TB_LEAVE_USAGE_POLICY)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeavePolicyServiceImpl implements LeavePolicyService {

    // ===== axis 코드값 상수 =====
    private static final String AXIS1_HIRE_DATE = "HIRE_DATE";
    private static final String AXIS1_FISCAL_YEAR = "FISCAL_YEAR";

    private static final String AXIS3_MONTHLY_ONLY = "MONTHLY_ONLY";
    private static final String AXIS3_PRORATE = "PRORATE";
    private static final String AXIS3_NEXT_YEAR_BULK = "NEXT_YEAR_BULK";

    private static final String AXIS4_CEIL = "CEIL";
    private static final String AXIS4_ROUND = "ROUND";
    private static final String AXIS4_FLOOR = "FLOOR";
    private static final String AXIS4_HALF_DAY = "HALF_DAY";

    private static final String AXIS5_LEGAL = "LEGAL";
    private static final String AXIS5_CUSTOM = "CUSTOM";

    private static final int AXIS5_LEGAL_START_YEAR = 3;
    private static final int AXIS5_LEGAL_INTERVAL = 2;
    private static final int AXIS5_LEGAL_MIN_MAX_DAYS = 25;

    // 연차 유효기간은 12개월(법정) 고정 (prafta-028: 24개월 연장옵션 폐지)
    private static final int AXIS6_VALIDITY_12 = 12;

    private static final String YN_Y = "Y";
    private static final String YN_N = "N";

    // ===== 사용 단위 (단일, prafta-024) =====
    private static final String USAGE_UNIT_FULL_DAY = "FULL_DAY";
    private static final String USAGE_UNIT_HALF_DAY = "HALF_DAY";
    private static final String USAGE_UNIT_HOUR_2 = "HOUR_2";
    private static final String USAGE_UNIT_HOUR_1 = "HOUR_1";
    private static final String USAGE_UNIT_MIN_30 = "MIN_30";

    // ===== 페이징 한도 =====
    private static final int MAX_PAGE_SIZE = 100;

    // ===== 영향 분석(화면 8) 상수 =====
    /** 활성 직원 조회 상한 (대량 사업장 가드, §9.8) */
    private static final int IMPACT_USER_LIMIT = 500;
    /** 본연차 기본 일수 (§4.7.3) */
    private static final int BASE_ANNUAL_DAYS = 15;
    /** 1년 미만 법정 월차 최대치 (§5.4) */
    private static final int MONTHLY_MAX = 11;

    // ===== CHANGE_TYPE =====
    private static final String CHANGE_TYPE_CREATE = "CREATE";
    private static final String CHANGE_TYPE_UPDATE = "UPDATE";

    private final LeavePolicyMapper leavePolicyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public LeavePolicyVO findActivePolicy(String cmpnyCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return leavePolicyMapper.selectActivePolicy(cmpnyCd);
    }

    @Override
    @Transactional
    public Long createPolicy(String cmpnyCd, LeavePolicyCommand command, String authCd, String userCd) {
        return saveInternal(cmpnyCd, null, command, authCd, userCd, CHANGE_TYPE_CREATE);
    }

    @Override
    @Transactional
    public Long updatePolicy(String cmpnyCd, Long policySeq, LeavePolicyCommand command, String authCd, String userCd) {
        if (policySeq == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return saveInternal(cmpnyCd, policySeq, command, authCd, userCd, CHANGE_TYPE_UPDATE);
    }

    @Override
    public PagedResult<LeavePolicyHistoryVO> findHistory(String cmpnyCd, String authCd, int page, int size) {
        // 정책서 §8.5.7 - 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)만 이력 조회 허용.
        // 이력에 변경자 실명(USER_NM 평문)이 포함되므로 변경/분석 경로와 동일하게 진입부에서 강제(T4-02 보강).
        ensureManager(authCd, cmpnyCd, "history");

        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        int safePage = (page < 1) ? 1 : page;
        int safeSize = (size < 1) ? 20 : Math.min(size, MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        long total = leavePolicyMapper.countPolicyHistory(cmpnyCd);
        List<LeavePolicyHistoryVO> items = leavePolicyMapper.selectPolicyHistory(cmpnyCd, offset, safeSize);

        return PagedResult.<LeavePolicyHistoryVO>builder()
                .page(safePage)
                .size(safeSize)
                .totalCount(total)
                .items(items)
                .build();
    }

    @Override
    public ImpactSummaryVO previewImpact(String cmpnyCd, LeavePolicyCommand command, String authCd) {
        // 정책서 §8.5.7 - 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)만 미리보기 허용
        ensureManager(authCd, cmpnyCd, "impact-preview");

        if (cmpnyCd == null || cmpnyCd.isBlank() || command == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        LeavePolicyVO current = leavePolicyMapper.selectActivePolicy(cmpnyCd);
        // 미리보기 단계에서도 axis 조합 자체는 유효해야 한다 (사용자 화면에서 잘못 띄우지 않도록)
        // 단, APPLY_FROM_DATE 과거 검증은 여기서 수행하지 않는다 (저장 시점에 강제).
        validateAxisMatrix(command, /*enforceFutureApply*/ false);

        return computeImpact(cmpnyCd, current, command);
    }

    @Override
    public AnalyzeImpactVO analyzeImpact(String cmpnyCd, LeavePolicyCommand command, String authCd) {
        // 정책서 §8.5.7 - 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)만 영향 분석 허용
        ensureManager(authCd, cmpnyCd, "analyze-impact");

        if (cmpnyCd == null || cmpnyCd.isBlank() || command == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // axis 조합 자체는 유효해야 한다. 저장이 아니므로 APPLY_FROM_DATE 과거 검증은 하지 않음(§9.10-1은 저장 시점 강제).
        validateAxisMatrix(command, /*enforceFutureApply*/ false);

        LeavePolicyVO current = leavePolicyMapper.selectActivePolicy(cmpnyCd);

        // §9.10-4: 현재 정책과 변경할 정책이 동일하면(변경 axis 0건) 분석 거부.
        // 단 현재 활성 정책이 없으면(신규 도입) "모든 axis 신규"로 간주하여 진행한다.
        List<ImpactDiffVO> diff = buildDiff(current, command);
        boolean anyChange = current == null
                || diff.stream().anyMatch(d -> !"UNCHANGED".equals(d.getChangeType()));
        if (!anyChange) {
            log.info("영향 분석 - 변경 사항 없음. cmpnyCd={}", cmpnyCd);
            throw new ApiException(AttdErrorCode.ATTD_400_021);
        }

        // 활성 직원 1년치 부여 근사 시뮬레이션
        List<AffectedEmployeeBaseVO> users = leavePolicyMapper.selectActiveUsersForImpact(cmpnyCd, IMPACT_USER_LIMIT);
        LocalDate applyDate = parseYyyymmddOrToday(command.applyFromDate());

        List<AffectedEmployeeVO> affected = new ArrayList<>();
        BigDecimal additionalTotal = BigDecimal.ZERO;
        int totalEmployees = users.size();

        for (AffectedEmployeeBaseVO u : users) {
            BigDecimal currentSim = simulateAnnualGrant(u, current, applyDate);
            BigDecimal targetSim = simulateAnnualGrant(u, asVO(command), applyDate);
            BigDecimal expectedAdditional = targetSim.subtract(currentSim);

            // 추가 부여(>0)인 직원만 "영향받는 직원"으로 노출
            if (expectedAdditional.compareTo(BigDecimal.ZERO) > 0) {
                additionalTotal = additionalTotal.add(expectedAdditional);
                affected.add(AffectedEmployeeVO.builder()
                        .userCd(u.getUserCd())
                        .userNm(u.getUserNm())
                        .deptNm(u.getDeptNm())
                        .positionNm(null) // TB_USER 직급 컬럼 없음 (D-4)
                        .hireDate(u.getHireDate())
                        .currentGrant(scale1(u.getCurrentGrant()))
                        .currentUsed(scale1(u.getCurrentUsed()))
                        .expectedAdditional(scale1(expectedAdditional))
                        .mainImpact(determineMainImpact(u, current, command, applyDate, expectedAdditional))
                        .build());
            }
        }

        int affectedCount = affected.size();

        // T4-06 (3.4): "분석 결과 없음" 사유 구분. ① 변경 없음은 위에서 ATTD_400_021 로 이미 차단됨.
        //   ② 대상 직원 없음(입사일 미입력/비활성 → 전원 제외) ③ 대상은 있으나 추가 부여 없음.
        String noResultReason = null;
        if (totalEmployees == 0) {
            noResultReason = "NO_TARGET";
        } else if (affectedCount == 0) {
            noResultReason = "NO_ADDITIONAL";
        }

        AnalyzeImpactSummaryVO summary = AnalyzeImpactSummaryVO.builder()
                .totalEmployees(totalEmployees)
                .affectedCount(affectedCount)
                .normalCount(Math.max(totalEmployees - affectedCount, 0))
                .additionalDaysTotal(scale1(additionalTotal))
                .noResultReason(noResultReason)
                .build();

        log.info("영향 분석 완료(근사). cmpnyCd={}, 전체={}, 영향={}, 추가합계={}, 사유={}",
                cmpnyCd, totalEmployees, affectedCount, additionalTotal, noResultReason);

        return AnalyzeImpactVO.builder()
                .summary(summary)
                .diff(diff)
                .affectedEmployees(affected)
                .currentPolicySummary(buildPolicySummaryFromVO(current))
                .targetPolicySummary(buildPolicySummaryFromCmd(command))
                .build();
    }

    // ============================================================
    // 내부 처리
    // ============================================================

    /**
     * 정책 생성/변경 공통 처리. {@code policySeqIfUpdate}가 null이면 CREATE, 아니면 UPDATE 의미.
     */
    private Long saveInternal(String cmpnyCd, Long policySeqIfUpdate, LeavePolicyCommand command,
                              String authCd, String userCd, String changeType) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || command == null
                || userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 1. 권한 검증 (정책서 §8.5.7)
        ensureManager(authCd, cmpnyCd, changeType);

        // 2. 회사 행 락 (직렬화 보장)
        LeavePolicyVO prev;
        try {
            prev = leavePolicyMapper.selectActivePolicyForUpdate(cmpnyCd);
        } catch (PessimisticLockingFailureException e) {
            // 락 대기 시간 초과 또는 데드락 → 사용자에게 재시도 유도
            log.warn("정책 변경 락 획득 실패. cmpnyCd={}, changeType={}", cmpnyCd, changeType, e);
            throw new ApiException(AttdErrorCode.ATTD_409_010);
        }

        // 3. UPDATE 모드: policySeq가 현재 활성 정책과 일치하는지 확인
        if (CHANGE_TYPE_UPDATE.equals(changeType)) {
            if (prev == null || !Objects.equals(prev.getPolicySeq(), policySeqIfUpdate)) {
                // 활성 정책이 없거나, 활성 정책의 POLICY_SEQ가 요청과 다르다 → 충돌 (동시 변경된 케이스 가능)
                log.warn("UPDATE 대상 정책이 현재 활성 정책과 불일치. cmpnyCd={}, 요청 policySeq={}, 현재 활성={}",
                        cmpnyCd, policySeqIfUpdate, prev == null ? null : prev.getPolicySeq());
                throw new ApiException(AttdErrorCode.ATTD_409_010);
            }
        }

        // 4. Cross-axis 검증 + APPLY_FROM_DATE 검증
        validateAxisMatrix(command, /*enforceFutureApply*/ true);

        // 5. 영향 분석 계산 (HISTORY.IMPACT_SUMMARY로 보존)
        ImpactSummaryVO impact = computeImpact(cmpnyCd, prev, command);

        // 6. 기존 활성 정책 비활성화
        if (prev != null) {
            int updated = leavePolicyMapper.deactivatePolicy(cmpnyCd, prev.getPolicySeq(), userCd);
            if (updated < 1) {
                log.warn("기존 활성 정책 비활성화 실패. cmpnyCd={}, policySeq={}",
                        cmpnyCd, prev.getPolicySeq());
                throw new ApiException(AttdErrorCode.ATTD_409_010);
            }
        }

        // 7. 신규 정책 INSERT (POLICY_SEQ 회수)
        //    UX_TB_LEAVE_POLICY_ACTIVE UNIQUE 위반(동시 INSERT race) 시 ATTD_409_010 매핑.
        LeavePolicyVO newPolicy = buildNewPolicyVO(cmpnyCd, command, userCd);
        try {
            leavePolicyMapper.insertPolicy(newPolicy);
        } catch (DataIntegrityViolationException e) {
            log.warn("활성 정책 UNIQUE 위반 (동시 INSERT race). cmpnyCd={}, changeType={}",
                    cmpnyCd, changeType, e);
            throw new ApiException(AttdErrorCode.ATTD_409_010);
        }
        Long newPolicySeq = newPolicy.getPolicySeq();
        if (newPolicySeq == null) {
            log.error("신규 정책 POLICY_SEQ 회수 실패. cmpnyCd={}", cmpnyCd);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }

        // 8. TB_LEAVE_USAGE_POLICY 1:1 INSERT
        leavePolicyMapper.insertUsagePolicy(newPolicy);

        // 9. TB_LEAVE_POLICY_HISTORY INSERT (PREV/NEW snapshot + IMPACT_SUMMARY)
        LeavePolicyHistoryVO history = buildHistory(cmpnyCd, newPolicySeq, changeType,
                prev, newPolicy, impact, command.changeReason(), userCd);
        leavePolicyMapper.insertPolicyHistory(history);

        log.info("정책 {} 완료. cmpnyCd={}, newPolicySeq={}, prevPolicySeq={}, 수행자={}",
                changeType, cmpnyCd, newPolicySeq, prev == null ? null : prev.getPolicySeq(), userCd);

        return newPolicySeq;
    }

    /**
     * 권한 가드. 정책서 §8.5.7에 따라 AUTH_MASTER 또는 AUTH_HR_MANAGER만 허용.
     *
     * <p>위반 시 보안 민감 응답을 위해 일반화된 403만 반환하고, 상세는 서버 로그에 기록.
     */
    private void ensureManager(String authCd, String cmpnyCd, String operation) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("정책 {} 권한 없음. cmpnyCd={}, authCd={}", operation, cmpnyCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_011);
        }
    }

    // ============================================================
    // Cross-axis 검증 (§8.5.3 매트릭스)
    // ============================================================

    /**
     * 정책서 §8.5.3 Cross-axis 활성 매트릭스에 따른 검증.
     *
     * <p>{@code enforceFutureApply}=true일 때 {@code APPLY_FROM_DATE}가 오늘 미만이면 거부 (§8.5.8 과거 소급 금지).
     *
     * <p>위반 시 {@link AttdErrorCode#ATTD_400_020}을 throw하고, 어느 규칙이 위반되었는지는
     * 서버 로그에만 기록한다.
     */
    private void validateAxisMatrix(LeavePolicyCommand cmd, boolean enforceFutureApply) {
        // 필수값 1차 검증
        requireNonBlank(cmd.policyPreset(), "policyPreset");
        requireNonBlank(cmd.axis1GrantBase(), "axis1GrantBase");
        requireNonBlank(cmd.axis3FirstYearMethod(), "axis3FirstYearMethod");
        requireNonBlank(cmd.axis5TenureMode(), "axis5TenureMode");
        requireNonNull(cmd.axis5StartYear(), "axis5StartYear");
        requireNonNull(cmd.axis5Interval(), "axis5Interval");
        requireNonNull(cmd.axis5MaxDays(), "axis5MaxDays");
        requireNonNull(cmd.axis6ValidityMonths(), "axis6ValidityMonths");
        requireNonBlank(cmd.axis7UsePromotion(), "axis7UsePromotion");
        requireNonBlank(cmd.applyFromDate(), "applyFromDate");

        // AXIS1 값 검증
        if (!AXIS1_HIRE_DATE.equals(cmd.axis1GrantBase()) && !AXIS1_FISCAL_YEAR.equals(cmd.axis1GrantBase())) {
            violation("AXIS1_GRANT_BASE 값이 유효하지 않음: " + cmd.axis1GrantBase());
        }

        // AXIS3 값 검증
        if (!AXIS3_MONTHLY_ONLY.equals(cmd.axis3FirstYearMethod())
                && !AXIS3_PRORATE.equals(cmd.axis3FirstYearMethod())
                && !AXIS3_NEXT_YEAR_BULK.equals(cmd.axis3FirstYearMethod())) {
            violation("AXIS3_FIRST_YEAR_METHOD 값이 유효하지 않음: " + cmd.axis3FirstYearMethod());
        }

        // 매트릭스 #1: AXIS1=HIRE_DATE → AXIS3 ∈ {MONTHLY_ONLY}만 허용
        if (AXIS1_HIRE_DATE.equals(cmd.axis1GrantBase())) {
            if (!AXIS3_MONTHLY_ONLY.equals(cmd.axis3FirstYearMethod())) {
                violation("AXIS1=HIRE_DATE는 AXIS3=MONTHLY_ONLY만 허용 (현재 AXIS3=" + cmd.axis3FirstYearMethod() + ")");
            }
        }

        // 매트릭스(prafta-029): AXIS1=FISCAL_YEAR → AXIS3 ∈ {PRORATE, NEXT_YEAR_BULK}만 허용 (MONTHLY_ONLY 비표준 금지)
        if (AXIS1_FISCAL_YEAR.equals(cmd.axis1GrantBase()) && AXIS3_MONTHLY_ONLY.equals(cmd.axis3FirstYearMethod())) {
            violation("AXIS1=FISCAL_YEAR는 AXIS3=PRORATE/NEXT_YEAR_BULK만 허용 (현재 AXIS3=MONTHLY_ONLY)");
        }

        // 매트릭스: AXIS1=HIRE_DATE → AXIS2 (회계연도 시작월/일)는 의미 없음. NULL이어도 무방.
        // 매트릭스: AXIS1=FISCAL_YEAR → AXIS2 필수
        if (AXIS1_FISCAL_YEAR.equals(cmd.axis1GrantBase())) {
            requireNonBlank(cmd.axis2FiscalStartMm(), "axis2FiscalStartMm (AXIS1=FISCAL_YEAR 시 필수)");
            requireNonBlank(cmd.axis2FiscalStartDd(), "axis2FiscalStartDd (AXIS1=FISCAL_YEAR 시 필수)");
            // 형식 검증 (01~12, 01~31)
            if (!isValidMm(cmd.axis2FiscalStartMm())) {
                violation("AXIS2_FISCAL_START_MM 형식 오류: " + cmd.axis2FiscalStartMm());
            }
            if (!isValidDd(cmd.axis2FiscalStartDd())) {
                violation("AXIS2_FISCAL_START_DD 형식 오류: " + cmd.axis2FiscalStartDd());
            }
        }

        // AXIS3_PREGRANT_YN 값 검증 (Y/N)
        String pregrantYn = (cmd.axis3PregrantYn() == null || cmd.axis3PregrantYn().isBlank())
                ? YN_N : cmd.axis3PregrantYn();
        if (!YN_Y.equals(pregrantYn) && !YN_N.equals(pregrantYn)) {
            violation("AXIS3_PREGRANT_YN 값이 유효하지 않음: " + pregrantYn);
        }

        // 매트릭스 보강: AXIS3=PRORATE 와 AXIS3_PREGRANT_YN='Y' 동시 활성화 금지
        // (사유: PRORATE=회계연도 시점 비례부여, PREGRANT=입사일 시점 일괄선부여 — 첫 해 부여 방식이 상호 모순)
        if (AXIS3_PRORATE.equals(cmd.axis3FirstYearMethod()) && YN_Y.equals(pregrantYn)) {
            violation("AXIS3=PRORATE 와 AXIS3_PREGRANT_YN='Y' 는 동시에 활성화할 수 없습니다.");
        }

        // 매트릭스 #3: AXIS3=PRORATE만 AXIS4 입력 의미. 그 외는 CEIL 강제 (호출 측 입력 무시)
        // 매트릭스 #4: AXIS4=HALF_DAY → ALLOW_HALF_DAY=Y 강제
        // 본 검증은 buildNewPolicyVO에서 정규화하므로 여기서는 PRORATE일 때만 AXIS4 값 자체의 유효성만 본다.
        if (AXIS3_PRORATE.equals(cmd.axis3FirstYearMethod())) {
            String rounding = (cmd.axis4ProrateRounding() == null || cmd.axis4ProrateRounding().isBlank())
                    ? AXIS4_CEIL : cmd.axis4ProrateRounding();
            if (!AXIS4_CEIL.equals(rounding)
                    && !AXIS4_ROUND.equals(rounding)
                    && !AXIS4_FLOOR.equals(rounding)
                    && !AXIS4_HALF_DAY.equals(rounding)) {
                violation("AXIS4_PRORATE_ROUNDING 값이 유효하지 않음: " + rounding);
            }
        }

        // 매트릭스 #5: AXIS5_TENURE_MODE=LEGAL → START_YEAR=3, INTERVAL=2 강제, MAX_DAYS>=25
        // 매트릭스 #6: AXIS5_TENURE_MODE=CUSTOM → 1<=START_YEAR<=3, 1<=INTERVAL<=2, MAX_DAYS>=25
        if (!AXIS5_LEGAL.equals(cmd.axis5TenureMode()) && !AXIS5_CUSTOM.equals(cmd.axis5TenureMode())) {
            violation("AXIS5_TENURE_MODE 값이 유효하지 않음: " + cmd.axis5TenureMode());
        }
        if (cmd.axis5MaxDays() < AXIS5_LEGAL_MIN_MAX_DAYS) {
            violation("AXIS5_MAX_DAYS는 25 이상이어야 함 (법정): " + cmd.axis5MaxDays());
        }
        if (AXIS5_LEGAL.equals(cmd.axis5TenureMode())) {
            // LEGAL은 START_YEAR/INTERVAL을 buildNewPolicyVO에서 강제 (입력 무시).
            // 검증 단계에서는 별도 위반 없음.
        } else {
            // CUSTOM
            if (cmd.axis5StartYear() < 1 || cmd.axis5StartYear() > 3) {
                violation("AXIS5_START_YEAR는 CUSTOM 시 1~3 범위 (현재 " + cmd.axis5StartYear() + ")");
            }
            if (cmd.axis5Interval() < 1 || cmd.axis5Interval() > 2) {
                violation("AXIS5_INTERVAL은 CUSTOM 시 1~2 범위 (현재 " + cmd.axis5Interval() + ")");
            }
        }

        // AXIS6_VALIDITY_MONTHS는 12개월(법정) 고정 (prafta-028: 24개월 연장옵션 폐지)
        if (cmd.axis6ValidityMonths() != AXIS6_VALIDITY_12) {
            violation("AXIS6_VALIDITY_MONTHS는 12(법정)만 허용 (현재 " + cmd.axis6ValidityMonths() + ")");
        }

        // AXIS7 값 검증
        if (!YN_Y.equals(cmd.axis7UsePromotion()) && !YN_N.equals(cmd.axis7UsePromotion())) {
            violation("AXIS7_USE_PROMOTION 값이 유효하지 않음: " + cmd.axis7UsePromotion());
        }

        // 사용 단위(단일, prafta-024): 값이 있으면 화이트리스트여야 한다(공백은 buildNewPolicyVO에서 FULL_DAY로 정규화).
        //   결정 2b: AXIS3=PRORATE + AXIS4=HALF_DAY(0.5일 단위 절사)면 USAGE_UNIT은 HALF_DAY만 허용.
        if (cmd.usageUnit() != null && !cmd.usageUnit().isBlank()) {
            if (!isValidUsageUnit(cmd.usageUnit())) {
                violation("USAGE_UNIT 값이 유효하지 않음: " + cmd.usageUnit());
            }
            boolean effectiveHalfDayRounding = AXIS3_PRORATE.equals(cmd.axis3FirstYearMethod())
                    && AXIS4_HALF_DAY.equals(cmd.axis4ProrateRounding());
            if (effectiveHalfDayRounding && !USAGE_UNIT_HALF_DAY.equals(cmd.usageUnit())) {
                violation("AXIS4=HALF_DAY(0.5일 단위 절사)는 USAGE_UNIT=HALF_DAY만 허용 (현재 " + cmd.usageUnit() + ")");
            }
        }

        // APPLY_FROM_DATE 형식 + 과거 소급 금지 검증
        if (!isValidYyyymmdd(cmd.applyFromDate())) {
            violation("APPLY_FROM_DATE 형식 오류: " + cmd.applyFromDate());
        }
        if (enforceFutureApply) {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            if (cmd.applyFromDate().compareTo(today) < 0) {
                violation("APPLY_FROM_DATE 과거 소급 금지 (요청 " + cmd.applyFromDate() + " < 오늘 " + today + ")");
            }
        }
    }

    /**
     * 검증 통과 후 신규 정책 VO 빌드. Cross-axis 매트릭스 #3, #4, #5 강제 규칙은 본 메서드에서 적용.
     */
    private LeavePolicyVO buildNewPolicyVO(String cmpnyCd, LeavePolicyCommand cmd, String userCd) {
        LeavePolicyVO vo = new LeavePolicyVO();
        vo.setCmpnyCd(cmpnyCd);
        vo.setPolicyPreset(cmd.policyPreset());
        vo.setAxis1GrantBase(cmd.axis1GrantBase());

        // AXIS1=HIRE_DATE이면 AXIS2는 NULL로 정규화 (회계연도 무관)
        if (AXIS1_HIRE_DATE.equals(cmd.axis1GrantBase())) {
            vo.setAxis2FiscalStartMm(null);
            vo.setAxis2FiscalStartDd(null);
        } else {
            vo.setAxis2FiscalStartMm(cmd.axis2FiscalStartMm());
            vo.setAxis2FiscalStartDd(cmd.axis2FiscalStartDd());
        }

        vo.setAxis3FirstYearMethod(cmd.axis3FirstYearMethod());
        vo.setAxis3PregrantYn((cmd.axis3PregrantYn() == null || cmd.axis3PregrantYn().isBlank())
                ? YN_N : cmd.axis3PregrantYn());

        // 매트릭스 #3: AXIS3≠PRORATE → AXIS4='CEIL' 강제 저장 (요청값 무시)
        if (AXIS3_PRORATE.equals(cmd.axis3FirstYearMethod())) {
            vo.setAxis4ProrateRounding((cmd.axis4ProrateRounding() == null || cmd.axis4ProrateRounding().isBlank())
                    ? AXIS4_CEIL : cmd.axis4ProrateRounding());
        } else {
            vo.setAxis4ProrateRounding(AXIS4_CEIL);
        }

        // 매트릭스 #5: AXIS5_TENURE_MODE=LEGAL → START_YEAR=3, INTERVAL=2 강제
        vo.setAxis5TenureMode(cmd.axis5TenureMode());
        if (AXIS5_LEGAL.equals(cmd.axis5TenureMode())) {
            vo.setAxis5StartYear(AXIS5_LEGAL_START_YEAR);
            vo.setAxis5Interval(AXIS5_LEGAL_INTERVAL);
        } else {
            vo.setAxis5StartYear(cmd.axis5StartYear());
            vo.setAxis5Interval(cmd.axis5Interval());
        }
        vo.setAxis5MaxDays(cmd.axis5MaxDays());

        vo.setAxis6ValidityMonths(cmd.axis6ValidityMonths());
        vo.setAxis7UsePromotion(cmd.axis7UsePromotion());

        vo.setUseYn(YN_Y);
        vo.setApplyFromDate(cmd.applyFromDate());
        vo.setInsertNo(userCd);
        vo.setUpdateNo(userCd);

        // TB_LEAVE_USAGE_POLICY 컬럼 (1:1)
        // 사용 단위(단일, prafta-024): AXIS4=HALF_DAY(0.5일 절사)면 HALF_DAY 강제(결정 2b),
        //   그 외에는 화이트리스트 정규화(미지정/비정상 값은 FULL_DAY).
        vo.setUsageUnit(normalizeUsageUnit(cmd.usageUnit(), vo.getAxis4ProrateRounding()));

        // LC-06: 반반차(0.25일) 허용 토글 — USAGE_UNIT 계층과 독립. 미전송/비정상 값은 'N'(fail-closed).
        vo.setAllowQuarter(normalizeYn(cmd.allowQuarter(), YN_N));

        // 법정연차 결재 여부 (prafta-019-E 결정 #2) — 기본 N(즉시 확정)
        vo.setAprvUseYn(normalizeYn(cmd.aprvUseYn(), YN_N));

        return vo;
    }

    // ============================================================
    // IMPACT_SUMMARY 계산
    // ============================================================

    /**
     * 영향 분석. 본 메서드는 단순 근사이며 실제 일배치 부여 결과와 차이가 있을 수 있다.
     *
     * <p>TODO(developer): 정책서 §8.5에 정확한 추가 부여 일수 계산 공식이 명시되어 있지 않다.
     * 현재 구현은 (1) 활성 사용자 수, (2) AXIS5_MAX_DAYS 변경분 × 사용자 수 만으로 추정한다.
     * Attd_09(부여/사용 대시보드) 작업 시 실제 계산 공식이 확정되면 본 메서드 재검토 필요.
     */
    private ImpactSummaryVO computeImpact(String cmpnyCd, LeavePolicyVO current, LeavePolicyCommand cmd) {
        int affectedUserCount = leavePolicyMapper.countActiveUsers(cmpnyCd);

        List<String> axesChanged = diffAxes(current, cmd);

        // 추가 부여 일수 단순 근사: AXIS5_MAX_DAYS 차이 × 사용자 수
        BigDecimal estimatedAdditional = BigDecimal.ZERO;
        if (current != null && cmd.axis5MaxDays() != null && current.getAxis5MaxDays() != null) {
            int diff = cmd.axis5MaxDays() - current.getAxis5MaxDays();
            if (diff > 0) {
                estimatedAdditional = BigDecimal.valueOf((long) diff * affectedUserCount)
                        .setScale(1, RoundingMode.HALF_UP);
            }
        }

        String previewedAt = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return ImpactSummaryVO.builder()
                .affectedUserCount(affectedUserCount)
                .estimatedAdditionalDays(estimatedAdditional)
                .axesChanged(axesChanged)
                .previewedAt(previewedAt)
                .build();
    }

    /**
     * 현재 정책과 신규 command의 axis 컬럼별 차이를 산출.
     * current가 null이면 모든 axis가 "신규"로 기록된다.
     */
    private List<String> diffAxes(LeavePolicyVO current, LeavePolicyCommand cmd) {
        List<String> changed = new ArrayList<>();
        if (current == null) {
            changed.add("AXIS1_GRANT_BASE");
            changed.add("AXIS3_FIRST_YEAR_METHOD");
            changed.add("AXIS5_TENURE_MODE");
            changed.add("AXIS5_MAX_DAYS");
            changed.add("AXIS6_VALIDITY_MONTHS");
            changed.add("AXIS7_USE_PROMOTION");
            return changed;
        }
        if (!Objects.equals(current.getAxis1GrantBase(), cmd.axis1GrantBase())) {
            changed.add("AXIS1_GRANT_BASE");
        }
        if (!Objects.equals(current.getAxis2FiscalStartMm(), cmd.axis2FiscalStartMm())
                || !Objects.equals(current.getAxis2FiscalStartDd(), cmd.axis2FiscalStartDd())) {
            changed.add("AXIS2_FISCAL_START");
        }
        if (!Objects.equals(current.getAxis3FirstYearMethod(), cmd.axis3FirstYearMethod())
                || !Objects.equals(current.getAxis3PregrantYn(), cmd.axis3PregrantYn())) {
            changed.add("AXIS3_FIRST_YEAR_METHOD");
        }
        if (!Objects.equals(current.getAxis4ProrateRounding(), cmd.axis4ProrateRounding())) {
            changed.add("AXIS4_PRORATE_ROUNDING");
        }
        if (!Objects.equals(current.getAxis5TenureMode(), cmd.axis5TenureMode())
                || !Objects.equals(current.getAxis5StartYear(), cmd.axis5StartYear())
                || !Objects.equals(current.getAxis5Interval(), cmd.axis5Interval())) {
            changed.add("AXIS5_TENURE_MODE");
        }
        if (!Objects.equals(current.getAxis5MaxDays(), cmd.axis5MaxDays())) {
            changed.add("AXIS5_MAX_DAYS");
        }
        if (!Objects.equals(current.getAxis6ValidityMonths(), cmd.axis6ValidityMonths())) {
            changed.add("AXIS6_VALIDITY_MONTHS");
        }
        if (!Objects.equals(current.getAxis7UsePromotion(), cmd.axis7UsePromotion())) {
            changed.add("AXIS7_USE_PROMOTION");
        }
        return changed;
    }

    // ============================================================
    // HISTORY 빌드
    // ============================================================

    private LeavePolicyHistoryVO buildHistory(String cmpnyCd, Long policySeq, String changeType,
                                              LeavePolicyVO prev, LeavePolicyVO next,
                                              ImpactSummaryVO impact, String reason, String userCd) {
        LeavePolicyHistoryVO h = new LeavePolicyHistoryVO();
        h.setHistId(leavePolicyMapper.selectNextHistId(cmpnyCd));
        h.setCmpnyCd(cmpnyCd);
        h.setPolicySeq(policySeq);
        h.setChangeType(changeType);
        h.setPrevSnapshot(serializePolicyForSnapshot(prev));
        h.setNewSnapshot(serializePolicyForSnapshot(next));
        h.setChangeReason(reason);
        h.setImpactSummary(serializeImpact(impact));
        h.setInsertNo(userCd);
        return h;
    }

    /**
     * 정책 VO를 snapshot용 JSON 문자열로 직렬화.
     * NULL이면 NULL을 반환 (DB의 PREV_SNAPSHOT은 nullable).
     */
    private String serializePolicyForSnapshot(LeavePolicyVO vo) {
        if (vo == null) {
            return null;
        }
        // LinkedHashMap으로 axis 순서 보존
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("policySeq", vo.getPolicySeq());
        snap.put("cmpnyCd", vo.getCmpnyCd());
        snap.put("policyPreset", vo.getPolicyPreset());
        snap.put("axis1GrantBase", vo.getAxis1GrantBase());
        snap.put("axis2FiscalStartMm", vo.getAxis2FiscalStartMm());
        snap.put("axis2FiscalStartDd", vo.getAxis2FiscalStartDd());
        snap.put("axis3FirstYearMethod", vo.getAxis3FirstYearMethod());
        snap.put("axis3PregrantYn", vo.getAxis3PregrantYn());
        snap.put("axis4ProrateRounding", vo.getAxis4ProrateRounding());
        snap.put("axis5TenureMode", vo.getAxis5TenureMode());
        snap.put("axis5StartYear", vo.getAxis5StartYear());
        snap.put("axis5Interval", vo.getAxis5Interval());
        snap.put("axis5MaxDays", vo.getAxis5MaxDays());
        snap.put("axis6ValidityMonths", vo.getAxis6ValidityMonths());
        snap.put("axis7UsePromotion", vo.getAxis7UsePromotion());
        snap.put("aprvUseYn", vo.getAprvUseYn());
        snap.put("useYn", vo.getUseYn());
        snap.put("applyFromDate", vo.getApplyFromDate());
        snap.put("usageUnit", vo.getUsageUnit());
        snap.put("allowQuarter", vo.getAllowQuarter()); // LC-06: 반반차 토글(이력 스냅샷 보존, additive)
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            log.error("정책 snapshot JSON 직렬화 실패", e);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    private String serializeImpact(ImpactSummaryVO impact) {
        if (impact == null) {
            return null;
        }
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("affectedUserCount", impact.getAffectedUserCount());
        snap.put("estimatedAdditionalDays", impact.getEstimatedAdditionalDays());
        snap.put("axesChanged", impact.getAxesChanged());
        snap.put("previewedAt", impact.getPreviewedAt());
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            log.error("IMPACT_SUMMARY JSON 직렬화 실패", e);
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    // ============================================================
    // 영향 분석(화면 8) - diff / 시뮬레이션 / 주요 영향
    // ============================================================

    /**
     * axis별 변경 사항(diff)을 Baim_07 UI 순서(1,3,4,2,5,6,7)로 산출한다(§9.5, 가드레일 2).
     *
     * <p>비활성 판단(§9.5.2):
     * <ul>
     *   <li>axis2(회계연도 시작일): FISCAL_YEAR 의존 → 한쪽이 HIRE_DATE면 (비활성)/(비활성)→새값</li>
     *   <li>axis4(비례 반올림): PRORATE 의존 → 한쪽이 PRORATE 아니면 (조건부 비활성)</li>
     * </ul>
     *
     * <p>{@code current}가 null이면(신규 도입) 현재값은 빈 값으로 두고 모든 변경 가능 axis를 ACTIVATED/CHANGED로 본다.
     */
    private List<ImpactDiffVO> buildDiff(LeavePolicyVO current, LeavePolicyCommand t) {
        List<ImpactDiffVO> list = new ArrayList<>();

        String curAxis1 = current == null ? null : current.getAxis1GrantBase();
        String curAxis3 = current == null ? null : current.getAxis3FirstYearMethod();

        // 표시 1: axis1 연차 부여 기준
        list.add(simpleDiff(1, "연차 부여 기준",
                axis1Label(curAxis1), axis1Label(t.axis1GrantBase()),
                curAxis1, t.axis1GrantBase()));

        // 표시 2: axis3 입사 첫해 처리 방식
        list.add(simpleDiff(3, "입사 첫해 처리 방식",
                axis3Label(curAxis3), axis3Label(t.axis3FirstYearMethod()),
                curAxis3, t.axis3FirstYearMethod()));

        // 표시 3: axis4 비례 부여 시 반올림 (PRORATE 의존 - 조건부 비활성)
        list.add(conditionalDiff(4, "비례 부여 시 반올림",
                axis4Label(current == null ? null : current.getAxis4ProrateRounding()),
                axis4Label(t.axis4ProrateRounding()),
                AXIS3_PRORATE.equals(curAxis3),
                AXIS3_PRORATE.equals(t.axis3FirstYearMethod()),
                "(조건부 비활성)"));

        // 표시 4: axis2 회계연도 시작일 (FISCAL_YEAR 의존 - 비활성)
        list.add(conditionalDiff(2, "회계연도 시작일",
                fiscalLabel(current == null ? null : current.getAxis2FiscalStartMm(),
                        current == null ? null : current.getAxis2FiscalStartDd()),
                fiscalLabel(t.axis2FiscalStartMm(), t.axis2FiscalStartDd()),
                AXIS1_FISCAL_YEAR.equals(curAxis1),
                AXIS1_FISCAL_YEAR.equals(t.axis1GrantBase()),
                "(비활성)"));

        // 표시 5: axis5 근속 가산 정책 (mode/start/interval/max 중 하나라도 다르면 변경)
        String curAxis5 = tenureSummary(current == null ? null : current.getAxis5TenureMode(),
                current == null ? null : current.getAxis5StartYear(),
                current == null ? null : current.getAxis5Interval(),
                current == null ? null : current.getAxis5MaxDays());
        String tgtAxis5 = tenureSummary(t.axis5TenureMode(), t.axis5StartYear(), t.axis5Interval(), t.axis5MaxDays());
        boolean axis5Changed = current != null
                && (!Objects.equals(current.getAxis5TenureMode(), t.axis5TenureMode())
                || !Objects.equals(current.getAxis5StartYear(), t.axis5StartYear())
                || !Objects.equals(current.getAxis5Interval(), t.axis5Interval())
                || !Objects.equals(current.getAxis5MaxDays(), t.axis5MaxDays()));
        if (current == null || axis5Changed) {
            list.add(ImpactDiffVO.builder().axisNum(5).axisName("근속 가산 정책")
                    .fromValue(curAxis5).toValue(tgtAxis5).changeType(current == null ? "ACTIVATED" : "CHANGED")
                    .note(null).build());
        } else {
            list.add(ImpactDiffVO.builder().axisNum(5).axisName("근속 가산 정책")
                    .fromValue(null).toValue(tgtAxis5).changeType("UNCHANGED").note(tgtAxis5).build());
        }

        // 표시 6: axis6 연차 유효기간
        list.add(simpleDiff(6, "연차 유효기간",
                validityLabel(current == null ? null : current.getAxis6ValidityMonths()),
                validityLabel(t.axis6ValidityMonths()),
                current == null ? null : String.valueOf(current.getAxis6ValidityMonths()),
                String.valueOf(t.axis6ValidityMonths())));

        // 표시 7: axis7 연차 사용촉진 제도
        list.add(simpleDiff(7, "연차 사용촉진 제도",
                promotionLabel(current == null ? null : current.getAxis7UsePromotion()),
                promotionLabel(t.axis7UsePromotion()),
                current == null ? null : current.getAxis7UsePromotion(),
                t.axis7UsePromotion()));

        return list;
    }

    /** 단순 변경 비교 diff 1행 (코드값 동일하면 UNCHANGED). */
    private ImpactDiffVO simpleDiff(int axisNum, String axisName, String fromLabel, String toLabel,
                                    String fromCode, String toCode) {
        boolean changed = !Objects.equals(fromCode, toCode);
        if (!changed) {
            return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                    .fromValue(null).toValue(toLabel).changeType("UNCHANGED").note(toLabel).build();
        }
        // 현재값이 없던(신규) 경우는 ACTIVATED, 그 외 CHANGED
        String type = (fromCode == null || fromCode.isBlank()) ? "ACTIVATED" : "CHANGED";
        return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                .fromValue(fromLabel).toValue(toLabel).changeType(type).note(null).build();
    }

    /**
     * 조건부(의존) axis diff. {@code curActive}/{@code tgtActive}는 의존 조건 충족 여부.
     * - 활성→비활성: DEACTIVATED, toValue=disabledLabel
     * - 비활성→활성: ACTIVATED, fromValue=disabledLabel
     * - 둘 다 활성: 값 비교(simple)
     * - 둘 다 비활성: UNCHANGED(비활성 상태 유지)
     */
    private ImpactDiffVO conditionalDiff(int axisNum, String axisName, String fromLabel, String toLabel,
                                         boolean curActive, boolean tgtActive, String disabledLabel) {
        if (curActive && !tgtActive) {
            return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                    .fromValue(fromLabel).toValue(disabledLabel).changeType("DEACTIVATED").note(null).build();
        }
        if (!curActive && tgtActive) {
            return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                    .fromValue(disabledLabel).toValue(toLabel).changeType("ACTIVATED").note(null).build();
        }
        if (!curActive) {
            // 둘 다 비활성 → 변경 없음(비활성 유지)
            return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                    .fromValue(null).toValue(disabledLabel).changeType("UNCHANGED").note(disabledLabel).build();
        }
        // 둘 다 활성 → 값 비교
        boolean changed = !Objects.equals(fromLabel, toLabel);
        if (!changed) {
            return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                    .fromValue(null).toValue(toLabel).changeType("UNCHANGED").note(toLabel).build();
        }
        return ImpactDiffVO.builder().axisNum(axisNum).axisName(axisName)
                .fromValue(fromLabel).toValue(toLabel).changeType("CHANGED").note(null).build();
    }

    /**
     * 직원 1명의 1년치 부여량 근사 시뮬레이션(§9.8-5, prafta-baim07-impact-001).
     *
     * <p><b>월차(min(monthsSinceHire, 11))는 두 AXIS 정책 공통(법정, AXIS 무관)</b>이라 항상 더하고,
     * <b>본연차 + 근속가산만 AXIS1/AXIS3 매트릭스로 분기</b>한다. 정밀 부여엔진
     * ({@link LeaveGrantEngineServiceImpl#resolveFiscalEntitlement})의 공식을 미러링 복제하여 회계연도
     * 비례/일괄/도래횟수 분기를 반영한다(이전 근사: 회계연도 정책이면 무조건 +15 → 모순 제거).
     *
     * <p>AXIS1=HIRE_DATE는 회계연도 분기에 진입하지 않으므로 AXIS2 NULL이 정상이다. AXIS1=FISCAL_YEAR인데
     * AXIS2 mm/dd가 NULL/파싱실패면 본연차 0 폴백 + WARN(시뮬은 fail-soft — 엔진처럼 throw하지 않는다).
     *
     * @param policy null이면(현재 정책 없음/신규) 0 기준
     */
    private BigDecimal simulateAnnualGrant(AffectedEmployeeBaseVO emp, LeavePolicyVO policy, LocalDate applyDate) {
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        int monthsSinceHire = monthsSinceHire(emp.getHireDate(), applyDate);

        // 월차: 두 정책 공통(법정, AXIS 무관). 1년 미만 경과개월수 근사, 최대 11.
        int monthly = Math.max(0, Math.min(monthsSinceHire, MONTHLY_MAX));
        BigDecimal days = BigDecimal.valueOf(monthly);

        String axis1 = policy.getAxis1GrantBase();
        if (AXIS1_HIRE_DATE.equals(axis1)) {
            // HIRE_DATE: 1년 미만이면 본연차 0(월차만), 1년 이상이면 본연차 15 + 근속가산(floor(months/12)).
            if (monthsSinceHire >= 12) {
                int tenureYears = monthsSinceHire / 12;
                days = days.add(BigDecimal.valueOf(BASE_ANNUAL_DAYS))
                        .add(BigDecimal.valueOf(tenureBonus(policy, tenureYears)));
            }
            return days;
        }

        if (AXIS1_FISCAL_YEAR.equals(axis1)) {
            // FISCAL_YEAR: AXIS2 mm/dd 파싱. NULL/파싱실패면 본연차 0 폴백 + WARN(fail-soft).
            LocalDate hire = parseHireDateOrNull(emp.getHireDate());
            Integer mm = simParseMmOrNull(policy.getAxis2FiscalStartMm());
            Integer dd = simParseDdOrNull(policy.getAxis2FiscalStartDd());
            if (hire == null || mm == null || dd == null) {
                log.warn("영향 분석 시뮬 - FISCAL_YEAR인데 AXIS2(mm/dd) 또는 입사일이 유효하지 않아 본연차 0 폴백. userCd={}, axis2Mm={}, axis2Dd={}",
                        emp.getUserCd(), policy.getAxis2FiscalStartMm(), policy.getAxis2FiscalStartDd());
                return days;
            }

            int crossed = simCountFiscalStartsCrossed(hire, applyDate, mm, dd);
            String axis3 = policy.getAxis3FirstYearMethod();
            BigDecimal annual = simResolveFiscalAnnual(policy, hire, applyDate, mm, dd, crossed, axis3, monthsSinceHire);
            return days.add(annual);
        }

        // AXIS1 미인식(방어): 월차만.
        return days;
    }

    /**
     * FISCAL_YEAR 본연차 + 근속가산 산정(crossed/AXIS3 분기). prafta-baim07-impact-001 D 매트릭스.
     *
     * <p><b>⚠️ {@link LeaveGrantEngineServiceImpl#resolveFiscalEntitlement}(1430~1498행)와 동일 공식의
     * 미러링 복제. 엔진 변경 시 동기화 필요(드리프트 주의).</b> 단, 시뮬은 본연차+근속가산 합을 BigDecimal로
     * 반환할 뿐 컴포넌트 분리/멱등키는 다루지 않는다.
     */
    private BigDecimal simResolveFiscalAnnual(LeavePolicyVO policy, LocalDate hire, LocalDate applyDate,
                                              int mm, int dd, int crossed, String axis3, int monthsSinceHire) {
        if (crossed >= 2) {
            // 회계연도 시작 2회 이상 도래: 본연차 15 + 근속가산(max(crossed, floor(months/12))).
            int tenureYear = Math.max(crossed, monthsSinceHire / 12);
            return BigDecimal.valueOf(BASE_ANNUAL_DAYS)
                    .add(BigDecimal.valueOf(tenureBonus(policy, tenureYear)));
        }
        if (crossed == 1) {
            if (AXIS3_PRORATE.equals(axis3)) {
                // crossed==1 + PRORATE: 전년 부분기 비례 본연차(AXIS4 반올림). 근속가산은 첫 회계연도엔 없음.
                LocalDate currentFiscalStart = simCurrentFiscalStart(applyDate, mm, dd);
                return simComputeProratedAnnualDays(hire, currentFiscalStart, policy.getAxis4ProrateRounding());
            }
            // crossed==1 + NEXT_YEAR_BULK / MONTHLY_ONLY(비표준 잔존, 엔진 1463행과 동일 폴백) / null: 본연차 15 일괄.
            return BigDecimal.valueOf(BASE_ANNUAL_DAYS);
        }
        // crossed==0: 본연차 미부여(월차만).
        return BigDecimal.ZERO;
    }

    /**
     * AXIS3=PRORATE 첫 회계연도 비례 본연차 일수.
     *
     * <p><b>⚠️ {@link LeaveGrantEngineServiceImpl#computeProratedAnnualDays}(1488행)와 동일 공식의
     * 미러링 복제. 엔진 변경 시 동기화 필요(드리프트 주의).</b>
     * 비례 = (입사~currentFiscalStart 일수 ÷ 365) × 15, AXIS4 반올림. 0 이하/365 초과는 방어.
     */
    private BigDecimal simComputeProratedAnnualDays(LocalDate hire, LocalDate currentFiscalStart, String axis4) {
        long partialDays = java.time.temporal.ChronoUnit.DAYS.between(hire, currentFiscalStart);
        if (partialDays <= 0) {
            return BigDecimal.ZERO;
        }
        if (partialDays > 365) {
            partialDays = 365; // 입사~회계연도 시작이 1년 초과인 비정상치 방어(상한 1년분)
        }
        double raw = (partialDays / 365.0) * BASE_ANNUAL_DAYS;
        return simApplyAxis4Rounding(raw, axis4);
    }

    /**
     * AXIS4 반올림(SYS038).
     *
     * <p><b>⚠️ {@link LeaveGrantEngineServiceImpl#applyAxis4Rounding}(1506행)와 동일 공식의 미러링 복제.
     * 엔진 변경 시 동기화 필요(드리프트 주의).</b> CEIL/ROUND/FLOOR/HALF_DAY, 그 외/널은 올림.
     */
    private BigDecimal simApplyAxis4Rounding(double raw, String axis4) {
        if (AXIS4_FLOOR.equals(axis4)) {
            return BigDecimal.valueOf(Math.floor(raw));
        }
        if (AXIS4_ROUND.equals(axis4)) {
            return BigDecimal.valueOf(Math.round(raw));
        }
        if (AXIS4_HALF_DAY.equals(axis4)) {
            // 0.5일 단위 절사(내림): raw 를 0.5 배수로 내림.
            return BigDecimal.valueOf(Math.floor(raw * 2.0) / 2.0);
        }
        // CEIL 및 그 외/널 = 올림
        return BigDecimal.valueOf(Math.ceil(raw));
    }

    /**
     * 입사일 이후 applyDate까지 회계연도 시작일을 넘긴 횟수.
     *
     * <p><b>⚠️ {@link LeaveGrantEngineServiceImpl#countFiscalStartsCrossed}(1861행)와 동일 공식의
     * 미러링 복제. 엔진 변경 시 동기화 필요(드리프트 주의).</b>
     * 입사일이 회계연도 시작일과 같은 날이면 그날을 1회로 센다(이미 도래).
     */
    private int simCountFiscalStartsCrossed(LocalDate hire, LocalDate today, int startMm, int startDd) {
        int count = 0;
        for (int y = hire.getYear(); y <= today.getYear(); y++) {
            LocalDate fs = simSafeMonthDay(y, startMm, startDd);
            if (!fs.isBefore(hire) && !fs.isAfter(today)) {
                count++;
            }
        }
        return count;
    }

    /** applyDate 시점 직전 도래한 회계연도 시작일(같은 날 포함). 엔진 currentFiscalStart(1848행) 미러링. */
    private LocalDate simCurrentFiscalStart(LocalDate today, int startMm, int startDd) {
        LocalDate thisYearStart = simSafeMonthDay(today.getYear(), startMm, startDd);
        if (!thisYearStart.isAfter(today)) {
            return thisYearStart;
        }
        return simSafeMonthDay(today.getYear() - 1, startMm, startDd);
    }

    /**
     * 윤년/말일 보정(02/29 등): 해당 연·월의 마지막 일을 넘으면 말일로 클램프.
     *
     * <p><b>⚠️ {@link LeaveGrantEngineServiceImpl#safeMonthDay}(1875행)와 동일 공식의 미러링 복제.
     * 엔진 변경 시 동기화 필요(드리프트 주의).</b>
     */
    private LocalDate simSafeMonthDay(int year, int mm, int dd) {
        int m = Math.min(Math.max(mm, 1), 12);
        LocalDate first = LocalDate.of(year, m, 1);
        int last = first.lengthOfMonth();
        int d = Math.min(Math.max(dd, 1), last);
        return LocalDate.of(year, m, d);
    }

    /** yyyyMMdd 입사일 문자열 → LocalDate(유효하지 않으면 null, fail-soft). */
    private LocalDate parseHireDateOrNull(String hireDate) {
        if (hireDate == null || hireDate.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(hireDate, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /** AXIS2 MM(01~12) 파싱. 엔진과 달리 NULL/범위초과는 null 반환(E 폴백 트리거). */
    private Integer simParseMmOrNull(String mm) {
        Integer v = parseIntOrNull(mm);
        if (v == null || v < 1 || v > 12) {
            return null;
        }
        return v;
    }

    /** AXIS2 DD(01~31) 파싱. 엔진과 달리 NULL/범위초과는 null 반환(E 폴백 트리거). 말일 보정은 simSafeMonthDay. */
    private Integer simParseDdOrNull(String dd) {
        Integer v = parseIntOrNull(dd);
        if (v == null || v < 1 || v > 31) {
            return null;
        }
        return v;
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 근속 가산 일수 근사(§10.3). {@code year} = 근속 연차(1-based).
     * bonus = year &gt;= start ? floor((year-start)/interval)+1 : 0, 단 (본연차+bonus) &le; maxDays.
     */
    private int tenureBonus(LeavePolicyVO policy, int year) {
        Integer startObj = policy.getAxis5StartYear();
        Integer intervalObj = policy.getAxis5Interval();
        Integer maxObj = policy.getAxis5MaxDays();
        int start = (startObj == null) ? AXIS5_LEGAL_START_YEAR : startObj;
        int interval = (intervalObj == null || intervalObj < 1) ? AXIS5_LEGAL_INTERVAL : intervalObj;
        int maxDays = (maxObj == null) ? AXIS5_LEGAL_MIN_MAX_DAYS : maxObj;
        int bonus = (year >= start) ? ((year - start) / interval) + 1 : 0;
        int total = Math.min(BASE_ANNUAL_DAYS + bonus, maxDays);
        return Math.max(0, total - BASE_ANNUAL_DAYS);
    }

    /**
     * "주요 영향" 메시지 결정(§9.6 우선순위, 단일 메시지). 근사 기반.
     */
    private String determineMainImpact(AffectedEmployeeBaseVO emp, LeavePolicyVO current,
                                       LeavePolicyCommand target, LocalDate applyDate,
                                       BigDecimal expectedAdditional) {
        int monthsSinceHire = monthsSinceHire(emp.getHireDate(), applyDate);
        boolean isUnder1Year = monthsSinceHire < 12;
        String curAxis1 = current == null ? null : current.getAxis1GrantBase();

        // 우선순위 1: 1년 미만 + 회계연도 → 입사일 전환.
        // prafta-baim07-impact-001 F: expectedAdditional이 0이면 "1년차 15일 추가" 문구는 숫자와 모순이므로
        //   추가 부여(>0)일 때만 이 메시지를 낸다(0이면 아래 우선순위/기존 부여 유지로 떨어짐).
        if (isUnder1Year
                && AXIS1_FISCAL_YEAR.equals(curAxis1)
                && AXIS1_HIRE_DATE.equals(target.axis1GrantBase())
                && expectedAdditional != null
                && expectedAdditional.compareTo(BigDecimal.ZERO) > 0) {
            int additionalMonthly = Math.max(0, MONTHLY_MAX - Math.max(0, Math.min(monthsSinceHire, MONTHLY_MAX)));
            return String.format("1년 미만 월차 %d일 + 1년차 %d일 추가 발생", additionalMonthly, BASE_ANNUAL_DAYS);
        }

        // 우선순위 2: 변경 시점 기준 11~12개월 (1년 도래 임박)
        if (monthsSinceHire >= 11 && monthsSinceHire < 12) {
            return String.format("1년차 도래 시 %d일 추가 발생", BASE_ANNUAL_DAYS);
        }

        // 우선순위 3: 회계연도 → 입사일 전환 시 월차 누락분 근사
        if (AXIS1_FISCAL_YEAR.equals(curAxis1) && AXIS1_HIRE_DATE.equals(target.axis1GrantBase())) {
            int missingMonthly = Math.max(0, MONTHLY_MAX - Math.max(0, Math.min(monthsSinceHire, MONTHLY_MAX)));
            if (missingMonthly > 0) {
                return String.format("나머지 월차 %d일 발생", missingMonthly);
            }
        }

        // 우선순위 4: 근속 가산 정책(axis5) 변경 영향
        boolean axis5Changed = current != null
                && (!Objects.equals(current.getAxis5TenureMode(), target.axis5TenureMode())
                || !Objects.equals(current.getAxis5StartYear(), target.axis5StartYear())
                || !Objects.equals(current.getAxis5Interval(), target.axis5Interval())
                || !Objects.equals(current.getAxis5MaxDays(), target.axis5MaxDays()));
        if (axis5Changed && expectedAdditional != null && expectedAdditional.compareTo(BigDecimal.ZERO) != 0) {
            return String.format("근속 가산 정책 변경으로 %s일 차이", expectedAdditional.stripTrailingZeros().toPlainString());
        }

        // 우선순위 5: 기타 - 추가 부여 일수만 표기
        if (expectedAdditional != null && expectedAdditional.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("정책 변경으로 %s일 추가 발생", expectedAdditional.stripTrailingZeros().toPlainString());
        }
        return "기존 부여 유지";
    }

    // ----- 코드값 → 한글 라벨 매핑 -----

    private String axis1Label(String code) {
        if (AXIS1_HIRE_DATE.equals(code)) return "입사일 기준";
        if (AXIS1_FISCAL_YEAR.equals(code)) return "회계연도 기준";
        return code == null ? "-" : code;
    }

    private String axis3Label(String code) {
        if (AXIS3_MONTHLY_ONLY.equals(code)) return "월차만 부여";
        if (AXIS3_PRORATE.equals(code)) return "비례 부여";
        if (AXIS3_NEXT_YEAR_BULK.equals(code)) return "차년도 일괄 부여";
        return code == null ? "-" : code;
    }

    private String axis4Label(String code) {
        if (AXIS4_CEIL.equals(code)) return "올림";
        if (AXIS4_ROUND.equals(code)) return "반올림";
        if (AXIS4_FLOOR.equals(code)) return "내림";
        if (AXIS4_HALF_DAY.equals(code)) return "0.5일 단위 절사";
        return code == null ? "-" : code;
    }

    private String fiscalLabel(String mm, String dd) {
        if (mm == null || mm.isBlank() || dd == null || dd.isBlank()) return "-";
        return String.format("%s월 %s일", stripLeadingZero(mm), stripLeadingZero(dd));
    }

    private String validityLabel(Integer months) {
        if (months == null) return "-";
        return months + "개월";
    }

    private String promotionLabel(String yn) {
        if (YN_Y.equals(yn)) return "사용 (자동 통지)";
        if (YN_N.equals(yn)) return "사용 안 함";
        return yn == null ? "-" : yn;
    }

    private String tenureSummary(String mode, Integer startYear, Integer interval, Integer maxDays) {
        if (mode == null) return "-";
        int s = (startYear == null) ? AXIS5_LEGAL_START_YEAR : startYear;
        int i = (interval == null) ? AXIS5_LEGAL_INTERVAL : interval;
        int m = (maxDays == null) ? AXIS5_LEGAL_MIN_MAX_DAYS : maxDays;
        String modeLabel = AXIS5_LEGAL.equals(mode) ? "법정 기준" : "회사 정책";
        return String.format("%s (n=%d, m=%d, max=%d)", modeLabel, s, i, m);
    }

    /** 현재 정책 VO → 한 줄 요약(예: "회계연도 기준 (비례 부여)"). */
    private String buildPolicySummaryFromVO(LeavePolicyVO vo) {
        if (vo == null) return "현재 활성 정책 없음";
        return policySummary(vo.getAxis1GrantBase(), vo.getAxis3FirstYearMethod());
    }

    private String buildPolicySummaryFromCmd(LeavePolicyCommand cmd) {
        return policySummary(cmd.axis1GrantBase(), cmd.axis3FirstYearMethod());
    }

    private String policySummary(String axis1, String axis3) {
        return axis1Label(axis1) + " (" + axis3Label(axis3) + ")";
    }

    /** command를 시뮬레이션 입력용 LeavePolicyVO로 변환(타깃 정책 표현). */
    private LeavePolicyVO asVO(LeavePolicyCommand cmd) {
        LeavePolicyVO vo = new LeavePolicyVO();
        vo.setAxis1GrantBase(cmd.axis1GrantBase());
        vo.setAxis2FiscalStartMm(cmd.axis2FiscalStartMm());
        vo.setAxis2FiscalStartDd(cmd.axis2FiscalStartDd());
        vo.setAxis3FirstYearMethod(cmd.axis3FirstYearMethod());
        vo.setAxis4ProrateRounding(cmd.axis4ProrateRounding());
        vo.setAxis5TenureMode(cmd.axis5TenureMode());
        vo.setAxis5StartYear(cmd.axis5StartYear());
        vo.setAxis5Interval(cmd.axis5Interval());
        vo.setAxis5MaxDays(cmd.axis5MaxDays());
        vo.setAxis6ValidityMonths(cmd.axis6ValidityMonths());
        vo.setAxis7UsePromotion(cmd.axis7UsePromotion());
        return vo;
    }

    /** HIRE_DATE(YYYYMMDD) ~ applyDate 사이 경과 개월수. 입사일이 없거나 미래면 0. */
    private int monthsSinceHire(String hireDate, LocalDate applyDate) {
        if (hireDate == null || hireDate.length() != 8) return 0;
        try {
            LocalDate hire = LocalDate.parse(hireDate, DateTimeFormatter.BASIC_ISO_DATE);
            if (hire.isAfter(applyDate)) return 0;
            long months = java.time.temporal.ChronoUnit.MONTHS.between(hire, applyDate);
            return (int) Math.max(0, months);
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDate parseYyyymmddOrToday(String ymd) {
        if (ymd != null && ymd.length() == 8) {
            try {
                return LocalDate.parse(ymd, DateTimeFormatter.BASIC_ISO_DATE);
            } catch (Exception ignore) {
                // fallthrough
            }
        }
        return LocalDate.now();
    }

    private BigDecimal scale1(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        return v.setScale(1, RoundingMode.HALF_UP);
    }

    private String stripLeadingZero(String s) {
        if (s == null) return "";
        try {
            return String.valueOf(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return s;
        }
    }

    // ============================================================
    // 유틸
    // ============================================================

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            violation("필수값 누락: " + fieldName);
        }
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            violation("필수값 누락: " + fieldName);
        }
    }

    private void violation(String detailForLog) {
        log.warn("정책 axis 검증 위반: {}", detailForLog);
        throw new ApiException(AttdErrorCode.ATTD_400_020);
    }

    private boolean isValidMm(String mm) {
        if (mm == null || mm.length() != 2) return false;
        try {
            int m = Integer.parseInt(mm);
            return m >= 1 && m <= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDd(String dd) {
        if (dd == null || dd.length() != 2) return false;
        try {
            int d = Integer.parseInt(dd);
            return d >= 1 && d <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidYyyymmdd(String ymd) {
        if (ymd == null || ymd.length() != 8) return false;
        try {
            LocalDate.parse(ymd, DateTimeFormatter.BASIC_ISO_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeYn(String yn, String defaultValue) {
        if (yn == null || yn.isBlank()) return defaultValue;
        if (YN_Y.equals(yn) || YN_N.equals(yn)) return yn;
        return defaultValue;
    }

    /**
     * 사용 단위(단일, prafta-024) 정규화.
     * AXIS4=HALF_DAY(0.5일 단위 절사)면 HALF_DAY 강제(결정 2b).
     * 그 외에는 화이트리스트(FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30) 값만 인정하고,
     * 공백/비정상 값은 FULL_DAY로 정규화한다.
     */
    private String normalizeUsageUnit(String usageUnit, String axis4ProrateRounding) {
        if (AXIS4_HALF_DAY.equals(axis4ProrateRounding)) {
            return USAGE_UNIT_HALF_DAY;
        }
        if (isValidUsageUnit(usageUnit)) {
            return usageUnit;
        }
        return USAGE_UNIT_FULL_DAY;
    }

    private boolean isValidUsageUnit(String u) {
        return USAGE_UNIT_FULL_DAY.equals(u)
                || USAGE_UNIT_HALF_DAY.equals(u)
                || USAGE_UNIT_HOUR_2.equals(u)
                || USAGE_UNIT_HOUR_1.equals(u)
                || USAGE_UNIT_MIN_30.equals(u);
    }
}
