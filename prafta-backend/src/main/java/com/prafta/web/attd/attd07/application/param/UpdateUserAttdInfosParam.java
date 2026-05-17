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

        List<UpdateUserAttdInfosModel> models = requests.stream()
            .map(req -> new UpdateUserAttdInfosModel(
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
            ))
            .toList();

        return new UpdateUserAttdInfosParam(models);
    }
}
