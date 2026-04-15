package com.prafta.web.user.user01.application.command;

public record WithdrawMyAccountCommand(
    String cmpnyCd,
    String userCd
) {}
