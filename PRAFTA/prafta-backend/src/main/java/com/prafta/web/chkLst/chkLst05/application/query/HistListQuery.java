package com.prafta.web.chkLst.chkLst05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst05.application.param.HistListParam;

/**
 * 순회점검 이력 조회 쿼리(PRAFTA-SUBCON-T6-AUDIT-03).
 *
 * <p>{@code fileType} 은 사진 서빙 조인용 파일유형(SYS010)이다 — 응답 이력=001(일일점검),
 * 불량조치 이력=006(점검조치사진). 엔드포인트별로 서비스가 세팅한다.
 */
public record HistListQuery(
	String siteCd
	, String fromWorkDate
	, String toWorkDate
	, String chkptCd
	, String inspectItemCd
	, String fileType
	, String gvCmpnyCd
	, String gvUserCd
){
	/** 점검(일일점검) 첨부사진 파일유형(SYS010 '001'). */
	public static final String FILE_TYPE_INSPECT = "001";

	/** 조치 첨부사진 파일유형(SYS010 '006' 점검조치사진). */
	public static final String FILE_TYPE_DEFECT_ACTION = "006";

	public static HistListQuery from(HistListParam param, String fileType) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new HistListQuery(
			param.siteCd()
			, param.fromWorkDate()
			, param.toWorkDate()
			, param.chkptCd()
			, param.inspectItemCd()
			, fileType
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
