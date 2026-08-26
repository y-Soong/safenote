<template>
  <!--
    프라프타 홈페이지 — 요금 안내 (/pricing)
    플랜: 근태·인사 / 산업안전 / 통합(추천) / 엔터프라이즈(100인 이상).
    ※ 실제 단가는 미확정 — '가격 협의' placeholder. 확정 시 price 값만 교체.
  -->
  <div class="intro-page">
    <IntroHeader />

    <section class="phead">
      <div class="phead__inner">
        <p class="phead__kicker">PRICING</p>
        <h1 class="phead__title">요금 안내</h1>
        <p class="phead__slogan">
          필요한 만큼만 — 근태·인사, 산업안전, 또는 둘을 하나로.
        </p>
      </div>
    </section>

    <section class="section">
      <div class="section__inner">
        <ul class="plans">
          <li
            v-for="p in plans"
            :key="p.key"
            class="plan card"
            :class="{ 'plan--featured': p.featured }"
          >
            <div v-if="p.featured" class="plan__ribbon">가장 인기</div>
            <h3 class="plan__name">{{ p.name }}</h3>
            <p class="plan__target">{{ p.target }}</p>
            <div class="plan__price">
              <span class="plan__amount">{{ p.price }}</span>
              <span v-if="p.unit" class="plan__unit">{{ p.unit }}</span>
            </div>
            <ul class="plan__feats">
              <li v-for="f in p.feats" :key="f">{{ f }}</li>
            </ul>
            <button
              type="button"
              class="btn"
              :class="p.featured ? 'btn--primary' : 'btn--outline'"
              @click="goContact"
            >
              {{ p.cta }}
            </button>
          </li>
        </ul>

        <p class="note">
          · 표시 금액은 안내용이며 사업장 규모·기능 구성에 따라 협의됩니다.
          <strong>100인 이상 사업장은 엔터프라이즈로 별도 책정</strong
          >됩니다.<br />
          · 정확한 견적은
          <button type="button" class="linkbtn" @click="goContact">
            도입 문의</button
          >로 안내드립니다.
        </p>
      </div>
    </section>

    <section class="cta">
      <div class="cta__inner">
        <h2 class="cta__title">우리 현장에 맞는 구성이 궁금하신가요?</h2>
        <button
          type="button"
          class="btn btn--primary btn--lg"
          @click="goContact"
        >
          맞춤 견적 문의하기
        </button>
      </div>
    </section>

    <IntroFooter />
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import IntroHeader from "./components/IntroHeader.vue";
import IntroFooter from "./components/IntroFooter.vue";

const router = useRouter();
const goContact = () => router.push("/contact");

// TODO: 실제 단가 확정 시 price/unit 값 교체 (현재 협의 placeholder)
const plans = [
  {
    key: "attd",
    name: "근태·인사",
    target: "출퇴근·근태·연차 관리가 필요한 현장",
    price: "가격 협의",
    unit: "1인 / 월",
    feats: [
      "모바일 출퇴근 · GPS 지오펜싱",
      "근무표 / 교대 관리",
      "일용직 전용 근태 · QR 발급",
      "연차 자동관리 · 사용촉진",
      "근태 통계 자동 집계",
    ],
    cta: "도입 문의",
    featured: false,
  },
  {
    key: "safety",
    name: "산업안전",
    target: "위험성평가·안전점검·TBM 관리가 필요한 현장",
    price: "가격 협의",
    unit: "1인 / 월",
    feats: [
      "위험성평가 · 개선 이력 관리",
      "안전점검 체크리스트",
      "TBM(작업 전 안전회의) 기록",
      "아차사고 관리",
      "위험요인·TBM 자료 AI 추천",
    ],
    cta: "도입 문의",
    featured: false,
  },
  {
    key: "all",
    name: "통합",
    target: "근태·인사 + 산업안전을 하나로",
    price: "가격 협의",
    unit: "1인 / 월",
    feats: [
      "근태·인사 전체 기능",
      "산업안전 전체 기능",
      "사고 시 시계열 통합 증빙",
      "인사–안전 데이터 단일 시스템",
      "가장 합리적인 통합 단가",
    ],
    cta: "도입 문의",
    featured: true,
  },
  {
    key: "enterprise",
    name: "엔터프라이즈",
    target: "100인 이상 사업장 · 다현장/다사업장",
    price: "별도 견적",
    unit: "",
    feats: [
      "통합 플랜 전체 기능",
      "다사업장·조직 규모 대응",
      "전담 도입·운영 지원",
      "맞춤 정책/연동 협의",
      "확장 로드맵 우선 적용",
    ],
    cta: "엔터프라이즈 문의",
    featured: false,
  },
];
</script>

<style scoped>
.intro-page {
  height: 100vh;
  height: 100dvh;
  overflow-y: auto;
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 17.5px;
  line-height: 1.72;
  word-break: keep-all;
  overflow-wrap: break-word;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 20px;
  border: 1px solid transparent;
  border-radius: var(--btn-radius-lg);
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
  width: 100%;
}
.btn--lg {
  height: 46px;
  font-size: 15px;
  width: auto;
}
.btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.btn--primary:hover {
  background: var(--color-primary-hover);
}
.btn--outline {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.btn--outline:hover {
  background: var(--color-primary);
  color: #fff;
}
.btn--white {
  background: #fff;
  color: var(--color-primary-pressed);
}
.btn--white:hover {
  background: rgba(255, 255, 255, 0.9);
}

.phead {
  background: linear-gradient(180deg, #f1faf4 0%, var(--color-surface) 100%);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}
.phead__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 56px var(--header-padding-x);
}
.phead__kicker {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.12em;
  margin: 0 0 10px;
}
.phead__title {
  color: var(--color-text-strong);
  font-size: 38px;
  font-weight: 800;
  margin: 0 0 8px;
}
.phead__slogan {
  font-size: 17.5px;
  color: var(--color-text-muted);
  margin: 0;
}

.section {
  padding: 56px var(--header-padding-x);
}
.section__inner {
  max-width: 1080px;
  margin: 0 auto;
}

.plans {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  align-items: stretch;
}
.card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
}
.plan {
  padding: 26px 20px;
  display: flex;
  flex-direction: column;
  position: relative;
}
.plan--featured {
  border: 2px solid var(--color-primary);
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.18);
}
.plan__ribbon {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 14px;
  border-radius: 20px;
  white-space: nowrap;
}
.plan__name {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 4px 0 6px;
}
.plan__target {
  font-size: 14.5px;
  color: var(--color-text-muted);
  min-height: 42px;
  margin: 0 0 14px;
  line-height: 1.5;
}
.plan__price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}
.plan__amount {
  font-size: 25px;
  font-weight: 800;
  color: var(--color-primary-pressed);
}
.plan__unit {
  font-size: 13px;
  color: var(--color-text-muted);
}
.plan__feats {
  list-style: none;
  margin: 0 0 20px;
  padding: 0;
  flex: 1 1 auto;
}
.plan__feats li {
  position: relative;
  padding: 8px 0 8px 22px;
  font-size: 15px;
  color: var(--color-text);
}
.plan__feats li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: var(--color-primary);
  font-weight: 800;
}

.note {
  max-width: 880px;
  margin: 28px auto 0;
  font-size: 14.5px;
  color: var(--color-text-muted);
  text-align: center;
  line-height: 1.7;
}
.note strong {
  color: var(--color-text-strong);
}
.linkbtn {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  text-decoration: underline;
}

.cta {
  padding: 64px var(--header-padding-x);
  text-align: center;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  color: var(--color-text);
}
.cta__title {
  color: var(--color-text-strong);
  font-size: 30px;
  font-weight: 800;
  margin: 0 0 20px;
}

@media (max-width: 960px) {
  .plans {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 560px) {
  .plans {
    grid-template-columns: 1fr;
  }
}
</style>
