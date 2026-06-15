package com.prafta.app.attd.attd01.result;

/**
 * prafta-app check-out: 셀프 퇴근 대상 근태 조회 결과 (TB_USER_ATTD_MGMT).
 *
 * <p>prafta-app-026: 재퇴근(re-checkout) 도입으로 대상 선정이 "열린 근태(CHECK_OUT_TIME NULL)"
 *   에서 "D+1 윈도우 내 최신 출근행(CHECK_OUT_TIME 유무 무관)"으로 확장됐다.
 *   {@code checkOutTime} 이 null 이면 최초 퇴근, 비-null 이면 재퇴근(시각 덮어쓰기) 분기에 사용한다.
 *   D+1 윈도우/사업장/마감 검증은 ServiceImpl 에서 수행하므로 판정에 필요한 원천값만 담는다.
 */
public record OpenAttdResult(
    String attdId
    , String workYmd
    , int workSeq
    , String siteCd
    , String nodeCd
    , String checkOutTime   // prafta-app-026: null=최초 퇴근, 非null=재퇴근(이미 퇴근됨)
) {
}
