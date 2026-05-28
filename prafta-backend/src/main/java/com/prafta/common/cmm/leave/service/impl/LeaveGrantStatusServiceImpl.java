package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.leave.mapper.LeaveGrantStatusMapper;
import com.prafta.common.cmm.leave.service.LeaveGrantStatusService;
import com.prafta.common.cmm.leave.vo.LeaveGrantStatusVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveGrantStatusService} 구현체.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8 (STATUS ↔ EXPIRE_YN/DEL_YN 매핑표),
 * §8.5.6 (RESET_ALL 시 CANCELED).
 *
 * <p>STATUS 전이 규칙 (단방향, 우선순위 CANCELED &gt; EXPIRED &gt; EXHAUSTED &gt; ACTIVE):
 * <pre>
 *   ACTIVE     -> EXPIRE_YN='N', DEL_YN='N'
 *   EXHAUSTED  -> EXPIRE_YN='N', DEL_YN='N' (USED_DAYS >= GRANT_DAYS)
 *   EXPIRED    -> EXPIRE_YN='Y', DEL_YN='N' (AVAIL_TO_DATE < 오늘)
 *   CANCELED   -> EXPIRE_YN='N', DEL_YN='N' (소프트 취소)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveGrantStatusServiceImpl implements LeaveGrantStatusService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXHAUSTED = "EXHAUSTED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_CANCELED = "CANCELED";

    private static final String YN_Y = "Y";
    private static final String YN_N = "N";

    private static final String SYSTEM_UPDATER = "SYSTEM";

    private final LeaveGrantStatusMapper leaveGrantStatusMapper;

    @Override
    @Transactional
    public void recalcStatus(String cmpnyCd, String grantId) {
        LeaveGrantStatusVO grant = leaveGrantStatusMapper.selectGrantById(cmpnyCd, grantId);
        if (grant == null) {
            log.warn("부여건 STATUS 재평가 대상 없음. cmpnyCd={}, grantId={}", cmpnyCd, grantId);
            return;
        }

        // 절대 규칙: CANCELED 행은 절대 다른 상태로 되돌리지 않음 (§8.5.8 #2)
        if (STATUS_CANCELED.equals(grant.getStatus())) {
            log.debug("CANCELED 상태 부여건은 재평가하지 않음. grantId={}", grantId);
            return;
        }

        String nextStatus = resolveNextStatus(grant);
        if (nextStatus.equals(grant.getStatus())) {
            log.debug("STATUS 변경 없음. grantId={}, status={}", grantId, nextStatus);
            return;
        }

        applyStatusTransition(cmpnyCd, grantId, nextStatus, SYSTEM_UPDATER);
        log.info("부여건 STATUS 전이 완료. grantId={}, {} -> {}", grantId, grant.getStatus(), nextStatus);
    }

    @Override
    @Transactional
    public void recalcUsedDays(String cmpnyCd, String grantId) {
        LeaveGrantStatusVO grant = leaveGrantStatusMapper.selectGrantById(cmpnyCd, grantId);
        if (grant == null) {
            log.warn("USED_DAYS 재계산 대상 없음. cmpnyCd={}, grantId={}", cmpnyCd, grantId);
            return;
        }

        // CANCELED는 사용/부여 회계가 동결됨
        if (STATUS_CANCELED.equals(grant.getStatus())) {
            log.debug("CANCELED 상태 부여건은 USED_DAYS를 재계산하지 않음. grantId={}", grantId);
            return;
        }

        BigDecimal rawSum = leaveGrantStatusMapper.selectUsedDaysSum(cmpnyCd, grantId);
        BigDecimal usedSum = (rawSum == null) ? BigDecimal.ZERO : rawSum;

        // 절대 규칙: 기 발생 연차 사후 차감 금지. 합계가 GRANT_DAYS 초과 시 GRANT_DAYS로 클램프 (§8.5.8 #2)
        BigDecimal grantDays = (grant.getGrantDays() == null) ? BigDecimal.ZERO : grant.getGrantDays();
        BigDecimal clamped = usedSum;
        if (usedSum.compareTo(grantDays) > 0) {
            log.warn("tb_user_leave_use 합계({})가 GRANT_DAYS({})를 초과하여 클램프함. grantId={}",
                    usedSum, grantDays, grantId);
            clamped = grantDays;
        }

        // TODO(developer): tb_user_leave_use.LEAVE_MINUTES (분 단위 사용분) 처리는 본 작업 범위 밖.
        // 사용단위(SYS025) 코드는 prafta-019-A 재정렬 후 00=1일 / 01=반차 / 02=시간차2시간 /
        // 03=시간차1시간 / 04=시간차30분 이며, 시간차 차감은 LeaveDeductionService 가
        // "그날 소정근로분 기준 동적 환산"으로 LEAVE_DAYS(decimal(8,5))에 저장한다.
        // 본 분기는 LEAVE_DAYS 합계만 사용한다. Attd_09(단계 3)에서 LEAVE_MINUTES 합산이 필요해지면 추가.

        leaveGrantStatusMapper.updateUsedDays(cmpnyCd, grantId, clamped, SYSTEM_UPDATER);
        log.info("USED_DAYS 재계산 완료. grantId={}, usedDays={}", grantId, clamped);

        // STATUS 재평가 (USED_DAYS 갱신값 반영)
        grant.setUsedDays(clamped);
        String nextStatus = resolveNextStatus(grant);
        if (!nextStatus.equals(grant.getStatus())) {
            applyStatusTransition(cmpnyCd, grantId, nextStatus, SYSTEM_UPDATER);
            log.info("USED_DAYS 재계산 후 STATUS 전이. grantId={}, {} -> {}",
                    grantId, grant.getStatus(), nextStatus);
        }
    }

    @Override
    @Transactional
    public int expireOverdueGrants() {
        int affected = leaveGrantStatusMapper.expireOverdueGrants();
        log.info("연차 만료 배치 처리 건수: {}", affected);
        return affected;
    }

    @Override
    @Transactional
    public void cancelGrant(String cmpnyCd, String grantId, String reasonByUserCd) {
        LeaveGrantStatusVO grant = leaveGrantStatusMapper.selectGrantById(cmpnyCd, grantId);
        if (grant == null) {
            log.warn("부여건 취소 대상 없음. cmpnyCd={}, grantId={}", cmpnyCd, grantId);
            return;
        }

        if (STATUS_CANCELED.equals(grant.getStatus())) {
            log.debug("이미 CANCELED 상태인 부여건. grantId={}", grantId);
            return;
        }

        // 소프트 취소: STATUS=CANCELED, DEL_YN=N 유지 (§8.5.6 / §8.5.8)
        String updater = (reasonByUserCd == null || reasonByUserCd.isBlank()) ? SYSTEM_UPDATER : reasonByUserCd;
        applyStatusTransition(cmpnyCd, grantId, STATUS_CANCELED, updater);
        log.info("부여건 강제 취소 완료. grantId={}, {} -> CANCELED, 수행자={}",
                grantId, grant.getStatus(), updater);
    }

    /**
     * 현재 grant 행 상태로부터 전이할 다음 STATUS를 판정한다.
     * CANCELED는 호출 전에 early return 되므로 본 메서드에서는 다루지 않는다.
     *
     * <p>전이 우선순위: EXPIRED &gt; EXHAUSTED &gt; ACTIVE.
     */
    private String resolveNextStatus(LeaveGrantStatusVO grant) {
        // 1. 만료 판정 (AVAIL_TO_DATE 형식: YYYYMMDD varchar(8))
        if (isExpired(grant.getAvailToDate())) {
            return STATUS_EXPIRED;
        }

        // 2. 소진 판정 (USED_DAYS >= GRANT_DAYS)
        BigDecimal usedDays = (grant.getUsedDays() == null) ? BigDecimal.ZERO : grant.getUsedDays();
        BigDecimal grantDays = (grant.getGrantDays() == null) ? BigDecimal.ZERO : grant.getGrantDays();
        if (grantDays.compareTo(BigDecimal.ZERO) > 0 && usedDays.compareTo(grantDays) >= 0) {
            return STATUS_EXHAUSTED;
        }

        return STATUS_ACTIVE;
    }

    /**
     * YYYYMMDD 문자열 비교로 만료 여부를 판정한다.
     * 시스템 시각(KST 기준 prafta 표준) 오늘 미만이면 만료.
     */
    private boolean isExpired(String availToDate) {
        if (availToDate == null || availToDate.length() != 8) {
            return false;
        }
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return availToDate.compareTo(today) < 0;
    }

    /**
     * STATUS 전이 시 EXPIRE_YN/DEL_YN을 §8.5.8 매핑표대로 동시 UPDATE.
     */
    private void applyStatusTransition(String cmpnyCd, String grantId, String nextStatus, String updateBy) {
        String expireYn;
        String delYn;
        switch (nextStatus) {
            case STATUS_EXPIRED:
                expireYn = YN_Y;
                delYn = YN_N;
                break;
            case STATUS_ACTIVE:
            case STATUS_EXHAUSTED:
            case STATUS_CANCELED:
            default:
                expireYn = YN_N;
                delYn = YN_N;
                break;
        }
        leaveGrantStatusMapper.updateStatusWithSync(cmpnyCd, grantId, nextStatus, expireYn, delYn, updateBy);
    }
}
