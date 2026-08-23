<!--
  ApprovalLineDetailSheet.vue — 결재라인 상세(진행 현황) 바텀시트
  - 작업 ID: PRAFTA-내승인요청결재라인-2 / UI-내승인요청결재라인-1
  - 참조 패턴: BaseBottomSheet(셸) + RequestStatusFilterSheet(시트 사용법) + ApprovalLineSection(단계 행 레이아웃, 읽기전용 변형)
  - props.item: MyReqItemResponse (클릭된 카드 원본 — reqId/reqTypeDisplay/targetYmdDisplay 표시용)
  - 본 시트는 근태 요청(01~06,10)에서만 열린다 — LC_MOVE/LC_DELETE는 RequestCard.vue 가드(PRAFTA-내승인요청결재라인-3)로
    "자세히" 진입점 자체가 없어 이 컴포넌트에 도달하지 않는다.
-->
<template>
  <BaseBottomSheet
    :model-value="modelValue"
    title="결재 진행 현황"
    :show-footer="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 컨텍스트 라인 -->
    <p class="aprv-detail__context">
      {{ item?.reqTypeDisplay }} · {{ item?.targetYmdDisplay }}
      <span v-if="showSiteBadge" class="aprv-detail__site-badge"
        >당시 소속: {{ item?.siteName }}</span
      >
    </p>

    <!-- 로딩 -->
    <p v-if="isLoading" class="aprv-detail__state">불러오는 중...</p>

    <!-- 에러 -->
    <div v-else-if="errorMessage" class="aprv-detail__state aprv-detail__state--error">
      <p>{{ errorMessage }}</p>
      <button type="button" class="aprv-detail__retry" @click="onRetry">다시 시도</button>
    </div>

    <!-- 빈 상태 -->
    <p v-else-if="steps.length === 0" class="aprv-detail__state">
      등록된 결재라인이 없습니다.
    </p>

    <!-- 단계 리스트 -->
    <ol v-else class="aprv-detail__list">
      <li v-for="step in steps" :key="step.approvalStep" class="aprv-step">
        <span class="aprv-step__badge">{{ step.approvalStep }}</span>
        <div class="aprv-step__body">
          <div class="aprv-step__top">
            <span class="aprv-step__name">{{ step.approverUserNm }}</span>
            <span class="aprv-step__status" :class="stepStatusClass(step.stepStatus)">
              <span class="aprv-step__status-dot" aria-hidden="true"></span>
              {{ step.stepStatusDisplay }}
            </span>
          </div>
          <p v-if="step.approvalDate" class="aprv-step__date">{{ step.approvalDate }}</p>
          <p v-if="step.approvalComment" class="aprv-step__comment">
            {{ step.approvalComment }}
          </p>
        </div>
      </li>
    </ol>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null }, // MyReqItemResponse (reqId 포함)
  // 작업지시서_소속이동-이력가시성-보정 T3.
  currentSiteCd: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const showSiteBadge = computed(() => {
  const itemSite = props.item?.siteCd
  const current = props.currentSiteCd
  return !!itemSite && !!current && itemSite !== current
})

// 반응형 상태
const isLoading = ref(false)
const errorMessage = ref('')
const steps = ref([]) // ApprovalStepItemResponse[]

// GET /appApi/req06/my/{reqId}/approval-line 호출 → steps/isLoading/errorMessage 갱신
// stale-response 가드: fetch 시작 시점의 reqId를 캡처해두고, 응답 도착 시점에
// 시트가 열려있고(props.modelValue) 그 reqId가 여전히 현재 카드와 일치할 때만 상태를 갱신한다.
// (카드 A 조회 중 닫고 응답 빠른 카드 B를 열면 A의 응답이 나중에 와도 B 상태를 덮어쓰지 않도록 방지)
const fetchApprovalLine = async () => {
  const reqId = props.item?.reqId
  // reqId 가 없으면(방어) 호출하지 않는다.
  if (!reqId) return

  const isCurrent = () => props.modelValue && props.item?.reqId === reqId

  isLoading.value = true
  errorMessage.value = ''
  try {
    const { data } = await api.get(`/appApi/req06/my/${reqId}/approval-line`)
    if (!isCurrent()) return // stale 응답 폐기
    steps.value = Array.isArray(data?.steps) ? data.steps : []
  } catch (e) {
    console.error('[ApprovalLineDetailSheet] 결재라인 조회 실패:', e?.message)
    if (!isCurrent()) return // stale 응답 폐기
    // 403(소유권 검증 실패 등)을 포함해 백엔드 메시지를 우선 노출, 없으면 기본 문구.
    errorMessage.value = resolveApiErrorMessage(
      e,
      '결재라인을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    if (isCurrent()) isLoading.value = false
  }
}

const onRetry = () => {
  fetchApprovalLine()
}

// 시트가 열릴 때 조회, 닫힐 때 상태 리셋(다음 오픈 시 이전 카드의 잔여 데이터가 잠깐 보이지 않도록).
watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      fetchApprovalLine()
    } else {
      steps.value = []
      errorMessage.value = ''
      isLoading.value = false
    }
  },
)

// 단계 상태(SYS044 00/01/02/03) → 표시 톤 매핑 (RequestCard.badgeClass 와 동일 패턴의 단순 UI 토글)
const stepStatusClass = (code) => {
  switch (code) {
    case '02':
      return 'aprv-step__status--primary' // 승인
    case '03':
      return 'aprv-step__status--danger' // 반려
    case '00':
    case '01':
    default:
      return 'aprv-step__status--warning' // 대기중/신청
  }
}
</script>

<style scoped>
.aprv-detail__context {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.aprv-detail__site-badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  margin-left: 6px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 500;
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
  white-space: nowrap;
  vertical-align: middle;
}

.aprv-detail__state {
  margin: 0;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.aprv-detail__state--error {
  color: var(--color-danger);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.aprv-detail__retry {
  height: 36px;
  padding: 0 16px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}

.aprv-detail__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aprv-step {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}

.aprv-step__badge {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
}

.aprv-step__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.aprv-step__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.aprv-step__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.aprv-step__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}
.aprv-step__status-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
}
.aprv-step__status--warning {
  background: var(--color-warning-tint);
  color: var(--color-warning);
}
.aprv-step__status--primary {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.aprv-step__status--danger {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}

.aprv-step__date {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.aprv-step__comment {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 18px;
}
</style>
