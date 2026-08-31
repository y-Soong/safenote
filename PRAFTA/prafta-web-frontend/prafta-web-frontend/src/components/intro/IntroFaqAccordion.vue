<template>
  <div class="intro-faq">
    <div
      v-for="(item, i) in items"
      :key="i"
      class="intro-faq__item"
      :class="{ 'is-open': openIndex === i }"
    >
      <button
        type="button"
        class="intro-faq__question"
        :aria-expanded="openIndex === i"
        @click="toggle(i)"
      >
        <span>{{ item.q }}</span>
        <span class="intro-faq__caret" aria-hidden="true">＋</span>
      </button>
      <div v-show="openIndex === i" class="intro-faq__answer">
        <p>{{ item.a }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";

defineProps({
  items: { type: Array, required: true },
});

const openIndex = ref(-1);
const toggle = (i) => {
  openIndex.value = openIndex.value === i ? -1 : i;
};
</script>

<style scoped>
.intro-faq__item {
  border-bottom: 1px solid var(--color-border);
}
.intro-faq__question {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 4px;
  background: none;
  border: none;
  text-align: left;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-strong);
  cursor: pointer;
}
.intro-faq__caret {
  color: var(--color-text-muted);
  transition: transform 0.15s ease;
}
.intro-faq__item.is-open .intro-faq__caret {
  transform: rotate(45deg);
  color: var(--color-primary);
}
.intro-faq__answer {
  padding: 0 4px 20px;
}
.intro-faq__answer p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
  font-size: 14px;
}
</style>
