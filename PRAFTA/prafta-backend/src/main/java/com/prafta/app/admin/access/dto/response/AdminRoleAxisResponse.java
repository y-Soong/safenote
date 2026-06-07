package com.prafta.app.admin.access.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P1-B1: 진입판정(access-context)의 역할코드 축(roleAxis).
 *
 * <p>[권한매트릭스 §0] 관리자는 "역할코드 축"(TB_USER.AUTH_CD)과 "노드관계 축"(TB_SITE_NODE 정/부관리자)의
 * 2축으로 구성된다. 본 응답은 그중 역할코드 축을 표현한다. 판정은 {@code AuthRoleUtils} 상수 비교로만 한다(DB 추가조회 없음).
 *
 * <p>JSON 키는 계약(plan §3-0)에 맞춰 {@code isMaster/isHr/isSafe} 로 고정한다(Jackson boolean is-prefix 변형 방지).
 */
@Getter
@Builder
public class AdminRoleAxisResponse {

    /** TB_USER.AUTH_CD 원본(master/hr/safe/999999). */
    private final String authCd;

    /** 회사 마스터 여부. */
    @JsonProperty("isMaster")
    private final boolean master;

    /** HR 담당자 여부. */
    @JsonProperty("isHr")
    private final boolean hr;

    /** 산업안전관리자 여부. */
    @JsonProperty("isSafe")
    private final boolean safe;
}
