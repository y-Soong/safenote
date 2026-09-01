<template>
  <div class="daily-join" ref="rootRef">
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
      <!-- 입장 승인제(R4): pendingApprovalYn='Y' 응답이면 승인 대기 안내로 분기(플래그 부재/N 은 기존 문구 폴백) -->
      <p v-if="pendingApproval" class="daily-join__done-desc">
        가입이 완료되었습니다. 관리자 승인 후 해당 아이디로 로그인할 수
        있습니다. 문의는 사업장 관리자에게 해주세요.
      </p>
      <p v-else class="daily-join__done-desc">
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
        <label class="daily-join__label">비밀번호</label>
        <input
          class="daily-join__input"
          type="password"
          v-model="password"
          ref="passwordRef"
          minlength="6"
          maxlength="15"
          placeholder="6 ~ 15자, 영문/숫자/특수문자 중 2종 이상"
          autocomplete="new-password"
        />
        <span class="daily-join__msg">{{ passwordMsg }}</span>
      </div>

      <div class="daily-join__row">
        <label class="daily-join__label">비밀번호 확인</label>
        <input
          class="daily-join__input"
          type="password"
          v-model="passwordConfirm"
          ref="passwordConfirmRef"
          minlength="6"
          maxlength="15"
          placeholder="비밀번호를 한 번 더 입력하세요"
          autocomplete="new-password"
        />
        <span class="daily-join__msg">{{ passwordConfirmMsg }}</span>
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
              <span
                >({{ terms.requiredYn === "N" ? "선택" : "필수" }})
                {{ terms.systValDNm }}</span
              >
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
// 입장 승인제(D5/D6) — 가입 직후 계정은 승인대기('04'). 서버 pendingApprovalYn='Y' 소비(qa M-2).
const pendingApproval = ref(false);

/* 회사/사업장 정보 (joinCd 에서 파싱) */
const cmpnyCd = ref("");
const siteCd = ref("");
const cmpnyNm = ref("");
const siteNm = ref("");

/* 입력값 */
const userId = ref("");
const password = ref("");
const passwordConfirm = ref("");
const userNm = ref("");
const mblNo = ref("");
const certNo = ref("");

/* 검증 상태 */
const userIdChecked = ref(false);
const userIdMsg = ref("");
const checkedUserId = ref(""); // 중복확인을 통과한 아이디 값
const passwordMsg = ref("");
const passwordConfirmMsg = ref("");
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
const passwordRef = ref(null);
const passwordConfirmRef = ref(null);
const mblNoRef = ref(null);

/* 루트 컨테이너 ref (모바일 가시영역 높이 주입용) */
const rootRef = ref(null);

/* 모바일 동적 주소창 대응: 실제 보이는 높이(window.innerHeight)를 CSS 변수(--djoin-h)로 주입한다.
   주소창이 노출/숨김되면 resize 가 발생해 다시 측정 → 컨테이너 높이가 항상 가시영역과 일치하여
   내부 스크롤로 하단(약관·회원가입 버튼)까지 도달 가능. */
function setViewportHeight() {
  if (rootRef.value) {
    rootRef.value.style.setProperty("--djoin-h", `${window.innerHeight}px`);
  }
}

/* ============ Life Cycle ============ */
onMounted(async () => {
  setViewportHeight();
  window.addEventListener("resize", setViewportHeight);
  window.addEventListener("orientationchange", setViewportHeight);

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
  window.removeEventListener("resize", setViewportHeight);
  window.removeEventListener("orientationchange", setViewportHeight);
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

/**
 * 필수약관 목록 조회.
 *
 * ★필수 여부의 단일 출처는 서버(TB_TERMS.REQUIRED_YN='Y')다. 종전에는 SYS008 코드표
 *   (syst-info-lists)를 받아 "SYS008 에 있으면 전부 필수"로 그려, 선택약관인 006
 *   (연동 회사 제3자 제공 동의)까지 필수 체크를 강요했다. 006 은 제출 목록에 실려도
 *   서버가 필수약관만 저장하므로(DailyJoinServiceImpl.insertTermsAgreement) 버려졌고,
 *   로그인 후 게이트(termsGate ②)가 같은 약관을 다시 물었다.
 *   006 은 연동 사업장 소속자에게만 묻는 게이트 전용 약관이라 가입 화면에서 제외한다.
 *   여기를 코드표 기반으로 되돌리지 말 것(중복 동의 재발).
 *
 * 약관 버전은 서버가 신뢰값으로 저장하므로 화면에서는 termsId 만 사용한다.
 */
const fnGetTermsList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/join-terms-lists");

    if (response.status === 200) {
      // 화면·제출 계약은 systValDCd/systValDNm 필드명을 쓰므로 그 형태로 정규화한다.
      termsList.value = (response.data?.joinTermsList || []).map((o) => ({
        systValDCd: o.termsId,
        systValDNm: o.termsNm,
        // 위치정보 S2 ⑤: 서버가 필수/선택을 함께 내린다. 006(연동 제3자 제공)이 선택으로 섞인다.
        requiredYn: o.requiredYn,
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
        userId: userId.value,
      },
    });

    if (response.status === 200) {
      if (response.data?.uniqueYn === "Y") {
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
  // 중복확인 통과 직후 동일값 재평가로 인한 불필요한 리셋 방지
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
    // SMS-PPURIO-08: 발송 실패인데 입력한 번호를 지워 재입력을 강요하지 않는다(mblNo 초기화 제거).
    proxy.$alert(
      resolveApiErrorMessage(
        err,
        "인증번호 발송에 실패했습니다.\n잠시 후 다시 시도해 주세요."
      )
    );
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

  // ★실제로 체크한 항목만 넘긴다. 선택약관은 동의한 경우에만 서버가 기록한다
  //   (미동의를 기록하면 나중에 연동됐을 때 006 게이트가 다시 뜨지 않는다).
  const agrTermsList = termsList.value
    .filter((terms) => terms.checked)
    .map((terms) => ({
      termsId: terms.systValDCd,
    }));

  try {
    const response = await axios.post("/comApi/dailyJoin/insert-daily-user", {
      cmpnyCd: cmpnyCd.value,
      siteCd: siteCd.value,
      userId: userId.value,
      userPw: password.value,
      userNm: userNm.value,
      mblNo: mblNo.value,
      certNo: certNo.value,
      agrTermsList: agrTermsList,
    });

    if (response.status === 200) {
      joinedUserId.value = response.data?.userId || userId.value;
      pendingApproval.value = response.data?.pendingApprovalYn === "Y";
      isJoined.value = true;
    }
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "회원가입에 실패했습니다."));
  }
};

/* ============ 입력 검증 ============ */
/** 비밀번호 규칙: 6~15자 + 영문/숫자/특수문자 중 2종 이상 (정규 사용자 규칙 미러). */
function fnIsValidPassword(pw) {
  if (pw == null) {
    return false;
  }
  if (pw.length < 6 || pw.length > 15) {
    return false;
  }
  let typeCount = 0;
  if (/[0-9]/.test(pw)) typeCount++;
  if (/[a-zA-Z]/.test(pw)) typeCount++;
  if (/[^a-zA-Z0-9]/.test(pw)) typeCount++;
  return typeCount >= 2;
}

function fnValidateInput() {
  if (!userIdChecked.value) {
    proxy.$alert("아이디 중복확인을 완료해주세요.");
    return false;
  }
  if (proxy.$util.isEmpty(password.value)) {
    proxy.$alert("비밀번호를 입력해주세요.");
    if (passwordRef.value) passwordRef.value.focus();
    return false;
  }
  // 6~15자 + 영문/숫자/특수문자 중 2종 이상 (정규 사용자 규칙, 공통 정책 §3.1)
  if (!fnIsValidPassword(password.value)) {
    proxy.$alert(
      "비밀번호는 6 ~ 15자, 영문/숫자/특수문자 중 2종 이상이어야 합니다."
    );
    if (passwordRef.value) passwordRef.value.focus();
    return false;
  }
  if (proxy.$util.isEmpty(passwordConfirm.value)) {
    proxy.$alert("비밀번호 확인을 입력해주세요.");
    if (passwordConfirmRef.value) passwordConfirmRef.value.focus();
    return false;
  }
  if (password.value !== passwordConfirm.value) {
    proxy.$alert("비밀번호가 일치하지 않습니다.");
    if (passwordConfirmRef.value) passwordConfirmRef.value.focus();
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
  // ★필수 약관만 검증한다. requiredYn==="N"(선택, 006)은 체크하지 않아도 진행할 수 있다.
  const allChecked = termsList.value
    .filter((terms) => terms.requiredYn !== "N")
    .every((terms) => terms.checked);
  if (!allChecked) {
    proxy.$alert("필수 약관에 모두 동의해주세요.");
    return false;
  }
  return true;
}
</script>

<style scoped>
/* 모바일 브라우저(삼성 인터넷/Chrome)의 동적 주소창 때문에 100vh 는 실제 보이는 영역보다
   커서 하단(약관 끝·회원가입 버튼)이 잘린다. 컨테이너 높이를 JS 로 측정한 실제 가시영역
   (--djoin-h = window.innerHeight)에 정확히 맞춘 뒤 내부 세로 스크롤을 허용해, 폼이 길어도
   끝까지 스크롤 도달이 가능하다. 미지원/측정 전에는 dvh→vh 순으로 폴백.
   하단 안전영역(제스처 바/홈 인디케이터)만큼 여백을 더해 버튼이 가려지지 않게 한다. */
.daily-join {
  height: 100vh;
  height: 100dvh;
  height: var(--djoin-h, 100dvh);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  box-sizing: border-box;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--card-padding);
  padding-bottom: calc(var(--card-padding) + env(safe-area-inset-bottom, 0px));
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
    padding-bottom: calc(0.75rem + env(safe-area-inset-bottom, 0px));
  }
  .daily-join__form,
  .daily-join__error,
  .daily-join__done {
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
