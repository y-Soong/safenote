package com.prafta.common.cmm.leave.promotion.service.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionNotiConst;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionNotiService;
import com.prafta.common.cmm.leave.promotion.vo.PromotionLogInsertVO;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 사용촉진 1차 통지 배치 구현 (PRAFTA-COM-008-A-2).
 *
 * <p>흐름: ① 마스터({@code tb_leave_promotion_log}) 1행 적재(STATUS=NOTICED, FIRST, NOTICED_DATE)
 *   → ② SYS045 LEAVE_PROMOTION_NOTICE PUSH outbox 적재(PENDING). 둘 다 멱등 키로 중복 차단.
 *
 * <p>멱등 전략: 마스터 INSERT 의 UNIQUE(CMPNY_CD, DEDUP_KEY) 충돌이 "이미 통지됨"의 단일 진실이다.
 *   마스터가 신규 적재된 경우에만 outbox 를 적재한다(같은 회차 1통 보장). outbox 자체도 DEDUP_KEY 를
 *   동일 값으로 두어 이중 안전망을 둔다.
 *
 * <p>예외 격리: 본 구현은 내부 예외를 흡수(로그만)한다. 배치의 한 사용자 실패가 다른 사용자를
 *   막지 않게 하기 위함이다(com-004 afterCommit 격리 철학). 트랜잭션은 호출부(배치)가 사용자 단위로 연다.
 *
 * <p>PII: 본문 합성에 평문 USER_NM(LeaveApprovalNotiMapper.selectUserNm) 만 사용. 복호화 호출 없음.
 *   DATA_PAYLOAD 에는 라우팅 키만 직렬화(평문 이름 미포함).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeavePromotionNotiServiceImpl implements LeavePromotionNotiService {

    private final LeavePromotionMapper leavePromotionMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final ObjectMapper objectMapper;

    @Override
    public int notifyFirstPromotion(PromotionTargetResult target, String today, String insertNo) {
        if (target == null) {
            return 0;
        }
        String cmpnyCd = target.cmpnyCd();
        String userCd = target.userCd();
        String availTo = target.baseAvailToDate();
        String dedupKey = buildNoticeDedupKey(userCd, availTo);
        try {
            // ① 마스터 1행 멱등 적재. 충돌(DuplicateKeyException) = 이미 통지됨 → outbox 도 건너뜀.
            boolean inserted = insertNoticeMaster(target, today, dedupKey, insertNo);
            if (!inserted) {
                log.info("[leavePromotion] 1차 통지 멱등 스킵(이미 통지됨) — userCd={}, availTo={}", userCd, availTo);
                return 0;
            }

            // ② PUSH outbox 적재(PENDING). outbox DEDUP_KEY 도 같은 회차 키.
            insertNoticeOutbox(target, dedupKey, insertNo);

            log.info("[leavePromotion] 1차 촉진 통지 적재 — userCd={}, availTo={}, 잔여={}",
                    userCd, availTo, formatDays(target.remainingDays()));
            return 1;
        } catch (Exception e) {
            // 한 사용자 실패가 배치를 막지 않도록 흡수.
            log.error("[leavePromotion] 1차 촉진 통지 적재 실패(배치 영향 없음) — cmpnyCd={}, userCd={}",
                    cmpnyCd, userCd, e);
            return 0;
        }
    }

    /**
     * 마스터 1행 적재. UNIQUE(CMPNY_CD, DEDUP_KEY) 충돌이면 false(이미 통지됨).
     *
     * @return 신규 적재 true / 멱등 충돌 false
     */
    private boolean insertNoticeMaster(PromotionTargetResult target, String today,
                                       String dedupKey, String insertNo) {
        PromotionLogInsertVO vo = new PromotionLogInsertVO();
        vo.setPromoId(leavePromotionMapper.selectNextPromoId(target.cmpnyCd()));
        vo.setCmpnyCd(target.cmpnyCd());
        vo.setSiteCd(target.siteCd());
        vo.setUserCd(target.userCd());
        vo.setBaseGrantId(target.baseGrantId());
        vo.setBaseAvailToDate(target.baseAvailToDate());
        vo.setPromoStage("FIRST");
        vo.setNoticedDate(today);
        vo.setStage1DesignatedDays(BigDecimal.ZERO);
        vo.setStage2TargetDays(BigDecimal.ZERO);
        vo.setStage2DesignatedDate(null);
        vo.setStatus("NOTICED");
        vo.setDedupKey(dedupKey);
        vo.setInsertNo(insertNo);
        try {
            leavePromotionMapper.insertPromotionLog(vo);
            return true;
        } catch (DuplicateKeyException dup) {
            return false;
        }
    }

    /** PUSH outbox 1행 적재(SEND_STATUS='PENDING'). 채번/INSERT 는 LeaveDashboardMapper 재사용. */
    private void insertNoticeOutbox(PromotionTargetResult target, String dedupKey, String insertNo) {
        String userNm = resolveUserNm(target.cmpnyCd(), target.userCd());
        String title = LeavePromotionNotiConst.NOTICE_TITLE;
        String body = String.format(LeavePromotionNotiConst.NOTICE_BODY_FORMAT,
                userNm, formatDays(target.remainingDays()));
        String payload = buildNoticePayload(target);

        try {
            NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
            outbox.setNotiId(leaveDashboardMapper.selectNextNotiId(target.cmpnyCd()));
            outbox.setCmpnyCd(target.cmpnyCd());
            outbox.setSiteCd(target.siteCd());
            outbox.setTargetUserCd(target.userCd());
            outbox.setNotiType(LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_NOTICE);
            outbox.setChannel(LeavePromotionNotiConst.CHANNEL_PUSH);
            outbox.setTitle(title);
            outbox.setBody(body);
            outbox.setDataPayload(payload);
            outbox.setSendStatus(LeavePromotionNotiConst.SEND_STATUS_PENDING);
            outbox.setDedupKey(dedupKey);
            outbox.setInsertNo(insertNo);
            leaveDashboardMapper.insertNotiOutbox(outbox);
        } catch (DuplicateKeyException dup) {
            // outbox 가 같은 회차로 이미 적재됨(이중 안전망 충돌) → 흡수.
            log.info("[leavePromotion] 1차 통지 outbox 중복 적재 무시 — userCd={}, dedupKey={}",
                    target.userCd(), dedupKey);
        }
    }

    /** 회차 멱등 키. 같은 사용자·같은 본연차 만료일 = 같은 1차 회차. */
    private String buildNoticeDedupKey(String userCd, String availTo) {
        return "PROMO_NOTICE_" + userCd + "_" + availTo;
    }

    /** 근로자명 평문 조회. 미존재/스코프 밖이면 빈 문자열(본문 합성 안전 폴백). */
    private String resolveUserNm(String cmpnyCd, String userCd) {
        String nm = leaveApprovalNotiMapper.selectUserNm(cmpnyCd, userCd);
        return (nm == null) ? "" : nm;
    }

    /** DATA_PAYLOAD(라우팅 키만, PII 미포함). 실패 시 빈 객체 폴백. */
    private String buildNoticePayload(PromotionTargetResult target) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", LeavePromotionNotiConst.NOTI_TYPE_PROMOTION_NOTICE);
        data.put("userCd", target.userCd());
        data.put("baseAvailToDate", target.baseAvailToDate());
        data.put("stage", "FIRST");
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[leavePromotion] payload 직렬화 실패 — userCd={}", target.userCd(), e);
            return "{}";
        }
    }

    /** 일수 표기: 불필요한 소수 0 제거(1.0→"1", 6.0→"6"). null→"0". */
    private String formatDays(BigDecimal days) {
        if (days == null) {
            return "0";
        }
        return days.stripTrailingZeros().toPlainString();
    }
}
