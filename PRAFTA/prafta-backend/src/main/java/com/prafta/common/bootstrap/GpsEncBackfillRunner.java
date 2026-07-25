package com.prafta.common.bootstrap;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.bootstrap.mapper.GpsEncBackfillMapper;
import com.prafta.common.bootstrap.result.GpsEncBackfillRow;
import com.prafta.common.security.crypto.GpsCoordCrypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GPS좌표-암호화-전환-05: 기존 평문 GPS 좌표 일회성 백필 러너(프로퍼티 게이트 — 선례
 * {@code PlatformOperatorBootstrapRunner} 패턴).
 *
 * <p>{@code prafta.gps-enc.backfill.enabled=true} 일 때만 빈으로 등록되어 기동 시 1회 동작한다.
 * 3테이블(TB_USER_ATTD_GPS / TB_TBM_ATTENDANCE / TB_TBM_SESSION)의
 * {@code *_ENC IS NULL AND 평문 존재} 행을 일괄 암호화해 암호문 컬럼을 채운다(운영 15+7+8=30행 —
 * 청크 불필요, 사용자 확정). 멱등: 재실행 시 대상 0건.
 *
 * <p>안전 장치:
 *   <ul>
 *     <li>행별 자가검증 — 암호화 직후 재복호화하여 정규화 평문(scale 7)과 equals 대조.
 *         불일치 시 예외로 전체 롤백(@Transactional).</li>
 *     <li>평문 컬럼은 건드리지 않는다 — 소거는 prafta-gps-enc-2-verify-and-purge.sql 수동 실행(4단계).</li>
 *     <li>로그는 테이블별 대상/성공 건수와 식별자 개수만 — 좌표 평문/암호문 값 절대 미출력.</li>
 *     <li>HTTP 표면 없음(러너) — 무인가 호출 원천 불가.</li>
 *   </ul>
 *
 * <p>실행 순서(plan §4-3): 코드 배포(fallback 운영 검증) 후 플래그 ON 재기동 → 로그 확인 → 플래그 OFF.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "prafta.gps-enc.backfill.enabled", havingValue = "true")
public class GpsEncBackfillRunner implements ApplicationRunner {

    private final GpsEncBackfillMapper gpsEncBackfillMapper;
    private final GpsCoordCrypto gpsCoordCrypto;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {

        log.info("[GPS백필] 시작 — 3테이블(*_ENC IS NULL 행만, 멱등)");

        int attdCnt = backfillTable("TB_USER_ATTD_GPS",
                gpsEncBackfillMapper.selectAttdGpsTargets(),
                (row, latEnc, lonEnc) -> gpsEncBackfillMapper.updateAttdGpsEnc(
                        row.cmpnyCd(), row.rowKey(), latEnc, lonEnc));

        int tbmAttdCnt = backfillTable("TB_TBM_ATTENDANCE",
                gpsEncBackfillMapper.selectTbmAttendanceTargets(),
                (row, latEnc, lonEnc) -> gpsEncBackfillMapper.updateTbmAttendanceEnc(
                        row.cmpnyCd(), row.rowKey(), latEnc, lonEnc));

        int tbmSessCnt = backfillTable("TB_TBM_SESSION",
                gpsEncBackfillMapper.selectTbmSessionTargets(),
                (row, latEnc, lonEnc) -> gpsEncBackfillMapper.updateTbmSessionEnc(
                        row.cmpnyCd(), row.rowKey(), latEnc, lonEnc));

        log.info("[GPS백필] 완료 — attdGps={}건, tbmAttendance={}건, tbmSession={}건",
                attdCnt, tbmAttdCnt, tbmSessCnt);
        log.warn("[GPS백필] ★ 백필 완료. 즉시 prafta.gps-enc.backfill.enabled 플래그를 끄세요(false). "
                + "재기동 시 멱등(0건) 처리되지만, 플래그는 일회성 원칙상 비활성으로 되돌린다. "
                + "이후 검증·평문 소거는 sql/migration/prafta-gps-enc-2-verify-and-purge.sql 수동 절차를 따른다.");
    }

    /**
     * 단일 테이블 백필 공통 처리: 행별 암호화 + 자가검증 + UPDATE.
     * 실패(자가검증 불일치/UPDATE 0건) 시 예외 전파로 전체 롤백. 좌표값 로그 출력 금지(식별자만).
     */
    private int backfillTable(String tableName, List<GpsEncBackfillRow> targets, EncUpdate updater) {

        if (targets == null || targets.isEmpty()) {
            log.info("[GPS백필] {} — 대상 0건(이미 백필됨 또는 평문 없음)", tableName);
            return 0;
        }

        int success = 0;
        for (GpsEncBackfillRow row : targets) {
            // 1) scale 7 정규화 평문(자가검증 기준값) + 암호화.
            String latPlain = gpsCoordCrypto.normalize(row.lat());
            String lonPlain = gpsCoordCrypto.normalize(row.lon());
            String latEnc = gpsCoordCrypto.encrypt(row.lat());
            String lonEnc = gpsCoordCrypto.encrypt(row.lon());

            // 2) 행별 자가검증: 재복호화 = 정규화 평문 equals. 불일치 시 전체 롤백(값 미출력, 식별자만).
            if (!latPlain.equals(gpsCoordCrypto.decryptToPlain(latEnc))
                    || !lonPlain.equals(gpsCoordCrypto.decryptToPlain(lonEnc))) {
                log.error("[GPS백필] 자가검증 불일치 — table={}, cmpnyCd={}, rowKey={} (전체 롤백)",
                        tableName, row.cmpnyCd(), row.rowKey());
                throw new IllegalStateException("GPS 백필 자가검증 실패: " + tableName);
            }

            // 3) 암호문 채움(평문 무변경). WHERE *_ENC IS NULL 재확인으로 경합/중복 채움 방지.
            int updated = updater.apply(row, latEnc, lonEnc);
            if (updated != 1) {
                log.error("[GPS백필] UPDATE 영향행 이상 — table={}, cmpnyCd={}, rowKey={}, updated={} (전체 롤백)",
                        tableName, row.cmpnyCd(), row.rowKey(), updated);
                throw new IllegalStateException("GPS 백필 UPDATE 실패: " + tableName);
            }
            success++;
        }

        log.info("[GPS백필] {} — 대상 {}건 / 성공 {}건", tableName, targets.size(), success);
        return success;
    }

    /** 테이블별 UPDATE 위임 함수형 인터페이스(행 + 암호문 쌍 → 영향행 수). */
    @FunctionalInterface
    private interface EncUpdate {
        int apply(GpsEncBackfillRow row, String latEnc, String lonEnc);
    }
}
