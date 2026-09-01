<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="page-head">
      <div class="page-head__inner">
        <p class="page-head__kicker">SAFETY</p>
        <h1 class="page-head__title">
          안전관리 - 안전활동이 그대로 기록이 됩니다
        </h1>
      </div>
    </section>

    <IntroAnchorTabs :tabs="tabs" />

    <IntroFeatureSection
      id="tbm"
      axis="safety"
      heading="TBM 개설부터 수료 서명까지 한 흐름으로"
      :bullets="tbmBullets"
    />

    <IntroFeatureSection
      id="risk"
      axis="safety"
      heading="가능성×중대성 매트릭스로, 위험성 평가부터 개선까지 이력으로"
      :bullets="riskBullets"
      reverse
    />

    <IntroFeatureSection
      id="inspection"
      axis="safety"
      heading="체크리스트 점검, 조작할 수 없는 기록으로"
      :bullets="inspectionBullets"
    />

    <IntroFeatureSection
      id="accident"
      axis="safety"
      heading="사고 1건을 열면, 관련 기록이 한 화면에 모입니다"
      :bullets="accidentBullets"
      reverse
    >
      <template #footnote>
        <p class="feature-note feature-note--strong">
          사고가 난 뒤 만드는 서류가 아니라, 평소 운영이 그대로 증빙이 됩니다.
        </p>
      </template>
    </IntroFeatureSection>

    <IntroFeatureSection
      id="subcontract"
      axis="safety"
      heading="원청–협력사 n차 구조를 시스템이 그대로 수용합니다"
      :bullets="subcontractBullets"
      :images="subcontractImages"
    >
      <template #footnote>
        <p class="feature-note">
          협력사 안전관리 실태가 "요청하면 오는 보고서"가 아니라 "시스템에
          쌓이는 데이터"가 됩니다.
        </p>
      </template>
    </IntroFeatureSection>

    <section id="ai" class="ai-section">
      <div class="ai-section__inner">
        <h3 class="ai-section__heading">문서 작업은 AI가, 판단은 담당자가</h3>
        <div class="grid grid--2">
          <IntroIconCard
            v-for="c in aiCards"
            :key="c.title"
            :title="c.title"
            :desc="c.desc"
          >
            <template #icon>{{ c.icon }}</template>
          </IntroIconCard>
        </div>
        <p class="feature-note feature-note--strong ai-section__footnote">
          AI는 판단을 대신하지 않습니다. 판단할 재료를 먼저 준비해 둘 뿐입니다.
        </p>
      </div>
    </section>

    <!-- 하단 크로스링크 밴드와 CTA 밴드는 2026-08-31 사용자 지시로 제거 -->

    <IntroFooter />
    <IntroFloatingCta />
  </div>
</template>

<script setup>
import IntroHeader from "@/components/intro/IntroHeader.vue";
import IntroFooter from "@/components/intro/IntroFooter.vue";
import IntroFloatingCta from "@/components/intro/IntroFloatingCta.vue";
import IntroAnchorTabs from "@/components/intro/IntroAnchorTabs.vue";
import IntroFeatureSection from "@/components/intro/IntroFeatureSection.vue";
import IntroIconCard from "@/components/intro/IntroIconCard.vue";
import subcontract1 from "@/assets/intro/subcontract-1.png";
import subcontract2 from "@/assets/intro/subcontract-2.png";
import subcontract3 from "@/assets/intro/subcontract-3.png";

const tabs = [
  { id: "tbm", label: "TBM" },
  { id: "risk", label: "위험성평가" },
  { id: "inspection", label: "순회점검" },
  { id: "accident", label: "사고관리" },
  { id: "subcontract", label: "하도급 연동" },
  { id: "ai", label: "AI 안전 지원" },
];

const tbmBullets = [
  "교육 개설 → 입실(비밀번호·QR·GPS 검증) → 교육 → 수료 서명",
  "교육자료(이미지·영상·유튜브·PDF) 관리",
  "협력사 근로자도 원청 세션에 입실 가능",
  "교육자료 기반 TBM 교육내용 AI 자동 생성 가능",
];
const riskBullets = [
  "가능성(5)×중대성(4) 정량 평가",
  "초기평가 → 개선 후 재평가 2단계 이력",
  "지속개선대상 지정 관리 기능",
  "이미지·텍스트 기반 AI 유해요인 - 개선안 자동 도출",
  "아차사고 → 위험성평가 흐름 제공",
];
const inspectionBullets = [
  "모바일 체크리스트(양호/불량) + 사진 첨부",
  "점검 수행일 서버 시간 강제 - 소급 입력 차단",
  "불량결과 별도 관리 기능 제공",
  '문항 변경 이력 보존 - "그 시점의 점검 의무"까지 증빙',
];
const accidentBullets = [
  "사고 등록 시 근태(당일)·순회점검(1주)·위험성평가(3개월)·아차사고(3개월)·TBM(당일) 기록을 확정 사본으로 일괄 구성",
  "재해등급·처리상태 관리",
  "정규직·일용직 모두 재해자 등록",
  "사고 발생 시 재해 등급 별 조치사항 가이드", 
];
const subcontractBullets = [
  "원청의 사업장 정보를 협력사에 상속하여 협력사는 세팅 없이 시작",
  "순회점검 결과는 원청 화면에 즉시 반영",
  "2차·3차···n차 협력사 수용 가능 구조 제공",
  "연동 해지 시 협력사 데이터는 협력사 소유로 독립",
];
const subcontractImages = [
  {
    src: subcontract1,
    alt: "하도급 데이터 연동요청 화면 대표",
    variant: "browser",
  },
  {
    src: subcontract2,
    alt: "하도급 데이터 연동요청 보조 화면 1",
    variant: "browser",
  },
  {
    src: subcontract3,
    alt: "하도급 데이터 연동요청 보조 화면 2",
    variant: "browser",
  },
];

const aiCards = [
  {
    icon: "🧾",
    title: "TBM 교육안 자동 생성",
    desc: "교육자료 + 해당 현장 위험성평가를 통합해 AI가 교육안 초안 자동 생성, AI 교육안 초안 생성 시 교육 분량 지정(800~5,000자) 기능 제공. 교육자료로 선택한 자료 혹은 위험성평가 항목에 따라 다른 교육안 생성",
  },
  {
    icon: "🔍",
    title: "위험성평가 유해요인·개선안 도출",
    desc: "이미지·텍스트 기반 AI 분석을 통해 유해요인·개선안 초안 제시, RAG 방식의 정보 검색을 통해 정확한 정보 출처 제공.",
  },
];
</script>

<style scoped>
.intro-page {
  background: var(--color-surface);
  color: var(--color-text);
}
.page-head {
  padding: 48px var(--header-padding-x) 32px;
  text-align: center;
  border-bottom: 1px solid var(--color-border);
}
.page-head__inner {
  max-width: 760px;
  margin: 0 auto;
}
.page-head__kicker {
  font-size: var(--intro-text-xs);
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--color-primary);
  margin: 0 0 10px;
}
.page-head__title {
  font-size: var(--intro-text-lg);
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0;
  line-height: var(--intro-lh-tight);
}
.feature-note {
  font-size: var(--intro-text-xs);
  color: var(--color-text-muted);
  margin: 6px 0 0;
}
.feature-note--strong {
  color: var(--color-text-strong);
  font-weight: 700;
}
.ai-section {
  padding: 64px var(--header-padding-x);
  background: var(--color-bg);
  scroll-margin-top: 116px;
}
.ai-section__inner {
  max-width: 1160px;
  margin: 0 auto;
  text-align: center;
}
.ai-section__heading {
  font-size: var(--intro-text-md);
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 28px;
}
.grid--2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  text-align: left;
}
.ai-section__footnote {
  margin-top: 24px;
}

@media (max-width: 767px) {
  .grid--2 {
    grid-template-columns: 1fr;
  }
}
</style>
