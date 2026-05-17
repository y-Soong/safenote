<template>
  <div class="main-layout">
    <!-- 좌측 사이드 메뉴 -->
    <div class="side-wrapper">
      <SideMenu
        :menus="sideMenus"
        :active-route="activeTab"
        @navigate="onNavigate"
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
            v-if="tab.route !== '/main'"
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
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fnGetMenuList } from "@/api/navigation";
import TopNav from "@/components/layout/TopNav.vue";
import SideMenu from "@/components/layout/SideMenu.vue";

const router = useRouter();
const route = useRoute();

const topMenus = ref([]);
const sideMenus = ref([]);
const allSideMenus = ref({});
const selectedTopMenuId = ref(null);

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

/* 상단 메뉴 선택 -> 좌측 메뉴 교체 */
function selectTopMenu(topMenuId) {
  selectedTopMenuId.value = topMenuId;
  sideMenus.value = allSideMenus.value[topMenuId] || [];
}

/* 메뉴/탭 네비게이션 */
function onNavigate(menu) {
  if (typeof menu === "object" && menu.route) {
    const route = `/main/${menu.route}`;
    const label = menu.label;
    const buttons = menu.buttons || {};
    addTab({ label, route, buttons });
  } else {
    const matched = topMenus.value.find((m) => m.id === menu);
    if (matched) selectTopMenu(menu);
  }
}

function addTab(tab) {
  // 홈 포함 탭 개수 제한 (10개)
  if (tabs.value.length > 10) {
    alert("탭은 최대 10개까지만 열 수 있습니다.");
    return;
  }

  const exists = tabs.value.find((t) => t.route === tab.route);
  if (!exists) tabs.value.push(tab);

  activeTab.value = tab.route;
  router.push(tab.route);
}

function selectTab(route) {
  activeTab.value = route;
  router.push(route);
}

function closeTab(route) {
  if (route === "/main") return; // 대시보드는 닫지 않음

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
  // sideMenuMap: { [topMenuId]: [{ id, label, route, buttons, children? }, ...] }
  for (const topId of Object.keys(sideMenuMap)) {
    const list = sideMenuMap[topId] || [];
    const found = findInList(list, targetRoute);
    if (found) return { topId, menu: found };
  }
  return null;
}

function findInList(list, targetRoute) {
  for (const item of list) {
    if (item.route && `/main/${item.route}` === targetRoute) return item;
    if (Array.isArray(item.children)) {
      const sub = findInList(item.children, targetRoute);
      if (sub) return sub;
    }
  }
  return null;
}

onMounted(async () => {
  try {
    const retMenu = await fnGetMenuList();
    topMenus.value = retMenu.topMenus || [];
    allSideMenus.value = retMenu.sideMenus || {};

    const defaultTop = topMenus.value[0];
    if (defaultTop) selectTopMenu(defaultTop.id);

    addTab({ label: "🏠", route: "/main", buttons: {} });

    // PRAFTA-005: URL이 /main 자식 라우트로 직진입된 경우 해당 탭을 자동 추가
    if (route.path && route.path !== "/main" && route.path.startsWith("/main/")) {
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
  tabs.value = tabs.value.filter((t) => t.route === "/main");

  // 홈을 활성화
  activeTab.value = "/main";
  router.push("/main");
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
