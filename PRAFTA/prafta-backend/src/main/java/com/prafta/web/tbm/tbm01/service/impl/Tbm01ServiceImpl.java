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
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.FileUrlSigner;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm01.application.command.TbmEduInfoCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemInfoCommand;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduMtrlModel;
import com.prafta.web.tbm.tbm01.application.param.TbmEduDetailParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduMtrlInfoParam;
import com.prafta.web.tbm.tbm01.application.query.TbmEduDetailQuery;
import com.prafta.web.tbm.tbm01.application.query.TbmEduInfoListQuery;
import com.prafta.web.tbm.tbm01.application.query.TbmEduItemInfoListQuery;
//import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoReq;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduDetailResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduInfoListResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduItemInfoListResponse;
import com.prafta.web.tbm.tbm01.mapper.Tbm01Mapper;
import com.prafta.web.tbm.tbm01.result.TbmEduInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduItemInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduUsedSessionResult;
import com.prafta.web.tbm.tbm01.service.Tbm01Service;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Tbm01ServiceImpl implements Tbm01Service{
	private final Tbm01Mapper tbm01Mapper;
	private final FileService fileService;
    private final FileMapper fileMapper;
    private final FileUrlSigner fileUrlSigner;   // 파일 서빙 서명 URL 발급(공통 인프라)

	public TbmEduInfoListResponse selectTbmEduInfo(TbmEduInfoListParam param) {
		// prafta-033-A: 999999(권한 미부여)는 콘텐츠 화면 진입 차단(서버에서도 거부)
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 콘텐츠 목록 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}

		TbmEduInfoListResponse response = null;

		List<TbmEduInfoResult> tbmEduInfoResultList = tbm01Mapper.selectTbmEduInfo(TbmEduInfoListQuery.from(param));

		List<TbmEduItemInfoResult> tbmEduItemInfoResult = tbm01Mapper.selectTbmEduItemInfo(TbmEduItemInfoListQuery.from(param));

		// 서명 URL 전환: 파일형 항목에 서명 절대 URL(fileUrl) 채움.
		applyFileUrls(tbmEduItemInfoResult, param.gvCmpnyCd());

		response = TbmEduInfoListResponse.builder()
										.tbmEduInfoResultList(tbmEduInfoResultList)
										.tbmEduItemInfoResultList(tbmEduItemInfoResult)
										.build();
		return response;
	}

	/**
	 * prafta-033-A: W-03 콘텐츠 상세(묶음+세부항목+사용 TBM 이력).
	 * 999999 진입 차단 + 스코프 격리(사업장 관리자는 자기 사업장+회사공통만).
	 */
	public TbmEduDetailResponse selectTbmEduDetail(TbmEduDetailParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 콘텐츠 상세 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}

		TbmEduDetailQuery query = TbmEduDetailQuery.from(param);

		TbmEduInfoResult master = tbm01Mapper.selectTbmEduDetail(query);
		if (master == null) {
			throw new ApiException(CommonErrorCode.COMMON_400_401);
		}

		// 스코프 격리: 회사 전체 권한(master/safe)이 아니면 회사공통 또는 자기 사업장만 열람 가능
		if (!AuthRoleUtils.isCompanyWide(param.gvAuthCd())) {
			boolean isCommon = master.siteCd() == null || master.siteCd().isEmpty();
			boolean isOwnSite = param.gvSiteCd() != null && param.gvSiteCd().equals(master.siteCd());
			if (!isCommon && !isOwnSite) {
				log.warn("TBM 콘텐츠 상세 스코프 위반 - authCd={}, ownSite={}, targetSite={}",
						param.gvAuthCd(), param.gvSiteCd(), master.siteCd());
				throw new ApiException(TbmErrorCode.TBM_403_003);
			}
		}

		List<TbmEduItemInfoResult> items = tbm01Mapper.selectTbmEduDetailItems(query);
		List<TbmEduUsedSessionResult> usedSessions = tbm01Mapper.selectTbmEduUsedSessions(query);

		// 서명 URL 전환: 파일형 항목에 서명 절대 URL(fileUrl) 채움.
		applyFileUrls(items, param.gvCmpnyCd());

		return TbmEduDetailResponse.builder()
				.tbmEduInfo(master)
				.tbmEduItemInfoList(items != null ? items : Collections.emptyList())
				.usedSessionList(usedSessions != null ? usedSessions : Collections.emptyList())
				.build();
	}
	
	public TbmEduItemInfoListResponse selectTbmEduItemInfo(TbmEduItemInfoListParam param) {
		
		TbmEduItemInfoListResponse response = null;
		
		List<TbmEduItemInfoResult> tbmEduItemInfoList = tbm01Mapper.selectTbmEduItemInfo(TbmEduItemInfoListQuery.from(param));

		// 서명 URL 전환: 파일형 항목에 서명 절대 URL(fileUrl) 채움.
		applyFileUrls(tbmEduItemInfoList, param.gvCmpnyCd());

		if(tbmEduItemInfoList!= null && tbmEduItemInfoList.size() > 0) {
			response = TbmEduItemInfoListResponse.builder()
						.tbmEduItemInfoList(tbmEduItemInfoList)
						.build();
		}
		
		return response;
	}
	
	@Transactional
	public void saveTbmEduInfos(TbmEduInfoParam param) {
		// prafta-033-A: 999999(권한 미부여)는 콘텐츠 저장 차단
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 콘텐츠 저장 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}

		// prafta-033-A: 회사공통(SITE_CD 비어있음) 콘텐츠 저장은 master/safe 만 허용
		boolean isCommonSave = (param.siteCd() == null || param.siteCd().isEmpty());
		if (isCommonSave && !AuthRoleUtils.canManageCommon(param.gvAuthCd())) {
			log.warn("TBM 회사공통 콘텐츠 저장 권한 없음 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_002);
		}

		// 사업장 콘텐츠 저장: 회사 전체 권한이 아니면 자기 사업장으로만 저장 가능(스코프 격리)
		if (!isCommonSave && !AuthRoleUtils.isCompanyWide(param.gvAuthCd())) {
			if (param.gvSiteCd() == null || !param.gvSiteCd().equals(param.siteCd())) {
				log.warn("TBM 타 사업장 콘텐츠 저장 시도 - authCd={}, ownSite={}, targetSite={}",
						param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
				throw new ApiException(TbmErrorCode.TBM_403_003);
			}
		}

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
				// prafta-033-A: 세부항목 타입 allow-list 검증 (01 이미지 / 02 동영상 / 03 유튜브URL / 04 PDF)
				if (!isAllowedItemType(model.mtrlItemType())) {
					log.warn("TBM 세부항목 타입 부적합 - mtrlItemType={}", model.mtrlItemType());
					throw new ApiException(CommonErrorCode.COMMON_400_002);
				}

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
		} catch (ApiException ae) {
			// 권한/검증 등 의도된 예외는 원본 그대로 전파
			throw ae;
		} catch (Exception e) {
			log.error("TBM 콘텐츠 저장 중 오류", e);
			throw new ApiException(CommonErrorCode.COMMON_500_001);
		}
	}

	/**
	 * 서명 URL 전환: 세부항목 리스트의 파일형 항목에 서명 절대 URL(fileUrl)을 채운다.
	 * <p>relPath = FILE_PATH + '/' + FILE_MGMT_CD + FILE_EXT(기존 프론트 조립 규칙 정합). 파일 없으면 미설정(NULL).
	 */
	private void applyFileUrls(List<TbmEduItemInfoResult> items, String cmpnyCd) {
		if (items == null) {
			return;
		}
		for (TbmEduItemInfoResult it : items) {
			if (StringUtils.hasText(it.getFilePath()) && StringUtils.hasText(it.getFileMgmtCd())) {
				String relPath = it.getFilePath() + "/" + it.getFileMgmtCd()
						+ (it.getFileExt() != null ? it.getFileExt() : "");
				it.setFileUrl(fileUrlSigner.sign(relPath, cmpnyCd));
			}
		}
	}

	/** prafta-033-A: 세부항목(미디어) 타입 allow-list. SYS018: 01/02/03/04 */
	private boolean isAllowedItemType(String mtrlItemType) {
		return "01".equals(mtrlItemType)
			|| "02".equals(mtrlItemType)
			|| "03".equals(mtrlItemType)
			|| "04".equals(mtrlItemType);
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