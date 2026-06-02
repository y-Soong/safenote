<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>내 정보</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- 사용자 기본정보 -->
        <div class="section-title">기본 정보</div>
        <div class="form-container">
          <div class="form-row-max">
            <label>아이디</label>
            <input v-model="userId" disabled />
          </div>
          <div class="form-row-max">
            <label>이름</label>
            <input v-model="userNm" disabled />
          </div>
          <div class="form-row-max">
            <label>사업장</label>
            <input v-model="siteNm" disabled />
          </div>
          <div class="form-row-max">
            <label>소속부서</label>
            <input v-model="nodeNm" disabled />
          </div>
          <div class="form-row-max">
            <label>휴대폰</label>
            <input v-model="mblNo" disabled />
          </div>
          <div class="form-row-max">
            <label>이메일</label>
            <input v-model="email" disabled />
          </div>
          <div class="form-row-max">
            <label>마지막 로그인</label>
            <input v-model="lastLoginDtime" disabled />
          </div>
        </div>

        <!-- 비밀번호 변경 -->
        <div class="section-title">비밀번호 변경</div>
        <div class="form-container">
          <div class="form-row-max">
            <label>현재 비밀번호</label>
            <input
              type="password"
              v-model="currentPw"
              placeholder="현재 비밀번호"
              autocomplete="current-password"
            />
          </div>
          <div class="form-row-max">
            <label>새 비밀번호</label>
            <input
              type="password"
              v-model="newPw"
              placeholder="8자 이상, 영문+숫자+특수문자"
              autocomplete="new-password"
            />
          </div>
          <div class="form-row-max">
            <label>비밀번호 확인</label>
            <input
              type="password"
              v-model="newPwConfirm"
              placeholder="새 비밀번호 재입력"
              autocomplete="new-password"
            />
          </div>
        </div>

        <!-- 푸터 버튼 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnChangePassword">
              비밀번호 변경
            </button>
            <button class="btn btn-withdrawal" @click="fnSelfWithdrawal">
              회원탈퇴
            </button>
            <button class="btn btn-default" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useUserStore } from "@/stores/userStore";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const modalRef = ref(null);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3,
});

// 기본정보
const userId = ref("");
const userNm = ref("");
const siteNm = ref("");
const nodeNm = ref("");
const mblNo = ref("");
const email = ref("");
const lastLoginDtime = ref("");

// 비밀번호 변경
const currentPw = ref("");
const newPw = ref("");
const newPwConfirm = ref("");

onMounted(async () => {
  await fnLoadMyInfo();
});

const fnLoadMyInfo = async () => {
  // userStore에서 기본값 먼저 세팅 (비-PII만)
  // 정책 §11.1에 따라 휴대폰/이메일은 store/sessionStorage에 두지 않으므로,
  // 아래 API 응답으로만 채워진다.
  userId.value =
    userStore.gv_userId || sessionStorage.getItem("gv_userId") || "";
  userNm.value =
    userStore.gv_userNm || sessionStorage.getItem("gv_userNm") || "";
  siteNm.value =
    userStore.gv_siteNm || sessionStorage.getItem("gv_siteNm") || "";

  // 본인 전용 API로 최신 정보 조회.
  // 조회 대상은 서버가 토큰으로만 결정하므로 클라이언트 식별자(params) 미전달 (IDOR 방지).
  try {
    const response = await axios.get("/webApi/user01/my-profile");
    if (response.status === 200 && response.data) {
      const info = response.data;
      userId.value = info.userId || userId.value;
      userNm.value = info.userNm || userNm.value;
      siteNm.value = info.siteNm || siteNm.value;
      nodeNm.value = info.nodeNm || "";
      mblNo.value = proxy.$util?.formatPhoneNumber
        ? proxy.$util.formatPhoneNumber(info.mblNo)
        : info.mblNo || "";
      email.value = info.email || "";
      if (info.lastLoginDtime) {
        lastLoginDtime.value = info.lastLoginDtime
          .replace("T", " ")
          .substring(0, 19);
      }
    }
  } catch {
    // 조회 실패 시 userStore 값으로 대체 (이미 세팅됨)
  }
};

const fnSelfWithdrawal = async () => {
  const confirmed = await proxy.$confirm(
    getMessage(MSG.MY_INFO_WITHDRAWAL_CONFIRM)
  );
  if (!confirmed) return;

  try {
    // 탈퇴 대상은 서버가 토큰으로만 결정한다. 식별자 파라미터는 더 이상 전송하지 않는다.
    await axios.post("/webApi/user01/withdraw-my-account", {});
    await proxy.$alert(getMessage(MSG.MY_INFO_WITHDRAWAL_SUCCESS));
    sessionStorage.clear();
    window.location.replace("/safenote");
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      getMessage(MSG.MY_INFO_WITHDRAWAL_FAILED)
    );
    await proxy.$alert(msg);
  }
};

const fnChangePassword = async () => {
  if (!currentPw.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_CURRENT_PW_REQUIRED));
    return;
  }
  if (!newPw.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_NEW_PW_REQUIRED));
    return;
  }
  if (newPw.value !== newPwConfirm.value) {
    await proxy.$alert(getMessage(MSG.MY_INFO_PW_MISMATCH));
    return;
  }
  if (newPw.value.length < 8) {
    await proxy.$alert(getMessage(MSG.MY_INFO_PW_TOO_SHORT));
    return;
  }

  const confirmed = await proxy.$confirm(
    getMessage(MSG.MY_INFO_PW_CHANGE_CONFIRM)
  );
  if (!confirmed) return;

  try {
    const response = await axios.post("/webApi/user01/update-my-passwd", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      userCd: sessionStorage.getItem("gv_userCd"),
      currentPw: currentPw.value,
      newPw: newPw.value,
    });
    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.MY_INFO_PW_CHANGED));
      currentPw.value = "";
      newPw.value = "";
      newPwConfirm.value = "";
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      getMessage(MSG.MY_INFO_PW_CHANGE_FAILED)
    );
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
.section-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-muted, #6b7280);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.75rem 1.2rem 0;
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.section-title:first-of-type {
  border-top: none;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding: 0.75rem 1.2rem 1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0.75rem 1.2rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}

.btn-withdrawal {
  background-color: #ef4444;
  color: #fff;
  border: none;
  padding: 0.35rem 0.85rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}
.btn-withdrawal:hover {
  background-color: #dc2626;
}
</style>
