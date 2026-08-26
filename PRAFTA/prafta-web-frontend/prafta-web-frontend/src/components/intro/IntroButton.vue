<template>
  <router-link
    v-if="to"
    :to="to"
    class="intro-button"
    :class="[`intro-button--${variant}`, `intro-button--${size}`]"
  >
    <slot />
  </router-link>
  <button
    v-else
    :type="type"
    class="intro-button"
    :class="[`intro-button--${variant}`, `intro-button--${size}`]"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: { type: String, default: "primary" },
  size: { type: String, default: "lg" },
  to: { type: String, default: "" },
  type: { type: String, default: "button" },
  disabled: { type: Boolean, default: false },
});
</script>

<style scoped>
.intro-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--btn-radius-lg);
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  border: 1px solid transparent;
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
  white-space: nowrap;
}
.intro-button--lg {
  height: 48px;
  padding: 0 24px;
  font-size: 15px;
}
.intro-button--sm {
  height: 38px;
  padding: 0 16px;
  font-size: 13px;
}
.intro-button--primary {
  background: var(--color-primary);
  color: #fff;
}
.intro-button--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(16, 24, 40, 0.12);
}
.intro-button--ghost {
  background: transparent;
  color: var(--color-text-strong);
  border-color: var(--color-border-strong);
}
.intro-button--ghost:hover:not(:disabled) {
  border-color: var(--color-text-muted);
  transform: translateY(-2px);
}
.intro-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>
