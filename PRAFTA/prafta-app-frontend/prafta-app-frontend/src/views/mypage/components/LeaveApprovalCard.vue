<!--
  LeaveApprovalCard.vue — 연차 결재 카드 (대기/이력 공용, 사용자 모드)
  - 작업: 사용자연차결재-02 / UI 명세: UI-LA03
  - props.item: 서버 pending/history 응답 1건
      { reqId, approvalStep, reqTypeNm, requesterUserNm, nodeNm, targetYmd,
        leaveNm, unitNm, leaveDays, startTime, endTime, reqDate, selfYn,
        // (이력 전용) myDecision('02'|'03'), myDecisionNm, myComment }
  - props.mode: 'pending' | 'history'
  - 디자인 토큰: 부모(.leave-approval-view) 상속.
  - planner 스코프: template + style + 단순 표시 파생(computed)만.
-->
<template>
  <article class="lac" role="button" tabindex="0" @click="$emit('click')">
    <!-- 상단: 연차종류 + 배지 -->
    <header class="lac__top">
      <span class="lac__type">
        {{ item.leaveNm || item.reqTypeNm || '연차' }}
        <span v-if="item.unitNm" class="lac__unit">· {{ item.unitNm }}</span>
        <span v-if="leaveDaysText" class="lac__unit">· {{ leaveDaysText }}</span>
      </span>
      <span class="lac__badges">
        <span v-if="item.selfYn === 'Y'" class="lac__chip lac__chip--self">본인</span>
        <span v-if="mode === 'history' && item.myDecisionNm" class="lac__chip" :class="decisionChipClass">
          {{ item.myDecisionNm }}
        </span>
      </span>
    </header>

    <!-- 본문: 요청자 + 대상일자 + 구간 -->
    <div class="lac__body">
      <p class="lac__requester">
        <span class="lac__requester-name">{{ item.requesterUserNm || '-' }}</span>
        <span v-if="item.nodeNm" class="lac__requester-dept">{{ item.nodeNm }}</span>
      </p>
      <p v-if="item.targetYmd" class="lac__target">{{ targetYmdText }}</p>
      <p v-if="rangeText" class="lac__range">{{ rangeText }}</p>
    </div>

    <!-- 반려 사유(이력 반려 건만) -->
    <div v-if="mode === 'history' && item.myDecision === '03' && item.myComment" class="lac__reject">
      <span class="lac__reject-label">반려 사유</span>
      <span>{{ item.myComment }}</span>
    </div>

    <!-- 하단: 요청일시 -->
    <footer class="lac__meta">
      <span>{{ reqDateText }}</span>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { formatYmdDisplay, formatDateTimeDisplay } from '@/utils/approvalFormat'

const props = defineProps({
  item: { type: Object, required: true },
  mode: { type: String, default: 'pending' }, // 'pending' | 'history'
})
defineEmits(['click'])

// 표시 파생(단순 포맷 — 비즈니스 로직 아님)
const leaveDaysText = computed(() => {
  const d = props.item?.leaveDays
  return d != null ? `${d}일` : ''
})
const targetYmdText = computed(() => fmtYmd(props.item?.targetYmd))
const reqDateText = computed(() => {
  const s = props.item?.reqDate
  return s ? `요청 ${formatDateTimeDisplay(s)}` : ''
})
const rangeText = computed(() => {
  const s = props.item?.startTime
  const e = props.item?.endTime
  if (s && e) return `${fmtHm(s)} ~ ${fmtHm(e)}`
  return ''
})
const decisionChipClass = computed(() =>
  props.item?.myDecision === '02' ? 'lac__chip--primary' : 'lac__chip--danger',
)

// 날짜 표시는 approvalFormat 단일 출처에 위임(점). HHMM → HH:MM 시각 포맷.
function fmtYmd(v) {
  return formatYmdDisplay(v)
}
function fmtHm(v) {
  if (v && v.length === 4) return `${v.slice(0, 2)}:${v.slice(2, 4)}`
  return v || ''
}
</script>

<style scoped>
.lac {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}
.lac__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.lac__type {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.lac__unit {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.lac__badges {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}
.lac__chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
}
.lac__chip--self {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.lac__chip--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.lac__chip--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.lac__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.lac__requester {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.lac__requester-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.lac__requester-dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.lac__target,
.lac__range {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.lac__reject {
  background: var(--color-bg);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 17px;
}
.lac__reject-label {
  color: var(--color-text-primary);
  font-weight: 500;
  margin-right: 6px;
}
.lac__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 0.5px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
</style>
