package com.prafta.common.cmm.sms;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.sms.mapper.SmsSendResultMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 발송 결과를 <b>독립 트랜잭션</b>으로 기록하는 전용 빈.
 *
 * <p>★{@code AuthCodeSmsDispatcher} 안의 메서드로 두지 않고 별도 빈으로 분리한 이유:
 *    같은 클래스 내부에서 {@code this.markResult()} 를 호출하면 Spring AOP 프록시를 우회해
 *    {@code REQUIRES_NEW} 가 <b>조용히 무시</b>된다(plan §3 T7). 그 경우 호출자 트랜잭션이 롤백되면
 *    {@code SEND_STATUS='FAILED'} 기록까지 함께 사라져 요청서 §7-3 요구가 무너진다.
 *
 * <p>진입점 A·B 는 트랜잭션이 없어 REQUIRES_NEW 여도 자연 동작하고,
 *    진입점 C 는 호출자 트랜잭션이 있을 수 있어 독립 커밋이 필수다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSendResultRecorder {

    private final SmsSendResultMapper smsSendResultMapper;

    /**
     * 발송 결과를 새 트랜잭션에서 커밋한다. 호출자 트랜잭션의 롤백에 영향을 받지 않는다.
     *
     * <p>★기록 실패가 사용자 요청을 죽이지 않게 예외를 삼킨다(발송 자체는 이미 끝난 상태).
     *
     * <p>SMS2-D2(sec M-2): 별도 발송 로그 테이블을 만들지 않았으므로(1차 D3) 이 UPDATE 는
     *    <b>발송 사실의 유일한 기록</b>이다. 따라서 두 실패를 로그에서 반드시 구분한다.
     *    <ul>
     *      <li>{@code updated == 0} — 기록 대상 행이 없음(refKey 불일치 / 이미 PENDING 아님 = 중복 기록 시도)</li>
     *      <li>예외 — 기록 자체가 실패(컬럼 길이 1406, DB 단절 등). <b>스택트레이스 필수</b></li>
     *    </ul>
     *    예외 쪽에는 grep 가능한 고정 프리픽스 {@code [SMS기록실패]} 를 붙인다
     *    ({@code AiQuotaServiceImpl} 의 "고정 문구 유지" 선례 미러 — 운영 로그 검색이 유일한 탐지 수단).
     *
     * <p>운영 점검 쿼리(기록 실패로 PENDING 이 고착된 행 탐지):
     * <pre>
     *   SELECT COUNT(1) FROM TB_SMS_AUTH_CODE
     *    WHERE SEND_STATUS = 'PENDING' AND INSERT_DATE &lt; NOW() - INTERVAL 10 MINUTE;
     * </pre>
     * ★PENDING 은 레이트리밋 카운트에 포함되므로, 고착이 누적되면 정상 사용자가 발송을 못 하게 된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markResult(String refKey, SmsSendResult result) {
        try {
            int updated = smsSendResultMapper.updateSmsSendResult(
                refKey
                , result.status().name()
                , result.messageKey()
                , result.errCd()
                , result.errMsg()
            );
            if (updated == 0) {
                // refKey 불일치 또는 이미 PENDING 이 아님(중복 기록 시도). 기록 실패와는 원인이 다르다.
                log.warn("[SMS기록없음] SMS 발송 결과 기록 대상 없음(refKey 불일치 또는 이미 PENDING 아님) - refKey={}, 상태={}",
                    refKey, result.status());
            }
        } catch (Exception e) {
            // ★스택트레이스를 반드시 남긴다. 클래스명만 남기면 1406(컬럼 길이 초과) 같은 원인을 특정할 수 없어
            //   PENDING 고착의 근본 원인을 영영 못 찾는다.
            log.error("[SMS기록실패] SMS 발송 결과 기록 실패(행이 PENDING 으로 남는다) - refKey={}, 상태={}",
                refKey, result.status(), e);
        }
    }
}
