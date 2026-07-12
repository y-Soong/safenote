package com.prafta.common.schedule.site.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.site.service.SiteExpireService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-COM-001-T2-3 — 사업종료일 도래 사업장 자정 비활성 배치 스케줄러.
 *
 * <p>정책서: {@code .claude/context/policies/common} §6.1(종료일 경과 시 자동 비활성화),
 * §3.5(미사용=로그인 차단).
 *
 * <p>매일 자정(KST 기준)에 {@code END_DATE &lt;= 오늘 AND USE_YN='Y'} 인 사업장을
 * USE_YN='N' 으로 일괄 전이한다. {@code DailyUserExpireScheduler} 미러
 * (try-catch 예외 격리, 처리 건수 로깅).
 *
 * <p>운영 게이트: {@code prafta.site.expire.enabled}(기본 false). off 면 즉시 return.
 * (즉시 처리 분기는 저장 시 서비스에서 수행하고, 본 배치는 미래 종료일 도래분을 담당.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteExpireScheduler {

    private final SiteExpireService siteExpireService;

    /** 운영 게이트(기본 false). 운영 검증 후 true 로 전환. */
    @Value("${prafta.site.expire.enabled:false}")
    private boolean expireEnabled;

    /**
     * cron: 초 분 시 일 월 요일 → 매일 00:00:00 실행.
     * Spring Scheduling 은 서버 기본 TimeZone 을 따른다(prafta = Asia/Seoul).
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueSites() {
        if (!expireEnabled) {
            log.info("사업장 만료 배치 비활성(게이트 off) — 스킵");
            return;
        }
        log.info("사업장 만료 배치 시작");
        try {
            int affected = siteExpireService.expireOverdueSites();
            log.info("사업장 만료 배치 완료. 처리 건수={}", affected);
        } catch (Exception e) {
            log.error("사업장 만료 배치 실행 중 예외 발생", e);
        }
    }
}
