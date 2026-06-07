package com.prafta.web.chkLst.chkLst04.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.chkLst.ChkLstErrorCode;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst04ServiceImpl implements ChkLst04Service {

	private final ChkLst04Mapper chkLst04Mapper;

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
	public void saveDefectAction(DefectActionParam param) {

		DefectActionCommand command = DefectActionCommand.from(param);

		log.info("점검 불량 조치 저장 진입 - siteCd={}, chkptCd={}, inspectItemCd={}, workDate={}",
				command.siteCd(), command.chkptCd(), command.inspectItemCd(), command.workDate());

		// IDOR 가드: 대상 불량행이 호출자 권한 사업장에 실재하는 불량('N')인지 서버 검증.
		int inScope = chkLst04Mapper.countDefectInScope(command);
		if (inScope <= 0) {
			log.error("점검 불량 조치 저장 거부 - 권한 외 사업장이거나 미존재 불량. siteCd={}, chkptCd={}, inspectItemCd={}, workDate={}",
					command.siteCd(), command.chkptCd(), command.inspectItemCd(), command.workDate());
			throw new ApiException(ChkLstErrorCode.CHKLST_403_002);
		}

		chkLst04Mapper.saveDefectAction(command);

		log.info("점검 불량 조치 저장 완료 - siteCd={}, chkptCd={}, inspectItemCd={}, workDate={}",
				command.siteCd(), command.chkptCd(), command.inspectItemCd(), command.workDate());
	}
}
