package com.prafta.web.attd.attd06.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.push.ShiftSchChangeNotiService;
import com.prafta.common.cmm.schedule.service.ScheduleChangeGuardService;
import com.prafta.common.cmm.schedule.vo.ScheduleLockVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd06.application.command.DeleteShiftTeamCommand;
import com.prafta.web.attd.attd06.application.command.DeleteShiftTeamUserCommand;
import com.prafta.web.attd.attd06.application.command.InsertShiftTeamUsersCommand;
import com.prafta.web.attd.attd06.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd06.application.command.ShiftTeamUserCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamLeadersCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamNmCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamPeriodCommand;
import com.prafta.web.attd.attd06.application.command.UserWorkPlanCommand;
import com.prafta.web.attd.attd06.application.model.InsertShiftTeamUsersModel;
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamParam;
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamUserParam;
import com.prafta.web.attd.attd06.application.param.InsertShiftTeamUsersParam;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftTeamUserInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftTypeDetailListsParam;
import com.prafta.web.attd.attd06.application.param.ShiftTypeListsParam;
import com.prafta.web.attd.attd06.application.param.ShiftUserSchInfosParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamLeadersParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamNmParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamPeriodParam;
import com.prafta.web.attd.attd06.application.param.UserListsParam;
import com.prafta.web.attd.attd06.application.query.SchCdListQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTeamUserInfosQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTypeDetailListsQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTypeListsQuery;
import com.prafta.web.attd.attd06.application.query.UserListsQuery;
import com.prafta.web.attd.attd06.dto.response.ShiftSchSaveResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTeamUserInfosResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeDetailListsResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeListsResponse;
import com.prafta.web.attd.attd06.dto.response.UserListsResponse;
import com.prafta.web.attd.attd06.mapper.Attd06Mapper;
import com.prafta.web.attd.attd06.application.model.UserWorkPlanModel;
import com.prafta.web.attd.attd06.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd06.result.ShiftTeamMemberResult;
import com.prafta.web.attd.attd06.result.ShiftTeamPeriodResult;
import com.prafta.web.attd.attd06.result.ShiftTeamUserInfosResult;
import com.prafta.web.attd.attd06.result.ShiftTypeDetailListsResult;
import com.prafta.web.attd.attd06.result.ShiftTypeListsResult;
import com.prafta.web.attd.attd06.result.UserListsResult;
import com.prafta.web.attd.attd06.service.Attd06Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd06ServiceImpl implements Attd06Service {

    private final Attd06Mapper attd06Mapper;

    // prafta-com-016-D-0 + 공통 가드①: 연차(종일/반차/시간차)·OT 보존 판정(read-only).
    private final ScheduleChangeGuardService scheduleChangeGuardService;

    // prafta-com-016-D-2: 교대 스케줄 변경 PUSH 생산자(afterCommit + REQUIRES_NEW).
    private final ShiftSchChangeNotiService shiftSchChangeNotiService;

    // prafta-com-016-D 보안 재작업: 쓰기 경로 인가(canManageUser/canManageNode) + R5 마감 판정(isClosedForUser) 단일 출처.
    //   attd05 와 동일하게 AttdCloseService 를 재사용한다(대조군 정합). master/hr/safe 는 canManageUser/Node 가
    //   전사 통과(prafta-042) → 정상 운영 무영향.
    private final AttdCloseService attdCloseService;

    @Override
    public ShiftTypeListsResponse getShiftTypeLists(ShiftTypeListsParam param) {

        List<ShiftTypeListsResult> shiftTypeListsResultList = attd06Mapper.selectShiftTypeLists(ShiftTypeListsQuery.from(param));

        if (shiftTypeListsResultList == null || shiftTypeListsResultList.isEmpty()) {
            return null;
        }

        return ShiftTypeListsResponse.builder()
                .shiftTypeListsResultList(shiftTypeListsResultList)
                .build();
    }

    @Override
    public UserListsResponse getUserLists(UserListsParam param) {

        List<UserListsResult> userListsResultList = attd06Mapper.selectUserLists(UserListsQuery.from(param));

        if (userListsResultList == null || userListsResultList.isEmpty()) {
            return null;
        }

        return UserListsResponse.builder()
                .userListsResultList(userListsResultList)
                .build();
    }

    @Override
    public ShiftTypeDetailListsResponse getShiftTypeDetailLists(ShiftTypeDetailListsParam param) {

        List<ShiftTypeDetailListsResult> shiftTypeDetailListsResultList = attd06Mapper.selectShiftTypeDetailLists(ShiftTypeDetailListsQuery.from(param));

        if (shiftTypeDetailListsResultList == null || shiftTypeDetailListsResultList.isEmpty()) {
            return null;
        }

        return ShiftTypeDetailListsResponse.builder()
                .shiftTypeDetailListsResultList(shiftTypeDetailListsResultList)
                .build();
    }

    @Override
    public ShiftTeamUserInfosResponse getShiftTeamUserInfos(ShiftTeamUserInfosParam param) {

        List<ShiftTeamUserInfosResult> shiftTeamUserInfosResultList = attd06Mapper.selectShiftTeamUserInfos(ShiftTeamUserInfosQuery.from(param));

        if (shiftTeamUserInfosResultList == null || shiftTeamUserInfosResultList.isEmpty()) {
            return null;
        }

        return ShiftTeamUserInfosResponse.builder()
                .shiftTeamUserInfosResultList(shiftTeamUserInfosResultList)
                .build();
    }

    @Override
    @Transactional
    public ShiftSchSaveResponse insertShiftSchInfos(ShiftSchInfosParam param) {

    	// prafta-com-016-D 보안 재작업(High/IDOR): 팀 생성 = 편입 대상 조원 각각의 work_plan 을 덮어쓰므로,
    	//   대상 userCd 마다 관리 권한(canManageUser)을 강제한다(하나라도 실패 시 ATTD_403_002 → 전체 롤백).
    	String siteCd = param.shiftMeta().siteCd();
    	Map<String, Boolean> manageCache = new LinkedHashMap<>();
    	for (ShiftSchInfosParam.TeamParam teamParam : param.teamList()) {
    		for (ShiftSchInfosParam.MemberParam memberParam : teamParam.memberList()) {
    			ensureCanManageTargetUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
    					siteCd, memberParam.userCd(), manageCache);
    		}
    	}

    	String shiftTeamId = attd06Mapper.selectShiftTeamCd(param.gvCmpnyCd());

    	attd06Mapper.insertShiftTeam(ShiftTeamCommand.from(param.shiftMeta(), shiftTeamId, param.gvCmpnyCd(), param.gvUserCd()));

    	List<ShiftSchSaveResponse.BlockedWorkPlan> blockedList = new ArrayList<>();

    	// prafta-com-016-D-2: 사용자별 "실제 덮인 날" 누적 → 1건 이상인 조원만 PUSH 대상(D-Q2).
    	Map<String, List<String>> changedByUser = new LinkedHashMap<>();

    	for(ShiftSchInfosParam.TeamParam teamParam : param.teamList()) {
    		attd06Mapper.insertShiftTeamUser(ShiftTeamUserCommand.from(teamParam, param.shiftMeta(), shiftTeamId, param.gvCmpnyCd(), param.gvUserCd()));

    		List<String> schCdPattern = attd06Mapper.selectShiftPtrnSchList(SchCdListQuery.from(
					param.gvCmpnyCd()
					, param.shiftMeta().siteCd()
					, param.shiftMeta().shiftCd()
				));

    		if (schCdPattern == null || schCdPattern.isEmpty()) {
                // 패턴이 아예 없으면 해당 팀은 작업계획 생성 스킵
                continue;
            }

    		for(ShiftSchInfosParam.MemberParam memberParam : teamParam.memberList()) {

    			UserWorkPlanCommand command = UserWorkPlanCommand.from(
    	                memberParam
    	                , param.shiftMeta()
    	                , schCdPattern
    	                , teamParam.teamIdx()
    	                , param.gvCmpnyCd()
    	                , param.gvUserCd()
    	            );

    			List<String> changedYmds = new ArrayList<>();

    			// prafta-com-016-D-0 + 가드①: (연차 종일/반차/시간차 ∨ OT)인 날짜는 덮어쓰기 제외(보존) + 보존 목록 수집.
    			List<UserWorkPlanModel> upsertModels = filterLockedDays(
    			        command.userWorkPlanModelList()
    			        , param.gvCmpnyCd()
    			        , param.shiftMeta().siteCd()
    			        , memberParam.userCd()
    			        , blockedList
    			        , changedYmds);

    			if (!changedYmds.isEmpty()) {
    			    changedByUser.computeIfAbsent(memberParam.userCd(), k -> new ArrayList<>()).addAll(changedYmds);
    			}

    			// 전부 보존/빈 리스트면 mapper 호출 스킵
                if (upsertModels.isEmpty()) {
                    continue;
                }

                attd06Mapper.upsertUserWorkPlanList(new UserWorkPlanCommand(upsertModels));
    		}
    	}

    	// prafta-com-016-D-2: 실제 1건 이상 덮인 조원에게만 PUSH 예약(afterCommit). 팀명은 신규 저장 메타에서.
    	notifyShiftSchChanged(param.gvCmpnyCd(), param.shiftMeta().siteCd(), param.shiftMeta().shiftTeamNm(),
    	        changedByUser, param.gvUserCd());

    	return ShiftSchSaveResponse.builder()
    	        .blockedList(blockedList)
    	        .build();
    }

    @Override
    @Transactional
    public ShiftSchSaveResponse updateShiftUserSchInfos(ShiftUserSchInfosParam param) {

    	// prafta-com-016-D 보안 재작업(High/IDOR): 사용자 스케줄 변경 대상 userCd 마다 관리 권한 강제.
    	String guardSiteCd = param.shiftMeta().siteCd();
    	Map<String, Boolean> manageCache = new LinkedHashMap<>();
    	for (ShiftUserSchInfosParam.TeamParam teamParam : param.teamList()) {
    		for (ShiftUserSchInfosParam.MemberParam memberParam : teamParam.memberList()) {
    			ensureCanManageTargetUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
    					guardSiteCd, memberParam.userCd(), manageCache);
    		}
    	}

    	List<ShiftSchSaveResponse.BlockedWorkPlan> blockedList = new ArrayList<>();

    	for(ShiftUserSchInfosParam.TeamParam teamParam : param.teamList()) {
    		List<String> schCdPattern = attd06Mapper.selectShiftPtrnSchList(SchCdListQuery.from(
					param.gvCmpnyCd()
					, param.shiftMeta().siteCd()
					, param.shiftMeta().shiftCd()
				));

    		if (schCdPattern == null || schCdPattern.isEmpty()) {
                // 패턴이 아예 없으면 해당 팀은 작업계획 생성 스킵
                continue;
            }

    		for(ShiftUserSchInfosParam.MemberParam memberParam : teamParam.memberList()) {

    			UserWorkPlanCommand command = UserWorkPlanCommand.from(
    	                memberParam
    	                , param.shiftMeta()
    	                , schCdPattern
    	                , teamParam.teamIdx()
    	                , param.gvCmpnyCd()
    	                , param.gvUserCd()
    	            );

    			// prafta-com-016-D-0 + 가드①: 연차(any unit)·OT 날짜 보존(덮어쓰기 제외).
    			List<UserWorkPlanModel> upsertModels = filterLockedDays(
    			        command.userWorkPlanModelList()
    			        , param.gvCmpnyCd()
    			        , param.shiftMeta().siteCd()
    			        , memberParam.userCd()
    			        , blockedList
    			        , null);

    			// 전부 보존/빈 리스트면 mapper 호출 스킵
                if (upsertModels.isEmpty()) {
                    continue;
                }

                attd06Mapper.upsertUserWorkPlanList(new UserWorkPlanCommand(upsertModels));
    		}
    	}

    	return ShiftSchSaveResponse.builder()
    	        .blockedList(blockedList)
    	        .build();
    }

    /**
     * prafta-com-016-D-0 + 공통 스케줄 변경 가드① — 교대 근무계획 자동 생성/재생성 모델 목록에서
     *   확정 연차(종일/반차/시간차 — USE_UNIT_TYPE 무관) 또는 OT(등록/신청)가 있는 날짜를 덮어쓰기에서
     *   제외(보존)한다. 근로일/휴무일을 불문하고 잠긴 날은 기존 스케줄·연차를 유지한다(R1/R2 통합).
     *
     * <p>기존 {@code filterBlockedOffLeaveDays}(R1=휴무+종일연차만)를 공통 가드 기반으로 일반화한 것이다.
     *
     * <ul>
     *   <li>잠긴 날 = upsert 제외, 보존 목록(blockedList)에 reason(LEAVE/OT)·dayType(WORK/OFF)·
     *       사용단위(leaveUseUnitType) 와 함께 누적(016-D-4 팝업 표시·필터용). 한 날이 연차·OT 양쪽이면 둘 다 1행씩.</li>
     *   <li>잠기지 않은 날 = upsert 대상으로 통과 + changedYmdsSink 에 누적(PUSH 대상 산정용, null 이면 미집계).</li>
     *   <li>dayType: 그 모델의 교대 패턴 schCd 가 null 이면 OFF(휴무일), 아니면 WORK(근로일).</li>
     * </ul>
     *
     * <p>마감(R5, prafta-com-016-D QA 재작업): 공통 가드는 마감 여부를 직접 보지 않으므로, 마감 기간 보호를
     *   본 메서드에서 별도로 수행한다. attd05 와 동일 출처({@code attdCloseService.isClosedForUser})로
     *   마감월(YYYYMM cascade) 날짜를 덮어쓰기에서 제외(보존)하고 reason="CLOSED" 로 blockedList 에 누적한다
     *   (LEAVE/OT 와 동일 sink, FE 는 reason 으로 구분). 한편 com-008-D 교대잠금(ATTD_400_160)은 attd06 자체
     *   생성 경로(INSERT...ON DUPLICATE KEY)에는 적용하지 않는다(자기모순 방지) — 그 동작은 변경하지 않는다.
     */
    private List<UserWorkPlanModel> filterLockedDays(
            List<UserWorkPlanModel> models
            , String cmpnyCd
            , String siteCd
            , String userCd
            , List<ShiftSchSaveResponse.BlockedWorkPlan> blockedList
            , List<String> changedYmdsSink) {

        if (models == null || models.isEmpty()) {
            return new ArrayList<>();
        }

        // 모델 일자 목록(중복 없음) → 가드 1회 호출.
        List<String> ymds = new ArrayList<>();
        for (UserWorkPlanModel m : models) {
            if (m.workYmd() != null && !ymds.contains(m.workYmd())) {
                ymds.add(m.workYmd());
            }
        }

        List<ScheduleLockVO> locks = scheduleChangeGuardService.findLockedDays(cmpnyCd, siteCd, userCd, ymds);

        // 잠긴 날 인덱스(ymd → 잠금 사유 목록). 같은 날 연차+OT 동시 가능.
        Map<String, List<ScheduleLockVO>> lockMap = new LinkedHashMap<>();
        if (locks != null) {
            for (ScheduleLockVO lock : locks) {
                lockMap.computeIfAbsent(lock.getWorkYmd(), k -> new ArrayList<>()).add(lock);
            }
        }

        // prafta-com-016-D R5(QA 지적) — 마감된 기간(TB_ATTD_CLOSE cascade)의 날짜는 보존(덮어쓰기 제외).
        //   attd05 와 동일 출처(attdCloseService.isClosedForUser)를 재사용한다. 마감은 월(YYYYMM) 단위이므로
        //   대상 월을 1회씩만 판정해 캐시한다.
        //   표시 정책: 마감일은 blockedList 에 담지 않고 "조용히 제외"한다(작업지시서 허용안). blockedList sink 는
        //   016-D-4 팝업 전용으로 reason 을 LEAVE/OT 로만 분류하므로, CLOSED 를 섞으면 연차로 오분류된다 →
        //   팝업(FE 템플릿) 변경 없이 정합을 유지하기 위해 마감일은 덮어쓰기에서만 제외하고 안내 목록에는 누적하지 않는다.
        Map<String, Boolean> closedYmCache = new LinkedHashMap<>();

        List<UserWorkPlanModel> upsertModels = new ArrayList<>();
        for (UserWorkPlanModel m : models) {
            // 1) 마감 보존: 잠금(연차/OT) 여부와 독립적으로, 마감월이면 무조건 보존(덮어쓰기 제외) — 조용히 제외.
            String closeYm = (m.workYmd() != null && m.workYmd().length() >= 6)
                    ? m.workYmd().substring(0, 6) : m.workYmd();
            Boolean closed = closedYmCache.get(closeYm);
            if (closed == null) {
                closed = attdCloseService.isClosedForUser(cmpnyCd, siteCd, userCd, closeYm);
                closedYmCache.put(closeYm, closed);
            }
            if (Boolean.TRUE.equals(closed)) {
                // 마감일은 upsert 대상에서 제외하고 changedYmdsSink 에도 누적하지 않는다(PUSH/안내 모두 비대상).
                continue;
            }

            List<ScheduleLockVO> dayLocks = lockMap.get(m.workYmd());
            if (dayLocks != null && !dayLocks.isEmpty()) {
                // 잠긴 날 → 보존(덮어쓰기 제외). dayType 은 그 날 교대 패턴 schCd 유무로 판정.
                String dayType = (m.schCd() == null) ? "OFF" : "WORK";
                for (ScheduleLockVO lock : dayLocks) {
                    blockedList.add(ShiftSchSaveResponse.BlockedWorkPlan.builder()
                            .userCd(m.userCd())
                            .workYmd(m.workYmd())
                            .reason(lock.getReason() == null ? null : lock.getReason().name())
                            .dayType(dayType)
                            .leaveUseUnitType(lock.getLeaveUseUnitType())
                            .build());
                }
                continue;
            }
            // 잠기지 않은 날 → 덮어쓰기 대상.
            upsertModels.add(m);
            if (changedYmdsSink != null) {
                changedYmdsSink.add(m.workYmd());
            }
        }
        return upsertModels;
    }

    /**
     * prafta-com-016-D-2: 사용자별 "실제 덮인 날 목록"을 받아 1건 이상인 조원에게만 PUSH 예약(afterCommit).
     *   전부 보존(0건)인 조원은 통보 대상에서 제외(D-Q2). 저장 1회당 조원별 1건(D-Q1).
     */
    private void notifyShiftSchChanged(String cmpnyCd, String siteCd, String shiftTeamNm,
                                       Map<String, List<String>> changedByUser, String actorUserCd) {
        if (changedByUser == null || changedByUser.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> e : changedByUser.entrySet()) {
            List<String> ymds = e.getValue();
            if (ymds == null || ymds.isEmpty()) {
                continue;
            }
            shiftSchChangeNotiService.notifyShiftSchChange(cmpnyCd, siteCd, e.getKey(), shiftTeamNm, ymds, actorUserCd);
        }
    }
    // ###############################

    @Override
    @Transactional
    public void updateShiftTeamNm(UpdateShiftTeamNmParam param) {
        // prafta-com-016-D 보안 재작업(High/IDOR): 팀 단위 경로 — 해당 교대팀 소속 멤버 각각에 관리 권한 강제.
        ensureCanManageTeamMembers(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
                param.siteCd(), param.shiftCd(), param.shiftTeamId());
        attd06Mapper.updateShiftTeamNm(UpdateShiftTeamNmCommand.from(param));
    }

    @Override
    @Transactional
    public void deleteShiftTeamUser(DeleteShiftTeamUserParam param) {
        // prafta-com-016-D 보안 재작업(High/IDOR): 조원 제거 대상 userCd 의 관리 권한 강제.
        ensureCanManageTargetUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
                param.siteCd(), param.userCd(), new LinkedHashMap<>());
        attd06Mapper.deleteShiftTeamUser(DeleteShiftTeamUserCommand.from(param));
    }

    @Override
    @Transactional
    public ShiftSchSaveResponse insertShiftTeamUsers(InsertShiftTeamUsersParam param) {

        // prafta-com-016-D 보안 재작업(High/IDOR): 조원 추가(D-3 확대 경로) 대상 userCd 마다 관리 권한 강제.
        //   하나라도 실패하면 ATTD_403_002 로 전체 롤백(소속 INSERT/덮어쓰기 미발생).
        Map<String, Boolean> manageCache = new LinkedHashMap<>();
        for (InsertShiftTeamUsersModel model : param.insertShiftTeamUsersModelList()) {
            ensureCanManageTargetUser(model.gvAuthCd(), model.gvUserCd(), model.gvCmpnyCd(),
                    model.siteCd(), model.userCd(), manageCache);
        }

        List<ShiftSchSaveResponse.BlockedWorkPlan> blockedList = new ArrayList<>();
        // 사용자별 실제 덮인 날(PUSH 대상). 팀(=shiftTeamId) 단위로 팀명·기간을 캐시한다.
        Map<String, List<String>> changedByUser = new LinkedHashMap<>();
        Map<String, ShiftTeamInfoResult> teamInfoCache = new LinkedHashMap<>();

        // 합류일(서버 기준 오늘)+1일 = 덮어쓰기 시작일(R4: 당일 제외, 다음날부터).
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
        String genStartYmd = java.time.LocalDate.now().plusDays(1).format(fmt);

        for (InsertShiftTeamUsersModel model : param.insertShiftTeamUsersModelList()) {
            // 1) 소속 INSERT(재가입 흡수) — 기존 동작 유지.
            attd06Mapper.insertShiftTeamUsers(InsertShiftTeamUsersCommand.from(model));

            // 2) work_plan 덮어쓰기(R4: 합류일+1 ~ 교대근무 종료일). 팀 기간/팀명 조회(캐시).
            ShiftTeamInfoResult teamInfo = teamInfoCache.computeIfAbsent(model.shiftTeamId(),
                    k -> attd06Mapper.selectShiftTeamInfo(
                            model.gvCmpnyCd(), model.siteCd(), model.shiftCd(), model.shiftTeamId()));

            if (teamInfo == null || teamInfo.strDate() == null || teamInfo.strDate().isBlank()
                    || teamInfo.endDate() == null || teamInfo.endDate().isBlank()) {
                // 팀 기간이 비정상이면 소속만 추가하고 계획 생성은 생략.
                continue;
            }

            // 종료일이 덮어쓰기 시작일(합류일+1)보다 이전이면 생성할 구간이 없음.
            if (teamInfo.endDate().compareTo(genStartYmd) < 0) {
                continue;
            }

            List<String> schCdPattern = attd06Mapper.selectShiftPtrnSchList(SchCdListQuery.from(
                    model.gvCmpnyCd(), model.siteCd(), model.shiftCd()));
            if (schCdPattern == null || schCdPattern.isEmpty()) {
                continue;
            }

            // 순환 위상 기준일 = 팀 STR_DATE(기존 조원과 동일 위상).
            UserWorkPlanCommand command = UserWorkPlanCommand.fromRange(
                    model.userCd()
                    , model.siteCd()
                    , teamInfo.strDate()
                    , genStartYmd
                    , teamInfo.endDate()
                    , schCdPattern
                    , model.teamIdx()
                    , model.gvCmpnyCd()
                    , model.gvUserCd());

            List<String> changedYmds = new ArrayList<>();

            List<UserWorkPlanModel> upsertModels = filterLockedDays(
                    command.userWorkPlanModelList()
                    , model.gvCmpnyCd()
                    , model.siteCd()
                    , model.userCd()
                    , blockedList
                    , changedYmds);

            if (!changedYmds.isEmpty()) {
                changedByUser.computeIfAbsent(model.userCd(), k -> new ArrayList<>()).addAll(changedYmds);
            }

            if (!upsertModels.isEmpty()) {
                attd06Mapper.upsertUserWorkPlanList(new UserWorkPlanCommand(upsertModels));
            }
        }

        // 3) PUSH — 실제 덮인 조원만(D-Q2). 팀명은 해당 사용자의 팀 정보에서 가져온다.
        //    한 호출은 동일 팀(shiftTeamId)에 묶이는 게 일반적이나, 안전하게 사용자별 팀명을 재해석한다.
        for (Map.Entry<String, List<String>> e : changedByUser.entrySet()) {
            List<String> ymds = e.getValue();
            if (ymds == null || ymds.isEmpty()) {
                continue;
            }
            // 해당 사용자가 속한 모델의 팀명 1건 사용.
            String shiftTeamNm = resolveTeamNmForUser(param, e.getKey(), teamInfoCache);
            String cmpnyCd = firstCmpnyCd(param);
            String siteCd = firstSiteCd(param, e.getKey());
            String actor = firstActor(param);
            shiftSchChangeNotiService.notifyShiftSchChange(cmpnyCd, siteCd, e.getKey(), shiftTeamNm, ymds, actor);
        }

        return ShiftSchSaveResponse.builder()
                .blockedList(blockedList)
                .build();
    }

    /** 추가 대상 모델에서 해당 사용자가 속한 팀의 팀명(캐시) 1건을 찾는다. */
    private String resolveTeamNmForUser(InsertShiftTeamUsersParam param, String userCd,
                                        Map<String, ShiftTeamInfoResult> teamInfoCache) {
        for (InsertShiftTeamUsersModel m : param.insertShiftTeamUsersModelList()) {
            if (userCd.equals(m.userCd())) {
                ShiftTeamInfoResult info = teamInfoCache.get(m.shiftTeamId());
                return info == null ? null : info.shiftTeamNm();
            }
        }
        return null;
    }

    private String firstCmpnyCd(InsertShiftTeamUsersParam param) {
        return param.insertShiftTeamUsersModelList().isEmpty()
                ? null : param.insertShiftTeamUsersModelList().get(0).gvCmpnyCd();
    }

    private String firstSiteCd(InsertShiftTeamUsersParam param, String userCd) {
        for (InsertShiftTeamUsersModel m : param.insertShiftTeamUsersModelList()) {
            if (userCd.equals(m.userCd())) {
                return m.siteCd();
            }
        }
        return null;
    }

    private String firstActor(InsertShiftTeamUsersParam param) {
        return param.insertShiftTeamUsersModelList().isEmpty()
                ? null : param.insertShiftTeamUsersModelList().get(0).gvUserCd();
    }

    @Override
    @Transactional
    public void updateShiftTeamLeaders(UpdateShiftTeamLeadersParam param) {
        // prafta-com-016-D 보안 재작업(High/IDOR): 조장 지정/해제 대상 userCd 의 관리 권한 강제.
        ensureCanManageTargetUser(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
                param.siteCd(), param.userCd(), new LinkedHashMap<>());
        attd06Mapper.updateShiftTeamLeaders(UpdateShiftTeamLeadersCommand.from(param));
    }

    @Override
    @Transactional
    public ShiftSchSaveResponse updateShiftTeamPeriod(UpdateShiftTeamPeriodParam param) {

        // prafta-com-016-D 보안 재작업(High/IDOR): 기간 변경은 팀 전체 work_plan(연장 구간)을 재생성하므로,
        //   해당 교대팀 소속 멤버 각각에 관리 권한을 강제한다(팀 단위 경로).
        ensureCanManageTeamMembers(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
                param.siteCd(), param.shiftCd(), param.shiftTeamId());

        UpdateShiftTeamPeriodCommand command = UpdateShiftTeamPeriodCommand.from(param);

        // prafta-com-016-D-0/D-2: 연장 구간 work_plan 생성 시 보존(연차/OT) 날짜 누적 + 사용자별 실제 덮인 날 누적(PUSH).
        List<ShiftSchSaveResponse.BlockedWorkPlan> blockedSink = new ArrayList<>();
        Map<String, List<String>> changedByUser = new LinkedHashMap<>();

        // 기간 연장 시 연장 구간 근무계획 생성을 위해, 변경 전 기존 기간을 먼저 확보한다.
        ShiftTeamPeriodResult before = attd06Mapper.selectShiftTeamPeriod(command);

        // 기간 값 변경(기존 동작 불변: STR_DATE/END_DATE 갱신).
        attd06Mapper.updateShiftTeamPeriod(command);

        // 변경 전 기간이 없으면(이상 케이스) 연장 동기화는 생략.
        if (before == null) {
            return buildPeriodResponse(blockedSink);
        }

        final String oldStr = before.strDate();
        final String oldEnd = before.endDate();
        final String newStr = command.strDate();
        final String newEnd = command.endDate();

        // 기존 기간이 비정상(NULL)이면 순환 위상 기준일을 잡을 수 없어 연장 생성 불가 — 단축/값변경만 반영하고 종료.
        if (oldStr == null || oldStr.isBlank() || oldEnd == null || oldEnd.isBlank()) {
            return buildPeriodResponse(blockedSink);
        }
        if (newStr == null || newStr.isBlank() || newEnd == null || newEnd.isBlank()) {
            return buildPeriodResponse(blockedSink);
        }

        // 연장 구간 산출(문자열 YYYYMMDD 비교).
        //   - 앞쪽 연장: newStr < oldStr  => 생성 [newStr, oldStr-1]
        //   - 뒤쪽 연장: newEnd > oldEnd  => 생성 [oldEnd+1, newEnd]
        //   - 단축분 삭제는 현행 유지(요구 05-2/D-5: 단축은 손대지 않음 = 기존 수기편집/계획 보존).
        boolean extendFront = newStr.compareTo(oldStr) < 0;
        boolean extendBack  = newEnd.compareTo(oldEnd) > 0;

        if (!extendFront && !extendBack) {
            // 연장 없음(단축 또는 동일) — 추가 생성 불필요(기존 계획 보존).
            return buildPeriodResponse(blockedSink);
        }

        // 교대 패턴/소속 멤버 확보.
        List<String> schCdPattern = attd06Mapper.selectShiftPtrnSchList(SchCdListQuery.from(
                param.gvCmpnyCd()
                , param.siteCd()
                , param.shiftCd()
        ));
        if (schCdPattern == null || schCdPattern.isEmpty()) {
            return buildPeriodResponse(blockedSink);
        }

        List<ShiftTeamMemberResult> members = attd06Mapper.selectShiftTeamActiveMembers(command);
        if (members == null || members.isEmpty()) {
            return buildPeriodResponse(blockedSink);
        }

        // 순환 위상 기준일 = 원래 시작일(oldStr). 연장 구간도 기존 구간과 패턴이 어긋나지 않는다.
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

        for (ShiftTeamMemberResult member : members) {
            // 앞쪽 연장 구간 [newStr, oldStr-1]
            if (extendFront) {
                String genStart = newStr;
                String genEnd = java.time.LocalDate.parse(oldStr, fmt).minusDays(1).format(fmt);
                generateExtendedPlan(member, oldStr, genStart, genEnd, schCdPattern, param, blockedSink, changedByUser);
            }
            // 뒤쪽 연장 구간 [oldEnd+1, newEnd]
            if (extendBack) {
                String genStart = java.time.LocalDate.parse(oldEnd, fmt).plusDays(1).format(fmt);
                String genEnd = newEnd;
                generateExtendedPlan(member, oldStr, genStart, genEnd, schCdPattern, param, blockedSink, changedByUser);
            }
        }

        // prafta-com-016-D-2: 연장 구간이 실제 1건 이상 덮인 조원에게만 PUSH. 팀명은 팀 정보 조회.
        if (!changedByUser.isEmpty()) {
            ShiftTeamInfoResult teamInfo = attd06Mapper.selectShiftTeamInfo(
                    param.gvCmpnyCd(), param.siteCd(), param.shiftCd(), param.shiftTeamId());
            String shiftTeamNm = (teamInfo == null) ? null : teamInfo.shiftTeamNm();
            notifyShiftSchChanged(param.gvCmpnyCd(), param.siteCd(), shiftTeamNm, changedByUser, param.gvUserCd());
        }

        // 연장 구간 보존(연차/OT) 날짜 목록을 응답으로 반환(FE 팝업).
        return buildPeriodResponse(blockedSink);
    }

    /** prafta-com-013-05-2(재작업): 기간수정 응답 빌더(보존 날짜 목록 동봉). */
    private ShiftSchSaveResponse buildPeriodResponse(List<ShiftSchSaveResponse.BlockedWorkPlan> blockedSink) {
        return ShiftSchSaveResponse.builder()
                .blockedList(blockedSink)
                .build();
    }

    /**
     * prafta-com-013-05-2: 한 멤버의 연장 구간 근무계획을 생성하여 upsert.
     *   - cycleAnchorYmd = 원래 시작일(oldStr) 기준 위상 유지.
     *   - 016-D-0/가드① 동일 적용: (연차 any unit ∨ OT)인 날짜는 덮어쓰기 제외(보존).
     */
    private void generateExtendedPlan(
            ShiftTeamMemberResult member
            , String cycleAnchorYmd
            , String genStartYmd
            , String genEndYmd
            , List<String> schCdPattern
            , UpdateShiftTeamPeriodParam param
            , List<ShiftSchSaveResponse.BlockedWorkPlan> blockedSink
            , Map<String, List<String>> changedByUser) {

        UserWorkPlanCommand command = UserWorkPlanCommand.fromRange(
                member.userCd()
                , param.siteCd()
                , cycleAnchorYmd
                , genStartYmd
                , genEndYmd
                , schCdPattern
                , member.teamIdx()
                , param.gvCmpnyCd()
                , param.gvUserCd()
        );

        List<String> changedYmds = new ArrayList<>();

        List<UserWorkPlanModel> upsertModels = filterLockedDays(
                command.userWorkPlanModelList()
                , param.gvCmpnyCd()
                , param.siteCd()
                , member.userCd()
                , blockedSink
                , changedYmds);

        if (!changedYmds.isEmpty()) {
            changedByUser.computeIfAbsent(member.userCd(), k -> new ArrayList<>()).addAll(changedYmds);
        }

        if (upsertModels.isEmpty()) {
            return;
        }
        attd06Mapper.upsertUserWorkPlanList(new UserWorkPlanCommand(upsertModels));
    }

    @Override
    @Transactional
    public void deleteShiftTeam(DeleteShiftTeamParam param) {
        // prafta-com-016-D 보안 재작업(High/IDOR): 팀 삭제 = 소속 멤버 전원 영향. 팀 단위 경로로 멤버별 관리 권한 강제.
        ensureCanManageTeamMembers(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(),
                param.siteCd(), param.shiftCd(), param.shiftTeamId());
        attd06Mapper.deleteShiftTeam(DeleteShiftTeamCommand.from(param));
        attd06Mapper.deleteShiftTeamAllUser(DeleteShiftTeamCommand.from(param));
    }

    /**
     * prafta-com-016-D 보안 재작업 — 대상 사용자(targetUserCd) 관리 권한 검증(attd05 ensureCanManageTargetUser 동일 패턴).
     *
     * <p>master/hr/safe 또는 대상 사용자가 소속한 부서(및 상위 부서)의 정·부 관리자만 허용한다.
     * 동일 대상의 반복 검증을 피하려고 결과를 {@code cache}(siteCd|targetUserCd 키)에 보관한다.
     * 권한이 없으면 {@link AttdErrorCode#ATTD_403_002} 를 던져 트랜잭션 전체를 롤백한다.
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
            log.warn("교대팀 관리 권한 없음 - 요청자 userCd={}, authCd={}, 대상 userCd={}, siteCd={}",
                    requesterUserCd, authCd, targetUserCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
    }

    /**
     * prafta-com-016-D 보안 재작업 — 팀 단위 쓰기 경로(팀명/기간/팀삭제) 권한 검증.
     *
     * <p>대상이 팀 전체이므로, 해당 교대팀 현 소속 멤버 각각에 {@link #ensureCanManageTargetUser} 를 적용한다
     * (canManageNode 에 필요한 nodeCd 가 본 경로 입력에 없으므로, 영향 멤버별 canManageUser 로 더 정밀하게 강제 —
     * 작업지시서 "영향 멤버가 명확하면 멤버별 canManageUser" 정합). master/hr/safe 는 전사 통과(prafta-042).
     * 멤버가 없으면(빈 팀) 추가 영향 없음 — 통과시켜 정상 운영(빈 팀 정리)을 막지 않는다.
     */
    private void ensureCanManageTeamMembers(String authCd, String requesterUserCd, String cmpnyCd,
            String siteCd, String shiftCd, String shiftTeamId) {

        List<String> memberUserCds = attd06Mapper.selectShiftTeamMemberUserCds(cmpnyCd, siteCd, shiftCd, shiftTeamId);
        if (memberUserCds == null || memberUserCds.isEmpty()) {
            return;
        }
        Map<String, Boolean> cache = new LinkedHashMap<>();
        for (String targetUserCd : memberUserCds) {
            if (targetUserCd == null || targetUserCd.isBlank()) {
                continue;
            }
            ensureCanManageTargetUser(authCd, requesterUserCd, cmpnyCd, siteCd, targetUserCd, cache);
        }
    }
}
