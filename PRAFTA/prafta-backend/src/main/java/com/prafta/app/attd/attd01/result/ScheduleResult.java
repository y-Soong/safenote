package com.prafta.app.attd.attd01.result;

import java.time.LocalDateTime;

/**
 * prafta-app-002: 사용자 근무계획 + 스케줄 정의 조인 결과.
 *
 * <p>매핑 대상: AppAttd01Mapper.selectScheduleByRange (TB_USER_WORK_PLAN + TB_SCH_MGMT LEFT JOIN).
 * <p>prafta-com-008-E-2: 연차-스케줄 모델 전환 — WORK_PLAN_CD 는 항상 SCH_CD 만 가리킨다.
 *   연차일 판정은 work_plan 이 아니라 TB_USER_LEAVE_USE(CONFIRMED, 종일) 기준으로 단일화한다.
 *   leaveCd/leaveNm 은 더 이상 work_plan 에서 채워지지 않으며 항상 null(record 위치매핑 유지를 위해 필드만 잔존).
 * <p>시간 컬럼은 모두 varchar(4) HHMM, 휴게분(FST/SEC_SCH_BRK_MIN)은 varchar(3).
 *   2구간 여부는 secSchStrTime 이 not null 인지로 판정한다.
 */
public record ScheduleResult(
    String workYmd
    , String workPlanCd
    , String schCd
    , String schNo
    , String schType
    , String fstSchStrTime
    , String fstSchEndTime
    , String fstSchBrkMin
    , String secSchStrTime
    , String secSchEndTime
    , String secSchBrkMin
    , String leaveCd
    , String leaveNm

    // PRAFTA-FIXEDOT-2(M18): 고정연장(전방·후방 FROM/TO, HHMM, NULL=없음).
    //   소비처(J1)가 용도별로 분리 사용: 표기(주간/일상세 fixedOtSummary·예정 합계)만 소비하고
    //   지각/조퇴 판정(effectiveStart/End)·면제 프레임은 소정만 유지(정책 ②, 판정식 불변).
    // ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지).
    , String preFixedOtStrTime
    , String preFixedOtEndTime
    , String fixedOtStrTime
    , String fixedOtEndTime

    // 작업지시서_소속이동-이력가시성-보정: 그 근무일 배정 당시 사업장.
    , String siteCd
    , String siteNm

    // 작업지시서_소속이동-이력가시성-보정(QA High 보정): 동일 WORK_YMD 에 SITE_CD 가 다른
    //   TB_USER_WORK_PLAN 행이 공존할 때(소속이동 시 구 사업장 DEFAULT_SCH 행 잔존 등) 결정론적
    //   선택을 위한 타이브레이크 기준시각(WP.UPDATE_DATE 우선, 없으면 WP.INSERT_DATE). 표시 비대상,
    //   병합 로직 전용. 반드시 마지막 필드(위치매핑).
    , LocalDateTime effectiveDtime
) {
}
