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
import com.prafta.web.attd.attd06.dto.response.ShiftSchSaveResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTeamUserInfosResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeDetailListsResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeListsResponse;
import com.prafta.web.attd.attd06.dto.response.UserListsResponse;

public interface Attd06Service {

    ShiftTypeListsResponse getShiftTypeLists(ShiftTypeListsParam param);

    UserListsResponse getUserLists(UserListsParam param);

    ShiftTypeDetailListsResponse getShiftTypeDetailLists(ShiftTypeDetailListsParam param);

    ShiftTeamUserInfosResponse getShiftTeamUserInfos(ShiftTeamUserInfosParam param);

    // prafta-com-013-05-1: 차단(연차+휴무 중복)된 날짜 목록을 응답으로 반환.
    ShiftSchSaveResponse insertShiftSchInfos(ShiftSchInfosParam param);

    ShiftSchSaveResponse updateShiftUserSchInfos(ShiftUserSchInfosParam param);

    void updateShiftTeamNm(UpdateShiftTeamNmParam param);

    void deleteShiftTeamUser(DeleteShiftTeamUserParam param);

    // prafta-com-016-D-3: 조원 추가 시 work_plan 덮어쓰기(합류일+1 ~ 종료일) + 보존(연차/OT) 날짜 목록 반환.
    ShiftSchSaveResponse insertShiftTeamUsers(InsertShiftTeamUsersParam param);

    void updateShiftTeamLeaders(UpdateShiftTeamLeadersParam param);

    // prafta-com-013-05-2(재작업): 기간 연장 구간 work_plan 생성 시 차단(연차+휴무) 날짜 목록을 응답으로 반환.
    ShiftSchSaveResponse updateShiftTeamPeriod(UpdateShiftTeamPeriodParam param);

    void deleteShiftTeam(DeleteShiftTeamParam param);

//    void updateShiftSchInfos(UpdateShiftSchInfosParam param);

}
