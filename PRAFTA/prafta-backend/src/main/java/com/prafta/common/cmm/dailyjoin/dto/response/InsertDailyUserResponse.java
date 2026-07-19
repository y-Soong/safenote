package com.prafta.common.cmm.dailyjoin.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 일일사용자 회원가입 응답.
 * 발급된 사용자ID와 승인대기 플래그를 반환한다.
 *
 * <p>pendingApprovalYn='Y' — 입장 승인제(D5/D6): 가입 직후 계정은 승인대기('04')이며
 * 관리자 승인 후 로그인 가능. 가입 완료 화면의 "승인 대기" 안내 문구 분기용(R4).
 */
@Value
@Builder
public class InsertDailyUserResponse {
    String userId;
    String pendingApprovalYn;
}
