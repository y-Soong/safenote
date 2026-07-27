package com.prafta.app.dailycontract.dailycontract01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 본인 서명 계약서 메타 응답 (GET /appApi/dailycontract01/my-sign — UI-DC-04 메타 카드).
 *
 * <p>signYn='N' 이면 서명본 없음(빈 상태 화면). 파일 경로는 응답에 포함하지 않는다 —
 * 서명본은 스트림 EP 로만 열람/저장한다(교부 의무 §6-1).
 *
 * <p>멀티페이지 지원(T4)으로 {@code formatType}/{@code pageCount} 를 추가했다. 신규 앱은
 * {@code GET my-sign-page?page=N} 으로 페이지를 나열하고 저장은 {@code GET my-sign-file}(원본 PDF)을
 * 사용한다. 구버전 앱은 두 필드를 무시하고 기존 {@code my-sign-image}(폴백 PNG)를 계속 쓴다.
 */
@Value
@Builder
public class MySignResponse {
    String signYn;          // 'Y' | 'N'
    String signId;
    Integer contractVer;
    String firstWorkDate;   // YYYYMMDD
    String signDtime;       // YYYY-MM-DD HH:mm:ss
    String formatType;      // 'PDF' | 'IMG' (서명본 파일 유실 시 null)
    Integer pageCount;      // 서명본 페이지 수(레거시 PNG=1, 파일 유실 시 null)
}
