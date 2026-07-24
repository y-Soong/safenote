<template>
  <!--
    프라프타 홈페이지 — 도입 문의 (/contact)
    출처: 홈페이지 IA 명세 §4. 제출은 백엔드 도입문의 API로 전송(DB 적재).
    ※ 백엔드(TB_INQUIRY/엔드포인트)는 별도 적용 예정. 엔드포인트 미배포 시 error 상태로 안내.
  -->
  <div class="intro-page">
    <IntroHeader />

    <section class="phead">
      <div class="phead__inner">
        <p class="phead__kicker">CONTACT</p>
        <h1 class="phead__title">도입 문의</h1>
        <p class="phead__slogan">
          현장 규모와 운영 방식을 알려주시면 맞춤 구성을 제안드립니다.
        </p>
      </div>
    </section>

    <section class="section">
      <div class="section__inner">
        <div class="contact">
          <!-- 폼 -->
          <div class="contact__form card">
            <template v-if="state !== 'success'">
              <form @submit.prevent="handleSubmit" novalidate>
                <!-- 허니팟(스팸 방지) : 사용자에게 보이지 않음 -->
                <input
                  v-model="form.company_url"
                  class="hp"
                  type="text"
                  tabindex="-1"
                  autocomplete="off"
                  aria-hidden="true"
                />

                <div class="field">
                  <label for="companyNm"
                    >회사명 <span class="req">*</span></label
                  >
                  <input
                    id="companyNm"
                    v-model.trim="form.companyNm"
                    type="text"
                    maxlength="50"
                    :class="{ 'is-error': errors.companyNm }"
                  />
                  <p v-if="errors.companyNm" class="err">
                    {{ errors.companyNm }}
                  </p>
                </div>

                <div class="field-row">
                  <div class="field">
                    <label for="contactNm"
                      >담당자명 <span class="req">*</span></label
                    >
                    <input
                      id="contactNm"
                      v-model.trim="form.contactNm"
                      type="text"
                      maxlength="30"
                      :class="{ 'is-error': errors.contactNm }"
                    />
                    <p v-if="errors.contactNm" class="err">
                      {{ errors.contactNm }}
                    </p>
                  </div>
                  <div class="field">
                    <label for="mblNo">연락처 <span class="req">*</span></label>
                    <input
                      id="mblNo"
                      v-model.trim="form.mblNo"
                      type="tel"
                      maxlength="20"
                      placeholder="010-0000-0000"
                      :class="{ 'is-error': errors.mblNo }"
                    />
                    <p v-if="errors.mblNo" class="err">{{ errors.mblNo }}</p>
                  </div>
                </div>

                <div class="field-row">
                  <div class="field">
                    <label for="email">이메일 <span class="req">*</span></label>
                    <input
                      id="email"
                      v-model.trim="form.email"
                      type="email"
                      maxlength="100"
                      placeholder="name@company.com"
                      :class="{ 'is-error': errors.email }"
                    />
                    <p v-if="errors.email" class="err">{{ errors.email }}</p>
                  </div>
                  <div class="field">
                    <label for="inquiryType">문의 유형</label>
                    <select id="inquiryType" v-model="form.inquiryType">
                      <option
                        v-for="o in inquiryTypes"
                        :key="o.value"
                        :value="o.value"
                      >
                        {{ o.label }}
                      </option>
                    </select>
                  </div>
                </div>

                <div class="field">
                  <label for="content"
                    >문의 내용 <span class="req">*</span></label
                  >
                  <textarea
                    id="content"
                    v-model.trim="form.content"
                    rows="6"
                    maxlength="2000"
                    :class="{ 'is-error': errors.content }"
                  ></textarea>
                  <p v-if="errors.content" class="err">{{ errors.content }}</p>
                </div>

                <div class="agree-block">
                  <label
                    class="agree"
                    :class="{ 'is-error': errors.privacyAgreeYn }"
                  >
                    <input v-model="form.privacyAgreeYn" type="checkbox" />
                    <span
                      >개인정보 수집·이용에 동의합니다.
                      <span class="req">*</span></span
                    >
                  </label>
                  <button
                    type="button"
                    class="agree__view"
                    @click="showPrivacy = true"
                  >
                    전문 보기
                  </button>
                </div>
                <p v-if="errors.privacyAgreeYn" class="err">
                  {{ errors.privacyAgreeYn }}
                </p>

                <p v-if="state === 'error'" class="form-error">
                  {{ submitError }}
                </p>

                <button
                  type="submit"
                  class="btn btn--primary btn--lg btn--block"
                  :disabled="state === 'submitting'"
                >
                  {{ state === "submitting" ? "전송 중…" : "문의 보내기" }}
                </button>
              </form>
            </template>

            <!-- 성공 -->
            <div v-else class="done">
              <div class="done__icon">✓</div>
              <h3 class="done__title">문의가 접수되었습니다.</h3>
              <p class="done__desc">
                담당자가 확인 후 빠르게 연락드리겠습니다. 감사합니다.
              </p>
              <button type="button" class="btn btn--ghost" @click="resetForm">
                새 문의 작성
              </button>
            </div>
          </div>

          <!-- 직접 연락 안내 -->
          <aside class="contact__aside card">
            <h3 class="aside__title">바로 연락하기</h3>
            <ul class="aside__list">
              <li><span>대표</span> 윤순기</li>
              <li>
                <span>전화</span> <a href="tel:01077635257">010-7763-5257</a>
              </li>
              <li>
                <span>이메일</span>
                <a href="mailto:dudjswp@gmail.com">dudjswp@gmail.com</a>
              </li>
            </ul>
            <p class="aside__note">
              근태·인사 + 산업안전 통합 SaaS, PRAFTA. 현장의 사람과 안전을
              하나의 서비스로.
            </p>
          </aside>
        </div>
      </div>
    </section>

    <!-- 개인정보 수집·이용 동의 전문 (도입 문의 응대 목적) -->
    <div v-if="showPrivacy" class="modal" @click.self="showPrivacy = false">
      <div
        class="modal__panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="privacyTitle"
      >
        <div class="modal__head">
          <h2 id="privacyTitle" class="modal__title">
            개인정보 수집·이용 동의
          </h2>
          <button
            type="button"
            class="modal__close"
            aria-label="닫기"
            @click="showPrivacy = false"
          >
            ×
          </button>
        </div>
        <div class="modal__body">
          <p class="modal__intro">
            프라프타(이하 “회사”)는 「개인정보 보호법」에 따라 도입 문의의 접수
            및 응대를 위하여 아래와 같이 개인정보를 수집·이용합니다. 내용을
            확인하신 후 동의 여부를 결정해 주시기 바랍니다.
          </p>
          <table class="ptable">
            <tbody>
              <tr>
                <th>수집·이용 목적</th>
                <td>
                  도입 문의 접수 및 응대, 상담·견적 안내, 영업 목적의 연락
                </td>
              </tr>
              <tr>
                <th>수집 항목</th>
                <td>
                  회사명, 담당자명, 연락처(휴대전화번호), 이메일 주소, 문의
                  유형, 문의 내용
                </td>
              </tr>
              <tr>
                <th>보유·이용 기간</th>
                <td>
                  문의 처리 완료 후 3년간 보관 후 지체 없이 파기<br />
                  (관계 법령에 별도의 보존 의무가 있는 경우 해당 기간 동안 보관)
                </td>
              </tr>
            </tbody>
          </table>
          <p class="modal__note">
            귀하는 개인정보 수집·이용에 대한 동의를 거부하실 권리가 있습니다.
            다만 동의를 거부하실 경우 도입 문의의 접수 및 응대가 제한될 수
            있습니다.
          </p>
          <p class="modal__note modal__note--muted">
            수집한 개인정보는 위 목적 범위 내에서만 이용하며, 정보주체의 별도
            동의 없이 제3자에게 제공하지 않습니다. 문의 사항은 dudjswp@gmail.com
            으로 연락해 주시기 바랍니다.
          </p>
        </div>
        <div class="modal__foot">
          <button
            type="button"
            class="btn btn--ghost"
            @click="showPrivacy = false"
          >
            닫기
          </button>
          <button
            type="button"
            class="btn btn--primary"
            @click="agreeFromModal"
          >
            동의하고 닫기
          </button>
        </div>
      </div>
    </div>

    <IntroFooter />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from "vue";
import api from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import IntroHeader from "./components/IntroHeader.vue";
import IntroFooter from "./components/IntroFooter.vue";

// 백엔드 도입문의 접수 엔드포인트 (별도 적용 예정 — 백엔드 정리 문서 참조)
const INQUIRY_ENDPOINT = "/comApi/inquiry/register";

const inquiryTypes = [
  { value: "PRODUCT", label: "제품 문의" },
  { value: "QUOTE", label: "견적 문의" },
  { value: "PARTNER", label: "제휴 문의" },
  { value: "ETC", label: "기타" },
];

const createForm = () => ({
  companyNm: "",
  contactNm: "",
  mblNo: "",
  email: "",
  inquiryType: "PRODUCT",
  content: "",
  privacyAgreeYn: false,
  company_url: "", // honeypot (봇이 채우면 서버에서 무시/차단)
});

const form = reactive(createForm());
const errors = reactive({});
const state = ref("idle"); // idle | submitting | success | error
const submitError = ref("");
const showPrivacy = ref(false);

// 모달에서 바로 동의 처리
const agreeFromModal = () => {
  form.privacyAgreeYn = true;
  delete errors.privacyAgreeYn;
  showPrivacy.value = false;
};

const onKeydown = (e) => {
  if (e.key === "Escape") showPrivacy.value = false;
};
onMounted(() => window.addEventListener("keydown", onKeydown));
onBeforeUnmount(() => window.removeEventListener("keydown", onKeydown));

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TEL_RE = /^[0-9\-+\s()]{9,20}$/;

const validate = () => {
  Object.keys(errors).forEach((k) => delete errors[k]);
  if (!form.companyNm) errors.companyNm = "회사명을 입력해 주세요.";
  if (!form.contactNm) errors.contactNm = "담당자명을 입력해 주세요.";
  if (!form.mblNo) errors.mblNo = "연락처를 입력해 주세요.";
  else if (!TEL_RE.test(form.mblNo))
    errors.mblNo = "연락처 형식을 확인해 주세요.";
  if (!form.email) errors.email = "이메일을 입력해 주세요.";
  else if (!EMAIL_RE.test(form.email))
    errors.email = "이메일 형식을 확인해 주세요.";
  if (!form.content) errors.content = "문의 내용을 입력해 주세요.";
  if (!form.privacyAgreeYn)
    errors.privacyAgreeYn = "개인정보 수집·이용 동의가 필요합니다.";
  return Object.keys(errors).length === 0;
};

const handleSubmit = async () => {
  if (state.value === "submitting") return;
  if (!validate()) return;

  state.value = "submitting";
  submitError.value = "";
  try {
    await api.post(INQUIRY_ENDPOINT, {
      companyNm: form.companyNm,
      contactNm: form.contactNm,
      mblNo: form.mblNo,
      email: form.email,
      inquiryType: form.inquiryType,
      content: form.content,
      privacyAgreeYn: form.privacyAgreeYn ? "Y" : "N",
      company_url: form.company_url, // honeypot 전달(서버 검증)
    });
    state.value = "success";
  } catch (err) {
    state.value = "error";
    submitError.value = resolveApiErrorMessage(
      err,
      "문의 전송 중 오류가 발생했습니다. 잠시 후 다시 시도하시거나 전화로 연락 주세요."
    );
  }
};

const resetForm = () => {
  Object.assign(form, createForm());
  Object.keys(errors).forEach((k) => delete errors[k]);
  state.value = "idle";
  submitError.value = "";
};
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
}
.btn--lg {
  height: 46px;
  font-size: 15px;
}
.btn--block {
  width: 100%;
  margin-top: 6px;
}
.btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.btn--primary:hover {
  background: var(--color-primary-hover);
}
.btn--primary:disabled {
  opacity: 0.6;
  cursor: default;
}
.btn--ghost {
  background: transparent;
  color: var(--color-text-strong);
  border-color: var(--color-border-strong);
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
.card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 28px;
}

.contact {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  align-items: start;
}

.field {
  margin-bottom: 16px;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
label {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-strong);
  margin-bottom: 7px;
}
.req {
  color: var(--color-danger);
}
input[type="text"],
input[type="tel"],
input[type="email"],
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  font: inherit;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--input-radius);
  padding: 10px 12px;
}
input:focus,
select:focus,
textarea:focus {
  outline: var(--focus-ring-width) solid var(--color-focus-ring);
  outline-offset: var(--outline-offset);
  border-color: var(--color-primary);
}
textarea {
  resize: vertical;
}
.is-error {
  border-color: var(--color-danger);
}
.err {
  color: var(--color-danger);
  font-size: 13.5px;
  margin: 6px 0 0;
}
.form-error {
  color: var(--color-danger);
  font-size: 13.5px;
  margin: 4px 0 12px;
}

.agree-block {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
  margin: 8px 0 4px;
}
.agree {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  font-weight: 500;
  color: var(--color-text);
  cursor: pointer;
}
.agree input {
  margin-top: 3px;
}
.agree.is-error span {
  color: var(--color-danger);
}
.agree__view {
  background: none;
  border: 0;
  padding: 0;
  margin-top: 1px;
  color: var(--color-primary);
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
  flex: 0 0 auto;
}
.agree__view:hover {
  color: var(--color-primary-hover);
}

.hp {
  position: absolute;
  left: -9999px;
  width: 1px;
  height: 1px;
  opacity: 0;
}

/* 개인정보 수집·이용 동의 모달 */
.modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 20, 0.55);
}
.modal__panel {
  background: var(--color-surface);
  border-radius: var(--card-radius);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 600px;
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
}
.modal__title {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0;
}
.modal__close {
  background: none;
  border: 0;
  font-size: 26px;
  line-height: 1;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0 4px;
}
.modal__close:hover {
  color: var(--color-text-strong);
}
.modal__body {
  padding: 22px 24px;
  overflow-y: auto;
}
.modal__intro {
  font-size: 15.5px;
  color: var(--color-text);
  margin: 0 0 18px;
}
.ptable {
  width: 100%;
  border-collapse: collapse;
  font-size: 15px;
  margin: 0 0 18px;
}
.ptable th,
.ptable td {
  border: 1px solid var(--color-border-strong);
  padding: 12px 14px;
  text-align: left;
  vertical-align: top;
}
.ptable th {
  width: 34%;
  background: #f5fbf7;
  color: var(--color-text-strong);
  font-weight: 700;
  white-space: nowrap;
}
.ptable td {
  color: var(--color-text);
}
.modal__note {
  font-size: 14.5px;
  color: var(--color-text);
  margin: 0 0 10px;
}
.modal__note--muted {
  color: var(--color-text-muted);
  font-size: 13.5px;
  margin-bottom: 0;
}
.modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
}

.done {
  text-align: center;
  padding: 32px 12px;
}
.done__icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.done__title {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 8px;
}
.done__desc {
  color: var(--color-text-muted);
  margin: 0 0 22px;
}

.contact__aside {
  background: var(--color-bg);
}
.aside__title {
  font-size: 18px;
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 14px;
}
.aside__list {
  list-style: none;
  margin: 0 0 16px;
  padding: 0;
}
.aside__list li {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--color-border);
  font-size: 15.5px;
}
.aside__list li:first-child {
  border-top: 0;
}
.aside__list span {
  flex: 0 0 48px;
  color: var(--color-text-muted);
  font-weight: 700;
}
.aside__list a {
  color: var(--color-primary);
  text-decoration: none;
}
.aside__list a:hover {
  text-decoration: underline;
}
.aside__note {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

@media (max-width: 860px) {
  .contact {
    grid-template-columns: 1fr;
  }
  .field-row {
    grid-template-columns: 1fr;
  }
  .phead__title {
    font-size: 28px;
  }
}
</style>
