<!--
  TbmRiskInfoSheet.vue — 위험성평가 정보 바텀시트 (web Risk_03 "위험성 평가 정보" 팝업 내용)
  - 작업 ID: PRAFTA-TBM-RISK-SHEET (분해: prafta-app-tbm-user-detail-plan.md §4 F6, §3 A7)
  - 트리거: 교육중 화면(TbmInProgressView)에서 위험성 항목 클릭 시 부모가 open(선택 risk 전달).
  - 표시 필드: web SessionRiskItem(Tbm02Mapper.selectSessionRisks) 기준
      공정(processNm) / 위험요인유형(riskTypeNm) / 유해·위험요인(hazardNm) / 평가상태(assessmentStatusNm)
    ⚠️ Risk_03 팝업이 추가로 점수/대책 등을 노출한다면 백엔드 A7 응답 확장 필요(plan §6 Q5: developer 확인).
  - 참조 패턴: BaseBottomSheet.vue, OffsiteReasonSheet.vue(토큰 자급).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + 단순 표시만.
-->
<template>
  <transition name="tbm-sheet-fade">
    <div
      v-if="modelValue"
      class="risk-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="위험성 평가 정보"
      @click.self="onClose"
    >
      <div class="risk-sheet">
        <div class="risk-sheet__handle" aria-hidden="true"></div>

        <header class="risk-sheet__header">
          <h2 class="risk-sheet__title">위험성 평가 정보</h2>
          <button type="button" class="risk-sheet__close" aria-label="닫기" @click="onClose">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="risk-sheet__body">
          <dl class="risk-info">
            <div class="risk-info__row">
              <dt class="risk-info__dt">공정</dt>
              <dd class="risk-info__dd">{{ risk.processNm || '-' }}</dd>
            </div>
            <div class="risk-info__row">
              <dt class="risk-info__dt">위험요인 유형</dt>
              <dd class="risk-info__dd">{{ risk.riskTypeNm || '-' }}</dd>
            </div>
            <div class="risk-info__row">
              <dt class="risk-info__dt">유해·위험요인</dt>
              <dd class="risk-info__dd">{{ risk.hazardNm || '-' }}</dd>
            </div>
            <div class="risk-info__row">
              <dt class="risk-info__dt">평가 상태</dt>
              <dd class="risk-info__dd">
                <span class="risk-info__badge">{{ risk.assessmentStatusNm || '-' }}</span>
              </dd>
            </div>
          </dl>

          <!-- ⚠️ developer 확인 필요(Q5): Risk_03 팝업의 추가 상세(점수/대책 등)가 필요하면
               A7 응답 확장 후 이 영역에 추가 표시. -->
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 선택된 위험성 항목 1건(SessionRiskItem):
  //  { processNm, riskTypeNm, hazardNm, assessmentStatusNm, assessmentStatus, ... }
  risk: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue'])

const onClose = () => {
  emit('update:modelValue', false)
}

// props 사용(린트 가드)
void props
</script>

<style scoped>
.risk-sheet__dimmer {
  /* 토큰 자급 */
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-sm: 6px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 120;
}
.risk-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}
.risk-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.risk-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.risk-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.risk-sheet__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.risk-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
}
.risk-info {
  margin: 0;
}
.risk-info__row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
  padding: var(--space-md) 0;
  border-bottom: 0.5px solid var(--color-border-light);
}
.risk-info__row:last-child {
  border-bottom: 0;
}
.risk-info__dt {
  flex-shrink: 0;
  width: 96px;
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.risk-info__dd {
  flex: 1;
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-word;
}
.risk-info__badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.tbm-sheet-fade-enter-active,
.tbm-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.tbm-sheet-fade-enter-from,
.tbm-sheet-fade-leave-to {
  opacity: 0;
}
</style>
