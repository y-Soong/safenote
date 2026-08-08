package com.prafta.common.schedule.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.schedule.sms.mapper.SmsRetentionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMS 인증 관련 보존기간 정리 배치 — 4차 / sec T-11 · T-3. <b>기본 비활성(게이트)</b>.
 *
 * <h3>★왜 필요한가</h3>
 * <ul>
 *   <li>{@code TB_SMS_VERIFY_ATTEMPT} — 3차에 신설됐고 <b>판정은 최근 1시간만 보는데 행은 무기한 누적</b>된다.
 *       무인증 EP 가 쓰기를 유발하는 구조라 방치하면 계속 커진다(sec T-3).</li>
 *   <li>{@code TB_SMS_AUTH_CODE} — 1차부터 있던 테이블로 보존/파기 정책이 정의된 적이 없다(sec 별건 백로그 5).</li>
 * </ul>
 * 둘 다 평문 PII 는 없지만(HMAC·AES-GCM) <b>특정 번호의 인증/검증 이력</b>이라 준식별 데이터다
 * (공통 정책서 §11.3 — 보존기간 정의 필요).
 *
 * <h3>★게이트/토글</h3>
 * {@code prafta.sms.retention.enabled=true} 일 때만 동작한다(미설정/false 면 즉시 건너뜀).
 * 게이트·cron 패턴은 {@code NoticeAckCleanupScheduler} / {@code LeaveGrantScheduler} 를 미러한다.
 * {@code @EnableScheduling} 은 {@code MainApplication} 에 이미 존재한다.
 * <p>★<b>운영 검증 후 수동 ON</b>: 첫 실행 전에 반드시 삭제 대상 건수를 SELECT 로 먼저 확인할 것
 *    (배포 절차서 §8). 되돌릴 수 없는 물리 삭제다.
 *
 * <h3>★{@code @Transactional} 을 붙이지 않는다</h3>
 * 배치 단위로 <b>나눠 커밋</b>하는 것이 이 배치의 목적이다. 전체를 한 트랜잭션으로 묶으면
 * 긴 트랜잭션 + 넓은 갭락이 생겨 인증 흐름 전체(발송 가드의 정책행 직렬화 포함)가 그 뒤에 줄을 선다.
 * 중간에 실패해도 이미 지운 만큼은 유지되며 다음 실행이 이어서 지운다(멱등).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRetentionScheduler {

    /**
     * 1회 실행의 최대 반복 횟수(무한 루프 방지). {@code batchSize} 와 곱해 1회 실행의 삭제 상한이 된다.
     * ★상한을 두는 이유: 예상 밖의 대량 적재가 있어도 배치 1회가 DB 를 장시간 점유하지 않게 한다.
     *   남은 분량은 다음 실행이 이어서 지운다.
     */
    private static final int MAX_LOOP_PER_TABLE = 50;

    private final SmsRetentionMapper smsRetentionMapper;

    /** 게이트: 기본 false(비활성). 운영에서 대상 건수 확인 후 true 로 켠다. */
    @Value("${prafta.sms.retention.enabled:false}")
    private boolean retentionEnabled;

    /** 보존일수. 기본 90일. */
    @Value("${prafta.sms.retention.days:90}")
    private int retentionDays;

    /** 1회 DELETE 상한(행). 기본 1000. */
    @Value("${prafta.sms.retention.batch-size:1000}")
    private int batchSize;

    /** 일배치(기본 매일 04:20). {@code prafta.sms.retention.cron} 으로 재정의 가능. */
    @Scheduled(cron = "${prafta.sms.retention.cron:0 20 4 * * *}")
    public void runRetention() {

        if (!retentionEnabled) {
            log.debug("SMS 보존기간 정리 배치 비활성(prafta.sms.retention.enabled=false) - 건너뜀");
            return;
        }
        if (retentionDays <= 0 || batchSize <= 0) {
            // ★설정 오타로 "전량 삭제" 가 되지 않게 막는다. 0/음수는 실행하지 않는다.
            log.error("SMS 보존기간 정리 배치 설정 오류 - days={}, batchSize={} (둘 다 1 이상이어야 한다). 실행하지 않는다",
                    retentionDays, batchSize);
            return;
        }

        try {
            int attempts = purge("TB_SMS_VERIFY_ATTEMPT",
                    () -> smsRetentionMapper.deleteOldVerifyAttempts(retentionDays, batchSize));
            int authCodes = purge("TB_SMS_AUTH_CODE",
                    () -> smsRetentionMapper.deleteOldAuthCodes(retentionDays, batchSize));

            log.info("SMS 보존기간 정리 배치 완료 - 보존={}일, 검증시도 {}건, 인증코드 {}건 삭제",
                    retentionDays, attempts, authCodes);
        } catch (Exception e) {
            // ★배치 예외가 스케줄러 스레드를 죽이지 않게 흡수한다(다른 스케줄러 선례 동일).
            log.error("SMS 보존기간 정리 배치 실행 중 예외 발생", e);
        }
    }

    /** 배치 LIMIT 로 나눠 삭제하고 총 삭제 행 수를 반환한다. */
    private int purge(String tableName, DeleteBatch batch) {
        int total = 0;
        for (int i = 0; i < MAX_LOOP_PER_TABLE; i++) {
            int deleted = batch.run();
            total += deleted;
            if (deleted < batchSize) {
                return total;
            }
        }
        log.warn("SMS 보존기간 정리 - {} 의 삭제 대상이 1회 실행 상한({}행)을 넘었다. 남은 분량은 다음 실행에서 처리한다",
                tableName, MAX_LOOP_PER_TABLE * batchSize);
        return total;
    }

    /** 배치 DELETE 1회를 감싸는 함수형 인터페이스(테이블별 매퍼 호출 위임). */
    @FunctionalInterface
    private interface DeleteBatch {
        int run();
    }
}
