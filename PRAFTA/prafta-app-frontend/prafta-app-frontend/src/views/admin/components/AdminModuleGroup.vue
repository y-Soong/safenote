<!--
  AdminModuleGroup.vue — 관리자 메인 모듈 카테고리 그룹 컨테이너 (신규)
  - 작업: GRP-1 (분해: .claude/requests/app_requests/작업지시서_관리자앱-메인메뉴-카테고리그룹핑.plan.md)
  - 배경: AdminLauncherView.vue 의 8~9개 모듈이 세로로 계속 나열되던 문제를 해소하기 위해
    카테고리(승인/근태/안전·교육/현장 운영)로 묶고 접이식(아코디언)으로 표시한다.
    "메뉴가 늘어도 화면이 계속 길어지지 않는 확장성"이 목표(2026-08-29 사용자 확정) —
    항상 펼침 방식은 세로 길이가 그대로라 이 목표를 달성하지 못해 채택하지 않음(plan.md §1-1).
  - 순수 프레젠테이션 컨테이너: 펼침 여부(expanded)는 부모(AdminLauncherView)가 소유하고
    sessionStorage 로 지속한다(재진입 시 유지, plan.md §1-4) — 본 컴포넌트는 상태를 갖지 않고
    toggle 만 emit 한다(AdminApprovalGroupCard.vue 와 동일한 "부모가 상태 소유" 패턴).
  - 그룹 내부에는 기존 AdminModuleSection 들을 그대로 배치한다(슬롯) — 모듈 행 자체는 무변경.
  - 아이콘: 부모(AdminLauncherView) 의 전역 스프라이트(#i-admin-chev-right/#i-admin-chev-down)를
    재사용한다(AdminModuleSection.vue 와 동일한 관례 — 본 컴포넌트는 항상 AdminLauncherView 하위에서만
    렌더되므로 자체 스프라이트를 선언하지 않는다).
  - ⚠️ C1 무관: 활성/스코프/배지 판정은 하지 않는다(그 값들은 여전히 AdminModuleSection 이 표시).
    본 컴포넌트가 갖는 유일한 표시 로직은 "접힘 상태일 때 그룹 배지 합계 노출"뿐이다.
  - 디자인 토큰: 부모(.admin-launcher-view)에서 선언한 var(--...) 를 상속(자체 선언 없음).
-->
<template>
  <section class="module-group">
    <button
      type="button"
      class="module-group__header"
      :aria-expanded="expanded"
      :aria-controls="panelId"
      @click="onToggle"
    >
      <span class="module-group__title">{{ title }}</span>

      <!-- 접힘 상태에서만 그룹 합계 배지 노출(펼침 시엔 각 모듈 행 배지가 이미 보이므로 중복 방지) -->
      <span
        v-if="!expanded && badgeCount > 0"
        class="module-group__badge"
        :aria-label="`미처리 ${badgeCount}건`"
        >{{ badgeText }}</span
      >

      <svg class="icon module-group__chev" width="20" height="20" aria-hidden="true">
        <use :href="expanded ? '#i-admin-chev-down' : '#i-admin-chev-right'" />
      </svg>
    </button>

    <div v-show="expanded" :id="panelId" class="module-group__panel">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 그룹 표시명(예: '승인', '근태')
  title: { type: String, required: true },
  // 그룹 식별키(예: 'APPROVAL_GROUP') — aria-controls id 조합 및 부모 toggle 식별에 사용
  groupKey: { type: String, required: true },
  // 펼침 여부 — 부모가 소유(sessionStorage 지속, plan.md §1-4). 본 컴포넌트는 값을 갖지 않는다(제어 컴포넌트).
  expanded: { type: Boolean, default: false },
  // 그룹 내 모듈 미처리 건수 합계(서버 산출 moduleBadgeMap 을 부모가 합산). 접힘 상태에서만 표시.
  badgeCount: { type: Number, default: 0 },
})

const emit = defineEmits(['toggle'])

const panelId = computed(() => `module-group-panel-${props.groupKey}`)

// 세 자리 이상은 배지가 헤더 레이아웃을 밀어내므로 99+ 로 절삭 표기(AdminModuleSection.vue 와 동일 관례).
const badgeText = computed(() => (props.badgeCount > 99 ? '99+' : String(props.badgeCount)))

// 헤더 탭 → 부모에 groupKey 로 toggle emit(펼침 상태 자체는 부모가 갱신).
const onToggle = () => {
  emit('toggle', props.groupKey)
}
</script>

<style scoped>
.module-group {
  display: flex;
  flex-direction: column;
}

.module-group__header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  width: 100%;
  min-height: 40px;
  padding: var(--space-xs) var(--space-xs);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}

.module-group__title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-secondary);
  letter-spacing: 0.02em;
}

/* 접힘 상태 그룹 합계 배지 — AdminModuleSection__badge 와 동일 톤(danger) */
.module-group__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  border-radius: var(--radius-full);
}

.module-group__chev {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

.module-group__panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding-top: var(--space-xs);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
