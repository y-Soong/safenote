package com.prafta.app.notice.notice02.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 자료실 생성 요청. cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 * editPwd 는 생성 시 필수(BCrypt 해시 저장, master 포함).
 *
 * <p>자료실 서버 강제값(NOTICE_TYPE/POPUP_YN/PIN_YN/TARGET_SCOPE 등)은 클라 입력을 받지 않는다.
 * 앱은 등록(생성)까지만 — 수정/삭제는 웹 위임(noticeId 입력 없음).
 */
@Getter
@Setter
@NoArgsConstructor
public class AppArchiveSaveRequest {
    private String archiveTypeCd;   // 자료타입(필수)
    private String title;           // 제목(필수)
    private String content;         // 내용
    private String editPwd;         // 신규 비번(필수)

    private List<AppArchiveFileRequest> fileList; // 첨부 다건
}
