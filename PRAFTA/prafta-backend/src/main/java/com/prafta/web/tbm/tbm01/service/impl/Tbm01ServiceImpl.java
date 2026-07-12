package com.prafta.web.tbm.tbm01.service.impl;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import com.prafta.web.tbm.tbm01.application.command.TbmEduAiAnalyzeCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduInfoCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemInfoCommand;
import com.prafta.web.tbm.tbm01.application.model.TbmEduAiAnalyzeItemModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;
import com.prafta.web.tbm.tbm01.application.model.TbmEduMtrlModel;
import com.prafta.web.tbm.tbm01.application.param.TbmEduAiAnalyzeParam;
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
import com.prafta.web.tbm.tbmai01.service.TbmAi01Service;

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
    private final TbmAi01Service tbmAi01Service;  // 저장 후 AI 자동 큐잉(best-effort, 커밋 이후 트리거)

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

		// AI 자동 큐잉을 위해 확정 mtrlCd 를 try 밖으로 보존(신규는 채번값, 수정은 param 값).
		String mtrlCd = "";
		try {
			String fileMgmtCd = "";

			if(param.mtrlCd().isEmpty()) {
				mtrlCd = tbm01Mapper.selectMtrlCd(param.gvCmpnyCd());
			} else {
				// 회사 스코프(IDOR 방어): 수정 모드에 공급된 mtrlCd 가 자기 회사 소유인지 검증.
				// 미검증 시 타 회사 MTRL_CD 로 마스터(제목/내용) 및 세부항목을 UPSERT(덮어쓰기/추가) 가능.
				if (tbm01Mapper.countOwnedMtrl(param.mtrlCd(), param.gvCmpnyCd()) == 0) {
					log.warn("TBM 콘텐츠 저장 - 타 회사/미존재 mtrlCd 차단 - mtrlCd={}", param.mtrlCd());
					throw new ApiException(TbmErrorCode.TBM_404_040);
				}
				// T5-2: 이미 TBM 세션에서 사용(취소 외)된 교육자료는 수정 불가
				if (tbm01Mapper.selectTbmEduLockingSessionCnt(param.mtrlCd(), param.gvCmpnyCd()) > 0) {
					log.warn("TBM 콘텐츠 저장 - 사용 중 교육자료 수정 차단 - mtrlCd={}", param.mtrlCd());
					throw new ApiException(TbmErrorCode.TBM_409_055);
				}
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

				// [High] 항목 소유 검증(IDOR 방어): 기존 항목 수정(mtrlItemCd 제공) 시,
				// 서버에서 해당 항목의 부모 교육자료(MTRL_CD)를 도출해 현재 저장 중인 mtrlCd 와 일치하는지 확인.
				// null(타 회사/미존재)이거나 불일치면 거부. 미검증 시 타 회사 항목 행을 mergeTbmEduItemInfo(UPSERT)로 덮어쓸 수 있음.
				// 신규 항목(mtrlItemCd 빈값)은 스킵(정상 INSERT). 성능: 항목당 1회.
				if (StringUtils.hasText(model.mtrlItemCd())) {
					String ownerMtrlCd = tbm01Mapper.selectMtrlCdByItemCd(model.mtrlItemCd(), param.gvCmpnyCd());
					if (ownerMtrlCd == null || ownerMtrlCd.isEmpty() || !ownerMtrlCd.equals(mtrlCd)) {
						log.warn("TBM 세부항목 저장 - 타 회사/미존재/부모불일치 항목 차단 - mtrlItemCd={}, ownerMtrlCd={}, mtrlCd={}",
								model.mtrlItemCd(), ownerMtrlCd, mtrlCd);
						throw new ApiException(TbmErrorCode.TBM_404_040);
					}
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

		// 저장 성공 → 커밋 이후 AI 자동 큐잉(best-effort). 커밋 전 트리거 시 러너가 방금 저장한
		// 항목/AI_ANALYZE_YN 을 못 보므로 반드시 afterCommit 에서 실행한다.
		registerAiEnqueueAfterCommit(mtrlCd, param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd());
	}

	/**
	 * 트랜잭션 커밋 이후에 AI 자동 큐잉을 트리거한다(best-effort).
	 * <p>동기화가 활성이면 {@link TransactionSynchronization#afterCommit()} 로 등록하고,
	 *    비활성(트랜잭션 없음)이면 즉시 호출로 폴백한다. enqueueOnSave 자체가 예외를 삼키므로
	 *    저장 흐름에는 영향이 없다.
	 */
	private void registerAiEnqueueAfterCommit(String mtrlCd, String cmpnyCd, String userCd, String authCd) {
		if (!StringUtils.hasText(mtrlCd) || !StringUtils.hasText(cmpnyCd)) {
			return;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					tbmAi01Service.enqueueOnSave(mtrlCd, cmpnyCd, userCd, authCd);
				}
			});
		} else {
			tbmAi01Service.enqueueOnSave(mtrlCd, cmpnyCd, userCd, authCd);
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
			// T5-2: 삭제 대상 항목별로 소속 교육자료(MTRL_CD)를 서버 도출해 IDOR/사용 중 잠금 검증.
			// 동일 교육자료의 중복 카운트 조회를 줄이기 위해 distinct(Set)로 1회만 검사.
			Set<String> checkedMtrlCds = new HashSet<>();
			for(TbmEduItemModel model : param.tbmEduItemModelList()) {
				String parentMtrlCd = tbm01Mapper.selectMtrlCdByItemCd(model.mtrlItemCd(), param.gvCmpnyCd());
				// IDOR 방어: 타 회사/미존재 항목이면 도출 결과 없음
				if (parentMtrlCd == null || parentMtrlCd.isEmpty()) {
					log.warn("TBM 세부항목 삭제 - 타 회사/미존재 항목 차단 - mtrlItemCd={}", model.mtrlItemCd());
					throw new ApiException(TbmErrorCode.TBM_404_040);
				}
				// 사용 중(취소 외 세션 참조) 교육자료의 항목은 삭제 불가
				if (checkedMtrlCds.add(parentMtrlCd)
						&& tbm01Mapper.selectTbmEduLockingSessionCnt(parentMtrlCd, param.gvCmpnyCd()) > 0) {
					log.warn("TBM 세부항목 삭제 - 사용 중 교육자료 삭제 차단 - mtrlCd={}", parentMtrlCd);
					throw new ApiException(TbmErrorCode.TBM_409_055);
				}
			}

			for(TbmEduItemModel model : param.tbmEduItemModelList()) {

				tbm01Mapper.deleteTbmEduItemInfo(TbmEduItemCommand.from(model, param.gvCmpnyCd()));
			}
		}
	}
	
	public void saveTbmEdu(TbmEduMtrlInfoParam param) {

		if(param != null && param.tbmEduMtrlModelList().size() > 0) {
			for(TbmEduMtrlModel model : param.tbmEduMtrlModelList()) {

				// 회사 스코프(IDOR 방어): 수정 모드(mtrlCd 보유)에 공급된 mtrlCd 가 자기 회사 소유인지 검증.
				// 미검증 시 타 회사 MTRL_CD 로 마스터(제목/내용/사용여부)를 UPSERT(덮어쓰기) 가능.
				if (model.mtrlCd() != null && !model.mtrlCd().isEmpty()) {
					if (tbm01Mapper.countOwnedMtrl(model.mtrlCd(), model.gvCmpnyCd()) == 0) {
						log.warn("TBM 교육자료 그리드 저장 - 타 회사/미존재 mtrlCd 차단 - mtrlCd={}", model.mtrlCd());
						throw new ApiException(TbmErrorCode.TBM_404_040);
					}
					// T5-2: 그리드 인라인 저장(기존 자료 수정)도 사용 중(취소 외 세션 참조)이면 차단
					if (tbm01Mapper.selectTbmEduLockingSessionCnt(model.mtrlCd(), model.gvCmpnyCd()) > 0) {
						log.warn("TBM 교육자료 그리드 저장 - 사용 중 교육자료 수정 차단 - mtrlCd={}", model.mtrlCd());
						throw new ApiException(TbmErrorCode.TBM_409_055);
					}
				}

				tbm01Mapper.mergeTbmEduInfo(TbmEduInfoCommand.from(model));
			}
		}
	}

	public void deleteTbmEdu(TbmEduMtrlInfoParam param) {

		if(param != null && param.tbmEduMtrlModelList().size() > 0) {
			// T5-2: 그리드 인라인 삭제 경로에도 IDOR/사용 중 잠금 가드 적용(기존엔 둘 다 부재).
			// 동일 자료 중복 카운트 조회를 줄이기 위해 distinct(Set)로 1회만 검사.
			Set<String> checkedMtrlCds = new HashSet<>();
			for(TbmEduMtrlModel model : param.tbmEduMtrlModelList()) {
				if (model.mtrlCd() != null && !model.mtrlCd().isEmpty()
						&& checkedMtrlCds.add(model.mtrlCd())) {
					// 회사 스코프(IDOR 방어): 공급된 mtrlCd 가 자기 회사 소유인지 검증
					if (tbm01Mapper.countOwnedMtrl(model.mtrlCd(), model.gvCmpnyCd()) == 0) {
						log.warn("TBM 교육자료 그리드 삭제 - 타 회사/미존재 mtrlCd 차단 - mtrlCd={}", model.mtrlCd());
						throw new ApiException(TbmErrorCode.TBM_404_040);
					}
					// 사용 중(취소 외 세션 참조) 자료 삭제 차단(FK 위반/세션콘텐츠 고아 방지)
					if (tbm01Mapper.selectTbmEduLockingSessionCnt(model.mtrlCd(), model.gvCmpnyCd()) > 0) {
						log.warn("TBM 교육자료 그리드 삭제 - 사용 중 교육자료 삭제 차단 - mtrlCd={}", model.mtrlCd());
						throw new ApiException(TbmErrorCode.TBM_409_055);
					}
				}
			}

			for(TbmEduMtrlModel model : param.tbmEduMtrlModelList()) {

				tbm01Mapper.deleteTbmEduInfo(TbmEduInfoCommand.from(model));
			}
		}
	}

	/**
	 * 사용 중(잠긴) 교육자료의 세부항목 AI 분석 지정(AI_ANALYZE_YN)만 갱신.
	 * <p>교육자료는 두고두고 재사용되므로, TBM 세션에 사용되어 내용 수정이 잠긴 경우에도
	 * 나중에 AI 분석 대상으로 지정/해제할 수 있어야 한다. 따라서 이 경로만은 잠금(TBM_409_055)
	 * 검증을 생략한다. 대신 권한/회사 스코프(IDOR) 검증은 저장 경로와 동일하게 유지한다.
	 */
	@Transactional
	public void updateTbmEduItemAiAnalyze(TbmEduAiAnalyzeParam param) {
		// 권한 미부여(999999)는 차단
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM AI 분석 지정 저장 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}

		// 소속 교육자료 필수
		if (param.mtrlCd() == null || param.mtrlCd().isEmpty()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		// 회사 스코프(IDOR 방어): 공급된 mtrlCd 가 자기 회사 소유인지 검증
		if (tbm01Mapper.countOwnedMtrl(param.mtrlCd(), param.gvCmpnyCd()) == 0) {
			log.warn("TBM AI 분석 지정 저장 - 타 회사/미존재 mtrlCd 차단 - mtrlCd={}", param.mtrlCd());
			throw new ApiException(TbmErrorCode.TBM_404_040);
		}

		if (param.itemList() == null || param.itemList().isEmpty()) {
			return;
		}

		for (TbmEduAiAnalyzeItemModel model : param.itemList()) {
			// 항목 코드 필수(신규 미저장 행은 이 경로 대상 아님)
			if (model.mtrlItemCd() == null || model.mtrlItemCd().isEmpty()) {
				log.warn("TBM AI 분석 지정 저장 - 항목 코드 누락 행 스킵");
				continue;
			}

			// AI_ANALYZE_YN allow-list('Y'/'N')만 허용
			String aiYn = model.aiAnalyzeYn();
			if (!"Y".equals(aiYn) && !"N".equals(aiYn)) {
				log.warn("TBM AI 분석 지정 값 부적합 - aiAnalyzeYn={}", aiYn);
				throw new ApiException(CommonErrorCode.COMMON_400_002);
			}

			// 항목 소유/부모 매칭 검증(IDOR 방어): 항목의 부모 교육자료가 요청 mtrlCd 와 일치해야 함
			String ownerMtrlCd = tbm01Mapper.selectMtrlCdByItemCd(model.mtrlItemCd(), param.gvCmpnyCd());
			if (ownerMtrlCd == null || ownerMtrlCd.isEmpty() || !ownerMtrlCd.equals(param.mtrlCd())) {
				log.warn("TBM AI 분석 지정 저장 - 타 회사/미존재/부모불일치 항목 차단 - mtrlItemCd={}, ownerMtrlCd={}, mtrlCd={}",
						model.mtrlItemCd(), ownerMtrlCd, param.mtrlCd());
				throw new ApiException(TbmErrorCode.TBM_404_040);
			}

			tbm01Mapper.updateTbmEduItemAiAnalyze(
					TbmEduAiAnalyzeCommand.from(model, param.gvUserCd(), param.gvCmpnyCd()));
		}

		// AI 분석 지정(Y/N) 변경 반영 후 커밋 이후 자동 큐잉(신규 Y 지정 항목 재큐잉).
		registerAiEnqueueAfterCommit(param.mtrlCd(), param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd());
	}
}