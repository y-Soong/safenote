package com.prafta.app.attd.attd01.dto.response;

import java.math.BigDecimal;

/**
 * 앱 근태 일/주 응답의 부분연차(시간차/반차) 마커 1건(표시 전용).
 *
 * <p>prafta-app-018-E 의 단건 스칼라(leaveTypeName/leaveUnitType/leaveTimeRange/leaveDays)가
 * 하루 1건만 표현하던 한계를 보완해, 같은 날 다건(예: 14:30~16:30 시간차 + 17:00~17:30 시간차)을
 * 리스트로 내려보내기 위한 항목. 필드 의미/포맷은 단건 스칼라와 동일하다(FE attdFormat.formatLeaveMarker
 * 가 그대로 소비). leaveUnitType=SYS025 코드 원값, leaveTimeRange="HHMM~HHMM"(둘 다 있을 때만, 없으면 null),
 * leaveDays=차감일수 원값(정규화는 FE), leaveTypeName=연차종류명.
 *
 * <p>근무일 산출(slot/dayType/status)에는 일절 관여하지 않는다(표시 전용).
 *
 * <p>PRAFTA_COM_002-B-1: pendingApproval=승인 대기(요청중) 연차 여부. true 면 FE 가 마커 옆에 "요청중" 배지를 부가한다.
 *   판정=REQ_ID NOT NULL AND TB_USER_ATTD_REQ.REQ_STATUS='01'(신청). 무결재 즉시확정/승인('02')은 false(배지 없음).
 *   record 이므로 생성 호출부(toLeaveMarkers)도 함께 수정한다.
 */
public record LeaveMarkerItem(
        String leaveTypeName
        , String leaveUnitType
        , String leaveTimeRange
        , BigDecimal leaveDays
        , boolean pendingApproval
) {
}
