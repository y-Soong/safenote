package com.prafta.app.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 자료실 목록 조회 요청(검색조건). 자료타입 + 등록월 + 제목/내용 키워드.
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단). 사업장/소속부서 조건 없음(회사 전체 공통).
 *
 * <p>모바일 편의상 등록월은 단일 파라미터(registMonth, 'YYYY-MM' 또는 'YYYYMM')로 받아
 * 서버에서 월초/월말(startDate/endDate)로 변환한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppArchiveListRequest {
    private String archiveTypeCd;  // 자료타입 필터(빈문자/누락 시 전체)
    private String registMonth;    // 등록월 'YYYY-MM' 또는 'YYYYMM' (빈문자/누락 시 전체 기간)
    private String titleKeyword;   // 제목/내용 통합 키워드
}
