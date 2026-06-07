package com.prafta.app.tbm.tbm01.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import com.prafta.app.tbm.tbm01.application.command.TbmEnterCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmExitCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmLeaveBeforeCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmWithdrawCommand;
import com.prafta.app.tbm.tbm01.application.query.TbmDetailQuery;
import com.prafta.app.tbm.tbm01.application.query.TbmSessionListQuery;
import com.prafta.app.tbm.tbm01.application.query.TbmSessionQuery;
import com.prafta.app.tbm.tbm01.result.TbmAttendanceResult;
import com.prafta.app.tbm.tbm01.result.TbmAttendeeResult;
import com.prafta.app.tbm.tbm01.result.TbmCompletionResult;
import com.prafta.app.tbm.tbm01.result.TbmContentItemResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionContentResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionListResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionRiskResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionStateResult;

/**
 * prafta-app-004-C: 앱 TBM 입실/종료(tbm01) Mapper.
 * <p>cmpnyCd 는 token 출처(IDOR 차단). 출결은 본인(userCd)+REGULAR 만 조회/갱신.
 */
@Mapper
public interface AppTbm01Mapper {

    /** 세션 단건 조회(회사 스코프). 입실 검증/컨텍스트 공용. */
    TbmSessionResult selectSession(TbmSessionQuery query);

    /** 본인 출결 단건 조회(세션+REGULAR+userCd). 기입실/미종료 판정. */
    TbmAttendanceResult selectMyAttendance(TbmSessionQuery query);

    /** 출결 INSERT(입실). ATTENDANCE_CD 채번은 SQL 내부. UNIQUE 충돌 시 호출부에서 멱등 처리. */
    int insertAttendance(TbmEnterCommand command);

    /** 출결 UPDATE(종료). 본인+미종료만 갱신. 영향 행수로 성공 판정. */
    int updateExit(TbmExitCommand command);

    // -------------------------------------------------------------------------
    // prafta-app-tbm: 사용자 앱 TBM 허브 조회/액션 (A1~A10)
    // -------------------------------------------------------------------------

    /** A1: 참석가능(OPENED) 세션 리스트(회사+사업장 스코프). */
    List<TbmSessionListResult> selectAvailableSessions(TbmSessionListQuery query);

    /** A2: 교육중(IN_PROGRESS) + 내 출결 존재 세션 리스트. */
    List<TbmSessionListResult> selectInProgressSessions(TbmSessionListQuery query);

    /** A3: 교육완료(내 출결 존재 AND (세션 COMPLETED OR 내 EXIT_AT 존재)) 세션 리스트. */
    List<TbmSessionListResult> selectCompletedSessions(TbmSessionListQuery query);

    /** A4: 참석자 리스트(이름+입실시각, PII 최소). 세션 스코프(회사+사업장)로 제한. */
    List<TbmAttendeeResult> selectSessionAttendees(TbmDetailQuery query);

    /** A5: 세션 상태(STATUS_CD + STARTED_AT/ENDED_AT + 동기화상태 LEFT JOIN). */
    TbmSessionStateResult selectSessionState(TbmDetailQuery query);

    /** A6: 교육내용+자료 묶음 헤더(콘텐츠 ⨝ 자료마스터). */
    List<TbmSessionContentResult> selectSessionContents(TbmDetailQuery query);

    /** A6: 자료 묶음 세부항목(TB_TBM_EDU_MTRL_ITEM, USE_YN='Y'). 세션의 전체 묶음 항목 일괄 조회. */
    List<TbmContentItemResult> selectSessionContentItems(TbmDetailQuery query);

    /** A6/A10: 세션 교육내용 본문(CONTENT_BODY) 단건. */
    String selectSessionContentBody(TbmDetailQuery query);

    /** A7/A10: 연계 위험성평가 리스트(web selectSessionRisks 포팅). */
    List<TbmSessionRiskResult> selectSessionRisks(TbmDetailQuery query);

    /** A10: 완료 상세 헤더(title/contentBody/이수상태/서명파일코드/종료일시). */
    TbmCompletionResult selectMyCompletion(TbmDetailQuery query);

    /** A10: 자료 묶음 제목 목록(명만). */
    List<String> selectSessionMaterialTitles(TbmDetailQuery query);

    /** A8: 시작전 퇴실(출결 물리 삭제). 본인+미종료만. 영향 행수로 멱등 판정. */
    int deleteAttendance(TbmLeaveBeforeCommand command);

    /** A9: 중도퇴실(미이수 종료 UPDATE). 본인+미종료만. 영향 행수로 멱등 판정. */
    int updateWithdraw(TbmWithdrawCommand command);

    /**
     * prafta-app-022-6: 본인 진행중(입실O·미종료) TBM 출결의 SESSION_CD 목록.
     *
     * <p>진행중 = ENTRY_AT IS NOT NULL AND EXIT_AT IS NULL. 본인(REGULAR+USER_CD) + DEL_YN='N',
     * 사업장(siteCd) 스코프(세션 사업장 EXISTS)로 제한한다. 퇴근 시 진행중 세션을 자동 중도퇴실하기
     * 위한 조회. 진행중 세션이 없으면 빈 목록(no-op).
     *
     * @param query 회사/사업장/userCd 스코프(TbmSessionListQuery, sessionCd 미사용)
     */
    List<String> selectInProgressAttendanceSessionCds(TbmSessionListQuery query);
}
