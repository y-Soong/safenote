package com.prafta.web.chkLst.chkLst04.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.application.param.DefectActionParam;

/**
 * 불량조치 저장 커맨드(chkLst04).
 *
 * <p>PRAFTA-SUBCON-T6-06: 조치자 성명 스냅샷(ACTION_USER_NM) — 타 티어에서 USER_CD 조인이 불가하므로 저장 시점 값을 박는다.
 * <p>[정책 변경] 후행 덮어쓰기(last-writer-wins) 전환으로 소유 판정용 actionKey 는 폐기됐다(무조건 UPSERT).
 * <p>{@code fileMgmtCd} : 조치 첨부사진 파일코드(미첨부면 빈 문자열) — 서비스가 사진 저장 후 채워 넣는다.
 */
public record DefectActionCommand(
	String siteCd
	, String chkptCd
	, String inspectItemCd
	, String workDate
	, String actionDesc
	, String fileMgmtCd
	, String gvCmpnyCd
	, String gvUserCd
	, String gvUserNm
){
	public static DefectActionCommand from(DefectActionParam param, String fileMgmtCd) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new DefectActionCommand(
			param.siteCd()
			, param.chkptCd()
			, param.inspectItemCd()
			, param.workDate()
			, param.actionDesc()
			, fileMgmtCd == null ? "" : fileMgmtCd
			, param.gvCmpnyCd()
			, param.gvUserCd()
			, param.gvUserNm()
		);
	}
}
