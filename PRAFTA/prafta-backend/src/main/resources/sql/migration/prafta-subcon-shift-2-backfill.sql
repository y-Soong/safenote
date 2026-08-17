-- ============================================================================
-- SHIFT-LINK-T6 — 기존 ACTIVE 링크 교대 정의 4테이블 백필(일회성)
-- 작성일: 2026-08-17
-- 적용 환경: MySQL 8 (개발 DB 먼저 → 운영 DB 순차. 실행은 사용자 Workbench 수동 — 본 파일은 작성만)
-- 출처: 작업지시서_교대근무타입-사업장연동-복제전파 §3.2(e) / plan SHIFT-LINK-T6
--
-- 실행 전제:
--   1. prafta-subcon-shift-1-link-src-ddl.sql (LINK_SRC 컬럼) 선적용 필수.
--   2. T1~T5 코드 **배포 후 실행 권장** — 백필만 먼저 하면 이후 원청이 교대를 새로 만들 때
--      전파(T3)가 없어 원본-미러 정합 공백이 생긴다.
--   3. §A 사전 검증에서 결손이 1건이라도 나오면 백필을 **중단하고 사용자 보고**
--      (중지된 SCH_CD 를 참조하는 교대 처리 방침은 그때 결정 — plan §5-3).
--
-- 설계:
--   - 링크 구동형(set-based): TB_SITE_LINK 의 STATUS='ACTIVE' AND DST_SITE_CD IS NOT NULL 행을
--     드라이버로 4테이블 INSERT...SELECT + NOT EXISTS 안티조인(DST 에 동일 SHIFT_CD 부재 시만).
--     LINK_ID 하드코딩 없음 → 링크 현황이 다른 개발 DB 에도 같은 파일 그대로 적용 가능.
--   - 체인 해소 = 반복 실행: §B 블록을 "신규 INSERT 건수가 0 이 될 때까지" 반복 실행한다.
--     (운영 체인 깊이 2: 001→mLP5...→IqzQ... — 2회면 수렴, 안전상 3회차에 0건 확인.)
--     LINK_ID 1→5 순서 의존은 반복 실행으로 자연 해소된다.
--   - 부모·자식은 한 실행 단위: 부모(§B-1)를 먼저 INSERT 한 뒤, 자식 3문(§B-2~4)은
--     "DST 부모 존재 + DST 자식 0건" 술어로 같은 회차에 채운다.
--   - 백필 값 = 원본 행 그대로 + LINK_SRC_CMPNY_CD=SRC, LINK_SRC_SHIFT_CD=원본 SHIFT_CD,
--     INSERT_NO/UPDATE_NO='SYSTEM', 부모 USE_YN='Y' 필터(초기 복제 T2 와 동일 원칙).
--
-- 운영 ACTIVE 링크 5건 기대표 (2026-08-17 실측 — 지시서 §3.2(e)):
--   | LINK_ID | SRC                            | DST                            | 비고 |
--   |       1 | 001/00001                      | mLP5JWe5EOFPS17zZOKj/00002     |      |
--   |       5 | mLP5JWe5EOFPS17zZOKj/00002     | IqzQKPKMpu75RsCoDN6m/00002     | 체인(1 의 미러가 SRC — 2회차에 채워짐) |
--   |       6 | 001/00001                      | DpqyoXhpHsAKhHxymCZP/00003     | 중곡사업장(본 건 발단) |
--   |       4 | nrTnBjSa2woeztqfPGIP/00001     | TOUAfi60vmh8qrIXypdw/00003     |      |
--   |       3 | TOUAfi60vmh8qrIXypdw/00002     | BftXVyADUca25jZaSqzu/00002     |      |
-- ============================================================================


-- ============================================================================
-- §A. 사전 검증 — SRC 교대의 PTRN/ASSIGN 이 참조하는 SCH_CD 가 DST 미러 TB_SCH_MGMT 에
--     존재하는지 확인. (초기 복제가 USE_YN='Y' 만 복제했으므로, 원청에서 이후 중지된
--     SCH_CD 를 참조하는 교대가 있으면 결손 가능 — FK 가 없어 INSERT 자체는 성공하나
--     미러 화면에서 참조가 깨진다.)
--     §A-3 은 DST 자체 교대와의 SHIFT_CD 충돌 사전검증(qa D-2).
--     ※ 세 쿼리 모두 0건이어야 §B 진행. 1건이라도 나오면 중단 + 사용자 보고.
--     ※ (qa D-3) 체인 링크가 존재하면 §B 반복 회차에서 SRC 데이터가 새로 노출되므로,
--        §B 회차 사이에 §A 를 재실행해 신규 노출된 SRC 를 재검증할 것.
-- ============================================================================

-- §A-1. 패턴 회차(PTRN)의 SCH_CD 결손 목록
SELECT
    L.LINK_ID
    , L.SRC_CMPNY_CD
    , L.SRC_SITE_CD
    , L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , P.SHIFT_CD
    , P.PTRN_IDX
    , P.SCH_CD
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD AND S.USE_YN = 'Y')
    INNER JOIN tb_shift_sch_ptrn_mgmt P
        ON (P.CMPNY_CD = S.CMPNY_CD AND P.SITE_CD = S.SITE_CD AND P.SHIFT_CD = S.SHIFT_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND NOT EXISTS (
        SELECT 1
          FROM tb_sch_mgmt DS
         WHERE DS.CMPNY_CD = L.DST_CMPNY_CD
           AND DS.SITE_CD  = L.DST_SITE_CD
           AND DS.SCH_CD   = P.SCH_CD
  )
LIMIT 100;

-- §A-2. 배정표(ASSIGN)의 SCH_CD 결손 목록 (SCH_CD 는 NULL 허용 — 휴무 행 제외)
SELECT
    L.LINK_ID
    , L.SRC_CMPNY_CD
    , L.SRC_SITE_CD
    , L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , A.SHIFT_CD
    , A.TEAM_IDX
    , A.DAY_NO
    , A.SCH_CD
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD AND S.USE_YN = 'Y')
    INNER JOIN tb_shift_sch_assign_mgmt A
        ON (A.CMPNY_CD = S.CMPNY_CD AND A.SITE_CD = S.SITE_CD AND A.SHIFT_CD = S.SHIFT_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND A.SCH_CD IS NOT NULL
  AND NOT EXISTS (
        SELECT 1
          FROM tb_sch_mgmt DS
         WHERE DS.CMPNY_CD = L.DST_CMPNY_CD
           AND DS.SITE_CD  = L.DST_SITE_CD
           AND DS.SCH_CD   = A.SCH_CD
  )
LIMIT 100;

-- §A-3. DST 자체 교대 충돌 사전검증 (qa D-2) — DST 사업장에 LINK_SRC 가 NULL 인 자체 교대가
--       SRC 와 동일 SHIFT_CD 로 존재하는지 확인. 존재하면 §B-1 은 조용히 스킵되어
--       원본-미러 정합 공백이 남고, 이후 원청 신규 교대의 전파(T3)가 코드 충돌 시
--       원청 저장 자체를 롤백시킬 수 있다.
--       ※ 1건이라도 나오면 백필 실행을 **중단하고 사용자 보고** (해당 자체 교대 처리 방침
--          결정 후 재개 — 임의 삭제/변경 금지).
SELECT
    L.LINK_ID
    , L.SRC_CMPNY_CD
    , L.SRC_SITE_CD
    , L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , D.SHIFT_CD
    , D.SHIFT_NO
    , D.USE_YN
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD)
    INNER JOIN tb_shift_sch_mgmt D
        ON (D.CMPNY_CD = L.DST_CMPNY_CD AND D.SITE_CD = L.DST_SITE_CD AND D.SHIFT_CD = S.SHIFT_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND D.LINK_SRC_CMPNY_CD IS NULL
LIMIT 100;


-- ============================================================================
-- §B. 백필 본문 — 아래 4문(§B-1~4)을 **한 세트로 순서대로** 실행하고,
--     §B-1 의 신규 INSERT 건수(affected rows)가 0 이 될 때까지 세트 전체를 반복 실행한다.
--     (체인 하위 링크는 상위 회차에서 SRC 데이터가 생긴 뒤에야 채워진다.)
--     멱등: NOT EXISTS 안티조인이라 재실행해도 중복 INSERT 없음.
--     ※ (qa D-3) 체인 링크 존재 시 §B 회차 사이에 §A(특히 §A-1·§A-2·§A-3)를 재실행해
--        직전 회차로 신규 노출된 SRC 를 재검증한 뒤 다음 회차를 진행할 것.
-- ============================================================================

-- §B-1. 부모: TB_SHIFT_SCH_MGMT (원본 USE_YN='Y' + DST 에 동일 SHIFT_CD 부재 시만)
INSERT INTO tb_shift_sch_mgmt (
      CMPNY_CD
    , SITE_CD
    , SHIFT_CD
    , SHIFT_NO
    , SHIFT_PTRN_CNT
    , SHIFT_TEAM_CNT
    , SHIFT_CYCLE_DAYS
    , USE_YN
    , LINK_SRC_CMPNY_CD
    , LINK_SRC_SHIFT_CD
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , S.SHIFT_CD
    , S.SHIFT_NO
    , S.SHIFT_PTRN_CNT
    , S.SHIFT_TEAM_CNT
    , S.SHIFT_CYCLE_DAYS
    , S.USE_YN
    , L.SRC_CMPNY_CD
    , S.SHIFT_CD
    , 'SYSTEM'
    , NOW()
    , 'SYSTEM'
    , NOW()
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND S.USE_YN = 'Y'
  AND NOT EXISTS (
        SELECT 1
          FROM tb_shift_sch_mgmt D
         WHERE D.CMPNY_CD = L.DST_CMPNY_CD
           AND D.SITE_CD  = L.DST_SITE_CD
           AND D.SHIFT_CD = S.SHIFT_CD
  );

-- §B-2. 자식: TB_SHIFT_SCH_PTRN_MGMT (DST 부모 존재 + DST 자식 0건인 SHIFT_CD 만 — 부모·자식 정합 유지)
INSERT INTO tb_shift_sch_ptrn_mgmt (
      CMPNY_CD
    , SITE_CD
    , SHIFT_CD
    , PTRN_IDX
    , SCH_CD
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , P.SHIFT_CD
    , P.PTRN_IDX
    , P.SCH_CD
    , 'SYSTEM'
    , NOW()
    , 'SYSTEM'
    , NOW()
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD AND S.USE_YN = 'Y')
    INNER JOIN tb_shift_sch_ptrn_mgmt P
        ON (P.CMPNY_CD = S.CMPNY_CD AND P.SITE_CD = S.SITE_CD AND P.SHIFT_CD = S.SHIFT_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND EXISTS (
        SELECT 1
          FROM tb_shift_sch_mgmt DM
         WHERE DM.CMPNY_CD = L.DST_CMPNY_CD
           AND DM.SITE_CD  = L.DST_SITE_CD
           AND DM.SHIFT_CD = S.SHIFT_CD
           -- SEC-2: 자식 부착 대상을 "이 링크의 SRC 에서 온 미러 부모"로 한정
           --        (DST 자체 교대가 동일 SHIFT_CD 로 존재해도 원청 자식이 오귀속되지 않도록)
           AND DM.LINK_SRC_CMPNY_CD = L.SRC_CMPNY_CD
           AND DM.LINK_SRC_SHIFT_CD = S.SHIFT_CD
  )
  AND NOT EXISTS (
        SELECT 1
          FROM tb_shift_sch_ptrn_mgmt DP
         WHERE DP.CMPNY_CD = L.DST_CMPNY_CD
           AND DP.SITE_CD  = L.DST_SITE_CD
           AND DP.SHIFT_CD = S.SHIFT_CD
  );

-- §B-3. 자식: TB_SHIFT_SCH_TEAM_META_INFO (술어 동일)
INSERT INTO tb_shift_sch_team_meta_info (
      CMPNY_CD
    , SITE_CD
    , SHIFT_CD
    , TEAM_IDX
    , TEAM_NM
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , T.SHIFT_CD
    , T.TEAM_IDX
    , T.TEAM_NM
    , 'SYSTEM'
    , NOW()
    , 'SYSTEM'
    , NOW()
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD AND S.USE_YN = 'Y')
    INNER JOIN tb_shift_sch_team_meta_info T
        ON (T.CMPNY_CD = S.CMPNY_CD AND T.SITE_CD = S.SITE_CD AND T.SHIFT_CD = S.SHIFT_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND EXISTS (
        SELECT 1
          FROM tb_shift_sch_mgmt DM
         WHERE DM.CMPNY_CD = L.DST_CMPNY_CD
           AND DM.SITE_CD  = L.DST_SITE_CD
           AND DM.SHIFT_CD = S.SHIFT_CD
           -- SEC-2: 자식 부착 대상을 미러 부모로 한정 (§B-2 와 동일 취지)
           AND DM.LINK_SRC_CMPNY_CD = L.SRC_CMPNY_CD
           AND DM.LINK_SRC_SHIFT_CD = S.SHIFT_CD
  )
  AND NOT EXISTS (
        SELECT 1
          FROM tb_shift_sch_team_meta_info DT
         WHERE DT.CMPNY_CD = L.DST_CMPNY_CD
           AND DT.SITE_CD  = L.DST_SITE_CD
           AND DT.SHIFT_CD = S.SHIFT_CD
  );

-- §B-4. 자식: TB_SHIFT_SCH_ASSIGN_MGMT (술어 동일)
INSERT INTO tb_shift_sch_assign_mgmt (
      CMPNY_CD
    , SITE_CD
    , SHIFT_CD
    , TEAM_IDX
    , DAY_NO
    , ASSIGN_YN
    , SCH_CD
    , INSERT_NO
    , INSERT_DATE
    , UPDATE_NO
    , UPDATE_DATE
)
SELECT
      L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , A.SHIFT_CD
    , A.TEAM_IDX
    , A.DAY_NO
    , A.ASSIGN_YN
    , A.SCH_CD
    , 'SYSTEM'
    , NOW()
    , 'SYSTEM'
    , NOW()
FROM tb_site_link L
    INNER JOIN tb_shift_sch_mgmt S
        ON (S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD AND S.USE_YN = 'Y')
    INNER JOIN tb_shift_sch_assign_mgmt A
        ON (A.CMPNY_CD = S.CMPNY_CD AND A.SITE_CD = S.SITE_CD AND A.SHIFT_CD = S.SHIFT_CD)
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
  AND EXISTS (
        SELECT 1
          FROM tb_shift_sch_mgmt DM
         WHERE DM.CMPNY_CD = L.DST_CMPNY_CD
           AND DM.SITE_CD  = L.DST_SITE_CD
           AND DM.SHIFT_CD = S.SHIFT_CD
           -- SEC-2: 자식 부착 대상을 미러 부모로 한정 (§B-2 와 동일 취지)
           AND DM.LINK_SRC_CMPNY_CD = L.SRC_CMPNY_CD
           AND DM.LINK_SRC_SHIFT_CD = S.SHIFT_CD
  )
  AND NOT EXISTS (
        SELECT 1
          FROM tb_shift_sch_assign_mgmt DA
         WHERE DA.CMPNY_CD = L.DST_CMPNY_CD
           AND DA.SITE_CD  = L.DST_SITE_CD
           AND DA.SHIFT_CD = S.SHIFT_CD
  );


-- ============================================================================
-- §C. 사후 검증
-- ============================================================================

-- §C-1. 링크별 원본(USE_YN='Y') 대비 미러 4테이블 건수 비교 — srcXxx = dstXxx 여야 정상.
--   (원본 카운트도 부모 USE_YN='Y' 스코프 — 백필이 활성분만 복제했으므로 동일 기준 비교.)
SELECT
    L.LINK_ID
    , L.SRC_CMPNY_CD
    , L.SRC_SITE_CD
    , L.DST_CMPNY_CD
    , L.DST_SITE_CD
    , (SELECT COUNT(*) FROM tb_shift_sch_mgmt S
        WHERE S.CMPNY_CD = L.SRC_CMPNY_CD AND S.SITE_CD = L.SRC_SITE_CD AND S.USE_YN = 'Y')      AS srcShiftCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_mgmt D
        WHERE D.CMPNY_CD = L.DST_CMPNY_CD AND D.SITE_CD = L.DST_SITE_CD)                          AS dstShiftCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_ptrn_mgmt P
            INNER JOIN tb_shift_sch_mgmt S2
                ON (S2.CMPNY_CD = P.CMPNY_CD AND S2.SITE_CD = P.SITE_CD AND S2.SHIFT_CD = P.SHIFT_CD)
        WHERE P.CMPNY_CD = L.SRC_CMPNY_CD AND P.SITE_CD = L.SRC_SITE_CD AND S2.USE_YN = 'Y')      AS srcPtrnCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_ptrn_mgmt DP
        WHERE DP.CMPNY_CD = L.DST_CMPNY_CD AND DP.SITE_CD = L.DST_SITE_CD)                        AS dstPtrnCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_team_meta_info T
            INNER JOIN tb_shift_sch_mgmt S3
                ON (S3.CMPNY_CD = T.CMPNY_CD AND S3.SITE_CD = T.SITE_CD AND S3.SHIFT_CD = T.SHIFT_CD)
        WHERE T.CMPNY_CD = L.SRC_CMPNY_CD AND T.SITE_CD = L.SRC_SITE_CD AND S3.USE_YN = 'Y')      AS srcTeamMetaCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_team_meta_info DT
        WHERE DT.CMPNY_CD = L.DST_CMPNY_CD AND DT.SITE_CD = L.DST_SITE_CD)                        AS dstTeamMetaCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_assign_mgmt A
            INNER JOIN tb_shift_sch_mgmt S4
                ON (S4.CMPNY_CD = A.CMPNY_CD AND S4.SITE_CD = A.SITE_CD AND S4.SHIFT_CD = A.SHIFT_CD)
        WHERE A.CMPNY_CD = L.SRC_CMPNY_CD AND A.SITE_CD = L.SRC_SITE_CD AND S4.USE_YN = 'Y')      AS srcAssignCnt
    , (SELECT COUNT(*) FROM tb_shift_sch_assign_mgmt DA
        WHERE DA.CMPNY_CD = L.DST_CMPNY_CD AND DA.SITE_CD = L.DST_SITE_CD)                        AS dstAssignCnt
FROM tb_site_link L
WHERE L.STATUS = 'ACTIVE'
  AND L.DST_SITE_CD IS NOT NULL
ORDER BY L.LINK_ID ASC
LIMIT 100;
-- ※ dst 카운트는 "미러에 원래 있던 자체 교대"까지 포함하므로, dst > src 면 자체 생성분이
--    있는지 LINK_SRC_CMPNY_CD IS NULL 로 구분 확인. (운영은 미러 교대 0건 실측이라 동수 기대.)

-- §C-2. 중곡사업장(완료 기준 6번) — 원청 교대 3건이 보여야 함(운영 기대 3행).
SELECT
    M.CMPNY_CD
    , M.SITE_CD
    , M.SHIFT_CD
    , M.SHIFT_NO
    , M.USE_YN
    , M.LINK_SRC_CMPNY_CD
    , M.LINK_SRC_SHIFT_CD
FROM tb_shift_sch_mgmt M
WHERE M.CMPNY_CD = 'DpqyoXhpHsAKhHxymCZP'
  AND M.SITE_CD  = '00003'
ORDER BY M.SHIFT_CD ASC
LIMIT 50;

-- §C-3. 복제 금지 확인 — 실인원 계열(TEAM_MGMT/TEAM_USER)은 본 백필이 접촉하지 않는다.
--       (INSERT_NO 는 두 테이블의 DEFAULT 가 'SYSTEM' 이라 판정 기준이 못 됨 — 건수 비교로 확인.)
--       아래 두 카운트를 §B 실행 **전과 후**에 각각 실행해 값이 동일함을 확인한다.
SELECT COUNT(*) AS teamMgmtCnt
FROM tb_shift_sch_team_mgmt G
    INNER JOIN tb_site_link L
        ON (L.DST_CMPNY_CD = G.CMPNY_CD AND L.DST_SITE_CD = G.SITE_CD AND L.STATUS = 'ACTIVE');

SELECT COUNT(*) AS teamUserCnt
FROM tb_shift_sch_team_user U
    INNER JOIN tb_site_link L
        ON (L.DST_CMPNY_CD = U.CMPNY_CD AND L.DST_SITE_CD = U.SITE_CD AND L.STATUS = 'ACTIVE');
