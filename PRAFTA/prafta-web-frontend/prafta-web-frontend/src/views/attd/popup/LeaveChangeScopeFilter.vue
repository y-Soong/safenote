<!--
  LeaveChangeScopeFilter.vue — 접수함(Attd_10.vue) "연차 변경 요청 대기" 소섹션 전용
  사업장·부서·하위부서 포함 축약형 필터 (표시 + 선택, 로직 없음)
  - 연결 작업: 접수함연차변경다중사업장확장-003 / UI 명세: UI-001
  - 부모: Attd_10.vue — `.ra-inbox__block--leavechange` 의 `.ra-inbox__head` 바로 아래
  - 데이터: 사업장 목록 = 부모가 이미 보유한 accessibleSites(GET /webApi/reqinbox/accessible-sites,
    ReqInboxSiteFilter 와 동일 출처 재사용 — 별도 조회 없음). 부서 목록 = 부모가
    GET /comApi/baseinfo/site-node-lists?siteCd=... 로 조회해 nodeList prop 으로 내려준다
    (Attd_13.vue fnLoadNodeList 패턴 재사용 — 신규 API 아님).
  - 정책 출처: request-approval/05-screen-structure.md §5.4 "조직 셀렉터: 사업장/전체 부서 —
    사용자 스코프 내에서만 노출", common/08-permissions.md §8.4(조직 스코프 규칙).
  - 참조 패턴: ReqInboxSiteFilter.vue(사업장 셀렉트 구조), Attd_13.vue .checkbox-label(하위부서 포함).
  - 부서 셀렉트/하위부서 체크박스 활성 여부는 부모가 계산해 nodeEnabled prop 으로 내려준다(사업장이
    정확히 1곳으로 좁혀졌는지 판단은 사업장 목록·역할을 아는 부모의 책임 — 본 컴포넌트는 로직 없음).
  - TODO(developer): v-model 변경 시 목록 재조회·부서목록 재조회 트리거는 모두 부모(Attd_10.vue) 책임.
-->
<template>
  <div class="lc-scope-filter">
    <div class="lc-scope-filter__row">
      <label class="lc-scope-filter__label" for="lcScopeSiteSelect">사업장</label>
      <BaseSelect
        id="lcScopeSiteSelect"
        class="lc-scope-filter__select"
        :model-value="siteCd"
        :disabled="loading"
        @update:model-value="onSiteChange"
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

    <div class="lc-scope-filter__row">
      <label class="lc-scope-filter__label" for="lcScopeNodeSelect">부서</label>
      <BaseSelect
        id="lcScopeNodeSelect"
        class="lc-scope-filter__select"
        :model-value="nodeCd"
        :disabled="!nodeEnabled || nodeListLoading"
        @update:model-value="onNodeChange"
      >
        <option value="">전체 부서</option>
        <option
          v-for="node in nodeList"
          :key="node.nodeCd"
          :value="node.nodeCd"
        >
          {{ node.nodeNm }}
        </option>
      </BaseSelect>
    </div>

    <label class="lc-scope-filter__checkbox checkbox-label">
      <input
        type="checkbox"
        :checked="incSubNodeYn === 'Y'"
        :disabled="!nodeEnabled"
        @change="onIncSubNodeChange"
      />
      하위부서 포함
    </label>

    <span v-if="!nodeEnabled" class="lc-scope-filter__hint">
      부서 필터는 사업장을 1곳 선택하면 사용할 수 있어요.
    </span>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from "vue";
import BaseSelect from "@/components/common/BaseSelect.vue";

const props = defineProps({
  // 서버 완성 목록 — [{siteCd, siteNo, siteNm}]. 부모가 이미 보유한 accessibleSites 재사용.
  accessibleSites: { type: Array, default: () => [] },
  // 선택된 siteCd. 빈 문자열 = "전체 사업장".
  siteCd: { type: String, default: "" },
  // 부모가 GET /comApi/baseinfo/site-node-lists 로 조회한 [{nodeCd, nodeNm}] 목록.
  nodeList: { type: Array, default: () => [] },
  nodeCd: { type: String, default: "" },
  incSubNodeYn: { type: String, default: "Y" },
  // 부서 셀렉트/하위부서 체크박스 활성 여부 — 부모가 "사업장 1곳으로 좁혀졌는지" 판단해 전달.
  nodeEnabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  nodeListLoading: { type: Boolean, default: false },
});

const emit = defineEmits(["update:siteCd", "update:nodeCd", "update:incSubNodeYn"]);

// UI 토글 수준의 값 전달만 수행 — 재조회/사업장→부서 연쇄 로직은 부모 책임(TODO(developer) 참조).
const onSiteChange = (value) => {
  emit("update:siteCd", value);
};
const onNodeChange = (value) => {
  emit("update:nodeCd", value);
};
const onIncSubNodeChange = (e) => {
  emit("update:incSubNodeYn", e.target.checked ? "Y" : "N");
};
</script>

<style scoped>
/* Attd_10.vue 의 .ra-* 톤(var(--color-x, #fallback) 관례)을 그대로 따르되, 소섹션 헤더에
   들어가는 축약형이라 ReqInboxSiteFilter 보다 padding/gap 을 줄인다(요청서 §2-3 "UI가 깨지지
   않도록" — Attd_13.vue 의 전면 검색바를 그대로 이식하지 않는다). */
.lc-scope-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-surface, #fff);
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.lc-scope-filter__row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.lc-scope-filter__label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text, #374151);
  white-space: nowrap;
}

.lc-scope-filter__select {
  min-width: 8rem;
}

.lc-scope-filter__checkbox {
  margin-left: -0.15rem;
}

.lc-scope-filter__hint {
  font-size: 0.72rem;
  color: var(--color-text-muted, #9ca3af);
}

/* 하위부서 포함 체크박스 (Attd_13.vue .checkbox-label 패턴 차용 — 축약형이라 폰트만 살짝 축소) */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  width: 12px;
  height: 12px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}

.checkbox-label input[type="checkbox"]:disabled {
  cursor: not-allowed;
}
</style>
