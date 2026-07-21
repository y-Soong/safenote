package com.prafta.common.cmm.leave.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;

/**
 * {@link LeaveConversionPolicyService} 구현 — 480분(8시간) 고정 (2026-07-21 전환).
 *
 * <p>tb_leave_conversion_policy 조회·저장·이력 로직은 폐기했다(경위는 인터페이스 javadoc).
 * 테이블은 dormant 상태로 유지하며 본 구현은 어떤 상황에서도 480을 반환한다 —
 * R2(원장 무반올림)·R3(하한)·R4(캡)와의 정합은 480이 30분 단위 유한소수(0.0625)이자
 * 120의 배수(반반차 120분·반차 240분 정렬)라는 성질로 보장된다.
 */
@Service
public class LeaveConversionPolicyServiceImpl implements LeaveConversionPolicyService {

    @Override
    public int selectConversionMinutes(String cmpnyCd, String workYmd) {
        // 파라미터는 시그니처 호환용으로만 유지 (설정형 복원 대비) — 항상 480 고정.
        return DEFAULT_CONV_MINUTES;
    }
}
