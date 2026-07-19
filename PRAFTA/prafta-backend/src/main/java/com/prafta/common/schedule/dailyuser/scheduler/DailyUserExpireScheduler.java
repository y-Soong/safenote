package com.prafta.common.schedule.dailyuser.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.cmm.dailyuser.service.DailyUserExpireService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-app-027-1 — 일용직 만료 계정 자정 비활성 배치 스케줄러.
 *
 * <p>정책서: {@code .claude/context/policies/common/05-slot-management.md} §5.6(자정 만료 배치),
 * {@code common/03-account-auth.md} §3.5(미사용=로그인 차단).
 *
 * <p>매일 자정(KST 기준)에 {@code WORK_EXPIRE_DATE &lt; 오늘 AND USE_YN='Y'} 인 일용직을
 * USE_YN='N' + ACCOUNT_STATUS='05'(비활성화)로 일괄 전이한다. {@link LeaveStatusScheduler} 미러
 * (try-catch 예외 격리, 처리 건수 로깅).
 *
 * <p>운영 게이트: {@code prafta.daily-user.expire.enabled}(기본 false). off 면 즉시 return.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyUserExpireScheduler {

    private final DailyUserExpireService dailyUserExpireService;
    // 일용직 계약서+승인제(D7) — 미사용 입장 승인요청의 당일 자정 만료 편승.
    private final DailyEntryService dailyEntryService;

    /** 운영 게이트(기본 false). 운영 검증 후 true 로 전환. */
    @Value("${prafta.daily-user.expire.enabled:false}")
    private boolean expireEnabled;

    /**
     * cron: 초 분 시 일 월 요일 → 매일 00:00:00 실행.
     * Spring Scheduling 은 서버 기본 TimeZone 을 따른다(prafta = Asia/Seoul).
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueDailyUsers() {
        if (!expireEnabled) {
            log.info("일용직 만료 배치 비활성(게이트 off) — 스킵");
            return;
        }
        log.info("일용직 만료 배치 시작");
        try {
            int affected = dailyUserExpireService.expireOverdueDailyUsers();
            log.info("일용직 만료 배치 완료. 처리 건수={}", affected);
        } catch (Exception e) {
            log.error("일용직 만료 배치 실행 중 예외 발생", e);
        }

        // 입장 승인제(D7) — 요청일이 지난 대기('01')/승인('02') 승인요청 일괄 만료('04') 편승.
        // 계정 만료 트랜잭션과 분리된 자체 트랜잭션 + try-catch 로 상호 예외 격리
        // (서비스 트랜잭션 내부 편승 시 실패가 rollback-only 로 계정 만료까지 오염시키는 것을 방지).
        try {
            int expiredRequests = dailyEntryService.expireOverdueRequests();
            log.info("일용직 입장 승인요청 만료 처리 완료. 처리 건수={}", expiredRequests);
        } catch (Exception e) {
            log.error("일용직 입장 승인요청 만료 처리 중 예외 발생(계정 만료 처리와 격리)", e);
        }
    }
}
