package com.prafta.web.attd.attd08.application.param;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd08.dto.request.AttdListsRequest;

/**
 * Attd_08 근태 현황 조회 파라미터.
 *
 * <p>★ security H-1(2026-08-07): 종전에는 토큰에서 {@code gv_cmpnyCd} 만 받아, 같은 회사 사용자가
 * {@code siteCd}/{@code nodeCd} 를 임의 지정해 타 사업장·타 부서의 근태(PII)를 열람할 수 있었다.
 * 인가 판정에 필요한 토큰 클레임(userCd/authCd/siteCd)을 함께 실어 서비스 진입부에서
 * {@code SiteAccessService.assertSiteAccess} + {@code AttdCloseService.canManageNode} 로 강제한다
 * (Attd_11/Attd_15 와 동일 패턴).
 */
public record AttdListsParam(
      String fromDate
    , String toDate
    , String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
    , String gvSiteCd
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Logger log = LoggerFactory.getLogger(AttdListsParam.class);

    public static AttdListsParam from(AttdListsRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("AttdListsParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("AttdListsParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (request.getFromDate() == null || request.getFromDate().isBlank()) {
            log.warn("AttdListsParam.from - required field missing: fromDate");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getToDate() == null || request.getToDate().isBlank()) {
            log.warn("AttdListsParam.from - required field missing: toDate");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isBlank()) {
            log.warn("AttdListsParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // security H-1: 인가 판정 입력(토큰 클레임) 결손이면 fail-closed.
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("AttdListsParam.from - token claim missing: gv_cmpnyCd / gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(request.getFromDate(), DATE_FMT);
            to   = LocalDate.parse(request.getToDate(),   DATE_FMT);
        } catch (DateTimeParseException e) {
            log.warn("AttdListsParam.from - invalid date format (expected YYYY-MM-DD). fromDate={}, toDate={}",
                    request.getFromDate(), request.getToDate());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (from.isAfter(to)) {
            log.warn("AttdListsParam.from - fromDate is after toDate. fromDate={}, toDate={}",
                    request.getFromDate(), request.getToDate());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (ChronoUnit.MONTHS.between(from, to) > 3
                || (ChronoUnit.MONTHS.between(from, to) == 3 && to.getDayOfMonth() > from.getDayOfMonth()))
            throw new ApiException(AttdErrorCode.ATTD_400_004);

        return new AttdListsParam(
              request.getFromDate()
            , request.getToDate()
            , request.getSiteCd()
            , request.getNodeCd()
            , request.getIncSubNodeYn() == null ? "N" : request.getIncSubNodeYn()
            , request.getUserNm()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
