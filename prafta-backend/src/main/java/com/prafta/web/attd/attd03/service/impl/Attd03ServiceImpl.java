package com.prafta.web.attd.attd03.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.exception.leave.LeaveApiException;
import com.prafta.web.attd.attd03.dto.LeaveNoDupChkQry;
import com.prafta.web.attd.attd03.dto.LeaveTypeListQry;
import com.prafta.web.attd.attd03.dto.LeaveTypeListReq;
import com.prafta.web.attd.attd03.dto.LeaveTypeListRes;
import com.prafta.web.attd.attd03.dto.LeaveTypeReq;
import com.prafta.web.attd.attd03.dto.LeaveTypeSave;
import com.prafta.web.attd.attd03.mapper.Attd03Mapper;
import com.prafta.web.attd.attd03.service.Attd03Service;
import com.prafta.web.attd.attd03.vo.LeaveNoDupChk;
import com.prafta.web.attd.attd03.vo.LeaveType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Attd03ServiceImpl implements Attd03Service{
	private final Attd03Mapper attd03Mapper;
		
	public Attd03ServiceImpl(Attd03Mapper attd03Mapper) {
		this.attd03Mapper = attd03Mapper;
	}
	
	public void updateLeaveType(LeaveTypeReq dto, Map<String, Object> tokenInfo) {
		
		String leaveCd = null;
		
		if(dto.getLeaveCd() != null && dto.getLeaveCd() != "") {
			leaveCd = dto.getLeaveCd();
		} else {
			leaveCd = attd03Mapper.selectLeaveCd(tokenInfo);
		}
		
		LeaveNoDupChkQry leaveNoDupChkQry = LeaveNoDupChkQry.builder()
															.leaveNo(dto.getLeaveNo())
															.build();
		
		LeaveNoDupChk leaveNoDupChk = attd03Mapper.selectLeaveNoDupChkResult(leaveNoDupChkQry, tokenInfo);
		
		if(leaveNoDupChk != null) {
			throw new LeaveApiException("이미 사용중인 연차코드입니다.");
		}
		
		LeaveTypeSave leaveTypeSave = LeaveTypeSave.builder()
												.leaveCd(leaveCd)
				
									            // A. 타입구분
									            .leaveType(dto.getLeaveType())
									            .grantType(dto.getGrantType())
								
									            // B. 기본구분
									            .leaveNo(dto.getLeaveNo())
									            .leaveNm(dto.getLeaveNm())
									            .paidType(dto.getPaidType())
									            .leaveNatureType(dto.getLeaveNatureType())
									            .useYn(dto.getUseYn())
									            .leaveDesc(dto.getLeaveDesc())
								
									            // C. 사용규칙 - 사용자 신청 타입(leaveType=01)
									            .maxAplyDays(dto.getMaxAplyDays())
									            .useUnitType(dto.getUseUnitType())
									            .availTermType(dto.getAvailTermType())
									            .availFromDt(dto.getAvailFromDt())
									            .availToDt(dto.getAvailToDt())
								
									            // C. 사용규칙 - 관리자 부여 타입(수동부여)
									            .grantDays(dto.getGrantDays())
									            .adminAvailTermType(dto.getAdminAvailTermType())
									            .adminAvailFromDt(dto.getAdminAvailFromDt())
									            .adminAvailToDt(dto.getAdminAvailToDt())
								
									            // C. 사용규칙 - 관리자 부여 타입(자동부여)
									            .grantBaseType(dto.getGrantBaseType())
									            .grantOffsetMonth(dto.getGrantOffsetMonth())
								
									            // D. 결재 및 증빙
									            .aprvUseYn(dto.getAprvUseYn())
									            .aprvStepCnt(dto.getAprvStepCnt())
									            .hrFinalAprvYn(dto.getHrFinalAprvYn())
									            .evidenceYn(dto.getEvidenceYn())
									            .evidenceGuideMsg(dto.getEvidenceGuideMsg())
								
									            .build();
		
		attd03Mapper.updateLeaveType(leaveTypeSave, tokenInfo);
		
	}
	
	public LeaveTypeListRes getLeaves(LeaveTypeListReq dto, Map<String, Object> tokenInfo) {
		
		LeaveTypeListQry leaveTypeListQry = LeaveTypeListQry.builder()
															.leaveNo(dto.getLeaveNo())
															.leaveNm(dto.getLeaveNm())
															.leaveType(dto.getLeaveType())
															.useYn(dto.getUseYn())
															.build();
		
		LeaveTypeListRes leaveTypeListRes = null;
		
		List<LeaveType> leaveTypeList = attd03Mapper.selectLeaves(leaveTypeListQry, tokenInfo);
		
		if(leaveTypeList != null && leaveTypeList.size() > 0) {
			leaveTypeListRes = LeaveTypeListRes.builder()
												.leaveTypeList(leaveTypeList)
												.build();
		}
		
		return leaveTypeListRes;
		
	}
	
	
	
	
	
	
	
	
//	public HolidayListRes selectHoliday(HolidayListdto dto, Map<String, Object> tokenInfo) {
//		
//		HolidayListQry holidayListQry = HolidayListQry.builder()
//													.year(dto.getYear())
//													.month(dto.getMonth())
//													.build();
//		
//		HolidayListRes holidayListRes = null;
//		
//		List<Holiday> holidayList = attd02Mapper.selectHoliday(holidayListQry, tokenInfo);
//		
//		if(holidayList != null && holidayList.size() > 0) {
//			holidayListRes = HolidayListRes.builder()
//											.holidayList(holidayList)
//											.build();
//		}
//		
//		return holidayListRes;
//	}
//	
//	public void updateHolidayInfo(Holidaydto dto, Map<String, Object> tokenInfo) {
//		
//		System.out.println("dto :: " + dto);
//		
//		String holidayId = null;
//		String holidayType = null;
//		
//		HolidaySave holidaySave = HolidaySave.builder()
//											.siteCd(dto.getSiteCd())
//											.holidayNm(dto.getHolidayNm())
//											.holidayYmd(dto.getHolidayYmd())
////											.holidayType(dto.getHolidayType())
//											.repeatYearly(dto.isRepeatYearly())
//											.useYn(dto.getUseYn())
//											.build();
//		
//		if(!dto.getHolidayId().isEmpty()) {
//			holidayId = dto.getHolidayId();
//			holidayType = dto.getHolidayType();
//		} else {
//			if(dto.isRepeatYearly()) {
//				holidayId = attd02Mapper.selectHolidayRuleId(tokenInfo);
//				holidayType = "03";			/* 반복 */
//			} else {
//				holidayId = attd02Mapper.selectHolidayId(tokenInfo);
//				holidayType = "02";			/* 반복 */
//			}
//		}
//		
//		holidaySave = holidaySave.toBuilder().holidayId(holidayId).holidayType(holidayType).build();
//		
//		if(dto.isRepeatYearly()) {
//			attd02Mapper.updateHolidayRule(holidaySave, tokenInfo);
//		} else {
//			attd02Mapper.updateHoliday(holidaySave, tokenInfo);
//		}
//	}
//	
////	
////	public SchInfoListRes selectSchInfoList(SchInfoListdto dto, Map<String, Object> tokenInfo) {
////		SchInfoListQry dtoDto = SchInfoListQry.builder()
////												.siteCd(dto.getSiteCd())
////												.schNo(dto.getSchNo())
////												.schType(dto.getSchType())
////												.useYn(dto.getUseYn())
////												.build();
////		
////		SchInfoListRes resDto = null;
////		
////		List<SchInfo> schInfoList = attd01Mapper.selectSchInfoList(dtoDto, tokenInfo);
////		
////		if(schInfoList != null && !schInfoList.isEmpty()) {
////			resDto = SchInfoListRes.builder()
////									.schInfoList(schInfoList)
////									.build();
////		}
////		
////		return resDto;
////	
////	}
////	
////	@Transactional
////	public void updateSchInfo(SchInfodto dto, Map<String, Object> tokenInfo) {
////		
////		String schCd = null;
////		
////		if(dto.getSchCd() != null && dto.getSchCd() != "") {
////			schCd = dto.getSchCd();
////		} else {
////			SchCdQry schCdQry = SchCdQry.builder().siteCd(dto.getSiteCd()).build();
////			
////			schCd = attd01Mapper.selectSchCd(schCdQry, tokenInfo);
////		}
////		
////		SchInfoSave schInfoSave = SchInfoSave.builder()
////							    .cmpnyCd(dto.getCmpnyCd())
////							    .siteCd(dto.getSiteCd())
////							    .schCd(schCd)
////							    .schNo(dto.getSchNo())
////							    .schType(dto.getSchType())
////							    .applyDate(dto.getApplyDate())
////				
////							    .fstSchStrTime(dto.getFstSchStrTime())
////							    .fstSchEndTime(dto.getFstSchEndTime())
////							    .fstSchBrkMin(dto.getFstSchBrkMin())
////				
////							    .secSchStrTime(dto.getSecSchStrTime())
////							    .secSchEndTime(dto.getSecSchEndTime())
////							    .secSchBrkMin(dto.getSecSchBrkMin())
////				
////							    .useYn(dto.getUseYn())
////							    .build();
////		
////		attd01Mapper.updateSchInfo(schInfoSave, tokenInfo);
////		
////		SchInfoHistQry schInfoHistQry = SchInfoHistQry.builder()
////														.siteCd(dto.getSiteCd())
////														.schCd(schCd)
////														.build();
////		
////		int histIdx = attd01Mapper.selectSchHistIdx(schInfoHistQry, tokenInfo);
////		
////		SchInfoHistSave schInfoHistSave = SchInfoHistSave.builder()
////			    .cmpnyCd(dto.getCmpnyCd())
////			    .siteCd(dto.getSiteCd())
////			    .histIdx(histIdx)
////			    .schCd(schCd)
////			    .applyDate(dto.getApplyDate())
////
////			    .fstSchStrTime(dto.getFstSchStrTime())
////			    .fstSchEndTime(dto.getFstSchEndTime())
////			    .fstSchBrkMin(dto.getFstSchBrkMin())
////
////			    .secSchStrTime(dto.getSecSchStrTime())
////			    .secSchEndTime(dto.getSecSchEndTime())
////			    .secSchBrkMin(dto.getSecSchBrkMin())
////
////			    .useYn(dto.getUseYn())
////			    .build();
////		
////		attd01Mapper.insertSchHistInfo(schInfoHistSave, tokenInfo);
////	}
////	
////	public SchInfoHistRes selectSchHistList(SchInfoHistdto dto, Map<String, Object> tokenInfo) {
////		
////		SchInfoHistQry schInfoHistQry = SchInfoHistQry.builder()
////				.siteCd(dto.getSiteCd())
////				.schCd(dto.getSchCd())
////				.build();
////		
////		SchInfoHistRes schInfoHistRes = null;
////		
////		List<SchHist> schHistList = attd01Mapper.selectSchHistList(schInfoHistQry, tokenInfo);
////		
////		if(schHistList != null && schHistList.size() > 0) {
////			schInfoHistRes = SchInfoHistRes.builder()
////											.schHistList(schHistList)
////											.build();
////		}
////		
////		return schInfoHistRes;
////	}
}
