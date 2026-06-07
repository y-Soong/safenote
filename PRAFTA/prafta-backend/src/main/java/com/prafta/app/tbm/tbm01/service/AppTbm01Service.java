package com.prafta.app.tbm.tbm01.service;

import com.prafta.app.tbm.tbm01.application.param.TbmEnterParam;
import com.prafta.app.tbm.tbm01.application.param.TbmEntryContextParam;
import com.prafta.app.tbm.tbm01.application.param.TbmExitParam;
import com.prafta.app.tbm.tbm01.application.param.TbmSessionDetailParam;
import com.prafta.app.tbm.tbm01.application.param.TbmSessionListParam;
import com.prafta.app.tbm.tbm01.dto.response.TbmActionResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmAttendeeListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmCompletionResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmContentResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEnterResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEntryContextResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmExitResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmRiskListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionStateResponse;
import com.prafta.common.dto.TokenInfo;

/**
 * prafta-app-004-C: 앱 TBM 입실/종료(tbm01) 서비스.
 * <p>정규직(REGULAR) MVP. @Transactional 은 구현체에 부여한다.
 */
public interface AppTbm01Service {

    /** C3: 입실 컨텍스트 조회(상태/GPS/기입실/종료서명요구). */
    TbmEntryContextResponse selectEntryContext(TbmEntryContextParam param);

    /** C1: 입실(비번/GPS/잠금 검증 + 출결 INSERT, UNIQUE 충돌 멱등). */
    TbmEnterResponse enter(TbmEnterParam param);

    /** C2: 종료(본인 입실 확인 + 비번/잠금 + 종료서명 업로드 + 출결 UPDATE). */
    TbmExitResponse exit(TbmExitParam param);

    // -------------------------------------------------------------------------
    // prafta-app-tbm: 사용자 앱 TBM 허브 조회/액션 (A1~A10)
    // -------------------------------------------------------------------------

    /** A1/A2/A3: 탭별 세션 리스트(AVAILABLE/IN_PROGRESS/COMPLETED). */
    TbmSessionListResponse selectSessions(TbmSessionListParam param);

    /** A4: 참석자 리스트(이름+입실시각, PII 최소). */
    TbmAttendeeListResponse selectAttendees(TbmSessionDetailParam param);

    /** A5: 세션 시작/종료 상태(STATUS_CD 판정). */
    TbmSessionStateResponse selectState(TbmSessionDetailParam param);

    /** A6: 교육내용 + 자료 묶음(≤3). */
    TbmContentResponse selectContent(TbmSessionDetailParam param);

    /** A7: 연계 위험성평가 리스트. */
    TbmRiskListResponse selectRisks(TbmSessionDetailParam param);

    /** A8: 시작전 퇴실(출결 취소, 물리 삭제). 멱등. */
    TbmActionResponse leaveBefore(TbmSessionDetailParam param);

    /** A9: 중도퇴실(미이수 종료). 멱등. */
    TbmActionResponse withdraw(TbmSessionDetailParam param);

    /** A10: 완료 상세(교육내용/자료명/위험성제목/서명파일코드). */
    TbmCompletionResponse selectMyCompletion(TbmSessionDetailParam param);

    /**
     * prafta-app-022-6: 본인 진행중(입실O·미종료) TBM 세션을 모두 중도퇴실(withdraw) 처리한다.
     *
     * <p>근무 퇴근 시 attd01 이 호출한다(attd01 → tbm01 단방향, 순환 금지). 기존 중도퇴실
     * 로직({@code updateWithdraw}, EXIT_TYPE_CD='SELF'·COMPLETION_STATUS_CD='NOT_COMPLETED')을
     * 세션별로 재사용한다. 진행중 세션이 없으면 no-op(멱등). 호출부(checkOut)의 트랜잭션 내에서
     * best-effort 로 호출되며, 실패가 퇴근 커밋을 막지 않도록 호출부에서 예외를 격리한다.
     *
     * @param token JWT 클레임 정보(cmpnyCd/siteCd/userCd 출처)
     * @return 중도퇴실 처리된 세션 수(이미 종료된 세션 제외)
     */
    int withdrawAllInProgress(TokenInfo token);
}
