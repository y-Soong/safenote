package com.prafta.app.leave.leaveflow.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.leave.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.app.leave.leaveflow.application.command.LeaveUseCommand;
import com.prafta.app.leave.leaveflow.application.helper.LeaveUnitGranularity;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyMetaParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApproverSearchParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveDayScheduleParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveDeductionPreviewParam;
import com.prafta.app.leave.leaveflow.dto.response.ApprovalPresetListResponse;
import com.prafta.app.leave.leaveflow.dto.response.ApproverSearchResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMetaResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveDayScheduleResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveDeductionPreviewResponse;
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
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.application.query.FileReadQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.mapper.LeavePolicyMapper;
import com.prafta.common.cmm.leave.service.LeaveApprovalNotiService;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService.BorrowFamily;
import com.prafta.common.cmm.leave.service.LeaveRemnantCoverService;
import com.prafta.common.cmm.leave.util.BreakWaiveCapUtils;
import com.prafta.common.cmm.leave.util.FiscalYearUtils;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.BreakMergeResult;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfDayBoundary;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfPart;
import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.cmm.leave.vo.BorrowGrantResultVO.BorrowGrantSlotVO;
import com.prafta.common.cmm.leave.vo.HourlyChargeVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.RemnantTriggerPlanVO;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AdvisoryLockTxUtils;
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
    /** 연차개편: 활성정책 AXIS2_FISCAL_START_MM/_DD 조회(회계연도 경계 산출 입력). 락 없는 조회 메서드 재사용. */
    private final LeavePolicyMapper leavePolicyMapper;
    /** prafta-com-011-2 가불: 한도 projection/만료검증/가불 GRANT 생성·회수 코어(com-011-1 산출). */
    private final LeaveGrantEngineService leaveGrantEngineService;
    /** LC-07: preview 응답의 convMinutes(고정단위 케이스) — 신청 대상일 기준 환산시간(F4) 단일 출처. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;
    /** PC-05: 짜투리 잔여 보전 — 발동 판정(D5)·발동 처리(D6) 단일 출처(웹과 공유 빈, 회수 D7은 웹 훅 전담). */
    private final LeaveRemnantCoverService leaveRemnantCoverService;
    /** 일자별 스케줄(휴게 포함) 조회 — 시간차 휴게 가로지름 사전 안내(day-schedule). 공통 매퍼 재사용. */
    private final LeaveDeductionMapper leaveDeductionMapper;
    /** 연차 신청 증빙 필수화(2026-08-29): 증빙 파일 업로드/열람(FileService 인프라 재사용 — AppNearMiss01 패턴 미러). */
    private final FileService fileService;
    private final FileMapper fileMapper;

    // 법정정책 미존재 시 폴백 단위(종일만).
    private static final String FALLBACK_UNIT_CODE = "00";
    /** 연차 신청 증빙 필수화(2026-08-29): SYS010 신규 채번 파일타입(연차 증빙자료 — secure 저장). */
    private static final String FILE_TYPE_EVIDENCE = "008";

    // ===== prafta-com-011-2 가불 시스템 연차 코드(엔진 LEAVE_CD 상수와 동일) =====
    /** 본연차 시스템 코드 → BorrowFamily.ANNUAL. */
    private static final String LEAVE_CD_ANNUAL = "SYS_ANNUAL";
    /** 월차 시스템 코드 → BorrowFamily.MONTHLY. */
    private static final String LEAVE_CD_MONTHLY = "SYS_MONTHLY";
    /** 근속가산 연차 시스템 코드(엔진 LEAVE_CD_TENURE 와 동일 값). */
    private static final String LEAVE_CD_TENURE_BONUS = "SYS_TENURE_BONUS";
    /** 일괄선부여 연차 시스템 코드. */
    private static final String LEAVE_CD_PREGRANT = "SYS_PREGRANT";
    /** 사용촉진 연차 시스템 코드. */
    private static final String LEAVE_CD_PROMOTION = "SYS_PROMOTION";

    /**
     * Baim_07 "법정휴가 신청 결재" 스위치({@code tb_leave_policy.APRV_USE_YN})가 지배하는 법정 5종.
     *
     * <p>2026-09-04 사용자 확정: 종전에는 {@code SYSTEM_YN='Y'} 인 시스템 시드 전체가 이 스위치를 탔다.
     * 실제 기준은 <b>{@code SYSTEM_YN='Y' AND LEAVE_NATURE_TYPE='01'}(법정)</b> 이며, 그 집합이 곧
     * 아래 5종이다. 약정 시드(SYS_BIRTHDAY 생일 안식휴가 / SYS_CAREER 경력 인정 휴가 — 둘 다
     * {@code LEAVE_NATURE_TYPE='02'})는 비법정 타입과 동일하게 타입별
     * {@code tb_leave_type_mgmt.APRV_USE_YN} 을 따른다.
     *
     * <p>짜투리 보전 대상({@code LeaveRemnantCoverServiceImpl.TARGET_LEAVE_CDS})과 동일 집합이다 —
     * 두 곳 모두 "법정 5종"이라는 같은 축을 쓰므로 한쪽만 바꾸지 않는다.
     * {@code LEAVE_NATURE_TYPE} 을 직접 읽지 않고 코드 목록으로 두는 이유: 조회 record 가 위치매핑이라
     * 컬럼 추가 비용이 크고, 시스템 시드는 프로비저닝이 고정 목록으로 넣는 값이라 열거가 안전하다.
     *
     * <p>★{@code statutory}(=SYSTEM_YN) 자체의 의미는 바꾸지 않는다 — 사용단위 계층·가불 대상 판정 등
     * 다른 분기는 종전 그대로여야 하므로, 결재 판정에만 이 화이트리스트를 겹쳐 쓴다.
     * 웹 {@code LeaveFlowServiceImpl.POLICY_APRV_LEAVE_CDS} 와 동일 집합(미러).
     */
    private static final Set<String> POLICY_APRV_LEAVE_CDS = Set.of(
            LEAVE_CD_ANNUAL, LEAVE_CD_MONTHLY, LEAVE_CD_TENURE_BONUS,
            LEAVE_CD_PREGRANT, LEAVE_CD_PROMOTION);

    /** 연차개편: 사용자 신청 타입 [SYS021] '01'. 한도=MAX_APLY_DAYS, 잔여=회계연도 사용분 차감. */
    private static final String LEAVE_TYPE_USER_APPLY = "01";
    /** prafta-com-016-B(3-1): 사용가능기간 [SYS026] '01' 설정안함 = 전체 누적(lifetime). 그 외는 회계연도 윈도우. (웹 미러) */
    private static final String AVAIL_TERM_NONE = "01";
    /** 연차개편 동시성: '01' 신청 직렬화 advisory lock 타임아웃(초). */
    private static final int LEAVE01_LOCK_TIMEOUT_SEC = 5;
    // 연차 원장 일수 컬럼 최대 스케일(TB_USER_LEAVE_USE.LEAVE_DAYS / TB_USER_LEAVE_GRANT.USED_DAYS = decimal(8,5)).
    // apply-meta balanceDays 를 이 스케일로 전달해 시간차 잔여(예 0.075일)를 손실 없이 앱에 넘긴다(반올림 오표기 제거).
    private static final int LEDGER_DECIMAL_SCALE = 5;

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
    // (폐지) 반반차 '05' — 2026-08-07 반차 시간대 도입(HB-04)으로 신청·검증 경로에서 제거.
    //   코드값(SYS025 '05')과 과거 데이터 조회 경로는 존치하며, 신청은 단위 게이팅(ATTD_400_102)과
    //   아래 else 분기(ATTD_400_054)에서 fail-closed 로 거부된다.

    /** 반차 파트: 시작기준(늦게 출근) — 면제 = [근무시작, 경계). */
    private static final String HALF_PART_START = "START";
    /** 반차 파트: 종료기준(일찍 퇴근) — 면제 = [경계, 근무종료). */
    private static final String HALF_PART_END = "END";

    private static final String USE_CONFIRMED = "CONFIRMED";

    @Override
    public LeaveApplyMetaResponse selectApplyMeta(LeaveApplyMetaParam param) {

        log.info("[leaveflow] 연차 신청 메타 조회 시작 userCd={}", param.userCd());

        // 연차개편: 사용자 신청('01') 잔여 = MAX_APLY_DAYS - 당해 회계연도 사용분. 회계연도 경계를 단일출처(FiscalYearUtils)로 산출.
        FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(param.cmpnyCd());

        List<LeaveTypeMetaRow> rows =
                appLeaveFlowMapper.selectApplicableLeaveTypes(param.cmpnyCd(), param.userCd(),
                        fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());

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
            // HB-04(D-8): 반반차('05') 폐지 — 구 설정 'QUARTER_DAY' 는 반차('01')로 축소 해석된다
            //   (usageUnitToCode 정규화). 허용집합에 '05' 가 나오는 경로는 없다.
            String code = LeaveUnitGranularity.usageUnitToCode(companyPolicy.usageUnit());
            statutoryAllowedUnits = LeaveUnitGranularity.allowedUnitsByCode(code);
            statutoryAprvRequired = isYes(companyPolicy.policyAprvUseYn());
        }

        // prafta-com-011-2 가불: 시스템 법정 월차/본연차 항목에 가불 가능 여부/한도를 노출한다(표시·게이팅용).
        //   입사일은 토큰 도출 userCd 로 1회 조회(식별값 본문 비신뢰). 미존재면 가불 한도 0(=비가불 동일).
        String hireDate = appLeaveFlowMapper.selectUserHireDate(param.cmpnyCd(), param.userCd());

        // E4·E5(당일분모 전환): 오늘 기준 본인 분모(convMinutes — 기본 근무타입 근사치).
        //   2026-08-09 표기 규약 변경: FE 는 날짜 미정 문맥 잔여 표기를 일 단위 단독으로 전환 —
        //   apply-meta 의 convMinutes 는 구버전 앱 호환(additive)으로 잔존하는 참고 필드(신 FE 미사용).
        //   preview 쪽 convMinutes(신청일 기준 E1 당일분모)는 표기 유지 대상 — 본 건과 무관.
        //   구 D2 의 사용자 속성 기반 시간차
        //   차단(hourlyBlocked 판정 + stripHourlyUnits)은 E5 로 해제 — 분모가 당일 배정 스케줄로
        //   전환되어 "기본 근무타입 미지정(교대 등)" 차단 근거가 소멸했다. 미배정일 시간차는 날짜
        //   속성으로 서버가 최종 차단(ATTD_400_110/194 — submit·preview)하고 FE 는 day-schedule
        //   기반 게이팅(T5)으로 안내한다.
        Integer personalConv = leaveConversionPolicyService.resolvePersonalConvMinutes(
                param.cmpnyCd(), param.userCd(), todayYmd());

        List<LeaveApplyMetaResponse.LeaveTypeItem> items = new ArrayList<>(rows.size());
        for (LeaveTypeMetaRow row : rows) {

            boolean isStatutory = isYes(row.systemYn());

            // allowedUnits: 법정=회사 USAGE_UNIT 계층 / 비법정=타입 USE_UNIT_TYPE 계층(NULL→00 폴백).
            //   E5: 구 D2 strip(개인 분모 불가 시 시간차 단위 제거)은 해제 — 정책 계층 그대로 반환.
            List<String> allowedUnits = isStatutory
                    ? statutoryAllowedUnits
                    : LeaveUnitGranularity.allowedUnitsByCode(
                            (row.useUnitType() == null) ? FALLBACK_UNIT_CODE : row.useUnitType());

            // aprvRequired: 법정 5종(NATURE='01')=정책 APRV_USE_YN / 그 외=타입 APRV_USE_YN.
            //   2026-09-04: 종전에는 SYSTEM_YN='Y' 전체가 정책 스위치를 탔다(약정 시드 SYS_BIRTHDAY/
            //   SYS_CAREER 포함). 스위치 문구와 실제 적용범위를 법정 5종으로 일치시킨다.
            boolean policyAprvTarget = isStatutory && POLICY_APRV_LEAVE_CDS.contains(row.leaveCd());
            boolean aprvRequired = policyAprvTarget
                    ? statutoryAprvRequired
                    : isYes(row.typeAprvUseYn());

            double balanceDays = toScaledDouble(row.balanceDays());
            boolean applicable = balanceDays > 0.0;

            // 가불 한도(prafta-com-011-2): 시스템 법정 월차/본연차일 때만 산정. 비대상/입사일 미존재면 0.
            double borrowQuota = 0.0;
            boolean borrowable = false;
            String borrowExpiryYmd = null;
            BorrowFamily family = isStatutory ? borrowFamilyOf(row.leaveCd()) : null;
            if (family != null) {
                BigDecimal quota = leaveGrantEngineService.computeBorrowQuota(
                        param.cmpnyCd(), param.userCd(), hireDate, family);
                borrowQuota = toScaledDouble(quota);
                borrowable = borrowQuota > 0.0;
                // prafta-com-011-5: 가불분 만료(소멸)일 — FE 표시 + 만료초과 alert 가드용. 산정 불가면 null.
                if (borrowable) {
                    borrowExpiryYmd = resolveBorrowExpiryYmd(param.cmpnyCd(), param.userCd(), hireDate, family);
                }
            }

            items.add(new LeaveApplyMetaResponse.LeaveTypeItem(
                    row.leaveCd()
                    , row.leaveNm()
                    , (row.systemYn() == null ? "N" : row.systemYn())
                    , aprvRequired
                    , allowedUnits
                    , balanceDays
                    , applicable
                    , borrowable
                    , borrowQuota
                    , borrowExpiryYmd
                    , (row.evidenceYn() == null ? "N" : row.evidenceYn())
                    , row.evidenceGuideMsg()
            ));
        }

        // 잔여 "N일 H시간 M분" 표기용 환산시간(분) — 오늘 기준 본인 분모 근사치(E4 참고치 규약:
        //   기본 근무타입 기준, 미산출 480 폴백 — 실스케줄과 편차 허용, 사용자 확정 2026-08-03).
        //   (신청 대상일이 아직 미정인 폼 진입 시점 표기라 근사로 충분 — 확정 분모는 preview/제출 시 재산출)
        int convMinutes = (personalConv != null) ? personalConv : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;

        log.info("[leaveflow] 연차 신청 메타 조회 완료 userCd={}, 종류수={}, conv={}",
                param.userCd(), items.size(), convMinutes);

        // hourlyBlocked = 항상 false(E5 해제·필드는 구 앱 FE 하위호환용 유지 — P4 릴리즈 시차 안전).
        return new LeaveApplyMetaResponse(items, convMinutes, false);
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

        // sec 리뷰(Critical, 2026-08-29): evidenceFileId 가 오면 EVIDENCE_YN='Y'/'N' 여부와 무관하게
        //   항상 존재/타입(FILE_TYPE='008')/소유권(업로더=본인) 을 검증한다. 이 검증이 없으면 가짜 값이나
        //   타인(또는 007 근로계약서 등 다른 보호 파일타입)의 fileMgmtCd 를 그대로 저장할 수 있었다
        //   (재현 시나리오: 순차 채번되는 fileMgmtCd 를 추측해 본인 신청에 실어 보내는 IDOR).
        if (p.evidenceFileId() != null && !p.evidenceFileId().isBlank()) {
            int validEvidence = appLeaveFlowMapper.countValidEvidenceFile(cmpny, p.evidenceFileId(), user);
            if (validEvidence <= 0) {
                log.warn("[leaveflow] 연차 신청 거부: 유효하지 않은 증빙 파일(존재/타입/소유권 불일치) "
                        + "(userCd={}, leaveCd={})", user, leaveCd);
                throw new ApiException(AttdErrorCode.ATTD_400_216);
            }
        }

        // 연차 신청 증빙 필수화(2026-08-29): EVIDENCE_YN='Y' 타입인데 증빙 파일 미첨부면 신규 신청 거부.
        //   이진값 그대로 강제(안내문구 조건부 파싱 없음) — 신규 신청 건에만 적용(소급 없음, §2-1).
        //   previewDeduction(조회 전용)에는 이 가드를 두지 않는다 — 미리보기가 첨부 전 정상 플로우를 막지 않도록.
        if (isYes(type.evidenceYn())
                && (p.evidenceFileId() == null || p.evidenceFileId().isBlank())) {
            log.info("[leaveflow] 연차 신청 거부: 증빙 파일 미첨부 (userCd={}, leaveCd={})", user, leaveCd);
            throw new ApiException(AttdErrorCode.ATTD_400_215);
        }

        boolean statutory = isYes(type.systemYn());
        // 결재 판정은 법정 5종(NATURE='01')만 정책 스위치를 탄다(2026-09-04, apply-meta 와 동일 규칙).
        //   statutory 자체는 아래 가불/단위 분기에서 종전 의미 그대로 쓰이므로 건드리지 않는다.
        boolean policyAprvTarget = statutory && POLICY_APRV_LEAVE_CDS.contains(leaveCd);
        boolean aprvRequired;
        if (policyAprvTarget) {
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

        // 3) 사용 단위 구조검증 + 차감 계산(웹 LeaveFlowServiceImpl 미러).
        //    연차개편(LC-03/LC-04): 시간차(02/03/04)는 여기서 구조 검증만 하고, 차감액(그날 누적
        //    기준 차액)은 아래 try 블록에서 사용자·일 advisory lock 획득 후 산출한다(F5 직렬화).
        boolean hourlyUnit = UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit);
        BigDecimal leaveDays = null;
        Integer leaveMinutes = null;
        String startTime = null;
        String endTime = null;

        // 2-B) BW-04 휴게 무시 게이트(단위 분기 진입 전, 순서 고정): ① 반차/시간차 외 단위 거부(ATTD_400_219)
        //   ② 회사 BRK_WAIVE_ALLOW_YN='N' 거부(ATTD_400_217). ③ 신청자 = 토큰 본인은 기존 IDOR 구조상 자동.
        //   미체크('N')면 게이트 미진입 — 기존 요청은 저장 결과 바이트 동일(END·미체크 무회귀).
        // v2 법정 하한 상한제(BW2-04, plan §3-2 검증 순서 고정): ① null→0(Param) ② 음수/15 배수 아님 → 221
        //   ③ waiveRequested(= 'Y' || W>0) 게이트 219/217 ④ 시간차 + W>0 → 221(§7 Q8) ⑤ 반차 W > 실효 cap → 221.
        //   저장 BRK_WAIVE_YN 은 파생값(waiveRequested), BRK_WAIVE_MIN 은 반차 W_eff / 시간차 편입분 / 기록 전용 0.
        final int brkWaiveMin = p.brkWaiveMin();
        if (!BreakWaiveCapUtils.isValidStep(brkWaiveMin)) {
            log.info("[leaveflow] 휴게 넘김 거부: 분량 단위 부적합 (userCd={}, workYmd={}, W={})", user, workYmd, brkWaiveMin);
            throw new ApiException(AttdErrorCode.ATTD_400_221);
        }
        final boolean brkWaive = isYes(p.brkWaiveYn()) || brkWaiveMin > 0;
        final String brkWaiveYnEff = brkWaive ? "Y" : "N";
        Integer brkWaiveMinEff = null; // 'Y' 저장 시 분량(반차 W_eff / 시간차 편입분 / 기록 전용 0)
        assertBrkWaiveAllowed(cmpny, user, workYmd, unit, hourlyUnit, brkWaive);
        if (hourlyUnit && brkWaiveMin > 0) {
            log.info("[leaveflow] 휴게 넘김 거부: 시간차에 분량 전송 (userCd={}, workYmd={}, unit={}, W={})",
                    user, workYmd, unit, brkWaiveMin);
            throw new ApiException(AttdErrorCode.ATTD_400_221);
        }

        if (UNIT_FULL.equals(unit)) {
            leaveDays = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            leaveDays = new BigDecimal("0.50000");
            // 반차는 소정근로의 절반을 차감하므로 근무 스케줄이 있어야 한다.
            //   스케줄 없는 날(경계 산출 불가)은 반차 신청 불가(종일 연차만 가능). 웹 LeaveFlow 동일.
            // HB-02: 파트(시작기준/종료기준)를 받아 경계 시각을 확정 저장한다(정책 §8.5.10).
            // v2(BW2-04): 파트별 경계를 분량 W 로 산출(R2 — 경계 쪽 휴게부터 W 만큼 근로 전환). W=0 은 v1 미체크와 바이트 동일.
            //   파트 미상(null)은 END 로 산출만 하고 아래 ATTD_400_195 로 거부한다(거부 순서 종전 유지: 110 → 195).
            String halfPart = normalizeHalfPart(p.halfPart());
            HalfDayBoundary hb = leaveDeductionService.getHalfDayBoundary(
                    cmpny, site, user, workYmd, HalfPart.of(halfPart), brkWaiveMin);
            if (hb == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            if (halfPart == null) {
                // sec L-1: 클라 원시 입력을 그대로 출력하지 않는다(CR/LF 로그 위조 여지). 값은 미출력.
                log.info("[leaveflow] 반차 신청 거부: 파트 미지정/부적합 (userCd={}, workYmd={})", user, workYmd);
                throw new ApiException(AttdErrorCode.ATTD_400_195);
            }
            // ⑤ 실효 cap = min(법정 cap(X=H), 파트 movable). W 가 넘으면 221(클램프 저장 금지 — 사용자가 고른 값과 저장값 불일치 방지).
            if (brkWaive) {
                BreakWaiveCapUtils.CapResult cap = leaveDeductionService.getBreakWaiveCap(
                        cmpny, site, user, workYmd, hb.exemptMinutes());
                int effCap = (cap == null) ? 0 : Math.min(cap.capMin(), hb.movableBreakMin());
                if (brkWaiveMin > effCap) {
                    log.info("[leaveflow] 휴게 넘김 거부: 실효 cap 초과 (userCd={}, workYmd={}, part={}, W={}, cap={}, movable={}, req={})",
                            user, workYmd, halfPart, brkWaiveMin, cap == null ? null : cap.capMin(),
                            hb.movableBreakMin(), cap == null ? null : cap.requiredMin());
                    throw new ApiException(AttdErrorCode.ATTD_400_221);
                }
                brkWaiveMinEff = hb.appliedWaiveMin(); // W=0(기록 전용) 이면 0
            }
            leaveMinutes = hb.exemptMinutes(); // = daily / 2 (경계 산식과 동일 출처 — 값 불일치 원천 차단, H 불변)
            // 쉬는 구간 = 파트 기준 면제 구간(START = [근무시작, 경계) / END = [경계, 근무종료)).
            //   체크했는데 recordOnly(휴게가 쉬는 구간 안 / 분만 타입 G-2)면 미체크 경계 그대로 + 요청 기록만.
            startTime = ScheduleWorkMinutesUtils.hhmmOfDay(hb.exemptStartMin());
            endTime = ScheduleWorkMinutesUtils.hhmmOfDay(hb.exemptEndMin());
            // ★ Q5 정정(2026-08-07): START_DATE = END_DATE = workYmd 고정.
            //   연차 1행 = 하루가 코드베이스 전반의 불변식이라(실측 63행 전부 동일), END_DATE 를 익일로
            //   저장하면 `START_DATE <= d AND END_DATE >= d` 형태의 기간 술어 6곳이 야간 반차를 이틀로
            //   매칭한다(특히 selectOccupiedLeaveDaysOnDate 가 다음날 종일 연차를 ATTD_400_111 로 거부).
            //   자정 넘김은 "END_TIME < START_TIME 이면 익일"이라는 시각 wrap 으로만 표현하고,
            //   실제 instant 가 필요한 소비처(OT 면제구간·겹침 SQL)가 그 규약을 해석한다.
            log.info("[leaveflow] 반차 경계 확정: userCd={}, workYmd={}, part={}, {}~{}, 면제={}분",
                    user, workYmd, halfPart, startTime, endTime, leaveMinutes);
            if (brkWaive) {
                log.info("[leaveflow] 휴게 미이용 요청 확정: userCd={}, workYmd={}, unit={}, part={}, recordOnly={}, "
                                + "저장구간={}~{}, W={}, W_eff={}, movable={}",
                        user, workYmd, unit, halfPart, hb.recordOnly(), startTime, endTime,
                        brkWaiveMin, hb.appliedWaiveMin(), hb.movableBreakMin());
            }
        } else if (hourlyUnit) {
            startTime = p.startTime();
            endTime = p.endTime();
            Integer sMin = DateTimeUtils.hhmmToMinutes(startTime);
            Integer eMin = DateTimeUtils.hhmmToMinutes(endTime);
            if (sMin == null || eMin == null || eMin <= sMin) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            int minutes = eMin - sMin;
            int unitMin = unitMinutes(unit);
            // 단위 배수 검증은 신청 구간 길이 기준(Q-10 확정 — 체크 시 차감 분은 배수 검증 대상 아님).
            if (minutes % unitMin != 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            // 시간차 마일스톤(하한 가드) 기준 D = 그날 소정근로분 — 스케줄 필수(기존 가드 유지).
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            if (minutes > daily) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            // 근무시간 내 검증(prafta-app-018-B 보완): 시간차 연차는 정규 근무구간(스케줄) 안에서만
            //   신청 가능. 예) 스케줄 07:00~15:00 인데 03:00~04:30 신청 → 근무하지 않는 시간이라 거부.
            //   BW-04: 체크 시에도 "신청 구간"으로 검증한다 — 확장 구간은 근무 종료 클램프로 근무 시간을 넘지 않고,
            //   확장 구간으로 검증하면 2구간 사이 공백을 지나는 정상 케이스가 거부된다(6차 A안 주석 참조).
            if (!leaveDeductionService.withinScheduledWorkHours(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_103);
            }
            if (brkWaive) {
                // BW-04(요청서 §1-2, 2026-09-04 A안 개정): 체크 시 가로지름 거부(ATTD_400_055)를 건너뛰고,
                //   신청 시작부터 "근로"를 신청 길이만큼 확보하도록 쉬는 구간을 늘린다(휴게·구간 공백 건너뜀,
                //   양 끝에 맞닿은 휴게는 흡수). 저장 시각 = 확장 구간, 차감 = 신청 길이(클램프 시 확보 근로분).
                //   확장 결과가 신청과 같으면(건너뛸 휴게 없음) recordOnly — 시각 불변 + 요청 기록만.
                //   차감 0분(신청 이후 근로 없음)은 연차가 아니므로 거부(ATTD_400_052 재사용).
                BreakMergeResult mr = leaveDeductionService.mergeAdjacentBreaks(cmpny, site, user, workYmd, sMin, eMin);
                if (mr == null || mr.chargeMinutes() <= 0) {
                    log.info("[leaveflow] 시간차 신청 거부: 휴게 편입 결과 차감 0분/산출 불가 (userCd={}, workYmd={}, {}~{})",
                            user, workYmd, startTime, endTime);
                    throw new ApiException(AttdErrorCode.ATTD_400_052);
                }
                // v2(BW2-05, R4): 편입 후 남는 회사 휴게(B − absorbed)가 법정 하한(req, X=차감분) 미만이면 222 거부.
                //   absorbed 는 면제 구간 ∩ 휴게 시각 구간(공백 미포함 — mr.waivedBreakMinutes 는 공백 포함이라 사용 금지).
                int absorbed = assertHourlyLegalBreakAfterAbsorb(cmpny, site, user, workYmd, mr);
                startTime = ScheduleWorkMinutesUtils.hhmmOfDay(mr.exemptStartMin());
                endTime = ScheduleWorkMinutesUtils.hhmmOfDay(mr.exemptEndMin());
                leaveMinutes = mr.chargeMinutes();
                brkWaiveMinEff = mr.recordOnly() ? 0 : absorbed;
                log.info("[leaveflow] 휴게 미이용 요청 확정: userCd={}, workYmd={}, unit={}, part=-, recordOnly={}, "
                                + "저장구간={}~{}, 신청={}분, 차감={}분, 편입휴게={}분, absorbed={}분, clamped={}",
                        user, workYmd, unit, mr.recordOnly(), startTime, endTime,
                        mr.requestMinutes(), mr.chargeMinutes(), mr.waivedBreakMinutes(), absorbed, mr.clamped());
            } else {
                // 휴게 가로지름 거부(§8.5.9) — 휴게시각 미설정이면 skip
                if (leaveDeductionService.crossesBreak(cmpny, site, user, workYmd, sMin, eMin)) {
                    throw new ApiException(AttdErrorCode.ATTD_400_055);
                }
                leaveMinutes = minutes;
            }
            // 차감액(leaveDays)은 lock 획득 후 calcHourlyCharge 로 산출(아래 try 블록).
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

        // 5) 잔여 확보(타입 분기).
        //    '02'(또는 SYSTEM_YN='Y'): 기존 차감 GRANT 경로(만료 임박 우선, FOR UPDATE) 유지 — 회귀 0.
        //    '01'(사용자 신청): GRANT 가 없으므로 회계연도 한도(MAX_APLY_DAYS) 대비 사용분 검증.
        //      차감 GRANT 가 없어 FOR UPDATE 를 못 쓰니 (USER_CD,LEAVE_CD) advisory lock 으로 직렬화
        //      → 사용분 재집계 → 한도검증 → (8)INSERT 순서로 중복신청 레이스를 방지한다.
        boolean userApplyType = LEAVE_TYPE_USER_APPLY.equals(type.leaveType()) && !statutory;

        // prafta-com-011-2 가불: isBorrow=true 면 결재 강제(결정 §4) + 시스템 법정 월차/본연차만 허용.
        //   직접입력/무결재 자동확정 경로로 가불 진입 차단(가불은 항상 결재선 필수).
        boolean borrow = p.isBorrow();
        BorrowFamily borrowFamily = null;
        String hireDate = null;
        if (borrow) {
            if (hourlyUnit) {
                // LC-04(plan §8-③): 시간차 + 가불 조합 서버 거부 — 가불은 종일/반차만(웹 미러).
                //   (가불 분할 INSERT 는 LEAVE_MINUTES 를 첫 행에만 실어 그날 누적 판정을 오염시킴.)
                //   HB-04: 반반차 폐지로 구 `|| UNIT_QUARTER` 조건 제거(도달 불가 — 단위 게이팅에서 이미 거부).
                throw new ApiException(AttdErrorCode.ATTD_400_183);
            }
            if (!statutory) {
                throw new ApiException(AttdErrorCode.ATTD_400_180); // 가불=법정 연차만
            }
            if (userApplyType) {
                throw new ApiException(AttdErrorCode.ATTD_400_180); // '01' 사용자 신청은 가불 비대상
            }
            borrowFamily = borrowFamilyOf(leaveCd);
            if (borrowFamily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_180); // 월차/본연차 외 법정타입은 가불 비대상
            }
            aprvRequired = true; // 결재 강제(체크박스/타입 APRV_USE_YN 무시)
            hireDate = appLeaveFlowMapper.selectUserHireDate(cmpny, user);
            // 만료(소멸) 경과 일자 fail-closed 차단(서버, 프론트 alert 우회 방지).
            leaveGrantEngineService.assertBorrowWorkYmdWithinExpiry(cmpny, user, hireDate, workYmd, borrowFamily);
        }

        List<GrantCharge> charges = null; // 차감 대상(부여 ID + 일수). '01'은 [(null, leaveDays)]. 짜투리 발동 시 null.
        RemnantTriggerPlanVO remnantPlan = null; // PC-05: 짜투리 발동 계획(발동 시 charges 대신 사용, 웹 미러)
        Integer hourlyConv = null;       // 시간차 분모(발동 판정 입력 — calcHourlyCharge 결과 재사용)
        String lockKey = null;
        String dayLockKey = null;
        String remnantLockKey = null;
        boolean lockDeferred = false;    // '01' lock 해제가 afterCompletion 에 등록됐는지
        boolean dayLockDeferred = false; // leaveDay lock 해제가 afterCompletion 에 등록됐는지
        boolean remnantLockDeferred = false; // leaveRemnant lock 해제가 afterCompletion 에 등록됐는지
        try {
            if (hourlyUnit) {
                // F5: 같은 사용자·같은 날 시간차 누적 판정 직렬화 — leave01 advisory lock 패턴 재사용.
                //   재정산(LC-05)과 같은 키로 상호 배타(재정산 중 신청 차단). 웹 미러.
                dayLockKey = HourlyLeaveChargeUtils.leaveDayLockKey(cmpny, user, workYmd);
                acquireLeaveDayLock(dayLockKey);
                // 보안리뷰(Medium): finally 해제는 커밋 이전이라 "해제~커밋" 창에서 후속 신청이
                //   미커밋 스냅샷으로 그날 누적(하한 마일스톤/점유 가드)을 판정할 수 있다
                //   → 해제를 트랜잭션 완료(afterCompletion, 커밋/롤백 불문) 시점으로 미룬다.
                //   같은 커넥션 보장 근거는 AdvisoryLockTxUtils javadoc 참조. 등록 실패 시 finally 폴백. 웹 미러.
                dayLockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(dayLockKey, this::releaseLeaveDayLock);

                // LC-03: 분모 = 회사 환산시간(신청 대상일 기준, F4). 그날 시간차 누적(전 타입 합산, F3)
                //   기준 하한(3단 마일스톤)/캡(1.0) 가드를 거친 이번 건 차액을 부과한다.
                HourlyChargeVO hc = leaveDeductionService.calcHourlyCharge(cmpny, site, user, workYmd, leaveMinutes);
                if (hc == null) {
                    throw new ApiException(AttdErrorCode.ATTD_400_052);
                }
                leaveDays = hc.chargeDays();
                hourlyConv = hc.convMinutes(); // PC-05: 발동 판정 입력(분모 재조회 없이 재사용)
                log.info("[leaveflow] 시간차 차감 산출: userCd={}, workYmd={}, {}분(누적 {}분), conv={}, "
                                + "charge={}, dayTotal={}, 하한={}, 캡={}",
                        user, workYmd, leaveMinutes, hc.cumMinutesAfter(), hc.convMinutes(),
                        hc.chargeDays().toPlainString(), hc.dayTotalDays().toPlainString(),
                        hc.floorApplied(), hc.capApplied());
            }

            // 4-B) 같은 날 중복 등록 가드.
            //   - 이미 점유된 연차 일수(종일=1.0 / 반차·시간차=LEAVE_DAYS) + 신규 신청 일수 > 1.0 이면 거부.
            //     → 종일 등록일에 반차 추가, 종일 중복, 반차 누적 초과 등 "하루 초과" 중복 차단(ATTD_400_111).
            //   - 시간차(02/03/04)는 합산이 1.0 이하여도 기존 시간차와 시간대가 겹치면 거부(ATTD_400_112).
            //     겹치지 않는 시간차 병행 신청은 허용(정책).
            //   (LC-04: 시간차 leaveDays 산출이 lock 하에 이뤄지므로 본 가드도 try 내부로 이동 — 판정 동일.)
            BigDecimal occupied = appLeaveFlowMapper.selectOccupiedLeaveDaysOnDate(cmpny, user, workYmd);
            if (occupied == null) {
                occupied = BigDecimal.ZERO;
            }
            if (occupied.add(leaveDays).compareTo(BigDecimal.ONE) > 0) {
                log.info("[leaveflow] 연차 신청 거부: 같은 날 하루 초과 중복 (userCd={}, workYmd={}, 점유={}, 신규={})",
                        user, workYmd, occupied.toPlainString(), leaveDays.toPlainString());
                throw new ApiException(AttdErrorCode.ATTD_400_111);
            }
            // HB-09(D4): 겹침 검사 게이트를 "시각을 가진 단위"로 확장 — 반차도 경계 시각을 갖게 되어
            //   반차 ↔ 시간차 시간대 충돌을 막을 수 있다(예: 시작기준 반차 09:00~14:15 + 10:00~11:00 시간차.
            //   합계 0.625 ≤ 1.0 이라 ATTD_400_111 로는 걸리지 않아 지금까지 통과하던 조합).
            //   ★ sec N-2(2026-08-07): 판정을 SQL wrap CASE 에서 Java 로 이관(그날 원 스케줄 프레임 정렬).
            if (startTime != null && endTime != null) {
                if (leaveDeductionService.overlapsTimeLeaveOnDate(
                        cmpny, site, user, workYmd, startTime, endTime)) {
                    log.info("[leaveflow] 연차 신청 거부: 같은 날 연차 시간대 겹침 (userCd={}, workYmd={}, {}~{})",
                            user, workYmd, startTime, endTime);
                    throw new ApiException(AttdErrorCode.ATTD_400_112);
                }
            }

            if (userApplyType) {
                lockKey = leave01LockKey(cmpny, user, leaveCd);
                acquireLeave01Lock(lockKey);
                // 보안리뷰(Medium): leaveDay lock 과 동일 사유 — 회계연도 사용분 합산 판정도
                //   커밋 이전 해제 시 같은 창이 생기므로 afterCompletion 해제로 통일. 웹 미러.
                lockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(lockKey, this::releaseLeave01Lock);

                Integer maxAplyDays = type.maxAplyDays();
                if (maxAplyDays == null) {
                    // '01'인데 한도 미설정 → fail-closed(신청불가).
                    log.info("[leaveflow] 연차 신청 거부: 사용자 신청 한도(MAX_APLY_DAYS) 미설정 (userCd={}, leaveCd={})", user, leaveCd);
                    throw new ApiException(AttdErrorCode.ATTD_400_051);
                }
                // prafta-com-016-B(3-1): 사용가능기간 분기(웹 LeaveFlowServiceImpl 미러).
                //   '01' 설정안함 = 전체 누적(윈도우 없음, lifetime), 그 외('02' 해당연도내 포함) = 회계연도 윈도우(현행).
                BigDecimal used;
                if (AVAIL_TERM_NONE.equals(type.availTermType())) {
                    used = appLeaveFlowMapper.selectTotalUsedDays(cmpny, user, leaveCd);
                } else {
                    FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(cmpny);
                    used = appLeaveFlowMapper.selectFiscalUsedDays(
                            cmpny, user, leaveCd, fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());
                }
                if (used == null) {
                    used = BigDecimal.ZERO;
                }
                if (used.add(leaveDays).compareTo(BigDecimal.valueOf(maxAplyDays)) > 0) {
                    log.info("[leaveflow] 연차 신청 거부: 사용가능기간({}) 한도 초과 (userCd={}, leaveCd={}, used={}, req={}, max={})",
                            type.availTermType(), user, leaveCd, used, leaveDays, maxAplyDays);
                    throw new ApiException(AttdErrorCode.ATTD_400_051);
                }
                charges = List.of(new GrantCharge(null, leaveDays));
            } else if (borrow) {
                // prafta-com-011-2 (Q1=b): 잔여 우선 차감 + 부족분만 가불.
                charges = resolveBorrowCharges(cmpny, user, leaveCd, workYmd, leaveDays, borrowFamily, hireDate, user);
            } else {
                // PC-02(D8): 일반 신청 분할 차감 — 단일 부여 전량 충당(selectDeductibleGrant 단건)에서
                //   만료 임박순 다부여 분할 충당으로 교체(조각 부여 교착 해소, resolveBorrowCharges
                //   잔여 우선 루프와 동일 패턴 — 가불 없이). 합산 잔여 부족이면 기존 ATTD_400_051 유지. 웹 미러.
                // PC-05(D3~D6): 잔여 부족 지점에서 짜투리 발동을 판정해 충족 시 거부 대신 발동 처리(웹 미러).
                if (leaveDays.signum() > 0
                        && sumDeductibleRemaining(cmpny, user, leaveCd, workYmd).compareTo(leaveDays) < 0) {
                    // N9: 발동 판정~기록을 사용자 단위 advisory lock 으로 직렬화.
                    //   시간차 신청이면 위 leaveDay lock 이후 획득(획득 순서 leaveDay → leaveRemnant 고정 — 데드락 방지).
                    remnantLockKey = LeaveRemnantCoverService.remnantLockKey(cmpny, user);
                    acquireRemnantLock(remnantLockKey);
                    remnantLockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(
                            remnantLockKey, this::releaseRemnantLock);
                    // E7: 짜투리 발동 판정의 최소 사용단위 요금은 "신청 대상일의 분모" 기준 —
                    //   시간차는 calcHourlyCharge 결과(당일 분모) 재사용, 고정단위는 당일 분모 직접 조회.
                    //   null(미배정일 종일 신청 등)이면 evaluateTrigger 가 시간차 제외 최소단위(반차)로 판정(정합). 웹 미러.
                    Integer convForRemnant = (hourlyConv != null)
                            ? hourlyConv
                            : leaveConversionPolicyService.resolveDailyConvMinutes(cmpny, site, user, workYmd);
                    remnantPlan = leaveRemnantCoverService.evaluateTrigger(
                            cmpny, user, workYmd, leaveCd, unit, leaveMinutes, leaveDays, convForRemnant);
                    if (remnantPlan == null) {
                        // 2026-08-17: 대상 날짜 기준 유효한 부여가 0건이면 "잔여 부족"이 아니라
                        //   "부여 유효기간 밖 날짜"다(예: 부여 시작 8/16 계정의 8/13 자 신청).
                        //   화면 잔여(오늘 기준)와 판정이 어긋나 보여 혼란이 실발생 — 날짜 관점으로 안내한다.
                        if (appLeaveFlowMapper.countGrantsValidOnDate(cmpny, user, leaveCd, workYmd) == 0) {
                            log.info("[leaveflow] 연차 신청 거부: 대상일 기준 유효 부여 없음(부여 유효기간 밖) "
                                            + "(userCd={}, leaveCd={}, workYmd={})",
                                    user, leaveCd, workYmd);
                            throw new ApiException(AttdErrorCode.ATTD_400_204);
                        }
                        log.info("[leaveflow] 연차 신청 거부: 합산 잔여 부족(짜투리 발동 비대상) "
                                        + "(userCd={}, leaveCd={}, needed={})",
                                user, leaveCd, leaveDays.toPlainString());
                        throw new ApiException(AttdErrorCode.ATTD_400_051);
                    }
                    // D6: 실제 차감 = 잔여 전액 — 요청(REQ.LEAVE_DAYS)·통보도 실차감 기준(원장·표시 정합).
                    leaveDays = remnantPlan.remnantDays();
                } else {
                    charges = resolveGeneralCharges(cmpny, user, leaveCd, workYmd, leaveDays);
                }
            }

            submitLeaveCore(p, cmpny, site, user, workYmd, leaveCd, unit, aprvRequired,
                    leaveDays, leaveMinutes, startTime, endTime, charges, remnantPlan,
                    brkWaiveYnEff, brkWaiveMinEff);
        } finally {
            // afterCompletion 등록 성공분은 여기서 해제하지 않는다(이중 해제 방지) —
            //   커밋/롤백 직후 같은 커넥션에서 해제된다. 등록 실패(동기화 비활성) 시에만 폴백.
            if (lockKey != null && !lockDeferred) {
                releaseLeave01Lock(lockKey);
            }
            if (dayLockKey != null && !dayLockDeferred) {
                releaseLeaveDayLock(dayLockKey);
            }
            if (remnantLockKey != null && !remnantLockDeferred) {
                releaseRemnantLock(remnantLockKey);
            }
        }
    }

    @Override
    public LeaveDeductionPreviewResponse previewDeduction(LeaveDeductionPreviewParam p) {
        final String cmpny = p.gvCmpnyCd();
        final String site = p.gvSiteCd();
        final String user = p.gvUserCd();
        final String workYmd = p.workYmd();
        final String leaveCd = p.leaveCd();
        final String unit = p.useUnitType();

        // 1) 타입 메타 (submitLeave 미러)
        LeaveTypeInfoRow type = appLeaveFlowMapper.selectLeaveTypeInfo(cmpny, leaveCd);
        if (type == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        boolean statutory = isYes(type.systemYn());
        boolean hourlyUnit = UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit);

        // 1-B) PRAFTA-APP-022 룰B 미러: 출근 기록 존재 일자 연차 불가 — 신청 시 거부될 값 사전 차단.
        if (appLeaveFlowMapper.countAttendanceByDate(cmpny, site, user, workYmd) > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_108);
        }

        // 2) 단위 게이팅 미러(D2). HB-04: 반반차('05')는 허용집합에서 폐지되어 여기서 거부된다.
        List<String> allowedUnits = resolveAllowedUnits(cmpny, statutory, type.useUnitType());
        if (!allowedUnits.contains(unit)) {
            throw new ApiException(AttdErrorCode.ATTD_400_102);
        }

        // 3) 단위 구조검증 + 예상 차감 산출 (submitLeave 단위 분기 미러 — INSERT 없음, 조회 전용)
        BigDecimal charge;
        boolean floorApplied = false;
        boolean capApplied = false;
        Integer convFromCharge = null; // 시간차는 calcHourlyCharge 가 이미 분모를 조회하므로 재사용
        BigDecimal floorDays = null; // 발동 마일스톤 요금(0.5/1.0) — FE 하한 안내 단위 분기용(미발동 null)
        Integer previewMinutes = null; // 시간차 신청 분(짜투리 발동 판정 입력 — PC-05, 웹 미러)
        // HB-03: 반차 경계 미리보기(신청 전 "몇 시부터/까지" 표기). 반차 외 단위/산출 불가면 전부 null.
        HalfDayBoundary halfBoundary = null;
        // BW-04: 휴게 무시 preview 필드(§4-1). 미체크/해당 없음이면 null 또는 'N'.
        // v2(BW2-04): submit 과 동일 검증 순서(① null→0 ② step 221 ③ 게이트 ④ 시간차 W>0 221 ⑤ 반차 실효 cap 221).
        final int brkWaiveMin = p.brkWaiveMin();
        if (!BreakWaiveCapUtils.isValidStep(brkWaiveMin)) {
            throw new ApiException(AttdErrorCode.ATTD_400_221);
        }
        final boolean brkWaive = isYes(p.brkWaiveYn()) || brkWaiveMin > 0;
        String brkWaiveAppliedYn = "N";
        String brkWaiveRecordOnlyYn = "N";
        String brkWaiveExemptRange = null;
        Integer brkWaivedMinutes = null;
        Integer brkChargeMinutes = null;
        Integer brkWaiveMinResp = null; // v2: 반차 W_eff / 시간차 absorbed(저장 예정값)
        Integer brkWaiveCapMinResp = null; // v2: 반차+파트 실효 cap / 파트 없음·시간차 법정 cap
        // 시간차 겹침 선검사(HB-09)에 쓸 구간 — 체크 시 합친 구간(저장값과 동일 프레임).
        String overlapStart = p.startTime();
        String overlapEnd = p.endTime();
        assertBrkWaiveAllowed(cmpny, user, workYmd, unit, hourlyUnit, brkWaive);
        if (hourlyUnit && brkWaiveMin > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_221);
        }

        if (UNIT_FULL.equals(unit)) {
            charge = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            // 반차는 스케줄 필수(submitLeave 동일 거부). 고정요금 0.5.
            //   기존 3필드(halfDayBoundaryTime/halfStartPartRange/halfEndPartRange)는 END·미체크 경계 — 의미 불변
            //   (단, v2 §7 Q3: halfPart 가 오면 그 파트·W 기준 경계로 채운다 — 아래).
            halfBoundary = leaveDeductionService.getHalfDayBoundary(cmpny, site, user, workYmd);
            if (halfBoundary == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            charge = new BigDecimal("0.50000");
            String previewPart = normalizeHalfPart(p.halfPart());
            if (previewPart != null) {
                // v2(§7 Q3): 파트·W 기준 결과 시각 — submit 과 동일 산출·검증(실효 cap = min(법정 cap, 파트 movable)).
                HalfDayBoundary hbW = leaveDeductionService.getHalfDayBoundary(
                        cmpny, site, user, workYmd, HalfPart.of(previewPart), brkWaiveMin);
                if (hbW == null) {
                    throw new ApiException(AttdErrorCode.ATTD_400_110);
                }
                BreakWaiveCapUtils.CapResult cap = leaveDeductionService.getBreakWaiveCap(
                        cmpny, site, user, workYmd, hbW.exemptMinutes());
                int effCap = (cap == null) ? 0 : Math.min(cap.capMin(), hbW.movableBreakMin());
                if (brkWaive && brkWaiveMin > effCap) {
                    throw new ApiException(AttdErrorCode.ATTD_400_221);
                }
                halfBoundary = hbW;
                brkWaiveCapMinResp = effCap;
                if (brkWaive) {
                    brkWaiveAppliedYn = hbW.appliedWaiveMin() > 0 ? "Y" : "N";
                    brkWaiveRecordOnlyYn = hbW.appliedWaiveMin() > 0 ? "N" : "Y";
                    brkWaiveExemptRange = exemptRange(hbW);
                    brkWaiveMinResp = hbW.appliedWaiveMin();
                }
            } else {
                // 파트 없음(종전): 법정 cap 만 내리고, 체크 시 양 파트를 "전부 넘김"으로 요약(v1 호환).
                BreakWaiveCapUtils.CapResult cap = leaveDeductionService.getBreakWaiveCap(
                        cmpny, site, user, workYmd, halfBoundary.exemptMinutes());
                brkWaiveCapMinResp = (cap == null) ? null : cap.capMin();
                if (brkWaive && cap != null && brkWaiveMin > cap.capMin()) {
                    throw new ApiException(AttdErrorCode.ATTD_400_221);
                }
                if (brkWaive) {
                    HalfDayBoundary hbS = leaveDeductionService.getHalfDayBoundary(cmpny, site, user, workYmd, HalfPart.START, true);
                    HalfDayBoundary hbE = leaveDeductionService.getHalfDayBoundary(cmpny, site, user, workYmd, HalfPart.END, true);
                    boolean anyApplied = (hbS != null && !hbS.recordOnly()) || (hbE != null && !hbE.recordOnly());
                    brkWaiveAppliedYn = anyApplied ? "Y" : "N";
                    brkWaiveRecordOnlyYn = anyApplied ? "N" : "Y";
                }
            }
        } else if (hourlyUnit) {
            Integer sMin = DateTimeUtils.hhmmToMinutes(p.startTime());
            Integer eMin = DateTimeUtils.hhmmToMinutes(p.endTime());
            if (sMin == null || eMin == null || eMin <= sMin) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            int minutes = eMin - sMin;
            if (minutes % unitMinutes(unit) != 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_110);
            }
            if (minutes > daily) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            if (!leaveDeductionService.withinScheduledWorkHours(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_103);
            }
            int chargeMinutes = minutes;
            if (brkWaive) {
                // BW-04: submitLeave 시간차 체크 분기 미러 — 가로지름 거부 스킵 + 붙은 휴게 편입.
                BreakMergeResult mr = leaveDeductionService.mergeAdjacentBreaks(cmpny, site, user, workYmd, sMin, eMin);
                if (mr == null || mr.chargeMinutes() <= 0) {
                    throw new ApiException(AttdErrorCode.ATTD_400_052);
                }
                chargeMinutes = mr.chargeMinutes();
                // v2(BW2-05, R4): 편입 후 법정 하한 미달이면 222(submit 미러). absorbed = 저장 예정 BRK_WAIVE_MIN.
                int absorbed = assertHourlyLegalBreakAfterAbsorb(cmpny, site, user, workYmd, mr);
                BreakWaiveCapUtils.CapResult cap = leaveDeductionService.getBreakWaiveCap(
                        cmpny, site, user, workYmd, mr.chargeMinutes());
                brkWaiveCapMinResp = (cap == null) ? null : cap.capMin();
                brkWaiveMinResp = mr.recordOnly() ? 0 : absorbed;
                overlapStart = ScheduleWorkMinutesUtils.hhmmOfDay(mr.exemptStartMin());
                overlapEnd = ScheduleWorkMinutesUtils.hhmmOfDay(mr.exemptEndMin());
                brkWaiveAppliedYn = mr.recordOnly() ? "N" : "Y";
                brkWaiveRecordOnlyYn = mr.recordOnly() ? "Y" : "N";
                brkWaiveExemptRange = ScheduleWorkMinutesUtils.toHhmm(mr.exemptStartMin()) + "~"
                        + ScheduleWorkMinutesUtils.toHhmm(mr.exemptEndMin());
                brkWaivedMinutes = mr.waivedBreakMinutes();
            } else if (leaveDeductionService.crossesBreak(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_055);
            }
            brkChargeMinutes = chargeMinutes;
            // (BW-06 법정 경고 산출은 v2 에서 폐지 — 응답 필드는 BW2-07 이 일괄 제거할 때까지 null 로 내린다.)
            // advisory lock 없이 산출(조회 전용 추정치). 제출 시 lock 하에 재계산되며 코어 산식이
            //   단일 출처(HourlyLeaveChargeUtils)라 동시 신청이 없는 한 preview == 확정값. 웹 미러.
            //   BW-04: 입력은 차감 분(체크 시 신청 − 휴게 겹침) — submitLeave 의 leaveMinutes 와 동일 값.
            HourlyChargeVO hc = leaveDeductionService.calcHourlyCharge(cmpny, site, user, workYmd, chargeMinutes);
            if (hc == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            charge = hc.chargeDays();
            floorApplied = hc.floorApplied();
            capApplied = hc.capApplied();
            convFromCharge = hc.convMinutes();
            floorDays = hc.floorDays();
            previewMinutes = chargeMinutes;
        } else {
            throw new ApiException(AttdErrorCode.ATTD_400_054);
        }

        // 4) 사후 신청 마감 가드 (submitLeave 미러)
        if (workYmd.compareTo(todayYmd()) < 0) {
            String closeYm = workYmd.substring(0, 6);
            if (attdCloseService.isClosedForUser(cmpny, site, user, closeYm)) {
                throw new ApiException(AttdErrorCode.ATTD_400_050);
            }
        }

        // 5) 같은 날 중복 등록 가드 (submitLeave 4-B 미러)
        BigDecimal occupied = appLeaveFlowMapper.selectOccupiedLeaveDaysOnDate(cmpny, user, workYmd);
        if (occupied == null) {
            occupied = BigDecimal.ZERO;
        }
        if (occupied.add(charge).compareTo(BigDecimal.ONE) > 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_111);
        }
        // HB-09: 반차도 겹침 검사 대상(preview 는 파트 미확정이라 실제 경계 구간으로 검사할 수 없어
        //   시간차만 사전 판정한다 — 반차는 submitLeave 에서 최종 판정. preview 는 사전 안내 용도).
        //   BW-04: 체크 시 합친 구간(저장값)으로 검사한다(submitLeave 와 동일 판정).
        if (hourlyUnit
                && leaveDeductionService.overlapsTimeLeaveOnDate(
                        cmpny, site, user, workYmd, overlapStart, overlapEnd)) {
            throw new ApiException(AttdErrorCode.ATTD_400_112);
        }

        // 6) 잔여 부족 판정 — 에러가 아니라 플래그(FE 사전 경고). 가불은 preview 비대상(종일/반차 전용).
        boolean insufficient = false;
        boolean grantBased = false; // 부여 기반 신청 여부(짜투리 발동 preview 대상 — PC-05, 웹 미러)
        if (charge.signum() > 0) {
            boolean userApplyType = LEAVE_TYPE_USER_APPLY.equals(type.leaveType()) && !statutory;
            if (userApplyType) {
                Integer maxAplyDays = type.maxAplyDays();
                if (maxAplyDays == null) {
                    insufficient = true; // 한도 미설정 = 신청불가(fail-closed, submitLeave 동일 기준)
                } else {
                    BigDecimal used;
                    if (AVAIL_TERM_NONE.equals(type.availTermType())) {
                        used = appLeaveFlowMapper.selectTotalUsedDays(cmpny, user, leaveCd);
                    } else {
                        FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(cmpny);
                        used = appLeaveFlowMapper.selectFiscalUsedDays(
                                cmpny, user, leaveCd, fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());
                    }
                    if (used == null) {
                        used = BigDecimal.ZERO;
                    }
                    insufficient = used.add(charge).compareTo(BigDecimal.valueOf(maxAplyDays)) > 0;
                }
            } else {
                // PC-02(D8): 부여 기반 — 합산 잔여(만료 임박순 전 부여) 기준으로 교체(분할 차감과 동일 판정).
                //   (FOR UPDATE SQL 재사용 — 비트랜잭션 preview 라 autocommit 으로 행 잠금이 즉시
                //   해제되어 실질 잠금 부작용 없음 — 기존 단건 판정과 동일 관례.) 웹 미러.
                grantBased = true;
                insufficient = sumDeductibleRemaining(cmpny, user, leaveCd, workYmd).compareTo(charge) < 0;
            }
        }

        // E1·E4: convMinutes = 신청 대상일 기준 당일 분모. 시간차는 calcHourlyCharge 가 이미
        //   조회(산출 불가면 ATTD_400_194 전파), 고정단위(종일/반차/반반차)는 표기 전용이라
        //   폴백 체인 = 당일 분모 → 참고치(개인 기본 근무타입, E4 규약 — 편차 허용, 사용자 확정
        //   2026-08-03) → 480(FE formatLeaveDays 폴백과 정합). 웹 미러.
        Integer convDaily = (convFromCharge != null)
                ? convFromCharge
                : leaveConversionPolicyService.resolveDailyConvMinutes(cmpny, site, user, workYmd);
        Integer convPersonal = (convDaily != null)
                ? convDaily
                : leaveConversionPolicyService.resolvePersonalConvMinutes(cmpny, user, workYmd);
        int conv = (convPersonal != null) ? convPersonal : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;

        // PC-05(D6) preview: 부여 기반 신청이 잔여 부족이면 짜투리 발동 여부를 판정해 안내(FE UI-D).
        //   발동 예상이면 신청은 성공하므로 insufficient 를 내리고 발동 필드를 싣는다.
        //   lock 없는 추정치 — 제출 시 remnant lock 하에 재판정(시간차 preview 관례 미러). 웹 미러.
        //   E7: 판정 입력 conv 는 submit 과 동일하게 "당일 분모"(convDaily — 참고치 폴백 미적용)를
        //   전달한다. 참고치를 섞으면 미배정일 최소단위 판정이 submit(반차)과 어긋난다(preview≠확정).
        boolean remnantTriggered = false;
        BigDecimal remnantDays = null;
        Integer companyCoverMinutes = null;
        if (insufficient && grantBased) {
            RemnantTriggerPlanVO plan = leaveRemnantCoverService.evaluateTrigger(
                    cmpny, user, workYmd, leaveCd, unit, previewMinutes, charge, convDaily);
            if (plan != null) {
                remnantTriggered = true;
                remnantDays = plan.remnantDays();
                companyCoverMinutes = plan.coverMinutes();
                insufficient = false;
            }
        }

        // 2026-08-17: 잔여 부족의 원인 구분 — 대상 날짜 기준 유효한 부여가 0건이면(부여 유효기간 밖 날짜)
        //   FE 가 "예상 차감 초과" 대신 날짜 안내를 띄우도록 플래그 + 이후 가장 이른 부여 시작일을 싣는다.
        //   (submit 의 ATTD_400_204 분기와 동일 판정 — preview≠확정 불일치 방지.)
        boolean noGrantOnDate = false;
        String grantAvailFromDate = null;
        if (insufficient && grantBased
                && appLeaveFlowMapper.countGrantsValidOnDate(cmpny, user, leaveCd, workYmd) == 0) {
            noGrantOnDate = true;
            grantAvailFromDate = appLeaveFlowMapper.selectMinAvailFromAfterDate(cmpny, user, leaveCd, workYmd);
        }

        log.debug("[leaveflow] 예상 차감 preview: userCd={}, workYmd={}, unit={}, charge={}, 하한={}, 캡={}, "
                        + "잔여부족={}, conv={}, 짜투리발동={}, 유효부여없음={}",
                user, workYmd, unit, charge.toPlainString(), floorApplied, capApplied, insufficient, conv,
                remnantTriggered, noGrantOnDate);

        return new LeaveDeductionPreviewResponse(charge, floorApplied, capApplied, insufficient, conv, floorDays,
                remnantTriggered, remnantDays, companyCoverMinutes,
                halfDayBoundaryTime(halfBoundary), halfStartPartRange(halfBoundary), halfEndPartRange(halfBoundary),
                noGrantOnDate, grantAvailFromDate,
                brkWaiveAppliedYn, brkWaiveRecordOnlyYn, brkWaiveExemptRange, brkWaivedMinutes, brkChargeMinutes,
                brkWaiveMinResp, brkWaiveCapMinResp);
    }

    @Override
    public LeaveDayScheduleResponse selectDaySchedule(LeaveDayScheduleParam p) {
        // 시간차 휴게 가로지름(ATTD_400_055) 사전 안내용 조회 전용 — 스케줄 없는 날은 에러가 아니라
        //   hasSchedule=false (신청 가능 여부 판정은 submitLeave/preview 가 담당, 여기선 표시 정보만).
        // HB-03: 반차 경계 미리보기 3필드를 함께 내린다(additive — 구 앱 무영향).
        //   ★ 경계는 서버 산식(ScheduleWorkMinutesUtils)이 단일 출처다. FE 재계산 금지.
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(
                p.cmpnyCd(), p.siteCd(), p.userCd(), p.workYmd());
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(sch);
        // BW-06: 회사 토글(미노출 판단) — 활성 사용정책 없으면 'N'(submit 게이트와 동일 fail-closed).
        LeaveUsagePolicyRow companyPolicy = appLeaveFlowMapper.selectCompanyUsageUnit(p.cmpnyCd());
        String brkWaiveAllowYn = (companyPolicy != null && isYes(companyPolicy.brkWaiveAllowYn())) ? "Y" : "N";
        if (sch == null || hb == null) {
            return LeaveDayScheduleResponse.of(sch,
                    halfDayBoundaryTime(hb), halfStartPartRange(hb), halfEndPartRange(hb),
                    null, brkWaiveAllowYn, "N", null, null, null, null, null);
        }
        // v2(BW2-04, §7 Q3): day-schedule 은 cap·step·movable 만 내린다(파트·W 결과 시각은 preview).
        //   cap 은 법정 cap(X=H) — 회사 토글 N 이어도 필드는 내린다(FE 가 섹션 자체를 숨김). 서버 단일 출처, FE 재계산 금지.
        HalfDayBoundary startPlain = ScheduleWorkMinutesUtils.halfDayBoundary(sch, HalfPart.START, false);
        HalfDayBoundary endPlain = hb; // END·미체크 = HB-03 기존값
        BreakWaiveCapUtils.CapResult cap = leaveDeductionService.getBreakWaiveCap(
                p.cmpnyCd(), p.siteCd(), p.userCd(), p.workYmd(), hb.exemptMinutes());
        int movableStart = ScheduleWorkMinutesUtils.movableBreakMinutes(sch, HalfPart.START);
        int movableEnd = ScheduleWorkMinutesUtils.movableBreakMinutes(sch, HalfPart.END);
        return LeaveDayScheduleResponse.of(sch,
                halfDayBoundaryTime(endPlain), halfStartPartRange(endPlain), halfEndPartRange(endPlain),
                halfDayBoundaryTime(startPlain),
                brkWaiveAllowYn,
                hb.breakTimeRegistered() ? "Y" : "N",
                cap == null ? null : cap.capMin(),
                BreakWaiveCapUtils.STEP_MIN,
                BreakWaiveCapUtils.guideText(cap),
                movableStart,
                movableEnd);
    }

    /**
     * v2(BW2-05, R4): 시간차 휴게 편입 후 법정 휴게 하한 검증(submit·preview 공용).
     * {@code absorbed} = 면제 구간 ∩ 휴게 시각 구간(공백 미포함 — {@code mr.waivedBreakMinutes()} 는 구간 공백을 포함하므로 사용 금지).
     * {@code B − absorbed < req}(req 는 X=차감분으로 산출) 이면 {@code ATTD_400_222}. cap 산출 불가(스케줄 없음)면 검증 생략.
     *
     * @return absorbed(저장 예정 BRK_WAIVE_MIN — recordOnly 여부는 호출부가 0 으로 정리)
     */
    private int assertHourlyLegalBreakAfterAbsorb(String cmpny, String site, String user, String workYmd,
                                                  BreakMergeResult mr) {
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpny, site, user, workYmd);
        int absorbed = BreakWaiveCapUtils.breakMinutesWithin(sch, mr.exemptStartMin(), mr.exemptEndMin());
        BreakWaiveCapUtils.CapResult cap = leaveDeductionService.getBreakWaiveCap(
                cmpny, site, user, workYmd, mr.chargeMinutes());
        if (cap != null && cap.companyBreakMin() - absorbed < cap.requiredMin()) {
            log.info("[leaveflow] 시간차 휴게 편입 거부(R4 법정 하한): userCd={}, workYmd={}, R={}, req={}, B={}, absorbed={}",
                    user, workYmd, cap.remainWorkMin(), cap.requiredMin(), cap.companyBreakMin(), absorbed);
            throw new ApiException(AttdErrorCode.ATTD_400_222);
        }
        return absorbed;
    }

    /** BW-06: 파트 기준 쉬는(면제) 구간 표기 "HHMM~HHMM". 산출 불가면 null. */
    private String exemptRange(HalfDayBoundary hb) {
        return (hb == null) ? null
                : ScheduleWorkMinutesUtils.toHhmm(hb.exemptStartMin()) + "~"
                        + ScheduleWorkMinutesUtils.toHhmm(hb.exemptEndMin());
    }

    // ============================================================
    // 연차 신청 증빙 필수화(2026-08-29): 업로드/제출 분리 아키텍처
    // ============================================================

    @Override
    public String uploadEvidenceFile(TokenInfo tokenInfo, MultipartFile file) {
        if (tokenInfo == null || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null
                || tokenInfo.gv_siteCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String cmpny = tokenInfo.gv_cmpnyCd();
        String site = tokenInfo.gv_siteCd();
        String user = tokenInfo.gv_userCd();

        // AppNearMiss01Controller.report 패턴 미러: 채번 후 FileService.fileSave 로 저장(008=secure base).
        String fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(cmpny, FILE_TYPE_EVIDENCE));
        fileService.fileSave(FileInfoParam.from(cmpny, user, site, FILE_TYPE_EVIDENCE, fileMgmtCd, file));

        log.info("[leaveflow] 연차 증빙 파일 업로드 완료 userCd={}, fileMgmtCd={}", user, fileMgmtCd);
        return fileMgmtCd;
    }

    @Override
    public FileBytesResult loadEvidenceFile(TokenInfo tokenInfo, String fileMgmtCd) {
        if (tokenInfo == null || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (fileMgmtCd == null || fileMgmtCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String cmpny = tokenInfo.gv_cmpnyCd();
        String user = tokenInfo.gv_userCd();

        String ownerUserCd = appLeaveFlowMapper.selectEvidenceFileOwner(cmpny, fileMgmtCd);
        if (ownerUserCd == null) {
            // 대상 없음(존재 비노출) — 일반 404.
            throw new ApiException(AttdErrorCode.ATTD_404_020);
        }
        boolean isOwner = user.equals(ownerUserCd);
        boolean isApprover = false;
        if (!isOwner) {
            // 신청 제출 전(orphan 업로드)이면 reqId 가 없어 결재자 판정 자체가 성립하지 않는다(업로드자 본인만 열람).
            String reqId = appLeaveFlowMapper.selectReqIdByEvidenceFileId(cmpny, fileMgmtCd);
            if (reqId != null) {
                isApprover = isApproverOf(cmpny, reqId, user);
            }
        }
        if (!isOwner && !isApprover) {
            log.warn("[leaveflow] 증빙 파일 열람 스코프 위반(IDOR 차단) - userCd={}, fileMgmtCd={}", user, fileMgmtCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        FileBytesResult file = fileService.loadFileBytes(new FileReadQuery(cmpny, fileMgmtCd));
        if (file == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_020);
        }
        return file;
    }

    /** AppAdminApprovalServiceImpl.isApproverOf 패턴 재사용 — reqId 결재선에 userCd 포함 여부. */
    private boolean isApproverOf(String cmpnyCd, String reqId, String userCd) {
        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(cmpnyCd, reqId);
        if (steps == null) {
            return false;
        }
        for (ApprovalStepVO s : steps) {
            if (userCd.equals(s.getApproverUserCd())) {
                return true;
            }
        }
        return false;
    }

    /** HB-02: 반차 파트 정규화. START/END(대소문자 무관) 외 값·공백은 null(호출부 fail-closed 거부). */
    private String normalizeHalfPart(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toUpperCase();
        if (HALF_PART_START.equals(v) || HALF_PART_END.equals(v)) {
            return v;
        }
        return null;
    }

    /**
     * BW-04 휴게 무시 게이트(submit·preview 공용, 순서 고정).
     * <ol>
     *   <li>체크('Y')인데 단위가 반차(01)/시간차(02/03/04)가 아니면 {@code ATTD_400_219}.</li>
     *   <li>회사 활성 사용정책 {@code BRK_WAIVE_ALLOW_YN} 이 'Y' 가 아니면(정책 없음 포함 — fail-closed) {@code ATTD_400_217}.</li>
     * </ol>
     * 미체크('N')면 아무것도 하지 않는다(기존 요청 무회귀).
     *
     * @param brkWaive v2(BW2-04): {@code waiveRequested = 'Y'.equals(brkWaiveYn) || brkWaiveMin > 0}(217/219 판정 무변경)
     */
    private void assertBrkWaiveAllowed(String cmpnyCd, String userCd, String workYmd, String unit,
                                       boolean hourlyUnit, boolean brkWaive) {
        if (!brkWaive) {
            return;
        }
        if (!UNIT_HALF.equals(unit) && !hourlyUnit) {
            log.info("[leaveflow] 휴게 무시 요청 거부: 반차/시간차 외 단위 (userCd={}, workYmd={}, unit={})",
                    userCd, workYmd, unit);
            throw new ApiException(AttdErrorCode.ATTD_400_219);
        }
        LeaveUsagePolicyRow companyPolicy = appLeaveFlowMapper.selectCompanyUsageUnit(cmpnyCd);
        if (companyPolicy == null || !isYes(companyPolicy.brkWaiveAllowYn())) {
            log.info("[leaveflow] 휴게 무시 요청 거부: 회사 미허용(BRK_WAIVE_ALLOW_YN) (userCd={}, workYmd={}, 정책존재={})",
                    userCd, workYmd, companyPolicy != null);
            throw new ApiException(AttdErrorCode.ATTD_400_217);
        }
    }

    /** HB-03: 반차 경계 시각 표기(HHMM, 자정 경계는 "2400"). 산출 불가면 null. */
    private String halfDayBoundaryTime(HalfDayBoundary hb) {
        return (hb == null) ? null : ScheduleWorkMinutesUtils.toHhmm(hb.boundaryMin());
    }

    /** HB-03: 시작기준(늦게 출근) 반차가 쉬는 구간 표기 "HHMM~HHMM". 산출 불가면 null. */
    private String halfStartPartRange(HalfDayBoundary hb) {
        return (hb == null) ? null
                : ScheduleWorkMinutesUtils.toHhmm(hb.workStartMin()) + "~"
                        + ScheduleWorkMinutesUtils.toHhmm(hb.boundaryMin());
    }

    /** HB-03: 종료기준(일찍 퇴근) 반차가 쉬는 구간 표기 "HHMM~HHMM". 산출 불가면 null. */
    private String halfEndPartRange(HalfDayBoundary hb) {
        return (hb == null) ? null
                : ScheduleWorkMinutesUtils.toHhmm(hb.boundaryMin()) + "~"
                        + ScheduleWorkMinutesUtils.toHhmm(hb.workEndMin());
    }

    /**
     * PC-02(D8): 일반(비가불·비'01') 신청의 부여 충당 계획 — 만료 임박순(AVAIL_TO_DATE ASC) 다부여 분할 차감(웹 미러).
     *
     * <ul>
     *   <li>잔여&gt;0 활성 부여를 만료 임박순으로 needed 까지 채운다(FOR UPDATE 직렬화 —
     *       {@link #resolveBorrowCharges} 잔여 우선 루프와 동일 패턴, 가불 없이).</li>
     *   <li>합산 잔여 &lt; needed 면 기존과 동일하게 ATTD_400_051(조각 부여 교착만 해소, 거부 기준 불변).</li>
     *   <li>needed 0(하한/캡 이후 차액 0 등)은 기존 단건 경로 유지 — 잔여 0 부여에도 0 차감 행이
     *       기록되던 기존 동작 보존(REQ-사용행 연결 유지, 회귀 0).</li>
     * </ul>
     */
    private List<GrantCharge> resolveGeneralCharges(String cmpny, String user, String leaveCd, String workYmd,
                                                    BigDecimal needed) {
        if (needed.signum() <= 0) {
            DeductibleGrantRow grant = appLeaveFlowMapper.selectDeductibleGrant(cmpny, user, leaveCd, workYmd, needed);
            if (grant == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_051);
            }
            return List.of(new GrantCharge(grant.grantId(), needed));
        }
        List<DeductibleGrantRow> grants = appLeaveFlowMapper.selectDeductibleGrants(cmpny, user, leaveCd, workYmd);
        List<GrantCharge> charges = new ArrayList<>();
        BigDecimal remaining = needed;
        for (DeductibleGrantRow g : grants) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal take = avail.min(remaining);
            charges.add(new GrantCharge(g.grantId(), take));
            remaining = remaining.subtract(take);
        }
        if (remaining.signum() > 0) {
            log.info("[leaveflow] 연차 신청 거부: 합산 잔여 부족 (userCd={}, leaveCd={}, needed={}, 부족분={})",
                    user, leaveCd, needed.toPlainString(), remaining.toPlainString());
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
        return charges;
    }

    /**
     * PC-02(D8): 차감 가능한 활성 부여의 합산 잔여 — preview 잔여 부족 판정·짜투리 사전 판정(웹 미러).
     *
     * <p>보안리뷰 M-1: FOR UPDATE 목록 조회 재사용 시 행 잠금 보유 후 remnant advisory lock 을
     * 대기하는 순서 역전(회수 경로와 교차 → GET_LOCK 타임아웃 정지)이 생겨, 잠금 없는 SUM 전용
     * 쿼리로 분리했다. 실제 차감 계획({@code resolveGeneralCharges})은 여전히 FOR UPDATE 로 재판정한다.
     */
    private BigDecimal sumDeductibleRemaining(String cmpny, String user, String leaveCd, String workYmd) {
        BigDecimal sum = appLeaveFlowMapper.selectDeductibleRemainingSum(cmpny, user, leaveCd, workYmd);
        return (sum == null) ? BigDecimal.ZERO : sum;
    }

    /**
     * prafta-com-011-2 가불(Q1=b): 신청 일수를 잔여 부여로 만료 임박순 분할 차감하고, 부족분(deficit)만큼
     *   가불 슬롯에 충당하는 차감 계획을 만든다(웹 미러, 통일 모델 §6-2).
     *
     * <ul>
     *   <li>잔여(현재 사용가능 active GRANT 의 GRANT_DAYS-USED_DAYS, AVAIL_FROM&lt;=workYmd)를 만료 임박순으로
     *       needed 까지 채운다(FOR UPDATE 로 직렬화). 미발생 가불 GRANT(AVAIL_FROM&gt;workYmd)는 여기 안 잡힘(D2 잠금).</li>
     *   <li>남은 deficit 이 0 이면 가불 0건(=일반 신청과 동일 결과, 단 결재 강제).</li>
     *   <li>deficit>0 이면 가불 한도(computeBorrowQuota) 와 비교 — 초과면 ATTD_400_182. 통과면 createBorrowGrant
     *       로 가불 슬롯(전량 GRANT, AVAIL_FROM=발생일)을 기존 재사용→신규 생성으로 충당받아 각 슬롯 grantId 로
     *       deficit 만큼 leave_use 를 분할 차감한다(슬롯 days 합 = deficit).</li>
     * </ul>
     */
    private List<GrantCharge> resolveBorrowCharges(String cmpny, String user, String leaveCd, String workYmd,
                                                   BigDecimal needed, BorrowFamily family, String hireDate,
                                                   String operatorUserCd) {
        List<GrantCharge> charges = new ArrayList<>();
        BigDecimal remaining = needed;

        // 1) 잔여 우선 차감(만료 임박순, FOR UPDATE).
        List<DeductibleGrantRow> grants =
                appLeaveFlowMapper.selectBorrowDeductibleGrants(cmpny, user, leaveCd, workYmd);
        for (DeductibleGrantRow g : grants) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal take = avail.min(remaining);
            charges.add(new GrantCharge(g.grantId(), take));
            remaining = remaining.subtract(take);
        }

        // 2) 부족분만 가불.
        if (remaining.signum() > 0) {
            BigDecimal quota = leaveGrantEngineService.computeBorrowQuota(cmpny, user, hireDate, family);
            if (remaining.compareTo(nz(quota)) > 0) {
                log.info("[leaveflow] 가불 한도 초과 거부 userCd={}, leaveCd={}, deficit={}, quota={}",
                        user, leaveCd, remaining.toPlainString(), nz(quota).toPlainString());
                throw new ApiException(AttdErrorCode.ATTD_400_182);
            }
            BorrowGrantResultVO result = leaveGrantEngineService.createBorrowGrant(
                    cmpny, user, hireDate, family, remaining, workYmd, operatorUserCd);
            BigDecimal borrowRemaining = remaining;
            for (BorrowGrantSlotVO slot : result.getSlots()) {
                if (borrowRemaining.signum() <= 0) {
                    break;
                }
                if (slot.getGrantId() == null) {
                    // 통일 모델(§6-2): 충당 슬롯은 항상 유효 grantId(재사용/신규)를 가진다. 이미 정기 발생 슬롯은
                    //   createBorrowGrant 가 slots 에서 제외(또는 ATTD_400_182 롤백)하므로 null 도달은 비정상 → 차단.
                    throw new ApiException(AttdErrorCode.ATTD_400_182);
                }
                BigDecimal take = nz(slot.getDays()).min(borrowRemaining);
                charges.add(new GrantCharge(slot.getGrantId(), take));
                borrowRemaining = borrowRemaining.subtract(take);
            }
            if (borrowRemaining.signum() > 0) {
                // 가불 GRANT 가 부족분을 다 못 채움(슬롯 합 < deficit) → 한도 초과로 차단.
                throw new ApiException(AttdErrorCode.ATTD_400_182);
            }
        }
        return charges;
    }

    /** null-safe BigDecimal(0 폴백). */
    private BigDecimal nz(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    /** prafta-com-011-2: 차감 대상 1건(부여 ID + 일수). 가불 split 시 한 신청이 여러 GrantCharge 로 분할된다. */
    private record GrantCharge(String grantId, BigDecimal days) {
    }

    /**
     * 연차 신청 본 흐름(요청 INSERT → 결재라인 → 사용기록 INSERT → 즉시확정 → PUSH).
     *
     * <p>잔여 확보(타입 분기)는 호출부에서 끝낸 뒤 진입한다. {@code grantId} 가 null 이면 '01'(차감 GRANT 없음)이라
     *   {@code recomputeGrantUsedDays} 를 생략한다.
     * <p>PC-05: {@code remnantPlan} 이 있으면(짜투리 발동) charges 루프 대신 발동 처리
     *   ({@code LeaveRemnantCoverService.applyTrigger})로 use 행·COVER 를 기록한다(웹 미러).
     */
    private void submitLeaveCore(LeaveApplyParam p, String cmpny, String site, String user, String workYmd,
                                 String leaveCd, String unit, boolean aprvRequired,
                                 BigDecimal leaveDays, Integer leaveMinutes, String startTime, String endTime,
                                 List<GrantCharge> charges, RemnantTriggerPlanVO remnantPlan,
                                 String brkWaiveYn, Integer brkWaiveMin) {

        // 6) 요청 INSERT(REQ_TYPE='05'). 결재 Y면 신청('01'), N이면 즉시 승인('02').
        //    NODE_CD: 본문(p.nodeCd())은 위조 가능해 신뢰하지 않는다. 다만 종전처럼 null 을 저장하면
        //    "이 요청이 어느 부서 건인지" 기록이 사라져, NODE_CD 로 부서 스코프를 판정하는 쪽에서
        //    연차 요청이 통째로 탈락한다(캘린더 '처리 필요' 강조 미표시 / 부서 지정 근태 마감이
        //    미처리 연차를 못 막음 / 결재함 부서명 공란). 그래서 "안 쓴다" 대신 신청 시점에 서버가
        //    직접 조회한 소속부서를 박는다 — 출처만 신뢰 가능해지고 '요청 시점 스냅샷' 성질은 동일하며,
        //    이후 소속이동이 있어도 이 행의 값은 변하지 않는다.
        String reqNodeCd = attdCloseService.resolveUserNodeCd(cmpny, site, user);
        String reqId = appLeaveFlowMapper.selectNextReqId(cmpny);
        String reqStatus = aprvRequired ? REQ_APPLIED : REQ_APPROVED;
        // ★ HB-02(§9-1): 경계 시각은 LEAVE_USE 뿐 아니라 신청 테이블(TB_USER_ATTD_REQ)에도 저장한다.
        //   미결 연차 잠금 술어(ScheduleGuardMapper·Attd05Mapper·DefaultSchGenMapper·UserTransferMapper)가
        //   "REQ 에 시각이 있는가"를 "시각 민감 연차" 판정에 쓰므로, 반차만 빠지면 술어 의미와 데이터가 어긋난다.
        //   ★ Q5 정정: START_DATE = END_DATE = workYmd 고정(자정 넘김은 시각 wrap 으로만 표현).
        appLeaveFlowMapper.insertLeaveReq(new LeaveReqInsertCommand(
                reqId, cmpny, site, user, reqStatus, p.reason(), workYmd, reqNodeCd,
                workYmd, startTime, workYmd, endTime, p.leaveType(), leaveDays, user,
                // prafta-leavemulti: 기간신청 묶음 ID. 단일일 신청은 null → 컬럼 NULL(종전과 동일).
                p.groupId()));

        // 7) 결재 Y → 라인 일괄 생성. PRAFTA-003(2026-08-27, 결재선 필수화, 웹 LeaveFlowServiceImpl
        //    submitLeave 미러): "본인 + 자체근태승인 ON" 이어도 신청 즉시 자동확정하지 않는다. 본인
        //    지정 자격 게이트(ATTD_400_056)는 유지하되, 단계 생성은 self 여부 무관 항상 첫 단계
        //    APPLIED/나머지 WAIT 로 통일한다 — 명시적 승인 액션(2026-08-16 "관리자 본인결재 허용")이 필요.
        // PRAFTA-COM-004 시나리오 A hook 용: 차례가 도래한 첫 단계의 결재자/단계번호.
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

            for (int i = 0; i < approvers.size(); i++) {
                boolean isSelf = user.equals(approvers.get(i));
                if (isSelf && !selfAllowed) {
                    throw new ApiException(AttdErrorCode.ATTD_400_056);
                }
            }
            for (int i = 0; i < approvers.size(); i++) {
                String approver = approvers.get(i);
                ApprovalStepVO s = new ApprovalStepVO();
                s.setReqId(reqId);
                s.setApprovalStep(i + 1);
                s.setCmpnyCd(cmpny);
                s.setApproverUserCd(approver);
                s.setApprovalStatus(i == 0 ? STEP_APPLIED : STEP_WAIT);
                s.setInsertNo(user);
                approvalLineMapper.insertApprovalStep(s);
            }
            // 차례가 도래한 첫 단계(항상 index 0, self 포함) — 시나리오 A 발송 대상.
            turnApprover = approvers.get(0);
            turnStep = 1; // approvalStep 은 1-based
        }

        // 8) 차감 예약(CONFIRMED) + 부여 USED_DAYS 동기화(웹 202~212).
        //    비가불/'01'은 charges 가 1건([단일 grantId, leaveDays] 또는 [null, leaveDays])이라 기존과 동일 동작.
        //    가불(Q1=b)은 잔여+가불 여러 GRANT 로 분할 차감될 수 있어 charge 별로 leave_use 를 분할 INSERT 한다.
        //    '01'(사용자 신청)은 grantId=null → GRANT 가 없으므로 recomputeGrantUsedDays 생략(잔여=회계연도 사용분 파생).
        //    leaveMinutes 는 분할 시 의미가 모호하므로 첫 charge 에만 싣는다(가불은 종일 위주, 표시·집계는 일수 기준).
        String leaveId = null; // 무결재 즉시확정 통보(notifyLeaveUsedNoAprv)용 — 마지막 INSERT 한 사용기록 ID.
        if (remnantPlan != null) {
            // PC-05(D6): 짜투리 발동 — 잔여 전액을 대상 5종 부여로 분할 차감(use 행: 신청 REQ_ID·단위,
            //   LEAVE_CD 는 부여 귀속) + 회사 부담분 TB_LEAVE_REMNANT_COVER 기록 + 영향 GRANT 재집계(웹 미러).
            // BW-04(Q-2 확정): applyTrigger 시그니처를 확장해 brkWaiveYn·evidenceFileId 를 함께 전달한다
            //   (종전 증빙 파일 ID 유실 결함도 같은 커밋에서 해소 — 첫 행에만 저장, 휴게 무시는 전 행).
            //   v2(BW2-04): BRK_WAIVE_YN 은 파생값(waiveRequested), BRK_WAIVE_MIN 은 확정 분량을 함께 전달.
            leaveId = leaveRemnantCoverService.applyTrigger(cmpny, site, user, workYmd, unit,
                    startTime, endTime, leaveMinutes, p.reason(), reqId, remnantPlan, user,
                    brkWaiveYn, p.evidenceFileId(), brkWaiveMin);
        } else {
            boolean firstCharge = true;
            for (GrantCharge charge : charges) {
                leaveId = appLeaveFlowMapper.selectNextLeaveId(cmpny);
                LeaveUseCommand use = LeaveUseCommand.builder()
                        .leaveId(leaveId).cmpnyCd(cmpny).siteCd(site).userCd(user).leaveCd(leaveCd)
                        .reqId(reqId).grantId(charge.grantId())
                        .startDate(workYmd).startTime(startTime).endDate(workYmd).endTime(endTime)
                        .useUnitType(unit).leaveDays(charge.days()).leaveMinutes(firstCharge ? leaveMinutes : null)
                        .leaveReason(p.reason())
                        // 연차 신청 증빙 필수화(2026-08-29): 분할 차감 시 첫 charge 행에만 저장(leaveMinutes 관례 동일).
                        .evidenceFileId(firstCharge ? p.evidenceFileId() : null)
                        // BW-04: 휴게 무시 요청은 판정 속성이라 분할 차감 모든 행에 동일 값(REQ_DTIME 은 매퍼 NOW()).
                        //   v2(BW2-04): YN 은 파생값(waiveRequested), MIN 은 확정 분량 — 전 행 동일.
                        .brkWaiveYn(brkWaiveYn)
                        .brkWaiveMin(brkWaiveMin)
                        .leaveStatus(USE_CONFIRMED).insertNo(user)
                        .build();
                appLeaveFlowMapper.insertLeaveUse(use);
                if (charge.grantId() != null) {
                    appLeaveFlowMapper.recomputeGrantUsedDays(cmpny, charge.grantId(), user);
                }
                firstCharge = false;
            }
        }

        // 9) PRAFTA-003(2026-08-27): 결재 Y 는 이제 신청 즉시 확정되는 경로가 없다(본인 결재자
        //   포함 전 건 정상 결재 단계로 생성 — §7 참조). REQ_STATUS 확정은 웹 approveStep(명시적
        //   승인 API 호출)을 통해서만 이뤄진다(앱에는 연차 승인 경로 자체가 없음).
        //   prafta-com-008-E-2: 연차-스케줄 모델 전환 — work_plan 에 LEAVE_CD 를 더 이상 쓰지 않는다.
        //   출근 차단(§8.3)은 leave_use(CONFIRMED 종일) 존재로 판정하므로 위 insertLeaveUse 가 곧 차단 근거다.
        //   (work_plan 은 SCH_CD 유지 → 연차 취소 시 자동 근무일 복귀).

        // PRAFTA-COM-004 PUSH 적재 hook (예외 격리 — @Transactional 본 흐름에 예외 전파 금지).
        //  - 시나리오 A: 결재 Y 이면 항상 첫 단계 결재자(self 포함) 1인에게 차례 도래 통보.
        //  - 시나리오 B: 순수 무결재(!aprvRequired)만.
        //  앱에는 연차 승인 경로가 없으므로 "다음 단계" hook 은 없다(웹 approveStep 단일 경로).
        try {
            // prafta-leavemulti: 기간(From-To) 신청은 날짜별 REQ N건으로 분해되므로 묶음 ID 를 함께 넘겨
            //   알림을 1건으로 수렴시킨다. ★단일일 신청은 p.groupId()==null → 종전 동작과 완전히 동일.
            if (aprvRequired && turnApprover != null) {
                leaveApprovalNotiService.notifyApprovalTurn(cmpny, site, user, reqId, turnStep, turnApprover, user,
                        p.groupId());
            }
            if (!aprvRequired) {
                leaveApprovalNotiService.notifyLeaveUsedNoAprv(cmpny, site, user, leaveId, unit, leaveDays,
                        workYmd, startTime, endTime, user, p.groupId());
            }
        } catch (Exception e) {
            log.error("[leaveflow] 연차 신청 PUSH 적재 hook 실패(신청 영향 없음) reqId={}", reqId, e);
        }

        log.info("[leaveflow] 연차 신청 완료 reqId={}, userCd={}, leaveCd={}, unit={}, days={}, aprv={}",
                reqId, user, leaveCd, unit, leaveDays, aprvRequired);
    }

    /**
     * 연차개편: 당해 회계연도 윈도우 산출(단일출처 {@link FiscalYearUtils}).
     * 활성정책 AXIS2_FISCAL_START_MM/_DD 로 산출하며, 정책 미존재/NULL 이면 1월 1일 폴백.
     * (AXIS1_GRANT_BASE 가 HIRE_DATE 여도 본 폴백/정책 MM/DD 를 그대로 적용 — 사용자 신청 한도는 회사 회계연도 기준.)
     */
    private FiscalYearUtils.FiscalWindow resolveFiscalWindow(String cmpnyCd) {
        LeavePolicyVO policy = leavePolicyMapper.selectActivePolicy(cmpnyCd);
        String mm = (policy == null) ? null : policy.getAxis2FiscalStartMm();
        String dd = (policy == null) ? null : policy.getAxis2FiscalStartDd();
        return FiscalYearUtils.fiscalWindow(LocalDate.now(), mm, dd);
    }

    /** 연차개편 동시성: '01' 신청 직렬화 키(회사+사용자+연차코드). */
    private String leave01LockKey(String cmpnyCd, String userCd, String leaveCd) {
        return "leave01:" + cmpnyCd + ":" + userCd + ":" + leaveCd;
    }

    /** advisory lock 획득. 타임아웃/오류면 동시 처리로 보고 ATTD_400_051 로 변환(중복신청 차단). */
    private void acquireLeave01Lock(String lockKey) {
        Integer got = appLeaveFlowMapper.getAdvisoryLock(lockKey, LEAVE01_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leaveflow] '01' 신청 advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
    }

    /** advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseLeave01Lock(String lockKey) {
        try {
            appLeaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leaveflow] '01' 신청 advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /**
     * LC-04(F5): 시간차 사용자·일 단위 직렬화 lock 획득(웹 미러). 타임아웃/오류면 같은 날 동시
     * 처리로 보고 중복신청 계열(ATTD_400_111)로 변환 — leave01 lock 실패→400_051 변환 관례 미러.
     */
    private void acquireLeaveDayLock(String lockKey) {
        Integer got = appLeaveFlowMapper.getAdvisoryLock(lockKey, LEAVE01_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leaveflow] 시간차 leaveDay advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_111);
        }
    }

    /** LC-04: 시간차 leaveDay advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseLeaveDayLock(String lockKey) {
        try {
            appLeaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leaveflow] 시간차 leaveDay advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /**
     * PC-05(N9): 짜투리 발동 판정~기록 직렬화 lock 획득(웹 미러). 타임아웃/오류면 동시 신청으로
     * 보고 잔여 부족 계열(ATTD_400_051)로 변환 — leave01 lock 실패 변환 관례 미러.
     */
    private void acquireRemnantLock(String lockKey) {
        Integer got = appLeaveFlowMapper.getAdvisoryLock(lockKey, LEAVE01_LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leaveflow] 짜투리 leaveRemnant advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
    }

    /** PC-05: 짜투리 leaveRemnant advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseRemnantLock(String lockKey) {
        try {
            appLeaveFlowMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leaveflow] 짜투리 leaveRemnant advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    /**
     * 허용 사용단위 집합 산출(018-A {@link LeaveUnitGranularity} SSOT 재사용).
     * 법정 = 회사 USAGE_UNIT 계층(정책 미존재면 종일만 폴백) / 비법정 = 타입 USE_UNIT_TYPE 계층(NULL→00).
     * HB-04(D-8): 반반차 폐지 — 구 설정 'QUARTER_DAY'·'05' 는 반차로 축소 해석되어 '05' 는 반환되지 않는다.
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
     * prafta-com-011-2 가불 패밀리 판정. 시스템 법정 월차(SYS_MONTHLY)/본연차(SYS_ANNUAL)만 대상.
     * 그 외 leaveCd(타 법정/비법정)는 가불 비대상 → null(호출부에서 ATTD_400_180).
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
     * prafta-com-011-5: 가불분 만료(소멸)일 YYYYMMDD 산정(FE 표시 + 만료초과 alert 가드용, read-only).
     * <p>월차 = 입사 + 1년 − 1일(첫해 월차 일괄소멸일, 엔진 동일 규칙). 본연차 = 차기 부여 본연차 정상 만료일
     *   ({@code projectNextAnnualGrant().availToYmd}). 입사일 미존재/파싱 불가/산정 불가면 null(FE 가 만료 미표시).
     *   서버 fail-closed 검증({@code assertBorrowWorkYmdWithinExpiry})과 동일 산식이라 표시와 차단 기준이 일치한다.
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

    /**
     * BigDecimal → double (null=0.0). leave01 toScaledDouble 패턴과 정합.
     *
     * <p>apply-meta balanceDays(연차 원장 잔여 = decimal(8,5) 가감산)를 원장 정밀도로 전달한다.
     * 과거 소수 1자리 반올림(setScale(1))이 시간차 잔여를 왜곡해 앱 분 환산 오표기를 유발하던
     * 결함을 제거하고, 원장 스케일(5)로 손실 없이 넘긴다.</p>
     */
    private double toScaledDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        // 원장 최대 스케일(decimal(8,5)) 로 맞춤 — 반올림 없이 원값 그대로 전달.
        return value.setScale(LEDGER_DECIMAL_SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
