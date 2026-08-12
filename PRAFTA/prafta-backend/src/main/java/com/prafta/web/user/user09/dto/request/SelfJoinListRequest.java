package com.prafta.web.user.user09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소정-09: 셀프가입 신청 목록 조회 요청 (User_09).
 *
 * <p>회사코드/권한/요청자는 담지 않는다 — 전부 JWT 클레임에서만 도출한다(cross-tenant 방지).
 */
@Getter
@Setter
@NoArgsConstructor
public class SelfJoinListRequest {

    /** 조회 사업장 (필수) */
    private String siteCd;

    /** 소속부서 (선택 — 비전사 역할은 서버 게이트가 필수로 강제한다) */
    private String nodeCd;

    /** 하위부서 포함 여부 (Y/N, 기본 N) */
    private String incSubNodeYn;

    /** 상태 필터 — '06' 승인대기(기본) / '07' 거부 */
    private String accountStatus;

    /** 사용자ID·이름 통합 검색어 (선택) */
    private String userKeyword;
}
