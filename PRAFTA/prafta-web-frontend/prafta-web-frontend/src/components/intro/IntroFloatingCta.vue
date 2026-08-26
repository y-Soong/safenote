<template>
  <router-link
    v-if="route.path !== '/contact'"
    to="/contact"
    class="intro-floating-cta"
    aria-label="도입문의"
  >
    <span class="intro-floating-cta__icon" aria-hidden="true">💬</span>
    <span class="intro-floating-cta__label">도입문의</span>
  </router-link>
</template>

<script setup>
import { useRoute } from "vue-router";

const route = useRoute();
</script>

<style scoped>
.intro-floating-cta {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 30;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 18px;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  text-decoration: none;
  font-weight: 700;
  font-size: 14px;
  box-shadow:
    0 4px 12px rgba(16, 24, 40, 0.18),
    0 2px 4px rgba(16, 24, 40, 0.12);
  transition:
    transform 0.15s ease,
    background 0.15s ease;
}
.intro-floating-cta:hover {
  background: var(--color-primary-hover);
  transform: translateY(-2px) scale(1.03);
}
.intro-floating-cta__icon {
  font-size: 16px;
  line-height: 1;
}

@media (max-width: 767px) {
  .intro-floating-cta {
    right: 14px;
    bottom: 14px;
    height: 44px;
    padding: 0 14px;
  }
  .intro-floating-cta__label {
    font-size: 13px;
  }
}
</style>
