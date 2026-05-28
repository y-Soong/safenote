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
import com.prafta.web.attd.attd05.application.model.SchTypeDeleModel;
import com.prafta.web.attd.attd05.application.model.SchTypeModel;
import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeDeleParam;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeParam;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd05.application.query.SchListQuery;
import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
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
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd05ServiceImpl implements Attd05Service {

    private final Attd05Mapper attd05Mapper;
    private final com.prafta.web.attd.leaveflow.service.LeaveFlowService leaveFlowService;
    private final com.prafta.web.attd.attd07.service.AttdCloseService attdCloseService;

    /** 검증 스킵 사유 코드 - 근무타입 생성일 이전 */
    private static final String REASON_BEFORE_CREATE = "BEFORE_CREATE";
    /** 검증 스킵 사유 코드 - effective USE_YN 이 'N' 인 기간 */
    private static final String REASON_USE_YN_N = "USE_YN_N";
    /** 검증 스킵 사유 코드 - 연차 잔여 부족(직접 차감 불가, prafta-021) */
    private static final String REASON_INSUFFICIENT_LEAVE = "INSUFFICIENT_LEAVE";

    @Override
    public UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param) {

    	UserWorkPlansResponse response = null;

    	List<UserResult> userListResultList = attd05Mapper.selectUserList(UserWorkPlansQuery.from(param));

    	List<DayResult> dayResultList = attd05Mapper.selectDayList(UserWorkPlansQuery.from(param));

    	List<SchedResult> schedResultList = attd05Mapper.selectSchedList(UserWorkPlansQuery.from(param));

    	response = UserWorkPlansResponse.builder()
    									.userListResultList(userListResultList)
    									.dayResultList(dayResultList)
    									.schedResultList(schedResultList)
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

    	// 관리 기능 권한 가드 — 근무계획/연차 직접 입력·차감은 매니저(MASTER/HR) 전용 (prafta-021 보안).
    	// JWT 기반 authCd 사용(body 위조 불가). 일반 사용자의 타인 연차 무단 차감 차단.
    	if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
    		log.warn("근무계획 저장 권한 없음 - authCd={}", param.gvAuthCd());
    		throw new ApiException(AttdErrorCode.ATTD_403_002);
    	}

    	List<SchTypeModel> modelList = param.schTypeModelList();

    	if (modelList == null || modelList.isEmpty()) {
    		return SaveUserWorkPlansResponse.builder()
    										.savedCount(0)
    										.skippedList(new ArrayList<>())
    										.build();
    	}

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

    		// PRAFTA-028 - 마감된 기간(부서)의 근무타입 변경(저장) 차단
    		String closeYm = (model.workYmd() != null && model.workYmd().length() >= 6)
    				? model.workYmd().substring(0, 6) : model.workYmd();
    		if (attdCloseService.isClosedForUser(model.gvCmpnyCd(), model.siteCd(), model.userCd(), closeYm)) {
    			throw new ApiException(AttdErrorCode.ATTD_400_042);
    		}

    		String workPlanCd = model.workPlanCd();
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

    		// 법정 휴가 적용 셀: 결재 없이 즉시 연차 사용 기록 + 잔여 차감 (prafta-021).
    		// 잔여 부족이면 해당 셀은 저장하지 않고 스킵. (이미 기록됨/정상 차감은 통과하여 근무계획 저장)
    		if (workPlanCd != null && legalLeaveCds.contains(workPlanCd)) {
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
    	// 관리 기능 권한 가드 (prafta-021 보안) — 매니저(MASTER/HR) 전용.
    	if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
    		log.warn("근무계획 삭제 권한 없음 - authCd={}", param.gvAuthCd());
    		throw new ApiException(AttdErrorCode.ATTD_403_002);
    	}
    	for(SchTypeDeleModel model : param.schTypeDeleModelList()) {
    		// PRAFTA-028 - 마감된 기간(부서)의 근무계획 삭제 차단
    		if (attdCloseService.isClosedForUser(model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.workYm())) {
    			throw new ApiException(AttdErrorCode.ATTD_400_042);
    		}
    		attd05Mapper.deleteUserWorkPlans(SchTypeDeleCommand.from(model));
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
    	return "근무타입을 지정할 수 없는 날짜입니다.";
    }
}
