<template>
  <nav class="intro-anchor-tabs" aria-label="섹션 바로가기">
    <div class="intro-anchor-tabs__inner">
      <a
        v-for="tab in tabs"
        :key="tab.id"
        :href="`#${tab.id}`"
        class="intro-anchor-tabs__link"
        :class="{ 'is-active': activeId === tab.id }"
      >
        {{ tab.label }}
      </a>
    </div>
  </nav>
</template>

<script setup>
import { ref } from "vue";

defineProps({
  tabs: { type: Array, required: true },
});

// TODO(developer): IntersectionObserver로 현재 뷰포트 섹션에 맞춰 activeId 갱신
const activeId = ref("");
</script>

<style scoped>
.intro-anchor-tabs {
  position: sticky;
  top: 64px;
  z-index: 15;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: saturate(180%) blur(8px);
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}
.intro-anchor-tabs__inner {
  max-width: 1160px;
  margin: 0 auto;
  padding: 0 var(--header-padding-x);
  display: flex;
  gap: 24px;
}
.intro-anchor-tabs__link {
  flex: 0 0 auto;
  padding: 14px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}
.intro-anchor-tabs__link:hover {
  color: var(--color-text-strong);
}
.intro-anchor-tabs__link.is-active {
  color: var(--color-text-strong);
  border-bottom-color: var(--color-primary);
}
</style>
