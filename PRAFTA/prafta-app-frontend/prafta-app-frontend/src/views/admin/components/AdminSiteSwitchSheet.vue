<!--
  AdminSiteSwitchSheet.vue — 현장 전환 셀렉터(하단 시트)
  - 작업 ID: 001-P1-F3 (분해: .claude/requests/app_requests/001-phase1-admin-shell-plan.md §5)
  - 출처: 001_관리자모드-권한매트릭스.md §3.1 (현장전환 노출 = USE_YN='Y' 사업장 수 > 1, 역할 무관)
  - 공통 컴포넌트 재사용: BaseBottomSheet (components/common/BaseBottomSheet.vue)
      → HB-14(F-6)에서 공통 디렉토리로 승격 완료(D9 해소).
  - planner 라운드 스코프: template + style. 선택 사업장 emit 만(영속화/전파는 developer, D5).
  - 디자인 토큰: 부모(AdminLauncherView)에서 선언한 var(--...) 상속.
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="현장 전환"
    :show-footer="false"
    @update:model-value="onSheetToggle"
  >
    <ul class="site-list" role="listbox" aria-label="사업장 목록">
      <li v-for="site in sites" :key="site.siteCd">
        <button
          type="button"
          class="site-row"
          :class="{ 'site-row--active': site.siteCd === currentSiteCd }"
          role="option"
          :aria-selected="site.siteCd === currentSiteCd"
          @click="onSelect(site)"
        >
          <span class="site-row__main">
            <span class="site-row__name">{{ site.siteNm }}</span>
            <span v-if="site.siteNo" class="site-row__no">{{ site.siteNo }}</span>
          </span>
          <svg
            v-if="site.siteCd === currentSiteCd"
            class="icon site-row__check"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </button>
      </li>

      <!-- empty -->
      <li v-if="!sites || sites.length === 0" class="site-empty">
        선택 가능한 사업장이 없습니다.
      </li>
    </ul>
  </BaseBottomSheet>
</template>

<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'

defineProps({
  // v-model (시트 노출)
  modelValue: { type: Boolean, default: false },
  // 접근 가능 사업장 목록 [{ siteCd, siteNo, siteNm }] (access-context.accessibleSites)
  sites: { type: Array, default: () => [] },
  // 현재 선택 사업장 코드 (체크 표시)
  currentSiteCd: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'select'])

// 시트 dimmer/닫기 → 부모 v-model 동기화
const onSheetToggle = (open) => {
  emit('update:modelValue', open)
}

// 사업장 선택 → 부모로 전달(전파/재조회는 부모, D5)
const onSelect = (site) => {
  if (!site?.siteCd) return
  emit('select', site)
}
</script>

<style scoped>
.site-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.site-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  padding: 14px 2px;
  background: transparent;
  border: 0;
  border-top: 0.5px solid var(--color-border-light);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.site-list li:first-child .site-row {
  border-top: 0;
}

.site-row__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.site-row__name {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.site-row__no {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.site-row--active .site-row__name {
  color: var(--color-primary);
  font-weight: 700;
}
.site-row__check {
  color: var(--color-primary);
}

.site-empty {
  padding: 24px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
