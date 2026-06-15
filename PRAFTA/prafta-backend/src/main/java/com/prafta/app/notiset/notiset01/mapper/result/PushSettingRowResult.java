package com.prafta.app.notiset.notiset01.mapper.result;

/**
 * tb_user_push_setting 1행 조회 결과 (PRAFTA-APP-021-1).
 *
 * <p>설정 조회 시 사용자의 저장된 행(NOTI_TYPE + USE_YN)을 운반한다. 마스터 행은
 * NOTI_TYPE='__MASTER__'. 행이 없는 NOTI_TYPE 은 opt-out 으로 ON 간주(응답 단계 보정).
 *
 * @param notiType 알림 유형[SYS045] 또는 마스터 키
 * @param useYn    수신 여부 'Y'/'N'
 */
public record PushSettingRowResult(
    String notiType
    , String useYn
){
}
