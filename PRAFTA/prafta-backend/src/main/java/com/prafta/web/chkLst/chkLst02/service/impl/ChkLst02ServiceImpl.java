package com.prafta.web.chkLst.chkLst02.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.web.chkLst.chkLst02.application.command.ChkptInspectItemCommand;
import com.prafta.web.chkLst.chkLst02.application.model.ChkptInspectItemModel;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemHistListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemHistListQuery;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemListQuery;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemHistListResponse;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemListResponse;
import com.prafta.web.chkLst.chkLst02.mapper.ChkLst02Mapper;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemHistResult;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemResult;
import com.prafta.web.chkLst.chkLst02.service.ChkLst02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst02ServiceImpl implements ChkLst02Service{
	private final ChkLst02Mapper chkLst02Mapper;

	/** 변경유형 코드: TB_CHKPT_INSPECT_ITEM_HIST.CHG_TYPE */
	private static final String CHG_TYPE_REGIST = "01";	// 등록
	private static final String CHG_TYPE_MODIFY = "02";	// 수정(명칭/시행일/정렬순서)
	private static final String CHG_TYPE_DISABLE = "03";	// 사용중지(Y→N)
	private static final String CHG_TYPE_ENABLE = "04";	// 재사용(N→Y)

	public ChkptInspectItemListResponse selectChkptInspectItemList(ChkptInspectItemListParam param) {

		ChkptInspectItemListResponse response = null;

		List<ChkptInspectItemResult> chkptInspectItemResultList = chkLst02Mapper.selectChkptInspectItemList(ChkptInspectItemListQuery.from(param));

		if(chkptInspectItemResultList != null && chkptInspectItemResultList.size() > 0) {
			response = ChkptInspectItemListResponse.builder().chkptInspectItemResultList(chkptInspectItemResultList).build();
		}

		return response;
	}

	@Transactional
	public void updateChkptInspectItemList(ChkptInspectItemParam param) {
		for(ChkptInspectItemModel model : param.chkptInspectItemModelList()) {
			ChkptInspectItemCommand command = ChkptInspectItemCommand.from(model);

			// 신규 문항: 이력 적재를 위해 문항코드를 선채번한 뒤 저장 + 등록 이력
			if (command.inspectItemCd() == null || command.inspectItemCd().isBlank()) {
				String newItemCd = chkLst02Mapper.selectNextInspectItemCd(command);
				command = withInspectItemCd(command, newItemCd);
				chkLst02Mapper.mergeChkptInspectItemList(command);
				chkLst02Mapper.insertChkptInspectItemHist(command, CHG_TYPE_REGIST);
				continue;
			}

			ChkptInspectItemResult existing = chkLst02Mapper.selectChkptInspectItem(command);

			// 코드가 지정됐지만 미존재(방어): 등록으로 처리
			if (existing == null) {
				chkLst02Mapper.mergeChkptInspectItemList(command);
				chkLst02Mapper.insertChkptInspectItemHist(command, CHG_TYPE_REGIST);
				continue;
			}

			// 변경 없는 행은 저장/이력 모두 생략(감사 이력 오염 방지)
			String newStrDate = normalizeYmd(command.strDate());
			String newUseYn = (command.useYn() == null || command.useYn().isBlank()) ? "Y" : command.useYn();
			boolean useYnChanged = !Objects.equals(existing.useYn(), newUseYn);
			boolean changed = useYnChanged
					|| !Objects.equals(existing.inspectItemSubj(), command.inspectItemSubj())
					|| !Objects.equals(existing.strDate(), newStrDate)
					|| existing.sortIdx() != command.sortIdx();
			if (!changed) {
				continue;
			}

			chkLst02Mapper.mergeChkptInspectItemList(command);

			String chgType = CHG_TYPE_MODIFY;
			if (useYnChanged) {
				chgType = "N".equals(newUseYn) ? CHG_TYPE_DISABLE : CHG_TYPE_ENABLE;
			}
			chkLst02Mapper.insertChkptInspectItemHist(command, chgType);
		}
	}

	@Transactional
	public void deleteChkptInspectItemList(ChkptInspectItemParam param) {
		for(ChkptInspectItemModel model : param.chkptInspectItemModelList()) {
			ChkptInspectItemCommand command = ChkptInspectItemCommand.from(model);

			ChkptInspectItemResult existing = chkLst02Mapper.selectChkptInspectItem(command);

			chkLst02Mapper.updateChkptInspectItemList(command);

			// 이미 미사용이던 행은 이력 생략
			if (existing != null && !"N".equals(existing.useYn())) {
				chkLst02Mapper.insertChkptInspectItemHist(command, CHG_TYPE_DISABLE);
			}
		}
	}

	public ChkptInspectItemHistListResponse selectChkptInspectItemHistList(ChkptInspectItemHistListParam param) {

		List<ChkptInspectItemHistResult> chkptInspectItemHistResultList = chkLst02Mapper.selectChkptInspectItemHistList(ChkptInspectItemHistListQuery.from(param));

		return ChkptInspectItemHistListResponse.builder()
				.chkptInspectItemHistResultList(chkptInspectItemHistResultList)
				.build();
	}

	/** 시행일 비교용 정규화: 'YYYY-MM-DD' → 'YYYYMMDD' (merge SQL 의 REPLACE/SUBSTRING 과 동일 규칙) */
	private String normalizeYmd(String value) {
		if (value == null) return null;
		String compact = value.replace("-", "");
		return compact.length() > 8 ? compact.substring(0, 8) : compact;
	}

	/** record 인 command 에 선채번한 문항코드를 채운 사본 생성 */
	private ChkptInspectItemCommand withInspectItemCd(ChkptInspectItemCommand command, String inspectItemCd) {
		return new ChkptInspectItemCommand(
			command.cmpnyCd()
			, command.siteCd()
			, command.chkLstType()
			, inspectItemCd
			, command.inspectItemSubj()
			, command.sortIdx()
			, command.strDate()
			, command.useYn()
			, command.gvCmpnyCd()
			, command.gvUserCd()
		);
	}
}
