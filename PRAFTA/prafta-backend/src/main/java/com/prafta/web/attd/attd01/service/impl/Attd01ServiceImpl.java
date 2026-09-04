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
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AttdSchLockMessages;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd01.application.command.SchInfoCommand;
import com.prafta.web.attd.attd01.application.command.SchInfoHistCommand;
import com.prafta.web.attd.attd01.application.command.ShiftAssignCommand;
import com.prafta.web.attd.attd01.application.command.ShiftPatternCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTypeCommand;
import com.prafta.web.attd.attd01.application.param.AssignedUsersParam;
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
import com.prafta.web.attd.attd01.dto.response.AssignedUsersResponse;
import com.prafta.web.attd.attd01.dto.response.SchInfoHistResponse;
import com.prafta.web.attd.attd01.dto.response.SchInfoListResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchDetailResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchInfoListResponse;
import com.prafta.web.attd.attd01.mapper.Attd01Mapper;
import com.prafta.web.attd.attd01.result.AssignedUserResult;
import com.prafta.web.attd.attd01.result.SchHistResult;
import com.prafta.web.attd.attd01.result.SchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftAssignInfoResult;
import com.prafta.web.attd.attd01.result.ShiftPatternInfoResult;
import com.prafta.web.attd.attd01.result.ShiftSchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTypeInfoResult;
import com.prafta.web.attd.attd01.result.WorkPlanDayResult;
import com.prafta.web.attd.attd01.service.Attd01Service;
import com.prafta.web.attd.attd07.service.AttdCloseService;
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

	// F-12-2: 배정현황 조회(PII 노출 신규 화면) cross-site IDOR 가드 — User_03 원장 기반 사업장 접근 인가.
	private final SiteAccessService siteAccessService;

	// 보안수정(security High): 배정현황 조회 부서 스코프 게이트(canManageNode) — Attd_11/Attd_16 전례와 동일 빈 재사용.
	private final AttdCloseService attdCloseService;

	public Attd01ServiceImpl(Attd01Mapper attd01Mapper,
			ScheduleChangeGuardService scheduleChangeGuardService,
			SiteLinkPropagationService siteLinkPropagationService,
			SiteAccessService siteAccessService,
			AttdCloseService attdCloseService) {
		this.attd01Mapper = attd01Mapper;
		this.scheduleChangeGuardService = scheduleChangeGuardService;
		this.siteLinkPropagationService = siteLinkPropagationService;
		this.siteAccessService = siteAccessService;
		this.attdCloseService = attdCloseService;
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

		// F-2: 휴게시각 서버 검증(fail-open 봉합). 신규/수정 공통 — API 직접 호출 등 프론트 우회 경로도 방어.
		//   BW-10(G-6): 공용 BreakTimeValidator 로 위임(Platform_01 프로비저닝과 같은 함수) — 폭≠분 거부 + '2400'/자정 넘김 종료 파싱.
		validateBreakTimeRange(command);

		// BW-10(G-6): 휴게 종료 미전송(구 FE/API 직접 호출)이면 시작 + 휴게분으로 서버가 파생 저장(FE 파생과 동치).
		//   전송된 값은 위 검증(폭 == 분)을 통과한 그대로 둔다. 현행/이력 짝 규약대로 이력 스냅샷에도 동일 값.
		String fstBrkEndTime = resolveBreakEnd(command.fstBrkEndTime(), command.fstBrkStrTime(), command.fstSchBrkMin());
		String secBrkEndTime = resolveBreakEnd(command.secBrkEndTime(), command.secBrkStrTime(), command.secSchBrkMin());
		command = command.withBreakEndTimes(fstBrkEndTime, secBrkEndTime);

		// PRAFTA-FIXEDOT-1: 고정연장근무 검증(V1~V6). 고정연장 미입력(4필드 전부 NULL)이면 즉시 통과
		// — 기존 근무타입 저장 경로는 동작 변화 없음.
		validateFixedOt(command);

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

		attd01Mapper.insertSchHistInfo(SchInfoHistCommand.from(param, histIdx, schCd)
				.withBreakEndTimes(fstBrkEndTime, secBrkEndTime)); // BW-10: 파생 종료를 이력에도 동일 반영

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
			// F-12-1: 차단 대상 일자·건수를 응답 메시지에 실어 관리자가 원인을 바로 알 수 있게 한다
			// (이름/USER_CD 는 절대 넣지 않는다 — AttdOverlapMessages PII 규약 승계).
			throw new ApiException(AttdErrorCode.ATTD_400_162,
					AttdSchLockMessages.timeChangeBlockedMessage(blockedYmds));
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
			// F-12-1: 배정 일자·건수를 응답 메시지에 실어 관리자가 원인을 바로 알 수 있게 한다
			// (이름/USER_CD 는 절대 넣지 않는다 — AttdOverlapMessages PII 규약 승계).
			List<String> futureYmds = futurePlans.stream()
					.map(WorkPlanDayResult::workYmd)
					.toList();
			throw new ApiException(AttdErrorCode.ATTD_400_163,
					AttdSchLockMessages.deactivateBlockedMessage(futureYmds));
		}
	}

	/**
	 * F-2: 근무타입 저장 시 휴게시각의 서버측 검증(fail-open 봉합).
	 *
	 * <p>프론트 {@code SchInfoPop.vue validateWorkTime} 의 오버나이트 {@code withinSpan}
	 * ([start,24:00)∪[00:00,end)) 로직을 서버로 이식하고, 프론트에도 없던 "휴게 종료
	 * (=휴게 시작+휴게분)가 근무범위를 초과하는지" 검증을 추가한다(갭2, 신규 규칙).
	 *
	 * <p>휴게시각(BRK_STR_TIME)이 NULL/빈값이면 검증을 생략한다 — 휴게시간은 선택 항목이며
	 * (정책서 attd §3.2), 값을 새로 입력하지 않고 저장하는 기존 행(예: 회사 001 25건 중 4건처럼
	 * BRK_MIN&gt;0인데 BRK_STR_TIME이 NULL인 운영 데이터)은 이 검증에 걸리지 않는다.
	 *
	 * <p>schType='02'(2구간)는 각 구간이 자기 근무범위 안에서만 벗어나지 않으면 되므로
	 * withinSpan 을 구간별로 독립 적용한다(교차 케이스 별도 규칙 불필요).
	 */
	private void validateBreakTimeRange(SchInfoCommand command) {
		// BW-10(G-6): 검증 본문은 공용 BreakTimeValidator(common.cmm.sch.util)로 이관 — 프로비저닝 경로와 단일 함수.
		//   종전 3규칙(휴게분 ≤ 근무시간 / 시작 ∈ 범위 / 시작+분 ≤ 종료) + 신규 "휴게 시각 폭 == 휴게분"(종료 전송 시).
		com.prafta.common.cmm.sch.util.BreakTimeValidator.validate(command.schType(),
				command.fstSchStrTime(), command.fstSchEndTime(), command.fstSchBrkMin(),
				command.fstBrkStrTime(), command.fstBrkEndTime(),
				command.secSchStrTime(), command.secSchEndTime(), command.secSchBrkMin(),
				command.secBrkStrTime(), command.secBrkEndTime());
	}

	/**
	 * BW-10(G-6): 저장할 휴게 종료 시각. 전송값이 있으면(검증 통과분) 그대로, 없으면 시작 + 휴게분 파생(시작도 없으면 null).
	 */
	private static String resolveBreakEnd(String sentBrkEndTime, String brkStrTime, String schBrkMin) {
		if(sentBrkEndTime != null && !sentBrkEndTime.isBlank()) {
			// ★ 프론트는 "HH:mm"(콜론 포함)으로 보낸다. 컬럼은 varchar(4) 이므로 HHMM 4자리로 정규화해 저장한다 (2026-09-04 QA 적발).
			String normalized = com.prafta.common.cmm.sch.util.BreakTimeValidator.normalizeHhmm(sentBrkEndTime);
			if(normalized != null) {
				return normalized;
			}
		}
		return com.prafta.common.cmm.sch.util.BreakTimeValidator.deriveBreakEnd(brkStrTime, schBrkMin);
	}

	/** "HH:mm" 또는 "HHmm" 문자열을 0~1440(24:00 포함) 분값으로 변환. 형식 오류/미입력 시 null. */
	private static Integer toBreakMinutes(String v) {
		if(v == null) {
			return null;
		}
		String s = v.trim().replaceAll("\\D", "");
		if(s.length() < 4) {
			return null;
		}
		int h = Integer.parseInt(s.substring(0, 2));
		int m = Integer.parseInt(s.substring(2, 4));
		if(h == 24 && m == 0) {
			return 24 * 60;
		}
		if(h < 0 || h > 23 || m < 0 || m > 59) {
			return null;
		}
		return h * 60 + m;
	}

	/** 휴게시간(분) 문자열 파싱. 미입력/파싱 불가 시 0. */
	private static int parseBreakMin(String v) {
		if(v == null || v.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch(NumberFormatException e) {
			return 0;
		}
	}

	/** 오버나이트 고려 근무 길이(분): 종료&lt;=시작이면 자정을 넘긴 것으로 보고 1440을 더한다. */
	private static int breakSpanMinutes(int start, int end) {
		return end > start ? end - start : end + 1440 - start;
	}

	/**
	 * PRAFTA-FIXEDOT-1: 근무타입 고정연장근무(전방·후방 FROM/TO 2쌍) 서버 검증 — plan §1-2 V1~V6.
	 *
	 * <ul>
	 *   <li>V1 쌍 완결성: 전방·후방 각각 시작/종료 둘 다 입력 또는 둘 다 미입력.</li>
	 *   <li>V2 전방 위치: 당일 내 구간(전일 걸침 미지원) + 종료가 소정 1구간 시작 이하(같으면 연속 허용).</li>
	 *   <li>V3 후방 위치: 시작이 소정 마지막 구간(schType 02 면 2구간) 종료 이상(같으면 연속 허용).
	 *       소정 구간이 자정을 넘기는 타입은 일자 프레임 빈 구간 [마지막 소정 종료, 1구간 시작) 안만.</li>
	 *   <li>V4 겹침 금지(일반화 — qa G1~G3 봉합): 소정 1·2구간 + 전방·후방 점유를 일자 프레임
	 *       [0,1440) 구간으로 전개(자정 넘김은 wrap 분할)해 전 쌍(pairwise) 겹침 검사.
	 *       V2/V3 방향성 검사의 안전망.</li>
	 *   <li>V5 자정 넘김: 후방은 종료&lt;=시작이면 +1440 해석(기존 스케줄 오버나이트 규약 준용).
	 *       단 소정 구간이 이미 자정을 넘기는 타입에서는 후방의 재차 자정 넘김 불가.</li>
	 *   <li>V6 휴게 적법성: 소정+고정연장 합산 실근로 기준으로 법정 휴게(4h 이상 30분·8h 이상 60분)
	 *       충족 검증. ★고정연장이 있을 때만 발동 — 고정연장 없는 기존 근무타입의 저장 동작은 불변.</li>
	 * </ul>
	 *
	 * <p>V7(고정연장 4h 초과 경고)은 비차단 경고라 프론트(SchInfoPop) 전담. 이웃날(전일/익일) 점유
	 * 겹침은 2단계 겹침 가드(M9·J9) 범위라 여기서 판단하지 않는다.
	 */
	private void validateFixedOt(SchInfoCommand command) {

		boolean hasPreStr = hasTimeValue(command.preFixedOtStrTime());
		boolean hasPreEnd = hasTimeValue(command.preFixedOtEndTime());
		boolean hasRearStr = hasTimeValue(command.fixedOtStrTime());
		boolean hasRearEnd = hasTimeValue(command.fixedOtEndTime());

		// V1: 쌍 완결성 — 한쪽만 입력 시 거부.
		if(hasPreStr != hasPreEnd) {
			throw new ApiException(AttdErrorCode.ATTD_400_198,
					"전방 고정연장근무는 시작·종료 시각을 모두 입력해야 합니다.");
		}
		if(hasRearStr != hasRearEnd) {
			throw new ApiException(AttdErrorCode.ATTD_400_198,
					"후방 고정연장근무는 시작·종료 시각을 모두 입력해야 합니다.");
		}

		boolean hasPre = hasPreStr;
		boolean hasRear = hasRearStr;
		// 고정연장 미사용 — 기존 근무타입 저장 경로 그대로 통과(무회귀 핵심).
		if(!hasPre && !hasRear) {
			return;
		}

		// 소정 구간 시각 파싱 — 고정연장 위치 검증의 기준. 비정상이면 판단 불가로 차단(fail-closed).
		Integer fstStart = toBreakMinutes(command.fstSchStrTime());
		Integer fstEnd = toBreakMinutes(command.fstSchEndTime());
		if(fstStart == null || fstEnd == null || fstStart.equals(fstEnd)) {
			throw new ApiException(AttdErrorCode.ATTD_400_198,
					"소정 근무시간이 올바르지 않아 고정연장근무를 설정할 수 없습니다.");
		}
		boolean twoSeg = "02".equals(command.schType());
		Integer secStart = null;
		Integer secEnd = null;
		if(twoSeg) {
			secStart = toBreakMinutes(command.secSchStrTime());
			secEnd = toBreakMinutes(command.secSchEndTime());
			if(secStart == null || secEnd == null || secStart.equals(secEnd)) {
				throw new ApiException(AttdErrorCode.ATTD_400_198,
						"소정 근무시간이 올바르지 않아 고정연장근무를 설정할 수 없습니다.");
			}
		}

		// 소정 구간 자정 넘김 여부 — 하나라도 넘기면(anyWrap) 후방은 일자 프레임의
		// [마지막 소정 종료, 1구간 시작) 빈 구간 안에만 허용된다(qa G1 봉합 — 2구간 사이 배치 금지 포함).
		boolean seg1Wrap = fstEnd < fstStart;
		boolean seg2Wrap = twoSeg && secEnd < secStart;
		boolean anyWrap = seg1Wrap || seg2Wrap;
		int lastEnd = twoSeg ? secEnd : fstEnd;

		Integer preStr = null;
		Integer preEnd = null;
		Integer rearStr = null;
		Integer rearEnd = null;
		int preDur = 0;
		int rearDur = 0;

		if(hasPre) {
			preStr = toBreakMinutes(command.preFixedOtStrTime());
			preEnd = toBreakMinutes(command.preFixedOtEndTime());
			if(preStr == null || preEnd == null) {
				throw new ApiException(AttdErrorCode.ATTD_400_198,
						"전방 고정연장근무 시각 형식이 올바르지 않습니다.");
			}
			// V2: 전방은 당일 내 구간만(전일 걸침 미지원 — plan §1-2 V2 확정).
			if(preStr >= preEnd) {
				throw new ApiException(AttdErrorCode.ATTD_400_198,
						"전방 고정연장근무는 당일 내 구간이어야 합니다(시작 시각이 종료 시각보다 빨라야 합니다).");
			}
			// V2: 종료 <= 소정 1구간 시작(같으면 연속 — 허용). 소정 새벽 잔여 점유와의 겹침은
			// 아래 pairwise 전수 검사가 잡는다(qa G2).
			if(preEnd > fstStart) {
				throw new ApiException(AttdErrorCode.ATTD_400_198,
						"전방 고정연장근무 종료 시각은 소정 근무 시작 시각 이전이어야 합니다.");
			}
			preDur = preEnd - preStr;
		}

		if(hasRear) {
			rearStr = toBreakMinutes(command.fixedOtStrTime());
			rearEnd = toBreakMinutes(command.fixedOtEndTime());
			if(rearStr == null || rearEnd == null) {
				throw new ApiException(AttdErrorCode.ATTD_400_198,
						"후방 고정연장근무 시각 형식이 올바르지 않습니다.");
			}
			// 시작=종료는 자정 넘김 규약(V5)상 24시간 해석이 되므로 거부.
			if(rearStr.equals(rearEnd)) {
				throw new ApiException(AttdErrorCode.ATTD_400_198,
						"후방 고정연장근무 시작 시각과 종료 시각이 같을 수 없습니다.");
			}
			if(!anyWrap) {
				// V3: 시작 >= 소정 마지막 구간 종료(같으면 연속 — 허용). 익일 걸침(종료<=시작)은 V5 로 허용.
				if(rearStr < lastEnd) {
					throw new ApiException(AttdErrorCode.ATTD_400_198,
							"후방 고정연장근무 시작 시각은 소정 근무 종료 시각 이후여야 합니다.");
				}
			} else {
				// 소정이 자정을 넘기는 타입(예: 22~06 또는 2구간 19~03): 일자 프레임 소정 점유는
				// [시작,24:00)∪[00:00,종료) 로 감기므로, 후방은 빈 구간 [마지막 소정 종료, 1구간 시작)
				// 안에서 시작해야 한다(qa G1: 2구간 사이·1구간 내부 배치 차단).
				if(rearStr < lastEnd || rearStr >= fstStart) {
					throw new ApiException(AttdErrorCode.ATTD_400_198,
							"후방 고정연장근무 시작 시각은 소정 근무 종료 시각 이후여야 합니다.");
				}
				if(rearEnd < rearStr) {
					throw new ApiException(AttdErrorCode.ATTD_400_198,
							"소정 근무가 자정을 넘기는 근무타입에서는 후방 고정연장근무가 다시 자정을 넘길 수 없습니다.");
				}
			}
			rearDur = breakSpanMinutes(rearStr, rearEnd);
		}

		// V4(일반화 — qa G1~G3 봉합): 소정 1·2구간 + 전방·후방 점유를 일자 프레임 [0,1440) 구간으로
		// 전개(자정 넘김은 [시작,24:00)∪[00:00,종료) 분할 — +1440 규약과 등가)해 전 쌍(pairwise) 겹침 검사.
		// 위 V2/V3 방향성 검사(명확한 안내 메시지 용도)의 뒤를 받치는 안전망이다.
		List<int[]> occupancies = new ArrayList<>();
		addDayFrameOccupancy(occupancies, 0, fstStart, fstEnd);
		if(twoSeg) {
			addDayFrameOccupancy(occupancies, 1, secStart, secEnd);
		}
		if(hasPre) {
			addDayFrameOccupancy(occupancies, 2, preStr, preEnd);
		}
		if(hasRear) {
			addDayFrameOccupancy(occupancies, 3, rearStr, rearEnd);
		}
		String[] segLabels = { "소정 1구간", "소정 2구간", "전방 고정연장", "후방 고정연장" };
		for(int i = 0; i < occupancies.size(); i++) {
			for(int j = i + 1; j < occupancies.size(); j++) {
				int[] a = occupancies.get(i);
				int[] b = occupancies.get(j);
				// 같은 구간의 분할 조각끼리는 비교하지 않는다.
				if(a[0] == b[0]) {
					continue;
				}
				if(a[1] < b[2] && b[1] < a[2]) {
					throw new ApiException(AttdErrorCode.ATTD_400_198,
							segLabels[a[0]] + " 시간과 " + segLabels[b[0]] + " 시간이 겹칩니다.");
				}
			}
		}

		// V6: 휴게 적법성 — 소정+고정연장 합산 실근로 기준. 고정연장 존재 시에만 발동(기존 타입 무회귀).
		int fstBrk = parseBreakMin(command.fstSchBrkMin());
		int secBrk = twoSeg ? parseBreakMin(command.secSchBrkMin()) : 0;
		int workMin = Math.max(0, breakSpanMinutes(fstStart, fstEnd) - fstBrk);
		if(twoSeg) {
			workMin += Math.max(0, breakSpanMinutes(secStart, secEnd) - secBrk);
		}
		int totalWorkMin = workMin + preDur + rearDur;
		int requiredBreakMin = totalWorkMin >= 480 ? 60 : (totalWorkMin >= 240 ? 30 : 0);
		if(fstBrk + secBrk < requiredBreakMin) {
			throw new ApiException(AttdErrorCode.ATTD_400_198,
					"소정+고정연장 합산 근로시간(" + totalWorkMin + "분) 기준 법정 휴게시간("
							+ requiredBreakMin + "분) 이상을 입력해야 합니다.");
		}
	}

	/** 시각 문자열 입력 여부(NULL/빈값이면 미입력). */
	private static boolean hasTimeValue(String v) {
		return v != null && !v.isBlank();
	}

	/**
	 * PRAFTA-FIXEDOT-1(V4 일반화): 구간 1개를 일자 프레임 [0,1440) 점유 구간으로 전개해 목록에 추가.
	 * 종료&lt;=시작(자정 넘김)이면 [시작,24:00)∪[00:00,종료) 두 조각으로 분할한다.
	 * 원소 형식: {구간ID, 시작분, 종료분} — 반개구간 [시작, 종료).
	 */
	private static void addDayFrameOccupancy(List<int[]> list, int segId, int start, int end) {
		if(end > start) {
			list.add(new int[] { segId, start, end });
			return;
		}
		list.add(new int[] { segId, start, 1440 });
		if(end > 0) {
			list.add(new int[] { segId, 0, end });
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

		// SHIFT-LINK-T4: 미러 사업장 교대근무 타입 정의 전면 잠금(신규 생성 거부 — T2-04 근무타입 게이트 동형).
		//   미러 교대 정의는 원본 소유사의 전파로만 갱신된다. authCd/회사 스코프는 JWT 도출값만 신뢰
		//   (siteCd 는 요청값이나 gvCmpnyCd 스코프 내 판정이라 타 테넌트 영향 없음 — updateSchInfo 게이트 동일).
		//   조회(selectShiftSchInfoList/selectShiftSchDetail)는 차단하지 않는다(지시서 §3.3).
		String siteCd = param.shiftType().siteCd();
		if (attd01Mapper.selectSiteLinkSrcCmpny(param.gvCmpnyCd(), siteCd) != null) {
			log.warn("미러 사업장 교대근무 타입 생성 거부 - gvCmpnyCd={}, siteCd={}", param.gvCmpnyCd(), siteCd);
			throw new ApiException(SubconErrorCode.SUBCON_403_006);
		}

		int shiftSchNoCnt = attd01Mapper.selectShiftSchNoCount(ShiftSchNoCountQuery.from(param.shiftType(), param.gvCmpnyCd()));
		
		if(shiftSchNoCnt > 0) {
			throw new ApiException(AttdErrorCode.ATTD_400_002);
		}
		
		String shiftCd = attd01Mapper.selectShiftCd(ShiftCdQuery.from(param));
		
		attd01Mapper.insertShiftSch(ShiftTypeCommand.from(param, shiftCd));
		
		// SEC-1: 하위 항목(패턴/팀/배정표)의 siteCd 는 게이트를 통과한 부모(siteCd = shiftType.siteCd)로
		//   서버 강제 — 요청 항목별 siteCd 를 신뢰하면 부모만 게이트 판정되는 구조상 미러 사업장
		//   하위 테이블에 임의 행 주입이 가능해진다(보안 강제 — security SEC-1 / qa D-1).
		if(param.shiftPatternList() != null && param.shiftPatternList().size() > 0) {
			for(ShiftPatternParam shiftPatternParam : param.shiftPatternList()) {
				attd01Mapper.insertShiftSchPtrn(ShiftPatternCommand.from(shiftPatternParam, shiftCd, siteCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}

		if(param.shiftTeamList() != null && param.shiftTeamList().size() > 0) {
			for(ShiftTeamParam shiftTeamParam : param.shiftTeamList()) {

				attd01Mapper.insertShiftSchTeam(ShiftTeamCommand.from(shiftTeamParam, shiftCd, siteCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}

		if(param.shiftAssignList() != null && param.shiftAssignList().size() > 0) {
			for(ShiftAssignParam shiftAssignParam : param.shiftAssignList()) {

				attd01Mapper.insertShiftSchAssign(ShiftAssignCommand.from(shiftAssignParam, shiftCd, siteCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}

		// SHIFT-LINK-T3: 저장 후 교대 정의 미러 재귀 전파(신규 생성 전파 한정 — insert-only.
		//   활성 링크 없으면 no-op, 전파 실패 시 원본 저장 전체 롤백. 값은 DB 원본 행에서만 복제).
		siteLinkPropagationService.propagateShiftInfo(param.gvCmpnyCd(), siteCd, shiftCd);
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

	/**
	 * F-12-2: 근무타입별 배정현황 조회 — 정책서 attd/03-work-types.md §3.3
	 * ("근무 유형 삭제는 배정된 근로자가 없는 경우에만 가능") 이행을 관리자가 사전에 확인할 수 있게 한다.
	 * guardScheduleDeactivate 와 동일 기준(오늘 이후 배정)으로 집계해, 팝업 노출 인원이 실제
	 * 사용중지 시 차단되는 대상과 일치하도록 맞춘다.
	 *
	 * <p>보안수정(security High): assertSiteAccess 만으로는 "자기 사업장 fast path" 때문에
	 * 일반 사원도 인증만 되면 이 API 를 직접 호출해 자기 사업장 전 직원의 이름·부서·배정기간을
	 * 열람할 수 있다(Attd_11/Attd_16 전례와 동일 문제 — LNB 미노출은 서버 방어가 아니다).
	 * master/hr/safe 는 사업장 전체, 그 외는 canManageNode 로 검증한 본인 부서(+하위)만 허용한다.
	 * 이 화면은 nodeCd 를 요청 파라미터로 받지 않으므로(임의로 넓힐 여지 차단), 세션 클레임
	 * (gv_nodeCd)을 조회 범위의 앵커로 강제한다.
	 */
	@Override
	public AssignedUsersResponse selectAssignedUsers(AssignedUsersParam param) {

		log.info("근무타입 배정현황 조회 진입 - schCd={}, siteCd={}", param.schCd(), param.siteCd());

		// 신규 PII(사용자명·부서) 노출 조회 화면 — cross-site IDOR 방지(User_03 원장 기반 인가).
		siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(),
				param.gvSiteCd(), param.siteCd());

		// 역할·부서 스코프 게이트 — master/hr/safe 는 사업장 전체(scopeNodeCd=null),
		// 그 외는 본인 부서(gv_nodeCd)를 실제로 관리하는지 canManageNode 로 검증 후 그 부서(+하위)만 허용.
		boolean siteWide = AuthRoleUtils.canManageAllNodes(param.gvAuthCd());
		String scopeNodeCd = null;
		if (!siteWide) {
			if (param.gvNodeCd() == null || param.gvNodeCd().isBlank()
					|| !attdCloseService.canManageNode(param.gvAuthCd(), param.gvUserCd(),
							param.gvCmpnyCd(), param.siteCd(), param.gvNodeCd())) {
				log.warn("근무타입 배정현황 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
						param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.gvNodeCd());
				throw new ApiException(AttdErrorCode.ATTD_403_002);
			}
			scopeNodeCd = param.gvNodeCd();
		}

		String todayYmd = LocalDate.now().format(YMD);

		List<AssignedUserResult> assignedUserResultList = attd01Mapper.selectAssignedUsersBySchCd(
				param.gvCmpnyCd(), param.siteCd(), param.schCd(), todayYmd, scopeNodeCd);

		return AssignedUsersResponse.builder()
				.assignedUserResultList(assignedUserResultList)
				.build();
	}
}
