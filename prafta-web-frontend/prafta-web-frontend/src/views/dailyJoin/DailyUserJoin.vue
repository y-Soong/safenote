<template>
  <div class="daily-join">
    <header class="daily-join__header">
      <h1 class="daily-join__title">일일사용자 회원가입</h1>
    </header>

    <LoadingSpinner v-if="isLoading" />

    <div v-else-if="isLinkInvalid" class="daily-join__error">
      <p>유효하지 않은 회원가입 링크입니다.</p>
      <p class="daily-join__error-desc">사업장 관리자에게 문의해주세요.</p>
    </div>

    <div v-else-if="isJoined" class="daily-join__done">
      <p class="daily-join__done-mark">✅ 가입 완료</p>
      <p class="daily-join__done-id">
        발급된 아이디: <strong>{{ joinedUserId }}</strong>
      </p>
      <p class="daily-join__done-desc">
        해당 아이디로 출입/근태 등록이 가능합니다. 문의는 사업장 관리자에게
        해주세요.
      </p>
    </div>

    <form v-else class="daily-join__form" @submit.prevent="handleSubmit">
      <div class="daily-join__row">
        <label class="daily-join__label">회사명</label>
        <input
          class="daily-join__input"
          :value="cmpnyNm"
          disabled
          placeholder="회사명"
        />
      </div>
      <div class="daily-join__row">
        <label class="daily-join__label">사업장</label>
        <input
          class="daily-join__input"
          :value="siteNm"
          disabled
          placeholder="사업장명"
        />
      </div>

      <hr class="daily-join__divider" />

      <div class="daily-join__row">
        <label class="daily-join__label">아이디</label>
        <div class="daily-join__field">
          <input
            class="daily-join__input"
            v-model="userId"
            ref="userIdRef"
            minlength="4"
            maxlength="10"
            placeholder="4 ~ 10자"
          />
          <button
            type="button"
            class="btn btn-primary"
            @click="handleUserIdDupleCheck"
          >
            중복확인
          </button>
        </div>
        <span class="daily-join__msg">{{ userIdMsg }}</span>
      </div>

      <div class="daily-join__row">
        <label class="daily-join__label">이름</label>
        <input
          class="daily-join__input"
          v-model="userNm"
          maxlength="15"
          placeholder="최대 15자"
        />
      </div>

      <div class="daily-join__row">
        <label class="daily-join__label">휴대폰 번호</label>
        <div class="daily-join__field">
          <input
            class="daily-join__input"
            v-model="mblNo"
            ref="mblNoRef"
            maxlength="13"
            placeholder="휴대폰 번호"
            :disabled="mblNoLocked"
          />
          <button
            type="button"
            class="btn btn-primary"
            :disabled="timer > 0 || mblNoLocked"
            @click="handleSmsAuthReq"
          >
            {{ timer > 0 ? `${timer}초 후 재요청` : "인증요청" }}
          </button>
        </div>
      </div>

      <div class="daily-join__row">
        <label class="daily-join__label">인증번호</label>
        <div class="daily-join__field">
          <input
            class="daily-join__input"
            v-model="certNo"
            maxlength="6"
            placeholder="인증번호 6자리"
            :disabled="smsVerified"
          />
          <button
            type="button"
            class="btn btn-primary"
            :disabled="smsVerified"
            @click="handleSmsAuthChk"
          >
            확인
          </button>
        </div>
        <span class="daily-join__msg">{{ smsAuthMsg }}</span>
      </div>

      <hr class="daily-join__divider" />

      <div class="daily-join__terms">
        <label class="daily-join__terms-all">
          <input type="checkbox" v-model="allAgreed" @change="handleAllAgree" />
          <span>전체 동의하기</span>
        </label>

        <ul class="daily-join__terms-list">
          <li
            v-for="terms in termsList"
            :key="terms.systValDCd"
            class="daily-join__terms-item"
          >
            <label class="daily-join__terms-label">
              <input type="checkbox" v-model="terms.checked" />
              <span>(필수) {{ terms.systValDNm }}</span>
            </label>
            <button
              type="button"
              class="daily-join__terms-view"
              @click="handleViewTerms(terms)"
            >
              보기
            </button>
          </li>
        </ul>
      </div>

      <button type="submit" class="btn btn-primary btn-lg daily-join__submit">
        회원가입
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import TermsDetailPop from "@/components/popup/TermsDetailPop.vue";

const route = useRoute();
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

/* 화면 상태 */
const isLoading = ref(true);
const isLinkInvalid = ref(false);
const isJoined = ref(false);
const joinedUserId = ref("");

/* 회사/사업장 정보 (joinCd 에서 파싱) */
const cmpnyCd = ref("");
const siteCd = ref("");
const cmpnyNm = ref("");
const siteNm = ref("");

/* 입력값 */
const userId = ref("");
const userNm = ref("");
const mblNo = ref("");
const certNo = ref("");

/* 검증 상태 */
const userIdChecked = ref(false);
const userIdMsg = ref("");
const checkedUserId = ref(""); // 중복확인을 통과한 아이디 값
const smsVerified = ref(false);
const smsAuthMsg = ref("");
const mblNoLocked = ref(false);

/* 약관 */
const termsList = ref([]);
const allAgreed = ref(false);

/* 인증 타이머 */
const timer = ref(0);
let timerInterval = null;

/* focus 용 ref */
const userIdRef = ref(null);
const mblNoRef = ref(null);

/* ============ Life Cycle ============ */
onMounted(async () => {
  // joinCd 파싱: {회사코드}-{사업장코드5자리}
  const joinCd = route.params.joinCd || "";
  const parts = String(joinCd).split("-");

  if (
    parts.length !== 2 ||
    proxy.$util.isEmpty(parts[0]) ||
    parts[1].length !== 5
  ) {
    isLinkInvalid.value = true;
    isLoading.value = false;
    return;
  }

  cmpnyCd.value = parts[0];
  siteCd.value = parts[1];

  // 회사/사업장 정보 조회
  const siteOk = await fnGetSiteInfo();
  if (!siteOk) {
    isLinkInvalid.value = true;
    isLoading.value = false;
    return;
  }

  // 약관 목록 조회
  await fnGetTermsList();

  isLoading.value = false;
});

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
});

/* ============ API Functions ============ */

/** 회사/사업장 정보 조회. 성공 시 true. */
const fnGetSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/dailyJoin/site-infos", {
      params: {
        cmpnyCd: cmpnyCd.value,
        siteCd: siteCd.value,
      },
    });

    if (response.status === 200 && response.data) {
      cmpnyNm.value = response.data.cmpnyNm || "";
      siteNm.value = response.data.siteNm || "";
      return true;
    }
    return false;
  } catch (err) {
    console.error("[DailyUserJoin] site-infos 조회 실패", err);
    return false;
  }
};

/** 약관 목록 조회 (SYS008). */
const fnGetTermsList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS008"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      // 약관 버전은 서버가 신뢰값으로 저장하므로 화면에서는 termsId 만 사용한다.
      termsList.value = resData
        .filter((o) => o.systValDCd != null)
        .map((o) => ({
          ...o,
          checked: false,
        }));
    }
  } catch (err) {
    proxy.$alert(
      resolveApiErrorMessage(err, "약관 조회 중 오류가 발생했습니다.")
    );
  }
};

/** 사용자ID 중복확인. */
const handleUserIdDupleCheck = async () => {
  if (proxy.$util.isEmpty(userId.value)) {
    await proxy.$alert("아이디를 입력해주세요.");
    if (userIdRef.value) userIdRef.value.focus();
    return;
  }

  if (userId.value.length < 4 || userId.value.length > 10) {
    await proxy.$alert("아이디는 4 ~ 10자 사이로 입력해주세요.");
    return;
  }

  try {
    const response = await axios.get("/comApi/dailyJoin/user-id-duple-checks", {
      params: {
        cmpnyCd: cmpnyCd.value,
        userId: proxy.$util.toUpperCase(userId.value),
      },
    });

    if (response.status === 200) {
      if (response.data?.uniqueYn === "Y") {
        userId.value = proxy.$util.toUpperCase(userId.value);
        checkedUserId.value = userId.value;
        userIdChecked.value = true;
        userIdMsg.value = "✅ 사용 가능한 아이디입니다.";
      } else {
        userIdChecked.value = false;
        userIdMsg.value = "❌ 이미 사용중인 아이디입니다.";
        await proxy.$alert("이미 사용중인 아이디입니다.");
      }
    }
  } catch (err) {
    userIdChecked.value = false;
    proxy.$alert(resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다."));
  }
};

/** 아이디 입력값 변경 시 중복확인 상태 리셋 (확인 후 ID 변조 방지). */
watch(userId, (newVal) => {
  // 중복확인 통과 시 toUpperCase 재대입으로 인한 불필요한 리셋 방지
  if (newVal === checkedUserId.value) {
    return;
  }
  userIdChecked.value = false;
  userIdMsg.value = "";
});

/** SMS 인증번호 발송 요청. */
const handleSmsAuthReq = async () => {
  if (
    proxy.$util.isEmpty(mblNo.value) ||
    !proxy.$util.validatePhoneNumber(mblNo.value)
  ) {
    await proxy.$alert("휴대폰 번호를 확인해주세요.");
    mblNo.value = "";
    if (mblNoRef.value) mblNoRef.value.focus();
    return;
  }

  // 입력값 포맷 정규화 (하이픈)
  mblNo.value = proxy.$util.formatPhoneNumber(mblNo.value);

  try {
    const response = await axios.post("/comApi/baseinfo/sms-auth-sends", {
      cmpnyCd: cmpnyCd.value,
      mblNo: mblNo.value,
      dupChkYn: "Y",
    });

    if (response.status === 200) {
      await proxy.$alert("인증번호가 발송되었습니다.");

      // 60초 재요청 타이머
      timer.value = 60;
      if (timerInterval) clearInterval(timerInterval);
      timerInterval = setInterval(() => {
        timer.value--;
        if (timer.value <= 0) {
          clearInterval(timerInterval);
        }
      }, 1000);
    }
  } catch (err) {
    mblNo.value = "";
    proxy.$alert(resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다."));
  }
};

/** SMS 인증번호 확인. */
const handleSmsAuthChk = async () => {
  if (proxy.$util.isEmpty(certNo.value)) {
    await proxy.$alert("인증번호를 입력해주세요.");
    return;
  }
  if (proxy.$util.isEmpty(mblNo.value)) {
    await proxy.$alert("휴대폰 번호를 입력해주세요.");
    return;
  }

  try {
    const response = await axios.post("/comApi/baseinfo/sms-auth-checks", {
      cmpnyCd: cmpnyCd.value,
      mblNo: mblNo.value,
      certNo: certNo.value,
    });

    if (response.status === 200) {
      smsVerified.value = true;
      mblNoLocked.value = true;
      smsAuthMsg.value = "✅ 인증이 완료되었습니다.";
      await proxy.$alert("인증번호가 확인되었습니다.");
    }
  } catch (err) {
    smsAuthMsg.value = "❌ 인증에 실패했습니다.";
    proxy.$alert(resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다."));
  }
};

/** 전체 동의 토글. */
const handleAllAgree = () => {
  termsList.value.forEach((terms) => {
    terms.checked = allAgreed.value;
  });
};

/** 약관 상세 보기 팝업. */
const handleViewTerms = (terms) => {
  openPop(TermsDetailPop, {
    termsId_p: terms.systValDCd,
    termsNm_p: terms.systValDNm,
  });
};

/** 회원가입 제출. */
const handleSubmit = async () => {
  if (!fnValidateInput()) {
    return;
  }

  const agrTermsList = termsList.value.map((terms) => ({
    termsId: terms.systValDCd,
  }));

  try {
    const response = await axios.post("/comApi/dailyJoin/insert-daily-user", {
      cmpnyCd: cmpnyCd.value,
      siteCd: siteCd.value,
      userId: userId.value,
      userNm: userNm.value,
      mblNo: mblNo.value,
      certNo: certNo.value,
      agrTermsList: agrTermsList,
    });

    if (response.status === 200) {
      joinedUserId.value = response.data?.userId || userId.value;
      isJoined.value = true;
    }
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "회원가입에 실패했습니다."));
  }
};

/* ============ 입력 검증 ============ */
function fnValidateInput() {
  if (!userIdChecked.value) {
    proxy.$alert("아이디 중복확인을 완료해주세요.");
    return false;
  }
  if (proxy.$util.isEmpty(userNm.value)) {
    proxy.$alert("이름을 입력해주세요.");
    return false;
  }
  if (!smsVerified.value) {
    proxy.$alert("휴대폰 번호 인증을 완료해주세요.");
    return false;
  }
  if (termsList.value.length === 0) {
    proxy.$alert(
      "약관 정보를 불러오지 못했습니다.\n새로고침 후 다시 시도해주세요."
    );
    return false;
  }
  const allChecked = termsList.value.every((terms) => terms.checked);
  if (!allChecked) {
    proxy.$alert("필수 약관에 모두 동의해주세요.");
    return false;
  }
  return true;
}
</script>

<style scoped>
/* 화면 높이를 뷰포트로 고정하고, 내용이 넘치면 세로 스크롤바를 노출한다
   (폼이 길어져 회원가입 버튼이 잘리는 현상 방지) */
.daily-join {
  height: 100vh;
  overflow-y: auto;
  box-sizing: border-box;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--card-padding);
  font-family: "Pretendard", sans-serif;
}
.daily-join__header {
  width: 100%;
  max-width: 480px;
  text-align: center;
  padding: var(--card-padding) 0;
}
.daily-join__title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-strong);
}
.daily-join__error,
.daily-join__done {
  width: 100%;
  max-width: 480px;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: var(--card-padding);
  text-align: center;
  color: var(--color-text);
}
.daily-join__error-desc,
.daily-join__done-desc {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
.daily-join__done-mark {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-primary);
}
.daily-join__done-id {
  margin-top: 0.75rem;
  color: var(--color-text-strong);
}
.daily-join__form {
  width: 100%;
  max-width: 480px;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: var(--card-padding);
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.daily-join__row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.daily-join__label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--color-text-strong);
}
.daily-join__field {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}
.daily-join__input {
  flex: 1;
  min-width: 0;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  padding: 0.45rem 0.8rem;
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}
.daily-join__input::placeholder {
  color: var(--color-text-muted);
}
.daily-join__input:focus {
  border-color: var(--color-border-strong);
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
  outline-offset: var(--outline-offset);
}
.daily-join__input:disabled {
  background: var(--color-bg);
  color: var(--color-text-muted);
  cursor: not-allowed;
}
.daily-join__msg {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  min-height: 1rem;
}
.daily-join__divider {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0.3rem 0;
}
.daily-join__terms {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.daily-join__terms-all {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--color-text-strong);
  cursor: pointer;
}
.daily-join__terms-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-left: 0.5rem;
}
.daily-join__terms-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.daily-join__terms-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-text);
  cursor: pointer;
}
.daily-join__terms-view {
  background: none;
  border: none;
  color: var(--color-primary);
  font-size: 0.8rem;
  cursor: pointer;
  text-decoration: underline;
}
.daily-join__submit {
  width: 100%;
  margin-top: 0.5rem;
}
@media (max-width: 480px) {
  .daily-join {
    padding: 0.75rem;
  }
  .daily-join__form,
  .daily-join__error,
  .daily-join__done {
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
