# **9. 데이터 구조 (확장 가능)**

4종 요청을 단일 엔티티 + 서브타입 페이로드로 모델링한다. 향후 신규 요청 유형(예: 외출/조퇴 신청, 출장) 추가 시 페이로드 추가만으로 확장 가능.

## **9.1 공통 엔티티 — ApprovalRequest**

| **필드** | **타입** | **설명** |
| --- | --- | --- |
| id | string (REQ-…) | 요청 식별자. 유형 접두어 — 스케줄 SC, 보정 AT, 초과근무 OT, 연차 AL |
| type | enum | schedule_edit / attendance_correction / overtime / annual_leave |
| subtype | enum (선택) | 예: overtime의 early/extension/holiday/no_schedule, correction의 missing/duplicate/order/gps/time_leave |
| status | enum | pending / approved / rejected / cancelled |
| requester | User ref | 요청자 (사번·이름·소속 부서 스냅샷 동시 저장 — 조직 변경 후에도 이력 유지) |
| siteId / orgNodeId | FK | 사업장·조직 노드 — 권한 스코프 판정과 마감 기준일 조회용 |
| targetDate / targetRange | Date / DateRange | 요청 대상 일자(또는 구간) |
| payload | JSON (서브타입) | §9.2 서브타입별 페이로드 참조 |
| reason | text | 요청자 사유 |
| attachments | File[] | 증빙 첨부 (이름·크기·MIME·업로드 시각) |
| lock | Lock? | 선점 정보 — userId·startedAt. 없으면 미잠금. (공통 §9) |
| decision | Decision? | 처리 결과 — approverId·decidedAt·mode(asis/adjust/reject)·adjustedPayload·comment |
| history | Event[] | 상신·자동계산·잠금·처리·알림 등 이벤트 시퀀스 (timestamp·actor·action·meta) |
| deadlineAt | Date | 마감 기준일에서 계산되는 처리 마감(영업일). 사업장 마스터 → 마감 기준일 + 휴일 정책으로 산출 |
| createdAt / updatedAt | Date | 표준 메타 |

## **9.2 서브타입 페이로드**

### **스케줄 수정 (schedule_edit)**

- before: { workCode, segments: [{startAt, endAt, breakMin}] }

- after:  { workCode, segments: [{startAt, endAt, breakMin}] }

### **근태 보정 (attendance_correction)**

- correctionType: missing | duplicate | order | gps | time_leave | date_attribution

- originalLogs: TimeLog[]  (원본 로그 — 보존, 화면 표시용 스냅샷)

- correctedLogs: TimeLog[] (보정 결과)

### **초과근무 (overtime)**

- claimMode: pre | post   (사전/사후)

- claimedRange: { startAt, endAt }

- systemCalculated: { startAt, endAt, minutes }   // 표준화 단위·면제 시간 반영

- approvedRange?: { startAt, endAt }              // 승인값(조정 후 승인 시)

### **연차 (annual_leave)**

- leaveTypeCode, unit: day | half | hour

- appliedRange: { startAt, endAt } or { date, halfPart }

- approvalSteps: [{stepNo, approverId, decision, decidedAt}]   // 다단 결재 추적

- hrFinal: boolean

## **9.3 권한·스코프와의 연동**

- 가시 범위 판정: requester.orgNodeId가 (a) 본인 자손 노드이거나 (b) 본인이 사업장 권한을 가진 타 사업장이면 노출 (공통 §8.4).

- 처리 권한 판정: 가시 범위 + (자체근태승인 ON 노드의 담당 정/부 또는 위임 상위 노드의 담당 정/부). HR은 결재단계 다단·HR최종 ON 케이스.

- 본인 결재 판정: requester.id === approver.id → §9.5 자기 승인 원칙 분기.

## **9.4 마감 기준일 데이터 연동**

- Site 마스터: scheduleCloseRule(매월 N영업일) / attendanceCloseRule(매월 N영업일) — 메모리 결정사항.

- 각 요청의 deadlineAt = 해당 사업장 attendanceCloseRule을 휴일 마스터(공휴일·사업장별 휴일)로 영업일 변환.

- 자동 마감 금지(메모리 결정) — 마감 도래해도 차단 사유 잔존 시 마감 지연 상태로 표시. 본 화면 알림 배너는 마감 관리 화면과 같은 미결 카운트에 묶임.

