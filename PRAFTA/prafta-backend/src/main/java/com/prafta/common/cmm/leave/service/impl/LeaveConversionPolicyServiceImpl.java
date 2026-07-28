package com.prafta.common.cmm.leave.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveConversionPolicyService} 구현 — 개인 기본 근무타입 소정근로분 (PC-03, D1).
 *
 * <p>480 상수 반환(2026-07-21 전환)을 폐기하고, {@code tb_user.DEFAULT_SCH_CD} 가 가리키는
 * 스케줄의 대상일 기준 유효 버전에서 소정근로분을 산출한다. 산식은
 * {@link ScheduleWorkMinutesUtils}(그날 D 계산과 단일 출처)를 공유한다.
 * 480 초과는 480 캡(근로자 유리, §5-③), 산출 불가는 null(fail-closed, N5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveConversionPolicyServiceImpl implements LeaveConversionPolicyService {

    /** 기본 근무타입 스케줄 조회(effective-dating) — selectDailySchedule 패턴 공유 매퍼. */
    private final LeaveDeductionMapper leaveDeductionMapper;

    @Override
    public Integer resolvePersonalConvMinutes(String cmpnyCd, String userCd, String workYmd) {
        if (cmpnyCd == null || userCd == null || workYmd == null) {
            return null;
        }

        // DEFAULT_SCH_CD 미지정(교대 등)/참조 스케줄 미존재면 행 없음 → null(fail-closed).
        DailyScheduleVO sch = leaveDeductionMapper.selectUserDefaultSchedule(cmpnyCd, userCd, workYmd);
        if (sch == null) {
            return null;
        }

        Integer minutes = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(sch);
        if (minutes == null || minutes <= 0) {
            // 스케줄은 있으나 시각 비정상/0 이하 — 데이터 품질 이슈로 보고 차단 측(fail-closed).
            log.warn("[leave-conv] 개인 분모 산출 실패 - 기본 근무타입 시각 비정상: cmpnyCd={}, userCd={}, workYmd={}, schCd={}",
                    cmpnyCd, userCd, workYmd, sch.getSchCd());
            return null;
        }

        // 480 캡(§5-③): 휴게 미입력 등으로 소정근로가 8시간을 초과하는 근무타입은 8시간 기준(근로자 유리).
        return Math.min(minutes, DEFAULT_CONV_MINUTES);
    }
}
