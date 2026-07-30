<!--
  LeavePromotion_01.vue — 연차 사용촉진 관리 (탭 셸)
  - 출처: 작업지시서_연차촉진-1차현황-화면-및-배치활성화.md §6(T3), 확정 D7(신규 메뉴 없이 탭 추가)
  - 참조 패턴: views/attd/Attd_01.vue (웹 표준 밑줄형 14px 탭바 — 메모리 reference_web_tab_bar_canonical_style)
  - 구조: 1차 현황(LeavePromotion_01_1, 기본 선택) / 2차 직권지정(LeavePromotion_01_2, 기존 화면 그대로 이동)
  - 이력: 기존 본 파일의 2차 화면 내용은 LeavePromotion_01_2.vue 로 무회귀 이동(복사)됨.
-->
<template>
  <div class="viewComm lp01-container">
    <div class="lp01-tab-bar">
      <button
        type="button"
        :class="['lp01-tab-btn', { active: activeTab === 'first' }]"
        @click="activeTab = 'first'"
      >
        1차 현황
      </button>
      <button
        type="button"
        :class="['lp01-tab-btn', { active: activeTab === 'second' }]"
        @click="activeTab = 'second'"
      >
        2차 직권지정
      </button>
    </div>
    <div class="lp01-tab-content">
      <LeavePromotion_01_1
        v-show="activeTab === 'first'"
        :title="props.title"
        :buttons="props.buttons"
      />
      <LeavePromotion_01_2
        v-show="activeTab === 'second'"
        :title="props.title"
        :buttons="props.buttons"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineOptions } from "vue";
import LeavePromotion_01_1 from "./LeavePromotion_01_1.vue";
import LeavePromotion_01_2 from "./LeavePromotion_01_2.vue";

defineOptions({ name: "LeavePromotion_01" });

const props = defineProps({
  title: String,
  buttons: Object,
});

// 탭 전환 상태(UI 토글). 기본 = 1차 현황(지시서 §6).
const activeTab = ref("first");
</script>

<style scoped>
/* Attd_01 탭바 패턴 그대로 차용(클래스 접두만 lp01-) */
.lp01-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.lp01-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}
.lp01-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted);
  cursor: pointer;
}
.lp01-tab-btn:hover {
  color: var(--color-text);
}
.lp01-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}
.lp01-tab-content {
  flex: 1;
  min-height: 0;
}
</style>
