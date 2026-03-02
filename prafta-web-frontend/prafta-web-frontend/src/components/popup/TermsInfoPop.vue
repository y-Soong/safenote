<template>
  <div class="modal-overlay prafta-modal-popup min-h-screen">
    <div
      class="modal-content-wide"
      :style="{ top: position.y + 'px', left: position.x + 'px' }"
      ref="modalRef"
    >
      <div class="modal-header" @mousedown="startDrag">
        <span>{{ props.termsNm_p }}</span>
        <button class="icon-button" @click="$emit('close')">✕</button>
      </div>

      <!-- 약관 기본정보 -->
      <div class="form-container ml-5 mr-5">
        <div class="form-row-max mt-5">
          <label>약관명</label>
          <input
            v-model="termsNm"
            placeholder="예: 서비스이용약관"
            :readonly="readOnlyFlg"
          />

          <label class="font-semibold">약관버전</label>
          <input v-model="termsVersion" class="input" readonly />
        </div>

        <div class="form-row-max mt-5">
          <label>필수여부</label>
          <div class="form-row-max-control">
            <BaseSelect id="useYn" v-model="requiredYn" :readonly="readOnlyFlg">
              <option
                v-for="opt in systCodeArr['SYS003'] || []"
                :key="opt.systValDCd"
                :value="opt.systValDCd"
              >
                {{ opt.systValDNm }}
              </option>
            </BaseSelect>
          </div>

          <label>시행일자</label>
          <div class="form-row-max-control">
            <CalendarSrch
              style="width: 100%; height: 2rem"
              v-model="strDate"
              :readonly="readOnlyFlg"
            />
          </div>
        </div>

        <!-- 약관 본문 에디터 -->
        <div class="mt-5">
          <label>약관 본문</label>
          <QuillEditor
            v-model:content="termsContent"
            contentType="html"
            theme="snow"
            style="height: 20rem"
            ref="editorRef"
          />
        </div>

        <div class="form-row-max mt-5">
          <label>비고</label>
          <input
            v-model="termsDesc"
            placeholder="비고 입력"
            :readonly="readOnlyFlg"
          />
        </div>
      </div>

      <div class="modal-footer">
        <div class="btn-group">
          <button class="btn btn-primary" @click="fnSaveTerms">저장</button>
          <button class="btn btn-primary" @click="$emit('close')">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  getCurrentInstance,
  defineEmits,
  onMounted,
  defineProps,
  watch,
} from "vue";
import { QuillEditor } from "@vueup/vue-quill";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import CalendarSrch from "@/components/common/CalendarSrch";
import axios from "@/api/axios";
import BaseSelect from "@/components/common/BaseSelect.vue";

const modalRef = ref(null);
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();
const props = defineProps({
  termsId_p: String,
  termsNm_p: String,
  termsVersion_p: String,
  onSearch: Function,
  onSubSearch: Function,
});
const systCodeArr = ref({});
// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const cmpnyCd = ref("");
const termsId = ref("");
const termsNm = ref("");
const termsVersion = ref("");
const requiredYn = ref("Y");
const strDate = ref("");
const termsDesc = ref("");
const termsContent = ref("");
const readOnlyFlg = ref(false);
const editorRef = ref(null);

watch(readOnlyFlg, (newVal) => {
  if (editorRef.value?.getQuill) {
    const quill = editorRef.value.getQuill();
    quill.enable(!newVal); // true → 편집 가능, false → 읽기 전용
  }
});

onMounted(async () => {
  cmpnyCd.value = sessionStorage.getItem("gv_cmpnyCd");
  termsId.value = props.termsId_p;
  termsNm.value = props.termsNm_p;
  requiredYn.value = "Y";

  if (proxy.$util.isEmpty(props.termsVersion_p)) {
    termsVersion.value = "";
    strDate.value = proxy.$util.getToday();
  } else {
    termsVersion.value = props.termsVersion_p;
    fnGetTermsInfo();
  }

  await fnGetSystinfoList();
});

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS003"],
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
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

const fnGetTermsInfo = async () => {
  try {
    const response = await axios.get("/webApi/baim03/terms-detail-info-list", {
      params: {
        termsId: termsId.value,
        termsVersion: termsVersion.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      if (response.data.termsDetailInfoList.length == 1) {
        termsContent.value = response.data.termsDetailInfoList[0].termsContent;
        strDate.value = proxy.$util.formatDateString(
          response.data.termsDetailInfoList[0].strDate
        );
        termsDesc.value = response.data.termsDetailInfoList[0].termsDesc;
        if (
          response.data.termsDetailInfoList[0].strDate <=
          proxy.$util.getToday().replaceAll("-", "")
        ) {
          readOnlyFlg.value = true;
        }
      } else {
        const alertMsg =
          "약관조회 중 오류가 발생했습니다.\n관리자에게 문의해주세요.";
        fnAlertMsg(alertMsg, () => {
          emit("close");
          props.onSearch();
          props.onSubSearch();
        });
      }
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

const fnSaveTerms = async () => {
  fnConfirmMsg("저장하시겠습니까 ?", async () => {
    try {
      const response = await axios.post("/webApi/baim03/updateTermsInfo", {
        termsId: termsId.value,
        termsNm: termsNm.value,
        requiredYn: requiredYn.value,
        termsContent: termsContent.value,
        strDate: strDate.value,
        termsDesc: termsDesc.value,
      });

      if (response.status === 200) {
        const alertMsg = "처리됐습니다.";
        fnAlertMsg(alertMsg, () => {
          emit("close");
          props.onSearch();
          props.onSubSearch();
        });
      }
    } catch (err) {
      alert(err.response.data.message);
    }
  });
};

/* User Function */
async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}

async function fnConfirmMsg(message, afterConfirmCallback) {
  const result = await proxy.$confirm(message);
  if (result && afterConfirmCallback) {
    afterConfirmCallback(); // ✅ 확인 눌렀을 때만 실행
  }
}
</script>

<style scoped>
/* 약관명/약관버전 행과 동일한 가로 비율(필수여부·시행일자) */
.form-row-max-control {
  flex: 1 1 150px;
  min-width: 0;
}

.input {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px;
  width: 100%;
}
.btn-save {
  background-color: #0d9488;
  color: white;
  padding: 10px 16px;
  border-radius: 6px;
}
.btn-cancel {
  background-color: #ddd;
  padding: 10px 16px;
  border-radius: 6px;
}
</style>
