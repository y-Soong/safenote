package com.prafta.app.approval.admin.application.param;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 001-P2-B6: 앱 관리자 승인 이력(A-5) 조회 Param.
 *
 * <p>기간 미지정 시 기본 30일(plan §A7). 정렬은 PROCESS_DATE DESC 고정(요청서).
 */
public record ApprovalHistoryParam(
      String group
    , String startDate
    , String endDate
    , String keyword
    , int page
    , int pageSize
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    // Fix1: (page-1)*pageSize 의 int 오버플로/offset 폭주 방어용 page 상한. 실제 offset 상한(빈 페이지 전환)은
    //   서비스(MAX_OFFSET)에서 일관 처리하므로, 여기서는 오버플로만 막는 넉넉한 천장만 둔다.
    private static final int MAX_PAGE = 1_000_000;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static ApprovalHistoryParam of(String group, String startDate, String endDate,
            String keyword, Integer page, Integer pageSize, TokenInfo token) {
        if (token == null || token.gv_cmpnyCd() == null || token.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String g = (group == null || group.isBlank()) ? "ALL" : group.trim().toUpperCase();
        // 기간 기본값: endDate=오늘, startDate=오늘-30일. WORK_YMD(YYYYMMDD) 기준 필터.
        String end = normalizeYmd(endDate, LocalDate.now());
        String start = normalizeYmd(startDate, LocalDate.now().minusDays(DEFAULT_RANGE_DAYS));
        int p = (page == null || page < 1) ? 1 : Math.min(page, MAX_PAGE);
        int ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new ApprovalHistoryParam(g, start, end,
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                p, ps, token.gv_cmpnyCd(), token.gv_userCd(), token.gv_siteCd(), token.gv_authCd());
    }

    private static String normalizeYmd(String raw, LocalDate fallback) {
        if (raw != null && raw.matches("\\d{8}")) {
            return raw;
        }
        return fallback.format(YMD);
    }
}
