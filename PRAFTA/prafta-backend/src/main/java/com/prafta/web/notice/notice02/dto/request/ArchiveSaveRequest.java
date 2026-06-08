package com.prafta.web.notice.notice02.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자료실 생성/수정 요청.
 * 생성 시 noticeId 는 무시(서버 채번), 수정 시 noticeId 필수.
 * editPwd 는 생성 시 필수(BCrypt 해시 저장), 수정 시 검증용(master 면제).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 *
 * <p>자료실 서버 강제값(NOTICE_TYPE/POPUP_YN/PIN_YN/TARGET_SCOPE 등)은 클라 입력을 받지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ArchiveSaveRequest {
    private String noticeId;        // 수정 시만
    private String archiveTypeCd;   // 자료타입(필수)
    private String title;           // 제목(필수)
    private String content;         // 내용
    private String editPwd;         // 생성 시 신규 비번 / 수정 시 검증 비번

    private List<ArchiveFileRequest> fileList; // 첨부 다건
}
