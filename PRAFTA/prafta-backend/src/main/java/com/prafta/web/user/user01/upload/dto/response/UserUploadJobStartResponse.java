package com.prafta.web.user.user01.upload.dto.response;

import java.util.List;

import com.prafta.web.user.user01.util.UserExcelValueRestorer;

/**
 * 비동기 업로드 시작 응답 (PRAFTA-037-F6).
 * 잡 ID 와 파싱된 행 수를 즉시 반환하여 프론트가 폴링을 시작할 수 있게 한다.
 *
 * <p>2026-08-15 추가 — {@code adjustments}: 엑셀 서식 유실로 앞자리 0 이 떨어진 값을 복원한 내역이다.
 * 파싱은 잡 등록 전 <b>동기</b> 구간에서 끝나므로 이 응답에 담아 즉시 내려준다(잡 테이블에 컬럼을 늘리지 않는다).
 * 조용히 고치지 않고 사용자가 눈으로 검증할 수 있게 하는 것이 목적이다.
 */
public record UserUploadJobStartResponse(
        String jobId
        , int totalRows
        , List<UserExcelValueRestorer.Adjustment> adjustments
) {
}
