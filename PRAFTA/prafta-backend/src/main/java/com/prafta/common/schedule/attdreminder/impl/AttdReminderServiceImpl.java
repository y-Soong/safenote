package com.prafta.common.schedule.attdreminder.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.schedule.attdreminder.AttdReminderConst;
import com.prafta.common.schedule.attdreminder.mapper.AttdReminderMapper;
import com.prafta.common.schedule.attdreminder.AttdReminderService;
import com.prafta.common.schedule.attdreminder.AttdReminderTargetResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 출근/퇴근 5분 전 리마인더(W4/W5) 적재 구현 (PRAFTA-APP-021-4).
 *
 * <p>서버 LocalTime 기준 (현재+5분) HHMM 으로 대상을 산출한다(단일 타임존, §8-R 5). 적재는 멱등
 * dedupKey(UNIQUE 충돌 흡수)로 매분 재실행돼도 1건만 남는다. 본 주기 적재는 독립 트랜잭션
 * (REQUIRES_NEW)에서 수행되며, 개별 적재 실패는 다음 대상 진행에 영향을 주지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttdReminderServiceImpl implements AttdReminderService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HHmm");
    /** 적재 INSERT_NO(시스템 배치). */
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final AttdReminderMapper attdReminderMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int dispatchReminders() {
        // (현재+5분) 시각 — 분 단위. 날짜는 오늘(자정 5분 전이라도 동일 일자 스케줄 기준; 자정 직전
        //   23:55 종료/익일 00:00 시작 등 극단 경계는 ±5분 알림이라 영향 경미, §8-R 5 단일 타임존).
        LocalDate today = LocalDate.now();
        LocalTime targetTime = LocalTime.now().plusMinutes(AttdReminderConst.LEAD_MINUTES);
        String workYmd = today.format(YMD);
        String targetHhmm = targetTime.format(HHMM);

        int sent = 0;
        sent += dispatchCheckIn(workYmd, targetHhmm);
        sent += dispatchCheckOut(workYmd, targetHhmm);
        if (sent > 0) {
            log.info("[attdReminder] 리마인더 적재 완료. workYmd={}, targetHhmm={}, 적재={}건", workYmd, targetHhmm, sent);
        }
        return sent;
    }

    /** W4 출근 리마인더 적재. dedupKey=CHECKIN_REMIND_{userCd}_{workYmd}_{workSeq}. */
    private int dispatchCheckIn(String workYmd, String targetHhmm) {
        List<AttdReminderTargetResult> targets = attdReminderMapper.selectCheckInTargets(workYmd, targetHhmm);
        if (targets == null || targets.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (AttdReminderTargetResult t : targets) {
            String dedupKey = "CHECKIN_REMIND_" + t.userCd() + "_" + workYmd + "_" + t.workSeq();
            String payload = buildPayload(AttdReminderConst.NOTI_TYPE_CHECKIN, workYmd, t.workSeq());
            if (insertReminder(t, AttdReminderConst.NOTI_TYPE_CHECKIN,
                    AttdReminderConst.CHECKIN_TITLE, AttdReminderConst.CHECKIN_BODY, payload, dedupKey)) {
                sent++;
            }
        }
        return sent;
    }

    /** W5 퇴근 리마인더 적재. dedupKey=CHECKOUT_REMIND_{userCd}_{workYmd}_{workSeq}. */
    private int dispatchCheckOut(String workYmd, String targetHhmm) {
        List<AttdReminderTargetResult> targets = attdReminderMapper.selectCheckOutTargets(workYmd, targetHhmm);
        if (targets == null || targets.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (AttdReminderTargetResult t : targets) {
            String dedupKey = "CHECKOUT_REMIND_" + t.userCd() + "_" + workYmd + "_" + t.workSeq();
            String payload = buildPayload(AttdReminderConst.NOTI_TYPE_CHECKOUT, workYmd, t.workSeq());
            if (insertReminder(t, AttdReminderConst.NOTI_TYPE_CHECKOUT,
                    AttdReminderConst.CHECKOUT_TITLE, AttdReminderConst.CHECKOUT_BODY, payload, dedupKey)) {
                sent++;
            }
        }
        return sent;
    }

    /** outbox 1건 적재(PENDING). dedupKey UNIQUE 충돌은 멱등 흡수(false 반환). 적재 성공 시 true. */
    private boolean insertReminder(AttdReminderTargetResult t, String notiType,
                                   String title, String body, String payload, String dedupKey) {
        try {
            String notiId = leaveDashboardMapper.selectNextNotiId(t.cmpnyCd());
            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(notiId);
            outbox.setCmpnyCd(t.cmpnyCd());
            outbox.setSiteCd(t.siteCd());
            outbox.setTargetUserCd(t.userCd());
            outbox.setNotiType(notiType);
            outbox.setChannel(AttdReminderConst.CHANNEL_PUSH);
            outbox.setTitle(title);
            outbox.setBody(body);
            outbox.setDataPayload(payload);
            outbox.setSendStatus(AttdReminderConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(SYSTEM_ACTOR);
            leaveDashboardMapper.insertNotiOutbox(outbox);
            return true;
        } catch (DuplicateKeyException dup) {
            // 매분 재실행 멱등: 이미 같은 분/구간으로 적재됨 → 흡수.
            log.debug("[attdReminder] 중복 적재 무시 (type={}, dedupKey={})", notiType, dedupKey);
            return false;
        }
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String notiType, String workYmd, int workSeq) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", notiType);
        data.put("workYmd", workYmd);
        data.put("workSeq", workSeq);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[attdReminder] payload 직렬화 실패 (type={}, workYmd={})", notiType, workYmd, e);
            return "{}";
        }
    }
}
