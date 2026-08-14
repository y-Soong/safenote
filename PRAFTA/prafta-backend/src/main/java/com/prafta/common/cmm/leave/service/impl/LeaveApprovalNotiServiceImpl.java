package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.service.LeaveApprovalNotiConst;
import com.prafta.common.cmm.leave.service.LeaveApprovalNotiService;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 결재 PUSH 생산자(outbox PENDING 적재) 구현 (PRAFTA-COM-004).
 *
 * <p>web/app 연차 신청·승인 hook 에서 호출된다. 호출부도 try-catch 로 격리하지만, 본 구현도
 * 내부에서 예외를 흡수(로그만)하여 연차 본 흐름에 절대 영향을 주지 않는다.
 *
 * <p>트랜잭션 경계: 별도 트랜잭션을 열지 않는다({@code @Transactional} 미부여). 신청/결재 흐름의
 * 트랜잭션 내부에서 호출되며, outbox 적재는 멱등(dedup UNIQUE(CMPNY_CD, DEDUP_KEY))이라 중복
 * 적재는 흡수된다. 적재 실패는 삼키므로 연차 신청/결재를 막지 않는다.
 *
 * <p>본문의 신청자명은 평문 {@code USER_NM} 조회값이다(AES-GCM 복호화 호출 없음).
 * DATA_PAYLOAD 에는 평문 이름을 넣지 않고 라우팅 키만 직렬화한다(PII 한정 — BODY 에만).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApprovalNotiServiceImpl implements LeaveApprovalNotiService {

    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void notifyApprovalTurn(String cmpnyCd, String siteCd, String applicantUserCd,
                                   String reqId, int approvalStep, String approverUserCd,
                                   String insertNo) {
        // 기존 시그니처 유지 — 묶음 없이(groupId=null) 위임한다. 동작 완전 동일.
        notifyApprovalTurn(cmpnyCd, siteCd, applicantUserCd, reqId, approvalStep, approverUserCd,
                insertNo, null);
    }

    @Override
    public void notifyApprovalTurn(String cmpnyCd, String siteCd, String applicantUserCd,
                                   String reqId, int approvalStep, String approverUserCd,
                                   String insertNo, String groupId) {
        try {
            if (approverUserCd == null || approverUserCd.isBlank()) {
                log.info("[leaveAprvNoti] 차례 도래 결재자 없음 — 적재 생략 (reqId={}, step={})", reqId, approvalStep);
                return;
            }
            String applicantNm = resolveUserNm(cmpnyCd, applicantUserCd);
            String title = LeaveApprovalNotiConst.TURN_TITLE;
            String body = String.format(LeaveApprovalNotiConst.TURN_BODY_FORMAT, applicantNm);
            String payload = buildTurnPayload(reqId, approvalStep, applicantUserCd);
            // prafta-leavemulti: 기간신청 묶음이면 묶음 단위 키를 써서 1건만 적재한다
            //   (날짜별 REQ N건 → 결재자에게 알림 N개 가는 것 방지. 14일이면 14개).
            //   ★ groupId == null(단일일) 이면 종전 키 그대로 → 무회귀.
            boolean grouped = (groupId != null && !groupId.isBlank());
            String dedupKey = grouped
                    ? "LV_TURN_GRP_" + groupId + "_" + approvalStep
                    : "LV_TURN_" + reqId + "_" + approvalStep;

            // 묶음 2번째 이후는 같은 키가 되므로 INSERT 전에 존재 여부를 확인해 건너뛴다.
            //   ★예외 기반 흡수를 쓰지 않는 이유: 이 적재는 호출자(연차 신청) 트랜잭션 안에서 실행되므로
            //     DuplicateKeyException 을 유발하면 휴가 1건당 에러 로그가 13개 남고 트랜잭션 오염 우려도 있다.
            //     단일 트랜잭션 내 순차 호출이라 검사~삽입 사이 경합이 없다.
            //   단일일(grouped=false)은 키가 매번 달라 검사 자체를 생략한다 → 쿼리 증가 없음(무회귀).
            if (grouped && leaveApprovalNotiMapper.countOutboxByDedupKey(cmpnyCd, dedupKey) > 0) {
                log.info("[leaveAprvNoti] 묶음 차례 도래 PUSH 이미 적재됨 — 생략 (groupId={}, step={})",
                        groupId, approvalStep);
                return;
            }

            insertOutbox(cmpnyCd, siteCd, approverUserCd, LeaveApprovalNotiConst.NOTI_TYPE_APPROVAL_TURN,
                    title, body, payload, dedupKey, insertNo);

            log.info("[leaveAprvNoti] 결재 차례 도래 PUSH 적재 (reqId={}, step={}, approver={}, groupId={})",
                    reqId, approvalStep, approverUserCd, groupId);
        } catch (Exception e) {
            // 적재 실패는 연차 흐름을 막지 않는다 — 로그만 남기고 흡수.
            log.error("[leaveAprvNoti] 결재 차례 도래 PUSH 적재 실패(연차 흐름 영향 없음) (reqId={}, step={})",
                    reqId, approvalStep, e);
        }
    }

    @Override
    public void notifyLeaveUsedNoAprv(String cmpnyCd, String siteCd, String applicantUserCd,
                                      String leaveId, String useUnitType, BigDecimal leaveDays,
                                      String workYmd, String startTime, String endTime,
                                      String insertNo) {
        // 기존 시그니처 유지 — 묶음 없이(groupId=null) 위임한다. 동작 완전 동일.
        notifyLeaveUsedNoAprv(cmpnyCd, siteCd, applicantUserCd, leaveId, useUnitType, leaveDays,
                workYmd, startTime, endTime, insertNo, null);
    }

    @Override
    public void notifyLeaveUsedNoAprv(String cmpnyCd, String siteCd, String applicantUserCd,
                                      String leaveId, String useUnitType, BigDecimal leaveDays,
                                      String workYmd, String startTime, String endTime,
                                      String insertNo, String groupId) {
        try {
            List<String> admins =
                    leaveApprovalNotiMapper.selectNodeAdmins(cmpnyCd, siteCd, applicantUserCd);
            if (admins == null || admins.isEmpty()) {
                log.info("[leaveAprvNoti] 무결재 사용 통보 대상 노드 관리자 없음 (leaveId={}, userCd={})",
                        leaveId, applicantUserCd);
                return;
            }

            String applicantNm = resolveUserNm(cmpnyCd, applicantUserCd);
            String title = LeaveApprovalNotiConst.USED_TITLE;
            String body = buildUsedBody(applicantNm, useUnitType, leaveDays, workYmd, startTime, endTime);
            String payload = buildUsedPayload(leaveId, applicantUserCd, workYmd);

            int sent = 0;
            for (String targetUserCd : admins) {
                // 자기 알림 방지: 신청자 본인이 노드 관리자면 제외.
                if (targetUserCd == null || targetUserCd.equals(applicantUserCd)) {
                    continue;
                }
                // prafta-leavemulti: 묶음이면 (묶음×관리자) 단위로 1건만 — 날짜별 N개 방지.
                //   ★ groupId == null(단일일) 이면 종전 키 그대로 → 무회귀.
                boolean grouped = (groupId != null && !groupId.isBlank());
                String dedupKey = grouped
                        ? "LV_USED_GRP_" + groupId + "_" + targetUserCd
                        : "LV_USED_" + leaveId + "_" + targetUserCd;

                // 묶음 2번째 이후는 같은 키 → INSERT 전에 확인해 건너뛴다(호출자 트랜잭션 오염·에러로그 폭증 방지).
                //   단일일은 검사 생략 → 쿼리 증가 없음(무회귀). 기존 DuplicateKey 흡수는 그대로 남긴다.
                if (grouped && leaveApprovalNotiMapper.countOutboxByDedupKey(cmpnyCd, dedupKey) > 0) {
                    continue;
                }
                try {
                    insertOutbox(cmpnyCd, siteCd, targetUserCd, LeaveApprovalNotiConst.NOTI_TYPE_USED_NO_APRV,
                            title, body, payload, dedupKey, insertNo);
                    sent++;
                } catch (DuplicateKeyException dup) {
                    // 동일 (사용기록×관리자) 중복 적재 → 흡수하고 다음 관리자 진행(멱등).
                    log.info("[leaveAprvNoti] 무결재 사용 통보 중복 적재 무시 (leaveId={}, admin={})",
                            leaveId, targetUserCd);
                }
            }

            log.info("[leaveAprvNoti] 무결재 연차 사용 통보 PUSH 적재 완료 (leaveId={}, userCd={}, 대상수={})",
                    leaveId, applicantUserCd, sent);
        } catch (Exception e) {
            log.error("[leaveAprvNoti] 무결재 연차 사용 통보 PUSH 적재 실패(연차 흐름 영향 없음) (leaveId={}, userCd={})",
                    leaveId, applicantUserCd, e);
        }
    }

    /** outbox 1행 적재(SEND_STATUS='PENDING'). DuplicateKeyException 은 상위로 전파(시나리오 B 가 흡수). */
    private void insertOutbox(String cmpnyCd, String siteCd, String targetUserCd, String notiType,
                              String title, String body, String payload, String dedupKey, String insertNo) {
        String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
        NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
        outbox.setNotiId(notiId);
        outbox.setCmpnyCd(cmpnyCd);
        outbox.setSiteCd(siteCd);
        outbox.setTargetUserCd(targetUserCd);
        outbox.setNotiType(notiType);
        outbox.setChannel(LeaveApprovalNotiConst.CHANNEL_PUSH);
        outbox.setTitle(title);
        outbox.setBody(body);
        outbox.setDataPayload(payload);
        outbox.setSendStatus(LeaveApprovalNotiConst.SEND_STATUS_PENDING);
        outbox.setDedupKey(dedupKey);
        outbox.setInsertNo(insertNo);
        leaveDashboardMapper.insertNotiOutbox(outbox);
    }

    /** 신청자명 평문 조회. 미존재/스코프 밖이면 빈 문자열(본문 합성용 안전 폴백). */
    private String resolveUserNm(String cmpnyCd, String userCd) {
        String nm = leaveApprovalNotiMapper.selectUserNm(cmpnyCd, userCd);
        return (nm == null) ? "" : nm;
    }

    /**
     * 시나리오 B 본문 합성. 단위(종일/반차/시간차)별 분기.
     * 날짜 YYYYMMDD→YYYY-MM-DD, 시각 HHMM→HH:MM, 일수는 불필요한 소수 0 제거.
     */
    private String buildUsedBody(String applicantNm, String useUnitType, BigDecimal leaveDays,
                                 String workYmd, String startTime, String endTime) {
        String date = toHyphenDate(workYmd);
        if (LeaveApprovalNotiConst.UNIT_HALF.equals(useUnitType)) {
            return String.format(LeaveApprovalNotiConst.USED_BODY_HALF_FORMAT, applicantNm, date);
        }
        if (!LeaveApprovalNotiConst.UNIT_FULL.equals(useUnitType)) {
            // 02/03/04 = 시간차
            return String.format(LeaveApprovalNotiConst.USED_BODY_HOURLY_FORMAT,
                    applicantNm, date, toColonTime(startTime), toColonTime(endTime));
        }
        // 종일(00)
        return String.format(LeaveApprovalNotiConst.USED_BODY_FULL_FORMAT,
                applicantNm, date, formatDays(leaveDays));
    }

    /** 시나리오 A DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildTurnPayload(String reqId, int approvalStep, String applicantUserCd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", LeaveApprovalNotiConst.NOTI_TYPE_APPROVAL_TURN);
        data.put("reqId", reqId);
        data.put("approvalStep", approvalStep);
        data.put("applicantUserCd", applicantUserCd);
        return serialize(data);
    }

    /** 시나리오 B DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildUsedPayload(String leaveId, String applicantUserCd, String workYmd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", LeaveApprovalNotiConst.NOTI_TYPE_USED_NO_APRV);
        data.put("leaveId", leaveId);
        data.put("applicantUserCd", applicantUserCd);
        data.put("workYmd", workYmd);
        return serialize(data);
    }

    private String serialize(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[leaveAprvNoti] payload 직렬화 실패", e);
            return "{}";
        }
    }

    /** YYYYMMDD → YYYY-MM-DD. 형식이 다르면 원본 반환. */
    private String toHyphenDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            return (yyyymmdd == null) ? "" : yyyymmdd;
        }
        return yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8);
    }

    /** HHMM → HH:MM. 형식이 다르면 원본 반환. */
    private String toColonTime(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) {
            return (hhmm == null) ? "" : hhmm;
        }
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2, 4);
    }

    /** 일수 표기: 불필요한 소수 0 제거(1.00000→"1", 0.50000→"0.5"). null→"0". */
    private String formatDays(BigDecimal days) {
        if (days == null) {
            return "0";
        }
        return days.stripTrailingZeros().toPlainString();
    }
}
