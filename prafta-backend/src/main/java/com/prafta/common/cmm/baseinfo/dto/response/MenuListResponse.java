package com.prafta.common.cmm.baseinfo.dto.response;

import java.util.List;
import java.util.Map;

import com.prafta.common.cmm.baseinfo.vo.SideMenu;
import com.prafta.common.cmm.baseinfo.vo.TopMenu;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuListResponse {
	// 첫번째 변수
    List<TopMenu> topMenus;
    // 두번째 변수
    Map<String, List<SideMenu>> sideMenus; // key = KEY_ID("user","baim"...)
}
