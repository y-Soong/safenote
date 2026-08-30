package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.EvidenceAttendeeResult;
import com.prafta.web.tbm.tbm04.result.EvidenceMtrlResult;
import com.prafta.web.tbm.tbm04.result.EvidenceRiskResult;
import com.prafta.web.tbm.tbm04.result.EvidenceSessionDetailResult;

import lombok.Builder;
import lombok.Value;

/**
 * TBM 증빙 교육일지(건별) 상세 응답 (POST /webApi/tbm04/evidence-session-details).
 *
 * <p>요청 sessionCds 중 인가 통과분만 sessionList 에 담긴다(미인가 건 조용히 제외).
 * 부속 목록(참석자/위험성평가/교육자료)은 sessionCd 로 화면이 그룹핑한다.
 */
@Value
@Builder
public class EvidenceSessionDetailResponse {
    List<EvidenceSessionDetailResult> sessionList;
    List<EvidenceAttendeeResult> attendeeList;
    List<EvidenceRiskResult> riskList;
    List<EvidenceMtrlResult> mtrlList;
}
