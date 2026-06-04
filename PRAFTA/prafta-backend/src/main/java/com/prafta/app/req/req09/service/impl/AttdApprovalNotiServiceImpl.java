package com.prafta.app.req.req09.service.impl;

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
import com.prafta.app.req.req09.AttdApprovalNotiConst;
import com.prafta.app.req.req09.service.AttdApprovalNotiService;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 근태 요청 결재 PUSH 생산자(outbox PENDING 적재) 구현 (PRAFTA-APP-009-2).
 *
 * <p>연차 {@code LeaveApprovalNotiServiceImpl} 미러. 근태 신청 hook 에서 호출된다.
 *
 * <p><b>트랜잭션 격리(PRAFTA-APP-009-001 보안 Medium 수정):</b> 과거에는 별도 트랜잭션 없이
 * 호출부({@code AppReq07ServiceImpl} 3 register)의 {@code @Transactional} 에 참여했다. 이 경우
 * outbox 적재 중 DB 예외(특히 dedupKey UNIQUE 충돌)가 발생하면 try-catch 로 흡수하더라도 커넥션이
 * 이미 {@code rollback-only} 로 마킹되어, 커밋 시 {@code UnexpectedRollbackException} 으로 요청+
 * 결재라인 INSERT 전체가 함께 롤백될 수 있었다.
 *
 * <p>이를 막기 위해 {@code notify*} 는 본 트랜잭션에 outbox INSERT 를 직접 실행하지 않고,
 * 적재 작업을 본 트랜잭션 <i>커밋 이후</i>({@code afterCommit})로 등록한다. 요청+결재라인이 확정
 * 커밋된 뒤에만 outbox 를 적재하므로,
 * <ul>
 *   <li>적재 실패가 본 흐름을 절대 롤백시키지 못하고(커밋이 이미 끝남),</li>
 *   <li>롤백된 요청에 PUSH 가 가는 일도 없다(커밋 성공 시에만 afterCommit 실행).</li>
 * </ul>
 * 활성 트랜잭션이 없으면(테스트/배치) 즉시 실행으로 폴백한다. afterCommit 콜백 내부의 실제 적재는
 * {@code REQUIRES_NEW} 새 트랜잭션 경계에서 수행되며(자기 프록시 경유), 콜백 내부에서 모든 예외를
 * 흡수해 호출자에게 전파하지 않는다.
 *
 * <p>대상 조회/본문 합성/outbox 적재는 신규 SQL 을 두지 않고 연차 공용 매퍼를 재사용한다.
 *  - 노드 관리자 조회: {@link LeaveApprovalNotiMapper#selectNodeAdmins}
 *  - 신청자명(평문 USER_NM): {@link LeaveApprovalNotiMapper#selectUserNm}
 *  - outbox 채번/INSERT: {@link LeaveDashboardMapper#selectNextNotiId} / {@link LeaveDashboardMapper#insertNotiOutbox}
 *
 * <p>본문의 신청자명은 평문 {@code USER_NM} 조회값이다(AES-GCM 복호화 호출 없음).
 * DATA_PAYLOAD 에는 평문 이름을 넣지 않고 라우팅 키만 직렬화한다(PII 한정 — BODY 에만).
 */
@Slf4j
@Service
public class AttdApprovalNotiServiceImpl implements AttdApprovalNotiService {

    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /**
     * 자기 프록시 참조 — afterCommit 콜백에서 {@code @Transactional(REQUIRES_NEW)} 메서드를
     * 프록시 경유로 호출하기 위함. {@code this} 직접 호출은 AOP 우회로 트랜잭션이 적용되지 않는다.
     * 자기 주입 순환을 끊기 위해 {@code @Lazy}.
     */
    private final AttdApprovalNotiService self;

    public AttdApprovalNotiServiceImpl(LeaveApprovalNotiMapper leaveApprovalNotiMapper,
                                       LeaveDashboardMapper leaveDashboardMapper,
                                       ObjectMapper objectMapper,
                                       @Lazy AttdApprovalNotiService self) {
        this.leaveApprovalNotiMapper = leaveApprovalNotiMapper;
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    // ── 진입부: afterCommit 등록(본 트랜잭션 미참여) ──

    @Override
    public void notifyAttdApprovalTurn(String cmpnyCd, String siteCd, String applicantUserCd,
                                       String reqId, int approvalStep, String approverUserCd,
                                       String insertNo) {
        // 적재에 필요한 값은 모두 인자로 캡처되어 있다(트랜잭션 종료 후 재조회 불필요).
        runAfterCommit(() -> self.runTurnOutbox(
                cmpnyCd, siteCd, applicantUserCd, reqId, approvalStep, approverUserCd, insertNo));
    }

    @Override
    public void notifyAttdApprovalRequest(String cmpnyCd, String siteCd, String applicantUserCd,
                                          String reqId, String insertNo) {
        runAfterCommit(() -> self.runRequestOutbox(cmpnyCd, siteCd, applicantUserCd, reqId, insertNo));
    }

    /**
     * 적재 작업을 본 트랜잭션 커밋 이후로 등록한다. 활성 트랜잭션이 없으면 즉시 실행(테스트/배치).
     * 등록 자체의 예외도 흡수해 호출 흐름(신청)에 절대 영향을 주지 않는다.
     */
    private void runAfterCommit(Runnable task) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 커밋 이후 실행 — 본 흐름은 이미 확정 커밋됨. 예외는 여기서 전부 흡수.
                        try {
                            task.run();
                        } catch (Exception e) {
                            log.error("[attdAprvNoti] afterCommit outbox 적재 실패(신청 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                // 활성 트랜잭션 없음(테스트/배치) — 즉시 실행 폴백.
                task.run();
            }
        } catch (Exception e) {
            // 등록/즉시실행 단계의 예외도 신청 흐름을 막지 않는다 — 로그만 남기고 흡수.
            log.error("[attdAprvNoti] PUSH 적재 등록 실패(신청 흐름 영향 없음)", e);
        }
    }

    // ── 실행부: REQUIRES_NEW 새 트랜잭션 경계(자기 프록시 경유 호출) ──

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runTurnOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                              String reqId, int approvalStep, String approverUserCd, String insertNo) {
        if (approverUserCd == null || approverUserCd.isBlank()) {
            log.info("[attdAprvNoti] 차례 도래 결재자 없음 — 적재 생략 (reqId={}, step={})", reqId, approvalStep);
            return;
        }
        String applicantNm = resolveUserNm(cmpnyCd, applicantUserCd);
        String title = AttdApprovalNotiConst.TURN_TITLE;
        String body = String.format(AttdApprovalNotiConst.TURN_BODY_FORMAT, applicantNm);
        String payload = buildTurnPayload(reqId, approvalStep, applicantUserCd);
        String dedupKey = "ATTD_TURN_" + reqId + "_" + approvalStep;

        try {
            insertOutbox(cmpnyCd, siteCd, approverUserCd, AttdApprovalNotiConst.NOTI_TYPE_APPROVAL_TURN,
                    title, body, payload, dedupKey, insertNo);
            log.info("[attdAprvNoti] 근태 결재 차례 도래 PUSH 적재 (reqId={}, step={}, approver={})",
                    reqId, approvalStep, approverUserCd);
        } catch (DuplicateKeyException dup) {
            // 동일 (요청×단계) 중복 적재 → 흡수(멱등). dedupKey UNIQUE 충돌은 정상 멱등 동작.
            log.info("[attdAprvNoti] 차례 도래 중복 적재 무시 (reqId={}, step={})", reqId, approvalStep);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runRequestOutbox(String cmpnyCd, String siteCd, String applicantUserCd,
                                 String reqId, String insertNo) {
        List<String> admins =
                leaveApprovalNotiMapper.selectNodeAdmins(cmpnyCd, siteCd, applicantUserCd);
        if (admins == null || admins.isEmpty()) {
            log.info("[attdAprvNoti] 자체근태승인 승인 요망 대상 노드 관리자 없음 (reqId={}, userCd={})",
                    reqId, applicantUserCd);
            return;
        }

        String applicantNm = resolveUserNm(cmpnyCd, applicantUserCd);
        String title = AttdApprovalNotiConst.REQUEST_TITLE;
        String body = String.format(AttdApprovalNotiConst.REQUEST_BODY_FORMAT, applicantNm);
        String payload = buildRequestPayload(reqId, applicantUserCd);

        int sent = 0;
        for (String targetUserCd : admins) {
            // 자기 알림 방지: 신청자 본인이 노드 관리자면 제외.
            if (targetUserCd == null || targetUserCd.equals(applicantUserCd)) {
                continue;
            }
            String dedupKey = "ATTD_REQ_" + reqId + "_" + targetUserCd;
            try {
                insertOutbox(cmpnyCd, siteCd, targetUserCd, AttdApprovalNotiConst.NOTI_TYPE_APPROVAL_REQUEST,
                        title, body, payload, dedupKey, insertNo);
                sent++;
            } catch (DuplicateKeyException dup) {
                // 동일 (요청×관리자) 중복 적재 → 흡수하고 다음 관리자 진행(멱등).
                log.info("[attdAprvNoti] 승인 요망 중복 적재 무시 (reqId={}, admin={})", reqId, targetUserCd);
            }
        }

        log.info("[attdAprvNoti] 자체근태승인 승인 요망 PUSH 적재 완료 (reqId={}, userCd={}, 대상수={})",
                reqId, applicantUserCd, sent);
    }

    /** outbox 1행 적재(SEND_STATUS='PENDING'). DuplicateKeyException 은 상위로 전파(적재부 루프/단건이 흡수). */
    private void insertOutbox(String cmpnyCd, String siteCd, String targetUserCd, String notiType,
                              String title, String body, String payload, String dedupKey, String insertNo) {
        String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
        NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
        outbox.setNotiId(notiId);
        outbox.setCmpnyCd(cmpnyCd);
        outbox.setSiteCd(siteCd);
        outbox.setTargetUserCd(targetUserCd);
        outbox.setNotiType(notiType);
        outbox.setChannel(AttdApprovalNotiConst.CHANNEL_PUSH);
        outbox.setTitle(title);
        outbox.setBody(body);
        outbox.setDataPayload(payload);
        outbox.setSendStatus(AttdApprovalNotiConst.SEND_STATUS_PENDING);
        outbox.setDedupKey(dedupKey);
        outbox.setInsertNo(insertNo);
        leaveDashboardMapper.insertNotiOutbox(outbox);
    }

    /** 신청자명 평문 조회. 미존재/스코프 밖이면 빈 문자열(본문 합성용 안전 폴백). */
    private String resolveUserNm(String cmpnyCd, String userCd) {
        String nm = leaveApprovalNotiMapper.selectUserNm(cmpnyCd, userCd);
        return (nm == null) ? "" : nm;
    }

    /** 차례 도래 DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildTurnPayload(String reqId, int approvalStep, String applicantUserCd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", AttdApprovalNotiConst.NOTI_TYPE_APPROVAL_TURN);
        data.put("reqId", reqId);
        data.put("approvalStep", approvalStep);
        data.put("applicantUserCd", applicantUserCd);
        return serialize(data);
    }

    /** 승인 요망 DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildRequestPayload(String reqId, String applicantUserCd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", AttdApprovalNotiConst.NOTI_TYPE_APPROVAL_REQUEST);
        data.put("reqId", reqId);
        data.put("applicantUserCd", applicantUserCd);
        return serialize(data);
    }

    private String serialize(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[attdAprvNoti] payload 직렬화 실패", e);
            return "{}";
        }
    }
}
