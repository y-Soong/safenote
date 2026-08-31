<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="page-head">
      <div class="page-head__inner">
        <p class="page-head__kicker">SAFETY</p>
        <h1 class="page-head__title">안전관리 — 안전활동이 그대로 기록이 됩니다</h1>
      </div>
    </section>

    <IntroAnchorTabs :tabs="tabs" />

    <IntroFeatureSection
      id="tbm"
      axis="safety"
      label="안전관리"
      heading="매일 아침 TBM, 개설부터 수료 서명까지 한 흐름으로"
      :bullets="tbmBullets"
    />

    <IntroFeatureSection
      id="risk"
      axis="safety"
      label="안전관리"
      heading="가능성×중대성 매트릭스로, 평가부터 개선까지 이력으로"
      :bullets="riskBullets"
      reverse
    />

    <IntroFeatureSection
      id="inspection"
      axis="safety"
      label="안전관리"
      heading="체크리스트 점검, 조작할 수 없는 기록으로"
      :bullets="inspectionBullets"
    />

    <IntroFeatureSection
      id="accident"
      axis="safety"
      label="안전관리"
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
      label="안전관리"
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
        <p class="ai-section__label">안전관리</p>
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

    <section class="cross-link">
      <div class="cross-link__inner">
        <router-link to="/attendance" class="feature-link"
          >모든 안전활동의 기반에는 근태가 있습니다. 누가, 언제, 어느 현장에
          있었는가 — 근태관리 보기 →</router-link
        >
      </div>
    </section>

    <IntroCtaBand heading="안전관리, PRAFTA로 시작해 보세요." />

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
import IntroCtaBand from "@/components/intro/IntroCtaBand.vue";
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
  "세션 개설 → 입실(비밀번호·QR·GPS 검증) → 교육 → 수료 서명",
  "교육자료(이미지·영상·유튜브·PDF) 관리",
  "참석 기록은 소속회사별로 귀속",
  "협력사 근로자도 원청 세션에 입실 가능",
];
const riskBullets = [
  "가능성(5)×중대성(4) 정량 평가, 색상 등급",
  "초기평가 → 개선 후 재평가 2단계 이력",
  "근로자는 앱에서 제출, 관리자는 웹에서 검토·개선안 작성",
  "완료된 아차사고를 참조로 연결",
];
const inspectionBullets = [
  "모바일 체크리스트(양호/불량) + 사진 첨부",
  "점검 수행일 서버 시간 강제 — 소급 입력 차단",
  "불량 원천값 불변, 조치는 별도 기록",
  "문항 변경 이력 보존 — \"그 시점의 점검 의무\"까지 증빙",
];
const accidentBullets = [
  "사고 등록 시 근태(당일)·순회점검(1주)·위험성평가(3개월)·아차사고(3개월)·TBM(당일) 기록을 확정 사본으로 일괄 구성",
  "재해등급·처리상태 관리",
  "정규직·일용직 모두 재해자 등록",
];
const subcontractBullets = [
  "미러 사업장 — 원청 기준을 협력사에 복제·동기화, 협력사는 세팅 없이 시작",
  "근태·위험성평가·아차사고는 \"요청→승인→스냅샷\" 공유",
  "순회점검 결과는 원청 화면에 즉시 반영",
  "2차·3차 협력사까지 수용",
  "연동 해지 시 협력사 데이터는 협력사 소유로 독립(락인 없음)",
];
const subcontractImages = [
  { src: subcontract1, alt: "하도급 데이터 연동요청 화면 대표", variant: "browser" },
  { src: subcontract2, alt: "하도급 데이터 연동요청 보조 화면 1", variant: "browser" },
  { src: subcontract3, alt: "하도급 데이터 연동요청 보조 화면 2", variant: "browser" },
];

const aiCards = [
  { icon: "🧾", title: "TBM 교육안 자동 생성", desc: "교육자료 AI 분석 + 해당 현장 위험성평가를 통합해 교육안 초안 생성, 분량 지정(800~5,000자). 관리자 검토·확정 후에만 사용." },
  { icon: "🔍", title: "위험성평가 유해요인·개선안 도출", desc: "현장 사진 기반으로 AI가 유해요인·개선안 초안 제시, 관리자 확정이 항상 우선." },
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
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--color-primary);
  margin: 0 0 10px;
}
.page-head__title {
  font-size: clamp(1.5rem, 3vw, 2.2rem);
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0;
  line-height: 1.4;
}
.feature-note {
  font-size: 13px;
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
.ai-section__label {
  color: var(--color-primary);
  font-weight: 700;
  font-size: 13px;
  margin: 0 0 10px;
}
.ai-section__heading {
  font-size: 1.5rem;
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
.feature-link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}
.cross-link {
  padding: 32px var(--header-padding-x);
  text-align: center;
}
.cross-link__inner {
  max-width: 720px;
  margin: 0 auto;
}

@media (max-width: 767px) {
  .grid--2 {
    grid-template-columns: 1fr;
  }
}
</style>
