<!--
  ReqInboxSiteFilter.vue — 접수함(Attd_10.vue) 사업장 선택 필터 (표시 + 선택, 로직 없음)
  - 연결 작업: 접수함다중사업장권한확장-003 / ATTD10-사업장선택UI개편-002 / UI 명세: UI-002
  - 부모: Attd_10.vue — `.ra-tabs` 아래 / `<ViewHeader>` 위에 삽입. "연차 상신" 탭 제외 4개 탭 공용.
  - 데이터: GET /webApi/reqinbox/accessible-sites → accessibleSites (접수함다중사업장권한확장-002)
  - 정책 출처: request-approval/05-screen-structure.md §5.4 "조직 셀렉터: 사업장 / 전체 부서
    — 사용자 스코프 내에서만 노출". 접근 가능 사업장이 1개 이하이면 컴포넌트가 스스로 숨는다.
  - ★2026-08-28 UI 개편(ATTD10-사업장선택UI개편-002): 내부 구현을 BaseSelect 드롭다운에서
    SiteSelectorButtons.vue("코드/버튼/네임" 칩) 위임으로 교체. 외부 계약(props/emit)은 무변경 —
    Attd_10.vue 는 이 교체로 인해 수정할 필요가 없다(watch(selectedSiteCd) 트리거 그대로 유지).
    카드 박스 스타일(테두리/배경/여백)은 SiteSelectorButtons.vue 가 자체 소유하므로 이 컴포넌트는
    순수 pass-through 래퍼로 축소되었다.
  - 참조 패턴: SiteSelectorButtons.vue(신규 공용 컴포넌트, UI-001)
  - TODO(developer): v-model(selectedSiteCd) 변경 시 접수함 재조회 트리거는 부모(Attd_10.vue)의
    책임이다 — 본 컴포넌트는 값 전달만 한다(비즈니스 로직 금지). 교체 후 4개 탭(스케줄 수정/근태
    보정/초과근무 상신/근무타입 변경) 전부에서 회귀 확인 필요(qa 인계 사항).
-->
<template>
  <SiteSelectorButtons
    :accessible-sites="accessibleSites"
    :model-value="modelValue"
    :loading="loading"
    @update:model-value="onSelect"
  />
</template>

<script setup>
import { defineProps, defineEmits } from "vue";
import SiteSelectorButtons from "@/components/common/SiteSelectorButtons.vue";

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
/* 카드 박스 스타일(테두리/배경/radius/여백)과 칩 스타일은 모두 SiteSelectorButtons.vue 로
   이관되었다. 이 컴포넌트는 Attd_10.vue 와의 기존 계약(props/emit 시그니처)을 유지하기 위한
   얇은 pass-through 래퍼이며 자체 스타일을 갖지 않는다. */
</style>
