package com.prafta.web.attd.attd06.service;

import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamParam;
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamUserParam;
import com.prafta.web.attd.attd06.application.param.InsertShiftTeamUsersParam;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftTeamUserInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftTypeDetailListsParam;
import com.prafta.web.attd.attd06.application.param.ShiftTypeListsParam;
import com.prafta.web.attd.attd06.application.param.ShiftUserSchInfosParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamLeadersParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamNmParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamPeriodParam;
import com.prafta.web.attd.attd06.application.param.UserListsParam;
import com.prafta.web.attd.attd06.dto.response.ShiftTeamUserInfosResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeDetailListsResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeListsResponse;
import com.prafta.web.attd.attd06.dto.response.UserListsResponse;

public interface Attd06Service {

    ShiftTypeListsResponse getShiftTypeLists(ShiftTypeListsParam param);

    UserListsResponse getUserLists(UserListsParam param);

    ShiftTypeDetailListsResponse getShiftTypeDetailLists(ShiftTypeDetailListsParam param);

    ShiftTeamUserInfosResponse getShiftTeamUserInfos(ShiftTeamUserInfosParam param);

    void insertShiftSchInfos(ShiftSchInfosParam param);
    
    void updateShiftUserSchInfos(ShiftUserSchInfosParam param);

    void updateShiftTeamNm(UpdateShiftTeamNmParam param);

    void deleteShiftTeamUser(DeleteShiftTeamUserParam param);

    void insertShiftTeamUsers(InsertShiftTeamUsersParam param);

    void updateShiftTeamLeaders(UpdateShiftTeamLeadersParam param);

    void updateShiftTeamPeriod(UpdateShiftTeamPeriodParam param);

    void deleteShiftTeam(DeleteShiftTeamParam param);

//    void updateShiftSchInfos(UpdateShiftSchInfosParam param);

}
