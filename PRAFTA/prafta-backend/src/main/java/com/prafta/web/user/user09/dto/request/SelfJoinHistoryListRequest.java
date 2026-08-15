package com.prafta.web.user.user09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소정-09: 셀프가입 처리 이력 목록 조회 요청 (User_09 처리 이력 탭).
 *
 * <p>회사코드/권한/요청자는 담지 않는다 — 전부 JWT 클레임에서만 도출한다(cross-tenant 방지).
 *
 * <p>대기 목록({@link SelfJoinListRequest})과 달리 상태 필터가 없다. 이력의 구분 축은 계정 상태가
 * 아니라 <b>감사 로그의 처리 결과</b>(승인/거부)다 — 승인 즉시 계정 상태가 일반 재직자('01')와
 * 같아져 TB_USER 만으로는 승인 건을 구분할 수 없기 때문이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SelfJoinHistoryListRequest {

    /** 조회 사업장 (필수) */
    private String siteCd;

    /** 소속부서 (선택 — 비전사 역할은 서버 게이트가 필수로 강제한다) */
    private String nodeCd;

    /** 하위부서 포함 여부 (Y/N, 기본 N) */
    private String incSubNodeYn;

    /** 사용자ID·이름 통합 검색어 (선택) */
    private String userKeyword;

    /** 처리 결과 필터 (선택) — 'APPROVE' 승인 / 'REJECT' 거부 / 미지정=전체 */
    private String actionType;

    /** 처리일시 시작일 (선택, yyyy-MM-dd) */
    private String startDate;

    /** 처리일시 종료일 (선택, yyyy-MM-dd) */
    private String endDate;

    /** 페이지 번호 (1-base, 기본 1) */
    private Integer page;

    /** 페이지 크기 (기본 20, 상한 100) */
    private Integer pageSize;
}
