package com.prafta.web.attd.attd16.application.param;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd16.dto.request.LeaveUsageCalendarRequest;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 조회 파라미터.
 *
 * <p>Attd15 {@code Weekly52hListsParam} 패턴을 그대로 따른다. 회사/요청자/권한/토큰 사업장은
 * 오직 JWT 클레임에서만 도출하고(요청 파라미터로 cmpnyCd 를 받지 않는다), 조회 사업장(siteCd)의
 * 접근 인가는 서비스 계층 {@code SiteAccessService.assertSiteAccess} 가 강제한다.
 *
 * <p>월 범위(monthStart/monthEnd)는 클라이언트 값을 신뢰하지 않고 서버가 searchYm(YYYYMM)에서
 * 재계산한다. monthEnd 는 해당 월의 말일(윤년/월별 일수 자동 반영).
 */
public record LeaveUsageCalendarParam(
        String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String searchYm
        , String monthStart
        , String monthEnd
        , String gvCmpnyCd
        , String gvAuthCd
        , String gvUserCd
        , String gvSiteCd
) {
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static LeaveUsageCalendarParam from(LeaveUsageCalendarRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (request.getSiteCd() == null || request.getSiteCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (request.getSearchYm() == null || !request.getSearchYm().matches("\\d{6}"))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        YearMonth ym;
        try {
            ym = YearMonth.parse(request.getSearchYm(), YM);
        } catch (DateTimeParseException e) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 월 범위는 서버 재계산(클라이언트 값 불신). monthEnd = 해당 월 말일.
        String monthStart = ym.atDay(1).format(YMD);
        String monthEnd = ym.atEndOfMonth().format(YMD);

        return new LeaveUsageCalendarParam(
                request.getSiteCd()
                , request.getNodeCd()
                , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
                , request.getSearchYm()
                , monthStart
                , monthEnd
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_authCd()
                , tokenInfo.gv_userCd()
                , tokenInfo.gv_siteCd()
        );
    }
}
