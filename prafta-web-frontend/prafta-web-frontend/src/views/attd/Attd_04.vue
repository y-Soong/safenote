<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewBody attd04-body">
      <div class="attd04-main">
        <div class="attd04-card">
          <header class="attd04-page-head">
            <h2 class="attd04-title">출퇴근 시간 표준화</h2>
            <p class="attd04-lead">
              출퇴근 시각 차이를 정규 근무 계산에 반영합니다.
            </p>
          </header>

          <div class="attd04-divider" />

          <section class="attd04-section">
            <h3 class="attd04-section-title">표준화 기준</h3>
            <p class="attd04-section-desc">
              시작 시간과 종료 시간의 표준화 단위를 각각 설정할 수 있습니다.
            </p>

            <div class="attd04-block">
              <div class="attd04-block-head">
                <span class="attd04-block-title">시작 시간 표준화</span>
                <span class="attd04-block-sub"
                  >이른 출근을 시작 시각 계산에 반영</span
                >
              </div>
              <div
                class="attd04-chip-row"
                role="radiogroup"
                aria-label="시작 시간"
              >
                <button
                  v-for="opt in (systCodeArr['SYS029'] || []).filter(
                    (o) => o.systValDCd != null
                  )"
                  :key="`start-${opt.systValDCd ?? 'none'}`"
                  type="button"
                  role="radio"
                  :aria-checked="startStdTimeType === opt.systValDCd"
                  :class="[
                    'attd04-chip',
                    { active: startStdTimeType === opt.systValDCd },
                  ]"
                  @click="startStdTimeType = opt.systValDCd"
                >
                  {{ opt.systValDNm }}
                </button>
              </div>
            </div>

            <div class="attd04-block">
              <div class="attd04-block-head">
                <span class="attd04-block-title">종료 시간 표준화</span>
                <span class="attd04-block-sub"
                  >늦은 퇴근을 종료 시각 계산에 반영</span
                >
              </div>
              <div
                class="attd04-chip-row"
                role="radiogroup"
                aria-label="종료 시간"
              >
                <button
                  v-for="opt in (systCodeArr['SYS029'] || []).filter(
                    (o) => o.systValDCd != null
                  )"
                  :key="opt.systValDCd"
                  type="button"
                  role="radio"
                  :aria-checked="endStdTimeType === opt.systValDCd"
                  :class="[
                    'attd04-chip',
                    { active: endStdTimeType === opt.systValDCd },
                  ]"
                  @click="endStdTimeType = opt.systValDCd"
                >
                  {{ opt.systValDNm }}
                </button>
              </div>
            </div>

            <p class="attd04-info">
              <span class="attd04-info-icon" aria-hidden="true">i</span>
              실제 출퇴근 시각은 유지되며, 계산에만 반영됩니다.
            </p>
          </section>

          <section class="attd04-section attd04-example-section">
            <div class="attd04-example-row">
              <div class="attd04-example-text">
                <h3 class="attd04-section-title">계산 예시</h3>
                <p class="attd04-section-desc attd04-section-desc--tight">
                  9시 출근·18시 퇴근 근무 기준, 표준화 단위(5·10·15·30분)별
                  결과를 팝업에서 확인할 수 있습니다.
                </p>
              </div>
              <button
                type="button"
                class="btn-example"
                @click="fnOpenExamplePop"
              >
                계산 예시
              </button>
            </div>
          </section>

          <div class="attd04-actions">
            <button type="button" class="btn-reset" @click="fnReset">
              초기화
            </button>
            <button type="button" class="btn-save" @click="fnSave">저장</button>
          </div>
        </div>
      </div>

      <div class="attd04-aside">
        <div class="attd04-panel">
          <div class="attd04-panel-tabs">
            <span class="attd04-panel-tab active" role="heading" aria-level="2">
              변경 이력
            </span>
          </div>
          <div class="attd04-panel-body">
            <div class="attd04-history-scroll">
              <div
                v-for="(row, idx) in attdStdTimeRuleHistResultList"
                :key="idx"
                class="attd04-history-row"
              >
                <div class="attd04-history-meta">
                  {{ row.insertDate }}&nbsp;&nbsp;{{ row.insertNm }}
                </div>
                <div class="attd04-history-detail">
                  <span class="attd04-history-label">{{
                    row.stdTimeRuleTypeNm
                  }}</span>
                  <span class="attd04-history-sep">-</span>
                  <span class="attd04-history-new">{{
                    row.stdTimeTypeNm
                  }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import CommuteStdExamplePop from "@/views/attd/popup/CommuteStdExamplePop.vue";
import axios from "@/api/axios";

defineOptions({ name: "Attd_04" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

const systCodeArr = ref([]);
const attdStdTimeRuleResultList = ref([]);
const attdStdTimeRuleHistResultList = ref([]);

const startStdTimeType = ref();
const endStdTimeType = ref();

const fnOpenExamplePop = () => {
  openPop(CommuteStdExamplePop, {});
};

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnAttdStdTimeRuleSet = () => {
  if (
    attdStdTimeRuleResultList.value != null &&
    attdStdTimeRuleResultList.value.length > 0
  ) {
    for (var i = 0; i < attdStdTimeRuleResultList.value.length; i++) {
      if (attdStdTimeRuleResultList.value[i].stdTimeRuleType == "01") // 출근
      {
        startStdTimeType.value = attdStdTimeRuleResultList.value[i].stdTimeType;
      } else if (
        attdStdTimeRuleResultList.value[i].stdTimeRuleType == "02"
      ) // 퇴근
      {
        endStdTimeType.value = attdStdTimeRuleResultList.value[i].stdTimeType;
      }
    }
  }
};

const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS029"] },
    });
    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      systCodeArr.value = grouped;
      console.log(systCodeArr.value);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "시스템코드 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  try {
    const response = await axios.get(
      "/webApi/attd04/attd-std-time-rule-lists",
      {}
    );
    console.log(response.status === 200);

    if (response.status === 200) {
      var resData = response.data;
      attdStdTimeRuleResultList.value = resData.attdStdTimeRuleResultList;
      attdStdTimeRuleHistResultList.value =
        resData.attdStdTimeRuleHistResultList;

      fnAttdStdTimeRuleSet();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnReset = () => {
  startStdTimeType.value = "01";
  endStdTimeType.value = "01";
};

const fnSave = async () => {
  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/attd04/save-attd-std-time-rules",
      {
        startStdTimeType: startStdTimeType.value,
        endStdTimeType: endStdTimeType.value,
      }
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});
</script>

<style scoped>
.attd04-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  gap: 1rem;
  overflow: hidden;
}

.attd04-main {
  flex: 8 1 0;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.attd04-aside {
  flex: 2 1 0;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 우측 열 하단에 배경이 보이도록 — 변경이력 패널이 화면 하단에 붙어 보이지 않게 */
  padding-bottom: 1rem;
  box-sizing: border-box;
}

.attd04-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  background: #fff;
  padding: 1.25rem 1.5rem;
  flex-shrink: 0;
}

.attd04-page-head {
  margin-bottom: 0;
}

.attd04-title {
  margin: 0 0 0.35rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

.attd04-lead {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  line-height: 1.5;
}

.attd04-divider {
  height: 1px;
  background: var(--color-border, #e5e7eb);
  margin: 1rem 0 1.25rem;
}

.attd04-section {
  margin-bottom: 1.5rem;
}

.attd04-section:last-of-type {
  margin-bottom: 0;
}

.attd04-section-title {
  margin: 0 0 0.35rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

.attd04-section-desc {
  margin: 0 0 1rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  line-height: 1.5;
}

.attd04-block {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 1rem 1.125rem;
  margin-bottom: 0.75rem;
}

.attd04-block:last-of-type {
  margin-bottom: 0;
}

.attd04-block-head {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-bottom: 0.75rem;
}

.attd04-block-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}

.attd04-block-sub {
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
}

.attd04-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.attd04-chip {
  padding: 0.45rem 0.85rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 8px;
  background: #fff;
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s,
    color 0.2s;
}

.attd04-chip:hover {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-text-strong, #111827);
}

.attd04-chip.active {
  border-color: var(--color-primary, #16a34a);
  background: rgba(22, 163, 74, 0.08);
  color: var(--color-primary, #16a34a);
  font-weight: 500;
}

.attd04-info {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin: 1rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6b7280);
  line-height: 1.5;
}

.attd04-info-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid var(--color-border, #d1d5db);
  font-size: 0.65rem;
  font-weight: 700;
  font-style: normal;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted, #6b7280);
  margin-top: 1px;
}

.attd04-example-section {
  margin-top: 0.25rem;
}

.attd04-example-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.attd04-example-text {
  flex: 1;
  min-width: 12rem;
}

.attd04-example-text .attd04-section-title {
  margin-bottom: 0.25rem;
}

.attd04-section-desc--tight {
  margin-bottom: 0;
}

.btn-example {
  flex-shrink: 0;
  margin-top: 0.15rem;
  padding: 0.5rem 1.1rem;
  border: 1px solid var(--color-primary, #16a34a);
  border-radius: 8px;
  background: rgba(22, 163, 74, 0.08);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}

.btn-example:hover {
  background: rgba(22, 163, 74, 0.14);
}

.attd04-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1.5rem;
  padding-top: 0.25rem;
}

.btn-reset {
  padding: 0.5rem 1.1rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 8px;
  background: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-strong, #111827);
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}

.btn-reset:hover {
  background: var(--color-bg, #f9fafb);
}

.btn-save {
  padding: 0.5rem 1.25rem;
  border: none;
  border-radius: 8px;
  background: var(--color-primary, #16a34a);
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-save:hover {
  background: #15803d;
}

/* 우측(4) 변경 이력 — 열 높이에 맞춤, 목록만 세로 스크롤 */
.attd04-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.attd04-panel-tabs {
  display: flex;
  width: 100%;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  flex-shrink: 0;
}

.attd04-panel-tab {
  flex: 1;
  padding: 0.6rem 1rem;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  text-align: center;
}

.attd04-panel-tab.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom: 2px solid var(--color-primary, #16a34a);
  margin-bottom: -1px;
}

/* Attd_02 .panel-body 와 동일: 본문 여백으로 카드 하단 테두리·라운드가 드러남 */
.attd04-panel-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 1rem;
}

.attd04-history-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
}

.attd04-history-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.attd04-history-meta {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  margin-bottom: 0.3rem;
  line-height: 1.4;
}

.attd04-history-detail {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.875rem;
  color: var(--color-text-strong, #111827);
  line-height: 1.5;
}

.attd04-history-label {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.8125rem;
}

.attd04-history-old {
  color: var(--color-text-muted, #6b7280);
  text-decoration: line-through;
  font-size: 0.8125rem;
}

.attd04-history-sep {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.75rem;
}

.attd04-history-arrow {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.75rem;
}

.attd04-history-new {
  font-weight: 500;
  color: var(--color-primary, #16a34a);
}
</style>
