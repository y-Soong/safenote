package com.prafta.web.notice.notice01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.notice.notice01.application.command.NoticeAckCommand;
import com.prafta.web.notice.notice01.application.command.NoticeDeleteCommand;
import com.prafta.web.notice.notice01.application.command.NoticeFileSaveCommand;
import com.prafta.web.notice.notice01.application.command.NoticeSaveCommand;
import com.prafta.web.notice.notice01.application.command.NoticeTargetSaveCommand;
import com.prafta.web.notice.notice01.application.command.NoticeUpdateCommand;
import com.prafta.web.notice.notice01.application.query.NoticeFileQuery;
import com.prafta.web.notice.notice01.application.query.NoticeIdSeqQuery;
import com.prafta.web.notice.notice01.application.query.NoticeInfoQuery;
import com.prafta.web.notice.notice01.application.query.NoticeListQuery;
import com.prafta.web.notice.notice01.application.query.NoticePopupQuery;
import com.prafta.web.notice.notice01.application.query.NoticeScopeQuery;
import com.prafta.web.notice.notice01.result.NoticeFileResult;
import com.prafta.web.notice.notice01.result.NoticePopupResult;
import com.prafta.web.notice.notice01.result.NoticeResult;
import com.prafta.web.notice.notice01.result.NoticeScopeResult;
import com.prafta.web.notice.notice01.result.NoticeTargetResult;

@Mapper
public interface Notice01Mapper {

    // ── 권한/스코프 검증 ───────────────────────────────────────
    // 사업장 접근 권한 확인 (tb_user_site_auth, USE_YN='Y'). 1 이상이면 접근 가능
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // 노드가 발행자 소속 노드(또는 그 자손)인지 확인 (소속 사업장, 재귀 CTE). 1 이상이면 스코프 내
    int countNodeInSelfSubtree(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("rootNodeCd") String rootNodeCd
        , @Param("targetNodeCd") String targetNodeCd
    );

    // 노드가 해당 사업장에 실재하는지 확인 (권한 보유 사업장 전 노드 허용 판정용). 1 이상이면 존재
    int countNodeInSite(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nodeCd") String nodeCd
    );

    // 수신자(또는 발행자)가 일용직인지 확인 (tb_daily_user, USE_YN='Y'). 1 이상이면 일용직
    int countDailyUser(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
    );

    // ── 047-3 관리 CRUD ───────────────────────────────────────
    // 관리 목록 (검색 + 본인 뱃지 LEFT JOIN ack). 정렬 §5
    List<NoticeResult> selectNoticeList(NoticeListQuery query);

    // 단건 상세 마스터 (본인 뱃지 포함)
    NoticeResult selectNoticeOne(NoticeInfoQuery query);

    // 단건 첨부 list (tb_notice_file + tb_file_info)
    List<NoticeFileResult> selectNoticeFileList(NoticeInfoQuery query);

    // 단건 대상 list (tb_notice_target + 사업장/노드명)
    List<NoticeTargetResult> selectNoticeTargetList(NoticeInfoQuery query);

    // 채번: N + YYYYMMDD + 3자리 SEQ (회사+당일 MAX+1)
    String selectNextNoticeId(NoticeIdSeqQuery query);

    // 마스터 INSERT
    int insertNotice(NoticeSaveCommand command);

    // 마스터 UPDATE (EDIT_PWD 미변경)
    int updateNotice(NoticeUpdateCommand command);

    // 대상 다건 INSERT (foreach, TARGET_SEQ = index+1)
    int insertNoticeTargets(NoticeTargetSaveCommand command);

    // 첨부 다건 INSERT (foreach)
    int insertNoticeFiles(NoticeFileSaveCommand command);

    // 대상 전체 삭제 (수정 시 재설정용, 물리 삭제)
    int deleteNoticeTargetByNoticeId(
        @Param("cmpnyCd") String cmpnyCd, @Param("noticeId") String noticeId);

    // 첨부 매핑 전체 삭제 (수정 시 재설정용, 물리 삭제. tb_file_info 는 유지)
    int deleteNoticeFileByNoticeId(
        @Param("cmpnyCd") String cmpnyCd, @Param("noticeId") String noticeId);

    // 논리삭제 (DEL_YN='Y')
    int updateNoticeDelYn(NoticeDeleteCommand command);

    // 비번 해시 조회 (검증용). 없으면 null
    String selectNoticePwdHash(NoticeInfoQuery query);

    // 수정 정책용: 마스터 단건(상태 판정 — popupYn/from/to/pinYn/delYn). 없으면 null
    NoticeResult selectNoticeForUpdate(NoticeInfoQuery query);

    // ── PIN 정규화 (§5) ───────────────────────────────────────
    // 현재 고정 공지 수 (CMPNY_CD 단위, FOR UPDATE 로 잠금)
    int selectPinnedCountForUpdate(
        @Param("cmpnyCd") String cmpnyCd, @Param("excludeNoticeId") String excludeNoticeId);

    // 지정 순번 이상(>=) 고정 공지 +1 시프트
    int shiftPinOrderUp(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("fromOrder") int fromOrder
        , @Param("excludeNoticeId") String excludeNoticeId
        , @Param("gvUserCd") String gvUserCd
    );

    // 고정 공지 순번 1..M 재압축 (삭제/해제 후)
    int compactPinOrder(
        @Param("cmpnyCd") String cmpnyCd, @Param("gvUserCd") String gvUserCd);

    // ── 047-3 발행자 대상선택 트리 ─────────────────────────────
    // 발행자 스코프 사업장/노드 트리 (전사 / 권한보유사업장 / 소속사업장 자기노드+자손)
    List<NoticeScopeResult> selectScopeSiteNodeTree(NoticeScopeQuery query);

    // ── 047-4 노출/ACK/다운로드 ────────────────────────────────
    // 로그인 팝업 노출 공지 list (§6 의사쿼리, 대상매칭+이력제외). LIMIT 10 + 정렬 §5
    List<NoticePopupResult> selectPopupNoticeList(NoticePopupQuery query);

    // 확인/숨김 UPSERT (CONFIRMED/SNOOZED)
    int upsertNoticeAck(NoticeAckCommand command);

    // 열람(read) UPSERT — LAST_READ_DATE 만 갱신/생성(ACK_TYPE 보존)
    int upsertNoticeRead(NoticeAckCommand command);

    // 다운로드 발급 시 노출 대상 재검증 (047-001). 1 이상이면 요청자가 해당 공지의 노출 대상
    int countNoticeVisibleToUser(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("noticeId") String noticeId
        , @Param("curSiteCd") String curSiteCd
        , @Param("curNodeCd") String curNodeCd
        , @Param("isDaily") boolean isDaily
    );

    // 공지 등록자(INSERT_NO) 조회 (047-001, 발행자 본인 통과 판정용). 없으면 null
    String selectNoticeInsertNo(
        @Param("cmpnyCd") String cmpnyCd, @Param("noticeId") String noticeId);

    // PIN 직렬화 advisory 잠금 (BUG-3). tb_cmm_seq 행잠금으로 회사 단위 직렬화. 반환값 미사용
    String lockPinSerialization(@Param("cmpnyCd") String cmpnyCd);

    // 첨부 단건 조회 (토큰 발급/스트림용, tb_notice_file + tb_file_info)
    NoticeFileResult selectNoticeFileOne(NoticeFileQuery query);

    // ── 047-5 배치 ─────────────────────────────────────────────
    // 팝업 종료 6개월 경과 공지의 확인이력 물리삭제. 현행 노출중 공지 제외
    int deleteExpiredNoticeAck(@Param("cutoffYmd") String cutoffYmd);
}
