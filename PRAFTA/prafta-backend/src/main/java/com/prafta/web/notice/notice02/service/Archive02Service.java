package com.prafta.web.notice.notice02.service;

import com.prafta.web.notice.notice02.application.param.ArchiveDeleteParam;
import com.prafta.web.notice.notice02.application.param.ArchiveFileDlParam;
import com.prafta.web.notice.notice02.application.param.ArchiveFileUploadParam;
import com.prafta.web.notice.notice02.application.param.ArchiveInfoParam;
import com.prafta.web.notice.notice02.application.param.ArchiveListParam;
import com.prafta.web.notice.notice02.application.param.ArchivePwdParam;
import com.prafta.web.notice.notice02.application.param.ArchiveSaveParam;
import com.prafta.web.notice.notice02.application.param.ArchiveTypeParam;
import com.prafta.web.notice.notice02.dto.response.ArchiveDetailResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveFileDlResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveFileUploadResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveListResponse;
import com.prafta.web.notice.notice02.dto.response.ArchivePwdResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveSaveResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveTypeResponse;
import com.prafta.web.notice.notice02.result.ArchiveFileResult;

/**
 * 자료실(Archive) 서비스 (PRAFTA-053).
 */
public interface Archive02Service {

    // ── 053-3 자료타입 드롭다운 ───────────────────────────────
    ArchiveTypeResponse selectArchiveTypes(ArchiveTypeParam param);

    // ── 053-4 CRUD ────────────────────────────────────────────
    ArchiveListResponse selectArchiveList(ArchiveListParam param);

    ArchiveDetailResponse selectArchiveInfo(ArchiveInfoParam param);

    ArchiveSaveResponse saveArchive(ArchiveSaveParam param);

    void updateArchive(ArchiveSaveParam param);

    ArchivePwdResponse verifyPwd(ArchivePwdParam param);

    void deleteArchive(ArchiveDeleteParam param);

    /** 첨부 단건 업로드(save-archive 선행). FileService 재사용 후 FILE_MGMT_CD 반환. */
    ArchiveFileUploadResponse uploadFile(ArchiveFileUploadParam param);

    ArchiveFileDlResponse issueFileDownloadToken(ArchiveFileDlParam param);

    /** 다운로드 토큰 검증 후 첨부 메타 반환(스트림용). 토큰 scope/claim 검증은 호출 측에서 선행. */
    ArchiveFileResult resolveDownloadFile(String cmpnyCd, String noticeId, String fileMgmtCd);
}
