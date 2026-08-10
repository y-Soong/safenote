package com.prafta.common.cmm.leave.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveConversionPolicyService} 구현 (PC-03 D1 → 당일분모 전환 E1).
 *
 * <p>실차감 분모 = 당일 배정 스케줄 소정근로분({@link #resolveDailyConvMinutes} — E1),
 * E4 참고 분모 = 개인 기본 근무타입 소정근로분({@link #resolvePersonalConvMinutes} —
 * 2026-08-09 표기 규약 변경 이후 내부 판정·구버전 호환용, 신 FE 표기 미사용).
 * 산식은 양쪽 모두 {@link ScheduleWorkMinutesUtils}(그날 D 계산과 단일 출처)를 공유한다.
 * 480 초과는 480 캡(근로자 유리, E7), 산출 불가는 null(fail-closed).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveConversionPolicyServiceImpl implements LeaveConversionPolicyService {

    /** 당일 배정 스케줄·기본 근무타입 스케줄 조회(effective-dating) 공유 매퍼. */
    private final LeaveDeductionMapper leaveDeductionMapper;

    @Override
    public Integer resolveDailyConvMinutes(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null) {
            return null;
        }

        // 당일 배정 스케줄(TB_USER_WORK_PLAN → TB_SCH_MGMT). 미배정/연차 코드면 행 없음 → null(fail-closed, E2).
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            return null;
        }
        return capConvMinutes(sch, "당일", cmpnyCd, userCd, workYmd);
    }

    @Override
    public Integer resolvePersonalConvMinutes(String cmpnyCd, String userCd, String workYmd) {
        if (cmpnyCd == null || userCd == null || workYmd == null) {
            return null;
        }

        // E4 참고 표시 전용 — DEFAULT_SCH_CD 미지정(교대 등)/참조 스케줄 미존재면 행 없음 → null.
        DailyScheduleVO sch = leaveDeductionMapper.selectUserDefaultSchedule(cmpnyCd, userCd, workYmd);
        if (sch == null) {
            return null;
        }
        return capConvMinutes(sch, "개인(참고)", cmpnyCd, userCd, workYmd);
    }

    /**
     * 스케줄 시각 → 소정근로분 → 480 캡(E7) 공통 처리.
     * 시각 비정상/0 이하는 데이터 품질 이슈로 보고 차단 측(null, fail-closed).
     */
    private Integer capConvMinutes(DailyScheduleVO sch, String kind,
                                   String cmpnyCd, String userCd, String workYmd) {
        Integer minutes = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(sch);
        if (minutes == null || minutes <= 0) {
            log.warn("[leave-conv] {} 분모 산출 실패 - 스케줄 시각 비정상: cmpnyCd={}, userCd={}, workYmd={}, schCd={}",
                    kind, cmpnyCd, userCd, workYmd, sch.getSchCd());
            return null;
        }

        // 480 캡(E7): 휴게 미입력 등으로 소정근로가 8시간을 초과하는 스케줄은 8시간 기준(근로자 유리).
        return Math.min(minutes, DEFAULT_CONV_MINUTES);
    }
}
