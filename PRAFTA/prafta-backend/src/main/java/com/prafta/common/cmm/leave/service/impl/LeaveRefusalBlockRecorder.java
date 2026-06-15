package com.prafta.common.cmm.leave.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalLogInsertVO;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 노무수령거부 차단 증빙 적재 전담 컴포넌트 (PRAFTA-COM-008-B §1-2).
 *
 * <p>★ 별도 빈으로 분리한 이유(가장 중요한 함정): BLOCKED 이력 + 관리자 PUSH outbox 는
 * 차단 예외 throw 로 출근/근태 트랜잭션이 롤백되어도 보존되어야 한다("막되 반드시 기록").
 * 이를 위해 {@link #recordBlockAndAlert}에 {@code @Transactional(REQUIRES_NEW)} 를 부여하여
 * 호출 시점에 독립 트랜잭션으로 선(先)커밋한다. 같은 클래스 내부(self-invocation) 호출은
 * Spring AOP 프록시를 타지 않아 새 트랜잭션이 열리지 않으므로, 가드 서비스
 * ({@code LeaveRefusalGuardServiceImpl})와 분리해 외부 빈으로 주입받아 호출한다.
 *
 * <p>본 컴포넌트 내부 실패는 흡수하지 않고 log.error 로 남긴다(증빙 누락 가시화). 단,
 * PUSH outbox 적재 실패가 BLOCKED 이력 적재나 호출부의 차단 throw 를 무효화하면 안 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveRefusalBlockRecorder {

    /** 역할 기반 알림 대상 관리자 권한(master/hr). 노드 어드민은 매퍼가 별도 합산. */
    private static final List<String> ADMIN_AUTH_CDS =
            List.of(AuthRoleUtils.AUTH_MASTER, AuthRoleUtils.AUTH_HR_MANAGER);

    private final LeaveRefusalMapper leaveRefusalMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final ObjectMapper objectMapper;

    /**
     * BLOCKED 이력 INSERT + 관리자 PUSH outbox 적재를 독립 트랜잭션(REQUIRES_NEW)으로 선커밋한다.
     *
     * <p>호출부(가드)는 본 메서드 반환 후 차단 예외를 throw 한다. 본 메서드가 커밋되었으므로
     * 차단으로 출근 트랜잭션이 롤백되어도 증빙은 남는다. 내부 예외는 흡수하지 않으나(로깅),
     * 호출부는 본 메서드 실패와 무관하게 항상 차단 throw 를 수행해야 한다(가드 책임).
     *
     * @param cmpnyCd        회사 코드
     * @param siteCd         사업장 코드
     * @param userCd         대상 근로자 코드
     * @param targetYmd      차단 대상일(YYYYMMDD) = leave_use.START_DATE
     * @param relatedLeaveId 차단 대상 연차사용 ID(tb_user_leave_use.LEAVE_ID)
     * @param attemptType    시도 유형(CHECK_IN/CHECK_OUT/ATTD_CREATE/ADMIN_ENTRY)
     * @param operatorUserCd 시도 주체(근로자 본인 또는 관리자 USER_CD) — INSERT_NO
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBlockAndAlert(String cmpnyCd, String siteCd, String userCd, String targetYmd,
                                    String relatedLeaveId, String attemptType, String operatorUserCd) {
        // 시각 포함 dedup 키 — 같은 사람·같은 날 여러 번 시도해도 시도별 누적(UNIQUE 유지).
        //   {CMPNY}_{USER}_{YMD}_BLOCKED_{HHMMSS}_{attemptType}
        String hhmmss = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"));
        String blockDedupKey = cmpnyCd + "_" + userCd + "_" + targetYmd
                + "_" + LeaveRefusalConst.EVENT_BLOCKED + "_" + hhmmss + "_" + attemptType;

        // 1) BLOCKED 사실 기록(시도별 누적). 출근 원본은 무수정.
        RefusalLogInsertVO blockLog = new RefusalLogInsertVO();
        blockLog.setRefusalId(leaveRefusalMapper.selectNextRefusalId(cmpnyCd));
        blockLog.setCmpnyCd(cmpnyCd);
        blockLog.setSiteCd(siteCd);
        blockLog.setUserCd(userCd);
        blockLog.setTargetYmd(targetYmd);
        blockLog.setEventType(LeaveRefusalConst.EVENT_BLOCKED);
        blockLog.setAttemptType(attemptType);
        blockLog.setRelatedLeaveId(relatedLeaveId);
        blockLog.setDetectNow(true); // DETECT_DTIME = NOW() (차단 시각)
        blockLog.setDedupKey(blockDedupKey);
        blockLog.setInsertNo(operatorUserCd);
        leaveRefusalMapper.insertRefusalLog(blockLog);

        log.info("[leaveRefusal] 노무수령거부 차단 이력 기록 (userCd={}, targetYmd={}, attemptType={}, leaveId={})",
                userCd, targetYmd, attemptType, relatedLeaveId);

        // 2) 관리자 PUSH(outbox 적재). 발송은 com-002 워커 책임(현재 게이트 off 가능).
        alertAdmins(cmpnyCd, siteCd, userCd, targetYmd, attemptType, operatorUserCd);
    }

    /**
     * 대상 관리자(master/hr + 노드 main/sub)별 outbox(PENDING) 적재. 관리자 0명이면 종료.
     * 본문에 대상 근로자명(평문 USER_NM)만 합성(관리자 식별 최소 PII). DATA_PAYLOAD 엔 평문 PII 미포함.
     */
    private void alertAdmins(String cmpnyCd, String siteCd, String userCd, String targetYmd,
                             String attemptType, String operatorUserCd) {
        List<String> admins =
                leaveRefusalMapper.selectSiteRefusalAdmins(cmpnyCd, siteCd, userCd, ADMIN_AUTH_CDS);
        if (admins == null || admins.isEmpty()) {
            log.info("[leaveRefusal] 차단 PUSH 대상 관리자 없음 (userCd={}, targetYmd={})", userCd, targetYmd);
            return;
        }

        // 본문 합성용 대상 근로자명(평문). 없으면(스코프 밖/비활성) 일반 표현으로 폴백.
        String userNm = leaveApprovalNotiMapper.selectUserNm(cmpnyCd, userCd);
        String displayNm = (userNm != null && !userNm.isBlank()) ? userNm : "직원";

        String title = LeaveRefusalConst.BLOCK_ALERT_TITLE;
        String body = String.format(LeaveRefusalConst.BLOCK_ALERT_BODY_FORMAT, displayNm, targetYmd);
        String payload = buildAlertPayload(userCd, targetYmd, attemptType);

        for (String adminUserCd : admins) {
            try {
                String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(notiId);
                outbox.setCmpnyCd(cmpnyCd);
                outbox.setSiteCd(siteCd);
                outbox.setTargetUserCd(adminUserCd);
                outbox.setNotiType(LeaveRefusalConst.NOTI_TYPE_BLOCK_ALERT);
                outbox.setChannel(LeaveRefusalConst.CHANNEL_PUSH);
                outbox.setTitle(title);
                outbox.setBody(body);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(LeaveRefusalConst.SEND_STATUS_PENDING);
                // 시각 포함(시도별) — 같은 관리자·대상·날에 여러 시도 PUSH 누적 허용.
                outbox.setDedupKey("LRB_" + adminUserCd + "_" + userCd + "_" + targetYmd
                        + "_" + attemptType + "_" + java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HHmmss")));
                outbox.setInsertNo(operatorUserCd);
                leaveDashboardMapper.insertNotiOutbox(outbox);
            } catch (DuplicateKeyException dup) {
                // 동일 키 중복 적재 → 흡수하고 다음 관리자 진행.
                log.info("[leaveRefusal] 차단 PUSH 중복 적재 무시 (admin={}, userCd={}, targetYmd={})",
                        adminUserCd, userCd, targetYmd);
            }
        }

        log.info("[leaveRefusal] 차단 PUSH 적재 완료 (userCd={}, targetYmd={}, attemptType={}, 대상수={})",
                userCd, targetYmd, attemptType, admins.size());
    }

    /** 관리자 알림 DATA_PAYLOAD JSON 직렬화. 실패 시 빈 객체 폴백. PII 평문(이름 등) 미포함. */
    private String buildAlertPayload(String targetUserCd, String targetYmd, String attemptType) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", LeaveRefusalConst.NOTI_TYPE_BLOCK_ALERT);
        data.put("targetUserCd", targetUserCd);
        data.put("targetYmd", targetYmd);
        data.put("attemptType", attemptType);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[leaveRefusal] 차단 알림 payload 직렬화 실패 (targetYmd={})", targetYmd, e);
            return "{}";
        }
    }
}
