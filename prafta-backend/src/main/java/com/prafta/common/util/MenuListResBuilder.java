package com.prafta.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.prafta.common.cmm.baseinfo.dto.response.MenuListResponse;
import com.prafta.common.cmm.baseinfo.result.MenuInfoResult;
import com.prafta.common.cmm.baseinfo.vo.Buttons;
import com.prafta.common.cmm.baseinfo.vo.SideMenu;
import com.prafta.common.cmm.baseinfo.vo.TopMenu;

public class MenuListResBuilder {

    public static MenuListResponse build(
            List<MenuInfoResult> menuInfoList,
            Function<String, String> topLabelResolver
    ) {
        if (menuInfoList == null || menuInfoList.isEmpty()) {
//            return new MenuListRes(Collections.emptyList(), Collections.emptyMap());
        	return MenuListResponse.builder()
                    .topMenus(Collections.emptyList())
                    .sideMenus(Collections.emptyMap())
                    .build();
        }

        // 1) keyId로 그룹핑 (순서 유지하려면 LinkedHashMap)
        Map<String, List<MenuInfoResult>> byKey = menuInfoList.stream()
                .filter(m -> !isEmpty(m.getKeyId()))
                .collect(Collectors.groupingBy(
                        MenuInfoResult::getKeyId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 2) 탑 메뉴 구성 — TB_SYST_MENU_M.MENU_IDX(숫자) 오름차순, 동률이면 keyId.
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

        // 3) 사이드 메뉴 구성
        // topMenus 순서와 맞추고 싶으면, topMenus 기준으로 key를 순회하는 방식을 추천
        Map<String, List<SideMenu>> sideMenus = new LinkedHashMap<>();

        for (TopMenu tm : topMenus) {
            String keyId = tm.getId();
            List<MenuInfoResult> rows = byKey.getOrDefault(keyId, Collections.emptyList());

            // 그룹 내부 정렬 — TB_SYST_MENU_D.MENU_IDX(숫자) 오름차순, 동률이면 id.
            //    (id는 'MENU_M_ID_MENU_IDX' 문자열이라 문자열 정렬 시 10 < 2 로 깨짐 → 숫자 정렬)
            rows.sort(Comparator
                    .comparingInt((MenuInfoResult m) -> nz(m.getMenuSubIdx()))
                    .thenComparing(m -> nullToEmpty(m.getId())));

            int base = Math.abs(keyId.hashCode() % 100) * 100; // key별 100단위 블록
            int seq = 1;

            List<SideMenu> items = new ArrayList<>();
            for (MenuInfoResult m : rows) {
                String itemId = nullToEmpty(m.getId());
                if (isEmpty(itemId)) {
                    itemId = String.valueOf(base + seq++);
                }

                Buttons btn = Buttons.builder()
                        .search(yn(m.getBtnSrch()))
                        .create(yn(m.getBtnNew()))
                        .delete(yn(m.getBtnDelt()))
                        .save(yn(m.getBtnSave()))
                        .excel(yn(m.getBtnExcl()))
                        .build();

                items.add(SideMenu.builder()
                        .id(itemId)
                        .label(nullToEmpty(m.getSideMenu())) // sideMenu 컬럼이 사이드 메뉴명
                        .route(nullToEmpty(m.getRoute()))
                        .buttons(btn)
                        .build());
            }

            sideMenus.put(keyId, items);
        }

//        return new MenuListRes(topMenus, sideMenus);
        return MenuListResponse.builder()
                .topMenus(topMenus)
                .sideMenus(sideMenus)
                .build();
    }

    // ----- helpers
    /** MENU_IDX null이면 맨 뒤로 보내기 위한 치환값. */
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
