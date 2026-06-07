package com.prafta.common.schedule.tbm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.tbm.tbm02.mapper.Tbm02Mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 15분 자동 교육시작 배치(prafta-051-06). <b>기본 비활성(게이트)</b>.
 *
 * <p>교육준비(OPENED) 전이 후 15분이 경과(PREP_START_AT + 15분 도래)하면 교육시작(IN_PROGRESS)
 * 으로 자동 전이한다. 매 주기 {@code bulkStartExpiredPrep} 한 번으로 전수 일괄 전이하며, UPDATE
 * WHERE 에 상태(OPENED)+시각(PREP_START_AT &lt;= NOW()-15분) 조건을 동시에 포함하므로 수동
 * 교육시작(start-session)/연장(extend-prep)과 원자적이다(경합 안전).
 *
 * <p>{@code prafta.tbm.autostart.enabled=true} 일 때만 동작(미설정/false 면 매 실행 즉시 건너뜀).
 * fixedDelay(기본 30초)는 직전 실행 종료 후 간격을 두므로 실행이 겹치지 않는다. 예외는 log.error
 * 후 삼켜(다음 주기 재시도) 워커 루프가 죽지 않게 한다. 게이트/패턴은 {@code PushSendScheduler} 를
 * 미러한다. {@code @EnableScheduling} 은 MainApplication 에 이미 존재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TbmAutoStartScheduler {

    private final Tbm02Mapper tbm02Mapper;

    /** 게이트: 기본 false(비활성). 마이그 선적용 + 운영 검증 후 true 로 켠다. */
    @Value("${prafta.tbm.autostart.enabled:false}")
    private boolean autoStartEnabled;

    /**
     * fixedDelay(기본 30초): 직전 실행 종료 시점 기준 간격. 비중첩이라 동시 실행이 없다.
     * {@code prafta.tbm.autostart.interval-ms} 로 재정의 가능.
     */
    @Scheduled(fixedDelayString = "${prafta.tbm.autostart.interval-ms:30000}")
    @Transactional
    public void runAutoStart() {
        if (!autoStartEnabled) {
            log.debug("TBM 자동 교육시작 배치 비활성(prafta.tbm.autostart.enabled=false) — 건너뜀");
            return;
        }
        try {
            int started = tbm02Mapper.bulkStartExpiredPrep();
            if (started > 0) {
                log.info("TBM 자동 교육시작 배치 1주기 완료. IN_PROGRESS 전이 {}건", started);
            }
        } catch (Exception e) {
            log.error("TBM 자동 교육시작 배치 실행 중 예외 발생", e);
        }
    }
}
