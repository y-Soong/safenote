<template>
  <!--
    프라프타 홈페이지 — 메인(랜딩)
    출처: PRAFTA 서비스소개서 / .claude 홈페이지 IA 명세 §1
    위치 규칙: 신규 화면은 views/intro/ 하위에만 생성.
  -->
  <div class="intro-page">
    <IntroHeader />

    <!-- 1. 히어로 -->
    <section class="hero">
      <span class="hero__bg" aria-hidden="true"></span>
      <div class="hero__inner">
        <div class="hero__copy">
          <span class="hero__badge"
            ><span class="hero__dot"></span>근태·인사 + 산업안전 통합 SaaS</span
          >
          <h1 class="hero__title">
            <span class="hero__nowrap">현장의 사람과 안전을,</span><br />
            <span class="hero__accent">하나의 서비스로</span>
          </h1>
          <p class="hero__desc">
            흩어진 출퇴근·연차·근무 관리와 위험성평가·TBM·안전점검을
            <strong>PRAFTA 하나로</strong> — 노무와 산업재해 리스크를 동시에
            줄입니다.
          </p>
          <div class="hero__actions">
            <button
              type="button"
              class="btn btn--primary btn--lg"
              @click="goContact"
            >
              도입 문의하기
            </button>
            <a
              class="btn btn--ghost btn--lg"
              :href="BROCHURE_URL"
              download="PRAFTA_서비스소개서.pdf"
              >소개서 다운로드</a
            >
            <button type="button" class="btn btn--text" @click="goService">
              서비스 로그인 →
            </button>
          </div>
          <ul class="hero__chips">
            <li>근태·인사 관리</li>
            <li>산업안전 관리</li>
            <li>AI 안전 지원</li>
          </ul>
        </div>

        <!-- 웹 대시보드(브라우저 프레임) + 모바일 위험성 발굴(자체 폰 프레임 겹침) 목업 -->
        <div class="hero__visual">
          <span class="hero__glow" aria-hidden="true"></span>
          <div class="hero__shot">
            <div class="hero__bar" aria-hidden="true">
              <i></i><i></i><i></i>
            </div>
            <img
              class="hero__dash"
              :src="dashboardImg"
              alt="PRAFTA 관리자 대시보드 화면"
            />
          </div>
          <img
            class="hero__phone"
            :src="riskAppImg"
            alt="PRAFTA 모바일 위험성 발굴 화면"
          />
        </div>
      </div>
    </section>

    <!-- 2. 핵심 가치(특장점 4) -->
    <section class="section">
      <div class="section__inner">
        <p class="section__kicker">WHY PRAFTA</p>
        <h2 class="section__title">PRAFTA 도입 시, 달라지는 점</h2>
        <ul class="cards cards--4">
          <li v-for="(v, i) in values" :key="v.title" class="card value">
            <div class="value__num">{{ i + 1 }}</div>
            <h3 class="card__title">{{ v.title }}</h3>
            <p class="card__desc">{{ v.desc }}</p>
          </li>
        </ul>
      </div>
    </section>

    <!-- 3. 무엇을 하나로 묶나 (3축) -->
    <section class="section section--alt">
      <div class="section__inner">
        <h2 class="section__title">근태·인사와 산업안전을 하나로</h2>
        <ul class="cards cards--3">
          <li v-for="p in pillars" :key="p.title" class="card pillar">
            <h3 class="card__title">{{ p.title }}</h3>
            <ul class="pillar__list">
              <li v-for="it in p.items" :key="it">{{ it }}</li>
            </ul>
          </li>
        </ul>
        <p class="strip">
          근태·인사관리와 산업안전관리를 하나로 — 노무 및 산업재해 리스크를
          동시에 줄이는 플랫폼
        </p>
      </div>
    </section>

    <!-- 4. 도입 효과 / 문제 해결 -->
    <section class="section">
      <div class="section__inner">
        <p class="section__kicker section__kicker--danger">PAIN POINTS</p>
        <h2 class="section__title">현장에서, 이런 어려움 없으셨나요?</h2>
        <div class="problem">
          <div class="card problem__col">
            <h3 class="problem__head">업무 비효율</h3>
            <ul class="problem__list">
              <li v-for="t in painEfficiency" :key="t">{{ t }}</li>
            </ul>
          </div>
          <div class="card problem__col problem__col--risk">
            <h3 class="problem__head">사업 리스크</h3>
            <ul class="problem__list problem__list--risk">
              <li v-for="t in painRisk" :key="t">{{ t }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- 5. 도입 전 → 후 비교 -->
    <section class="section">
      <div class="section__inner">
        <h2 class="section__title section__title--lg">
          현장의 일하는 방식이<br class="br-mobile" />
          이렇게 달라집니다.
        </h2>
        <div class="compare">
          <div class="compare__head">
            <span class="compare__h-area"></span>
            <span class="compare__h-before">도입 전</span>
            <span class="compare__h-after">PRAFTA 도입 후</span>
          </div>
          <div v-for="row in compare" :key="row.area" class="compare__row">
            <span class="compare__area">{{ row.area }}</span>
            <span class="compare__before"><i>✕</i>{{ row.before }}</span>
            <span class="compare__after"><i>✓</i>{{ row.after }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 6. 핵심 기능 미리보기 + 화면 목업 -->
    <section class="section section--alt">
      <div class="section__inner">
        <h2 class="section__title">핵심 기능 미리보기</h2>
        <p class="section__lead">현장 관리에 필요한 기능을 한 곳에서.</p>
        <!-- TODO: MockupFrame 에 videoSrc/appVideoSrc 전달 시 자동재생 영상으로 교체 -->
        <div class="preview">
          <MockupFrame
            label="PRAFTA"
            :overlap="true"
            class="preview__mock"
          />
        </div>
        <ul class="cards cards--3 preview__cards">
          <li v-for="f in featurePreview" :key="f.title" class="card feature">
            <div class="feature__icon"><IntroIcon :name="f.icon" /></div>
            <h3 class="card__title">{{ f.title }}</h3>
            <p class="card__desc">{{ f.desc }}</p>
          </li>
        </ul>
        <div class="section__more">
          <button type="button" class="btn btn--ghost" @click="goServiceIntro">
            서비스 자세히 보기
          </button>
        </div>
      </div>
    </section>

    <!-- 7. 업종별 활용 -->
    <section class="section">
      <div class="section__inner">
        <h2 class="section__title">업종별 활용</h2>
        <p class="section__lead">
          제조, 물류, 산업 현장 등 우리 사업장에 맞는 서비스를 지원합니다.
        </p>
        <ul class="cards cards--4">
          <li v-for="ind in industries" :key="ind.title" class="card industry">
            <div class="industry__icon"><IntroIcon :name="ind.icon" /></div>
            <h3 class="card__title">{{ ind.title }}</h3>
            <p class="card__desc">{{ ind.desc }}</p>
          </li>
        </ul>
      </div>
    </section>

    <!-- 6. AI 안전 지원 배너 -->
    <section class="aiband">
      <div class="aiband__inner">
        <div>
          <p class="aiband__kicker">AI SAFETY</p>
          <h2 class="aiband__title">
            AI 안전 지원 — 최종 판단은 사람이 합니다
          </h2>
          <p class="aiband__desc">
            위험요인·개선안과 TBM 회의자료 초안을 AI가 제안하고,<br />
            모든 결과는 담당자 검토·승인 후에만 공식 기록으로 확정됩니다.
          </p>
        </div>
        <button type="button" class="btn btn--ghost" @click="goServiceIntro">
          AI 기능 보기
        </button>
      </div>
    </section>

    <!-- 9. 하단 CTA -->
    <section class="cta">
      <div class="cta__inner">
        <h2 class="cta__title">현장의 사람과 안전을, 하나의 서비스로.</h2>
        <p class="cta__desc">도입 문의 및 데모는 아래로 연락 주세요.</p>
        <div class="cta__actions">
          <button
            type="button"
            class="btn btn--primary btn--lg"
            @click="goContact"
          >
            도입 문의하기
          </button>
          <a
            class="btn btn--ghost btn--lg"
            :href="BROCHURE_URL"
            download="PRAFTA_서비스소개서.pdf"
            >소개서 다운로드</a
          >
        </div>
      </div>
    </section>

    <IntroFooter />
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import IntroHeader from "./components/IntroHeader.vue";
import IntroFooter from "./components/IntroFooter.vue";
import MockupFrame from "./components/MockupFrame.vue";
import IntroIcon from "./components/IntroIcon.vue";

const SERVICE_PATH = "/safenote";
const BROCHURE_URL = "/downloads/safenote-brochure.pdf";
// 히어로 목업 이미지 (Claude 디자인 산출물, public/intro/)
const dashboardImg = "/intro/dashboard-web.png"; // 웹 관리자 대시보드
const riskAppImg = "/intro/risk-app.png"; // 모바일 위험성 발굴 (배경 투명)
const router = useRouter();
const goService = () => router.push(SERVICE_PATH);
const goContact = () => router.push("/contact");
const goServiceIntro = () => router.push("/service");

const values = [
  {
    title: "하나의 시스템, 합리적인 가격",
    desc: "근태·인사 관리와 산업안전 관리를 하나의 시스템, 합리적인 가격으로 사용할 수 있습니다.",
  },
  {
    title: "자동으로 쌓이는 증빙",
    desc: "근태·인사, 각종 교육 및 산업안전 데이터가 하나의 시스템에서 자동으로 증빙·관리됩니다.",
  },
  {
    title: "현장 전 인력을 촘촘하게",
    desc: "정규직부터 일용직까지, 관리하지 못하는 현장 인력 없이 촘촘한 관리가 가능합니다.",
  },
  {
    title: "법률·규제에 유연하게 대응",
    desc: "근로기준법, 중대재해처벌법, 개인정보보호법 등 각종 규제에 유연하게 대응합니다.",
  },
];

const pillars = [
  {
    title: "근태·인사 관리",
    items: [
      "모바일 출퇴근 · GPS 지오펜싱",
      "근무표/교대 · 일용직 전용 관리",
      "연차 자동관리 · 근태 통계",
    ],
  },
  {
    title: "산업안전 관리",
    items: [
      "위험성평가 · 안전점검",
      "TBM(작업 전 안전회의)",
      "아차사고 · 증빙 자동 보존",
    ],
  },
  {
    title: "AI 안전 지원",
    items: [
      "위험요인·개선안 AI 추천",
      "TBM 자료 초안 자동 작성",
      "담당자 검토·승인 후 확정",
    ],
  },
];

const painEfficiency = [
  "수기·엑셀 근태 정산에 드는 관리 공수",
  "종이 점검표·TBM 일지 작성과 보관 부담",
  "인사 부서와 안전 부서 간 데이터 단절",
  "일용직 근태를 따로 관리하던 이중 업무",
];
const painRisk = [
  "근로기준법 위반(연차 미부여·사용촉진 누락) 과태료 리스크",
  "중대재해처벌법 대응 시 '안전관리 증빙 부재' 리스크",
  "원거리·대리 출퇴근으로 인한 인건비 누수",
  "휴대폰 없는 일용 인력의 관리 사각지대",
];

const featurePreview = [
  {
    icon: "clock",
    title: "출퇴근·근태",
    desc: "모바일 출퇴근과 GPS 지오펜싱, 근무표·교대, 월마감까지 관리합니다.",
  },
  {
    icon: "users",
    title: "일용직 관리",
    desc: "휴대폰 없는 일용직도 임시계정+QR로 누락 없이 관리합니다.",
  },
  {
    icon: "calendar",
    title: "연차·통계",
    desc: "연차 자동관리·사용촉진 자동화와 근태 통계 자동 집계.",
  },
  {
    icon: "alert",
    title: "위험성평가",
    desc: "위험요인 발굴·평가·개선 이력을 체계적으로 보존합니다.",
  },
  {
    icon: "clipboard",
    title: "TBM·안전점검",
    desc: "TBM 참석 기록과 안전점검 체크리스트를 증빙으로 남깁니다.",
  },
  {
    icon: "cpu",
    title: "AI 안전 지원",
    desc: "위험요인·개선안과 TBM 자료 초안을 AI가 제안합니다.",
  },
];

// 도입 전 → 후 비교
const compare = [
  {
    area: "근태 정산",
    before: "수기·엑셀로 매월 수작업 정산",
    after: "출퇴근 자동 기록·집계로 정산 공수 최소화",
  },
  {
    area: "점검·일지",
    before: "종이 점검표·TBM 일지 작성·보관",
    after: "모바일 기록 + 이력 자동 보존",
  },
  {
    area: "부서 데이터",
    before: "인사 부서와 안전 부서 데이터 단절",
    after: "근태·인사 + 안전을 한 시스템에서 통합",
  },
  {
    area: "일용직",
    before: "정규직과 따로 관리하는 이중 업무",
    after: "이름·연락처만으로 임시계정+QR 일원화",
  },
  {
    area: "증빙",
    before: "사고 시 안전관리 증빙 부재",
    after: "QR·계정 기반 시계열 증빙 자동 축적",
  },
  {
    area: "법규 대응",
    before: "연차·안전 의무를 수동으로 챙김",
    after: "연차 사용촉진·안전 이력 자동 대응",
  },
];

// 업종별 소구
const industries = [
  {
    icon: "factory",
    title: "제조",
    desc: "교대·시프트 근무와 라인별 인력 배치, 작업 전 TBM·위험성평가를 한 흐름으로 관리.",
  },
  {
    icon: "truck",
    title: "물류·유통",
    desc: "다현장·외근 GPS 출퇴근, 성수기 일용직 QR 투입, 상하차 현장 안전점검까지.",
  },
  {
    icon: "building",
    title: "건설현장",
    desc: "현장별 출역 관리와 위험성평가·TBM·아차사고 증빙으로 중대재해처벌법에 대응.",
  },
  {
    icon: "flask",
    title: "화학물질 취급",
    desc: "취급 현장 특화 안전관리(2차 로드맵)와 점검·교육 이력 보존으로 업종 규제 대응.",
  },
];
</script>

<style scoped>
.intro-page {
  /* 관리자 셸의 html/body/#app{height:100%} 제약 하에서 intro 페이지를 자체 스크롤 컨테이너로 만든다 */
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

/* 버튼 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: var(--btn-radius-lg);
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
}
.btn--lg {
  height: 50px;
  padding: 0 26px;
  font-size: 16.5px;
}
.btn--ghost {
  font-size: 16px;
}
.btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.btn--primary:hover {
  background: var(--color-primary-hover);
}
.btn--ghost {
  background: transparent;
  color: var(--color-text-strong);
  border-color: var(--color-border-strong);
  height: 40px;
  padding: 0 18px;
}
.btn--ghost:hover {
  border-color: var(--color-text-muted);
}
.btn--white {
  background: #fff;
  color: var(--color-primary-pressed);
}
.btn--white:hover {
  background: rgba(255, 255, 255, 0.9);
}

/* 히어로 */
.hero {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid var(--color-border);
}
.hero__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  background:
    radial-gradient(
      820px 480px at 88% -8%,
      rgba(22, 163, 74, 0.2),
      transparent 60%
    ),
    radial-gradient(
      560px 400px at 2% 112%,
      rgba(217, 119, 6, 0.12),
      transparent 60%
    ),
    linear-gradient(180deg, #f3fbf5 0%, var(--color-surface) 72%);
}
.hero__bg::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image: radial-gradient(
    rgba(17, 24, 39, 0.05) 1px,
    transparent 1px
  );
  background-size: 22px 22px;
  -webkit-mask-image: linear-gradient(180deg, #000, transparent 70%);
  mask-image: linear-gradient(180deg, #000, transparent 70%);
}
.hero__inner {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto;
  padding: 84px var(--header-padding-x) 92px;
  display: grid;
  grid-template-columns: 1fr 1.08fr;
  gap: 44px;
  align-items: center;
}
.hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #dcfce7;
  color: var(--color-primary-pressed);
  font-weight: 800;
  font-size: 14.5px;
  padding: 8px 16px;
  border-radius: 999px;
}
.hero__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-warning-text);
}
.hero__title {
  color: var(--color-text-strong);
  font-size: 48px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.03em;
  margin: 20px 0 18px;
}
.hero__nowrap {
  white-space: nowrap;
}
.hero__accent {
  background: linear-gradient(
    110deg,
    var(--color-primary),
    var(--color-primary-hover)
  );
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero__desc {
  color: var(--color-text-muted);
  font-size: 19px;
  line-height: 1.65;
  margin: 0 0 30px;
  max-width: 540px;
}
.hero__desc strong {
  color: var(--color-primary-pressed);
  font-weight: 800;
}
.hero__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.btn--text {
  background: none;
  border: none;
  height: auto;
  padding: 0 6px;
  color: var(--color-primary);
  font-weight: 700;
  font-size: 15.5px;
}
.btn--text:hover {
  color: var(--color-primary-hover);
}
.hero__chips {
  list-style: none;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 0;
  margin: 26px 0 0;
}
.hero__chips li {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-primary-pressed);
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 7px 14px;
  border-radius: 999px;
}
/* 히어로 비주얼 */
.hero__visual {
  position: relative;
  display: flex;
  justify-content: center;
  padding: 0 24px 14px 0;
}
.hero__glow {
  position: absolute;
  inset: 8% 4% 4% 4%;
  z-index: 0;
  background: linear-gradient(
    135deg,
    rgba(22, 163, 74, 0.35),
    rgba(22, 163, 74, 0)
  );
  filter: blur(46px);
  border-radius: 50%;
}
.hero__shot {
  position: relative;
  z-index: 1;
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--color-border);
  box-shadow: 0 18px 50px rgba(16, 24, 40, 0.18);
}
.hero__bar {
  height: 30px;
  background: #f1f3f5;
  border-bottom: 1px solid #e9edf1;
  display: flex;
  align-items: center;
  gap: 7px;
  padding-left: 14px;
}
.hero__bar i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cdd3da;
}
.hero__dash {
  display: block;
  width: 100%;
  height: auto;
}
.hero__phone {
  position: absolute;
  z-index: 3;
  right: -6px;
  bottom: -16px;
  width: 23%;
  max-width: 148px;
  height: auto;
  filter: drop-shadow(0 18px 30px rgba(16, 24, 40, 0.28));
}

/* 섹션 공통 */
.section {
  padding: 76px var(--header-padding-x);
}
.section--alt {
  background: #f5fbf7;
}
.section__inner {
  max-width: 1080px;
  margin: 0 auto;
}
.section__kicker {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.12em;
  text-align: center;
  margin: 0 0 6px;
}
.section__kicker--danger {
  color: var(--color-danger);
}
.section__title {
  color: var(--color-text-strong);
  font-size: 33px;
  font-weight: 800;
  letter-spacing: -0.02em;
  text-align: center;
  margin: 0 0 10px;
}
.section__title--lg {
  font-size: 42px;
}
.br-mobile {
  display: none;
}
.section__lead {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 18px;
  margin: 0 0 36px;
}
.section__more {
  text-align: center;
  margin-top: 28px;
}

/* 카드 그리드 */
.cards {
  list-style: none;
  margin: 28px 0 0;
  padding: 0;
  display: grid;
  gap: 18px;
}
.cards--4 {
  grid-template-columns: repeat(4, 1fr);
}
.cards--3 {
  grid-template-columns: repeat(3, 1fr);
}
.card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: var(--card-padding);
}
.card__title {
  color: var(--color-text-strong);
  font-size: 19px;
  font-weight: 700;
  margin: 0 0 7px;
}
.card__desc {
  color: var(--color-text-muted);
  font-size: 15.5px;
  margin: 0;
}

.value {
  border-top: 4px solid var(--color-primary);
}
.value__num {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-warning-bg);
  color: var(--color-primary-pressed);
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.pillar__list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
}
.pillar__list li {
  position: relative;
  padding: 5px 0 5px 15px;
  color: var(--color-text-muted);
  font-size: 15.5px;
}
.pillar__list li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: var(--color-primary);
  font-weight: 800;
}
.strip {
  margin: 32px auto 0;
  max-width: 880px;
  text-align: center;
  font-size: 23px;
  font-weight: 800;
  line-height: 1.45;
  color: var(--color-primary-pressed);
}

/* 문제 */
.problem {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-top: 28px;
}
.problem__col {
  border-top: 4px solid var(--color-text-muted);
}
.problem__col--risk {
  border-top-color: var(--color-danger);
}
.problem__col {
  padding: 28px 30px;
}
.problem__head {
  color: var(--color-text-strong);
  font-size: 23px;
  font-weight: 800;
  margin: 0 0 12px;
}
.problem__list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.problem__list li {
  position: relative;
  padding: 13px 0 13px 28px;
  font-size: 18px;
  color: var(--color-text);
  border-top: 1px solid var(--color-border);
}
.problem__list li:first-child {
  border-top: 0;
}
.problem__list li::before {
  content: "✕";
  position: absolute;
  left: 0;
  top: 8px;
  color: var(--color-text-muted);
  font-weight: 800;
}
.problem__list--risk li::before {
  color: var(--color-danger);
}

/* 기능/업종 아이콘 색상칩 */
.feature__icon,
.industry__icon {
  width: 54px;
  height: 54px;
  border-radius: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-muted);
}
.feature__icon :deep(svg),
.industry__icon :deep(svg) {
  width: 27px;
  height: 27px;
}

/* AI 배너 */
.aiband {
  background: #eef9f1;
  color: var(--color-text);
  border-top: 1px solid #cdeed7;
  border-bottom: 1px solid #cdeed7;
}
.aiband__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 56px var(--header-padding-x);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}
.aiband__kicker {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.12em;
  margin: 0 0 8px;
}
.aiband__title {
  color: var(--color-text-strong);
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 10px;
}
.aiband__desc {
  color: var(--color-text-muted);
  font-size: 16.5px;
  margin: 0;
  max-width: 680px;
}
.aiband .btn--ghost {
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.aiband .btn--ghost:hover {
  background: var(--color-primary);
  color: #fff;
}

/* CTA */
.cta {
  padding: 84px var(--header-padding-x);
  text-align: center;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  color: var(--color-text);
}
.cta__inner {
  max-width: 720px;
  margin: 0 auto;
}
.cta__title {
  color: var(--color-text-strong);
  font-size: 34px;
  font-weight: 800;
  margin: 0 0 12px;
}
.cta__desc {
  color: var(--color-text-muted);
  font-size: 17px;
  margin: 0 0 26px;
}
.cta__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
.btn--white-outline {
  background: transparent;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.7);
}
.btn--white-outline:hover {
  background: rgba(255, 255, 255, 0.12);
}

/* 도입 전 → 후 비교 */
.compare {
  margin-top: 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  overflow: hidden;
  background: var(--color-surface);
  box-shadow: var(--card-shadow);
}
.compare__head,
.compare__row {
  display: grid;
  grid-template-columns: 0.8fr 1.1fr 1.1fr;
}
.compare__head {
  background: var(--color-bg);
  font-weight: 800;
  font-size: 16.5px;
  color: var(--color-text-strong);
}
.compare__head span {
  padding: 12px 16px;
}
.compare__h-after {
  color: var(--color-primary-pressed);
}
.compare__row {
  border-top: 1px solid var(--color-border);
  font-size: 18px;
}
.compare__row span {
  padding: 13px 16px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.compare__area {
  font-weight: 700;
  color: var(--color-text-strong);
}
.compare__before {
  color: var(--color-text-muted);
}
.compare__after {
  color: var(--color-text);
}
.compare__before i,
.compare__after i {
  font-style: normal;
  font-weight: 800;
  flex: 0 0 auto;
}
.compare__before i {
  color: var(--color-danger);
}
.compare__after i {
  color: var(--color-primary);
}
.compare__h-label {
  display: none;
}

/* 기능 미리보기 + 목업 */
.preview {
  max-width: 700px;
  margin: 12px auto 44px;
}
.preview__cards {
  margin-top: 0;
}
/* 미리보기 목업: 모바일을 작게, 웹 우측 하단 코너에 살짝 겹치게(텍스트 안 가리도록) */
.preview :deep(.mock--overlap) {
  padding: 0 6px 16px 0;
}
.preview :deep(.mock--overlap .mock__app) {
  width: 176px;
  right: 8px;
  bottom: -4px;
}

/* 업종 아이콘은 .feature__icon 규칙과 색상칩(tint) 공유 */

/* 반응형 */
@media (max-width: 960px) {
  .hero__inner {
    grid-template-columns: 1fr;
    gap: 36px;
  }
  .hero__visual {
    order: -1;
  }
  .hero__title {
    font-size: 48px;
  }
  .cards--4 {
    grid-template-columns: repeat(2, 1fr);
  }
  .preview {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 860px) {
  .cards--3,
  .cards--4 {
    grid-template-columns: 1fr;
  }
  .cards--prev {
    grid-template-columns: 1fr 1fr;
  }
  .problem {
    grid-template-columns: 1fr;
  }
  .hero__title {
    font-size: 37px;
  }
  .hero__phone {
    width: 27%;
    max-width: 124px;
    right: 0;
  }
  /* 비교표: 모바일에서 행 단위로 스택 */
  .compare__head {
    display: none;
  }
  .compare__row {
    grid-template-columns: 1fr;
  }
  .compare__row span {
    padding: 8px 16px;
  }
  .compare__area {
    padding-top: 14px;
    font-size: 14px;
  }
  .compare__row {
    padding-bottom: 8px;
  }
}
</style>
