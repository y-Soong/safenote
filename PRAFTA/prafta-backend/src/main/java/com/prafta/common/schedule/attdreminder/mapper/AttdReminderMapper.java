package com.prafta.common.schedule.attdreminder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.schedule.attdreminder.AttdReminderTargetResult;

/**
 * 출근/퇴근 5분 전 리마인더(W4/W5) 대상 산출 전용 Mapper (PRAFTA-APP-021-4).
 *
 * <p>채번/적재는 {@code LeaveDashboardMapper.selectNextNotiId/insertNotiOutbox} 를 재사용하므로
 * 본 매퍼는 대상 산출 쿼리 2종만 보유한다. 시각은 모두 서버 LocalTime 기준 HHMM(단일 타임존, §8-R 5).
 * 컬럼 콤마 leading, {@code #{}} 바인딩, SELECT * 금지.
 *
 * <p>패키지는 {@code @MapperScan(com.prafta.**.**.mapper)} 에 잡히도록 {@code ...attdreminder.mapper}
 * 에 둔다(XML 도 {@code .../attdreminder/mapper/} 폴더 — 전 프로젝트 매퍼 공통 컨벤션).
 */
@Mapper
public interface AttdReminderMapper {

    /**
     * 출근 리마인더(W4) 대상: 오늘({@code workYmd}) 스케줄의 어느 구간 시작시각이 {@code targetHhmm}
     * (=현재+5분)과 같은 사용자. 단, 종일 확정 연차일은 제외, 해당 구간(workSeq) 출근 기록이 이미
     * 있으면 제외. 2구간 스케줄이면 1구간/2구간 시작을 각각 매칭(구간별 1행).
     *
     * @param workYmd    오늘(YYYYMMDD)
     * @param targetHhmm 현재+5분(HHMM)
     */
    List<AttdReminderTargetResult> selectCheckInTargets(@Param("workYmd") String workYmd,
                                                        @Param("targetHhmm") String targetHhmm);

    /**
     * 퇴근 리마인더(W5) 대상: 스케줄의 <b>마지막 구간 종료시각</b>(2구간=2구간 종료, 그 외=1구간 종료)이
     * {@code targetHhmm}(=현재+5분)과 같은 사용자. 단, 그 마지막 구간에 출근 기록이 있고(미출근이면 제외,
     * §8-R 3) 아직 퇴근하지 않은(CHECK_OUT_TIME NULL) 경우만.
     *
     * <p>오버나이트(자정 넘김) 근무 대응: 근무계획은 시작일({@code workYmd}) 기준으로 저장되는데,
     * 자정을 넘겨 종료하는 스케줄은 종료 시각이 다음 날(오늘 시점 실행에서는 {@code workYmd}, 근무계획
     * 상으로는 그 전날 = {@code prevWorkYmd})에 온다. 두 날짜를 모두 조회해 각 근무계획의 실제 자정 넘김
     * 여부(마지막 구간 종료시각이 시작시각보다 이른지)로 분기한다(한쪽만 매칭).
     *
     * @param workYmd     오늘(YYYYMMDD) — 당일 종료(자정 안 넘김) 스케줄 매칭용
     * @param prevWorkYmd 어제(YYYYMMDD) — 전날 시작 오버나이트 스케줄 매칭용
     * @param targetHhmm  현재+5분(HHMM)
     */
    List<AttdReminderTargetResult> selectCheckOutTargets(@Param("workYmd") String workYmd,
                                                         @Param("prevWorkYmd") String prevWorkYmd,
                                                         @Param("targetHhmm") String targetHhmm);
}
