package com.prafta.common.schedule.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.push.PushSenderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FCM 공용 PUSH 전송 워커 스케줄러 (PRAFTA-COM-002). <b>기본 비활성(게이트)</b>.
 *
 * <p>설계: {@code .claude/requests/common/refs/prafta-com-002/01_작업지시서_FCM전송워커.md} §3, §4 /
 * decisions B-1(주기/게이트), B-3(동시성).
 *
 * <p>{@code prafta.push.worker.enabled=true} 일 때만 동작한다(미설정/false면 매 실행 즉시 건너뜀).
 * fixedDelay(기본 30초)는 직전 실행 종료 후 간격을 두므로 실행이 겹치지 않아(비중첩) 단일 인스턴스
 * 전제에서 동시성 위험이 낮다. 예외는 log.error 후 삼켜(다음 주기 재시도) 워커 루프가 죽지 않게 한다.
 * {@code @EnableScheduling} 은 {@code MainApplication} 에 이미 존재한다.
 *
 * <p>{@code LeaveGrantScheduler}(연차 정기부여) 게이트 패턴을 미러한다(단 cron 이 아닌 fixedDelay).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushSendScheduler {

    private final PushSenderService pushSenderService;

    /** 게이트: 기본 false(비활성). 운영 검증 후 true 로 켠다(마이그 선적용 필수). */
    @Value("${prafta.push.worker.enabled:false}")
    private boolean workerEnabled;

    /**
     * fixedDelay(기본 30초): 직전 실행 종료 시점 기준 간격. 비중첩이라 동시 실행이 없다.
     * {@code prafta.push.worker.interval-ms} 로 재정의 가능.
     */
    @Scheduled(fixedDelayString = "${prafta.push.worker.interval-ms:30000}")
    public void runPushWorker() {
        if (!workerEnabled) {
            log.debug("FCM 전송 워커 비활성(prafta.push.worker.enabled=false) — 건너뜀");
            return;
        }
        try {
            int sent = pushSenderService.dispatchPending();
            if (sent > 0) {
                log.info("FCM 전송 워커 1주기 완료. SENT {}건", sent);
            }
        } catch (Exception e) {
            log.error("FCM 전송 워커 실행 중 예외 발생", e);
        }
    }
}
