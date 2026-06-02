<!--
  PasswordRuleGuide.vue — 비밀번호 규칙 실시간 충족 표시 카드
  - 작업 ID: PRAFTA-APP-010-21
  - 호출자: PasswordChangeView (010-12)
  - 표시 전용: 규칙 충족 판정은 부모(props)가 전달. 본 컴포넌트는 체크/원 아이콘만 렌더.
-->
<template>
  <div class="prg">
    <p class="prg__title">비밀번호 규칙</p>
    <p class="prg__desc">숫자·영문·특수문자 중 2가지 이상 포함, 6~15자</p>
    <ul class="prg__list">
      <li
        v-for="rule in rules"
        :key="rule.key"
        class="prg__item"
        :class="{ 'prg__item--met': rule.met }"
      >
        <svg class="icon prg__icon" width="16" height="16" aria-hidden="true">
          <use :href="rule.met ? '#i-prg-check' : '#i-prg-circle'" />
        </svg>
        <span class="prg__label">{{ rule.label }}</span>
      </li>
    </ul>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="prg__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-prg-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </symbol>
        <symbol id="i-prg-circle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
defineProps({
  // [{ key, label, met:Boolean }]
  rules: {
    type: Array,
    default: () => [],
  },
})
</script>

<style scoped>
.prg {
  position: relative;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
}
.prg__title {
  margin: 0 0 var(--space-xs);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.prg__desc {
  margin: 0 0 var(--space-sm);
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.prg__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.prg__item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--color-text-tertiary);
}
.prg__item--met {
  color: var(--color-primary);
}
.prg__icon {
  flex-shrink: 0;
}
.prg__label {
  font-size: 13px;
}
.prg__sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
