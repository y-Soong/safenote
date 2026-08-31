<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="mission">
      <!-- 줄바꿈 위치를 카피 의도대로 고정한다(자동 줄바꿈에 맡기지 않음) -->
      <p class="mission__slogan">
        <span class="mission__line">현장이 돌아가는 것만으로</span>
        <span class="mission__line"
          >근태와 안전이 증명되는 회사를 만듭니다.</span
        >
      </p>
    </section>

    <IntroBrandStory />

    <section class="section">
      <div class="section__inner section__inner--narrow">
        <p class="lead">
          프라프타는 근태·인사와 산업안전을 하나의 시스템으로 묶은 현장 운영
          SaaS 'PRAFTA'를 만듭니다. 하드웨어 설치 없이 앱과 웹만으로, 일용직이
          많고 하도급 구조가 겹겹인 현장에서도 운영 기록이 곧 증빙이 되도록
          설계했습니다.
        </p>
      </div>
    </section>

    <IntroFooter />
    <IntroFloatingCta />
  </div>
</template>

<script setup>
import IntroHeader from "@/components/intro/IntroHeader.vue";
import IntroFooter from "@/components/intro/IntroFooter.vue";
import IntroFloatingCta from "@/components/intro/IntroFloatingCta.vue";
import IntroBrandStory from "@/components/intro/IntroBrandStory.vue";

// 회사 정보 표(회사명·대표·사업자등록번호)와 하단 CTA 밴드는 2026-08-31 사용자 지시로 제거.
// 회사 정보는 푸터에 그대로 남아 있다.
</script>

<style scoped>
.intro-page {
  background: var(--color-surface);
  color: var(--color-text);
}
.mission {
  padding: 64px var(--header-padding-x) 40px;
  text-align: center;
}
.mission__slogan {
  max-width: 760px;
  margin: 0 auto;
  font-size: clamp(1.4rem, 3vw, 2rem);
  font-weight: 800;
  color: var(--color-text-strong);
  line-height: 1.5;
  /* 줄바꿈은 .mission__line 으로 고정하되, 좁은 화면에서 한 줄이 더 접힐 때
     한국어가 음절 단위로 갈라지지 않도록(예: "만/듭니다.") 어절 단위를 유지한다. */
  word-break: keep-all;
}
.mission__line {
  display: block;
}
.section {
  padding: 56px var(--header-padding-x);
}
.section__inner {
  max-width: 900px;
  margin: 0 auto;
}
.section__inner--narrow {
  max-width: 680px;
}
.lead {
  text-align: center;
  color: var(--color-text-muted);
  line-height: 1.8;
  font-size: 15px;
  margin: 0;
}
</style>
