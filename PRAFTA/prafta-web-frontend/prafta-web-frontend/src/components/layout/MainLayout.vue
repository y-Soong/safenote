<template>
  <div class="main-layout">
    <!-- 좌측 사이드 메뉴 -->
    <div class="side-wrapper">
      <SideMenu
        :menus="sideMenus"
        :favorite-items="favoriteItems"
        :active-route="activeTab"
        @navigate="onNavigate"
        @toggle-favorite="onToggleFavorite"
      />
    </div>

    <!-- 우측 본문 전체 -->
    <div class="right-panel">
      <!-- 상단 네비게이션 -->
      <TopNav :menus="topMenus" @navigate="onNavigate" />

      <!-- 탭 영역 -->
      <div class="tabs">
        <div
          v-for="tab in tabs"
          :key="tab.route"
          :class="['tab-item', { active: tab.route === activeTab }]"
          @click="selectTab(tab.route)"
        >
          {{ tab.label }}
          <span
            v-if="tab.route !== '/safenote/main'"
            class="close-btn"
            @click.stop="closeTab(tab.route)"
          >
            ×
          </span>
        </div>

        <!-- 🔹 모든 탭 닫기 (홈 제외) 버튼 -->
        <div class="tab-item close-all" @click="closeAllTabsExceptHome">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="w-4 h-4"
            font-family="Pretendard, sans-serif"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </div>
      </div>

      <!-- 콘텐츠 영역 -->
      <main class="main-content">
        <!-- route 변수는 안 쓰므로 제거 -->
        <!-- PRAFTA-005 후속: 메뉴 로드/탭 구성 완료(ready) 전까지 렌더를 보류한다.
             새 탭 직진입 시 뷰가 먼저 렌더되어 공통 버튼 props(buttons)가 빈 채로
             고정되는 문제를 방지. -->
        <router-view v-if="ready" v-slot="{ Component }">
          <!-- 열린 탭들의 컴포넌트만 캐시 -->
          <keep-alive :include="cachedNames">
            <component :is="Component" v-bind="getActiveTabProps()" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  fnGetMenuList,
  fnGetFavorites,
  fnToggleFavorite,
} from "@/api/navigation";
import { useDashboardNavStore } from "@/stores/dashboardNavStore";
import { resolveApiErrorMessage } from "@/utils/apiError";
import TopNav from "@/components/layout/TopNav.vue";
import SideMenu from "@/components/layout/SideMenu.vue";

const router = useRouter();
const route = useRoute();
const dashNav = useDashboardNavStore();

const topMenus = ref([]);
const sideMenus = ref([]);
const allSideMenus = ref({});
const selectedTopMenuId = ref(null);

/* 전역 즐겨찾기 menuDId 집합(탭 무관, 사용자별 영속).
   식별자는 MENU_D_ID(= menu.route)로 통일한다(BE favorites/토글 EP 키계와 동일). */
const favoriteIds = ref(new Set());

/* 탭 상태 */
const tabs = ref([]);
const activeTab = ref(null);

/* PRAFTA-005 후속: 메뉴 로드 + 탭 구성이 끝나야 본문(router-view)을 렌더한다.
   새 탭 직진입 시 뷰가 메뉴 로드보다 먼저 마운트되어 공통 버튼 props가
   빈 채로 고정되는 문제를 막기 위한 게이트. */
const ready = ref(false);

/* keep-alive 에 포함시킬 '컴포넌트 이름' 목록
   - router.resolve(t.route).name 은 '라우트 이름'이므로
     각 뷰 파일에서 defineOptions({ name: 'User_02' }) 등으로
     '컴포넌트 이름'을 라우트 이름과 동일하게 맞춰두면 정확히 매칭됩니다. */
const cachedNames = computed(() =>
  tabs.value.map((t) => router.resolve(t.route).name).filter(Boolean)
);

/* 전역 즐겨찾기 그룹용 메뉴 목록(탭 무관).
   allSideMenus 의 모든 탭/그룹 items 를 평탄화하여 즐겨찾기인 항목만 모은다.
   각 item 에 최신 isFavorite 플래그를 주입한다. */
const favoriteItems = computed(() => {
  const result = [];
  const seen = new Set();
  const map = allSideMenus.value || {};
  Object.keys(map).forEach((topId) => {
    const groups = map[topId] || [];
    groups.forEach((group) => {
      (group.items || []).forEach((item) => {
        // 즐겨찾기 식별자는 MENU_D_ID(= item.route) 기준
        if (favoriteIds.value.has(item.route) && !seen.has(item.route)) {
          seen.add(item.route);
          result.push({ ...item, isFavorite: true });
        }
      });
    });
  });
  return result;
});

/* 현재 favoriteIds 기준으로 sideMenus 의 각 item.isFavorite 를 동기화한다.
   별표 토글 후 현재 탭 LNB 의 별 아이콘을 즉시 반영하기 위함. */
function applyFavoritesToSideMenus() {
  sideMenus.value = (sideMenus.value || []).map((group) => ({
    ...group,
    items: (group.items || []).map((item) => ({
      ...item,
      isFavorite: favoriteIds.value.has(item.route),
    })),
  }));
}

/* 상단 메뉴 선택 -> 좌측 메뉴 교체(중분류 그룹 배열).
   즐겨찾기 상태를 각 item 에 주입하여 별표 초기 표시를 맞춘다. */
function selectTopMenu(topMenuId) {
  selectedTopMenuId.value = topMenuId;
  const groups = allSideMenus.value[topMenuId] || [];
  sideMenus.value = groups.map((group) => ({
    ...group,
    items: (group.items || []).map((item) => ({
      ...item,
      isFavorite: favoriteIds.value.has(item.route),
    })),
  }));
}

/* 즐겨찾기 별표 토글: 낙관적 갱신 후 서버 호출, 실패 시 롤백. */
async function onToggleFavorite(menu) {
  // 즐겨찾기 식별자는 MENU_D_ID(= menu.route)로 통일(BE 실재검증 키계와 동일)
  if (!menu || !menu.route) return;
  const menuDId = menu.route;
  const wasFavorite = favoriteIds.value.has(menuDId);

  // 낙관적 갱신
  const next = new Set(favoriteIds.value);
  if (wasFavorite) next.delete(menuDId);
  else next.add(menuDId);
  favoriteIds.value = next;
  applyFavoritesToSideMenus();

  try {
    await fnToggleFavorite(menuDId);
  } catch (err) {
    // 실패 시 롤백
    const rollback = new Set(favoriteIds.value);
    if (wasFavorite) rollback.add(menuDId);
    else rollback.delete(menuDId);
    favoriteIds.value = rollback;
    applyFavoritesToSideMenus();
    alert(resolveApiErrorMessage(err, "즐겨찾기 변경 중 오류가 발생했습니다."));
  }
}

/* 메뉴/탭 네비게이션 */
function onNavigate(menu) {
  if (typeof menu === "object" && menu.route) {
    const route = `/safenote/main/${menu.route}`;
    const label = menu.label;
    const buttons = menu.buttons || {};
    addTab({ label, route, buttons });
  } else {
    const matched = topMenus.value.find((m) => m.id === menu);
    if (matched) selectTopMenu(menu);
  }
}

/* 탭 추가. 성공 여부를 반환한다 (탭 상한 거부 시 false — 호출부가 후속 정리 판단). */
function addTab(tab) {
  // 홈 포함 탭 개수 제한 (10개)
  if (tabs.value.length > 10) {
    alert("탭은 최대 10개까지만 열 수 있습니다.");
    return false;
  }

  const exists = tabs.value.find((t) => t.route === tab.route);
  if (!exists) tabs.value.push(tab);

  activeTab.value = tab.route;
  router.push(tab.route);
  return true;
}

function selectTab(route) {
  activeTab.value = route;
  router.push(route);
}

function closeTab(route) {
  if (route === "/safenote/main") return; // 대시보드는 닫지 않음

  const idx = tabs.value.findIndex((t) => t.route === route);
  if (idx !== -1) {
    tabs.value.splice(idx, 1);

    if (activeTab.value === route) {
      const nextTab = tabs.value[idx] || tabs.value[idx - 1];
      if (nextTab) {
        activeTab.value = nextTab.route;
        router.push(nextTab.route);
      } else {
        activeTab.value = null;
      }
    }
  }
}

function getActiveTabProps() {
  const tab = tabs.value.find((t) => t.route === activeTab.value);
  return tab?.buttons ? { title: tab.label, buttons: tab.buttons } : {};
}

/**
 * PRAFTA-005: 새 탭에서 /main/{동적경로}로 직진입한 경우,
 * 라우터는 이미 해당 자식 라우트로 매칭을 마쳤지만 tabs 배열에는 홈 탭만 있다.
 * 현재 URL에 해당하는 메뉴를 메뉴 트리에서 찾아 탭으로 자동 추가하고 active 처리한다.
 */
function findMenuByRoute(targetRoute, sideMenuMap) {
  // sideMenuMap: { [topMenuId]: [{ subGroupNm, subGroupIdx, items:[{id,label,route,buttons,isFavorite}] }] }
  for (const topId of Object.keys(sideMenuMap)) {
    const groups = sideMenuMap[topId] || [];
    const found = findInGroups(groups, targetRoute);
    if (found) return { topId, menu: found };
  }
  return null;
}

/* 중분류 그룹 배열을 순회하여 라우트가 일치하는 메뉴 item 을 찾는다. */
function findInGroups(groups, targetRoute) {
  for (const group of groups) {
    const items = (group && group.items) || [];
    for (const item of items) {
      if (item.route && `/safenote/main/${item.route}` === targetRoute) {
        return item;
      }
    }
  }
  return null;
}

/**
 * PRAFTA-DASHBOARD-T1: 대시보드 위젯의 "탭 열기 요청" 수신부.
 * 메뉴 트리에서 라우트를 찾아 기존 onNavigate 와 동일 흐름(selectTopMenu + addTab)으로 탭을 연다.
 * 메뉴 트리에 없으면(=해당 사용자 권한 메뉴가 아님) 이동을 거부하고 잔여 주입 파라미터를 정리한다.
 */
watch(
  () => dashNav.openTabRequest,
  (req) => {
    if (!req || !req.routeName) return;
    const targetRoute = `/safenote/main/${req.routeName}`;
    const matched = findMenuByRoute(targetRoute, allSideMenus.value);
    if (!matched) {
      alert("해당 화면에 대한 접근 권한이 없습니다.");
      // 이동 실패 시 주입 파라미터가 남아 다음 진입에 오적용되지 않도록 정리
      dashNav.consumeParams(req.routeName);
      return;
    }
    // 탭 열기 성공 시에만 LNB(상단 메뉴 그룹)를 전환한다 — 거부 시 화면과 LNB 불일치 방지
    const added = addTab({
      label: matched.menu.label,
      route: targetRoute,
      buttons: matched.menu.buttons || {},
    });
    if (!added) {
      // 탭 상한 등으로 열기 실패 — 잔존 주입 파라미터 정리 (consume-once 보장)
      dashNav.consumeParams(req.routeName);
      return;
    }
    selectTopMenu(matched.topId);
  }
);

onMounted(async () => {
  try {
    const retMenu = await fnGetMenuList();
    topMenus.value = retMenu.topMenus || [];
    allSideMenus.value = retMenu.sideMenus || {};

    // 즐겨찾기 목록 로드(비치명적). menu item 의 isFavorite 초기 상태 보정 및
    // 전역 즐겨찾기 그룹 구성에 사용한다.
    const favorites = await fnGetFavorites();
    favoriteIds.value = new Set(favorites || []);

    const defaultTop = topMenus.value[0];
    if (defaultTop) selectTopMenu(defaultTop.id);

    addTab({ label: "🏠", route: "/safenote/main", buttons: {} });

    // PRAFTA-005: URL이 /main 자식 라우트로 직진입된 경우 해당 탭을 자동 추가
    if (
      route.path &&
      route.path !== "/safenote/main" &&
      route.path.startsWith("/safenote/main/")
    ) {
      const matched = findMenuByRoute(route.path, allSideMenus.value);
      if (matched) {
        // 해당 메뉴가 속한 상단 메뉴 탭을 선택
        selectTopMenu(matched.topId);
        const label = matched.menu.label || route.meta?.title || route.path;
        addTab({
          label,
          route: route.path,
          buttons: matched.menu.buttons || {},
        });
      } else {
        // 메뉴 트리에서 찾지 못해도 라우트 메타에서 라벨만 가져와 탭에 추가
        const label = route.meta?.title || route.path;
        addTab({ label, route: route.path, buttons: {} });
      }
    }
  } finally {
    // 메뉴 로드/탭 구성이 끝난 뒤에만 본문을 렌더한다.
    // (에러가 나도 ready를 true로 두어 화면이 영구 공백으로 멈추지 않게 한다.)
    ready.value = true;
  }
});

function closeAllTabsExceptHome() {
  // 홈(/main)만 남기고 모두 제거
  tabs.value = tabs.value.filter((t) => t.route === "/safenote/main");

  // 홈을 활성화
  activeTab.value = "/safenote/main";
  router.push("/safenote/main");
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #ffffff;
}

.side-wrapper {
  width: 180px;
  color: #111827;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  font-size: 0.8rem;
  font-weight: bold;
}

.right-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.tabs {
  display: flex;
  align-items: center;
  padding: 0rem 0.2rem;
  background: #e9f4f0;
  border-bottom: 1px solid #ccc;
  white-space: nowrap;
}

.tab-item {
  margin-top: 0.5rem;
  padding: 0.3rem 1rem;
  margin-right: 0.5rem;
  background: #d5eee5;
  border-radius: 6px 6px 0 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  max-width: 200px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  font-size: 0.8rem;
  font-family: "Pretendard", sans-serif;
}

.tab-item.active {
  background: #ffffff;
  font-weight: bold;
  border-bottom: 2px solid transparent;
}

.close-btn {
  color: #888;
  cursor: pointer;
  padding: 0 0.1rem;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  background: #e9f4f0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.tab-item.special {
  background: #f5dada;
  color: #a00;
  font-weight: bold;
}
.tab-item.special:hover {
  background: #f0bcbc;
}

.tab-list {
  display: flex;
  flex: 1;
  overflow-x: auto;
}

.close-all {
  margin-left: auto;
  background: #f5dada;
  color: #a00;
  font-weight: bold;
  flex-shrink: 0;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid #e5e7eb;
  font-size: 0.8rem;
}
.close-all:hover {
  background: #f0bcbc;
}
</style>
