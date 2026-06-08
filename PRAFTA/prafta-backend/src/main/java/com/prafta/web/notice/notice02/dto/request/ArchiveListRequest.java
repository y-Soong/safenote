package com.prafta.web.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자료실 목록 조회 요청(검색조건). 조건 3개: 자료타입 + 등록월 + 제목/내용 키워드.
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단). 사업장/소속부서 조건 없음(회사 전체 공통).
 */
@Getter
@Setter
@NoArgsConstructor
public class ArchiveListRequest {
    private String archiveTypeCd;  // 자료타입 필터(빈문자/누락 시 전체)
    private String titleKeyword;   // 제목/내용 통합 키워드
    private String startDate;      // 등록기간 시작 YYYY-MM-DD (등록월→월초)
    private String endDate;        // 등록기간 종료 YYYY-MM-DD (등록월→월말)
}
