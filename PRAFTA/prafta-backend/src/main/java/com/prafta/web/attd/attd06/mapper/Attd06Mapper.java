package com.prafta.web.attd.attd06.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd06.application.command.DeleteShiftTeamCommand;
import com.prafta.web.attd.attd06.application.command.DeleteShiftTeamUserCommand;
import com.prafta.web.attd.attd06.application.command.InsertShiftTeamUsersCommand;
import com.prafta.web.attd.attd06.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd06.application.command.ShiftTeamUserCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamLeadersCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamNmCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamPeriodCommand;
import com.prafta.web.attd.attd06.application.command.UserWorkPlanCommand;
import com.prafta.web.attd.attd06.application.query.SchCdListQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTeamUserInfosQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTypeDetailListsQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTypeListsQuery;
import com.prafta.web.attd.attd06.application.query.UserListsQuery;
import com.prafta.web.attd.attd06.result.ShiftTeamUserInfosResult;
import com.prafta.web.attd.attd06.result.ShiftTypeDetailListsResult;
import com.prafta.web.attd.attd06.result.ShiftTypeListsResult;
import com.prafta.web.attd.attd06.result.UserListsResult;

@Mapper
public interface Attd06Mapper {

    List<ShiftTeamUserInfosResult> selectShiftTeamUserInfos(ShiftTeamUserInfosQuery query);

    List<ShiftTypeListsResult> selectShiftTypeLists(ShiftTypeListsQuery query);

    List<UserListsResult> selectUserLists(UserListsQuery query);

    List<ShiftTypeDetailListsResult> selectShiftTypeDetailLists(ShiftTypeDetailListsQuery query);
    
    String selectShiftTeamCd(String gvCmpnyCd);
    
    void insertShiftTeam(ShiftTeamCommand command);

    void insertShiftTeamUser(ShiftTeamUserCommand command);

    void updateShiftTeamNm(UpdateShiftTeamNmCommand command);

    void deleteShiftTeamUser(DeleteShiftTeamUserCommand command);

    void insertShiftTeamUsers(InsertShiftTeamUsersCommand command);

    void updateShiftTeamLeaders(UpdateShiftTeamLeadersCommand command);

    void updateShiftTeamPeriod(UpdateShiftTeamPeriodCommand command);

    void deleteShiftTeam(DeleteShiftTeamCommand command);
    
    void deleteShiftTeamAllUser(DeleteShiftTeamCommand command);
    
    List<String> selectShiftPtrnSchList(SchCdListQuery query);
    
    void upsertUserWorkPlanList(UserWorkPlanCommand command);
}
