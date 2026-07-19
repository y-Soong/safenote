package com.prafta.web.chkLst.chkLst04.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.chkLst.ChkLstErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.application.command.DefectActionCommand;
import com.prafta.web.chkLst.chkLst04.application.param.ChkptTargetListParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectActionParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectListParam;
import com.prafta.web.chkLst.chkLst04.application.param.InspectItemListParam;
import com.prafta.web.chkLst.chkLst04.application.query.ChkptTargetListQuery;
import com.prafta.web.chkLst.chkLst04.application.query.DefectListQuery;
import com.prafta.web.chkLst.chkLst04.application.query.InspectItemListQuery;
import com.prafta.web.chkLst.chkLst04.dto.response.ChkptTargetListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.DefectListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.InspectItemListResponse;
import com.prafta.web.chkLst.chkLst04.mapper.ChkLst04Mapper;
import com.prafta.web.chkLst.chkLst04.result.ChkptTargetResult;
import com.prafta.web.chkLst.chkLst04.result.DefectResult;
import com.prafta.web.chkLst.chkLst04.result.InspectItemResult;
import com.prafta.web.chkLst.chkLst04.service.ChkLst04Service;
import com.prafta.web.subcon.subcon02.application.param.DefectActionPropagateParam;
import com.prafta.web.subcon.subcon02.service.ChkptResultHistRecorder;
import com.prafta.web.subcon.subcon02.service.InspectAnswerPropagationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst04ServiceImpl implements ChkLst04Service {

	/** [보안검토 M3] 조치 상세 길이 상한(타 테넌트로 전파되는 사용자 입력 — 무제한 입력 차단). */
	private static final int ACTION_DESC_MAX_LEN = 1000;

	/** 불량조치 사진 파일 유형(006: 점검조치사진 — SYS010, 점검 사진 001 과 구분). */
	private static final String FILE_TYPE_DEFECT_ACTION = "006";

	private final ChkLst04Mapper chkLst04Mapper;

	private final FileService fileService;
	private final FileMapper fileMapper;

	/** PRAFTA-SUBCON-T6-06: 불량조치 write-through 전파(연동 없으면 no-op). */
	private final InspectAnswerPropagationService inspectAnswerPropagationService;

	/** PRAFTA-SUBCON-T6-AUDIT-02: 불량조치 덮어쓰기 감사 이력 캡처(기점 티어 — W3). */
	private final ChkptResultHistRecorder chkptResultHistRecorder;

	@Override
	public DefectListResponse selectDefectList(DefectListParam param) {

		log.info("점검 불량 목록 조회 진입 - siteCd={}, chkLstType={}, actionStatus={}",
				param.siteCd(), param.chkLstType(), param.actionStatus());

		// 사업장 스코프/IDOR 는 쿼리 내 TB_USER_SITE_AUTH 조인으로 호출자 권한 사업장만 노출.
		List<DefectResult> defectResultList = chkLst04Mapper.selectDefectList(DefectListQuery.from(param));

		return DefectListResponse.builder()
				.defectResultList(defectResultList)
				.build();
	}

	@Override
	public ChkptTargetListResponse selectChkptTargetList(ChkptTargetListParam param) {

		List<ChkptTargetResult> chkptTargetResultList = chkLst04Mapper.selectChkptTargetList(ChkptTargetListQuery.from(param));

		return ChkptTargetListResponse.builder()
				.chkptTargetResultList(chkptTargetResultList)
				.build();
	}

	@Override
	public InspectItemListResponse selectInspectItemList(InspectItemListParam param) {

		List<InspectItemResult> inspectItemResultList = chkLst04Mapper.selectInspectItemList(InspectItemListQuery.from(param));

		return InspectItemListResponse.builder()
				.inspectItemResultList(inspectItemResultList)
				.build();
	}

	@Override
	@Transactional
	public void saveDefectAction(DefectActionParam param, MultipartFile file) {

		log.info("점검 불량 조치 저장 진입 - siteCd={}, chkptCd={}, inspectItemCd={}, workDate={}, 사진첨부={}",
				param.siteCd(), param.chkptCd(), param.inspectItemCd(), param.workDate(), file != null && !file.isEmpty());

		// [보안검토 M3] 조치 내역은 write-through 로 타 테넌트 행에 그대로 기록되므로 진입부에서 길이를 검증한다
		//   (전파 도중 컬럼 제약으로 터지면 원본 저장까지 롤백된다).
		validateActionDesc(param.actionDesc());

		// IDOR 가드: 대상 불량행이 호출자 권한 사업장에 실재하는 불량('N')인지 서버 검증(파일 저장 전에 수행 — 고아 파일 방지).
		int inScope = chkLst04Mapper.countDefectInScope(DefectActionCommand.from(param, ""));
		if (inScope <= 0) {
			log.error("점검 불량 조치 저장 거부 - 권한 외 사업장이거나 미존재 불량. siteCd={}, chkptCd={}, inspectItemCd={}, workDate={}",
					param.siteCd(), param.chkptCd(), param.inspectItemCd(), param.workDate());
			throw new ApiException(ChkLstErrorCode.CHKLST_403_002);
		}

		// 조치 사진 저장(첨부 시에만) — 점검 응답 사진과 동일 패턴(FileService.fileSave). 미첨부면 텍스트만.
		String fileMgmtCd = "";
		if (file != null && !file.isEmpty()) {
			fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(param.gvCmpnyCd(), FILE_TYPE_DEFECT_ACTION));
			fileService.fileSave(FileInfoParam.from(
					param.gvCmpnyCd()
					, param.gvUserCd()
					, param.siteCd()
					, FILE_TYPE_DEFECT_ACTION
					, fileMgmtCd
					, file));
		}

		DefectActionCommand command = DefectActionCommand.from(param, fileMgmtCd);

		// PRAFTA-SUBCON-T6-AUDIT-02(W3): write 직전 좌표 존재여부로 CHG_TYPE(신규/덮어쓰기)을 판정.
		boolean defectExisted = chkptResultHistRecorder.existsDefectAction(
				command.gvCmpnyCd(), command.siteCd(), command.chkptCd(), command.inspectItemCd(), command.workDate());

		// [정책 변경] 후행 덮어쓰기(last-writer-wins) — 선처리 우선/충돌 차단 없이 무조건 UPSERT.
		chkLst04Mapper.saveDefectAction(command);

		// PRAFTA-SUBCON-T6-AUDIT-02(W3): write 직후 방금 쓴 조치행을 HIST 로 append(트리거 주체=조치자 USER_CD).
		chkptResultHistRecorder.captureDefect(
				command.gvCmpnyCd(), command.siteCd(), command.chkptCd(), command.inspectItemCd(), command.workDate(),
				ChkptResultHistRecorder.chgType(defectExisted), command.gvUserCd());

		// 체인 전 티어의 대응 좌표에 조치 복제(내역 + 사진). 연동되지 않은 점검대상이면 매핑 부재로 no-op.
		inspectAnswerPropagationService.propagateDefectAction(new DefectActionPropagateParam(
				command.gvCmpnyCd()
				, command.siteCd()
				, command.chkptCd()
				, command.inspectItemCd()
				, command.workDate()
				, command.actionDesc()
				, command.fileMgmtCd()
				, command.gvUserCd()
				, command.gvUserNm()));

		log.info("점검 불량 조치 저장 완료 - siteCd={}, chkptCd={}, inspectItemCd={}, workDate={}",
				command.siteCd(), command.chkptCd(), command.inspectItemCd(), command.workDate());
	}

	/** [보안검토 M3] 조치 상세 검증 — 필수 + 길이 상한(타 테넌트로 전파되는 사용자 입력). */
	private void validateActionDesc(String actionDesc) {
		if (actionDesc == null || actionDesc.isBlank()) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		if (actionDesc.length() > ACTION_DESC_MAX_LEN) {
			log.warn("점검 불량 조치 저장 거부 - 조치 상세 길이 초과. len={}", actionDesc.length());
			throw new ApiException(ChkLstErrorCode.CHKLST_400_003);
		}
	}
}
