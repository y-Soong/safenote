-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: prafta
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `seq_site_cd`
--

DROP TABLE IF EXISTS `seq_site_cd`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seq_site_cd` (
  `DATE_PREFIX` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `LAST_SEQ` int DEFAULT '0',
  PRIMARY KEY (`DATE_PREFIX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_aprv_line_preset`
--

DROP TABLE IF EXISTS `tb_aprv_line_preset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_aprv_line_preset` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `PRESET_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '프리셋 ID (회사별 채번: P + YYYYMMDD + SEQ)',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '소유 사용자 (본인 프리셋)',
  `PRESET_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '프리셋 이름',
  `DEFAULT_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '기본 프리셋 여부 (사용자당 최대 1개)',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`PRESET_ID`),
  KEY `IX_TB_APRV_LINE_PRESET_OWNER` (`CMPNY_CD`,`USER_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 결재라인 프리셋 (사용자별 마스터)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_aprv_line_preset_d`
--

DROP TABLE IF EXISTS `tb_aprv_line_preset_d`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_aprv_line_preset_d` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `PRESET_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '프리셋 ID (tb_aprv_line_preset.PRESET_ID)',
  `STEP_NO` int NOT NULL COMMENT '결재 단계 순서 (1부터)',
  `APPROVER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '지정 결재자',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`PRESET_ID`,`STEP_NO`),
  KEY `IX_TB_APRV_LINE_PRESET_D_APPROVER` (`APPROVER_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 결재라인 프리셋 디테일 (결재 순서)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_attd_close`
--

DROP TABLE IF EXISTS `tb_attd_close`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_attd_close` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '*' COMMENT '마감 대상 부서 노드 (전체 사업장 마감은 ''*'')',
  `INC_SUB_YN` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '하위부서 포함 여부 (Y 포함 / N 해당부서만)',
  `CLOSE_YM` char(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '마감 기준월 (YYYYMM)',
  `CLOSE_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT '마감 상태 (OPEN 미마감 / CLOSED 마감)',
  `CLOSE_DTIME` datetime DEFAULT NULL COMMENT '마감 일시',
  `CLOSE_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '마감자 사용자 코드',
  `UNCLOSE_DTIME` datetime DEFAULT NULL COMMENT '마감 해제 일시',
  `UNCLOSE_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '마감 해제자 사용자 코드',
  `CLOSE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '마감/해제 사유',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`NODE_CD`,`CLOSE_YM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 마감 상태 (회사+사업장+월)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_attd_close_hist`
--

DROP TABLE IF EXISTS `tb_attd_close_hist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_attd_close_hist` (
  `HIST_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (FNC_CMM_SEQ_NEXTVAL, PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `CLOSE_YM` char(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '마감 기준월 (YYYYMM)',
  `NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '*' COMMENT '마감 대상 부서 노드 (전체 사업장 마감은 ''*'')',
  `INC_SUB_YN` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '하위부서 포함 여부 (Y 포함 / N 해당부서만)',
  `ACTION_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '액션 (CLOSE 마감 / UNCLOSE 해제)',
  `ACTION_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '액션 수행자',
  `ACTION_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '액션 일시',
  `ACTION_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '액션 사유',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`HIST_ID`),
  KEY `IX_TB_ATTD_CLOSE_HIST` (`CMPNY_CD`,`SITE_CD`,`CLOSE_YM`,`ACTION_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 마감/해제 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_attd_std_time_rule`
--

DROP TABLE IF EXISTS `tb_attd_std_time_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_attd_std_time_rule` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `STD_TIME_RULE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 적용 타입[SYS028]',
  `STD_TIME_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 타입[SYS029]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`STD_TIME_RULE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_attd_std_time_rule_his`
--

DROP TABLE IF EXISTS `tb_attd_std_time_rule_his`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_attd_std_time_rule_his` (
  `HIST_IDX` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력IDX',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `STD_TIME_RULE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 적용 타입[SYS028]',
  `STD_TIME_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시간 표준화 타입[SYS029]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIST_IDX`,`CMPNY_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_audit_log`
--

DROP TABLE IF EXISTS `tb_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_audit_log` (
  `AUDIT_ID` varchar(25) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '감사 로그 ID (PK, 회사별 채번: A + YYYYMMDD + SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '행위자 사용자 코드(비로그인 행위는 NULL)',
  `ACTION_TYPE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '감사 액션 유형[SYS060] 01:다운로드',
  `RESOURCE_TYPE` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 리소스 유형 (예: USER_CREATE_TEMPLATE)',
  `RESOURCE_KEY` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '대상 리소스 식별자(양식 다운로드는 NULL)',
  `IP_ADDRESS` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청 IP (IPv6 지원, 추출 실패 시 NULL)',
  `USER_AGENT` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청 User-Agent',
  `DETAIL` json DEFAULT NULL COMMENT '추가 페이로드(JSON, PII 평문 금지)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부(감사는 무삭제 원칙)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자(=USER_CD or SYSTEM)',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`AUDIT_ID`),
  KEY `IX_AUDIT_LOG_TIME` (`CMPNY_CD`,`INSERT_DATE`),
  KEY `IX_AUDIT_LOG_USER` (`CMPNY_CD`,`USER_CD`,`INSERT_DATE`),
  KEY `IX_AUDIT_LOG_ACTION` (`CMPNY_CD`,`ACTION_TYPE`,`INSERT_DATE`),
  KEY `IX_AUDIT_LOG_RESOURCE` (`CMPNY_CD`,`RESOURCE_TYPE`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='감사 로그 (다운로드/권한 변경/상태 변경 등)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_auth_token`
--

DROP TABLE IF EXISTS `tb_auth_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_auth_token` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `TOKEN_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '토큰(세션) 식별자',
  `CLIENT_TYPE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WEB/APP',
  `DEVICE_ID` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '앱 디바이스 식별(가능하면)',
  `REFRESH_TOKEN_HASH` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '리프레시 토큰 해시',
  `ISSUED_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `EXPIRE_DTIME` datetime NOT NULL COMMENT '리프레시 토큰 만료',
  `REVOKED_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `REVOKED_DTIME` datetime DEFAULT NULL,
  `IP_ADDR` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `USER_AGENT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`TOKEN_ID`),
  UNIQUE KEY `UX_AUTH_TOKEN_RTH` (`REFRESH_TOKEN_HASH`),
  KEY `IX_TOKEN_USER` (`CMPNY_CD`,`USER_CD`,`CLIENT_TYPE`,`REVOKED_YN`),
  KEY `IX_TOKEN_EXPIRE` (`EXPIRE_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_baim_val_d`
--

DROP TABLE IF EXISTS `tb_baim_val_d`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_baim_val_d` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `BAIM_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사변수코드',
  `BAIM_VAL_D_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사상세변수코드',
  `BAIM_VAL_D_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사상세변수이름',
  `SORT_IDX` int DEFAULT NULL COMMENT '순번',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_D_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사상세변수 정보1',
  `VAL_D_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사상세변수 정보2',
  `VAL_D_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사상세변수 설명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`CMPNY_CD`,`BAIM_VAL_CD`,`BAIM_VAL_D_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_baim_val_m`
--

DROP TABLE IF EXISTS `tb_baim_val_m`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_baim_val_m` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `BAIM_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사변수코드',
  `BAIM_VAL_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '운영사변수이름',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사변수 정보1',
  `VAL_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사변수 정보2',
  `VAL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '운영사변수 비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`CMPNY_CD`,`BAIM_VAL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_chkpt_inspect_answer`
--

DROP TABLE IF EXISTS `tb_chkpt_inspect_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_chkpt_inspect_answer` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CHKPT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSPECT_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목코드',
  `WORK_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검일자',
  `INSPECT_ANSWER_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검답변타입[SYS009]',
  `ANSWER_DESC` text COLLATE utf8mb4_unicode_ci COMMENT '점검답변상세',
  `FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '첨부사진코드',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKPT_CD`,`INSPECT_ITEM_CD`,`WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_chkpt_inspect_item`
--

DROP TABLE IF EXISTS `tb_chkpt_inspect_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_chkpt_inspect_item` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `CHKLST_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크리스트 타입',
  `INSPECT_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목코드',
  `INSPECT_ITEM_SUBJ` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목명칭',
  `SORT_IDX` int DEFAULT NULL COMMENT '정렬순서',
  `STR_DATE` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일자',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용유무',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`CHKLST_TYPE`,`INSPECT_ITEM_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_chkpt_type_mgmt`
--

DROP TABLE IF EXISTS `tb_chkpt_type_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_chkpt_type_mgmt` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `CHKLST_TYPE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크리스트 타입',
  `CHKPT_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크포인트 코드',
  `CHKPT_NM` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '체크포인트명',
  `CHKPT_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `MGMT_USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '체크포인트 관리자ID',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT NULL COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKLST_TYPE`,`CHKPT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_cmm_seq`
--

DROP TABLE IF EXISTS `tb_cmm_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_cmm_seq` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SEQ_KEY` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CURR_VAL` int NOT NULL DEFAULT '0',
  `MAX_VAL` int NOT NULL DEFAULT '99999',
  PRIMARY KEY (`CMPNY_CD`,`SEQ_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_cmpny`
--

DROP TABLE IF EXISTS `tb_cmpny`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_cmpny` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `CMPNY_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사명',
  `BSNS_LCN_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업자번호',
  `ADDR_1` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '주소',
  `ADDR_2` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세주소',
  `ZIP_CODE` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '우편번호',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사용여부',
  `CONTRACT_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계약여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_daily_link_mgmt`
--

DROP TABLE IF EXISTS `tb_daily_link_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_daily_link_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `LINK_MGMT_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '링크관리ID',
  `LINK_TOKEN_HASH` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '링크토큰 해시(원문 저장 금지)',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `CNT_BASE_DT` date DEFAULT NULL COMMENT '생성카운트 기준일(일별 제한용)',
  `MAX_CREATE_CNT` int NOT NULL DEFAULT '1' COMMENT '생성가능 인원수',
  `CREATED_CNT` int NOT NULL DEFAULT '0' COMMENT '생성완료 인원수',
  `SMS_VERIFY_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT 'SMS 인증 사용여부',
  `EXPIRE_DTIME` datetime DEFAULT NULL COMMENT '링크 만료일시',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`LINK_MGMT_ID`),
  UNIQUE KEY `UK_DAILY_LINK_TOKEN_HASH` (`CMPNY_CD`,`LINK_TOKEN_HASH`),
  KEY `IX_DAILY_LINK_SITE` (`CMPNY_CD`,`SITE_CD`),
  KEY `IX_DAILY_LINK_EXPIRE` (`CMPNY_CD`,`USE_YN`,`EXPIRE_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 계정 생성 링크 관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_daily_user`
--

DROP TABLE IF EXISTS `tb_daily_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_daily_user` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `USER_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자ID(USER_CD 기반 자동생성, 표시용)',
  `USER_NM` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자명',
  `USER_PW` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '비밀번호(해시) - QR발급 사용자는 난수',
  `MBL_NO_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '휴대폰번호 AES-GCM (v1.base64url)',
  `MBL_NO_HMAC` varchar(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 HMAC-SHA256 Base64URL',
  `MBL_NO_LAST4` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
  `REG_TYPE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '가입경로SYS030]',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `ACCOUNT_STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기',
  `WORK_EXPIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '계정 만료일(YYYYMMDD, 자정 배치 기준)',
  `WITHDRAWAL_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회원탈퇴일',
  `PWD_FAIL_CNT` int NOT NULL DEFAULT '0' COMMENT '비밀번호실패횟수',
  `PWD_LOCK_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '비밀번호잠금여부',
  `PWD_LOCK_EXPIRE_DTIME` datetime DEFAULT NULL COMMENT '비밀번호 인증 실패 잠금 만료일시',
  `LAST_LOGIN_DTIME` datetime DEFAULT NULL COMMENT '마지막 로그인일시',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`),
  UNIQUE KEY `UX_TB_DAILY_USER_MBL` (`CMPNY_CD`,(if((`USE_YN` = _utf8mb4'Y'),`MBL_NO_HMAC`,NULL))),
  KEY `IX_TB_DAILY_USER_MBL_LOOKUP` (`CMPNY_CD`,`MBL_NO_HMAC`),
  KEY `IX_TB_DAILY_USER_EXPIRE` (`WORK_EXPIRE_DATE`,`USE_YN`),
  KEY `IX_TB_DAILY_USER_SITE` (`CMPNY_CD`,`SITE_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일용직 사용자';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_daily_user_link_policy`
--

DROP TABLE IF EXISTS `tb_daily_user_link_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_daily_user_link_policy` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '정책 사용 여부',
  `DAY_LIMIT_CNT` int NOT NULL COMMENT '발급 허용 수(슬롯/정원)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자 ID',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자 ID',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사업장별 일일계정 발급 정책';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_daily_user_slot`
--

DROP TABLE IF EXISTS `tb_daily_user_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_daily_user_slot` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SLOT_NO` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '슬롯 번호(1~N)',
  `SLOT_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '슬롯구분[SYS014]',
  `FIXED_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '고정여부[SYS017]',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `CURR_USER_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '현재 점유중 사용자CD',
  `SLOT_STATUS` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '00' COMMENT '슬롯상태[SYS015]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`),
  KEY `IDX_DAILY_SLOT_STATUS` (`CMPNY_CD`,`SITE_CD`,`SLOT_STATUS`),
  KEY `IDX_DAILY_SLOT_USER` (`CMPNY_CD`,`CURR_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 슬롯(현재 점유 상태만 관리)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_daily_user_slot_his`
--

DROP TABLE IF EXISTS `tb_daily_user_slot_his`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_daily_user_slot_his` (
  `HIS_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력ID(PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SLOT_NO` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '슬롯 번호',
  `WORK_DATE` char(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 일자(YYYYMMDD)',
  `USER_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '할당 사용자ID',
  `ISSUE_CHANNEL` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '발급채널[SYS014]',
  `OCCUPY_DTIME` datetime NOT NULL COMMENT '점유 시작 일시',
  `RELEASE_DTIME` datetime DEFAULT NULL COMMENT '해제 일시',
  `RELEASE_USER` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '점유해제자',
  `RELEASE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제유형[SYS016]',
  `RELEASE_REASON` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '해제 사유',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIS_ID`),
  KEY `IDX_SLOT_HIS_DAY` (`CMPNY_CD`,`SITE_CD`,`WORK_DATE`),
  KEY `IDX_SLOT_HIS_SLOT` (`CMPNY_CD`,`SITE_CD`,`SLOT_NO`,`WORK_DATE`),
  KEY `IDX_SLOT_HIS_USER` (`CMPNY_CD`,`USER_ID`,`WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 슬롯 사용 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_del_user`
--

DROP TABLE IF EXISTS `tb_del_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_del_user` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자ID',
  `USER_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일자',
  PRIMARY KEY (`CMPNY_CD`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_file_info`
--

DROP TABLE IF EXISTS `tb_file_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_file_info` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일코드',
  `FILE_NM` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '파일명',
  `FILE_TYPE` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일타입[SYS010]',
  `FILE_PATH` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일저장경로',
  `FILE_EXT` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '파일확장자',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`FILE_MGMT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_holiday`
--

DROP TABLE IF EXISTS `tb_holiday`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_holiday` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `HOLIDAY_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일ID',
  `HOLIDAY_NM` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일명',
  `HOLIDAY_YMD` date NOT NULL COMMENT '휴일',
  `HOLIDAY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일타입[SYS020]',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`HOLIDAY_ID`),
  KEY `IX_HOLIDAY_DAY` (`CMPNY_CD`,`HOLIDAY_YMD`),
  KEY `IX_HOLIDAY_DAY_TYPE` (`CMPNY_CD`,`HOLIDAY_YMD`,`HOLIDAY_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='휴일관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_holiday_rule`
--

DROP TABLE IF EXISTS `tb_holiday_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_holiday_rule` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `HOLIDAY_RULE_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일규칙ID',
  `HOLIDAY_RULE_NM` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일규칙명',
  `HOLIDAY_MM` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '월(01~12)',
  `HOLIDAY_DD` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '일(01~31)',
  `HOLIDAY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴일타입[SYS020]',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`HOLIDAY_RULE_ID`),
  UNIQUE KEY `UK_HOLIDAY_RULE_MMDD` (`CMPNY_CD`,`HOLIDAY_MM`,`HOLIDAY_DD`,`HOLIDAY_TYPE`),
  KEY `IX_HOLIDAY_RULE_USE` (`CMPNY_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='휴일규칙(매년고정 회사휴일)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_leave_policy`
--

DROP TABLE IF EXISTS `tb_leave_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_leave_policy` (
  `POLICY_SEQ` bigint NOT NULL AUTO_INCREMENT COMMENT '정책 일련번호 (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `POLICY_PRESET` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '프리셋: HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM',
  `AXIS1_GRANT_BASE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1번: HIRE_DATE/FISCAL_YEAR [SYS036]',
  `AXIS2_FISCAL_START_MM` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2번: 회계연도 시작월 (01~12)',
  `AXIS2_FISCAL_START_DD` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2번: 회계연도 시작일 (01~31)',
  `AXIS3_FIRST_YEAR_METHOD` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '3번: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK [SYS037]',
  `AXIS3_PREGRANT_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '3번 보조: 입사일 일괄선부여 여부 (프리셋 4번 표현)',
  `AXIS4_PRORATE_ROUNDING` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CEIL' COMMENT '4번: CEIL/ROUND/FLOOR/HALF_DAY [SYS038] (AXIS3=PRORATE 시만 유효, 그 외는 CEIL 강제)',
  `AXIS5_TENURE_MODE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LEGAL' COMMENT '5번: LEGAL/CUSTOM',
  `AXIS5_START_YEAR` int NOT NULL DEFAULT '3' COMMENT '5번: 가산 시작 연차 (1~3, LEGAL 시 3 강제)',
  `AXIS5_INTERVAL` int NOT NULL DEFAULT '2' COMMENT '5번: 가산 주기 (1~2, LEGAL 시 2 강제)',
  `AXIS5_MAX_DAYS` int NOT NULL DEFAULT '25' COMMENT '5번: 최대 연차일수 (25 이상, 법정)',
  `AXIS6_VALIDITY_MONTHS` int NOT NULL DEFAULT '12' COMMENT '6번: 유효기간(개월) 12 또는 24',
  `AXIS7_USE_PROMOTION` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '7번: 사용촉진 Y/N',
  `APRV_USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '법정연차 신청 결재 여부 (Y: 결재라인, N: 즉시확정)',
  `USE_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '활성 여부 (회사당 Y는 1건, 서비스 레이어에서 보장)',
  `APPLY_FROM_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '정책 적용 시작일 (YYYYMMDD)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`POLICY_SEQ`),
  UNIQUE KEY `UX_TB_LEAVE_POLICY_ACTIVE` (((case when (`USE_YN` = _utf8mb4'Y') then `CMPNY_CD` end))),
  KEY `IX_TB_LEAVE_POLICY_ACTIVE` (`CMPNY_CD`,`USE_YN`,`APPLY_FROM_DATE`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회사 법정 연차 부여 정책 (7개 axis)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_leave_policy_history`
--

DROP TABLE IF EXISTS `tb_leave_policy_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_leave_policy_history` (
  `HIST_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `POLICY_SEQ` bigint NOT NULL COMMENT '변경된 TB_LEAVE_POLICY.POLICY_SEQ',
  `CHANGE_TYPE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'CREATE/UPDATE/PRESET_CHANGE',
  `PREV_SNAPSHOT` json DEFAULT NULL COMMENT '변경 전 정책 전체 스냅샷',
  `NEW_SNAPSHOT` json NOT NULL COMMENT '변경 후 정책 전체 스냅샷',
  `CHANGE_REASON` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IMPACT_SUMMARY` json DEFAULT NULL COMMENT '영향 분석 결과 (영향 인원, 추가 부담)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`HIST_ID`),
  KEY `IX_TB_LEAVE_POLICY_HIST` (`CMPNY_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 정책 변경 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_leave_refusal_log`
--

DROP TABLE IF EXISTS `tb_leave_refusal_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_leave_refusal_log` (
  `REFUSAL_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노무수령거부 로그 ID (PK, 회사별 채번: LR + YYYYMMDD + SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 근로자 코드',
  `TARGET_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노무수령거부 대상일 (YYYYMMDD, =연차촉진 사용지정일)',
  `EVENT_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이벤트 유형[SYS064] NOTICED:통지발송 / CHECKIN_DETECTED:대상일출근감지 / ADMIN_ALERTED:관리자알림발송',
  `RELATED_NOTI_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 알림 ID (tb_noti_outbox.NOTI_ID, NOTICED/ADMIN_ALERTED 시)',
  `RELATED_ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, CHECKIN_DETECTED 시)',
  `DETECT_DTIME` datetime DEFAULT NULL COMMENT '출근 감지 일시 (CHECKIN_DETECTED 시)',
  `DETAIL` json DEFAULT NULL COMMENT '추가 페이로드 (PII 평문 금지)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부 (사실기록 무삭제 원칙) Y:삭제 / N:정상',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자 (관리자 USER_CD or SYSTEM)',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  `DEDUP_KEY` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '중복 방지 키 ({CMPNY_CD}_{USER_CD}_{TARGET_YMD}_{EVENT_TYPE})',
  PRIMARY KEY (`REFUSAL_ID`),
  UNIQUE KEY `UK_REFUSAL_LOG_DEDUP` (`CMPNY_CD`,`DEDUP_KEY`),
  KEY `IX_REFUSAL_LOG_USER` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`TARGET_YMD`),
  KEY `IX_REFUSAL_LOG_TARGET` (`CMPNY_CD`,`SITE_CD`,`TARGET_YMD`,`EVENT_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='노무수령거부 통지/감지/알림 사실 기록 (출퇴근 원본 무수정)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_leave_type_mgmt`
--

DROP TABLE IF EXISTS `tb_leave_type_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_leave_type_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `LEAVE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차코드',
  `LEAVE_NO` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차번호',
  `LEAVE_NM` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차명',
  `LEAVE_TYPE` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차타입[SYS021]',
  `GRANT_TYPE` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부여방식[SYS022]',
  `PAID_TYPE` char(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '유급구분[SYS023]',
  `LEAVE_NATURE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '휴가성격[SYS024]',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `SYSTEM_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '시스템 시드 여부 (Y: PRAFTA-018 법정 연차용, 화면 편집 불가)',
  `LEAVE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `MAX_APLY_DAYS` tinyint unsigned DEFAULT NULL COMMENT '최대 신청일수',
  `USE_UNIT_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용 단위[SYS025]',
  `AVAIL_TERM_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용가능기간 타입[SYS026]',
  `AVAIL_FROM_DT` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용기간 FROM',
  `AVAIL_TO_DT` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 사용기간 TO',
  `GRANT_DAYS` tinyint unsigned DEFAULT NULL COMMENT '부여일 수',
  `ADMIN_AVAIL_TERM_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 부여 연차 사용가능기간 타입[SYS026]',
  `ADMIN_AVAIL_FROM_DT` varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 부여 연차 사용기간 FROM',
  `ADMIN_AVAIL_TO_DT` varchar(6) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 부여 연차 사용기간 TO',
  `GRANT_BASE_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '자동 부여 기준일[SYS027]',
  `GRANT_OFFSET_MONTH` tinyint unsigned DEFAULT NULL COMMENT '자동부여 실행시점',
  `GRANT_ASSIGN_MMDD` char(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '자동부여 지정일 MMDD (기준일=03 부여일지정 시 필수)',
  `APRV_USE_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '결재여부',
  `EVIDENCE_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '증빙여부',
  `EVIDENCE_GUIDE_MSG` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '증빙안내문구',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`LEAVE_CD`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_01` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_02` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`,`LEAVE_TYPE`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_03` (`CMPNY_CD`,`LEAVE_NO`,`USE_YN`,`LEAVE_TYPE`,`GRANT_TYPE`),
  KEY `IX_TB_LEAVE_TYPE_MGMT_SYSTEM` (`CMPNY_CD`,`SYSTEM_YN`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_leave_usage_policy`
--

DROP TABLE IF EXISTS `tb_leave_usage_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_leave_usage_policy` (
  `POLICY_SEQ` bigint NOT NULL COMMENT 'TB_LEAVE_POLICY.POLICY_SEQ 1:1',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `ALLOW_FULL_DAY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '1일 단위 (항상 Y, 변경불가)',
  `ALLOW_HALF_DAY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '0.5일 단위 (AXIS4=HALF_DAY 시 Y 강제)',
  `ALLOW_HOUR_2` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '시간차 2시간 허용 (SYS025-02)',
  `ALLOW_HOUR_1` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '시간차 1시간 허용 (SYS025-03)',
  `ALLOW_MIN_30` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '시간차 30분 허용 (SYS025-04)',
  `USAGE_UNIT` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FULL_DAY' COMMENT '?뚯궗 ?덉슜 ?ъ슜 ?⑥쐞 (?⑥씪): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`POLICY_SEQ`),
  CONSTRAINT `FK_TB_LEAVE_USAGE_POLICY` FOREIGN KEY (`POLICY_SEQ`) REFERENCES `tb_leave_policy` (`POLICY_SEQ`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 사용 단위 정책 (휴게시간/시간단위 시작시각/1일환산은 시스템 강제)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_near_miss`
--

DROP TABLE IF EXISTS `tb_near_miss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_near_miss` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `NEAR_MISS_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사건 ID (사업장별 채번: NM + YYYYMMDD + SEQ)',
  `INCIDENT_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사건유형[SYS061] 100:아차사고 200:경미사고 300:유해·위험요인발견',
  `PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '공정코드[COM002]',
  `OCCUR_DTIME` datetime NOT NULL COMMENT '발생일시',
  `LOCATION_DESC` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '발생장소(직접입력)',
  `DESCRIPTION` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사건 경위(무슨 일이 있었나)',
  `POTENTIAL_SEVERITY_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '잠재적 중대성[SYS062] 100:경미 200:중대 300:치명(실제 사고였다면)',
  `IMMEDIATE_ACTION_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '보고자 즉시 조치사항',
  `ADMIN_TEMP_ACTION_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '관리자 임시조치 메모(앱 1차확인 시 입력, 보고자 IMMEDIATE_ACTION_DESC 와 분리)',
  `CAUSE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '추정 원인(웹 정밀조사)',
  `PREVENTION_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '재발방지 대책(웹 정밀조사)',
  `FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '현장 사진(tb_file_info 관리코드)',
  `REPORT_STATUS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '100' COMMENT '처리상태[SYS063] 100:접수 200:검토중 300:조치중 400:완료 900:반려',
  `REPORTER_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '보고자(tb_user.USER_CD)',
  `REPORT_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '보고일시',
  `REVIEWER_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '검토 관리자(tb_user.USER_CD)',
  `REVIEW_DTIME` datetime DEFAULT NULL COMMENT '검토(분류)일시',
  `SRC_PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '원 위험성평가요청 공정코드(재분류 출처)',
  `SRC_ASSESSMENT_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '원 위험성평가요청 ID(tb_risk_assessment.ASSESSMENT_CD, 재분류 출처)',
  `REJECT_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '반려 사유(처리상태 900 반려 시 기록, 추정원인 CAUSE_DESC 와 분리)',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`NEAR_MISS_ID`),
  KEY `IX_TB_NEAR_MISS_STATUS` (`CMPNY_CD`,`SITE_CD`,`REPORT_STATUS_CD`),
  KEY `IX_TB_NEAR_MISS_REPORTER` (`CMPNY_CD`,`REPORTER_ID`),
  KEY `IX_TB_NEAR_MISS_SRC` (`CMPNY_CD`,`SITE_CD`,`SRC_PROCESS_CD`,`SRC_ASSESSMENT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='아차사고/사건 보고';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_noti_outbox`
--

DROP TABLE IF EXISTS `tb_noti_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_noti_outbox` (
  `NOTI_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '알림 ID (PK, 회사별 채번: N + YYYYMMDD + SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장 코드 (없으면 NULL)',
  `TARGET_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수신 대상 사용자 코드',
  `NOTI_TYPE` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '알림 유형[SYS045] LEAVE_GRANT_RECALLED:부여 연차 회수',
  `CHANNEL` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PUSH' COMMENT '발송 채널 PUSH:푸시',
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '알림 제목',
  `BODY` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '알림 본문',
  `DATA_PAYLOAD` json DEFAULT NULL COMMENT '추가 데이터 페이로드(JSON)',
  `SEND_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '발송 상태 PENDING:대기 / SENDING:발송중(claim) / SENT:완료 / FAILED:실패',
  `SENT_DATE` datetime DEFAULT NULL COMMENT '발송 완료 일시',
  `RETRY_CNT` int NOT NULL DEFAULT '0' COMMENT '재시도 횟수',
  `ERROR_MSG` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '발송 실패 사유',
  `DEDUP_KEY` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '중복 발송 방지 키(이벤트당 1건)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`NOTI_ID`),
  UNIQUE KEY `UK_NOTI_OUTBOX_DEDUP` (`CMPNY_CD`,`DEDUP_KEY`),
  KEY `IX_NOTI_OUTBOX_PENDING` (`CMPNY_CD`,`SEND_STATUS`,`INSERT_DATE`),
  KEY `IX_NOTI_OUTBOX_TARGET` (`CMPNY_CD`,`TARGET_USER_CD`,`NOTI_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='푸시 알림 outbox (발송 대기/이력)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_risk_assessment`
--

DROP TABLE IF EXISTS `tb_risk_assessment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_risk_assessment` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공정코드[COM002]',
  `RISK_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분코드',
  `HAZARD_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인코드',
  `ASSESSMENT_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '평가코드',
  `ASSESSMENT_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인 직접입력',
  `ASSESSMENT_STATUS` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '진행상태[SYS011]',
  `INIT_LIKELIHOOD_SCORE` int DEFAULT NULL COMMENT '초기평가 발생빈도',
  `INIT_SEVERITY_SCORE` int DEFAULT NULL COMMENT '초기평가 중대성',
  `INIT_RISK_LV` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가 위험성LEVEL',
  `INIT_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인설명',
  `INIT_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인사진',
  `INIT_ASSESSOR_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '초기평가자',
  `INIT_ASSESS_DATE` datetime DEFAULT NULL COMMENT '초기평가일시',
  `REVAL_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선예정일',
  `REVAL_BEFORE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선전 임시조치 사항',
  `REVAL_LIKELIHOOD_SCORE` int DEFAULT NULL COMMENT '개선 후 발생빈도',
  `REVAL_SEVERITY_SCORE` int DEFAULT NULL COMMENT '개선 후 중대성',
  `REVAL_RISK_LV` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선 후 평가 위험성LEVEL',
  `REVAL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선내용',
  `REVAL_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선사진',
  `REVAL_ASSESSOR_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '개선후평가자',
  `REVAL_ASSESS_DATE` datetime DEFAULT NULL COMMENT '개선완료일',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`PROCESS_CD`,`ASSESSMENT_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_risk_site_hazard`
--

DROP TABLE IF EXISTS `tb_risk_site_hazard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_risk_site_hazard` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `RISK_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분코드',
  `HAZARD_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '유해요인코드',
  `HAZARD_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '유해요인명',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드',
  `HAZARD_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '유해요인설명',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`RISK_TYPE_CD`,`HAZARD_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_risk_type`
--

DROP TABLE IF EXISTS `tb_risk_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_risk_type` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `RISK_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분코드',
  `RISK_TYPE_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험요인구분명',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드',
  `PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공정코드[COM002]',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용여부',
  `RISK_TYPE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '위험요인비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`RISK_TYPE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_sch_mgmt`
--

DROP TABLE IF EXISTS `tb_sch_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_sch_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 코드',
  `SCH_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 번호',
  `SCH_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄타입[SYS019]',
  `BASE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '기본스케줄여부[SYS003]',
  `APPLY_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용일자',
  `FST_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 시작시간',
  `FST_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 종료시간',
  `FST_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게시간',
  `FST_BRK_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게 시작(HHMM)',
  `FST_BRK_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게 종료(HHMM)',
  `SEC_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 시작시간',
  `SEC_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 종료시간',
  `SEC_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게시간',
  `SEC_BRK_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게 시작(HHMM)',
  `SEC_BRK_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게 종료(HHMM)',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SCH_CD`),
  KEY `IX_TB_SCH_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`USE_YN`,`SCH_TYPE`,`SCH_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사업장 근무타입 관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_sch_mgmt_hist`
--

DROP TABLE IF EXISTS `tb_sch_mgmt_hist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_sch_mgmt_hist` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `HIST_IDX` int NOT NULL COMMENT '이력시퀀스',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 코드',
  `APPLY_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용일자',
  `FST_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 시작시간',
  `FST_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '1구간 종료시간',
  `FST_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게시간',
  `FST_BRK_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게 시작(HHMM)',
  `FST_BRK_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '1구간 휴게 종료(HHMM)',
  `SEC_SCH_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 시작시간',
  `SEC_SCH_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 종료시간',
  `SEC_SCH_BRK_MIN` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게시간',
  `SEC_BRK_STR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게 시작(HHMM)',
  `SEC_BRK_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '2구간 휴게 종료(HHMM)',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`HIST_IDX`,`SCH_CD`),
  KEY `IX_TB_SCH_MGMT_HIST_LIST` (`CMPNY_CD`,`SITE_CD`,`HIST_IDX`,`USE_YN`,`SCH_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사업장 근무타입 이력관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_shift_sch_assign_mgmt`
--

DROP TABLE IF EXISTS `tb_shift_sch_assign_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_shift_sch_assign_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `DAY_NO` tinyint unsigned NOT NULL COMMENT '일자(1~SHIFT_CYCLE_DAYS)',
  `ASSIGN_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄유무',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '스케줄코드',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`,`DAY_NO`),
  KEY `IX_SHIFT_ASSIGN_01` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`DAY_NO`),
  KEY `IX_SHIFT_ASSIGN_02` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_shift_sch_mgmt`
--

DROP TABLE IF EXISTS `tb_shift_sch_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_shift_sch_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `SHIFT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무번호',
  `SHIFT_PTRN_CNT` tinyint unsigned NOT NULL COMMENT '교대 패턴 수',
  `SHIFT_TEAM_CNT` tinyint unsigned NOT NULL COMMENT '교대 팀 수',
  `SHIFT_CYCLE_DAYS` tinyint unsigned NOT NULL COMMENT '근무 교대주기',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부(Y/N)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`),
  KEY `IX_TB_SHIFT_SCH_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`SHIFT_NO`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_shift_sch_ptrn_mgmt`
--

DROP TABLE IF EXISTS `tb_shift_sch_ptrn_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_shift_sch_ptrn_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `PTRN_IDX` tinyint unsigned NOT NULL COMMENT '교대패턴 순번',
  `SCH_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스케줄 코드',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`PTRN_IDX`),
  KEY `IX_TB_SHIFT_SCH_PTRN_MGMT_LIST` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SCH_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_shift_sch_team_meta_info`
--

DROP TABLE IF EXISTS `tb_shift_sch_team_meta_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_shift_sch_team_meta_info` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `TEAM_NM` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '팀명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`TEAM_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_shift_sch_team_mgmt`
--

DROP TABLE IF EXISTS `tb_shift_sch_team_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_shift_sch_team_mgmt` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `SHIFT_TEAM_ID` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무팀ID',
  `SHIFT_TEAM_NM` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교대근무팀명',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작일자',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료일자',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SHIFT_TEAM_ID`),
  KEY `IDX_SHIFT_SCH_TEAM_MGMT_01` (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`),
  KEY `IDX_SHIFT_SCH_TEAM_MGMT_02` (`CMPNY_CD`,`SITE_CD`,`STR_DATE`,`END_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_shift_sch_team_user`
--

DROP TABLE IF EXISTS `tb_shift_sch_team_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_shift_sch_team_user` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SHIFT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무코드',
  `SHIFT_TEAM_ID` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교대근무팀ID',
  `TEAM_IDX` tinyint unsigned NOT NULL COMMENT '팀 순번',
  `USER_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `LEADER_YN` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '팀리더여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`SHIFT_CD`,`SHIFT_TEAM_ID`,`USER_CD`),
  KEY `IDX_SHIFT_TEAM_USER_01` (`CMPNY_CD`,`SITE_CD`,`USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='교대근무 팀 소속 사용자 관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_site`
--

DROP TABLE IF EXISTS `tb_site`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_site` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `SITE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장번호',
  `SITE_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장명',
  `ADDR_1` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '주소',
  `ADDR_2` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세주소',
  `ZIP_CODE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '우편번호',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업개시일',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업종료일',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `SITE_ADMIN_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장관리자코드',
  `TEL_NO` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장전화번호',
  `LAT` decimal(10,7) DEFAULT NULL COMMENT '사업장 중심 위도',
  `LON` decimal(10,7) DEFAULT NULL COMMENT '사업장 중심 경도',
  `GPS_RANGE` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SITE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` date DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`SITE_CD`,`CMPNY_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_site_node`
--

DROP TABLE IF EXISTS `tb_site_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_site_node` (
  `CMPNY_CD` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노드ID',
  `NODE_NM` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노드명',
  `NODE_TYPE` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '노드타입[COM004]',
  `PARENT_NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부모노드ID',
  `SELF_ATTD_APPRV_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '자체근태승인여부',
  `MAIN_ADMIN_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부서 정 관리자',
  `SUB_ADMIN_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부서 부 관리자',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`NODE_CD`),
  KEY `IX_NODE_PARENT` (`CMPNY_CD`,`SITE_CD`,`PARENT_NODE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일계정 슬롯(현재 점유 상태만 관리)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_sms_auth_code`
--

DROP TABLE IF EXISTS `tb_sms_auth_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_sms_auth_code` (
  `SMS_ID` bigint NOT NULL AUTO_INCREMENT,
  `MBL_NO_ENC` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `MBL_NO_HMAC` char(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `AUTH_CD` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `EXPIRED_AT` datetime NOT NULL,
  `VERIFIED_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'N',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`SMS_ID`),
  KEY `idx_sms_auth_mbl_hmac_exp` (`MBL_NO_HMAC`,`EXPIRED_AT`),
  KEY `idx_sms_auth_mbl_hmac_ins` (`MBL_NO_HMAC`,`INSERT_DATE`)
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_syst_auth_menu`
--

DROP TABLE IF EXISTS `tb_syst_auth_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_syst_auth_menu` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `AUTH_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '권한코드',
  `MENU_D_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BTN_SRCH` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '조회권한',
  `BTN_NEW` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '신규권한',
  `BTN_DELT` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '삭제권한',
  `BTN_SAVE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '저장권한',
  `BTN_EXCL` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '엑셀권한',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`AUTH_CD`,`MENU_D_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_syst_menu_d`
--

DROP TABLE IF EXISTS `tb_syst_menu_d`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_syst_menu_d` (
  `MENU_D_ID` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `MENU_M_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대메뉴ID',
  `MENU_VIEW` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MENU_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '메뉴명',
  `MENU_IDX` int DEFAULT NULL COMMENT '메뉴순번',
  `MENU_DESC` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `USE_YN` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`MENU_D_ID`,`MENU_M_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_syst_menu_m`
--

DROP TABLE IF EXISTS `tb_syst_menu_m`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_syst_menu_m` (
  `MENU_M_ID` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대메뉴ID',
  `MENU_SRC` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '메뉴사용처[SYS007]',
  `MENU_NM` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '메뉴명',
  `MENU_IDX` int DEFAULT NULL COMMENT '메뉴순번',
  `MENU_DESC` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `USE_YN` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`MENU_M_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_syst_val_d`
--

DROP TABLE IF EXISTS `tb_syst_val_d`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_syst_val_d` (
  `SYST_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_D_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_D_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수이름',
  `SORT_IDX` int DEFAULT NULL COMMENT '순번',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_D_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보1',
  `VAL_D_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보2',
  `VAL_D_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 설명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`SYST_VAL_CD`,`SYST_VAL_D_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_syst_val_m`
--

DROP TABLE IF EXISTS `tb_syst_val_m`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_syst_val_m` (
  `SYST_VAL_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수코드',
  `SYST_VAL_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시스템변수이름',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `VAL_INFO_1` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보1',
  `VAL_INFO_2` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 정보2',
  `VAL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시스템변수 설명',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` date DEFAULT NULL,
  PRIMARY KEY (`SYST_VAL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_attendance`
--

DROP TABLE IF EXISTS `tb_tbm_attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_attendance` (
  `ATTENDANCE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출결코드 (PK, 채번 A+YYYYMMDD+SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드',
  `USER_TYPE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상유형[SYS050] REGULAR:정규직(TB_USER) DAILY:일용직(TB_DAILY_USER)',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 USER_CD (유형에 따라 TB_USER 또는 TB_DAILY_USER)',
  `ENTRY_TYPE_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔',
  `ENTRY_BY_MANAGER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'QR 입실 처리 관리자 USER_CD',
  `ENTRY_AT` datetime DEFAULT NULL COMMENT '입실 시각',
  `ENTRY_GPS_LAT` decimal(10,7) DEFAULT NULL COMMENT '입실 위도',
  `ENTRY_GPS_LON` decimal(10,7) DEFAULT NULL COMMENT '입실 경도',
  `ENTRY_DISTANCE_M` int DEFAULT NULL COMMENT '입실 시 개설지점과의 거리(m)',
  `ENTRY_SIGN_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입실 서명 파일코드',
  `EXIT_TYPE_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료경로[SYS052] SELF:본인 MANAGER_QR_SCAN:관리자QR MANAGER_FORCED:관리자강제',
  `EXIT_BY_MANAGER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 처리 관리자 USER_CD',
  `EXIT_AT` datetime DEFAULT NULL COMMENT '종료 시각(NULL=미종료)',
  `EXIT_SIGN_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 서명 파일코드',
  `EXIT_FORCED_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '강제종료 사유(관리자 책임 기록)',
  `COMPLETION_STATUS_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이수상태[SYS053] COMPLETED:이수 NOT_COMPLETED:미이수',
  `NOT_COMPLETED_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '미이수 사유',
  `STATUS_UPDATED_BY` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이수상태 마지막 변경자',
  `STATUS_UPDATED_AT` datetime DEFAULT NULL COMMENT '이수상태 마지막 변경 시각',
  `DEL_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`ATTENDANCE_CD`),
  UNIQUE KEY `UK_TBM_ATTENDANCE_01` (`CMPNY_CD`,`SESSION_CD`,`USER_TYPE_CD`,`USER_CD`),
  KEY `IX_TBM_ATTENDANCE_01` (`CMPNY_CD`,`SESSION_CD`),
  KEY `IX_TBM_ATTENDANCE_02` (`CMPNY_CD`,`USER_TYPE_CD`,`USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 출결(정규직/일용직 통합)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_attendance_event`
--

DROP TABLE IF EXISTS `tb_tbm_attendance_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_attendance_event` (
  `EVENT_NO` bigint NOT NULL AUTO_INCREMENT COMMENT '이벤트 일련번호 (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드(비정규화, 조회용)',
  `ATTENDANCE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출결코드',
  `EVENT_TYPE_CD` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이벤트유형[SYS054] ENTER/START/SLIDE_CHANGED/GPS_UPDATED/BACKGROUND_IN/BACKGROUND_OUT/NETWORK_LOST/SIGNATURE_STARTED/END/FORCED_END',
  `EVENT_TIME` datetime(3) NOT NULL COMMENT '이벤트 발생시각(클라이언트 보고, ms)',
  `SERVER_RECEIVED_AT` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '서버 수신시각(ms, 위조불가 기준)',
  `EVENT_DATA` json DEFAULT NULL COMMENT '이벤트 부가데이터(JSON)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`EVENT_NO`),
  KEY `IX_TBM_ATT_EVENT_01` (`CMPNY_CD`,`ATTENDANCE_CD`,`EVENT_TIME`),
  KEY `IX_TBM_ATT_EVENT_02` (`CMPNY_CD`,`SESSION_CD`,`EVENT_TYPE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 출결 이벤트 로그';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_edu_mtrl`
--

DROP TABLE IF EXISTS `tb_tbm_edu_mtrl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_edu_mtrl` (
  `MTRL_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 코드',
  `CMPNY_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드 (NULL=회사공통, 값=해당 사업장 전용)',
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자로 제목',
  `CONTENTS` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교육자료 설명',
  `MTRL_TYPE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교육자로 타입',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용유무',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`MTRL_CD`),
  KEY `IX_TBM_EDU_MTRL_01` (`CMPNY_CD`,`USE_YN`),
  KEY `IX_TBM_EDU_MTRL_02` (`CMPNY_CD`,`SITE_CD`,`USE_YN`),
  CONSTRAINT `tb_tbm_edu_mtrl_chk_1` CHECK ((`USE_YN` in (_utf8mb4'Y',_utf8mb4'N')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_edu_mtrl_item`
--

DROP TABLE IF EXISTS `tb_tbm_edu_mtrl_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_edu_mtrl_item` (
  `MTRL_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 항목 코드',
  `MTRL_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 코드',
  `SORT_IDX` int NOT NULL DEFAULT '1' COMMENT '정렬순서',
  `MTRL_ITEM_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 항목 타입',
  `MTRL_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '교육자료 항목 설명',
  `FILE_MGMT_CD` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '파일코드',
  `THUMB_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '썸네일 파일코드 (동영상 첫프레임/PDF 첫페이지/이미지 리사이즈 자동생성)',
  `DURATION_SEC` int DEFAULT NULL COMMENT '미디어 길이(초) - 동영상만',
  `URL` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '외부링크',
  `USE_YN` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`MTRL_ITEM_CD`),
  KEY `IX_TBM_EDU_MTRL_ITEM_01` (`MTRL_CD`,`USE_YN`,`SORT_IDX`),
  KEY `IX_TBM_EDU_MTRL_ITEM_02` (`MTRL_CD`,`SORT_IDX`),
  CONSTRAINT `FK_TBM_EDU_MTRL_ITEM_01` FOREIGN KEY (`MTRL_CD`) REFERENCES `tb_tbm_edu_mtrl` (`MTRL_CD`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_pwd_fail`
--

DROP TABLE IF EXISTS `tb_tbm_pwd_fail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_pwd_fail` (
  `FAIL_NO` bigint NOT NULL AUTO_INCREMENT COMMENT '실패 일련번호 (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드',
  `PWD_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '비번유형[SYS055] ENTRY:입실 EXIT:종료',
  `USER_TYPE_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '대상유형[SYS050] REGULAR:정규직 DAILY:일용직',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시도자 USER_CD(식별 가능 시)',
  `ATTEMPTED_AT` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시도 시각',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`FAIL_NO`),
  KEY `IX_TBM_PWD_FAIL_01` (`CMPNY_CD`,`SESSION_CD`,`ATTEMPTED_AT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 비밀번호 실패 로그';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_session`
--

DROP TABLE IF EXISTS `tb_tbm_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_session` (
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드 (PK, 채번 T+YYYYMMDD+SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `EDU_TYPE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TBM' COMMENT '교육유형[SYS047] TBM:툴박스미팅 (확장용 고정값)',
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '세션 제목',
  `CONTENT_BODY` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '교육 내용(리치 HTML). 개설 시 필수(서버 검증)',
  `CONTENT_FORMAT_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RICH_HTML' COMMENT '교육내용 형식 RICH_HTML:리치텍스트(MVP 고정값)',
  `STATUS_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '세션상태[SYS046] DRAFT:작성중 OPENED:개설 IN_PROGRESS:진행중 COMPLETED:종료 CANCELLED:취소',
  `ENTRY_PWD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입실 비밀번호(랜덤6자리, OPENED부터 생성)',
  `EXIT_PWD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 비밀번호(입실≠종료)',
  `MANAGER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '개설자 USER_CD',
  `MANAGER_GPS_LAT` decimal(10,7) DEFAULT NULL COMMENT '개설 위도(AUTO 모드 시)',
  `MANAGER_GPS_LON` decimal(10,7) DEFAULT NULL COMMENT '개설 경도(AUTO 모드 시)',
  `GPS_VERIFY_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO' COMMENT 'GPS검증유형[SYS048] AUTO:자동 MANUAL:수동확인 DISABLED:비활성',
  `GPS_VERIFY_RADIUS_M` int NOT NULL DEFAULT '100' COMMENT 'GPS 검증반경(m, 50~1000)',
  `GPS_MANUAL_CONFIRM_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT 'MANUAL 모드 관리자 확인여부 Y:확인',
  `OPENED_AT` datetime DEFAULT NULL COMMENT '개설 시각',
  `STARTED_AT` datetime DEFAULT NULL COMMENT '교육 시작 시각(IN_PROGRESS 전이) [C단계]',
  `ENDED_AT` datetime DEFAULT NULL COMMENT '교육 종료 시각 [C단계]',
  `CANCELLED_AT` datetime DEFAULT NULL COMMENT '취소 시각',
  `CANCEL_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '취소 사유',
  `DEL_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제여부 Y/N (DRAFT 물리관리용, OPENED+ 는 STATUS_CD=CANCELLED 사용)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`SESSION_CD`),
  KEY `IX_TBM_SESSION_01` (`CMPNY_CD`,`SITE_CD`,`STATUS_CD`),
  KEY `IX_TBM_SESSION_02` (`CMPNY_CD`,`MANAGER_USER_CD`),
  KEY `IX_TBM_SESSION_03` (`CMPNY_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_session_content`
--

DROP TABLE IF EXISTS `tb_tbm_session_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_session_content` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드',
  `MTRL_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '교육자료 묶음코드 (TB_TBM_EDU_MTRL)',
  `DISPLAY_ORDER` int NOT NULL DEFAULT '0' COMMENT '세션 내 표시 순서',
  `OVERRIDE_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '세션별 설명 override (이 세션에서만 다른 설명)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`SESSION_CD`,`MTRL_CD`),
  KEY `IX_TBM_SESSION_CONTENT_01` (`CMPNY_CD`,`SESSION_CD`,`DISPLAY_ORDER`),
  KEY `IX_TBM_SESSION_CONTENT_02` (`CMPNY_CD`,`MTRL_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션-콘텐츠 묶음 매핑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_session_risk`
--

DROP TABLE IF EXISTS `tb_tbm_session_risk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_session_risk` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험성평가 사업장코드',
  `PROCESS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험성평가 공정코드[COM002]',
  `ASSESSMENT_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '위험성평가 평가코드',
  `DISPLAY_ORDER` int NOT NULL DEFAULT '0' COMMENT '표시 순서',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`SESSION_CD`,`SITE_CD`,`PROCESS_CD`,`ASSESSMENT_CD`),
  KEY `IX_TBM_SESSION_RISK_01` (`CMPNY_CD`,`SESSION_CD`,`DISPLAY_ORDER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션-위험성평가 매핑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tbm_session_state`
--

DROP TABLE IF EXISTS `tb_tbm_session_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_tbm_session_state` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SESSION_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TBM 세션코드',
  `CURRENT_MTRL_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '현재 표시중 콘텐츠 묶음코드',
  `CURRENT_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '현재 표시중 세부항목코드',
  `CURRENT_SLIDE_INDEX` int NOT NULL DEFAULT '0' COMMENT '현재 슬라이드 인덱스',
  `SYNC_STATE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PAUSED' COMMENT '동기화상태[SYS049] PLAYING:재생 PAUSED:정지',
  `LAST_UPDATED_BY` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '마지막 갱신 관리자',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SESSION_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TBM 세션 실시간 동기화 상태(UPSERT)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_terms`
--

DROP TABLE IF EXISTS `tb_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_terms` (
  `TERMS_ID` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관버전',
  `REQUIRED_YN` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '필수여부',
  `TERMS_CONTENT` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관본문',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일자',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용유무',
  `TERMS_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`TERMS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_terms_id_version`
--

DROP TABLE IF EXISTS `tb_terms_id_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_terms_id_version` (
  `TERMS_ID` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관버전',
  `REQUIRED_YN` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '필수여부',
  `TERMS_CONTENT` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관본문',
  `STR_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '시행일자',
  `TERMS_DESC` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`TERMS_ID`,`TERMS_VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_terms_user_agr_mgmt`
--

DROP TABLE IF EXISTS `tb_terms_user_agr_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_terms_user_agr_mgmt` (
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `TERMS_ID` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관ID(SYS008)',
  `TERMS_VERSION` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '약관버전',
  `AGR_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '동의여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`USER_CD`,`TERMS_ID`,`TERMS_VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user`
--

DROP TABLE IF EXISTS `tb_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `USER_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자ID',
  `USER_NM` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자명',
  `USER_PW` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비밀번호(해시)',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사업장코드',
  `NODE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '소속부서',
  `AUTH_CD` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '권한코드',
  `RANK_CD` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직급 코드 (BAIM_VAL COM007 직급 코드그룹 참조)',
  `MBL_NO_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '휴대폰번호 AES-GCM (v1.base64url)',
  `MBL_NO_HMAC` varchar(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 HMAC-SHA256 Base64URL (equals/중복/계정찾기)',
  `MBL_NO_LAST4` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '휴대폰번호 마지막4자리(마스킹/리스트용)',
  `EMAIL_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '이메일 AES-GCM (v1.base64url)',
  `EMAIL_HMAC` varchar(43) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이메일 HMAC-SHA256 Base64URL (equals/중복/계정찾기)',
  `EMAIL_DOMAIN` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이메일 도메인(선택)',
  `BIRTH_DT_ENC` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '생년월일 AES-GCM (v1.base64url)',
  `HIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입사일 (YYYYMMDD) — 연차 부여 기준',
  `EMPLOYMENT_TYPE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE',
  `CONTRACT_END_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계약 종료일 (YYYYMMDD, EMPLOYMENT_TYPE=CONTRACT일 때 필수)',
  `GENDER` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '성별',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `ACCOUNT_STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013] 01:활성화 02:잠김 03:탈퇴 04:인증대기',
  `PWD_LOCK_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '비밀번호잠금여부',
  `PWD_FAIL_CNT` int NOT NULL DEFAULT '0' COMMENT '비밀번호실패횟수',
  `PWD_LOCK_EXPIRE_DTIME` datetime DEFAULT NULL COMMENT '비밀번호 인증 실패 잠금 만료일시',
  `PWD_CHG_DTIME` datetime DEFAULT NULL COMMENT '비밀번호변경일시',
  `WITHDRAWAL_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회원탈퇴일',
  `LAST_LOGIN_DTIME` datetime DEFAULT NULL COMMENT '마지막로그인',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`),
  UNIQUE KEY `UX_TB_USER_ID` (`CMPNY_CD`,`USER_ID`),
  UNIQUE KEY `UX_TB_USER_MBL_NO` (`CMPNY_CD`,`MBL_NO_HMAC`),
  KEY `IX_TB_USER_STATUS` (`CMPNY_CD`,`USE_YN`,`ACCOUNT_STATUS`),
  KEY `IX_TB_USER_CONTRACT` (`CMPNY_CD`,`CONTRACT_END_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_attd_gps`
--

DROP TABLE IF EXISTS `tb_user_attd_gps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_attd_gps` (
  `GPS_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'GPS고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근태고유ID',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `GPS_INFO_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'GPS정보타입[SYS028]',
  `LAT` decimal(10,7) NOT NULL COMMENT '위도',
  `LON` decimal(10,7) NOT NULL COMMENT '경도',
  `ACCURACY` decimal(7,2) DEFAULT NULL COMMENT '정확도(m, 임계값 검증용)',
  `API_CALL_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '측정일자(YYYYMMDD)',
  `API_CALL_TIME` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '측정시간(HHmmss)',
  `IS_MOCKED` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT 'Mock위치여부(Y/N)',
  `IP_ADDR` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP주소(IPv6 대응)',
  `OFFSITE_REASON` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '외근(근무지 외) 사유',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`CMPNY_CD`,`GPS_ID`),
  KEY `idx_gps_attd` (`CMPNY_CD`,`ATTD_ID`),
  KEY `idx_gps_user` (`CMPNY_CD`,`USER_CD`,`API_CALL_DATE`),
  KEY `idx_gps_search` (`CMPNY_CD`,`SITE_CD`,`API_CALL_DATE`),
  KEY `idx_gps_abnormal` (`CMPNY_CD`,`API_CALL_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 GPS 기록';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_attd_hist`
--

DROP TABLE IF EXISTS `tb_user_attd_hist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_attd_hist` (
  `HIST_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근태고유ID',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `HIST_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력구분[SYS032]',
  `PROCESS_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리사유',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무일',
  `BEF_CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 출근일자',
  `BEF_CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 출근시간',
  `BEF_CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 퇴근일자',
  `BEF_CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경전 퇴근시간',
  `AFT_CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 출근일자',
  `AFT_CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 출근시간',
  `AFT_CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 퇴근일자',
  `AFT_CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '변경후 퇴근시간',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`HIST_ID`),
  KEY `IDX_ATTD_HIST_ATTD` (`CMPNY_CD`,`ATTD_ID`),
  KEY `IDX_ATTD_HIST_SITE` (`CMPNY_CD`,`SITE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태 처리 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_attd_mgmt`
--

DROP TABLE IF EXISTS `tb_user_attd_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_attd_mgmt` (
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근태고유ID',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무일',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '소속부서',
  `WORK_SEQ` int NOT NULL COMMENT '근무차수',
  `CHECK_IN_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출근일자',
  `CHECK_IN_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출근시간',
  `CHECK_IN_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '출근방법[SYS031]',
  `CHECK_IN_DEVICE_UUID` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '출근 실행 디바이스UUID(클라 제공, 부정탐지 보조)',
  `CHECK_OUT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근일자',
  `CHECK_OUT_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근시간',
  `CHECK_OUT_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근방법[SYS031]',
  `CHECK_OUT_DEVICE_UUID` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '퇴근 실행 디바이스UUID(클라 제공, 부정탐지 보조)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '삭제여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`ATTD_ID`),
  KEY `IDX_ATTD_USER_DATE` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`,`DEL_YN`),
  KEY `IDX_ATTD_SITE_DATE` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`,`NODE_CD`,`DEL_YN`),
  KEY `IDX_ATTD_NODE_DATGE` (`CMPNY_CD`,`NODE_CD`,`WORK_YMD`,`DEL_YN`),
  KEY `IDX_ATTD_INDEVICE` (`CMPNY_CD`,`WORK_YMD`,`CHECK_IN_DEVICE_UUID`,`DEL_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_attd_req`
--

DROP TABLE IF EXISTS `tb_user_attd_req`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_attd_req` (
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청자 사용자 코드',
  `REQ_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청 유형 (SYS032: 01근태생성/02근태수정/03초과근무생성/04초과근무수정/05연차사용/06연차수정)',
  `TARGET_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정 대상 ID (수정 요청 시: ATTD_ID / OT_ID / LEAVE_ID, 생성 요청 시 NULL)',
  `REQ_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청 상태 (SYS033: 01신청/02승인/03반려/04취소)',
  `REQ_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '요청 사유',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무 일자 (YYYYMMDD)',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무 노드 코드',
  `WORK_SEQ` int DEFAULT NULL COMMENT '근무 순번',
  `START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 일자 (YYYYMMDD)',
  `START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 시각 (HHMM)',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 일자 (YYYYMMDD)',
  `END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 시각 (HHMM)',
  `LEAVE_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연차 유형 (ANNUAL:연차 / HALF_AM:오전반차 / HALF_PM:오후반차 / SICK:병가 / FAMILY:경조사 등)',
  `LEAVE_DAYS` decimal(8,5) DEFAULT NULL COMMENT '사용 일수(시간차 환산)',
  `SCH_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '스케줄 코드 (REQ_TYPE=10 스케줄 수정 요청 시 변경 목표 SCH_CD, 그 외 NULL)',
  `PROCESS_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리자 사용자 코드',
  `PROCESS_COMMENT` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '처리 코멘트',
  `PROCESS_DATE` datetime DEFAULT NULL COMMENT '처리 일시',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`REQ_ID`),
  KEY `IDX_ATTD_REQ_USER` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`REQ_STATUS`),
  KEY `IDX_ATTD_REQ_STATUS` (`CMPNY_CD`,`SITE_CD`,`REQ_STATUS`,`REQ_TYPE`),
  KEY `IDX_ATTD_REQ_WORK_YMD` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`),
  KEY `IDX_ATTD_REQ_TARGET` (`TARGET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 근태 관련 요청 관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_attd_req_approval`
--

DROP TABLE IF EXISTS `tb_user_attd_req_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_attd_req_approval` (
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연관 요청 (tb_user_attd_req.REQ_ID)',
  `APPROVAL_STEP` int NOT NULL COMMENT '결재 단계 (1부터)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `APPROVER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '지정 결재자',
  `APPROVAL_STATUS` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '00' COMMENT '단계 상태 [SYS044]',
  `APPROVAL_COMMENT` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '결재 코멘트',
  `APPROVAL_DATE` datetime DEFAULT NULL COMMENT '처리 일시',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `UPDATE_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`REQ_ID`,`APPROVAL_STEP`),
  KEY `IX_TB_USER_ATTD_REQ_APPROVAL_APPROVER` (`APPROVER_USER_CD`,`APPROVAL_STATUS`),
  KEY `IX_TB_USER_ATTD_REQ_APPROVAL_REQ` (`CMPNY_CD`,`REQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='연차 요청별 결재라인 (사용자 정의)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_device`
--

DROP TABLE IF EXISTS `tb_user_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_device` (
  `DEVICE_UUID` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스UUID',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `DEVICE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스종류(IOS/ANDROID)',
  `DEVICE_MODEL` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '디바이스모델',
  `OS_VERSION` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OS버전',
  `APP_VERSION` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '앱버전',
  `PUSH_TOKEN` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'FCM/APNS 푸시토큰',
  `LAST_LOGIN_DTIME` datetime DEFAULT NULL COMMENT '최종로그인일시',
  `LAST_LOGIN_IP` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '최종로그인IP',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  `DEL_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부 N:정상 / Y:무효토큰 soft-delete',
  PRIMARY KEY (`DEVICE_UUID`),
  KEY `idx_user_device_user` (`USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='정규직 디바이스 점유 (글로벌 유니크 - 회사 가로질러 1디바이스=1계정)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_device_login_hist`
--

DROP TABLE IF EXISTS `tb_user_device_login_hist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_device_login_hist` (
  `DEVICE_LOGIN_NO` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스 로그인 이력 번호(PK, 회사별 채번: YYYYMM + SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `DEVICE_UUID` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '디바이스UUID(클라 제공, 네이티브 ANDROID_ID/IDFV 우선)',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '로그인 사용자 코드',
  `DEVICE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '디바이스 종류[자유값] ANDROID:안드로이드 / IOS:iOS (네이티브 미주입 시 NULL)',
  `DEVICE_MODEL` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '디바이스 모델',
  `OS_VERSION` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OS 버전',
  `APP_VERSION` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '앱 버전',
  `CLIENT_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '클라이언트 구분[자유값] APP:앱 / WEB:웹',
  `LOGIN_IP` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '로그인 IP(HttpServletRequest 추출)',
  `LOGIN_DTIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 일시',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  PRIMARY KEY (`DEVICE_LOGIN_NO`),
  KEY `IDX_DLH_DEVICE` (`CMPNY_CD`,`DEVICE_UUID`,`LOGIN_DTIME`),
  KEY `IDX_DLH_USER` (`CMPNY_CD`,`USER_CD`,`LOGIN_DTIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='디바이스 로그인 이력(append-only, 부정탐지 baseline 소스)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_hire_date_history`
--

DROP TABLE IF EXISTS `tb_user_hire_date_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_hire_date_history` (
  `HIST_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이력 ID (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `PREV_HIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 전 입사일',
  `NEW_HIRE_DATE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 후 입사일',
  `CHANGE_REASON` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경 사유 (자유 텍스트, 필수)',
  `HANDLING_TYPE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '처리 방식[SYS039] KEEP_AND_BACKFILL/KEEP_AND_APPLY_NEW/RESET_ALL',
  `AFFECTED_GRANT_SNAPSHOT` json DEFAULT NULL COMMENT '영향받은 부여 이력 스냅샷',
  `OLD_GRANT_TOTAL` decimal(5,1) DEFAULT NULL COMMENT '변경 전 법정 부여 총량 (수동 조정 추적, MANUAL 한정)',
  `NEW_GRANT_TOTAL` decimal(5,1) DEFAULT NULL COMMENT '변경 후 목표 법정 부여 총량 (수동 조정 추적, MANUAL 한정)',
  `WITHDRAW_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회수 사유 (차액<0 회수 발생 시 필수, MANUAL 한정)',
  `APPLIED_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '?뺤콉 湲곗? 遺?뿬 ?곸슜 ?꾨즺 ?щ? (Attd_09 遺?뿬 踰꾪듉?먯꽌 ?곸슜 ??Y)',
  `APPLIED_DATE` datetime DEFAULT NULL COMMENT '?곸슜 ?쇱떆',
  `APPLIED_BY` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?곸슜 ?섑뻾??(USER_CD)',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '변경자 (AUTH_MASTER 또는 AUTH_HR_MANAGER)',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경일시',
  PRIMARY KEY (`HIST_ID`),
  KEY `IX_TB_HIRE_HIST_USER` (`CMPNY_CD`,`USER_CD`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='입사일 변경 이력 (노무 감사용)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_leave_grant`
--

DROP TABLE IF EXISTS `tb_user_leave_grant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_leave_grant` (
  `GRANT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '부여 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `LEAVE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차 코드 (tb_leave_type_mgmt.LEAVE_CD)',
  `GRANT_TYPE` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부여 분류[SYS035] STATUTORY_*/MANUAL_* prefix (법정/약정 구분)',
  `GRANT_DAYS` decimal(5,1) NOT NULL COMMENT '부여 일수 (반차 0.5 단위 고려)',
  `USED_DAYS` decimal(8,5) NOT NULL DEFAULT '0.00000' COMMENT '사용 일수 캐시 (tb_user_leave_use 합계와 동기화)',
  `GRANT_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '부여 사유 (자동부여/관리자수동/특별부여 등)',
  `GRANT_BY_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '부여 방식 (AUTO:자동 / ADMIN:관리자수동)',
  `POLICY_SEQ` bigint DEFAULT NULL COMMENT '적용 정책 (TB_LEAVE_POLICY.POLICY_SEQ, 수동 부여는 NULL)',
  `GRANT_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '부여 일자 (YYYYMMDD)',
  `AVAIL_FROM_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 가능 시작일 (YYYYMMDD)',
  `AVAIL_TO_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 가능 종료일 (YYYYMMDD, 소멸일)',
  `IDEMPOTENCY_KEY` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '중복 부여 방지 키 ({USER_CD}_{YYYY}_ANNUAL 등). 자동부여 시 필수',
  `STATUS` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '상태[SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED (EXPIRE_YN과 동기화 — 정책서 §8.5.8)',
  `EXPIRE_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '소멸 여부 (배치로 AVAIL_TO_DATE 경과 시 Y)',
  `EXPIRE_DATE` datetime DEFAULT NULL COMMENT '소멸 처리 일시',
  `CANCEL_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회수(취소) 사유',
  `CANCEL_DATE` datetime DEFAULT NULL COMMENT '회수(취소) 일시',
  `CANCEL_BY` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '회수 수행자 (USER_CD)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`GRANT_ID`),
  UNIQUE KEY `UK_LEAVE_GRANT_IDEMPOTENCY` (`CMPNY_CD`,`IDEMPOTENCY_KEY`),
  KEY `IDX_LEAVE_GRANT_USER` (`CMPNY_CD`,`USER_CD`,`LEAVE_CD`,`EXPIRE_YN`),
  KEY `IDX_LEAVE_GRANT_AVAIL` (`CMPNY_CD`,`USER_CD`,`AVAIL_TO_DATE`,`EXPIRE_YN`),
  KEY `IDX_LEAVE_GRANT_TYPE` (`CMPNY_CD`,`LEAVE_CD`),
  KEY `IX_LEAVE_GRANT_STATUS` (`CMPNY_CD`,`USER_CD`,`STATUS`,`AVAIL_TO_DATE`),
  KEY `IX_LEAVE_GRANT_GTYPE` (`CMPNY_CD`,`GRANT_TYPE`,`GRANT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 연차 부여 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_leave_use`
--

DROP TABLE IF EXISTS `tb_user_leave_use`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_leave_use` (
  `LEAVE_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차 사용 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `LEAVE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연차 코드 (tb_leave_type_mgmt.LEAVE_CD)',
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 결재 미사용 또는 사후 등록 시 NULL)',
  `GRANT_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '차감 대상 부여 ID (tb_user_leave_grant.GRANT_ID)',
  `START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 시작일 (YYYYMMDD)',
  `START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 시각 (HHMM, 시간단위 휴가 시)',
  `END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 종료일 (YYYYMMDD)',
  `END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 시각 (HHMM, 시간단위 휴가 시)',
  `USE_UNIT_TYPE` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 단위 (tb_leave_type_mgmt.USE_UNIT_TYPE 복사, SYS025)',
  `LEAVE_DAYS` decimal(8,5) NOT NULL COMMENT '사용 일수 (시간차 동적 환산)',
  `LEAVE_MINUTES` int DEFAULT NULL COMMENT '사용 분 (시간단위 휴가 시)',
  `LEAVE_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '사용 사유',
  `EVIDENCE_FILE_ID` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '증빙 파일 ID (tb_leave_type_mgmt.EVIDENCE_YN=Y 시)',
  `LEAVE_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용 상태 (CONFIRMED:확정 / CANCELLED:취소)',
  `CANCEL_REASON` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '취소 사유',
  `CANCEL_DATE` datetime DEFAULT NULL COMMENT '취소 일시',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  `DIRECT_USE_KEY` varchar(80) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS ((case when ((`REQ_ID` is null) and (`LEAVE_STATUS` = _utf8mb4'CONFIRMED')) then concat(`USER_CD`,_utf8mb4'|',`START_DATE`,_utf8mb4'|',`LEAVE_CD`) else NULL end)) STORED COMMENT '직접 차감(결재 없음) 멱등 키 — 결재경유/취소건은 NULL',
  PRIMARY KEY (`LEAVE_ID`),
  UNIQUE KEY `UK_LEAVE_USE_DIRECT` (`CMPNY_CD`,`DIRECT_USE_KEY`),
  KEY `IDX_LEAVE_USE_USER` (`CMPNY_CD`,`USER_CD`,`START_DATE`,`LEAVE_STATUS`),
  KEY `IDX_LEAVE_USE_SITE` (`CMPNY_CD`,`SITE_CD`,`START_DATE`,`LEAVE_STATUS`),
  KEY `IDX_LEAVE_USE_GRANT` (`GRANT_ID`),
  KEY `IDX_LEAVE_USE_REQ` (`REQ_ID`),
  KEY `IDX_LEAVE_USE_TYPE` (`CMPNY_CD`,`LEAVE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 연차 사용 실적';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_overtime_mgmt`
--

DROP TABLE IF EXISTS `tb_user_overtime_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_overtime_mgmt` (
  `OT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '초과근무 ID (PK)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무자 사용자 코드',
  `ATTD_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 근태 ID (tb_user_attd_mgmt.ATTD_ID, 휴일근무 등 정규근태 없는 경우 NULL)',
  `REQ_ID` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '연관 요청 ID (tb_user_attd_req.REQ_ID, 사후 등록 시 NULL)',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무 일자 (YYYYMMDD)',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무 노드 코드',
  `PLAN_START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 시작 일자 (YYYYMMDD)',
  `PLAN_START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 시작 시각 (HHMM)',
  `PLAN_END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 종료 일자 (YYYYMMDD)',
  `PLAN_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '계획 종료 시각 (HHMM)',
  `ACTUAL_START_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '실제 시작 일자 (YYYYMMDD)',
  `ACTUAL_START_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '실제 시작 시각 (HHMM)',
  `ACTUAL_START_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시작 체크 방식 (GPS/QR/MANUAL 등)',
  `ACTUAL_END_DATE` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '실제 종료 일자 (YYYYMMDD)',
  `ACTUAL_END_TIME` varchar(4) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '실제 종료 시각 (HHMM)',
  `ACTUAL_END_METHOD` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 체크 방식 (GPS/QR/MANUAL 등)',
  `WORK_MINUTES` int DEFAULT NULL COMMENT '실제 근무 시간 (분 단위, 휴게시간 제외)',
  `BREAK_MINUTES` int DEFAULT '0' COMMENT '휴게 시간 (분 단위)',
  `OT_STATUS` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '초과근무 상태 (IN_PROGRESS:진행중 / COMPLETED:완료 / CANCELLED:취소)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`OT_ID`),
  KEY `IDX_OT_USER_YMD` (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`),
  KEY `IDX_OT_SITE_YMD` (`CMPNY_CD`,`SITE_CD`,`WORK_YMD`,`OT_STATUS`),
  KEY `IDX_OT_ATTD` (`ATTD_ID`),
  KEY `IDX_OT_REQ` (`REQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 초과근무 실적 관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_service_credit`
--

DROP TABLE IF EXISTS `tb_user_service_credit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_service_credit` (
  `CREDIT_ID` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '경력 인정 ID (PK)',
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 코드',
  `CREDIT_MONTHS` int NOT NULL COMMENT '인정 개월 수 (0 이상)',
  `REASON_TYPE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사유 유형[SYS042]',
  `REASON_DETAIL` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '상세 설명',
  `USE_YN` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CREDIT_ID`),
  KEY `IX_TB_USER_SERVICE_CREDIT_USER` (`CMPNY_CD`,`USER_CD`,`USE_YN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 경력 인정 (점진 부여 전용)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_site_auth`
--

DROP TABLE IF EXISTS `tb_user_site_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_site_auth` (
  `CMPNY_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `USER_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자CD',
  `SITE_CD` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USE_YN` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '사용여부',
  `INSERT_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`USER_CD`,`SITE_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_work_plan`
--

DROP TABLE IF EXISTS `tb_user_work_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_work_plan` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드',
  `WORK_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근무일',
  `WORK_PLAN_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근무계획코드[SCH_CD, LEAVE_CD]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`USER_CD`,`WORK_YMD`),
  KEY `IX_WORK_PLAN_SITE` (`CMPNY_CD`,`SITE_CD`),
  KEY `IX_WORK_PLAN_USER` (`CMPNY_CD`,`USER_CD`),
  KEY `IX_WORK_PLAN_USER_YMD` (`CMPNY_CD`,`USER_CD`,`WORK_YMD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 근무 계획';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'prafta'
--
/*!50003 DROP FUNCTION IF EXISTS `FNC_CMM_INFO_SRCH` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE FUNCTION `FNC_CMM_INFO_SRCH`(
    p_cmpny_cd      VARCHAR(50),
    p_srch_type     VARCHAR(50),
    p_srch_cd       VARCHAR(50),
    p_sub_srch_cd   VARCHAR(50)
) RETURNS varchar(512) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
    READS SQL DATA
BEGIN
    DECLARE v_result VARCHAR(512) DEFAULT '';

    IF p_cmpny_cd IS NULL OR p_cmpny_cd = '' THEN
        SET v_result = '';
    ELSEIF p_srch_type IS NULL OR p_srch_type = '' THEN
        SET v_result = '';
    ELSEIF p_srch_cd IS NULL OR p_srch_cd = '' THEN
        SET v_result = '';
    ELSE
        IF p_srch_type = 'SITE' THEN
            SELECT SITE_NM
              INTO v_result
              FROM TB_SITE
             WHERE CMPNY_CD = p_cmpny_cd
               AND SITE_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'NODE' THEN
            SELECT NODE_NM
              INTO v_result
              FROM TB_SITE_NODE
             WHERE CMPNY_CD = p_cmpny_cd
               AND SITE_CD  = p_srch_cd
               AND NODE_CD  = p_sub_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'SYST_VAL' THEN
            SELECT SYST_VAL_D_NM
              INTO v_result
              FROM TB_SYST_VAL_D
             WHERE SYST_VAL_CD    = p_sub_srch_cd
               AND SYST_VAL_D_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'BAIM_VAL' THEN
            SELECT BAIM_VAL_D_NM
              INTO v_result
              FROM TB_BAIM_VAL_D
             WHERE CMPNY_CD       = p_cmpny_cd
               AND BAIM_VAL_CD    = p_sub_srch_cd
               AND BAIM_VAL_D_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'USER_NM' THEN
            SELECT USER_NM
              INTO v_result
              FROM TB_USER
             WHERE CMPNY_CD = p_cmpny_cd
               AND USER_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'DAILY_USER_NM' THEN
            SELECT USER_NM
              INTO v_result
              FROM TB_DAILY_USER
             WHERE CMPNY_CD = p_cmpny_cd
               AND USER_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'USER_CD' THEN
            SELECT USER_CD
              INTO v_result
              FROM TB_USER
             WHERE CMPNY_CD = p_cmpny_cd
               AND USER_ID  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'FILE_PATH' THEN
            SELECT FILE_PATH
              INTO v_result
              FROM TB_FILE_INFO
             WHERE CMPNY_CD      = p_cmpny_cd
               AND FILE_MGMT_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'FILE_NAME' THEN
            SELECT CONCAT(FILE_MGMT_CD, FILE_EXT)
              INTO v_result
              FROM TB_FILE_INFO
             WHERE CMPNY_CD      = p_cmpny_cd
               AND FILE_MGMT_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'AUTH_LEVEL' THEN
            SELECT SORT_IDX
              INTO v_result
              FROM TB_BAIM_VAL_D
             WHERE CMPNY_CD       = p_cmpny_cd
               AND BAIM_VAL_CD    = 'COM005'
               AND BAIM_VAL_D_CD  = p_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'SCH_CD' THEN
            SELECT SCH_CD
              INTO v_result
              FROM TB_SCH_MGMT
             WHERE CMPNY_CD = p_cmpny_cd
               AND SITE_CD  = p_srch_cd
               AND SCH_NO   = p_sub_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'SHIFT_NO' THEN
            SELECT SHIFT_NO
              INTO v_result
              FROM TB_SHIFT_SCH_MGMT
             WHERE CMPNY_CD  = p_cmpny_cd
               AND SITE_CD   = p_srch_cd
               AND SHIFT_CD  = p_sub_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'SHIFT_TEAM_NM' THEN
            SELECT SHIFT_TEAM_NM
              INTO v_result
              FROM TB_SHIFT_TEAM_USER
             WHERE CMPNY_CD      = p_cmpny_cd
               AND SITE_CD       = p_srch_cd
               AND SHIFT_TEAM_ID = p_sub_srch_cd
             LIMIT 1;

        ELSEIF p_srch_type = 'LEAVE_CD' THEN
            SELECT LEAVE_CD
              INTO v_result
              FROM TB_LEAVE_TYPE_MGMT
             WHERE CMPNY_CD = p_cmpny_cd
               AND LEAVE_NO = p_srch_cd
             LIMIT 1;

        ELSE
            SET v_result = '';
        END IF;
    END IF;

    RETURN IFNULL(v_result, '');
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP FUNCTION IF EXISTS `FNC_CMM_SEQ_NEXTVAL` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE FUNCTION `FNC_CMM_SEQ_NEXTVAL`(
    p_cmpny_cd VARCHAR(50),
    p_seq_key  VARCHAR(50)
) RETURNS varchar(50) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
    MODIFIES SQL DATA
BEGIN
    DECLARE v_next INT;
	DECLARE v_max INT;
    DECLARE v_result VARCHAR(50);

    -- 1) 없으면 생성 (MAX_VAL은 테이블 기본값(현재 99999)을 따르게)
    INSERT INTO tb_cmm_seq (CMPNY_CD, SEQ_KEY)
    VALUES (p_cmpny_cd, p_seq_key)
    ON DUPLICATE KEY UPDATE SEQ_KEY = SEQ_KEY;

    -- 2) 원자적 증가 + 증가값을 LAST_INSERT_ID로 확보
    UPDATE tb_cmm_seq
       SET CURR_VAL = LAST_INSERT_ID(
                        CASE
                          WHEN CURR_VAL >= MAX_VAL THEN 1
                          ELSE CURR_VAL + 1
                        END
                      )
     WHERE CMPNY_CD = p_cmpny_cd
       AND SEQ_KEY  = p_seq_key;

    -- 3) 방금 증가한 값/최대값 가져오기
    SET v_next = LAST_INSERT_ID();

    SELECT MAX_VAL
      INTO v_max
      FROM tb_cmm_seq
     WHERE CMPNY_CD = p_cmpny_cd
       AND SEQ_KEY  = p_seq_key;

    -- 4) MAX_VAL 자리수만큼 0 패딩
    SET v_result = LPAD(v_next, CHAR_LENGTH(CAST(v_max AS CHAR)), '0');

    RETURN v_result;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP FUNCTION IF EXISTS `FNC_STD_TIME` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE FUNCTION `FNC_STD_TIME`(
    p_time_hhmm  VARCHAR(4),     -- '0612'
    p_unit       INT,            -- 0, 5, 10, 15, 30
    p_direction  VARCHAR(3)      -- 'IN' = 올림, 'OUT' = 내림
) RETURNS varchar(4) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
    DETERMINISTIC
BEGIN
    DECLARE v_total_min INT;
    DECLARE v_result_min INT;

    -- 입력값이 없거나 단위가 0이면 원본 그대로
    IF p_time_hhmm IS NULL OR p_time_hhmm = '' THEN
        RETURN p_time_hhmm;
    END IF;
    IF p_unit IS NULL OR p_unit = 0 THEN
        RETURN p_time_hhmm;
    END IF;

    -- 'HHMM' → 총 분
    SET v_total_min = CAST(SUBSTRING(p_time_hhmm, 1, 2) AS UNSIGNED) * 60
                    + CAST(SUBSTRING(p_time_hhmm, 3, 2) AS UNSIGNED);

    -- 방향에 따라 올림/내림
    IF p_direction = 'IN' THEN
        SET v_result_min = CEIL(v_total_min / p_unit) * p_unit;
    ELSE
        SET v_result_min = FLOOR(v_total_min / p_unit) * p_unit;
    END IF;

    -- 24시간 초과 보정 (예: 23:55 + 올림 → 24:00 → 그대로 두려면 이 분기 제거)
    IF v_result_min >= 1440 THEN
        SET v_result_min = 1439;  -- 23:59로 캡
    END IF;

    -- 다시 'HHMM' 4자리로
    RETURN LPAD(CONCAT(
        LPAD(FLOOR(v_result_min / 60), 2, '0'),
        LPAD(v_result_min % 60, 2, '0')
    ), 4, '0');
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP FUNCTION IF EXISTS `FN_DECRYPT` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE FUNCTION `FN_DECRYPT`(p_cipher TEXT) RETURNS varchar(255) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
    DETERMINISTIC
BEGIN
    DECLARE v_dec VARBINARY(1024);
    DECLARE v_text VARCHAR(255);
	DECLARE AES_KEY VARCHAR(32);
    
    SET AES_KEY = '20250924safenote230904prafta!!@';
    -- Base64 → 바이너리 변환
    SET v_dec = FROM_BASE64(p_cipher);
    -- AES 복호화
    SET v_text = AES_DECRYPT(v_dec, AES_KEY);
    RETURN v_text;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP FUNCTION IF EXISTS `FN_ENCRYPT` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE FUNCTION `FN_ENCRYPT`(p_text VARCHAR(255)) RETURNS text CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
    DETERMINISTIC
BEGIN
    DECLARE v_enc VARBINARY(1024);
    DECLARE v_b64 TEXT;
    DECLARE AES_KEY VARCHAR(32);
    
    SET AES_KEY = '20250924safenote230904prafta!!@';
    
    -- AES 암호화
    SET v_enc = AES_ENCRYPT(p_text, AES_KEY);
    -- Base64 인코딩해서 문자열 반환
    SET v_b64 = TO_BASE64(v_enc);
    RETURN v_b64;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `proc_encrypt_user` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE PROCEDURE `proc_encrypt_user`(
  IN p_user_id VARCHAR(64),
  IN p_phone VARCHAR(255),
  IN p_email VARCHAR(255)
)
BEGIN
  -- ⚠️ 위험: 키를 프로시저에 평문 하드코딩
  DECLARE v_key VARBINARY(64) DEFAULT 'my-very-unsafe-key-please-change'; 

  -- IV 생성 (12바이트 권장). MySQL RANDOM_BYTES 사용
  SET @iv = RANDOM_BYTES(12);

  -- AES_ENCRYPT(plain, key, iv) 사용 (MySQL 8.x에서 지원되는 형태)
  SET @phone_enc = AES_ENCRYPT(p_phone, v_key, @iv);
  SET @email_enc = AES_ENCRYPT(p_email, v_key, @iv);

  -- IV와 암호문을 함께 저장 (iv || ciphertext) — 나중에 꺼내서 분리 가능
  INSERT INTO user_personal (user_id, phone_enc, email_enc, key_id)
  VALUES (p_user_id, CONCAT(@iv, @phone_enc), CONCAT(@iv, @email_enc), 'key-in-proc');
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `PR_CMM_SEQ_NEXTVAL` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE PROCEDURE `PR_CMM_SEQ_NEXTVAL`(
    IN p_cmpny_cd VARCHAR(50),
    IN p_seq_key  VARCHAR(50),
    OUT p_next_str VARCHAR(50)   -- 변경: OUT 값을 문자열로
)
BEGIN
    DECLARE v_curr INT DEFAULT 0;
    DECLARE v_max  INT DEFAULT 99999;
    DECLARE v_len  INT;

    -- 1. 없으면 초기화
    INSERT INTO prafta.TB_CMM_SEQ (CMPNY_CD, SEQ_KEY, CURR_VAL, MAX_VAL)
    VALUES (p_cmpny_cd, p_seq_key, 0, v_max)
    ON DUPLICATE KEY UPDATE CURR_VAL = CURR_VAL;

    -- 2. 조회
    SELECT CURR_VAL, MAX_VAL
    INTO v_curr, v_max
    FROM prafta.TB_CMM_SEQ
    WHERE CMPNY_CD = p_cmpny_cd
      AND SEQ_KEY = p_seq_key
    FOR UPDATE;

    -- 3. 증가/롤링
    IF v_curr >= v_max THEN
        SET v_curr = 1;
    ELSE
        SET v_curr = v_curr + 1;
    END IF;

    -- 4. 저장
    UPDATE prafta.TB_CMM_SEQ
    SET CURR_VAL = v_curr
    WHERE CMPNY_CD = p_cmpny_cd
      AND SEQ_KEY = p_seq_key;

    -- 5. 길이 계산 후 문자열 변환
    SET v_len = LENGTH(v_max);  -- max 값 기준으로 자리수 추출
    SET p_next_str = LPAD(v_curr, v_len, '0');
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `SP_INSERT_SITE` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE PROCEDURE `SP_INSERT_SITE`(
	IN p_SITE_CD VARCHAR(50),
    IN p_SITE_NO VARCHAR(50),
    IN p_SITE_NM VARCHAR(100),
    IN p_CMPNY_CD VARCHAR(50),
    IN p_ADDR_1 VARCHAR(200),
    IN p_ADDR_2 VARCHAR(200),
    IN p_ZIP_CODE VARCHAR(20),
    IN p_STR_DATE VARCHAR(10),
    IN p_END_DATE VARCHAR(10),
    IN p_USE_YN VARCHAR(2),
    IN p_SITE_ADMIN_ID VARCHAR(50),
    IN p_TEL_NO VARCHAR(20),
    IN p_GPS_RANGE VARCHAR(4),
    IN p_SITE_DESC VARCHAR(500),
    IN p_GV_USER_CD VARCHAR(50)
)
BEGIN
    DECLARE v_prefix VARCHAR(8);
    DECLARE v_seq INT;
    DECLARE v_new_cd VARCHAR(50);

    SET v_prefix = DATE_FORMAT(NOW(), '%Y%m%d');
    
    IF p_SITE_CD IS NULL OR p_SITE_CD = '' 
		THEN 
			-- 시퀀스 가져오기 또는 초기화
			INSERT INTO prafta.SEQ_SITE_CD (DATE_PREFIX, LAST_SEQ)
			VALUES (v_prefix, 1)
			ON DUPLICATE KEY UPDATE LAST_SEQ = LAST_SEQ + 1;

			SELECT LAST_SEQ INTO v_seq
			FROM prafta.SEQ_SITE_CD
			WHERE DATE_PREFIX = v_prefix;
            
			SET v_new_cd = CONCAT(v_prefix, LPAD(v_seq, 4, '0'));
		ELSE
			SET v_new_cd = p_SITE_CD;
	END IF;    

    -- 최종 INSERT
    INSERT INTO prafta.TB_SITE (
        SITE_CD
        , SITE_NO
        , SITE_NM
        , CMPNY_CD
        , ADDR_1
        , ADDR_2
        , ZIP_CODE
        , STR_DATE
        , END_DATE
        , USE_YN
        , SITE_ADMIN_ID
        , TEL_NO
        , GPS_RANGE
        , SITE_DESC
        , INSERT_NO
        , INSERT_DATE
        , UPDATE_NO
        , UPDATE_DATE
    )
    VALUES (
        v_new_cd
        , p_SITE_NO
		, p_SITE_NM
		, p_CMPNY_CD
		, p_ADDR_1
		, p_ADDR_2
		, p_ZIP_CODE
		, REPLACE(p_STR_DATE, '-', '')
        , CASE WHEN p_USE_YN = 'Y' THEN NULL ELSE DATE_FORMAT(NOW(), '%Y%m%d') END
		, p_USE_YN
		, p_SITE_ADMIN_ID
		, REPLACE(p_TEL_NO, '-', '')
        , p_GPS_RANGE
		, p_SITE_DESC
		, p_GV_USER_CD
		, NOW()
        , p_GV_USER_CD
		, NOW()
    )
    ON DUPLICATE KEY UPDATE
		SITE_NO				= VALUES(SITE_NO)
        , SITE_NM			= VALUES(SITE_NM)
        , ADDR_1			= VALUES(ADDR_1)
        , ADDR_2			= VALUES(ADDR_2)
        , ZIP_CODE			= VALUES(ZIP_CODE)
        , STR_DATE			= VALUES(STR_DATE)
        , END_DATE			= VALUES(END_DATE)
        , USE_YN      		= IFNULL(USE_YN, 'Y')
        , SITE_ADMIN_ID		= VALUES(SITE_ADMIN_ID)
        , TEL_NO			= VALUES(TEL_NO)
        , GPS_RANGE			= VALUES(GPS_RANGE)
        , SITE_DESC			= VALUES(SITE_DESC)
        , UPDATE_NO			= VALUES(UPDATE_NO)
        , UPDATE_DATE		= VALUES(UPDATE_DATE)
    ;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-05 20:59:55
