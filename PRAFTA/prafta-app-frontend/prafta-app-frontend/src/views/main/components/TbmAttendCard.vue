<!--
  TbmAttendCard.vue — TBM 참석 카드 (4상태: 차단/가능/완료/없음)
  - 상세 요청서 §3.5, §4.5
  - 상세 §10 결정사항 12 (4상태 통일), 13 (카운트다운 제거)
  - 4상태별 콜아웃 톤·아이콘·문구·버튼 활성 매트릭스를 한 카드 안에서 분기
-->
<template>
  <div class="card">
    <div class="head-row head-row--tight">
      <p class="card-title">TBM 참석</p>
      <button
        type="button"
        class="head-chev"
        aria-label="자세히 보기"
        @click="$emit('click:detail')"
      >
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>
    </div>

    <!-- 메타 1행 -->
    <p class="tbm-meta" :class="{ 'tbm-meta--muted': isNone }">{{ metaText }}</p>

    <!-- 콜아웃 -->
    <div class="tbm-callout" :class="calloutToneClass">
      <svg class="icon" width="16" height="16" aria-hidden="true">
        <use :href="`#${calloutIconId}`" />
      </svg>
      <span>{{ calloutText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 'BEFORE_CHECK_IN' | 'AVAILABLE' | 'ATTENDED' | 'NONE'
  tbmStatus: {
    type: String,
    default: 'NONE',
  },
  // 세션 시각 (HHMM, 예: "1000")
  sessionTime: {
    type: String,
    default: '',
  },
  // 세션 장소
  sessionLocation: {
    type: String,
    default: '',
  },
  // 진행자
  sessionLeader: {
    type: String,
    default: '',
  },
  // 참석 시각 (HHMM, ATTENDED 상태에서만)
  attendedAt: {
    type: String,
    default: '',
  },
})

defineEmits(['click:detail'])

const isNone = computed(() => props.tbmStatus === 'NONE')

// 메타 1행: "HH:MM · 장소 · 진행자" 또는 "예정된 TBM이 없습니다"
const formatHHMM = (s) => {
  if (!s || s.length < 4) return ''
  return `${s.slice(0, 2)}:${s.slice(2, 4)}`
}
const metaText = computed(() => {
  if (isNone.value) return '예정된 TBM이 없습니다'
  const parts = [formatHHMM(props.sessionTime), props.sessionLocation, props.sessionLeader].filter(
    Boolean,
  )
  return parts.join(' · ')
})

// 콜아웃 톤·아이콘·문구 매트릭스
const calloutToneClass = computed(() => {
  switch (props.tbmStatus) {
    case 'AVAILABLE':
      return 'tbm-callout--warn'
    case 'ATTENDED':
      return 'tbm-callout--success'
    case 'BEFORE_CHECK_IN':
    case 'NONE':
    default:
      return 'tbm-callout--neutral'
  }
})

const calloutIconId = computed(() => {
  switch (props.tbmStatus) {
    case 'BEFORE_CHECK_IN':
      return 'i-lock'
    case 'AVAILABLE':
      return 'i-clock'
    case 'ATTENDED':
      return 'i-circlecheck'
    case 'NONE':
    default:
      return 'i-caloff'
  }
})

const calloutText = computed(() => {
  switch (props.tbmStatus) {
    case 'BEFORE_CHECK_IN':
      return '근무 중에만 참석 가능'
    case 'AVAILABLE':
      // 동적 카운트다운 금지 (상세 §3.5.4)
      return '늦지 않게 참석해 주세요'
    case 'ATTENDED':
      return `${formatHHMM(props.attendedAt) || '--:--'} 참석 완료`
    case 'NONE':
    default:
      return '오늘은 TBM 일정이 없어요'
  }
})
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--color-border);
  padding: 16px;
  margin-bottom: 12px;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.head-row--tight {
  margin-bottom: 4px;
}

.head-chev {
  background: transparent;
  border: 0;
  padding: 4px;
  min-width: 44px;
  min-height: 44px;
  margin: -10px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-family: inherit;
}

.tbm-meta {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.tbm-meta--muted {
  color: var(--color-text-tertiary);
}

.tbm-callout {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
}
.tbm-callout--warn {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.tbm-callout--success {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.tbm-callout--neutral {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}

.tbm-btn {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0;
  font-family: inherit;
}
.tbm-btn--active {
  background: #ffffff;
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}
.tbm-btn--disabled {
  background: var(--color-bg);
  color: var(--color-text-tertiary);
  border: 1px solid var(--color-border);
  cursor: not-allowed;
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
