package com.prafta.web.tbm.tbmai01.application.query;

import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbmai01.application.param.TbmAiWorklistParam;

/**
 * TBM AI 분석 워크리스트 조회 쿼리(매퍼 전달용).
 *
 * <p>사업장 격리 게이트를 SQL WHERE 로 assertSiteAccess 와 동치 처리하기 위해 스코프 플래그를
 *    조립 시 1회 산출한다(Tbm01 선례).
 * <ul>
 *   <li>{@code canManageCommon} — 회사공통 행(SITE_CD 비어있음) 노출 허용(master/safe).</li>
 *   <li>{@code companyWide} — 회사 전체 사업장 통과(master/safe). 아니면 TB_USER_SITE_AUTH 매핑 사업장만.</li>
 * </ul>
 *    offset/limit 는 page/size 로부터 산출한다.
 */
public record TbmAiWorklistQuery(
    String keyword
    , String fileNm
    , String mtrlItemType
    , String aiStatus
    , String siteCd
    , boolean canManageCommon
    , boolean companyWide
    , int offset
    , int limit
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static TbmAiWorklistQuery from(TbmAiWorklistParam param) {
        return new TbmAiWorklistQuery(
            param.keyword()
            , param.fileNm()
            , param.mtrlItemType()
            , param.aiStatus()
            , param.siteCd()
            , AuthRoleUtils.canManageCommon(param.gvAuthCd())
            , AuthRoleUtils.isCompanyWide(param.gvAuthCd())
            , (param.page() - 1) * param.size()
            , param.size()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
