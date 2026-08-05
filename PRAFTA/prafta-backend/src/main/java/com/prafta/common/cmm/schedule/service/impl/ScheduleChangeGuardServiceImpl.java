package com.prafta.common.cmm.schedule.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.schedule.mapper.ScheduleGuardMapper;
import com.prafta.common.cmm.schedule.mapper.result.LeaveLockDayResult;
import com.prafta.common.cmm.schedule.service.ScheduleChangeGuardService;
import com.prafta.common.cmm.schedule.vo.ScheduleLockVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공통 스케줄 변경 가드 서비스 구현 (prafta-com-016 shared-schedule-guard).
 *
 * <p>read-only 판정만 수행한다(@Transactional 없음 — 호출자의 트랜잭션에 자연 참여하거나 단독 SELECT).
 * 연차/OT 잠금 일자를 각각 1쿼리로 일괄 조회하여 합산한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleChangeGuardServiceImpl implements ScheduleChangeGuardService {

    private final ScheduleGuardMapper scheduleGuardMapper;

    @Override
    public List<ScheduleLockVO> findLockedDays(String cmpnyCd, String siteCd, String userCd,
                                               List<String> workYmds) {

        List<ScheduleLockVO> locked = new ArrayList<>();
        if (workYmds == null || workYmds.isEmpty()) {
            return locked;
        }

        // 연차 잠금일 — 확정(USE_UNIT_TYPE 무관, 종일/반차/시간차 전부) + 미결 시간차 신청(E3,
        //   PENDING_YN='Y' — 미결 잠금은 시간차만. 신청 시점부터 그날 스케줄 변경을 막아 당일 분모를 보존).
        List<LeaveLockDayResult> leaveDays =
                scheduleGuardMapper.selectLeaveLockedDays(cmpnyCd, userCd, workYmds);
        for (LeaveLockDayResult row : leaveDays) {
            locked.add(new ScheduleLockVO(row.getWorkYmd(), ScheduleLockVO.Reason.LEAVE,
                    row.getUseUnitType(), "Y".equals(row.getPendingYn())));
        }

        // 초과근무 잠금일(등록 또는 신청).
        List<String> otDays =
                scheduleGuardMapper.selectOvertimeLockedDays(cmpnyCd, siteCd, userCd, workYmds);
        for (String ymd : otDays) {
            locked.add(new ScheduleLockVO(ymd, ScheduleLockVO.Reason.OT, null));
        }

        if (!locked.isEmpty()) {
            log.debug("스케줄 변경 가드 판정 - userCd={}, siteCd={}, 입력 {}건, 잠금 {}건",
                    userCd, siteCd, workYmds.size(), locked.size());
        }
        return locked;
    }
}
