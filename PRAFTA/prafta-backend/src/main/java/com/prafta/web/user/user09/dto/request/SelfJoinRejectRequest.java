package com.prafta.web.user.user09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소정-09: 셀프가입 거부 요청 (User_09).
 *
 * <p>거부는 계정을 삭제하지 않고 {@code ACCOUNT_STATUS='07' + USE_YN='N'} 로 보존한다.
 * 거부 사유는 감사 로그(tb_audit_log)로만 남긴다 — tb_user 에 사유 컬럼을 두지 않는다
 * (재가입 시 같은 행이 재활용되므로 컬럼에 남기면 과거 사유가 조용히 덮어써진다).
 */
@Getter
@Setter
@NoArgsConstructor
public class SelfJoinRejectRequest {

    /** 거부 대상 사용자 코드 (필수) */
    private String userCd;

    /** 거부 사유 (필수, 200자 이내) */
    private String rejectReason;
}
