package com.prafta.web.attd.attd15.application.param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd15.dto.request.Weekly52hListsRequest;

/**
 * ATTD15-T1 - 주52시간 관리 조회 파라미터.
 *
 * <p>Attd11 {@code MonthlyAttdSummaryParam} 패턴을 따른다. siteCd 는 SiteAccessService 로
 * cross-site IDOR 가드(서비스 계층), 권한 게이팅은 AttdCloseService.canManageNode 로 강제한다.
 *
 * <p>weekStartYmd 는 반드시 월요일이어야 한다(사용자 결정 §2.1 — 월~일 고정, 사업장 무관 전사 동일
 * 기준). weekEndYmd 는 클라이언트 값을 신뢰하지 않고 서버가 weekStartYmd+6일로 재계산한다.
 */
public record Weekly52hListsParam(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userNm
        , String weekStartYmd
        , String weekEndYmd
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static Weekly52hListsParam from(Weekly52hListsRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getWeekStartYmd() == null || request.getWeekStartYmd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        LocalDate weekStart;
        try {
            weekStart = LocalDate.parse(request.getWeekStartYmd(), YMD);
        } catch (DateTimeParseException e) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 주 기준 월~일 고정(사용자 결정 §2.1) — weekStartYmd 는 반드시 월요일.
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // weekEndYmd 는 서버 재계산(클라이언트 값 불신).
        String weekEndYmd = weekStart.plusDays(6).format(YMD);

        return new Weekly52hListsParam(
                request.getSiteCd()
                , request.getNodeCd()
                , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
                , request.getUserNm()
                , request.getWeekStartYmd()
                , weekEndYmd
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }
}
