package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import com.prafta.common.cmm.leave.vo.RemnantReportRowVO;

import lombok.Builder;
import lombok.Value;

/**
 * PC-07(D9-③·N2): 소멸 임박 짜투리 리포트 응답.
 * GET /attd09/leave-dashboard/remnant-report.
 *
 * <p>FE(Attd_09 — UI-B): 짜투리 보전 OFF 회사만 리포트 섹션 노출. 문구는
 * "미사용분은 연차미사용수당 정산 대상"("근로자 손해" 표현 금지 — D9). 절사 끝수는
 * {@code roundingDust} 배지로 구분(임계 0.001 — §5-④).
 */
@Value
@Builder
public class RemnantReportResponse {

    /** 짜투리 보전 옵션 ON 여부 — FE 섹션 분기. */
    boolean remnantPolicyOn;

    /** 0 &lt; 잔여 &lt; 본인 최소단위 요금인 사용자 목록(최근접 소멸일 오름차순). */
    List<RemnantReportRowVO> rows;
}
