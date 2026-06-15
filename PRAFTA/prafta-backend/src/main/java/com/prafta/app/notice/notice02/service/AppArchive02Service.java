package com.prafta.app.notice.notice02.service;

import com.prafta.app.notice.notice02.application.param.AppArchiveFileDlParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveFileUploadParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveInfoParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveListParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveSaveParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveTypeParam;
import com.prafta.app.notice.notice02.dto.response.AppArchiveDetailResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveFileDlResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveFileUploadResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveListResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveSaveResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveTypeResponse;

/**
 * 앱 자료실(Archive) 서비스 (prafta-app-025 J1-8). 조회 + 등록(작성+첨부) 전용.
 * 수정/삭제는 웹 위임(미신설).
 */
public interface AppArchive02Service {

    /** 자료타입 드롭다운(COM008, USE_YN='Y'). */
    AppArchiveTypeResponse selectArchiveTypes(AppArchiveTypeParam param);

    /** 자료실 목록(자료타입/등록월/키워드 검색). */
    AppArchiveListResponse selectArchiveList(AppArchiveListParam param);

    /** 자료실 단건 상세(마스터 + 첨부 list). */
    AppArchiveDetailResponse selectArchiveInfo(AppArchiveInfoParam param);

    /** 자료실 첨부 단건 선업로드(역할 게이트 + FileService). */
    AppArchiveFileUploadResponse uploadFile(AppArchiveFileUploadParam param);

    /** 자료실 등록(역할 게이트 + 자료타입/비번 필수 + 채번 + 강제값 고정). */
    AppArchiveSaveResponse saveArchive(AppArchiveSaveParam param);

    /** 자료실 첨부 다운로드 단기 토큰 발급(scope=ARCHIVE_FILE_DL). */
    AppArchiveFileDlResponse issueFileDownloadToken(AppArchiveFileDlParam param);
}
