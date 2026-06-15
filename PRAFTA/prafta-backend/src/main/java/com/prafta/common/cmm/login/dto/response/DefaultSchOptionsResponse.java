package com.prafta.common.cmm.login.dto.response;

import java.util.List;

import com.prafta.common.cmm.sch.vo.SchOptionVO;

/**
 * PRAFTA-COM-008-E-8 — 로그인 게이트의 기본 근무타입 선택지 응답.
 *
 * <p>scope=DEFAULT_SCH 임시 토큰으로 조회한 사업장 활성 근무타입 목록. 빈 목록 가능(200 + 빈 배열).
 */
public record DefaultSchOptionsResponse(
        List<SchOptionVO> schedules
) {
}
