package com.prafta.web.attd.attd07.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /attd07/delete-user-overtime.
 *
 * com-016-E - 일자 상세(AttdDayDetailPop) 관리자 직접 등록 OT 블록의 "초과근무 삭제" 버튼에서
 * 체크된 기저장 OT(OT_ID 보유) 여러 건을 한 번에 소프트삭제(DEL_YN='Y')한다.
 *
 * <p>com-013 #6 의 단건(otId) 수신을 다건(otIds) 수신으로 확장했다. FE 는 항상 배열로 전송한다.
 *
 * <p>body 의 siteCd / userCd 는 서버가 JWT 신원 및 OT 행 scope 와 교차 검증한다.
 * gvAuthCd / gvUserCd 는 토큰에서 도출하므로 body 위조로 권한 escalation 할 수 없다(IDOR 방지).
 * workYmd 는 마감 가드(ensureNotClosed) 판정에 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeleteUserOvertimeRequest {

    /** 삭제 대상 초과근무 ID 목록 (다건). 최소 1건 이상 필요. */
    @NotEmpty
    private List<@NotBlank String> otIds;

    @NotBlank
    private String siteCd;

    @NotBlank
    private String userCd;

    /** 근무일자 (YYYYMMDD) — 마감 가드 판정용. */
    @NotBlank
    private String workYmd;

    /** 근무 노드 코드 — 마감 가드(부서) 판정용. */
    private String nodeCd;

    /** 삭제 사유 (선택). 미입력 시 서버가 기본 문구로 이력을 기록한다. */
    private String reqReason;
}
