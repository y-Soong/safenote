package com.prafta.web.attd.attd06.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.attd.attd06.application.command.DeleteShiftTeamCommand;
import com.prafta.web.attd.attd06.application.command.DeleteShiftTeamUserCommand;
import com.prafta.web.attd.attd06.application.command.InsertShiftTeamUsersCommand;
import com.prafta.web.attd.attd06.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd06.application.command.ShiftTeamUserCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamLeadersCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamNmCommand;
import com.prafta.web.attd.attd06.application.command.UpdateShiftTeamPeriodCommand;
import com.prafta.web.attd.attd06.application.command.UserWorkPlanCommand;
import com.prafta.web.attd.attd06.application.model.InsertShiftTeamUsersModel;
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
import com.prafta.web.attd.attd06.application.query.SchCdListQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTeamUserInfosQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTypeDetailListsQuery;
import com.prafta.web.attd.attd06.application.query.ShiftTypeListsQuery;
import com.prafta.web.attd.attd06.application.query.UserListsQuery;
import com.prafta.web.attd.attd06.dto.response.ShiftTeamUserInfosResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeDetailListsResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeListsResponse;
import com.prafta.web.attd.attd06.dto.response.UserListsResponse;
import com.prafta.web.attd.attd06.mapper.Attd06Mapper;
import com.prafta.web.attd.attd06.result.ShiftTeamUserInfosResult;
import com.prafta.web.attd.attd06.result.ShiftTypeDetailListsResult;
import com.prafta.web.attd.attd06.result.ShiftTypeListsResult;
import com.prafta.web.attd.attd06.result.UserListsResult;
import com.prafta.web.attd.attd06.service.Attd06Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd06ServiceImpl implements Attd06Service {

    private final Attd06Mapper attd06Mapper;

    @Override
    public ShiftTypeListsResponse getShiftTypeLists(ShiftTypeListsParam param) {

        List<ShiftTypeListsResult> shiftTypeListsResultList = attd06Mapper.selectShiftTypeLists(ShiftTypeListsQuery.from(param));

        if (shiftTypeListsResultList == null || shiftTypeListsResultList.isEmpty()) {
            return null;
        }

        return ShiftTypeListsResponse.builder()
                .shiftTypeListsResultList(shiftTypeListsResultList)
                .build();
    }

    @Override
    public UserListsResponse getUserLists(UserListsParam param) {

        List<UserListsResult> userListsResultList = attd06Mapper.selectUserLists(UserListsQuery.from(param));

        if (userListsResultList == null || userListsResultList.isEmpty()) {
            return null;
        }

        return UserListsResponse.builder()
                .userListsResultList(userListsResultList)
                .build();
    }

    @Override
    public ShiftTypeDetailListsResponse getShiftTypeDetailLists(ShiftTypeDetailListsParam param) {

        List<ShiftTypeDetailListsResult> shiftTypeDetailListsResultList = attd06Mapper.selectShiftTypeDetailLists(ShiftTypeDetailListsQuery.from(param));

        if (shiftTypeDetailListsResultList == null || shiftTypeDetailListsResultList.isEmpty()) {
            return null;
        }

        return ShiftTypeDetailListsResponse.builder()
                .shiftTypeDetailListsResultList(shiftTypeDetailListsResultList)
                .build();
    }

    @Override
    public ShiftTeamUserInfosResponse getShiftTeamUserInfos(ShiftTeamUserInfosParam param) {

        List<ShiftTeamUserInfosResult> shiftTeamUserInfosResultList = attd06Mapper.selectShiftTeamUserInfos(ShiftTeamUserInfosQuery.from(param));

        if (shiftTeamUserInfosResultList == null || shiftTeamUserInfosResultList.isEmpty()) {
            return null;
        }

        return ShiftTeamUserInfosResponse.builder()
                .shiftTeamUserInfosResultList(shiftTeamUserInfosResultList)
                .build();
    }

    @Override
    @Transactional
    public void insertShiftSchInfos(ShiftSchInfosParam param) {
    	
    	String shiftTeamId = attd06Mapper.selectShiftTeamCd(param.gvCmpnyCd());
    	
    	attd06Mapper.insertShiftTeam(ShiftTeamCommand.from(param.shiftMeta(), shiftTeamId, param.gvCmpnyCd(), param.gvUserCd()));
    	
    	for(ShiftSchInfosParam.TeamParam teamParam : param.teamList()) {
    		attd06Mapper.insertShiftTeamUser(ShiftTeamUserCommand.from(teamParam, param.shiftMeta(), shiftTeamId, param.gvCmpnyCd(), param.gvUserCd()));
    		
    		List<String> schCdPattern = attd06Mapper.selectShiftPtrnSchList(SchCdListQuery.from(
				param.gvCmpnyCd()
				, param.shiftMeta().siteCd()
				, param.shiftMeta().shiftCd()
			));
    		
    		if (schCdPattern == null || schCdPattern.isEmpty()) {
                // 패턴이 아예 없으면 해당 팀은 작업계획 생성 스킵
                continue;
            }
    		
    		for(ShiftSchInfosParam.MemberParam memberParam : teamParam.memberList()) {
    			
    			UserWorkPlanCommand command = UserWorkPlanCommand.from(
    	                memberParam
    	                , param.shiftMeta()
    	                , schCdPattern
    	                , teamParam.teamIdx()
    	                , param.gvCmpnyCd()
    	                , param.gvUserCd()
    	            );
    			
    			// 전부 OFF인 경우 등 빈 리스트면 mapper 호출 스킵
                if (command.userWorkPlanModelList().isEmpty()) {
                    continue;
                }
    			
                attd06Mapper.upsertUserWorkPlanList(command);
    		}
    	}
    }
    
    @Override
    @Transactional
    public void updateShiftUserSchInfos(ShiftUserSchInfosParam param) {
    	
    	for(ShiftUserSchInfosParam.TeamParam teamParam : param.teamList()) {
    		List<String> schCdPattern = attd06Mapper.selectShiftPtrnSchList(SchCdListQuery.from(
				param.gvCmpnyCd()
				, param.shiftMeta().siteCd()
				, param.shiftMeta().shiftCd()
			));
    		
    		if (schCdPattern == null || schCdPattern.isEmpty()) {
                // 패턴이 아예 없으면 해당 팀은 작업계획 생성 스킵
                continue;
            }
    		
    		for(ShiftUserSchInfosParam.MemberParam memberParam : teamParam.memberList()) {
    			
    			UserWorkPlanCommand command = UserWorkPlanCommand.from(
    	                memberParam
    	                , param.shiftMeta()
    	                , schCdPattern
    	                , teamParam.teamIdx()
    	                , param.gvCmpnyCd()
    	                , param.gvUserCd()
    	            );
    			
    			// 전부 OFF인 경우 등 빈 리스트면 mapper 호출 스킵
                if (command.userWorkPlanModelList().isEmpty()) {
                    continue;
                }
    			
                attd06Mapper.upsertUserWorkPlanList(command);
    		}
    	}
    }
    // ###############################
    
    @Override
    @Transactional
    public void updateShiftTeamNm(UpdateShiftTeamNmParam param) {
        attd06Mapper.updateShiftTeamNm(UpdateShiftTeamNmCommand.from(param));
    }

    @Override
    @Transactional
    public void deleteShiftTeamUser(DeleteShiftTeamUserParam param) {
        attd06Mapper.deleteShiftTeamUser(DeleteShiftTeamUserCommand.from(param));
    }

    @Override
    @Transactional
    public void insertShiftTeamUsers(InsertShiftTeamUsersParam param) {
        for (InsertShiftTeamUsersModel model : param.insertShiftTeamUsersModelList()) {
            attd06Mapper.insertShiftTeamUsers(InsertShiftTeamUsersCommand.from(model));
        }
    }

    @Override
    @Transactional
    public void updateShiftTeamLeaders(UpdateShiftTeamLeadersParam param) {
        attd06Mapper.updateShiftTeamLeaders(UpdateShiftTeamLeadersCommand.from(param));
    }

    @Override
    @Transactional
    public void updateShiftTeamPeriod(UpdateShiftTeamPeriodParam param) {
        attd06Mapper.updateShiftTeamPeriod(UpdateShiftTeamPeriodCommand.from(param));
    }

    @Override
    @Transactional
    public void deleteShiftTeam(DeleteShiftTeamParam param) {
        attd06Mapper.deleteShiftTeam(DeleteShiftTeamCommand.from(param));
        attd06Mapper.deleteShiftTeamAllUser(DeleteShiftTeamCommand.from(param));
    }
}
