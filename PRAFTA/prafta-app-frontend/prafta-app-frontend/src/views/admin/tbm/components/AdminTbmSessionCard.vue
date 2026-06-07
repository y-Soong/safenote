<!--
  AdminTbmSessionCard.vue — 관리자 TBM 세션 카드 (교육관리 리스트 항목)
  - 작업 ID: 001-P5-T-F3 (분해: 001-phase5-admin-tbm-plan.md §2-1, §3 T-A1)
  - 표시(요청서): 상태 배지 / 교육 제목 / 출결·이수·미이수.
  - 참조 패턴: 사용자 TBM TbmSessionCard.vue(카드/배지) + web Tbm_02.vue 상태 배지 색 규칙.
  - 디자인 토큰은 부모(.admin-tbm-view)에서 상속(루트 재선언 없음 — 항상 셸 내부에서만 사용).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + 단순 표시 computed 만(데이터 가공 없음).
-->
<template>
  <button type="button" class="admin-tbm-card" @click="$emit('select', session)">
    <div class="admin-tbm-card__head">
      <span class="admin-tbm-card__status" :class="statusToneClass">{{ statusLabel }}</span>
      <p class="admin-tbm-card__title">{{ session.title || 'TBM 세션' }}</p>
    </div>

    <!-- 출결 / 이수 / 미이수 카운트 -->
    <div class="admin-tbm-card__counts">
      <span class="count">
        <span class="count__label">출결</span>
        <span class="count__value">{{ num(session.attendanceCount) }}</span>
      </span>
      <span class="count">
        <span class="count__label">이수</span>
        <span class="count__value count__value--ok">{{ num(session.completedCount) }}</span>
      </span>
      <span class="count">
        <span class="count__label">미이수</span>
        <span class="count__value count__value--danger">{{ num(session.notCompletedCount) }}</span>
      </span>
    </div>

    <p v-if="metaText" class="admin-tbm-card__meta">{{ metaText }}</p>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 세션 1건: { sessionCd, statusCd, statusNm, title, attendanceCount, completedCount, notCompletedCount, managerUserNm, openedAt }
  session: { type: Object, default: () => ({}) },
})

defineEmits(['select'])

// 카운트 표시(없으면 0)
const num = (v) => (v == null ? 0 : v)

// 상태 라벨(서버 statusNm 우선, 없으면 코드 매핑) — SYS046
const STATUS_LABELS = {
  DRAFT: '작성중',
  OPENED: '개설',
  IN_PROGRESS: '진행중',
  COMPLETED: '종료',
  CANCELLED: '취소',
}
const statusLabel = computed(
  () => props.session?.statusNm || STATUS_LABELS[props.session?.statusCd] || props.session?.statusCd || '-',
)

// 상태 배지 톤(web Tbm_02 상태 배지 색 규칙 준용)
const statusToneClass = computed(() => {
  switch (props.session?.statusCd) {
    case 'IN_PROGRESS':
      return 'admin-tbm-card__status--progress'
    case 'OPENED':
      return 'admin-tbm-card__status--opened'
    case 'COMPLETED':
      return 'admin-tbm-card__status--completed'
    case 'CANCELLED':
      return 'admin-tbm-card__status--cancelled'
    case 'DRAFT':
    default:
      return 'admin-tbm-card__status--draft'
  }
})

// 보조 메타(개설자 + 개설일시)
const metaText = computed(() => {
  const s = props.session || {}
  const parts = []
  if (s.managerUserNm) parts.push(`개설자 ${s.managerUserNm}`)
  if (s.openedAt) parts.push(`${s.openedAt} 개설`)
  return parts.join(' · ')
})
</script>

<style scoped>
.admin-tbm-card {
  width: 100%;
  text-align: left;
  display: block;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-md);
  cursor: pointer;
  font-family: inherit;
}
.admin-tbm-card:active {
  background: var(--color-bg);
}

.admin-tbm-card__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 상태 배지 */
.admin-tbm-card__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.admin-tbm-card__status--progress {
  background: var(--color-primary);
  color: var(--color-surface);
}
.admin-tbm-card__status--opened {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.admin-tbm-card__status--draft {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.admin-tbm-card__status--completed {
  background: var(--color-bg);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}
.admin-tbm-card__status--cancelled {
  background: var(--color-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-border);
}

/* 출결/이수/미이수 카운트 */
.admin-tbm-card__counts {
  display: flex;
  gap: var(--space-lg);
  margin-top: var(--space-md);
}
.count {
  display: inline-flex;
  align-items: baseline;
  gap: var(--space-xs);
}
.count__label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.count__value {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.count__value--ok {
  color: var(--color-primary);
}
.count__value--danger {
  color: var(--color-danger);
}

.admin-tbm-card__meta {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
