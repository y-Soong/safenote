package com.prafta.web.attd.attd05.service.impl;

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

    /** 검증 스킵 사유 코드 - 근무타입 생성일 이전 */
    private static final String REASON_BEFORE_CREATE = "BEFORE_CREATE";
    /** 검증 스킵 사유 코드 - effective USE_YN 이 'N' 인 기간 */
    private static final String REASON_USE_YN_N = "USE_YN_N";
    /** 검증 스킵 사유 코드 - 연차 잔여 부족(직접 차감 불가, prafta-021) */
    private static final String REASON_INSUFFICIENT_LEAVE = "INSUFFICIENT_LEAVE";
    /** 검증 스킵 사유 코드 - 근무 스케줄(SCH 배정)이 없는 빈 셀에는 연차를 적용할 수 없음 */
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

    @Override
    public UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param) {

    	UserWorkPlansResponse response = null;

    	List<UserResult> userListResultList = attd05Mapper.selectUserList(UserWorkPlansQuery.from(param));

    	List<DayResult> dayResultList = attd05Mapper.selectDayList(UserWorkPlansQuery.from(param));

    	List<SchedResult> schedResultList = attd05Mapper.selectSchedList(UserWorkPlansQuery.from(param));

    	// prafta-com-008-E-6: 연차-스케줄 모델 전환 — work_plan 에는 SCH_CD 가 유지되므로
    	//   그리드 "연차" 표시는 leave_use(종일 CONFIRMED) 오버레이로 동반시킨다(셀 렌더 단일 출처).
    	List<com.prafta.web.attd.attd05.result.LeaveOverlayResult> leaveOverlayList =
    			attd05Mapper.selectLeaveOverlayList(UserWorkPlansQuery.from(param));

    	// prafta-com-008-D-5: 교대 잠금 오버레이 동반(D-1 경계 술어 단일출처). 교대팀 소속 구간 SCH 셀은
    	//   프론트가 비활성/자물쇠 표시한다(연차 셀은 활성 — D-3). 저장 차단의 최종 강제는 BE 가드(D-3)가 수행.
    	List<com.prafta.web.attd.attd05.result.ShiftLockOverlayResult> shiftLockOverlayList =
    			attd05Mapper.selectShiftLockOverlayList(UserWorkPlansQuery.from(param));

    	response = UserWorkPlansResponse.builder()
    									.userListResultList(userListResultList)
    									.dayResultList(dayResultList)
    									.schedResultList(schedResultList)
    									.leaveOverlayResultList(leaveOverlayList)
    									.shiftLockOverlayResultList(shiftLockOverlayList)
    									.build();

    	return response;
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

    	LeaveTypeResponse response= null;

    	List<LeaveTypeResult> leaveTypeResultList = attd05Mapper.selectLeaveTypeList(LeaveTypeListQuery.from(param));

    	response = LeaveTypeResponse.builder().leaveTypeResultList(leaveTypeResultList).build();

    	return response;
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

    	// 법정 휴가 코드 집합 — 이 코드로 적용된 셀은 결재 없이 즉시 연차 사용 기록(차감) (prafta-021 B)
    	java.util.Set<String> legalLeaveCds = new java.util.HashSet<>(
    			attd05Mapper.selectLegalLeaveCds(firstModel.gvCmpnyCd()));

    	List<SkippedCellResult> skippedList = new ArrayList<>();
    	int savedCount = 0;

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

    		// prafta-com-008-D: 교대 잠금 가드 — 교대팀 소속 구간의 일반 근무(SCH) 셀 저장 차단.
    		//   ★연차 셀(법정연차 코드)은 D-3 정합으로 잠금 통과(연차만 허용). 가드를 연차 분기 이전에
    		//   "연차 아닌 셀"에만 적용한다. 관리자 예외 없음(권한 무관 — 가드는 authCd 를 보지 않는다).
    		boolean isLegalLeaveCell = workPlanCd != null && legalLeaveCds.contains(workPlanCd);
    		if (!isLegalLeaveCell) {
    			shiftMembershipService.assertNotShiftLocked(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd());

    			// 초과근무(등록 or 신청) 보유일의 스케줄(SCH) 변경 차단 — 앱 스케줄수정요청 게이트와 정합.
    			//   초과근무는 스케줄 기준으로 정산되므로 보유일의 스케줄 변경을 막는다. 해당 셀만 스킵(부분 저장).
    			//   (연차 셀은 본 가드 비대상 — 연차는 출근/초과근무와 양립 불가하여 자연 배제.)
    			if (attd05Mapper.countDayOvertime(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd()) > 0) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						REASON_HAS_OVERTIME, reasonText(REASON_HAS_OVERTIME)));
    				log.info("근무계획 저장 스킵(초과근무 보유일) - userCd={}, workYmd={}, schCd={}"
    						, model.userCd(), model.workYmd(), workPlanCd);
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
    		//     결과: 빈 셀 첫 적용(스케줄 없음 + 연차 없음)은 차단(원 요구사항 유지), 스케줄 있는 셀
    		//       첫 적용·이미 연차 셀 재적용은 허용. DEFAULT_SCH_CD 설정 여부와 무관.
    		if (isLegalLeaveCell) {
    			boolean hasSchedule = attd05Mapper.countUserWorkPlanSchedule(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd()) > 0;
    			boolean hasFullDayLeave = attd05Mapper.countFullDayLeaveUseOnCell(
    					model.gvCmpnyCd(), model.userCd(), model.workYmd()) > 0;
    			if (!hasSchedule && !hasFullDayLeave) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						REASON_NO_SCHEDULE_FOR_LEAVE, reasonText(REASON_NO_SCHEDULE_FOR_LEAVE)));
    				log.info("근무계획 연차 적용 스킵(근무 스케줄 없는 빈 셀) - userCd={}, workYmd={}, leaveCd={}"
    						, model.userCd(), model.workYmd(), workPlanCd);
    				continue;
    			}
    		}

    		// 법정 휴가 적용 셀: 결재 없이 즉시 연차 사용 기록 + 잔여 차감 (prafta-021).
    		// 잔여 부족이면 해당 셀은 저장하지 않고 스킵. (이미 기록됨/정상 차감은 통과하여 근무계획 저장)
    		// (isLegalLeaveCell 은 위 교대 잠금 가드 분기에서 이미 산출됨 — 재선언하지 않는다.)
    		if (isLegalLeaveCell) {
    			DirectLeaveResult result = leaveFlowService.recordDirectLeaveUsage(
    					model.gvCmpnyCd(), model.siteCd(), model.userCd(),
    					model.workYmd(), workPlanCd, model.gvUserCd());
    			if (result == DirectLeaveResult.INSUFFICIENT) {
    				skippedList.add(new SkippedCellResult(
    						model.userCd(), model.workYmd(), workPlanCd,
    						REASON_INSUFFICIENT_LEAVE, reasonText(REASON_INSUFFICIENT_LEAVE)));
    				log.info("근무계획 연차 적용 스킵(잔여 부족) - userCd={}, workYmd={}, leaveCd={}"
    						, model.userCd(), model.workYmd(), workPlanCd);
    				continue;
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

    	return SaveUserWorkPlansResponse.builder()
    									.savedCount(savedCount)
    									.skippedList(skippedList)
    									.build();
    }

    @Override
    @Transactional
    public void deleteUserWorkPlans(SchTypeDeleParam param) {
    	// 관리 기능 권한 가드 (prafta-041-4) — master/hr 또는 대상 사용자 소속(및 하위) 부서 관리자.
    	// 대상 사용자별 검증 결과 캐시로 중복 조회 방지. 권한 없는 대상 하나라도 있으면 전체 실패(트랜잭션 롤백).
    	Map<String, Boolean> manageCache = new java.util.HashMap<>();
    	for(SchTypeDeleModel model : param.schTypeDeleModelList()) {
    		// PRAFTA-041-4 - 대상 사용자 관리 권한 검증
    		ensureCanManageTargetUser(param.gvAuthCd(), model.gvUserCd(), model.gvCmpnyCd(),
    				model.siteCd(), model.userCd(), manageCache);
    		// PRAFTA-028 - 마감된 기간(부서)의 근무계획 삭제 차단
    		if (attdCloseService.isClosedForUser(model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYm())) {
    			throw new ApiException(AttdErrorCode.ATTD_400_042);
    		}
    		// prafta-com-008-D: 교대 잠금 가드(월 단위) — 해당 월(workYm)에 교대 소속 구간 일자가
    		//   1건이라도 있으면 월 전체 삭제를 차단한다(일별 LEAVE_TEAM_YMD 판정 불가 → 보수적 전면 차단).
    		//   관리자 예외 없음. 연차는 종일 leave_use 기준이므로 본 월 단위 work_plan 삭제와 무관.
    		shiftMembershipService.assertNotShiftLockedInMonth(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYm());
    		// 초과근무(등록 or 신청) 보유일이 포함된 월의 근무계획 일괄 삭제 차단(저장/셀 비우기 가드와 정합).
    		//   월 단위 삭제는 부분 스킵 불가 → 해당 월에 초과근무 보유일이 1건이라도 있으면 전체 차단(hard-throw).
    		if (attd05Mapper.countMonthOvertime(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYm()) > 0) {
    			log.info("근무계획 월 삭제 차단(초과근무 보유 월) - userCd={}, workYm={}",
    					model.userCd(), model.workYm());
    			throw new ApiException(AttdErrorCode.ATTD_400_161);
    		}
    		attd05Mapper.deleteUserWorkPlans(SchTypeDeleCommand.from(model));
    	}
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

    		// 초과근무(등록 or 신청) 보유일의 스케줄 셀 비우기 차단 — 저장 가드와 정합. 해당 셀만 스킵.
    		//   초과근무는 스케줄 기준으로 정산되므로 보유일의 스케줄 삭제(비우기)를 막는다.
    		//   (위 연차셀 skip 뒤 배치 → 연차 아닌 일반 SCH 셀에만 발동.)
    		if (attd05Mapper.countDayOvertime(
    				model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYmd()) > 0) {
    			skippedList.add(new SkippedCellResult(
    					model.userCd(), model.workYmd(), null,
    					REASON_HAS_OVERTIME, reasonText(REASON_HAS_OVERTIME)));
    			log.info("근무계획 셀 비우기 스킵(초과근무 보유일) - userCd={}, workYmd={}",
    					model.userCd(), model.workYmd());
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
    		return "근무 스케줄이 없는 날짜에는 연차를 적용할 수 없습니다.";
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
    	return "근무타입을 지정할 수 없는 날짜입니다.";
    }
}
