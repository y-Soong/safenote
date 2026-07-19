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
import com.prafta.common.cmm.push.result.TbmPushTargetRow;

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

    /**
     * 입실 참석자 전원에게 1건씩 적재. dedupKey = {dedupPrefix}_{sessionCd}_{cmpnyCd}_{userCd}(멱등).
     *
     * <p>PRAFTA-SUBCON-T5 F6: 연동 회사 지정으로 <b>타사 소속 참석자</b>가 섞인다. 대상 조회를 세션
     * 단위(개설사 소유 검증 포함)로 넓히고, outbox 는 <b>참석자 회사</b> 기준으로 적재한다
     * (PUSH 토큰/채번이 회사별이므로 개설사 회사코드로 적재하면 타사 참석자에게 도달하지 못한다).
     * dedupKey 에도 회사코드를 포함한다 — USER_CD 는 회사별 채번이라 회사 간 충돌이 가능하다.
     */
    private void fanOut(String cmpnyCd, String siteCd, String sessionCd, String actorUserCd,
                        String notiType, String title, String body, String dedupPrefix) {
        // cmpnyCd = 세션 개설사(호출부의 세션 가드에서 확정). 소유 검증은 매퍼 EXISTS 가 겸한다.
        List<TbmPushTargetRow> attendees = pushTargetMapper.selectTbmEnteredTargets(cmpnyCd, sessionCd);
        if (attendees == null || attendees.isEmpty()) {
            log.info("[tbmEventNoti] {} 통보 대상(입실 참석자) 없음 — 적재 생략 (sessionCd={})", notiType, sessionCd);
            return;
        }
        String payload = buildPayload(notiType, sessionCd);
        int sent = 0;
        for (TbmPushTargetRow target : attendees) {
            String targetUserCd = target.userCd();
            String targetCmpnyCd = target.cmpnyCd();
            if (targetUserCd == null || targetUserCd.isBlank()
                    || targetCmpnyCd == null || targetCmpnyCd.isBlank()) {
                continue;
            }
            // 자사 참석자는 세션 사업장을 그대로 쓰고, 타사 참석자는 사업장코드가 그 회사 네임스페이스라
            // 개설사 사업장을 넣을 수 없다(null). (N4)
            boolean ownAttendee = targetCmpnyCd.equals(cmpnyCd);

            // N3: dedupKey 에 회사코드를 넣지 않는다.
            //   TB_NOTI_OUTBOX 의 dedup UNIQUE 는 UK_NOTI_OUTBOX_DEDUP(CMPNY_CD, DEDUP_KEY) 로 <b>이미 회사별</b>
            //   이고 outbox 행을 참석자 회사로 적재하므로, 회사 내 유일한 USER_CD 만으로 유일성이 성립한다.
            //   회사코드를 붙이면 최악 102자로 DEDUP_KEY varchar(100) 를 넘겨(STRICT 모드 1406) REQUIRES_NEW
            //   트랜잭션이 롤백되고, catch 가 DuplicateKeyException 만 잡으므로 그 세션의 푸시가 통째로 유실된다.
            //   현재 구조: {prefix}(≤14) + '_' + sessionCd(16) + '_' + userCd(≤20) ≤ 51자 — 구조적으로 안전.
            String dedupKey = dedupPrefix + "_" + sessionCd + "_" + targetUserCd;
            try {
                String notiId = leaveDashboardMapper.selectNextNotiId(targetCmpnyCd);
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(notiId);
                outbox.setCmpnyCd(targetCmpnyCd);
                outbox.setSiteCd(ownAttendee ? siteCd : null);
                outbox.setTargetUserCd(targetUserCd);
                outbox.setNotiType(notiType);
                outbox.setChannel(TbmEventNotiConst.CHANNEL_PUSH);
                outbox.setTitle(title);
                outbox.setBody(body);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(TbmEventNotiConst.SEND_STATUS_PENDING);
                outbox.setDedupKey(dedupKey);
                // N4: 타사 참석자 행에 개설사 사용자코드를 남기지 않는다(타사 테넌트 데이터에 개설사 식별자
                //   혼입 방지). 자동 생산자 적재이므로 'SYSTEM' 으로 기록한다.
                outbox.setInsertNo(ownAttendee ? actorUserCd : "SYSTEM");
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
