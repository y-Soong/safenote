package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 단순 상태 전이 요청(prafta-051). sessionCd 만으로 식별되는 전이/재발급에 공용 사용.
 *
 * <p>대상: 교육시작(start-session) / 교육준비 연장(extend-prep) /
 * 교육종료(complete-session) / 종료비밀번호 재발급(regenerate-exit-password).
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionTransitionRequest {
	private String sessionCd;
}
