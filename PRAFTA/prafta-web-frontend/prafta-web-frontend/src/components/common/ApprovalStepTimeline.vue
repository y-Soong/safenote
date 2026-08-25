<!--
  ApprovalStepTimeline.vue — 결재 진행 타임라인 (신규 공통 컴포넌트)
  - 작업: 근태결재선통합-P3 (P3-2)
  - 참조 패턴: ApprovalLineDetailSheet.vue(prafta-app-frontend, 단계 카드 레이아웃)
    + TbmEventTimeline.vue(prafta-web-frontend, 웹 타임라인 리스트 마크업/CSS 변수 관례)
  - 순수 표시 컴포넌트 — API 호출 없음. steps 는 호출부(Attd_10.vue)가
    GET /webApi/reqinbox/approval-line(P3-1, 신규) 조회 결과를 그대로 내려준다.
  - step 1건 스키마(ApprovalStepVO 대응, 서버 응답 그대로):
      { approvalStep, approverUserCd, approverUserNm,
        approvalStatus('00'대기/'01'진행중/'02'승인/'03'반려, SYS044),
        approvalStatusNm, approvalComment, approvalDate }
-->
<template>
  <div class="approval-step-timeline">
    <!-- 로딩 — 재기획서 §7.1 "상세 패널 로딩 동안에는 관리자 결정 영역을 스켈레톤으로"와 동일 원칙 -->
    <p v-if="loading" class="approval-step-timeline__state">
      결재선을 불러오는 중...
    </p>

    <!-- 에러 -->
    <div
      v-else-if="errorMessage"
      class="approval-step-timeline__state approval-step-timeline__state--error"
    >
      <p>{{ errorMessage }}</p>
      <button
        type="button"
        class="approval-step-timeline__retry"
        @click="$emit('retry')"
      >
        다시 시도
      </button>
    </div>

    <!-- 빈 상태 -->
    <p v-else-if="!steps || steps.length === 0" class="approval-step-timeline__state">
      등록된 결재선이 없습니다.
    </p>

    <!-- 단계 리스트 -->
    <ol v-else class="approval-step-timeline__list">
      <li
        v-for="step in steps"
        :key="step.approvalStep"
        class="approval-step-timeline__step"
        :class="{
          'approval-step-timeline__step--current': step.approvalStatus === '01',
        }"
      >
        <span class="approval-step-timeline__badge">{{ step.approvalStep }}</span>
        <div class="approval-step-timeline__body">
          <div class="approval-step-timeline__top">
            <span class="approval-step-timeline__name">{{
              step.approverUserNm || step.approverUserCd || "-"
            }}</span>
            <span
              class="approval-step-timeline__status"
              :class="statusClass(step.approvalStatus)"
            >
              <span class="approval-step-timeline__status-dot" aria-hidden="true"></span>
              {{ step.approvalStatusNm || statusLabel(step.approvalStatus) }}
            </span>
          </div>
          <p v-if="step.approvalDate" class="approval-step-timeline__date">
            {{ step.approvalDate }}
          </p>
          <p v-if="step.approvalComment" class="approval-step-timeline__comment">
            {{ step.approvalComment }}
          </p>
        </div>
      </li>
    </ol>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from "vue";

// props 전용 — 이 컴포넌트는 API를 직접 호출하지 않는다(호출부가 조회해 steps 로 내려줌).
defineProps({
  /** ApprovalStepVO[] — 서버 응답을 그대로 전달 (approvalStep 오름차순 정렬은 서버 책임). */
  steps: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: "" },
});

// retry — 재조회는 호출부(Attd_10.vue)의 fetch 함수가 수행한다(TODO(developer)).
defineEmits(["retry"]);

// SYS044 결재 단계 상태 코드 — ApprovalStepGateServiceImpl / ApprovalLineDetailSheet.vue(app) 와 동일 매핑.
// 서버가 approvalStatusNm 을 내려주면 그 값을 우선 쓰고, 없을 때만 이 라벨로 대체한다(폴백).
const STATUS_LABEL = {
  "00": "대기",
  "01": "진행중",
  "02": "승인",
  "03": "반려",
};
const statusLabel = (code) => STATUS_LABEL[code] || code || "-";

// 상태별 톤 매핑 — ApprovalLineDetailSheet.vue(app) stepStatusClass 와 동일 규칙(웹 클래스명만 다름).
const statusClass = (code) => {
  switch (code) {
    case "02":
      return "approval-step-timeline__status--approved";
    case "03":
      return "approval-step-timeline__status--rejected";
    case "00":
    case "01":
    default:
      return "approval-step-timeline__status--pending";
  }
};
</script>

<style scoped>
.approval-step-timeline {
  font-size: 0.85rem;
}

.approval-step-timeline__state {
  margin: 0;
  padding: 1rem 0;
  text-align: center;
  font-size: 0.8rem;
  color: var(--color-text-muted, #9ca3af);
}
.approval-step-timeline__state--error {
  color: var(--color-danger, #ef4444);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
}
.approval-step-timeline__retry {
  height: var(--btn-height-sm, 26px);
  padding: 0 var(--btn-padding-sm, 10px);
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  color: var(--color-text, #374151);
  font-size: var(--btn-font-sm, 11px);
  cursor: pointer;
  font-family: inherit;
}
.approval-step-timeline__retry:hover {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}

.approval-step-timeline__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.approval-step-timeline__step {
  display: flex;
  gap: 0.6rem;
  padding: 0.6rem;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius-lg, 10px);
}
/* 현재 진행 중('01') 단계만 강조 — 그 외 단계는 완료/대기를 나란히 보여주되 시각적으로 조용하게 둔다. */
.approval-step-timeline__step--current {
  border-color: var(--color-primary, #16a34a);
  background: var(--color-primary-tint, #dcfce7);
}

.approval-step-timeline__badge {
  flex-shrink: 0;
  width: 1.4rem;
  height: 1.4rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border-strong, #d1d5db);
  color: var(--color-text, #374151);
  font-size: 0.72rem;
  font-weight: 600;
}
.approval-step-timeline__step--current .approval-step-timeline__badge {
  background: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
  color: #fff;
}

.approval-step-timeline__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.approval-step-timeline__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.approval-step-timeline__name {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--color-text-strong, #111827);
}

.approval-step-timeline__status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  height: 1.3rem;
  padding: 0 0.5rem;
  border-radius: 0.3rem;
  font-size: 0.72rem;
  font-weight: 500;
  flex-shrink: 0;
  white-space: nowrap;
}
.approval-step-timeline__status-dot {
  width: 0.35rem;
  height: 0.35rem;
  border-radius: 999px;
  background: currentColor;
}
.approval-step-timeline__status--pending {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.approval-step-timeline__status--approved {
  background: var(--color-primary-tint, #dcfce7);
  color: var(--color-primary, #16a34a);
}
.approval-step-timeline__status--rejected {
  background: var(--color-danger-tint, #fee2e2);
  color: var(--color-danger, #ef4444);
}

.approval-step-timeline__date {
  margin: 0;
  font-size: 0.72rem;
  color: var(--color-text-muted, #6b7280);
  font-variant-numeric: tabular-nums;
}

.approval-step-timeline__comment {
  margin: 0;
  font-size: 0.78rem;
  color: var(--color-text, #374151);
  line-height: 1.4;
}
</style>
