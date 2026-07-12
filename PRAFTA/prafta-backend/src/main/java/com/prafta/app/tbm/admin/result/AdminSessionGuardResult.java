package com.prafta.app.tbm.admin.result;

/**
 * 상태/스코프 게이트 검증용 경량 조회 결과.
 *
 * <p>수정/취소/비번재발급/라이브 제어 전, 현재 상태와 사업장/개설자/GPS검증유형을 확인해 비즈니스
 * 룰과 스코프 격리를 서버에서 재검증한다. managerNodeCd 는 노드관리자 스코프(개설자 NODE_CD ∈
 * scopedNodeCds) 판정용, gpsVerifyTypeCd 는 개별 이수처리(R3 T4) GPS 검증 세션 조건 판정용.
 */
public record AdminSessionGuardResult(
    String sessionCd
    , String siteCd
    , String statusCd
    , String managerUserCd
    , String managerNodeCd
    , String gpsVerifyTypeCd
    , Integer eduMinutes        // 교육준비(prepare) DB값 기준 교육시간 필수검증용(분, 1~60)
){
}
