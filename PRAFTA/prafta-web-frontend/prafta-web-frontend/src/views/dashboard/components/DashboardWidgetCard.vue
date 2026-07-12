<template>
  <!-- 대시보드 위젯 카드 공통 셸 (PRAFTA-DASHBOARD-T1)
       - 제목 + 우측 "바로가기" 이동 버튼 + 본문 slot
       - T2~T5 위젯은 본 컴포넌트의 slot 안에 데이터 표현을 구현한다 -->
  <section class="dash-card">
    <header class="dash-card__head">
      <h3 class="dash-card__title">{{ title }}</h3>
      <!-- 제목과 바로가기 사이의 헤더 액션 영역 (예: A1 기준월 네비) -->
      <div class="dash-card__head-actions">
        <slot name="head-actions" />
      </div>
      <button
        v-if="moveTarget"
        type="button"
        class="dash-card__move"
        @click="$emit('move', moveTarget)"
      >
        {{ moveLabel }} ›
      </button>
    </header>
    <div class="dash-card__body">
      <slot>
        <!-- 기본 placeholder (T2~T5에서 실제 위젯으로 대체) -->
        <div class="dash-card__placeholder">준비 중</div>
      </slot>
    </div>
  </section>
</template>

<script setup>
defineProps({
  // 카드 제목
  title: { type: String, required: true },
  // 이동 대상 라우트명 (예: 'Attd_05'). 없으면 이동 버튼 숨김
  moveTarget: { type: String, default: "" },
  // 이동 버튼 라벨
  moveLabel: { type: String, default: "바로가기" },
});

defineEmits(["move"]);
</script>

<style scoped>
.dash-card {
  background: var(--card-bg, #ffffff);
  border: var(--card-border, 1px solid #e5e7eb);
  border-radius: var(--card-radius, 16px);
  box-shadow: var(--card-shadow, 0 1px 2px rgba(16, 24, 40, 0.06));
  /* 통합 대시보드 무스크롤 목표 — 카드 여백 압축 */
  padding: 0.75rem 0.875rem;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.dash-card__head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.dash-card__title {
  margin: 0;
  /* "대시보드" 페이지 타이틀(dash-title)과 동일 크기 */
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
  white-space: nowrap;
}

/* 헤더 액션 영역: 우측으로 밀어 바로가기 버튼 바로 앞에 위치 (비어 있으면 폭 0) */
.dash-card__head-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.dash-card__move {
  border: 1px solid var(--color-border-strong, #d1d5db);
  background: var(--color-surface, #ffffff);
  color: var(--color-text-muted, #4b5563);
  border-radius: var(--btn-radius, 8px);
  height: var(--btn-height-sm, 26px);
  padding: 0 var(--btn-padding-sm, 10px);
  font-size: var(--btn-font-sm, 11px);
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.dash-card__move:hover {
  color: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
}

.dash-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* T2~T5 구현 전 임시 placeholder */
.dash-card__placeholder {
  flex: 1;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg, #f9fafb);
  border: 1px dashed var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
}
</style>
