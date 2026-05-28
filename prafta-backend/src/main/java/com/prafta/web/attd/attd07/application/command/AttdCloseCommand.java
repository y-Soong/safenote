package com.prafta.web.attd.attd07.application.command;

/**
 * 근태 마감/해제 upsert 입력 (prafta-019-C).
 *
 * @param actionType CLOSE 마감 / UNCLOSE 해제 (이력 기록용)
 */
public record AttdCloseCommand(
      String cmpnyCd
    , String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String closeYm
    , String userCd
    , String closeDesc
    , String actionType
) {
}
