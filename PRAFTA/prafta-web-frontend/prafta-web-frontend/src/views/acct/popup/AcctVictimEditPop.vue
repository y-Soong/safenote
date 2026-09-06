<template>
  <!-- prafta-065 D4: 재해자 최소 편집 팝업(추가·속성 수정·제외). 사고 헤더(발생일시·등급·경위) 수정은 범위 밖. -->
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>재해자 편집 · {{ acctId }}</span>
          <button class="icon-button" @click="$emit('close')" aria-label="닫기">
            ✕
          </button>
        </div>

        <div class="ve-body">
          <div class="ve-notice">
            ⓘ 인물 변경은 제외 후 추가로 처리합니다. 마지막 1명은 제외할 수
            없으며, 대표 재해자는 순번이 가장 낮은 인원으로 자동 지정됩니다.
          </div>
          <div class="ve-toolbar">
            <button class="btn btn-second" @click="fnOpenVictimSearch">
              재해자 추가
            </button>
            <span class="ve-count" v-if="rows.length">{{ rows.length }}명</span>
          </div>

          <div v-if="loading" class="ve-state">재해자 조회 중...</div>
          <table v-else class="data-grid ve-grid">
            <thead>
              <tr>
                <th class="ve-grid__rep">대표</th>
                <th>이름 · 유형</th>
                <th>재해결과<span class="req">*</span></th>
                <th>요양일수</th>
                <th>휴업일수</th>
                <th>부상부위</th>
                <th>부상내용</th>
                <th class="ve-grid__act"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="rows.length === 0">
                <td colspan="8" class="edu-grid-empty">
                  재해자 정보가 없습니다.
                </td>
              </tr>
              <tr
                v-for="r in rows"
                :key="r._key"
                :class="{ 'is-new': r._isNew }"
              >
                <td class="ve-grid__rep">
                  <span v-if="r.representativeYn === 'Y'" class="acc-rep-badge"
                    >대표</span
                  >
                  <span v-else-if="r._isNew" class="ve-new-badge">신규</span>
                </td>
                <td>
                  {{ r.userNm }}
                  <span class="ve-sub"
                    >({{
                      r.userTypeNm ||
                      (r.userTypeCd === "DAILY" ? "일용" : "정규")
                    }}<template v-if="r.nodeNm"> · {{ r.nodeNm }}</template
                    >)</span
                  >
                </td>
                <td>
                  <BaseSelect v-model="r.victimResultCd">
                    <option value="">선택</option>
                    <option
                      v-for="opt in resultOptions"
                      :key="opt.systValDCd"
                      :value="opt.systValDCd"
                    >
                      {{ opt.systValDNm }}
                    </option>
                  </BaseSelect>
                </td>
                <td>
                  <input
                    type="number"
                    min="0"
                    max="3650"
                    v-model.trim="r.careDays"
                    placeholder="미확정"
                  />
                </td>
                <td>
                  <input
                    type="number"
                    min="0"
                    max="3650"
                    v-model.trim="r.restDays"
                    placeholder="미확정"
                  />
                </td>
                <td>
                  <input
                    type="text"
                    maxlength="100"
                    v-model.trim="r.injuryPart"
                  />
                </td>
                <td>
                  <input
                    type="text"
                    maxlength="500"
                    v-model.trim="r.injuryDesc"
                  />
                </td>
                <!-- 제외 버튼은 항상 활성 — 1명 남으면 클릭 시 안내(메모리: disabled 는 안내 도달 불가) -->
                <td class="ve-grid__act">
                  <button class="btn btn-second btn-sm" @click="fnRemove(r)">
                    제외
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="ve-legend">
            ※ 재해 결과·일수는 참고 기록이며 재해등급을 자동 판정하지 않습니다.
          </div>
        </div>

        <div class="modal-foot">
          <button class="btn btn-primary" :disabled="saving" @click="fnSave">
            저장
          </button>
          <button class="btn btn-second" @click="$emit('close')">닫기</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import VictimSearchPop from "./VictimSearchPop.vue";

const props = defineProps({
  siteCd: { type: String, required: true },
  acctId: { type: String, required: true },
  onSaved: Function, // 저장/제외 반영 후 부모 재조회 콜백
});
defineEmits(["close"]);
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 4,
});

// 행 = 서버 행(_isNew=false, _orig 보유) + 미저장 신규 행(_isNew=true).
//   저장 시 신규 행은 add, 서버 행은 로드 시점 대비 변경된 것만 update(fnSaveLegalSteps 패턴).
const rows = ref([]);
const systCodeArr = ref({}); // SYS084
const loading = ref(false);
const saving = ref(false);
const resultOptions = computed(() => systCodeArr.value["SYS084"] || []);

const MAX_DAYS = 3650;
const MIN_VICTIMS_MSG = "재해자는 최소 1명 이상이어야 합니다.";

const victimKey = (v) => `${v.userTypeCd}_${v.userCd}`;
const baseParams = () => ({ siteCd: props.siteCd, acctId: props.acctId });

onMounted(async () => {
  await Promise.all([fnGetSystinfoList(), fnLoad()]);
});

// SYS084(재해결과) 코드 로드 — Acct_01/AcctCreatePop 과 같은 방식
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS084"] },
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
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "코드 조회 중 오류가 발생했습니다.")
    );
  }
};

// 서버 행 → 편집 행 변환. 일수 null 은 빈 문자열(미확정)로 바인딩한다.
const toEditRow = (v) => {
  const careDays = v.careDays == null ? "" : String(v.careDays);
  const restDays = v.restDays == null ? "" : String(v.restDays);
  const injuryPart = v.injuryPart || "";
  const injuryDesc = v.injuryDesc || "";
  const victimResultCd = v.victimResultCd || "";
  return {
    ...v,
    victimResultCd,
    careDays,
    restDays,
    injuryPart,
    injuryDesc,
    _key: `s_${v.victimSeq}`,
    _isNew: false,
    _orig: { victimResultCd, careDays, restDays, injuryPart, injuryDesc },
  };
};

const fnLoad = async () => {
  loading.value = true;
  try {
    const response = await axios.get("/webApi/acct01/victims", {
      params: baseParams(),
    });
    if (response.status === 200) {
      rows.value = (response.data?.victimList || []).map(toEditRow);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "재해자 조회 중 오류가 발생했습니다.")
    );
  } finally {
    loading.value = false;
  }
};

// 재해자 추가: 검색 팝업(다건). 현재 행(서버 행 + 미저장 신규 행)은 잠금으로 넘겨 중복 선택을 막는다.
//   반환 인원은 '미저장 신규' 행으로 로컬에 추가하고, 재해 결과 입력 후 저장 시 add API 를 호출한다
//   (서버가 재해 결과 코드를 필수로 요구하므로 추가 직후 즉시 호출하지 않음).
const fnOpenVictimSearch = () => {
  openPop(VictimSearchPop, {
    siteCd: props.siteCd,
    lockedKeys: rows.value.map(victimKey),
    onSelect: onVictimsPicked,
  });
};

const onVictimsPicked = (list) => {
  const existing = {};
  rows.value.forEach((r) => {
    existing[victimKey(r)] = true;
  });
  const appended = (list || [])
    .filter((v) => !existing[victimKey(v)])
    .map((v) => ({
      victimSeq: null,
      userTypeCd: v.userTypeCd,
      userTypeNm: v.userTypeNm || "",
      userCd: v.userCd,
      userNm: v.userNm,
      mblNoLast4: v.mblNoLast4 || "",
      nodeNm: v.nodeNm || "",
      victimResultCd: "",
      careDays: "",
      restDays: "",
      injuryPart: "",
      injuryDesc: "",
      representativeYn: "N",
      _key: `n_${victimKey(v)}`,
      _isNew: true,
      _orig: null,
    }));
  rows.value = [...rows.value, ...appended];
};

// 제외: 미저장 신규 행은 로컬 제거. 서버 행은 잔여 1명이면 안내(서버 D5 와 동일 메시지), 아니면 confirm 후 즉시 remove.
const fnRemove = async (row) => {
  if (saving.value) return;
  if (row._isNew) {
    rows.value = rows.value.filter((r) => r._key !== row._key);
    return;
  }
  const savedCnt = rows.value.filter((r) => !r._isNew).length;
  if (savedCnt <= 1) {
    await proxy.$alert(MIN_VICTIMS_MSG);
    return;
  }
  const ok = await proxy.$confirm(
    `${row.userNm} 재해자를 제외하시겠습니까?\n이미 확정된 근태·TBM 스냅샷은 삭제되지 않습니다.`
  );
  if (!ok) return;
  saving.value = true;
  try {
    await axios.post("/webApi/acct01/victims/remove", {
      ...baseParams(),
      victimSeq: row.victimSeq,
    });
    // 미저장 신규 행은 재조회 후에도 유지한다(사용자 입력 보존).
    const pendingNew = rows.value.filter((r) => r._isNew);
    await fnLoad();
    rows.value = [...rows.value, ...pendingNew];
    if (typeof props.onSaved === "function") await props.onSaved();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "재해자 제외 중 오류가 발생했습니다.")
    );
  } finally {
    saving.value = false;
  }
};

// 일수 검증: 빈값(미확정) 허용, 그 외 0 이상 정수 + 상한 3650. 반환 = 오류 메시지("" 이면 정상)
const validateDays = (val, label, name) => {
  if (val === "" || val == null) return "";
  const s = String(val);
  if (!/^\d+$/.test(s))
    return `${name} 재해자의 ${label}는 0 이상 정수로 입력하세요.`;
  if (Number(s) > MAX_DAYS)
    return `${name} 재해자의 ${label}는 ${MAX_DAYS}일을 초과할 수 없습니다.`;
  return "";
};

const fnValidate = () => {
  for (const r of rows.value) {
    if (!r.victimResultCd)
      return `${r.userNm} 재해자의 재해 결과를 선택하세요.`;
    const careMsg = validateDays(r.careDays, "요양일수", r.userNm);
    if (careMsg) return careMsg;
    const restMsg = validateDays(r.restDays, "휴업일수", r.userNm);
    if (restMsg) return restMsg;
  }
  return "";
};

const toDaysOrNull = (val) => (val === "" || val == null ? null : Number(val));

const toAttrBody = (r) => ({
  victimResultCd: r.victimResultCd,
  careDays: toDaysOrNull(r.careDays),
  restDays: toDaysOrNull(r.restDays),
  injuryPart: r.injuryPart || null,
  injuryDesc: r.injuryDesc || null,
});

const isChanged = (r) =>
  !!r._orig &&
  (r.victimResultCd !== r._orig.victimResultCd ||
    String(r.careDays) !== String(r._orig.careDays) ||
    String(r.restDays) !== String(r._orig.restDays) ||
    (r.injuryPart || "") !== (r._orig.injuryPart || "") ||
    (r.injuryDesc || "") !== (r._orig.injuryDesc || ""));

// 저장: 신규 행 → add, 변경된 서버 행 → update 를 순차 POST. 중간 실패 시 중단 + 서버 메시지 안내 후 재조회.
const fnSave = async () => {
  if (saving.value) return;
  const msg = fnValidate();
  if (msg) {
    await proxy.$alert(msg);
    return;
  }
  const newRows = rows.value.filter((r) => r._isNew);
  const changedRows = rows.value.filter((r) => !r._isNew && isChanged(r));
  if (newRows.length === 0 && changedRows.length === 0) {
    await proxy.$alert("변경된 내용이 없습니다.");
    return;
  }
  saving.value = true;
  let failed = null;
  const savedNewKeys = {};
  try {
    for (const r of newRows) {
      try {
        await axios.post("/webApi/acct01/victims/add", {
          ...baseParams(),
          victim: {
            userTypeCd: r.userTypeCd,
            userCd: r.userCd,
            ...toAttrBody(r),
          },
        });
        savedNewKeys[r._key] = true;
      } catch (err) {
        failed = err;
        break;
      }
    }
    if (!failed) {
      for (const r of changedRows) {
        try {
          await axios.post("/webApi/acct01/victims/update", {
            ...baseParams(),
            victimSeq: r.victimSeq,
            ...toAttrBody(r),
          });
        } catch (err) {
          failed = err;
          break;
        }
      }
    }
    if (failed) {
      await proxy.$alert(
        resolveApiErrorMessage(failed, "저장 중 오류가 발생했습니다.")
      );
    } else {
      await proxy.$alert("저장되었습니다.");
    }
    // 서버 기준으로 다시 맞춘다. 저장에 실패한 신규 행은 입력값을 보존해 재시도할 수 있게 남긴다.
    const pendingNew = rows.value.filter(
      (r) => r._isNew && !savedNewKeys[r._key]
    );
    await fnLoad();
    rows.value = [...rows.value, ...pendingNew];
    if (typeof props.onSaved === "function") await props.onSaved();
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.ve-body {
  padding: var(--card-padding, 20px);
  overflow-y: auto;
  max-height: 70vh;
}
.ve-notice {
  background: var(--color-warning-bg, #fffbeb);
  border: 1px solid var(--color-warning-bg, #fde68a);
  border-radius: var(--input-radius, 10px);
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-warning-text, #92400e);
  margin-bottom: 0.75rem;
}
.ve-toolbar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.6rem;
}
.ve-count {
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--color-primary-hover, #15803d);
}
.ve-state {
  padding: 2rem;
  text-align: center;
  color: var(--color-text-muted, #8b94a3);
}
.ve-grid {
  width: 100%;
  font-size: 0.8rem;
}
.ve-grid th,
.ve-grid td {
  padding: 0.4rem 0.45rem;
  vertical-align: middle;
}
.ve-grid input {
  width: 100%;
  padding: 0.3rem 0.45rem;
  font-size: 0.78rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
}
.ve-grid :deep(select) {
  width: 100%;
  padding: 0.3rem 0.45rem;
  font-size: 0.78rem;
}
.ve-grid tr.is-new {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.05));
}
.ve-grid__rep {
  width: 48px;
  text-align: center;
}
.ve-grid__act {
  width: 64px;
  text-align: center;
}
.ve-sub {
  color: var(--color-text-muted, #8b94a3);
  font-size: 0.72rem;
}
.ve-new-badge {
  font-size: 0.66rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-pill, 999px);
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.acc-rep-badge {
  display: inline-block;
  font-size: 0.66rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-pill, 999px);
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.12));
  color: var(--color-primary-hover, #15803d);
}
.ve-legend {
  font-size: 0.7rem;
  color: var(--color-text-muted, #8b94a3);
  margin-top: 0.75rem;
  padding-top: 0.6rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
}
.req {
  color: var(--color-danger, #ef4444);
  margin-left: 2px;
}
.modal-foot {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  padding: 0.875rem 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}
.modal-foot .btn {
  flex: 1;
}
</style>
