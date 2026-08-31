<template>
  <header class="intro-header">
    <div class="intro-header__inner">
      <router-link to="/" class="intro-header__brand" aria-label="PRAFTA 홈">
        <span class="intro-header__brand-text"
          >PRA<span class="intro-header__brand-accent">F</span>TA</span
        >
      </router-link>

      <nav
        class="intro-header__nav"
        :class="{ 'is-open': mobileMenuOpen }"
        aria-label="주요 메뉴"
      >
        <router-link
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="intro-header__link"
          :class="{ 'is-active': isActive(link.path) }"
          @click="closeMobileMenu"
        >
          {{ link.label }}
        </router-link>
        <IntroButton
          to="/contact"
          variant="primary"
          size="sm"
          class="intro-header__cta intro-header__cta--mobile"
          @click="closeMobileMenu"
          >도입문의</IntroButton
        >
      </nav>

      <IntroButton
        to="/contact"
        variant="primary"
        size="sm"
        class="intro-header__cta intro-header__cta--desktop"
        >도입문의</IntroButton
      >

      <button
        type="button"
        class="intro-header__hamburger"
        :aria-expanded="mobileMenuOpen"
        aria-label="메뉴 열기"
        @click="toggleMobileMenu"
      >
        <span class="intro-header__hamburger-bar"></span>
        <span class="intro-header__hamburger-bar"></span>
        <span class="intro-header__hamburger-bar"></span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import IntroButton from "./IntroButton.vue";

const route = useRoute();
const mobileMenuOpen = ref(false);

const navLinks = [
  { path: "/attendance", label: "근태관리" },
  { path: "/safety", label: "안전관리" },
  { path: "/pricing", label: "이용요금" },
  { path: "/", label: "회사소개" },
];

const isActive = (path) => route.path === path;
const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value;
};
const closeMobileMenu = () => {
  mobileMenuOpen.value = false;
};
</script>

<style scoped>
.intro-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: saturate(180%) blur(8px);
  border-bottom: 1px solid var(--color-border);
}
.intro-header__inner {
  max-width: 1160px;
  margin: 0 auto;
  height: 64px;
  padding: 0 var(--header-padding-x);
  display: flex;
  align-items: center;
  gap: 28px;
}
.intro-header__brand {
  text-decoration: none;
}
.intro-header__brand-text {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0.01em;
  color: var(--color-text-strong);
}
.intro-header__brand-accent {
  color: var(--color-brand-yellow);
}
.intro-header__nav {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 24px;
}
.intro-header__link {
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  padding-bottom: 2px;
  border-bottom: 2px solid transparent;
}
.intro-header__link:hover {
  color: var(--color-text-strong);
}
.intro-header__link.is-active {
  color: var(--color-text-strong);
  border-bottom-color: var(--color-primary);
}
.intro-header__cta--mobile {
  display: none;
}
.intro-header__hamburger {
  display: none;
  flex-direction: column;
  gap: 4px;
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
}
.intro-header__hamburger-bar {
  width: 20px;
  height: 2px;
  background: var(--color-text-strong);
}

@media (max-width: 767px) {
  .intro-header__nav {
    position: fixed;
    top: 64px;
    left: 0;
    right: 0;
    background: var(--color-surface);
    border-bottom: 1px solid var(--color-border);
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    padding: 12px var(--header-padding-x) 20px;
    display: none;
  }
  .intro-header__nav.is-open {
    display: flex;
  }
  .intro-header__link {
    width: 100%;
    padding: 10px 0;
  }
  .intro-header__cta--desktop {
    display: none;
  }
  .intro-header__cta--mobile {
    display: inline-flex;
    margin-top: 8px;
  }
  .intro-header__hamburger {
    display: flex;
    margin-left: auto;
  }
}
</style>
