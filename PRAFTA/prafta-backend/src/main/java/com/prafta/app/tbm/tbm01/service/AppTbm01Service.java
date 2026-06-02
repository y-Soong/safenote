package com.prafta.app.tbm.tbm01.service;

import com.prafta.app.tbm.tbm01.application.param.TbmEnterParam;
import com.prafta.app.tbm.tbm01.application.param.TbmEntryContextParam;
import com.prafta.app.tbm.tbm01.application.param.TbmExitParam;
import com.prafta.app.tbm.tbm01.dto.response.TbmEnterResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEntryContextResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmExitResponse;

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
}
