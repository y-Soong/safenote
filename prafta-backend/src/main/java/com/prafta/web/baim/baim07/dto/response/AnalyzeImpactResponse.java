package com.prafta.web.baim.baim07.dto.response;

import com.prafta.common.cmm.leave.vo.AnalyzeImpactVO;

import lombok.Builder;
import lombok.Value;

/**
 * 정책 변경 영향 분석(화면 8) 응답.
 *
 * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.7
 *
 * <p>읽기 전용 시뮬레이션 결과(저장 없음, §9.9). {@code summary}/{@code diff}/{@code affectedEmployees}를
 * 그대로 화면에 매핑한다.
 */
@Value
@Builder
public class AnalyzeImpactResponse {

    AnalyzeImpactVO impact;
}
