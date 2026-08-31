<template>
  <section
    :id="id || undefined"
    class="intro-feature"
    :class="{ 'intro-feature--reverse': reverse }"
  >
    <div class="intro-feature__inner">
      <div class="intro-feature__copy">
        <p
          v-if="label"
          class="intro-feature__label"
          :class="`intro-feature__label--${axis}`"
        >
          {{ label }}
        </p>
        <h3 class="intro-feature__heading">{{ heading }}</h3>
        <IntroSentences
          v-if="description"
          class="intro-feature__desc"
          :text="description"
        />
        <ul v-if="bullets && bullets.length" class="intro-feature__bullets">
          <li v-for="(b, i) in bullets" :key="i">
            <IntroSentences :text="b" tag="span" />
          </li>
        </ul>
        <slot name="footnote" />
      </div>

      <div class="intro-feature__visual">
        <IntroImageGallery v-if="images && images.length" :images="images" />
        <div v-else class="intro-feature__placeholder">
          <slot name="placeholder">
            <span class="intro-feature__placeholder-icon" aria-hidden="true"
              >⬡</span
            >
            <span class="intro-feature__placeholder-text">{{ heading }}</span>
          </slot>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import IntroImageGallery from "./IntroImageGallery.vue";
import IntroSentences from "./IntroSentences.vue";

defineProps({
  id: { type: String, default: "" },
  axis: { type: String, default: "neutral" },
  label: { type: String, default: "" },
  heading: { type: String, required: true },
  description: { type: String, default: "" },
  bullets: { type: Array, default: () => [] },
  images: { type: Array, default: () => [] },
  reverse: { type: Boolean, default: false },
});
</script>

<style scoped>
.intro-feature {
  padding: 64px var(--header-padding-x);
  scroll-margin-top: 116px;
}
.intro-feature__inner {
  max-width: 1160px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: center;
}
.intro-feature--reverse .intro-feature__inner {
  direction: rtl;
}
.intro-feature--reverse .intro-feature__copy,
.intro-feature--reverse .intro-feature__visual {
  direction: ltr;
}
.intro-feature__label {
  display: inline-block;
  font-size: var(--intro-text-xs);
  font-weight: 700;
  letter-spacing: 0.02em;
  margin: 0 0 10px;
  color: var(--color-primary);
}
.intro-feature__label--attd {
  color: var(--color-attd);
}
.intro-feature__label--safety {
  color: var(--color-primary);
}
.intro-feature__heading {
  font-size: var(--intro-text-md);
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 12px;
  line-height: var(--intro-lh-tight);
}
.intro-feature__desc {
  color: var(--color-text-muted);
  font-size: var(--intro-text-sm);
  line-height: var(--intro-lh-base);
  margin: 0 0 18px;
}
.intro-feature__bullets {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.intro-feature__bullets li {
  position: relative;
  padding-left: 20px;
  color: var(--color-text);
  font-size: var(--intro-text-sm);
  line-height: var(--intro-lh-base);
}
.intro-feature__bullets li::before {
  content: "";
  position: absolute;
  left: 0;
  top: calc(0.85em - 3px);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}
.intro-feature__visual {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.intro-feature__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 220px;
  border-radius: var(--card-radius);
  border: 1px dashed var(--color-border-strong);
  background: var(--color-bg);
  padding: 24px;
  text-align: center;
}
.intro-feature__placeholder-icon {
  font-size: 32px;
  color: var(--color-primary);
}
.intro-feature__placeholder-text {
  font-size: var(--intro-text-xs);
  color: var(--color-text-muted);
}

@media (max-width: 1023px) {
  .intro-feature__inner {
    grid-template-columns: 1fr;
  }
  .intro-feature--reverse .intro-feature__inner {
    direction: ltr;
  }
}
@media (max-width: 767px) {
  .intro-feature {
    padding: 44px var(--header-padding-x);
  }
}
</style>
