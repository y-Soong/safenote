<template>
  <section class="intro-why" :class="{ 'intro-why--alt': alt }">
    <div class="intro-why__inner">
      <h2 class="intro-why__title">왜 하나의 시스템이어야 할까요</h2>
      <p class="intro-why__lead">
        근태 시스템 따로, 안전 시스템 따로 쓰면 같은 현장·같은 사람의 데이터가
        두 번 입력되고, 두 곳에서 어긋납니다.
      </p>
      <div class="intro-why__grid">
        <IntroIconCard
          v-for="c in cards"
          :key="c.title"
          :title="c.title"
          :desc="c.desc"
        >
          <template #icon>{{ c.icon }}</template>
        </IntroIconCard>
      </div>
    </div>
  </section>
</template>

<script setup>
import IntroIconCard from "./IntroIconCard.vue";

defineProps({
  // 배경을 한 단계 어둡게(교차 섹션용)
  alt: { type: Boolean, default: false },
});

// 문구는 guide-v2.md §5.1 S2 원문 — 임의 변경 금지
const cards = [
  {
    icon: "🔗",
    title: "진짜 통합",
    desc: "출퇴근·조직·권한이라는 하나의 기반 위에 근태와 안전활동이 함께 기록됩니다. 누가, 언제, 어느 현장에 있었는지가 모든 기록의 뿌리가 됩니다.",
  },
  {
    icon: "📱",
    title: "무설치 경량 SaaS",
    desc: "하드웨어·설치물 없이 근로자는 앱, 관리자는 웹으로. 기존 프로세스와 병행하며 도입할 수 있습니다.",
  },
  {
    icon: "📜",
    title: "법규를 시스템으로",
    desc: "근로기준법의 계약서 교부·보존, 연차 부여·사용촉진부터 중대재해처벌법 대응 기록까지, 법 요건을 운영 흐름에 담았습니다.",
  },
  {
    icon: "🏗️",
    title: "현장 특화",
    desc: "매일 바뀌는 일용직, 겹겹의 하도급 구조, GPS 지오펜스 출퇴근. 사무실이 아니라 현장을 기준으로 설계했습니다.",
  },
];
</script>

<style scoped>
.intro-why {
  padding: 72px var(--header-padding-x);
}
.intro-why--alt {
  background: var(--color-bg);
}
.intro-why__inner {
  max-width: 1160px;
  margin: 0 auto;
}
.intro-why__title {
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--color-text-strong);
  text-align: center;
  margin: 0 0 10px;
  word-break: keep-all;
}
.intro-why__lead {
  text-align: center;
  color: var(--color-text-muted);
  margin: 0 0 36px;
  word-break: keep-all;
}
.intro-why__grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(4, 1fr);
}

@media (max-width: 1023px) {
  .intro-why__grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 767px) {
  .intro-why__grid {
    grid-template-columns: 1fr;
  }
  .intro-why {
    padding: 48px var(--header-padding-x);
  }
}
</style>
