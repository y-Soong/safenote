package com.prafta.web.user.user01.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 엑셀 업로드 — 서식 유실로 앞자리 0 이 떨어진 값을 되살린다.
 *
 * <p><b>왜 필요한가</b><br>
 * 셀 서식이 텍스트가 아니면 엑셀이 값을 숫자로 저장하면서 <b>맨 앞의 0 하나를</b> 떨어뜨린다.
 * {@code 01077635257} → {@code 1077635257}, {@code 050101} → {@code 50101}.
 * 자릿수 검증만 있으면 이런 값이 <b>그대로 통과해 잘못 저장된다</b>(휴대폰 10자리 하한을 통과함).
 *
 * <p><b>왜 "텍스트 서식이 아니면 거부"로 하지 않는가</b><br>
 * 사용자가 실수해서가 아니라 도구가 바꾸기 때문이다 — 다른 편집기로 열었다 저장하거나 복사·붙여넣기만
 * 해도 서식은 날아간다. 값은 멀쩡한데 거부되면 원인을 알 수 없다. 그래서 <b>복원 후 검증</b>한다.
 *
 * <p><b>복원 규칙</b><br>
 * "10자리면 0을 붙인다" 같은 이동전화 전제 규칙은 쓰지 않는다. 유선번호(02·031·033…)에서 깨진다.
 * 대신 <b>후보 중 유효한 것을 고른다</b>:
 * <ol>
 *   <li>구분자(하이픈·공백·괄호·점)만 제거한다</li>
 *   <li>숫자가 아닌 문자가 남으면 <b>복원하지 않는다</b> — 원본을 그대로 돌려주어 검증에서 걸리게 한다</li>
 *   <li>후보 두 개를 만든다 — {@code 그대로} / {@code "0" + 그대로}</li>
 *   <li>유효 패턴에 맞는 후보가 <b>정확히 하나면</b> 그것을 채택한다</li>
 *   <li>둘 다 맞거나 둘 다 안 맞으면 손대지 않는다 — 추측으로 값을 바꾸지 않는다</li>
 * </ol>
 * 이 방식은 이동전화·서울(02)·지역번호(031 등)·인터넷전화(070)를 한 규칙으로 처리하고,
 * 0 으로 시작하지 않는 대표번호(15xx·16xx·18xx)에 0 을 붙이는 사고도 자연히 막는다.
 */
public final class UserExcelValueRestorer {

    private UserExcelValueRestorer() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 전화번호 구분자로 허용하는 문자(제거 대상). 이 외의 비숫자 문자는 형식 오류로 본다. */
    private static final Pattern PHONE_SEPARATORS = Pattern.compile("[\\s\\-().]");

    /** 숫자만으로 이루어졌는지. */
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

    /** 이동전화: 01[016789] + 7~8자리 → 총 10~11자리. */
    private static final Pattern KR_MOBILE = Pattern.compile("^01[016789]\\d{7,8}$");

    /** 서울(02) + 7~8자리 → 총 9~10자리. */
    private static final Pattern KR_SEOUL = Pattern.compile("^02\\d{7,8}$");

    /** 지역번호 3자리 + 7~8자리 → 총 10~11자리. */
    private static final Pattern KR_AREA3 = Pattern.compile(
            "^0(31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64)\\d{7,8}$");

    /** 인터넷전화 070 + 8자리. */
    private static final Pattern KR_VOIP = Pattern.compile("^070\\d{8}$");

    /** 생년월일 자릿수 — 양식은 YYMMDD(6) 이지만 다른 경로가 YYYYMMDD(8) 를 보낼 수 있어 둘 다 받는다. */
    private static final int BIRTH_LEN_SHORT = 6;
    private static final int BIRTH_LEN_LONG = 8;

    /**
     * 보정 내역 1건. 사용자에게 "무엇이 어떻게 바뀌었는지" 보여주기 위한 것으로,
     * 조용히 고치지 않는다는 원칙의 근거 자료다.
     *
     * @param rowNo    엑셀 실제 행번호(1-based, 사용자가 화면에서 보는 번호)
     * @param columnNm 컬럼명(양식 헤더)
     * @param before   원본 입력값
     * @param after    보정된 값
     */
    public record Adjustment(int rowNo, String columnNm, String before, String after) {
    }

    /**
     * 전화번호 복원. 복원 불가/불필요하면 <b>구분자만 제거한 값</b>을 돌려준다(검증은 호출측 몫).
     *
     * @param raw     셀 원본 문자열
     * @param rowNo   엑셀 행번호(1-based) — 보정 기록용
     * @param sink    보정 내역 수집처(null 이면 기록하지 않음)
     * @return 복원(또는 정규화)된 문자열. 입력이 null/공백이면 null
     */
    public static String restorePhone(String raw, int rowNo, List<Adjustment> sink) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String stripped = PHONE_SEPARATORS.matcher(trimmed).replaceAll("");
        // 한글·영문 등이 섞였으면 복원하지 않는다. 원본을 그대로 넘겨 검증에서 형식 오류로 떨어뜨린다.
        //   (여기서 숫자만 남기면 "김철수010..." 같은 값이 멀쩡한 번호로 둔갑한다)
        if (!DIGITS_ONLY.matcher(stripped).matches()) {
            return trimmed;
        }

        boolean asIsValid = isKrPhone(stripped);
        String padded = "0" + stripped;
        boolean paddedValid = isKrPhone(padded);

        // 정확히 padded 만 유효할 때에만 0 을 되살린다.
        if (!asIsValid && paddedValid) {
            record(sink, rowNo, "휴대폰번호", trimmed, padded);
            return padded;
        }
        return stripped;
    }

    /**
     * 생년월일 복원. 6자리 미만이면 앞을 0 으로 채워 YYMMDD 로 맞춘다.
     * 복원 불가면 원본을 그대로 돌려준다(검증은 호출측 몫).
     *
     * @param raw   셀 원본 문자열
     * @param rowNo 엑셀 행번호(1-based) — 보정 기록용
     * @param sink  보정 내역 수집처(null 이면 기록하지 않음)
     * @return 복원된 문자열. 입력이 null/공백이면 null
     */
    public static String restoreBirth(String raw, int rowNo, List<Adjustment> sink) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // 숫자가 아닌 문자가 있으면 손대지 않는다(검증에서 형식 오류).
        if (!DIGITS_ONLY.matcher(trimmed).matches()) {
            return trimmed;
        }
        // 6자리 미만만 복원 대상. 7자리는 YYMMDD 도 YYYYMMDD 도 아니므로 추측하지 않는다.
        if (trimmed.length() >= BIRTH_LEN_SHORT) {
            return trimmed;
        }
        String padded = "0".repeat(BIRTH_LEN_SHORT - trimmed.length()) + trimmed;
        record(sink, rowNo, "생년월일", trimmed, padded);
        return padded;
    }

    /** 한국 전화번호 형식(이동전화·서울·지역·인터넷전화) 여부. */
    public static boolean isKrPhone(String digits) {
        if (digits == null) {
            return false;
        }
        return KR_MOBILE.matcher(digits).matches()
                || KR_SEOUL.matcher(digits).matches()
                || KR_AREA3.matcher(digits).matches()
                || KR_VOIP.matcher(digits).matches();
    }

    /** 이동전화 여부. 휴대폰번호 컬럼은 로그인 계정·초기 비밀번호·SMS 인증에 쓰이므로 이동전화만 허용한다. */
    public static boolean isKrMobile(String digits) {
        return digits != null && KR_MOBILE.matcher(digits).matches();
    }

    /**
     * 생년월일 유효성. 6자리(YYMMDD) 또는 8자리(YYYYMMDD)이며 실제 달력상 존재하는 날짜여야 한다.
     * 6자리는 세기를 알 수 없으므로 19YY·20YY 중 하나라도 유효하면 통과시킨다(2/29 대응).
     */
    public static boolean isValidBirth(String digits) {
        if (digits == null || !DIGITS_ONLY.matcher(digits).matches()) {
            return false;
        }
        if (digits.length() == BIRTH_LEN_LONG) {
            return isRealDate(
                    Integer.parseInt(digits.substring(0, 4)),
                    Integer.parseInt(digits.substring(4, 6)),
                    Integer.parseInt(digits.substring(6, 8)));
        }
        if (digits.length() == BIRTH_LEN_SHORT) {
            int yy = Integer.parseInt(digits.substring(0, 2));
            int mm = Integer.parseInt(digits.substring(2, 4));
            int dd = Integer.parseInt(digits.substring(4, 6));
            return isRealDate(1900 + yy, mm, dd) || isRealDate(2000 + yy, mm, dd);
        }
        return false;
    }

    /** 실제 달력상 존재하는 날짜인지. */
    private static boolean isRealDate(int year, int month, int day) {
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    /** 보정 내역 적재(수집처가 없으면 무시). */
    private static void record(List<Adjustment> sink, int rowNo, String columnNm, String before, String after) {
        if (sink != null) {
            sink.add(new Adjustment(rowNo, columnNm, before, after));
        }
    }
}
