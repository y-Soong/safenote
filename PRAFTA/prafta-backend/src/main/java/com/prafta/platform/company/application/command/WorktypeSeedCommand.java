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
    // 휴게(선택 — Platform_01 입력 확장 2026-08-17): 분(varchar)·시작/종료 HHMM. 미입력이면 null(휴게 없음).
    , String fstSchBrkMin
    , String fstBrkStrTime
    , String fstBrkEndTime
    , String insertNo
) {
}
