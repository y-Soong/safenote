<template>
  <div class="min-h-screen flex flex-col bg-white">
    <div class="flex items-center p-4 mt-10">
      <button @click="goBack" class="mr-2 text-gray-600 hover:text-gray-900">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-8 w-8"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M15 19l-7-7 7-7"
          />
        </svg>
      </button>
    </div>

    <!-- 전체동의하기 -->
    <div class="flex-1 px-6">
      <h3 class="text-gray-800 font-semibold mb-6 text-2xl">약관에 동의해주세요</h3>

      <label class="flex p-4 bg-gray-100 rounded-md">
        <input type="checkbox" class="hidden" v-model="checked" @click="fnAllClick" />
        <span
          class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2 rounded-md"
          :class="checked ? 'bg-green-500 border-green-500' : 'bg-transparent'"
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

      <!-- 세부약관 내용 -->
      <div class="form-container pt-5">
        <label
          class="flex items-center cursor-pointer select-none mb-4"
          v-for="terms in termsList"
          :key="terms.SYST_VAL_D_CD"
        >
          <input type="checkbox" v-model="terms.checked" class="hidden" />
          <span
            class="w-6 h-6 flex items-center justify-center border-2 border-gray-400 transition-all duration-200 mr-2"
            :class="terms.checked ? 'bg-green-500 border-green-500' : 'bg-transparent'"
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
          <span>{{ '(필수) ' + terms.SYST_VAL_D_NM }}</span>

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

    <!-- 하단 버튼 영역 -->
    <div class="p-6">
      <button
        class="w-full bg-green-700 text-white py-3 rounded-md hover:bg-green-800 transition"
        @click="fnJoinUser()"
      >
        다음
      </button>
    </div>
  </div>
</template>
<script setup>
/* eslint-disable */
import { ref, getCurrentInstance, onMounted, defineEmits } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'

const { proxy } = getCurrentInstance()
const emit = defineEmits(['close'])

const router = useRouter()

const systCodeArr = ref({});
const termsList = ref([]);

const checked = ref(false);

onMounted(async () => {
  await fnGetSystinfoList();
});

// API 호출
const fnGetSystinfoList = async () => {
  const systCodeList = ["SYS008"];

  try {
    const response = await axios.post("/comApi/baseinfo/getSystinfoList", {
      systCodeList: systCodeList,
    });

    if (response.status === 200) {
      const grouped = {};
      response.data.forEach((item) => {
        if (proxy.$util.isNotEmpty(item.SYST_VAL_D_CD)) {
          const key = item.SYST_VAL_CD;
          if (!grouped[key]) {
            grouped[key] = [];
          }
          grouped[key].push(item);
        }
      });

      systCodeArr.value = grouped;

      termsList.value = (grouped["SYS008"] || [])
        .filter((o) => o.SYST_VAL_D_CD != null)
        .map((o) => ({
          ...o,
          checked: false, // 각 항목별 체크 상태 추가
        }));
    }
  } catch (err) {
    proxy.$alert(err.response.data.message);
  }
};


/* User Function */
function fnJoinUser() {
  let joinFlg = true;

  termsList.value.forEach((terms) => {
    if (!terms.checked) joinFlg = false;
  });

  if (joinFlg) {
    router.push({
      path: '/JoinUser',
      query: {
        termsList_p: termsList.value,
      },
    })
  } else {
    proxy.$alert("동의하지 않은 필수 약관이 있습니다.");
  }

}

function fnAllClick() {
  termsList.value.forEach((terms) => {
    terms.checked = !checked.value;
  });
}

function fnViewTerms(terms) {
  router.push({
    path: '/TermsDetail',
    query: {
      termsId_p: terms.SYST_VAL_D_CD,
      termsNm_p: terms.SYST_VAL_D_NM,
    },
  })
}

const goBack = () => {
  router.back()
}
</script>
