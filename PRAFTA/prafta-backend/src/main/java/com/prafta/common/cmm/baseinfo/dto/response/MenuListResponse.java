package com.prafta.common.cmm.baseinfo.dto.response;

import java.util.List;
import java.util.Map;

import com.prafta.common.cmm.baseinfo.vo.SideMenuGroup;
import com.prafta.common.cmm.baseinfo.vo.TopMenu;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuListResponse {
	// 첫번째 변수 — 대분류(상단 탭) 목록
    List<TopMenu> topMenus;
    // 두번째 변수 — 탭별 중분류 그룹 목록.
    //   key = 대분류 MENU_M_ID("attdHr","safety","system","dailyAcct")
    //   value = 중분류 그룹 리스트(각 그룹은 subGroupNm/subGroupIdx/items)
    Map<String, List<SideMenuGroup>> sideMenus;
}
