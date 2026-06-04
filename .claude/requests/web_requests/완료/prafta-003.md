## /src/views/attd/popup/AttdDayDetailPop.vue 화면 수정요청사항

# 백엔드 
	1. com.prafta.web.attd.attd07에 신규 API 개발
		- post 통신
		- url : update-user-overtime-requests
		- 신규 테이블 사용

	2. com.prafta.web.attd.attd07에 기존 API 수정
		- 테이블 변경(tb_user_attd_req)에 대응할 요소가 있는지 체크 후 필요 시 작업
	
# 프론트 엔드 
	1. AttdDayDetailPop.vue 화면의 추가근무와 관련해서 추가된 UI를 백엔드와 연결지어 완성한다.

	2. 근로자 근무 요청과 관련해서 테이블(tb_user_attd_req) 수정이 영향도가 있는지 체크, 영향도 있을 시 신규 테이블에 맞게끔 수정


# 요청사항 세부
	1. 추가근무는 아래 TB_USER_OVERTIME_MGMT 테이블을 참고한다.
	2. 추가근무는 총 근무시간(표준화 적용 시간 기준) - 스케줄 시간을 한 나머지 시간에 대해서만 등록할 수 있어야 한다.
	3. 2번 조건은 각 구간별로 지켜져야한다.
		Ex1) 스케줄 09:00 ~ 18:00 / 실제 근무 시간 08:42 ~ 21:11, 21:57 ~ 23:23 / 표준화 적용 근무 시간 : 09:00 ~ 21:00, 22:00 ~ 23:00
			: Ex1 케이스에선 초과근무는 1구간 - 18:00 ~ 21:00 범위에서 올릴 수 있고, 2구간 - 22:00 ~ 23:00 범위에서 올릴 수 있다.
		Ex2) 스케줄 03:00 ~ 09:00, 14:00 ~ 20:00 / 실제 근무시간 02:44 ~ 09:31, 12:59 ~ 21:02 /  표준화 적용 근무 시간 : 03:00 ~ 09:30, 13:00 ~ 21:00
			: Ex2 케이스에선 초과근무는 1구간 - 09:00 ~ 09:30, 2구간 - 13:00 ~ 14:00, 20:00 ~ 21:00
	4. tb_user_attd_req 테이블은 기존에 근무 수정 및 생성요청 용도로 사용하려다 사용성을 확장해서 초과근무, 휴가신청 등의 요청까지 범용적으로 담기 위해 테이블 구조를 변경
		: 구조 변경에 따라 기존 com.prafta.web.attd.attd07 코드들을 점검하고 수정할 부분이 있는지 체크해줘 (기존 기능에서 별도의 개선을 하려하지말고 변경된 테이블 구조에 맞춰 바꿀 부분만 바꿔줘)



(신규 테이블)
CREATE TABLE TB_USER_OVERTIME_MGMT (
    OT_ID                   VARCHAR(20)   NOT NULL                COMMENT '초과근무 ID (PK)',
    CMPNY_CD                VARCHAR(50)   NOT NULL                COMMENT '회사 코드',
    SITE_CD                 VARCHAR(50)   NOT NULL                COMMENT '사업장 코드',
    USER_CD                 VARCHAR(20)   NOT NULL                COMMENT '근무자 사용자 코드',
    
    -- 연관 정보
    ATTD_ID                 VARCHAR(20)   NULL                    COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL)',
    REQ_ID                  VARCHAR(20)   NULL                    COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL)',
    
    -- 근무일/근무위치
    WORK_YMD                VARCHAR(8)    NOT NULL                COMMENT '근무 일자 (YYYYMMDD)',
    NODE_CD                 VARCHAR(50)   NULL                    COMMENT '근무 노드 코드',
    
    -- 초과근무 유형 (가산수당 계산용)
    OT_TYPE                 VARCHAR(10)   NOT NULL                COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',
    
    -- 계획 시각 (신청/승인 시점)
    PLAN_START_DATE         VARCHAR(8)    NULL                    COMMENT '계획 시작 일자 (YYYYMMDD)',
    PLAN_START_TIME         VARCHAR(4)    NULL                    COMMENT '계획 시작 시각 (HHMM)',
    PLAN_END_DATE           VARCHAR(8)    NULL                    COMMENT '계획 종료 일자 (YYYYMMDD)',
    PLAN_END_TIME           VARCHAR(4)    NULL                    COMMENT '계획 종료 시각 (HHMM)',
    
    -- 실제 수행 시각 (가산수당 계산 기준)
    ACTUAL_START_DATE       VARCHAR(8)    NOT NULL                COMMENT '실제 시작 일자 (YYYYMMDD)',
    ACTUAL_START_TIME       VARCHAR(4)    NOT NULL                COMMENT '실제 시작 시각 (HHMM)',
    ACTUAL_START_METHOD     VARCHAR(2)    NULL                    COMMENT '시작 체크 방식 (GPS/QR/MANUAL 등)',
    ACTUAL_END_DATE         VARCHAR(8)    NULL                    COMMENT '실제 종료 일자 (YYYYMMDD)',
    ACTUAL_END_TIME         VARCHAR(4)    NULL                    COMMENT '실제 종료 시각 (HHMM)',
    ACTUAL_END_METHOD       VARCHAR(2)    NULL                    COMMENT '종료 체크 방식 (GPS/QR/MANUAL 등)',
    
    -- 근무시간 계산 결과 (가산수당 계산용 캐시)
    WORK_MINUTES            INT           NULL                    COMMENT '실제 근무 시간 (분 단위, 휴게시간 제외)',
    BREAK_MINUTES           INT           NULL  DEFAULT 0          COMMENT '휴게 시간 (분 단위)',
    
    -- OT 상태 (진행 중/완료/취소)
    OT_STATUS               VARCHAR(10)   NOT NULL                COMMENT '초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소)',
    
    -- 공통 관리 컬럼
    DEL_YN                  VARCHAR(1)    NOT NULL  DEFAULT 'N'   COMMENT '삭제 여부',
    INSERT_NO               VARCHAR(50)   NOT NULL                COMMENT '등록자',
    INSERT_DATE             DATETIME      NOT NULL                COMMENT '등록 일시',
    UPDATE_NO               VARCHAR(50)   NULL                    COMMENT '수정자',
    UPDATE_DATE             DATETIME      NULL                    COMMENT '수정 일시',
    
    PRIMARY KEY (OT_ID),
    
    -- 조회 성능용 인덱스
    KEY IDX_OT_USER_YMD     (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD),
    KEY IDX_OT_SITE_YMD     (CMPNY_CD, SITE_CD, WORK_YMD, OT_STATUS),
    KEY IDX_OT_ATTD         (ATTD_ID),
    KEY IDX_OT_REQ          (REQ_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 초과근무 실적 관리';

(테이블 변경 - 기존)
CREATE TABLE `tb_user_attd_req` (
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청고유ID',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근태고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청자(근로자)코드',
  `REQ_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청구분[SYS032]',
  `REQ_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청상태[SYS033]',
  `REQ_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청사유',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청근무일',
  `WORK_SEQ` int DEFAULT NULL COMMENT '요청근무차수',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청소속부서',
  `CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청출근일자',
  `CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청출근시간',
  `CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청퇴근일자',
  `CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청퇴근시간',
  `PROCESS_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자(관리자)코드',
  `PROCESS_COMMENT` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리코멘트(반려사유 등)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`REQ_ID`),
  KEY `idx_req_attd` (`ATTD_ID`),
  KEY `idx_req_user` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`REQ_STATUS`),
  KEY `idx_req_admin` (`CMPNY_CD`,`SITE_CD`,`REQ_STATUS`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근로자 근태 요청'

(테이블 변경 - 신규)
CREATE TABLE TB_USER_ATTD_REQ (
    REQ_ID              VARCHAR(20)   NOT NULL                COMMENT '요청 ID (PK)',
    CMPNY_CD            VARCHAR(50)   NOT NULL                COMMENT '회사 코드',
    SITE_CD             VARCHAR(50)   NOT NULL                COMMENT '사업장 코드',
    USER_CD             VARCHAR(20)   NOT NULL                COMMENT '요청자 사용자 코드',
    
    -- 요청 유형 및 대상
    REQ_TYPE            VARCHAR(10)   NOT NULL                COMMENT '요청 유형 (SYS032: 01~06)',
    TARGET_ID           VARCHAR(20)   NULL                    COMMENT '수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL)',
    
    -- 요청 상태 및 사유
    REQ_STATUS          VARCHAR(10)   NOT NULL                COMMENT '요청 상태 (REQUESTED/APPROVED/REJECTED/CANCELLED)',
    REQ_REASON          VARCHAR(500)  NULL                    COMMENT '요청 사유',
    
    -- 근무일 정보 (근태/초과근무 요청 시 사용)
    WORK_YMD            VARCHAR(8)    NULL                    COMMENT '근무 일자 (YYYYMMDD)',
    NODE_CD             VARCHAR(50)   NULL                    COMMENT '근무 노드 코드',
    WORK_SEQ            INT           NULL                    COMMENT '근무 순번',
    
    -- 시작/종료 시각 (범용 - 요청 유형별로 의미 다름)
    -- 근태:     출근일시 ~ 퇴근일시
    -- 초과근무: OT 시작일시 ~ OT 종료일시
    -- 연차:     휴가 시작일 ~ 휴가 종료일 (TIME은 NULL)
    START_DATE          VARCHAR(8)    NULL                    COMMENT '시작 일자 (YYYYMMDD)',
    START_TIME          VARCHAR(4)    NULL                    COMMENT '시작 시각 (HHMM)',
    END_DATE            VARCHAR(8)    NULL                    COMMENT '종료 일자 (YYYYMMDD)',
    END_TIME            VARCHAR(4)    NULL                    COMMENT '종료 시각 (HHMM)',
    
    -- 초과근무 전용
    OT_TYPE             VARCHAR(10)   NULL                    COMMENT '초과근무 유형 (EXTEND:연장 / NIGHT:야간 / HOLIDAY:휴일)',
    
    -- 연차 전용
    LEAVE_TYPE          VARCHAR(10)   NULL                    COMMENT '연차 유형 (ANNUAL:연차 / HALF_AM:오전반차 / HALF_PM:오후반차 / SICK:병가 / FAMILY:경조사 등)',
    LEAVE_DAYS          DECIMAL(3,1)  NULL                    COMMENT '사용 일수 (0.5, 1.0, 2.0...)',
    
    -- 처리(승인/반려) 정보
    PROCESS_USER_CD     VARCHAR(20)   NULL                    COMMENT '처리자 사용자 코드',
    PROCESS_COMMENT     VARCHAR(500)  NULL                    COMMENT '처리 코멘트',
    PROCESS_DATE        DATETIME      NULL                    COMMENT '처리 일시',
    
    -- 공통 관리 컬럼
    DEL_YN              VARCHAR(1)    NOT NULL  DEFAULT 'N'   COMMENT '삭제 여부',
    INSERT_NO           VARCHAR(50)   NOT NULL                COMMENT '등록자',
    INSERT_DATE         DATETIME      NOT NULL                COMMENT '등록 일시',
    UPDATE_NO           VARCHAR(50)   NULL                    COMMENT '수정자',
    UPDATE_DATE         DATETIME      NULL                    COMMENT '수정 일시',
    
    PRIMARY KEY (REQ_ID),
    
    -- 조회 성능용 인덱스
    KEY IDX_ATTD_REQ_USER     (CMPNY_CD, SITE_CD, USER_CD, REQ_STATUS),
    KEY IDX_ATTD_REQ_STATUS   (CMPNY_CD, SITE_CD, REQ_STATUS, REQ_TYPE),
    KEY IDX_ATTD_REQ_WORK_YMD (CMPNY_CD, SITE_CD, WORK_YMD),
    KEY IDX_ATTD_REQ_TARGET   (TARGET_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 근태 관련 요청 관리';