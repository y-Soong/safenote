package com.prafta.web.tbm.tbm01.service.impl;

import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.command.TbmEduInfoCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemInfoCommand;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduMtrlModel;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduMtrlInfoParam;
import com.prafta.web.tbm.tbm01.application.query.TbmEduInfoListQuery;
import com.prafta.web.tbm.tbm01.application.query.TbmEduItemInfoListQuery;
//import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoReq;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduInfoListResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduItemInfoListResponse;
import com.prafta.web.tbm.tbm01.mapper.Tbm01Mapper;
import com.prafta.web.tbm.tbm01.result.TbmEduInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduItemInfoResult;
import com.prafta.web.tbm.tbm01.service.Tbm01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Tbm01ServiceImpl implements Tbm01Service{
	private final Tbm01Mapper tbm01Mapper;
	private final FileService fileService;
    private final FileMapper fileMapper;
	
	public TbmEduInfoListResponse selectTbmEduInfo(TbmEduInfoListParam param) {
		TbmEduInfoListResponse response = null;
		
		List<TbmEduInfoResult> tbmEduInfoResultList = tbm01Mapper.selectTbmEduInfo(TbmEduInfoListQuery.from(param));
		
		List<TbmEduItemInfoResult> tbmEduItemInfoResult = tbm01Mapper.selectTbmEduItemInfo(TbmEduItemInfoListQuery.from(param));
		
		response = TbmEduInfoListResponse.builder()
										.tbmEduInfoResultList(tbmEduInfoResultList)
										.tbmEduItemInfoResultList(tbmEduItemInfoResult)
										.build();
		return response;
	}
	
	public TbmEduItemInfoListResponse selectTbmEduItemInfo(TbmEduItemInfoListParam param) {
		
		TbmEduItemInfoListResponse response = null;
		
		List<TbmEduItemInfoResult> tbmEduItemInfoList = tbm01Mapper.selectTbmEduItemInfo(TbmEduItemInfoListQuery.from(param));
		
		if(tbmEduItemInfoList!= null && tbmEduItemInfoList.size() > 0) {
			response = TbmEduItemInfoListResponse.builder()
						.tbmEduItemInfoList(tbmEduItemInfoList)
						.build();
		}
		
		return response;
	}
	
	@Transactional
	public void saveTbmEduInfos(TbmEduInfoParam param) {
		try {
			String mtrlCd = "";
			String fileMgmtCd = "";
			
			if(param.mtrlCd().isEmpty()) {
				mtrlCd = tbm01Mapper.selectMtrlCd(param.gvCmpnyCd());
			} else {
				mtrlCd = param.mtrlCd();
			}
			
			tbm01Mapper.mergeTbmEduInfo(TbmEduInfoCommand.from(param, mtrlCd));
			
			for(TbmEduItemInfoModel model : param.tbmEduItemInfoModelList()) {
				fileMgmtCd = model.fileMgmtCd();
				
				MultipartFile file = null;
				if (StringUtils.hasText(model.itemBase64())) {
					byte[] bytes = Base64.getDecoder().decode(model.itemBase64().trim());
					String fileName = StringUtils.hasText(model.itemOriginalFilename())
							? model.itemOriginalFilename()
							: "upload.bin";
					file = new BytesMultipartFile("item", fileName, null, bytes);
				}
				
				if (file != null && !file.isEmpty()) {
					fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(param.gvCmpnyCd(), "003"));			// 002 : TBM 교육자료
					
					fileService.fileSave(FileInfoParam.from(
    					param.gvCmpnyCd()
    					, param.gvUserCd()
    					, ""							// TBM 교육자료는 회사 공통으로 생성
    					, "003"							// 위험성 평가
    					, fileMgmtCd
    					, file
					));
				}
				
				String mtrlItemCd;
				
				if(model.mtrlItemCd() == null) {
					mtrlItemCd = tbm01Mapper.selectMtrlItemCd(param.gvCmpnyCd());
				} else {
					mtrlItemCd = model.mtrlItemCd();
				}

				TbmEduItemInfoCommand command = TbmEduItemInfoCommand.from(model, param, mtrlItemCd, mtrlCd, fileMgmtCd);
				
				tbm01Mapper.mergeTbmEduItemInfo(command);
			}
		} catch (Exception e) {
			throw new ApiException(CommonErrorCode.COMMON_500_001);
		}
	}	
	
	public void deleteTbmEduItemInfo(TbmEduItemParam param) {
		
		if(param != null && param.tbmEduItemModelList().size() > 0) {
			for(TbmEduItemModel model : param.tbmEduItemModelList()) {
				
				tbm01Mapper.deleteTbmEduItemInfo(TbmEduItemCommand.from(model));
			}
		}
	}
	
	public void saveTbmEdu(TbmEduMtrlInfoParam param) {

		if(param != null && param.tbmEduMtrlModelList().size() > 0) {
			for(TbmEduMtrlModel model : param.tbmEduMtrlModelList()) {
				
				tbm01Mapper.mergeTbmEduInfo(TbmEduInfoCommand.from(model));
			}
		}
	}
	
	public void deleteTbmEdu(TbmEduMtrlInfoParam param) {
		
		if(param != null && param.tbmEduMtrlModelList().size() > 0) {
			for(TbmEduMtrlModel model : param.tbmEduMtrlModelList()) {
				
				tbm01Mapper.deleteTbmEduInfo(TbmEduInfoCommand.from(model));
			}
		}
	}
}