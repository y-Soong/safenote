package com.prafta.common.schedule.attdreminder;

/**
 * 출근/퇴근 리마인더(W4/W5) 대상 1행 (PRAFTA-APP-021-4).
 *
 * <p>대상 산출 쿼리({@code AttdReminderMapper.selectCheckInTargets}/{@code selectCheckOutTargets})의
 * 결과. workSeq 는 매칭된 구간(1=1구간, 2=2구간)으로, dedupKey 구성과 payload 라우팅에 사용한다.
 * record 위치 매핑(SELECT 컬럼 순서 = 생성자 인자 순서)을 준수한다.
 */
public record AttdReminderTargetResult(
    String cmpnyCd
    , String siteCd
    , String userCd
    , String nodeCd
    , int workSeq
) {
}
