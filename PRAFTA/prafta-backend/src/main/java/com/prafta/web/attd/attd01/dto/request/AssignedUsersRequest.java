package com.prafta.web.attd.attd01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** F-12-2: 근무타입별 배정현황 조회 요청. schCd 는 경로변수로 별도 전달된다. */
@Getter
@Setter
@NoArgsConstructor
public class AssignedUsersRequest{
	private String siteCd;
}
