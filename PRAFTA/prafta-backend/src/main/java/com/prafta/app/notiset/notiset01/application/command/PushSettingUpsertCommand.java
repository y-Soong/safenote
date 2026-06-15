package com.prafta.app.notiset.notiset01.application.command;

/**
 * 푸시 설정 1행 upsert Command (PRAFTA-APP-021-1).
 *
 * <p>tb_user_push_setting 의 PK(CMPNY_CD, USER_CD, NOTI_TYPE) 단위 upsert.
 * NOTI_TYPE 은 SYS045 코드 또는 마스터 키('__MASTER__'). USE_YN 은 'Y'/'N'.
 */
public record PushSettingUpsertCommand(
    String cmpnyCd
    , String userCd
    , String notiType
    , String useYn
    , String actor
){
    public static PushSettingUpsertCommand of(String cmpnyCd, String userCd,
                                              String notiType, boolean on, String actor) {
        return new PushSettingUpsertCommand(cmpnyCd, userCd, notiType, on ? "Y" : "N", actor);
    }
}
