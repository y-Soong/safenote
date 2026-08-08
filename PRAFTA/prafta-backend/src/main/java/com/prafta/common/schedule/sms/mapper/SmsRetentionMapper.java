package com.prafta.common.schedule.sms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SMS 인증 관련 테이블 보존기간 정리(purge) 매퍼 — 4차 / sec T-11 · T-3.
 *
 * <p>대상
 * <ul>
 *   <li>{@code TB_SMS_VERIFY_ATTEMPT} — 검증 시도 로그. 판정은 최근 1시간만 보는데 행은 무기한 누적됐다.</li>
 *   <li>{@code TB_SMS_AUTH_CODE} — 인증코드. 상한 판정은 최대 1일, 화면 통계는 24시간만 본다.</li>
 * </ul>
 *
 * <p>★둘 다 HMAC·암호화 컬럼만 담고 평문 PII 는 없지만, <b>특정 번호의 인증/검증 이력</b>이라
 *    준식별 데이터에 해당한다(공통 정책서 §11.3 보존기간 정의 필요).
 *
 * <p>★★삭제는 <b>배치 LIMIT</b> 로 나눠 실행한다. 한 문장으로 수십만 행을 지우면
 *    긴 트랜잭션과 넓은 갭락이 생겨 인증 흐름 전체가 그 뒤에 줄을 선다.
 */
@Mapper
public interface SmsRetentionMapper {

    /**
     * 보존기간이 지난 검증 시도 로그 삭제(1회 호출 = 최대 {@code batchSize} 행).
     *
     * @param retentionDays 보존일수
     * @param batchSize     1회 삭제 상한
     * @return 실제 삭제된 행 수(0 이면 더 이상 대상 없음)
     */
    int deleteOldVerifyAttempts(@Param("retentionDays") int retentionDays
            , @Param("batchSize") int batchSize);

    /**
     * 보존기간이 지난 인증코드 삭제(1회 호출 = 최대 {@code batchSize} 행).
     *
     * <p>★{@code INSERT_DATE} 기준이다. 상한 판정(최대 1일)·화면 통계(24시간) 어느 쪽도
     *    보존일수(기본 90일) 안쪽이라 삭제가 판정에 영향을 주지 않는다.
     *
     * @param retentionDays 보존일수
     * @param batchSize     1회 삭제 상한
     * @return 실제 삭제된 행 수(0 이면 더 이상 대상 없음)
     */
    int deleteOldAuthCodes(@Param("retentionDays") int retentionDays
            , @Param("batchSize") int batchSize);
}
