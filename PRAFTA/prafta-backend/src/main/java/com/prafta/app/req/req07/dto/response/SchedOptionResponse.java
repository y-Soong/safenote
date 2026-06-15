package com.prafta.app.req.req07.dto.response;

import java.util.List;

import com.prafta.app.req.req07.dto.response.result.SchedOptionResult;

/**
 * prafta-app-007 F2: 스케줄 수정 요청 폼의 "스케줄 선택" 목록 응답.
 *
 * <p>식별값(cmpnyCd/siteCd)은 JWT 에서만 도출하므로 응답에 포함하지 않는다.
 * 빈 결과는 200 + 빈 배열 (예외 아님).
 *
 * <p>prafta-com-008-E-9a: 사업장 BASE_YN 폐기 → 사용자 본인 기본 근무타입(userDefaultSchCd)을 동반한다.
 *   프론트(SchedPickSheet/SchedModifyForm)가 이 값으로 "기본" 칩 표시·상단 정렬을 처리한다(미설정 시 null).
 *
 * <p>prafta-com-008-D-5: 스케줄수정 폼 진입 시 대상 일자(workYmd)가 교대팀 소속 구간이면 shiftLocked=true.
 *   프론트가 제출 버튼 비활성 + 안내를 표시한다(서버 D-3 가드가 최종 강제). workYmd 미전달 시 false.
 */
public record SchedOptionResponse(
        List<SchedOptionResult> schedules
        , String userDefaultSchCd
        , boolean shiftLocked
) {
}
