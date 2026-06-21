package com.prafta.common.cmm.push.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.prafta.common.cmm.push.LeaveDirectSetNotiConst;
import com.prafta.common.cmm.push.LeaveDirectSetNotiService;

import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 연차/월차 직접 등록 통보(prafta-com-016-C-2) PUSH 생산자 구현.
 *
 * <p>{@code ApprovalResultNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 격리 패턴을 따른다.
 * 여러 날을 한 번에 등록해도 대상 근로자 1인에게 <b>묶어 1건</b>만 적재한다.
 * BODY/payload 에 PII 는 넣지 않는다(날짜 건수만).
 */
@Slf4j
@Service
public class LeaveDirectSetNotiServiceImpl implements LeaveDirectSetNotiService {

    /** 본문 표시용 날짜 포맷(YYYYMMDD → M월 d일). */
    private static final DateTimeFormatter YMD_IN = DateTimeFormatter.BASIC_ISO_DATE;

    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출(@Lazy 순환 차단). */
    private final LeaveDirectSetNotiService self;

    public LeaveDirectSetNotiServiceImpl(LeaveDashboardMapper leaveDashboardMapper,
                                         ObjectMapper objectMapper,
                                         @Lazy LeaveDirectSetNotiService self) {
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Override
    public void notifyLeaveDirectSet(String cmpnyCd, String siteCd, String targetUserCd,
                                     List<String> workYmds, String actorUserCd) {
        if (targetUserCd == null || targetUserCd.isBlank() || workYmds == null || workYmds.isEmpty()) {
            return;
        }
        // 호출자(저장 흐름)의 가변 리스트를 afterCommit 시점까지 안전 보관하려고 방어 복사.
        List<String> snapshot = new ArrayList<>(workYmds);
        runAfterCommit(() -> self.runLeaveDirectSetOutbox(cmpnyCd, siteCd, targetUserCd, snapshot, actorUserCd));
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
                            log.error("[leaveDirectSetNoti] afterCommit outbox 적재 실패(저장 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[leaveDirectSetNoti] PUSH 적재 등록 실패(저장 흐름 영향 없음)", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runLeaveDirectSetOutbox(String cmpnyCd, String siteCd, String targetUserCd,
                                        List<String> workYmds, String actorUserCd) {
        if (workYmds == null || workYmds.isEmpty()) {
            return;
        }
        List<String> sorted = new ArrayList<>(workYmds);
        sorted.sort(java.util.Comparator.naturalOrder());

        String body = buildBody(sorted);
        String payload = buildPayload(targetUserCd, sorted);
        // 묶음 1건 멱등키: 같은 근로자·같은 등록일(today)·동일 배치 내용(최소일+건수)에 1건만 적재.
        //   재시도(동일 배치)는 흡수, 별도 배치(다른 날 추가 등록)는 내용이 달라 새 1건 적재.
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String dedupKey = "LV_SET_" + targetUserCd + "_" + today + "_" + sorted.get(0) + "_" + sorted.size();

        try {
            String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(notiId);
            outbox.setCmpnyCd(cmpnyCd);
            outbox.setSiteCd(siteCd);
            outbox.setTargetUserCd(targetUserCd);
            outbox.setNotiType(LeaveDirectSetNotiConst.NOTI_TYPE);
            outbox.setChannel(LeaveDirectSetNotiConst.CHANNEL_PUSH);
            outbox.setTitle(LeaveDirectSetNotiConst.TITLE);
            outbox.setBody(body);
            outbox.setDataPayload(payload);
            outbox.setSendStatus(LeaveDirectSetNotiConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(actorUserCd);
            leaveDashboardMapper.insertNotiOutbox(outbox);
            log.info("[leaveDirectSetNoti] 연차/월차 직접 등록 PUSH 적재 (target={}, 건수={})",
                    targetUserCd, sorted.size());
        } catch (DuplicateKeyException dup) {
            log.info("[leaveDirectSetNoti] 동일 배치 중복 적재 무시 (target={}, dedupKey={})", targetUserCd, dedupKey);
        }
    }

    /** 본문 "관리자가 N월 N일 외 X건을 연차/월차로 등록했습니다" (PII 미포함, 날짜·건수만). */
    private String buildBody(List<String> sorted) {
        String firstLabel = formatYmdLabel(sorted.get(0));
        if (sorted.size() == 1) {
            return "관리자가 " + firstLabel + "을(를) 연차/월차로 등록했습니다.";
        }
        return "관리자가 " + firstLabel + " 외 " + (sorted.size() - 1) + "건을 연차/월차로 등록했습니다.";
    }

    /** YYYYMMDD → "M월 d일". 파싱 불가 시 원문 폴백. */
    private String formatYmdLabel(String ymd) {
        if (ymd == null || !ymd.matches("\\d{8}")) {
            return ymd;
        }
        try {
            LocalDate d = LocalDate.parse(ymd, YMD_IN);
            return d.getMonthValue() + "월 " + d.getDayOfMonth() + "일";
        } catch (Exception e) {
            return ymd;
        }
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String targetUserCd, List<String> sorted) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", LeaveDirectSetNotiConst.NOTI_TYPE);
        data.put("count", sorted.size());
        // 라우팅에 사용할 대표 월(YYYYMM) 1개만 — 날짜 전체 나열은 하지 않는다(payload 비대화 방지).
        data.put("ym", sorted.get(0).length() >= 6 ? sorted.get(0).substring(0, 6) : sorted.get(0));
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[leaveDirectSetNoti] payload 직렬화 실패 (target={})", targetUserCd, e);
            return "{}";
        }
    }
}
