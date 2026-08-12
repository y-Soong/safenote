package com.prafta.common.cmm.stdwork.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 소정-02: 소정근로시간 이력 등록/정정 결과.
 *
 * <p>★경고와 차단의 분리 (plan §8 Q4 확정): 주 15시간 미만 / 육아기 범위 밖은
 * <b>저장을 허용</b>하고 경고 문구만 반환한다. 실제 차단 사유는 예외
 * ({@code StdWorkErrorCode})로 던져지므로 본 결과에 도달하지 않는다.
 */
@Getter
@Builder
public class StdWorkHoursSaveResult {

    /** 저장된 이력 행의 적용 시작일 (YYYYMMDD) */
    private final String applyStrDate;

    /** 직전 열린 이력 행을 자동 마감한 경우 그 종료일 (YYYYMMDD, 마감 없었으면 null) */
    private final String closedPrevEndDate;

    /**
     * 단축 종료 후 자동 생성된 복귀 행의 적용 시작일 (YYYYMMDD).
     *
     * <p>생성하지 않은 경우 null — ①무기한 행 등록 ②직전 열린 행이 없어 마감이 없었던 경우
     * ③복귀 구간이 기존 행과 겹쳐 skip 한 경우(이때는 warnings 에 안내 1건이 들어간다).
     */
    private final String restoreStrDate;

    /** 자동 생성된 복귀 행의 주 소정근로 분 (직전 행에서 승계). 생성 없으면 null */
    private final Integer restoreWeekStdMinutes;

    /** 자동 생성된 복귀 행의 사유코드 (직전 행에서 승계). 생성 없으면 null */
    private final String restoreReasonCd;

    /**
     * 정정(correct)으로 종료일이 바뀌면서 <b>함께 이동된</b> 복귀 행의 새 적용 시작일 (YYYYMMDD).
     *
     * <p>이동이 없었으면 null. 화면은 "복귀 이력 시작일도 함께 조정되었습니다" 안내에 쓴다.
     */
    private final String movedRestoreStrDate;

    /** 이동 전 복귀 행의 적용 시작일 (YYYYMMDD). 이동이 없었으면 null */
    private final String movedRestoreFromStrDate;

    /**
     * 경고 문구 목록 (저장은 완료됨).
     *
     * <p>화면은 이 목록을 경고 배너로 표시한다. 비어 있으면 경고 없음.
     */
    private final List<String> warnings;

    /** 경고가 1건 이상인지 여부. */
    public boolean hasWarning() {
        return warnings != null && !warnings.isEmpty();
    }

    /** 단축 종료 후 복귀 행이 자동 생성되었는지 여부. */
    public boolean hasRestoreRow() {
        return restoreStrDate != null;
    }

    /** 정정으로 복귀 행이 함께 이동되었는지 여부. */
    public boolean hasMovedRestoreRow() {
        return movedRestoreStrDate != null;
    }
}
