<template>
  <section id="brand-story" class="brand-story" ref="rootEl">
    <div class="brand-story__stage" ref="stageEl">
      <div class="brand-story__eyebrow" ref="eyebrowEl">
        PRAFTA&nbsp;&nbsp;SAFETY&nbsp;&nbsp;CYCLE
      </div>

      <div class="brand-story__phrase" ref="phraseEl">
        <span
          v-for="(unit, i) in cycleUnits"
          :key="unit.piece + i"
          class="brand-story__unit"
          :data-piece="unit.piece"
        >
          <small>{{ unit.ko }}</small>
          <span class="brand-story__en"
            ><span class="brand-story__cap">{{ unit.piece }}</span
            ><span class="brand-story__rest">{{ unit.rest }}</span></span
          >
        </span>
      </div>
      <!-- 화살표(→)는 원본과 동일하게 unit 사이에 렌더 — developer가 v-for 인덱스로 조인 처리 -->
      <!-- TODO(developer): 원본처럼 .unit 사이 .joint(→) 삽입 및 reset()/play() 애니메이션 로직 이식 -->

      <div class="brand-story__logo">
        <div class="brand-story__wordmark" ref="wordmarkEl">
          <span
            v-for="(l, i) in wordmarkLetters"
            :key="l + i"
            class="brand-story__slot"
            :data-safety="i === 3 ? '' : undefined"
            >{{ l }}</span
          >
        </div>
        <div class="brand-story__tagline" ref="taglineEl">
          실행은 다시 예방으로 —
          <b>안전관리의 모든 사이클</b>을 하나의 시스템에 담았습니다
        </div>
        <div class="brand-story__sub" ref="subEl">
          Prevent · Record · Assess · Follow-up · Track · Act
        </div>
      </div>

      <button
        type="button"
        class="brand-story__replay"
        ref="replayEl"
        @click="play"
      >
        ↺ REPLAY
      </button>
    </div>

    <p class="brand-story__bridge">
      사명은 안전관리의 사이클에서 왔습니다. 지금 PRAFTA는 그 사이클의
      뿌리인 근태까지, 현장 운영 전체를 하나의 시스템에 담습니다.
    </p>
  </section>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";

const cycleUnits = [
  { piece: "P", ko: "예방하고", rest: "revent" },
  { piece: "R", ko: "기록하고", rest: "ecord" },
  { piece: "A", ko: "평가하고", rest: "ssess" },
  { piece: "F", ko: "후속조치하고", rest: "ollow-up" },
  { piece: "T", ko: "추적하고", rest: "rack" },
  { piece: "A", ko: "실행한다", rest: "ct" },
];
const wordmarkLetters = ["P", "R", "A", "F", "T", "A"];

const rootEl = ref(null);
const stageEl = ref(null);
const eyebrowEl = ref(null);
const phraseEl = ref(null);
const wordmarkEl = ref(null);
const taglineEl = ref(null);
const subEl = ref(null);
const replayEl = ref(null);

// 원본 IIFE의 timers/wait/clearAll 이식 — setTimeout 핸들 배열로 관리
let timers = [];
const wait = (fn, ms) => timers.push(setTimeout(fn, ms));
const clearAllTimers = () => {
  timers.forEach((id) => clearTimeout(id));
  timers = [];
};

// 원본 reset() 이식 — show/on 클래스 전부 제거, 타이머 정리
function reset() {
  clearAllTimers();
  eyebrowEl.value && eyebrowEl.value.classList.remove("show");
  taglineEl.value && taglineEl.value.classList.remove("show");
  subEl.value && subEl.value.classList.remove("show");
  replayEl.value && replayEl.value.classList.remove("show");
  if (phraseEl.value) {
    Array.from(phraseEl.value.children).forEach((el) =>
      el.classList.remove("show")
    );
  }
  if (wordmarkEl.value) {
    Array.from(wordmarkEl.value.children).forEach((el) =>
      el.classList.remove("on")
    );
  }
}

// 원본 finalFrame() 이식 — reduced-motion 시 애니메이션 없이 최종 프레임 즉시 표시
function finalFrame() {
  eyebrowEl.value && eyebrowEl.value.classList.add("show");
  if (phraseEl.value) {
    Array.from(phraseEl.value.children).forEach((el) =>
      el.classList.add("show")
    );
  }
  if (wordmarkEl.value) {
    Array.from(wordmarkEl.value.children).forEach((el) =>
      el.classList.add("on")
    );
  }
  taglineEl.value && taglineEl.value.classList.add("show");
  subEl.value && subEl.value.classList.add("show");
  replayEl.value && replayEl.value.classList.add("show");
}

// 원본 play() 이식 — eyebrow → phrase 6유닛 순차 표시 → wordmark 슬롯 점등 → tagline/sub → replay 순
const play = () => {
  reset();

  const reduced = window.matchMedia(
    "(prefers-reduced-motion: reduce)"
  ).matches;
  if (reduced) {
    finalFrame();
    return;
  }

  let t = 400;
  wait(() => eyebrowEl.value && eyebrowEl.value.classList.add("show"), t);

  // 1) 사이클 문구 6유닛 순차 표시
  t += 500;
  const units = phraseEl.value ? Array.from(phraseEl.value.children) : [];
  units.forEach((unit) => {
    wait(() => unit.classList.add("show"), t);
    t += 430;
  });

  // 2) 잠시 유지 후 워드마크 전환 준비
  t += 1400;

  // 3) 워드마크 슬롯 순차 점등
  const slots = wordmarkEl.value ? Array.from(wordmarkEl.value.children) : [];
  slots.forEach((slot, i) => {
    wait(() => slot.classList.add("on"), t + i * 110);
  });
  t += slots.length * 110 + 300;

  // 4) tagline + sub 동시 노출
  wait(() => {
    taglineEl.value && taglineEl.value.classList.add("show");
    subEl.value && subEl.value.classList.add("show");
  }, t);
  t += 700;

  // 5) REPLAY 버튼 노출
  wait(() => replayEl.value && replayEl.value.classList.add("show"), t);
};

// 원본 window load 트리거 대신 IntersectionObserver(threshold 0.4)로 뷰포트 진입 1회만 재생
let observer;
onMounted(() => {
  if (typeof IntersectionObserver === "undefined") {
    play();
    return;
  }
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          play();
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.4 }
  );
  if (rootEl.value) observer.observe(rootEl.value);
});
onBeforeUnmount(() => {
  clearAllTimers();
  if (observer) observer.disconnect();
});
</script>

<style scoped>
.brand-story {
  padding: 56px var(--header-padding-x);
  background: var(--color-surface);
}
.brand-story__stage {
  --bs-bg: #161a1f;
  --bs-ink: #f2f4f6;
  --bs-muted: #7e8792;
  --bs-safety: #f5b301;
  --bs-line: #2a3038;
  position: relative;
  width: min(96vw, 1120px);
  aspect-ratio: 16 / 9;
  margin: 0 auto;
  background:
    radial-gradient(120% 90% at 50% 108%, #1d232b 0%, var(--bs-bg) 55%),
    var(--bs-bg);
  border: 1px solid var(--bs-line);
  border-radius: 12px;
  overflow: hidden;
  container-type: inline-size;
}
.brand-story__stage::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--bs-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--bs-line) 1px, transparent 1px);
  background-size: 5cqi 5cqi;
  opacity: 0.14;
  pointer-events: none;
}
.brand-story__eyebrow {
  position: absolute;
  top: 8cqi;
  left: 50%;
  transform: translateX(-50%);
  font-size: 1.5cqi;
  letter-spacing: 0.55em;
  text-indent: 0.55em;
  color: var(--bs-muted);
  text-transform: uppercase;
  opacity: 0;
  transition: opacity 0.8s ease;
  white-space: nowrap;
}
.brand-story__eyebrow.show {
  opacity: 1;
}
.brand-story__phrase {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5cqi;
}
.brand-story__unit {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.1cqi;
  opacity: 0;
  transform: translateY(1.6cqi);
  transition:
    opacity 0.55s ease,
    transform 0.55s ease;
  color: var(--bs-ink);
}
.brand-story__unit.show {
  opacity: 1;
  transform: translateY(0);
}
.brand-story__unit small {
  font-size: 1.55cqi;
  color: var(--bs-muted);
  letter-spacing: 0.1em;
  font-weight: 600;
  white-space: nowrap;
}
.brand-story__en {
  font-size: 2.9cqi;
  font-weight: 500;
  letter-spacing: 0.02em;
  white-space: nowrap;
  line-height: 1;
}
.brand-story__cap {
  font-weight: 800;
  color: var(--bs-safety);
  display: inline-block;
  line-height: 1;
}
.brand-story__logo {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3.4cqi;
  pointer-events: none;
}
.brand-story__wordmark {
  display: flex;
  gap: 0.7cqi;
}
.brand-story__slot {
  font-size: 13cqi;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.01em;
  color: var(--bs-ink);
  opacity: 0;
  transform: scale(0.96);
  transition:
    opacity 0.35s ease,
    transform 0.35s ease,
    color 0.6s ease;
}
.brand-story__slot.on {
  opacity: 1;
  transform: scale(1);
}
.brand-story__slot[data-safety] {
  color: var(--bs-safety);
}
.brand-story__tagline {
  font-size: 2.3cqi;
  color: var(--bs-ink);
  letter-spacing: 0.04em;
  font-weight: 500;
  opacity: 0;
  transform: translateY(1cqi);
  transition:
    opacity 0.8s ease 0.1s,
    transform 0.8s ease 0.1s;
}
.brand-story__tagline b {
  color: var(--bs-safety);
  font-weight: 700;
}
.brand-story__tagline.show {
  opacity: 1;
  transform: translateY(0);
}
.brand-story__sub {
  font-size: 1.45cqi;
  color: var(--bs-muted);
  letter-spacing: 0.24em;
  text-indent: 0.24em;
  text-transform: uppercase;
  opacity: 0;
  transition: opacity 0.8s ease 0.5s;
  white-space: nowrap;
}
.brand-story__sub.show {
  opacity: 0.9;
}
.brand-story__replay {
  position: absolute;
  right: 2.4cqi;
  bottom: 2.2cqi;
  z-index: 9;
  background: transparent;
  border: 1px solid var(--bs-line);
  color: var(--bs-muted);
  font-size: 1.6cqi;
  letter-spacing: 0.12em;
  padding: 0.9cqi 2cqi;
  border-radius: 999px;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition:
    opacity 0.5s ease,
    color 0.2s,
    border-color 0.2s;
}
.brand-story__replay.show {
  opacity: 1;
  pointer-events: auto;
}
.brand-story__replay:hover {
  color: var(--bs-safety);
  border-color: var(--bs-safety);
}
.brand-story__bridge {
  max-width: 720px;
  margin: 24px auto 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

@media (prefers-reduced-motion: reduce) {
  .brand-story__unit,
  .brand-story__slot,
  .brand-story__tagline,
  .brand-story__sub,
  .brand-story__eyebrow {
    transition: none;
  }
}
</style>
