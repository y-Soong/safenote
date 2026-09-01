-- =====================================================================================
-- 위치정보 동의철회·중지 도입 S1 — 사업장 지오펜스 정비
--
-- 근거 : .claude/refs/위치정보_동의철회_중지_작업지시서.md §2-4 · §3-9 · §6-1
-- 코드 : Baim01ServiceImpl.assertGeofenceConfigured (서버 필수 검증)
--        SiteInfoPop.vue (화면 필수 검증 + 기본값 100m)
-- 작성 : 2026-09-01
--
-- ★★적용 순서
--   1) §1 사전 현황 확인 (개발/운영 각각)
--   2) §2 GPS 반경 정규화 실행
--   3) §3 사후 검증
--   4) §4 중심좌표 결측분은 SQL 로 채울 수 없다 — 화면에서 처리(아래 참조)
--
-- ★개발 DB / 운영 DB 양쪽에 적용한다(마이그레이션 양환경 동시적용 원칙).
-- =====================================================================================


-- =====================================================================================
-- §1. 사전 현황
-- =====================================================================================

-- (1-1) 전체 요약
SELECT COUNT(*)                                              AS SITES
     , SUM(USE_YN = 'Y')                                     AS ACTIVE
     , SUM(LINK_SRC_SITE_CD IS NOT NULL)                     AS MIRRORED
     , SUM(LAT IS NULL OR LON IS NULL)                       AS NO_CENTER
     , SUM(GPS_RANGE IS NULL OR TRIM(GPS_RANGE) = '')        AS NO_RANGE
     , SUM(GPS_RANGE REGEXP '^[0-9]+$' AND CAST(GPS_RANGE AS UNSIGNED) < 10) AS RANGE_TOO_SMALL
     , SUM(ADDR_1 IS NULL OR TRIM(ADDR_1) = '')              AS NO_ADDR
  FROM TB_SITE;

-- (1-2) 반경 값 분포
SELECT IFNULL(GPS_RANGE, '(NULL)') AS GPS_RANGE
     , COUNT(*)                    AS CNT
     , SUM(USE_YN = 'Y')           AS ACTIVE
     , SUM(LINK_SRC_SITE_CD IS NOT NULL) AS MIRRORED
  FROM TB_SITE
 GROUP BY GPS_RANGE
 ORDER BY CNT DESC;

-- (1-3) ★정규화 대상 목록 — 실행 전에 이 목록을 기록해 둔다(사후 대조용).
SELECT CMPNY_CD
     , SITE_CD
     , SITE_NM
     , IFNULL(GPS_RANGE, '(NULL)') AS BEFORE_RANGE
     , USE_YN
     , (LINK_SRC_SITE_CD IS NOT NULL) AS IS_MIRROR
  FROM TB_SITE
 WHERE GPS_RANGE IS NULL
    OR TRIM(GPS_RANGE) = ''
    OR GPS_RANGE NOT REGEXP '^[0-9]+$'
    OR CAST(GPS_RANGE AS UNSIGNED) < 10
 ORDER BY CMPNY_CD, SITE_CD;


-- =====================================================================================
-- §2. GPS 반경 정규화 — 결측·비숫자·10m 미만을 100m 로
--
--   ★'0' 을 반드시 포함하는 이유: 반경 0m 는 지오펜스가 점이 되어 <b>모든 출근을 외근으로 판정</b>한다.
--     운영에 활성 사업장 1곳이 실제로 이 값을 갖고 있었다.
--   ★상한(10,000m)은 손대지 않는다 — 기존 150/200/500 은 현장별로 정한 값이므로 유지한다.
--   ★미러(연동) 사업장도 함께 갱신한다. 원본이 다시 저장될 때 전파값으로 덮이지만,
--     그때까지의 현재 상태가 어긋나 있는 것이 더 위험하다.
--
--   ★★Workbench 안전 업데이트 모드(1175) 우회가 필요하다.
--     TB_SITE 의 PK 는 (CMPNY_CD, SITE_CD) 복합키인데 본 WHERE 절이 키 컬럼을 쓰지 않아
--     "You are using safe update mode..." 로 거부된다. 아래 세션 토글을 UPDATE 와 함께 실행한다.
--     ★SET SQL_SAFE_UPDATES = 1 을 반드시 같이 실행할 것 — 세션 설정이라 커넥션이 살아 있는 동안
--       계속 꺼진 채로 남는다(Preferences 에서 끄면 이후 모든 세션에 영향을 주므로 세션 토글을 쓴다).
-- =====================================================================================

SET SQL_SAFE_UPDATES = 0;

UPDATE TB_SITE
   SET GPS_RANGE = '100'
     , UPDATE_NO = 'SYSTEM'
 WHERE GPS_RANGE IS NULL
    OR TRIM(GPS_RANGE) = ''
    OR GPS_RANGE NOT REGEXP '^[0-9]+$'
    OR CAST(GPS_RANGE AS UNSIGNED) < 10;

SET SQL_SAFE_UPDATES = 1;

-- ※ UPDATE_DATE 는 date 타입이라 시각이 남지 않는다. 정규화 사실은 본 파일과 §1-3 기록으로 남긴다.


-- =====================================================================================
-- §3. 사후 검증 — 아래 세 값이 모두 0 이어야 한다
-- =====================================================================================

SELECT SUM(GPS_RANGE IS NULL OR TRIM(GPS_RANGE) = '')                         AS STILL_NO_RANGE
     , SUM(GPS_RANGE NOT REGEXP '^[0-9]+$')                                   AS STILL_NOT_NUMERIC
     , SUM(GPS_RANGE REGEXP '^[0-9]+$' AND CAST(GPS_RANGE AS UNSIGNED) < 10)  AS STILL_TOO_SMALL
  FROM TB_SITE;


-- =====================================================================================
-- §4. 중심좌표(LAT/LON) 결측 — ★SQL 로 채울 수 없다
--
--   운영 실측(2026-09-01): 사업장 13곳 중 7곳에 중심좌표가 없다. 주소는 13곳 전부 있다.
--   좌표는 주소 지오코딩(카카오)으로만 산출되며 이는 화면(SiteInfoPop)에서 수행된다.
--
--   ★처리 방법: 아래 목록의 사업장을 사업장관리(Baim_01) 화면에서 하나씩 열고
--     [주소찾기] 로 주소를 다시 선택한 뒤 저장한다. 저장 시점에 좌표가 확정된다.
--     (S1 배포 후에는 좌표 없이 저장 자체가 거부되므로, 재저장하면 반드시 채워진다.)
--
--   ★영향 안내: 좌표가 채워지는 순간부터 그 사업장의 외근 판정이 살아난다.
--     종전에는 좌표 결측 → 지오펜스 폴백 → 전부 '온사이트(정상)' 로 처리되고 있었다.
--     정상 출근으로 보이던 건이 외근으로 뒤집힐 수 있고, 외근일 때만 좌표를 저장하므로
--     위치정보 수집량도 늘어난다. <b>고객사에 사전 안내한 뒤 적용한다.</b>
-- =====================================================================================

-- (4-1) 좌표 결측 사업장 목록 — 화면에서 재저장할 대상
SELECT CMPNY_CD
     , SITE_CD
     , SITE_NM
     , ADDR_1
     , USE_YN
     , (LINK_SRC_SITE_CD IS NOT NULL) AS IS_MIRROR
  FROM TB_SITE
 WHERE LAT IS NULL
    OR LON IS NULL
 ORDER BY USE_YN DESC, CMPNY_CD, SITE_CD;

-- (4-2) 완료 확인 — 전부 처리되면 0
SELECT COUNT(*) AS STILL_NO_CENTER
  FROM TB_SITE
 WHERE (LAT IS NULL OR LON IS NULL)
   AND USE_YN = 'Y'
   AND LINK_SRC_SITE_CD IS NULL;   -- 미러는 원본 소유사가 채운다


-- =====================================================================================
-- §5. 롤백
--
--   ★정규화 전 값은 §1-3 결과로만 복원할 수 있다. 실행 전 반드시 기록해 둘 것.
--   반경 정규화 자체는 되돌릴 필요가 거의 없다(결측·0m 는 어떤 경우에도 정상 값이 아니다).
--   문제가 생기면 §1-3 기록을 근거로 개별 UPDATE 한다.
-- =====================================================================================
