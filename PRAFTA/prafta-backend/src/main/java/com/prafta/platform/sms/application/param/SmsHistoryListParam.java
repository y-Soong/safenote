package com.prafta.platform.sms.application.param;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.regex.Pattern;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.platform.sms.dto.request.SmsHistoryListRequest;

/**
 * Platform_05: SMS 발송 이력 목록 조회 파라미터.
 *
 * <p>운영자 식별({@code gvUserCd})은 <b>토큰에서만</b> 취한다(로그용). 요청 바디/쿼리의 사용자 값은 불신한다.
 *
 * <p>★목적/상태는 화이트리스트로만 받는다 — 임의 문자열이 조회 술어로 흘러가지 않게 계약을 좁힌다.
 * <p>★기간은 미입력이면 서버가 <b>최근 7일</b>로 채운다. 다만 <b>최대 기간 상한은 두지 않는다</b>
 *    (사용자 확정 — 전 구간 누적이 수백 건 규모라 전 구간 조회가 부담이 아니고, 상한을 두면
 *    과거 기록을 볼 방법이 사라진다). 노출 범위는 {@link #MAX_PAGE_SIZE} 와 페이징이 제한한다.
 *
 * <p>★★{@code AUTH_CD}(6자리 인증번호 평문)는 이 계약 어디에도 등장하지 않는다. 조회 조건으로도,
 *    응답으로도 다루지 않는다 — 만료 전 인증번호는 그 자체로 계정 탈취에 쓰이는 유효 자격증명이다.
 */
public record SmsHistoryListParam(
        String startDate
        , String endDate
        , String purposeCd
        , String sendStatus
        /**
         * 검색용 휴대폰 <b>정규화 숫자열</b>(평문 PII). 미입력이면 null.
         * ★서비스 계층이 HMAC 으로 변환한 뒤 버린다 — 매퍼/응답/로그로 내려보내지 않는다.
         */
        , String mblNo
        , int page
        , int pageSize
        , String gvUserCd
) {
    /** 인증 목적 화이트리스트 — 셀프가입·본인인증·비밀번호재설정 / 플랫폼 위치열람 / 앱 휴대폰 변경. */
    private static final Set<String> ALLOWED_PURPOSE_CD =
            Set.of("SELF_JOIN", "PLATFORM_LOCATION", "MOBILE_CHANGE");

    /** 발송 상태 화이트리스트 — TB_SMS_AUTH_CODE.SEND_STATUS 컬럼 주석과 동일 집합. */
    private static final Set<String> ALLOWED_SEND_STATUS =
            Set.of("PENDING", "SENT", "FAILED", "SKIPPED");

    /** 기본 페이지 크기. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 페이지 크기 상한.
     *
     * <p>★상한이 없으면 {@code pageSize=100000} 한 방으로 전 구간 마스킹 휴대폰을 덤프할 수 있다
     * (PII 최소 노출 원칙 위배 — 공통 정책서 §11.1).
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 페이지 번호 상한.
     *
     * <p>★상한이 없으면 {@code page * pageSize} 의 int 곱셈이 오버플로해 <b>음수 OFFSET</b> 이 되고
     * (예: {@code page=2147483647&pageSize=100} → offset -200) MySQL 1064 로 500 이 난다.
     */
    public static final int MAX_PAGE = 100_000;

    /** 기본 조회 기간(일) — 오늘 포함 최근 7일. */
    private static final int DEFAULT_PERIOD_DAYS = 7;

    /** 발송기간 입력 형식(yyyy-MM-dd). 화면은 CalendarSrch 로만 입력하므로 형식 위반은 미입력 취급한다. */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** 서버 기준 오늘(Asia/Seoul). JVM 기본 TZ 에 의존하면 UTC 환경에서 하루가 밀린다. */
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    /** 휴대폰 검색 입력 허용 자릿수(국제번호 발송·HMAC 버킷 회피 차단 — BaseinfoServiceImpl 과 동일 규칙). */
    private static final int MBL_NO_MIN_LEN = 9;
    private static final int MBL_NO_MAX_LEN = 11;

    public static SmsHistoryListParam from(SmsHistoryListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String purposeCd = blankToNull(request.getPurposeCd());
        if (purposeCd != null && !ALLOWED_PURPOSE_CD.contains(purposeCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        String sendStatus = blankToNull(request.getSendStatus());
        if (sendStatus != null && !ALLOWED_SEND_STATUS.contains(sendStatus)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 기간 — 미입력/형식위반은 서버 기본값(최근 7일)으로 채운다. 무기간 전체 조회 경로를 만들지 않는다.
        LocalDate today = LocalDate.now(ZONE_KST);
        String startDate = normalizeDate(request.getStartDate());
        String endDate = normalizeDate(request.getEndDate());
        if (startDate == null) {
            startDate = today.minusDays(DEFAULT_PERIOD_DAYS - 1L).toString();
        }
        if (endDate == null) {
            endDate = today.toString();
        }
        // yyyy-MM-dd 는 사전순 비교가 날짜순 비교와 일치한다.
        if (startDate.compareTo(endDate) > 0) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
        if (page > MAX_PAGE) {
            page = MAX_PAGE;
        }
        int pageSize = (request.getPageSize() == null || request.getPageSize() < 1)
                ? DEFAULT_PAGE_SIZE : request.getPageSize();
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        return new SmsHistoryListParam(
                startDate
                , endDate
                , purposeCd
                , sendStatus
                , normalizeMblNo(request.getMblNo())
                , page
                , pageSize
                , tokenInfo.gv_userCd()
        );
    }

    /** yyyy-MM-dd 형식만 통과시키고 그 외(공백/형식 위반)는 미입력(null)으로 본다. */
    private static String normalizeDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return DATE_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    /**
     * 휴대폰 검색 입력 정규화 — 숫자만 남긴다(하이픈/공백 허용).
     *
     * <p>★적재 시점과 <b>같은 정규화</b>({@code Normalizers.normalizePhone})를 써야 HMAC 이 일치한다.
     *    다른 규칙으로 만들면 예외 없이 조용히 0건이 나온다.
     * <p>자릿수 범위를 벗어나면 400 으로 끊는다 — 어차피 매칭될 수 없는 입력이라 쿼리를 태울 이유가 없다.
     */
    private static String normalizeMblNo(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = Normalizers.normalizePhone(value);
        if (digits == null || digits.length() < MBL_NO_MIN_LEN || digits.length() > MBL_NO_MAX_LEN) {
            // ★입력값 자체를 예외 메시지/로그에 담지 않는다(평문 PII).
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return digits;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
