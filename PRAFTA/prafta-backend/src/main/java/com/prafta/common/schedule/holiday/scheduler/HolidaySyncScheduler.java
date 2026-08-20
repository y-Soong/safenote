package com.prafta.common.schedule.holiday.scheduler;

import java.time.Year;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.schedule.holiday.service.HolidaySyncService;

import lombok.extern.slf4j.Slf4j;

/**
 * 공휴일 동기화 스케줄러 (공공데이터포털 getRestDeInfo). <b>기본 비활성(게이트)</b>.
 *
 * <p><b>2026-08-20 복원</b>: 종전에는 {@code @Scheduled} 가 전부 주석 처리돼 <b>스케줄 자체가 등록되지
 * 않았다.</b> 그 결과 {@code syncYear} 의 유일한 호출 경로가 신규 고객사 프로비저닝 {@code afterCommit}
 * (올해+내년, best-effort)뿐이라, <b>회사가 새로 생기지 않으면 공휴일이 영영 갱신되지 않는</b> 상태였다.
 * 실제로 운영 데이터에 2028년 공휴일이 0건이다(2026·2027만 존재).
 *
 * <p>다른 스케줄러 13종의 관례에 맞춰 게이트({@code prafta.holiday.sync.enabled}) + cron 프로퍼티화로
 * 복원한다. 게이트가 꺼져 있으면 {@code log.debug} 만 남기고 즉시 반환한다(운영 로그 대조용 —
 * 지시서 §4 검증 방식).
 *
 * <p>★{@code holiday.api.service-key} 가 비어 있으면 API 호출이 실패한다. 켜기 전 운영 설정 파일에
 * 키가 있는지 반드시 확인할 것(실패해도 배치가 죽지 않고 로그만 남으므로 무증상이 될 수 있다).
 *
 * <p>상세: {@code .claude/refs/스케줄러_전면_활성화_작업지시서.md} §3-3
 */
@Slf4j
@Component
public class HolidaySyncScheduler {

    private final HolidaySyncService syncService;

    /** 게이트: 기본 false(비활성). 운영 검증 후 true 로 켠다. */
    @Value("${prafta.holiday.sync.enabled:false}")
    private boolean syncEnabled;

    public HolidaySyncScheduler(HolidaySyncService syncService) {
        this.syncService = syncService;
    }

    /** 내년 공휴일 동기화. 기본 매년 12/25 03:10(연말 확정 공고 이후). */
    @Scheduled(cron = "${prafta.holiday.sync.next-year-cron:0 10 3 25 12 *}")
    public void syncNextYear() {
        if (!syncEnabled) {
            log.debug("공휴일 동기화 비활성(prafta.holiday.sync.enabled=false) — 내년분 건너뜀");
            return;
        }
        int nextYear = Year.now().getValue() + 1;
        try {
            syncService.syncYear(nextYear);
            log.info("공휴일 동기화 완료 (대상연도={})", nextYear);
        } catch (Exception e) {
            // 스케줄 루프가 죽지 않도록 예외를 삼킨다(다음 주기 재시도).
            log.error("공휴일 동기화 실패 (대상연도={})", nextYear, e);
        }
    }

    /** 올해 공휴일 재동기화(대체공휴일 추가 지정 등 보정 대응). 기본 매월 1일 04:00. */
    @Scheduled(cron = "${prafta.holiday.sync.resync-cron:0 0 4 1 * *}")
    public void resyncThisYear() {
        if (!syncEnabled) {
            log.debug("공휴일 동기화 비활성(prafta.holiday.sync.enabled=false) — 올해분 건너뜀");
            return;
        }
        int year = Year.now().getValue();
        try {
            syncService.syncYear(year);
            log.info("공휴일 재동기화 완료 (대상연도={})", year);
        } catch (Exception e) {
            log.error("공휴일 재동기화 실패 (대상연도={})", year, e);
        }
    }
}
