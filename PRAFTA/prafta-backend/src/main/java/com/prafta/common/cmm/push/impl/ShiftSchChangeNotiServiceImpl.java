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
import com.prafta.common.cmm.push.ShiftSchChangeNotiConst;
import com.prafta.common.cmm.push.ShiftSchChangeNotiService;

import lombok.extern.slf4j.Slf4j;

/**
 * 교대근무 팀 스케줄 변경 통보(prafta-com-016-D-2) PUSH 생산자 구현.
 *
 * <p>{@code LeaveDirectSetNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 격리 패턴을 복제한다.
 * 실제 덮인 날이 1건 이상인 조원에게만, 묶어 1건만 적재한다(저장 1회당 1건, D-Q1).
 * BODY 에는 팀명만 치환하고 PII 는 넣지 않는다.
 */
@Slf4j
@Service
public class ShiftSchChangeNotiServiceImpl implements ShiftSchChangeNotiService {

    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출(@Lazy 순환 차단). */
    private final ShiftSchChangeNotiService self;

    public ShiftSchChangeNotiServiceImpl(LeaveDashboardMapper leaveDashboardMapper,
                                         ObjectMapper objectMapper,
                                         @Lazy ShiftSchChangeNotiService self) {
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Override
    public void notifyShiftSchChange(String cmpnyCd, String siteCd, String targetUserCd,
                                     String shiftTeamNm, List<String> changedYmds, String actorUserCd) {
        if (targetUserCd == null || targetUserCd.isBlank() || changedYmds == null || changedYmds.isEmpty()) {
            return;
        }
        // 호출자(저장 흐름)의 가변 리스트를 afterCommit 시점까지 안전 보관하려고 방어 복사.
        List<String> snapshot = new ArrayList<>(changedYmds);
        runAfterCommit(() -> self.runShiftSchChangeOutbox(cmpnyCd, siteCd, targetUserCd, shiftTeamNm, snapshot, actorUserCd));
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
                            log.error("[shiftSchChangeNoti] afterCommit outbox 적재 실패(저장 흐름 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[shiftSchChangeNoti] PUSH 적재 등록 실패(저장 흐름 영향 없음)", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runShiftSchChangeOutbox(String cmpnyCd, String siteCd, String targetUserCd,
                                        String shiftTeamNm, List<String> changedYmds, String actorUserCd) {
        if (changedYmds == null || changedYmds.isEmpty()) {
            return;
        }
        List<String> sorted = new ArrayList<>(changedYmds);
        sorted.sort(java.util.Comparator.naturalOrder());

        String body = buildBody(shiftTeamNm);
        String payload = buildPayload(sorted);
        // 묶음 1건 멱등키: 같은 근로자·같은 저장일(today)·동일 배치 내용(최소일+건수)에 1건만 적재.
        //   재시도(동일 배치)는 흡수, 별도 저장(다른 날 추가 등)은 내용이 달라 새 1건 적재.
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String dedupKey = "SHIFT_CHG_" + targetUserCd + "_" + today + "_" + sorted.get(0) + "_" + sorted.size();

        try {
            String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(notiId);
            outbox.setCmpnyCd(cmpnyCd);
            outbox.setSiteCd(siteCd);
            outbox.setTargetUserCd(targetUserCd);
            outbox.setNotiType(ShiftSchChangeNotiConst.NOTI_TYPE);
            outbox.setChannel(ShiftSchChangeNotiConst.CHANNEL_PUSH);
            outbox.setTitle(ShiftSchChangeNotiConst.TITLE);
            outbox.setBody(body);
            outbox.setDataPayload(payload);
            outbox.setSendStatus(ShiftSchChangeNotiConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(actorUserCd);
            leaveDashboardMapper.insertNotiOutbox(outbox);
            log.info("[shiftSchChangeNoti] 교대 스케줄 변경 PUSH 적재 (target={}, 변경건수={})",
                    targetUserCd, sorted.size());
        } catch (DuplicateKeyException dup) {
            log.info("[shiftSchChangeNoti] 동일 배치 중복 적재 무시 (target={}, dedupKey={})", targetUserCd, dedupKey);
        }
    }

    /** 본문 — 팀명만 치환(PII 미포함). 팀명 빈값이면 안내용 일반 문구로 폴백. */
    private String buildBody(String shiftTeamNm) {
        String teamNm = (shiftTeamNm == null || shiftTeamNm.isBlank()) ? "교대근무팀" : shiftTeamNm;
        return ShiftSchChangeNotiConst.BODY_TEMPLATE.replace("{teamNm}", teamNm);
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(List<String> sorted) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", ShiftSchChangeNotiConst.NOTI_TYPE);
        data.put("count", sorted.size());
        // 라우팅에 사용할 대표 월(YYYYMM) 1개만 — 날짜 전체 나열은 하지 않는다(payload 비대화 방지).
        data.put("ym", sorted.get(0).length() >= 6 ? sorted.get(0).substring(0, 6) : sorted.get(0));
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[shiftSchChangeNoti] payload 직렬화 실패", e);
            return "{}";
        }
    }
}
