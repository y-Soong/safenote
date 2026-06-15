package com.prafta.app.notice.notice02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.notice.notice02.application.command.AppArchiveFileSaveCommand;
import com.prafta.app.notice.notice02.application.command.AppArchiveSaveCommand;
import com.prafta.app.notice.notice02.application.query.AppArchiveFileQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveIdSeqQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveInfoQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveListQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveTypeQuery;
import com.prafta.app.notice.notice02.result.AppArchiveFileResult;
import com.prafta.app.notice.notice02.result.AppArchiveResult;
import com.prafta.app.notice.notice02.result.AppArchiveTypeResult;

/**
 * 앱 자료실(Archive) 매퍼 (prafta-app-025 J1-8).
 *
 * <p>앱 완전분리: 웹 Archive02Mapper 를 호출하지 않고 자기 매퍼만 사용한다(app-023 분리원칙).
 * SQL 은 웹 Archive02Mapper.xml 의 조회/등록 쿼리를 복제하되, NOTICE_TYPE='ARCHIVE' 스코프를
 * 전 쿼리에 박는다(공지 누수 차단). 수정/삭제/비번검증 쿼리는 복제하지 않는다(웹 위임).
 */
@Mapper
public interface AppArchive02Mapper {

    // ── 조회 ──────────────────────────────────────────────
    /** 자료타입 드롭다운(COM008, USE_YN='Y'). */
    List<AppArchiveTypeResult> selectArchiveTypeList(AppArchiveTypeQuery query);

    /** 자료타입 코드 유효성 카운트(forCreate=true: 존재 + USE_YN='Y'). */
    int countArchiveTypeValid(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("baimValCd") String baimValCd
        , @Param("archiveTypeCd") String archiveTypeCd
        , @Param("forCreate") boolean forCreate);

    /** 목록(자료타입/등록월→기간/키워드 검색, ARCHIVE 스코프, 최신순). */
    List<AppArchiveResult> selectArchiveList(AppArchiveListQuery query);

    /** 단건 마스터(ARCHIVE 스코프). */
    AppArchiveResult selectArchiveOne(AppArchiveInfoQuery query);

    /** 단건 첨부 list(ARCHIVE 스코프 조인). */
    List<AppArchiveFileResult> selectArchiveFileList(AppArchiveInfoQuery query);

    /** 첨부 단건 조회(토큰 발급/스트림용, ARCHIVE 스코프 조인). */
    AppArchiveFileResult selectArchiveFileOne(AppArchiveFileQuery query);

    /** 다운로드 존재검증(존재 + CMPNY + ARCHIVE + DEL_YN='N'). */
    int countArchiveVisible(AppArchiveFileQuery query);

    // ── 등록 ──────────────────────────────────────────────
    /** 채번('A' + YYYYMMDD + SEQ, ARCHIVE 스코프). */
    String selectNextArchiveId(AppArchiveIdSeqQuery query);

    /** 마스터 INSERT(강제값 리터럴 고정). */
    int insertArchive(AppArchiveSaveCommand command);

    /** 첨부 다건 INSERT(foreach). */
    int insertArchiveFiles(AppArchiveFileSaveCommand command);

    // ── 등록 권한(전사 노드관리자 판정) ───────────────────────
    /**
     * 전사(사업장 무관) 노드 정/부 관리자 존재 카운트(등록 게이트용).
     * <p>access-context의 existsNodeAdminAnySite 와 동일 의미(TB_SITE_NODE MAIN_ADMIN_CD/SUB_ADMIN_CD,
     * CMPNY_CD 스코프). 자료실=회사 전체 공통이라 특정 사업장 한정이 아닌 전사 기준으로 판정한다.
     */
    int countNodeAdminAnySite(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd);
}
