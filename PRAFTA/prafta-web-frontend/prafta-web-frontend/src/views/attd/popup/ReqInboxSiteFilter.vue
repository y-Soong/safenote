<!--
  ReqInboxSiteFilter.vue — 접수함(Attd_10.vue) 사업장 선택 필터 (표시 + 선택, 로직 없음)
  - 연결 작업: 접수함다중사업장권한확장-003 / UI 명세: UI-001
  - 부모: Attd_10.vue — `.ra-tabs` 아래 / `<ViewHeader>` 위에 삽입
  - 데이터: GET /webApi/reqinbox/accessible-sites → accessibleSites (접수함다중사업장권한확장-002)
  - 정책 출처: request-approval/05-screen-structure.md §5.4 "조직 셀렉터: 사업장 / 전체 부서
    — 사용자 스코프 내에서만 노출". 접근 가능 사업장이 1개 이하이면 컴포넌트가 스스로 숨는다
    (AppAdminAccessServiceImpl.siteSwitchEnabled 와 동일 원칙 — 단일 사업장 관리자 무회귀).
  - 참조 패턴: 동일 폴더 AttdNeighborDaySegments.vue (props-only 표시 컴포넌트 · CSS 변수)
  - TODO(developer): v-model(selectedSiteCd) 변경 시 접수함 재조회 트리거는 부모(Attd_10.vue)의
    책임이다 — 본 컴포넌트는 값 전달만 한다(비즈니스 로직 금지).
-->
<template>
  <div v-if="accessibleSites.length > 1" class="ra-site-filter">
    <label class="ra-site-filter__label" for="raSiteFilterSelect">사업장</label>
    <BaseSelect
      id="raSiteFilterSelect"
      class="ra-site-filter__select"
      :model-value="modelValue"
      :disabled="loading"
      @update:model-value="onSelect"
    >
      <option value="">전체 사업장 ({{ accessibleSites.length }})</option>
      <option
        v-for="site in accessibleSites"
        :key="site.siteCd"
        :value="site.siteCd"
      >
        {{ site.siteNm }}
      </option>
    </BaseSelect>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from "vue";
import BaseSelect from "@/components/common/BaseSelect.vue";

const props = defineProps({
  // 서버 완성 목록 — [{siteCd, siteNo, siteNm}]. 이 컴포넌트는 필터링/정렬을 하지 않는다.
  accessibleSites: { type: Array, default: () => [] },
  // 선택된 siteCd. 빈 문자열 = "전체 사업장".
  modelValue: { type: String, default: "" },
  // 부모의 accessible-sites 조회 로딩 상태 — 조회 중 선택 비활성화.
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);

// UI 토글 수준의 값 전달만 수행 — 재조회/API 호출은 부모 책임(TODO(developer) 참조).
const onSelect = (value) => {
  emit("update:modelValue", value);
};
</script>

<style scoped>
/* Attd_10.vue 의 .ra-* 톤(var(--color-x, #fallback) 관례)을 그대로 따른다. */
.ra-site-filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  margin-bottom: 0.75rem;
}

.ra-site-filter__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text, #374151);
  white-space: nowrap;
}

.ra-site-filter__select {
  min-width: 12rem;
}
</style>
