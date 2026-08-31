<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="page-head">
      <div class="page-head__inner">
        <p class="page-head__kicker">ATTENDANCE</p>
        <h1 class="page-head__title">근태관리 — 출근 한 번이면, 마감까지 빈틈없이</h1>
      </div>
    </section>

    <IntroAnchorTabs :tabs="tabs" />

    <IntroFeatureSection
      id="clock"
      axis="attd"
      label="근태관리"
      heading="GPS 지오펜스로, 현장에서의 출퇴근을 정확하게"
      :bullets="clockBullets"
    />

    <IntroFeatureSection
      id="schedule"
      axis="attd"
      label="근태관리"
      heading="교대 패턴을 등록하면, 1년치 스케줄이 자동으로"
      :bullets="scheduleBullets"
      reverse
    />

    <IntroFeatureSection
      id="leave"
      axis="attd"
      label="근태관리"
      heading="부여부터 촉진까지, 연차 관리가 스스로 돌아갑니다"
      :bullets="leaveBullets"
    />

    <IntroFeatureSection
      id="approval"
      axis="attd"
      label="근태관리"
      heading="네 종류의 결재를 한 화면에서"
      :bullets="approvalBullets"
      reverse
    />

    <IntroFeatureSection
      id="closing"
      axis="attd"
      label="근태관리"
      heading="빠진 것이 있으면, 마감이 잠깁니다"
      :bullets="closingBullets"
    >
      <template #footnote>
        <p class="feature-note">
          마감된 근태 데이터는 앞으로 급여 처리 시스템과의 연동 등으로
          확장될 수 있습니다.
        </p>
        <p class="feature-note feature-note--strong">
          마감이 곧 검증입니다. 빠진 기록이 있는 채로는 닫히지 않습니다.
        </p>
      </template>
    </IntroFeatureSection>

    <IntroFeatureSection
      id="daily-worker"
      axis="attd"
      label="근태관리"
      heading="QR 한 번으로 출역 등록부터 근로계약 체결·교부·3년 보존까지"
      :bullets="dailyWorkerBullets"
      :images="dailyWorkerImages"
      reverse
    />

    <section class="cross-link">
      <div class="cross-link__inner">
        <router-link to="/safety" class="feature-link"
          >일용직의 출퇴근 기록은 그대로 안전활동의 기반이 됩니다 — 안전관리 보기 →</router-link
        >
      </div>
    </section>

    <IntroCtaBand heading="근태관리, PRAFTA로 시작해 보세요." />

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
import IntroCtaBand from "@/components/intro/IntroCtaBand.vue";
import contractEsign from "@/assets/intro/contract-esign.jpg";
import contractAdmin from "@/assets/intro/contract-admin.png";

const tabs = [
  { id: "clock", label: "출퇴근 기록" },
  { id: "schedule", label: "근무일정·교대" },
  { id: "leave", label: "휴가·연차" },
  { id: "approval", label: "요청·승인" },
  { id: "closing", label: "근태 마감" },
  { id: "daily-worker", label: "일용직 관리·전자계약" },
];

const clockBullets = [
  "사업장별 지오펜스 반경 설정, 범위 밖은 \"근무지 외\" 태그",
  "외근은 사유 입력으로 등록",
  "GPS 미확인 건은 \"확인 필요\"로 관리되어 마감 전 반드시 해소",
  "하루 2구간 근무·재출근 지원",
  "스케줄 대비 지각·조퇴·초과근무 자동 판정과 인정시간 산출",
];
const scheduleBullets = [
  "근무 유형(시간·휴게·구간) 정의",
  "교대 패턴 조합으로 연간 스케줄 자동 생성",
  "회사 지정 휴일 자동 제외",
  "미래 변경 시 확정 휴가는 보호",
];
const leaveBullets = [
  "법정 연차 자동 부여(입사일·회계연도 기준 설정)",
  "1년 미만 월차 자동 부여",
  "종일·반차·시간 단위 사용, 소정근로시간 기준 자동 환산",
  "잔여·사용 현황과 신청 충돌 자동 경고",
  "사용촉진(1·2차) 대상 자동 산출·지정, 촉진 지정 연차의 출근 차단(노무 수령 거부)까지 시스템이 처리",
];
const approvalBullets = [
  "스케줄 수정·근태 보정·초과근무·연차 4탭 통합 승인함",
  "마감 임박 건 D-day 표시",
  "다단 결재·HR 최종 승인 설정",
  "승인·반려 사유 필수, 전 이력 보존",
  "관리자 동시 처리 충돌 방지(선점)",
];
const closingBullets = [
  "시간 표준화(시작 올림·종료 내림)",
  "자동 휴게 공제",
  "마감 차단 3조건(미결 요청·GPS 미확인·미승인 추가근무) — 해소 전 마감 불가",
  "마감 후 데이터가 급여·인사 기준",
  "현황 조회·엑셀 다운로드",
];
const dailyWorkerBullets = [
  "QR 셀프 등록(휴대폰 없는 근로자는 관리자 QR로 커버)",
  "입장 승인제 + 일괄 승인",
  "근로계약서 자동 작성·전자서명(사업장별 양식·버전 관리)",
  "서명본 SHA-256 무결성·추가 전용 저장",
  "앱 상시 열람(근로기준법 제17조)·3년 보존(제42조)",
  "현장별 슬롯 관리, 당일 자정 자동 만료",
];
const dailyWorkerImages = [
  { src: contractEsign, alt: "근로계약서 전자서명 화면", variant: "phone" },
  { src: contractAdmin, alt: "계약서관리 화면", variant: "browser" },
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
  color: var(--color-attd);
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
.feature-link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}
.cross-link {
  padding: 32px var(--header-padding-x);
  text-align: center;
  background: var(--color-bg);
}
.cross-link__inner {
  max-width: 720px;
  margin: 0 auto;
}
</style>
