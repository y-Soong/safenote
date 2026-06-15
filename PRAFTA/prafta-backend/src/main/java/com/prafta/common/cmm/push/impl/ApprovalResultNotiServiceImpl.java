package com.prafta.common.cmm.push.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.push.ApprovalResultNotiConst;
import com.prafta.common.cmm.push.ApprovalResultNotiService;

import lombok.extern.slf4j.Slf4j;

/**
 * 결재 결과 통보(W2) PUSH 생산자 구현 (PRAFTA-APP-021-3a).
 *
 * <p>{@code AttdApprovalNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 격리 패턴을 그대로 따른다.
 * 본 결재 트랜잭션이 확정 커밋된 뒤에만 outbox 를 적재하므로, 적재 실패가 결재 본 흐름을 절대
 * 롤백시키지 못하고(커밋이 이미 끝남), 롤백된 결재에 PUSH 가 가는 일도 없다(커밋 성공 시에만 실행).
 *
 * <p>본문은 최소(승인/반려 여부만). 반려 사유 전문·PII·날짜는 BODY/payload 에 미포함한다(§8-R 4).
 * DATA_PAYLOAD 는 라우팅 키(type/reqId)만 직렬화한다.
 */
@Slf4j
@Service
public class ApprovalResultNotiServiceImpl implements ApprovalResultNotiService {

    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출하기 위함(@Lazy 로 순환 차단). */
    private final ApprovalResultNotiService self;

    public ApprovalResultNotiServiceImpl(LeaveDashboardMapper leaveDashboardMapper,
                                         ObjectMapper objectMapper,
                                         @Lazy ApprovalResultNotiService self) {
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    // ── 진입부: afterCommit 등록(본 트랜잭션 미참여) ──

    @Override
    public void notifyLeaveResult(String cmpnyCd, String siteCd, String applicantUserCd,
                                  String reqId, boolean approved, String actorUserCd) {
        runAfterCommit(() -> self.runLeaveResultOutbox(
                cmpnyCd, siteCd, applicantUserCd, reqId, approved, actorUserCd));
    }

    @Override
    public void notifyAttdResult(String cmpnyCd, String siteCd, String applicantUserCd,
                                 String reqId, boolean approved, String actorUserCd) {
        runAfterCommit(() -> self.runAttdResultOutbox(
                cmpnyCd, siteCd, applicantUserCd, reqId, approved, actorUserCd));
    }

    /**
     * 적재 작업을 본 트랜잭션 커밋 이후로 등록한다. 활성 트랜잭션이 없으면 즉시 실행(테스트/배치).
     * 등록 자체의 예외도 흡수해 결재 본 흐름에 절대 영향을 주지 않는다.
     */
    private void runAfterCommit(Runnable task) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            task.run();
                        } catch (Exception e) {
                            log.error("[aprvResultNoti] afterCommit outbox 적재 실패(결재 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[aprvResultNoti] PUSH 적재 등록 실패(결재 흐름 영향 없음)", e);
        }
    }

    // ── 실행부: REQUIRES_NEW 새 트랜잭션 경계(자기 프록시 경유 호출) ──

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runLeaveResultOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                                     String reqId, boolean approved, String actorUserCd) {
        String notiType = approved
                ? ApprovalResultNotiConst.NOTI_TYPE_LEAVE_APPROVED
                : ApprovalResultNotiConst.NOTI_TYPE_LEAVE_REJECTED;
        String body = approved ? ApprovalResultNotiConst.BODY_APPROVED : ApprovalResultNotiConst.BODY_REJECTED;
        String dedupKey = "LV_RESULT_" + reqId + "_" + (approved ? "APPROVED" : "REJECTED");
        insertOrSkip(cmpnyCd, siteCd, applicantUserCd, notiType,
                ApprovalResultNotiConst.LEAVE_TITLE, body, reqId, dedupKey, actorUserCd, "연차");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runAttdResultOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                                    String reqId, boolean approved, String actorUserCd) {
        String notiType = approved
                ? ApprovalResultNotiConst.NOTI_TYPE_ATTD_APPROVED
                : ApprovalResultNotiConst.NOTI_TYPE_ATTD_REJECTED;
        String body = approved ? ApprovalResultNotiConst.BODY_APPROVED : ApprovalResultNotiConst.BODY_REJECTED;
        String dedupKey = "ATTD_RESULT_" + reqId + "_" + (approved ? "APPROVED" : "REJECTED");
        insertOrSkip(cmpnyCd, siteCd, applicantUserCd, notiType,
                ApprovalResultNotiConst.ATTD_TITLE, body, reqId, dedupKey, actorUserCd, "근태/OT");
    }

    /** 신청자 1인 outbox 적재(PENDING). dedupKey UNIQUE 충돌은 멱등 흡수. 신청자 코드가 비면 적재 생략. */
    private void insertOrSkip(String cmpnyCd, String siteCd, String applicantUserCd, String notiType,
                              String title, String body, String reqId, String dedupKey,
                              String actorUserCd, String logTag) {
        if (applicantUserCd == null || applicantUserCd.isBlank()) {
            log.info("[aprvResultNoti] {} 결과 통보 대상(신청자) 없음 — 적재 생략 (reqId={})", logTag, reqId);
            return;
        }
        String payload = buildPayload(notiType, reqId);
        try {
            String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(notiId);
            outbox.setCmpnyCd(cmpnyCd);
            outbox.setSiteCd(siteCd);
            outbox.setTargetUserCd(applicantUserCd);
            outbox.setNotiType(notiType);
            outbox.setChannel(ApprovalResultNotiConst.CHANNEL_PUSH);
            outbox.setTitle(title);
            outbox.setBody(body);
            outbox.setDataPayload(payload);
            outbox.setSendStatus(ApprovalResultNotiConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(actorUserCd);
            leaveDashboardMapper.insertNotiOutbox(outbox);
            log.info("[aprvResultNoti] {} 결재 결과 PUSH 적재 (reqId={}, type={}, target={})",
                    logTag, reqId, notiType, applicantUserCd);
        } catch (DuplicateKeyException dup) {
            // 동일 (요청×결과) 중복 적재 → 멱등 흡수.
            log.info("[aprvResultNoti] {} 결과 통보 중복 적재 무시 (reqId={}, type={})", logTag, reqId, notiType);
        }
    }

    /** DATA_PAYLOAD(라우팅 키만, PII/사유 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String notiType, String reqId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", notiType);
        data.put("reqId", reqId);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[aprvResultNoti] payload 직렬화 실패 (reqId={})", reqId, e);
            return "{}";
        }
    }
}
