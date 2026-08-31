<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="page-head">
      <div class="page-head__inner">
        <h1 class="page-head__title">이용요금</h1>
        <p class="page-head__lead">
          인원 기반 월 과금 — 그달에 사용한 만큼만 부과됩니다. 설치비·하드웨어
          비용이 없습니다.
        </p>
      </div>
    </section>

    <section class="section">
      <div class="section__inner">
        <table class="pricing-table">
          <thead>
            <tr>
              <th>구분</th>
              <th>요금</th>
              <th>부과 기준</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pricingRows" :key="row.label" :class="{ 'is-enterprise': row.enterprise }">
              <td data-label="구분">{{ row.label }}</td>
              <td data-label="요금">{{ row.price }}</td>
              <td data-label="부과 기준">{{ row.basis }}</td>
            </tr>
          </tbody>
        </table>
        <p class="pricing-note">
          모든 요금은 VAT 별도이며, 월 사용량 기준으로 부과됩니다.
        </p>
      </div>
    </section>

    <section class="section section--alt">
      <div class="section__inner">
        <h2 class="section__title">도입 절차</h2>
        <div class="steps">
          <div v-for="(s, i) in steps" :key="s.title" class="steps__item">
            <span class="steps__no">{{ i + 1 }}</span>
            <div>
              <h3>{{ s.title }}</h3>
              <p>{{ s.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <IntroCtaBand
      heading="정규 근로자 100명 이상 사업장은 도입문의로 상담해 주세요."
      button-label="도입문의"
      button-to="/contact"
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

const pricingRows = [
  { label: "정규직 근로자", price: "인당 월 5,000원", basis: "해당 월 사용 인원 수", enterprise: false },
  { label: "일용직 일일계정", price: "계정당 월 3,000원", basis: "해당 월 활성화된 일일계정 수", enterprise: false },
  { label: "엔터프라이즈", price: "협의", basis: "사업장 정규 근로자 100명 이상 — 별도 상담", enterprise: true },
];

const steps = [
  { title: "계약", desc: "도입 규모와 현장 상황을 확인하고 계약을 체결합니다." },
  { title: "사업장·조직·권한 세팅", desc: "사업장, 조직 구조, 사용자 권한을 설정합니다." },
  { title: "현장 온보딩", desc: "관리자 교육을 진행하고, 근로자는 QR 안내만으로 시작합니다." },
];
</script>

<style scoped>
.intro-page {
  background: var(--color-surface);
  color: var(--color-text);
}
.page-head {
  padding: 56px var(--header-padding-x) 40px;
  text-align: center;
}
.page-head__inner {
  max-width: 640px;
  margin: 0 auto;
}
.page-head__title {
  font-size: clamp(1.6rem, 3vw, 2.2rem);
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 12px;
}
.page-head__lead {
  color: var(--color-text-muted);
  line-height: 1.7;
  margin: 0;
}
.section {
  padding: 48px var(--header-padding-x);
}
.section--alt {
  background: var(--color-bg);
}
.section__inner {
  max-width: 900px;
  margin: 0 auto;
}
.section__title {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-text-strong);
  text-align: center;
  margin: 0 0 28px;
}
.pricing-table {
  width: 100%;
  border-collapse: collapse;
  border: var(--card-border);
  border-radius: var(--card-radius);
  overflow: hidden;
}
.pricing-table th,
.pricing-table td {
  padding: 16px 20px;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
}
.pricing-table th {
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-weight: 700;
}
.pricing-table tr.is-enterprise td {
  background: var(--color-bg);
  font-weight: 700;
  color: var(--color-text-strong);
}
.pricing-note {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 16px 0 0;
}
.steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.steps__item {
  display: flex;
  gap: 14px;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
}
.steps__no {
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.steps__item h3 {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--color-text-strong);
}
.steps__item p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .steps {
    grid-template-columns: 1fr;
  }
  .pricing-table thead {
    display: none;
  }
  .pricing-table tr {
    display: block;
    margin-bottom: 12px;
    border-radius: var(--card-radius);
    border: var(--card-border);
  }
  .pricing-table td {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    border-bottom: 1px solid var(--color-border);
  }
  .pricing-table td::before {
    content: attr(data-label);
    color: var(--color-text-muted);
    font-weight: 600;
  }
}
</style>
