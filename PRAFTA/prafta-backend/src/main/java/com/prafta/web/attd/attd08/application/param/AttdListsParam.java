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

public record AttdListsParam(
      String fromDate
    , String toDate
    , String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String gvCmpnyCd
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
        );
    }
}
