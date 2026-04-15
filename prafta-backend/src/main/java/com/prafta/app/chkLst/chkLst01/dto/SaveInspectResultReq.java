package com.prafta.app.chkLst.chkLst01.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class SaveInspectResultReq {
	String cmpnyCd;
	String siteCd;
	String userCd;
    String chkptCd;
    String workDate;
    MultipartFile items;
}