package com.prafta.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.prafta.common.cmm.baseinfo.dto.response.MenuListResponse;
import com.prafta.common.cmm.baseinfo.result.MenuInfoResult;
import com.prafta.common.cmm.baseinfo.vo.Buttons;
import com.prafta.common.cmm.baseinfo.vo.SideMenu;
import com.prafta.common.cmm.baseinfo.vo.SideMenuGroup;
import com.prafta.common.cmm.baseinfo.vo.TopMenu;

/**
 * 사용자 메뉴 조회 결과(평면 행 목록)를 LNB 응답(대분류 탭 + 탭별 중분류 그룹)으로 변환한다.
 *
 * <p>LNB 재편(lnb-restructure) 이후 응답 계약(프론트 고정):
 * <pre>
 *   sideMenus[topMenuId] = [
 *     { subGroupNm, subGroupIdx, items: [ { id, label, route, buttons, isFavorite } ] }
 *   ]
 * </pre>
 * - subGroupNm 이 NULL/빈값이면 해당 탭의 MENU_M.MENU_NM(대분류명)으로 대체(D4).
 * - 정렬: subGroupIdx ASC → 그룹 내 MENU_IDX(menuSubIdx) ASC.
 * - topMenus 는 USE_YN='Y' 인 대분류 탭을 MENU_IDX 순으로 자동 반영.
 */
public class MenuListResBuilder {

    /** 즐겨찾기 정보 없이 호출하는 하위 호환 진입점(전부 isFavorite=false). */
    public static MenuListResponse build(
            List<MenuInfoResult> menuInfoList,
            Function<String, String> topLabelResolver
    ) {
        return build(menuInfoList, topLabelResolver, Collections.emptySet());
    }

    /**
     * @param menuInfoList     권한 필터링된 메뉴 행 목록(BaseinfoMapper.selectMenuList 결과).
     * @param topLabelResolver keyId(MENU_M_ID) -> 대분류 라벨 외부 해석기(없으면 null 반환 가능).
     * @param favoriteMenuDIds 사용자별 즐겨찾기 MENU_D_ID 집합(없으면 빈 집합).
     */
    public static MenuListResponse build(
            List<MenuInfoResult> menuInfoList,
            Function<String, String> topLabelResolver,
            Set<String> favoriteMenuDIds
    ) {
        if (menuInfoList == null || menuInfoList.isEmpty()) {
            return MenuListResponse.builder()
                    .topMenus(Collections.emptyList())
                    .sideMenus(Collections.emptyMap())
                    .build();
        }

        final Set<String> favorites = (favoriteMenuDIds == null)
                ? Collections.emptySet() : favoriteMenuDIds;

        // 1) keyId(MENU_M_ID)로 그룹핑 (순서 유지 위해 LinkedHashMap)
        Map<String, List<MenuInfoResult>> byKey = menuInfoList.stream()
                .filter(m -> !isEmpty(m.getKeyId()))
                .collect(Collectors.groupingBy(
                        MenuInfoResult::getKeyId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 2) 대분류(탭) 구성 — MENU_M.MENU_IDX 오름차순, 동률이면 keyId.
        //    매핑 전에 정렬해 그 순서를 그대로 결과 리스트에 보존한다(매핑 후 재정렬 금지).
        List<TopMenu> topMenus = byKey.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, List<MenuInfoResult>> e) -> nz(e.getValue().get(0).getMenuIdx()))
                        .thenComparing(Map.Entry::getKey))
                .map(e -> {
                    String keyId = e.getKey();
                    MenuInfoResult first = e.getValue().get(0);

                    // label 우선순위: 외부 resolver > MenuInfo.topMenuNm > keyId
                    String label = null;
                    if (topLabelResolver != null) {
                        label = topLabelResolver.apply(keyId);
                    }
                    if (isEmpty(label)) label = first.getTopMenuNm();
                    if (isEmpty(label)) label = keyId;

                    return TopMenu.builder()
                            .id(keyId)
                            .label(label)
                            .build();
                })
                .collect(Collectors.toList());

        // 3) 사이드 메뉴 구성 — 탭별 중분류 그룹 리스트.
        //    topMenus 순서대로 순회하여 응답 순서를 탭 순서와 일치시킨다.
        Map<String, List<SideMenuGroup>> sideMenus = new LinkedHashMap<>();

        for (TopMenu tm : topMenus) {
            String keyId = tm.getId();
            String topLabel = tm.getLabel(); // D4 fallback 용 대분류명
            List<MenuInfoResult> rows = byKey.getOrDefault(keyId, Collections.emptyList());

            // 중분류로 그룹핑 전, 전체 행을 (subGroupIdx, menuSubIdx) 순으로 정렬한다.
            //   - subGroupIdx ASC : 중분류 순서
            //   - menuSubIdx(MENU_IDX) ASC : 그룹 내 메뉴 순서
            //   - route(MENU_D_ID) : 동률 안정 정렬
            List<MenuInfoResult> sortedRows = new ArrayList<>(rows);
            sortedRows.sort(Comparator
                    .comparingInt((MenuInfoResult m) -> nz(m.getSubGroupIdx()))
                    .thenComparingInt(m -> nz(m.getMenuSubIdx()))
                    .thenComparing(m -> nullToEmpty(m.getRoute())));

            // subGroupIdx 기준 그룹핑(정렬된 순서 보존을 위해 LinkedHashMap).
            Map<Integer, List<MenuInfoResult>> bySubGroup = new LinkedHashMap<>();
            for (MenuInfoResult m : sortedRows) {
                bySubGroup
                        .computeIfAbsent(nz(m.getSubGroupIdx()), k -> new ArrayList<>())
                        .add(m);
            }

            List<SideMenuGroup> groups = new ArrayList<>();
            for (Map.Entry<Integer, List<MenuInfoResult>> ge : bySubGroup.entrySet()) {
                List<MenuInfoResult> groupRows = ge.getValue();
                MenuInfoResult head = groupRows.get(0);

                // 중분류명: SUB_GROUP_NM 이 비었으면 대분류명으로 대체(D4).
                String subGroupNm = nullToEmpty(head.getSubGroupNm());
                if (isEmpty(subGroupNm)) {
                    subGroupNm = topLabel;
                }

                List<SideMenu> items = new ArrayList<>();
                for (MenuInfoResult m : groupRows) {
                    String route = nullToEmpty(m.getRoute());

                    Buttons btn = Buttons.builder()
                            .search(yn(m.getBtnSrch()))
                            .create(yn(m.getBtnNew()))
                            .delete(yn(m.getBtnDelt()))
                            .save(yn(m.getBtnSave()))
                            .excel(yn(m.getBtnExcl()))
                            .build();

                    items.add(SideMenu.builder()
                            .id(nullToEmpty(m.getId()))
                            .label(nullToEmpty(m.getSideMenu())) // sideMenu 컬럼이 사이드 메뉴명
                            .route(route)
                            .buttons(btn)
                            .favorite(favorites.contains(route)) // route == MENU_D_ID
                            .build());
                }

                groups.add(SideMenuGroup.builder()
                        .subGroupNm(subGroupNm)
                        .subGroupIdx(head.getSubGroupIdx())
                        .items(items)
                        .build());
            }

            sideMenus.put(keyId, groups);
        }

        return MenuListResponse.builder()
                .topMenus(topMenus)
                .sideMenus(sideMenus)
                .build();
    }

    // ----- helpers
    /** 정렬 키 null 치환값(맨 뒤로). */
    private static int nz(Integer v) {
        return v == null ? Integer.MAX_VALUE : v;
    }

    private static String yn(String v) {
        if (v == null) return "N";
        String t = v.trim().toUpperCase(Locale.ROOT);
        return "Y".equals(t) ? "Y" : "N";
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
