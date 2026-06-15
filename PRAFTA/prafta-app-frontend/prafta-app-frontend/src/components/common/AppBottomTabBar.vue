<!--
  AppBottomTabBar.vue — 사용자 모드 공통 하단 5탭 (홈 / 근태 / 안전 / TBM / 마이)
  - 작업 ID: prafta-app-025 J1-2
  - 목적: MainView 의 HomeTabBar 와 MyAttendanceView 의 자체 nav(bottomTabs)로 분기되어 있던
          하단 탭바를 단일 공통 컴포넌트로 통일한다. 모든 사용자 모드 1뎁스 화면이 동일 탭바를 쓴다.
  - 기존 HomeTabBar.vue 를 확장/대체(5탭 + tbmBadgeCount 유지). MyAttendanceView 의 4탭(마이 누락) 문제 해소.
  - props:
      activeTab     : 'home' | 'attd' | 'safety' | 'tbm' | 'my' (현재 화면 강조)
      tbmBadgeCount : TBM 탭 미참석 배지 수(0이면 숨김)
  - emit: click:tab(tabKey) — 부모가 추가 동작을 붙일 수 있게 유지. 라우팅은 본 컴포넌트가 중앙화하여 수행.
  - 결정(J1-2 §2): 라우팅 일관성을 위해 본 컴포넌트가 탭별 목적지 router.push 를 직접 수행한다(중앙화).
      → 각 화면이 onTabClick 을 중복 구현하던 분기를 제거. 단 현재 활성 탭 재클릭은 무동작.
  - 결정(메인 세션 J1-2 결정 2/3): 전역 네비게이션에 근무중 게이트를 걸지 않는다(단순 라우팅).
      TBM 근무중 게이트는 MainView 카드 ">"(onTabDetail)에만 유지한다(탭바와 별개).
  - 아이콘 스프라이트: 부모 화면 스프라이트 의존을 제거하고 본 컴포넌트가 자체 내장한다(재사용성↑).
      MainView HomeIcons 의 i-home/i-cal/i-shield/i-monitor/i-user 라인아이콘을 동일하게 복제하되,
      id 충돌을 피하기 위해 'i-tab-' 접두 심볼로 둔다.
  - 디자인 토큰: 부모 화면 루트(.home-view 등)에서 선언된 var(--...) 를 상속(scoped).
-->
<template>
  <nav
    class="app-tabbar"
    :style="{ gridTemplateColumns: `repeat(${tabs.length}, 1fr)` }"
    aria-label="하단 탭바"
  >
    <!-- 자체 아이콘 스프라이트(부모 의존 제거) -->
    <svg width="0" height="0" class="app-tabbar__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-tab-home" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 12l-2 0l9-9l9 9l-2 0" />
          <path d="M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-7" />
          <path d="M10 21v-6a2 2 0 0 1 2-2h0a2 2 0 0 1 2 2v6" />
        </symbol>
        <symbol id="i-tab-cal" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="5" width="16" height="16" rx="2" />
          <line x1="16" y1="3" x2="16" y2="7" />
          <line x1="8" y1="3" x2="8" y2="7" />
          <line x1="4" y1="11" x2="20" y2="11" />
        </symbol>
        <symbol id="i-tab-shield" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3l8 4v6c0 4.4-3.6 8-8 8s-8-3.6-8-8V7l8-4z" />
        </symbol>
        <symbol id="i-tab-monitor" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="12" rx="1" />
          <line x1="7" y1="20" x2="17" y2="20" />
          <line x1="9" y1="16" x2="9" y2="20" />
          <line x1="15" y1="16" x2="15" y2="20" />
        </symbol>
        <symbol id="i-tab-user" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1" />
        </symbol>
      </defs>
    </svg>

    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="app-tabbar__tab"
      :class="{ 'app-tabbar__tab--active': activeTab === tab.key }"
      :aria-current="activeTab === tab.key ? 'page' : undefined"
      @click="onTabClick(tab.key)"
    >
      <span class="app-tabbar__badge-wrap">
        <svg class="icon" width="24" height="24" aria-hidden="true">
          <use :href="`#${tab.iconId}`" />
        </svg>
        <span
          v-if="tab.key === 'tbm' && tbmBadgeCount > 0"
          class="app-tabbar__badge"
          :aria-label="`미참석 ${tbmBadgeCount}건`"
          >{{ tbmBadgeCount }}</span
        >
      </span>
      <span class="app-tabbar__lbl">{{ tab.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { isDailyWorker } from '@/utils/employment'

const props = defineProps({
  // 'home' | 'attd' | 'safety' | 'tbm' | 'my'
  activeTab: { type: String, default: 'home' },
  // TBM 탭 미참석 배지(0이면 숨김)
  tbmBadgeCount: { type: Number, default: 0 },
})

// 부모가 추가 동작을 붙일 수 있게 emit 은 유지(라우팅은 내장).
const emit = defineEmits(['click:tab'])

const router = useRouter()

// 탭 정의 — 시안 순서 그대로. 아이콘은 본 컴포넌트 자체 스프라이트('i-tab-*')를 참조.
const ALL_TABS = [
  { key: 'home', label: '홈', iconId: 'i-tab-home' },
  { key: 'attd', label: '근태', iconId: 'i-tab-cal' },
  { key: 'safety', label: '안전', iconId: 'i-tab-shield' },
  { key: 'tbm', label: 'TBM', iconId: 'i-tab-monitor' },
  { key: 'my', label: '마이', iconId: 'i-tab-user' },
]

// prafta-app-025 J1-4: 일용직(DAILY)은 근태 탭 숨김 → 5탭에서 'attd' 제외(4탭).
//   grid 컬럼 수는 template 에서 tabs.length 로 동적 설정해 레이아웃 균등 유지.
//   세션값(gv_employmentType)을 컴포넌트가 직접 조회 — 모든 장착 화면에서 일관 동작.
const tabs = computed(() =>
  isDailyWorker() ? ALL_TABS.filter((tab) => tab.key !== 'attd') : ALL_TABS,
)

// 탭 키 → 목적지 라우트(J1-2 §2 결정: MainView 진입 목적지와 동일하게 매칭).
//   safety → /SafetyHub (J1-3 신규), tbm → /TbmHub.
const TAB_ROUTE = {
  home: '/MainView',
  attd: '/MyAttendance',
  safety: '/SafetyHub',
  tbm: '/TbmHub',
  my: '/MyPage',
}

// 라우팅 중앙화. 현재 탭과 동일하면 무동작(중복 push 방지).
//   메인 세션 결정 3: 전역 네비게이션에 근무중 게이트 미적용 — 단순 라우팅.
const onTabClick = (tabKey) => {
  emit('click:tab', tabKey)
  if (tabKey === props.activeTab) return
  const target = TAB_ROUTE[tabKey]
  if (!target) return
  router.push(target)
}
</script>

<style scoped>
.app-tabbar {
  position: relative;
  height: 72px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border);
  display: grid;
  /* 컬럼 수는 template 의 :style 인라인 바인딩(tabs.length)이 결정한다(J1-4: 일용직 4탭).
     아래 값은 인라인 미적용 환경 폴백. */
  grid-template-columns: repeat(5, 1fr);
  align-items: center;
  padding-bottom: 8px;
}

.app-tabbar__tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 8px 0;
  color: var(--color-text-secondary);
  font-family: inherit;
  min-height: 56px;
}
.app-tabbar__tab--active {
  color: var(--color-primary);
}

.app-tabbar__lbl {
  font-size: 12px;
  font-weight: 500;
}

.app-tabbar__badge-wrap {
  position: relative;
}

.app-tabbar__badge {
  position: absolute;
  top: -4px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: var(--color-danger);
  color: var(--color-on-danger);
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
  text-align: center;
  border-radius: var(--radius-full);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}

.app-tabbar__sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
