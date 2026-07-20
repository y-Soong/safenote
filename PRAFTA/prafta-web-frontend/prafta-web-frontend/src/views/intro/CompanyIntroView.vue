<template>
  <!--
    회사소개 랜딩 (루트 도메인 = '/')
    - 일반 브라우저/검색엔진에서 진입하는 공개 페이지.
    - SafeNote 서비스(관리자 웹)는 '/safenote' 경로로 분리되어 있으며,
      이 페이지의 CTA 버튼에서 해당 경로로 이동한다.
    - 도메인 연결 후에는 prafta.com( 이 페이지 ) / prafta.com/safenote( 서비스 )로 매핑된다.
    - 초안: 카피/이미지/섹션 구성은 추후 마케팅 확정에 맞춰 교체.
  -->
  <div class="intro">
    <!-- 상단 네비게이션 -->
    <header class="intro-nav">
      <div class="intro-nav__inner">
        <a class="intro-brand" href="#top">
          <span class="intro-brand__mark">PRAFTA</span>
          <span class="intro-brand__sub">SafeNote</span>
        </a>
        <nav class="intro-nav__links">
          <a href="#features">기능</a>
          <a href="#how">도입 흐름</a>
          <a href="#contact">문의</a>
        </nav>
        <button
          type="button"
          class="intro-btn intro-btn--primary intro-btn--sm"
          @click="goService"
        >
          서비스 로그인
        </button>
      </div>
    </header>

    <!-- 히어로 -->
    <section id="top" class="intro-hero">
      <div class="intro-hero__inner">
        <p class="intro-hero__eyebrow">현장 안전·근태 통합 관리</p>
        <h1 class="intro-hero__title">
          더 안전한 현장을 위한<br />
          가장 간단한 방법, <span class="accent">SafeNote</span>
        </h1>
        <p class="intro-hero__desc">
          출퇴근부터 TBM, 위험성평가, 아차사고 관리까지.<br />
          현장에서 일어나는 모든 안전·근태 기록을 하나의 흐름으로 연결합니다.
        </p>
        <div class="intro-hero__actions">
          <button
            type="button"
            class="intro-btn intro-btn--primary intro-btn--lg"
            @click="goService"
          >
            SafeNote 시작하기
          </button>
          <a class="intro-btn intro-btn--ghost intro-btn--lg" href="#features"
            >기능 둘러보기</a
          >
        </div>
      </div>
    </section>

    <!-- 주요 기능 -->
    <section id="features" class="intro-section">
      <div class="intro-section__inner">
        <h2 class="intro-section__title">핵심 기능</h2>
        <p class="intro-section__lead">현장 관리에 필요한 기능을 한 곳에서.</p>
        <ul class="intro-features">
          <li v-for="f in features" :key="f.title" class="intro-feature">
            <div class="intro-feature__icon" aria-hidden="true">
              {{ f.icon }}
            </div>
            <h3 class="intro-feature__title">{{ f.title }}</h3>
            <p class="intro-feature__desc">{{ f.desc }}</p>
          </li>
        </ul>
      </div>
    </section>

    <!-- 도입 흐름 -->
    <section id="how" class="intro-section intro-section--alt">
      <div class="intro-section__inner">
        <h2 class="intro-section__title">도입은 이렇게</h2>
        <ol class="intro-steps">
          <li v-for="(s, i) in steps" :key="s.title" class="intro-step">
            <span class="intro-step__no">{{ i + 1 }}</span>
            <div>
              <h3 class="intro-step__title">{{ s.title }}</h3>
              <p class="intro-step__desc">{{ s.desc }}</p>
            </div>
          </li>
        </ol>
      </div>
    </section>

    <!-- CTA -->
    <section id="contact" class="intro-cta">
      <div class="intro-cta__inner">
        <h2 class="intro-cta__title">현장 안전관리, 지금 시작하세요</h2>
        <p class="intro-cta__desc">도입 문의 및 데모는 아래로 연락 주세요.</p>
        <div class="intro-cta__actions">
          <button
            type="button"
            class="intro-btn intro-btn--primary intro-btn--lg"
            @click="goService"
          >
            서비스 바로가기
          </button>
          <a
            class="intro-btn intro-btn--ghost intro-btn--lg"
            href="mailto:contact@prafta.com"
          >
            contact@prafta.com
          </a>
        </div>
      </div>
    </section>

    <!-- 푸터 -->
    <footer class="intro-footer">
      <div class="intro-footer__inner">
        <span>© {{ year }} PRAFTA. All rights reserved.</span>
        <span class="intro-footer__links">
          <router-link to="/privacy">개인정보 처리방침</router-link>
          <router-link to="/account-deletion">계정 삭제 안내</router-link>
          <a href="#top">SafeNote</a>
        </span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

// SafeNote 서비스(관리자 웹) 진입 경로. 라우터 base 변경 시 이 상수만 갱신한다.
const SERVICE_PATH = "/safenote";

const router = useRouter();
const goService = () => router.push(SERVICE_PATH);

const year = computed(() => new Date().getFullYear());

// 초안 콘텐츠 — 실제 카피는 마케팅 확정 후 교체.
const features = [
  {
    icon: "🕒",
    title: "출퇴근·근태",
    desc: "GPS 기반 출퇴근 기록과 근무계획, 월마감까지 한 번에 관리합니다.",
  },
  {
    icon: "🦺",
    title: "TBM·안전점검",
    desc: "현장 TBM과 안전점검을 모바일에서 즉시 기록하고 공유합니다.",
  },
  {
    icon: "⚠️",
    title: "위험성평가",
    desc: "위험요인 발굴부터 개선 조치까지 평가 이력을 체계적으로 남깁니다.",
  },
  {
    icon: "🔔",
    title: "아차사고·사건",
    desc: "아차사고를 신속히 접수하고 임시조치·후속 처리를 추적합니다.",
  },
  {
    icon: "📝",
    title: "연차·결재",
    desc: "연차 신청과 결재 라인을 자동화해 관리 부담을 줄입니다.",
  },
  {
    icon: "📊",
    title: "통합 현황",
    desc: "현장별 안전·근태 데이터를 한 화면에서 확인합니다.",
  },
];

const steps = [
  {
    title: "문의 및 상담",
    desc: "현장 규모와 운영 방식을 알려주시면 맞춤 구성을 제안합니다.",
  },
  {
    title: "사업장·사용자 등록",
    desc: "관리자 웹에서 사업장과 사용자를 등록하고 권한을 설정합니다.",
  },
  {
    title: "현장 운영 시작",
    desc: "모바일 앱으로 출퇴근·안전관리를 바로 시작합니다.",
  },
];
</script>

<style scoped>
.intro {
  min-height: 100vh;
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.6;
}

/* 네비게이션 */
.intro-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: saturate(180%) blur(8px);
  border-bottom: 1px solid var(--color-border);
}
.intro-nav__inner {
  max-width: 1080px;
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 var(--header-padding-x);
  display: flex;
  align-items: center;
  gap: 24px;
}
.intro-brand {
  display: flex;
  align-items: baseline;
  gap: 8px;
  text-decoration: none;
}
.intro-brand__mark {
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-text-strong);
}
.intro-brand__sub {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
}
.intro-nav__links {
  margin-left: auto;
  display: flex;
  gap: 22px;
}
.intro-nav__links a {
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 14px;
}
.intro-nav__links a:hover {
  color: var(--color-text-strong);
}

/* 공통 버튼 */
.intro-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: var(--btn-radius-lg);
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
}
.intro-btn--sm {
  height: var(--btn-height);
  padding: 0 var(--btn-padding);
  font-size: var(--btn-font);
}
.intro-btn--lg {
  height: 44px;
  padding: 0 22px;
  font-size: 15px;
}
.intro-btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.intro-btn--primary:hover {
  background: var(--color-primary-hover);
}
.intro-btn--ghost {
  background: transparent;
  color: var(--color-text-strong);
  border-color: var(--color-border-strong);
}
.intro-btn--ghost:hover {
  border-color: var(--color-text-muted);
}

/* 히어로 */
.intro-hero {
  background: linear-gradient(180deg, var(--color-bg), var(--color-surface));
  border-bottom: 1px solid var(--color-border);
}
.intro-hero__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 88px var(--header-padding-x) 96px;
  text-align: center;
}
.intro-hero__eyebrow {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 14px;
}
.intro-hero__title {
  color: var(--color-text-strong);
  font-size: 40px;
  line-height: 1.25;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0 0 18px;
}
.intro-hero__title .accent {
  color: var(--color-primary);
}
.intro-hero__desc {
  color: var(--color-text-muted);
  font-size: 17px;
  margin: 0 auto 32px;
  max-width: 560px;
}
.intro-hero__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 섹션 */
.intro-section {
  padding: 80px var(--header-padding-x);
}
.intro-section--alt {
  background: var(--color-bg);
}
.intro-section__inner {
  max-width: 1080px;
  margin: 0 auto;
}
.intro-section__title {
  color: var(--color-text-strong);
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  text-align: center;
  margin: 0 0 8px;
}
.intro-section__lead {
  text-align: center;
  color: var(--color-text-muted);
  margin: 0 0 40px;
}

/* 기능 카드 */
.intro-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.intro-feature {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: var(--card-padding);
}
.intro-feature__icon {
  font-size: 28px;
  margin-bottom: 12px;
}
.intro-feature__title {
  color: var(--color-text-strong);
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 6px;
}
.intro-feature__desc {
  color: var(--color-text-muted);
  font-size: 14px;
  margin: 0;
}

/* 도입 흐름 */
.intro-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.intro-step {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
}
.intro-step__no {
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
.intro-step__title {
  color: var(--color-text-strong);
  font-size: 16px;
  font-weight: 700;
  margin: 2px 0 4px;
}
.intro-step__desc {
  color: var(--color-text-muted);
  font-size: 14px;
  margin: 0;
}

/* CTA */
.intro-cta {
  padding: 88px var(--header-padding-x);
  text-align: center;
}
.intro-cta__inner {
  max-width: 720px;
  margin: 0 auto;
}
.intro-cta__title {
  color: var(--color-text-strong);
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 10px;
}
.intro-cta__desc {
  color: var(--color-text-muted);
  margin: 0 0 28px;
}
.intro-cta__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 푸터 */
.intro-footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}
.intro-footer__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px var(--header-padding-x);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-text-muted);
  font-size: 13px;
}
.intro-footer__links {
  display: inline-flex;
  gap: 18px;
}
.intro-footer__links a {
  color: var(--color-text-muted);
  text-decoration: none;
}
.intro-footer__links a:hover {
  color: var(--color-text-strong);
}

/* 반응형 */
@media (max-width: 860px) {
  .intro-features,
  .intro-steps {
    grid-template-columns: 1fr;
  }
  .intro-hero__title {
    font-size: 30px;
  }
  .intro-nav__links {
    display: none;
  }
}
</style>
