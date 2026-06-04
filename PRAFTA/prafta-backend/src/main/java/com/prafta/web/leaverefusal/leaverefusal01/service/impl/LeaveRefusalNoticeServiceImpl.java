package com.prafta.web.leaverefusal.leaverefusal01.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.vo.NotiOutboxInsertVO;
import com.prafta.common.cmm.leave.vo.RefusalLogInsertVO;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.leaverefusal.LeaveRefusalErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.leaverefusal.leaverefusal01.application.model.LeaveRefusalNoticeModel;
import com.prafta.web.leaverefusal.leaverefusal01.application.param.LeaveRefusalNoticeParam;
import com.prafta.web.leaverefusal.leaverefusal01.dto.response.LeaveRefusalNoticeResponse;
import com.prafta.web.leaverefusal.leaverefusal01.service.LeaveRefusalNoticeService;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 노무수령거부 통지 발송 구현 (PRAFTA-COM-001 기능1).
 *
 * <p>권한 게이트: master/hr 만 허용(위반 시 403). 대상별로 outbox(PENDING) + 로그(NOTICED)를
 * 한 트랜잭션으로 적재하며, dedup UNIQUE 로 멱등이다(이미 통지된 건은 건너뛰고 에러 아님).
 *
 * <p>공용 매퍼({@link LeaveRefusalMapper})와 outbox 재사용({@link LeaveDashboardMapper})으로
 * 신규 INSERT SQL 난립을 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRefusalNoticeServiceImpl implements LeaveRefusalNoticeService {

    private final LeaveRefusalMapper leaveRefusalMapper;
    private final LeaveDashboardMapper leaveDashboardMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LeaveRefusalNoticeResponse sendNotices(LeaveRefusalNoticeParam param) {
        // 권한 게이트: master/hr 만 통지 발송 가능.
        if (!AuthRoleUtils.isManager(param.gvAuthCd())) {
            log.warn("[leaveRefusal] 통지 발송 권한 없음 (userCd={}, authCd={})", param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(LeaveRefusalErrorCode.LEAVEREFUSAL_403_001);
        }

        String cmpnyCd = param.gvCmpnyCd();
        String operatorUserCd = param.gvUserCd();

        // IDOR 가드: INSERT 전에 모든 대상의 회사 스코프/정합/실재를 일괄 검증.
        // 하나라도 회사 밖/부정합/미존재면 처리 0건으로 전체 거부(fail-closed, 부분 주입 방지).
        for (LeaveRefusalNoticeModel m : param.targets()) {
            int valid = leaveRefusalMapper.countValidTarget(cmpnyCd, m.siteCd(), m.userCd());
            if (valid <= 0) {
                log.warn("[leaveRefusal] 통지 대상 정합성 검증 실패 — 전체 거부 (cmpnyCd={}, siteCd={}, userCd={})",
                        cmpnyCd, m.siteCd(), m.userCd());
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001,
                        "통지 대상이 회사 소속이 아니거나 존재하지 않습니다(userCd=" + m.userCd() + ")");
            }
        }

        int noticed = 0;

        for (LeaveRefusalNoticeModel m : param.targets()) {
            boolean inserted = sendOne(cmpnyCd, operatorUserCd, m);
            if (inserted) {
                noticed++;
            }
        }

        log.info("[leaveRefusal] 통지 발송 완료 (cmpnyCd={}, 요청={}, 신규적재={})",
                cmpnyCd, param.targets().size(), noticed);

        return LeaveRefusalNoticeResponse.builder()
                .requestedCount(param.targets().size())
                .noticedCount(noticed)
                .build();
    }

    /**
     * 단건 통지: outbox(PENDING) + 로그(NOTICED) 적재. 멱등(중복 dedup 흡수).
     *
     * @return 신규 적재면 true, 이미 통지됨(dedup 충돌)이면 false
     */
    private boolean sendOne(String cmpnyCd, String operatorUserCd, LeaveRefusalNoticeModel m) {
        String dedupOutbox = "LRN_" + m.userCd() + "_" + m.targetYmd();

        // 1) outbox INSERT(PENDING). dedup 충돌이면 이미 통지된 것 → 멱등 종료.
        String notiId = leaveDashboardMapper.selectNextNotiId(cmpnyCd);
        NotiOutboxInsertVO outbox = new NotiOutboxInsertVO();
        outbox.setNotiId(notiId);
        outbox.setCmpnyCd(cmpnyCd);
        outbox.setSiteCd(m.siteCd());
        outbox.setTargetUserCd(m.userCd());
        outbox.setNotiType(LeaveRefusalConst.NOTI_TYPE_NOTICE);
        outbox.setChannel(LeaveRefusalConst.CHANNEL_PUSH);
        outbox.setTitle(LeaveRefusalConst.NOTICE_TITLE);
        outbox.setBody(String.format(LeaveRefusalConst.NOTICE_BODY_FORMAT, m.targetYmd()));
        outbox.setDataPayload(buildNoticePayload(m.targetYmd()));
        outbox.setSendStatus(LeaveRefusalConst.SEND_STATUS_PENDING);
        outbox.setDedupKey(dedupOutbox);
        outbox.setInsertNo(operatorUserCd);
        try {
            leaveDashboardMapper.insertNotiOutbox(outbox);
        } catch (DuplicateKeyException dup) {
            log.info("[leaveRefusal] 통지 중복(이미 발송) 건너뜀 (userCd={}, targetYmd={})", m.userCd(), m.targetYmd());
            return false;
        }

        // 2) 사실 로그 INSERT(NOTICED). 멱등(ON DUPLICATE KEY UPDATE).
        RefusalLogInsertVO log0 = new RefusalLogInsertVO();
        log0.setRefusalId(leaveRefusalMapper.selectNextRefusalId(cmpnyCd));
        log0.setCmpnyCd(cmpnyCd);
        log0.setSiteCd(m.siteCd());
        log0.setUserCd(m.userCd());
        log0.setTargetYmd(m.targetYmd());
        log0.setEventType(LeaveRefusalConst.EVENT_NOTICED);
        log0.setRelatedNotiId(notiId);
        log0.setDedupKey(cmpnyCd + "_" + m.userCd() + "_" + m.targetYmd() + "_" + LeaveRefusalConst.EVENT_NOTICED);
        log0.setInsertNo(operatorUserCd);
        leaveRefusalMapper.insertRefusalLog(log0);

        return true;
    }

    /** 근로자 통지 DATA_PAYLOAD JSON 직렬화(Jackson). 실패 시 빈 객체 폴백. PII 평문 미포함. */
    private String buildNoticePayload(String targetYmd) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", LeaveRefusalConst.NOTI_TYPE_NOTICE);
        data.put("targetYmd", targetYmd);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("[leaveRefusal] 통지 payload 직렬화 실패 (targetYmd={})", targetYmd, e);
            return "{}";
        }
    }
}
