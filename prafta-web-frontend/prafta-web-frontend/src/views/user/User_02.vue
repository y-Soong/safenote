<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @Search="fnSearch"
      @save="fnSave"
    />

    <!--
    @search="fnSubSearch"
    @create="fnCreate"
    -->

    <!-- 검색 영역 -->
    <div class="viewSearch">
      <div>
        <label>권한명</label>
        <input v-model.trim="authNm" type="text" />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- LEFT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 13%">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">권한 리스트</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 65vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th class="w-30">권한명</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(auth, idx) in authList" :key="auth.baimValCd">
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td @dblclick="fnSubSearch(auth)">{{ auth.baimValDNm }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- RIGHT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 1">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">메뉴 리스트</span>
          </div>
          <div
            v-if="authMenuList.length > 0"
            class="custom-btn-area menu-batch-control"
          >
            <template v-for="item in batchControlItems" :key="item.field">
              <span class="menu-batch-group">
                <span class="menu-batch-field">{{ item.label }}</span>
                <button
                  type="button"
                  class="btn btn-sm btn-custom"
                  :disabled="checkedCount === 0"
                  @click="fnSetFieldForChecked(item.field, 'Y')"
                >
                  사용
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-custom"
                  :disabled="checkedCount === 0"
                  @click="fnSetFieldForChecked(item.field, 'N')"
                >
                  미사용
                </button>
              </span>
            </template>
            <span class="menu-batch-divider" />
            <button
              type="button"
              class="btn btn-sm btn-primary"
              :disabled="checkedCount === 0"
              @click="fnSetAllForChecked('Y')"
            >
              일괄 사용
            </button>
            <button
              type="button"
              class="btn btn-sm btn-custom"
              :disabled="checkedCount === 0"
              @click="fnSetAllForChecked('N')"
            >
              일괄 미사용
            </button>
          </div>
        </div>

        <div
          class="table-box"
          style="
            --box-h: 65vh;
            --box-sticky-top: 1px;
            --box-ox: auto;
            width: 100%;
          "
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
                <th>대메뉴명</th>
                <th>소메뉴명</th>
                <th class="editableCell" style="width: 8%">사용여부</th>
                <th style="width: 10%">메뉴사용처</th>
                <th class="editableCell" style="width: 10%">조회</th>
                <th class="editableCell" style="width: 10%">생성</th>
                <th class="editableCell" style="width: 10%">삭제</th>
                <th class="editableCell" style="width: 10%">저장</th>
                <th class="editableCell" style="width: 10%">엑셀</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!authMenuList || authMenuList.length === 0">
                <tr>
                  <td colspan="11" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(menu, idx) in authMenuList" :key="menu.id">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      v-if="menu.menuSrc === '001'"
                      type="checkbox"
                      v-model="menu.chk"
                    />
                  </td>
                  <td>{{ menu.menuMNm }}</td>
                  <td>{{ menu.menuDNm }}</td>
                  <td>
                    <BaseSelect v-model="menu.useYn" name="useYn">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <BaseSelect
                      v-model="menu.menuSrc"
                      :readonly="true"
                      name="menuSrc"
                    >
                      <option
                        v-for="opt in (systCodeArr['SYS007'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <BaseSelect v-model="menu.btnSrch" name="btnSrch">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <BaseSelect v-model="menu.btnNew" name="btnNew">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <BaseSelect v-model="menu.btnDel">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <BaseSelect v-model="menu.btnSave">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td>
                    <BaseSelect v-model="menu.btnExcl">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from 'vue';
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';
import BaseSelect from '@/components/common/BaseSelect.vue';

// =========================== Define ===========================
defineOptions({ name: 'User_02' });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const authList = ref([]);
const authMenuList = ref([]);
const systCodeArr = ref({});
const baseInfoArr = ref([]);
const authNm = ref('');
const authCd = ref('');
const headChk = ref(false);

// =========================== Computed ===========================
const batchControlItems = [
  { field: 'useYn', label: '사용여부' },
  { field: 'btnSrch', label: '조회' },
  { field: 'btnNew', label: '생성' },
  { field: 'btnDel', label: '삭제' },
  { field: 'btnSave', label: '저장' },
  { field: 'btnExcl', label: '엑셀' },
];
const checkedCount = computed(() =>
  authMenuList.value.filter((m) => m.chk === true).length
);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();

// =========================== Life Cycle ===========================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  authMenuList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

// =========================== Methods ===========================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS003", "SYS007"],
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  authList.value = [];
  authMenuList.value = [];

  try {
    // const response = await axios.get("/comApi/baseinfo/syst-info", {
    const response = await axios.get("/comApi/baseinfo/base-info", {  
      params: {
        code: "COM005",
        nameD: authNm.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      // authList.value = response.data.systInfoList;
      authList.value = response.data.baseInfoList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSubSearch = async (auth) => {
  authMenuList.value = [];

  if (proxy.$util.isNotEmpty(auth)) {
    authCd.value = auth.baimValDCd;
  }

  try {
    const response = await axios.get("/webApi/user02/auth-menu-lists", {
      params: {
        authCd: authCd.value,
      },
    });

    if (response.status === 200) {
      authMenuList.value = response.data.authMenuList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredMenu = authMenuList.value.filter((menu) => menu.chk);

  for (var i = 0; i < filteredMenu.length; i++) {
    filteredMenu[i].authCd = authCd.value;
  }

  if (filteredMenu.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/user02/update-auth-menu-infos",
      filteredMenu
    );

    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      fnSubSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnButtonControll = () => {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  // localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  authMenuList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

/** chk=true 항목에 대해 지정 필드를 value로 설정 */
const fnSetFieldForChecked = (field, value) => {
  authMenuList.value
    .filter((m) => m.chk === true)
    .forEach((m) => {
      m[field] = value;
    });
};

/** chk=true 항목에 대해 사용여부·조회·생성·삭제·저장·엑셀 전부 value로 설정 */
const fnSetAllForChecked = (value) => {
  const fields = ['useYn', 'btnSrch', 'btnNew', 'btnDel', 'btnSave', 'btnExcl'];
  authMenuList.value
    .filter((m) => m.chk === true)
    .forEach((m) => {
      fields.forEach((f) => {
        m[f] = value;
      });
    });
};
</script>

<style scoped>
/* custom-btn-area 내부: subtitle-row 우측에 Baim_06 방식 배치 */
.menu-batch-control {
  flex-wrap: wrap;
}
.menu-batch-group {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
}
.menu-batch-field {
  font-size: 11px;
  color: var(--color-text-muted, #6b7280);
  margin-right: 0.1rem;
}
.menu-batch-divider {
  width: 1px;
  height: 1rem;
  background: var(--color-border, #d1d5db);
  margin: 0 0.25rem;
}
.menu-batch-control .btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
