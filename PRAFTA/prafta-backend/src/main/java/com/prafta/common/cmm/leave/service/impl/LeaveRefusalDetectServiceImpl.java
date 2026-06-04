package com.prafta.common.cmm.leave.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.service.LeaveRefusalDetectService;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalLogInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalTargetVO;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 노무수령거부 출근 감지(기능2) + 관리자 PUSH(기능3) 구현 (PRAFTA-COM-001).
 *
 * <p>출근 체크인 hook 에서 호출된다. 호출부가 try-catch 로 전체 격리하지만, 본 구현도
 * 내부에서 예외를 흡수(로그만)하여 체크인 흐름에 절대 영향을 주지 않는다.
 *
 * <p>트랜잭션 경계: 별도 트랜잭션을 열지 않는다(@Transactional 미부여). 체크인 트랜잭션의
 * INSERT 직후 같은 흐름에서 호출되며, 사실 로그/outbox 적재는 멱등(dedup UNIQUE)이라
 * 부분 적재가 발생해도 재호출로 수렴한다. 감지/알림 실패는 삼키므로 체크인을 막지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRefusalDetectServiceImpl implements LeaveRefusalDetectService {

    /** 역할 기반 알림 대상 관리자 권한(master/hr). 노드 어드민은 매퍼가 별도 합산. */
    private static final List<String> ADMIN_AUTH_CDS =
            List.of(AuthRoleUtils.AUTH_MASTER, AuthRoleUtils.AUTH_HR_MANAGER);

    private final LeaveRefusalMapper leaveRefusalMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void detectAndAlert(String cmpnyCd, String siteCd, String userCd, String nodeCd,
                               String workYmd, String attdId, String insertNo) {
        try {
            // 1) 대상일 판정: 통지(NOTICED) 행 존재 && 비휴일.
            RefusalTargetVO target =
                    leaveRefusalMapper.selectRefusalTarget(cmpnyCd, siteCd, userCd, workYmd);
            if (target == null) {
                // 미통지 또는 휴일 → 노무수령거부 대상 아님. 조용히 종료.
                return;
            }
            String targetYmd = target.getTargetYmd();
            log.info("[leaveRefusal] 노무수령거부 대상일 출근 감지 (userCd={}, targetYmd={}, attdId={})",
                    userCd, targetYmd, attdId);

            // 2) CHECKIN_DETECTED 사실 기록(멱등). 출근 원본은 무수정.
            RefusalLogInsertVO detectLog = new RefusalLogInsertVO();
            detectLog.setRefusalId(leaveRefusalMapper.selectNextRefusalId(cmpnyCd));
            detectLog.setCmpnyCd(cmpnyCd);
            detectLog.setSiteCd(siteCd);
            detectLog.setUserCd(userCd);
            detectLog.setTargetYmd(targetYmd);
            detectLog.setEventType(LeaveRefusalConst.EVENT_CHECKIN_DETECTED);
            detectLog.setRelatedAttdId(attdId);
            detectLog.setDetectNow(true);
            detectLog.setDedupKey(dedupKey(cmpnyCd, userCd, targetYmd, LeaveRefusalConst.EVENT_CHECKIN_DETECTED));
            detectLog.setInsertNo(insertNo);
            leaveRefusalMapper.insertRefusalLog(detectLog);

            // 3) 관리자 PUSH(기능3).
            alertAdmins(cmpnyCd, siteCd, userCd, targetYmd, attdId, insertNo);
        } catch (Exception e) {
            // 감지/알림 실패는 체크인을 막지 않는다 — 로그만 남기고 흡수.
            log.error("[leaveRefusal] 출근 감지/관리자 알림 처리 실패(체크인 영향 없음) (userCd={}, workYmd={})",
                    userCd, workYmd, e);
        }
    }

    /**
     * 기능3: 대상 관리자(master/hr + 노드 main/sub 어드민)별 outbox(PENDING) 적재 +
     * 대표 ADMIN_ALERTED 사실 기록. 관리자 0명이면 CHECKIN_DETECTED 만 남기고 종료.
     */
    private void alertAdmins(String cmpnyCd, String siteCd, String userCd, String targetYmd,
                             String attdId, String insertNo) {
        List<String> admins =
                leaveRefusalMapper.selectSiteRefusalAdmins(cmpnyCd, siteCd, userCd, ADMIN_AUTH_CDS);
        if (admins == null || admins.isEmpty()) {
            log.info("[leaveRefusal] 관리자 PUSH 대상 없음 (userCd={}, targetYmd={})", userCd, targetYmd);
            return;
        }

        String title = LeaveRefusalConst.CHECKIN_ALERT_TITLE;
        String body = String.format(LeaveRefusalConst.CHECKIN_ALERT_BODY_FORMAT, targetYmd);
        String payload = buildAlertPayload(userCd, targetYmd, attdId);

        String firstNotiId = null;
        for (String adminUserCd : admins) {
            try {
                String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(notiId);
                outbox.setCmpnyCd(cmpnyCd);
                outbox.setSiteCd(siteCd);
                outbox.setTargetUserCd(adminUserCd);
                outbox.setNotiType(LeaveRefusalConst.NOTI_TYPE_CHECKIN_ALERT);
                outbox.setChannel(LeaveRefusalConst.CHANNEL_PUSH);
                outbox.setTitle(title);
                outbox.setBody(body);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(LeaveRefusalConst.SEND_STATUS_PENDING);
                outbox.setDedupKey("LRA_" + adminUserCd + "_" + userCd + "_" + targetYmd);
                outbox.setInsertNo(insertNo);
                leaveDashboardMapper.insertNotiOutbox(outbox);
                if (firstNotiId == null) {
                    firstNotiId = notiId;
                }
            } catch (DuplicateKeyException dup) {
                // 동일 (관리자×대상×날짜) 중복 적재 → 흡수하고 다음 관리자 진행.
                log.info("[leaveRefusal] 관리자 PUSH 중복 적재 무시 (admin={}, userCd={}, targetYmd={})",
                        adminUserCd, userCd, targetYmd);
            }
        }

        // 대표 ADMIN_ALERTED 사실 기록 1건(멱등).
        RefusalLogInsertVO alertLog = new RefusalLogInsertVO();
        alertLog.setRefusalId(leaveRefusalMapper.selectNextRefusalId(cmpnyCd));
        alertLog.setCmpnyCd(cmpnyCd);
        alertLog.setSiteCd(siteCd);
        alertLog.setUserCd(userCd);
        alertLog.setTargetYmd(targetYmd);
        alertLog.setEventType(LeaveRefusalConst.EVENT_ADMIN_ALERTED);
        alertLog.setRelatedNotiId(firstNotiId);
        alertLog.setDedupKey(dedupKey(cmpnyCd, userCd, targetYmd, LeaveRefusalConst.EVENT_ADMIN_ALERTED));
        alertLog.setInsertNo(insertNo);
        leaveRefusalMapper.insertRefusalLog(alertLog);

        log.info("[leaveRefusal] 관리자 PUSH 적재 완료 (userCd={}, targetYmd={}, 대상수={})",
                userCd, targetYmd, admins.size());
    }

    /** 사실 로그 dedup 키 = {CMPNY_CD}_{USER_CD}_{TARGET_YMD}_{EVENT_TYPE}. */
    private String dedupKey(String cmpnyCd, String userCd, String targetYmd, String eventType) {
        return cmpnyCd + "_" + userCd + "_" + targetYmd + "_" + eventType;
    }

    /** 관리자 알림 DATA_PAYLOAD JSON 직렬화(Jackson). 실패 시 빈 객체 폴백. PII 평문 미포함. */
    private String buildAlertPayload(String targetUserCd, String targetYmd, String attdId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", LeaveRefusalConst.NOTI_TYPE_CHECKIN_ALERT);
        data.put("targetUserCd", targetUserCd);
        data.put("targetYmd", targetYmd);
        data.put("attdId", attdId);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[leaveRefusal] 관리자 알림 payload 직렬화 실패 (targetYmd={})", targetYmd, e);
            return "{}";
        }
    }
}
