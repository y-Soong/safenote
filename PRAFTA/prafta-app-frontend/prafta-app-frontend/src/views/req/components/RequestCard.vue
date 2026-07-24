<!--
  RequestCard.vue — 요청 카드 (SYS032 6종 × SYS033 4종 = 24 케이스)
  - 작업 ID: PRAFTA-APP-006-7
  - props.item: MyReqItemResponse (백엔드 PRAFTA-APP-006-1 응답 1건)
  - PRAFTA-내승인요청결재라인-3(옵션 B, 2026-07-24 확정): LC_MOVE/LC_DELETE(연차 이동/삭제)는
    다단계 결재라인 개념이 없으므로 "자세히" 진입점 자체를 렌더링하지 않는다(대체 뷰도 만들지 않음).
-->
<template>
  <article
    class="req-card"
    :class="{ 'req-card--static': !hasDetailLink }"
    :role="hasDetailLink ? 'button' : undefined"
    :tabindex="hasDetailLink ? 0 : undefined"
    @click="onClick"
  >
    <!-- 상단: 유형 + 상태 배지 -->
    <header class="req-card__top">
      <span class="req-card__type">{{ item.reqTypeDisplay }}</span>
      <span class="req-card__badge" :class="badgeClass">
        <span class="req-card__badge-dot" aria-hidden="true"></span>
        <span class="req-card__badge-label">{{ item.reqStatusDisplay }}</span>
      </span>
    </header>

    <!-- 본문: 대상일자 + 요약 -->
    <div class="req-card__body">
      <p class="req-card__target-ymd">{{ item.targetYmdDisplay }}</p>
      <p
        v-for="(line, idx) in item.summaryLines || []"
        :key="idx"
        class="req-card__summary-line"
      >
        {{ line }}
      </p>
    </div>

    <!-- 반려 사유 (REQ_STATUS='03' 일 때만) -->
    <div v-if="item.reqStatus === '03' && item.rejectReason" class="req-card__reject">
      <span class="req-card__reject-label">반려 사유</span>
      <span class="req-card__reject-dot" aria-hidden="true"> · </span>
      <span class="req-card__reject-body">{{ item.rejectReason }}</span>
    </div>

    <!-- 하단 메타 -->
    <footer class="req-card__meta">
      <span class="req-card__meta-dates">
        {{ item.reqDateDisplay
        }}<template v-if="item.processedDateDisplay"> / {{ item.processedDateDisplay }}</template>
      </span>
      <span v-if="hasDetailLink" class="req-card__meta-more">
        자세히
        <svg width="12" height="12" aria-hidden="true">
          <use href="#i-req-chev-right" />
        </svg>
      </span>
    </footer>

    <!-- 인라인 SVG sprite (본 컴포넌트 전용) -->
    <svg width="0" height="0" class="req-card__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-req-chev-right"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
      </defs>
    </svg>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
})

const emit = defineEmits(['click'])

// PRAFTA-내승인요청결재라인-3: LC_MOVE/LC_DELETE(연차 이동/삭제)는 결재라인 상세 진입 대상 아님.
const hasDetailLink = computed(() => !String(props.item?.reqType || '').startsWith('LC_'))

const onClick = () => {
  if (!hasDetailLink.value) return
  emit('click')
}

const badgeClass = computed(() => {
  switch (props.item?.reqStatus) {
    case '01':
      return 'req-card__badge--warning' // 대기
    case '02':
      return 'req-card__badge--primary' // 승인
    case '03':
      return 'req-card__badge--danger' // 반려
    case '04':
      return 'req-card__badge--neutral' // 취소
    default:
      return 'req-card__badge--neutral'
  }
})
</script>

<style scoped>
.req-card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}
.req-card--static {
  cursor: default;
}

.req-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.req-card__type {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.req-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}
.req-card__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
}

.req-card__badge--warning {
  background: var(--color-warning-tint);
  color: var(--color-warning);
}
.req-card__badge--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.req-card__badge--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.req-card__badge--neutral {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}

.req-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.req-card__target-ymd {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.req-card__summary-line {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-secondary);
  line-height: 18px;
  font-variant-numeric: tabular-nums;
}

.req-card__reject {
  background: var(--color-bg);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-secondary);
  line-height: 17px;
}
.req-card__reject-label {
  color: var(--color-text-primary);
  font-weight: 500;
}
.req-card__reject-dot {
  color: var(--color-text-secondary);
}

.req-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 0.5px solid var(--color-border-light);
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.req-card__meta-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
</style>
