<!--
  AdminApprovalCard.vue — 승인 요청 카드 (대기/이력 공용)
  - 작업 ID: 001-P2-F3 (분해: 001-phase2-admin-approval-plan.md §2-1 / §5.5)
  - 참조 패턴: views/req/components/RequestCard.vue (사용자측 요청 카드)
  - props.item: 정규화 요청 아이템(서버 pending/history 응답 1건)
      { reqId, group, reqTypeNm, requesterUserNm, nodeNm, targetYmdDisplay, summaryLines:[],
        reqDateDisplay, deadlineDday, deadlineLevel('danger'|'warning'|'normal'),
        selfYn('Y'|'N'), lockedYn(bool), lockedByNm,
        reqStatus, reqStatusNm(이력 시), rejectReason(이력 반려 시) }
  - ② 본인 결재 차단 / ③ 선점(처리 중) / ④ 마감(D-N) 배지는 서버 산출값 렌더만(C1).
  - 디자인 토큰: 부모(.admin-approval-view) 상속.
  - planner 라운드 스코프: template + style 완성 + 표시 파생(computed)만.
-->
<template>
  <article
    class="apc"
    :class="{ 'apc--locked': item.lockedYn }"
    role="button"
    tabindex="0"
    @click="$emit('click')"
  >
    <!-- 상단: 유형 + 상태/배지 -->
    <header class="apc__top">
      <span class="apc__type">{{ item.reqTypeNm || '-' }}</span>
      <span class="apc__badges">
        <!-- 본인 결재(②) -->
        <span v-if="item.selfYn === 'Y'" class="apc__chip apc__chip--self">본인</span>
        <!-- 선점 중(③) -->
        <span v-if="item.lockedYn" class="apc__chip apc__chip--locked">
          {{ item.lockedByNm || '다른 관리자' }} 처리 중
        </span>
        <!-- 이력 상태(처리결과) — 이력 카드에서만 reqStatusNm 존재 -->
        <span v-if="item.reqStatusNm" class="apc__chip" :class="statusChipClass">
          {{ item.reqStatusNm }}
        </span>
      </span>
    </header>

    <!-- 본문: 요청자 + 대상일자 + 요약 -->
    <div class="apc__body">
      <p class="apc__requester">
        <span class="apc__requester-name">{{ item.requesterUserNm || '-' }}</span>
        <span v-if="item.nodeNm" class="apc__requester-dept">{{ item.nodeNm }}</span>
      </p>
      <p v-if="item.targetYmdDisplay" class="apc__target">{{ item.targetYmdDisplay }}</p>
      <p
        v-for="(line, idx) in item.summaryLines || []"
        :key="idx"
        class="apc__summary"
      >
        {{ line }}
      </p>
    </div>

    <!-- 반려 사유(이력 반려 건만) -->
    <div v-if="item.rejectReason" class="apc__reject">
      <span class="apc__reject-label">반려 사유</span>
      <span class="apc__reject-body">{{ item.rejectReason }}</span>
    </div>

    <!-- 하단 메타: 요청일시 + 마감 배지(④) -->
    <footer class="apc__meta">
      <span class="apc__meta-date">{{ item.reqDateDisplay || '' }}</span>
      <span v-if="hasDeadline" class="apc__deadline" :class="deadlineClass">
        {{ deadlineText }}
      </span>
    </footer>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
})

defineEmits(['click'])

// 마감 배지(④): deadlineDday 가 숫자일 때만 표시. deadlineLevel 로 색상 분기(서버 산출).
const hasDeadline = computed(
  () => props.item?.deadlineDday !== null && props.item?.deadlineDday !== undefined,
)
const deadlineText = computed(() => {
  const d = props.item?.deadlineDday
  if (d === null || d === undefined) return ''
  if (Number(d) <= 0) return '마감 도래'
  return `마감 D-${d}`
})
const deadlineClass = computed(() => {
  switch (props.item?.deadlineLevel) {
    case 'danger':
      return 'apc__deadline--danger'
    case 'warning':
      return 'apc__deadline--warning'
    default:
      return 'apc__deadline--normal'
  }
})

// 이력 처리결과 칩 색상(SYS033: 02승인 / 03반려 / 04취소)
const statusChipClass = computed(() => {
  switch (props.item?.reqStatus) {
    case '02':
      return 'apc__chip--primary'
    case '03':
      return 'apc__chip--danger'
    case '04':
      return 'apc__chip--neutral'
    default:
      return 'apc__chip--neutral'
  }
})
</script>

<style scoped>
.apc {
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
.apc--locked {
  opacity: 0.65;
}

.apc__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.apc__type {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.apc__badges {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}
.apc__chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
}
.apc__chip--self {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.apc__chip--locked {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}
.apc__chip--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.apc__chip--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.apc__chip--neutral {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}

.apc__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.apc__requester {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.apc__requester-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.apc__requester-dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.apc__target {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.apc__summary {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 18px;
  font-variant-numeric: tabular-nums;
}

.apc__reject {
  background: var(--color-bg);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 17px;
}
.apc__reject-label {
  color: var(--color-text-primary);
  font-weight: 500;
  margin-right: 6px;
}

.apc__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 0.5px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
.apc__deadline {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 700;
}
.apc__deadline--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.apc__deadline--warning {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.apc__deadline--normal {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
</style>
