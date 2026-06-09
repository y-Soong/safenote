package com.prafta.app.leave.leaveflow.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.leave.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.app.leave.leaveflow.application.command.LeaveUseCommand;
import com.prafta.app.leave.leaveflow.application.helper.LeaveUnitGranularity;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyMetaParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApproverSearchParam;
import com.prafta.app.leave.leaveflow.dto.response.ApprovalPresetListResponse;
import com.prafta.app.leave.leaveflow.dto.response.ApproverSearchResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMetaResponse;
import com.prafta.app.leave.leaveflow.mapper.AppLeaveFlowMapper;
import com.prafta.app.leave.leaveflow.result.ApproverRow;
import com.prafta.app.leave.leaveflow.result.DeductibleGrantRow;
import com.prafta.app.leave.leaveflow.result.LeaveTypeInfoRow;
import com.prafta.app.leave.leaveflow.result.LeaveTypeMetaRow;
import com.prafta.app.leave.leaveflow.result.LeaveUsagePolicyRow;
import com.prafta.app.leave.leaveflow.service.AppLeaveFlowService;
import com.prafta.app.mypage.mypage01.mapper.AppMypage01Mapper;
import com.prafta.app.mypage.mypage01.result.PresetMasterResult;
import com.prafta.app.mypage.mypage01.result.PresetStepResult;
import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.cmm.leave.service.LeaveApprovalNotiService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-018-A: 앱 연차 신청 폼 메타 조회 서비스 구현(읽기 전용).
 *
 * <p>식별값은 토큰 기반 param 만 사용한다(IDOR). allowedUnits 계층은 {@link LeaveUnitGranularity}(SSOT) 로 산출.
 *   결재선 프리셋/결재자 검색은 신규 SQL 을 두지 않고, 프리셋은 mypage01 매퍼를 재사용한다(중복 신설 금지).
 *   결재자 검색만 LIMIT/OFFSET 보강이 필요하여 leaveflow 전용 매퍼로 분리(mypage01 원본 무변경).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppLeaveFlowServiceImpl implements AppLeaveFlowService {

    private final AppLeaveFlowMapper appLeaveFlowMapper;
    private final AppMypage01Mapper appMypage01Mapper;

    // 018-B: 공통/웹 빈 재사용(로직 미복제). 차감계산=common, 결재단계=common, 사후마감=web(결정 1).
    private final LeaveDeductionService leaveDeductionService;
    private final ApprovalLineMapper approvalLineMapper;
    private final AttdCloseService attdCloseService;
    /** PRAFTA-COM-004: 연차 결재 PUSH 생산자(outbox 적재, 예외 격리). 앱엔 승인 경로 없으므로 신청 hook 만. */
    private final LeaveApprovalNotiService leaveApprovalNotiService;

    // 법정정책 미존재 시 폴백 단위(종일만).
    private static final String FALLBACK_UNIT_CODE = "00";

    // ===== 018-B 상수(웹 LeaveFlowServiceImpl 미러) =====
    // 요청 상태 [SYS033]
    private static final String REQ_APPLIED = "01";
    private static final String REQ_APPROVED = "02";
    // 결재 단계 상태 [SYS044]
    private static final String STEP_WAIT = "00";
    private static final String STEP_APPLIED = "01";
    private static final String STEP_APPROVED = "02";
    // 사용 단위 [SYS025]
    private static final String UNIT_FULL = "00";
    private static final String UNIT_HALF = "01";
    private static final String UNIT_HOUR2 = "02";
    private static final String UNIT_HOUR1 = "03";
    private static final String UNIT_MIN30 = "04";

    private static final String USE_CONFIRMED = "CONFIRMED";
    private static final String SELF_APPROVE_COMMENT = "자체근태승인 자동 승인";

    @Override
    public LeaveApplyMetaResponse selectApplyMeta(LeaveApplyMetaParam param) {

        log.info("[leaveflow] 연차 신청 메타 조회 시작 userCd={}", param.userCd());

        List<LeaveTypeMetaRow> rows =
                appLeaveFlowMapper.selectApplicableLeaveTypes(param.cmpnyCd(), param.userCd());

        // 법정 종류 공통 출처(회사 단일 USAGE_UNIT + 법정 결재여부). 미존재면 폴백.
        LeaveUsagePolicyRow companyPolicy = appLeaveFlowMapper.selectCompanyUsageUnit(param.cmpnyCd());

        List<String> statutoryAllowedUnits;
        boolean statutoryAprvRequired;
        if (companyPolicy == null) {
            // 법정정책 미존재는 비정상 — 비폭주 폴백(종일만, 결재 불필요)
            log.warn("[leaveflow] 회사 활성 법정 연차정책 미존재 — allowedUnits=[00], aprvRequired=false 폴백 cmpnyCd={}",
                    param.cmpnyCd());
            statutoryAllowedUnits = LeaveUnitGranularity.allowedUnitsByCode(FALLBACK_UNIT_CODE);
            statutoryAprvRequired = false;
        } else {
            String code = LeaveUnitGranularity.usageUnitToCode(companyPolicy.usageUnit());
            statutoryAllowedUnits = LeaveUnitGranularity.allowedUnitsByCode(code);
            statutoryAprvRequired = isYes(companyPolicy.policyAprvUseYn());
        }

        List<LeaveApplyMetaResponse.LeaveTypeItem> items = new ArrayList<>(rows.size());
        for (LeaveTypeMetaRow row : rows) {

            boolean isStatutory = isYes(row.systemYn());

            // allowedUnits: 법정=회사 USAGE_UNIT 계층 / 비법정=타입 USE_UNIT_TYPE 계층(NULL→00 폴백)
            List<String> allowedUnits = isStatutory
                    ? statutoryAllowedUnits
                    : LeaveUnitGranularity.allowedUnitsByCode(
                            (row.useUnitType() == null) ? FALLBACK_UNIT_CODE : row.useUnitType());

            // aprvRequired: 법정=정책 APRV_USE_YN / 비법정=타입 APRV_USE_YN
            boolean aprvRequired = isStatutory
                    ? statutoryAprvRequired
                    : isYes(row.typeAprvUseYn());

            double balanceDays = toScaledDouble(row.balanceDays());
            boolean applicable = balanceDays > 0.0;

            items.add(new LeaveApplyMetaResponse.LeaveTypeItem(
                    row.leaveCd()
                    , row.leaveNm()
                    , (row.systemYn() == null ? "N" : row.systemYn())
                    , aprvRequired
                    , allowedUnits
                    , balanceDays
                    , applicable
            ));
        }

        log.info("[leaveflow] 연차 신청 메타 조회 완료 userCd={}, 종류수={}", param.userCd(), items.size());

        return new LeaveApplyMetaResponse(items);
    }

    @Override
    public ApprovalPresetListResponse selectApprovalPresets(LeaveApplyMetaParam param) {

        log.info("[leaveflow] 결재선 프리셋 조회 시작 userCd={}", param.userCd());

        // mypage01 재사용(중복 신설 금지) — 본인/회사 스코프 동일 시그니처
        List<PresetMasterResult> masters =
                appMypage01Mapper.selectPresetMasters(param.cmpnyCd(), param.userCd());
        List<PresetStepResult> steps =
                appMypage01Mapper.selectPresetStepsByUser(param.cmpnyCd(), param.userCd());

        // presetId 기준 스텝 그룹핑(스텝은 PRESET_ID/STEP_NO 오름차순 정렬되어 옴)
        Map<String, List<ApprovalPresetListResponse.PresetStepItem>> stepsByPreset = new LinkedHashMap<>();
        for (PresetStepResult s : steps) {
            stepsByPreset
                    .computeIfAbsent(s.presetId(), k -> new ArrayList<>())
                    .add(new ApprovalPresetListResponse.PresetStepItem(
                            (s.stepNo() == null ? 0 : s.stepNo())
                            , s.approverUserCd()
                            , s.userNm()
                            , s.userId()
                            , s.rankNm()
                            , s.nodeNm()
                    ));
        }

        List<ApprovalPresetListResponse.PresetItem> presets = new ArrayList<>(masters.size());
        for (PresetMasterResult m : masters) {
            List<ApprovalPresetListResponse.PresetStepItem> stepItems =
                    stepsByPreset.getOrDefault(m.presetId(), new ArrayList<>());
            presets.add(new ApprovalPresetListResponse.PresetItem(
                    m.presetId()
                    , m.presetNm()
                    , isYes(m.defaultYn())
                    , stepItems
            ));
        }

        log.info("[leaveflow] 결재선 프리셋 조회 완료 userCd={}, 프리셋수={}", param.userCd(), presets.size());

        return new ApprovalPresetListResponse(presets);
    }

    @Override
    public ApproverSearchResponse searchApprovers(LeaveApproverSearchParam param) {

        log.info("[leaveflow] 결재자 검색 시작 userCd={}, page={}, size={}", param.excludeUserCd(), param.page(), param.size());

        // size+1 조회로 다음 페이지 존재(hasNext) 판정
        List<ApproverRow> rows = appLeaveFlowMapper.searchApprovers(
                param.cmpnyCd()
                , param.siteCd()
                , param.excludeUserCd()
                , param.keyword()
                , param.limitWithLookahead()
                , param.offset()
        );

        boolean hasNext = rows.size() > param.size();
        List<ApproverRow> pageRows = hasNext ? rows.subList(0, param.size()) : rows;

        List<ApproverSearchResponse.ApproverItem> approvers = new ArrayList<>(pageRows.size());
        for (ApproverRow r : pageRows) {
            approvers.add(new ApproverSearchResponse.ApproverItem(
                    r.userCd()
                    , r.userId()
                    , r.userNm()
                    , r.rankNm()
                    , r.nodeNm()
            ));
        }

        log.info("[leaveflow] 결재자 검색 완료 userCd={}, 결과수={}, hasNext={}",
                param.excludeUserCd(), approvers.size(), hasNext);

        return new ApproverSearchResponse(approvers, hasNext);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLeave(LeaveApplyParam p) {

        final String cmpny = p.gvCmpnyCd();
        final String site = p.gvSiteCd();
        final String user = p.gvUserCd();
        final String workYmd = p.workYmd();
        final String leaveCd = p.leaveCd();
        final String unit = p.useUnitType();

        log.info("[leaveflow] 연차 신청 시작 userCd={}, leaveCd={}, workYmd={}, unit={}", user, leaveCd, workYmd, unit);

        // 1) 타입 메타 + 결재 여부 (법정=정책 APRV_USE_YN, 비법정=타입 APRV_USE_YN)
        LeaveTypeInfoRow type = appLeaveFlowMapper.selectLeaveTypeInfo(cmpny, leaveCd);
        if (type == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        boolean statutory = isYes(type.systemYn());
        boolean aprvRequired;
        if (statutory) {
            aprvRequired = isYes(appLeaveFlowMapper.selectPolicyAprvUseYn(cmpny));
        } else {
            aprvRequired = isYes(type.aprvUseYn());
        }

        // 1-B) PRAFTA-APP-022 룰B: 출근 기록이 존재하는 일자의 연차 신청 차단(전 사용단위 공통 게이트).
        //   정책 §9.4 "연차 신청 조건 = 해당 일자에 출근 기록이 없을 것"(시간 방향 무관). 단위 분기/차감 진입 전
        //   단일 게이트로 빠르게 거부한다. 식별값은 토큰 도출값(IDOR). 미래엔 실적이 없어 자연 통과.
        if (appLeaveFlowMapper.countAttendanceByDate(cmpny, site, user, workYmd) > 0) {
            log.info("[leaveflow] 연차 신청 거부: 출근 기록 존재 일자 (userCd={}, workYmd={})", user, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_108);
        }

        // 2) 단위 게이팅(D2, 웹엔 없음) — 허용단위 산출(018-A LeaveUnitGranularity SSOT 재사용)
        //    법정 = 회사 USAGE_UNIT 계층 / 비법정 = 타입 USE_UNIT_TYPE 계층(NULL→00 폴백).
        //    잘못된 단위로 차감계산에 진입하지 않도록 구조검증/차감 전에 거부한다.
        List<String> allowedUnits = resolveAllowedUnits(cmpny, statutory, type.useUnitType());
        if (!allowedUnits.contains(unit)) {
            log.warn("[leaveflow] 허용되지 않은 사용단위 신청 userCd={}, leaveCd={}, unit={}, allowed={}",
                    user, leaveCd, unit, allowedUnits);
            throw new ApiException(AttdErrorCode.ATTD_400_102);
        }

        // 3) 사용 단위 구조검증 + 차감 계산(웹 99~135 동일)
        BigDecimal leaveDays;
        Integer leaveMinutes = null;
        String startTime = null;
        String endTime = null;

        if (UNIT_FULL.equals(unit)) {
            leaveDays = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            leaveDays = new BigDecimal("0.50000");
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily != null) {
                leaveMinutes = daily / 2;
            }
        } else if (UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit)) {
            startTime = p.startTime();
            endTime = p.endTime();
            Integer sMin = DateTimeUtils.hhmmToMinutes(startTime);
            Integer eMin = DateTimeUtils.hhmmToMinutes(endTime);
            if (sMin == null || eMin == null || eMin <= sMin) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            int minutes = eMin - sMin;
            int unitMin = unitMinutes(unit);
            if (minutes % unitMin != 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null || minutes > daily) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            // 근무시간 내 검증(prafta-app-018-B 보완): 시간차 연차는 정규 근무구간(스케줄) 안에서만
            //   신청 가능. 예) 스케줄 07:00~15:00 인데 03:00~04:30 신청 → 근무하지 않는 시간이라 거부.
            if (!leaveDeductionService.withinScheduledWorkHours(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_103);
            }
            // 휴게 가로지름 거부(§8.5.9) — 휴게시각 미설정이면 skip
            if (leaveDeductionService.crossesBreak(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_055);
            }
            leaveDays = leaveDeductionService.calcDeductionDays(minutes, daily);
            if (leaveDays == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            leaveMinutes = minutes;
        } else {
            throw new ApiException(AttdErrorCode.ATTD_400_054);
        }

        // 4) 사후 신청은 근태 마감 전까지만(웹 137~146, prafta-028 부서 단위 정밀판정)
        String today = todayYmd();
        if (workYmd.compareTo(today) < 0) {
            String closeYm = workYmd.substring(0, 6);
            if (attdCloseService.isClosedForUser(cmpny, site, user, closeYm)) {
                throw new ApiException(AttdErrorCode.ATTD_400_050);
            }
        }

        // 5) 차감 대상 부여(잔여 충분, 만료 임박 우선, FOR UPDATE)
        DeductibleGrantRow grant = appLeaveFlowMapper.selectDeductibleGrant(cmpny, user, leaveCd, workYmd, leaveDays);
        if (grant == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }

        // 6) 요청 INSERT(REQ_TYPE='05'). 결재 Y면 신청('01'), N이면 즉시 승인('02').
        //    nodeCd 는 본문 비신뢰 → null 저장(자기승인 판정은 서버 USER→NODE 조인으로 독립 수행).
        String reqId = appLeaveFlowMapper.selectNextReqId(cmpny);
        String reqStatus = aprvRequired ? REQ_APPLIED : REQ_APPROVED;
        appLeaveFlowMapper.insertLeaveReq(new LeaveReqInsertCommand(
                reqId, cmpny, site, user, reqStatus, p.reason(), workYmd, null,
                workYmd, startTime, workYmd, endTime, p.leaveType(), leaveDays, user));

        // 7) 결재 Y → 라인 일괄 생성 + 자기 승인 원칙(§9.5, 웹 161~200 미러)
        boolean fullyAutoApproved = false;
        // PRAFTA-COM-004 시나리오 A hook 용: 차례가 도래한 첫 수동 단계의 결재자/단계번호.
        String turnApprover = null;
        int turnStep = -1;
        if (aprvRequired) {
            List<String> approvers = resolveApprovers(p);
            if (approvers == null || approvers.isEmpty()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            // 보안(결재자 스코프 가드): 본문 approverUserCds 는 신뢰 입력이므로, 각 결재자가
            //   동일 회사 + 동일 사업장 + 재직 사용자인지 서버에서 검증한다(타 사업장/회사/존재없는
            //   USER_CD 를 결재자로 주입 → 그 결재함에 신청 노출되는 cross-tenant 침해 차단).
            //   중복 제거 수와 유효 사용자 수가 다르면 거부.
            List<String> distinctApprovers = new ArrayList<>(new LinkedHashSet<>(approvers));
            int validCount = appLeaveFlowMapper.countValidApprovers(cmpny, site, distinctApprovers);
            if (validCount != distinctApprovers.size()) {
                log.info("[leaveflow] 연차 신청 거부: 유효하지 않은 결재자 포함 (userCd={}, 요청={}, 유효={})",
                        user, distinctApprovers.size(), validCount);
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            boolean selfAllowed = isYes(appLeaveFlowMapper.selectUserNodeSelfApproveYn(cmpny, user));

            int currentIdx = -1; // 첫 수동(비-자동승인) 단계 인덱스
            for (int i = 0; i < approvers.size(); i++) {
                boolean isSelf = user.equals(approvers.get(i));
                if (isSelf && !selfAllowed) {
                    throw new ApiException(AttdErrorCode.ATTD_400_056);
                }
                if (!isSelf && currentIdx < 0) {
                    currentIdx = i;
                }
            }
            for (int i = 0; i < approvers.size(); i++) {
                String approver = approvers.get(i);
                boolean isSelf = user.equals(approver);
                ApprovalStepVO s = new ApprovalStepVO();
                s.setReqId(reqId);
                s.setApprovalStep(i + 1);
                s.setCmpnyCd(cmpny);
                s.setApproverUserCd(approver);
                if (isSelf) {
                    s.setApprovalStatus(STEP_APPROVED); // 본인 + 자체근태승인 ON → 자동 승인
                    s.setApprovalComment(SELF_APPROVE_COMMENT);
                } else {
                    s.setApprovalStatus(i == currentIdx ? STEP_APPLIED : STEP_WAIT);
                }
                s.setInsertNo(user);
                approvalLineMapper.insertApprovalStep(s);
            }
            fullyAutoApproved = (currentIdx < 0); // 전 단계가 본인 자동승인 → 즉시 확정
            if (currentIdx >= 0) {
                // 차례가 도래한 첫 수동 단계(STEP_APPLIED) — 시나리오 A 발송 대상.
                turnApprover = approvers.get(currentIdx);
                turnStep = currentIdx + 1; // approvalStep 은 1-based
            }
        }

        // 8) 차감 예약(CONFIRMED) + 부여 USED_DAYS 동기화(웹 202~212)
        String leaveId = appLeaveFlowMapper.selectNextLeaveId(cmpny);
        LeaveUseCommand use = LeaveUseCommand.builder()
                .leaveId(leaveId).cmpnyCd(cmpny).siteCd(site).userCd(user).leaveCd(leaveCd)
                .reqId(reqId).grantId(grant.grantId())
                .startDate(workYmd).startTime(startTime).endDate(workYmd).endTime(endTime)
                .useUnitType(unit).leaveDays(leaveDays).leaveMinutes(leaveMinutes)
                .leaveReason(p.reason()).leaveStatus(USE_CONFIRMED).insertNo(user)
                .build();
        appLeaveFlowMapper.insertLeaveUse(use);
        appLeaveFlowMapper.recomputeGrantUsedDays(cmpny, grant.grantId(), user);

        // 9) 즉시확정 + 출근차단(웹 214~223)
        if (aprvRequired && fullyAutoApproved) {
            appLeaveFlowMapper.updateReqStatus(cmpny, reqId, REQ_APPROVED, user, SELF_APPROVE_COMMENT);
        }
        boolean confirmedNow = !aprvRequired || fullyAutoApproved;
        if (confirmedNow && UNIT_FULL.equals(unit)) {
            appLeaveFlowMapper.upsertWorkPlanLeave(cmpny, site, user, workYmd, leaveCd, user);
        }

        // PRAFTA-COM-004 PUSH 적재 hook (예외 격리 — @Transactional 본 흐름에 예외 전파 금지).
        //  - 시나리오 A: 결재 Y + 차례 도래 단계(첫 수동) 결재자 1인. fullyAutoApproved 면 미발송(D1).
        //  - 시나리오 B: 순수 무결재(!aprvRequired)만. fullyAutoApproved 제외(D1).
        //  앱에는 연차 승인 경로가 없으므로 "다음 단계" hook 은 없다(웹 approveStep 단일 경로).
        try {
            if (aprvRequired && turnApprover != null) {
                leaveApprovalNotiService.notifyApprovalTurn(cmpny, site, user, reqId, turnStep, turnApprover, user);
            }
            if (!aprvRequired) {
                leaveApprovalNotiService.notifyLeaveUsedNoAprv(cmpny, site, user, leaveId, unit, leaveDays,
                        workYmd, startTime, endTime, user);
            }
        } catch (Exception e) {
            log.error("[leaveflow] 연차 신청 PUSH 적재 hook 실패(신청 영향 없음) reqId={}", reqId, e);
        }

        log.info("[leaveflow] 연차 신청 완료 reqId={}, userCd={}, leaveCd={}, unit={}, days={}, aprv={}",
                reqId, user, leaveCd, unit, leaveDays, aprvRequired);
    }

    /**
     * 허용 사용단위 집합 산출(018-A {@link LeaveUnitGranularity} SSOT 재사용).
     * 법정 = 회사 USAGE_UNIT 계층(정책 미존재면 종일만 폴백) / 비법정 = 타입 USE_UNIT_TYPE 계층(NULL→00).
     */
    private List<String> resolveAllowedUnits(String cmpnyCd, boolean statutory, String typeUseUnitType) {
        if (statutory) {
            LeaveUsagePolicyRow companyPolicy = appLeaveFlowMapper.selectCompanyUsageUnit(cmpnyCd);
            if (companyPolicy == null) {
                return LeaveUnitGranularity.allowedUnitsByCode(FALLBACK_UNIT_CODE);
            }
            return LeaveUnitGranularity.allowedUnitsByCode(
                    LeaveUnitGranularity.usageUnitToCode(companyPolicy.usageUnit()));
        }
        return LeaveUnitGranularity.allowedUnitsByCode(
                (typeUseUnitType == null) ? FALLBACK_UNIT_CODE : typeUseUnitType);
    }

    /**
     * 결재자 목록 결정(결정 2): approverUserCds 1차, 비어있고 presetId 가 있으면 본인 소유 프리셋을 전개.
     * 프리셋 전개는 mypage01 {@code selectPresetStepsById}(소유자 스코프) 재사용(타인 프리셋 차단·중복 신설 금지).
     */
    private List<String> resolveApprovers(LeaveApplyParam p) {
        List<String> approvers = p.approverUserCds();
        if (approvers != null && !approvers.isEmpty()) {
            return approvers;
        }
        String presetId = p.presetId();
        if (presetId == null || presetId.isBlank()) {
            return approvers;
        }
        List<PresetStepResult> steps =
                appMypage01Mapper.selectPresetStepsById(p.gvCmpnyCd(), p.gvUserCd(), presetId);
        List<String> expanded = new ArrayList<>(steps.size());
        for (PresetStepResult s : steps) {
            if (s.approverUserCd() != null && !s.approverUserCd().isBlank()) {
                expanded.add(s.approverUserCd());
            }
        }
        return expanded;
    }

    private int unitMinutes(String unit) {
        if (UNIT_HOUR2.equals(unit)) return 120;
        if (UNIT_HOUR1.equals(unit)) return 60;
        return 30; // UNIT_MIN30
    }

    private String todayYmd() {
        LocalDate d = LocalDate.now();
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /** 'Y'(대소문자 무관 'Y') → true. null/그외 → false. */
    private boolean isYes(String yn) {
        return "Y".equalsIgnoreCase(yn);
    }

    /**
     * BigDecimal → double (null=0.0, 소수 1자리 반올림 — leave01 toScaledDouble 패턴, balanceDays 정합).
     */
    private double toScaledDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        return value.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
