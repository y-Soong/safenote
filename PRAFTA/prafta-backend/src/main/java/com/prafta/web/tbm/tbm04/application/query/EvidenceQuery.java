package com.prafta.web.tbm.tbm04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm04.application.param.EvidenceListParam;

/**
 * TBM 증빙자료 조회 쿼리 — 반기 기간(KST 일자) 파생 + 스코프(W-12 historyWhere 미러).
 *
 * <p>기간 판정 축은 세션 종료일(ENDED_AT, KST 벽시계 일자)이다.
 */
public record EvidenceQuery(
    String fromDate            // YYYY-MM-DD (반기 시작일)
    , String toDate            // YYYY-MM-DD (반기 종료일)
    , String siteCd            // 자사 사업장 필터(선택)
    , boolean companyWide      // master/safe: 회사 전체, 그 외: 자기 사업장만(W-12 동일)
    , String scopeSiteCd
    , String gvCmpnyCd
){
    public static EvidenceQuery from(EvidenceListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        String fromDate = "H1".equals(param.half()) ? param.year() + "-01-01" : param.year() + "-07-01";
        String toDate = "H1".equals(param.half()) ? param.year() + "-06-30" : param.year() + "-12-31";

        return new EvidenceQuery(
            fromDate
            , toDate
            , param.siteCd()
            , AuthRoleUtils.isCompanyWide(param.gvAuthCd())
            , param.gvSiteCd()
            , param.gvCmpnyCd()
        );
    }
}
