package com.prafta.common.cmm.push.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.prafta.common.cmm.push.TransferReservedNotiConst;
import com.prafta.common.cmm.push.TransferReservedNotiService;

import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 소속이동 예약 통보(PRAFTA-WEB_001-3, Terminal C) PUSH 생산자 구현.
 *
 * <p>{@code LeaveDirectSetNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 자기프록시 격리 패턴을 따른다.
 * 등록 본 트랜잭션 커밋 이후에만 outbox 1건을 적재하고, 적재 실패는 등록을 롤백하지 않는다(best-effort).
 * 발송 자체는 워커({@code PushSenderServiceImpl})가 비동기로 수행하며, FCM 미구성이면 워커가 skip 한다.
 */
@Slf4j
@Service
public class TransferReservedNotiServiceImpl implements TransferReservedNotiService {

    /** 본문 표시용 날짜 포맷(YYYYMMDD → YYYY-MM-DD). */
    private static final DateTimeFormatter YMD_IN = DateTimeFormatter.BASIC_ISO_DATE;

    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출(@Lazy 순환 차단). */
    private final TransferReservedNotiService self;

    public TransferReservedNotiServiceImpl(LeaveDashboardMapper leaveDashboardMapper,
                                           ObjectMapper objectMapper,
                                           @Lazy TransferReservedNotiService self) {
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Override
    public void notifyTransferReserved(String cmpnyCd, String siteCd, String targetUserCd, String reservationId,
                                       String moveDateYmd, String toSiteNm, String toNodeNm,
                                       String defaultSchNm, String moveReason, String actorUserCd) {
        if (isBlank(cmpnyCd) || isBlank(targetUserCd) || isBlank(reservationId)) {
            return;
        }
        runAfterCommit(() -> self.runTransferReservedOutbox(cmpnyCd, siteCd, targetUserCd, reservationId,
                moveDateYmd, toSiteNm, toNodeNm, defaultSchNm, moveReason, actorUserCd));
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
                            log.error("[transferReservedNoti] afterCommit outbox 적재 실패(등록 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[transferReservedNoti] PUSH 적재 등록 실패(등록 흐름 영향 없음)", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runTransferReservedOutbox(String cmpnyCd, String siteCd, String targetUserCd, String reservationId,
                                          String moveDateYmd, String toSiteNm, String toNodeNm,
                                          String defaultSchNm, String moveReason, String actorUserCd) {
        // 예약 1건당 1회만 적재(오발송 방지) — UNIQUE(CMPNY_CD, DEDUP_KEY) 백스톱.
        String dedupKey = TransferReservedNotiConst.DEDUP_PREFIX + reservationId;
        String body = buildBody(moveDateYmd, toSiteNm, toNodeNm, defaultSchNm, moveReason);
        String payload = buildPayload(reservationId);

        try {
            String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(notiId);
            outbox.setCmpnyCd(cmpnyCd);
            outbox.setSiteCd(siteCd);
            outbox.setTargetUserCd(targetUserCd);
            outbox.setNotiType(TransferReservedNotiConst.NOTI_TYPE);
            outbox.setChannel(TransferReservedNotiConst.CHANNEL_PUSH);
            outbox.setTitle(TransferReservedNotiConst.TITLE);
            outbox.setBody(body);
            outbox.setDataPayload(payload);
            outbox.setSendStatus(TransferReservedNotiConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(actorUserCd);
            leaveDashboardMapper.insertNotiOutbox(outbox);
            log.info("[transferReservedNoti] 소속이동 예약 PUSH 적재 (target={}, reservationId={})",
                    targetUserCd, reservationId);
        } catch (DuplicateKeyException dup) {
            log.info("[transferReservedNoti] 동일 예약 중복 적재 무시 (target={}, dedupKey={})", targetUserCd, dedupKey);
        }
    }

    /** 본문: 이동일 + 지정 항목(사업장/부서/기본근무타입/사유). 일용직은 근무타입 줄 생략. */
    private String buildBody(String moveDateYmd, String toSiteNm, String toNodeNm,
                             String defaultSchNm, String moveReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("소속이동이 예약되었습니다.");
        sb.append("\n이동일: ").append(formatYmdLabel(moveDateYmd));
        if (!isBlank(toSiteNm)) {
            sb.append("\n이동 사업장: ").append(toSiteNm);
        }
        if (!isBlank(toNodeNm)) {
            sb.append("\n이동 부서: ").append(toNodeNm);
        }
        if (!isBlank(defaultSchNm)) {
            sb.append("\n기본 근무타입: ").append(defaultSchNm);
        }
        if (!isBlank(moveReason)) {
            sb.append("\n사유: ").append(truncate(moveReason));
        }
        return sb.toString();
    }

    /** YYYYMMDD → "YYYY-MM-DD". 파싱 불가 시 원문 폴백. */
    private String formatYmdLabel(String ymd) {
        if (ymd == null || !ymd.matches("\\d{8}")) {
            return ymd == null ? "" : ymd;
        }
        try {
            LocalDate d = LocalDate.parse(ymd, YMD_IN);
            return d.toString();
        } catch (Exception e) {
            return ymd;
        }
    }

    /** DATA_PAYLOAD(라우팅 키만 — PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String reservationId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", TransferReservedNotiConst.NOTI_TYPE);
        data.put("reservationId", reservationId);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[transferReservedNoti] payload 직렬화 실패 (reservationId={})", reservationId, e);
            return "{}";
        }
    }

    /** 본문 사유 길이 가드. */
    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= TransferReservedNotiConst.BODY_REASON_MAX_LEN
                ? s
                : s.substring(0, TransferReservedNotiConst.BODY_REASON_MAX_LEN);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
