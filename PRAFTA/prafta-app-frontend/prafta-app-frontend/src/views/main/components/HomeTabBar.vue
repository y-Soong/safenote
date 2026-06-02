<!--
  HomeTabBar.vue — 하단 5탭 (홈 / 근태 / 안전 / TBM / 마이)
  - 상세 요청서 §1.3, §4.1 (탭바 72px, TBM 미참석 카운트 배지)
  - 본 라운드: 활성 탭만 홈. 나머지는 emit 만 (라우팅은 부모에서 TODO)
-->
<template>
  <nav class="tabbar" aria-label="홈 하단 탭바">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="tab"
      :class="{ 'tab--active': activeTab === tab.key }"
      :aria-current="activeTab === tab.key ? 'page' : undefined"
      @click="$emit('click:tab', tab.key)"
    >
      <span class="tab-badge-wrap">
        <svg class="icon" width="24" height="24" aria-hidden="true">
          <use :href="`#${tab.iconId}`" />
        </svg>
        <!-- TBM 탭 미참석 카운트 배지 -->
        <span
          v-if="tab.key === 'tbm' && tbmBadgeCount > 0"
          class="tab-badge"
          :aria-label="`미참석 ${tbmBadgeCount}건`"
          >{{ tbmBadgeCount }}</span
        >
      </span>
      <span class="tab__lbl">{{ tab.label }}</span>
    </button>
  </nav>
</template>

<script setup>
defineProps({
  // 'home' | 'attd' | 'safety' | 'tbm' | 'my'
  activeTab: {
    type: String,
    default: 'home',
  },
  // TBM 탭 미참석 카운트 (0 이면 배지 숨김)
  tbmBadgeCount: {
    type: Number,
    default: 0,
  },
})

defineEmits(['click:tab'])

// 탭 정의 — 시안 순서 그대로
const tabs = [
  { key: 'home', label: '홈', iconId: 'i-home' },
  { key: 'attd', label: '근태', iconId: 'i-cal' },
  { key: 'safety', label: '안전', iconId: 'i-shield' },
  { key: 'tbm', label: 'TBM', iconId: 'i-monitor' },
  { key: 'my', label: '마이', iconId: 'i-user' },
]
</script>

<style scoped>
.tabbar {
  position: relative;
  height: 72px;
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  align-items: center;
  padding-bottom: 8px;
}

.tab {
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
  /* hit area 44×44px 보장 */
  min-height: 56px;
}
.tab--active {
  color: var(--color-primary);
}

.tab__lbl {
  font-size: 12px;
  font-weight: 500;
}

.tab-badge-wrap {
  position: relative;
}

.tab-badge {
  position: absolute;
  top: -4px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: var(--color-danger);
  color: #ffffff;
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
</style>
