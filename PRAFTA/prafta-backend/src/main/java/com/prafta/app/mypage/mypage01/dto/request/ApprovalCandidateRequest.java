package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * prafta-app-010-05: 결재자 후보 검색 요청 (선택 필터).
 *
 * <p>회사/사업장 스코프와 본인 제외는 service 가 JWT 로 강제한다. 여기서는 선택 필터만 받는다.
 */
@Data
public class ApprovalCandidateRequest {
    private String userNm; // 이름 부분검색(선택)
    private String nodeCd; // 부서 필터(선택)
}
