package com.prafta.web.baim.baim07.dto.response;

import com.prafta.common.cmm.leave.vo.LeavePolicyVO;

import lombok.Builder;
import lombok.Value;

/**
 * 활성 정책 단건 응답.
 *
 * <p>응답 컨테이너로 LeavePolicyVO를 그대로 노출한다. 별도 가공이 필요해지면
 * 본 클래스에 매핑 로직을 추가한다.
 */
@Value
@Builder
public class LeavePolicyResponse {

    LeavePolicyVO policy;
}
