package com.prafta.web.attd.attd07.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
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
import com.prafta.web.attd.attd07.application.model.OvertimeItemModel;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
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
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyOvertimeResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.result.UserAttdReqResult;
import com.prafta.web.attd.attd07.service.Attd07Service;
import com.prafta.web.attd.attd07.util.AttdReqTypeUtils;
import com.prafta.web.attd.attd07.util.AttdScheduleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd07ServiceImpl implements Attd07Service {

    private final Attd07Mapper attd07Mapper;

    @Override
    public AttdRecordListResponse getMonthlyAttdList(MonthlyAttdListParam param) {

        List<MonthlyAttdListResult> attdRecordResultList = attd07Mapper.selectMonthlyAttdList(MonthlyAttdListQuery.from(param));
        List<MonthlyAttdReqSummaryResult> monthlyAttdReqSummaryResultList = attd07Mapper.selectMonthlyAttdReqSummary(MonthlyAttdListQuery.from(param));

        return AttdRecordListResponse.builder()
                .attdRecordResultList(attdRecordResultList)
                .monthlyAttdReqSummaryResultList(monthlyAttdReqSummaryResultList)
                .build();
    }

    @Override
    @Transactional
    public void updateUserAttdInfos(UpdateUserAttdInfosParam param) {
        for (UpdateUserAttdInfosModel model : param.updateUserAttdInfosModelList()) {
        	String attdId = "";

        	if(model.attdId() != null) {
        		attdId = model.attdId();
        	} else {
        		attdId = attd07Mapper.selectAttdId(model.gvCmpnyCd());
        	}

            attd07Mapper.updateUserAttdInfos(UpdateUserAttdInfosCommand.from(attdId, model));

            String histId = attd07Mapper.selectHistId(model.gvCmpnyCd());

            attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.from(histId, attdId, model));
        }
    }

    @Override
    public DailyAttdDetailsResponse getDailyAttdDetails(DailyAttdDetailsParam param) {

        // SEC-019 - 매니저 전용 게이트.
        // AttdDayDetailPop 은 정책서 §14.1의 관리자 화면(근태 현황 조회)에서 호출되는 일자 상세 팝업이다.
        // 일반 작업자가 본 endpoint로 타인의 출퇴근/OT/PII(userNm/userId)에 접근하지 못하도록
        // JWT 기반 gvAuthCd를 사용해 게이트한다. body 위조로 권한 escalation을 할 수 없다.
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("daily-attd-details rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // SEC-019 - cross-user IDOR 재검증.
        // Param.from 단계에서 body siteCd ↔ JWT gv_siteCd 일치는 이미 검증 완료.
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

        List<MonthlyAttdReqResult> monthlyAttdReqResultList = attd07Mapper.selectMonthlyAttdReq(DailyAttdDetailsQuery.from(param));

        // PRAFTA-003-6: 해당 일자에 등록된 초과근무 목록을 함께 조회한다.
        List<DailyOvertimeResult> dailyOvertimeResultList = attd07Mapper.selectDailyOvertimeList(DailyAttdDetailsQuery.from(param));

        return DailyAttdDetailsResponse.builder()
                .dailyAttdDetailsResult(dailyAttdDetailsResult)
                .dailyAttdDetailHistoryResultList(dailyAttdDetailHistoryResultList)
                .monthlyAttdReqResultList(monthlyAttdReqResultList)
                .dailyOvertimeResultList(dailyOvertimeResultList)
                .build();
    }

    @Override
    @Transactional
    public void dailyAttdDetailDelete(DailyAttdDetailDeleteParam param) {
        DailyAttdDetailDeleteCommand command = DailyAttdDetailDeleteCommand.from(param);

        attd07Mapper.insertDailyAttdDetailDeleteHist(command);
        attd07Mapper.dailyAttdDetailDelete(command);
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

        // [보안 재작업] SEC-015 - 매니저 전용 게이트 (prafta-009 범위 외 발견 결함 보완).
        // 근태 요청 승인은 일반 작업자가 호출해선 안 된다. 게이트가 없으면 동일 회사 내
        // 권한 없는 사용자가 타인 근태 수정 요청을 승인할 수 있다. 역할 검사는 JWT 기반
        // gvAuthCd를 사용하므로 body 위조로 권한 escalation을 할 수 없다.
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("approve rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // 2. 대기(pending) 상태의 요청만 승인 가능 (defence-in-depth - UPDATE 측에서도 REQ_STATUS='REQUESTED'로 필터함).
        if (!"REQUESTED".equals(reqRow.reqStatus())) {
            log.warn("approve rejected - REQ already processed. reqId={}, status={}",
                    reqRow.reqId(), reqRow.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 3. 자기 승인 차단 (REQ.USER_CD == 요청자의 gvUserCd). 
//        if (reqRow.userCd() != null && reqRow.userCd().equals(param.gvUserCd())) {
//            log.warn("approve rejected - self approval. reqId={}, userCd={}",
//                    reqRow.reqId(), reqRow.userCd());
//            throw new ApiException(AttdErrorCode.ATTD_403_001);
//        }

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

        // 5. TARGET_ID(근태 수정 경로의 ATTD_ID)를 결정한다.
        //    우선순위: REQ.TARGET_ID -> 기존 MGMT row -> 신규 시퀀스 값.
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

        // 6. 서버 권위 키 필드를 사용해 merge/hist 모델을 빌드한다.
        //    body로 전달된 출퇴근 값은 작업자가 기록 요청한 값이므로 그대로 사용한다.
        UpdateUserAttdInfosModel model = new UpdateUserAttdInfosModel(
            targetId
            , reqRow.siteCd()
            , reqRow.nodeCd()
            , reqRow.userCd()
            , null                       // userId는 mapper에서 미사용
            , reqRow.workSeq()
            , reqRow.workYmd()

            , param.oriCheckInDate()
            , param.oriCheckInTime()
            , param.oriCheckOutDate()
            , param.oriCheckOutTime()

            , param.checkInDate()
            , param.checkInTime()
            , param.checkInMethod()
            , param.checkOutDate()
            , param.checkOutTime()
            , param.checkOutMethod()
            , param.processComment()     // TB_USER_ATTD_HIST.PROCESS_REASON으로 저장됨
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );

        // 7. TB_USER_ATTD_MGMT MERGE.
        attd07Mapper.updateUserAttdInfos(UpdateUserAttdInfosCommand.from(targetId, model));

        // 8. TB_USER_ATTD_HIST INSERT (HIST_TYPE='01').
        String histId = attd07Mapper.selectHistId(param.gvCmpnyCd());
        attd07Mapper.insertUserAttdInfos(InsertUserAttdHistsCommand.from(histId, targetId, model));

        // 9. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향을 받아야 한다 (REQ_STATUS='REQUESTED' 가드).
        int updated = attd07Mapper.updateUserAttdReqApprove(UpdateUserAttdRequestCommand.from(targetId, param));
        if (updated == 0) {
            // lost-update / 동시 승인 충돌: @Transactional 경계로 전체 롤백.
            log.warn("approve rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }
    }

    // ============================================================
    // PRAFTA-008 - 근태 요청 반려
    // ============================================================

    @Override
    @Transactional
    public void rejectUserAttdRequest(RejectUserAttdRequestParam param) {

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

        // [보안 재작업] SEC-015 - 매니저 전용 게이트. 근태 요청 반려는 일반 작업자가
        // 호출해선 안 된다. 역할 검사는 JWT 기반 gvAuthCd를 사용하므로 body 위조로
        // 권한 escalation을 할 수 없다 (rejectUserOvertimeRequest 와 동일 패턴).
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("reject rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // 2. 대기(pending) 상태의 요청만 반려 가능 (UPDATE 측에서도 REQ_STATUS='REQUESTED'로 필터함).
        if (!"REQUESTED".equals(reqRow.reqStatus())) {
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
        //    - 수정요청(ATTD_MODIFY): REQ.TARGET_ID의 ATTD_ID를 그대로 사용한다.
        //    - 생성요청(ATTD_CREATE): TARGET_ID가 NULL이므로 시퀀스에서 새 ATTD_ID를 발급한다.
        //      반려는 미반영이므로 TB_USER_ATTD_MGMT에는 INSERT하지 않고 HIST 행에만 사용한다.
        String histAttdId;
        if (reqRow.targetId() != null && !reqRow.targetId().isEmpty()) {
            histAttdId = reqRow.targetId();
        } else {
            histAttdId = attd07Mapper.selectAttdId(param.gvCmpnyCd());
        }

        // 5. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향을 받아야 한다 (REQ_STATUS='REQUESTED' 가드).
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
    }

    // ============================================================
    // PRAFTA-010 - 초과근무 요청 반려
    // ============================================================

    @Override
    @Transactional
    public void rejectUserOvertimeRequest(RejectUserOvertimeRequestParam param) {

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

        // SEC-015 - 매니저 전용 게이트. 역할 검사는 JWT 기반 gvAuthCd를 사용하므로
        // body 위조로 권한 escalation을 할 수 없다.
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("OT reject rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // 2. 대기(pending) 상태의 요청만 반려 가능 (UPDATE 측에서도 REQ_STATUS='REQUESTED'로 필터함).
        if (!"REQUESTED".equals(reqRow.reqStatus())) {
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

        // 3. TB_USER_ATTD_REQ UPDATE - 정확히 1행만 영향을 받아야 한다 (REQ_STATUS='REQUESTED' 가드).
        //    HIST 미기록 - 초과근무 반려는 처리 컬럼 갱신만 수행한다.
        int updated = attd07Mapper.updateUserAttdReqReject(RejectUserAttdRequestCommand.from(param));
        if (updated == 0) {
            // lost-update / 동시 처리 충돌: @Transactional 경계로 전체 롤백.
            log.warn("OT reject rejected - REQ status changed concurrently. reqId={}", reqRow.reqId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        log.info("초과근무 요청 반려 완료. reqId={}", reqRow.reqId());
    }

    // ============================================================
    // PRAFTA-003 - 초과근무(Overtime) 등록
    // ============================================================

    @Override
    @Transactional
    public void updateUserOvertimeRequests(UpdateUserOvertimeRequestParam param) {

        // SEC-015 - 매니저 전용 게이트. OT 등록은 일반 작업자에 의해 호출되어선 안 된다
        // (작업자는 요청 흐름을 거친다). 역할 검사는 JWT 기반 gvAuthCd를 사용하므로
        // body 위조로 권한 escalation을 할 수 없다.
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("OT register rejected - insufficient privilege. userCd={}, authCd={}",
                    param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // SEC-016 - 자기 등록(self-registration) 차단. 매니저는 본인의 OT를 등록해선 안 된다.
        // 이는 작업자 요청 감사 추적을 우회하는 행위이다.
//        if (param.userCd() != null && param.userCd().equals(param.gvUserCd())) {
//            log.warn("OT register rejected - self registration not allowed. userCd={}",
//                    param.userCd());
//            throw new ApiException(AttdErrorCode.ATTD_403_003);
//        }

        // SEC-017 (a) - 대상 사용자가 호출자의 회사/사이트 scope 안에 존재해야 한다.
        int userExists = attd07Mapper.selectUserExistInCmpnySite(
                param.gvCmpnyCd(), param.siteCd(), param.userCd());
        if (userExists <= 0) {
            log.warn("OT register rejected - target user not in scope. cmpnyCd={}, siteCd={}, userCd={}",
                    param.gvCmpnyCd(), param.siteCd(), param.userCd());
            throw new ApiException(AttdErrorCode.ATTD_404_011);
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
        if (param.reqId() != null && !param.reqId().isEmpty()) {
            UserAttdReqResult reqRow = attd07Mapper.selectUserAttdReqByReqId(
                    param.reqId(), param.gvCmpnyCd());
            if (reqRow == null) {
                log.warn("OT register rejected - reqId not found. reqId={}, cmpnyCd={}",
                        param.reqId(), param.gvCmpnyCd());
                throw new ApiException(AttdErrorCode.ATTD_404_001);
            }
            if (StringEqualsUtils.isMismatched(param.userCd(), reqRow.userCd())
                    || StringEqualsUtils.isMismatched(param.siteCd(), reqRow.siteCd())) {
                log.warn("OT register rejected - reqId scope mismatch. reqId={}, paramUser={}, reqUser={}, paramSite={}, reqSite={}",
                        reqRow.reqId(),
                        param.userCd(), reqRow.userCd(),
                        param.siteCd(), reqRow.siteCd());
                throw new ApiException(AttdErrorCode.ATTD_400_005);
            }
        }

        // 1. defence-in-depth - controller에서 이미 @NotEmpty 검증 완료.
        if (param.overtimes() == null || param.overtimes().isEmpty()) {
            log.warn("OT register rejected - empty overtimes list. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_010);
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

        // 4. 스케줄과 표준화된 actual 구간을 로드한다.
        AllowedWindowResult windows = attd07Mapper.selectAllowedWindow(
                OvertimeAllowedWindowQuery.from(param));
        if (windows == null) {
            log.warn("OT register rejected - no schedule row. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_404_010);
        }

        // PRAFTA-011 - WORK_SEQ 인덱스를 보존한 구간 배열을 사용한다.
        //   schBySeq[1]/[2] = 1·2구간 스케줄, stdBySeq[1]/[2] = 1·2구간 표준화 actual.
        int[][] schBySeq = AttdScheduleUtils.buildScheduledSegmentsBySeq(param.workYmd(), windows);
        int[][] stdBySeq = AttdScheduleUtils.buildStandardizedSegmentsBySeq(param.workYmd(), windows);

        // PRAFTA-003-1 (Q1) - 해당 일자에 완료된 근무 기록이 없는 경우, 별도 에러 코드로 분리.
        // 완료된 actual 근무 구간이 없으면 "스케줄 외 시간(outside of schedule)" 윈도우를
        // 도출할 수 없으므로, OT 범위 자체가 잘못되었다고 시사하는 대신 "출퇴근 기록 누락"이라는
        // 사전조건이 누락되었다는 점을 호출자에게 알린다.
        if (stdBySeq[1] == null && stdBySeq[2] == null) {
            log.warn("OT register rejected - no standardized actual work segments. userCd={}, workYmd={}",
                    param.userCd(), param.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_014);
        }

        // 5. PRAFTA-011 - 등록가능시간을 구간별로 분리 계산한다.
        //    각 WORK_SEQ 에 대해 "그 구간 actual - 그 구간 schedule" 차집합을 구하고,
        //    매칭되는 스케줄이 없는 구간은 그 구간 근무 전체를 등록가능으로 본다.
        //    구간별 결과를 모두 합쳐 최종 allowed 윈도우를 만든다.
        //    1구간/2구간 actual 은 (오버나이트 보정 포함) 서로 겹치지 않도록 stamp 되어 있다.
        List<int[]> allowedAll = new ArrayList<>(2);
        for (int seq = 1; seq <= 2; seq++) {
            int[] actSeg = stdBySeq[seq];
            if (actSeg == null) {
                // 해당 구간의 actual 근무 기록이 없으면 등록가능시간도 없다.
                continue;
            }

            List<int[]> actList = new ArrayList<>(1);
            actList.add(actSeg);

            int[] schSeg = schBySeq[seq];
            List<int[]> seqAllowed;
            if (schSeg == null) {
                // 매칭 스케줄 없는 구간 - 그 구간 근무 전체를 등록가능으로 본다.
                seqAllowed = IntervalUtils.merge(actList);
            } else {
                List<int[]> schList = new ArrayList<>(1);
                schList.add(schSeg);
                // 구간 actual - 구간 schedule 차집합.
                seqAllowed = IntervalUtils.subtract(
                        IntervalUtils.merge(actList), IntervalUtils.merge(schList));
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

        // 6-1. PRAFTA-009-001 - 기존 활성 OT 행과의 중복(시간 구간 겹침) 검사.
        //      INSERT 루프 진입 전에 요청된 모든 OT 구간을 DB의 기존 활성 행과 대조한다.
        //      하나라도 겹치면 부분 INSERT 없이 전체 거부한다.
        //      이 화면은 항상 ATTD_ID 를 보유하므로 ATTD_ID 기준으로 검사한다.
        //      겹침 판정/NULL 종료 처리 규칙은 selectOverlappingOvertimeCount 참조.
        if (param.attdId() != null && !param.attdId().isEmpty()) {
            for (OvertimeItemModel ot : param.overtimes()) {
                String reqStart = ot.startDate() + ot.startTime();
                String reqEnd = ot.endDate() + ot.endTime();

                int overlapCount = attd07Mapper.selectOverlappingOvertimeCount(
                        param.gvCmpnyCd(), param.siteCd(), param.userCd(),
                        param.attdId(), reqStart, reqEnd);

                if (overlapCount > 0) {
                    log.warn("OT register rejected - overlaps existing active OT. userCd={}, attdId={}, reqStart={}, reqEnd={}, overlapCount={}",
                            param.userCd(), param.attdId(), reqStart, reqEnd, overlapCount);
                    throw new ApiException(AttdErrorCode.ATTD_409_002);
                }
            }
        }

        // 7. 각 OT row를 INSERT. 시퀀스는 row마다 가져와 동시 insert에서도 고유 ID를 보장한다.
        for (int i = 0; i < param.overtimes().size(); i++) {
            OvertimeItemModel ot = param.overtimes().get(i);
            int[] stamp = reqStamps.get(i);
            int workMinutes = stamp[1] - stamp[0];

            String otId = attd07Mapper.selectOtId(param.gvCmpnyCd());
            attd07Mapper.insertUserOvertime(
                    InsertUserOvertimeCommand.from(otId, param, ot, workMinutes));
        }
    }
}
