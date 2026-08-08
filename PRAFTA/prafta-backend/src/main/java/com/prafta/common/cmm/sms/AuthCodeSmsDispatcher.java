package com.prafta.common.cmm.sms;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.prafta.common.error.sms.SmsErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.normalize.Normalizers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증번호 SMS 발송 디스패처. 발송 호출부 3곳(baseinfo / 앱 마이페이지 / 플랫폼 위치열람)의 공통 진입점이다.
 *
 * <p>표준 흐름
 * <pre>
 *   String refKey = dispatcher.newRefKey();        // ① INSERT 전에 생성
 *   mapper.insert...(... , refKey);                 // ② 인증코드 INSERT(커밋)
 *   dispatcher.dispatch(refKey, phoneNorm, code, validMinutes);  // ③ 발송 + 결과 기록
 * </pre>
 *
 * <p>★refKey 를 INSERT 전에 만들어 왕복시키는 이유: 인증코드 커맨드들이 전부 Java record(불변)라
 *    {@code useGeneratedKeys}/{@code keyProperty} 로 PK 를 되받을 수 없다(setter 부재). record 를
 *    mutable class 로 바꾸는 것보다 refKey 왕복이 diff 가 훨씬 작고 안전하다(plan §3 T9).
 *
 * <p>★트랜잭션: 본 빈에는 {@code @Transactional} 을 붙이지 않는다. 외부 HTTP 호출을 트랜잭션 안에
 *    가두면 DB 커넥션을 네트워크 대기 동안 점유하고, 롤백 시 실패 기록까지 사라진다(요청서 §7-3).
 *    결과 기록만 {@link SmsSendResultRecorder}(REQUIRES_NEW)에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCodeSmsDispatcher {

    /** refKey 길이(뿌리오 제약: 32자 이내). TB_SMS_AUTH_CODE.SEND_REF_KEY varchar(32) 와 일치. */
    private static final int REF_KEY_LENGTH = 32;

    /** refKey 문자 집합(영숫자). 벤더/DB 어느 쪽에서도 이스케이프 이슈가 없는 범위만 사용. */
    private static final char[] REF_KEY_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SmsSender smsSender;
    private final SmsSendResultRecorder smsSendResultRecorder;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 발송 추적키 생성(32자 영숫자).
     * ★인증코드 INSERT <b>전에</b> 생성해 INSERT 문에 함께 넣어야 한다(발송 결과 UPDATE 의 조인키).
     */
    public String newRefKey() {
        StringBuilder sb = new StringBuilder(REF_KEY_LENGTH);
        for (int i = 0; i < REF_KEY_LENGTH; i++) {
            sb.append(REF_KEY_CHARS[secureRandom.nextInt(REF_KEY_CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * 인증번호 문자 발송 + 결과 기록.
     *
     * <p>동작
     * <ol>
     *   <li>게이트 OFF/설정 미완 → {@code SKIPPED} 기록 후 정상 반환(<b>예외 없음</b>).
     *       기존과 동일한 성공 응답이 나가야 한다(요청서 §7-7, 무회귀 최우선).</li>
     *   <li>발송 시도 → 결과를 <b>먼저</b> 기록(독립 커밋) → 실패면 그때 {@link ApiException} 을 던진다.
     *       순서가 뒤집히면 FAILED 기록이 사라진다.</li>
     * </ol>
     *
     * @param refKey       {@link #newRefKey()} 로 만들어 INSERT 에 함께 저장한 값
     * @param phoneNorm    수신번호(하이픈 없는 숫자만)
     * @param authCode     서버 생성 6자리 인증번호. ★로그 금지
     * @param validMinutes 코드 유효 시간(분). 진입점 A·C = 1, 진입점 B(앱 마이페이지) = 3
     * @return 발송 상태(SENT 또는 SKIPPED). FAILED 는 예외로 전환되므로 반환되지 않는다
     * @throws ApiException SMS_502_001/002/003 — 발송 실패
     */
    public SmsSendStatus dispatch(String refKey, String phoneNorm, String authCode, int validMinutes) {

        String phoneLast4 = Normalizers.last4(phoneNorm);

        if (!smsSender.isEnabled()) {
            smsSendResultRecorder.markResult(refKey, SmsSendResult.skipped());
            log.info("SMS 인증번호 발송 스킵(게이트 OFF) - refKey={}, mblLast4={}", refKey, phoneLast4);
            return SmsSendStatus.SKIPPED;
        }

        // ★본문은 고정 템플릿 + 서버 생성 코드만(사용자 입력 유입 없음). 본문 자체는 로그 금지(인증번호 포함).
        String content = AuthCodeMessageTemplate.build(authCode, validMinutes);

        // SMS2-D2(sec M-3): markResult 를 try/finally 로 감싸 어떤 경로로 빠져나가도 정확히 1회 기록되게 한다.
        // ★기존 순서(기록 → 예외)는 유지한다. finally 에서 기록하고 그 뒤에 ApiException 을 던지므로 동일하다.
        // ★markResult 자체는 예외를 던지지 않으므로(SmsSendResultRecorder 가 전면 흡수) finally 가 원래 예외를 덮지 않는다.
        SmsSendResult result = null;
        try {
            result = smsSender.send(new SmsSendRequest(refKey, phoneNorm, content));
        } catch (RuntimeException e) {
            // 벤더 클라이언트는 실패를 결과 객체로 돌려주도록 설계돼 있지만(예외/결과 이중 채널 방지),
            // 예상 밖의 RuntimeException(NPE·직렬화·프록시 오류 등)이 새어 나오면
            // 기록 없이 빠져나가 행이 PENDING 으로 영구 고착된다. 결과 객체로 변환해 반드시 기록한다.
            log.error("SMS 인증번호 발송 중 예상 밖 예외 - refKey={}, mblLast4={}", refKey, phoneLast4, e);
            result = SmsSendResult.failed(SmsFailureKind.TRANSPORT, "DISPATCH_ERROR", e.getClass().getSimpleName());
        } finally {
            smsSendResultRecorder.markResult(
                refKey
                , result != null
                    ? result
                    : SmsSendResult.failed(SmsFailureKind.TRANSPORT, "DISPATCH_ERROR", "발송 결과 미확정"));
        }

        if (result.status() == SmsSendStatus.FAILED) {
            log.warn("SMS 인증번호 발송 실패 - refKey={}, mblLast4={}, errCd={}", refKey, phoneLast4, result.errCd());
            throw new ApiException(toErrorCode(result.failureKind()));
        }

        log.info("SMS 인증번호 발송 결과 - refKey={}, 상태={}, mblLast4={}, msgKey={}",
            refKey, result.status(), phoneLast4, result.messageKey());
        return result.status();
    }

    /**
     * SMS2-D6: 실발송 게이트 상태 위임 조회.
     *
     * <p>호출부(진입점)가 {@code SmsSender} 를 직접 주입하지 않게 하려는 위임 메서드다
     * (요청서 1차 §7-1 — 호출부는 인터페이스 1개에만 의존).
     * 게이트 OFF 일 때만 필요한 전처리(예: 수신번호 복호화)를 건너뛰는 판정에 쓴다.
     *
     * <p>★킬스위치(SMS2-B3)를 이 판정에 포함하지 않는다. 킬스위치는 "발송 시도를 막고 사용자에게
     *    실패를 알리는" 런타임 비상정지이고, 게이트는 "애초에 발송 기능이 꺼져 있다"는 배포 설정이다.
     *    복호화 스킵은 후자에만 해당한다.
     */
    public boolean isSendEnabled() {
        return smsSender.isEnabled();
    }

    /**
     * 실패 분류 → 사용자 노출 에러코드.
     * ★벤더 원문 코드/사유는 사용자 메시지에 담지 않는다(내부 구성 노출 방지). DB·서버 로그에만 남긴다.
     */
    private SmsErrorCode toErrorCode(SmsFailureKind kind) {
        if (kind == null) {
            return SmsErrorCode.SMS_502_001;
        }
        return switch (kind) {
            case TOKEN -> SmsErrorCode.SMS_502_003;
            case VENDOR_REJECTED -> SmsErrorCode.SMS_502_002;
            case TRANSPORT -> SmsErrorCode.SMS_502_001;
        };
    }
}
