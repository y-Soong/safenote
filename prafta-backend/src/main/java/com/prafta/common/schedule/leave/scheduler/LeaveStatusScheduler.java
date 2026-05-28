package com.prafta.common.schedule.leave.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 부여 만료 배치 스케줄러.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8
 *
 * <p>매일 자정+5분(KST 기준)에 실행되어
 * {@code AVAIL_TO_DATE &lt; 오늘 AND STATUS='ACTIVE'}인 부여건을
 * STATUS='EXPIRED'로 일괄 전이한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveStatusScheduler {

    private final LeaveGrantStatusService leaveGrantStatusService;

    /**
     * cron: 초 분 시 일 월 요일 → 매일 00:05:00 실행.
     * Spring Scheduling은 서버 기본 TimeZone을 따른다. prafta는 Asia/Seoul 환경.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void expireOverdueGrants() {
        log.info("연차 만료 배치 시작");
        try {
            int affected = leaveGrantStatusService.expireOverdueGrants();
            log.info("연차 만료 배치 완료. 처리 건수={}", affected);
        } catch (Exception e) {
            log.error("연차 만료 배치 실행 중 예외 발생", e);
        }
    }
}
