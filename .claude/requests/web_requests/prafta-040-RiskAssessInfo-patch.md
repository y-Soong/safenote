# PRAFTA-040-5 — RiskAssessInfo.vue "아차사고로 전환" 액션 (골격 패치 명세)

> 대상: `prafta-web-frontend/prafta-web-frontend/src/views/risk/popup/RiskAssessInfo.vue` (기존 1907줄 화면 보완).
> 본 파일은 **정확한 삽입 위치를 지정한 패치 명세**다. planner 환경에서 Edit 도구 부재 + 파일 내 대형 print-HTML 문자열 보존 위험 때문에 전체 재작성 대신 패치 명세로 제공한다(골격 수준 허용 범위). developer가 아래 2개 삽입을 그대로 적용한다.
> 신규 색상/픽셀 없음(기존 `.btn-report` 토큰 재사용). script는 TODO 마커만.

---

## 패치 ① — footer 좌측에 [아차사고로 전환] 버튼 추가

위치: `<div class="footer-buttons-left">` 블록 내, 기존 "개선완료보고서" 버튼 `</button>` 바로 다음 줄(현 라인 346~347 사이).

추가할 마크업:
```html
<!-- PRAFTA-040-5: 아차사고로 전환 (설계 §4-B). 완료(003)/마감(004) 외 상태에서만 노출. -->
<button
  class="btn btn-report"
  v-if="
    props.riskAssessmentData.assessmentStatus != '003' &&
    props.riskAssessmentData.assessmentStatus != '004'
  "
  @click="fnConvertToNearMiss()"
>
  아차사고로 전환
</button>
```

## 패치 ② — 전환 핸들러 추가 (script)

위치: `<script setup>` 내, `// 정리 onBeforeUnmount(...)` 바로 위(현 라인 1446 직전).

추가할 코드:
```js
// PRAFTA-040-5: 위험성평가요청 → 아차사고 재분류 (설계 §4-B)
const fnConvertToNearMiss = async () => {
  const ok = await proxy.$confirm(
    "이 위험성평가요청을 아차사고로 전환합니다. 계속할까요?"
  );
  if (!ok) return;

  // TODO(developer): POST /webApi/nearmiss01/reclassify-from-assessment
  //   요청: srcProcessCd=props.riskAssessmentData.processCd,
  //         srcAssessmentCd=props.riskAssessmentData.assessmentCd,
  //         (추가 입력 필요 시 사건유형/발생일시/경위 등은 후속 입력 폼 또는 기본값)
  //   성공 시:
  //     1) 원 tb_risk_assessment 이관 처리 — 처리방식 미확정(결정필요 D2:
  //        '이관' 상태값 신설 vs USE_YN='N'). 서버에서 확정 후 처리.
  //     2) props.onSave 콜백으로 목록 새로고침 + emit('close').
  //     3) 중복 전환(이미 전환된 건) 방지 — 서버 검증 결과 alert 처리.
};
```

---

## 적용 시 주의
- 버튼 노출 조건은 기존 저장 버튼과 동일(`assessmentStatus != '003' && != '004'`). 결정필요 D2 확정 전이라도 버튼·confirm까지는 노출 가능하나, 실제 전환 API/이관 로직은 D2 확정 후 구현.
- `proxy`(getCurrentInstance), `props` 는 기존 파일에 이미 선언되어 있어 추가 import 불필요.
- 백엔드 의존: PRAFTA-040-2 E6 `/webApi/nearmiss01/reclassify-from-assessment`.
