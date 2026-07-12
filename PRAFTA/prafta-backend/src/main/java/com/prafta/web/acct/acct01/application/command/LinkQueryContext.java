package com.prafta.web.acct.acct01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.application.param.LinkQueryParam;
import com.prafta.web.acct.acct01.result.AcctResult;

/**
 * 연계 조회 컨텍스트 — 사고 헤더(발생일/시각/재해자/유형/사업장)에서 도출한 매칭키와
 * 사용자 선택 필터를 합친 mapper 입력. body siteCd 가 아니라 사고 헤더 siteCd 를 사용한다(IDOR).
 *
 * <p>occurYmd(YYYYMMDD) 로부터 기간 경계를 mapper SQL 에서 계산한다(STR_TO_DATE 기반).
 */
public record LinkQueryContext(
    String gvCmpnyCd
    , String siteCd             // 사고 헤더 사업장(신뢰 원천)
    , String victimUserCd
    , String victimUserTypeCd   // REGULAR/DAILY
    , String occurYmd           // YYYYMMDD
    , String occurTime          // HHMM
    // 선택 필터
    , String chklstType
    , String chkptCd
    , String processCd
    , String riskTypeCd
    , String hazardCd
){
    public static LinkQueryContext of(LinkQueryParam param, AcctResult header) {

        if (param == null || header == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LinkQueryContext(
            header.cmpnyCd()
            , header.siteCd()
            , header.victimUserCd()
            , header.victimUserTypeCd()
            , header.occurYmd()
            , header.occurTime()
            , param.chklstType()
            , param.chkptCd()
            , param.processCd()
            , param.riskTypeCd()
            , param.hazardCd()
        );
    }
}
