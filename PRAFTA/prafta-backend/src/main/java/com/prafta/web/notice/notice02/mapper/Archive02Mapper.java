package com.prafta.web.notice.notice02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.notice.notice02.application.command.ArchiveDeleteCommand;
import com.prafta.web.notice.notice02.application.command.ArchiveFileSaveCommand;
import com.prafta.web.notice.notice02.application.command.ArchiveSaveCommand;
import com.prafta.web.notice.notice02.application.command.ArchiveUpdateCommand;
import com.prafta.web.notice.notice02.application.query.ArchiveFileQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveIdSeqQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveInfoQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveListQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveTypeQuery;
import com.prafta.web.notice.notice02.result.ArchiveFileResult;
import com.prafta.web.notice.notice02.result.ArchiveResult;
import com.prafta.web.notice.notice02.result.ArchiveTypeResult;

/**
 * 자료실(Archive) 매퍼 (PRAFTA-053). tb_notice 공유 + NOTICE_TYPE='ARCHIVE' 스코프를
 * 전 쿼리에 예외 없이 박는다. 공지(Notice01Mapper)와 분리하여 타입 누수를 구조적으로 막는다.
 */
@Mapper
public interface Archive02Mapper {

    // ── 자료타입 드롭다운 (053-3) ─────────────────────────────
    // tb_baim_val_d (CMPNY_CD, BAIM_VAL_CD=<상수>, USE_YN='Y') ORDER BY SORT_IDX
    List<ArchiveTypeResult> selectArchiveTypeList(ArchiveTypeQuery query);

    // 자료타입 코드 유효성 카운트. forCreate=true 면 USE_YN='Y' 만(신규 생성 거부 정책), false 면 존재만.
    int countArchiveTypeValid(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("baimValCd") String baimValCd
        , @Param("archiveTypeCd") String archiveTypeCd
        , @Param("forCreate") boolean forCreate
    );

    // ── CRUD (053-4) ──────────────────────────────────────────
    // 목록 (자료타입/등록월/키워드 검색, NOTICE_TYPE='ARCHIVE'). 최신순
    List<ArchiveResult> selectArchiveList(ArchiveListQuery query);

    // 단건 마스터 (NOTICE_TYPE='ARCHIVE')
    ArchiveResult selectArchiveOne(ArchiveInfoQuery query);

    // 단건 첨부 list (tb_notice_file + tb_file_info, ARCHIVE 스코프 조인)
    List<ArchiveFileResult> selectArchiveFileList(ArchiveInfoQuery query);

    // 채번: 'A' + YYYYMMDD + 3자리 SEQ (회사+당일 MAX+1, NOTICE_TYPE='ARCHIVE')
    String selectNextArchiveId(ArchiveIdSeqQuery query);

    // 마스터 INSERT (강제값 리터럴 고정)
    int insertArchive(ArchiveSaveCommand command);

    // 마스터 UPDATE (자료타입/제목/내용만, EDIT_PWD 미변경, ARCHIVE 스코프)
    int updateArchive(ArchiveUpdateCommand command);

    // 첨부 다건 INSERT (foreach)
    int insertArchiveFiles(ArchiveFileSaveCommand command);

    // 첨부 매핑 전체 삭제 (수정 재설정용, 물리 삭제. tb_file_info 는 유지)
    int deleteArchiveFileByNoticeId(ArchiveInfoQuery query);

    // 논리삭제 (DEL_YN='Y', ARCHIVE 스코프)
    int updateArchiveDelYn(ArchiveDeleteCommand command);

    // 비번 해시 조회 (검증용, ARCHIVE 스코프). 없으면 null
    String selectArchivePwdHash(ArchiveInfoQuery query);

    // 수정/삭제 전 존재 판정용 단건 마스터 (ARCHIVE 스코프). 없으면 null
    ArchiveResult selectArchiveForUpdate(ArchiveInfoQuery query);

    // 첨부 단건 조회 (토큰 발급/스트림용, ARCHIVE 스코프 조인)
    ArchiveFileResult selectArchiveFileOne(ArchiveFileQuery query);

    // 다운로드 존재검증 (자료실=회사 전체 공통이라 대상 재귀 불필요). 1 이상이면 접근 가능
    int countArchiveVisible(ArchiveFileQuery query);

    // ── 등록 권한(전사 노드관리자 판정) ───────────────────────
    // 전사(사업장 무관) 노드 정/부 관리자 존재 카운트(등록/첨부업로드 게이트용).
    // access-context AppAdminAccessMapper.existsNodeAdminAnySite 와 동일 의미(TB_SITE_NODE, CMPNY_CD 스코프).
    int countNodeAdminAnySite(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
    );
}
