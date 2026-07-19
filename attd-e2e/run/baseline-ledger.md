# E0 기준선 스냅샷 (2026-07-17 밤, 실행 개시 전)

## 원장 (tb_user_leave_grant / tb_user_leave_use)
- **QT 계정 전원(QTHR·QTUSERA/B/C/D/F/G) GRANT 0건 · use 0건** — 완전 클린 상태.
  - ⚠️ 7-12 QT 테스트 당시와 불일치(당시 grant/use 다수 적재) → cleanup 부분 실행 또는 타 작업 영향 추정. **아침 보고 항목**.
  - E0에서 신규 GRANT 시드 필요(웹 Attd_09 부여관리 UI): QTUSERA/C/D/G + 신설 H/I.
- 회사 001 전체 GRANT 총 258건(타 계정) — QE는 QT 계정만 사용하므로 간섭 없음.

## 근무계획 (tb_user_work_plan)
- 00010 계정(A/C/D/G): **0건** → E0에서 웹 Attd_05로 7/17~8/31 배정 필요(기본 QT8H=00001).
- QTUSERB·QTUSERF(00003): 20260713~20261231 각 124건 존재(소속이동 발효 산출물 포함 추정 — QE-6-4에서 SITE_CD 분리 검증).

## 휴일 (tb_holiday, 2026-07~08, CMPNY 001)
| YMD(KST) | 명칭 | TYPE | USE_YN |
|---|---|---|---|
| 07-15 | [QT-0] QT test holiday | 02 | **N**(해제됨) |
| 07-17(오늘) | 제헌절 | 01(공공동기화) | Y |
| 07-20 | [QT-10-2] holiday on confirmed leave day | 02 | Y |
| 07-31 | [QT-10-1] holiday on pending leave day | 02 | Y |
| 08-15 | 광복절 | 01 | Y |
| 08-17 | 대체공휴일(광복절) | 01 | Y |
- ⚠️ 오늘(7/17)=시스템상 휴일 → "오늘" 출퇴근 케이스는 휴일 출근 조건(지시서 §1.4 감안 규정).
- ⚠️ QE-3-x에서 8/3·8/4·8/5·8/6·8/7 신규 등록 예정 — 기존 8월 휴일(8/15·8/17)과 겹치지 않음 확인.

## 사업장 GPS (tb_site)
- 00010(QT): LAT/LON **null**, GPS_RANGE null → 지오펜스 미적용(좌표 무관 출퇴근 가능).
- 00003(여수): null/0 → 동일.
- 00001(중곡): 37.5532178/126.9377458, 150m → 지오펜스 케이스 필요 시 활용.

## 계정
- QTHR(hr)·QTUSERA/C/D/G(00010)·QTUSERB/F(00003)·QTDAILY1(D2026071200017)·QTGLOBALOK1(00001) 생존, USE_YN=Y.
- QTUSERE 부재 확인(지시서 규약대로 사용 금지).

## 오염 판정 기준선
- QT 계정 원장: GRANT=use=0 → 이후 모든 잔액은 QE 적재분. GRANT.USED_DAYS = SUM(use CONFIRMED) 정합 쿼리로 세션마다 검증.
