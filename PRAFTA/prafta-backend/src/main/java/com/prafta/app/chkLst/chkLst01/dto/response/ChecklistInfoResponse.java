package com.prafta.app.chkLst.chkLst01.dto.response;

import java.util.List;

import com.prafta.app.chkLst.chkLst01.result.ChecklistInfoResult;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-036-B1: 체크리스트 정보 조회 응답.
 * <p>응답 키 보존: 'checklistInfos' (앱 FE ChkLst.vue 호출부 의존).
 * <p>prafta-app-011: 체크포인트 컨텍스트(checkpoint) 객체 추가.
 *   FE 는 checkpoint.chkptNm / checkpoint.siteNm / checkpoint.totalCount 를 컨텍스트 카드에 표시.
 */
@Getter
@Builder
public class ChecklistInfoResponse {

    /** 기존 FE 호환 — 점검 항목 리스트 */
    private final List<ChecklistInfoResult> checklistInfos;

    /** prafta-app-011 신규 — 체크포인트 컨텍스트 (FE 컨텍스트 카드용) */
    private final CheckpointContextResponse checkpoint;
}
