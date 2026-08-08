package com.prafta.common.error.sms;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * SMS 발송(뿌리오 연동) 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>★주의: 앱 인터셉터는 COMMON_400_003 / COMMON_400_600 을 토큰 오류로 간주해 강제 로그아웃시킨다.
 *    따라서 발송 실패에는 COMMON_400_003/600 을 절대 재사용하지 않고 본 SMS_* 코드를 사용한다.
 *
 * <p>★사용자 노출 메시지에는 벤더(뿌리오) 원문 코드/사유를 담지 않는다(내부 구성 노출 방지).
 *    원인은 서버 로그와 TB_SMS_AUTH_CODE 의 SEND_ERR_CD / SEND_ERR_MSG 컬럼에만 남긴다.
 */
public enum SmsErrorCode implements ApiErrorCode {

    // 400: 인증번호 발송 레이트리밋(동일 휴대폰 최근 1분 내 재발송 거부).
    //      프론트 60초 재발송 타이머와 동일 값이라 정상 사용자에게는 무회귀.
    //      PLATFORM_400_013(플랫폼 위치열람 게이트 1분 제한) 과 같은 성격/문구.
    SMS_400_001(HttpStatus.BAD_REQUEST, "인증번호가 방금 발송되었습니다. 잠시 후 다시 시도해 주세요.")
    // 400: 인증번호 대입 실패 상한 도달로 해당 번호의 검증이 일시 잠긴 상태(SMS2-A1 / sec C-2).
    //      ★[3차 / sec N-2 · qa Q-8] "영구 무효화 → 시간 잠금" 으로 바뀌었다. 문구도 함께 고쳤다.
    //        2차의 "인증번호를 다시 요청해 주세요" 는 이제 거짓 안내다 — 재발송이 아니라 시간이 지나면 풀린다
    //        (잠금 시간 = TB_SMS_SEND_POLICY.VERIFY_LOCK_SEC, 기본 180초).
    //      ★플랫폼 흐름(PLATFORM_400_010)은 열거 방지를 위해 초과를 구분하지 않으나,
    //        진입점 A 는 요청서 2차 A-4 지시에 따라 의도적으로 구분한다. 노출되는 정보는
    //        "이 번호로 최근 시도가 여러 번 있었다" 뿐이고 그 시도를 한 것이 공격자 자신이라 oracle 가치가 없다.
    //        반대로 구분하지 않으면 정상 사용자가 올바른 코드를 넣어도 계속 "불일치"만 보게 되어 이탈한다.
    , SMS_400_002(HttpStatus.BAD_REQUEST, "인증 시도 횟수를 초과했습니다.\n잠시 후 다시 시도해 주세요.")
    // 400: 번호별 시간당/일별 발송 상한 초과(정책값 TB_SMS_SEND_POLICY). 문자를 받은 사용자가 반복 요청한 상태.
    , SMS_400_003(HttpStatus.BAD_REQUEST, "인증번호 발송 횟수가 많습니다.\n잠시 후 다시 시도해 주세요.")
    // 400: 요청 IP 또는 로그인 사용자별 상한 초과.
    //      ★사용자 메시지를 SMS_400_003 과 동일하게 둔다 — 어느 축에 걸렸는지 알려주면
    //        공격자가 어느 축을 우회할지 알게 된다. 축 구분은 서버 로그와 에러코드로만.
    , SMS_400_004(HttpStatus.BAD_REQUEST, "인증번호 발송 횟수가 많습니다.\n잠시 후 다시 시도해 주세요.")
    // 400: [3차 / sec N-3] 번호별 시간당 "검증 시도" 상한 초과(TB_SMS_SEND_POLICY.VERIFY_HOUR_LIMIT).
    //      ★발송 상한(SMS_400_003/004)과 다른 축이다. 검증 EP 는 무인증인데 2차까지 호출 자체에
    //        상한이 없어 요청당 쓰기 3쿼리를 무제한 유발할 수 있었다(DB 쓰기 DoS).
    , SMS_400_005(HttpStatus.BAD_REQUEST, "인증 시도가 너무 많습니다.\n잠시 후 다시 시도해 주세요.")
    // 502: 발송 게이트웨이 호출 실패(연결 실패/타임아웃/HTTP 오류/응답 이상).
    , SMS_502_001(HttpStatus.BAD_GATEWAY, "인증번호 발송에 실패했습니다.\n잠시 후 다시 시도해 주세요.")
    // 502: 게이트웨이가 실패코드를 반환(발신번호 미등록·잔액 부족·수신거부 등 벤더 판정 실패).
    , SMS_502_002(HttpStatus.BAD_GATEWAY, "인증번호 발송이 거절되었습니다.\n잠시 후 다시 시도해 주세요.")
    // 502: 게이트웨이 토큰 발급 실패(계정/인증키 오류 등).
    , SMS_502_003(HttpStatus.BAD_GATEWAY, "인증번호 발송 서버 인증에 실패했습니다.\n관리자에게 문의해 주세요.")
    // 503: 전역 시간당 상한 초과로 킬스위치가 발동해 발송이 전면 중지된 상태.
    //      해제는 Platform_05(SMS 발송 관리) 화면에서 운영자 수동으로만 가능하다(자동 복구 금지).
    //      ★503 대역 신규라 앱 인터셉터의 강제 로그아웃 코드와 겹치지 않는다.
    , SMS_503_001(HttpStatus.SERVICE_UNAVAILABLE, "인증번호 발송이 일시 중지되었습니다.\n관리자에게 문의해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    SmsErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
