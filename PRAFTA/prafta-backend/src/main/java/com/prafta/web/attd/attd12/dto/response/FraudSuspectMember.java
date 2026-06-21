package com.prafta.web.attd.attd12.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-016-F 9-1 - 공유 기기에 관여한 로그인 1건(사번/이름 + 그 기기에서의 로그인 시각·메타).
 *
 * <p>기기 중심 표시 모델: 하나의 FraudSuspectRow(기기) 아래에 여러 멤버(로그인)가 묶인다.
 *   같은 사용자가 같은 기기를 여러 번 로그인했으면 멤버가 여러 건일 수 있다(각 로그인 시각 노출).
 */
@Getter
@Builder
public class FraudSuspectMember {
    private final String userCd;
    private final String userId;
    private final String userNm;
    private final String loginDtime;   // YYYYMMDDHHMMSS (해당 기기에서의 APP 로그인 시각)
    private final String clientType;   // APP / WEB
}
