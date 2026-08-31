<template>
  <section id="brand-story" class="brand-story" ref="rootEl">
    <div class="brand-story__stage" ref="stageEl">
      <div class="brand-story__eyebrow" ref="eyebrowEl">
        PRAFTA&nbsp;&nbsp;SAFETY&nbsp;&nbsp;CYCLE
      </div>

      <!-- 원본과 동일하게 unit 사이에 joint(→)를 끼워 넣는다.
           play()가 phraseEl.children 순서대로 순차 노출하므로 DOM 순서가 곧 연출 순서다. -->
      <div class="brand-story__phrase" ref="phraseEl">
        <template v-for="(unit, i) in cycleUnits" :key="unit.piece + i">
          <span class="brand-story__unit" :data-piece="unit.piece">
            <small>{{ unit.ko }}</small>
            <span class="brand-story__en"
              ><span class="brand-story__cap">{{ unit.piece }}</span
              ><span class="brand-story__rest">{{ unit.rest }}</span></span
            >
          </span>
          <span v-if="i < cycleUnits.length - 1" class="brand-story__joint"
            >→</span
          >
        </template>
      </div>

      <div class="brand-story__logo">
        <div class="brand-story__wordmark" ref="wordmarkEl">
          <span
            v-for="(l, i) in wordmarkLetters"
            :key="l + i"
            class="brand-story__slot"
            :data-safety="i === 3 ? '' : undefined"
            >{{ l }}</span
          >
          <!-- data-safety 는 표시자일 뿐이고, 실제 옐로 강조는 글자가 모두 모인 뒤
               is-safety 클래스로 입힌다(원본 slot.safety 와 동일 타이밍). -->
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
      사명은 안전관리의 사이클에서 왔습니다. 지금 PRAFTA는 그 사이클의 뿌리인
      근태까지, 현장 운영 전체를 하나의 시스템에 담습니다.
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

// 날아가는 글자(flyer)는 JS로 만들어 붙이므로 scoped 스타일의 data-v-* 속성이 없어
// 컴포넌트 CSS가 적용되지 않는다. 스테이지에서 스코프 속성명을 읽어 그대로 복사한다.
function scopeAttrName() {
  const el = stageEl.value;
  if (!el) return null;
  return (
    Array.from(el.attributes)
      .map((a) => a.name)
      .find((n) => n.startsWith("data-v-")) || null
  );
}

// phrase 안의 연출 대상 — children 순서(unit → joint → unit …)가 곧 등장 순서다
const piecesOf = () =>
  phraseEl.value ? Array.from(phraseEl.value.children) : [];
const unitsOf = () =>
  phraseEl.value
    ? Array.from(phraseEl.value.querySelectorAll(".brand-story__unit"))
    : [];
const slotsOf = () =>
  wordmarkEl.value
    ? Array.from(wordmarkEl.value.querySelectorAll(".brand-story__slot"))
    : [];

function removeFlyers() {
  if (!stageEl.value) return;
  stageEl.value
    .querySelectorAll(".brand-story__flyer")
    .forEach((f) => f.remove());
}

// 원본 reset() 이식 — show/on 클래스·flyer·인라인 스타일을 전부 되돌리고 타이머 정리
function reset() {
  clearAllTimers();
  removeFlyers();
  stageEl.value && stageEl.value.classList.remove("is-isolate");
  eyebrowEl.value && eyebrowEl.value.classList.remove("show");
  taglineEl.value && taglineEl.value.classList.remove("show");
  subEl.value && subEl.value.classList.remove("show");
  replayEl.value && replayEl.value.classList.remove("show");
  if (phraseEl.value) {
    phraseEl.value.style.display = "";
    piecesOf().forEach((el) => el.classList.remove("show"));
    phraseEl.value
      .querySelectorAll(".brand-story__cap")
      .forEach((c) => (c.style.visibility = ""));
  }
  slotsOf().forEach((s) => {
    s.classList.remove("on", "is-safety");
    s.style.visibility = "";
  });
}

// 원본 finalFrame() 이식 — reduced-motion 시 애니메이션 없이 최종 프레임 즉시 표시.
// 최종 프레임에서는 사이클 문구를 반드시 감춘다(워드마크와 같은 자리라 남기면 겹쳐 보인다).
function finalFrame() {
  if (phraseEl.value) phraseEl.value.style.display = "none";
  eyebrowEl.value && eyebrowEl.value.classList.add("show");
  const slots = slotsOf();
  slots.forEach((s) => s.classList.add("on"));
  slots[3] && slots[3].classList.add("is-safety");
  taglineEl.value && taglineEl.value.classList.add("show");
  subEl.value && subEl.value.classList.add("show");
  replayEl.value && replayEl.value.classList.add("show");
}

// 원본 3단계 — 남은 머리글자(P·R·A·F·T·A)가 워드마크 자리로 날아가 모인다.
// 슬롯은 좌표만 잡아둔 채 감추고, 날아간 flyer 가 그 자리에 안착한 뒤 실제 슬롯으로 교체한다.
function flyToWordmark() {
  const stage = stageEl.value;
  const phrase = phraseEl.value;
  if (!stage || !phrase) return;

  const stageRect = stage.getBoundingClientRect();
  const units = unitsOf();
  const slots = slotsOf();
  const scopeAttr = scopeAttrName();

  slots.forEach((s) => {
    s.style.visibility = "hidden";
    s.classList.add("on");
  });

  units.forEach((unit, i) => {
    const cap = unit.querySelector(".brand-story__cap");
    const slot = slots[i];
    if (!cap || !slot) return;

    const from = cap.getBoundingClientRect();
    const to = slot.getBoundingClientRect();

    const flyer = document.createElement("span");
    flyer.className = "brand-story__flyer";
    if (scopeAttr) flyer.setAttribute(scopeAttr, "");
    flyer.textContent = unit.dataset.piece;
    flyer.style.left = `${from.left - stageRect.left}px`;
    flyer.style.top = `${from.top - stageRect.top}px`;
    flyer.style.fontSize = `${from.height}px`;
    stage.appendChild(flyer);
    cap.style.visibility = "hidden";

    const dx = to.left - from.left;
    const dy = to.top - from.top;
    const scale = to.height / from.height;

    wait(
      () => {
        flyer.style.transform = `translate(${dx}px, ${dy}px)`;
        flyer.style.fontSize = `${from.height * scale}px`;
        flyer.style.fontWeight = "900";
        if (!slot.hasAttribute("data-safety")) {
          flyer.style.color = "var(--bs-ink)";
        }
      },
      40 + i * 110
    );
  });

  const n = units.length;

  // 4) 도착한 flyer 를 실제 워드마크로 교체하고, 사이클 문구는 완전히 감춘다
  wait(
    () => {
      phrase.style.display = "none";
      slots.forEach((s) => (s.style.visibility = ""));
      removeFlyers();
      slots[3] && slots[3].classList.add("is-safety");
    },
    1400 + n * 110
  );

  // 5) tagline + sub
  wait(
    () => {
      taglineEl.value && taglineEl.value.classList.add("show");
      subEl.value && subEl.value.classList.add("show");
    },
    2050 + n * 110
  );

  // 6) REPLAY
  wait(
    () => replayEl.value && replayEl.value.classList.add("show"),
    2750 + n * 110
  );
}

// 원본 play() 이식 — eyebrow → 사이클 문구 순차 등장 → 머리글자만 남김(isolate)
//   → 워드마크로 수렴(flyer) → tagline/sub → replay
const play = () => {
  reset();

  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduced) {
    finalFrame();
    return;
  }

  let t = 400;
  wait(() => eyebrowEl.value && eyebrowEl.value.classList.add("show"), t);

  // 1) 사이클 문구를 unit → joint 순서로 하나씩 세운다
  t += 500;
  piecesOf().forEach((el) => {
    wait(() => el.classList.add("show"), t);
    t += el.classList.contains("brand-story__unit") ? 430 : 160;
  });

  // 2) 잠시 유지한 뒤 한글·영문 꼬리·화살표를 지워 머리글자만 남긴다
  t += 1400;
  wait(() => stageEl.value && stageEl.value.classList.add("is-isolate"), t);

  // 3) 머리글자가 워드마크로 수렴
  t += 850;
  wait(() => flyToWordmark(), t);
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
.brand-story__joint {
  font-size: 2.2cqi;
  color: var(--bs-muted);
  align-self: center;
  padding-top: 2.4cqi;
  opacity: 0;
  transition: opacity 0.55s ease;
}
.brand-story__joint.show {
  opacity: 0.85;
}

/* isolate 단계 — 한글 설명·영문 꼬리·화살표를 지우고 머리글자만 남긴다 */
.brand-story__stage.is-isolate .brand-story__unit small,
.brand-story__stage.is-isolate .brand-story__rest,
.brand-story__stage.is-isolate .brand-story__joint {
  opacity: 0;
  transition: opacity 0.7s ease;
}

/* 워드마크 자리로 날아가는 머리글자 (JS 생성 — scopeAttrName() 으로 스코프 속성 부여) */
.brand-story__flyer {
  position: absolute;
  z-index: 5;
  font-weight: 800;
  color: var(--bs-safety);
  line-height: 1;
  transition:
    transform 1.05s cubic-bezier(0.72, -0.02, 0.16, 1),
    font-size 1.05s cubic-bezier(0.72, -0.02, 0.16, 1),
    color 0.5s ease;
  will-change: transform;
  white-space: nowrap;
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
    color 0.6s ease,
    text-shadow 0.6s ease;
}
.brand-story__slot.on {
  opacity: 1;
  transform: scale(1);
}
.brand-story__slot.is-safety {
  color: var(--bs-safety);
  text-shadow: 0 0 4cqi rgba(245, 179, 1, 0.35);
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
  .brand-story__joint,
  .brand-story__flyer,
  .brand-story__slot,
  .brand-story__tagline,
  .brand-story__sub,
  .brand-story__eyebrow {
    transition: none;
  }
}
</style>
