package com.prafta.web.notice.notice01.service;

import com.prafta.web.notice.notice01.application.param.NoticeAckParam;
import com.prafta.web.notice.notice01.application.param.NoticeDeleteParam;
import com.prafta.web.notice.notice01.application.param.NoticeFileDlParam;
import com.prafta.web.notice.notice01.application.param.NoticeInfoParam;
import com.prafta.web.notice.notice01.application.param.NoticeListParam;
import com.prafta.web.notice.notice01.application.param.NoticePopupParam;
import com.prafta.web.notice.notice01.application.param.NoticePwdParam;
import com.prafta.web.notice.notice01.application.param.NoticeSaveParam;
import com.prafta.web.notice.notice01.application.param.NoticeFileUploadParam;
import com.prafta.web.notice.notice01.application.param.NoticeScopeParam;
import com.prafta.web.notice.notice01.dto.response.NoticeDetailResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeFileDlResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeFileUploadResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeListResponse;
import com.prafta.web.notice.notice01.dto.response.NoticePopupResponse;
import com.prafta.web.notice.notice01.dto.response.NoticePwdResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeSaveResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeScopeResponse;
import com.prafta.web.notice.notice01.result.NoticeFileResult;

public interface Notice01Service {

    // ── 047-3 관리 ────────────────────────────────────────────
    NoticeListResponse selectNoticeList(NoticeListParam param);

    NoticeDetailResponse selectNoticeInfo(NoticeInfoParam param);

    NoticeSaveResponse saveNotice(NoticeSaveParam param);

    void updateNotice(NoticeSaveParam param);

    NoticePwdResponse verifyPwd(NoticePwdParam param);

    void deleteNotice(NoticeDeleteParam param);

    NoticeScopeResponse selectScopeTree(NoticeScopeParam param);

    /**
     * 공지 첨부 단건 업로드(save-notice 선행). 확장자 화이트리스트 검증 + 디스크 저장 +
     * tb_file_info 선저장 후 발급된 FILE_MGMT_CD 를 반환한다(공통 FileService 재사용).
     */
    NoticeFileUploadResponse uploadFile(NoticeFileUploadParam param);

    // ── 047-4 노출/ACK/다운로드 ───────────────────────────────
    NoticePopupResponse selectPopupNotices(NoticePopupParam param);

    void ackConfirm(NoticeAckParam param);

    void ackSnooze(NoticeAckParam param);

    void read(NoticeAckParam param);

    NoticeFileDlResponse issueFileDownloadToken(NoticeFileDlParam param);

    /** 다운로드 토큰 검증 후 첨부 메타 반환(스트림용). 토큰 scope/claim 검증은 호출 측에서 선행. */
    NoticeFileResult resolveDownloadFile(String cmpnyCd, String noticeId, String fileMgmtCd);
}
