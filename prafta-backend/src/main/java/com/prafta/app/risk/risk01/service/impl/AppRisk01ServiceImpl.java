package com.prafta.app.risk.risk01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.app.risk.risk01.dto.RiskAssessmentReq;
import com.prafta.app.risk.risk01.dto.RiskAssessmentSave;
import com.prafta.app.risk.risk01.dto.RiskInfoQry;
import com.prafta.app.risk.risk01.dto.RiskInfoReq;
import com.prafta.app.risk.risk01.dto.RiskInfoRes;
import com.prafta.app.risk.risk01.mapper.AppRisk01Mapper;
import com.prafta.app.risk.risk01.service.AppRisk01Service;
import com.prafta.app.risk.risk01.vo.RiskCategory;
import com.prafta.app.risk.risk01.vo.RiskHazard;
import com.prafta.app.risk.risk01.vo.RiskType;
import com.prafta.common.cmm.file.dto.FileInfoSave;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.exception.RiskApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppRisk01ServiceImpl implements AppRisk01Service {

    private final AppRisk01Mapper appRisk01Mapper;
    private final ObjectMapper objectMapper;
    private final FileService fileService;
    private final FileMapper fileMapper;
    
    
    public RiskInfoRes selectRiskTypeInfo(RiskInfoReq request, Map<String, Object> tokenInfo) {
    	
    	RiskInfoQry reqDto = RiskInfoQry.builder()
				.siteCd(request.getSiteCd())
				.build();
    	
    	RiskInfoRes resDto = null;
    	
    	List<RiskCategory> riskCategoryList = appRisk01Mapper.selectRiskCategory(reqDto, tokenInfo);
    	
    	List<RiskType> riskTypeList = appRisk01Mapper.selectRiskType(reqDto, tokenInfo);
    	
    	List<RiskHazard> riskHazardList = appRisk01Mapper.selectRiskHazard(reqDto, tokenInfo);
    	
    	if(riskCategoryList != null) {
    		resDto = RiskInfoRes.builder()
					.riskCategoryList(riskCategoryList)
					.build();
    	}
    	
    	if(riskTypeList != null) {
    		resDto = resDto.toBuilder()
					.riskTypeList(riskTypeList)
					.build();
    	}
    	
    	if(riskHazardList != null) {
    		resDto = resDto.toBuilder()
					.riskHazardList(riskHazardList)
					.build();
    	}
    	
    	return resDto;
    }
    
    public void saveRiskAssessments(RiskAssessmentReq request, MultipartFile file, Map<String, Object> tokenInfo) {
    	try {
    		String fileMgmtCd = "";
    		if (file != null && !file.isEmpty()) {
    			
    			FileInfoSave baseQuery = FileInfoSave.builder()
                        .cmpnyCd(tokenInfo.get("gv_cmpnyCd").toString())
                        .userId(tokenInfo.get("gv_userId").toString())
                        .siteCd(request.getSiteCd())
                        .fileType("002")     	// 002: 위험성평가
                        .itemCd("")      // 1번은 itemCd=inspectItemCd 였고, 2번은 riskId 같은 업무키를 넣는 걸 추천
                        .fileName("")
                        .build();
    			
    			System.out.println("baseQuery :: " + baseQuery);
    			
    			fileMgmtCd = fileMapper.selectFileMgmtCd(baseQuery);
    			
    			FileInfoSave queryWithKey = baseQuery.toBuilder()
                        .fileMgmtCd(fileMgmtCd)
                        .build();
    			
    			fileService.saveFile(queryWithKey, file);
    		}
    		
    		RiskAssessmentSave riskHazardInfoSave = RiskAssessmentSave.builder()
    				.siteCd(request.getSiteCd())
    				.processCd(request.getProcessCd())
    				.riskTypeCd(request.getRiskTypeCd())
    				.hazardCd(request.getHazardCd())
    				.assessmentDesc(request.getAssessmentDesc())
    				.assessmentStatus("001")						// 검토요청 상태
    				.initDesc(request.getInitDesc())
    				
    				.initLikelihoodScore(request.getInitLikelihoodScore())
    				.initSeverityScore(request.getInitSeverityScore())
    				.initRiskLv(request.getInitRiskLv())
    				.initFileMgmtCd(fileMgmtCd)
					.build();
    		
    		appRisk01Mapper.mergeRiskAssessment(riskHazardInfoSave, tokenInfo);
    	} catch (Exception e) {
    		throw new RiskApiException(e.getMessage());
    	}
    }

//    @Override
//    public ChecklistInfoRes selectChkLstInfo(ChecklistInfoReq request, Map<String, Object> tokenInfo) {
//    	
//    	ChecklistInfoQry reqDto = ChecklistInfoQry.builder()
//    									.cmpnyCd(request.getCmpnyCd())
//    									.siteCd(request.getSiteCd())
//    									.chkptCd(request.getChkptCd())
//    									.chkptNm(request.getChkptNm())
//    									.build();
//    	
//    	List<ChecklistInfo> resDto = appRisk01Mapper.selectChkLstInfo(reqDto, tokenInfo); 
//    	
//    	ChecklistInfoRes retDto = ChecklistInfoRes.builder()
//    								.checklistInfos(resDto)
//    								.build();
//        return retDto;
//    }
//
//    @Override
//    @Transactional
//    public void saveInspectResult(SaveInspectResultReq request, Map<String, MultipartFile> files, Map<String, Object> tokenInfo) {
//    	
//        try {
//            // MultipartFile -> JSON String
//            MultipartFile itemsFile = request.getItems();
//            String itemsJson = null;
//            if (itemsFile != null && !itemsFile.isEmpty()) {
//                itemsJson = new String(itemsFile.getBytes(),StandardCharsets.UTF_8);
//            }
//
//            // JSON 파싱 -> List<FileInfo>
//            List<FileInfo> items = new ArrayList<>();
//            if (itemsJson != null && !itemsJson.isEmpty()) {
//                JsonNode node = objectMapper.readTree(itemsJson);
//                if (node.isArray()) {
//                    for (JsonNode n : node) {
//                        items.add(objectMapper.treeToValue(n, FileInfo.class));
//                    }
//                }
//            }
//
//            // files Map -> itemCd 기준으로 매핑 (키: files[ITEM_CD])
//            Pattern p = Pattern.compile("^files\\[(.+)]$");
//            Map<String, MultipartFile> fileByItemCd = new HashMap<>();
//            if (files != null) {
//                for (Map.Entry<String, MultipartFile> e : files.entrySet()) {
//                    Matcher m = p.matcher(e.getKey());
//                    if (m.find()) {
//                        fileByItemCd.put(m.group(1), e.getValue());
//                    }
//                }
//            }
//            
//            // items 를 돌면서 "건별"로 fileMgmtCd 생성 + FileService 호출
//            for (FileInfo it : items) {
//            	String userId = tokenInfo.get("gv_userId").toString();  // or request.getUserCd()
//            	String fileMgmtCd = "";
//            	String answerDesc = "";									// 점검답변
//            	answerDesc = it.getAnswerDesc();
//            	
//            	MultipartFile img = fileByItemCd.get(it.getItemCd());
//            	if (img != null && !img.isEmpty()) {
//            		// 기본 FileSaveQuery (fileMgmtCd 없이)
//                    FileInfoSave baseQuery = FileInfoSave.builder()
//                            .cmpnyCd(request.getCmpnyCd())
//                            .userId(userId)
//                            .siteCd(request.getSiteCd())
//                            .fileType("001")          // 일일점검 타입
//                            .itemCd(it.getItemCd())
//                            .fileName(it.getFileName())
//                            .build();
//                    // 건별 fileMgmtCd 생성
//            		fileMgmtCd = fileMapper.selectFileMgmtCd(baseQuery);
//            		
//            		FileInfoSave queryWithKey = baseQuery.toBuilder()
//                            .fileMgmtCd(fileMgmtCd)
//                            .build();
//            		
//            		fileService.saveFile(queryWithKey, img);
//            	}
//
//                ChecklistInfoSave checklistInfoSave = ChecklistInfoSave.builder()
//						.cmpnyCd(request.getCmpnyCd())
//						.siteCd(request.getSiteCd())
//						.workDate(request.getWorkDate())
//						.inspectItemCd(it.getItemCd())
//						.inspectAnswerType(it.getInspectValue())
//						.answerDesc(it.getAnswerDesc())
//						.fileMgmtCd(fileMgmtCd)
//						.build();
//
//                appRisk01Mapper.mergeChkptInspectAnswer(checklistInfoSave, tokenInfo);
//            }
//
//        } catch (Exception e) {
//            log.error("saveInspectResult 처리 중 오류", e);
//            throw new RuntimeException("일일점검 결과 저장 중 오류", e);
//        }
//    }
}
