package com.prafta.common.cmm.push.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.push.AttdLateEarlyNotiConst;
import com.prafta.common.cmm.push.AttdLateEarlyNotiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지각/조퇴 감지 통보(M1) PUSH 생산자 구현 (PRAFTA-APP-021-3c).
 *
 * <p>{@code LeaveRefusalDetectServiceImpl} 의 단순 격리 패턴을 따른다(별도 트랜잭션 미개시 —
 * 출퇴근 트랜잭션 흐름에서 호출되며 내부 예외를 흡수). 멱등 dedupKey(ATTD_LATE_/ATTD_EARLY_ + attdId)로
 * 같은 근태 레코드 1건당 1회만 적재된다.
 *
 * <p>판정은 raw 실근태(표준화 미적용). 자정 넘김 오판 방지를 위해 (일자+시각) 통합 분 stamp 로 비교한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttdLateEarlyNotiServiceImpl implements AttdLateEarlyNotiService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void detectLate(String cmpnyCd, String siteCd, String workerUserCd, String nodeCd,
                           String workYmd, String attdId, String checkInDate, String checkInTime,
                           String schStartHhmm, String actorUserCd) {
        try {
            // 스케줄 시작/출근 시각 결측이면 판정 불가 → no-op.
            if (!hasHhmm(schStartHhmm) || !hasHhmm(checkInTime) || workYmd == null) {
                return;
            }
            String inYmd = hasYmd(checkInDate) ? checkInDate : workYmd;
            long schStartStamp = toMinuteStamp(workYmd, schStartHhmm);
            long actInStamp = toMinuteStamp(inYmd, checkInTime);
            if (actInStamp <= schStartStamp) {
                return; // 정시/조기 출근 — 지각 아님.
            }
            String workerNm = resolveUserNm(cmpnyCd, workerUserCd);
            String title = AttdLateEarlyNotiConst.LATE_TITLE;
            String body = String.format(AttdLateEarlyNotiConst.LATE_BODY_FORMAT, workerNm);
            String dedupKeyPrefix = "ATTD_LATE_" + attdId + "_";
            fanOutToNodeAdmins(cmpnyCd, siteCd, workerUserCd, nodeCd, workYmd, attdId,
                    AttdLateEarlyNotiConst.EVENT_LATE, title, body, dedupKeyPrefix, actorUserCd);
        } catch (Exception e) {
            log.error("[lateEarlyNoti] 지각 감지/통보 처리 실패(체크인 영향 없음) (userCd={}, workYmd={})",
                    workerUserCd, workYmd, e);
        }
    }

    @Override
    public void detectEarly(String cmpnyCd, String siteCd, String workerUserCd, String nodeCd,
                            String workYmd, String attdId, String checkOutDate, String checkOutTime,
                            String schStartHhmm, String schEndHhmm, String actorUserCd) {
        try {
            if (!hasHhmm(schEndHhmm) || !hasHhmm(checkOutTime) || workYmd == null) {
                return;
            }
            // 야간(종료<시작)이면 스케줄 종료는 근무일 익일로 본다(웹 Attd_11 동일).
            String endYmd = workYmd;
            if (hasHhmm(schStartHhmm) && schEndHhmm.compareTo(schStartHhmm) < 0) {
                endYmd = ymdPlusDays(workYmd, 1);
            }
            long schEndStamp = toMinuteStamp(endYmd, schEndHhmm);
            String outYmd = hasYmd(checkOutDate) ? checkOutDate : workYmd;
            long actOutStamp = toMinuteStamp(outYmd, checkOutTime);
            if (actOutStamp >= schEndStamp) {
                return; // 정시/연장 퇴근 — 조퇴 아님.
            }
            String workerNm = resolveUserNm(cmpnyCd, workerUserCd);
            String title = AttdLateEarlyNotiConst.EARLY_TITLE;
            String body = String.format(AttdLateEarlyNotiConst.EARLY_BODY_FORMAT, workerNm);
            String dedupKeyPrefix = "ATTD_EARLY_" + attdId + "_";
            fanOutToNodeAdmins(cmpnyCd, siteCd, workerUserCd, nodeCd, workYmd, attdId,
                    AttdLateEarlyNotiConst.EVENT_EARLY, title, body, dedupKeyPrefix, actorUserCd);
        } catch (Exception e) {
            log.error("[lateEarlyNoti] 조퇴 감지/통보 처리 실패(체크아웃 영향 없음) (userCd={}, workYmd={})",
                    workerUserCd, workYmd, e);
        }
    }

    /**
     * 근로자 소속 노드 main/sub 관리자에게 1건씩 적재. 본인이 관리자여도 자기 알림 제외.
     * dedupKey = {prefix}{adminUserCd} (근태 레코드×관리자 단위 1건, 멱등).
     */
    private void fanOutToNodeAdmins(String cmpnyCd, String siteCd, String workerUserCd, String nodeCd,
                                    String workYmd, String attdId, String event,
                                    String title, String body, String dedupKeyPrefix, String actorUserCd) {
        List<String> admins = leaveApprovalNotiMapper.selectNodeAdmins(cmpnyCd, siteCd, workerUserCd);
        if (admins == null || admins.isEmpty()) {
            log.info("[lateEarlyNoti] {} 통보 대상 노드 관리자 없음 (userCd={}, workYmd={})", event, workerUserCd, workYmd);
            return;
        }
        String payload = buildPayload(event, workerUserCd, workYmd, attdId);
        int sent = 0;
        for (String adminUserCd : admins) {
            // 자기 알림 방지: 근로자 본인이 노드 관리자면 제외.
            if (adminUserCd == null || adminUserCd.isBlank() || adminUserCd.equals(workerUserCd)) {
                continue;
            }
            String dedupKey = dedupKeyPrefix + adminUserCd;
            try {
                String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(notiId);
                outbox.setCmpnyCd(cmpnyCd);
                outbox.setSiteCd(siteCd);
                outbox.setTargetUserCd(adminUserCd);
                outbox.setNotiType(AttdLateEarlyNotiConst.NOTI_TYPE);
                outbox.setChannel(AttdLateEarlyNotiConst.CHANNEL_PUSH);
                outbox.setTitle(title);
                outbox.setBody(body);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(AttdLateEarlyNotiConst.SEND_STATUS_PENDING);
                outbox.setDedupKey(dedupKey);
                outbox.setInsertNo(actorUserCd);
                leaveDashboardMapper.insertNotiOutbox(outbox);
                sent++;
            } catch (DuplicateKeyException dup) {
                log.info("[lateEarlyNoti] {} 중복 적재 무시 (attdId={}, admin={})", event, attdId, adminUserCd);
            }
        }
        log.info("[lateEarlyNoti] {} PUSH 적재 완료 (userCd={}, attdId={}, 대상수={})", event, workerUserCd, attdId, sent);
    }

    /** 근로자명 평문 조회. 미존재/스코프 밖이면 빈 문자열(본문 합성 안전 폴백). */
    private String resolveUserNm(String cmpnyCd, String userCd) {
        String nm = leaveApprovalNotiMapper.selectUserNm(cmpnyCd, userCd);
        return (nm == null) ? "" : nm;
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 평문 미포함). 실패 시 빈 객체 폴백. */
    private String buildPayload(String event, String workerUserCd, String workYmd, String attdId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", AttdLateEarlyNotiConst.NOTI_TYPE);
        data.put("event", event);
        data.put("workerUserCd", workerUserCd);
        data.put("workYmd", workYmd);
        data.put("attdId", attdId);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[lateEarlyNoti] payload 직렬화 실패 (attdId={})", attdId, e);
            return "{}";
        }
    }

    // ── stamp 유틸(웹 Attd_11 / 앱 AppAttd01ServiceImpl 동일 규칙) ──

    private boolean hasHhmm(String hhmm) {
        return hhmm != null && hhmm.matches("\\d{4}");
    }

    private boolean hasYmd(String ymd) {
        return ymd != null && ymd.matches("\\d{8}");
    }

    /** 일자(YYYYMMDD) + 시각(HHmm) 을 1970-01-01 기준 통합 분(minute) stamp 로 환산. */
    private long toMinuteStamp(String ymd, String hhmm) {
        LocalDate d = LocalDate.parse(ymd, YMD);
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return d.toEpochDay() * 1440L + (long) hh * 60L + mm;
    }

    private String ymdPlusDays(String ymd, int days) {
        return LocalDate.parse(ymd, YMD).plusDays(days).format(YMD);
    }
}
