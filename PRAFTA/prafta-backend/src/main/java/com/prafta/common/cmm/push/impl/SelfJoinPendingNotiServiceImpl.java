package com.prafta.common.cmm.push.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.prafta.common.cmm.push.SelfJoinPendingNotiConst;
import com.prafta.common.cmm.push.SelfJoinPendingNotiService;
import com.prafta.common.cmm.push.mapper.PushTargetMapper;
import com.prafta.common.util.AuthRoleUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 셀프가입 승인 대기 통보(M6) PUSH 생산자 구현.
 *
 * <p>{@code RiskAssessNotiServiceImpl} 의 afterCommit + REQUIRES_NEW 격리 패턴을 그대로 따른다.
 * 가입 접수 트랜잭션이 확정 커밋된 뒤에만 outbox 를 적재한다(가입 접수 롤백 금지).
 *
 * <p><b>수신자</b> — 신청자 소속 노드 + 조상 노드의 정/부 관리자(재직·활성). 조회 게이트와 같은
 * 술어를 쓰는 {@code PushTargetMapper.selectNodeAdminChainUserCds} 단일 출처를 재사용한다.
 * 조상 체인 전체에 관리자가 0명일 때만 해당 사업장 master/hr 로 폴백한다.
 *
 * <p><b>★알려진 한계(설계상 수용)</b> — dedupKey 가 <b>수신자 + 날짜 + 사업장</b> 단위라,
 * <b>같은 날 두 번째 이후에 접수된 신청은 추가 알림을 만들지 않는다</b>(UNIQUE 충돌로 멱등 흡수).
 * 관리자는 하루 1건의 알림만 받고, 그 사이 쌓인 실제 대기 건수는 화면(승인 대기 탭)과 관리자
 * 런처 배지로 확인한다. 적재 후 본문을 갱신할 수 없으므로 메시지에 건수를 넣지 않는다 —
 * 넣으면 3명이 신청해도 영원히 "1건"으로 남는 거짓 정보가 된다.
 *
 * <p><b>로깅</b> — 신청자 이름/휴대폰/아이디를 남기지 않는다(사업장/부서/수신자 수만).
 */
@Slf4j
@Service
public class SelfJoinPendingNotiServiceImpl implements SelfJoinPendingNotiService {

    /** 수신자 0명일 때만 쓰는 폴백 역할 = 사업장 master/hr(실제 승인 가능자). */
    private static final List<String> FALLBACK_AUTH_CDS =
            List.of(AuthRoleUtils.AUTH_MASTER, AuthRoleUtils.AUTH_HR_MANAGER);

    /** dedupKey 날짜 축(서버 단일 타임존 규약 — AttdReminderServiceImpl 동일). */
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PushTargetMapper pushTargetMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    /** 자기 프록시 — afterCommit 콜백에서 REQUIRES_NEW 메서드를 프록시 경유로 호출(@Lazy 순환 차단). */
    private final SelfJoinPendingNotiService self;

    public SelfJoinPendingNotiServiceImpl(PushTargetMapper pushTargetMapper,
                                          LeaveDashboardMapper leaveDashboardMapper,
                                          ObjectMapper objectMapper,
                                          @Lazy SelfJoinPendingNotiService self) {
        this.pushTargetMapper = pushTargetMapper;
        this.leaveDashboardMapper = leaveDashboardMapper;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Override
    public void notifyJoinRequested(String cmpnyCd, String siteCd, String nodeCd, String applicantUserCd) {
        runAfterCommit(() -> self.runJoinRequestedOutbox(cmpnyCd, siteCd, nodeCd, applicantUserCd));
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
                            log.error("[selfJoinPendingNoti] afterCommit outbox 적재 실패(가입 접수 영향 없음)", e);
                        }
                    }
                });
            } else {
                task.run();
            }
        } catch (Exception e) {
            log.error("[selfJoinPendingNoti] PUSH 적재 등록 실패(가입 접수 영향 없음)", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void runJoinRequestedOutbox(String cmpnyCd, String siteCd, String nodeCd, String applicantUserCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank() || siteCd == null || siteCd.isBlank()) {
            log.info("[selfJoinPendingNoti] 식별자 부족으로 통보 생략 (siteCd={})", siteCd);
            return;
        }

        List<String> admins = resolveTargets(cmpnyCd, siteCd, nodeCd);
        if (admins == null || admins.isEmpty()) {
            // 부서 관리자도 사업장 master/hr 도 없는 상태. 예외를 던지지 않는다(가입 접수는 이미 성공).
            log.info("[selfJoinPendingNoti] 통보 대상 없음 (siteCd={}, nodeCd={})", siteCd, nodeCd);
            return;
        }

        String todayYmd = LocalDate.now().format(YMD);
        String payload = buildPayload(siteCd);
        int inserted = 0;

        for (String adminUserCd : admins) {
            if (adminUserCd == null || adminUserCd.isBlank()) {
                continue;
            }
            // ★수신자별로 다른 키여야 한다. UNIQUE 가 (CMPNY_CD, DEDUP_KEY) 회사 스코프라
            //   수신자를 빼면 첫 관리자 행만 저장되고 나머지는 조용히 흡수된다(무증상 누락).
            String dedupKey = buildDedupKey(adminUserCd, todayYmd, siteCd);
            try {
                NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
                outbox.setNotiId(leaveDashboardMapper.selectNextNotiId(cmpnyCd));
                outbox.setCmpnyCd(cmpnyCd);
                outbox.setSiteCd(siteCd);
                outbox.setTargetUserCd(adminUserCd);
                outbox.setNotiType(SelfJoinPendingNotiConst.NOTI_TYPE);
                outbox.setChannel(SelfJoinPendingNotiConst.CHANNEL_PUSH);
                outbox.setTitle(SelfJoinPendingNotiConst.TITLE);
                outbox.setBody(SelfJoinPendingNotiConst.BODY);
                outbox.setDataPayload(payload);
                outbox.setSendStatus(SelfJoinPendingNotiConst.SEND_STATUS_PENDING);
                outbox.setDedupKey(dedupKey);
                outbox.setInsertNo(applicantUserCd);
                leaveDashboardMapper.insertNotiOutbox(outbox);
                inserted++;
            } catch (DuplicateKeyException dup) {
                // 같은 날 이미 통보된 수신자 → 흡수하고 ★다음 수신자로 계속 진행(return 금지).
                log.debug("[selfJoinPendingNoti] 중복 적재 무시 (dedupKey={})", dedupKey);
            }
        }

        log.info("[selfJoinPendingNoti] 셀프가입 승인 대기 PUSH 적재 완료 (siteCd={}, nodeCd={}, 대상={}명, 신규={}건)",
                siteCd, nodeCd, admins.size(), inserted);
    }

    /**
     * 수신자 산출 — 부서(+조상) 정/부 관리자 우선, 0명일 때만 사업장 master/hr 폴백.
     *
     * <p>폴백은 "승인 가능자가 master/hr 뿐인 조직"을 구제하기 위한 것이다. 관리자가 1명이라도
     * 있으면 폴백하지 않는다 — 전사 발송 금지(결정 B).
     */
    private List<String> resolveTargets(String cmpnyCd, String siteCd, String nodeCd) {

        if (nodeCd != null && !nodeCd.isBlank()) {
            List<String> nodeAdmins = pushTargetMapper.selectNodeAdminChainUserCds(cmpnyCd, siteCd, nodeCd);
            if (nodeAdmins != null && !nodeAdmins.isEmpty()) {
                return nodeAdmins;
            }
        }

        List<String> fallback = pushTargetMapper.selectSiteRoleAdminUserCds(cmpnyCd, siteCd, FALLBACK_AUTH_CDS);
        log.info("[selfJoinPendingNoti] 부서(+상위) 정/부 관리자 0명 → 사업장 master/hr 폴백 (siteCd={}, nodeCd={}, 폴백대상={}명)",
                siteCd, nodeCd, fallback == null ? 0 : fallback.size());
        return fallback;
    }

    /**
     * dedupKey 생성 — {@code SELFJOIN_PEND_{targetUserCd}_{yyyyMMdd}_{siteCd}} (최대 94자).
     *
     * <p>정상 데이터에서는 상한(100자)에 도달할 수 없다(접두 14 + USER_CD 20 + 1 + 8 + 1 + SITE_CD 50).
     * 그래도 초과하면 <b>잘라내지 않는다</b> — 뒤를 자르면 서로 다른 수신자의 키가 같아져 알림이
     * 사라진다. 대신 사업장을 뺀 축약 키로 폴백하고 경고를 남긴다(수신자·날짜 축은 보존).
     */
    private String buildDedupKey(String adminUserCd, String todayYmd, String siteCd) {

        String key = SelfJoinPendingNotiConst.DEDUP_PREFIX + adminUserCd + "_" + todayYmd + "_" + siteCd;
        if (key.length() <= SelfJoinPendingNotiConst.DEDUP_KEY_MAX_LEN) {
            return key;
        }
        String shortened = SelfJoinPendingNotiConst.DEDUP_PREFIX + adminUserCd + "_" + todayYmd;
        log.warn("[selfJoinPendingNoti] dedupKey 길이 초과({}자) → 사업장 제외 축약 키로 폴백", key.length());
        return shortened;
    }

    /** DATA_PAYLOAD(라우팅 키만). 신청자 식별자·PII 미포함. 직렬화 실패 시 빈 객체 폴백. */
    private String buildPayload(String siteCd) {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", SelfJoinPendingNotiConst.NOTI_TYPE);
        data.put("siteCd", siteCd);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[selfJoinPendingNoti] payload 직렬화 실패 (siteCd={})", siteCd, e);
            return "{}";
        }
    }
}
