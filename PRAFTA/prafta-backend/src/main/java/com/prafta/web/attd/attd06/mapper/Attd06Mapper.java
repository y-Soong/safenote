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
import com.prafta.web.attd.attd06.result.LeaveRangeResult;
import com.prafta.web.attd.attd06.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd06.result.ShiftTeamMemberResult;
import com.prafta.web.attd.attd06.result.ShiftTeamPeriodResult;
import com.prafta.web.attd.attd06.result.ShiftTeamUserInfosResult;
import com.prafta.web.attd.attd06.result.ShiftTypeDetailListsResult;
import com.prafta.web.attd.attd06.result.ShiftTypeListsResult;
import com.prafta.web.attd.attd06.result.UserListsResult;

import org.apache.ibatis.annotations.Param;

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

    // prafta-com-013-05-1: 사용자 구간과 겹치는 종일 확정 연차 구간(START/END) 목록.
    List<LeaveRangeResult> selectFullDayLeaveRanges(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("startYmd") String startYmd
            , @Param("endYmd") String endYmd);

    // prafta-com-013-05-2: 교대팀 현재 적용기간(연장 판정 기준).
    ShiftTeamPeriodResult selectShiftTeamPeriod(UpdateShiftTeamPeriodCommand command);

    // prafta-com-013-05-2: 교대팀 현 소속 멤버(연장 구간 재생성 대상).
    List<ShiftTeamMemberResult> selectShiftTeamActiveMembers(UpdateShiftTeamPeriodCommand command);

    // prafta-com-016-D 보안 재작업: 팀 단위 쓰기(팀명/기간/팀삭제) 권한 가드용 — 해당 교대팀 현 소속(탈퇴 미마킹) 멤버 USER_CD.
    List<String> selectShiftTeamMemberUserCds(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("shiftCd") String shiftCd
            , @Param("shiftTeamId") String shiftTeamId);

    // prafta-com-016-D-2/D-3: 교대팀 1건의 기간(STR/END)과 팀명(PUSH 본문 치환용) 조회.
    ShiftTeamInfoResult selectShiftTeamInfo(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("siteCd") String siteCd
            , @Param("shiftCd") String shiftCd
            , @Param("shiftTeamId") String shiftTeamId);
}
