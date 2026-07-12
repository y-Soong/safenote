package com.prafta.web.attd.attd07.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;
import com.prafta.web.attd.attd07.dto.request.UpdateUserAttdInfosRequest;

public record UpdateUserAttdInfosParam(
    List<UpdateUserAttdInfosModel> updateUserAttdInfosModelList
) {
    public static UpdateUserAttdInfosParam from(List<UpdateUserAttdInfosRequest> requests, TokenInfo tokenInfo) {

        if (requests == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // [보안 하드닝] cross-site IDOR 차단: 요청이 사업장(siteCd)을 지정했으면 호출자 세션 사업장(gv_siteCd)과
        //   반드시 일치해야 한다. 불일치는 변조로 간주하고 거부한다(ATTD_400_005).
        //   - body 가 siteCd 를 보냈는데 gv_siteCd 가 없으면(스코프 검증 불가) fail-closed 로 거부한다
        //     (OT 직접등록 경로와 동일한 보안 강도 — defense-in-depth). 후단 노드 게이트가 최종 인가를 수행하지만,
        //     사업장 스코프 검증을 여기서 선제 차단해 비대칭/잠재 회귀를 없앤다.
        //   - body siteCd 자체가 비어 있으면 후단 resolveUserNodeCd(SITE_CD=blank)→null→403_002 로 fail-closed.
        final String gvSiteCd = tokenInfo.gv_siteCd();
        final boolean gvSitePresent = gvSiteCd != null && !gvSiteCd.isBlank();

        List<UpdateUserAttdInfosModel> models = requests.stream()
            .map(req -> {
                String reqSiteCd = req.getSiteCd();
                boolean bodySitePresent = reqSiteCd != null && !reqSiteCd.isBlank();
                if (bodySitePresent && (!gvSitePresent || !reqSiteCd.equals(gvSiteCd))) {
                    throw new ApiException(AttdErrorCode.ATTD_400_005);
                }
                return new UpdateUserAttdInfosModel(
                	req.getAttdId()
                    , req.getSiteCd()
                    , req.getNodeCd()
                    , req.getUserCd()
                    , req.getUserId()
                    , req.getWorkSeq()
                    , req.getWorkYmd()

                    , req.getOriCheckInDate()
                    , req.getOriCheckInTime()
                    , req.getOriCheckOutDate()
                    , req.getOriCheckOutTime()

                    , req.getCheckInDate()
                    , req.getCheckInTime()
                    , req.getCheckInMethod()
                    , req.getCheckOutDate()
                    , req.getCheckOutTime()
                    , req.getCheckOutMethod()
                    , req.getReason()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                    , tokenInfo.gv_authCd()
                    , tokenInfo.gv_siteCd()
                );
            })
            .toList();

        return new UpdateUserAttdInfosParam(models);
    }
}
