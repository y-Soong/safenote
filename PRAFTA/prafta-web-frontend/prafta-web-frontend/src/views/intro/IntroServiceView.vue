<template>
  <!--
    프라프타 홈페이지 — 서비스 소개 (/service, PRAFTA)
    출처: PRAFTA 서비스소개서 / 홈페이지 IA 명세 §3. ★ = 차별화 기능.
  -->
  <div class="intro-page">
    <IntroHeader />

    <!-- 서비스 히어로 -->
    <section class="phead">
      <div class="phead__inner">
        <p class="phead__kicker">PRAFTA</p>
        <h1 class="phead__title">
          PRAFTA — 근태·인사 + 산업안전 통합 플랫폼
        </h1>
        <p class="phead__slogan">현장의 사람과 안전을, 하나의 서비스로</p>
      </div>
    </section>

    <!-- 개요 3축 -->
    <section class="section">
      <div class="section__inner">
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

    <!-- 핵심기능 1: 근태·인사 -->
    <section class="section section--alt">
      <div class="section__inner">
        <div class="feathead">
          <span class="feathead__badge">1</span>
          <div>
            <p class="feathead__lbl">핵심 기능</p>
            <h2 class="feathead__nm">근태 · 인사</h2>
          </div>
        </div>
        <!-- TODO: MockupFrame 에 videoSrc/appVideoSrc 전달 시 자동재생 영상으로 교체 -->
        <MockupFrame label="근태·인사 화면" class="feat-mock" />
        <div class="groups">
          <div v-for="g in attdGroups" :key="g.head" class="card group">
            <h3 class="group__head">{{ g.head }}</h3>
            <ul class="group__list">
              <li
                v-for="it in g.items"
                :key="it.t"
                :class="{ 'is-star': it.star }"
              >
                <span class="mk">{{ it.star ? "★" : "•" }}</span>
                <span
                  ><b>{{ it.t }}</b
                  ><em>{{ it.d }}</em></span
                >
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- 핵심기능 2: 산업안전 -->
    <section class="section">
      <div class="section__inner">
        <div class="feathead">
          <span class="feathead__badge">2</span>
          <div>
            <p class="feathead__lbl">핵심 기능</p>
            <h2 class="feathead__nm">산업안전</h2>
          </div>
        </div>
        <!-- TODO: MockupFrame 에 videoSrc/appVideoSrc 전달 시 자동재생 영상으로 교체 -->
        <MockupFrame label="산업안전 화면" class="feat-mock" />
        <div class="groups">
          <div v-for="g in safetyGroups" :key="g.head" class="card group">
            <h3 class="group__head">{{ g.head }}</h3>
            <ul class="group__list">
              <li
                v-for="it in g.items"
                :key="it.t"
                :class="{ 'is-star': it.star }"
              >
                <span class="mk">{{ it.star ? "★" : "•" }}</span>
                <span
                  ><b>{{ it.t }}</b
                  ><em>{{ it.d }}</em></span
                >
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 안전 지원 -->
    <section class="section section--alt">
      <div class="section__inner">
        <p class="section__kicker section__kicker--amber">AI SAFETY</p>
        <h2 class="section__title">AI 안전 지원 — 최종 판단은 사람이 합니다</h2>
        <div class="ai">
          <div class="ai__list">
            <div class="card ai__card">
              <b>위험요인·개선안 AI 추천</b>
              <p>
                작업 내용과 현장 사진을 함께 입력하면 위험요인과 개선안 초안을
                자동 작성. KOSHA·중대재해처벌법 등 공신력 있는 자료를 주기적으로
                반영해 근거를 강화합니다.
              </p>
            </div>
            <div class="card ai__card">
              <b>TBM 회의자료 초안 자동 작성</b>
              <p>
                작업별 안전교육 자료의 초안을 AI가 작성해 담당자의 작성 부담을
                줄여줍니다.
              </p>
            </div>
          </div>
          <div class="guard">
            <h3 class="guard__title">🛡 법적 책임 영역의 안전장치</h3>
            <ul>
              <li>
                모든 AI 결과는 담당자 검토·승인을 거친 후에만 공식 기록으로 확정
              </li>
              <li>AI는 초안을 제안할 뿐, 최종 판단은 사람이 합니다</li>
              <li>
                입력·출력 이력은 재현 가능하도록 보관되어 사후 검증에 대응
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- 법규 대응 전용 섹션 -->
    <section class="section section--alt">
      <div class="section__inner">
        <p class="section__kicker">COMPLIANCE</p>
        <h2 class="section__title">법규 대응 — 의무를 기능으로 충족합니다</h2>
        <div class="laws">
          <div class="laws__head">
            <span>법령</span><span>핵심 의무</span><span>PRAFTA 대응</span>
          </div>
          <div v-for="l in laws" :key="l.law" class="laws__row">
            <span class="laws__law">{{ l.law }}</span>
            <span class="laws__duty"
              ><b class="laws__m">핵심 의무</b>{{ l.duty }}</span
            >
            <span class="laws__feat"
              ><b class="laws__m">PRAFTA 대응</b>{{ l.feature }}</span
            >
          </div>
        </div>
        <p class="laws__note">
          ※ 본 안내는 일반적 대응 범위이며, 구체적 법적 판단·적용 범위는 사업장
          상황에 따라 달라질 수 있습니다.
        </p>
      </div>
    </section>

    <!-- 확장 로드맵 -->
    <section class="section">
      <div class="section__inner">
        <h2 class="section__title">
          확장 로드맵 <span class="road-badge">2차 오픈 예정</span>
        </h2>
        <ul class="cards cards--3">
          <li v-for="(r, i) in roadmap" :key="r.title" class="card road">
            <div class="road__num">{{ i + 1 }}</div>
            <h3 class="card__title">{{ r.title }}</h3>
            <p class="card__desc">{{ r.desc }}</p>
          </li>
        </ul>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta">
      <div class="cta__inner">
        <h2 class="cta__title">도입을 고민 중이신가요?</h2>
        <button
          type="button"
          class="btn btn--primary btn--lg"
          @click="goContact"
        >
          도입 문의하기
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
import MockupFrame from "./components/MockupFrame.vue";

const router = useRouter();
const goContact = () => router.push("/contact");

// 법규 대응 매핑 (법령 → 핵심 의무 → PRAFTA 대응)
const laws = [
  {
    law: "산업안전보건법",
    duty: "위험성평가 의무화(제36조), 작업 전 안전점검·안전교육 실시 및 기록 보존",
    feature:
      "위험성평가 · 안전점검 체크리스트 · TBM 참석 기록 · 교육 이수 관리와 이력 자동 보존",
  },
  {
    law: "중대재해처벌법",
    duty: "안전보건관리체계 구축·이행 및 점검 증빙(위험요인 파악·개선·점검의 반복 입증)",
    feature:
      "위험요인 등록·개선 이력, 아차사고 관리, 위험요인·개선안 AI 추천, 사고 시 시계열 증빙 자동 축적",
  },
  {
    law: "근로기준법",
    duty: "연차 부여·사용촉진(미사용 수당 리스크), 근로시간·출퇴근 기록 관리",
    feature:
      "연차 자동 부여 · 사용촉진 자동화, GPS 출퇴근 기록, 근무표·교대, 근태 통계 자동 집계",
  },
  {
    law: "개인정보보호법",
    duty: "근로자 개인정보 보호 — 암호화 저장·접근 통제",
    feature:
      "휴대폰·이메일 등 PII를 AES-GCM으로 암호화 저장하고 접근 이력을 관리",
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

const attdGroups = [
  {
    head: "출퇴근 · 근무관리",
    items: [
      {
        t: "모바일 출퇴근 체크",
        d: "현장 직원은 스마트폰으로, 관리자는 실시간 현황 확인",
      },
      {
        t: "GPS 지오펜싱 + 외근 관리",
        d: "사업장 반경 인정, 외근 시 사유·위치 기록으로 인건비 정산 신뢰성 확보",
      },
      { t: "근무표·교대 관리", d: "시프트·교대 스케줄을 한 화면에서 편성" },
      {
        t: "노무수령 거부",
        d: "지시 없는 근로의 임금·산재 리스크 사전 차단",
        star: true,
      },
    ],
  },
  {
    head: "일용직 관리 — 현장 특화",
    items: [
      {
        t: "일용직 전용 근태",
        d: "정규직과 분리된 체계로 일자별 투입·총 근무시간 확인",
      },
      {
        t: "휴대폰 없는 일용직도 누락 없이",
        d: "이름·연락처만으로 임시계정+전용 QR 발급, 관리자 단말 스캔으로 출퇴근·TBM 기록",
        star: true,
      },
      {
        t: "증빙 자료 제공",
        d: "사고 발생 시 QR·계정 기반 안전관리 증빙을 시계열로 제공",
      },
    ],
  },
  {
    head: "연차 · 통계",
    items: [
      {
        t: "연차 자동 관리",
        d: "고객사별 유연한 정책, 사용촉진 자동화로 미사용 연차수당 리스크 차단",
        star: true,
      },
      {
        t: "시간단위 연차 지원",
        d: "반차를 넘어 시간 단위로 유연하게 사용·관리",
      },
      {
        t: "근태 통계 자동 집계",
        d: "월별 근무·연차·결근 현황을 자동 집계해 의사결정 지원",
      },
    ],
  },
];

const safetyGroups = [
  {
    head: "위험성평가 · 안전점검",
    items: [
      {
        t: "위험성평가",
        d: "현장 위험요인 등록·평가·개선 이력을 체계적으로 관리·보존",
      },
      {
        t: "안전점검 체크리스트",
        d: "현장별 점검 항목을 관리하고 점검 이력을 누락 없이 보존",
      },
      {
        t: "아차사고 관리",
        d: "사고로 이어질 뻔한 위험상황을 기록·관리해 선제 대응",
      },
    ],
  },
  {
    head: "TBM (작업 전 안전점검회의)",
    items: [
      {
        t: "TBM 참석 기록·증빙",
        d: "작업별 안전수칙을 전파하고 참석 기록을 남겨 교육 이행 증빙",
      },
      {
        t: "TBM 회의자료 초안 AI 작성",
        d: "작업별 안전교육 자료 초안을 AI가 작성해 담당자 부담 경감",
        star: true,
      },
    ],
  },
  {
    head: "AI 안전 지원",
    items: [
      {
        t: "위험요인·개선안 AI 추천",
        d: "작업 내용+현장 사진 입력 시 위험요인·개선안 초안 자동 작성",
        star: true,
      },
      {
        t: "공신력 있는 근거 반영",
        d: "KOSHA·중대재해처벌법 등 자료를 주기적으로 수집·반영해 근거 강화",
      },
    ],
  },
];

const roadmap = [
  {
    title: "오프라인 출결장치 연동",
    desc: "오프라인 캡스 등 물리 출결 단말과 연동하여 다양한 현장 환경에 대응",
  },
  {
    title: "하도급사 데이터 연결",
    desc: "원청·하도급사 간 안전·근무 데이터를 연결해 다단계 현장을 통합 관리",
  },
  {
    title: "화학물질 취급 안전관리",
    desc: "화학물질 취급 현장에 특화된 안전관리 기능으로 업종별 규제 대응 강화",
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
  border: 1px solid transparent;
  border-radius: var(--btn-radius-lg);
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}
.btn--lg {
  height: 46px;
  padding: 0 24px;
  font-size: 15px;
}
.btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.btn--primary:hover {
  background: var(--color-primary-hover);
}

.phead {
  background: linear-gradient(180deg, #f1faf4 0%, var(--color-surface) 100%);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}
.phead__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 60px var(--header-padding-x);
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
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}
.phead__slogan {
  font-size: 19px;
  color: var(--color-text-muted);
  margin: 0;
}

.section {
  padding: 64px var(--header-padding-x);
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
.section__kicker--amber {
  color: var(--color-warning-text);
}
.section__title {
  color: var(--color-text-strong);
  font-size: 31px;
  font-weight: 800;
  letter-spacing: -0.02em;
  text-align: center;
  margin: 0 0 24px;
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 18px;
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

.pillar__list {
  list-style: none;
  margin: 8px 0 0;
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
  margin: 24px 0 0;
  background: var(--color-surface);
  border-left: 5px solid var(--color-primary);
  border-radius: 10px;
  padding: 14px 18px;
  font-weight: 700;
  color: var(--color-primary-pressed);
}

.feathead {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.feathead__badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--color-primary);
  color: #fff;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.feathead__lbl {
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
  margin: 0;
}
.feathead__nm {
  font-size: 25px;
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0;
}

.groups {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.group__head {
  font-size: 15px;
  font-weight: 800;
  color: var(--color-primary);
  margin: 0 0 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}
.group__list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.group__list li {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}
.group__list .mk {
  flex: 0 0 14px;
  color: var(--color-primary);
  font-weight: 800;
}
.group__list li.is-star .mk {
  color: var(--color-warning-text);
}
.group__list b {
  display: block;
  font-size: 15px;
  color: var(--color-text-strong);
}
.group__list em {
  display: block;
  font-style: normal;
  font-size: 13.5px;
  color: var(--color-text-muted);
  margin-top: 3px;
  line-height: 1.5;
}

.ai {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
  align-items: stretch;
}
.ai__list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.ai__card b {
  display: block;
  font-size: 18px;
  color: var(--color-text-strong);
  margin-bottom: 7px;
}
.ai__card p {
  margin: 0;
  font-size: 15px;
  color: var(--color-text-muted);
  line-height: 1.6;
}
.guard {
  background: var(--color-warning-bg);
  border: 1px solid #fbbf24;
  border-radius: var(--card-radius);
  padding: 20px;
}
.guard__title {
  color: var(--color-warning-text);
  font-size: 18px;
  font-weight: 800;
  margin: 0 0 12px;
}
.guard ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.guard li {
  position: relative;
  padding: 8px 0 8px 17px;
  font-size: 15px;
  color: #7c4a11;
  line-height: 1.55;
}
.guard li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: var(--color-warning-text);
  font-weight: 800;
}

.road-badge {
  display: inline-block;
  vertical-align: middle;
  font-size: 12px;
  font-weight: 800;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border-radius: 7px;
  padding: 4px 10px;
  margin-left: 8px;
}
.road__num {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--color-warning-bg);
  color: var(--color-primary-pressed);
  font-weight: 800;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.cta {
  padding: 72px var(--header-padding-x);
  text-align: center;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  color: var(--color-text);
}
.cta__title {
  color: var(--color-text-strong);
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 22px;
}

/* 기능 옆 목업 */
.feat-mock {
  margin: 4px 0 24px;
}

/* 법규 대응 표 */
.laws {
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  overflow: hidden;
  background: var(--color-surface);
  box-shadow: var(--card-shadow);
}
.laws__head,
.laws__row {
  display: grid;
  grid-template-columns: 0.7fr 1.3fr 1.6fr;
}
.laws__head {
  background: var(--color-primary-pressed);
  color: #fff;
  font-weight: 800;
  font-size: 14.5px;
}
.laws__head span {
  padding: 12px 16px;
}
.laws__row {
  border-top: 1px solid var(--color-border);
  font-size: 13.5px;
}
.laws__row span {
  padding: 14px 16px;
}
.laws__law {
  font-weight: 800;
  color: var(--color-primary-pressed);
}
.laws__duty {
  color: var(--color-text-muted);
}
.laws__feat {
  color: var(--color-text);
}
.laws__m {
  display: none;
}
.laws__note {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 860px) {
  .cards--3,
  .groups,
  .ai {
    grid-template-columns: 1fr;
  }
  .phead__title {
    font-size: 26px;
  }
  .laws__head {
    display: none;
  }
  .laws__row {
    grid-template-columns: 1fr;
    padding: 4px 0 12px;
  }
  .laws__row span {
    padding: 6px 16px;
  }
  .laws__law {
    padding-top: 14px;
    font-size: 15px;
  }
  .laws__m {
    display: block;
    font-size: 11px;
    font-weight: 800;
    color: var(--color-text-muted);
    margin-bottom: 3px;
    letter-spacing: 0.04em;
  }
}
</style>
