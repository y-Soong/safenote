package com.prafta.web.user.user01.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.attd.leaveflow.service.LeaveFlowService;
import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.PendingRequestTerminationResult;
import com.prafta.web.user.user01.service.UserPendingRequestTerminationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 대기요청 일괄 종결 공유 서비스 구현 (F1 / QT-11-7).
 *
 * <p>소속이동 발효({@link User01TransferExecutionServiceImpl})의 (e) 진행중 요청 일괄 반려/취소
 * 블록을 추출한 단일 출처다. 소속이동·비활성·탈퇴 세 경로가 동일한 상태 전이/원장 원복 시퀀스를
 * 공유하도록 하여 중복 구현을 방지한다.
 *
 * <p>진행중 요청 반려/취소·연차 원장 원복은 기존 인터랙티브 반려 서비스가 호출자(승인자) 권위·단건
 * REQ_ID 컨텍스트에 강결합되어 배치/훅 재사용이 불가하므로, 각 캐노니컬 상태 전이(컬럼 집합)를
 * 동일하게 미러하는 회사+사용자 스코프 일괄 UPDATE({@link UserTransferMapper})로 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPendingRequestTerminationServiceImpl implements UserPendingRequestTerminationService {

    private final UserTransferMapper userTransferMapper;
    private final LeaveFlowService leaveFlowService; // QT-11-7: 종료된 연차 요청의 원장 원복(정상 반려와 단일 출처).

    @Override
    @Transactional
    public PendingRequestTerminationResult terminateAllPendingFor(
            String cmpnyCd, String userCd, String reason, String actor) {

        // QT-11-7: 상태 UPDATE 는 연차 원장(use 행·GRANT USED_DAYS)을 건드리지 않는다. 종료되는 연차 요청의
        //   REQ_ID 를 UPDATE 전에 스냅샷해 두었다가, UPDATE 직후 정상 반려와 동일한 원복 시퀀스를 태운다.
        //   (스냅샷을 안 하면 REQ_STATUS='01' 조건이 깨져 대상을 다시 찾을 수 없다.)
        List<String> applicantLeaveReqIds = userTransferMapper.selectActiveLeaveReqIdsByApplicant(cmpnyCd, userCd);

        //   신청자(대상자)건 취소. REQ_TYPE 필터 없음 → 근태보정(01/02)·연차(05)·스케줄수정(10) 자동 포함.
        int reqCancelled = userTransferMapper.cancelActiveAttdReqByApplicant(cmpnyCd, userCd, reason, actor);
        //   승인대기 결재자(대상자)건 반려 — 대상 REQ_ID 스냅샷 후 본 요청/결재단계 동일 집합 키로 반려
        //   (순차 UPDATE 상호 무력화 방지).
        List<String> approverReqIds = userTransferMapper.selectActiveApproverReqIds(cmpnyCd, userCd);
        int reqRejected = 0;
        int stepRejected = 0;
        List<String> approverLeaveReqIds = List.of();
        if (approverReqIds != null && !approverReqIds.isEmpty()) {
            // 원복 대상(연차 사용 '05')만 추린다 — 상태 UPDATE 전에 확정해야 REQ_TYPE 재조회가 안전하다.
            approverLeaveReqIds = userTransferMapper.selectLeaveReqIdsIn(cmpnyCd, approverReqIds);
            reqRejected = userTransferMapper.rejectAttdReqByReqIds(cmpnyCd, approverReqIds, reason, actor);
            stepRejected = userTransferMapper.rejectApprovalStepsForApproverByReqIds(
                    cmpnyCd, userCd, approverReqIds, reason, actor);
        }

        //   QT-11-7: 종료된 연차 요청의 차감 원복(use 행 취소 + GRANT 재집계 + 가불 회수 + 시간차 재정산).
        //   ★ 결재자였을 뿐인 제3자(신청자)의 연차도 여기서 함께 복구된다 — 반려됐는데 차감만 남는 상태 방지.
        //   동일 트랜잭션(REQUIRED) — 원복 실패 시 호출자 전체 롤백(부분 처리 금지).
        int leaveRestored = 0;
        for (String reqId : concatDistinct(applicantLeaveReqIds, approverLeaveReqIds)) {
            leaveFlowService.restoreLeaveLedgerOnTerminate(cmpnyCd, reqId, reason, actor);
            leaveRestored++;
        }
        if (leaveRestored > 0) {
            log.info("대기요청 종결 - 종료된 연차 요청 원장 원복 {}건. userCd={}", leaveRestored, userCd);
        }
        int otCancelled = userTransferMapper.cancelActiveOvertimeByUser(cmpnyCd, userCd, actor);
        int leaveChgRejected = userTransferMapper.rejectActiveLeaveChangeByUser(cmpnyCd, userCd, reason, actor);

        log.info("대기요청 일괄 종결 - userCd={}, attdReq취소={}, attdReq반려={}, 결재단계반려={}, OT취소={}, 연차변경반려={}, 연차원복={}",
                userCd, reqCancelled, reqRejected, stepRejected, otCancelled, leaveChgRejected, leaveRestored);

        return new PendingRequestTerminationResult(
                reqCancelled, reqRejected, stepRejected, otCancelled, leaveChgRejected, leaveRestored);
    }

    /**
     * QT-11-7 — 두 REQ_ID 목록을 중복 없이 합친다(입력 순서 유지, null/빈값 방어).
     * 대상자가 신청자이면서 동시에 결재자인 요청은 이론상 없지만(본인 결재는 자동승인),
     * 원복이 2회 실행되지 않도록 방어한다(원복 자체는 멱등이나 불필요한 재정산 호출을 피한다).
     */
    private static List<String> concatDistinct(List<String> a, List<String> b) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (a != null) {
            for (String s : a) {
                if (s != null && !s.isBlank()) merged.add(s);
            }
        }
        if (b != null) {
            for (String s : b) {
                if (s != null && !s.isBlank()) merged.add(s);
            }
        }
        return new ArrayList<>(merged);
    }
}
