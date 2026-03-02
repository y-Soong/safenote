<template>
  <select
    style="width: 100%"
    :value="modelValue || null"
    :disabled="computedDisabled"
    @change="onChange"
    :class="[{ 'pointer-events-none': readonly }]"
  >
    <slot />
  </select>
  <!-- 네이티브 form 제출 시 값 보존 -->
  <input
    v-if="readonly && name"
    type="hidden"
    :name="name"
    :value="modelValue"
  />
</template>

<script setup>
// ================ Imports ================
import { computed, defineProps, defineEmits } from "vue";

// ================ Props & Emits ================
const props = defineProps({
  modelValue: [String, Number, null],
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  name: { type: String, default: "" }, // 폼 제출 시 사용
});

const emit = defineEmits(["update:modelValue"]);

// ================ Computed Properties ================
const computedDisabled = computed(() => props.disabled || props.readonly);

// ================ Methods/Functions ================
const onChange = (e) => {
  if (props.readonly) return; // 읽기 전용이면 무시
  emit("update:modelValue", e.target.value);
};
</script>

<style scoped>
select {
  padding: 0.4rem 0.6rem;
  font-size: 0.875rem;
  font-family: "Pretendard", sans-serif;
}
</style>
