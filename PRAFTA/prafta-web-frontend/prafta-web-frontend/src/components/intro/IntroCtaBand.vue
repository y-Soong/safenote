<template>
  <section class="intro-cta-band">
    <div class="intro-cta-band__inner">
      <h2 class="intro-cta-band__heading">{{ heading }}</h2>
      <p v-if="subtext" class="intro-cta-band__subtext">{{ subtext }}</p>
      <IntroButton :to="buttonTo" variant="primary" size="lg">{{
        buttonLabel
      }}</IntroButton>
    </div>
  </section>
</template>

<script setup>
import IntroButton from "./IntroButton.vue";

defineProps({
  heading: { type: String, required: true },
  subtext: { type: String, default: "" },
  buttonLabel: { type: String, default: "도입문의" },
  buttonTo: { type: String, default: "/contact" },
});
</script>

<style scoped>
.intro-cta-band {
  background: var(--color-bg);
  padding: 64px var(--header-padding-x);
  text-align: center;
}
.intro-cta-band__inner {
  max-width: 640px;
  margin: 0 auto;
}
.intro-cta-band__heading {
  font-size: var(--intro-text-md);
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 10px;
}
.intro-cta-band__subtext {
  color: var(--color-text-muted);
  margin: 0 0 24px;
}
</style>
