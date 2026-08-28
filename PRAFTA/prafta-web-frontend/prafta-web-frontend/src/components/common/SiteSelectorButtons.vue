<!--
  SiteSelectorButtons.vue — "코드 / 버튼 / 네임" 형태의 사업장 선택 공용 컴포넌트 (표시 + 선택, 로직 없음)
  - 연결 작업: ATTD10-사업장선택UI개편-001 / UI 명세: UI-001
  - 최초 소비처: ReqInboxSiteFilter.vue (Attd_10.vue "요청 승인 관리" 4개 탭 공용 필터)
  - 데이터: 호출부가 이미 조회해 둔 accessibleSites를 props로 받는다. 이 컴포넌트는 별도 API 호출을 하지 않는다.
  - 정책 출처: 없음(순수 UI/UX 개선). 단, "접근 가능 사업장 1개 이하이면 스스로 숨는다" 무회귀 원칙은
    기존 ReqInboxSiteFilter.vue(request-approval/05-screen-structure.md §5.4 근거)에서 그대로 계승한다.
  - 참조 패턴: AttdNeighborDaySegments.vue(props-only 표시 컴포넌트·CSS 변수+rem), button.css .btn-primary
    (선택 강조 색상·포커스 링 규약).
  - TODO(developer): v-model(selectedSiteCd 등) 변경 시 재조회 트리거는 호출부(부모)의 책임이다 —
    본 컴포넌트는 값 전달만 한다(비즈니스 로직 금지).
-->
<template>
  <div v-if="accessibleSites.length > 1" class="site-selector-buttons">
    <span v-if="label" class="site-selector-buttons__label">{{ label }}</span>

    <div
      ref="scrollEl"
      class="site-selector-buttons__scroll"
      @wheel="onWheel"
    >
      <div
        class="site-selector-buttons__list"
        role="group"
        aria-label="사업장 선택"
      >
        <button
          type="button"
          class="site-selector-buttons__chip"
          :class="{
            'site-selector-buttons__chip--active': modelValue === '',
          }"
          :aria-pressed="modelValue === ''"
          :disabled="loading"
          @click="onSelect('')"
        >
          <span class="site-selector-buttons__chip-name">{{ allOptionLabel }}</span>
          <span class="site-selector-buttons__chip-count"
            >({{ accessibleSites.length }})</span
          >
        </button>

        <button
          v-for="site in accessibleSites"
          :key="site.siteCd"
          type="button"
          class="site-selector-buttons__chip"
          :class="{
            'site-selector-buttons__chip--active': modelValue === site.siteCd,
          }"
          :aria-pressed="modelValue === site.siteCd"
          :disabled="loading"
          @click="onSelect(site.siteCd)"
        >
          <span class="site-selector-buttons__chip-code">{{ site.siteCd }}</span>
          <span class="site-selector-buttons__chip-name">{{ site.siteNm }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref } from "vue";

const scrollEl = ref(null);

// 데스크탑 마우스 휠(세로 스크롤)을 가로 스크롤로 변환 — 칩이 많아져 스크롤이 필요해져도
// 사용자가 shift+휠 같은 별도 조작 없이 자연스럽게 좌우로 넘길 수 있게 한다.
const onWheel = (e) => {
  if (!scrollEl.value || e.deltaY === 0 || e.deltaX !== 0) return;
  if (scrollEl.value.scrollWidth <= scrollEl.value.clientWidth) return;
  e.preventDefault();
  scrollEl.value.scrollLeft += e.deltaY;
};

const props = defineProps({
  // 서버 완성 목록 — [{siteCd, siteNo, siteNm}]. 이 컴포넌트는 필터링/정렬을 하지 않는다.
  accessibleSites: { type: Array, default: () => [] },
  // 선택된 siteCd. 빈 문자열 = "전체".
  modelValue: { type: String, default: "" },
  // 호출부의 조회 로딩 상태 — 로딩 중 칩 비활성화.
  loading: { type: Boolean, default: false },
  // 좌측 라벨 텍스트. 빈 문자열로 넘기면 라벨을 렌더링하지 않는다(다른 화면에서 재사용 대비).
  label: { type: String, default: "사업장" },
  // "전체" 칩의 표시 문구(호출부마다 문구를 다르게 쓸 수 있도록 커스터마이즈 허용).
  allOptionLabel: { type: String, default: "전체 사업장" },
});

const emit = defineEmits(["update:modelValue"]);

// UI 토글 수준의 값 전달만 수행 — 재조회/API 호출은 호출부 책임(TODO(developer) 참조).
const onSelect = (value) => {
  emit("update:modelValue", value);
};
</script>

<style scoped>
/* 웹 토큰 파일(tokens.css)에는 spacing 토큰이 없다. 색상/radius만 CSS 변수, 간격은 rem
   (ReqInboxSiteFilter.vue·AttdNeighborDaySegments.vue와 동일 규약). */
.site-selector-buttons {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  margin-bottom: 0.75rem;
}

.site-selector-buttons__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  flex-shrink: 0;
}

/* 사업장 수가 늘어나도 줄바꿈으로 필터 바 높이가 들쭉날쭉해지지 않도록, 화면 크기와
   무관하게 항상 한 줄 가로 스크롤로 고정한다(스크롤 가능함이 보이도록 스크롤바 상시 노출 +
   데스크탑 마우스 휠 지원은 스크립트의 onWheel 참조). */
.site-selector-buttons__scroll {
  flex: 1 1 auto;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 0.25rem;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border-strong) transparent;
}

.site-selector-buttons__scroll::-webkit-scrollbar {
  height: 5px;
}

.site-selector-buttons__scroll::-webkit-scrollbar-thumb {
  background: var(--color-border-strong);
  border-radius: 999px;
}

.site-selector-buttons__scroll::-webkit-scrollbar-track {
  background: transparent;
}

.site-selector-buttons__list {
  display: flex;
  flex-wrap: nowrap;
  gap: 0.375rem;
}

/* 칩 전용 스타일 — button.css 의 .btn 계열과 모양(pill·2단 텍스트)이 달라 신규 정의.
   선택 강조 색상/포커스 링은 .btn-primary 규약(tokens.css --color-primary 계열)을 그대로 따른다. */
.site-selector-buttons__chip {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 0.78rem;
  font-weight: 600;
  font-family: "Pretendard", sans-serif;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.site-selector-buttons__chip:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.site-selector-buttons__chip:focus-visible {
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width) var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}

.site-selector-buttons__chip:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 선택 강조 — button.css .btn-primary 와 동일한 배경/글자색 규약(흰 글자는 button.css 전역
   선례를 그대로 따름 — tokens.css에 --color-on-primary 토큰이 없어 #fff 직접 지정이 기존 관례). */
.site-selector-buttons__chip--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
}

.site-selector-buttons__chip--active:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #ffffff;
}

.site-selector-buttons__chip-code {
  font-variant-numeric: tabular-nums;
  opacity: 0.85;
}

.site-selector-buttons__chip-name {
  white-space: nowrap;
}

.site-selector-buttons__chip-count {
  opacity: 0.75;
}

/* 좁은 화면: 라벨까지 한 줄에 우겨넣지 않고 위로 뺀다(칩 가로 스크롤 자체는 이제
   전 화면 크기에서 공통 동작이라 별도 처리 불요 — 작업지시서 §검증 포인트
   "반응형에서 버튼/칩이 깨지지 않는지"). */
@media (max-width: 768px) {
  .site-selector-buttons {
    align-items: flex-start;
  }
}
</style>
