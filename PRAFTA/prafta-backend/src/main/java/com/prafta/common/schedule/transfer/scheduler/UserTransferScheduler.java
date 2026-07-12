package com.prafta.common.schedule.transfer.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.TransferReservationExecRow;
import com.prafta.web.user.user01.service.User01TransferExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 소속이동 발효 자정 배치 — PRAFTA-WEB_001-2(Terminal B). <b>기본 비활성(게이트)</b>.
 *
 * <p>매일 자정에 발효일 도래(MOVE_DATE &lt;= 오늘) RESERVED 예약을 전수 조회한 뒤
 * 예약 1건씩 {@link User01TransferExecutionService#executeReservation} 로 위임한다.
 * 한 예약의 실패가 다른 예약/회사를 막지 않도록 행 단위로 예외를 격리(try-catch)하고,
 * 실패분은 별도 트랜잭션으로 STATUS='FAILED'(+FAIL_REASON) 처리한다.
 *
 * <p>운영 게이트: {@code prafta.transfer.execute.enabled}(기본 false). off 면 즉시 return.
 * cron 은 {@code prafta.transfer.execute.cron} 으로 재정의 가능(기본 매일 00:10).
 * {@code SiteExpireScheduler}/{@code DefaultSchGenScheduler} 패턴 미러.
 *
 * <p>멱등: 발효 완료분은 STATUS='APPLIED' 가 되어 {@code selectDueReservations} 에서 제외된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTransferScheduler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserTransferMapper userTransferMapper;
    private final User01TransferExecutionService transferExecutionService;

    /** 운영 게이트(기본 false). 운영 검증 후 true 로 전환. */
    @Value("${prafta.transfer.execute.enabled:false}")
    private boolean executeEnabled;

    /**
     * 기본 매일 00:10 실행({@code prafta.transfer.execute.cron} 재정의 가능).
     * cron: 초 분 시 일 월 요일. Spring Scheduling 은 서버 기본 TimeZone(Asia/Seoul)을 따른다.
     */
    @Scheduled(cron = "${prafta.transfer.execute.cron:0 10 0 * * *}")
    public void executeDueTransfers() {
        if (!executeEnabled) {
            log.info("소속이동 발효 배치 비활성(prafta.transfer.execute.enabled=false) — 스킵");
            return;
        }

        String today = LocalDate.now().format(YMD);
        log.info("소속이동 발효 배치 시작 — 기준일(today)={}", today);

        List<TransferReservationExecRow> dueList = userTransferMapper.selectDueReservations(today);
        int success = 0;
        int skipped = 0;
        int failed = 0;
        for (TransferReservationExecRow row : dueList) {
            try {
                boolean applied = transferExecutionService.executeReservation(row);
                if (applied) {
                    success++;
                } else {
                    // 선점 패배/이미 처리(RESERVED 아님) — 이중작업 방지로 빠진 건(실패 아님).
                    skipped++;
                }
            } catch (Exception e) {
                // 행 단위 격리 — 해당 예약만 FAILED, 다른 예약 무영향(재검증 실패 사유는 FAIL_REASON 에 적재).
                failed++;
                log.error("소속이동 발효 실패(격리) — cmpnyCd={}, reservationId={}, userCd={}",
                        row.cmpnyCd(), row.reservationId(), row.userCd(), e);
                try {
                    transferExecutionService.markFailed(
                            row.cmpnyCd(), row.reservationId(), summarize(e), row.insertNo());
                } catch (Exception markEx) {
                    log.error("소속이동 발효 실패 마킹 자체 오류 — reservationId={}", row.reservationId(), markEx);
                }
            }
        }

        log.info("소속이동 발효 배치 완료 — 대상 {}건, 성공 {}건, 스킵 {}건, 실패 {}건",
                dueList.size(), success, skipped, failed);
    }

    /** 예외 요약(클래스명 + 메시지). FAIL_REASON 저장용. */
    private static String summarize(Exception e) {
        String msg = e.getMessage();
        return e.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }
}
