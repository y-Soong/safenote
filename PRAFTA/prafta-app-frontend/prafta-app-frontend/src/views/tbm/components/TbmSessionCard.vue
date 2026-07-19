<!--
  TbmSessionCard.vue — TBM 공용 세션 카드 (참석가능/교육중/교육완료 3탭 공용)
  - 작업 ID: PRAFTA-TBM-CARD (분해: prafta-app-tbm-user-detail-plan.md §5)
  - 표시 필드는 variant prop 로 탭별 분기:
      'AVAILABLE'    → 개설자 / 제목 / 개설일시
      'IN_PROGRESS'  → 개설자 / 제목 / 시작일시
      'COMPLETED'    → 개설자 / 제목 / 종료일시 + 이수상태 배지
  - 참조 패턴: TbmAttendCard.vue(메타/콜아웃), TbmEntryView 카드.
  - 디자인 토큰은 부모(.tbm-hub-view 등)에서 상속받는다(루트 재선언 없음 — 항상 허브 내부에서만 사용).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + 단순 표시 computed 만.
-->
<template>
  <button type="button" class="tbm-card" @click="$emit('select', session)">
    <div class="tbm-card__head">
      <p class="tbm-card__title">{{ session.title || 'TBM 세션' }}</p>

      <!-- 개최사 배지(PRAFTA-SUBCON-T5): 타사(연동) 세션일 때만 서버가 hostCmpnyNm 을 내려준다.
           표시값은 서버 relabel(나를 지정한 직상위 회사) — 프론트에서 조립하지 않는다. -->
      <span v-if="hostBadgeLabel" class="tbm-card__badge tbm-card__badge--host">
        {{ hostBadgeLabel }}
      </span>

      <span
        v-if="variant === 'COMPLETED' && completionLabel"
        class="tbm-card__badge"
        :class="completionToneClass"
      >
        {{ completionLabel }}
      </span>
    </div>

    <p v-if="managerText" class="tbm-card__manager">{{ managerText }}</p>
    <p v-if="dateText" class="tbm-card__date">{{ dateText }}</p>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 'AVAILABLE' | 'IN_PROGRESS' | 'COMPLETED'
  variant: { type: String, default: 'AVAILABLE' },
  // 세션 카드 데이터(서버 응답 항목 1건)
  //  공통: { sessionCd, title, managerUserNm, openedAt, startedAt, endedAt, completionStatusCd }
  //  T5 추가: { hostCmpnyNm } ← 타사(연동) 세션일 때만 존재. 자사 세션은 undefined(배지 미표시).
  session: { type: Object, default: () => ({}) },
})

defineEmits(['select'])

// 개최사 배지(타사 세션 구분) — 자사 세션이면 빈 문자열(배지 미표시, 기존 UI 무변화)
const hostBadgeLabel = computed(() => props.session?.hostCmpnyNm || '')

// 개설자 표기("개설자 홍길동")
const managerText = computed(() => {
  const nm = props.session?.managerUserNm
  return nm ? `개설자 ${nm}` : ''
})

// 탭별 표시 일시 1행(라벨 + 일시)
const dateText = computed(() => {
  const s = props.session || {}
  switch (props.variant) {
    case 'IN_PROGRESS':
      return s.startedAt ? `${s.startedAt} 시작` : ''
    case 'COMPLETED':
      // 종료일시 출처(세션 ENDED_AT vs 내 EXIT_AT)는 백엔드 계약에서 결정(plan §6 Q10).
      return s.endedAt ? `${s.endedAt} 종료` : ''
    case 'AVAILABLE':
    default:
      return s.openedAt ? `${s.openedAt} 개설` : ''
  }
})

// 완료 탭 이수상태 배지
const completionLabel = computed(() => {
  switch (props.session?.completionStatusCd) {
    case 'COMPLETED':
      return '이수'
    case 'NOT_COMPLETED':
      return '미이수'
    default:
      return ''
  }
})
const completionToneClass = computed(() =>
  props.session?.completionStatusCd === 'COMPLETED'
    ? 'tbm-card__badge--ok'
    : 'tbm-card__badge--muted',
)
</script>

<style scoped>
.tbm-card {
  width: 100%;
  text-align: left;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-md);
  cursor: pointer;
  font-family: inherit;
  display: block;
}
.tbm-card:active {
  background: var(--color-bg);
}
.tbm-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.tbm-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.tbm-card__badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.tbm-card__badge--ok {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.tbm-card__badge--muted {
  background: var(--color-danger-tint);
  color: var(--color-danger-text);
}
/* 개최사(타사 연동) 배지 — 중립 톤(이수 배지와 시각적으로 구분) */
.tbm-card__badge--host {
  margin-left: auto;
  background: var(--color-bg);
  border: 0.5px solid var(--color-border);
  color: var(--color-text-secondary);
  font-weight: 500;
  max-width: 40%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tbm-card__manager {
  margin: var(--space-sm) 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.tbm-card__date {
  margin: var(--space-xs) 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
