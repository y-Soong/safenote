package com.prafta.app.selfjoin.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 관리자 모드 — 셀프가입 처리 이력 목록 조회 요청(무한 스크롤).
 *
 * <p>회사코드/권한/요청자는 담지 않는다 — 전부 JWT 클레임에서만 도출한다(cross-tenant 방지).
 *
 * <p>{@code actionType} 화이트리스트, {@code pageSize} 상한(100), 날짜 형식 검증은
 * 웹 {@code SelfJoinHistoryListParam.from} 이 단일 출처로 수행한다. 컨트롤러는 본 DTO 값을
 * 웹 Request 로 옮겨 담아 그 팩토리를 호출한다(검증 우회 금지).
 */
@Getter
@Setter
@NoArgsConstructor
public class AppSelfJoinHistoryRequest {

    /** 조회 사업장 (선택 — 공백이면 컨트롤러가 토큰 사업장으로 폴백). */
    private String siteCd;

    /** 소속부서 (선택 — 비전사 역할은 서버 게이트가 필수로 강제한다). */
    private String nodeCd;

    /** 하위부서 포함 여부 (Y/N, 기본 N). */
    private String incSubNodeYn;

    /** 사용자ID·이름 통합 검색어 (선택). */
    private String userKeyword;

    /** 처리 결과 필터 (선택) — 'APPROVE' 승인 / 'REJECT' 거부 / 미지정=전체. */
    private String actionType;

    /** 처리일시 시작일 (선택, yyyy-MM-dd). 앱 v1 은 UI 미노출이나 계약은 유지한다. */
    private String startDate;

    /** 처리일시 종료일 (선택, yyyy-MM-dd). 앱 v1 은 UI 미노출이나 계약은 유지한다. */
    private String endDate;

    /** 페이지 번호 (1-base, 기본 1). */
    private Integer page;

    /** 페이지 크기 (기본 20, 상한 100 — 서버 강제). */
    private Integer pageSize;
}
