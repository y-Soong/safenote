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
        <router-view v-slot="{ Component }">
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
import { useRouter } from "vue-router";
import { fnGetMenuList } from "@/api/navigation";
import TopNav from "@/components/layout/TopNav.vue";
import SideMenu from "@/components/layout/SideMenu.vue";

const router = useRouter();

const topMenus = ref([]);
const sideMenus = ref([]);
const allSideMenus = ref({});
const selectedTopMenuId = ref(null);

/* 탭 상태 */
const tabs = ref([]);
const activeTab = ref(null);

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

onMounted(async () => {
  const retMenu = await fnGetMenuList();
  topMenus.value = retMenu.topMenus;
  allSideMenus.value = retMenu.sideMenus;

  const defaultTop = topMenus.value[0];
  if (defaultTop) selectTopMenu(defaultTop.id);

  addTab({ label: "🏠", route: "/main", buttons: {} });
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
