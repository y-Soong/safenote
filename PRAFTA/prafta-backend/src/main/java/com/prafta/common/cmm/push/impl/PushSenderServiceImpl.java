package com.prafta.common.cmm.push.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.push.FcmClient;
import com.prafta.common.cmm.push.FcmSendResult;
import com.prafta.common.cmm.push.PushSenderService;
import com.prafta.common.cmm.push.PushWorkerConst;
import com.prafta.common.cmm.push.mapper.PushOutboxMapper;
import com.prafta.common.cmm.push.vo.DeviceTokenVO;
import com.prafta.common.cmm.push.vo.PushOutboxRowVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FCM 공용 PUSH 전송 서비스 구현 (PRAFTA-COM-002, consumer).
 *
 * <p>처리 흐름(1주기):
 * <ol>
 *   <li>FCM 가용성 검사(키 부재/초기화 실패면 skip).</li>
 *   <li>selectPendingForSend(batchSize, maxRetry) 로 PENDING 배치 조회.</li>
 *   <li>행마다 claimSending(affected=1 만 처리) → 토큰 조회 → 디바이스별 전송.</li>
 *   <li>행 결과 종합으로 상태전이(SENT / FAILED / PENDING 재시도).</li>
 * </ol>
 *
 * <p>트랜잭션 경계: 배치를 한 트랜잭션으로 묶지 않는다(@Transactional 미부여).
 * SqlSessionTemplate 의 statement 단위 커밋으로 각 상태전이가 독립 반영되어
 * 부분 성공이 보존되고(중간 크래시에도 SENT 행은 유지), claim/상태가드가 멱등을 보장한다.
 *
 * <p>다중 디바이스 결과 우선순위: SUCCESS &gt; (영구실패) INVALID_TOKEN &gt; (재시도) TRANSIENT.
 * 하나라도 SUCCESS 면 SENT. SUCCESS 0 + TRANSIENT≥1 이면 재시도(혹은 한도초과 FAILED).
 * SUCCESS 0 + TRANSIENT 0(전부 INVALID) 이면 FAILED(ALL_TOKENS_INVALID).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSenderServiceImpl implements PushSenderService {

    private final PushOutboxMapper pushOutboxMapper;
    private final FcmClient fcmClient;
    private final ObjectMapper objectMapper;

    /** 1주기 claim 배치 건수. */
    @org.springframework.beans.factory.annotation.Value("${prafta.push.worker.batch-size:50}")
    private int batchSize;

    /** 최대 재시도 횟수(초과 시 FAILED 고정). */
    @org.springframework.beans.factory.annotation.Value("${prafta.push.worker.max-retry:3}")
    private int maxRetry;

    @Override
    public int dispatchPending() {
        if (!fcmClient.isAvailable()) {
            log.info("[push] FCM 미가용(키 부재/초기화 실패) — 발송 주기 skip.");
            return 0;
        }

        List<PushOutboxRowVO> rows = pushOutboxMapper.selectPendingForSend(batchSize, maxRetry);
        if (rows.isEmpty()) {
            return 0;
        }
        log.info("[push] 발송 대기 {}건 처리 시작(batchSize={}, maxRetry={}).", rows.size(), batchSize, maxRetry);

        int sentCount = 0;
        for (PushOutboxRowVO row : rows) {
            try {
                if (processRow(row)) {
                    sentCount++;
                }
            } catch (Exception e) {
                // 행 단위 예외는 격리: 다음 행 진행. 해당 행은 다음 주기에 재집힘(SENDING 잔류 가능 → §주의).
                log.error("[push] 발송 행 처리 중 예외(notiId={}): {}", row.getNotiId(), e.getMessage(), e);
            }
        }
        log.info("[push] 발송 주기 완료. SENT {}건 / 대상 {}건.", sentCount, rows.size());
        return sentCount;
    }

    /**
     * 1행 처리. claim → 토큰조회 → 전송 → 상태전이.
     *
     * @return SENT 로 전이되면 true.
     */
    private boolean processRow(PushOutboxRowVO row) {
        String notiId = row.getNotiId();
        String actor = PushWorkerConst.WORKER_ACTOR;

        // 1) claim: PENDING → SENDING (affected=1 만 이 워커가 처리; 멱등/중복발송 방지).
        if (pushOutboxMapper.claimSending(notiId, actor) != 1) {
            log.debug("[push] claim 실패(이미 처리/상태변경됨) notiId={} — skip.", notiId);
            return false;
        }

        // 2) DATA_PAYLOAD 파싱(실패는 재시도 무의미 → FAILED).
        Map<String, String> dataMap;
        try {
            dataMap = parsePayload(row.getDataPayload());
        } catch (Exception e) {
            log.warn("[push] DATA_PAYLOAD 파싱 실패 notiId={}: {}", notiId, e.getMessage());
            pushOutboxMapper.markFailed(notiId, row.getRetryCnt(),
                    PushWorkerConst.ERR_INVALID_PAYLOAD, actor);
            return false;
        }

        // 3) 대상 토큰 조회. 0건 → 즉시 FAILED(NO_DEVICE_TOKEN), 재시도 누적 없음.
        List<DeviceTokenVO> devices = pushOutboxMapper.selectDeviceTokens(row.getTargetUserCd());
        if (devices.isEmpty()) {
            log.info("[push] 대상 디바이스 없음 notiId={}, targetUserCd={} — FAILED(NO_DEVICE_TOKEN).",
                    notiId, row.getTargetUserCd());
            pushOutboxMapper.markFailed(notiId, row.getRetryCnt(),
                    PushWorkerConst.ERR_NO_DEVICE_TOKEN, actor);
            return false;
        }

        // 4) 디바이스별 전송 + 결과 종합.
        boolean anySuccess = false;
        boolean anyTransient = false;
        String lastTransientCode = null;
        for (DeviceTokenVO d : devices) {
            FcmSendResult result =
                    fcmClient.send(d.getPushToken(), row.getTitle(), row.getBody(), dataMap);
            switch (result) {
                case SUCCESS -> anySuccess = true;
                case INVALID_TOKEN -> pushOutboxMapper.softDeleteDeviceToken(d.getDeviceUuid(), actor);
                case TRANSIENT_FAILURE -> {
                    anyTransient = true;
                    lastTransientCode = "TRANSIENT_FAILURE";
                }
            }
        }

        // 5) 행 결과 상태전이.
        if (anySuccess) {
            pushOutboxMapper.markSent(notiId, actor);
            return true;
        }
        if (anyTransient) {
            // 일시 실패: PENDING 복귀(RETRY_CNT+1). 한도 도달 예정이면 FAILED 고정.
            int nextRetry = row.getRetryCnt() + 1;
            if (nextRetry >= maxRetry) {
                log.info("[push] 재시도 한도 도달 notiId={} (retry {}>={}) — FAILED.", notiId, nextRetry, maxRetry);
                pushOutboxMapper.markFailed(notiId, nextRetry, truncate(lastTransientCode), actor);
            } else {
                pushOutboxMapper.incrementRetryAndRevertPending(notiId, truncate(lastTransientCode), actor);
            }
            return false;
        }
        // SUCCESS 0 + TRANSIENT 0 → 전부 INVALID(이미 soft-delete됨) → 영구 실패.
        log.info("[push] 전 디바이스 토큰 무효 notiId={} — FAILED(ALL_TOKENS_INVALID).", notiId);
        pushOutboxMapper.markFailed(notiId, row.getRetryCnt(),
                PushWorkerConst.ERR_ALL_TOKENS_INVALID, actor);
        return false;
    }

    /** json 원문 → Map&lt;String,String&gt;. null/빈값이면 빈 맵(notification 만 전송). */
    private Map<String, String> parsePayload(String dataPayload) throws Exception {
        if (dataPayload == null || dataPayload.isBlank()) {
            return Map.of();
        }
        Map<String, String> parsed =
                objectMapper.readValue(dataPayload, new TypeReference<Map<String, String>>() {});
        return parsed == null ? Map.of() : parsed;
    }

    /** ERROR_MSG 컬럼(varchar500) 초과분 가드. */
    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() <= PushWorkerConst.ERROR_MSG_MAX_LEN
                ? msg
                : msg.substring(0, PushWorkerConst.ERROR_MSG_MAX_LEN);
    }
}
