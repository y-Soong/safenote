package com.prafta.app.notice.notice02.application.query;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.util.StringUtils;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.AppArchiveConstants;
import com.prafta.app.notice.notice02.application.param.AppArchiveListParam;

/**
 * 앱 자료실 목록 조회 쿼리. 등록월(registMonth 'YYYY-MM'/'YYYYMM')을
 * 해당 월 1일(startDate) ~ 말일(endDate) 'YYYY-MM-DD' 로 서버에서 변환한다(웹 FE 변환 로직 동형).
 *
 * <p>baimValCdForJoin 은 자료타입명 LEFT JOIN 용 코드그룹 상수(COM008). 미주입 시 타입명 NULL.
 */
public record AppArchiveListQuery(
    String archiveTypeCd
    , String titleKeyword
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String baimValCdForJoin
){
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static AppArchiveListQuery from(AppArchiveListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        String startDate = "";
        String endDate = "";
        // 등록월 'YYYY-MM' 또는 'YYYYMM' → 월초/월말(YYYY-MM-DD). 형식 불량/미선택이면 전체 기간.
        Integer[] ym = parseYearMonth(param.registMonth());
        if (ym != null) {
            LocalDate first = LocalDate.of(ym[0], ym[1], 1);
            LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
            startDate = first.format(YMD);
            endDate = last.format(YMD);
        }

        return new AppArchiveListQuery(
            param.archiveTypeCd()
            , param.titleKeyword()
            , startDate
            , endDate
            , param.gvCmpnyCd()
            , AppArchiveConstants.ARCHIVE_BAIM_VAL_CD
        );
    }

    /** 'YYYY-MM' 또는 'YYYYMM' → [year, month]. 파싱 불가 시 null(전체 기간). */
    private static Integer[] parseYearMonth(String registMonth) {
        if (!StringUtils.hasText(registMonth)) return null;
        String digits = registMonth.replace("-", "").trim();
        if (digits.length() != 6) return null;
        try {
            int year = Integer.parseInt(digits.substring(0, 4));
            int month = Integer.parseInt(digits.substring(4, 6));
            if (month < 1 || month > 12) return null;
            return new Integer[] { year, month };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
