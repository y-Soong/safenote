package com.prafta.web.attd.attd06.result;

public record ShiftTypeDetailListsResult(
	String cmpnyCd
    , String siteCd
    , String shiftCd
    , String teamIdx
    , String teamNm
    , String dayNo
    , String assignYn
    , String schCd
    , String schNo
    , String fstSchTime
    , String secSchTime
    , String schType

    /* PRAFTA-FIXEDOT-3(표기): 고정연장(전방/후방) 구간 — "HHMM-HHMM" 또는 null(고정연장 없음).
       ★ fstSchTime/secSchTime 에 섞지 않는다: FE 타임라인이 fstSchTime 을 slice(0,2) 로 "시"만
       잘라 쓰기 때문에 접두/접미를 붙이면 표기가 깨진다(qa 이관 사유).
       ⚠️ record 끝 = SELECT 끝 동일 순서(MyBatis 위치 기반 매핑, 중간 삽입 금지). */
    , String preFixedOtTime
    , String fixedOtTime
) {
}
