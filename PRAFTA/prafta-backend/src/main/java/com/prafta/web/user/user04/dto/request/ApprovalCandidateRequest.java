package com.prafta.web.user.user04.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결재자 후보(사용자 리스트) 조회 요청 (prafta-019-D, prafta-020).
 * 사업장 스코프는 토큰 사업장으로 강제(본인 소속 사업장 한정). 소속부서(nodeCd)·사용자명은 필터.
 */
@Getter
@Setter
@NoArgsConstructor
public class ApprovalCandidateRequest {

    @FieldLabel("사용자명")
    @Size(max = 50)
    private String userNm;

    /** 소속부서 노드 필터 (선택, 본인 사업장 내). */
    @FieldLabel("소속부서")
    @Size(max = 50)
    private String nodeCd;
}
