package com.prafta.app.req.req06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-006: 본인 요청 목록 조회 Request.
 *
 * <p>모든 파라미터는 선택. 식별값(cmpnyCd/siteCd/userCd)은 토큰에서만 도출하므로 본 DTO 에 포함하지 않는다(IDOR).
 * <p>limit 는 서버 고정 20 — 클라가 보내도 무시.
 */
@Getter
@Setter
@NoArgsConstructor
public class MyReqListRequest {

    /** REQ_TYPE 다중 (쉼표 구분). 기본 6종 전체. 07/08/09 시스템 코드는 서버가 fail-closed 로 거부. */
    private String reqTypes;

    /** REQ_STATUS 다중 (쉼표 구분). 기본 4종 전체. */
    private String reqStatuses;

    /** 대상일자(WORK_YMD) 범위 시작 (YYYYMMDD). */
    private String targetYmdFrom;

    /** 대상일자(WORK_YMD) 범위 종료 (YYYYMMDD). */
    private String targetYmdTo;

    /** 정렬 옵션: PENDING_FIRST / RECENT / TARGET_DATE. 기본 PENDING_FIRST. */
    private String sort;

    /** 페이지 오프셋. 기본 0. */
    private Integer offset;
}
