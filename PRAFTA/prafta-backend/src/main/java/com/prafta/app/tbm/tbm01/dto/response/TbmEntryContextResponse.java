package com.prafta.app.tbm.tbm01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-004-C3: TBM 입실 컨텍스트 응답.
 *
 * <p>입실 화면이 사전 분기(비번/GPS/종료서명 노출)에 사용한다.
 * <p>D5 정책: GPS 좌표 원본은 노출하지 않는다(검증유형/반경만 제공, 거리는 입실 시 응답).
 * <p>D1 정책: 종료 시에만 서명 필수 → requiresExitSignature=true 고정.
 */
@Getter
@Builder
public class TbmEntryContextResponse {
    private final String sessionCd;
    private final String title;
    private final String statusCd;             // SYS046
    private final String gpsVerifyTypeCd;      // SYS048 AUTO/MANUAL/DISABLED
    private final Integer gpsVerifyRadiusM;
    private final boolean entryAvailable;      // STATUS_CD='OPENED' (D3)
    private final boolean alreadyEntered;      // 본인 기입실 여부
    private final boolean requiresExitSignature; // D1: 종료 시 서명 필수(=true)
}
