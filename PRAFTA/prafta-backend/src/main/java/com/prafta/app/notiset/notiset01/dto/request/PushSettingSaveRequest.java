package com.prafta.app.notiset.notiset01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 푸시 설정 저장(PUT) 요청 바디 (PRAFTA-APP-021-1).
 *
 * <p>형태: { "masterOn": true, "items": [{ "toggleKey": "W1_LEAVE_RECALL", "on": false }, ...] }.
 *
 * <p>USER_CD 는 본문이 아니라 JWT 클레임(gv_userCd)에서만 도출한다(IDOR 차단 — Param 단계).
 * 관리자 전용 토글/읽기전용 항목은 서버 화이트리스트({@code PushNotiTypeConst.isSavableToggle})로
 * 무시/거부한다(클라가 임의로 보내도 저장되지 않음).
 */
@Getter
@Setter
public class PushSettingSaveRequest {

    /** 마스터 스위치 ON 여부(전체 수신 차단/허용). null 이면 미변경(저장 생략). */
    private Boolean masterOn;

    /** 토글별 ON/OFF 변경 목록(부분/전체 모두 허용). null/빈 목록이면 토글 변경 없음. */
    private List<PushSettingToggleItem> items;

    /** 토글 1건. */
    @Getter
    @Setter
    public static class PushSettingToggleItem {
        /** 토글키(PushNotiTypeConst 의 W/M 키). */
        private String toggleKey;
        /** 수신 여부(true=ON, false=OFF). null 이면 해당 항목 무시. */
        private Boolean on;
    }
}
