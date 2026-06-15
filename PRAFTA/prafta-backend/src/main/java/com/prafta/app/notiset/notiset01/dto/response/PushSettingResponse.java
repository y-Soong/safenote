package com.prafta.app.notiset.notiset01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * 푸시 설정 조회(GET) 응답 (PRAFTA-APP-021-1).
 *
 * <p>형태: { "masterOn": true, "isAdmin": false, "items": [{ toggleKey, group, on, savable }, ...] }.
 *
 * <p>opt-out 정합: 설정 행이 없는 토글은 on=true 로 채워 내린다(미설정=수신).
 * isAdmin=false 면 items 에 관리자 토글(M*)을 포함하지 않는다.
 *
 * <p>{@code isAdmin} 은 boolean 이므로 Lombok/Jackson 의 is-접두 탈락(isAdmin→admin) 함정을 피하기
 * 위해 {@link JsonProperty}("isAdmin") 으로 키를 고정한다(메모리 feedback_lombok_jackson_boolean_is_prefix).
 */
@Getter
@Builder
public class PushSettingResponse {

    /** 마스터 스위치 ON 여부(행 없음/'__MASTER__'!='N' → true). */
    private final boolean masterOn;

    /** 관리자 토글 노출 대상 여부(노드 main/sub 관리자 또는 master/hr/safe). */
    @JsonProperty("isAdmin")
    private final boolean isAdmin;

    /** 토글 항목 목록(노출 순서 보존). */
    private final List<PushSettingToggleResponse> items;

    /** 토글 1건 응답. */
    @Getter
    @Builder
    public static class PushSettingToggleResponse {
        /** 토글키(W/M/R 그룹). */
        private final String toggleKey;
        /** 토글 그룹(WORKER/ADMIN/READONLY). FE 가 섹션 분류/disabled 판단에 사용. */
        private final String group;
        /** 현재 수신 여부(매핑 NOTI_TYPE 중 하나라도 OFF 면 false, 미설정이면 true). */
        private final boolean on;
        /** 사용자가 끌 수 있는 항목인지(READONLY 면 false, 관리자 토글은 isAdmin 시 true). */
        private final boolean savable;
    }
}
