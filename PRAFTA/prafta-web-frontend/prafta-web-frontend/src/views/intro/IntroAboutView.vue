<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="mission">
      <p class="mission__slogan">
        현장이 돌아가는 것만으로 근태와 안전이 증명되는 회사를 만듭니다.
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

    <section class="section section--alt">
      <div class="section__inner section__inner--narrow">
        <table class="company-table">
          <tbody>
            <tr v-for="row in companyInfo" :key="row.label">
              <th>{{ row.label }}</th>
              <td>{{ row.value }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <IntroCtaBand
      heading="프라프타와 함께, 현장 운영을 하나로 만들어 보세요."
    />

    <IntroFooter />
    <IntroFloatingCta />
  </div>
</template>

<script setup>
import IntroHeader from "@/components/intro/IntroHeader.vue";
import IntroFooter from "@/components/intro/IntroFooter.vue";
import IntroFloatingCta from "@/components/intro/IntroFloatingCta.vue";
import IntroCtaBand from "@/components/intro/IntroCtaBand.vue";
import IntroBrandStory from "@/components/intro/IntroBrandStory.vue";

// 설립/소재지는 PH(§10 목록 #5)라 표에서 제외 — 임의 값 생성 금지
const companyInfo = [
  { label: "회사명", value: "프라프타" },
  { label: "대표", value: "김여진" },
  { label: "사업자등록번호", value: "111-12-46366" },
  { label: "주요 서비스", value: "PRAFTA(근태·안전 통합 SaaS)" },
];
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
  /* 한국어는 기본값(word-break:normal)에서 음절 단위로 끊겨 "만/듭니다."처럼 어절 중간이
     갈라진다. keep-all 로 어절(띄어쓰기) 단위 줄바꿈을 강제하고, balance 로 두 줄 길이를 맞춘다. */
  word-break: keep-all;
  text-wrap: balance;
}
.section {
  padding: 56px var(--header-padding-x);
}
.section--alt {
  background: var(--color-bg);
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
.company-table {
  width: 100%;
  border-collapse: collapse;
}
.company-table th,
.company-table td {
  padding: 14px 4px;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
}
.company-table th {
  width: 160px;
  color: var(--color-text-muted);
  font-weight: 600;
}
.company-table td {
  color: var(--color-text-strong);
  font-weight: 600;
}
</style>
