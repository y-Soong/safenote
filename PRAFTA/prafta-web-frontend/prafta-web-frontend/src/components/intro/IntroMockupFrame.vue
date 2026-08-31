<template>
  <div class="intro-mockup" :class="`intro-mockup--${variant}`">
    <div v-if="variant === 'browser'" class="intro-mockup__chrome">
      <span class="intro-mockup__dot"></span>
      <span class="intro-mockup__dot"></span>
      <span class="intro-mockup__dot"></span>
    </div>
    <div class="intro-mockup__screen">
      <img :src="src" :alt="alt" loading="lazy" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  variant: { type: String, default: "browser" },
  src: { type: String, required: true },
  alt: { type: String, required: true },
});
</script>

<style scoped>
.intro-mockup {
  border-radius: var(--card-radius);
  overflow: hidden;
  border: var(--card-border);
  box-shadow: var(--card-shadow);
  background: var(--color-surface);
}
.intro-mockup--browser .intro-mockup__chrome {
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.intro-mockup__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-border-strong);
}
.intro-mockup__screen img {
  display: block;
  width: 100%;
  height: auto;
}
.intro-mockup--phone {
  max-width: 280px;
  margin: 0 auto;
  border-radius: 28px;
  padding: 10px;
  background: var(--color-text-strong);
}
.intro-mockup--phone .intro-mockup__screen {
  border-radius: 20px;
  overflow: hidden;
}
</style>
