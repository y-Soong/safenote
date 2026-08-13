<template>
  <div class="modal-overlay prafta-modal-popup min-h-screen">
    <div
      class="modal-content-narrow"
      :style="{ top: position.y + 'px', left: position.x + 'px' }"
      ref="modalRef"
    >
      <div class="modal-header" @mousedown="startDrag">
        <span>이용약관</span>
        <button class="icon-button" @click="fnClose">✕</button>
      </div>

      <!-- 약관 기본정보 -->
      <div class="form-container pl-5 pt-5">
        <!-- 약관 본문 에디터 -->
        <label class="flex items-center cursor-pointer select-none pb-2">
          <input
            type="checkbox"
            v-model="checked"
            class="hidden"
            @click="fnAllClick"
          />
          <span
            class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2"
            :class="
              checked ? 'bg-green-500 border-green-500' : 'bg-transparent'
            "
          >
            <svg
              v-if="checked"
              xmlns="http://www.w3.org/2000/svg"
              class="h-4 w-4 text-white"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="3"
                d="M5 13l4 4L19 7"
              />
            </svg>
          </span>
          <span>전체 동의하기</span>
        </label>

        <div class="form-container pl-5">
          <label
            class="flex items-center cursor-pointer select-none mb-3"
            v-for="terms in termsList"
            :key="terms.systValDCd"
          >
            <input type="checkbox" v-model="terms.checked" class="hidden" />
            <span
              class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2"
              :class="
                terms.checked
                  ? 'bg-green-500 border-green-500'
                  : 'bg-transparent'
              "
            >
              <svg
                v-if="terms.checked"
                xmlns="http://www.w3.org/2000/svg"
                class="h-4 w-4 text-white"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="3"
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </span>
            <span>{{ "(필수) " + terms.systValDNm }}</span>

            <!-- 오른쪽 영역: (보기) 버튼 -->
            <button
              type="button"
              class="text-green-600 hover:underline"
              @click.stop="fnViewTerms(terms)"
            >
              (보기)
            </button>
          </label>
        </div>
      </div>

      <div class="modal-footer">
        <div class="btn-group">
          <button class="w-full btn btn-primary" @click="fnJoinUser">
            {{ btnName }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  getCurrentInstance,
  defineEmits,
  onMounted,
  defineProps,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import TermsDetailPop from "@/components/popup/TermsDetailPop.vue";
import JoinUserPop from "@/views/login/popup/JoinUserPop.vue";

// ================ Props & Emits ================
const props = defineProps({
  loginFlg_p: String,
  userTermsNonAgrList_p: Object,
  onMoveMain: Function,
  onUserLogout: Function,
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Refs (Variables) ================
const checked = ref(false);
const termsList = ref([]);
const btnName = ref("회원가입");
const userTermsNonAgrList = ref({});

// ================ Life Cycle Functions ================
onMounted(async () => {
  if (proxy.$util.isNotEmpty(props.loginFlg_p)) {
    btnName.value = "약관동의";
    console.log(props.userTermsNonAgrList_p);
    userTermsNonAgrList.value = props.userTermsNonAgrList_p;
  }
  await fnGetSystinfoList();
});

// ================ API Functions ================
//
// ★필수약관 목록은 서버(TB_TERMS.REQUIRED_YN='Y')가 판정한다.
//   종전에는 SYS008 코드표(syst-info-lists)를 받아 "SYS008 에 있으면 전부 필수"로 그렸다.
//   그 탓에 선택약관인 006(연동 회사 제3자 제공 동의)까지 필수 체크를 강요했고, 006 은
//   가입 시 저장되지 않는 약관이라 로그인 후 게이트가 다시 물었다(앱 TermsInfo.vue 와 동일 결함).
//   여기를 코드표 기반으로 되돌리지 말 것(중복 동의 재발).
//
//   화면·저장(update-auth-menu-info) 계약은 systValDCd/systValDNm 필드명을 쓰므로
//   응답을 그 형태로 정규화해 넘긴다(계약 변경 없음).
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/join-terms-lists");

    if (response.status === 200) {
      const requiredTerms = (response.data?.joinTermsList || []).map((o) => ({
        systValDCd: o.termsId,
        systValDNm: o.termsNm,
        termsVersion: o.termsVersion,
        checked: false, // 각 항목별 체크 상태 추가
      }));

      if (proxy.$util.isNotEmpty(userTermsNonAgrList.value)) {
        // 로그인 후 미동의 약관 재동의 분기 — 서버가 준 미동의 목록으로 한 번 더 좁힌다.
        termsList.value = requiredTerms.filter((sys) =>
          userTermsNonAgrList.value.some(
            (terms) => terms.termsId === sys.systValDCd
          )
        );
      } else {
        termsList.value = requiredTerms;
      }
    }
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

const fnUpdateUserTermsAgrList = async () => {
  const filteredMenu = termsList.value;

  if (filteredMenu.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  console.log(filteredMenu);

  try {
    const response = await axios.post(
      "/comApi/login/update-auth-menu-info",
      filteredMenu
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.TERMS_UPDATE_SUCCESS));
      props.onMoveMain();
      emit("close");
    }
  } catch (err) {
    proxy.$alert(resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다."));
  }
};

// ================ Methods/Functions ================
const fnClose = () => {
  if (proxy.$util.isNotEmpty(props.loginFlg_p)) {
    props.onUserLogout();
    proxy.$alert(getMessage(MSG.TERMS_REQUIRED));
  }
  emit("close");
};

const fnJoinUser = () => {
  // 목록이 비면(조회 실패) 통과시키지 않는다 — every() 는 빈 배열에서 true 라 약관을
  //   하나도 못 본 채 가입 화면으로 넘어갔다(fail-open). 앱 TermsInfo.vue 와 동일 보강.
  if (termsList.value.length === 0) {
    proxy.$alert("약관 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    return;
  }

  const joinFlg = termsList.value.every((terms) => terms.checked);

  if (joinFlg) {
    if (proxy.$util.isNotEmpty(props.loginFlg_p)) {
      fnUpdateUserTermsAgrList();
    } else {
      openPop(JoinUserPop, {});
      emit("close");
    }
  } else {
    proxy.$alert(getMessage(MSG.TERMS_AGREEMENT_REQUIRED));
  }
};

const fnAllClick = () => {
  termsList.value.forEach((terms) => {
    terms.checked = !checked.value;
  });
};

const fnViewTerms = (terms) => {
  openPop(TermsDetailPop, {
    termsId_p: terms.systValDCd,
    termsNm_p: terms.systValDNm,
  });
};
</script>

<style scoped></style>
