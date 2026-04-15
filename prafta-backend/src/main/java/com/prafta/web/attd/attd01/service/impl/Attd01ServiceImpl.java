package com.prafta.web.attd.attd01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.attd.attd01.application.command.SchInfoCommand;
import com.prafta.web.attd.attd01.application.command.SchInfoHistCommand;
import com.prafta.web.attd.attd01.application.command.ShiftAssignCommand;
import com.prafta.web.attd.attd01.application.command.ShiftPatternCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTypeCommand;
import com.prafta.web.attd.attd01.application.param.SchInfoHistParam;
import com.prafta.web.attd.attd01.application.param.SchInfoListParam;
import com.prafta.web.attd.attd01.application.param.SchInfoParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchDetailParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoListParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftAssignParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftPatternParam;
import com.prafta.web.attd.attd01.application.param.ShiftSchInfoParam.ShiftTeamParam;
import com.prafta.web.attd.attd01.application.query.SchCdQuery;
import com.prafta.web.attd.attd01.application.query.SchInfoHistQuery;
import com.prafta.web.attd.attd01.application.query.SchInfoListQuery;
import com.prafta.web.attd.attd01.application.query.ShiftCdQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchDetailQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchInfoListQuery;
import com.prafta.web.attd.attd01.dto.response.SchInfoHistResponse;
import com.prafta.web.attd.attd01.dto.response.SchInfoListResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchDetailResponse;
import com.prafta.web.attd.attd01.dto.response.ShiftSchInfoListResponse;
import com.prafta.web.attd.attd01.mapper.Attd01Mapper;
import com.prafta.web.attd.attd01.result.SchHistResult;
import com.prafta.web.attd.attd01.result.SchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftAssignInfoResult;
import com.prafta.web.attd.attd01.result.ShiftPatternInfoResult;
import com.prafta.web.attd.attd01.result.ShiftSchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTypeInfoResult;
import com.prafta.web.attd.attd01.service.Attd01Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd01ServiceImpl implements Attd01Service{
	private final Attd01Mapper attd01Mapper;
		
	public Attd01ServiceImpl(Attd01Mapper attd01Mapper) {
		this.attd01Mapper = attd01Mapper;
	}
	
	public SchInfoListResponse selectSchInfoList(SchInfoListParam param) {
		
		SchInfoListResponse response = null;
		
		List<SchInfoResult> schInfoResultList = attd01Mapper.selectSchInfoList(SchInfoListQuery.from(param));
		
		if(schInfoResultList != null && !schInfoResultList.isEmpty()) {
			response = SchInfoListResponse.builder()
									.schInfoResultList(schInfoResultList)
									.build();
		}
		
		return response;
	
	}
	
	@Transactional
	public void updateSchInfo(SchInfoParam param) {
		
		String schCd = null;
		
		if(param.schCd() != null && param.schCd() != "") {
			schCd = param.schCd();
		} else {
			schCd = attd01Mapper.selectSchCd(SchCdQuery.from(param));
		}
		
		attd01Mapper.updateSchInfo(SchInfoCommand.from(param));
		
		int histIdx = attd01Mapper.selectSchHistIdx(SchInfoHistQuery.from(param));
		
		attd01Mapper.insertSchHistInfo(SchInfoHistCommand.from(param, histIdx, schCd));
	}
	
	public SchInfoHistResponse selectSchHistList(SchInfoHistParam param) {
		
		
		SchInfoHistResponse response = null;
		
		List<SchHistResult> schHistResultList = attd01Mapper.selectSchHistList(SchInfoHistQuery.from(param));
		
		if(schHistResultList != null && schHistResultList.size() > 0) {
			response = SchInfoHistResponse.builder()
											.schHistResultList(schHistResultList)
											.build();
		}
		
		return response;
	}
	
	@Transactional
	public void updateShiftSchInfo(ShiftSchInfoParam param) {
		
		String shiftCd = attd01Mapper.selectShiftCd(ShiftCdQuery.from(param));
		
		attd01Mapper.insertShiftSch(ShiftTypeCommand.from(param, shiftCd));
		
		if(param.shiftPatternList() != null && param.shiftPatternList().size() > 0) {
			for(ShiftPatternParam shiftPatternParam : param.shiftPatternList()) {
				attd01Mapper.insertShiftSchPtrn(ShiftPatternCommand.from(shiftPatternParam, shiftCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}
		
		if(param.shiftTeamList() != null && param.shiftTeamList().size() > 0) {
			for(ShiftTeamParam shiftTeamParam : param.shiftTeamList()) {
				
				attd01Mapper.insertShiftSchTeam(ShiftTeamCommand.from(shiftTeamParam, shiftCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}
		
		if(param.shiftAssignList() != null && param.shiftAssignList().size() > 0) {
			for(ShiftAssignParam shiftAssignParam : param.shiftAssignList()) {

				attd01Mapper.insertShiftSchAssign(ShiftAssignCommand.from(shiftAssignParam, shiftCd, param.gvCmpnyCd(), param.gvUserCd()));
			}
		}
	}
	
	public ShiftSchInfoListResponse selectShiftSchInfoList(ShiftSchInfoListParam param) {
		
		ShiftSchInfoListResponse response = null;
		
		List<ShiftSchInfoResult> shiftSchInfoResultList = attd01Mapper.selectShiftSchInfoList(ShiftSchInfoListQuery.from(param));
		
		if(shiftSchInfoResultList != null && shiftSchInfoResultList.size() > 0) {
			response = ShiftSchInfoListResponse.builder()
													.shiftSchInfoResultList(shiftSchInfoResultList)
													.build();
		}
	
		
		return response;
	}
	
	public ShiftSchDetailResponse selectShiftSchDetail(ShiftSchDetailParam param) {
		
		ShiftSchDetailResponse response = null;
		
		List<ShiftTypeInfoResult> shiftTypeInfoResultList = attd01Mapper.selectShiftTypeInfoList(ShiftSchDetailQuery.from(param));
		
		List<ShiftPatternInfoResult> shiftPatternInfoResultList = attd01Mapper.selectShiftPatternInfoList(ShiftSchDetailQuery.from(param));
		
		List<ShiftTeamInfoResult> shiftTeamInfoResultList = attd01Mapper.selectShiftTeamInfoList(ShiftSchDetailQuery.from(param));
		
		List<ShiftAssignInfoResult> shiftAssignInfoResultList = attd01Mapper.selectShiftAssignInfoList(ShiftSchDetailQuery.from(param));
		
		response = ShiftSchDetailResponse.builder()
											.shiftTypeInfoResultList(shiftTypeInfoResultList)
											.shiftPatternInfoResultList(shiftPatternInfoResultList)
											.shiftTeamInfoResultList(shiftTeamInfoResultList)
											.shiftAssignInfoResultList(shiftAssignInfoResultList)
											.build();
		
		return response;
	}
}
