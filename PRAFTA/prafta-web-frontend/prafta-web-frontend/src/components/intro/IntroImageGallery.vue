<template>
  <div class="intro-gallery">
    <!-- 슬라이드 스테이지: 모든 슬라이드를 같은 그리드 셀에 겹쳐 두어
         이미지 높이가 달라도(폰/브라우저 목업 혼재) 컨테이너 높이가 튀지 않게 한다. -->
    <div
      class="intro-gallery__stage"
      role="group"
      aria-roledescription="이미지 슬라이드"
      :aria-label="stageLabel"
      @touchstart.passive="onStageTouchStart"
      @touchend.passive="onStageTouchEnd"
    >
      <div
        v-for="(img, i) in images"
        :key="img.src"
        class="intro-gallery__slide"
        :class="{ 'is-active': i === index }"
        :aria-hidden="i === index ? undefined : 'true'"
      >
        <button
          type="button"
          class="intro-gallery__zoom"
          :aria-label="`${img.alt} — 크게 보기`"
          @click="openLightbox(i)"
        >
          <IntroMockupFrame
            :variant="img.variant || 'browser'"
            :src="img.src"
            :alt="img.alt"
          />
          <span class="intro-gallery__hint" aria-hidden="true"
            >⤢ 크게 보기</span
          >
        </button>
      </div>
    </div>

    <!-- 수동 슬라이드 컨트롤: 이미지가 2장 이상일 때만 노출 -->
    <div v-if="isMulti" class="intro-gallery__controls">
      <button
        type="button"
        class="intro-gallery__nav"
        aria-label="이전 이미지"
        @click="prev"
      >
        ‹
      </button>
      <div class="intro-gallery__dots">
        <button
          v-for="(img, i) in images"
          :key="`dot-${img.src}`"
          type="button"
          class="intro-gallery__dot"
          :class="{ 'is-active': i === index }"
          :aria-label="`${i + 1}번째 이미지 보기`"
          :aria-current="i === index ? 'true' : undefined"
          @click="goTo(i)"
        />
      </div>
      <button
        type="button"
        class="intro-gallery__nav"
        aria-label="다음 이미지"
        @click="next"
      >
        ›
      </button>
      <span class="intro-gallery__counter">
        {{ index + 1 }} / {{ images.length }}
      </span>
    </div>

    <p v-if="isMulti && current.alt" class="intro-gallery__caption">
      {{ current.alt }}
    </p>
  </div>

  <!-- 확대 보기(라이트박스). 부모의 overflow/sticky 영향을 받지 않도록 body 로 텔레포트 -->
  <Teleport to="body">
    <div
      v-if="lightboxOpen"
      class="intro-lightbox"
      role="dialog"
      aria-modal="true"
      :aria-label="`${current.alt || '이미지'} 확대 보기`"
      @touchstart.passive="onLightboxTouchStart"
      @touchend.passive="onLightboxTouchEnd"
    >
      <button
        ref="closeBtnRef"
        type="button"
        class="intro-lightbox__close"
        aria-label="닫기"
        @click="closeLightbox"
      >
        ✕
      </button>

      <button
        v-if="isMulti"
        type="button"
        class="intro-lightbox__nav intro-lightbox__nav--prev"
        aria-label="이전 이미지"
        @click="prev"
      >
        ‹
      </button>

      <div
        class="intro-lightbox__viewport"
        :class="{ 'is-zoomed': zoomed }"
        @click.self="closeLightbox"
      >
        <img
          class="intro-lightbox__img"
          :class="{ 'is-zoomed': zoomed }"
          :src="current.src"
          :alt="current.alt"
          @click.stop="toggleZoom"
        />
      </div>

      <button
        v-if="isMulti"
        type="button"
        class="intro-lightbox__nav intro-lightbox__nav--next"
        aria-label="다음 이미지"
        @click="next"
      >
        ›
      </button>

      <div class="intro-lightbox__bar">
        <span class="intro-lightbox__caption">{{ current.alt }}</span>
        <span v-if="isMulti" class="intro-lightbox__counter">
          {{ index + 1 }} / {{ images.length }}
        </span>
        <span class="intro-lightbox__hint">
          {{ zoomed ? "클릭하면 화면에 맞춤" : "클릭하면 원본 크기" }}
        </span>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from "vue";
import IntroMockupFrame from "./IntroMockupFrame.vue";

const props = defineProps({
  // [{ src, alt, variant: 'browser' | 'phone' }]
  images: { type: Array, default: () => [] },
});

const index = ref(0);
const lightboxOpen = ref(false);
const zoomed = ref(false);
const closeBtnRef = ref(null);

// 라이트박스를 연 요소(포커스 복귀용)
let lastFocused = null;

const isMulti = computed(() => props.images.length > 1);
const current = computed(() => props.images[index.value] || {});
const stageLabel = computed(() =>
  isMulti.value
    ? `이미지 ${index.value + 1} / ${props.images.length}`
    : current.value.alt || "이미지"
);

function goTo(i) {
  if (!props.images.length) return;
  const len = props.images.length;
  index.value = ((i % len) + len) % len;
  // 이미지가 바뀌면 확대 상태는 초기화한다(이전 이미지 배율이 남지 않도록)
  zoomed.value = false;
}
function prev() {
  goTo(index.value - 1);
}
function next() {
  goTo(index.value + 1);
}

function openLightbox(i) {
  goTo(i);
  lastFocused = document.activeElement;
  lightboxOpen.value = true;
}
function closeLightbox() {
  lightboxOpen.value = false;
}
function toggleZoom() {
  zoomed.value = !zoomed.value;
}

function onKeydown(e) {
  if (!lightboxOpen.value) return;
  if (e.key === "Escape") {
    e.preventDefault();
    closeLightbox();
  } else if (e.key === "ArrowLeft" && isMulti.value) {
    prev();
  } else if (e.key === "ArrowRight" && isMulti.value) {
    next();
  }
}

// 스와이프(모바일) — 확대 중에는 이미지 스크롤이 우선이므로 넘기지 않는다
let touchStartX = 0;
function onStageTouchStart(e) {
  touchStartX = e.changedTouches[0].clientX;
}
function onStageTouchEnd(e) {
  if (!isMulti.value) return;
  const dx = e.changedTouches[0].clientX - touchStartX;
  if (Math.abs(dx) < 50) return;
  dx < 0 ? next() : prev();
}
function onLightboxTouchStart(e) {
  touchStartX = e.changedTouches[0].clientX;
}
function onLightboxTouchEnd(e) {
  if (!isMulti.value || zoomed.value) return;
  const dx = e.changedTouches[0].clientX - touchStartX;
  if (Math.abs(dx) < 50) return;
  dx < 0 ? next() : prev();
}

function unlockBody() {
  document.body.style.overflow = "";
}

watch(lightboxOpen, async (open) => {
  if (open) {
    document.addEventListener("keydown", onKeydown);
    document.body.style.overflow = "hidden";
    await nextTick();
    closeBtnRef.value?.focus();
  } else {
    document.removeEventListener("keydown", onKeydown);
    unlockBody();
    zoomed.value = false;
    lastFocused?.focus?.();
    lastFocused = null;
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", onKeydown);
  unlockBody();
});
</script>

<style scoped>
.intro-gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ── 슬라이드 스테이지 ───────────────────────────── */
.intro-gallery__stage {
  display: grid;
}
.intro-gallery__slide {
  grid-area: 1 / 1;
  /* 스테이지 높이는 가장 큰 슬라이드 기준으로 고정되므로(전환 시 레이아웃 점프 방지),
     그보다 짧은 슬라이드는 세로 중앙에 놓아 위아래 여백이 한쪽으로 쏠리지 않게 한다. */
  align-self: center;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.25s ease;
}
.intro-gallery__slide.is-active {
  opacity: 1;
  visibility: visible;
}
.intro-gallery__zoom {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: none;
  cursor: zoom-in;
  position: relative;
  border-radius: var(--card-radius);
}
.intro-gallery__zoom:focus-visible {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
.intro-gallery__hint {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 5px 10px;
  border-radius: var(--btn-radius);
  background: rgba(17, 24, 39, 0.72);
  color: var(--color-surface);
  font-size: 12px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}
.intro-gallery__zoom:hover .intro-gallery__hint,
.intro-gallery__zoom:focus-visible .intro-gallery__hint {
  opacity: 1;
}

/* ── 컨트롤 ─────────────────────────────────────── */
.intro-gallery__controls {
  display: flex;
  align-items: center;
  gap: 10px;
}
.intro-gallery__nav {
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border: var(--card-border);
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    color 0.15s ease;
}
.intro-gallery__nav:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.intro-gallery__nav:focus-visible {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
.intro-gallery__dots {
  display: flex;
  align-items: center;
  gap: 6px;
}
.intro-gallery__dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: var(--color-border-strong);
  cursor: pointer;
  transition:
    background 0.15s ease,
    transform 0.15s ease;
}
.intro-gallery__dot.is-active {
  background: var(--color-primary);
  transform: scale(1.25);
}
.intro-gallery__dot:focus-visible {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
.intro-gallery__counter {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}
.intro-gallery__caption {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-muted);
}

/* ── 라이트박스(확대 보기) ───────────────────────── */
.intro-lightbox {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.92);
}
.intro-lightbox__viewport {
  flex: 1 1 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 16px 72px;
  overflow: auto;
  cursor: zoom-out;
}
.intro-lightbox__viewport.is-zoomed {
  align-items: flex-start;
  justify-content: flex-start;
}
.intro-lightbox__img {
  display: block;
  max-width: min(1400px, 92vw);
  max-height: 82vh;
  width: auto;
  height: auto;
  border-radius: 8px;
  background: var(--color-surface);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.45);
  cursor: zoom-in;
}
.intro-lightbox__img.is-zoomed {
  max-width: none;
  max-height: none;
  margin: auto;
  cursor: zoom-out;
}
.intro-lightbox__close,
.intro-lightbox__nav {
  position: absolute;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.72);
  color: #ffffff;
  cursor: pointer;
  transition:
    background 0.15s ease,
    border-color 0.15s ease;
}
.intro-lightbox__close:hover,
.intro-lightbox__nav:hover {
  background: rgba(17, 24, 39, 0.92);
  border-color: rgba(255, 255, 255, 0.6);
}
.intro-lightbox__close:focus-visible,
.intro-lightbox__nav:focus-visible {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
.intro-lightbox__close {
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  font-size: 16px;
}
.intro-lightbox__nav {
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  font-size: 24px;
  line-height: 1;
}
.intro-lightbox__nav--prev {
  left: 16px;
}
.intro-lightbox__nav--next {
  right: 16px;
}
.intro-lightbox__bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: linear-gradient(to top, rgba(17, 24, 39, 0.92), transparent);
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
}
.intro-lightbox__caption {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.intro-lightbox__counter {
  flex: 0 0 auto;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.intro-lightbox__hint {
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.6);
}

@media (max-width: 767px) {
  .intro-lightbox__viewport {
    padding: 56px 8px 84px;
  }
  .intro-lightbox__nav {
    width: 38px;
    height: 38px;
    font-size: 20px;
  }
  .intro-lightbox__nav--prev {
    left: 8px;
  }
  .intro-lightbox__nav--next {
    right: 8px;
  }
  .intro-lightbox__hint {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-gallery__slide,
  .intro-gallery__hint,
  .intro-gallery__nav,
  .intro-gallery__dot {
    transition: none;
  }
}
</style>
