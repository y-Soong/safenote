<template>
  <component :is="to ? 'router-link' : 'div'" :to="to || undefined" class="intro-icon-card">
    <div class="intro-icon-card__icon" aria-hidden="true">
      <slot name="icon">⬡</slot>
    </div>
    <h3 class="intro-icon-card__title">{{ title }}</h3>
    <p class="intro-icon-card__desc">{{ desc }}</p>
  </component>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, required: true },
  to: { type: String, default: "" },
});
</script>

<style scoped>
.intro-icon-card {
  display: block;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: var(--card-padding);
  text-decoration: none;
  color: inherit;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
a.intro-icon-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.1);
}
.intro-icon-card__icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--color-bg);
  color: var(--color-primary);
  margin-bottom: 14px;
  font-size: 20px;
}
.intro-icon-card__title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-strong);
  margin: 0 0 6px;
}
.intro-icon-card__desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-muted);
  margin: 0;
}
</style>
