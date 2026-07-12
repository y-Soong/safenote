package com.prafta.platform.company.application.command;

/**
 * 신규 고객사 기본 근무타입(TB_SCH_MGMT, ST001) 시드 INSERT 커맨드.
 *
 * <p>1구간(SCH_TYPE='01') 09:00~18:00 표준 근무타입 1건만 시드한다(SCH_CD='ST001' 고정).
 * Attd01(근무타입 관리) INSERT 패턴을 참고하되, 시드에 필요한 최소 컬럼만 채운다.
 */
public record WorktypeSeedCommand(
    String cmpnyCd
    , String siteCd
    , String schCd
    , String schNo
    , String schType
    , String applyDate
    , String fstSchStrTime
    , String fstSchEndTime
    , String insertNo
) {
}
