package com.prafta.app.notiset.notiset01.service;

import com.prafta.app.notiset.notiset01.application.param.PushSettingQueryParam;
import com.prafta.app.notiset.notiset01.application.param.PushSettingSaveParam;
import com.prafta.app.notiset.notiset01.dto.response.PushSettingResponse;

/**
 * 푸시 알림 수신 설정 서비스 (PRAFTA-APP-021-1, 앱).
 *
 * <p>본인(USER_CD=JWT) 설정만 조회/저장한다(IDOR 차단). opt-out 모델:
 * 미설정 토글은 ON 으로 채워 응답하고, 저장 시 토글의 ON/OFF 를 매핑 NOTI_TYPE 행으로 전개한다.
 */
public interface Notiset01Service {

    /** 본인 푸시 설정 + isAdmin 조회(미설정=ON 보정, isAdmin 시 관리자 토글 포함). */
    PushSettingResponse getMySettings(PushSettingQueryParam param);

    /** 본인 푸시 설정 저장(마스터 + 토글 일괄 upsert). 갱신 후 최신 설정 응답 반환. */
    PushSettingResponse saveMySettings(PushSettingSaveParam param);
}
