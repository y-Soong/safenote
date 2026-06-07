package com.prafta.common.schedule.notice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.notice.notice01.mapper.Notice01Mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공지 확인이력 정리 배치 (PRAFTA-047-5). <b>기본 비활성(게이트)</b>.
 *
 * <p>팝업 종료(POPUP_TO_YMD)가 오늘 기준 6개월 경과한 공지의 tb_notice_user_ack 행을 물리 삭제한다.
 * 현행 노출 중 공지(today BETWEEN FROM~TO)는 POPUP_TO_YMD &lt; cutoff 조건으로 자동 배제되므로
 * CONFIRMED 이력 오삭제가 발생하지 않는다(요청서 §9 경고 준수).
 *
 * <p>{@code prafta.notice.ack-cleanup.enabled=true} 일 때만 동작(미설정/false 면 즉시 건너뜀).
 * 게이트/cron 패턴은 LeaveGrantScheduler / PushSendScheduler 를 미러한다.
 * {@code @EnableScheduling} 은 MainApplication 에 이미 존재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeAckCleanupScheduler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Notice01Mapper notice01Mapper;

    /** 게이트: 기본 false(비활성). 운영 검증 후 true 로 켠다. */
    @Value("${prafta.notice.ack-cleanup.enabled:false}")
    private boolean cleanupEnabled;

    /**
     * 일배치(기본 매일 04:10). {@code prafta.notice.ack-cleanup.cron} 으로 재정의 가능.
     */
    @Scheduled(cron = "${prafta.notice.ack-cleanup.cron:0 10 4 * * *}")
    @Transactional
    public void runCleanup() {
        if (!cleanupEnabled) {
            log.debug("공지 확인이력 정리 배치 비활성(prafta.notice.ack-cleanup.enabled=false) — 건너뜀");
            return;
        }
        try {
            // 오늘 - 6개월 = cutoff. POPUP_TO_YMD 가 이보다 과거인 공지의 이력만 삭제.
            String cutoffYmd = LocalDate.now().minusMonths(6).format(YMD);
            int deleted = notice01Mapper.deleteExpiredNoticeAck(cutoffYmd);
            log.info("공지 확인이력 정리 배치 완료 - cutoff={}, 삭제 {}건", cutoffYmd, deleted);
        } catch (Exception e) {
            log.error("공지 확인이력 정리 배치 실행 중 예외 발생", e);
        }
    }
}
