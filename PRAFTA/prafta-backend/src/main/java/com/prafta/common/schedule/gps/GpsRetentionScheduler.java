package com.prafta.common.schedule.gps;

import java.util.function.IntSupplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.schedule.gps.mapper.GpsRetentionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위치정보(GPS 좌표) 보존기간 파기 배치. <b>기본 비활성(게이트) + 기본 dry-run</b>.
 *
 * <h3>★왜 필요한가</h3>
 * 위치기반서비스 이용약관 제7조 ① 이 <b>"수집일로부터 3년간 보존 후 파기"</b> 를 이용자에게
 * 약속하고 있는데, 정작 파기하는 코드가 없었다. 위치정보법 제23조(개인위치정보의 파기)는
 * 보유목적 달성 시 지체 없는 파기를 의무로 두고 있어, 약속만 있고 이행이 없는 상태는
 * 실태 점검에서 그대로 지적 대상이 된다.
 *
 * <h3>★대상 3종</h3>
 * <ul>
 *   <li>{@code TB_USER_ATTD_GPS} — 출퇴근(출역) 좌표</li>
 *   <li>{@code TB_TBM_ATTENDANCE} — TBM 입실 시 참석자 좌표</li>
 *   <li>{@code TB_TBM_SESSION} — TBM 세션 개설 관리자 좌표</li>
 * </ul>
 * TBM 두 테이블은 종전 약관에 언급이 없었으나 실제로는 좌표를 저장하고 있었다.
 * 약관 제7조를 확장하면서 파기 대상에 함께 편입했다(2026-09-01).
 *
 * <h3>★행이 아니라 좌표만 지운다</h3>
 * 세 테이블 모두 외근 사유·교육 이수 기록 등 <b>파기 대상이 아닌 업무 기록</b>을 함께 담고 있다.
 * 좌표 컬럼(평문/암호문 쌍)만 NULL 로 만들어 "어디였는지"만 없앤다.
 * 읽기 경로는 좌표 결측 행을 이미 정상 처리한다({@code GpsCoordCrypto} fallback 규칙 —
 * 좌표 없는 행이 운영에 이미 존재한다).
 *
 * <h3>★게이트 2단</h3>
 * <ol>
 *   <li>{@code prafta.gps.retention.enabled} — 기본 {@code false}. 꺼져 있으면 아무것도 하지 않는다.</li>
 *   <li>{@code prafta.gps.retention.dry-run} — 기본 {@code true}. 켜도 <b>건수만 로그로 남기고 파기하지 않는다.</b></li>
 * </ol>
 * 되돌릴 수 없는 파기라 <b>먼저 enabled 만 켜서 며칠간 대상 건수를 관찰</b>하고,
 * 예상과 맞는 것을 확인한 뒤 dry-run 을 내리는 순서를 의도한 것이다.
 *
 * <h3>★{@code @Transactional} 을 붙이지 않는다</h3>
 * 배치 단위로 나눠 커밋하는 것이 목적이다. 전체를 한 트랜잭션으로 묶으면 긴 트랜잭션과 넓은 락으로
 * 근태 체크·TBM 입실 흐름이 그 뒤에 줄을 선다. 중간에 실패해도 이미 파기한 만큼은 유지되고
 * 다음 실행이 이어서 파기한다(멱등).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GpsRetentionScheduler {

    /**
     * 1회 실행의 최대 반복 횟수(무한 루프 방지). {@code batchSize} 와 곱해 1회 실행의 파기 상한이 된다.
     * 남은 분량은 다음 실행이 이어서 처리한다.
     */
    private static final int MAX_LOOP_PER_TABLE = 50;

    private final GpsRetentionMapper gpsRetentionMapper;

    /** 게이트: 기본 false(비활성). */
    @Value("${prafta.gps.retention.enabled:false}")
    private boolean retentionEnabled;

    /** dry-run: 기본 true. 대상 건수만 로그로 남기고 실제 파기는 하지 않는다. */
    @Value("${prafta.gps.retention.dry-run:true}")
    private boolean dryRun;

    /**
     * 보존 개월수. 기본 36개월(= 약관 제7조 ① 의 3년).
     * ★일수가 아니라 개월수인 이유: 윤년이 섞이면 "3년"이 1095일이 되기도 1096일이 되기도 한다.
     *   {@code INTERVAL n MONTH} 는 달력 기준이라 이 흔들림이 없다.
     */
    @Value("${prafta.gps.retention.months:36}")
    private int retentionMonths;

    /** 1회 UPDATE 상한(행). 기본 1000. */
    @Value("${prafta.gps.retention.batch-size:1000}")
    private int batchSize;

    /** 일배치(기본 매일 04:40 — SMS 정리 04:20 직후). */
    @Scheduled(cron = "${prafta.gps.retention.cron:0 40 4 * * *}")
    public void runRetention() {

        if (!retentionEnabled) {
            log.debug("위치정보 파기 배치 비활성(prafta.gps.retention.enabled=false) - 건너뜀");
            return;
        }
        if (retentionMonths <= 0 || batchSize <= 0) {
            // ★설정 오타로 "전량 파기" 가 되지 않게 막는다. 0/음수는 실행하지 않는다.
            log.error("위치정보 파기 배치 설정 오류 - months={}, batchSize={} (둘 다 1 이상이어야 한다). 실행하지 않는다"
                    , retentionMonths, batchSize);
            return;
        }

        try {
            int attdGps = gpsRetentionMapper.countExpiredAttdGps(retentionMonths);
            int tbmAttendance = gpsRetentionMapper.countExpiredTbmAttendanceGps(retentionMonths);
            int tbmSession = gpsRetentionMapper.countExpiredTbmSessionGps(retentionMonths);

            if (dryRun) {
                log.info("위치정보 파기 배치 [dry-run] - 보존={}개월, 대상: 출퇴근좌표 {}건 / TBM입실좌표 {}건 / TBM개설자좌표 {}건."
                        + " 실제 파기는 하지 않았다(prafta.gps.retention.dry-run=true)"
                        , retentionMonths, attdGps, tbmAttendance, tbmSession);
                return;
            }

            int purgedAttdGps = purge("TB_USER_ATTD_GPS"
                    , () -> gpsRetentionMapper.purgeAttdGps(retentionMonths, batchSize));
            int purgedTbmAttendance = purge("TB_TBM_ATTENDANCE"
                    , () -> gpsRetentionMapper.purgeTbmAttendanceGps(retentionMonths, batchSize));
            int purgedTbmSession = purge("TB_TBM_SESSION"
                    , () -> gpsRetentionMapper.purgeTbmSessionGps(retentionMonths, batchSize));

            log.info("위치정보 파기 배치 완료 - 보존={}개월, 출퇴근좌표 {}/{}건, TBM입실좌표 {}/{}건, TBM개설자좌표 {}/{}건 파기"
                    , retentionMonths
                    , purgedAttdGps, attdGps
                    , purgedTbmAttendance, tbmAttendance
                    , purgedTbmSession, tbmSession);
        } catch (Exception e) {
            // ★배치 예외가 스케줄러 스레드를 죽이지 않게 흡수한다(다른 스케줄러 선례 동일).
            log.error("위치정보 파기 배치 실행 중 예외 발생", e);
        }
    }

    /** 배치 LIMIT 로 나눠 파기하고 총 파기 행 수를 반환한다. */
    private int purge(String tableName, IntSupplier batch) {
        int total = 0;
        for (int i = 0; i < MAX_LOOP_PER_TABLE; i++) {
            int purged = batch.getAsInt();
            total += purged;
            if (purged < batchSize) {
                return total;
            }
        }
        log.warn("위치정보 파기 - {} 의 대상이 1회 실행 상한({}행)을 넘었다. 남은 분량은 다음 실행에서 처리한다"
                , tableName, MAX_LOOP_PER_TABLE * batchSize);
        return total;
    }
}
