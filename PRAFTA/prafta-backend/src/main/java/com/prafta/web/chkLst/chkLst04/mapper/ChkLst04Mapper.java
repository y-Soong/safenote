package com.prafta.web.chkLst.chkLst04.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.chkLst.chkLst04.application.command.DefectActionCommand;
import com.prafta.web.chkLst.chkLst04.application.query.ChkptTargetListQuery;
import com.prafta.web.chkLst.chkLst04.application.query.DefectListQuery;
import com.prafta.web.chkLst.chkLst04.application.query.InspectItemListQuery;
import com.prafta.web.chkLst.chkLst04.result.ChkptTargetResult;
import com.prafta.web.chkLst.chkLst04.result.DefectResult;
import com.prafta.web.chkLst.chkLst04.result.InspectItemResult;

@Mapper
public interface ChkLst04Mapper {

	// 불량(INSPECT_ANSWER_TYPE='N') 목록 조회 + 조치 LEFT JOIN
	List<DefectResult> selectDefectList(DefectListQuery query);

	// 점검대상 검색팝업
	List<ChkptTargetResult> selectChkptTargetList(ChkptTargetListQuery query);

	// 점검문항 검색팝업
	List<InspectItemResult> selectInspectItemList(InspectItemListQuery query);

	// 조치 upsert (INSERT ... ON DUPLICATE KEY UPDATE)
	void saveDefectAction(DefectActionCommand command);

	// IDOR 가드: 조치 대상 불량행이 호출자 권한 사업장에 실재하는 불량('N')인지 검증
	int countDefectInScope(DefectActionCommand command);
}
