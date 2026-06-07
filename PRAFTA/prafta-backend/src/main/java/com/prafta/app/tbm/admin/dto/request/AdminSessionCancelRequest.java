package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-A4 관리자 TBM 세션 취소 요청(사유 필수). sessionCd 는 path 에서 받는다. */
@Getter
@Setter
@NoArgsConstructor
public class AdminSessionCancelRequest {
    private String cancelReason;
}
