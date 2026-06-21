<template>
  <div class="viewComm a06-container">
    <div class="a06-tab-bar">
      <button
        type="button"
        :class="['a06-tab-btn', { active: activeTab === 'setup' }]"
        @click="activeTab = 'setup'"
      >
        교대근무 팀 생성
      </button>
      <button
        type="button"
        :class="['a06-tab-btn', { active: activeTab === 'view' }]"
        @click="onSwitchToView"
      >
        교대근무 팀 관리
      </button>
    </div>
    <div class="a06-tab-content">
      <Attd_06_1
        v-show="activeTab === 'setup'"
        :title="props.title"
        :buttons="props.buttons"
        @save-complete="onSaveComplete"
      />
      <Attd_06_2
        ref="view2Ref"
        v-show="activeTab === 'view'"
        :title="props.title"
        :buttons="props.buttons"
        @go-setup="activeTab = 'setup'"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineOptions } from "vue";
import Attd_06_1 from "./Attd_06_1.vue";
import Attd_06_2 from "./Attd_06_2.vue";

defineOptions({ name: "Attd_06" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const activeTab = ref("setup");
const view2Ref = ref(null);

const onSwitchToView = () => {
  activeTab.value = "view";
  view2Ref.value?.refresh();
};

const onSaveComplete = () => {
  activeTab.value = "view";
  view2Ref.value?.refresh();
};
</script>

<style scoped>
.a06-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.a06-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.a06-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.a06-tab-btn:hover {
  color: var(--color-text, #374151);
}
.a06-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}
.a06-tab-content {
  flex: 1;
  min-height: 0;
}
</style>
