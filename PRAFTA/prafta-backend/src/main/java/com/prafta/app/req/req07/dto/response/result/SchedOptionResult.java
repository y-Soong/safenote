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
        , String fstStrTime
        , String fstEndTime
        , String secStrTime
        , String secEndTime

        // PRAFTA-FIXEDOT-2(표기): 고정연장(전방·후방, HHmm, NULL=없음) — 옵션 선택 시 고정연장
        // 유무/시각을 라벨로 노출(프론트 포맷). ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑).
        , String preFixedOtStrTime
        , String preFixedOtEndTime
        , String fixedOtStrTime
        , String fixedOtEndTime
) {
}
