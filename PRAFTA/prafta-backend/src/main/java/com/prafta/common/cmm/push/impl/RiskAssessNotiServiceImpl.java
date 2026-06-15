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
import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.push.RiskAssessNotiConst;
import com.prafta.common.cmm.push.RiskAssessNotiService;
import com.prafta.common.util.AuthRoleUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가 검토요청 통보(M5) PUSH 생산자 구현 (PRAFTA-APP-021-3d).
 *
 * <p>{@code AttdApprovalNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 격리 패턴을 따른다.
 * 저장 트랜잭션이 확정 커밋된 뒤에만 outbox 를 적재한다(저장 본 흐름 롤백 금지).
 *
 * <p>수신 대상 = 사업장 안전관리자(safe 역할) ∪ 작성자 소속 노드 main/sub 관리자(§8-R 1).
 * {@code LeaveRefusalMapper.selectSiteRefusalAdmins}(역할 AUTH_CD 목록 ∪ 노드 어드민, DISTINCT)를
 * 재사용하되 역할 목록을 safe 만 전달해 합집합을 얻는다. 작성자 본인은 제외한다.
 */
@Slf4j
@Service
public class RiskAssessNotiServiceImpl implements RiskAssessNotiService {

    /** 역할 기반 대상 = 사업장 안전관리자(safe)만. 노드 어드민은 매퍼가 별도 합산. */
    private static final List<String> ADMIN_AUTH_CDS =
            List.of(AuthRoleUtils.AUTH_SAFETY_MANAGER);

    private final LeaveRefusalMapper leaveRefusalMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출(@Lazy 순환 차단). */
    private final RiskAssessNotiService self;

    public RiskAssessNotiServiceImpl(LeaveRefusalMapper leaveRefusalMapper,
                                     LeaveDashboardMapper leaveDashboardMapper,
                                     ObjectMapper objectMapper,
                                     @Lazy RiskAssessNotiService self) {
        this.leaveRefusalMapper = leaveRefusalMapper;
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Override
    public void notifyReviewRequested(String cmpnyCd, String siteCd, String assessmentCd,
                                      String requesterUserCd, String actorUserCd) {
        runAfterCommit(() -> self.runReviewRequestedOutbox(
                cmpnyCd, siteCd, assessmentCd, requesterUserCd, actorUserCd));
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
                            log.error("[riskAssessNoti] afterCommit outbox 적재 실패(저장 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[riskAssessNoti] PUSH 적재 등록 실패(저장 흐름 영향 없음)", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runReviewRequestedOutbox(String cmpnyCd, String siteCd, String assessmentCd,
                                         String requesterUserCd, String actorUserCd) {
        List<String> admins =
                leaveRefusalMapper.selectSiteRefusalAdmins(cmpnyCd, siteCd, requesterUserCd, ADMIN_AUTH_CDS);
        if (admins == null || admins.isEmpty()) {
            log.info("[riskAssessNoti] 검토요청 통보 대상(safe/노드 관리자) 없음 (assessmentCd={}, siteCd={})",
                    assessmentCd, siteCd);
            return;
        }
        String payload = buildPayload(assessmentCd);
        int sent = 0;
        for (String adminUserCd : admins) {
            // 자기 알림 방지: 작성자 본인이 safe/노드 관리자면 제외.
            if (adminUserCd == null || adminUserCd.isBlank() || adminUserCd.equals(requesterUserCd)) {
                continue;
            }
            String dedupKey = "RISK_REQ_" + assessmentCd + "_" + adminUserCd;
            try {
                String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(notiId);
                outbox.setCmpnyCd(cmpnyCd);
                outbox.setSiteCd(siteCd);
                outbox.setTargetUserCd(adminUserCd);
                outbox.setNotiType(RiskAssessNotiConst.NOTI_TYPE);
                outbox.setChannel(RiskAssessNotiConst.CHANNEL_PUSH);
                outbox.setTitle(RiskAssessNotiConst.TITLE);
                outbox.setBody(RiskAssessNotiConst.BODY);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(RiskAssessNotiConst.SEND_STATUS_PENDING);
                outbox.setDedupKey(dedupKey);
                outbox.setInsertNo(actorUserCd);
                leaveDashboardMapper.insertNotiOutbox(outbox);
                sent++;
            } catch (DuplicateKeyException dup) {
                // 동일 (평가×관리자) 중복 적재 → 흡수하고 다음 관리자 진행(멱등).
                log.info("[riskAssessNoti] 검토요청 중복 적재 무시 (assessmentCd={}, admin={})", assessmentCd, adminUserCd);
            }
        }
        log.info("[riskAssessNoti] 위험성평가 검토요청 PUSH 적재 완료 (assessmentCd={}, 대상수={})", assessmentCd, sent);
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String assessmentCd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", RiskAssessNotiConst.NOTI_TYPE);
        data.put("assessmentCd", assessmentCd);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[riskAssessNoti] payload 직렬화 실패 (assessmentCd={})", assessmentCd, e);
            return "{}";
        }
    }
}
