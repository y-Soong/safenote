-- PRAFTA_COM_001 T7-17 교육 시간(인정시간) 컬럼 신설
-- 사용자가 교육 완료 시 인정받는 시간(분). 1~60 정수. 교육준비 시작(prepare) 시 필수.
-- 개설/수정/임시저장은 NULL 허용(있으면 1~60 검증), 교육준비 전이에서 DB값 기준 필수 검증.
-- 코드성 컬럼 아님(단순 수치) → 일반 COMMENT. 운영 선적용 필수.

ALTER TABLE tb_tbm_session
  ADD COLUMN EDU_MINUTES SMALLINT NULL
  COMMENT '교육 인정시간(분, 1~60). 교육준비 시작 시 필수'
  AFTER GPS_VERIFY_RADIUS_M;
