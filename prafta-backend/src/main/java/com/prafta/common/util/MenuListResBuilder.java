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

import com.prafta.common.cmm.baseinfo.dto.MenuListRes;
import com.prafta.common.cmm.baseinfo.vo.Buttons;
import com.prafta.common.cmm.baseinfo.vo.MenuInfo;
import com.prafta.common.cmm.baseinfo.vo.SideMenu;
import com.prafta.common.cmm.baseinfo.vo.TopMenu;

public class MenuListResBuilder {

    public static MenuListRes build(
            List<MenuInfo> menuInfoList,
            Function<String, String> topLabelResolver
    ) {
        if (menuInfoList == null || menuInfoList.isEmpty()) {
//            return new MenuListRes(Collections.emptyList(), Collections.emptyMap());
        	return MenuListRes.builder()
                    .topMenus(Collections.emptyList())
                    .sideMenus(Collections.emptyMap())
                    .build();
        }

        // 1) keyId로 그룹핑 (순서 유지하려면 LinkedHashMap)
        Map<String, List<MenuInfo>> byKey = menuInfoList.stream()
                .filter(m -> !isEmpty(m.getKeyId()))
                .collect(Collectors.groupingBy(
                        MenuInfo::getKeyId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 2) 탑 메뉴 구성
        List<TopMenu> topMenus = byKey.entrySet().stream()
                .map(e -> {
                    String keyId = e.getKey();
                    MenuInfo first = e.getValue().get(0);

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
                // 필요하면 정렬 규칙 유지/변경 가능
                .sorted(Comparator.comparing(TopMenu::getId))
                .collect(Collectors.toList());

        // 3) 사이드 메뉴 구성
        // topMenus 순서와 맞추고 싶으면, topMenus 기준으로 key를 순회하는 방식을 추천
        Map<String, List<SideMenu>> sideMenus = new LinkedHashMap<>();

        for (TopMenu tm : topMenus) {
            String keyId = tm.getId();
            List<MenuInfo> rows = byKey.getOrDefault(keyId, Collections.emptyList());

            // 그룹 내부 정렬 (id가 숫자 문자열이면 숫자정렬로 바꾸는 것도 가능)
            rows.sort(Comparator.comparing(m -> nullToEmpty(m.getId())));

            int base = Math.abs(keyId.hashCode() % 100) * 100; // key별 100단위 블록
            int seq = 1;

            List<SideMenu> items = new ArrayList<>();
            for (MenuInfo m : rows) {
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
        return MenuListRes.builder()
                .topMenus(topMenus)
                .sideMenus(sideMenus)
                .build();
    }

    // ----- helpers
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
