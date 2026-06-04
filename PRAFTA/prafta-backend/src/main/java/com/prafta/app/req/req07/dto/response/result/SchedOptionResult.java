package com.prafta.app.req.req07.dto.response.result;

/**
 * prafta-app-007 F2: 스케줄 선택 옵션 단건 (조회 결과 row).
 *
 * <p>라벨(시각 문자열 결합)은 프론트에서 포맷한다 — 본 record 는 원시 시각 필드만 내려준다.
 * (라벨 정책 변경 시 백엔드 무수정.)
 *
 * <p>시각 필드는 'HHmm' 4자리 문자열 (tb_sch_mgmt 의 varchar(4)). 2구간이 없는 스케줄은
 * secStrTime / secEndTime 이 null.
 */
public record SchedOptionResult(
        String schCd
        , String schNo
        , String baseYn
        , String fstStrTime
        , String fstEndTime
        , String secStrTime
        , String secEndTime
) {
}
