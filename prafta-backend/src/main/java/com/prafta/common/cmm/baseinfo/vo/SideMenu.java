package com.prafta.common.cmm.baseinfo.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SideMenu {
    private String id;     // 화면 고유 ID(숫자/문자 상관 X)
    private String label;  // ex) "사용자관리", "사업장관리"
    private String route;  // ex) "User_01", "Baim_01"
    private Buttons buttons;
}