package com.prafta.web.notice.notice01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 비밀번호 검증 요청(수정 모드 진입용).
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticePwdRequest {
    private String noticeId;
    private String editPwd;
}
