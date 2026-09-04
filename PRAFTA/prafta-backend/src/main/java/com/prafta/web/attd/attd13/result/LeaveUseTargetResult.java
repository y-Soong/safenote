package com.prafta.web.attd.attd13.result;

/**
 * 변경/삭제 대상 연차 사용행 검증용 최소 결과 (PRAFTA-COM-008-C).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 *
 * <p>T1(이동 재정의) 확장: 재차감·속성 승계(§2-4)에 필요한 컬럼을 <b>마지막에</b> 추가(기존 9컬럼 순서 불변).
 * {@code reqId} 연결 건은 T3 대표행 정규화로 항상 첫 분할행(MIN LEAVE_ID)이 로드되므로
 * {@code leaveMinutes} 가 곧 신청 총 분(불변식 1)이다. {@code reqLeaveCd} 는 REQ 원 종류
 * (TB_USER_ATTD_REQ.LEAVE_TYPE — PC-05 발동 건은 행 LEAVE_CD 가 부여 귀속이라 다를 수 있음).
 *
 * <p>F6(qa D-8): {@code evidenceFileId} 를 마지막에 추가(증빙 링크 승계 — SELECT 마지막 컬럼과 동시).
 *
 * <p>2026-08-14: {@code reqStatus}(연결된 연차사용 요청 TB_USER_ATTD_REQ.REQ_STATUS)를 <b>마지막에</b> 추가.
 * 결재 진행 중('01')인 연차를 변경/삭제 대상으로 삼는 것을 차단하는 데 쓴다. 결재를 거치지 않은
 * 직접 차감 건은 {@code reqId}·{@code reqStatus} 가 모두 null 이다.
 */
public record LeaveUseTargetResult(
      String leaveId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String leaveCd
    , String grantId
    , String startDate
    , String useUnitType
    , String leaveStatus
    , String reqId
    , String reqLeaveCd
    , String startTime
    , String endTime
    , Integer leaveMinutes
    , String promotionStage
    , String designatorType
    , String origDesignatedDate
    , String leaveReason
    , String evidenceFileId
    , String reqStatus
    /** BW-04(Q-1 승계): 원 행 BRK_WAIVE_YN. ★위치매핑 — 맨 끝. */
    , String brkWaiveYn
    /** BW-04(Q-1 승계): 원 행 BRK_WAIVE_REQ_DTIME('yyyy-MM-dd HH:mm:ss' 문자열, null 가능). ★위치매핑 — 맨 끝. */
    , String brkWaiveReqDtime
) {
}
