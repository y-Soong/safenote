<template>
  <section class="intro-hero">
    <div class="intro-hero__inner">
      <!-- 메인(/)에서는 페이지 대표 제목이라 h1, 다른 페이지에 붙일 때는 h2 로 낮춘다 -->
      <component :is="headingTag" class="intro-hero__title">
        근태부터 안전까지, 현장 운영을 하나로
      </component>
      <p class="intro-hero__desc">
        출퇴근·스케줄·연차의 근태관리와 TBM·위험성평가·점검의 안전관리. 따로
        쓰던 두 시스템을 PRAFTA 하나로 운영합니다.
      </p>

      <div class="intro-hero__actions">
        <IntroButton to="/contact" variant="primary" size="lg"
          >도입문의</IntroButton
        >
        <!-- 같은 페이지 안에 기능 맵이 있으면 앵커(#features)로, 없으면 해당 페이지로 이동 -->
        <IntroButton
          v-if="featuresTo"
          :to="featuresTo"
          variant="ghost"
          size="lg"
          >기능 살펴보기</IntroButton
        >
        <IntroButton v-else :href="featuresHref" variant="ghost" size="lg"
          >기능 살펴보기</IntroButton
        >
      </div>

      <div class="intro-hero__preview" aria-hidden="true">
        <div v-for="p in previewChips" :key="p.label" class="intro-hero__chip">
          <span>{{ p.icon }}</span>
          <span>{{ p.label }}</span>
        </div>
      </div>

      <ul class="intro-hero__badges">
        <li v-for="b in badges" :key="b">{{ b }}</li>
      </ul>
    </div>
  </section>
</template>

<script setup>
import IntroButton from "./IntroButton.vue";

defineProps({
  // 제목 태그 — 페이지당 h1 1개 규칙(guide §3.2)을 지키기 위해 호출 측에서 지정
  headingTag: { type: String, default: "h1" },
  // 같은 페이지 안의 기능 맵 앵커
  featuresHref: { type: String, default: "#features" },
  // 다른 페이지의 기능 맵으로 보낼 때 (지정하면 featuresHref 대신 라우터 이동)
  featuresTo: { type: String, default: "" },
});

// 문구는 guide-v2.md §5.1 S1 원문 — 임의 변경 금지
const previewChips = [
  { icon: "🕒", label: "출퇴근" },
  { icon: "📅", label: "스케줄" },
  { icon: "🦺", label: "TBM" },
  { icon: "⚠️", label: "위험성평가" },
];
const badges = [
  "하드웨어 설치 없음",
  "앱+웹 SaaS",
  "기존 프로세스 병행 도입 가능",
];
</script>

<style scoped>
.intro-hero {
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg));
  border-bottom: 1px solid var(--color-border);
  padding: 72px var(--header-padding-x) 56px;
  text-align: center;
}
.intro-hero__inner {
  max-width: 720px;
  margin: 0 auto;
}
.intro-hero__title {
  font-size: clamp(1.8rem, 4vw, 3rem);
  font-weight: 800;
  color: var(--color-text-strong);
  line-height: 1.3;
  margin: 0 0 18px;
  word-break: keep-all;
}
.intro-hero__desc {
  color: var(--color-text-muted);
  font-size: 16px;
  line-height: 1.7;
  margin: 0 0 28px;
  word-break: keep-all;
}
.intro-hero__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 36px;
}
.intro-hero__preview {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}
.intro-hero__chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--card-radius);
  border: var(--card-border);
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-text);
}
.intro-hero__badges {
  list-style: none;
  display: flex;
  justify-content: center;
  gap: 8px 20px;
  flex-wrap: wrap;
  margin: 0;
  padding: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>
