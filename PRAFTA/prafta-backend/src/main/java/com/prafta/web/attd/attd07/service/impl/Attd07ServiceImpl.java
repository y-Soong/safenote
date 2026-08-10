package com.prafta.web.attd.attd07.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.service.LeaveRefusalDetectService;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.cmm.push.ApprovalResultNotiService;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AttdOverlapMessages;
import com.prafta.common.util.AttdOverlapUtils;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.common.util.IntervalUtils;
import com.prafta.common.util.StringEqualsUtils;
import com.prafta.web.attd.attd07.application.command.DailyAttdDetailDeleteCommand;
import com.prafta.web.attd.attd07.application.command.InsertUserAttdHistsCommand;
import com.prafta.web.attd.attd07.application.command.InsertUserOvertimeCommand;
import com.prafta.web.attd.attd07.application.command.RejectUserAttdRequestCommand;
import com.prafta.web.attd.attd07.application.command.UpdateUserAttdInfosCommand;
import com.prafta.web.attd.attd07.application.command.UpdateUserAttdRequestCommand;
import com.prafta.web.attd.attd07.application.command.UpsertUserWorkPlanCommand;
import com.prafta.web.attd.attd07.application.model.OvertimeItemModel;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;
import com.prafta.web.attd.attd07.application.param.ApproveSchedModifyRequestParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
import com.prafta.web.attd.attd07.application.param.DeleteUserOvertimeParam;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;
import com.prafta.web.attd.attd07.application.param.RejectUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdInfosParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.application.query.DailyAttdDetailsQuery;
import com.prafta.web.attd.attd07.application.query.MonthlyAttdListQuery;
import com.prafta.web.attd.attd07.application.query.OvertimeAllowedWindowQuery;
import com.prafta.web.attd.attd07.dto.response.AttdRecordListResponse;
import com.prafta.web.attd.attd07.dto.response.DailyAttdDetailsResponse;
import com.prafta.web.attd.attd07.mapper.Attd07Mapper;
import com.prafta.web.attd.attd07.result.AllowedWindowResult;
import com.prafta.web.attd.attd07.result.AttdKeyFieldsResult;
import com.prafta.web.attd.attd07.result.AttdSnapshotResult;
import com.prafta.web.attd.attd07.result.ConfirmedLeaveResult;
import com.prafta.web.attd.attd07.result.LeaveExemptWindowResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DayAttdSegmentResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyLeaveChangeReqResult;
import com.prafta.web.attd.attd07.result.DailyOvertimeResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.OtLeaveExemptWindowView;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.result.MonthlyOvertimeResult;
import com.prafta.web.attd.attd07.result.NeighborAttdSegmentView;
import com.prafta.web.attd.attd07.result.UserAttdReqResult;
import com.prafta.web.attd.attd07.service.Attd07Service;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd07.util.AttdReqTypeUtils;
import com.prafta.web.attd.attd07.util.AttdScheduleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd07ServiceImpl implements Attd07Service {

    private final Attd07Mapper attd07Mapper;
    private final AttdCloseService attdCloseService;
    /** PRAFTA-APP-021-3a(W2): 근태/초과근무 보정 결재 결과(승인/반려) 통보 PUSH 생산자(신청자 1인, afterCommit 격리). */
    private final ApprovalResultNotiService approvalResultNotiService;
    /** PRAFTA-COM-008-B: 노무수령거부 차단 가드(공용 빈 재사용 — 관리자 직접 근태등록 ADMIN_ENTRY 진입 차단). */
    private final LeaveRefusalDetectService leaveRefusalDetectService;
    /** PRAFTA-COM-008-D: 교대 잠금 가드(공용 cmm 빈 — 스케줄수정 승인 시 교대 소속 구간 차단). */
    private final com.prafta.common.cmm.shift.service.ShiftMembershipService shiftMembershipService;
    /** 교차일(앞뒤 근무일) 근무 스케줄 시각 겹침 가드(공용 cmm 빈 — 야간 오버나이트 포함). */
    private final com.prafta.common.cmm.schedule.service.ScheduleOverlapGuardService scheduleOverlapGuardService;
    /** E3(당일분모 전환, W4): 연차 잠금일(확정 전 단위 + 미결 시간차) 스케줄수정 승인 하드 차단(공용 cmm 빈). */
    private final com.prafta.common.cmm.schedule.service.ScheduleChangeGuardService scheduleChangeGuardService;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final SiteAccessService siteAccessService;
    /** PC-07(N8): 일자상세 응답 convMinutes(대상 사용자·대상일 개인 분모) — AttdDayDetailPop 480 폴백 해소. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;

    /**
     * 출퇴근 방법(METHOD) 기본값. SYS031 '01'(사용자/앱). 근태 보정 승인 시 METHOD 미전달(앱 관리자 경로)일 때
     * CHECK_IN_METHOD(NOT NULL) 보정용 기본값으로 사용한다.
     */
    private static final String ATTD_METHOD_DEFAULT = "01";

    /**
     * PRAFTA-028 - 마감 가드. 대상 부서(nodeCd)+근무월이 마감 커버리지(전체/자기/상위 하위포함)에 들면
     * 데이터 변경을 차단한다(ATTD_400_042). 근무일자(workYmd, yyyyMMdd 또는 yyyyMM)에서 월을 추출한다.
     */
    private void ensureNotClosed(String cmpnyCd, String siteCd, String nodeCd, String workYmd) {
        String closeYm = (workYmd != null && workYmd.length() >= 6) ? workYmd.substring(0, 6) : workYmd;
        if (attdCloseService.isClosedForNode(cmpnyCd, siteCd, nodeCd, closeYm)) {
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }
    }

    /**
     * 근태/OT/스케줄 직접 승인·반려 자기처리 정책 게이트 (com-013-06-FU r28).
     *
     * <p>노드 {@code SELF_ATTD_APPRV_YN} 정책에 따라 처리자(approver)가 신청자(applicant)의
     * 요청을 직접 처리할 수 있는지 검증한다. 거부 시:
     * <ul>
     *   <li>자기처리(신청자==처리자) 또는 동일 노드 관리자 자기처리로 막힌 경우 →
     *       OT는 {@code ATTD_403_003}, 그 외(근태/스케줄)는 {@code ATTD_403_001}.</li>
     *   <li>그 외 권한 미달(타 노드 관리자가 자격 없이 시도 등) → {@code ATTD_403_002}.</li>
     * </ul>
     * 전사 역할(master/hr/safe)은 정책 메서드 내부에서 즉시 통과한다.
     *
     * @param isOvertime OT 경로면 자기처리 차단 코드를 403_003 으로(아니면 403_001).
     */
    private void ensureCanProcessAttdSelfPolicy(String authCd, String approverUserCd, String applicantUserCd,
                                                String cmpnyCd, String siteCd, String nodeCd, boolean isOvertime) {
        if (attdCloseService.canProcessAttdSelfPolicy(
                authCd, approverUserCd, applicantUserCd, cmpnyCd, siteCd, nodeCd)) {
            return;
        }
        // 자기처리(신청자==처리자) 여부로 에러코드를 분기한다.
        boolean selfProcess = applicantUserCd != null && applicantUserCd.equals(approverUserCd);
        if (selfProcess) {
            log.warn("근태 처리 거부 - 자기처리 차단. approverUserCd={}, applicantUserCd={}, nodeCd={}, isOvertime={}",
                    approverUserCd, applicantUserCd, nodeCd, isOvertime);
            throw new ApiException(isOvertime ? AttdErrorCode.ATTD_403_003 : AttdErrorCode.ATTD_403_001);
        }
        log.warn("근태 처리 거부 - 권한 미달. approverUserCd={}, applicantUserCd={}, authCd={}, nodeCd={}",
                approverUserCd, applicantUserCd, authCd, nodeCd);
        throw new ApiException(AttdErrorCode.ATTD_403_002);
    }

    @Override
    public AttdRecordListResponse getMonthlyAttdList(MonthlyAttdListParam param) {

        // 사업장 접근 인가(User_03 원장 기반) — 팝업 API 들과 대칭으로 목록 조회도 사업장 스코프를 강제한다.
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        List<MonthlyAttdListResult> attdRecordResultList = attd07Mapper.selectMonthlyAttdList(MonthlyAttdListQuery.from(param));
        List<MonthlyAttdReqSummaryResult> monthlyAttdReqSummaryResultList = attd07Mapper.selectMonthlyAttdReqSummary(MonthlyAttdListQuery.from(param));

        // PRAFTA-017 - 목록뷰에 함께 노출할 일자별 초과근무 목록(월 단위)을 조회한다.
        List<MonthlyOvertimeResult> monthlyOvertimeResultList = attd07Mapper.selectMonthlyOvertimeList(MonthlyAttdListQuery.from(param));

        // 연차 변경(이동/삭제) 활성 요청 요약 — 캘린더 셀 강조용(출발일·이동대상일 양쪽).
        List<MonthlyAttdReqSummaryResult> monthlyLeaveChangeSummaryResultList =
                attd07Mapper.selectMonthlyLeaveChangeReqSummary(MonthlyAttdListQuery.from(param));

        return AttdRecordListResponse.builder()
                .attdRecordResultList(attdRecordResultList)
                .monthlyAttdReqSummaryResultList(monthlyAttdReqSummaryResultList)
                .monthlyOvertimeResultList(monthlyOvertimeResultList)
                .monthlyLeaveChangeSummaryResultList(monthlyLeaveChangeSummaryResultList)
                .build();
    }

    @Override
    @Transactional
    public void updateUserAttdInfos(UpdateUserAttdInfosParam param) {
        List<UpdateUserAttdInfosModel> models = param.updateUserAttdInfosModelList();

        // 사업장 접근 인가 — 레코드별 siteCd 전수(중복 제거) 검증(User_03 원장 기반, 구 등식 가드 대체).
        //   siteCd 가 빈 레코드는 assert 가 403 으로 fail-closed 차단한다.
        models.stream().map(UpdateUserAttdInfosModel::siteCd).distinct().forEach(sc -> {
            UpdateUserAttdInfosModel first = models.get(0);
            siteAccessService.assertSiteAccess(first.gvCmpnyCd(), first.gvUserCd(), first.gvAuthCd(), first.gvSiteCd(), sc);
        });

        // 0. 근무 구간 시각 겹침 판정(정책서 attd §7.6) 준비:
        //    각 model 의 최종 ATTD_ID 를 먼저 확정(신규=채번)하고, 배치 내 attdId 집합/새 구간을 모은다.
        //    이렇게 해야 (a) DB 조회에서 배치에 포함된 attdId 를 모두 제외하고, (b) 배치 내 다른 model
        //    의 새 구간(아직 DB 에 없음)까지 합쳐 겹침을 검사할 수 있다. model.attdId() 가 null(신규)이면
        //    채번된 attdId 가 곧 제외 기준이 된다.
        String[] resolvedAttdIds = new String[models.size()];
        boolean[] isCreateFlags = new boolean[models.size()];
        for (int i = 0; i < models.size(); i++) {
            UpdateUserAttdInfosModel m = models.get(i);
            if (m.attdId() != null) {
                resolvedAttdIds[i] = m.attdId();
                isCreateFlags[i] = false;
            } else {
                resolvedAttdIds[i] = attd07Mapper.selectAttdId(m.gvCmpnyCd());
                isCreateFlags[i] = true;
            }
        }

        for (int i = 0; i < models.size(); i++) {
            UpdateUserAttdInfosModel model = models.get(i);

        	// ============================================================================
        	// [기존 행 대조 게이트] 관리자 직접 "수정"(model.attdId() 보유) 본문 키 필드 검증.
        	//   updateUserAttdInfos 매퍼는 upsert(ON DUPLICATE KEY UPDATE)라 WORK_YMD/WORK_SEQ/
        	//   SITE_CD/USER_CD 를 편집 시 갱신하지 않는다(불변 전제). 본문이 이 필드들에
        	//   기존 행과 다른 값을 보내면 조용한 병합이 일어나고, 마감(ensureNotClosed)·겹침(§7.6)
        	//   검사가 본문 값 기준으로 돌아 실제 저장 행과 어긋난다(마감 잠금 우회 여지).
        	//   승인 경로(updateUserAttdRequest)의 body/REQ 대조와 동일 원칙으로, 저장 전에
        	//   (cmpnyCd, attdId) 기존 행과 대조해 불일치를 차단한다. 후속 게이트(권한/마감/겹침)가
        	//   검증된 값 위에서 돌도록 루프 선두에 둔다. 신규 생성(attdId==null)은 대조 대상이
        	//   없으므로 기존 흐름 그대로다(다중 구간 동시 생성 무영향).
        	//   ★ NODE_CD 는 대조에서 제외한다 — 행의 NODE_CD 는 생성 시점 스냅샷(이후 무갱신)이고
        	//   본문 nodeCd 는 아래 게이트(2)가 "현재 소속 권위 노드"와 대조하므로, 부서 이동자의
        	//   과거 근태(구 노드 행) 편집 시 둘을 다 요구하면 어떤 본문 값도 통과 불가(데드락).
        	//   ON DUP 절이 NODE_CD 를 갱신하지 않아 제외해도 영속 위험 없음(qa 2026-08-10 판정).
        	// ============================================================================
        	if (model.attdId() != null) {
        		AttdKeyFieldsResult existingRow =
        				attd07Mapper.selectAttdKeyFieldsById(model.gvCmpnyCd(), model.attdId());
        		if (existingRow == null) {
        			// 수정 대상 부재(미존재/삭제됨) — 클라이언트 지정 attdId 를 신규 INSERT(또는 삭제 행
        			// 소생)로 흘려보내지 않고 명시적으로 거부한다(fail-closed).
        			log.warn("admin-direct attd rejected - 수정 대상 근태 행 부재. cmpnyCd={}, attdId={}, userCd={}, workYmd={}",
        					model.gvCmpnyCd(), model.attdId(), model.userCd(), model.workYmd());
        			throw new ApiException(AttdErrorCode.ATTD_404_012);
        		}
        		if (StringEqualsUtils.isMismatched(model.workYmd(), existingRow.workYmd())
        				|| StringEqualsUtils.isMismatched(model.workSeq(), existingRow.workSeq())
        				|| StringEqualsUtils.isMismatched(model.siteCd(), existingRow.siteCd())
        				|| StringEqualsUtils.isMismatched(model.userCd(), existingRow.userCd())) {
        			log.warn("admin-direct attd rejected - body/기존 행 mismatch(변조). attdId={},"
        					+ " bodyYmd={}, rowYmd={}, bodySeq={}, rowSeq={}, bodySite={}, rowSite={},"
        					+ " bodyUser={}, rowUser={}",
        					model.attdId(),
        					model.workYmd(), existingRow.workYmd(),
        					model.workSeq(), existingRow.workSeq(),
        					model.siteCd(), existingRow.siteCd(),
        					model.userCd(), existingRow.userCd());
        			throw new ApiException(AttdErrorCode.ATTD_400_005);
        		}
        	}

        	// ============================================================================
        	// [보안 하드닝] 관리자 직접 근태 생성/수정 인가 게이트 (형제 경로 updateUserOvertimeRequests 미러링).
        	//   기존엔 siteCd/userCd/nodeCd/attdId 를 전부 body 에서 받으면서 사업장/노드 관리 권한 게이트가
        	//   전무했다(IDOR). OT 직접등록과 동일한 5단 게이트를 per-model 로 적용한다.
        	//   역할 판정은 JWT 기반 gvAuthCd 를 쓰므로 body 위조로 권한 escalation 을 할 수 없다.
        	//   신규 에러코드 없음(전부 기존 재사용: 403_002/400_005/403_001/404_011).
        	//   기존 §7.6 겹침/OT 범위(400_114) 가드·METHOD 기본값 보정 등은 그대로 유지하고 순서만 게이트 뒤로 둔다.
        	// ============================================================================

        	// (1) 대상 사용자의 서버 권위 노드 확정. null/blank 면 소속 미상 → fail-closed 차단.
        	String authoritativeNodeCd =
        			attdCloseService.resolveUserNodeCd(model.gvCmpnyCd(), model.siteCd(), model.userCd());
        	if (authoritativeNodeCd == null || authoritativeNodeCd.isBlank()) {
        		log.warn("admin-direct attd rejected - 대상 사용자 소속 부서 미상(서버 노드 부재). cmpnyCd={}, siteCd={}, userCd={}",
        				model.gvCmpnyCd(), model.siteCd(), model.userCd());
        		throw new ApiException(AttdErrorCode.ATTD_403_002);
        	}

        	// (2) body nodeCd 가 서버 권위 노드와 불일치하면 변조로 간주하고 차단.
        	if (StringEqualsUtils.isMismatched(model.nodeCd(), authoritativeNodeCd)) {
        		log.warn("admin-direct attd rejected - body/서버 nodeCd mismatch(변조). cmpnyCd={}, siteCd={}, userCd={}, bodyNode={}, serverNode={}",
        				model.gvCmpnyCd(), model.siteCd(), model.userCd(), model.nodeCd(), authoritativeNodeCd);
        		throw new ApiException(AttdErrorCode.ATTD_400_005);
        	}

        	// (3) 자기처리/매니저 정책 게이트(isOvertime=false → 근태이므로 자기처리 차단 시 403_001).
        	//     전사 역할(master/hr/safe)은 게이트 내부에서 즉시 통과한다.
        	ensureCanProcessAttdSelfPolicy(model.gvAuthCd(), model.gvUserCd(), model.userCd(),
        			model.gvCmpnyCd(), model.siteCd(), authoritativeNodeCd, false);

        	// (5) 대상 사용자가 호출자의 회사/사이트 scope 안에 존재해야 한다.
        	int userExists = attd07Mapper.selectUserExistInCmpnySite(
        			model.gvCmpnyCd(), model.siteCd(), model.userCd());
        	if (userExists <= 0) {
        		log.warn("admin-direct attd rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
        				model.gvCmpnyCd(), model.siteCd(), model.userCd());
        		throw new ApiException(AttdErrorCode.ATTD_404_011);
        	}

        	// (4) PRAFTA-028 - 마감된 기간(부서)의 근태 직접 수정 차단. 게이트와 동일한 서버 권위 노드 사용.
        	ensureNotClosed(model.gvCmpnyCd(), model.siteCd(), authoritativeNodeCd, model.workYmd());

        	// PRAFTA-COM-008-B(B-3 1단계): 관리자 직접 근태등록 차단 가드(ADMIN_ENTRY).
        	//   촉진 확정 연차일에 관리자가 해당 근로자 근태(출근)를 직접 입력/생성하는 경로를 레코드 생성 이전에 차단한다.
        	//   대상이면 guard 내부에서 ATTD_400_150 차단 throw(BLOCKED 이력+PUSH 선커밋), 비대상이면 정상 진행.
        	//   userCd=대상 근로자, operatorUserCd=관리자(gvUserCd). 차단 대상은 근태기록 생성만(연차 관리는 문서 C 흐름).
        	leaveRefusalDetectService.guardAndRecord(
        			model.gvCmpnyCd(), model.siteCd(), model.userCd(), authoritativeNodeCd, model.workYmd(),
        			LeaveRefusalConst.ATTEMPT_ADMIN_ENTRY, model.gvUserCd());

        	// com-013 #5: attdId 유무로 관리자 직접 "생성"/"수정"을 구분한다(0단계에서 선확정).
        	//   - attdId 있음 → 기존 근태 수정(HIST_TYPE='12' 관리자 수정)
        	//   - attdId 없음 → 신규 채번 후 생성(HIST_TYPE='11' 관리자 생성)
        	//   ★요청 승인 경로(updateUserAttdRequest 등, HIST_TYPE='01')와 무관 — 본 경로(관리자 직접수정)만 분리.
        	String attdId = resolvedAttdIds[i];
        	boolean isCreate = isCreateFlags[i];

        	// 근무 구간 시각 겹침 금지(정책서 attd §7.6). MGMT 변경 이전에서:
        	//   - 이 model 의 새 구간(model.checkIn/Out, 퇴근 미입력=open 가능)
        	//   + 근무일 ±1 윈도우의 다른 구간(배치에 포함된 attdId 전체 제외, DEL_YN='N')  ← QT-2-6: 교차일 포함
        	//   + 배치 내 다른 model 의 새 구간(같은 회사/사업장/사용자/근무일 한정)
        	//   을 합쳐 겹침을 검사한다. 인접 경계는 허용.
        	{
        		List<AttdOverlapUtils.Segment> segs = new ArrayList<>();
        		AttdOverlapUtils.Segment selfSeg = buildSegment(model.workYmd(), model.workYmd(), attdId, null,
        				model.checkInDate(), model.checkInTime(), model.checkOutDate(), model.checkOutTime(), true);
        		if (selfSeg != null) {
        			warnIfNotJudgeable(selfSeg);
        			segs.add(selfSeg);

        			// (a) 근무일 ±1 윈도우의 다른 구간(배치 attdId 전체 제외) — DB 조회. 단건씩 제외할 수 없어
        			//     배치 attdId 를 모두 제외한 뒤 (b) 배치 내 새 구간으로 보완한다.
        			//     QT-2-6: 오버나이트(D 근무일, 퇴근 D+1) 와 이웃 근무일 근태의 실시각 겹침을 잡기 위해
        			//     조회 범위를 D-1 ~ D+1 로 넓힌다. 모든 구간은 model.workYmd() 기준 stamp 로 환산되어 비교된다.
        			//     겹침가드 개선(2026-08-06): 이웃날 open 은 그 근무일의 다음날 00:00 에서 종료(§7.6 D-2),
        			//     손상 구간(퇴근 ≤ 출근 등)은 판정 제외 후 경고 로그(D-1).
        			for (DayAttdSegmentResult ex : safe(attd07Mapper.selectAttdSegmentsAroundDayExcept(
        					model.gvCmpnyCd(), model.siteCd(), model.userCd(),
        					shiftYmd(model.workYmd(), -1), shiftYmd(model.workYmd(), 1), null))) {
        				if (containsAttdId(resolvedAttdIds, ex.attdId())) continue; // 배치 포함분은 (b)에서 새값으로 평가
        				AttdOverlapUtils.Segment s = buildSegment(model.workYmd(), ex.workYmd(), ex.attdId(), ex.workSeq(),
        						ex.checkInDate(), ex.checkInTime(), ex.checkOutDate(), ex.checkOutTime(), false);
        				if (s != null) {
        					warnIfNotJudgeable(s);
        					segs.add(s);
        				}
        			}

        			// (b) 배치 내 다른 model 의 새 구간(같은 회사/사업장/사용자/근무일 한정). 아직 저장 전이라 attdId 는 넘기지 않는다
        			//     (배치 내 구간임을 안내 문구에서 구분하기 위한 표식 — Segment.batchPending()).
        			for (int j = 0; j < models.size(); j++) {
        				if (j == i) continue;
        				UpdateUserAttdInfosModel other = models.get(j);
        				if (!sameScopeDay(model, other)) continue;
        				AttdOverlapUtils.Segment s = buildSegment(model.workYmd(), other.workYmd(), null, null,
        						other.checkInDate(), other.checkInTime(), other.checkOutDate(), other.checkOutTime(), false);
        				if (s != null) {
        					warnIfNotJudgeable(s);
        					segs.add(s);
        				}
        			}

        			AttdOverlapUtils.Conflict conflict = AttdOverlapUtils.findConflict(segs);
        			if (conflict != null) {
        				log.warn("admin-direct attd overlap rejected (§7.6). userCd={}, workYmd={}, attdId={}, otherWorkYmd={}, otherKind={}",
        						model.userCd(), model.workYmd(), attdId,
        						conflict.other().workYmd(), conflict.other().kind());
        				throw new ApiException(AttdErrorCode.ATTD_400_113,
        						AttdOverlapMessages.overlapMessage(model.workYmd(), conflict.other()));
        			}
        		}
        	}

        	// OT 범위 가드: 관리자 직접 "수정"(model.attdId() 존재)일 때, 새 실근무 구간이 그 근태에
        	//   연결된 모든 활성 OT 를 완전히 포함해야 한다. 줄여서 OT 가 범위 밖으로 삐져나오면 ATTD_400_114.
        	//   신규생성(model.attdId()==null, attdId=채번본)은 연결 OT 가 없어 스킵(동일 정합성 리스크 없음).
        	if (model.attdId() != null) {
        		ensureOvertimeWithinNewWindow(
        				model.gvCmpnyCd(), model.siteCd(), model.userCd(), attdId,
        				model.checkInDate(), model.checkInTime(), model.checkOutDate(), model.checkOutTime());
        	}

            attd07Mapper.updateUserAttdInfos(UpdateUserAttdInfosCommand.from(attdId, model));

            String histId = attd07Mapper.selectHistId(model.gvCmpnyCd());

            attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.forAdminDirect(histId, attdId, model, isCreate));
        }
    }

    /** 배치 attdId 배열에 주어진 attdId 가 포함되는지(null-safe). */
    private static boolean containsAttdId(String[] attdIds, String target) {
        if (target == null) return false;
        for (String id : attdIds) {
            if (target.equals(id)) return true;
        }
        return false;
    }

    /** 두 model 이 같은 (회사/사업장/사용자/근무일) 스코프인지 — 배치 내 겹침 검사 대상 판정. */
    private static boolean sameScopeDay(UpdateUserAttdInfosModel a, UpdateUserAttdInfosModel b) {
        return java.util.Objects.equals(a.gvCmpnyCd(), b.gvCmpnyCd())
                && java.util.Objects.equals(a.siteCd(), b.siteCd())
                && java.util.Objects.equals(a.userCd(), b.userCd())
                && java.util.Objects.equals(a.workYmd(), b.workYmd());
    }

    /**
     * 근무 구간 시각 겹침 금지(정책서 attd §7.6) 검사 — 편집 구간 새값이 근무일 ±1 윈도우의 다른 구간
     * (excludeAttdId 제외, DEL_YN='N')과 겹치면 ATTD_400_113. 출근 stamp 미확정 구간은 검사 제외,
     * 퇴근 미입력(open) 구간은 종료 +∞(SENTINEL)로 본다. 인접 경계는 허용.
     *
     * <p>QT-2-6: 종전에는 같은 근무일만 검사해 오버나이트 근태(WORK_YMD=D, 퇴근 D+1)와 이웃 근무일
     *   근태가 실시각으로 겹쳐도 통과했다(원장 이중 산입). 비교 기준선은 대상 근무일(workYmd)로 통일한다.
     *
     * <p>겹침가드 개선(2026-08-06): 이웃 근무일의 미마감(open) 구간은 그 근무일의 다음날 00:00 에서 종료하고
     *   (§7.6 D-2 — 이웃날 미마감 1건이 대상일 전체를 봉쇄하던 결함 해소), 손상 구간(퇴근 ≤ 출근 등)은
     *   판정에서 제외한다(D-1). 차단 시에는 원인(이웃날 미마감/당일 미마감/실제 겹침)을 날짜·차수·시각과 함께 안내한다.
     */
    private void ensureNoSegmentOverlap(String siteCd, String userCd, String workYmd, String excludeAttdId,
                                        String checkInDate, String checkInTime,
                                        String checkOutDate, String checkOutTime,
                                        String gvCmpnyCd) {
        AttdOverlapUtils.Segment newSeg = buildSegment(workYmd, workYmd, excludeAttdId, null,
                checkInDate, checkInTime, checkOutDate, checkOutTime, true);
        if (newSeg == null) {
            return; // 출근 stamp 미확정 → 검사 제외
        }
        List<AttdOverlapUtils.Segment> segs = new ArrayList<>();
        warnIfNotJudgeable(newSeg);
        segs.add(newSeg);
        for (DayAttdSegmentResult ex : safe(attd07Mapper.selectAttdSegmentsAroundDayExcept(
                gvCmpnyCd, siteCd, userCd, shiftYmd(workYmd, -1), shiftYmd(workYmd, 1), excludeAttdId))) {
            AttdOverlapUtils.Segment s = buildSegment(workYmd, ex.workYmd(), ex.attdId(), ex.workSeq(),
                    ex.checkInDate(), ex.checkInTime(), ex.checkOutDate(), ex.checkOutTime(), false);
            if (s != null) {
                warnIfNotJudgeable(s);
                segs.add(s);
            }
        }
        AttdOverlapUtils.Conflict conflict = AttdOverlapUtils.findConflict(segs);
        if (conflict != null) {
            log.warn("attd request overlap rejected (§7.6). userCd={}, workYmd={}, excludeAttdId={}, otherWorkYmd={}, otherKind={}",
                    userCd, workYmd, excludeAttdId, conflict.other().workYmd(), conflict.other().kind());
            throw new ApiException(AttdErrorCode.ATTD_400_113,
                    AttdOverlapMessages.overlapMessage(workYmd, conflict.other()));
        }
    }

    /**
     * 겹침 검사 윈도우 경계 산출 — workYmd 에서 {@code days} 일 이동한 yyyyMMdd.
     * 형식 오류 등으로 산출 불가하면 원본을 그대로 돌려준다(윈도우가 그날로 축소 = 종전 동작, fail-safe).
     */
    private static String shiftYmd(String workYmd, int days) {
        String shifted = DateTimeUtils.plusDays(workYmd, days);
        return (shifted == null) ? workYmd : shifted;
    }

    /**
     * (출근/퇴근 일자·시각) 한 행을 대상 근무일(baseWorkYmd) 기준 겹침 판정 구간으로 빌드한다.
     * 출근 stamp 미확정이면 null(검사 제외).
     *
     * <p>QT-2-6: 이웃 근무일(D±1) 행도 "대상 근무일(baseWorkYmd)" 을 기준선으로 stamp 화하므로
     *   (dayOffset 이 -1/+1 로 반영되어) 같은 축에서 비교된다.
     *
     * <p>겹침가드 개선(2026-08-06): 행이 속한 근무일(segWorkYmd)을 함께 넘겨야 이웃날 open clamp(§7.6 D-2)와
     *   원인별 안내 문구("몇 월 며칠 몇 차 근무")가 성립한다. segWorkYmd 는 <b>행의 WORK_YMD</b> 이며
     *   CHECK_IN_DATE 가 아니다(오버나이트 행은 출근일이 D 여도 WORK_YMD 가 D 다).
     */
    private static AttdOverlapUtils.Segment buildSegment(String baseWorkYmd, String segWorkYmd,
                                                         String attdId, String workSeq,
                                                         String inDate, String inTime,
                                                         String outDate, String outTime,
                                                         boolean self) {
        return AttdOverlapUtils.buildSegment(baseWorkYmd, segWorkYmd, attdId, workSeq,
                inDate, inTime, outDate, outTime, self);
    }

    /**
     * 겹침 판정에서 제외된 구간(손상 행 = 퇴근 ≤ 출근 / 퇴근일자 결측, 또는 이웃날 open 이 이미 근무일 경계를 넘긴 행)을
     * 운영 탐지용 경고 로그로 남긴다(사용자 노출 아님 — 화면 표시는 승인·상세 응답의 status 로 처리).
     */
    private static void warnIfNotJudgeable(AttdOverlapUtils.Segment seg) {
        if (seg.judgeable()) {
            return;
        }
        // 제외 사유를 구분해 로깅한다(운영 조사 시 오진 방지).
        //   CORRUPT      = 퇴근 ≤ 출근(길이 0/역전)
        //   그 외(OPEN)  = 이웃날 open 이 자기 근무일 경계를 이미 넘겨 clamp 결과 폭이 0 이하
        String reason = (seg.kind() == AttdOverlapUtils.SegmentKind.CORRUPT)
                ? "퇴근시각 미성립(퇴근 ≤ 출근)"
                : "이웃날 open 이 근무일 경계를 초과";
        log.warn("[겹침가드] 근태 행 판정 제외({}). workYmd={}, workSeq={}, attdId={}, kind={}",
                reason, seg.workYmd(), seg.workSeq(), seg.attdId(), seg.kind());
    }

    /** null-safe 리스트 보정(빈 리스트 반환). */
    private static <T> List<T> safe(List<T> list) {
        return (list == null) ? java.util.Collections.emptyList() : list;
    }

    /**
     * OT 범위 가드 — 근태 보정/직접수정으로 정해질 새 실근무 구간 [새 출근, 새 퇴근] 이 그 근태(attdId)에
     * 연결된 모든 활성 OT 를 완전히 포함하지 못하면(하나라도 범위 밖) ATTD_400_114 로 차단한다.
     * 시각 비교는 DB 쿼리에서 CONCAT 12자리(yyyyMMddHHmm) 문자열로 수행한다(OT 테이블 기존 비교 방식과 일치, 오버나이트 정확).
     * 새 퇴근 시각이 결측이면 open 으로 보아(쿼리 newEnd null 분기) 활성 OT 존재 자체가 위반이 된다.
     * 새 출근 시각은 스키마상 NOT NULL + 슬롯 검증으로 항상 존재하므로 결측 분기는 실무상 도달하지 않는다(도달 시 concatStamp 가 null → 시작측 비교만 생략, 퇴근측·미완료 검사는 유효).
     */
    private void ensureOvertimeWithinNewWindow(String cmpnyCd, String siteCd, String userCd, String attdId,
                                               String checkInDate, String checkInTime,
                                               String checkOutDate, String checkOutTime) {
        if (attdId == null || attdId.isEmpty()) {
            return; // 대상 근태 미확정 → 검사 불가(이론상 발생 안 함)
        }
        String newStart = concatStamp(checkInDate, checkInTime);   // 새 출근(yyyyMMddHHmm), 결측이면 null
        String newEnd = concatStamp(checkOutDate, checkOutTime);   // 새 퇴근, 결측이면 null(open)
        int otOutside = attd07Mapper.countActiveOvertimeOutsideAttdWindow(
                cmpnyCd, siteCd, userCd, attdId, newStart, newEnd);
        if (otOutside > 0) {
            log.warn("[OT-범위가드] 근태 수정 거부: 등록된 초과근무가 새 실근무 범위를 벗어남. userCd={}, attdId={}, newStart={}, newEnd={}",
                    userCd, attdId, newStart, newEnd);
            throw new ApiException(AttdErrorCode.ATTD_400_114);
        }
    }

    /**
     * 날짜(YYYYMMDD, 8) + 시각(HHmm, 4) → 12자리 시계열 비교 문자열(yyyyMMddHHmm).
     * 둘 중 하나라도 결측/형식 미달이면 null(=결정 불가 → 퇴근의 경우 open 취급).
     */
    private static String concatStamp(String date, String time) {
        if (date == null || date.length() != 8) return null;
        if (time == null || time.length() != 4) return null;
        return date + time;
    }

    @Override
    public DailyAttdDetailsResponse getDailyAttdDetails(DailyAttdDetailsParam param) {

        // SEC-019 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // SEC-019 - 매니저 전용 게이트.
        // AttdDayDetailPop 은 정책서 §14.1의 관리자 화면(근태 현황 조회)에서 호출되는 일자 상세 팝업이다.
        // 일반 작업자가 본 endpoint로 타인의 출퇴근/OT/PII(userNm/userId)에 접근하지 못하도록
        // JWT 기반 gvAuthCd를 사용해 게이트한다. body 위조로 권한 escalation을 할 수 없다.
        // PRAFTA-028 - master/hr 또는 해당 부서(상위 포함) 정·부 관리자만 일자 상세를 조회할 수 있다.
        //   (조회는 마감 여부와 무관하게 허용 — 마감돼도 팝업은 열려야 함)
        if (!attdCloseService.canManageNode(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("daily-attd-details rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // SEC-019 - cross-user IDOR 재검증.
        // 사업장 인가는 위 assertSiteAccess 로 완료.
        // 여기서는 대상 사용자가 호출자의 회사/사이트 scope 안에 실재하는지 DB 차원에서 다시 확인한다
        // (UpdateUserOvertimeRequestParam SEC-017 과 동일한 mapper 재사용).
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("daily-attd-details rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        DailyAttdDetailsResult dailyAttdDetailsResult = attd07Mapper.selectDailyAttdDetails(DailyAttdDetailsQuery.from(param));

        List<DailyAttdDetailHistoryResult> dailyAttdDetailHistoryResultList = attd07Mapper.selectDailyAttdDetailHistory(DailyAttdDetailsQuery.from(param));

        // PRAFTA: 연차(05/06) 승인/반려 처리 이력을 같은 '처리 이력' 목록에 병합한다(처리일시 최신순).
        //   근태/OT 이력(TB_USER_ATTD_HIST)에는 연차 결재 기록이 남지 않으므로 결재라인에서 별도 조회한다.
        List<DailyAttdDetailHistoryResult> leaveApprovalHistory = attd07Mapper.selectDailyLeaveApprovalHistory(DailyAttdDetailsQuery.from(param));

        // PRAFTA-APP-007-WEB-6: 스케줄 수정(10) 승인/반려 처리 이력도 같은 타임라인에 병합한다.
        //   결재라인/HIST 어디에도 안 잡히므로 처리된 REQ 행(REQ_STATUS 02/03)을 직접 소스로 추가한다.
        List<DailyAttdDetailHistoryResult> schedModifyHistory = attd07Mapper.selectDailySchedModifyHistory(DailyAttdDetailsQuery.from(param));

        boolean hasLeave = leaveApprovalHistory != null && !leaveApprovalHistory.isEmpty();
        boolean hasSched = schedModifyHistory != null && !schedModifyHistory.isEmpty();
        if (hasLeave || hasSched) {
            List<DailyAttdDetailHistoryResult> merged = new ArrayList<>(dailyAttdDetailHistoryResultList);
            if (hasLeave) merged.addAll(leaveApprovalHistory);
            if (hasSched) merged.addAll(schedModifyHistory);
            // insertDate 는 모든 소스가 원본 datetime 문자열("YYYY-MM-DD HH:mm:ss")이라 문자열 내림차순 = 최신순.
            //   (근태 HIST.INSERT_DATE / 연차 AP.APPROVAL_DATE / 스케줄 REQ.PROCESS_DATE 모두 datetime — 동일 포맷.)
            merged.sort((a, b) -> {
                String ad = a.insertDate() == null ? "" : a.insertDate();
                String bd = b.insertDate() == null ? "" : b.insertDate();
                return bd.compareTo(ad);
            });
            dailyAttdDetailHistoryResultList = merged;
        }

        List<MonthlyAttdReqResult> monthlyAttdReqResultList = attd07Mapper.selectMonthlyAttdReq(DailyAttdDetailsQuery.from(param));

        // PRAFTA-003-6: 해당 일자에 등록된 초과근무 목록을 함께 조회한다.
        List<DailyOvertimeResult> dailyOvertimeResultList = attd07Mapper.selectDailyOvertimeList(DailyAttdDetailsQuery.from(param));

        // PRAFTA-APP-018-F: 그날 확정 연차 사용내역(자동확정/직접 포함, 미처리 결재대기는 제외 — D 카드 소유).
        //   진입부 2단 권한 가드(canManageNode + selectUserExistInCmpnySite) 통과 후 호출되므로
        //   추가 권한 코드 불필요. 쿼리 WHERE 의 CMPNY/SITE/USER 스코프로 cross-site IDOR 이중 차단.
        List<ConfirmedLeaveResult> confirmedLeaveResultList = attd07Mapper.selectDailyConfirmedLeave(DailyAttdDetailsQuery.from(param));

        // 그날 걸려 있는 연차 변경(이동/삭제) 활성 요청 — 출발일·이동대상일 양쪽에서 매칭.
        //   확인/반려는 attd13 기존 EP 재사용(권한·마감·만료·충돌 재검증이 그쪽에 있음).
        //   조회 권한 근거는 위 confirmedLeaveResultList 와 동일(진입부 2단 가드 승계 + 쿼리 스코프).
        List<DailyLeaveChangeReqResult> leaveChangeReqResultList = attd07Mapper.selectDailyLeaveChangeReq(DailyAttdDetailsQuery.from(param));

        // E4(Q-2 확정, 2026-08-04): 일자 특정 화면이므로 convMinutes = "당일 분모 우선"(E1 이후 실차감
        //   분모 = 당일 배정 스케줄 — 그날 실제 차감값과 표기 정합). 산출 불가 시 폴백 체인 =
        //   참고치(개인 기본 근무타입, E4 규약 — 편차 허용, 사용자 확정 2026-08-03) → 480.
        Integer dailyConv = leaveConversionPolicyService.resolveDailyConvMinutes(
                param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        Integer personalConv = (dailyConv != null)
                ? dailyConv
                : leaveConversionPolicyService.resolvePersonalConvMinutes(
                        param.gvCmpnyCd(), param.userCd(), param.workYmd());
        int convMinutes = (personalConv != null)
                ? personalConv : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;

        // 겹침가드 개선(2026-08-06): 앞뒤 근무일(D-1/D+1) 근태 구간 — 겹침 가드가 이웃날 미마감 때문에 발동할 때
        //   관리자가 화면에서 원인 행을 특정할 수 있게 한다. 겹침 판정이 이미 쓰는 쿼리를 그대로 재사용(신규 쿼리 0건)하고
        //   당일 구간은 서비스에서 제외한다(time-card 가 이미 표시). 조회 권한 근거는 위 confirmedLeaveResultList 와 동일
        //   (진입부 2단 가드 승계 + 쿼리 스코프). 신규 노출 PII 없음(근태 시각만).
        List<NeighborAttdSegmentView> neighborAttdSegmentList =
                buildNeighborSegments(param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());

        // OT 칩 정합(2026-08-08): "등록 가능" 칩이 FE 자체 계산(실근태−스케줄)이라 연차 면제를 반영하지
        //   못해, 반차일 2차 재출근 구간에서 칩(전량)과 서버 검증(ATTD_400_012 거부)이 불일치했다
        //   (무인테스트 실측). 검증과 동일한 단일 출처(buildLeaveExemptSegments → PartialLeaveWindowUtils)
        //   로 산출한 면제 구간을 additive 로 내려 FE 가 계산 없이 그대로 뺀다. 구 FE 는 필드 무시(무영향).
        List<OtLeaveExemptWindowView> otLeaveExemptWindowList = buildOtLeaveExemptWindows(param);

        return DailyAttdDetailsResponse.builder()
                .dailyAttdDetailsResult(dailyAttdDetailsResult)
                .dailyAttdDetailHistoryResultList(dailyAttdDetailHistoryResultList)
                .monthlyAttdReqResultList(monthlyAttdReqResultList)
                .dailyOvertimeResultList(dailyOvertimeResultList)
                .confirmedLeaveResultList(confirmedLeaveResultList)
                .leaveChangeReqResultList(leaveChangeReqResultList)
                .convMinutes(convMinutes)
                .neighborAttdSegmentList(neighborAttdSegmentList)
                .otLeaveExemptWindowList(otLeaveExemptWindowList)
                .build();
    }

    /**
     * 앞뒤 근무일(D-1 / D+1) 근태 구간 뷰 목록을 만든다(당일 구간 제외).
     *
     * <p>겹침 판정용 쿼리({@code selectAttdSegmentsAroundDayExcept}, excludeAttdId=null)를 그대로 재사용한다.
     *   상태(status)는 각 행 자기 근무일 기준으로 판정하며, 표시 문자열은 서버가 완성한다.
     *   손상 행(CORRUPT)이 포함되면 운영 탐지용 경고 로그를 1회 남긴다.
     */
    private List<NeighborAttdSegmentView> buildNeighborSegments(String cmpnyCd, String siteCd,
                                                                String userCd, String workYmd) {
        List<NeighborAttdSegmentView> views = new ArrayList<>();
        boolean corruptFound = false;
        for (DayAttdSegmentResult row : safe(attd07Mapper.selectAttdSegmentsAroundDayExcept(
                cmpnyCd, siteCd, userCd, shiftYmd(workYmd, -1), shiftYmd(workYmd, 1), null))) {
            if (row.workYmd() == null || row.workYmd().equals(workYmd)) {
                continue; // 당일 구간은 time-card 가 이미 표시
            }
            AttdOverlapUtils.SegmentKind kind = AttdOverlapUtils.classify(
                    row.workYmd(), row.checkInDate(), row.checkInTime(), row.checkOutDate(), row.checkOutTime());
            if (kind == null) {
                kind = AttdOverlapUtils.SegmentKind.CORRUPT; // 출근 stamp 산출 불가(스키마상 NOT NULL — 이론상 미도달)
            }
            if (kind == AttdOverlapUtils.SegmentKind.CORRUPT) {
                corruptFound = true;
            }
            views.add(new NeighborAttdSegmentView(
                    row.workYmd(),
                    AttdOverlapMessages.dayLabel(workYmd, row.workYmd()),
                    row.workSeq(),
                    AttdOverlapMessages.seqLabel(row.workSeq()),
                    AttdOverlapMessages.stampText(row.workYmd(), row.checkInDate(), row.checkInTime()),
                    AttdOverlapMessages.checkOutTextFor(kind, row.workYmd(), row.checkOutDate(), row.checkOutTime()),
                    kind.name()));
        }
        if (corruptFound) {
            log.warn("[겹침가드] 앞뒤 근무일 구간에 퇴근시각 미성립(CORRUPT) 행 포함. siteCd={}, workYmd={}", siteCd, workYmd);
        }
        return views;
    }

    @Override
    @Transactional
    public void dailyAttdDetailDelete(DailyAttdDetailDeleteParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // [보안 재작업] SEC-015 - 매니저 전용 게이트. PRAFTA-016 이후 본 endpoint 는
        // 근태 + 연결 OT 를 연쇄 soft-delete 하므로 일반 작업자가 호출해선 안 된다.
        // 역할 검사는 JWT 기반 gvAuthCd 를 사용하므로 body 위조로 권한 escalation 을
        // 할 수 없다 (rejectUserAttdRequest 와 동일 패턴).
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("daily-attd-detail-delete rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // [보안 재작업] SEC-017 - 대상 사용자가 호출자의 회사/사이트 scope 안에
        // 실재하는지 DB 로 재확인한다 (rejectUserAttdRequest 와 동일 패턴).
        // body siteCd ↔ JWT gv_siteCd 일치는 Param.from 에서 이미 검증 완료.
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("daily-attd-detail-delete rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // PRAFTA-028 - 마감된 기간(부서)의 근태 삭제 차단 (ATTD_ID 의 근무월 기준)
        String delWorkYmd = attd07Mapper.selectAttdWorkYmd(param.gvCmpnyCd(), param.attdId());
        if (delWorkYmd != null) {
            String delYm = delWorkYmd.length() >= 6 ? delWorkYmd.substring(0, 6) : delWorkYmd;
            if (attdCloseService.isClosedForUser(param.gvCmpnyCd(), param.siteCd(), param.userCd(), delYm)) {
                throw new ApiException(AttdErrorCode.ATTD_400_042);
            }
        }

        DailyAttdDetailDeleteCommand command = DailyAttdDetailDeleteCommand.from(param);

        attd07Mapper.insertDailyAttdDetailDeleteHist(command);
        attd07Mapper.dailyAttdDetailDelete(command);

        // PRAFTA-016 - 근태 전체삭제 시 동일 ATTD_ID 를 가리키는 활성 OT 를 연쇄 soft-delete 한다.
        // REQ 상태는 변경하지 않는다. 동일 트랜잭션 내에서 처리한다.
        int cascadedOtCount = attd07Mapper.deleteOvertimeByAttdId(command);
        log.info("근태 전체삭제 OT 연쇄 삭제 완료. attdId={}, 삭제된 OT 건수={}",
                command.attdId(), cascadedOtCount);
    }

    @Override
    @Transactional
    public void updateUserAttdRequest(UpdateUserAttdRequestParam param) {
        // 1. 회사 scope으로 권위 있는 REQ row를 로드한다. 없으면 거부한다.
        UserAttdReqResult reqRow = attd07Mapper.selectUserAttdReqByReqId(param.reqId(), param.gvCmpnyCd());
        if (reqRow == null) {
            log.warn("approve rejected - REQ not found. reqId={}, cmpnyCd={}", param.reqId(), param.gvCmpnyCd());
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반) — 반려 경로(rejectUserAttdRequest)와 대칭.
        //   body 가 아닌 REQ 권위값(reqRow.siteCd)을 기준으로 판정한다.
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), reqRow.siteCd());

        // SEC-018: REQ_TYPE 가드.
        // 본 endpoint는 근태 수정 요청만 처리한다.
        // OT(초과근무)와 LEAVE(연차/병가) 요청은 각자 전용 승인 endpoint로 처리해야 한다.
        // 그렇지 않으면 여기서의 REQ_STATUS 전이가 비근태 요청을 근태 수정처럼
        // 무성하게 승인하는 결과를 낳는다 (타입 혼동).
        if (!AttdReqTypeUtils.isAttendanceReqType(reqRow.reqType())) {
            log.warn("approve rejected - wrong REQ_TYPE for attendance endpoint. reqId={}, reqType={}",
                    reqRow.reqId(), reqRow.reqType());
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // [보안 재작업] SEC-015 + com-013-06-FU(r28) - 매니저 전용 게이트 + 자기처리/상위결재 정책.
        // 근태 요청 승인은 일반 작업자가 호출해선 안 되고, 노드 SELF_ATTD_APPRV_YN='N' 정책에서
        // 노드 관리자 본인의 자기 승인을 차단하고 부모 1단계 노드 관리자에게만 승인을 허용한다.
        // 역할 검사는 JWT 기반 gvAuthCd 를 사용하므로 body 위조로 권한 escalation 을 할 수 없다.
        //   [보안 재작업 com-013-06-FU] 권한 판정 노드/사업장은 클라 body 가 아닌 REQ 의 권위값(reqRow)을 사용한다
        //   (스케줄/OT 반려 경로와 일관화 — body↔REQ mismatch 검사 이전이라도 게이트가 권위값으로 판정).
        //   신청자=reqRow.userCd, 처리자=param.gvUserCd, 노드=reqRow.nodeCd, 사업장=reqRow.siteCd.
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), reqRow.userCd(),
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), false);
        // PRAFTA-028 - 마감된 기간(부서)의 근태 요청 승인 차단 (REQ 권위 부서 기준)
        ensureNotClosed(param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), param.workYmd());

        // 2. 대기(pending) 상태의 요청만 승인 가능 (defence-in-depth - UPDATE 측에서도 REQ_STATUS='01'(신청)로 필터함).
        if (!AttdReqTypeUtils.REQ_STATUS_REQUESTED.equals(reqRow.reqStatus())) {
            log.warn("approve rejected - REQ already processed. reqId={}, status={}",
                    reqRow.reqId(), reqRow.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 3. 자기 승인 차단은 위 ensureCanProcessAttdSelfPolicy 로 일원화(노드 SELF_ATTD_APPRV_YN 정책).

        // 4. 요청 본문이 보안 민감 필드에 한해 저장된 REQ row와 일치하는지 검증한다.
        //    하나라도 불일치하면 변조(tampering)로 간주한다.
        if (StringEqualsUtils.isMismatched(param.userCd(),  reqRow.userCd())
            || StringEqualsUtils.isMismatched(param.siteCd(),  reqRow.siteCd())
            || StringEqualsUtils.isMismatched(param.workYmd(), reqRow.workYmd())
            || StringEqualsUtils.isMismatched(param.workSeq(), reqRow.workSeq())
            || StringEqualsUtils.isMismatched(param.nodeCd(),  reqRow.nodeCd())) {
            log.warn("approve rejected - body/REQ mismatch. reqId={}, paramUser={}, reqUser={},"
                    + " paramSite={}, reqSite={}, paramYmd={}, reqYmd={}, paramSeq={}, reqSeq={},"
                    + " paramNode={}, reqNode={}",
                    reqRow.reqId(),
                    param.userCd(),  reqRow.userCd(),
                    param.siteCd(),  reqRow.siteCd(),
                    param.workYmd(), reqRow.workYmd(),
                    param.workSeq(), reqRow.workSeq(),
                    param.nodeCd(),  reqRow.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 5. TARGET_ID(이 요청이 귀결될 ATTD_ID)를 결정한다.
        //    [PRAFTA-010-1-022] TARGET_ID 의미 재해석:
        //      TARGET_ID 는 "이 요청이 최종적으로 귀결될 TB_USER_ATTD_MGMT.ATTD_ID" 이다.
        //        - 수정요청(REQ_TYPE='02', 근태수정): 기존 근태의 ATTD_ID 가 들어온다.
        //        - 생성요청(REQ_TYPE='01', 근태생성): 사용자측 요청 시점에 사전 채번된 ATTD_ID 가 들어온다.
        //          (요청 요청서 prafta-010.md §2.1.1 - 사용자가 생성요청 시 ATTD_ID 를 먼저
        //           채번해 REQ.TARGET_ID 에 넣고, 관리자 승인 시 그 값을 MGMT 에 INSERT 한다.)
        //      따라서 REQ.TARGET_ID 가 비어있지 않으면 생성/수정 구분 없이 그대로 사용하면
        //      의도대로 동작한다. 승인 로직 코드 수정은 불필요하다.
        //    우선순위: REQ.TARGET_ID -> 기존 MGMT row -> 신규 시퀀스 값.
        //      아래 else 분기(selectExistingAttdId / selectAttdId)는 TARGET_ID 가 채워진
        //      정상 데이터에서는 도달하지 않는 레거시 방어 경로이다.
        String targetId;
        if (reqRow.targetId() != null && !reqRow.targetId().isEmpty()) {
            targetId = reqRow.targetId();
        } else {
            String existingAttdId = attd07Mapper.selectExistingAttdId(
                    param.gvCmpnyCd(),
                    reqRow.siteCd(),
                    reqRow.userCd(),
                    reqRow.workYmd(),
                    reqRow.workSeq());
            if (existingAttdId != null && !existingAttdId.isEmpty()) {
                targetId = existingAttdId;
            } else {
                targetId = attd07Mapper.selectAttdId(param.gvCmpnyCd());
            }
        }

        // 6. 처리 이력 "변경 전(BEF_*)" 은 클라이언트 입력(param.oriCheckIn*)을 신뢰하지 않고
        //    서버 권위 데이터로 채운다(감사 무결성). MERGE(7.) 직전 시점의 MGMT 현재값을 읽는다.
        //      - 수정요청(REQ_TYPE='02'): targetId 가 가리키는 기존 행의 현재 출퇴근값.
        //      - 생성요청(REQ_TYPE='01'): 사전 채번 ATTD_ID 가 아직 MGMT 에 없으므로 결과 없음 → BEF NULL(정상).
        //    이전에는 프론트가 보낸 oriAct{n}* 에 의존하여, 멀티 구간(2구간) 승인 시 그 값이 비면
        //    "변경 전" 이 누락되는 문제가 있었다. 서버 조회로 구간 무관하게 정확히 기록한다.
        AttdSnapshotResult beforeSnapshot = attd07Mapper.selectAttdSnapshotById(param.gvCmpnyCd(), targetId);
        String befCheckInDate  = beforeSnapshot != null ? beforeSnapshot.checkInDate()  : null;
        String befCheckInTime  = beforeSnapshot != null ? beforeSnapshot.checkInTime()  : null;
        String befCheckOutDate = beforeSnapshot != null ? beforeSnapshot.checkOutDate() : null;
        String befCheckOutTime = beforeSnapshot != null ? beforeSnapshot.checkOutTime() : null;

        // 6-1. 출퇴근 방법(METHOD) 기본값 보정.
        //   앱 관리자 승인 경로(AppAdminApprovalServiceImpl.doApproveAsis)는 REQ 에 METHOD 가
        //   보관되지 않아 checkInMethod/checkOutMethod 를 null 로 전달한다. 생성요청('01') 승인은
        //   신규 INSERT 경로라 CHECK_IN_METHOD(NOT NULL) 에 null 이 들어가 제약 위반이 발생한다.
        //   (웹 경로는 클라가 METHOD 를 채워 보내 비대칭으로 앱만 깨졌다.)
        //   → 출근 METHOD 는 항상 기본값(SYS031 '01')으로 보정(NOT NULL), 퇴근 METHOD 는
        //     퇴근시각이 있을 때만 기본값, 미퇴근(open)이면 null 유지(CHECK_OUT_METHOD 는 nullable).
        boolean hasCheckInMethod = param.checkInMethod() != null && !param.checkInMethod().isEmpty();
        boolean hasCheckOutMethod = param.checkOutMethod() != null && !param.checkOutMethod().isEmpty();
        boolean hasCheckOutTime = param.checkOutTime() != null && !param.checkOutTime().isEmpty();
        String resolvedCheckInMethod = hasCheckInMethod ? param.checkInMethod() : ATTD_METHOD_DEFAULT;
        String resolvedCheckOutMethod = hasCheckOutMethod
                ? param.checkOutMethod()
                : (hasCheckOutTime ? ATTD_METHOD_DEFAULT : null);

        // 7. 서버 권위 키 필드를 사용해 merge/hist 모델을 빌드한다.
        //    body로 전달된 출퇴근 값은 작업자가 기록 요청한 값이므로 그대로 사용한다.
        UpdateUserAttdInfosModel model = new UpdateUserAttdInfosModel(
            targetId
            , reqRow.siteCd()
            , reqRow.nodeCd()
            , reqRow.userCd()
            , null                       // userId는 mapper에서 미사용
            , reqRow.workSeq()
            , reqRow.workYmd()

            , befCheckInDate
            , befCheckInTime
            , befCheckOutDate
            , befCheckOutTime

            , param.checkInDate()
            , param.checkInTime()
            , resolvedCheckInMethod
            , param.checkOutDate()
            , param.checkOutTime()
            , resolvedCheckOutMethod
            , param.processComment()     // TB_USER_ATTD_HIST.PROCESS_REASON으로 저장됨
            , param.gvCmpnyCd()
            , param.gvUserCd()
            // [보안 하드닝 ripple] 모델 record 확장에 따른 값 보존만 수행한다.
            //   본 경로(요청 승인)는 진입부에서 이미 ensureCanProcessAttdSelfPolicy/ensureNotClosed/
            //   selectUserExistInCmpnySite 게이트를 통과하므로 이 두 필드는 게이트에 사용하지 않는다(동작 불변).
            , param.gvAuthCd()
            , reqRow.siteCd()
        );

        // 7-1. 근무 구간 시각 겹침 금지(정책서 attd §7.6). MGMT MERGE 이전에서 편집 구간 새값
        //      (param.checkIn/Out Date/Time, 퇴근 미입력=open 가능)이 같은 일자 다른 구간(targetId 제외,
        //      DEL_YN='N')과 겹치면 차단한다. open 타 구간(2차 출근만 있고 퇴근 미입력)도 SENTINEL 로 +∞ 처리한다
        //      (2차 출근만 있는 케이스가 핵심 — 보정으로 1차 종료를 2차 출근 이후로 늘리는 겹침 차단).
        ensureNoSegmentOverlap(
                reqRow.siteCd(), reqRow.userCd(), reqRow.workYmd(), targetId,
                param.checkInDate(), param.checkInTime(), param.checkOutDate(), param.checkOutTime(),
                param.gvCmpnyCd());

        // 7-2. OT 범위 가드(권위 가드): 보정 승인으로 정해질 새 실근무 구간이 그 근태(targetId)에 연결된
        //      모든 활성 OT 를 완전히 포함해야 한다. 줄여서 OT 가 범위 밖으로 삐져나오면 ATTD_400_114.
        //      요청~승인 사이 OT 가 변동될 수 있으므로 승인 시점에 재검사한다(앱 요청측 가드와 이중 방어).
        //      생성요청('01')이라도 targetId 는 채번된 신규 ATTD_ID 이므로 그에 연결된 OT 는 없어 자연히 0 → 무영향.
        ensureOvertimeWithinNewWindow(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), targetId,
                param.checkInDate(), param.checkInTime(), param.checkOutDate(), param.checkOutTime());

        // 8. TB_USER_ATTD_MGMT MERGE.
        attd07Mapper.updateUserAttdInfos(UpdateUserAttdInfosCommand.from(targetId, model));

        // 9. TB_USER_ATTD_HIST INSERT (HIST_TYPE='01').
        String histId = attd07Mapper.selectHistId(param.gvCmpnyCd());
        attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.from(histId, targetId, model));

        // 10. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향을 받아야 한다 (REQ_STATUS='01'(신청) 가드).
        int updated = attd07Mapper.updateUserAttdReqApprove(UpdateUserAttdRequestCommand.from(targetId, param));
        if (updated == 0) {
            // lost-update / 동시 승인 충돌: @Transactional 경계로 전체 롤백.
            log.warn("approve rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // PRAFTA-APP-021-3a(W2): 근태 보정 승인 결과를 신청자 본인에게 통보(afterCommit 격리, 승인 영향 없음).
        try {
            approvalResultNotiService.notifyAttdResult(
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.reqId(), true, param.gvUserCd());
        } catch (Exception e) {
            log.error("근태 승인 결과 통보 PUSH 적재 hook 실패(승인 영향 없음). reqId={}", reqRow.reqId(), e);
        }
    }

    // ============================================================
    // PRAFTA-008 - 근태 요청 반려
    // ============================================================

    @Override
    @Transactional
    public void rejectUserAttdRequest(RejectUserAttdRequestParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 1. 회사 scope으로 권위 있는 REQ row를 로드한다. 없으면 거부한다.
        UserAttdReqResult reqRow = attd07Mapper.selectUserAttdReqByReqId(param.reqId(), param.gvCmpnyCd());
        if (reqRow == null) {
            log.warn("reject rejected - REQ not found. reqId={}, cmpnyCd={}", param.reqId(), param.gvCmpnyCd());
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }

        // SEC-018: REQ_TYPE 가드.
        // 본 endpoint는 근태 수정/생성 요청만 반려한다. OT/LEAVE 요청이 근태 반려
        // 경로로 흘러들어 타입 혼동을 일으키지 않도록 fail-closed로 거부한다.
        if (!AttdReqTypeUtils.isAttendanceReqType(reqRow.reqType())) {
            log.warn("reject rejected - wrong REQ_TYPE for attendance endpoint. reqId={}, reqType={}",
                    reqRow.reqId(), reqRow.reqType());
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // [보안 재작업] SEC-015 + com-013-06-FU(r28) - 매니저 전용 게이트 + 자기처리/상위결재 정책.
        // 근태 요청 반려도 승인과 동일하게 노드 SELF_ATTD_APPRV_YN 정책을 적용한다.
        //   [보안 재작업] 권한 판정 노드/사업장은 클라 body 가 아닌 REQ 의 권위값(reqRow)을 사용한다
        //   (스케줄/OT 반려 경로와 일관화). 신청자=reqRow.userCd, 처리자=param.gvUserCd, 노드=reqRow.nodeCd, 사업장=reqRow.siteCd.
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), reqRow.userCd(),
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), false);
        // PRAFTA-028 - 마감된 기간(부서)의 근태 요청 반려 차단 (REQ 권위 부서 기준)
        ensureNotClosed(param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), param.workYmd());

        // 2. 대기(pending) 상태의 요청만 반려 가능 (UPDATE 측에서도 REQ_STATUS='01'(신청)로 필터함).
        if (!AttdReqTypeUtils.REQ_STATUS_REQUESTED.equals(reqRow.reqStatus())) {
            log.warn("reject rejected - REQ already processed. reqId={}, status={}",
                    reqRow.reqId(), reqRow.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 3. 요청 본문이 보안 민감 키 필드에 한해 저장된 REQ row와 일치하는지 검증한다.
        //    하나라도 불일치하면 변조(tampering)로 간주한다.
        if (StringEqualsUtils.isMismatched(param.userCd(),  reqRow.userCd())
            || StringEqualsUtils.isMismatched(param.siteCd(),  reqRow.siteCd())
            || StringEqualsUtils.isMismatched(param.workYmd(), reqRow.workYmd())
            || StringEqualsUtils.isMismatched(param.workSeq(), reqRow.workSeq())
            || StringEqualsUtils.isMismatched(param.nodeCd(),  reqRow.nodeCd())) {
            log.warn("reject rejected - body/REQ mismatch. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // [보안 재작업] SEC-017 - 대상 사용자가 호출자의 회사/사이트 scope 안에
        // 실재하는지 DB로 재확인한다 (rejectUserOvertimeRequest 와 동일 패턴).
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("reject rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // 4. HIST 행에 사용할 ATTD_ID를 결정한다.
        //    [PRAFTA-010-1-022] TARGET_ID 의미 재해석:
        //      TARGET_ID 는 "이 요청이 귀결될 ATTD_ID" 이다(수정요청=기존 ATTD_ID,
        //      생성요청=사용자측 요청 시점에 사전 채번된 ATTD_ID). 생성요청도 사전 채번
        //      규칙(prafta-010.md §2.1.1)에 따라 TARGET_ID 가 채워져 들어오므로,
        //      그대로 HIST 행 ATTD_ID 로 사용한다.
        //    - TARGET_ID 가 채워져 있으면(수정/생성 공통): 그 값을 그대로 사용한다.
        //    - TARGET_ID 가 NULL 인 경우(레거시 방어): 시퀀스에서 새 ATTD_ID 를 발급한다.
        //      어느 경우든 반려는 미반영(정책 §9.5)이므로 TB_USER_ATTD_MGMT 에는
        //      INSERT 하지 않고 HIST 행에만 사용한다.
        String histAttdId;
        if (reqRow.targetId() != null && !reqRow.targetId().isEmpty()) {
            histAttdId = reqRow.targetId();
        } else {
            histAttdId = attd07Mapper.selectAttdId(param.gvCmpnyCd());
        }

        // 5. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향을 받아야 한다 (REQ_STATUS='01'(신청) 가드).
        int updated = attd07Mapper.updateUserAttdReqReject(RejectUserAttdRequestCommand.from(param));
        if (updated == 0) {
            // lost-update / 동시 처리 충돌: @Transactional 경계로 전체 롤백.
            log.warn("reject rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 6. TB_USER_ATTD_HIST INSERT (HIST_TYPE='07', BEF_*/AFT_* 전부 NULL).
        String histId = attd07Mapper.selectHistId(param.gvCmpnyCd());
        attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.forReject(
                histId, histAttdId, param.gvCmpnyCd(), reqRow.siteCd(),
                reqRow.workYmd(), param.rejectReason(), param.gvUserCd()));

        log.info("근태 요청 반려 완료. reqId={}, reqType={}, attdId={}",
                reqRow.reqId(), reqRow.reqType(), histAttdId);

        // PRAFTA-APP-021-3a(W2): 근태 보정 반려 결과를 신청자 본인에게 통보(afterCommit 격리, 반려 영향 없음).
        try {
            approvalResultNotiService.notifyAttdResult(
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.reqId(), false, param.gvUserCd());
        } catch (Exception e) {
            log.error("근태 반려 결과 통보 PUSH 적재 hook 실패(반려 영향 없음). reqId={}", reqRow.reqId(), e);
        }
    }

    // ============================================================
    // PRAFTA-APP-007 - 스케줄 수정 요청('10') 승인 / 반려
    //   정책: attd/09 §9.2(승인 효과=스케줄 갱신/반려 시 기존 유지),
    //        request-approval/06 §6.1(승인=스케줄 갱신, 차단 사유 3종), §9.5(반려 사유 필수).
    //   tb_user_work_plan 은 사용자-일자당 단일 WORK_PLAN_CD 한 칸이다(WORK_SEQ 없음).
    //   승인 = 그 칸을 REQ 의 SCH_CD 로 upsert. 반려 = 미반영(상태 전이만). HIST 미기록(D3).
    // ============================================================

    /**
     * [D15] 스케줄 수정 승인 시 PROCESS_COMMENT 에 저장하는 구조화 마커의 접두.
     * 전체 형식은 'SCHED_MODIFY_APPROVED:OLD=<변경 전 WORK_PLAN_CD>' 이며,
     * 처리 이력 쿼리가 OLD= 뒤 코드로 "변경 전 스케줄" 시각을 복원한다(after 는 R.SCH_CD).
     * 이 마커는 사용자에게 노출하지 않는다(이력 쿼리에서 승인 사유는 NULL 로 치환).
     */
    private static final String SCHED_MODIFY_APPROVED_MARKER_PREFIX = "SCHED_MODIFY_APPROVED:OLD=";

    @Override
    @Transactional
    public void approveSchedModifyRequest(ApproveSchedModifyRequestParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 1. 회사 scope 으로 권위 있는 REQ row 를 로드한다. 없으면 거부한다.
        UserAttdReqResult reqRow = attd07Mapper.selectUserAttdReqByReqId(param.reqId(), param.gvCmpnyCd());
        if (reqRow == null) {
            log.warn("sched-modify approve rejected - REQ not found. reqId={}, cmpnyCd={}", param.reqId(), param.gvCmpnyCd());
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }

        // SEC-018: REQ_TYPE 가드. 본 endpoint 는 스케줄 수정 요청('10')만 처리한다.
        // 근태/OT/연차 요청이 이 경로로 흘러들어 잘못된 스케줄 반영(타입 혼동)을 일으키지
        // 않도록 fail-closed 로 거부한다.
        if (!AttdReqTypeUtils.isScheduleModifyReqType(reqRow.reqType())) {
            log.warn("sched-modify approve rejected - wrong REQ_TYPE. reqId={}, reqType={}",
                    reqRow.reqId(), reqRow.reqType());
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // SEC-015 + com-013-06-FU(r28) - 매니저 전용 게이트 + 자기처리/상위결재 정책.
        // 스케줄 수정 승인도 노드 SELF_ATTD_APPRV_YN 정책(자기처리는 ATTD_403_001)을 적용한다.
        //   [보안 정렬] 권한 판정 대상 부서/사업장은 클라 body 가 아닌 REQ 의 권위값(reqRow)을 사용한다.
        //   신청자=reqRow.userCd, 처리자=param.gvUserCd, 노드=reqRow.nodeCd.
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), reqRow.userCd(),
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), false);

        // PRAFTA-028 - 마감된 기간(부서)의 스케줄 변경 차단 (REQ 의 권위 부서/근무일 기준).
        //   현행 PRAFTA 마감 메커니즘은 근태 월마감(tb_attd_close) 단일이므로 그것으로 매핑한다(D5-a).
        ensureNotClosed(param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), reqRow.workYmd());

        // 2. 대기(pending '01' 신청) 상태의 요청만 승인 가능 (UPDATE 측에서도 REQ_STATUS='01' 가드).
        if (!AttdReqTypeUtils.REQ_STATUS_REQUESTED.equals(reqRow.reqStatus())) {
            log.warn("sched-modify approve rejected - REQ already processed. reqId={}, status={}",
                    reqRow.reqId(), reqRow.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 3. body 의 키 필드가 저장된 REQ row 와 일치하는지 검증한다(변조/IDOR 차단).
        if (StringEqualsUtils.isMismatched(param.userCd(),  reqRow.userCd())
            || StringEqualsUtils.isMismatched(param.siteCd(),  reqRow.siteCd())
            || StringEqualsUtils.isMismatched(param.workYmd(), reqRow.workYmd())
            || StringEqualsUtils.isMismatched(param.workSeq(), reqRow.workSeq())
            || StringEqualsUtils.isMismatched(param.nodeCd(),  reqRow.nodeCd())) {
            log.warn("sched-modify approve rejected - body/REQ mismatch. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 4. 대상 사용자가 호출자의 회사/사이트 scope 안에 실재하는지 DB 로 재확인한다.
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd());
        if (userExists <= 0) {
            log.warn("sched-modify approve rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // 5. 목표 스케줄 코드는 서버 권위 값(REQ row 의 SCH_CD)만 사용한다(클라 미신뢰).
        //    null/빈값이면 마이그 미적용 또는 등록 오류로 보고 fail-closed 거부한다(WORK_PLAN_CD 오염 방지).
        String schCd = reqRow.schCd();
        if (schCd == null || schCd.isEmpty()) {
            log.warn("sched-modify approve rejected - REQ.SCH_CD missing(데이터 부재/마이그 미적용). reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 5-1. [D15] upsert 로 덮어쓰기 전에 "변경 전 근무계획 코드"를 캡처한다(같은 트랜잭션).
        //      이 값을 PROCESS_COMMENT 마커에 직렬화하여, 처리 이력에서 변경 전→후 스케줄을 복원한다(무마이그).
        //      해당 일자에 근무계획이 없으면 null → 마커는 OLD= 빈값(변경 전 "없음")으로 남는다.
        String oldWorkPlanCd = attd07Mapper.selectUserWorkPlanCd(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.workYmd());

        // 5-2. prafta-com-008-D: 교대 잠금 가드 — 해당 근무일이 교대팀 소속 구간이면 스케줄수정 승인(반영)을 차단한다.
        //      관리자 예외 없음(요구 1-2: 관리자·사용자 모두 변경 불가). REQ 의 권위값(site/user/workYmd) 사용.
        //      upsert(work_plan 갱신) 이전에 차단하여 레코드 변경을 막는다.
        shiftMembershipService.assertNotShiftLocked(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.workYmd());

        // 5-3. 교차일 겹침 가드 — 승인(반영) 시 앞뒤 근무일의 스케줄과 시각이 겹치면(야간 오버나이트 포함)
        //      차단한다. work_plan upsert 이전에 검사하여 레코드 변경을 막는다. 단건 처리이므로 pending 없음(null).
        if (scheduleOverlapGuardService.hasCrossDayOverlap(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.workYmd(), schCd, null)) {
            log.warn("sched-modify approve rejected - 교차일 스케줄 겹침. reqId={}, workYmd={}, schCd={}",
                    reqRow.reqId(), reqRow.workYmd(), schCd);
            throw new ApiException(AttdErrorCode.ATTD_400_115);
        }

        // 5-4. E3(당일분모 전환, W4): 연차 잠금일(확정 연차 전 단위 + 미결 시간차 신청) 스케줄수정 승인 하드
        //      차단(ATTD_400_164). 시간차 분모(E1)가 당일 배정 스케줄이므로, 잠금일의 스케줄을 바꾸면 차감
        //      분모가 훼손된다. 판정 입력은 REQ 권위값(reqRow.siteCd/userCd/workYmd)만 사용(body 위조 무력).
        //      OT 잠금은 본 가드의 대상이 아니다(기존 경로 정책 불변 — 연차 잠금만 신설 차단).
        //      관리자 탈출구 = 연차 취소·처리 → 재승인. upsert(work_plan 갱신) 이전에 차단.
        List<com.prafta.common.cmm.schedule.vo.ScheduleLockVO> leaveLocks =
                scheduleChangeGuardService.findLockedDays(
                        param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), List.of(reqRow.workYmd()));
        boolean leaveLocked = leaveLocks.stream()
                .anyMatch(l -> l.getReason() == com.prafta.common.cmm.schedule.vo.ScheduleLockVO.Reason.LEAVE);
        if (leaveLocked) {
            log.warn("sched-modify approve rejected - 연차 잠금일(확정/미결 시간차) 스케줄 변경 차단(E3). reqId={}, workYmd={}",
                    reqRow.reqId(), reqRow.workYmd());
            // F-7(2026-08-06): 잠금 원인(확정 연차 / 미결 시간차)에 맞는 문구를 동적 주입한다(코드 번호는 유지).
            throw new ApiException(AttdErrorCode.ATTD_400_164,
                    com.prafta.common.cmm.schedule.ScheduleLockMessages.scheduleChangeBlockedMessage(leaveLocks));
        }

        // 6. tb_user_work_plan 의 (cmpny, site, user, ymd) 한 칸 WORK_PLAN_CD 를 SCH_CD 로 upsert (D1/D2).
        attd07Mapper.upsertUserWorkPlan(UpsertUserWorkPlanCommand.of(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.workYmd(), schCd, param.gvUserCd()));

        // 7. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향(REQ_STATUS='01' 가드). 0행이면 동시 처리 충돌 → 롤백.
        //    [D15] 승인 마커: 'SCHED_MODIFY_APPROVED:OLD=<oldWorkPlanCd>' (oldWorkPlanCd null 이면 빈값).
        //          마커 자체는 사용자에게 노출하지 않으며(이력 쿼리에서 승인 사유는 NULL 처리), oldCode 만 담는다
        //          (after 는 R.SCH_CD 로 이미 복원 가능). PROCESS_COMMENT 는 varchar(500) 이라 코드 1개는 충분.
        String approveMarker = SCHED_MODIFY_APPROVED_MARKER_PREFIX
                + (oldWorkPlanCd == null ? "" : oldWorkPlanCd);
        int updated = attd07Mapper.updateUserSchedModifyReqApprove(
                reqRow.reqId(), param.gvCmpnyCd(), reqRow.siteCd(), param.gvUserCd(), approveMarker);
        if (updated == 0) {
            log.warn("sched-modify approve rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        log.info("스케줄 수정 요청 승인 완료. reqId={}, userCd={}, workYmd={}, schCd={}",
                reqRow.reqId(), reqRow.userCd(), reqRow.workYmd(), schCd);

        // PRAFTA-COM-013-06-1(r30): 스케줄 수정 승인 결과를 신청자 본인에게 통보(afterCommit 격리, 승인 영향 없음).
        //   근태/초과근무 보정 결재와 동일 경로(notifyAttdResult) 재사용 — 본문은 승인/반려 여부만(타입 무관).
        //   dedupKey 는 reqId 단위로 분리되므로 근태/OT 와 충돌하지 않는다.
        try {
            approvalResultNotiService.notifyAttdResult(
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.reqId(), true, param.gvUserCd());
        } catch (Exception e) {
            log.error("스케줄 수정 승인 결과 통보 PUSH 적재 hook 실패(승인 영향 없음). reqId={}", reqRow.reqId(), e);
        }
    }

    @Override
    @Transactional
    public void rejectSchedModifyRequest(RejectUserAttdRequestParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 1. 회사 scope 으로 권위 있는 REQ row 를 로드한다. 없으면 거부한다.
        UserAttdReqResult reqRow = attd07Mapper.selectUserAttdReqByReqId(param.reqId(), param.gvCmpnyCd());
        if (reqRow == null) {
            log.warn("sched-modify reject rejected - REQ not found. reqId={}, cmpnyCd={}", param.reqId(), param.gvCmpnyCd());
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }

        // SEC-018: REQ_TYPE 가드. 본 endpoint 는 스케줄 수정 요청('10')만 반려한다.
        if (!AttdReqTypeUtils.isScheduleModifyReqType(reqRow.reqType())) {
            log.warn("sched-modify reject rejected - wrong REQ_TYPE. reqId={}, reqType={}",
                    reqRow.reqId(), reqRow.reqType());
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // SEC-015 + com-013-06-FU(r28) - 매니저 전용 게이트 + 자기처리/상위결재 정책 (REQ 권위 부서 기준).
        //   신청자=reqRow.userCd, 처리자=param.gvUserCd, 노드=reqRow.nodeCd. 자기처리는 ATTD_403_001.
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), reqRow.userCd(),
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), false);

        // PRAFTA-028 - 마감된 기간(부서)의 스케줄 요청 반려 차단 (REQ 의 권위 부서/근무일 기준).
        ensureNotClosed(param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), reqRow.workYmd());

        // 2. 대기('01' 신청) 상태의 요청만 반려 가능 (UPDATE 측에서도 REQ_STATUS='01' 가드).
        if (!AttdReqTypeUtils.REQ_STATUS_REQUESTED.equals(reqRow.reqStatus())) {
            log.warn("sched-modify reject rejected - REQ already processed. reqId={}, status={}",
                    reqRow.reqId(), reqRow.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 3. body 의 키 필드가 저장된 REQ row 와 일치하는지 검증한다(변조/IDOR 차단).
        if (StringEqualsUtils.isMismatched(param.userCd(),  reqRow.userCd())
            || StringEqualsUtils.isMismatched(param.siteCd(),  reqRow.siteCd())
            || StringEqualsUtils.isMismatched(param.workYmd(), reqRow.workYmd())
            || StringEqualsUtils.isMismatched(param.workSeq(), reqRow.workSeq())
            || StringEqualsUtils.isMismatched(param.nodeCd(),  reqRow.nodeCd())) {
            log.warn("sched-modify reject rejected - body/REQ mismatch. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 4. 대상 사용자가 호출자의 회사/사이트 scope 안에 실재하는지 DB 로 재확인한다.
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd());
        if (userExists <= 0) {
            log.warn("sched-modify reject rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // 5. 반려 사유 필수(§9.5). Param.from 에서 이미 @NotBlank/rejectReason 비어있음 검증되나
        //    defence-in-depth 로 service 에서도 재확인한다.
        if (param.rejectReason() == null || param.rejectReason().isBlank()) {
            log.warn("sched-modify reject rejected - empty rejectReason. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 6. TB_USER_ATTD_REQ UPDATE(반려 '03', 사유 기록). tb_user_work_plan 은 미변경(D4). HIST 미기록(D3).
        //    근태/OT 반려와 동일 command/mapper 재사용. 0행이면 동시 처리 충돌 → 롤백.
        int updated = attd07Mapper.updateUserAttdReqReject(RejectUserAttdRequestCommand.from(param));
        if (updated == 0) {
            log.warn("sched-modify reject rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        log.info("스케줄 수정 요청 반려 완료. reqId={}, userCd={}, workYmd={}",
                reqRow.reqId(), reqRow.userCd(), reqRow.workYmd());

        // PRAFTA-COM-013-06-1(r30): 스케줄 수정 반려 결과를 신청자 본인에게 통보(afterCommit 격리, 반려 영향 없음).
        //   근태/초과근무 보정 결재와 동일 경로(notifyAttdResult) 재사용.
        try {
            approvalResultNotiService.notifyAttdResult(
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.reqId(), false, param.gvUserCd());
        } catch (Exception e) {
            log.error("스케줄 수정 반려 결과 통보 PUSH 적재 hook 실패(반려 영향 없음). reqId={}", reqRow.reqId(), e);
        }
    }

    // ============================================================
    // PRAFTA-010 - 초과근무 요청 반려
    // ============================================================

    @Override
    @Transactional
    public void rejectUserOvertimeRequest(RejectUserOvertimeRequestParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 1. 회사 scope으로 권위 있는 REQ row를 로드한다. 없으면 거부한다.
        UserAttdReqResult reqRow = attd07Mapper.selectUserAttdReqByReqId(param.reqId(), param.gvCmpnyCd());
        if (reqRow == null) {
            log.warn("OT reject rejected - REQ not found. reqId={}, cmpnyCd={}", param.reqId(), param.gvCmpnyCd());
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }

        // SEC-018: REQ_TYPE 가드.
        // 본 endpoint는 초과근무 요청만 반려한다. 근태/연차 요청이 OT 반려 경로로
        // 흘러들어 타입 혼동을 일으키지 않도록 fail-closed로 거부한다.
        if (!AttdReqTypeUtils.isOvertimeReqType(reqRow.reqType())) {
            log.warn("OT reject rejected - wrong REQ_TYPE for overtime endpoint. reqId={}, reqType={}",
                    reqRow.reqId(), reqRow.reqType());
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // SEC-015 + com-013-06-FU(r28) - 매니저 전용 게이트 + 자기처리/상위결재 정책 (REQ 권위 부서 기준).
        //   신청자=reqRow.userCd, 처리자=param.gvUserCd, 노드=reqRow.nodeCd. OT 자기처리는 ATTD_403_003.
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), reqRow.userCd(),
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), true);
        // PRAFTA-028 - 마감된 기간(부서)의 초과근무 요청 반려 차단 (REQ 의 권위 부서/근무일 기준)
        ensureNotClosed(param.gvCmpnyCd(), reqRow.siteCd(), reqRow.nodeCd(), reqRow.workYmd());

        // 2. 대기(pending) 상태의 요청만 반려 가능 (UPDATE 측에서도 REQ_STATUS='01'(신청)로 필터함).
        if (!AttdReqTypeUtils.REQ_STATUS_REQUESTED.equals(reqRow.reqStatus())) {
            log.warn("OT reject rejected - REQ already processed. reqId={}, status={}",
                    reqRow.reqId(), reqRow.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // SEC-017 - body의 userCd / siteCd가 저장된 REQ row와 일치하는지 검증한다.
        //   불일치 시 변조(cross-user / cross-site IDOR)로 간주한다.
        if (StringEqualsUtils.isMismatched(param.userCd(), reqRow.userCd())
                || StringEqualsUtils.isMismatched(param.siteCd(), reqRow.siteCd())) {
            log.warn("OT reject rejected - body/REQ scope mismatch. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // SEC-017 - 대상 사용자가 호출자의 회사/사이트 scope 안에 실재하는지 DB로 재확인한다.
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("OT reject rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // 3. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향을 받아야 한다 (REQ_STATUS='01'(신청) 가드).
        int updated = attd07Mapper.updateUserAttdReqReject(RejectUserAttdRequestCommand.from(param));
        if (updated == 0) {
            // lost-update / 동시 처리 충돌: @Transactional 경계로 전체 롤백.
            log.warn("OT reject rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 4. PRAFTA-027 - 초과근무 반려 처리 이력(TB_USER_ATTD_HIST) 기록.
        //    근태 보정 반려가 HIST_TYPE='07'(BEF/AFT NULL) 을 남기듯, OT 반려도
        //    HIST_TYPE='09'(초과근무 반려) 이력을 남겨 일자 상세 "처리 이력"에 노출한다.
        //    HIST.ATTD_ID 앵커는 그날 근태기록의 ATTD_ID 를 사용한다(reqRow 기준).
        String histAttdId = attd07Mapper.selectAttdIdByDay(
                param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.workYmd());
        if (histAttdId != null && !histAttdId.isEmpty()) {
            String histId = attd07Mapper.selectHistId(param.gvCmpnyCd());
            attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.forOvertimeReject(
                    histId, histAttdId, param.gvCmpnyCd(), reqRow.siteCd(), reqRow.workYmd(),
                    param.rejectReason(), param.gvUserCd()));
        } else {
            // 운영 규칙상 도달 불가(OT 는 근태기록 없이 등록 불가). 핵심 동작(요청 반려)은 유지하고 이력만 생략한다.
            log.warn("OT reject - 처리 이력 생략: 그날 근태기록(ATTD_ID) 미존재. reqId={}, userCd={}, workYmd={}",
                    reqRow.reqId(), reqRow.userCd(), reqRow.workYmd());
        }

        log.info("초과근무 요청 반려 완료. reqId={}", reqRow.reqId());

        // PRAFTA-APP-021-3a(W2): 초과근무 보정 반려 결과를 신청자 본인에게 통보(afterCommit 격리, 반려 영향 없음).
        try {
            approvalResultNotiService.notifyAttdResult(
                    param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.reqId(), false, param.gvUserCd());
        } catch (Exception e) {
            log.error("초과근무 반려 결과 통보 PUSH 적재 hook 실패(반려 영향 없음). reqId={}", reqRow.reqId(), e);
        }
    }

    // ============================================================
    // PRAFTA-003 - 초과근무(Overtime) 등록
    // ============================================================

    @Override
    @Transactional
    public void updateUserOvertimeRequests(UpdateUserOvertimeRequestParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // SEC-015/SEC-016 + com-013-06-FU(r28) - 매니저 전용 게이트 + 자기처리/상위결재 정책.
        // OT 직접 등록은 일반 작업자가 호출해선 안 되고, 노드 SELF_ATTD_APPRV_YN='N' 정책에서
        // 노드 관리자 본인의 자기 등록을 차단하고 부모 1단계 노드 관리자에게만 등록을 허용한다.
        // 역할 검사는 JWT 기반 gvAuthCd 를 사용하므로 body 위조로 권한 escalation 을 할 수 없다.
        //   신청자(대상)=param.userCd, 처리자=param.gvUserCd, OT 자기처리는 ATTD_403_003.
        //
        // [보안 재작업 com-013-06-FU] OT 직접등록 경로는 reqRow(권위 row)가 없어 body↔REQ nodeCd
        //   mismatch 검사가 전혀 없었다. 그래서 'N' 노드 관리자가 본인 OT 를 등록할 때 body nodeCd 를
        //   본인이 관리자이면서 'Y'인 다른 노드로 위조하면 게이트가 4-b 로 빠져 자기처리 차단이 무력화되고,
        //   위조 nodeCd 가 TB_USER_OVERTIME_MGMT.NODE_CD 에까지 영속되었다.
        //   → 게이트 노드/영속 노드 모두 대상 사용자(param.userCd)의 서버 조회 노드로 강제한다.
        String authoritativeNodeCd =
                attdCloseService.resolveUserNodeCd(param.gvCmpnyCd(), param.siteCd(), param.userCd());
        // 소속 미상(서버노드 null/blank)이면 노드 단위 정책을 적용할 수 없으므로 fail-closed 차단.
        if (authoritativeNodeCd == null || authoritativeNodeCd.isBlank()) {
            log.warn("OT register rejected - 대상 사용자 소속 부서 미상(서버 노드 부재). cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        // body nodeCd 가 서버 권위 노드와 불일치하면 변조로 간주하고 차단(다른 경로의 mismatch 방어와 동일 레벨).
        if (StringEqualsUtils.isMismatched(param.nodeCd(), authoritativeNodeCd)) {
            log.warn("OT register rejected - body/서버 nodeCd mismatch(변조). cmpnyCd={}, siteCd={}, userCd={}, paramNode={}, serverNode={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.nodeCd(), authoritativeNodeCd);
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }
        // 게이트 인자 = 서버 권위 노드(클라 body 미신뢰).
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), param.userCd(),
                param.gvCmpnyCd(), param.siteCd(), authoritativeNodeCd, true);
        // PRAFTA-028 - 마감된 기간(부서)의 초과근무 등록/수정 차단 (게이트와 동일한 권위 노드 사용).
        ensureNotClosed(param.gvCmpnyCd(), param.siteCd(), authoritativeNodeCd, param.workYmd());

        // SEC-017 (a) - 대상 사용자가 호출자의 회사/사이트 scope 안에 존재해야 한다.
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("OT register rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // 일용직(EMPLOYMENT_TYPE='DAILY') 근로자는 초과근무를 등록할 수 없다(fail-closed).
        //   프론트(AttdDayDetailPop)는 일용직 셀에 OT 등록 UI 를 노출하지 않으나,
        //   body 위조/우회 호출을 막기 위해 서버에서도 강제 차단한다. (관리자 직접등록·요청 승인 공용 경로)
        if (attd07Mapper.selectDailyWorkerInScope(
                param.gvCmpnyCd(), param.siteCd(), param.userCd()) > 0) {
            log.info("OT register rejected - 일용직 OT 등록 불가. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_400_152);
        }

        // SEC-017 (b) - attdId가 전달된 경우, scope 안의 대상 사용자에 속한 ATTD여야 한다.
        if (param.attdId() != null && !param.attdId().isEmpty()) {
            int attdExists = attd07Mapper.selectAttdExistInScope(
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.attdId());
            if (attdExists <= 0) {
                log.warn("OT register rejected - attdId not in scope. cmpnyCd={}, siteCd={}, userCd={}, attdId={}",
                        param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.attdId());
                throw new ApiException(AttdErrorCode.ATTD_404_012);
            }
        }

        // SEC-017 (c) - reqId가 전달된 경우, body의 userCd / siteCd와 일치하는 REQ row를 참조해야 한다.
        // 근태 승인 경로와 동일한 권위 row 로더를 공유하기 위해 selectUserAttdReqByReqId를 재사용한다.
        // PRAFTA-025: reqRow / isModify 를 메서드 스코프로 끌어올려 03(생성=INSERT) / 04(수정=UPDATE) 분기에 사용한다.
        UserAttdReqResult reqRow = null;
        boolean isModify = false;
        if (param.reqId() != null && !param.reqId().isEmpty()) {
            reqRow = attd07Mapper.selectUserAttdReqByReqId(
                    param.reqId(), param.gvCmpnyCd());
            if (reqRow == null) {
                log.warn("OT register rejected - reqId not found. reqId={}, cmpnyCd={}",
                        param.reqId(), param.gvCmpnyCd());
                throw new ApiException(AttdErrorCode.ATTD_404_001);
            }
            // SEC-018: REQ_TYPE 가드. 본 endpoint는 초과근무 요청(03 생성 / 04 수정)만 처리한다.
            // 근태/연차 요청이 OT 승인 경로로 흘러들어 타입 혼동을 일으키지 않도록 fail-closed로 거부한다.
            if (!AttdReqTypeUtils.isOvertimeReqType(reqRow.reqType())) {
                log.warn("OT approve rejected - wrong REQ_TYPE for overtime endpoint. reqId={}, reqType={}",
                        reqRow.reqId(), reqRow.reqType());
                throw new ApiException(AttdErrorCode.ATTD_400_006);
            }
            isModify = AttdReqTypeUtils.isOvertimeModify(reqRow.reqType());
            if (StringEqualsUtils.isMismatched(param.userCd(), reqRow.userCd())
                    || StringEqualsUtils.isMismatched(param.siteCd(), reqRow.siteCd())) {
                log.warn("OT register rejected - reqId scope mismatch. reqId={}, paramUser={}, reqUser={}, paramSite={}, reqSite={}",
                        reqRow.reqId(),
                        param.userCd(), reqRow.userCd(),
                        param.siteCd(), reqRow.siteCd());
                throw new ApiException(AttdErrorCode.ATTD_400_005);
            }
            // 요청 승인 관리(Attd_10) 인박스 경유 승인: 대기('01' 신청) 상태의 요청만 승인 가능.
            // (이중 처리 방지 — 등록 전 선제 가드. 마감 처리는 INSERT/UPDATE 후 updateUserOvertimeReqApprove에서 수행.)
            if (!AttdReqTypeUtils.REQ_STATUS_REQUESTED.equals(reqRow.reqStatus())) {
                log.warn("OT approve rejected - REQ already processed. reqId={}, status={}",
                        reqRow.reqId(), reqRow.reqStatus());
                throw new ApiException(AttdErrorCode.ATTD_409_001);
            }
        }

        // 1. defence-in-depth - controller에서 이미 @NotEmpty 검증 완료.
        if (param.overtimes() == null || param.overtimes().isEmpty()) {
            log.warn("OT register rejected - empty overtimes list. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_010);
        }

        // PRAFTA-COM-008-B-3: 연차일 초과근무 차단.
        //   그날 종일('00') 확정 연차가 있으면(촉진/자발 무관 일괄 차단) OT 등록을 거부한다(INSERT 이전).
        //   "연차일자 조정"은 문서 C(연차 이동/삭제 동의·거부) 흐름으로 연결한다(BE 는 차단만, 안내 문구).
        if (attd07Mapper.countFullDayLeaveOn(
                param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd()) > 0) {
            log.info("[prafta-com-008-B-3] 웹 OT 등록 거부: 연차일 — userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_151);
        }

        // 2. 요청된 OT 구간을 분 stamp로 정규화한다.
        //    [QA 재작업 D1] stamp origin 은 workYmd-1 00:00 = 0 기준으로 통일되어 있으며,
        //    오버나이트 OT 가 workYmd+1 23:59(stamp 4319)까지 늘어날 수 있다.
        List<int[]> reqStamps = new ArrayList<>(param.overtimes().size());
        for (OvertimeItemModel ot : param.overtimes()) {
            int[] stamp = DateTimeUtils.toStampRange(param.workYmd(),
                                      ot.startDate(), ot.startTime(),
                                      ot.endDate(), ot.endTime());
            if (stamp == null) {
                log.warn("OT register rejected - invalid range. userCd={}, workYmd={}, ot={}-{}/{}-{}",
                        param.userCd(), param.workYmd(),
                        ot.startDate(), ot.startTime(), ot.endDate(), ot.endTime());
                throw new ApiException(AttdErrorCode.ATTD_400_011);
            }
            reqStamps.add(stamp);
        }

        // 3. 요청된 OT 구간 간 겹침 검사.
        if (IntervalUtils.hasOverlap(reqStamps)) {
            log.warn("OT register rejected - segments overlap. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_013);
        }

        // 4. 스케줄과 raw 실근태 구간을 로드한다.
        //    초과근무 등록 가능 범위는 "실근태 − 스케줄" 로 계산한다.
        AllowedWindowResult windows = attd07Mapper.selectAllowedWindow(
                OvertimeAllowedWindowQuery.from(param));
        if (windows == null) {
            // 근무계획(TB_USER_WORK_PLAN) 행이 없는 날(주말 등 미배정일). 정책 §7.5: 스케줄 없는 날
            // 근무는 전량 초과근무 대상이므로 OT 승인이 가능해야 한다. WP anchor 쿼리는 실근태까지
            // 함께 유실하므로, 스케줄을 비우고 실근태만 로드하는 폴백으로 윈도우를 구성한다.
            // 이후 구간 차집합 로직이 schSeg=null 을 자동으로 "전량 허용"으로 처리한다(아래 schSeg==null 분기).
            windows = attd07Mapper.selectActualWindowNoSchedule(
                    OvertimeAllowedWindowQuery.from(param));
        }
        if (windows == null) {
            // 폴백은 dummy anchor 라 정상적으론 null 이 아니다(방어). 실근태 부재는 아래
            // ATTD_400_014(출퇴근 기록 누락) 경로가 처리한다.
            log.warn("OT approve rejected - actual window load failed. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_014);
        }

        // PRAFTA-011 - WORK_SEQ 인덱스를 보존한 구간 배열을 사용한다.
        //   schBySeq[1]/[2] = 1·2구간 스케줄, actBySeq[1]/[2] = 1·2구간 raw 실근태.
        int[][] schBySeq = AttdScheduleUtils.buildScheduledSegmentsBySeq(param.workYmd(), windows);
        int[][] actBySeq = AttdScheduleUtils.buildActualSegmentsBySeq(param.workYmd(), windows);

        // PRAFTA-003-1 (Q1) - 해당 일자에 완료된 근무 기록이 없는 경우, 별도 에러 코드로 분리.
        // 완료된 actual 근무 구간이 없으면 "스케줄 외 시간(outside of schedule)" 윈도우를
        // 도출할 수 없으므로, OT 범위 자체가 잘못되었다고 시사하는 대신 "출퇴근 기록 누락"이라는
        // 사전조건이 누락되었다는 점을 호출자에게 알린다.
        if (actBySeq[1] == null && actBySeq[2] == null) {
            log.warn("OT register rejected - no actual work segments. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_014);
        }

        // 4-B. HB-08(D5) - 그날 확정 부분연차(반차/시간차)의 면제 구간을 로드한다.
        //    등록 가능 범위 = 실근태 - (스케줄 구간 ∪ 연차 면제 구간).
        //    ★ 지시서 문언("스케줄 구간에서 반차 쉬는 구간을 제외")대로 스케줄을 줄이면 D5 가 오히려
        //      악화된다(D5 는 2구간에 스케줄이 아예 null 이라 뺄 것이 없고, 스케줄을 줄이면 OT 가 늘어난다).
        //      필요한 연산은 피감수를 넓히는 합집합이다.
        //    회귀 금지 ①(조기출근 OT 120분): 면제 구간과 겹치지 않는 스케줄 밖 근무는 그대로 남는다.
        //    회귀 금지 ③(시간차): 시간차 면제 구간은 항상 스케줄 안이라 합집합해도 결과가 변하지 않는다.
        //    ★ 3차(§15-2): 면제 구간 환산은 반드시 그날 원 스케줄을 프레임으로 정렬한다(windows 보유).
        List<int[]> leaveExemptSegs = buildLeaveExemptSegments(param.workYmd(),
                attd07Mapper.selectLeaveExemptWindows(
                        param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd()),
                windows, param.userCd());

        // 5. PRAFTA-011 - 등록가능시간을 구간별로 분리 계산한다.
        //    각 WORK_SEQ 에 대해 "그 구간 actual - (그 구간 schedule ∪ 연차 면제 구간)" 차집합을 구한다.
        //    매칭 스케줄이 없는 구간도 연차 면제 구간은 빼야 한다(D5 교정 지점).
        //    구간별 결과를 모두 합쳐 최종 allowed 윈도우를 만든다.
        //    1구간/2구간 actual 은 (오버나이트 보정 포함) 서로 겹치지 않도록 stamp 되어 있다.
        List<int[]> allowedAll = new ArrayList<>(2);
        for (int seq = 1; seq <= 2; seq++) {
            int[] actSeg = actBySeq[seq];
            if (actSeg == null) {
                // 해당 구간의 actual 근무 기록이 없으면 등록가능시간도 없다.
                continue;
            }

            List<int[]> actList = new ArrayList<>(1);
            actList.add(actSeg);

            int[] schSeg = schBySeq[seq];
            // 피감수 = 그 구간 스케줄(있으면) ∪ 그날 연차 면제 구간.
            List<int[]> subtrahend = new ArrayList<>(leaveExemptSegs.size() + 1);
            if (schSeg != null) {
                subtrahend.add(schSeg);
            }
            subtrahend.addAll(leaveExemptSegs);

            List<int[]> seqAllowed;
            if (subtrahend.isEmpty()) {
                // 스케줄도 연차도 없는 구간 - 그 구간 근무 전체를 등록가능으로 본다(종전 동작).
                seqAllowed = IntervalUtils.merge(actList);
            } else {
                seqAllowed = IntervalUtils.subtract(
                        IntervalUtils.merge(actList), IntervalUtils.merge(subtrahend));
            }
            allowedAll.addAll(seqAllowed);
        }

        // 구간별 결과를 합쳐 정렬·병합한다 (인접 구간이 맞닿는 경우 대비).
        List<int[]> allowed = IntervalUtils.merge(allowedAll);

        // 6. 모든 요청 OT 구간은 allowed의 단일 sub-interval에 완전히 포함되어야 한다.
        for (int[] req : reqStamps) {
            if (!IntervalUtils.isContainedInAny(req, allowed)) {
                log.warn("OT register rejected - outside allowed window. userCd={}, workYmd={}, req={}-{}",
                        param.userCd(), param.workYmd(), req[0], req[1]);
                throw new ApiException(AttdErrorCode.ATTD_400_012);
            }
        }

        // com-013-06 A - 관리자 직접수정에서 in-place 갱신 대상(OT_ID 보유)인 행 목록.
        //   아래 기존 활성 OT 겹침 검사(6-1)에서 "자기 자신"을 제외하기 위함이다.
        //   그 외 경로(인박스 04 수정·앱 승인 등)는 요청에 OT_ID 가 없어 빈 목록이 되어 기존 동작과 동일하다.
        List<String> editingOtIds = new ArrayList<>();
        for (OvertimeItemModel ot : param.overtimes()) {
            if (ot.otId() != null && !ot.otId().isBlank()) {
                editingOtIds.add(ot.otId());
            }
        }

        // 6-1. PRAFTA-009-001 - 기존 활성 OT 행과의 중복(시간 구간 겹침) 검사.
        //      INSERT 루프 진입 전에 요청된 모든 OT 구간을 DB의 기존 활성 행과 대조한다.
        //      하나라도 겹치면 부분 INSERT 없이 전체 거부한다.
        //      이 화면은 항상 ATTD_ID 를 보유하므로 ATTD_ID 기준으로 검사한다.
        //      겹침 판정/NULL 종료 처리 규칙은 selectOverlappingOvertimeCount 참조.
        //      com-013-06 A: in-place 수정 대상 OT_ID 는 자기 자신과 겹쳐도 정상이므로 검사에서 제외한다.
        if (param.attdId() != null && !param.attdId().isEmpty()) {
            for (OvertimeItemModel ot : param.overtimes()) {
                String reqStart = ot.startDate() + ot.startTime();
                String reqEnd = ot.endDate() + ot.endTime();

                int overlapCount = attd07Mapper.selectOverlappingOvertimeCount(
                        param.gvCmpnyCd(), param.siteCd(), param.userCd(),
                        param.attdId(), reqStart, reqEnd, editingOtIds);

                if (overlapCount > 0) {
                    log.warn("OT register rejected - overlaps existing active OT. userCd={}, attdId={}, reqStart={}, reqEnd={}, overlapCount={}",
                            param.userCd(), param.attdId(), reqStart, reqEnd, overlapCount);
                    throw new ApiException(AttdErrorCode.ATTD_409_002);
                }
            }
        }

        // 7. 초과근무 기록 반영.
        //    - 03(생성) 또는 Attd_07 직접 등록 → 각 구간을 새 OT row로 INSERT.
        //    - 04(수정) → 기존 OT 행(TARGET_ID=OT_ID)을 요청 구간으로 UPDATE(단일 구간).
        //    수정도 위 2~6 검증(허용구간/겹침)을 그대로 거치므로 생성과 동일 규칙으로 재검증된다.
        if (isModify) {
            if (param.overtimes().size() != 1) {
                // 수정은 단일 구간만 허용한다(접수함은 요청당 1구간 전달). 그 외는 변조로 간주.
                log.warn("OT modify rejected - expected single segment. reqId={}, size={}",
                        reqRow.reqId(), param.overtimes().size());
                throw new ApiException(AttdErrorCode.ATTD_400_005);
            }
            if (reqRow.targetId() == null || reqRow.targetId().isEmpty()) {
                log.warn("OT modify rejected - missing TARGET_ID(OT_ID). reqId={}", reqRow.reqId());
                throw new ApiException(AttdErrorCode.ATTD_400_005);
            }
            OvertimeItemModel ot = param.overtimes().get(0);
            int[] stamp = reqStamps.get(0);
            int workMinutes = stamp[1] - stamp[0];
            int updatedOt = attd07Mapper.updateUserOvertimeModify(
                    reqRow.targetId(), param.gvCmpnyCd(), param.siteCd(), param.userCd(),
                    ot.startDate(), ot.startTime(), ot.endDate(), ot.endTime(),
                    workMinutes, param.gvUserCd());
            if (updatedOt == 0) {
                // 대상 OT가 스코프 밖이거나 이미 취소/삭제됨 → 잘못된 TARGET_ID 또는 변조.
                log.warn("OT modify rejected - target OT not updatable. reqId={}, otId={}",
                        reqRow.reqId(), reqRow.targetId());
                throw new ApiException(AttdErrorCode.ATTD_404_012);
            }
        } else {
            // Attd_07 직접 등록/수정(reqId=null).
            //   com-013-06 A: OT_ID 를 보유한 행은 in-place UPDATE, 미보유 행은 신규 INSERT.
            //   (UPDATE/INSERT 모두 위 2~6 검증(허용구간/겹침)을 그대로 거치므로 동일 규칙으로 재검증된다.)
            for (int i = 0; i < param.overtimes().size(); i++) {
                OvertimeItemModel ot = param.overtimes().get(i);
                int[] stamp = reqStamps.get(i);
                int workMinutes = stamp[1] - stamp[0];

                if (ot.otId() != null && !ot.otId().isBlank()) {
                    // 기존 OT 행 in-place 수정. ATTD_ID 까지 스코프에 포함해 타 일자/근태 행 이동(변조)을 차단한다.
                    //   대상이 스코프 밖이거나 이미 취소/삭제면 0행 → 잘못된 OT_ID 또는 변조로 간주하고 거부한다.
                    int updated = attd07Mapper.updateUserOvertimeDirect(
                            ot.otId(), param.gvCmpnyCd(), param.siteCd(), param.userCd(),
                            param.attdId(),
                            ot.startDate(), ot.startTime(), ot.endDate(), ot.endTime(),
                            workMinutes, param.gvUserCd());
                    if (updated == 0) {
                        log.warn("OT direct edit rejected - target OT not updatable. userCd={}, attdId={}, otId={}",
                                param.userCd(), param.attdId(), ot.otId());
                        throw new ApiException(AttdErrorCode.ATTD_404_012);
                    }
                } else {
                    // 신규 INSERT. 시퀀스는 row마다 가져와 동시 insert에서도 고유 ID를 보장한다.
                    String otId = attd07Mapper.selectOtId(param.gvCmpnyCd());
                    attd07Mapper.insertUserOvertime(
                            InsertUserOvertimeCommand.from(otId, param, ot, workMinutes));
                }
            }
        }

        // 8. reqId가 연결된 등록(요청 승인 관리 인박스 경유 승인)이면 해당 요청을 승인('02')으로 닫는다.
        //    Attd_07 직접 등록은 reqId=null 이므로 이 분기에 진입하지 않는다(기존 동작 불변).
        if (param.reqId() != null && !param.reqId().isEmpty()) {
            int reqUpdated = attd07Mapper.updateUserOvertimeReqApprove(
                    param.reqId(), param.gvCmpnyCd(), param.siteCd(), param.gvUserCd());
            if (reqUpdated == 0) {
                // lost-update / 동시 승인 충돌: @Transactional 경계로 OT INSERT까지 전체 롤백.
                log.warn("OT approve - REQ status changed concurrently. reqId={}", param.reqId());
                throw new ApiException(AttdErrorCode.ATTD_409_001);
            }
        }

        // 9. PRAFTA-027 - 초과근무 승인 처리 이력(TB_USER_ATTD_HIST) 기록.
        //    근태 보정 승인이 HIST_TYPE='01' 을 남기듯, OT 승인도 처리 이력을 남겨
        //    근무관리(Attd_07) 일자 상세의 "처리 이력" 팝업에 노출되게 한다.
        //    HIST.ATTD_ID 는 NOT NULL 이므로 그날 근태기록의 ATTD_ID 를 앵커로 사용한다
        //    (OT 는 출퇴근 기록이 있어야만 등록 가능 → 그날 ATTD_ID 가 항상 존재).
        //    등록된 각 OT 구간마다 1행씩, AFT_* 에 OT 시작/종료를 담는다.
        String histAttdId = attd07Mapper.selectAttdIdByDay(
                param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        if (histAttdId != null && !histAttdId.isEmpty()) {
            // 이력의 PROCESS_REASON 은 "관리자 처리사유"다. 본 엔드포인트는 두 경로를 겸하므로 구분한다.
            //   - Attd_07 직접 등록(reqId=null): reqReason = 관리자가 팝업 사유칸에 직접 입력한 값 → 그대로 기록.
            //   - 요청 승인(reqId 보유): 프론트가 보내는 reqReason 은 근로자가 쓴 신청 사유다. OT 승인에는
            //     관리자 사유 입력 UI 자체가 없으므로, 이를 처리사유로 적재하면 처리 이력에서 근로자 사유가
            //     관리자 사유로 둔갑한다. 근태 보정 승인과 동일하게 고정 라벨을 남긴다
            //     (근로자 사유는 처리 이력의 '요청 사유' 컬럼이 REQ 에서 직접 읽어 보여준다).
            boolean isReqApprove = param.reqId() != null && !param.reqId().isEmpty();
            String histProcessReason = isReqApprove ? OT_APPROVE_PROCESS_REASON : param.reqReason();
            for (OvertimeItemModel ot : param.overtimes()) {
                String histId = attd07Mapper.selectHistId(param.gvCmpnyCd());
                attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.forOvertimeApprove(
                        histId, histAttdId, param.gvCmpnyCd(), param.siteCd(), param.workYmd(),
                        ot.startDate(), ot.startTime(), ot.endDate(), ot.endTime(),
                        histProcessReason, param.gvUserCd()));
            }
        } else {
            // 운영 규칙상 도달 불가(OT 는 근태기록 없이 등록 불가). 핵심 동작(OT 등록)은 유지하고 이력만 생략한다.
            log.warn("OT approve - 처리 이력 생략: 그날 근태기록(ATTD_ID) 미존재. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
        }

        // PRAFTA-APP-021-3a(W2): 요청 승인(인박스 경유 = reqId 보유)일 때만 신청자 본인에게 결과 통보.
        //   Attd_07 직접 등록(reqId=null)은 신청자 통보 대상이 아니므로 건너뛴다. afterCommit 격리.
        if (reqRow != null) {
            try {
                approvalResultNotiService.notifyAttdResult(
                        param.gvCmpnyCd(), reqRow.siteCd(), reqRow.userCd(), reqRow.reqId(), true, param.gvUserCd());
            } catch (Exception e) {
                log.error("초과근무 승인 결과 통보 PUSH 적재 hook 실패(승인 영향 없음). reqId={}", reqRow.reqId(), e);
            }
        }
    }

    // ============================================================
    // com-016-E - 초과근무(OT) 다건 소프트삭제 (일자 상세 "초과근무 삭제")
    // ============================================================

    /** OT 삭제 사유 미입력 시 처리 이력에 남길 기본 문구. */
    private static final String DEFAULT_OT_DELETE_REASON = "관리자 직접 삭제";

    /** 초과근무 "요청 승인" 경로의 처리 이력 사유(관리자 사유 입력 UI 없음 → 고정 라벨). 근태 보정 승인과 동일 문구. */
    private static final String OT_APPROVE_PROCESS_REASON = "사용자 요청 승인";

    @Override
    @Transactional
    public void deleteUserOvertime(DeleteUserOvertimeParam param) {

        // SEC-017 - 사업장 접근 인가(User_03 원장 기반, 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // [보안 com-016-e-001] OT 삭제 경로를 형제 등록 경로(updateUserOvertimeRequests, 약 864~882)와
        //   대칭으로 하드닝한다. 기존 삭제 경로는 (a) 자기처리 정책 게이트(SELF_ATTD_APPRV_YN) 누락,
        //   (b) body nodeCd 신뢰로 인한 cross-node/마감 우회 여지가 있었다.
        //   → 대상 사용자의 서버 권위 노드를 산출해 body nodeCd 변조를 차단하고, 게이트/마감 인자를
        //     모두 권위 노드로 통일하며, 등록 경로와 동일한 자기처리/상위결재 정책 게이트를 적용한다.

        // 1) 권위 노드 산출 — 대상 사용자(param.userCd)의 서버 조회 노드(클라 body 미신뢰).
        String authoritativeNodeCd =
                attdCloseService.resolveUserNodeCd(param.gvCmpnyCd(), param.siteCd(), param.userCd());
        // 소속 미상(서버노드 null/blank)이면 노드 단위 정책을 적용할 수 없으므로 fail-closed 차단.
        if (authoritativeNodeCd == null || authoritativeNodeCd.isBlank()) {
            log.warn("delete-user-overtime rejected - 대상 사용자 소속 부서 미상(서버 노드 부재). cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        // body nodeCd 가 서버 권위 노드와 불일치하면 변조로 간주하고 차단(등록 경로 mismatch 방어와 동일 레벨).
        if (StringEqualsUtils.isMismatched(param.nodeCd(), authoritativeNodeCd)) {
            log.warn("delete-user-overtime rejected - body/서버 nodeCd mismatch(변조). cmpnyCd={}, siteCd={}, userCd={}, paramNode={}, serverNode={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.nodeCd(), authoritativeNodeCd);
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 2) 매니저 게이트 + 자기처리/상위결재 정책 게이트 — 게이트 인자 = 서버 권위 노드(클라 body 미신뢰).
        //    master/hr/safe 전사 통과는 ensureCanProcessAttdSelfPolicy 내부에서 유지된다.
        //    SELF_ATTD_APPRV_YN='N' 노드에서 본인 OT 자기삭제는 ATTD_403_003 으로 차단된다(등록 경로와 대칭).
        ensureCanProcessAttdSelfPolicy(param.gvAuthCd(), param.gvUserCd(), param.userCd(),
                param.gvCmpnyCd(), param.siteCd(), authoritativeNodeCd, true);

        // 3) 마감 가드 — 마감된 기간(부서)의 OT 삭제 차단 (게이트와 동일한 권위 노드 사용).
        ensureNotClosed(param.gvCmpnyCd(), param.siteCd(), authoritativeNodeCd, param.workYmd());

        // 4) 대상 사용자가 호출자의 회사/사이트 scope 안에 실재하는지 DB 로 재확인(IDOR 가드).
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("delete-user-overtime rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
        }

        // 5) 삭제 이력 앵커 — 그날 근태기록의 ATTD_ID(HIST.ATTD_ID 는 NOT NULL).
        //    OT 는 출퇴근 기록이 있어야만 등록되므로 정상 경로에서는 항상 존재한다.
        //    없으면(운영상 도달 불가) 핵심 동작(삭제)은 유지하고 이력만 생략한다.
        String histAttdId = attd07Mapper.selectAttdIdByDay(
                param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
        String reason = (param.reqReason() == null || param.reqReason().isBlank())
                ? DEFAULT_OT_DELETE_REASON : param.reqReason();

        // 6) otIds 루프 — 각 건을 scope 일치 + 활성 조건으로 소프트삭제한다.
        //    어느 한 건이라도 0행(스코프 밖/이미 삭제·취소)이면 ATTD_404_012 로 전체 롤백(부분삭제 금지).
        for (String otId : param.otIds()) {
            // 삭제 직전 OT 구간(시작/종료)을 이력 기록용으로 확보한다.
            DailyOvertimeResult target = attd07Mapper.selectOvertimeRowById(
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), otId);

            int deleted = attd07Mapper.deleteUserOvertimeById(
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), otId, param.gvUserCd());
            if (deleted == 0) {
                log.warn("delete-user-overtime rejected - OT not deletable. cmpnyCd={}, siteCd={}, userCd={}, otId={}",
                        param.gvCmpnyCd(), param.siteCd(), param.userCd(), otId);
                throw new ApiException(AttdErrorCode.ATTD_404_012);
            }

            // 7) 삭제 처리 이력(TB_USER_ATTD_HIST) 기록 — HIST_TYPE='13'(초과근무 삭제).
            //    AFT_* 에 방금 삭제된 OT 구간(실제 시작/종료)을 담아 일자 상세 "처리 이력"에 노출한다.
            if (histAttdId != null && !histAttdId.isEmpty() && target != null) {
                String histId = attd07Mapper.selectHistId(param.gvCmpnyCd());
                attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.forOvertimeDelete(
                        histId, histAttdId, param.gvCmpnyCd(), param.siteCd(), param.workYmd(),
                        target.actualStartDate(), target.actualStartTime(),
                        target.actualEndDate(), target.actualEndTime(),
                        reason, param.gvUserCd()));
            } else {
                log.warn("OT delete - 처리 이력 생략: ATTD_ID 또는 대상 OT 행 부재. userCd={}, workYmd={}, otId={}",
                        param.userCd(), param.workYmd(), otId);
            }

            log.info("초과근무 삭제 완료. otId={}, userCd={}, workYmd={}",
                    otId, param.userCd(), param.workYmd());
        }
    }

    /**
     * OT 칩 정합(2026-08-08): 일자상세 조회용 — 그날 확정 부분연차의 면제 구간을 표시용 뷰로 산출한다.
     *
     * <p>산출 경로는 OT 저장 검증(4-B)과 <b>완전히 동일</b>하다: 같은 매퍼 2본
     * ({@code selectLeaveExemptWindows} / {@code selectAllowedWindow}(+무스케줄 폴백)) →
     * {@code buildLeaveExemptSegments}. 조회와 검증이 다른 산식을 갖는 순간 칩 불일치가 재발하므로
     * 여기서 별도 계산을 추가하지 않는다.
     *
     * <p>windows 폴백까지 전부 null(근태·스케줄 모두 부재)이면 빈 리스트 — 그 경우 FE 칩 자체가
     * 뜨지 않아(실근태 없음) 무해하다. 조회 실패는 팝업 본체를 막지 않는다(표시 부가 정보).
     */
    private List<OtLeaveExemptWindowView> buildOtLeaveExemptWindows(DailyAttdDetailsParam param) {
        try {
            List<LeaveExemptWindowResult> rows = attd07Mapper.selectLeaveExemptWindows(
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            OvertimeAllowedWindowQuery query = new OvertimeAllowedWindowQuery(
                    param.gvCmpnyCd(), param.siteCd(), param.userCd(), param.workYmd());
            AllowedWindowResult windows = attd07Mapper.selectAllowedWindow(query);
            if (windows == null) {
                // 근무계획 미배정일 폴백 — OT 검증 경로(4.)와 동일
                windows = attd07Mapper.selectActualWindowNoSchedule(query);
            }
            if (windows == null) {
                return List.of();
            }
            List<int[]> segs = buildLeaveExemptSegments(param.workYmd(), rows, windows, param.userCd());
            List<OtLeaveExemptWindowView> out = new ArrayList<>(segs.size());
            for (int[] seg : segs) {
                out.add(OtLeaveExemptWindowView.fromStampRange(param.workYmd(), seg));
            }
            return out;
        } catch (RuntimeException e) {
            // 표시 부가 정보 산출 실패로 일자상세 전체를 막지 않는다(칩만 종전 FE 계산으로 폴백).
            log.warn("일자상세 - OT 면제 구간 산출 실패(칩 표시만 생략). userCd={}, workYmd={}",
                    param.userCd(), param.workYmd(), e);
            return List.of();
        }
    }

    /**
     * HB-08(D5): 연차 면제 구간 행을 workYmd 기준 분 stamp 구간으로 변환한다.
     * stamp origin 은 스케줄/실근태와 동일(workYmd-1 00:00 = 0).
     *
     * <p>★ 3차 재작업(§15-2, 2026-08-07): 환산은 <b>그날 원 스케줄을 프레임으로</b> 단일 진입점
     * ({@code PartialLeaveWindowUtils.exemptStampRange})에서만 수행한다. 스케줄 없이 환산하면
     * 야간 종료기준 반차({@code '0115'~'0430'} — 양 끝이 자정 이후라 행 안에 wrap 신호가 없다)가
     * 정확히 1440분 앞에 배치되어 면제가 빠지지 않는다(N-1, OT 수당 과다).
     *
     * <p>산출 실패(스케줄 프레임 부재·시각 비정상)는 <b>스킵하지 않는다</b>(§15-2-3 fail-open 금지) —
     * 그날 전체를 면제로 보아 OT 를 거부하고 WARN 을 남긴다.
     */
    private List<int[]> buildLeaveExemptSegments(String workYmd, List<LeaveExemptWindowResult> rows,
                                                 AllowedWindowResult windows, String userCd) {
        List<int[]> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        List<PartialLeaveWindowUtils.ScheduleSegment> schedule =
                PartialLeaveWindowUtils.scheduleSegments(
                        windows.plan1Start(), windows.plan1End(),
                        windows.plan2Start(), windows.plan2End());
        for (LeaveExemptWindowResult r : rows) {
            int[] range = PartialLeaveWindowUtils.exemptStampRange(
                    r.startTime(), r.endTime(), schedule);
            if (range == null) {
                log.warn("OT 등록가능범위 - 연차 면제 구간 환산 불가(그날 OT 보수 차단). userCd={}, workYmd={}, "
                                + "leave={}~{}, sch1={}~{}, sch2={}~{}",
                        userCd, workYmd, r.startTime(), r.endTime(),
                        windows.plan1Start(), windows.plan1End(),
                        windows.plan2Start(), windows.plan2End());
                out.add(PartialLeaveWindowUtils.fullDayBlockStampRange());
                continue;
            }
            out.add(range);
        }
        return out;
    }
}
