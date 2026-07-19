<template>
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
          <span>대리 입실 처리</span>
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

        <!-- 탭(정규직 / 일용직) -->
        <div class="tab-bar">
          <button
            type="button"
            class="tab-item"
            :class="{ active: userTypeCd === 'REGULAR' }"
            @click="fnSwitchTab('REGULAR')"
          >
            정규직
          </button>
          <button
            type="button"
            class="tab-item"
            :class="{ active: userTypeCd === 'DAILY' }"
            @click="fnSwitchTab('DAILY')"
          >
            일용직
          </button>
        </div>

        <p class="tab-hint">
          {{
            userTypeCd === "REGULAR"
              ? "당일 출근한 정규직만 조회됩니다. 휴대전화 사용이 불가한 정규직을 관리자 권한으로 입실 처리합니다."
              : "만료되지 않은 일용직만 검색됩니다. 관리자 권한으로 입실 처리합니다."
          }}
        </p>

        <!-- 대상 회사(PRAFTA-SUBCON-T5): 자사 + 이 교육에 지정된 연동 회사 -->
        <div v-if="allowedCmpnys.length > 1" class="target-row">
          <label>대상 회사</label>
          <select v-model="targetCmpnyCd" @change="fnSearch">
            <option v-for="c in allowedCmpnys" :key="c.cmpnyCd" :value="c.cmpnyCd">
              {{ c.cmpnyNm }}
            </option>
          </select>
          <span class="target-hint">
            ⓘ 이 교육에 지정된 연동 회사 직원만 입실할 수 있습니다.
          </span>
        </div>

        <!-- 검색 -->
        <div class="viewSearch">
          <div class="form-left">
            <label>이름 / 아이디</label>
            <input v-model="keyword" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 일용직 QR 스캔(T6-08): 외부 스캐너가 키보드처럼 코드 입력 → Enter -->
        <div v-if="userTypeCd === 'DAILY'" class="scan-row">
          <label>QR 스캔</label>
          <input
            v-model="scanInput"
            class="scan-input"
            placeholder="QR 스캐너로 코드를 스캔한 뒤 Enter"
            :disabled="isBusy"
            @keyup.enter="fnScanEnter"
          />
          <span class="scan-hint">스캔한 일용직을 즉시 입실 처리합니다.</span>
        </div>

        <!-- 그리드 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>이름</th>
                  <th>아이디</th>
                  <th>소속</th>
                  <th>사업장</th>
                  <th>상태</th>
                  <th>처리</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="isLoading">
                  <td colspan="6" class="grid-msg">조회 중...</td>
                </tr>
                <tr v-else-if="candidates.length === 0">
                  <td colspan="6" class="grid-msg">{{ emptyMsg }}</td>
                </tr>
                <!-- 행 키는 서버 발급 불투명 핸들(userCd 는 회사별 채번이라 체인 내에서 중복될 수 있다) -->
                <tr v-for="row in candidates" :key="row.entryHandle">
                  <td>{{ row.userNm }}</td>
                  <td>{{ row.userId }}</td>
                  <td>{{ affilCmpnyNm || "-" }}</td>
                  <!-- 타사 후보는 서버가 사업장을 내리지 않는다(사업장명으로 2차 회사가 식별되는 것 방지) -->
                  <td>{{ row.siteNm || row.siteCd || "-" }}</td>
                  <td>
                    <span
                      :class="
                        row.alreadyEntered ? 'tag-entered' : 'tag-pending'
                      "
                    >
                      {{ row.alreadyEntered ? "입실됨" : "미입실" }}
                    </span>
                  </td>
                  <td>
                    <button
                      class="btn btn-second btn-sm"
                      :disabled="row.alreadyEntered || isBusy"
                      @click="fnManagerEnter(row)"
                    >
                      입실 처리
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const { proxy } = getCurrentInstance();

const props = defineProps({
  sessionCd_p: String,
  siteCd_p: String,
  onSearch: Function,
});
defineEmits(["close"]);

const modalRef = ref(null);
const userTypeCd = ref("REGULAR"); // REGULAR | DAILY
const keyword = ref("");
const candidates = ref([]);
const isLoading = ref(false);
const isBusy = ref(false);
const scanInput = ref(""); // 일용직 QR 외부 스캐너 입력(T6-08)

// PRAFTA-SUBCON-T5: 입실 대상 회사(자사 + 이 교육에 지정된 연동 체인). 회사명은 서버 relabel 값.
const allowedCmpnys = ref([]); // [{ cmpnyCd, cmpnyNm }]
const targetCmpnyCd = ref(""); // 기본값 = 자사(서버 응답 첫 항목)
const affilCmpnyNm = ref(""); // 후보 목록의 소속 표시명(대상 회사 단위)
const emptyMsg = ref("대상 사용자가 없습니다."); // 후보 0건 안내(타사 검색어 미달 시 문구 교체)

// 타사(연동) 대상 여부: 대상 회사 목록 첫 항목이 자사이므로, 그 외를 고르면 타사다.
const isForeignTarget = () => {
  if (!allowedCmpnys.value.length) return false;
  const ownCd = allowedCmpnys.value[0].cmpnyCd;
  return !!targetCmpnyCd.value && targetCmpnyCd.value !== ownCd;
};

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(async () => {
  await fnLoadAllowedCmpnys();
  fnSearch();
});

// 입실 대상 회사 목록 조회(첫 항목 = 자사). 실패해도 자사 기준으로 동작하도록 폴백한다.
const fnLoadAllowedCmpnys = async () => {
  try {
    const response = await axios.get(
      "/webApi/tbm02/session-share-allowed-cmpnys",
      { params: { sessionCd: props.sessionCd_p } }
    );
    if (response.status === 200) {
      allowedCmpnys.value = response.data?.cmpnyList || [];
      targetCmpnyCd.value = allowedCmpnys.value.length
        ? allowedCmpnys.value[0].cmpnyCd
        : "";
    }
  } catch (err) {
    allowedCmpnys.value = [];
    targetCmpnyCd.value = "";
  }
};

const fnSwitchTab = (type) => {
  if (userTypeCd.value === type) return;
  userTypeCd.value = type;
  candidates.value = [];
  scanInput.value = "";
  fnSearch();
};

// 일용직 QR 스캔 입실(T6-08): 외부 스캐너 입력값(QR raw)을 파싱해 userCd 를 추출하고 대상 확정.
//  - 앱 QR 페이로드는 JSON({ userCd, cmpnyCd, ... }) → userCd/cmpnyCd 추출. JSON 이 아니면 입력값 자체를 userCd 로 간주(폴백).
//  - PRAFTA-SUBCON-T5 N1: QR 의 cmpnyCd 를 <b>대상 회사(targetCmpnyCd)로 쓰지 않는다</b>. 대상 회사는 화면에서
//    고른 값(자사 또는 1차 연동 회사)만 보내고, 실제 참석자 회사는 서버가 지정 체인 안에서 도출한다.
//    (임의 회사코드를 넣어보며 200/403 차이로 체인 소속을 알아내는 열거를 막는다.)
//    QR 의 cmpnyCd 는 동명 사용자코드를 가르는 힌트(qrCmpnyCd)로만 함께 보낸다 — 서버는 자기가 도출한
//    후보 집합 안에 있을 때만 채택한다.
const fnScanEnter = async () => {
  const raw = (scanInput.value || "").trim();
  if (!raw) return;

  let userCd = "";
  let scanCmpnyCd = "";
  try {
    const obj = JSON.parse(raw);
    userCd = obj && obj.userCd ? String(obj.userCd) : "";
    scanCmpnyCd = obj && obj.cmpnyCd ? String(obj.cmpnyCd) : "";
  } catch (e) {
    userCd = raw; // JSON 이 아니면 단순 코드(userCd)로 간주
  }
  if (!userCd) {
    await proxy.$alert("스캔한 QR에서 일용직 식별값을 찾을 수 없습니다.");
    scanInput.value = "";
    return;
  }

  const ok = await proxy.$confirm(
    "스캔한 일용직을 관리자 권한으로 입실 처리하시겠습니까?"
  );
  if (!ok) {
    scanInput.value = "";
    return;
  }
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/manager-enter",
      {
        sessionCd: props.sessionCd_p,
        userTypeCd: "DAILY",
        userCd,
        // 대상 회사 = 화면에서 고른 값(서버가 개설사/1차 회사인지 검증). QR 의 회사코드가 아니다.
        targetCmpnyCd: targetCmpnyCd.value,
        // 동명 사용자코드 판별 힌트(서버가 도출한 후보 안에 있을 때만 채택).
        qrCmpnyCd: scanCmpnyCd || "",
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      await proxy.$alert("스캔한 일용직이 입실 처리되었습니다.");
      scanInput.value = "";
      if (typeof props.onSearch === "function") props.onSearch();
      fnSearch(); // 목록 '입실됨' 반영
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "입실 처리 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 입실 후보 검색(정규직/일용직). EntryCandidateResponse: { userTypeCd, candidateList }
const fnSearch = async () => {
  if (isLoading.value) return;
  // 타사(연동) 대상은 검색어 2자 이상 필수(서버 N2 가드와 동일). 대상 회사만 고른 상태에서
  // 빈 검색어로 API 를 쳐서 400(TBM_400_060) alert 가 뜨는 것을 막고, 안내 문구만 노출한다.
  if (isForeignTarget() && keyword.value.trim().length < 2) {
    candidates.value = [];
    affilCmpnyNm.value = "";
    emptyMsg.value = "연동 회사 직원은 이름 또는 아이디를 2자 이상 입력해 조회하세요.";
    return;
  }
  emptyMsg.value = "대상 사용자가 없습니다.";
  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/tbm02/entry-candidates", {
      params: {
        sessionCd: props.sessionCd_p,
        userTypeCd: userTypeCd.value,
        keyword: keyword.value,
        // PRAFTA-SUBCON-T5: 대상 회사(미지정이면 서버가 자사로 처리). 서버가 체인 소속을 재검증한다.
        targetCmpnyCd: targetCmpnyCd.value,
      },
    });

    if (response.status === 200) {
      candidates.value = response.data?.candidateList || [];
      // 소속 표시는 서버 relabel 값만 사용한다(프론트가 회사코드로 이름을 조립하지 않는다).
      affilCmpnyNm.value = response.data?.affilCmpnyNm || "";
    }
  } catch (err) {
    candidates.value = [];
    affilCmpnyNm.value = "";
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

// 관리자 직접 입실(MANAGER_DIRECT). 성공 시 행 입실표시 + 부모 갱신
const fnManagerEnter = async (row) => {
  if (row.alreadyEntered) return;
  const ok = await proxy.$confirm(
    `${row.userNm} 님을 관리자 권한으로 입실 처리하시겠습니까?`
  );
  if (!ok) return;
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/manager-enter",
      {
        sessionCd: props.sessionCd_p,
        // PRAFTA-SUBCON-T5 M1: 대상 키는 서버가 발급한 불투명 핸들뿐이다.
        //   회사코드/사용자코드는 요청에 담지 않는다(프론트는 알지도 못한다).
        entryHandle: row.entryHandle,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      row.alreadyEntered = true;
      await proxy.$alert(`${row.userNm} 님이 입실 처리되었습니다.`);
      if (typeof props.onSearch === "function") props.onSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "입실 처리 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};
</script>

<style scoped>
.tab-bar {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem 0;
}

.tab-item {
  height: var(--btn-height);
  padding: 0 var(--btn-padding);
  font-size: var(--btn-font);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
  color: var(--color-text-muted);
  cursor: pointer;
}

.tab-item.active {
  background: var(--color-primary);
  color: var(--color-surface);
  border-color: var(--color-primary);
}

.tab-hint {
  margin: 0;
  padding: 0.5rem 1rem;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* 대상 회사 선택행(PRAFTA-SUBCON-T5) */
.target-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.25rem 1rem 0.5rem;
}

.target-row label {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.target-hint {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* 일용직 QR 스캔 입력행(T6-08) */
.scan-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.25rem 1rem 0.5rem;
}

.scan-row label {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.scan-input {
  flex: 1 1 16rem;
  min-width: 0;
}

.scan-hint {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.grid-msg {
  text-align: center;
  padding: 1rem;
  color: var(--color-text-muted);
}

.tag-entered {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.tag-pending {
  color: var(--color-warning-text);
  font-size: var(--btn-font-sm);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
