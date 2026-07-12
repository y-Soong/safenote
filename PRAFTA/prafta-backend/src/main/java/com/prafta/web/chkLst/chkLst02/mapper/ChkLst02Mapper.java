package com.prafta.web.chkLst.chkLst02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.chkLst.chkLst02.application.command.ChkptInspectItemCommand;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemHistListQuery;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemListQuery;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemHistResult;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemResult;

@Mapper
public interface ChkLst02Mapper {
	List<ChkptInspectItemResult> selectChkptInspectItemList(ChkptInspectItemListQuery query);

	void mergeChkptInspectItemList(ChkptInspectItemCommand command);

	void updateChkptInspectItemList(ChkptInspectItemCommand command);

	/** 문항 단건 조회(변경이력 diff 판정용) */
	ChkptInspectItemResult selectChkptInspectItem(ChkptInspectItemCommand command);

	/** 신규 문항코드 선채번(이력 적재를 위해 merge 이전에 코드 확정) */
	String selectNextInspectItemCd(ChkptInspectItemCommand command);

	/** 문항 변경이력 적재(변경 후 상태 스냅샷) */
	void insertChkptInspectItemHist(@Param("cmd") ChkptInspectItemCommand command, @Param("chgType") String chgType);

	/** 문항 변경이력 조회(문항관리 이력 팝업) */
	List<ChkptInspectItemHistResult> selectChkptInspectItemHistList(ChkptInspectItemHistListQuery query);
}
