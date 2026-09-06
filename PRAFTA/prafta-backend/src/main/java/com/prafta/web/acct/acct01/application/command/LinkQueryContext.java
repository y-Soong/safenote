package com.prafta.web.acct.acct01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.acct.acct01.application.param.LinkQueryParam;
import com.prafta.web.acct.acct01.result.AcctResult;
import com.prafta.web.acct.acct01.result.AcctVictimResult;

/**
 * 연계 조회 컨텍스트 — 사고 헤더(발생일/시각/재해자/유형/사업장)에서 도출한 매칭키와
 * 사용자 선택 필터를 합친 mapper 입력. body siteCd 가 아니라 사고 헤더 siteCd 를 사용한다(IDOR).
 *
 * <p>occurYmd(YYYYMMDD) 로부터 기간 경계를 mapper SQL 에서 계산한다(STR_TO_DATE 기반).
 * <p>prafta-065: 재해자는 대표(헤더 컬럼, of) 또는 자식 테이블의 지정 인원(ofVictim/forPrint).
 * victimSeq/victimUserNm 은 응답 echo 용이며 SQL 바인딩에는 쓰지 않는다(대표 경로는 null).
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
    // 재해자 식별(prafta-065)
    , Integer victimSeq
    , String victimUserNm       // 마스킹된 이름
){
    // 대표 재해자(헤더 컬럼) 기준 — 기존 호출 무회귀
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
            , null
            , null
        );
    }

    // 지정 재해자(자식 테이블 순번 실재 검증 완료 인원) 기준 + 사용자 선택 필터
    public static LinkQueryContext ofVictim(LinkQueryParam param, AcctResult header, AcctVictimResult victim) {

        if (param == null || header == null || victim == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LinkQueryContext(
            header.cmpnyCd()
            , header.siteCd()
            , victim.userCd()
            , victim.userTypeCd()
            , header.occurYmd()
            , header.occurTime()
            , param.chklstType()
            , param.chkptCd()
            , param.processCd()
            , param.riskTypeCd()
            , param.hazardCd()
            , victim.victimSeq()
            , victim.userNm()
        );
    }

    // 출력(③) 재해자 순회용 — 선택 필터 없음
    public static LinkQueryContext forPrint(AcctResult header, AcctVictimResult victim) {

        if (header == null || victim == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LinkQueryContext(
            header.cmpnyCd()
            , header.siteCd()
            , victim.userCd()
            , victim.userTypeCd()
            , header.occurYmd()
            , header.occurTime()
            , null
            , null
            , null
            , null
            , null
            , victim.victimSeq()
            , victim.userNm()
        );
    }
}
