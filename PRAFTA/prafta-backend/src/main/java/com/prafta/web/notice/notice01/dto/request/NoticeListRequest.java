package com.prafta.web.notice.notice01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 관리 목록 조회 요청(검색조건).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeListRequest {
    private String titleKeyword; // 제목 키워드
    private String popupYn;      // 팝업여부 필터 Y/N
    private String pinYn;        // 고정여부 필터 Y/N
    private String startDate;    // 등록기간 시작 YYYY-MM-DD
    private String endDate;      // 등록기간 종료 YYYY-MM-DD
    private String siteCd;       // 대상 사업장 코드(검색조건). 빈문자/누락 시 필터 미적용
    private String nodeCd;       // 대상 노드(소속부서) 코드(검색조건). siteCd 동반 시에만 의미
}
