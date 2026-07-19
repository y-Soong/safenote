package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * E10 정규직 관리자 대리입실 요청 바디(prafta-051 R-B).
 *
 * <p>대상 정규직 USER_CD 만 받는다. 회사/사업장/관리자 식별자는 JWT 에서 도출하며, 대상이 토큰
 * 스코프(세션 사업장/노드) 내인지는 서버가 재검증한다(IDOR 차단). 비번/GPS 는 받지 않는다(D-4).
 */
@Getter
@Setter
public class AdminManagerDirectRequest {
    // PRAFTA-SUBCON-T5 F9: 앱 대리입실은 자사 대상 전용이다(요청서 §3.2 는 검색 직접입실의 대상 확장을
    // 웹으로만 명시, §4 는 대상 선택 UI 를 웹 전용으로 둠). 회사코드를 받지 않는다(공격 표면 제거).
    // 앱의 타사 참여 경로는 일용직 QR 입실(P4)이다.
    private String userCd;
}
