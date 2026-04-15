package com.prafta.web.user.user01.application.command;

public record ScheduleWithdrawalCommand(
    String cmpnyCd,
    String userCd,
    String withdrawalDate
) {}
