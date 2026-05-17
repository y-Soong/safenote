<!--
  PRAFTA-003 / F1 + F2 — AttdDayDetailPop.vue patch (skeleton)
  ==========================================================
  이 파일은 planner가 작성한 *부분 적용 가능한* 골격이다.
  대상 파일: prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue
  developer는 아래 4개 blocks를 해당 위치에 머지한다. script body는 비어있으니 developer가 채워야 한다.
  template/style은 그대로 사용 가능하다 (CSS 변수만 사용, 하드코딩 없음).

  주의: 기존 파일은 매우 크고 정상 동작 중. 본 patch는 *추가*만 한다.
        기존 라인의 단순 키 교체(F2)는 본 patch 하단 "F2 patch list" 참조.
-->

<!-- ============================================================== -->
<!-- BLOCK A. (template) 추가근무 블록 head 우측에 "허용 범위 안내" 추가
     기존 line 441 부근 `<div class="ot-block-head"><span class="ot-block-title">초과근무</span></div>`
     를 아래 구조로 교체한다.
-->
<template>
  <div class="ot-block-head">
    <span class="ot-block-title">초과근무</span>
    <div
      v-if="otAllowedWindows.length"
      class="ot-allowed-hint"
      aria-label="초과근무 등록 가능 범위"
    >
      <span class="ot-allowed-lbl">등록 가능</span>
      <ul class="ot-allowed-list">
        <li
          v-for="(w, wi) in otAllowedWindows"
          :key="wi"
          class="ot-allowed-item"
        >
          {{ w.startLabel }} ~ {{ w.endLabel }}
        </li>
      </ul>
    </div>
    <div v-else class="ot-allowed-hint is-empty">
      등록 가능한 초과근무 범위가 없습니다.
    </div>
  </div>
</template>


<!-- ============================================================== -->
<!-- BLOCK B. (template) 추가근무 블록 하단(기존 line 532 부근)
     `+ {{ i+1 }}구간 초과근무 추가` 버튼 *바로 다음 줄*에 저장/반려 버튼 영역 신설.
     기존 add-ot-btn 그대로 유지, 그 아래에 ot-actions 영역을 추가.
-->
<template>
  <div v-if="hasAnyOt(i)" class="ot-actions">
    <button
      type="button"
      class="ot-save-btn"
      :disabled="!canSaveOt || otSaving"
      @click="fnApproveOvertime(i)"
    >
      <span v-if="!otSaving">초과근무 저장</span>
      <span v-else>저장 중…</span>
    </button>
    <button
      v-if="otHasReqId(i)"
      type="button"
      class="ot-reject-btn"
      :disabled="otSaving"
      @click="fnRejectOvertime(i)"
    >
      반려
    </button>
  </div>
</template>


<!-- ============================================================== -->
<!-- BLOCK C. (script setup) 추가근무 신규 API 연동 골격
     기존 `// ── 초과근무 관리 (UI 골격) ───` 주석 블록(line 1367 부근) 바로 위 또는 아래에 추가.
     **script body는 비어 있다. developer가 채운다.**
-->
<script setup>
// === overtime request (PRAFTA-003 F1) ============================
// 이 블록은 planner가 만든 골격이다. developer가 채워야 한다.

import { ref, computed } from 'vue'

// 1) 폼 상태 — otList는 기존 form.value.segments[i].otList 를 그대로 사용.
//    아래 otForm은 *저장 시점*의 임시 페이로드 컨테이너.
const otForm = ref({
  // TODO(developer): 필요 시 fill — 현재는 segments[i].otList 를 그대로 페이로드로 변환하므로 비워둘 수 있다.
  // 예: { reqId: null, attdId: null, reqReason: '' }
})

// 2) 허용 범위 (정책서 2,3번)
//    스케줄(plan1Start/End, plan2Start/End) - 표준화(act{n}InStdTime/act{n}OutStdTime) 차집합.
//    record.value(=r) 에서 추출. 아래는 계산 방식 가이드만 — 실제 구현은 developer.
const otAllowedWindows = computed(() => {
  // TODO(developer): subtractIntervals(stdSegs, schSegs) 결과를
  //   [{ startLabel: '18:00', endLabel: '21:00' }, ...] 형태로 반환.
  // 가이드:
  //   1. record.value 에서 plan1Start..plan2End 와 act{1,2}{In,Out}StdTime 추출 (HHmm).
  //   2. workYmd 자정 기준 분 stamp 로 normalize (익일 퇴근이면 +1440).
  //   3. mergeIntervals + subtractIntervals.
  //   4. 결과 stamp 를 다시 HH:mm 라벨로 포맷.
  return []
})

// 3) 저장 가능 여부
const otSaving = ref(false)
const canSaveOt = computed(() => {
  // TODO(developer): 모든 추가된 ot row 가 startDate/startTime/endDate/endTime 채워졌고
  //                  허용 범위에 포함되며 서로 겹치지 않을 때 true.
  return false
})

// 4) helper — 해당 segment 에 OT 행이 하나라도 있는지
const hasAnyOt = (segIdx) => {
  // TODO(developer): return !!form.value.segments?.[segIdx]?.otList?.length
  return false
}

// 5) helper — OT 행이 reqId 경유로 들어왔는지 (반려 버튼 노출 조건)
const otHasReqId = (segIdx) => {
  // TODO(developer): OT가 근로자 요청을 통해 들어온 경우에만 true.
  //   현재 화면에서는 사용자 요청 카드에 OT 가 별도 흐름이 아직 없으므로 false 유지.
  return false
}

// 6) 저장
const fnApproveOvertime = async (segIdx) => {
  // TODO(developer):
  //   1. await validateOt() (클라이언트 검증)
  //   2. const ok = await proxy.$confirm(getMessage(MSG.OT_SAVE_CONFIRM))
  //   3. payload = {
  //        userCd, siteCd, nodeCd, workYmd, attdId (해당 segment의 attd{n}Id),
  //        reqId: null, // 또는 요청 경유 시 setting
  //        reqReason: form.value.reason || '',
  //        overtimes: form.value.segments[segIdx].otList.map(o => ({
  //          otType: o.type === 'extend' ? 'EXTEND' : o.type === 'night' ? 'NIGHT' : 'HOLIDAY',
  //          startDate: ymdToYmdNum(o.startDate),
  //          startTime: o.startTime,
  //          endDate: ymdToYmdNum(o.endDate),
  //          endTime: o.endTime,
  //        })),
  //      }
  //   4. otSaving.value = true
  //   5. await axios.post('/webApi/attd07/update-user-overtime-requests', payload)
  //   6. 200 시 alert + emit('saved') + emit('close')
  //   7. catch → alert(err.response?.data?.message || getMessage(MSG.SAVE_ERROR))
  //   8. finally otSaving.value = false
}

// 7) 반려
const fnRejectOvertime = async (segIdx) => {
  // TODO(developer): 반려 API 연동 — 현재는 스텁.
  //   - confirm → POST /webApi/attd07/reject-user-overtime-request { reqId } (해당 API 별도 작업)
}
</script>


<!-- ============================================================== -->
<!-- BLOCK D. (style scoped) 신규 클래스 — 모두 토큰만 사용, 하드코딩 없음 -->
<style scoped>
/* PRAFTA-003 F1 — 초과근무 허용 범위 안내 */
.ot-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--header-gap);
}

.ot-allowed-hint {
  display: flex;
  align-items: center;
  gap: var(--header-right-gap);
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.ot-allowed-hint.is-empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.ot-allowed-lbl {
  color: var(--color-text);
  font-weight: 600;
}

.ot-allowed-list {
  display: flex;
  align-items: center;
  gap: var(--header-right-gap);
  margin: 0;
  padding: 0;
  list-style: none;
}

.ot-allowed-item {
  padding: 0 var(--header-right-gap);
  border: var(--card-border);
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  color: var(--color-text);
  line-height: var(--btn-height-sm);
  font-size: var(--btn-font-sm);
}

/* PRAFTA-003 F1 — 초과근무 저장/반려 액션 영역 */
.ot-actions {
  display: flex;
  gap: var(--header-right-gap);
  justify-content: flex-end;
  margin-top: var(--header-right-gap);
}

.ot-save-btn,
.ot-reject-btn {
  height: var(--btn-height);
  padding: 0 var(--btn-padding);
  border-radius: var(--btn-radius);
  border: var(--card-border);
  font-size: var(--btn-font);
  cursor: pointer;
}

.ot-save-btn {
  background: var(--color-primary);
  color: var(--color-surface);
  border-color: var(--color-primary);
}

.ot-save-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.ot-save-btn:active:not(:disabled) {
  background: var(--color-primary-pressed);
  border-color: var(--color-primary-pressed);
}

.ot-save-btn:disabled {
  background: var(--color-border-strong);
  border-color: var(--color-border-strong);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

.ot-reject-btn {
  background: var(--color-surface);
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.ot-reject-btn:hover:not(:disabled) {
  background: var(--color-bg);
}

.ot-reject-btn:disabled {
  color: var(--color-text-muted);
  border-color: var(--color-border-strong);
  cursor: not-allowed;
}
</style>


<!--
============================================================
F2 patch list — 단순 키 교체 (developer는 grep으로 일괄 교체)
============================================================

대상 파일:
  prafta-web-frontend/prafta-web-frontend/src/views/attd/popup/AttdDayDetailPop.vue

교체 매트릭스 (응답 매핑부 — selectMonthlyAttdReq 결과 사용처):

  line 1310 (reqCards.value 의 map 함수 내부)
    -      aftIn: fmtTime(req.checkInTime) || "-",
    +      aftIn: fmtTime(req.startTime) || "-",

  line 1311
    -      aftOut: fmtTime(req.checkOutTime) || "-",
    +      aftOut: fmtTime(req.endTime) || "-",

  line 1602 (fillSegmentFromReq)
    -  seg.inDate = ymdNumToDash(req.checkInDate) || props.date_p;
    +  seg.inDate = ymdNumToDash(req.startDate) || props.date_p;

  line 1603
    -  seg.in = req.checkInTime || "";
    +  seg.in = req.startTime || "";

  line 1604
    -  seg.outDate = ymdNumToDash(req.checkOutDate) || props.date_p;
    +  seg.outDate = ymdNumToDash(req.endDate) || props.date_p;

  line 1605
    -  seg.out = req.checkOutTime || "";
    +  seg.out = req.endTime || "";

손대지 말 것 (정책서 27행 — 기능 개선 금지, 변경된 컬럼 매칭만):
  - line 1500~1546 fnSave 의 payload 키 (checkInDate 등) — 이건 /update-user-attd-infos
    이며 TB_USER_ATTD_MGMT 직접 갱신. 컬럼 변경 없음.
  - line 1624~1646 fnApproveReq 의 payload — /update-user-attd-requests 호출.
    백엔드(B2-7,8)에서 Param/Command 의 필드명을 변경하면 *동시에* 이 페이로드도
    바꿔야 한다. developer가 백엔드 PR 머지 시점에 함께 처리.
-->
