<template>
  <div class="intro-page">
    <IntroHeader />

    <section class="page-head">
      <div class="page-head__inner">
        <h1 class="page-head__title">
          도입 규모와 현장 상황을 알려주시면, 맞는 방법을 제안해 드립니다.
        </h1>
      </div>
    </section>

    <section class="contact-layout">
      <form class="contact-form" @submit.prevent="handleSubmit">
        <div class="field">
          <label for="companyName">회사명 <span class="req">*</span></label>
          <input
            id="companyName"
            v-model="form.companyName"
            type="text"
            required
          />
        </div>
        <div class="field">
          <label for="managerName">담당자명 <span class="req">*</span></label>
          <input
            id="managerName"
            v-model="form.managerName"
            type="text"
            required
          />
        </div>
        <div class="field">
          <label for="phone">연락처 <span class="req">*</span></label>
          <input id="phone" v-model="form.phone" type="tel" required />
        </div>
        <div class="field">
          <label for="email">이메일 <span class="req">*</span></label>
          <input id="email" v-model="form.email" type="email" required />
        </div>

        <div class="field">
          <span class="field__label">관심 영역</span>
          <div class="checkbox-row">
            <label v-for="opt in interestOptions" :key="opt.value">
              <input
                type="checkbox"
                :value="opt.value"
                v-model="form.interests"
              />
              {{ opt.label }}
            </label>
          </div>
        </div>

        <div class="field">
          <label for="siteCount">사업장(현장) 수</label>
          <input
            id="siteCount"
            v-model="form.siteCount"
            type="number"
            min="0"
          />
        </div>

        <div class="field">
          <label for="headcountRange">상시 인원 규모</label>
          <select id="headcountRange" v-model="form.headcountRange">
            <option value="">선택해 주세요</option>
            <option
              v-for="opt in headcountOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </option>
          </select>
        </div>

        <div class="field">
          <label for="message">문의 내용 <span class="req">*</span></label>
          <textarea
            id="message"
            v-model="form.message"
            rows="5"
            required
          ></textarea>
        </div>

        <label class="agree-row">
          <input type="checkbox" v-model="form.agree" required />
          <span
            >개인정보 수집·이용에 동의합니다.
            <router-link to="/privacy">개인정보처리방침 보기</router-link></span
          >
        </label>

        <IntroButton
          type="submit"
          variant="primary"
          size="lg"
          :disabled="!canSubmit"
        >
          문의 보내기
        </IntroButton>
      </form>

      <aside class="contact-aside">
        <div class="contact-aside__card">
          <p class="contact-aside__title">
            100명 이상 사업장은 엔터프라이즈 도입 상담을 권장합니다.
          </p>
          <p class="contact-aside__email">
            직통 이메일:
            <a href="mailto:contact@prafta.com">contact@prafta.com</a>
          </p>
        </div>
      </aside>
    </section>

    <IntroFooter />
  </div>
</template>

<script setup>
import { reactive, computed } from "vue";
import IntroHeader from "@/components/intro/IntroHeader.vue";
import IntroFooter from "@/components/intro/IntroFooter.vue";
import IntroButton from "@/components/intro/IntroButton.vue";

const form = reactive({
  companyName: "",
  managerName: "",
  phone: "",
  email: "",
  interests: [],
  siteCount: "",
  headcountRange: "",
  message: "",
  agree: false,
});

const interestOptions = [
  { value: "attd", label: "근태관리" },
  { value: "safety", label: "안전관리" },
  { value: "both", label: "둘 다" },
];
const headcountOptions = [
  { value: "under30", label: "~30명" },
  { value: "30to100", label: "30~100명" },
  { value: "over100", label: "100명 이상" },
];

// 단순 필수값 체크(제출 버튼 활성화 조건) — 실제 전송/에러 처리는 developer 구현
const canSubmit = computed(
  () =>
    form.companyName &&
    form.managerName &&
    form.phone &&
    form.email &&
    form.message &&
    form.agree
);

// 값(value)을 화면 라벨로 변환
const interestLabel = (value) =>
  interestOptions.find((opt) => opt.value === value)?.label || value;
const headcountLabel = (value) =>
  headcountOptions.find((opt) => opt.value === value)?.label || value;

// mailto 본문 조립(§8 mailto 폴백) — 정적 사이트, 서버 전송 없음
const handleSubmit = () => {
  if (!canSubmit.value) return;

  const subject = `[PRAFTA 도입문의] ${form.companyName}`;
  const bodyLines = [
    `회사명: ${form.companyName}`,
    `담당자명: ${form.managerName}`,
    `연락처: ${form.phone}`,
    `이메일: ${form.email}`,
    `관심 영역: ${
      form.interests.length
        ? form.interests.map(interestLabel).join(", ")
        : "미선택"
    }`,
    `사업장(현장) 수: ${form.siteCount || "미입력"}`,
    `상시 인원 규모: ${
      form.headcountRange ? headcountLabel(form.headcountRange) : "미선택"
    }`,
    "",
    "문의 내용:",
    form.message,
  ];
  const body = bodyLines.join("\n");

  const mailtoUrl = `mailto:contact@prafta.com?subject=${encodeURIComponent(
    subject
  )}&body=${encodeURIComponent(body)}`;
  window.location.href = mailtoUrl;
};
</script>

<style scoped>
.intro-page {
  background: var(--color-surface);
  color: var(--color-text);
}
.page-head {
  padding: 56px var(--header-padding-x) 32px;
  text-align: center;
}
.page-head__inner {
  max-width: 720px;
  margin: 0 auto;
}
.page-head__title {
  font-size: var(--intro-text-lg);
  line-height: var(--intro-lh-tight);
  font-weight: 800;
  color: var(--color-text-strong);
  line-height: 1.4;
  margin: 0;
}
.contact-layout {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px var(--header-padding-x) 72px;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 32px;
  align-items: start;
}
.contact-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 28px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label,
.field__label {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-strong);
}
.req {
  color: var(--color-danger);
}
.field input[type="text"],
.field input[type="tel"],
.field input[type="email"],
.field input[type="number"],
.field select,
.field textarea {
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text);
  background: var(--color-surface);
}
.field input:focus,
.field select:focus,
.field textarea:focus {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
  border-color: var(--color-primary);
}
.checkbox-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--color-text);
}
.checkbox-row label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 400;
}
.agree-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-muted);
}
.agree-row a {
  color: var(--color-primary);
}
.contact-aside__card {
  background: var(--color-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: 20px;
}
.contact-aside__title {
  font-size: var(--intro-text-sm);
  font-weight: 700;
  color: var(--color-text-strong);
  margin: 0 0 12px;
}
.contact-aside__email {
  font-size: var(--intro-text-xs);
  color: var(--color-text-muted);
  margin: 0;
}
.contact-aside__email a {
  color: var(--color-primary);
}

@media (max-width: 1023px) {
  .contact-layout {
    grid-template-columns: 1fr;
  }
}
</style>
