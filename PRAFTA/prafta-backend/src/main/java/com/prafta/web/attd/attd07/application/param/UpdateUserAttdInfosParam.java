package com.prafta.web.attd.attd07.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
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

        // [보안 하드닝] cross-site IDOR 차단은 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가 — 레코드별 siteCd 를 서비스에서 전수 검증).
        //   - body siteCd 자체가 비어 있으면 후단 resolveUserNodeCd(SITE_CD=blank)→null→403_002 로 fail-closed.
        List<UpdateUserAttdInfosModel> models = requests.stream()
            .map(req -> {
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
