package com.prafta.app.safety.history.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 내 안전활동 이력 합본 행(점검/위험성 공통 표시 계약) — prafta-app-025 J1-10 B-6.
 *
 * <p>두 도메인(순회점검/위험성평가)을 단일 리스트로 제공하기 위한 평면 표시 DTO.
 *    프론트는 type 으로 배지/아이콘을 분기하고 title/subText/displayDate 를 그대로 렌더한다.
 *    occurredDate(YYYY-MM-DD)는 병합 정렬 키이자 표시 날짜. 사진은 filePath(서버 서빙 경로) 사용.
 */
@Getter
@Builder
public class MySafetyHistoryItem {

    /** 'INSPECT'(순회점검) | 'RISK'(위험성평가). */
    private String type;

    /** 안정 정렬/식별용 키(type + 도메인 키 조합). */
    private String key;

    /** 표시 제목(점검=체크포인트/항목, 위험성=공정/위험요인). */
    private String title;

    /** 보조 텍스트(점검=결과 O/X + 비고, 위험성=상태명 + 위험성 Lv). */
    private String subText;

    /** 표시 날짜(YYYY-MM-DD). */
    private String displayDate;

    /** 병합 정렬 키(YYYY-MM-DD; 위험성은 분단위가 있으나 정렬은 일 단위 동률 후 type 안정 정렬). */
    private String occurredDate;

    /** 첨부 사진 서빙 경로(없으면 null). */
    private String filePath;
}
