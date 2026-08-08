package com.prafta.web.user.user01.dto;

import java.util.List;

/**
 * 일괄 처리 실패 항목.
 *
 * <p>{@code sourceRow} 는 prafta-052 — 엑셀 업로드 실패 행의 원본 입력값(양식 컬럼 순서, 현행 13컬럼)이다.
 * 그리드 다중체크 저장 경로(updateUserInfoBatch)에는 원본 양식 행이 없으므로 null 로 둔다(하위호환).
 * 엑셀 동기/비동기 업로드 경로에서만 채워진다.
 * Jackson 기본 직렬화/역직렬화로 failsJson 라운드트립에서 보존된다.
 * 구버전 failsJson(sourceRow 키 없음) 역직렬화 시에는 null 로 채워진다.
 */
public record UserUpdateFailItem(
        int index,          // 몇 번째 요청인지(0-based)
        String errorItem,
        String errorCode,
        String message,
        List<String> sourceRow   // prafta-052: 엑셀 양식 원본값(현행 13컬럼). 그리드 경로는 null.
) {}
