package com.prafta.common.cmm.baseinfo.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
public class UserIdDupleCheckResponse {
	private String uniqueYn;
}
