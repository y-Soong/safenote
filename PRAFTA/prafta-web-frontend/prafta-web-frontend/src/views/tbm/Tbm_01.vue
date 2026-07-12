<template>
  <div class="viewComm tbm01-container">
    <!-- TBM_AI-RB-2: [교육자료 관리]/[AI 분석 관리] 2탭.
         Attd_01 표준: 셸은 탭바만 갖고, 각 탭 자식이 자기 ViewHeader 를 소유한다. -->
    <div class="tbm01-tab-bar">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        :class="['tbm01-tab-btn', { active: activeTab === t.key }]"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </div>
    <div class="tbm01-tab-content">
      <Tbm_01_1
        v-show="activeTab === 'material'"
        :title="props.title"
        :buttons="props.buttons"
      />
      <Tbm_01_2
        v-show="activeTab === 'aiAnalysis'"
        :title="props.title"
        :buttons="props.buttons"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineOptions } from "vue";
import Tbm_01_1 from "./Tbm_01_1.vue";
import Tbm_01_2 from "./Tbm_01_2.vue";

defineOptions({ name: "Tbm_01" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const tabs = [
  { key: "material", label: "교육자료 관리" },
  { key: "aiAnalysis", label: "AI 분석 관리" },
];
const activeTab = ref("material");
</script>

<style scoped>
/* 탭바 표준(Attd_01 .attd01-tab-bar/.attd01-tab-btn 스펙 준수 — 밑줄형 14px) */
.tbm01-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.tbm01-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.tbm01-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.tbm01-tab-btn:hover {
  color: var(--color-text, #374151);
}
.tbm01-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}
.tbm01-tab-content {
  flex: 1;
  min-height: 0;
}
</style>
