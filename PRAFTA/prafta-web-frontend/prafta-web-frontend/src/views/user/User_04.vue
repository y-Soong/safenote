<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 (사업장 세션 고정 / 소속부서·사용자명 필터) -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input type="text" v-model="siteNo" placeholder="사업장코드" disabled />
        <button class="search-btn" disabled>
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input type="text" v-model="siteNm" placeholder="사업장명" disabled />
      </div>
      <div>
        <label>소속부서</label>
        <input
          id="nodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          @blur="focusKill"
        />
        <button class="search-btn" @click="fnSiteNodeSearchPopOpen()">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="nodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          @blur="focusKill"
        />
      </div>
      <div>
        <label>사용자명</label>
        <input
          type="text"
          v-model.trim="searchNm"
          placeholder="사용자명"
          @keyup.enter="fnSearch"
        />
      </div>
    </div>

    <div class="viewBody al-body">
      <!-- 좌: 사용자 리스트 -->
      <section class="al-pane al-pane--cand">
        <h3 class="al-pane__title">사용자 리스트</h3>
        <div class="al-list">
          <div v-if="candidates.length === 0" class="al-empty">
            조회된 사용자가 없습니다.
          </div>
          <div
            v-for="c in candidates"
            :key="c.userCd"
            class="al-cand"
            :class="{ 'is-added': isInLine(c.userCd) }"
          >
            <div class="al-cand__info">
              <span class="al-cand__name">
                {{ c.userNm }}<span class="al-cand__id">({{ c.userId }})</span>
              </span>
              <span class="al-cand__meta">
                <template v-if="c.authNm">{{ c.authNm }} · </template
                >{{ c.rankNm || "직급없음" }}
                <template v-if="c.nodeNm"> · {{ c.nodeNm }}</template>
              </span>
            </div>
            <button
              class="al-cand__add"
              :disabled="isInLine(c.userCd)"
              @click="fnAddApprover(c)"
            >
              {{ isInLine(c.userCd) ? "추가됨" : "추가" }}
            </button>
          </div>
        </div>
      </section>

      <!-- 우: 프리셋 관리 -->
      <section class="al-pane al-pane--preset">
        <div class="al-preset-head">
          <h3 class="al-pane__title">결재라인 프리셋</h3>
          <button class="al-new-btn" @click="fnNewPreset">+ 새 프리셋</button>
        </div>

        <!-- 프리셋 칩 목록 -->
        <div class="al-preset-tabs">
          <div v-if="presets.length === 0" class="al-preset-tabs__empty">
            저장된 프리셋이 없습니다. 새로 만들어 주세요.
          </div>
          <button
            v-for="p in presets"
            :key="p.presetId"
            class="al-preset-chip"
            :class="{ active: selectedPresetId === p.presetId }"
            @click="fnSelectPreset(p)"
          >
            <span v-if="p.defaultYn === 'Y'" class="al-preset-chip__star"
              >★</span
            >
            {{ p.presetNm }}
          </button>
        </div>

        <!-- 선택/신규 프리셋 편집 -->
        <div class="al-editor">
          <div class="al-editor__row">
            <input
              type="text"
              v-model.trim="editName"
              class="al-editor__name"
              maxlength="100"
              placeholder="프리셋 이름"
            />
            <label class="al-editor__default">
              <input type="checkbox" v-model="editDefault" />
              기본 프리셋
            </label>
          </div>

          <p class="al-note">
            ※ 위에서 아래 순서로 결재가 진행됩니다. 신청 시 이 프리셋을 선택해
            적용합니다.
          </p>

          <div class="al-list al-list--line">
            <div v-if="editLine.length === 0" class="al-empty">
              좌측에서 결재자를 추가하세요.
            </div>
            <div v-for="(s, idx) in editLine" :key="s.userCd" class="al-step">
              <span class="al-step__no">{{ idx + 1 }}</span>
              <div class="al-step__info">
                <span class="al-step__name">
                  {{ s.userNm
                  }}<span class="al-step__id">({{ s.userId }})</span>
                </span>
              </div>
              <div class="al-step__actions">
                <button :disabled="idx === 0" @click="fnMoveUp(idx)">▲</button>
                <button
                  :disabled="idx === editLine.length - 1"
                  @click="fnMoveDown(idx)"
                >
                  ▼
                </button>
                <button class="al-step__del" @click="fnRemove(idx)">✕</button>
              </div>
            </div>
          </div>

          <div class="al-editor__actions">
            <button
              v-if="selectedPresetId"
              class="al-btn al-btn--danger"
              :disabled="saving"
              @click="fnDeletePreset"
            >
              삭제
            </button>
            <button
              class="al-btn al-btn--primary"
              :disabled="saving"
              @click="fnSavePreset"
            >
              {{ selectedPresetId ? "수정 저장" : "신규 저장" }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import search_icon from "@/assets/img/search_icon.png";
import ViewHeader from "@/components/common/ViewHeader.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";

// =========================== Define ===========================
defineOptions({ name: "User_04" });
const props = defineProps({
  title: String,
  buttons: Object,
});
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });

// 사업장(세션 고정) / 소속부서 / 사용자명
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const nodeCd = ref("");
const nodeNm = ref("");
const searchNm = ref("");

const candidates = ref([]);

// 프리셋 목록 — [{ presetId, presetNm, defaultYn, steps:[{stepNo,approverUserCd,userNm,userId}] }]
const presets = ref([]);
// 편집 상태
const selectedPresetId = ref(null); // null이면 신규
const editName = ref("");
const editDefault = ref(false);
// 결재라인 — [{ userCd, userNm, userId }]
const editLine = ref([]);
const saving = ref(false);

// =========================== Methods ===========================
const isInLine = (userCd) => editLine.value.some((s) => s.userCd === userCd);

const fnButtonControll = () => {
  localButtons.value.search = "Y";
  localButtons.value.excel = "N";
  localButtons.value.save = "N";
  localButtons.value.create = "N";
  localButtons.value.delete = "N";
};

// 세션값에서 사업장 정보 세팅 (수정 불가)
const fnInitSite = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
};

// ===== 조회 =====
const fnSearch = () => {
  fnLoadCandidates();
};

const fnLoadCandidates = async () => {
  try {
    const response = await axios.get("/webApi/user04/approval-candidates", {
      params: { userNm: searchNm.value, nodeCd: nodeCd.value },
    });
    if (response.status === 200) {
      candidates.value = response.data?.candidates ?? [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "사용자 리스트 조회 중 오류가 발생했습니다.")
    );
  }
};

const fnLoadPresets = async (keepSelectedId = null) => {
  try {
    const response = await axios.get("/webApi/user04/presets", {});
    if (response.status === 200) {
      presets.value = response.data?.presets ?? [];
      // 선택 유지 또는 기본/첫번째 선택
      const keep =
        keepSelectedId &&
        presets.value.find((p) => p.presetId === keepSelectedId);
      if (keep) {
        fnSelectPreset(keep);
      } else if (presets.value.length > 0) {
        const def =
          presets.value.find((p) => p.defaultYn === "Y") ?? presets.value[0];
        fnSelectPreset(def);
      } else {
        fnNewPreset();
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "프리셋 조회 중 오류가 발생했습니다.")
    );
  }
};

// ===== 소속부서 검색 (사업장은 세션 고정) =====
// 소속부서 코드/명 동기화 (User_01 표준 focusKill 패턴).
//   - 한쪽(코드 또는 명)을 비우면 나머지 한쪽도 비운다.
//   - 직접 입력한 값은 노드조회(site-node-lists)로 보정한다:
//     0건=둘 다 비움 / 1건=확정 세팅 / 여러건=검색 팝업으로 선택.
const focusKill = (e) => {
  if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeCd.value = "";
      nodeNm.value = "";
      return;
    }
    // 코드 직접 입력 → 명은 비우고 조회로 짝을 보정
    nodeNm.value = "";
    nodeFocusKill();
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
      nodeNm.value = "";
      return;
    }
    // 명 직접 입력 → 코드는 비우고 조회로 짝을 보정
    nodeCd.value = "";
    nodeFocusKill();
  }
};

// 직접 입력한 부서코드/명을 site-node-lists 로 조회해 보정한다.
const nodeFocusKill = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    if (response.status === 200) {
      const list = response.data?.siteNodeInfoList || [];
      if (list.length === 0) {
        nodeCd.value = "";
        nodeNm.value = "";
      } else if (list.length === 1) {
        nodeCd.value = list[0].nodeCd ?? "";
        nodeNm.value = list[0].nodeNm ?? "";
      } else {
        fnSiteNodeSearchPopOpen();
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: nodeCd.value,
    nodeNm_p: nodeNm.value,
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// ===== 프리셋 편집 =====
const fnSelectPreset = (p) => {
  selectedPresetId.value = p.presetId;
  editName.value = p.presetNm ?? "";
  editDefault.value = p.defaultYn === "Y";
  editLine.value = (p.steps ?? []).map((s) => ({
    userCd: s.approverUserCd,
    userNm: s.userNm,
    userId: s.userId,
  }));
};

const fnNewPreset = () => {
  selectedPresetId.value = null;
  editName.value = "";
  editDefault.value = false;
  editLine.value = [];
};

const fnAddApprover = (c) => {
  if (isInLine(c.userCd)) return;
  editLine.value.push({ userCd: c.userCd, userNm: c.userNm, userId: c.userId });
};

const fnRemove = (idx) => editLine.value.splice(idx, 1);

const fnMoveUp = (idx) => {
  if (idx <= 0) return;
  const arr = editLine.value;
  [arr[idx - 1], arr[idx]] = [arr[idx], arr[idx - 1]];
};

const fnMoveDown = (idx) => {
  const arr = editLine.value;
  if (idx >= arr.length - 1) return;
  [arr[idx + 1], arr[idx]] = [arr[idx], arr[idx + 1]];
};

const fnSavePreset = async () => {
  if (!editName.value) {
    return proxy.$alert("프리셋 이름을 입력해 주세요.");
  }
  if (editLine.value.length === 0) {
    return proxy.$alert("결재자를 1명 이상 추가해 주세요.");
  }
  saving.value = true;
  try {
    const res = await axios.post("/webApi/user04/presets/save", {
      presetId: selectedPresetId.value || "",
      presetNm: editName.value,
      defaultYn: editDefault.value ? "Y" : "N",
      approverUserCds: editLine.value.map((s) => s.userCd),
    });
    const savedId = res.data?.presetId ?? selectedPresetId.value;
    await proxy.$alert("저장되었습니다.");
    await fnLoadPresets(savedId);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "프리셋 저장 중 오류가 발생했습니다.")
    );
  } finally {
    saving.value = false;
  }
};

const fnDeletePreset = async () => {
  if (!selectedPresetId.value) return;
  // (8.4) FE 1차 가드: 기본 프리셋은 삭제 불가.
  //   서버도 USER_400_057로 차단하지만, 불필요한 호출/혼란을 줄이기 위해 즉시 차단한다.
  const selected = presets.value.find(
    (p) => p.presetId === selectedPresetId.value
  );
  if (selected && selected.defaultYn === "Y") {
    await proxy.$alert("기본 프리셋은 삭제할 수 없습니다.");
    return;
  }
  const ok = await proxy.$confirm("이 프리셋을 삭제하시겠습니까?");
  if (!ok) return;
  saving.value = true;
  try {
    await axios.post("/webApi/user04/presets/delete", {
      presetId: selectedPresetId.value,
    });
    await proxy.$alert("삭제되었습니다.");
    await fnLoadPresets();
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "프리셋 삭제 중 오류가 발생했습니다.")
    );
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  fnButtonControll();
  fnInitSite();
  fnLoadCandidates();
  fnLoadPresets();
});
</script>

<style scoped>
.al-body {
  display: flex;
  gap: 1rem;
  align-items: stretch;
}
.al-pane {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  padding: 0.75rem;
  background: var(--color-surface, #fff);
  min-height: 360px;
}
.al-pane--cand {
  flex: 0 0 38%;
}
.al-pane--preset {
  flex: 1;
}
.al-pane__title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text, #111827);
  margin: 0 0 0.5rem;
}
.al-note {
  font-size: 0.8rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0 0 0.5rem;
}
.al-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.al-list--line {
  min-height: 140px;
}
.al-empty {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.85rem;
  text-align: center;
  padding: 1.5rem 0;
}
.al-cand,
.al-step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.4rem;
}
.al-cand.is-added {
  opacity: 0.55;
}
.al-cand__info,
.al-step__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.al-cand__name,
.al-step__name {
  font-size: 0.9rem;
  color: var(--color-text, #111827);
}
.al-cand__id,
.al-step__id {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.8rem;
  margin-left: 0.15rem;
}
.al-cand__meta {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
}
/* 사용자 리스트의 "추가" 버튼 전용 스타일.
   주의: 조회영역 돋보기 버튼(.search-btn)은 전역 button.css 의 초록 버튼 스타일을
   그대로 써야 흰색 돋보기 아이콘이 보이므로 여기에 묶지 않는다. */
.al-cand__add {
  border: 1px solid var(--color-primary, #30796a);
  color: var(--color-primary, #30796a);
  background: var(--color-surface, #fff);
  border-radius: 0.35rem;
  padding: 0.3rem 0.7rem;
  font-size: 0.82rem;
  cursor: pointer;
}
.al-cand__add:disabled {
  border-color: var(--color-border, #d1d5db);
  color: var(--color-text-muted, #9ca3af);
  cursor: default;
}
.al-preset-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.al-new-btn {
  border: 1px solid var(--color-primary, #30796a);
  color: var(--color-primary, #30796a);
  background: var(--color-surface, #fff);
  border-radius: 0.35rem;
  padding: 0.25rem 0.6rem;
  font-size: 0.8rem;
  cursor: pointer;
}
.al-preset-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  padding: 0.4rem 0;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  margin-bottom: 0.5rem;
}
.al-preset-tabs__empty {
  font-size: 0.82rem;
  color: var(--color-text-muted, #9ca3af);
  padding: 0.2rem 0;
}
.al-preset-chip {
  border: 1px solid var(--color-border, #d1d5db);
  background: var(--color-surface, #fff);
  color: var(--color-text, #111827);
  border-radius: 1rem;
  padding: 0.25rem 0.7rem;
  font-size: 0.82rem;
  cursor: pointer;
}
.al-preset-chip.active {
  border-color: var(--color-primary, #30796a);
  background: var(--color-primary-tint, #dcfce7);
  color: var(--color-primary, #30796a);
  font-weight: 600;
}
.al-preset-chip__star {
  color: var(--color-amber, #f59e0b);
  margin-right: 0.15rem;
}
.al-editor {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.al-editor__row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.4rem;
}
.al-editor__name {
  flex: 1;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.35rem;
  padding: 0.4rem 0.6rem;
  font-size: 0.88rem;
}
.al-editor__default {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.83rem;
  color: var(--color-text, #111827);
  white-space: nowrap;
  cursor: pointer;
}
.al-step__no {
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 50%;
  background: var(--color-primary, #30796a);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  flex-shrink: 0;
}
.al-step__actions {
  display: flex;
  gap: 0.2rem;
}
.al-step__actions button {
  border: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-surface, #fff);
  border-radius: 0.3rem;
  padding: 0.15rem 0.4rem;
  cursor: pointer;
  font-size: 0.75rem;
}
.al-step__actions button:disabled {
  opacity: 0.4;
  cursor: default;
}
.al-step__del {
  color: var(--color-danger, #dc2626);
}
.al-editor__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.4rem;
  margin-top: 0.6rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-border, #f1f5f9);
}
.al-btn {
  border-radius: 0.35rem;
  padding: 0.4rem 1rem;
  font-size: 0.84rem;
  cursor: pointer;
  border: 1px solid transparent;
}
.al-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.al-btn--primary {
  background: var(--color-primary, #30796a);
  color: #fff;
}
.al-btn--danger {
  background: var(--color-surface, #fff);
  border-color: var(--color-danger, #dc2626);
  color: var(--color-danger, #dc2626);
}
</style>
