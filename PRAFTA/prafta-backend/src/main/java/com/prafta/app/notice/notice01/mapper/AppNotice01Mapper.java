package com.prafta.app.notice.notice01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.notice.notice01.application.command.AppNoticeAckCommand;
import com.prafta.app.notice.notice01.application.query.AppNoticeFileQuery;
import com.prafta.app.notice.notice01.application.query.AppNoticeInfoQuery;
import com.prafta.app.notice.notice01.application.query.AppNoticePopupQuery;
import com.prafta.app.notice.notice01.result.AppMyNoticeResult;
import com.prafta.app.notice.notice01.result.AppNoticeFileResult;
import com.prafta.app.notice.notice01.result.AppNoticePopupResult;
import com.prafta.app.notice.notice01.result.AppNoticeResult;

/**
 * 앱 공지 표시/ACK/다운로드 매퍼 (prafta-app-023-1).
 *
 * <p>웹 Notice01Mapper.xml 의 검증된 표시계열 SQL(selectPopupNoticeList / upsertNoticeRead /
 *    upsertNoticeAck / selectNoticeOne / selectNoticeFileList / countNoticeVisibleToUser /
 *    selectNoticeFileOne)을 앱 매퍼 XML 로 복제(web→app SQL 복제, 런타임 호출 금지).
 * <p>신규 쿼리 2개: selectMyNoticeList / countMyUnreadNotice (§0.2 근거).
 */
@Mapper
public interface AppNotice01Mapper {

    // 수신자가 일용직인지 확인 (tb_daily_user, USE_YN='Y'). 1 이상이면 일용직
    int countDailyUser(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
    );

    // 로그인 팝업 노출 공지 list (§6: 대상매칭 + 팝업게이트 + 이력제외). LIMIT 10 + 정렬 §5
    List<AppNoticePopupResult> selectPopupNoticeList(AppNoticePopupQuery query);

    // [신규] 내 공지 목록 (대상매칭 + 팝업게이트 제거, DEL_YN='N', 본인 ACK LEFT JOIN isUnread/isImportant). 정렬 §5
    List<AppMyNoticeResult> selectMyNoticeList(AppNoticePopupQuery query);

    // [신규] 내 미열람 공지 카운트 (동일 모수에서 LAST_READ_DATE IS NULL COUNT, Q2)
    int countMyUnreadNotice(AppNoticePopupQuery query);

    // 단건 상세 마스터 (본인 뱃지 포함)
    AppNoticeResult selectNoticeOne(AppNoticeInfoQuery query);

    // 단건 첨부 list (tb_notice_file + tb_file_info)
    List<AppNoticeFileResult> selectNoticeFileList(AppNoticeInfoQuery query);

    // 노출 대상 재검증 (§0.3 / 047-001). 1 이상이면 요청자가 해당 공지의 노출 대상
    int countNoticeVisibleToUser(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("noticeId") String noticeId
        , @Param("curSiteCd") String curSiteCd
        , @Param("curNodeCd") String curNodeCd
        , @Param("isDaily") boolean isDaily
    );

    // 공지 등록자(INSERT_NO) 조회 (발행자 본인 통과 판정용). 없으면 null
    String selectNoticeInsertNo(
        @Param("cmpnyCd") String cmpnyCd, @Param("noticeId") String noticeId);

    // 확인/숨김 UPSERT (CONFIRMED/SNOOZED)
    int upsertNoticeAck(AppNoticeAckCommand command);

    // 열람(read) UPSERT — LAST_READ_DATE 만 갱신/생성(ACK_TYPE 보존)
    int upsertNoticeRead(AppNoticeAckCommand command);

    // 한시숨김 가드용 마스터 단건(PIN_YN 판정). 없으면 null
    AppNoticeResult selectNoticeForSnooze(AppNoticeInfoQuery query);

    // 첨부 단건 조회 (토큰 발급용, tb_notice_file + tb_file_info)
    AppNoticeFileResult selectNoticeFileOne(AppNoticeFileQuery query);
}
