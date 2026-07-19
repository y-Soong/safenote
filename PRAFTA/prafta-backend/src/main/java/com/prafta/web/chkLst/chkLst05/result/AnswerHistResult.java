package com.prafta.web.chkLst.chkLst05.result;

/**
 * 점검 응답 덮어쓰기 이력 조회 결과(PRAFTA-SUBCON-T6-AUDIT-03).
 *
 * <p>MyBatis record 매핑은 SELECT 컬럼 순서 = 필드 순서다(어긋나면 값이 밀림 — feedback 승계).
 * 아래 필드 순서는 {@code ChkLst05Mapper.xml selectAnswerHistList} 의 SELECT 순서와 정확히 일치한다.
 *
 * <p>수행 회사/수행자는 <b>스냅샷 컬럼</b>이다(타사 USER_CD 조인 금지 — 동명이인/PII 오염 방지, plan §1-2).
 */
public record AnswerHistResult(
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
	, String inspectAnswerType		// 점검답변타입 스냅샷(Y 양호 / N 불량)
	, String answerDesc				// 답변 상세 스냅샷
	, String photoYn				// 사진 존재여부(Y/N)
	, String fileMgmtCd				// 사진 파일코드(+확장자) — 서빙용
	, String filePath				// 사진 경로 — 서빙용
	, String performCmpnyCd			// 수행 회사코드(인접 1차 relabel 스냅샷)
	, String performCmpnyNm			// 수행 회사명
	, String performUserCd			// 수행자 USER_CD 스냅샷(표시 전용)
	, String performUserNm			// 수행자 성명 스냅샷
) {
}
