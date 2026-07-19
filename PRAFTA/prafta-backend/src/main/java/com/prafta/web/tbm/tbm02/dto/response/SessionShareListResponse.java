package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.common.cmm.tbmshare.result.SessionShareRow;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동 회사 지정 현황 응답(PRAFTA-SUBCON-T5).
 *
 * <p>조회자가 직접 지정한 회사만 반환한다(개설사면 1차 회사, 체인 회사면 자기 재지정분).
 * 하위 재지정 회사는 개사 수(subCount)로만 노출한다(2차 이하 회사명 비노출 — 마스터 §1-3).
 * canManage 는 지정/해제 가능 구간(DRAFT/OPENED) 여부(서버 TBM_409_063 과 동일 기준).
 */
@Getter
@Builder
public class SessionShareListResponse {
	private String sessionCd;
	private boolean canManage;
	private List<SessionShareRow> shareList;
}
