package com.prafta.web.attd.attd01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.attd.attd01.dto.SchCdQry;
import com.prafta.web.attd.attd01.dto.SchInfoHistQry;
import com.prafta.web.attd.attd01.dto.SchInfoHistReq;
import com.prafta.web.attd.attd01.dto.SchInfoHistRes;
import com.prafta.web.attd.attd01.dto.SchInfoHistSave;
import com.prafta.web.attd.attd01.dto.SchInfoListQry;
import com.prafta.web.attd.attd01.dto.SchInfoListReq;
import com.prafta.web.attd.attd01.dto.SchInfoListRes;
import com.prafta.web.attd.attd01.dto.SchInfoReq;
import com.prafta.web.attd.attd01.dto.SchInfoSave;
import com.prafta.web.attd.attd01.dto.ShiftAssignSave;
import com.prafta.web.attd.attd01.dto.ShiftCdQry;
import com.prafta.web.attd.attd01.dto.ShiftPatternSave;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailQry;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailReq;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailRes;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListQry;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListReq;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListRes;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoReq;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoReq.ShiftAssign;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoReq.ShiftPattern;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoReq.ShiftTeam;
import com.prafta.web.attd.attd01.dto.ShiftTeamSave;
import com.prafta.web.attd.attd01.dto.ShiftTypeSave;
import com.prafta.web.attd.attd01.mapper.Attd01Mapper;
import com.prafta.web.attd.attd01.service.Attd01Service;
import com.prafta.web.attd.attd01.vo.SchHist;
import com.prafta.web.attd.attd01.vo.SchInfo;
import com.prafta.web.attd.attd01.vo.ShiftAssignInfo;
import com.prafta.web.attd.attd01.vo.ShiftPatternInfo;
import com.prafta.web.attd.attd01.vo.ShiftSchInfo;
import com.prafta.web.attd.attd01.vo.ShiftTeamInfo;
import com.prafta.web.attd.attd01.vo.ShiftTypeInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd01ServiceImpl implements Attd01Service{
	private final Attd01Mapper attd01Mapper;
		
	public Attd01ServiceImpl(Attd01Mapper attd01Mapper) {
		this.attd01Mapper = attd01Mapper;
	}
	
	public SchInfoListRes selectSchInfoList(SchInfoListReq dto, Map<String, Object> tokenInfo) {
		SchInfoListQry reqDto = SchInfoListQry.builder()
												.siteCd(dto.getSiteCd())
												.schNo(dto.getSchNo())
												.schType(dto.getSchType())
												.useYn(dto.getUseYn())
												.build();
		
		SchInfoListRes resDto = null;
		
		List<SchInfo> schInfoList = attd01Mapper.selectSchInfoList(reqDto, tokenInfo);
		
		if(schInfoList != null && !schInfoList.isEmpty()) {
			resDto = SchInfoListRes.builder()
									.schInfoList(schInfoList)
									.build();
		}
		
		return resDto;
	
	}
	
	@Transactional
	public void updateSchInfo(SchInfoReq dto, Map<String, Object> tokenInfo) {
		
		String schCd = null;
		
		if(dto.getSchCd() != null && dto.getSchCd() != "") {
			schCd = dto.getSchCd();
		} else {
			SchCdQry schCdQry = SchCdQry.builder().siteCd(dto.getSiteCd()).build();
			
			schCd = attd01Mapper.selectSchCd(schCdQry, tokenInfo);
		}
		
		SchInfoSave schInfoSave = SchInfoSave.builder()
							    .cmpnyCd(dto.getCmpnyCd())
							    .siteCd(dto.getSiteCd())
							    .schCd(schCd)
							    .schNo(dto.getSchNo())
							    .schType(dto.getSchType())
							    .applyDate(dto.getApplyDate())
				
							    .fstSchStrTime(dto.getFstSchStrTime())
							    .fstSchEndTime(dto.getFstSchEndTime())
							    .fstSchBrkMin(dto.getFstSchBrkMin())
				
							    .secSchStrTime(dto.getSecSchStrTime())
							    .secSchEndTime(dto.getSecSchEndTime())
							    .secSchBrkMin(dto.getSecSchBrkMin())
				
							    .useYn(dto.getUseYn())
							    .build();
		
		attd01Mapper.updateSchInfo(schInfoSave, tokenInfo);
		
		SchInfoHistQry schInfoHistQry = SchInfoHistQry.builder()
														.siteCd(dto.getSiteCd())
														.schCd(schCd)
														.build();
		
		int histIdx = attd01Mapper.selectSchHistIdx(schInfoHistQry, tokenInfo);
		
		SchInfoHistSave schInfoHistSave = SchInfoHistSave.builder()
			    .cmpnyCd(dto.getCmpnyCd())
			    .siteCd(dto.getSiteCd())
			    .histIdx(histIdx)
			    .schCd(schCd)
			    .applyDate(dto.getApplyDate())

			    .fstSchStrTime(dto.getFstSchStrTime())
			    .fstSchEndTime(dto.getFstSchEndTime())
			    .fstSchBrkMin(dto.getFstSchBrkMin())

			    .secSchStrTime(dto.getSecSchStrTime())
			    .secSchEndTime(dto.getSecSchEndTime())
			    .secSchBrkMin(dto.getSecSchBrkMin())

			    .useYn(dto.getUseYn())
			    .build();
		
		attd01Mapper.insertSchHistInfo(schInfoHistSave, tokenInfo);
	}
	
	public SchInfoHistRes selectSchHistList(SchInfoHistReq dto, Map<String, Object> tokenInfo) {
		
		SchInfoHistQry schInfoHistQry = SchInfoHistQry.builder()
				.siteCd(dto.getSiteCd())
				.schCd(dto.getSchCd())
				.build();
		
		SchInfoHistRes schInfoHistRes = null;
		
		List<SchHist> schHistList = attd01Mapper.selectSchHistList(schInfoHistQry, tokenInfo);
		
		if(schHistList != null && schHistList.size() > 0) {
			schInfoHistRes = SchInfoHistRes.builder()
											.schHistList(schHistList)
											.build();
		}
		
		return schInfoHistRes;
	}
	
	@Transactional
	public void updateShiftSchInfo(ShiftSchInfoReq dto, Map<String, Object> tokenInfo) {
		
		ShiftCdQry shiftCdQry = ShiftCdQry.builder().siteCd(dto.getShiftType().getSiteCd()).build();
		
		String shiftCd = attd01Mapper.selectShiftCd(shiftCdQry, tokenInfo);
		
		ShiftTypeSave shiftTypeSave = ShiftTypeSave.builder()
													.shiftCd(shiftCd)
													.shiftNo(dto.getShiftType().getShiftNo())
													.siteCd(dto.getShiftType().getSiteCd())
													.shiftPtrnCnt(dto.getShiftType().getShiftPtrnCnt())
													.shiftTeamCnt(dto.getShiftType().getShiftTeamCnt())
													.shiftCycleDays(dto.getShiftType().getShiftCycleDays())
													.useYn(dto.getShiftType().getUseYn())
													.build();
		
		attd01Mapper.insertShiftSch(shiftTypeSave, tokenInfo);
		
		if(dto.getShiftPatternList() != null && dto.getShiftPatternList().size() > 0) {
			for(ShiftPattern shiftPattern : dto.getShiftPatternList()) {
				ShiftPatternSave shiftPatternSave = ShiftPatternSave.builder()
																	.shiftCd(shiftCd)
																	.siteCd(shiftPattern.getSiteCd())
																	.ptrnIdx(shiftPattern.getPtrnIdx())
																	.schCd(shiftPattern.getSchCd())
																	.build();
				
				attd01Mapper.insertShiftSchPtrn(shiftPatternSave, tokenInfo);
			}
		}
		
		if(dto.getShiftTeamList() != null && dto.getShiftTeamList().size() > 0) {
			for(ShiftTeam shiftTeam : dto.getShiftTeamList()) {
				ShiftTeamSave shiftTeamSave = ShiftTeamSave.builder()
															.shiftCd(shiftCd)
															.siteCd(shiftTeam.getSiteCd())
															.teamIdx(shiftTeam.getTeamIdx())
															.teamNm(shiftTeam.getTeamNm())
															.build();
				
				attd01Mapper.insertShiftSchTeam(shiftTeamSave, tokenInfo);
			}
		}
		
		if(dto.getShiftAssignList() != null && dto.getShiftAssignList().size() > 0) {
			for(ShiftAssign shiftAssign : dto.getShiftAssignList()) {
				ShiftAssignSave shiftAssignSave = ShiftAssignSave.builder()
						.shiftCd(shiftCd)
						.siteCd(shiftAssign.getSiteCd())
						.dayNo(shiftAssign.getDayNo())
						.teamIdx(shiftAssign.getTeamIdx())
						.assignYn(shiftAssign.getAssignYn())
						.schCd(shiftAssign.getSchCd())
						.build();

				attd01Mapper.insertShiftSchAssign(shiftAssignSave, tokenInfo);
			}
		}
	}
	
	public ShiftSchInfoListRes selectShiftSchInfoList(ShiftSchInfoListReq dto, Map<String, Object> tokenInfo) {
		
		ShiftSchInfoListQry shiftSchInfoListQry = ShiftSchInfoListQry.builder()
																	.siteCd(dto.getSiteCd())
																	.shiftNo(dto.getShiftNo())
																	.shiftCycleDays(dto.getShiftCycleDays())
																	.useYn(dto.getUseYn())
																	.build();
		
		ShiftSchInfoListRes shiftSchInfoListRes = null;
		
		List<ShiftSchInfo> shiftSchInfoList = attd01Mapper.selectShiftSchInfoList(shiftSchInfoListQry, tokenInfo);
		
		if(shiftSchInfoList != null && shiftSchInfoList.size() > 0) {
			shiftSchInfoListRes = ShiftSchInfoListRes.builder()
													.shiftSchInfoList(shiftSchInfoList)
													.build();
		}
	
		
		return shiftSchInfoListRes;
	}
	
	public ShiftSchDetailRes selectShiftSchDetail(ShiftSchDetailReq dto, Map<String, Object> tokenInfo) {
		
		ShiftSchDetailQry shiftSchDetailQry = ShiftSchDetailQry.builder()
																.siteCd(dto.getSiteCd())
																.shiftCd(dto.getShiftCd())
																.build();
		
		ShiftSchDetailRes shiftSchDetailRes = null;
		
		List<ShiftTypeInfo> shiftTypeInfoList = attd01Mapper.selectShiftTypeInfoList(shiftSchDetailQry, tokenInfo);
		
		List<ShiftPatternInfo> shiftPatternInfoList = attd01Mapper.selectShiftPatternInfoList(shiftSchDetailQry, tokenInfo);
		
		List<ShiftTeamInfo> shiftTeamInfoList = attd01Mapper.selectShiftTeamInfoList(shiftSchDetailQry, tokenInfo);
		
		List<ShiftAssignInfo> shiftAssignInfoList = attd01Mapper.selectShiftAssignInfoList(shiftSchDetailQry, tokenInfo);
		
		shiftSchDetailRes = ShiftSchDetailRes.builder()
											.shiftTypeInfoList(shiftTypeInfoList)
											.shiftPatternInfoList(shiftPatternInfoList)
											.shiftTeamInfoList(shiftTeamInfoList)
											.shiftAssignInfoList(shiftAssignInfoList)
											.build();
		
		
		return shiftSchDetailRes;
	}
}
