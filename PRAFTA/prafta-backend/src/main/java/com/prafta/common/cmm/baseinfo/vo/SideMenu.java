package com.prafta.common.cmm.baseinfo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SideMenu {
    private String id;     // 화면 고유 ID(숫자/문자 상관 X)
    private String label;  // ex) "사용자관리", "사업장관리"
    private String route;  // ex) "User_01", "Baim_01"
    private Buttons buttons;
    // 즐겨찾기 별표 상태(사용자별). 프론트 고정 계약 키 "isFavorite" 보장을 위해 @JsonProperty 명시.
    @JsonProperty("isFavorite")
    private boolean favorite;
}