package com.prafta.web.attd.attd06.application.param;

import java.util.List;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.model.InsertShiftTeamUsersModel;
import com.prafta.web.attd.attd06.dto.request.InsertShiftTeamUsersRequest;

public record InsertShiftTeamUsersParam(
    List<InsertShiftTeamUsersModel> insertShiftTeamUsersModelList
) {
    public static InsertShiftTeamUsersParam from(List<InsertShiftTeamUsersRequest> requests, TokenInfo tokenInfo) {

        if (requests == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - InsertShiftTeamUsersRequest");

        List<InsertShiftTeamUsersModel> models = requests.stream()
            .map(req -> new InsertShiftTeamUsersModel(
                req.getSiteCd()
                , req.getShiftCd()
                , req.getShiftTeamId()
                , req.getTeamIdx()
                , req.getUserCd()
                , tokenInfo.gv_cmpnyCd()
                , tokenInfo.gv_userCd()
            ))
            .toList();

        return new InsertShiftTeamUsersParam(models);
    }
}
