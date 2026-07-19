package com.prafta.common.cmm.dailyentry.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.dailyentry.application.command.EntryNotiOutboxCommand;
import com.prafta.common.cmm.dailyentry.application.command.EntryRequestInsertCommand;
import com.prafta.common.cmm.dailyentry.application.query.EntryRequestListQuery;
import com.prafta.common.cmm.dailyentry.mapper.DailyEntryMapper;
import com.prafta.common.cmm.dailyentry.result.EntryLoginDecision;
import com.prafta.common.cmm.dailyentry.result.EntryLoginDecisionType;
import com.prafta.common.cmm.dailyentry.result.EntryRequestMetaResult;
import com.prafta.common.cmm.dailyentry.result.EntryRequestRow;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.dailyentry.DailyEntryErrorCode;
import com.prafta.common.error.dailylogin.DailyLoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일용직 입장 승인요청 core 서비스 구현.
 *
 * <p>상태 모델(plan §1, SYS082): 01 대기 / 02 승인 / 03 거부 / 04 만료 / 05 소진.
 * <p>인가 가드: 처리자는 master/hr 역할이어야 하며, master 외에는 해당 사업장의
 * TB_USER_SITE_AUTH(USE_YN='Y') 보유가 필수(승인 관리 패턴 미러 — 일용직은 노드 미배정이라 노드 스코프 제외).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyEntryServiceImpl implements DailyEntryService {

    private final DailyEntryMapper dailyEntryMapper;
    private final ObjectMapper objectMapper;

    /** [SYS082] 요청 상태 — 대기/승인. */
    private static final String REQ_STATUS_PENDING = "01";
    private static final String REQ_STATUS_APPROVED = "02";

    /** [SYS045] 알림 유형 — 일용직 입장 승인요청(사업장 관리자 대상). */
    private static final String NOTI_TYPE_ENTRY_REQ = "DAILY_ENTRY_REQ";

    /** 승인/거부 권한 역할(사업장 인가 가드 — master/hr, 승인 관리 패턴 미러). */
    private static final List<String> ENTRY_ADMIN_AUTH_CDS =
            List.of(AuthRoleUtils.AUTH_MASTER, AuthRoleUtils.AUTH_HR_MANAGER);

    /** 거부 사유 최대 길이(DDL varchar(200) 정합). */
    private static final int REJECT_REASON_MAX_LEN = 200;

    @Override
    public EntryLoginDecision findLoginDecision(String cmpnyCd, String userCd) {
        // open(01/02) 요청은 기능성 유니크(UX_DAILY_ENTRY_REQ_OPEN)로 계정당 최대 1건.
        // D7 코드 자체 보장 — 승인('02')은 REQ_DTIME 당일만 유효(XML WHERE 조건). 어제 승인 건은
        // 만료 배치 미가동이어도 여기서 제외 → NONE → 새 요청 생성 경로(과거 open 잔존 시 멱등 흡수 + 006).
        EntryRequestMetaResult open = dailyEntryMapper.selectOpenEntryRequest(cmpnyCd, userCd);
        if (open != null) {
            if (REQ_STATUS_APPROVED.equals(open.reqStatus())) {
                return new EntryLoginDecision(EntryLoginDecisionType.APPROVED, open.reqId());
            }
            return new EntryLoginDecision(EntryLoginDecisionType.PENDING, open.reqId());
        }
        // 당일 거부 이력 — 신규 요청 미생성 + 007 안내(효력=당일, 익일부터 새 요청 허용 — plan §1).
        if (dailyEntryMapper.selectTodayRejectedCnt(cmpnyCd, userCd) > 0) {
            return new EntryLoginDecision(EntryLoginDecisionType.REJECTED_TODAY, null);
        }
        return new EntryLoginDecision(EntryLoginDecisionType.NONE, null);
    }

    @Override
    @Transactional
    public void createEntryRequest(String cmpnyCd, String siteCd, String userCd, String reqType) {
        // 멱등 — open(01/02) 요청 존재 시 신규 생성 없이 종료(중복 요청 금지, plan §1).
        if (dailyEntryMapper.selectOpenEntryRequest(cmpnyCd, userCd) != null) {
            log.info("일용직 입장 승인요청 생성 스킵(open 요청 존재) — cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return;
        }
        // 당일 거부 — 같은 날 재요청 금지(007). 익일 로그인 시도부터 새 요청 생성 허용.
        if (dailyEntryMapper.selectTodayRejectedCnt(cmpnyCd, userCd) > 0) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_007);
        }

        String reqId = dailyEntryMapper.selectEntryRequestId(cmpnyCd);
        try {
            dailyEntryMapper.insertEntryRequest(
                    new EntryRequestInsertCommand(cmpnyCd, reqId, siteCd, userCd, reqType));
        } catch (DuplicateKeyException e) {
            // UX_DAILY_ENTRY_REQ_OPEN 경합(동시 생성) — open 존재와 동일 취급(멱등).
            log.info("일용직 입장 승인요청 동시 생성 경합 흡수 — cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return;
        }

        // 사업장 관리자 대상 푸시 outbox 적재(best-effort — 실패해도 요청 생성에는 영향 없음).
        enqueueSiteAdminPush(cmpnyCd, siteCd, reqId, reqType, userCd);

        log.info("일용직 입장 승인요청 생성 완료 — cmpnyCd={}, userCd={}, reqId={}, reqType={}",
                cmpnyCd, userCd, reqId, reqType);
    }

    @Override
    @Transactional
    public int consumeApprovedRequest(String cmpnyCd, String reqId, String userCd) {
        // '02' 승인 → '05' 소진 조건부 UPDATE. 호출자(재활성 트랜잭션)에 참여(REQUIRED)한다.
        return dailyEntryMapper.updateEntryRequestConsume(cmpnyCd, reqId, userCd);
    }

    @Override
    public List<EntryRequestRow> selectEntryRequests(EntryRequestListQuery query, String procUserCd, String procAuthCd) {
        if (query.siteCd() == null || query.siteCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 사업장 인가 가드 — 조회도 처리와 동일 기준으로 차단(타 사업장 목록 열람 방지).
        assertSiteAuthority(query.cmpnyCd(), procUserCd, procAuthCd, query.siteCd());

        return dailyEntryMapper.selectEntryRequestList(query);
    }

    @Override
    @Transactional
    public int approveRequests(String cmpnyCd, List<String> reqIds, String procUserCd, String procAuthCd) {
        if (reqIds == null || reqIds.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        int processed = 0;
        for (String reqId : reqIds) {
            processOne(cmpnyCd, reqId, procUserCd, procAuthCd, true, null);
            processed++;
        }

        log.info("일용직 입장 승인 처리 완료 — cmpnyCd={}, procUserCd={}, 처리건수={}", cmpnyCd, procUserCd, processed);
        return processed;
    }

    @Override
    @Transactional
    public void rejectRequest(String cmpnyCd, String reqId, String reason, String procUserCd, String procAuthCd) {
        // 거부 사유 필수(D10 — 사유 기록) + 길이 제한(DDL varchar(200), truncation 500 방지).
        if (reason == null || reason.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (reason.length() > REJECT_REASON_MAX_LEN) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        processOne(cmpnyCd, reqId, procUserCd, procAuthCd, false, reason);

        log.info("일용직 입장 거부 처리 완료 — cmpnyCd={}, reqId={}, procUserCd={}", cmpnyCd, reqId, procUserCd);
    }

    @Override
    @Transactional
    public int expireOverdueRequests() {
        // D7 — 요청일이 지난 대기(01)/승인(02) 요청 일괄 만료(04). 전 회사 대상(만료 배치 미러).
        return dailyEntryMapper.updateExpireOverdueEntryRequests("SYSTEM");
    }

    /**
     * 승인/거부 단건 처리 — 행 잠금 조회 + 사업장 인가 가드 + 대기('01') 상태 검증 + 조건부 전이.
     * 실패 시 예외로 전체 롤백(일괄 처리 all-or-nothing).
     */
    private void processOne(String cmpnyCd, String reqId, String procUserCd, String procAuthCd,
            boolean approve, String reason) {
        if (reqId == null || reqId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // FOR UPDATE 잠금 조회 — 동시 승인/거부/소진 직렬화. 미존재(타 회사 포함)는 404(존재 비노출).
        EntryRequestMetaResult meta = dailyEntryMapper.selectEntryRequestMetaForUpdate(cmpnyCd, reqId);
        if (meta == null) {
            throw new ApiException(DailyEntryErrorCode.DAILYENTRY_404_001);
        }

        // 사업장 인가 가드(IDOR) — 요청의 SITE_CD 기준으로 처리자의 권한을 재검증.
        assertSiteAuthority(cmpnyCd, procUserCd, procAuthCd, meta.siteCd());

        if (!REQ_STATUS_PENDING.equals(meta.reqStatus())) {
            throw new ApiException(DailyEntryErrorCode.DAILYENTRY_400_001);
        }

        int updated = approve
                ? dailyEntryMapper.updateEntryRequestApprove(cmpnyCd, reqId, procUserCd)
                : dailyEntryMapper.updateEntryRequestReject(cmpnyCd, reqId, reason, procUserCd);
        if (updated <= 0) {
            // 잠금 조회 이후 전이 실패는 정합 깨짐 — 이미 처리됨으로 통일 응답.
            throw new ApiException(DailyEntryErrorCode.DAILYENTRY_400_001);
        }
    }

    /**
     * 사업장 인가 가드 — 처리자는 master/hr 역할 + (master 외) 해당 사업장 TB_USER_SITE_AUTH(USE_YN='Y') 보유.
     * authCd/userCd 는 JWT 클레임 도출값만 신뢰한다(클라 바디 신뢰 금지).
     */
    private void assertSiteAuthority(String cmpnyCd, String userCd, String authCd, String siteCd) {
        if (!hasSiteAuthority(cmpnyCd, userCd, authCd, siteCd)) {
            throw new ApiException(DailyEntryErrorCode.DAILYENTRY_403_001);
        }
    }

    /**
     * 사업장 인가 보유 여부 — 승인/거부 가드와 동일 기준(단일 출처).
     * T3(계약서 도메인)가 계약서 관리·서명본 열람 인가에 재사용한다(예외 코드는 호출 도메인이 매핑).
     */
    @Override
    public boolean hasSiteAuthority(String cmpnyCd, String userCd, String authCd, String siteCd) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("일용직 관리 권한 없음(역할 차단) — userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            return false;
        }
        if (!AuthRoleUtils.AUTH_MASTER.equals(authCd)
                && dailyEntryMapper.selectSiteAuthCnt(cmpnyCd, userCd, siteCd) <= 0) {
            log.warn("일용직 관리 권한 없음(사업장 권한 미보유) — userCd={}, siteCd={}", userCd, siteCd);
            return false;
        }
        return true;
    }

    /**
     * 승인요청 발생 시 해당 사업장 관리자(master/hr ∩ SITE_AUTH) 대상 푸시 outbox 적재.
     *
     * <p>nearmiss01 enqueueSafetyManagerPush 미러 — 대상자별 1행, DEDUP_KEY 충돌은 흡수.
     * 푸시 본문에 일용직 이름 등 PII 는 싣지 않는다(잠금화면 노출 대비 최소화 — §11.1).
     * 적재 실패는 요청 생성에 영향을 주지 않는다(best-effort).
     */
    private void enqueueSiteAdminPush(String cmpnyCd, String siteCd, String reqId, String reqType, String insertNo) {
        try {
            List<String> targets = dailyEntryMapper.selectSiteEntryAdmins(cmpnyCd, siteCd, ENTRY_ADMIN_AUTH_CDS);
            if (targets == null || targets.isEmpty()) {
                log.info("일용직 입장 승인요청 푸시 대상 없음 — cmpnyCd={}, siteCd={}, reqId={}", cmpnyCd, siteCd, reqId);
                return;
            }

            String title = "일용직 입장 승인요청";
            String body = "일용직 입장 승인 요청이 접수되었습니다. 확인 후 처리해 주세요.";
            String payload = buildPayload(reqId, reqType);

            for (String targetUserCd : targets) {
                EntryNotiOutboxCommand cmd = new EntryNotiOutboxCommand(
                        cmpnyCd
                        , siteCd
                        , targetUserCd
                        , NOTI_TYPE_ENTRY_REQ
                        , title
                        , body
                        , payload
                        , "DAILY_ENTRY_REQ_" + reqId + "_" + targetUserCd
                        , insertNo);
                try {
                    dailyEntryMapper.insertEntryNotiOutbox(cmd);
                } catch (DuplicateKeyException dup) {
                    // 동일 이벤트 중복 적재(UNIQUE CMPNY_CD,DEDUP_KEY) → 흡수하고 다음 대상 진행.
                    log.info("일용직 입장 승인요청 푸시 중복 적재 무시 — reqId={}, target={}", reqId, targetUserCd);
                }
            }
            log.info("일용직 입장 승인요청 푸시 적재 완료 — reqId={}, 대상={}건", reqId, targets.size());
        } catch (Exception e) {
            log.error("일용직 입장 승인요청 푸시 적재 실패(요청 생성 영향 없음) — cmpnyCd={}, reqId={}", cmpnyCd, reqId, e);
        }
    }

    /** 푸시 data payload 직렬화(수동 문자열 조립 금지 — ObjectMapper 사용). */
    private String buildPayload(String reqId, String reqType) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("reqId", reqId);
        data.put("reqType", reqType);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("일용직 입장 승인요청 푸시 payload 직렬화 실패 — reqId={}", reqId, e);
            return "{}";
        }
    }
}
