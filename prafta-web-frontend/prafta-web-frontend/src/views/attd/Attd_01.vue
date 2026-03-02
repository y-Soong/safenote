<template>
  <div class="viewComm attd01-container">
    <div class="attd01-tab-bar">
      <button
        type="button"
        :class="['attd01-tab-btn', { active: activeTab === 'schedule' }]"
        @click="activeTab = 'schedule'"
      >
        스케줄관리
      </button>
      <button
        type="button"
        :class="['attd01-tab-btn', { active: activeTab === 'shift' }]"
        @click="activeTab = 'shift'"
      >
        교대근무타입관리
      </button>
    </div>
    <div class="attd01-tab-content">
      <Attd_01_1
        v-show="activeTab === 'schedule'"
        :title="props.title"
        :buttons="props.buttons"
      />
      <Attd_01_2
        v-show="activeTab === 'shift'"
        :title="props.title"
        :buttons="props.buttons"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineOptions } from "vue";
import Attd_01_1 from "./Attd_01_1.vue";
import Attd_01_2 from "./Attd_01_2.vue";

defineOptions({ name: "Attd_01" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const activeTab = ref("schedule");
</script>

<style scoped>
.attd01-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.attd01-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.attd01-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.attd01-tab-btn:hover {
  color: var(--color-text, #374151);
}
.attd01-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}
.attd01-tab-content {
  flex: 1;
  min-height: 0;
}
</style>
