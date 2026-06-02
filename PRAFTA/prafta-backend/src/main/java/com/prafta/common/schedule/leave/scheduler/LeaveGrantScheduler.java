package com.prafta.common.schedule.leave.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 자동 정기 연차 부여 스케줄러 (prafta-023 E). <b>기본 비활성(게이트)</b>.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.5~§8.5.8
 *
 * <p>설정 {@code prafta.leave.auto-grant.enabled=true} 일 때만 동작한다(미설정/false면 매 실행 즉시 건너뜀).
 * 무인(無人)으로 전 직원에게 부여가 나가므로, 부여 엔진 결과를 운영에서 검증한 뒤에만 활성화한다.
 * 활성 연차정책 보유 회사별로 입사일 보유 활성 직원에게 멱등 부여한다(중복 차단은 엔진 멱등키 §8.5.8).
 *
 * <p>한계(후속): 월차 per-월 누적은 미구현(엔진 당기 집계) — prafta-023 B 잔여.
 * 기존 만료 배치({@code LeaveStatusScheduler}, 00:05)와 동일한 @Scheduled 패턴을 따른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveGrantScheduler {

    private final LeaveGrantEngineService leaveGrantEngineService;

    /** 게이트: 기본 false(비활성). 운영에서 엔진 부여 결과 검증 후 true로 켠다. */
    @Value("${prafta.leave.auto-grant.enabled:false}")
    private boolean autoGrantEnabled;

    /**
     * 기본 매일 00:30 실행(만료 배치 00:05 이후). {@code prafta.leave.auto-grant.cron} 으로 재정의 가능.
     * cron: 초 분 시 일 월 요일. Spring Scheduling은 서버 기본 TimeZone(Asia/Seoul)을 따른다.
     */
    @Scheduled(cron = "${prafta.leave.auto-grant.cron:0 30 0 * * *}")
    public void runAutoGrant() {
        if (!autoGrantEnabled) {
            log.debug("자동 정기부여 배치 비활성(prafta.leave.auto-grant.enabled=false) — 건너뜀");
            return;
        }
        log.info("자동 정기부여 배치 시작");
        try {
            int granted = leaveGrantEngineService.runScheduledAutoGrant();
            log.info("자동 정기부여 배치 완료. 부여 {}명", granted);
        } catch (Exception e) {
            log.error("자동 정기부여 배치 실행 중 예외 발생", e);
        }
    }
}
