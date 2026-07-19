package com.prafta.web.chkLst.chkLst01.result;

/**
 * 점검대상 단건 원시행(PRAFTA-SUBCON-T6-03 — 미러 잠금 diff 판정용).
 *
 * <p>{@code linkSrcCmpnyCd} 가 NOT NULL 이면 연동 미러(수신) 행이다:
 * 명칭/비고/사용여부 변경은 403 으로 거부하고, 점검 담당자(MGMT_USER_CD) 지정만 허용한다.
 *
 * <p>record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ChkptRowRaw(
	String chkptNm
	, String chkptDesc
	, String useYn
	, String mgmtUserCd
	, String linkSrcCmpnyCd
){
}
