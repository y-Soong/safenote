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
            :key="terms.SYST_VAL_D_CD"
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
import TermsDetailPop from "@/components/popup/TermsDetailPop.vue";
import JoinUserPop from "@/components/popup/JoinUserPop.vue";

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
const systCodeArr = ref({});
const checked = ref(false);
const termsList = ref([]);
const btnName = ref("회원가입");
const userTermsNonAgrList = ref({});

// ================ Life Cycle Functions ================
onMounted(async () => {
  if (proxy.$util.isNotEmpty(props.loginFlg_p)) {
    btnName.value = "약관동의";
    userTermsNonAgrList.value = props.userTermsNonAgrList_p;
  }
  await fnGetSystinfoList();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS008"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      systCodeArr.value = grouped;

      if (proxy.$util.isNotEmpty(userTermsNonAgrList.value)) {
        termsList.value = (grouped["SYS008"] || [])
          .filter((o) => o.systValDCd != null)
          .map((o) => ({
            ...o,
            checked: false, // 각 항목별 체크 상태 추가
          }))
          .filter((sys) =>
            userTermsNonAgrList.value.some(
              (terms) => terms.TERMS_ID === sys.systValDCd
            )
          );
      } else {
        termsList.value = (grouped["SYS008"] || [])
          .filter((o) => o.systValDCd != null)
          .map((o) => ({
            ...o,
            checked: false, // 각 항목별 체크 상태 추가
          }));
      }
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

const fnUpdateUserTermsAgrList = async () => {
  const filteredMenu = termsList.value;

  if (filteredMenu.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/comApi/login/updateAuthMenuInfo",
      filteredMenu
    );

    if (response.status === 200) {
      proxy.$alert("이용약관이 업데이트 됐습니다.");
      props.onMoveMain();
      emit("close");
    }
  } catch (err) {
    proxy.$alert(err.response.data.message);
  }
};

// ================ Methods/Functions ================
const fnClose = () => {
  if (proxy.$util.isNotEmpty(props.loginFlg_p)) {
    props.onUserLogout();
    proxy.$alert("필수약관 미동의 시 서비스 이용이 불가합니다.");
  }
  emit("close");
};

const fnJoinUser = () => {
  const joinFlg = termsList.value.every((terms) => terms.checked);

  if (joinFlg) {
    if (proxy.$util.isNotEmpty(props.loginFlg_p)) {
      fnUpdateUserTermsAgrList();
    } else {
      openPop(JoinUserPop, {});
      emit("close");
    }
  } else {
    proxy.$alert("동의하지 않은 필수 약관이 있습니다.");
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
