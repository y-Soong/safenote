package com.prafta.web.attd.attd01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.schedule.service.ScheduleChangeGuardService;
import com.prafta.common.cmm.schedule.vo.ScheduleLockVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.application.command.SchInfoCommand;
import com.prafta.web.attd.attd01.application.command.SchInfoHistCommand;
import com.prafta.web.attd.attd01.application.command.ShiftAssignCommand;
import com.prafta.web.attd.attd01.application.command.ShiftPatternCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTypeCommand;
import com.prafta.web.attd.attd01.application.param.SchInfoHistParam;
import com.prafta.web.attd.attd01.application.param.SchInfoListParam;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchDetailParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoListParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftAssignParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftPatternParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftTeamParam;
import com.prafta.web.attd.attd01.application.query.SchCdQuery;
import com.prafta.web.attd.attd01.application.query.SchInfoHistQuery;
import com.prafta.web.attd.attd01.application.query.SchInfoListQuery;
import com.prafta.web.attd.attd01.application.query.SchNoCountQuery;
import com.prafta.web.attd.attd01.application.query.ShiftCdQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchDetailQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchInfoListQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchNoCountQuery;
import com.prafta.web.attd.attd01.dto.response.SchInfoHistResponse;
import com.prafta.web.attd.attd01.dto.response.SchInfoListResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchDetailResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchInfoListResponse;
import com.prafta.web.attd.attd01.mapper.Attd01Mapper;
import com.prafta.web.attd.attd01.result.SchHistResult;
import com.prafta.web.attd.attd01.result.SchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftAssignInfoResult;
import com.prafta.web.attd.attd01.result.ShiftPatternInfoResult;
import com.prafta.web.attd.attd01.result.ShiftSchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTypeInfoResult;
import com.prafta.web.attd.attd01.result.WorkPlanDayResult;
import com.prafta.web.attd.attd01.service.Attd01Service;
import com.prafta.web.subcon.subcon02.service.SiteLinkPropagationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd01ServiceImpl implements Attd01Service{

	/** YYYYMMDD 포맷터(가드 기준일·근무계획 일자 비교용). */
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final Attd01Mapper attd01Mapper;
	private final ScheduleChangeGuardService scheduleChangeGuardService;

	// PRAFTA-SUBCON-T2-05: 원본 근무타입 변경의 미러 재귀 전파(동기 + 동일 트랜잭션 — 실패 시 전체 롤백).
	private final SiteLinkPropagationService siteLinkPropagationService;

	public Attd01ServiceImpl(Attd01Mapper attd01Mapper,
			ScheduleChangeGuardService scheduleChangeGuardService,
			SiteLinkPropagationService siteLinkPropagationService) {
		this.attd01Mapper = attd01Mapper;
		this.scheduleChangeGuardService = scheduleChangeGuardService;
		this.siteLinkPropagationService = siteLinkPropagationService;
	}
	
	public SchInfoListResponse selectSchInfoList(SchInfoListParam param) {
		
		SchInfoListResponse response = null;
		
		List<SchInfoResult> schInfoResultList = attd01Mapper.selectSchInfoList(SchInfoListQuery.from(param));
		
		if(schInfoResultList != null && !schInfoResultList.isEmpty()) {
			response = SchInfoListResponse.builder()
									.schInfoResultList(schInfoResultList)
									.build();
		}
		
		return response;
	
	}
	
	@Override
	@Transactional
	public void updateSchInfo(SchInfoParam param) {

		// PRAFTA-SUBCON-T2-04: 미러 사업장 근무타입 전면 잠금(신규 생성 포함, 예외 없음 — §5-5).
		//   미러 근무타입은 원본 소유사의 전파로만 갱신된다. authCd/회사 스코프는 JWT 도출값만 신뢰.
		if (attd01Mapper.selectSiteLinkSrcCmpny(param.gvCmpnyCd(), param.siteCd()) != null) {
			log.warn("미러 사업장 근무타입 수정 거부 - gvCmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());
			throw new ApiException(SubconErrorCode.SUBCON_403_003);
		}

		String schCd = null;

		// 근무코드(SCH_NO) 중복 검사: 신규/수정 공통.
		// - 신규: param.schCd() 가 빈값이라 SchNoCountQuery 의 schCd 조건이 무시되어 동일 코드 존재 시 차단.
		// - 수정: 자기 자신(schCd)을 제외하고 다른 근무타입과의 코드 충돌만 차단.
		//   (프론트는 수정 모드에서 코드 입력을 비활성화하나, 클라이언트 우회 시도에 대한 서버 방어)
		int schNoCnt = attd01Mapper.selectSchNoCount(SchNoCountQuery.from(param));

		if(schNoCnt > 0) {
			throw new ApiException(AttdErrorCode.ATTD_400_001);
		}

		boolean isEdit = param.schCd() != null && !param.schCd().equals("");
		if(isEdit) {
			schCd = param.schCd();
		} else {
			schCd = attd01Mapper.selectSchCd(SchCdQuery.from(param));
		}

		SchInfoCommand command = SchInfoCommand.from(param, schCd);

		// com-016-A 공통 가드 ③: 근무타입의 시간/휴게 변경 시,
		// 그 타입을 쓰는 미래 적용분 근무계획에 시간차/반차 연차 또는 OT가 걸린 날이 있으면 하드 차단.
		// 신규 생성(isEdit=false)은 기존 근무계획이 없으므로 가드 불필요. 시간/휴게 외 변경(이름·사용여부 등)은 통과.
		if(isEdit) {
			guardScheduleTimeChange(command);
			guardScheduleDeactivate(command);
		}

		attd01Mapper.updateSchInfo(command);

		// 이력 시퀀스는 확정된 스케줄 코드(신규 발급 또는 기존) 기준으로 계산해야 한다.
		// 기존에는 param.schCd()(신규 시 빈값) 기준으로 계산되어 신규 생성 이력이 어긋날 수 있었다.
		int histIdx = attd01Mapper.selectSchHistIdx(SchInfoHistQuery.of(param, schCd));

		attd01Mapper.insertSchHistInfo(SchInfoHistCommand.from(param, histIdx, schCd));

		// PRAFTA-SUBCON-T2-05: 저장 후 미러 재귀 전파(신규 추가·사용중지·APPLY_DATE 포함 —
		//   활성 링크 없으면 no-op, 실패 시 원본 저장 전체 롤백. 미러 테넌트 HIST 도 함께 기록).
		siteLinkPropagationService.propagateSchInfo(param.gvCmpnyCd(), param.siteCd(), schCd);
	}

	/**
	 * com-016-A 공통 가드 ③: 근무타입(SCH_CD)의 시간/휴게 변경을 하드 차단할지 판정한다.
	 *
	 * <ol>
	 *   <li>시간/휴게가 실제로 바뀌는 경우에만 가드(이름·사용여부 등만 바뀌면 통과).</li>
	 *   <li>그 SCH_CD 를 쓰는 미래 적용분 근무계획(WORK_YMD &gt;= 기준일)을 조회. 기준일 = max(오늘, APPLY_DATE).</li>
	 *   <li>(userCd → 날짜목록) 그룹별로 {@link ScheduleChangeGuardService#findLockedDays} 호출 →
	 *       OT 또는 시간차/반차 연차(종일 '00' 제외)가 1건이라도 있으면 ATTD_400_162 로 차단.</li>
	 * </ol>
	 */
	private void guardScheduleTimeChange(SchInfoCommand command) {

		// (1) 시간/휴게 변경 여부 — 변경 없으면 가드 종료.
		int changed = attd01Mapper.selectSchTimeBreakChanged(command);
		if(changed == 0) {
			return;
		}

		// (2) 기준일 = max(오늘, APPLY_DATE). APPLY_DATE 가 미래면 그날부터, 아니면 오늘부터.
		String todayYmd = LocalDate.now().format(YMD);
		String applyYmd = command.applyDate() == null ? "" : command.applyDate().replace("-", "").trim();
		String fromYmd = (applyYmd.length() == 8 && applyYmd.compareTo(todayYmd) > 0) ? applyYmd : todayYmd;

		List<WorkPlanDayResult> futurePlans = attd01Mapper.selectFutureWorkPlanDaysBySchCd(
				command.gvCmpnyCd(), command.siteCd(), command.schCd(), fromYmd);

		if(futurePlans == null || futurePlans.isEmpty()) {
			return;
		}

		// (3) userCd 별로 날짜목록을 모아 공통 가드 호출.
		Map<String, List<String>> daysByUser = new LinkedHashMap<>();
		for(WorkPlanDayResult plan : futurePlans) {
			daysByUser.computeIfAbsent(plan.userCd(), k -> new ArrayList<>()).add(plan.workYmd());
		}

		List<String> blockedYmds = new ArrayList<>();
		for(Map.Entry<String, List<String>> entry : daysByUser.entrySet()) {
			List<ScheduleLockVO> locks = scheduleChangeGuardService.findLockedDays(
					command.gvCmpnyCd(), command.siteCd(), entry.getKey(), entry.getValue());
			for(ScheduleLockVO lock : locks) {
				// OT 는 전부 대상. LEAVE 는 종일('00') 제외 — 시간차/반차만 시간 변경에 영향.
				boolean isTimeSensitiveLeave = lock.getReason() == ScheduleLockVO.Reason.LEAVE
						&& !"00".equals(lock.getLeaveUseUnitType());
				boolean isOt = lock.getReason() == ScheduleLockVO.Reason.OT;
				if(isOt || isTimeSensitiveLeave) {
					blockedYmds.add(lock.getWorkYmd());
				}
			}
		}

		if(!blockedYmds.isEmpty()) {
			log.info("근무타입 시간/휴게 변경 차단 - schCd={}, siteCd={}, 영향 일자 {}건: {}",
					command.schCd(), command.siteCd(), blockedYmds.size(), blockedYmds);
			throw new ApiException(AttdErrorCode.ATTD_400_162);
		}
	}

	/**
	 * 사용중지 가드: 근무타입(SCH_CD)을 사용중지(USE_YN Y→N)하려는데 그 타입이 오늘 이후 근무계획에
	 * 배정돼 있으면 ATTD_400_163 으로 하드 차단한다.
	 *
	 * <p>사용중지된 근무타입이 근무계획에 남아 있으면 조회 쿼리들이 그날을 유효 스케줄로 보지 않아
	 * 앱 홈이 "스케줄 없음"으로 빠지고, 출퇴근 판정·연차 차감·출퇴근 리마인더까지 함께 어긋난다.
	 * 관리자가 근무계획관리에서 해당 일자의 근무타입을 먼저 바꾼 뒤 사용중지하도록 강제한다.
	 *
	 * <p>차단 조건: 요청 USE_YN='N' + 기존 USE_YN='Y'(전환) + 오늘 이후 배정 근무계획 1건 이상.
	 * 이미 사용중지된 타입의 재저장(N→N, 이름 수정 등)은 통과시킨다(정상 흐름을 막지 않는다).
	 * 과거 근무일 배정분은 실제 이력이므로 검사하지 않는다.
	 */
	private void guardScheduleDeactivate(SchInfoCommand command) {

		if(!"N".equals(command.useYn())) {
			return;
		}

		// 기존이 이미 'N' 이거나 행이 없으면(신규) 전환이 아니므로 통과.
		String currentUseYn = attd01Mapper.selectSchUseYn(
				command.gvCmpnyCd(), command.siteCd(), command.schCd());
		if(!"Y".equals(currentUseYn)) {
			return;
		}

		String todayYmd = LocalDate.now().format(YMD);
		List<WorkPlanDayResult> futurePlans = attd01Mapper.selectFutureWorkPlanDaysBySchCd(
				command.gvCmpnyCd(), command.siteCd(), command.schCd(), todayYmd);

		if(futurePlans != null && !futurePlans.isEmpty()) {
			log.info("근무타입 사용중지 차단 - schCd={}, siteCd={}, 배정된 미래 근무계획 {}건",
					command.schCd(), command.siteCd(), futurePlans.size());
			throw new ApiException(AttdErrorCode.ATTD_400_163);
		}
	}

	public SchInfoHistResponse selectSchHistList(SchInfoHistParam param) {
		
		
		SchInfoHistResponse response = null;
		
		List<SchHistResult> schHistResultList = attd01Mapper.selectSchHistList(SchInfoHistQuery.from(param));
		
		if(schHistResultList != null && schHistResultList.size() > 0) {
			response = SchInfoHistResponse.builder()
											.schHistResultList(schHistResultList)
											.build();
		}
		
		return response;
	}
	
	@Transactional
	public void updateShiftSchInfo(ShiftSchInfoParam param) {
		
		int shiftSchNoCnt = attd01Mapper.selectShiftSchNoCount(ShiftSchNoCountQuery.from(param.shiftType(), param.gvCmpnyCd()));
		
		if(shiftSchNoCnt > 0) {
			throw new ApiException(AttdErrorCode.ATTD_400_002);
		}
		
		String shiftCd = attd01Mapper.selectShiftCd(ShiftCdQuery.from(param));
		
		attd01Mapper.insertShiftSch(ShiftTypeCommand.from(param, shiftCd));
		
		if(param.shiftPatternList() != null && param.shiftPatternList().size() > 0) {
			for(ShiftPatternParam shiftPatternParam : param.shiftPatternList()) {
				attd01Mapper.insertShiftSchPtrn(ShiftPatternCommand.from(shiftPatternParam, shiftCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}
		
		if(param.shiftTeamList() != null && param.shiftTeamList().size() > 0) {
			for(ShiftTeamParam shiftTeamParam : param.shiftTeamList()) {
				
				attd01Mapper.insertShiftSchTeam(ShiftTeamCommand.from(shiftTeamParam, shiftCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}
		
		if(param.shiftAssignList() != null && param.shiftAssignList().size() > 0) {
			for(ShiftAssignParam shiftAssignParam : param.shiftAssignList()) {

				attd01Mapper.insertShiftSchAssign(ShiftAssignCommand.from(shiftAssignParam, shiftCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}
	}
	
	public ShiftSchInfoListResponse selectShiftSchInfoList(ShiftSchInfoListParam param) {
		
		ShiftSchInfoListResponse response = null;
		
		List<ShiftSchInfoResult> shiftSchInfoResultList = attd01Mapper.selectShiftSchInfoList(ShiftSchInfoListQuery.from(param));
		
		if(shiftSchInfoResultList != null && shiftSchInfoResultList.size() > 0) {
			response = ShiftSchInfoListResponse.builder()
													.shiftSchInfoResultList(shiftSchInfoResultList)
													.build();
		}
	
		
		return response;
	}
	
	public ShiftSchDetailResponse selectShiftSchDetail(ShiftSchDetailParam param) {
		
		ShiftSchDetailResponse response = null;
		
		List<ShiftTypeInfoResult> shiftTypeInfoResultList = attd01Mapper.selectShiftTypeInfoList(ShiftSchDetailQuery.from(param));
		
		List<ShiftPatternInfoResult> shiftPatternInfoResultList = attd01Mapper.selectShiftPatternInfoList(ShiftSchDetailQuery.from(param));
		
		List<ShiftTeamInfoResult> shiftTeamInfoResultList = attd01Mapper.selectShiftTeamInfoList(ShiftSchDetailQuery.from(param));
		
		List<ShiftAssignInfoResult> shiftAssignInfoResultList = attd01Mapper.selectShiftAssignInfoList(ShiftSchDetailQuery.from(param));
		
		response = ShiftSchDetailResponse.builder()
											.shiftTypeInfoResultList(shiftTypeInfoResultList)
											.shiftPatternInfoResultList(shiftPatternInfoResultList)
											.shiftTeamInfoResultList(shiftTeamInfoResultList)
											.shiftAssignInfoResultList(shiftAssignInfoResultList)
											.build();
		
		return response;
	}
}
