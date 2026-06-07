<!--
  AdminTabBar.vue — 관리자 모드 하단 5탭 (승인 / 근태 / 홈 / 안전 / TBM) (신규)
  - 작업 ID: 001-P1-F8 (분해: .claude/requests/app_requests/001-phase1-admin-ui-redesign.md)
  - 사용자 모드 HomeTabBar 패턴 차용(72px, grid 5열, active=primary).
  - 탭 순서 고정: 승인 / 근태 / 홈(중앙=대시보드 런처) / 안전 / TBM.
      · 현장처리/게시판/설정은 탭에 없음(의도 — 본문 섹션/우상단 설정으로 접근).
  - 탭 활성/비활성은 서버 moduleActiveMap 으로만 판정(C1):
      · 활성 탭 클릭 → emit('click:tab', key) (라우팅은 부모·developer).
      · 비활성 탭 → 회색 + 클릭 무동작(영역 고정 노출, IA "영역 고정" 원칙).
  - ⚠️ C1: 역할(AUTH_CD) 분기 없음. 탭 enabled 는 activeMap[moduleKey] 로만 산출.
  - 디자인 토큰: 부모(.admin-launcher-view)에서 선언한 var(--...) 상속(HomeTabBar 패턴 동일).
-->
<template>
  <nav class="admin-tabbar" aria-label="관리자 하단 탭바">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="admin-tab"
      :class="{
        'admin-tab--active': activeTab === tab.key && isTabEnabled(tab),
        'admin-tab--disabled': !isTabEnabled(tab),
      }"
      :disabled="!isTabEnabled(tab)"
      :aria-disabled="!isTabEnabled(tab)"
      :aria-current="activeTab === tab.key && isTabEnabled(tab) ? 'page' : undefined"
      @click="onTabClick(tab)"
    >
      <span class="admin-tab__icon-wrap">
        <svg class="icon" width="24" height="24" aria-hidden="true">
          <use :href="`#${tab.iconId}`" />
        </svg>
      </span>
      <span class="admin-tab__label">{{ tab.label }}</span>
    </button>
  </nav>
</template>

<script setup>
const props = defineProps({
  // 현재 활성 탭 key ('approval' | 'attd' | 'home' | 'safety' | 'tbm')
  activeTab: { type: String, default: 'home' },
  // 서버 산출 모듈 활성 맵(access-context.moduleActiveMap) — 탭 활성 판정 단일 출처(C1)
  moduleActiveMap: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['click:tab'])

// 탭 정의 — 사용자 확정 순서 그대로(중앙=홈).
//   moduleKey: 탭 활성 판정에 쓰는 access-context 모듈 키(승인=APPROVAL, 근태=ATTD_DETAIL,
//   홈=HOME, 안전=SAFETY, TBM=TBM). iconId 는 AdminLauncherView 스프라이트 심볼.
const tabs = [
  { key: 'approval', label: '승인', iconId: 'i-admin-approval', moduleKey: 'APPROVAL' },
  { key: 'attd', label: '근태', iconId: 'i-admin-attd', moduleKey: 'ATTD_DETAIL' },
  { key: 'home', label: '홈', iconId: 'i-admin-home', moduleKey: 'HOME' },
  { key: 'safety', label: '안전', iconId: 'i-admin-safety', moduleKey: 'SAFETY' },
  { key: 'tbm', label: 'TBM', iconId: 'i-admin-tbm', moduleKey: 'TBM' },
]

// 탭 활성 여부 — 서버 맵만 신뢰(키 없으면 비활성으로 폴백).
const isTabEnabled = (tab) => props.moduleActiveMap?.[tab.moduleKey] === true

// 비활성 탭은 무동작(영역 고정). 활성 탭만 부모로 emit(라우팅은 developer).
const onTabClick = (tab) => {
  if (!isTabEnabled(tab)) return
  emit('click:tab', tab.key)
}
</script>

<style scoped>
.admin-tabbar {
  position: relative;
  height: 72px;
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  align-items: center;
  padding-bottom: 8px;
}

.admin-tab {
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
  /* hit area 보장 */
  min-height: 56px;
}
.admin-tab--active {
  color: var(--color-primary);
}

/* 비활성 — 회색 + 클릭 차단(영역 고정 노출) */
.admin-tab--disabled {
  color: var(--color-disabled-text);
  cursor: not-allowed;
}

.admin-tab__label {
  font-size: 12px;
  font-weight: 500;
}

.admin-tab__icon-wrap {
  position: relative;
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
