package com.prafta.common.cmm.schedule.service;

import java.util.Map;

/**
 * 교차일(앞뒤 근무일) 근무 스케줄 시각 겹침 판정 공용 가드.
 *
 * <p>야간(오버나이트) 근무가 다음날로 넘어가는 구간과 인접일 근무 구간이 시각상 겹치는지를
 * 검사한다. 정책서 attd §7.6 의 "근무 구간 시각 겹침 금지" 를 같은 사용자의 <b>서로 다른 날짜</b>
 * 사이로 확장한 것이다(기존 ATTD_400_113 은 같은 근무일의 WORK_SEQ 구간 전용).
 *
 * <p>판정 규약(사용자 결정 2026-06-22):
 * <ul>
 *   <li>전날 종료시각(자정 넘김분) &gt; 당일 시작시각 이면 겹침으로 본다(양방향 — 당일이 야간이라
 *       다음날로 넘어가는 경우도 동일).</li>
 *   <li>인접 경계(앞 구간 종료 == 뒤 구간 시작)는 겹치지 않는 것으로 본다(ATTD_400_113 동일).</li>
 *   <li>2구간 근무타입(SCH_TYPE='02')은 1·2구간을 모두 펼쳐 검사 대상에 포함한다.</li>
 * </ul>
 *
 * <p>사용처: 웹 attd05 근무계획 직접저장, 웹 attd07 앱 스케줄수정 요청 승인(반영). 차단 방식은
 * 호출자가 결정한다(attd05=셀 스킵 후 사유 표시 / attd07=ApiException 차단).
 */
public interface ScheduleOverlapGuardService {

    /**
     * 대상일(workYmd)에 candidateSchCd 를 적용했을 때 앞날(D-1)·다음날(D+1)의 적용 스케줄과
     * 시각이 겹치는지 판정한다.
     *
     * @param cmpnyCd        회사코드
     * @param siteCd         사업장코드
     * @param userCd         대상 사용자코드
     * @param workYmd        대상 근무일(yyyyMMdd)
     * @param candidateSchCd 대상일에 적용하려는 근무타입(SCH_CD)
     * @param pendingSchByYmd 같은 저장 배치에서 함께 바뀌는 이웃 날짜의 적용 코드 오버라이드
     *                        (ymd → SCH_CD, 값이 빈 문자열/null 이면 그 날 "스케줄 없음").
     *                        DB(work_plan)보다 우선한다. 단건 처리(attd07)면 null 전달.
     * @return 겹치면 true
     */
    boolean hasCrossDayOverlap(String cmpnyCd, String siteCd, String userCd,
            String workYmd, String candidateSchCd, Map<String, String> pendingSchByYmd);
}
