package com.prafta.common.cmm.leave.service;

/**
 * 연차 시간차 "1일 환산시간"(분모) 서비스 (연차 시간차 환산 개편 LC-02 → 2026-07-21 480 고정 전환).
 *
 * <p>2026-07-21 사용자 결정: 환산시간을 <b>480분(8시간) 고정</b>으로 전환한다.
 * <ul>
 *   <li>회사별 설정(tb_leave_conversion_policy)·적용일 이력(F4 effective-dating)·Baim_07
 *       설정 화면은 폐기 — 유한소수 검증(R2) 때문에 현실적 대안값(420/360/450 등)이
 *       애초에 입력 불가라 설정의 실용성이 없었고, 설정 변경이 잔여 표기 변동·혼합 이력·
 *       최소단위 미만 끝수를 만드는 원천이었다.</li>
 *   <li>8시간 미만 스케줄 회사는 하한 가드(R3 — 그날 소정근로분 D 기준 0.25/0.5/1.0)가,
 *       초과 스케줄은 캡(R4 — 1.0)이 보호하므로 고정으로도 정합(잔여 왜곡은 근로자 유리 방향만).</li>
 *   <li>tb_leave_conversion_policy 테이블은 드랍하지 않고 방치(dormant) — 기존 행 정리는
 *       prafta-leave-conv-3-fix480-cleanup.sql 참조. 설정형으로 되돌릴 경우 git 이력 복원.</li>
 * </ul>
 *
 * <p>출처: 작업지시서_연차-시간차-환산-개편 T0·F4(원설계) / 정책서 attd/08-leave.md §8.5.9
 */
public interface LeaveConversionPolicyService {

    /** 1일 환산시간(분) — 480분(8시간) 전사 고정 (2026-07-21 결정). */
    int DEFAULT_CONV_MINUTES = 480;

    /**
     * 유효 환산시간(분) 조회 — 항상 480 고정.
     *
     * <p>시간차 차감(LC-03)·재정산(LC-05)·표기(LC-07)의 분모 단일 출처. 호출부 시그니처
     * 호환을 위해 파라미터는 유지하되 무시한다(설정형 복원 대비).
     */
    int selectConversionMinutes(String cmpnyCd, String workYmd);
}
