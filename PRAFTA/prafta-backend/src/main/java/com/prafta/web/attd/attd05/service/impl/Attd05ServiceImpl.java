package com.prafta.web.attd.attd05.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.attd.attd05.application.command.SchTypeCommand;
import com.prafta.web.attd.attd05.application.command.SchTypeDeleCommand;
import com.prafta.web.attd.attd05.application.command.WorkPlanCellDeleCommand;
import com.prafta.web.attd.attd05.application.model.SchTypeDeleModel;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;
import com.prafta.web.attd.attd05.application.model.WorkPlanCellDeleModel;
import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeDeleParam;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeParam;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.application.param.WorkPlanCellDeleParam;
import com.prafta.web.attd.attd05.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd05.application.query.SchListQuery;
import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
import com.prafta.web.attd.attd05.dto.response.DeleteWorkPlanCellsResponse;
import com.prafta.web.attd.attd05.dto.response.LeaveTypeResponse;
import com.prafta.web.attd.attd05.dto.response.SaveUserWorkPlansResponse;
import com.prafta.web.attd.attd05.dto.response.SchTypeListResponse;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;
import com.prafta.web.attd.attd05.mapper.Attd05Mapper;
import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.LeaveTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeUseYnResult;
import com.prafta.web.attd.attd05.result.SchTypeValidMeta;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.SkippedCellResult;
import com.prafta.web.attd.attd05.result.UserResult;
import com.prafta.web.attd.attd05.service.Attd05Service;
import com.prafta.web.attd.leaveflow.vo.DirectLeaveResult;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService.BorrowFamily;
import com.prafta.common.cmm.leave.mapper.LeavePolicyMapper;
import com.prafta.common.cmm.leave.util.FiscalYearUtils;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.web.attd.leaveflow.mapper.LeaveFlowMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd05ServiceImpl implements Attd05Service {

    private final Attd05Mapper attd05Mapper;
    private final com.prafta.web.attd.leaveflow.service.LeaveFlowService leaveFlowService;
    private final com.prafta.web.attd.attd07.service.AttdCloseService attdCloseService;
    /** PRAFTA-COM-008-D: 교대 잠금 가드(공용 cmm 빈 — 교대팀 소속 구간의 근무계획(SCH) 변경/삭제 차단). */
    private final com.prafta.common.cmm.shift.service.ShiftMembershipService shiftMembershipService;
    /** prafta-com-011-6 가불 메타: 가불 한도 projection/만료일 산정(공용 엔진, read-only). */
    private final LeaveGrantEngineService leaveGrantEngineService;
    /** prafta-com-011-6 가불 메타: 입사일 조회(엔진 입력). 웹 leaveflow 매퍼 재사용(중복 신설 금지, 식별값 토큰 도출). */
    private final LeaveFlowMapper leaveFlowMapper;
    /** prafta-com-011-6 가불 메타: 회계연도 윈도우 산출용 활성 정책 조회(락 없음, 조회 메서드 재사용). */
    private final LeavePolicyMapper leavePolicyMapper;
    /** prafta-com-016 공통 스케줄 변경 가드: 확정 연차(USE_UNIT_TYPE 무관)·OT 보유일의 스케줄 변경/삭제 거부 판정. */
    private final com.prafta.common.cmm.schedule.service.ScheduleChangeGuardService scheduleChangeGuardService;
    /** 교차일(앞뒤 근무일) 근무 스케줄 시각 겹침 가드(공용 cmm 빈 — 야간 오버나이트 포함). */
    private final com.prafta.common.cmm.schedule.service.ScheduleOverlapGuardService scheduleOverlapGuardService;
    /** prafta-com-016-C-2: 관리자 연차/월차 직접 등록 통보 PUSH 생산자(afterCommit + REQUIRES_NEW, 묶음 1건). */
    private final com.prafta.common.cmm.push.LeaveDirectSetNotiService leaveDirectSetNotiService;

    /** prafta-com-011-6 본연차 시스템 코드 → BorrowFamily.ANNUAL. */
    private static final String LEAVE_CD_ANNUAL = "SYS_ANNUAL";
    /** prafta-com-011-6 월차 시스템 코드 → BorrowFamily.MONTHLY. */
    private static final String LEAVE_CD_MONTHLY = "SYS_MONTHLY";

    /**
     * prafta-com-016-C-2: 근무계획관리에서 관리자가 직접 지정할 수 있는 휴가 종류 화이트리스트.
     * 법정 본연차(SYS_ANNUAL) + 법정 월차(SYS_MONTHLY) 2종만 허용. 그 외 종류(약정/근속가산 등)는 거부한다.
     * (recordDirectLeaveUsage 는 부여 기반 차감만 허용하므로 사용자 신청('01') 타입은 그쪽에서 자연 거부되나,
     *  지정 UI 가 2종으로 한정되므로 진입부에서 명시적으로 화이트리스트 검증한다.)
     */
    private static final java.util.Set<String> DIRECT_LEAVE_CD_WHITELIST =
            java.util.Set.of(LEAVE_CD_ANNUAL, LEAVE_CD_MONTHLY);

    /**
     * prafta-com-016-C-4: 종류 미지정 "법정 휴가" 자동 적용 시 소멸 임박 통합순 차감 후보(연차→월차 무관).
     * 순서는 정렬 기준(만료 임박)이 결정하므로 목록 순서 자체는 차감 우선순위가 아니다.
     */
    private static final java.util.List<String> AUTO_LEGAL_LEAVE_CDS =
            java.util.List.of(LEAVE_CD_ANNUAL, LEAVE_CD_MONTHLY);

    /** 검증 스킵 사유 코드 - 근무타입 생성일 이전 */
    private static final String REASON_BEFORE_CREATE = "BEFORE_CREATE";
    /** 검증 스킵 사유 코드 - effective USE_YN 이 'N' 인 기간 */
    private static final String REASON_USE_YN_N = "USE_YN_N";
    /** 검증 스킵 사유 코드 - 연차 잔여 부족(직접 차감 불가, prafta-021) */
    private static final String REASON_INSUFFICIENT_LEAVE = "INSUFFICIENT_LEAVE";
    /** 검증 스킵 사유 코드 - (B안) 교대팀 소속 구간의 스케줄 없는 빈 셀(휴무일)에는 연차를 적용할 수 없음.
     *  비교대 사용자의 빈 셀은 종일 연차/월차 적용을 허용한다(스케줄 무관 1일 차감). */
    private static final String REASON_NO_SCHEDULE_FOR_LEAVE = "NO_SCHEDULE_FOR_LEAVE";
    /** 셀 비우기 스킵 사유 코드 - 승인기반 연차(REQ_ID NOT NULL)는 셀에서 직접 삭제 불가 (prafta-com-008-E M2, deprecated by B-5) */
    private static final String REASON_APPROVED_LEAVE = "APPROVED_LEAVE";
    /**
     * 셀 비우기 스킵 사유 코드 - 연차 등록일 셀은 비우기로 삭제할 수 없음 (prafta-com-008-B-5 / D-E8).
     * 전면 동의 원칙: 직접연차(REQ_ID NULL) 포함 모든 종일 연차 셀은 비우기 대상에서 제외하고,
     * 연차 취소는 attd13 동의흐름(연차 변경/삭제 요청)을 셀 개별 진입으로 이용한다.
     */
    private static final String REASON_LEAVE_CELL_CONSENT_REQUIRED = "LEAVE_CELL_CONSENT_REQUIRED";
    /** 검증 스킵 사유 코드 - 해당일 초과근무 보유(등록 or 신청) → 스케줄 변경 불가(앱 스케줄수정요청 게이트와 정합) */
    private static final String REASON_HAS_OVERTIME = "HAS_OVERTIME";
    /** 검증 스킵 사유 코드 - 앞뒤 근무일의 근무 스케줄과 시각이 겹침(야간 오버나이트 포함) → 저장 불가. */
    private static final String REASON_SCHEDULE_OVERLAP = "SCHEDULE_OVERLAP";
    /** prafta-com-016 가드② 스킵 사유 - 해당일 확정 연차(종일/반차/시간차) 보유 → 일반 근무(SCH) 셀로 변경 불가. */
    private static final String REASON_HAS_LEAVE = "HAS_LEAVE";
    /** prafta-com-016-C-3 후속 - 월 부분 삭제에서 종일 확정 연차로 보존(삭제 제외)된 일자 안내 사유. */
    private static final String REASON_LEAVE_PRESERVED = "LEAVE_PRESERVED";
    /** prafta-com-016-C-2 스킵 사유 - 직접 지정 불가 휴가 종류(SYS_ANNUAL/SYS_MONTHLY 외)는 거부. */
    private static final String REASON_LEAVE_CD_NOT_ALLOWED = "LEAVE_CD_NOT_ALLOWED";
    /** 근태 E2E F4(4-13) 스킵 사유 - 해당일 처리 대기 스케줄수정요청(REQ_TYPE='10', 대기) 보유 → 관리자 직접 변경 불가(경합 방지). */
    private static final String REASON_HAS_PENDING_SCH_MODIFY = "HAS_PENDING_SCH_MODIFY";
    /** E3(당일분모 전환) 스킵 사유 - 해당일 미결(승인 대기) 시간차 연차 신청 보유 → 스케줄 변경 불가(당일 분모 보존). */
    private static final String REASON_HAS_PENDING_LEAVE = "HAS_PENDING_LEAVE";

    @Override
    public UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param) {

    	// com-013-04-FU-r19 (High/IDOR): 그리드 "조회"에도 저장/삭제와 대칭으로 노드 관리 권한을 강제한다.
    	//   회사(cmpnyCd)만 토큰 권위였던 기존 경로는 같은 회사 토큰만으로 임의 사업장/부서의 직원 명단·근무계획·
    	//   연차 오버레이 + 복호화 휴대폰까지 노출됐다. master/hr/safe 는 canManageNode 가 canManageAllNodes 로
    	//   전사 통과(prafta-042 정합). 노드 관리자는 본문 nodeCd 가 본인 관리(상위 포함) 노드일 때만 통과 →
    	//   incSubNodeYn=Y 로 상위 노드를 찍어도 countNodeAdmin 의 조상 재귀가 자연 차단(요청 nodeCd 기준 판정).
    	if (!attdCloseService.canManageNode(
    			param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
    		log.warn("근무계획 그리드 조회 권한 없음 - 요청자 userCd={}, authCd={}, siteCd={}, nodeCd={}",
    				param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
    		throw new ApiException(AttdErrorCode.ATTD_403_002);
    	}

    	UserWorkPlansResponse response = null;

    	List<UserResult> userListResultList = attd05Mapper.selectUserList(UserWorkPlansQuery.from(param));

    	List<DayResult> dayResultList = attd05Mapper.selectDayList(UserWorkPlansQuery.from(param));

    	// 소속이동-이력가시성-보정(웹 Attd_05): selectSchedList 가 사용자의 전체 사업장 이력을
    	//   반환하므로(조회 사업장 제한 철회), (userCd, wrkYmd) 당 1건으로 결정론적 병합한다.
    	List<SchedResult> schedResultList =
    			mergeSchedByUserAndDate(attd05Mapper.selectSchedList(UserWorkPlansQuery.from(param)), param.siteCd());

    	// prafta-com-008-E-6: 연차-스케줄 모델 전환 — work_plan 에는 SCH_CD 가 유지되므로
    	//   그리드 "연차" 표시는 leave_use(종일 CONFIRMED) 오버레이로 동반시킨다(셀 렌더 단일 출처).
    	List<com.prafta.web.attd.attd05.result.LeaveOverlayResult> leaveOverlayList =
    			attd05Mapper.selectLeaveOverlayList(UserWorkPlansQuery.from(param));

    	// 부분 휴가(반차/시간차) 오버레이 동반 — 종일과 달리 근무 스케줄이 살아있으므로 셀 값을 덮지 않고
    	//   (userCd, workYmd) 칩으로 사용단위/시간대만 표시한다(종일 오버레이와 분리된 단일 출처).
    	List<com.prafta.web.attd.attd05.result.PartialLeaveOverlayResult> partialLeaveOverlayList =
    			attd05Mapper.selectPartialLeaveOverlayList(UserWorkPlansQuery.from(param));

    	// prafta-com-008-D-5: 교대 잠금 오버레이 동반(D-1 경계 술어 단일출처). 교대팀 소속 구간 SCH 셀은
    	//   프론트가 비활성/자물쇠 표시한다(연차 셀은 활성 — D-3). 저장 차단의 최종 강제는 BE 가드(D-3)가 수행.
    	List<com.prafta.web.attd.attd05.result.ShiftLockOverlayResult> shiftLockOverlayList =
    			attd05Mapper.selectShiftLockOverlayList(UserWorkPlansQuery.from(param));

    	response = UserWorkPlansResponse.builder()
    									.userListResultList(userListResultList)
    									.dayResultList(dayResultList)
    									.schedResultList(schedResultList)
    									.leaveOverlayResultList(leaveOverlayList)
    									.partialLeaveOverlayResultList(partialLeaveOverlayList)
    									.shiftLockOverlayResultList(shiftLockOverlayList)
    									.build();

    	return response;
    }

    /**
     * 소속이동-이력가시성-보정(웹 Attd_05): selectSchedList 가 이제 사용자의 전체 사업장 이력을
     * 반환하므로(조회 사업장 제한 철회), 같은 (userCd, wrkYmd) 에 사업장이 다른 행이 공존할 수
     * 있다 - 소속이동 시 이미 실제 근태가 있어 신 사업장으로 백필되지 않은 과거 근무일이 대표적
     * 케이스. 앱(AppAttd01ServiceImpl.indexSchedule/prefersCandidate)과 동일한 우선순위로
     * 결정론적 1건 병합한다: ① 조회 사업장(sessionSiteCd) 일치 우선 ② 동률이면 effectiveDtime
     * (UPDATE_DATE 우선, 없으면 INSERT_DATE) 최신 우선 ③ 그마저 동률이면 기존 선택 유지.
     */
    private List<SchedResult> mergeSchedByUserAndDate(List<SchedResult> list, String sessionSiteCd) {
    	Map<String, SchedResult> merged = new LinkedHashMap<>();
    	for (SchedResult s : list) {
    		String key = s.userCd() + "|" + s.workYmd();
    		SchedResult current = merged.get(key);
    		if (current == null || prefersSchedCandidate(s, current, sessionSiteCd)) {
    			merged.put(key, s);
    		}
    	}
    	return new ArrayList<>(merged.values());
    }

    /** {@link #mergeSchedByUserAndDate} 후보 우선순위 판정(위 메서드 doc 참조). */
    private boolean prefersSchedCandidate(SchedResult candidate, SchedResult current, String sessionSiteCd) {
    	boolean candidateMatches = sessionSiteCd != null && sessionSiteCd.equals(candidate.siteCd());
    	boolean currentMatches = sessionSiteCd != null && sessionSiteCd.equals(current.siteCd());
    	if (candidateMatches != currentMatches) {
    		return candidateMatches;
    	}
    	if (candidate.effectiveDtime() == null) {
    		return false;
    	}
    	if (current.effectiveDtime() == null) {
    		return true;
    	}
    	return candidate.effectiveDtime().isAfter(current.effectiveDtime());
    }

    @Override
    public SchTypeListResponse getSchTypeList(SchTypeListParam param) {

    	SchListQuery query = SchListQuery.from(param);

    	List<SchTypeResult> schTypeResultList = attd05Mapper.selectSchTypeList(query);

    	// 근무타입(SCH_CD)별 effective-dating 버전 목록을 조회하여 검증 메타로 가공
    	List<SchTypeUseYnResult> useYnList = attd05Mapper.selectSchTypeUseYnList(query);
    	List<SchTypeValidMeta> schTypeValidMetaList = buildSchTypeValidMetaList(useYnList);

    	return SchTypeListResponse.builder()
    							  .schTypeResultList(schTypeResultList)
    							  .schTypeValidMetaList(schTypeValidMetaList)
    							  .build();
    }

    @Override
    public LeaveTypeResponse getLeaveTypeList(LeaveTypeListParam param) {

    	// prafta-com-011-6 가불 메타: 당해 회계연도 윈도우(LEAVE_TYPE='01' 잔여 산정 입력) 산출 — 단일출처 FiscalYearUtils.
    	FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(param.gvCmpnyCd());

    	List<LeaveTypeResult> rows = attd05Mapper.selectLeaveTypeList(
    			LeaveTypeListQuery.from(param, fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive()));

    	// prafta-com-011-6 가불 산정: 시스템 법정 월차(SYS_MONTHLY)/본연차(SYS_ANNUAL) 항목에 한해
    	//   가불 가능 여부/한도/만료일을 앱 selectApplyMeta 와 동일 산정(LeaveGrantEngineService 재사용)으로 채운다.
    	//   입사일은 토큰 도출 userCd 로 1회 조회(식별값 본문 비신뢰, IDOR 방지). 비대상/입사일 미존재면 비가불(quota 0).
    	String hireDate = leaveFlowMapper.selectUserHireDate(param.gvCmpnyCd(), param.gvUserCd());

    	List<LeaveTypeResult> leaveTypeResultList = new ArrayList<>(rows.size());
    	for (LeaveTypeResult row : rows) {

    		boolean borrowable = false;
    		BigDecimal borrowQuota = BigDecimal.ZERO;
    		String borrowExpiryYmd = null;

    		BorrowFamily family = "Y".equalsIgnoreCase(row.systemYn()) ? borrowFamilyOf(row.leaveCd()) : null;
    		if (family != null) {
    			BigDecimal quota = leaveGrantEngineService.computeBorrowQuota(
    					param.gvCmpnyCd(), param.gvUserCd(), hireDate, family);
    			borrowQuota = scaled(quota);
    			borrowable = borrowQuota.signum() > 0;
    			if (borrowable) {
    				borrowExpiryYmd = resolveBorrowExpiryYmd(param.gvCmpnyCd(), param.gvUserCd(), hireDate, family);
    			}
    		}

    		leaveTypeResultList.add(new LeaveTypeResult(
    				row.cmpnyCd()
    				, row.leaveCd()
    				, row.leaveNo()
    				, row.leaveNm()
    				, row.leaveType()
    				, row.aprvUseYn()
    				, row.leaveNatureType()
    				, row.systemYn()
    				, scaled(row.balanceDays())
    				, borrowable
    				, borrowQuota
    				, borrowExpiryYmd
    				// 연차 신청 증빙 필수화(2026-08-29): SQL 원값 그대로 통과(서비스 무가공)
    				, row.evidenceYn()
    				, row.evidenceGuideMsg()
    		));
    	}

    	return LeaveTypeResponse.builder().leaveTypeResultList(leaveTypeResultList).build();
    }

    /**
     * prafta-com-011-6: 당해 회계연도 윈도우 산출(단일출처 {@link FiscalYearUtils}).
     * 활성정책 AXIS2_FISCAL_START_MM/_DD 로 산출하며, 정책 미존재/NULL 이면 1월 1일 폴백(웹 LeaveFlowServiceImpl 동일).
     */
    private FiscalYearUtils.FiscalWindow resolveFiscalWindow(String cmpnyCd) {
    	LeavePolicyVO policy = leavePolicyMapper.selectActivePolicy(cmpnyCd);
    	String mm = (policy == null) ? null : policy.getAxis2FiscalStartMm();
    	String dd = (policy == null) ? null : policy.getAxis2FiscalStartDd();
    	return FiscalYearUtils.fiscalWindow(LocalDate.now(), mm, dd);
    }

    /**
     * prafta-com-011-6 가불 패밀리 판정. 시스템 법정 월차(SYS_MONTHLY)/본연차(SYS_ANNUAL)만 대상.
     * 그 외 leaveCd 는 가불 비대상 → null(가불 미노출). (웹 LeaveFlowServiceImpl.borrowFamilyOf 미러)
     */
    private BorrowFamily borrowFamilyOf(String leaveCd) {
    	if (LEAVE_CD_MONTHLY.equals(leaveCd)) {
    		return BorrowFamily.MONTHLY;
    	}
    	if (LEAVE_CD_ANNUAL.equals(leaveCd)) {
    		return BorrowFamily.ANNUAL;
    	}
    	return null;
    }

    /**
     * prafta-com-011-6: 가불분 만료(소멸)일 YYYYMMDD 산정(FE 표시 + 만료초과 alert 가드용, read-only).
     * 월차 = 입사 + 1년 − 1일(첫해 월차 일괄소멸일). 본연차 = 차기 부여 본연차 정상 만료일(엔진 projection).
     * 입사일 미존재/파싱 불가/산정 불가면 null. (앱 AppLeaveFlowServiceImpl.resolveBorrowExpiryYmd 미러)
     */
    private String resolveBorrowExpiryYmd(String cmpnyCd, String userCd, String hireDate, BorrowFamily family) {
    	if (family == BorrowFamily.MONTHLY) {
    		if (hireDate == null || !hireDate.matches("\\d{8}")) {
    			return null;
    		}
    		try {
    			LocalDate hire = LocalDate.parse(hireDate, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    			return hire.plusYears(1).minusDays(1)
    					.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    		} catch (java.time.format.DateTimeParseException e) {
    			return null;
    		}
    	}
    	// 본연차: 차기 부여 본연차 정상 만료일(엔진 projection 재사용). 산정 불가면 null.
    	return leaveGrantEngineService.projectNextAnnualGrant(cmpnyCd, userCd, hireDate).getAvailToYmd();
    }

    /** prafta-com-011-6: BigDecimal 잔여/한도 소수 1자리 반올림(앱 toScaledDouble 정합). null=0. */
    private BigDecimal scaled(BigDecimal value) {
    	if (value == null) {
    		return BigDecimal.ZERO;
    	}
    	return value.setScale(1, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public SaveUserWorkPlansResponse saveUserWorkPlans(SchTypeParam param) {

    	List<SchTypeModel> modelList = param.schTypeModelList();

    	if (modelList == null || modelList.isEmpty()) {
    		return SaveUserWorkPlansResponse.builder()
    										.savedCount(0)
    										.skippedList(new ArrayList<>())
    										.build();
    	}

    	// 관리 기능 권한 가드 (prafta-041-4) — master/hr 또는 대상 사용자 소속(및 하위) 부서 관리자.
    	// JWT 기반 authCd 사용(body 위조 불가). 대상 사용자별 검증 결과는 캐시하여 셀 반복 시 중복 조회 방지.
    	// 권한 없는 대상이 하나라도 있으면 전체 실패(트랜잭션 롤백, 부분 스킵 아님).
    	Map<String, Boolean> manageCache = new java.util.HashMap<>();

    	// 검증에 필요한 근무타입 effective-dating 버전 목록 조회 (대상 사업장 1건 기준)
    	SchTypeModel firstModel = modelList.get(0);
    	SchListQuery schQuery = new SchListQuery(firstModel.siteCd(), firstModel.gvCmpnyCd());
    	List<SchTypeUseYnResult> useYnList = attd05Mapper.selectSchTypeUseYnList(schQuery);

    	// SCH_CD 별 버전 목록 (APPLY_DATE 오름차순)
    	Map<String, List<SchTypeUseYnResult>> versionMap = groupBySchCd(useYnList);

    	List<SkippedCellResult> skippedList = new ArrayList<>();
    	int savedCount = 0;

    	// prafta-com-016-C-2: 이번 저장에서 연차/월차가 "새로 기록(RECORDED)"된 (userCd → 날짜목록).
    	//   사용자별 묶음 PUSH 1건 발송용. 멱등 중복(SKIPPED_DUP)·스킵은 제외(실제 신규 등록만 통보).
    	Map<String, List<String>> leaveSetByUser = new LinkedHashMap<>();
    	// 사용자별 사업장코드(PUSH siteCd 용 — 같은 사용자의 첫 등록 셀 기준).
    	Map<String, String> siteCdByUser = new java.util.HashMap<>();

    	// 교차일 겹침 가드용 pending 맵 — 같은 저장 배치에서 함께 바뀌는 이웃 날짜의 적용 코드를 DB 보다 우선 반영.
    	//   userCd → (workYmd → 적용 SCH_CD). 휴가 셀/빈값은 "" 로 두어 "그 날 스케줄 없음" 으로 취급한다.
    	//   (배치 내에서 인접 두 날을 동시에 오버나이트로 바꾸는 경우의 겹침도 정확히 잡기 위함.)
    	// prafta-060: 429행(isLegalLeaveCell, prafta-059)과 동일 기준으로 통일한다. 종전에는 이 판정만
    	//   legalLeaveCds(LEAVE_NATURE_TYPE='01' 게이트)를 썼는데, nature='02'(특별휴가류) 코드가 leaveCd 로
    	//   명시돼도 leaveCell=false 로 오판정되어 pendingSchByUser 에 "" 대신 코드값 원문이 그대로 들어갔다.
    	//   그 값이 우연히 실제 SCH_CD 와 같으면(코드값은 공용 컬럼이라 유니크 제약 없음) 겹침가드가 엉뚱한
    	//   근무타입 시간창으로 오탐/미탐할 수 있었다. hasExplicitLeaveCd 단독 기준으로 맞춰 이 결함을 제거한다.
    	Map<String, Map<String, String>> pendingSchByUser = new java.util.HashMap<>();
    	for (SchTypeModel m : modelList) {
    		boolean leaveCell = m.autoLegalLeave()
    				|| (m.leaveCd() != null && !m.leaveCd().isEmpty());
    		String schForNeighbor = (leaveCell || m.workPlanCd() == null) ? "" : m.workPlanCd();
    		pendingSchByUser.computeIfAbsent(m.userCd(), k -> new java.util.HashMap<>())
    				.put(m.workYmd(), schForNeighbor);
    	}

    	for (SchTypeModel model : modelList) {

    		// PRAFTA-041-4 - 대상 사용자 관리 권한 검증 (master/hr 즉시 통과, 그 외 노드 관리자 스코프)
    		ensureCanManageTargetUser(param.gvAuthCd(), model.gvUserCd(), model.gvCmpnyCd(),
    				model.siteCd(), model.userCd(), manageCache);

    		// PRAFTA-028 - 마감된 기간(부서)의 근무타입 변경(저장) 차단
    		String closeYm = (model.workYmd() != null && model.workYmd().length() >= 6)
    				? model.workYmd().substring(0, 6) : model.workYmd();
    		if (attdCloseService.isClosedForUser(model.gvCmpnyCd(), model.siteCd(), model.userCd(), closeYm)) {
    			throw new ApiException(AttdErrorCode.ATTD_400_042);
    		}

    		String workPlanCd = model.workPlanCd();

    		// prafta-com-016-C-2: 연차 셀 여부 판정 — 셀 값(workPlanCd)이 법정 휴가 코드이거나, 명시 leaveCd 가 온 경우.
    		//   효과적 연차코드(effectiveLeaveCd) = 명시 leaveCd 우선, 없으면 workPlanCd.
    		// prafta-com-016-C-4: 종류 미지정 자동 적용(autoLegalLeave) 셀도 연차 셀로 취급한다(차감은 자동 통합순).
    		boolean isAutoLegal = model.autoLegalLeave();
    		// ★ WORK_PLAN_CD 는 SCH_CD/LEAVE_CD 를 한 컬럼에 공유하므로 코드값이 우연히 겹칠 수 있다
    		//   (예: 근무타입 SCH_CD '00013' vs 하계휴가 LEAVE_CD '00013'). 따라서 휴가 셀 판정은
    		//   "FE 가 명시한 leaveCd(또는 autoLegalLeave)" 신호로만 한다. 과거의 workPlanCd 폴백은
    		//   근무타입을 휴가로 오인시켜 정상 SCH 지정을 차단했으므로 제거한다(com-008-E 이후 work_plan 은
    		//   LEAVE_CD 를 저장하지 않아 폴백 자체가 불필요).
    		boolean hasExplicitLeaveCd = model.leaveCd() != null && !model.leaveCd().isEmpty();
    		String effectiveLeaveCd = hasExplicitLeaveCd ? model.leaveCd() : workPlanCd;
    		// prafta-059: legalLeaveCds(LEAVE_NATURE_TYPE='01' 한정) 게이트를 제거한다.
    		//   nature='02'(특별휴가류, 예: SYS_BIRTHDAY 또는 관리자가 attd03 에서 만든 커스텀 연차)로
    		//   등록된 코드가 leaveCd 로 명시 전송돼도 이 게이트에 걸려 isLegalLeaveCell=false 가 되면서
    		//   잔액검증·화이트리스트체크·실차감(552행 DIRECT_LEAVE_CD_WHITELIST, recordDirectLeaveUsage)을
    		//   전부 우회하는 결함이 있었다. hasExplicitLeaveCd 만으로 연차 셀로 인식시키고, 화이트리스트 밖
    		//   코드는 이어지는 552행 체크(REASON_LEAVE_CD_NOT_ALLOWED)에서 nature 무관하게 걸러낸다.
    		boolean isLegalLeaveCell = isAutoLegal || hasExplicitLeaveCd;

    		// prafta-com-008-D: 교대 잠금 가드 — 교대팀 소속 구간의 일반 근무(SCH) 셀 저장 차단.
    		//   ★연차 셀(법정연차 코드)은 D-3 정합으로 잠금 통과(연차만 허용). 가드를 연차 분기 이전에
    		//   "연차 아닌 셀"에만 적용한다. 관리자 예외 없음(권한 무관 — 가드는 authCd 를 보지 않는다).
    		if (!isLegalLeaveCell) {
    			// 근태 E2E F4(4-13): 해당 (사용자+근무일)에 처리 대기 중인 스케줄수정요청(REQ_TYPE='10', REQ_STATUS='01')이
    			//   있으면 관리자 직접 스케줄 변경을 차단(스킵)한다. 승인/반려/취소된 요청은 잠그지 않는다(오탐 방지).
    			//   경합 방지: 대기 요청이 참조할 기준 스케줄을 관리자가 임의 변경하지 못하게 한다.
    			//   권한 가드(ensureCanManageTargetUser)·회사/사업장 스코프 통과 이후에만 조회된다.
    			if (attd05Mapper.countPendingSchModifyReq(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd()) > 0) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						REASON_HAS_PENDING_SCH_MODIFY, reasonText(REASON_HAS_PENDING_SCH_MODIFY)));
    				log.info("근무계획 저장 스킵(대기 스케줄수정요청 경합) - userCd={}, workYmd={}, schCd={}"
    						, model.userCd(), model.workYmd(), workPlanCd);
    				continue;
    			}

    			shiftMembershipService.assertNotShiftLocked(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd());

    			// prafta-com-016 가드② — 일반 근무(SCH) 셀로의 변경은 그 날 "확정 연차(종일/반차/시간차) 또는
    			//   초과근무(등록/신청)" 보유 시 거부(skip)한다. 공통 ScheduleChangeGuardService 로 단일 판정.
    			//   (연차 셀 적용은 가드 비대상 — 잠금은 기존 연차/OT 가 있는 날의 "스케줄 변경"을 막는 것.)
    			//   기존엔 OT(countDayOvertime) 만 막았으나, 시간차/반차 연차가 있는 날의 SCH 변경도 함께 막는다
    			//   (shared-schedule-guard 원칙: 시간차 연차는 그 날 근무시간 기준 신청 → 사후 스케줄 변경 보호).
    			List<com.prafta.common.cmm.schedule.vo.ScheduleLockVO> locks =
    					scheduleChangeGuardService.findLockedDays(
    							model.gvCmpnyCd(), model.siteCd(), model.userCd(),
    							java.util.List.of(model.workYmd()));
    			if (!locks.isEmpty()) {
    				// E3: 사유 우선순위 OT > 확정 연차 > 미결 시간차(전부 미결 잠금뿐일 때만 PENDING_LEAVE).
    				String reasonCode = resolveLockReasonCode(locks);
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						reasonCode, reasonText(reasonCode)));
    				log.info("근무계획 저장 스킵(스케줄 변경 가드) - userCd={}, workYmd={}, schCd={}, 사유={}"
    						, model.userCd(), model.workYmd(), workPlanCd, reasonCode);
    				continue;
    			}
    		}

    		List<SchTypeUseYnResult> versions = (workPlanCd == null) ? null : versionMap.get(workPlanCd);

    		// 근무타입(SCH_CD) 계열만 검증한다. 휴가코드(LEAVE_CD) 등은 버전 목록이 없으므로 검증 없이 통과.
    		if (versions != null && !versions.isEmpty()) {

    			String reasonCode = validateSchCell(model.workYmd(), versions);

    			if (reasonCode != null) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd()
    						, model.workYmd()
    						, workPlanCd
    						, reasonCode
    						, reasonText(reasonCode)
    				));
    				log.info("근무계획 저장 스킵 - userCd={}, workYmd={}, schCd={}, 사유={}"
    						, model.userCd(), model.workYmd(), workPlanCd, reasonCode);
    				continue;
    			}

    			// 교차일 겹침 가드(SCH 셀 한정) — 앞뒤 근무일의 스케줄과 시각이 겹치면(야간 오버나이트 포함)
    			//   저장하지 않고 스킵한다(차단). 같은 배치의 이웃 변경은 pendingSchByUser 로 반영(DB 우선).
    			if (!isLegalLeaveCell && scheduleOverlapGuardService.hasCrossDayOverlap(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd(), workPlanCd,
    					pendingSchByUser.get(model.userCd()))) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						REASON_SCHEDULE_OVERLAP, reasonText(REASON_SCHEDULE_OVERLAP)));
    				log.info("근무계획 저장 스킵(교차일 겹침) - userCd={}, workYmd={}, schCd={}"
    						, model.userCd(), model.workYmd(), workPlanCd);
    				continue;
    			}
    		}

    		// 빈 셀(근무 스케줄 미배정)에는 연차를 적용할 수 없다(서버 권위 가드 — FE 우회 방어).
    		//   "스케줄 있음" = TB_USER_WORK_PLAN 에 그 셀 행이 있고 WORK_PLAN_CD 가 실제 근무타입(SCH_CD)인 경우.
    		//   행이 없거나 WORK_PLAN_CD 가 연차코드면 0 → 해당 연차 셀 스킵(recordDirectLeaveUsage 미호출).
    		//
    		//   ★ prafta-com-008-E (QA M1 멱등 회귀 수정): DEFAULT_SCH_CD 미설정 사용자(실DB 대다수)는
    		//     연차 적용 시 work_plan 에 SCH 가 남지 않을 수 있다(위 E-6 분기에서 work_plan 미기록).
    		//     이 경우 이미 연차가 적용된 셀을 재저장하면 countUserWorkPlanSchedule==0 이 되어 거부되고,
    		//     FE(leaveOverlay 기반 isLeaveCell)는 통과시키므로 FE/BE 멱등 불일치가 발생한다.
    		//     → 거부 조건을 "근무 스케줄 없음 AND 그 셀에 이미 종일 연차도 없음" 일 때로 정밀화한다.
    		//       이미 종일 연차(leave_use 종일 CONFIRMED)가 있는 셀은 멱등으로 통과시키고,
    		//       아래 recordDirectLeaveUsage 의 멱등(SKIPPED_DUP) 처리에 위임한다(이중 차감 없음).
    		//       정합: countFullDayLeaveUseOnCell 의 술어(USE_UNIT_TYPE='00', LEAVE_STATUS='CONFIRMED',
    		//       DEL_YN='N', START_DATE<=workYmd<=END_DATE)는 recordDirectLeaveUsage 가 기록하는
    		//       종일 직접 연차(UNIT_FULL/USE_CONFIRMED/startDate=endDate=workYmd)와 동일 기준이다.
    		//     결과(B안 — 교대만 제한): 빈 셀(스케줄 없음 + 연차 없음)이라도
    		//       · 비교대 사용자: 종일 연차/월차 적용 허용(아래 recordDirectLeaveUsage → 1일 차감).
    		//         (종일 연차는 스케줄과 무관하게 1일 차감이라 빈 셀에도 적용 가능 — 앱 근로자 self-apply 와 동일 기준)
    		//       · 교대팀 소속 구간(휴무일 포함): 여전히 차단. 교대패턴이 근무/휴무를 결정하므로
    		//         그리드에서 휴무일에 연차를 임의 적용하지 않는다(교대 정합 보수 유지).
    		//       이미 종일 연차가 있는 셀은 (교대 여부 무관) 멱등 통과(hasFullDayLeave) → recordDirectLeaveUsage SKIPPED_DUP.
    		if (isLegalLeaveCell) {
    			boolean hasSchedule = attd05Mapper.countUserWorkPlanSchedule(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd()) > 0;
    			boolean hasFullDayLeave = attd05Mapper.countFullDayLeaveUseOnCell(
    					model.gvCmpnyCd(), model.userCd(), model.workYmd()) > 0;
    			boolean shiftMember = shiftMembershipService.isInShiftTeamOn(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd());
    			if (!hasSchedule && !hasFullDayLeave && shiftMember) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						REASON_NO_SCHEDULE_FOR_LEAVE, reasonText(REASON_NO_SCHEDULE_FOR_LEAVE)));
    				log.info("근무계획 연차 적용 스킵(교대팀 소속 빈 셀) - userCd={}, workYmd={}, leaveCd={}"
    						, model.userCd(), model.workYmd(), workPlanCd);
    				continue;
    			}
    		}

    		// 법정 휴가 적용 셀: 결재 없이 즉시 연차 사용 기록 + 잔여 차감 (prafta-021).
    		// 잔여 부족이면 해당 셀은 저장하지 않고 스킵. (이미 기록됨/정상 차감은 통과하여 근무계획 저장)
    		// (isLegalLeaveCell 은 위 교대 잠금 가드 분기에서 이미 산출됨 — 재선언하지 않는다.)
    		if (isLegalLeaveCell) {
    			// BW-04(qa §5-8): 관리자 직접 차감(근무계획 저장/엑셀) 경로는 휴게시간 무시 요청 비대상.
    			//   값이 'Y' 로 실려 오면 셀 스킵이 아니라 요청 전체 거부(ATTD_400_218) — 우회 경로 차단(fail-closed).
    			if ("Y".equals(model.brkWaiveYn())) {
    				log.info("근무계획 연차 적용 거부(휴게시간 무시 요청은 본인 신청 경로 한정) - userCd={}, workYmd={}"
    						, model.userCd(), model.workYmd());
    				throw new ApiException(AttdErrorCode.ATTD_400_218);
    			}
    			DirectLeaveResult result;
    			if (isAutoLegal) {
    				// prafta-com-016-C-4: 종류 미지정 → 후보(연차/월차) 중 소멸 임박 통합순으로 자동 1일 차감.
    				result = leaveFlowService.recordDirectLeaveUsageAuto(
    						model.gvCmpnyCd(), model.siteCd(), model.userCd(),
    						model.workYmd(), AUTO_LEGAL_LEAVE_CDS, model.gvUserCd());
    			} else {
    				// prafta-com-016-C-2(레거시/엑셀 back-compat): 명시 종류 직접 차감.
    				//   직접 지정 가능 휴가 종류 화이트리스트(연차 SYS_ANNUAL / 월차 SYS_MONTHLY) 검증.
    				//   화이트리스트 밖(법정성격이지만 약정/근속가산 등)은 이 화면에서 직접 지정 불가 → 거부.
    				if (!DIRECT_LEAVE_CD_WHITELIST.contains(effectiveLeaveCd)) {
    					skippedList.add(new SkippedCellResult(
    							model.userCd(), model.workYmd(), effectiveLeaveCd,
    							REASON_LEAVE_CD_NOT_ALLOWED, reasonText(REASON_LEAVE_CD_NOT_ALLOWED)));
    					log.info("근무계획 연차 적용 스킵(허용되지 않은 휴가 종류) - userCd={}, workYmd={}, leaveCd={}"
    							, model.userCd(), model.workYmd(), effectiveLeaveCd);
    					continue;
    				}
    				result = leaveFlowService.recordDirectLeaveUsage(
    						model.gvCmpnyCd(), model.siteCd(), model.userCd(),
    						model.workYmd(), effectiveLeaveCd, model.gvUserCd());
    			}
    			if (result == DirectLeaveResult.INSUFFICIENT) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), effectiveLeaveCd,
    						REASON_INSUFFICIENT_LEAVE, reasonText(REASON_INSUFFICIENT_LEAVE)));
    				log.info("근무계획 연차 적용 스킵(잔여 부족) - userCd={}, workYmd={}, auto={}, leaveCd={}"
    						, model.userCd(), model.workYmd(), isAutoLegal, effectiveLeaveCd);
    				continue;
    			}
    			// prafta-com-016-C-2: 실제 신규 기록(RECORDED)일 때만 묶음 PUSH 대상에 모은다(멱등 중복 제외).
    			if (result == DirectLeaveResult.RECORDED) {
    				leaveSetByUser.computeIfAbsent(model.userCd(), k -> new ArrayList<>()).add(model.workYmd());
    				siteCdByUser.putIfAbsent(model.userCd(), model.siteCd());
    			}
    		}

    		// prafta-com-008-E-2/E-6: 연차-스케줄 모델 전환 — 연차 셀이어도 work_plan 에는 LEAVE_CD 를 쓰지 않는다.
    		//   연차일 판정/출근차단은 leave_use(위에서 기록) 기준. work_plan 은 사용자 기본 근무타입(DEFAULT_SCH_CD)
    		//   을 유지/기록한다(없으면 work_plan 미기록 — 연차 취소 시 빈 날 = 휴무로 복귀). 일반(SCH) 셀은 그대로 저장.
    		if (isLegalLeaveCell) {
    			String defaultSchCd = attd05Mapper.selectUserDefaultSchCd(model.gvCmpnyCd(), model.userCd());
    			if (defaultSchCd == null || defaultSchCd.isEmpty()) {
    				// 기본 근무타입 미설정 → work_plan 미기록(연차는 leave_use 로만 표현). 셀 자체는 처리 완료로 카운트.
    				savedCount++;
    				continue;
    			}
    			attd05Mapper.saveUserWorkPlans(SchTypeCommand.from(model, defaultSchCd));
    			savedCount++;
    			continue;
    		}

    		attd05Mapper.saveUserWorkPlans(SchTypeCommand.from(model));
    		savedCount++;
    	}

    	log.info("근무계획 저장 완료 - 저장 {}건, 스킵 {}건", savedCount, skippedList.size());

    	// prafta-com-016-C-2: 연차/월차가 새로 등록된 근로자별로 묶음 PUSH 1건 예약(afterCommit + REQUIRES_NEW).
    	//   본 트랜잭션 커밋 이후에만 적재되므로 저장 흐름 롤백/실패에 영향 없음. PII 미포함(건수/날짜만).
    	for (Map.Entry<String, List<String>> e : leaveSetByUser.entrySet()) {
    		String targetUserCd = e.getKey();
    		leaveDirectSetNotiService.notifyLeaveDirectSet(
    				firstModel.gvCmpnyCd(), siteCdByUser.get(targetUserCd), targetUserCd,
    				e.getValue(), firstModel.gvUserCd());
    	}

    	return SaveUserWorkPlansResponse.builder()
    									.savedCount(savedCount)
    									.skippedList(skippedList)
    									.build();
    }

    @Override
    @Transactional
    public com.prafta.web.attd.attd05.dto.response.DeleteUserWorkPlansResponse deleteUserWorkPlans(SchTypeDeleParam param) {
    	// 관리 기능 권한 가드 (prafta-041-4) — master/hr 또는 대상 사용자 소속(및 하위) 부서 관리자.
    	// 대상 사용자별 검증 결과 캐시로 중복 조회 방지. 권한 없는 대상 하나라도 있으면 전체 실패(트랜잭션 롤백).
    	Map<String, Boolean> manageCache = new java.util.HashMap<>();
    	int deletedUserCount = 0;
    	// F-11-1: 매퍼가 실제 삭제 행 수(int)를 반환하도록 바뀌어, 사용자별 삭제 건수를 누적 집계한다.
    	int totalDeletedCount = 0;
    	List<SkippedCellResult> skippedList = new ArrayList<>();
    	for(SchTypeDeleModel model : param.schTypeDeleModelList()) {
    		// PRAFTA-041-4 - 대상 사용자 관리 권한 검증
    		ensureCanManageTargetUser(param.gvAuthCd(), model.gvUserCd(), model.gvCmpnyCd(),
    				model.siteCd(), model.userCd(), manageCache);
    		// PRAFTA-028 - 마감된 기간(부서)의 근무계획 삭제 차단 (마감/교대는 기존대로 전체 차단 — 회귀 보존)
    		if (attdCloseService.isClosedForUser(model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYm())) {
    			throw new ApiException(AttdErrorCode.ATTD_400_042);
    		}
    		// prafta-com-008-D: 교대 잠금 가드(월 단위) — 해당 월(workYm)에 교대 소속 구간 일자가
    		//   1건이라도 있으면 월 전체 삭제를 차단한다(일별 LEAVE_TEAM_YMD 판정 불가 → 보수적 전면 차단).
    		//   관리자 예외 없음. 연차는 종일 leave_use 기준이므로 본 월 단위 work_plan 삭제와 무관.
    		shiftMembershipService.assertNotShiftLockedInMonth(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYm());

    		// prafta-com-016-C-3: 초과근무(OT) 등록/신청 보유일은 "전체 차단"이 아니라 "부분 삭제로 제외"한다.
    		//   DELETE SQL(deleteUserWorkPlans)이 OT 보유일·연차 등록일을 NOT EXISTS 로 보존하므로,
    		//   여기서는 제외될 OT 보유일 목록을 사용자 안내(skippedList)용으로 조회한다.
    		//   (원문 countMonthOvertime 의 LEFT(...,6)=workYm 포맷 버그로 인한 전면 차단 무력화 경로를 폐기.)
    		String workYm6 = model.workYm() == null ? null : model.workYm().replace("-", "");
    		// OT 보존일 안내. 같은 사용자 내 중복 일자(연차와 OT 동시 보유) 방지를 위해 안내한 일자를 모은다.
    		java.util.Set<String> reportedDays = new java.util.HashSet<>();
    		List<String> otDays = attd05Mapper.selectMonthOvertimeDays(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), workYm6);
    		for (String otYmd : otDays) {
    			skippedList.add(new SkippedCellResult(
    					model.userCd(), otYmd, null,
    					REASON_HAS_OVERTIME, reasonText(REASON_HAS_OVERTIME)));
    			reportedDays.add(otYmd);
    		}
    		// prafta-com-016-C-3 후속: 확정 연차로 보존(삭제 제외)된 일자도 함께 안내한다.
    		//   E3(당일분모 전환, DFCT-1 보완): 보존·안내가 종일 한정에서 전 단위로 확장 —
    		//   시간차 분모(E1)가 당일 배정 스케줄이라 반차/반반차/시간차 보유일도 삭제 제외 대상.
    		//   OT 와 같은 일자(중복)는 OT 사유로 이미 안내했으므로 건너뛴다.
    		List<String> leaveDays = attd05Mapper.selectMonthLeaveDays(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), workYm6);
    		int leaveReported = 0;
    		for (String lvYmd : leaveDays) {
    			if (reportedDays.contains(lvYmd)) {
    				continue;
    			}
    			skippedList.add(new SkippedCellResult(
    					model.userCd(), lvYmd, null,
    					REASON_LEAVE_PRESERVED, reasonText(REASON_LEAVE_PRESERVED)));
    			reportedDays.add(lvYmd);
    			leaveReported++;
    		}
    		// E3(DFCT-1 보완): 미결 시간차 신청으로 보존된 일자 안내(HAS_PENDING_LEAVE — 셀 저장 skip 사유 재사용).
    		List<String> pendingHourlyDays = attd05Mapper.selectMonthPendingHourlyReqDays(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), workYm6);
    		int pendingReported = 0;
    		for (String pdYmd : pendingHourlyDays) {
    			if (reportedDays.contains(pdYmd)) {
    				continue;
    			}
    			skippedList.add(new SkippedCellResult(
    					model.userCd(), pdYmd, null,
    					REASON_HAS_PENDING_LEAVE, reasonText(REASON_HAS_PENDING_LEAVE)));
    			reportedDays.add(pdYmd);
    			pendingReported++;
    		}
    		if (!otDays.isEmpty() || leaveReported > 0 || pendingReported > 0) {
    			log.info("근무계획 월 삭제 부분 제외 - userCd={}, workYm={}, OT보존 {}건, 연차보존 {}건, 미결시간차보존 {}건",
    					model.userCd(), model.workYm(), otDays.size(), leaveReported, pendingReported);
    		}

    		totalDeletedCount += attd05Mapper.deleteUserWorkPlans(SchTypeDeleCommand.from(model));
    		deletedUserCount++;
    	}
    	log.info("근무계획 월 삭제 완료 - 대상 사용자 {}명, 실삭제 {}건, OT/연차 보존 제외 {}건",
    			deletedUserCount, totalDeletedCount, skippedList.size());
    	return com.prafta.web.attd.attd05.dto.response.DeleteUserWorkPlansResponse.builder()
    			.deletedUserCount(deletedUserCount)
    			.deletedCount(totalDeletedCount)
    			.skippedList(skippedList)
    			.build();
    }

    @Override
    @Transactional
    public DeleteWorkPlanCellsResponse deleteUserWorkPlanCells(WorkPlanCellDeleParam param) {

    	List<WorkPlanCellDeleModel> modelList = param.workPlanCellDeleModelList();
    	if (modelList == null || modelList.isEmpty()) {
    		return DeleteWorkPlanCellsResponse.builder()
    				.deletedCount(0)
    				.leaveRestoredCount(0)
    				.skippedList(new ArrayList<>())
    				.build();
    	}

    	// 관리 기능 권한 가드 (prafta-041-4) — master/hr 또는 대상 사용자 소속(및 하위) 부서 관리자.
    	// 대상 사용자별 검증 결과 캐시로 중복 조회 방지. 권한 없는 대상 하나라도 있으면 전체 실패(트랜잭션 롤백).
    	Map<String, Boolean> manageCache = new java.util.HashMap<>();

    	int deletedCount = 0;
    	int leaveRestoredCount = 0;
    	List<SkippedCellResult> skippedList = new ArrayList<>();

    	for (WorkPlanCellDeleModel model : modelList) {

    		// PRAFTA-041-4 - 대상 사용자 관리 권한 검증
    		ensureCanManageTargetUser(param.gvAuthCd(), model.gvUserCd(), model.gvCmpnyCd(),
    				model.siteCd(), model.userCd(), manageCache);

    		// PRAFTA-028 - 마감된 기간(부서)의 근무계획 삭제 차단 (workYmd → 월 추출)
    		String closeYm = (model.workYmd() != null && model.workYmd().length() >= 6)
    				? model.workYmd().substring(0, 6) : model.workYmd();
    		if (attdCloseService.isClosedForUser(model.gvCmpnyCd(), model.siteCd(), model.userCd(), closeYm)) {
    			throw new ApiException(AttdErrorCode.ATTD_400_042);
    		}

    		// prafta-com-008-B-5 (D-E8): 전면 동의 원칙 — 종일 확정 연차(leave_use)가 등록된 셀은
    		//   종류 불문(직접 REQ_ID NULL / 승인기반 REQ_ID NOT NULL) "비우기"로 삭제할 수 없다.
    		//   비우기는 근무 스케줄(work_plan)만 지우는 동작이며, 연차 취소는 셀 개별 진입 →
    		//   attd13 동의흐름(연차 변경/삭제 요청)을 통해서만 수행한다("연차 = 사용자의 권리").
    		//   따라서 연차 셀은 work_plan 행도 건드리지 않고 셀 전체를 skip 하고, 사유를 응답으로 안내한다.
    		//   ★직접연차 즉시취소(cancelDirectLeaveUsage) 경로는 제거. cancelDirectLeaveUsage 메서드 자체는
    		//    attd13 DELETE 확정 경로용으로 보존(여기서 호출만 제거).
    		String leaveCdOnCell = attd05Mapper.selectFullDayLeaveCdOnCell(
    				model.gvCmpnyCd(), model.userCd(), model.workYmd());
    		if (leaveCdOnCell != null) {
    			skippedList.add(new SkippedCellResult(
    					model.userCd()
    					, model.workYmd()
    					, leaveCdOnCell
    					, REASON_LEAVE_CELL_CONSENT_REQUIRED
    					, reasonText(REASON_LEAVE_CELL_CONSENT_REQUIRED)
    			));
    			log.info("근무계획 셀 비우기 스킵(연차 등록일 — 동의요청 경유 필요) - userCd={}, workYmd={}, leaveCd={}",
    					model.userCd(), model.workYmd(), leaveCdOnCell);
    			continue;
    		}

    		// prafta-com-016 가드② — 공통 스케줄 변경 가드로 OT(등록/신청) 및 (종일이 아닌) 확정 연차 보유일의
    		//   셀 비우기를 차단한다. 종일 연차는 위 selectFullDayLeaveCdOnCell(consent 경로)에서 이미 skip 됐으므로,
    		//   여기서는 OT 및 반차/시간차 연차가 있는 날의 SCH 셀 비우기를 막는다(저장 가드와 동일 단일출처).
    		//   기존 countDayOvertime 단독 호출을 ScheduleChangeGuardService 로 통합한다(중복 로직 정리).
    		List<com.prafta.common.cmm.schedule.vo.ScheduleLockVO> cellLocks =
    				scheduleChangeGuardService.findLockedDays(
    						model.gvCmpnyCd(), model.siteCd(), model.userCd(),
    						java.util.List.of(model.workYmd()));
    		if (!cellLocks.isEmpty()) {
    			// E3: 사유 우선순위 OT > 확정 연차 > 미결 시간차(전부 미결 잠금뿐일 때만 PENDING_LEAVE).
    			String reasonCode = resolveLockReasonCode(cellLocks);
    			skippedList.add(new SkippedCellResult(
    					model.userCd(), model.workYmd(), null,
    					reasonCode, reasonText(reasonCode)));
    			log.info("근무계획 셀 비우기 스킵(스케줄 변경 가드) - userCd={}, workYmd={}, 사유={}",
    					model.userCd(), model.workYmd(), reasonCode);
    			continue;
    		}

    		// prafta-com-008-D: 교대 잠금 가드 — 교대팀 소속 구간의 일반 근무(SCH) 셀 비우기 차단.
    		//   ★위 B-5 연차셀 skip(continue) 뒤에 배치 → "연차 아닌 일반 SCH 셀"에만 발동(D-3 정합:
    		//   연차 취소까지 막지 않는다). 관리자 예외 없음(권한 무관 — 가드는 authCd 를 보지 않는다).
    		shiftMembershipService.assertNotShiftLocked(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd());

    		// 연차 없는 일반 근무 셀: work_plan 셀 행이 있으면 삭제(빈 셀로 복귀). 없으면 no-op.
    		WorkPlanCellDeleCommand command = WorkPlanCellDeleCommand.from(model);
    		deletedCount += attd05Mapper.deleteUserWorkPlanCell(command);
    	}

    	// prafta-com-008-B-5: 비우기 경로에서 연차 차감 복원은 더 이상 발생하지 않는다(leaveRestoredCount 항상 0).
    	//   응답 구조 호환을 위해 필드는 유지한다(연차 취소는 attd13 확정 경로가 차감 복원 수행).
    	log.info("근무계획 셀 삭제 완료 - 삭제 {}건, 연차 등록일 스킵 {}건",
    			deletedCount, skippedList.size());

    	return DeleteWorkPlanCellsResponse.builder()
    			.deletedCount(deletedCount)
    			.leaveRestoredCount(leaveRestoredCount)
    			.skippedList(skippedList)
    			.build();
    }

    /**
     * 대상 사용자(targetUserCd) 관리 권한 검증 (PRAFTA-041-4).
     *
     * <p>master/hr 또는 대상 사용자가 소속한 부서(및 상위 부서)의 정/부 관리자만 허용한다.
     * 동일 대상에 대한 반복 검증을 피하려고 결과를 {@code cache} 에 보관한다.
     * 권한이 없으면 기존과 동일한 {@link AttdErrorCode#ATTD_403_002} 예외를 던진다.
     *
     * @param authCd          요청자 권한 코드(JWT)
     * @param requesterUserCd 요청자 사용자 코드(JWT)
     * @param cmpnyCd         회사 코드(JWT)
     * @param siteCd          대상 사업장 코드
     * @param targetUserCd    대상 사용자 코드
     * @param cache           대상 사용자별 권한 판정 캐시(siteCd|targetUserCd 키)
     */
    private void ensureCanManageTargetUser(String authCd, String requesterUserCd, String cmpnyCd,
    		String siteCd, String targetUserCd, Map<String, Boolean> cache) {

    	String cacheKey = siteCd + "|" + targetUserCd;
    	Boolean allowed = cache.get(cacheKey);
    	if (allowed == null) {
    		allowed = attdCloseService.canManageUser(authCd, requesterUserCd, cmpnyCd, siteCd, targetUserCd);
    		cache.put(cacheKey, allowed);
    	}
    	if (!allowed) {
    		log.warn("근무계획 관리 권한 없음 - 요청자 userCd={}, authCd={}, 대상 userCd={}, siteCd={}",
    				requesterUserCd, authCd, targetUserCd, siteCd);
    		throw new ApiException(AttdErrorCode.ATTD_403_002);
    	}
    }

    /**
     * effective-dating 버전 목록을 SCH_CD 별 검증 메타로 가공한다.
     * - createDt : MIN(APPLY_DATE)
     * - versionList : APPLY_DATE 오름차순 버전 목록
     */
    private List<SchTypeValidMeta> buildSchTypeValidMetaList(List<SchTypeUseYnResult> useYnList) {

    	Map<String, List<SchTypeUseYnResult>> versionMap = groupBySchCd(useYnList);

    	List<SchTypeValidMeta> metaList = new ArrayList<>();
    	for (Map.Entry<String, List<SchTypeUseYnResult>> entry : versionMap.entrySet()) {
    		List<SchTypeUseYnResult> versions = entry.getValue();
    		// APPLY_DATE 오름차순 정렬되어 있으므로 첫 행이 생성일
    		String createDt = versions.get(0).applyDate();
    		metaList.add(new SchTypeValidMeta(entry.getKey(), createDt, versions));
    	}
    	return metaList;
    }

    /**
     * effective-dating 버전 목록을 SCH_CD 키로 그룹핑한다.
     * 각 그룹은 APPLY_DATE 오름차순으로 정렬한다.
     */
    private Map<String, List<SchTypeUseYnResult>> groupBySchCd(List<SchTypeUseYnResult> useYnList) {

    	Map<String, List<SchTypeUseYnResult>> versionMap = new LinkedHashMap<>();

    	if (useYnList == null) {
    		return versionMap;
    	}

    	for (SchTypeUseYnResult row : useYnList) {
    		versionMap.computeIfAbsent(row.schCd(), k -> new ArrayList<>()).add(row);
    	}
    	for (List<SchTypeUseYnResult> versions : versionMap.values()) {
    		versions.sort(Comparator.comparing(SchTypeUseYnResult::applyDate));
    	}
    	return versionMap;
    }

    /**
     * 단일 셀(workYmd) 에 대한 근무타입(SCH_CD) 차단 검증.
     * @param workYmd  근무일 (yyyyMMdd)
     * @param versions APPLY_DATE 오름차순 정렬된 effective-dating 버전 목록
     * @return 위반 시 사유 코드, 정상이면 null
     */
    private String validateSchCell(String workYmd, List<SchTypeUseYnResult> versions) {

    	// 검증1) 생성일 = MIN(APPLY_DATE). workYmd 가 생성일 이전이면 차단.
    	String createDt = versions.get(0).applyDate();
    	if (workYmd == null || workYmd.compareTo(createDt) < 0) {
    		return REASON_BEFORE_CREATE;
    	}

    	// 검증2) effective USE_YN : APPLY_DATE <= workYmd 인 최신 버전의 USE_YN.
    	String effectiveUseYn = null;
    	for (SchTypeUseYnResult v : versions) {
    		if (v.applyDate().compareTo(workYmd) <= 0) {
    			effectiveUseYn = v.useYn();
    		} else {
    			break;
    		}
    	}
    	if ("N".equals(effectiveUseYn)) {
    		return REASON_USE_YN_N;
    	}

    	return null;
    }

    /**
     * 공통 스케줄 변경 가드(findLockedDays) 잠금 목록 → 대표 스킵 사유 코드(E3 확장).
     * 우선순위: OT > 확정 연차 > 미결 시간차. (한 날에 여러 잠금이 공존할 수 있으므로,
     * 안내는 조치 부담이 큰 순서로 대표 1건만 — 기존 otLocked 우선 로직 승계.)
     */
    private String resolveLockReasonCode(List<com.prafta.common.cmm.schedule.vo.ScheduleLockVO> locks) {
    	// F-7(2026-08-06): 판정 로직을 공용 헬퍼로 이관하고 본 메서드는 사유 코드 변환만 담당한다
    	//   (Attd_05 셀 스킵 사유 ↔ Attd_07/Req_07 차단 문구의 경로 간 판정 불일치 제거). 동작 불변.
    	switch (com.prafta.common.cmm.schedule.ScheduleLockMessages.resolveLockKind(locks)) {
    		case OVERTIME:
    			return REASON_HAS_OVERTIME;
    		case CONFIRMED_LEAVE:
    			return REASON_HAS_LEAVE;
    		default:
    			return REASON_HAS_PENDING_LEAVE;
    	}
    }

    /** 스킵 사유 코드에 대응하는 사유 문구 (한국어) */
    private String reasonText(String reasonCode) {
    	if (REASON_BEFORE_CREATE.equals(reasonCode)) {
    		return "근무타입 생성일 이전 날짜입니다.";
    	}
    	if (REASON_USE_YN_N.equals(reasonCode)) {
    		return "해당 날짜는 근무타입 미사용 기간입니다.";
    	}
    	if (REASON_INSUFFICIENT_LEAVE.equals(reasonCode)) {
    		return "연차 잔여가 부족하여 적용할 수 없습니다.";
    	}
    	if (REASON_NO_SCHEDULE_FOR_LEAVE.equals(reasonCode)) {
    		return "교대근무팀 소속 기간의 휴무일(근무 스케줄 없는 날)에는 연차를 적용할 수 없습니다.";
    	}
    	if (REASON_APPROVED_LEAVE.equals(reasonCode)) {
    		return "승인기반 연차는 셀에서 직접 삭제할 수 없습니다. 연차 변경/삭제 요청을 이용하세요.";
    	}
    	if (REASON_LEAVE_CELL_CONSENT_REQUIRED.equals(reasonCode)) {
    		return "연차 등록일은 비우기로 삭제할 수 없습니다. 셀을 눌러 연차 변경/삭제 요청을 이용하세요.";
    	}
    	if (REASON_HAS_OVERTIME.equals(reasonCode)) {
    		return "초과근무가 등록/신청된 날짜는 스케줄을 변경할 수 없습니다.";
    	}
    	if (REASON_HAS_LEAVE.equals(reasonCode)) {
    		return "연차(종일/반차/시간차)가 등록된 날짜는 근무 스케줄을 변경할 수 없습니다.";
    	}
    	if (REASON_SCHEDULE_OVERLAP.equals(reasonCode)) {
    		return "앞뒤 날짜의 근무 스케줄과 시간이 겹쳐 저장할 수 없습니다. 야간(오버나이트) 근무가 다음날 근무와 겹치지 않도록 조정해 주세요.";
    	}
    	if (REASON_LEAVE_PRESERVED.equals(reasonCode)) {
    		return "연차 등록일이라 삭제에서 제외되었습니다. 연차 취소는 연차 변경/삭제 요청을 이용하세요.";
    	}
    	if (REASON_LEAVE_CD_NOT_ALLOWED.equals(reasonCode)) {
    		return "이 화면에서 직접 지정할 수 있는 휴가는 연차/월차뿐입니다.";
    	}
    	if (REASON_HAS_PENDING_SCH_MODIFY.equals(reasonCode)) {
    		return "처리 대기 중인 스케줄 수정요청이 있어 변경할 수 없습니다. 요청을 먼저 처리해 주세요.";
    	}
    	if (REASON_HAS_PENDING_LEAVE.equals(reasonCode)) {
    		// ★ 문구 정정(2026-08-08, 반차 시간대 도입) — 미결 잠금 술어가 START/END_TIME NOT NULL 이고
    		//   반차 REQ 가 이제 경계 시각을 기록하므로 미결 "반차"도 이 사유로 스킵된다.
    		//   종전 문구("시간 단위 연차"만 지목)면 반차로 막힌 관리자가 없는 시간차를 찾게 된다(F-7 동일 계열).
    		//   F-7 은 ATTD_400_164(요청 승인 경로)만 정정했고 본 직접 저장 경로가 누락돼 있었다.
    		return "승인 대기 중인 반차 또는 시간 단위 연차 신청이 있는 날짜는 근무 스케줄을 변경할 수 없습니다. 신청을 먼저 처리해 주세요.";
    	}
    	return "근무타입을 지정할 수 없는 날짜입니다.";
    }
}
