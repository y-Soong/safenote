package com.prafta.app.selfjoin.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 관리자 모드 — 셀프가입 승인 대기 목록 조회 요청.
 *
 * <p>회사코드/권한/요청자는 담지 않는다 — 전부 JWT 클레임에서만 도출한다(cross-tenant 방지).
 *
 * <p>웹 {@code SelfJoinListRequest} 와 달리 <b>상태 필터(accountStatus)를 받지 않는다.</b>
 * 앱 대기 탭은 '06'(가입승인대기) 고정이며, 클라이언트가 '07'(거부)을 실어 거부 계정 목록(PII)을
 * 열람하는 경로를 아예 만들지 않는다. 상태값은 컨트롤러가 서버 상수로 채운다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppSelfJoinListRequest {

    /** 조회 사업장 (선택 — 공백이면 컨트롤러가 토큰 사업장으로 폴백). */
    private String siteCd;

    /** 소속부서 (선택 — 비전사 역할은 서버 게이트가 필수로 강제한다). */
    private String nodeCd;

    /** 하위부서 포함 여부 (Y/N, 기본 N). */
    private String incSubNodeYn;

    /** 사용자ID·이름 통합 검색어 (선택). */
    private String userKeyword;
}
