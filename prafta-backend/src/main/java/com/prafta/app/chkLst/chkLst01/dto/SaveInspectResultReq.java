package com.prafta.app.chkLst.chkLst01.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class SaveInspectResultReq {
	String cmpnyCd;						// 회사코드
	String siteCd;						// 사업장코드
	String userCd;						// 사용자코드
    String chkptCd;						// 점검항목코드
    String workDate;					// 점검일자
    MultipartFile items;
}