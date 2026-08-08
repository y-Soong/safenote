package com.prafta.common.cmm.sms;

/**
 * 인증번호 문자 본문 템플릿(고정).
 *
 * <p>★사용자 입력은 본문에 절대 들어가지 않는다. 서버가 생성한 6자리 인증번호와 유효분(정수)만 치환된다
 *    → 인젝션 표면 자체가 없다(요청서 §10).
 *
 * <p>★뿌리오 치환자 문법은 {@code [*...*]} 형태다. 본 문구에는 {@code *} 가 없으므로
 *    {@code [PRAFTA]} / {@code [123456]} 은 치환자로 해석되지 않는다.
 *
 * <p>완성 예: {@code [PRAFTA] 인증번호 [123456] 입력해 주세요. (1분 내 유효)}
 * <table border="1">
 *   <caption>본문 길이(LMS 자동전환 회피 근거 — SMS 한도 90 byte)</caption>
 *   <tr><th>기준</th><th>byte</th><th>비고</th></tr>
 *   <tr><td>EUC-KR/CP949(이통사 SMS 산정 기준)</td><td>55</td><td>한글 14자×2 + ASCII 27 = 55. 한도 90 대비 여유 35</td></tr>
 *   <tr><td>UTF-8(참고)</td><td>69</td><td>한글 14자×3 + ASCII 27 = 69. 어느 기준이어도 90 이내</td></tr>
 * </table>
 * 유효분({@code validMinutes})은 현재 1 또는 3(한 자리)이라 위 계산이 그대로 성립한다.
 * 두 자리가 되면 각 기준 +1 byte.
 *
 * <p>★브랜드 표기 변경 시 {@link #BRAND} 상수 한 줄만 고치면 된다(문구가 코드 곳곳에 흩어지지 않게 하기 위함).
 *    변경 시 위 byte 수 주석도 함께 갱신할 것.
 */
public final class AuthCodeMessageTemplate {

    /** 발신 브랜드 표기. ★변경은 이 한 줄만. */
    private static final String BRAND = "[PRAFTA]";

    /** 본문 포맷: {브랜드} 인증번호 [{코드}] 입력해 주세요. ({유효분}분 내 유효) */
    private static final String FORMAT = "%s 인증번호 [%s] 입력해 주세요. (%d분 내 유효)";

    private AuthCodeMessageTemplate() {
    }

    /**
     * 인증번호 문자 본문 생성.
     *
     * <p>★{@code validMinutes} 를 1 로 하드코딩하지 말 것. 앱 마이페이지 휴대폰 변경 흐름은 3분 유효이므로
     *    1 을 넣으면 문자가 거짓 안내를 하게 된다(plan §3 T5).
     *
     * @param authCode     서버 생성 6자리 인증번호
     * @param validMinutes 코드 유효 시간(분). 진입점 A·C = 1, 진입점 B(앱 마이페이지) = 3
     * @return 발송 본문. ★인증번호를 포함하므로 어떤 로그에도 출력 금지
     */
    public static String build(String authCode, int validMinutes) {
        return String.format(FORMAT, BRAND, authCode, validMinutes);
    }
}
