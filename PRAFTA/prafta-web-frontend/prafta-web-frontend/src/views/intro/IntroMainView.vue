<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="hero">
      <div class="hero__inner">
        <h1 class="hero__title">근태부터 안전까지, 현장 운영을 하나로</h1>
        <p class="hero__desc">
          출퇴근·스케줄·연차의 근태관리와 TBM·위험성평가·점검의 안전관리.
          따로 쓰던 두 시스템을 PRAFTA 하나로 운영합니다.
        </p>
        <div class="hero__actions">
          <IntroButton to="/contact" variant="primary" size="lg"
            >도입문의</IntroButton
          >
          <a href="#features" class="intro-button intro-button--ghost intro-button--lg"
            >기능 살펴보기</a
          >
        </div>
        <div class="hero__preview" aria-hidden="true">
          <div v-for="p in heroPreviewChips" :key="p.label" class="hero__preview-chip">
            <span>{{ p.icon }}</span>
            <span>{{ p.label }}</span>
          </div>
        </div>
        <ul class="hero__badges">
          <li v-for="b in heroBadges" :key="b">{{ b }}</li>
        </ul>
      </div>
    </section>

    <section class="section">
      <div class="section__inner">
        <h2 class="section__title">왜 하나의 시스템이어야 할까요</h2>
        <p class="section__lead">
          근태 시스템 따로, 안전 시스템 따로 쓰면 같은 현장·같은 사람의
          데이터가 두 번 입력되고, 두 곳에서 어긋납니다.
        </p>
        <div class="grid grid--4">
          <IntroIconCard
            v-for="c in whyCards"
            :key="c.title"
            :title="c.title"
            :desc="c.desc"
          >
            <template #icon>{{ c.icon }}</template>
          </IntroIconCard>
        </div>
      </div>
    </section>

    <section id="features" class="section section--alt">
      <div class="section__inner">
        <h2 class="section__title">PRAFTA가 대신하는 일</h2>
        <div class="feature-map">
          <div class="feature-map__col">
            <p class="feature-map__label feature-map__label--attd">근태관리</p>
            <ul>
              <li v-for="item in attdMapItems" :key="item.label">
                <router-link :to="item.to">{{ item.label }}</router-link>
              </li>
            </ul>
          </div>
          <div class="feature-map__col">
            <p class="feature-map__label feature-map__label--safety">안전관리</p>
            <ul>
              <li v-for="item in safetyMapItems" :key="item.label">
                <router-link :to="item.to">{{ item.label }}</router-link>
              </li>
            </ul>
          </div>
        </div>
        <div class="feature-map__special">
          <router-link
            v-for="s in specialMapItems"
            :key="s.label"
            :to="s.to"
            class="feature-map__special-item"
            >{{ s.label }}</router-link
          >
        </div>
      </div>
    </section>

    <IntroFeatureSection
      axis="attd"
      label="근태관리"
      heading="출근 한 번이면, 마감까지 빈틈없이"
      description="GPS 지오펜스 출퇴근에서 시작해 스케줄 대비 자동 판정, 시간 표준화, 휴게 공제, 마감까지. 수기 집계가 사라집니다."
      :bullets="attdHighlightBullets"
    >
      <template #footnote>
        <router-link to="/attendance" class="feature-link"
          >근태관리 자세히 보기 →</router-link
        >
      </template>
    </IntroFeatureSection>

    <IntroFeatureSection
      axis="safety"
      label="안전관리"
      heading="안전활동이 그대로 기록이 됩니다"
      description="TBM, 위험성평가, 순회점검을 모바일에서 수행하면 수료 서명·평가 이력·점검 결과가 자동으로 축적됩니다. 사고 1건을 조회하면 관련 기록이 한 화면에 묶입니다."
      :bullets="safetyHighlightBullets"
      reverse
    >
      <template #footnote>
        <router-link to="/safety" class="feature-link"
          >안전관리 자세히 보기 →</router-link
        >
      </template>
    </IntroFeatureSection>

    <section class="section">
      <div class="section__inner">
        <h2 class="section__title">다른 곳에는 없는 기능</h2>
        <div class="grid grid--3">
          <IntroIconCard
            v-for="c in specialCards"
            :key="c.title"
            :title="c.title"
            :desc="c.desc"
            :to="c.to"
          >
            <template #icon>{{ c.icon }}</template>
          </IntroIconCard>
        </div>
      </div>
    </section>

    <section class="section section--alt">
      <div class="section__inner">
        <h2 class="section__title">근로자는 앱으로, 관리자는 웹으로</h2>
        <div class="platform-split">
          <div class="platform-split__col">
            <h3>모바일 앱</h3>
            <ul>
              <li v-for="b in appBullets" :key="b">{{ b }}</li>
            </ul>
            <IntroMockupFrame
              variant="phone"
              :src="contractEsign"
              alt="모바일 앱에서 근로계약서 전자서명 화면"
            />
          </div>
          <div class="platform-split__col">
            <h3>관리자 웹</h3>
            <ul>
              <li v-for="b in webBullets" :key="b">{{ b }}</li>
            </ul>
            <div class="platform-split__placeholder" aria-hidden="true">
              <span>🖥️</span>
              <span>현황 대시보드</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section__inner">
        <h2 class="section__title">현장이 있는 모든 산업에서</h2>
        <div class="chips">
          <span v-for="c in industryChips" :key="c" class="chips__item">{{
            c
          }}</span>
        </div>
      </div>
    </section>

    <section class="section section--alt">
      <div class="section__inner section__inner--narrow">
        <h2 class="section__title">
          데이터는 필요한 만큼만, 권한은 필요한 사람에게만
        </h2>
        <ul class="bullet-list">
          <li v-for="b in securityBullets" :key="b">{{ b }}</li>
        </ul>
      </div>
    </section>

    <section class="pricing-teaser">
      <div class="pricing-teaser__inner">
        <p>
          정규직 인당 월 5,000원, 일용직 일일계정 월 3,000원(VAT 별도). 쓴
          만큼만 부과되고, 하드웨어 비용이 없습니다.
        </p>
        <router-link to="/pricing" class="feature-link">이용요금 보기 →</router-link>
      </div>
    </section>

    <section class="section">
      <div class="section__inner section__inner--narrow">
        <h2 class="section__title">자주 묻는 질문</h2>
        <IntroFaqAccordion :items="faqItems" />
      </div>
    </section>

    <IntroCtaBand
      heading="두 개의 시스템을 검토하고 계셨다면, 하나로 충분합니다."
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
import IntroButton from "@/components/intro/IntroButton.vue";
import IntroIconCard from "@/components/intro/IntroIconCard.vue";
import IntroFeatureSection from "@/components/intro/IntroFeatureSection.vue";
import IntroMockupFrame from "@/components/intro/IntroMockupFrame.vue";
import IntroFaqAccordion from "@/components/intro/IntroFaqAccordion.vue";
import IntroCtaBand from "@/components/intro/IntroCtaBand.vue";
import contractEsign from "@/assets/intro/contract-esign.jpg";

// 정적 콘텐츠 데이터(문구는 guide-v2.md §5.1 + 본 plan §2 수정 반영, 임의 추가 금지)
const heroPreviewChips = [
  { icon: "🕒", label: "출퇴근" },
  { icon: "📅", label: "스케줄" },
  { icon: "🦺", label: "TBM" },
  { icon: "⚠️", label: "위험성평가" },
];
const heroBadges = [
  "하드웨어 설치 없음",
  "앱+웹 SaaS",
  "기존 프로세스 병행 도입 가능",
];

const whyCards = [
  { icon: "🔗", title: "진짜 통합", desc: "출퇴근·조직·권한이라는 하나의 기반 위에 근태와 안전활동이 함께 기록됩니다. 누가, 언제, 어느 현장에 있었는지가 모든 기록의 뿌리가 됩니다." },
  { icon: "📱", title: "무설치 경량 SaaS", desc: "하드웨어·설치물 없이 근로자는 앱, 관리자는 웹으로. 기존 프로세스와 병행하며 도입할 수 있습니다." },
  { icon: "📜", title: "법규를 시스템으로", desc: "근로기준법의 계약서 교부·보존, 연차 부여·사용촉진부터 중대재해처벌법 대응 기록까지, 법 요건을 운영 흐름에 담았습니다." },
  { icon: "🏗️", title: "현장 특화", desc: "매일 바뀌는 일용직, 겹겹의 하도급 구조, GPS 지오펜스 출퇴근. 사무실이 아니라 현장을 기준으로 설계했습니다." },
];

// §2.2 정합성 노트: 실제 attendance.html/safety.html 앵커 6종만 링크
const attdMapItems = [
  { label: "출퇴근 기록", to: "/attendance#clock" },
  { label: "근무일정·교대", to: "/attendance#schedule" },
  { label: "휴가·연차", to: "/attendance#leave" },
  { label: "요청·승인", to: "/attendance#approval" },
  { label: "근태 마감", to: "/attendance#closing" },
  { label: "일용직 관리·전자계약", to: "/attendance#daily-worker" },
];
const safetyMapItems = [
  { label: "TBM", to: "/safety#tbm" },
  { label: "위험성평가", to: "/safety#risk" },
  { label: "순회점검", to: "/safety#inspection" },
  { label: "사고관리", to: "/safety#accident" },
  { label: "하도급 연동", to: "/safety#subcontract" },
  { label: "AI 안전 지원", to: "/safety#ai" },
];
const specialMapItems = [
  { label: "일용직 전자계약", to: "/attendance#daily-worker" },
  { label: "AI 안전 지원", to: "/safety#ai" },
];

const attdHighlightBullets = [
  "GPS 지오펜스 출퇴근",
  "스케줄 기반 지각/조퇴/초과근무 자동 판정",
  "마감 차단 조건으로 빠짐없이 확정되는 근태 데이터",
];
const safetyHighlightBullets = [
  "TBM 개설부터 수료 서명까지",
  "위험성평가 5×4 매트릭스와 개선 이력",
  "사고 1건 → 관련 활동 기록 일괄조회",
];

const specialCards = [
  { icon: "📝", title: "일용직 전자계약", desc: "QR 출역 등록부터 근로계약서 자동 작성·전자서명·3년 보존까지.", to: "/attendance#daily-worker" },
  { icon: "🔗", title: "하도급 연동", desc: "원청–협력사 n차 구조를 시스템이 그대로 수용, 승인 기반 데이터 공유.", to: "/safety#subcontract" },
  { icon: "🤖", title: "AI 안전 지원", desc: "TBM 교육안과 위험성평가 개선안을 AI가 초안 작성, 담당자가 검토·확정.", to: "/safety#ai" },
];

const appBullets = ["출퇴근", "휴가 신청", "TBM 입실", "점검", "계약서 열람"];
const webBullets = ["스케줄", "승인", "마감", "안전활동 관리", "현황 대시보드"];

const industryChips = ["건설", "제조", "물류", "시설관리", "조선", "플랜트", "도급·파견"];

const securityBullets = [
  "위치정보는 출퇴근 시점에만 최소 수집(상시 추적 없음)",
  "화면·사업장·조직 3축 권한 통제",
  "주요 변경의 감사 로그(누가·언제·무엇을·왜)",
  "동시 처리 충돌 방지(선점 잠금)",
];

// FAQ 6문항 중 5번(부분 도입) 비노출, 6번은 소요기간 PH 문장 제거
const faqItems = [
  { q: "하드웨어나 장비 설치가 필요한가요?", a: "아니요. 근로자용 모바일 앱과 관리자용 웹으로만 구성된 SaaS입니다. 현장 설치물이 없어 기존 프로세스와 병행하며 도입할 수 있습니다." },
  { q: "일용직 근로자도 관리할 수 있나요?", a: "네. QR로 당일 출역 등록이 가능하고, 근로계약서 전자서명·교부·3년 보존까지 시스템에서 처리됩니다. 계정은 현장별 슬롯으로 관리되어 당일 자정에 자동 만료됩니다." },
  { q: "협력사(하도급)도 함께 써야 하나요?", a: "원청 기준을 복제하는 미러 사업장 방식이라 협력사는 별도 세팅 없이 시작합니다. 데이터 공유는 협력사 승인 기반이며, 연동을 해지해도 협력사 데이터는 협력사 소유로 남습니다." },
  { q: "위치정보를 상시 수집하나요?", a: "아니요. 출퇴근 시점에만 최소한으로 처리하며 상시 추적하지 않습니다. 상세 위치 조회는 별도 권한으로 통제됩니다." },
  { q: "도입에 얼마나 걸리나요?", a: "계약 후 사업장·조직·권한 세팅을 거쳐 현장 온보딩으로 진행됩니다. 근로자는 QR 안내만으로 시작합니다." },
];
</script>

<style scoped>
.intro-page {
  background: var(--color-surface);
  color: var(--color-text);
}
.hero {
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg));
  border-bottom: 1px solid var(--color-border);
  padding: 72px var(--header-padding-x) 56px;
  text-align: center;
}
.hero__inner {
  max-width: 720px;
  margin: 0 auto;
}
.hero__title {
  font-size: clamp(1.8rem, 4vw, 3rem);
  font-weight: 800;
  color: var(--color-text-strong);
  line-height: 1.3;
  margin: 0 0 18px;
}
.hero__desc {
  color: var(--color-text-muted);
  font-size: 16px;
  line-height: 1.7;
  margin: 0 0 28px;
}
.hero__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 36px;
}
.hero__preview {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}
.hero__preview-chip {
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
.hero__badges {
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
.section {
  padding: 72px var(--header-padding-x);
}
.section--alt {
  background: var(--color-bg);
}
.section__inner {
  max-width: 1160px;
  margin: 0 auto;
}
.section__inner--narrow {
  max-width: 760px;
}
.section__title {
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--color-text-strong);
  text-align: center;
  margin: 0 0 10px;
}
.section__lead {
  text-align: center;
  color: var(--color-text-muted);
  margin: 0 0 36px;
}
.grid {
  display: grid;
  gap: 18px;
}
.grid--4 {
  grid-template-columns: repeat(4, 1fr);
}
.grid--3 {
  grid-template-columns: repeat(3, 1fr);
}
.feature-map {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.feature-map__col {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
}
.feature-map__label {
  font-weight: 700;
  font-size: 14px;
  margin: 0 0 14px;
}
.feature-map__label--attd {
  color: var(--color-attd);
}
.feature-map__label--safety {
  color: var(--color-primary);
}
.feature-map__col ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.feature-map__col a {
  color: var(--color-text);
  text-decoration: none;
  font-size: 14px;
}
.feature-map__col a:hover {
  color: var(--color-primary);
}
.feature-map__special {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}
.feature-map__special-item {
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid var(--color-border-strong);
  color: var(--color-text-strong);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}
.feature-link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}
.platform-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}
.platform-split__col h3 {
  color: var(--color-text-strong);
  margin: 0 0 14px;
}
.platform-split__col ul {
  list-style: none;
  margin: 0 0 18px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 14px;
}
.platform-split__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 220px;
  border-radius: var(--card-radius);
  border: 1px dashed var(--color-border-strong);
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-text-muted);
}
.chips {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}
.chips__item {
  padding: 8px 16px;
  border-radius: 999px;
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 13px;
}
.bullet-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bullet-list li {
  padding-left: 18px;
  position: relative;
  color: var(--color-text);
  font-size: 14px;
}
.bullet-list li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 7px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}
.pricing-teaser {
  background: var(--color-primary);
  color: #fff;
  padding: 28px var(--header-padding-x);
  text-align: center;
}
.pricing-teaser__inner {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}
.pricing-teaser .feature-link {
  color: #fff;
  text-decoration: underline;
}

@media (max-width: 1023px) {
  .grid--4,
  .grid--3,
  .feature-map,
  .platform-split {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 767px) {
  .grid--4,
  .grid--3,
  .feature-map,
  .platform-split {
    grid-template-columns: 1fr;
  }
  .section {
    padding: 48px var(--header-padding-x);
  }
}
</style>
