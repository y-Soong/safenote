package com.prafta.web.user.user10.dto.response;

import java.util.List;

import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSaveResult;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-10: 소정근로시간 이력 등록/정정 결과 응답 (User_10).
 *
 * <p>서비스가 돌려준 <b>부수효과</b>를 그대로 화면에 노출하기 위한 응답이다. 이 값들을 감추면
 * 관리자는 자기 조작이 다른 행을 만들거나 옮겼다는 사실을 모른 채 화면을 닫는다.
 * <ul>
 *   <li>{@code warnings} — 주 15시간 미만 / 육아기 범위 밖 / 복귀 행 생성 skip 등(저장은 완료).</li>
 *   <li>{@code closedPrevEndDate} — 직전 열린 행이 자동 마감된 종료일.</li>
 *   <li>{@code restoreStrDate} — 단축 종료 후 <b>자동 생성</b>된 복귀 행의 시작일.</li>
 *   <li>{@code movedRestoreStrDate} — 정정으로 <b>함께 이동</b>된 복귀 행의 새 시작일.</li>
 * </ul>
 */
@Value
@Builder
public class StdWorkSaveResponse {

    /** 저장된 이력 행의 적용 시작일 */
    String applyStrDate;

    /** 자동 마감된 직전 열린 행의 종료일 (마감 없었으면 null) */
    String closedPrevEndDate;

    /** 자동 생성된 복귀 행의 적용 시작일 (생성 없었으면 null) */
    String restoreStrDate;

    /** 자동 생성된 복귀 행의 주 소정근로 분 (생성 없었으면 null) */
    Integer restoreWeekStdMinutes;

    /** 정정으로 함께 이동된 복귀 행의 새 적용 시작일 (이동 없었으면 null) */
    String movedRestoreStrDate;

    /** 이동 전 복귀 행의 적용 시작일 (이동 없었으면 null) */
    String movedRestoreFromStrDate;

    /** 경고 문구 목록 (저장은 완료됨). 비어 있으면 경고 없음. */
    List<String> warnings;

    public static StdWorkSaveResponse of(StdWorkHoursSaveResult result) {
        return StdWorkSaveResponse.builder()
                .applyStrDate(result.getApplyStrDate())
                .closedPrevEndDate(result.getClosedPrevEndDate())
                .restoreStrDate(result.getRestoreStrDate())
                .restoreWeekStdMinutes(result.getRestoreWeekStdMinutes())
                .movedRestoreStrDate(result.getMovedRestoreStrDate())
                .movedRestoreFromStrDate(result.getMovedRestoreFromStrDate())
                .warnings(result.getWarnings() == null ? List.of() : result.getWarnings())
                .build();
    }
}
