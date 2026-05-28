package com.prafta.common.cmm.leave.service;

/**
 * tb_user_leave_grant의 STATUS 단일 SoT 동기화 + 만료 배치 + USED_DAYS 재계산 서비스.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>STATUS가 단일 진실(Single Source of Truth). EXPIRE_YN/DEL_YN은 STATUS 변경 시 단방향으로 함께 UPDATE된다.</li>
 *   <li>채널 비종속(channel-agnostic). 호출처 hook 연결은 본 작업 범위 밖이며,
 *       Attd_09(연차 사용) / Baim_07(정책 변경) / 입사일 변경 화면 작업 시 본 서비스를 호출한다.</li>
 *   <li>기 발생 연차 사후 차감 금지(§8.5.8 절대 규칙). USED_DAYS는 GRANT_DAYS로 클램프된다.</li>
 *   <li>STATUS='CANCELED' 행은 recalcStatus가 건드리지 않는다 (early return).</li>
 * </ul>
 *
 * <p>전이 우선순위: CANCELED &gt; EXPIRED &gt; EXHAUSTED &gt; ACTIVE.
 */
public interface LeaveGrantStatusService {

    /**
     * 단일 부여건의 STATUS를 재평가하여 EXHAUSTED/EXPIRED/ACTIVE 중 하나로 전이.
     * EXPIRE_YN/DEL_YN 동기화 포함.
     *
     * <p>STATUS='CANCELED'인 행은 변경하지 않고 즉시 종료한다.
     *
     * @param cmpnyCd 회사 코드
     * @param grantId 부여 ID
     */
    void recalcStatus(String cmpnyCd, String grantId);

    /**
     * USED_DAYS를 tb_user_leave_use 합계로 재계산하고 STATUS 재평가까지 수행한다.
     *
     * <p>합계가 GRANT_DAYS를 초과하면 USED_DAYS는 GRANT_DAYS로 클램프된다(기 발생 연차 보호).
     *
     * @param cmpnyCd 회사 코드
     * @param grantId 부여 ID
     */
    void recalcUsedDays(String cmpnyCd, String grantId);

    /**
     * 만료 배치 진입점. 회사 무관, 시스템 시각 기준.
     * AVAIL_TO_DATE &lt; 오늘 AND STATUS='ACTIVE'인 행을 STATUS='EXPIRED'로 전이.
     *
     * @return 만료 처리된 행 수
     */
    int expireOverdueGrants();

    /**
     * 부여건 강제 취소 (RESET_ALL 케이스 등).
     * STATUS='CANCELED'로 전이하고 DEL_YN은 'N'을 유지(소프트 취소).
     *
     * @param cmpnyCd          회사 코드
     * @param grantId          부여 ID
     * @param reasonByUserCd   취소 수행자 사용자 코드 (감사 추적용 UPDATE_NO 기록)
     */
    void cancelGrant(String cmpnyCd, String grantId, String reasonByUserCd);
}
