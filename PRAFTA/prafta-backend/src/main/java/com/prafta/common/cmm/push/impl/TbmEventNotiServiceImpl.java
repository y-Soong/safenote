package com.prafta.common.cmm.push.impl;

import java.util.LinkedHashMap;
import java.util.List;
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
import com.prafta.common.cmm.push.TbmEventNotiConst;
import com.prafta.common.cmm.push.TbmEventNotiService;
import com.prafta.common.cmm.push.mapper.PushTargetMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * TBM 교육 시작/종료 통보(W3) PUSH 생산자 구현 (PRAFTA-APP-021-3b).
 *
 * <p>{@code AttdApprovalNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 격리 패턴을 따른다.
 * 전이 트랜잭션이 확정 커밋된 뒤에만 입실 참석자 outbox 를 적재한다(전이 본 흐름 롤백 금지).
 * 수신 대상 = 실제 입실(enter)한 참석자 USER_CD 목록(§8-R 2). DATA_PAYLOAD 는 라우팅 키만.
 */
@Slf4j
@Service
public class TbmEventNotiServiceImpl implements TbmEventNotiService {

    private final PushTargetMapper pushTargetMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출(@Lazy 순환 차단). */
    private final TbmEventNotiService self;

    public TbmEventNotiServiceImpl(PushTargetMapper pushTargetMapper,
                                   LeaveDashboardMapper leaveDashboardMapper,
                                   ObjectMapper objectMapper,
                                   @Lazy TbmEventNotiService self) {
        this.pushTargetMapper = pushTargetMapper;
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    // ── 진입부: afterCommit 등록(본 트랜잭션 미참여) ──

    @Override
    public void notifyTbmStarted(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd) {
        runAfterCommit(() -> self.runTbmStartedOutbox(cmpnyCd, siteCd, sessionCd, actorUserCd));
    }

    @Override
    public void notifyTbmCompleted(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd) {
        runAfterCommit(() -> self.runTbmCompletedOutbox(cmpnyCd, siteCd, sessionCd, actorUserCd));
    }

    private void runAfterCommit(Runnable task) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            task.run();
                        } catch (Exception e) {
                            log.error("[tbmEventNoti] afterCommit outbox 적재 실패(전이 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[tbmEventNoti] PUSH 적재 등록 실패(전이 흐름 영향 없음)", e);
        }
    }

    // ── 실행부: REQUIRES_NEW 새 트랜잭션 경계(자기 프록시 경유 호출) ──

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runTbmStartedOutbox(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd) {
        fanOut(cmpnyCd, siteCd, sessionCd, actorUserCd,
                TbmEventNotiConst.NOTI_TYPE_TBM_STARTED,
                TbmEventNotiConst.STARTED_TITLE, TbmEventNotiConst.STARTED_BODY, "TBM_STARTED");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runTbmCompletedOutbox(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd) {
        fanOut(cmpnyCd, siteCd, sessionCd, actorUserCd,
                TbmEventNotiConst.NOTI_TYPE_TBM_COMPLETED,
                TbmEventNotiConst.COMPLETED_TITLE, TbmEventNotiConst.COMPLETED_BODY, "TBM_COMPLETED");
    }

    /** 입실 참석자 전원에게 1건씩 적재. dedupKey = {dedupPrefix}_{sessionCd}_{userCd}(멱등). */
    private void fanOut(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd,
                        String notiType, String title, String body, String dedupPrefix) {
        List<String> attendees = pushTargetMapper.selectTbmEnteredUserCds(cmpnyCd, sessionCd);
        if (attendees == null || attendees.isEmpty()) {
            log.info("[tbmEventNoti] {} 통보 대상(입실 참석자) 없음 — 적재 생략 (sessionCd={})", notiType, sessionCd);
            return;
        }
        String payload = buildPayload(notiType, sessionCd);
        int sent = 0;
        for (String targetUserCd : attendees) {
            if (targetUserCd == null || targetUserCd.isBlank()) {
                continue;
            }
            String dedupKey = dedupPrefix + "_" + sessionCd + "_" + targetUserCd;
            try {
                String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(notiId);
                outbox.setCmpnyCd(cmpnyCd);
                outbox.setSiteCd(siteCd);
                outbox.setTargetUserCd(targetUserCd);
                outbox.setNotiType(notiType);
                outbox.setChannel(TbmEventNotiConst.CHANNEL_PUSH);
                outbox.setTitle(title);
                outbox.setBody(body);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(TbmEventNotiConst.SEND_STATUS_PENDING);
                outbox.setDedupKey(dedupKey);
                outbox.setInsertNo(actorUserCd);
                leaveDashboardMapper.insertNotiOutbox(outbox);
                sent++;
            } catch (DuplicateKeyException dup) {
                // 동일 (세션×참석자) 중복 적재 → 흡수하고 다음 참석자 진행(멱등).
                log.info("[tbmEventNoti] {} 중복 적재 무시 (sessionCd={}, target={})", notiType, sessionCd, targetUserCd);
            }
        }
        log.info("[tbmEventNoti] {} PUSH 적재 완료 (sessionCd={}, 대상수={})", notiType, sessionCd, sent);
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String notiType, String sessionCd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", notiType);
        data.put("sessionCd", sessionCd);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[tbmEventNoti] payload 직렬화 실패 (sessionCd={})", sessionCd, e);
            return "{}";
        }
    }
}
