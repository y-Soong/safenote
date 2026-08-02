<!--
  AdminModuleSection.vue — 관리자 본문 세로 섹션 행 (신규)
  - 작업 ID: 001-P1-F9 (분해: .claude/requests/app_requests/001-phase1-admin-ui-redesign.md)
  - 기존 그리드 카드(AdminModuleCard)를 폐기하고, 사용자 모드 본문(세로 카드 나열) 패턴으로 교체.
  - 표시 전용 컴포넌트: enabled/scoped/note 를 props 로 받아 렌더만 한다.
      · enabled=false → 영역 고정 노출하되 회색 비활성(클릭 차단). [매트릭스 §3] ⛔ / IA "영역 고정".
      · scoped=true   → 🔵 "내 노드" 배지(데이터 스코프 적용 모듈). [매트릭스 §2]
      · note          → 보조 표기(예: 게시판 "준비중").
  - ⚠️ C1: 역할(AUTH_CD) 판정 로직 없음(서버 산출 enabled/scoped 만 신뢰).
  - 디자인 토큰: 부모(.admin-launcher-view)에서 선언한 var(--...) 상속.
-->
<template>
  <button
    type="button"
    class="module-section"
    :class="{ 'module-section--disabled': !enabled }"
    :disabled="!enabled"
    :aria-disabled="!enabled"
    @click="onClick"
  >
    <span class="module-section__icon-wrap">
      <svg class="icon module-section__icon" width="24" height="24" aria-hidden="true">
        <use :href="`#${iconId}`" />
      </svg>
    </span>

    <span class="module-section__body">
      <span class="module-section__title-row">
        <span class="module-section__title">{{ title }}</span>
        <!-- 미처리 건수 배지 — 활성 + 1건 이상일 때만. 화면에 들어가 보지 않아도
             신규 요청 유무를 홈에서 알 수 있게 한다(0건이면 비노출로 노이즈 방지). -->
        <span
          v-if="enabled && badgeCount > 0"
          class="module-section__badge"
          :aria-label="`미처리 ${badgeCount}건`"
          >{{ badgeText }}</span
        >
        <!-- 데이터 스코프 배지(노드 관리자 자기노드 등) — 활성 + scoped 일 때만 -->
        <span v-if="scoped && enabled" class="module-section__scope" aria-label="내 노드 범위"
          >내 노드</span
        >
        <!-- 보조 표기(예: 준비중) -->
        <span v-if="note" class="module-section__note">{{ note }}</span>
      </span>
    </span>

    <!-- 진입 chevron — 활성 섹션에서만 노출 -->
    <svg
      v-if="enabled"
      class="icon module-section__chev"
      width="20"
      height="20"
      aria-hidden="true"
    >
      <use href="#i-admin-chev-right" />
    </svg>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 모듈 표시명
  title: { type: String, required: true },
  // 스프라이트 심볼 id (예: 'i-admin-home')
  iconId: { type: String, required: true },
  // 서버 산출 활성 여부([매트릭스 §3]). false 면 회색 비활성(클릭 차단, 영역 고정).
  enabled: { type: Boolean, default: false },
  // 데이터 스코프 적용 모듈 여부(🔵 배지). [매트릭스 §2]
  scoped: { type: Boolean, default: false },
  // 보조 표기(예: '준비중')
  note: { type: String, default: '' },
  // 미처리 건수(서버 조회값). 0/미지정이면 배지 비노출.
  //   ⚠️ C1: 여기서 건수를 계산하지 않는다 — 부모가 서버 응답으로 채운 값만 표시한다.
  badgeCount: { type: Number, default: 0 },
})

const emit = defineEmits(['select'])

// 세 자리 이상은 배지가 행 레이아웃을 밀어내므로 99+ 로 절삭 표기한다.
const badgeText = computed(() => (props.badgeCount > 99 ? '99+' : String(props.badgeCount)))

// 비활성 섹션은 emit 차단(영역은 고정 노출하되 동작 없음).
const onClick = () => {
  if (!props.enabled) return
  emit('select')
}
</script>

<style scoped>
.module-section {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  min-height: 64px;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}

.module-section__icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary-tint);
}
.module-section__icon {
  color: var(--color-primary);
}

.module-section__body {
  flex: 1;
  min-width: 0;
}
.module-section__title-row {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}
.module-section__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.module-section__scope {
  padding: 2px 6px;
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 700;
  border-radius: var(--radius-full);
}
/* 미처리 건수 배지 — 스코프 배지(연한 톤)와 구분되도록 danger 톤 채움.
   토큰은 부모(.admin-launcher-view) 선언분을 상속한다. */
.module-section__badge {
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
.module-section__note {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
}

.module-section__chev {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

/* 비활성 — 영역 고정 노출하되 회색 + 클릭 차단 */
.module-section--disabled {
  background: var(--color-disabled-bg);
  border-color: var(--color-border-light);
  box-shadow: none;
  cursor: not-allowed;
}
.module-section--disabled .module-section__icon-wrap {
  background: var(--color-border-light);
}
.module-section--disabled .module-section__icon {
  color: var(--color-disabled-text);
}
.module-section--disabled .module-section__title {
  color: var(--color-disabled-text);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
