package com.prafta.app.chkLst.chkLst01.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoQry;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoReq;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoRes;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoSave;
import com.prafta.app.chkLst.chkLst01.dto.SaveInspectResultReq;
import com.prafta.app.chkLst.chkLst01.mapper.AppChkLst01Mapper;
import com.prafta.app.chkLst.chkLst01.service.AppChkLst01Service;
import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;
import com.prafta.common.cmm.file.application.model.FileInfoModel;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.dto.TokenInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppChkLst01ServiceImpl implements AppChkLst01Service {

    private final AppChkLst01Mapper appChkLst01Mapper;
    private final ObjectMapper objectMapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    @Override
    public ChecklistInfoRes selectChkLstInfo(ChecklistInfoReq request, TokenInfo tokenInfo) {
    	
    	ChecklistInfoQry reqDto = ChecklistInfoQry.builder()
    									.cmpnyCd(request.getCmpnyCd())
    									.siteCd(request.getSiteCd())
    									.chkptCd(request.getChkptCd())
    									.chkptNm(request.getChkptNm())
    									.build();
    	
    	List<ChecklistInfo> resDto = appChkLst01Mapper.selectChkLstInfo(reqDto, tokenInfo); 
    	
    	ChecklistInfoRes retDto = ChecklistInfoRes.builder()
    								.checklistInfos(resDto)
    								.build();
        return retDto;
    }

    @Override
    @Transactional
    public void saveInspectResult(SaveInspectResultReq request, Map<String, MultipartFile> files, TokenInfo tokenInfo) {
    	
        try {
            // MultipartFile -> JSON String
            MultipartFile itemsFile = request.getItems();
            String itemsJson = null;
            if (itemsFile != null && !itemsFile.isEmpty()) {
                itemsJson = new String(itemsFile.getBytes(),StandardCharsets.UTF_8);
            }

            List<FileInfoModel> items = new ArrayList<>();
            if (itemsJson != null && !itemsJson.isEmpty()) {
                JsonNode node = objectMapper.readTree(itemsJson);
                if (node.isArray()) {
                    for (JsonNode n : node) {
                        items.add(objectMapper.treeToValue(n, FileInfoModel.class));
                    }
                }
            }

            Pattern p = Pattern.compile("^files\\[(.+)]$");
            Map<String, MultipartFile> fileByItemCd = new HashMap<>();
            if (files != null) {
                for (Map.Entry<String, MultipartFile> e : files.entrySet()) {
                    Matcher m = p.matcher(e.getKey());
                    if (m.find()) {
                        fileByItemCd.put(m.group(1), e.getValue());
                    }
                }
            }
            
            for (FileInfoModel it : items) {
            	String userId = tokenInfo.gv_userCd();  // or request.getUserCd()
            	String fileMgmtCd = "";
            	String answerDesc = "";
            	answerDesc = it.answerDesc();

            	MultipartFile img = fileByItemCd.get(it.itemCd());
            	if (img != null && !img.isEmpty()) {
            		// 건별 fileMgmtCd 생성
            		fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(request.getCmpnyCd(), "001"));		// 001: 일일점검

            		fileService.fileSave(FileInfoParam.from(
            				request.getCmpnyCd()
            				, userId
            				, request.getSiteCd()
            				, "001"							// 일일점검
            				, fileMgmtCd
            				, img
            		));
            	}

                ChecklistInfoSave checklistInfoSave = ChecklistInfoSave.builder()
						.cmpnyCd(request.getCmpnyCd())
						.siteCd(request.getSiteCd())
						.workDate(request.getWorkDate())
						.inspectItemCd(it.itemCd())
						.inspectAnswerType(it.inspectValue())
						.answerDesc(it.answerDesc())
						.fileMgmtCd(fileMgmtCd)
						.build();

                appChkLst01Mapper.mergeChkptInspectAnswer(checklistInfoSave, tokenInfo);
            }

        } catch (Exception e) {

        }
    }
}
