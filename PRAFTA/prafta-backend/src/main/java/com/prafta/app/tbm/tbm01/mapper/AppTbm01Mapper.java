package com.prafta.app.tbm.tbm01.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.tbm.tbm01.application.command.TbmEnterCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmExitCommand;
import com.prafta.app.tbm.tbm01.application.command.TbmPwdFailCommand;
import com.prafta.app.tbm.tbm01.application.query.TbmPwdFailCountQuery;
import com.prafta.app.tbm.tbm01.application.query.TbmSessionQuery;
import com.prafta.app.tbm.tbm01.result.TbmAttendanceResult;
import com.prafta.app.tbm.tbm01.result.TbmSessionResult;

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

    /** 최근 N초 이내 비밀번호 실패 건수(D4 잠금 판정). */
    int countRecentPwdFail(TbmPwdFailCountQuery query);

    /** 비밀번호 실패 로그 INSERT(평문 미저장). */
    void insertPwdFail(TbmPwdFailCommand command);

    /** 비밀번호 성공 시 해당 세션/유형/시도자 실패 로그 정리(잠금 카운터 리셋). */
    void deletePwdFail(TbmPwdFailCommand command);

    /** 출결 INSERT(입실). ATTENDANCE_CD 채번은 SQL 내부. UNIQUE 충돌 시 호출부에서 멱등 처리. */
    int insertAttendance(TbmEnterCommand command);

    /** 출결 UPDATE(종료). 본인+미종료만 갱신. 영향 행수로 성공 판정. */
    int updateExit(TbmExitCommand command);
}
