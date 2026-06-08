package com.prafta.web.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자료실 삭제(논리삭제) 요청. editPwd 검증(master 면제).
 */
@Getter
@Setter
@NoArgsConstructor
public class ArchiveDeleteRequest {
    private String noticeId;
    private String editPwd;
}
