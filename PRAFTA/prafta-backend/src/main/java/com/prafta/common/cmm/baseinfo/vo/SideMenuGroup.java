package com.prafta.common.cmm.baseinfo.vo;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * LNB 중분류 그룹(accordion 그룹). 한 대분류(상단 탭) 아래에 N개의 중분류 그룹이 오고,
 * 각 그룹은 N개의 소분류 메뉴({@link SideMenu})를 가진다.
 *
 * <p>프론트 고정 계약: {@code { subGroupNm, subGroupIdx, items:[{id,label,route,buttons,isFavorite}] } }
 * (lnb-restructure-작업지시서 §5-1).
 */
@Value
@Builder
public class SideMenuGroup {
    /** 중분류명. TB_SYST_MENU_D.SUB_GROUP_NM 이 NULL/빈값이면 대분류명(MENU_M.MENU_NM)으로 대체(D4). */
    private String subGroupNm;
    /** 중분류 순서(탭 내 그룹 정렬). */
    private Integer subGroupIdx;
    /** 그룹 내 소분류 메뉴 목록. */
    private List<SideMenu> items;
}
