package com.prafta.common.cmm.baseinfo.dto;

import java.util.List;
import java.util.Map;

import com.prafta.common.cmm.baseinfo.vo.SideMenu;
import com.prafta.common.cmm.baseinfo.vo.TopMenu;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MenuListRes {
	// 첫번째 변수
    List<TopMenu> topMenus;
    // 두번째 변수
    Map<String, List<SideMenu>> sideMenus; // key = KEY_ID("user","baim"...)
}
