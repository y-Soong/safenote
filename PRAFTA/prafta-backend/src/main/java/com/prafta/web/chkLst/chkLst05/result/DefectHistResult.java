package com.prafta.web.chkLst.chkLst05.result;

/**
 * 불량조치 덮어쓰기 이력 조회 결과(PRAFTA-SUBCON-T6-AUDIT-03).
 *
 * <p>MyBatis record 매핑은 SELECT 컬럼 순서 = 필드 순서다. 조치자(ACTION_*) 컬럼은 화면이 응답 이력과 동일
 * 템플릿으로 표시하도록 {@code performCmpnyNm/performUserNm/performUserCd} 로 alias 하여 노출한다(스냅샷 컬럼).
 */
public record DefectHistResult(
	String siteCd					// 사업장코드
	, String siteNm					// 사업장명
	, String chkLstType				// 점검구분[COM001]
	, String chkLstTypeNm			// 점검구분명
	, String chkptCd				// 점검대상코드
	, String chkptNm				// 점검대상명칭
	, String inspectItemCd			// 점검문항코드
	, String inspectItemSubj		// 점검문항명
	, String workDate				// 점검일자(YYYYMMDD)
	, String chgDtime				// 이 write 발생 시각(YYYY-MM-DD HH:mm)
	, String chgType				// 변경유형 코드(01 신규 / 02 덮어쓰기)
	, String chgTypeNm				// 변경유형명
	, String actionDesc				// 조치 상세 스냅샷
	, String photoYn				// 사진 존재여부(Y/N)
	, String fileMgmtCd				// 조치 사진 파일코드(+확장자) — 서빙용
	, String filePath				// 조치 사진 경로 — 서빙용
	, String performCmpnyCd			// 조치 회사코드(ACTION_CMPNY_CD relabel 스냅샷)
	, String performCmpnyNm			// 조치 회사명
	, String performUserCd			// 조치자 USER_CD 스냅샷(표시 전용)
	, String performUserNm			// 조치자 성명 스냅샷
) {
}
