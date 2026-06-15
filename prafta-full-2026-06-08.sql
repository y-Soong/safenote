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
-- Dumping data for table `seq_site_cd`
--

LOCK TABLES `seq_site_cd` WRITE;
/*!40000 ALTER TABLE `seq_site_cd` DISABLE KEYS */;
/*!40000 ALTER TABLE `seq_site_cd` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_acct`
--

DROP TABLE IF EXISTS `tb_acct`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_acct` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `ACCT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사고 ID (사업장별 채번: ACC + YYYYMMDD + SEQ4)',
  `VICTIM_USER_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '재해자 사용자유형[SYS050] REGULAR:정규 DAILY:일용',
  `VICTIM_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '재해자 사용자코드(tb_user.USER_CD 또는 tb_daily_user.USER_CD)',
  `OCCUR_YMD` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사고 발생일(YYYYMMDD)',
  `OCCUR_TIME` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '발생 시각(HHMM)',
  `OCCUR_PLACE` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '발생 장소(직접입력)',
  `ACCT_GRADE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '재해등급[SYS065] 100:중대재해 200:일반산재 300:신고제외',
  `ACCT_DESC` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사고 경위',
  `EMPLOYER_DESC` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '신고의무자(직영/하수급 등 직접입력)',
  `PROCESS_STATUS_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '100' COMMENT '처리상태[SYS066] 100:접수 200:처리중 300:종결',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제여부',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`ACCT_ID`),
  KEY `IX_TB_ACCT_GRADE` (`CMPNY_CD`,`SITE_CD`,`ACCT_GRADE_CD`),
  KEY `IX_TB_ACCT_STATUS` (`CMPNY_CD`,`SITE_CD`,`PROCESS_STATUS_CD`),
  KEY `IX_TB_ACCT_OCCUR` (`CMPNY_CD`,`SITE_CD`,`OCCUR_YMD`),
  KEY `IX_TB_ACCT_VICTIM` (`CMPNY_CD`,`VICTIM_USER_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고관리 헤더';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_acct`
--

LOCK TABLES `tb_acct` WRITE;
/*!40000 ALTER TABLE `tb_acct` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_acct` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_acct_legal_step`
--

DROP TABLE IF EXISTS `tb_acct_legal_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_acct_legal_step` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `ACCT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사고 ID(tb_acct.ACCT_ID)',
  `STEP_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '절차코드(tb_acct_legal_step_master.STEP_CD)',
  `IS_DONE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '조치완료여부(Y/N) — 처리버튼→체크 방식',
  `DONE_DTIME` datetime DEFAULT NULL COMMENT '조치완료 처리일시',
  `DONE_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '조치완료 처리자(tb_user.USER_CD)',
  `REMARK` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '항목별 비고',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`ACCT_ID`,`STEP_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 법정 처리/기한 진행상태';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_acct_legal_step`
--

LOCK TABLES `tb_acct_legal_step` WRITE;
/*!40000 ALTER TABLE `tb_acct_legal_step` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_acct_legal_step` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_acct_legal_step_master`
--

DROP TABLE IF EXISTS `tb_acct_legal_step_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_acct_legal_step_master` (
  `STEP_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '절차코드(전사 공통)',
  `ACCT_GRADE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '적용 재해등급[SYS065] 100/200/300, 또는 ALL(전등급 공통)',
  `STEP_IDX` int NOT NULL COMMENT '절차 표시 순서',
  `STEP_NM` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '절차명(예: 중대재해 발생보고)',
  `ACTION_GUIDE` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '행동강령 문구(관리자 가이드)',
  `LEGAL_BASIS` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근거조문/과태료',
  `DEADLINE_RULE_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '기한규칙(상수) IMMEDIATE:지체없이 MONTH_PLUS_1:발생일+1개월(산안법 시행규칙§73) NONE:기한없음 TRACK:별도트랙',
  `STEP_NOTE` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '추가 안내(예: 시스템이 기한 계산 안 함)',
  `USE_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '사용여부[SYS003]',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`STEP_CD`),
  KEY `IX_TB_ACCT_STEP_MASTER_GRADE` (`ACCT_GRADE_CD`,`STEP_IDX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 법정절차 정의(seed)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_acct_legal_step_master`
--

LOCK TABLES `tb_acct_legal_step_master` WRITE;
/*!40000 ALTER TABLE `tb_acct_legal_step_master` DISABLE KEYS */;
INSERT INTO `tb_acct_legal_step_master` VALUES ('STEP_COMP_CLAIM_100','100',4,'근로복지공단 요양급여 신청','산재조사표 제출과 별개 트랙입니다. 재해자/유족이 신청하며 회사가 지원하세요.','산재보상보험법 §41 · 조사표 제출과 독립','TRACK',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_COMP_CLAIM_200','200',4,'근로복지공단 요양급여 신청','산재조사표 제출과 별개 트랙입니다. 재해자/유족이 신청하며 회사가 지원하세요.','산재보상보험법 §41 · 조사표 제출과 독립','TRACK',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_CRIT_INVST','100',3,'산업재해조사표 제출','관할 지방고용노동관서에 산업재해조사표를 제출하세요(중대재해도 별도 제출).','산안법 §57③ / 시행규칙 §73','MONTH_PLUS_1',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_CRIT_REPORT','100',2,'중대재해 발생보고','재해개요·피해상황·조치·전망을 관할 지방고용노동관서에 보고하세요.','산안법 §54② / 시행규칙 §67 · 미이행 과태료 3,000만원','IMMEDIATE','\"지체없이\" — 시스템이 기한을 계산하지 않습니다. 즉시 보고 후 완료 처리하세요.','Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_EXEMPT_REC','300',2,'재해 기록·보존','신고 의무는 없습니다. 사업장·인적사항·발생경위·재발방지계획을 기록·보존하세요.','산안법 §57① · 3일 미만 휴업도 기록 대상','NONE',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_INIT','ALL',1,'초기 조치 / 응급','부상자 처치·현장 보존·2차 재해 방지 조치를 즉시 시행하세요.','사업주 일반 안전조치 의무','NONE',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_INVESTIGATE','ALL',5,'사고 조사 / 재발방지 계획','원인 분석·재발방지 대책 수립 후 기록·보존하세요.','산안법 §57① 기록·보존 의무','NONE',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_NORM_INVST','200',2,'산업재해조사표 제출','사망 또는 3일 이상 휴업 시 관할 지방고용노동관서에 산업재해조사표를 제출하세요.','산안법 §57③ / 시행규칙 §73 · 미제출 과태료 1,500만원','MONTH_PLUS_1',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_SETTLE_100','100',6,'보상 / 합의','위로금·합의를 진행하고 합의서를 보관하세요.','민사·사내 절차','TRACK',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL),('STEP_SETTLE_200','200',6,'보상 / 합의','위로금·합의를 진행하고 합의서를 보관하세요.','민사·사내 절차','TRACK',NULL,'Y','SYSTEM','2026-06-06 17:49:53',NULL,NULL);
/*!40000 ALTER TABLE `tb_acct_legal_step_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_acct_link`
--

DROP TABLE IF EXISTS `tb_acct_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_acct_link` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `ACCT_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사고 ID(tb_acct.ACCT_ID)',
  `LINK_DOMAIN_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '연계도메인[SYS067] ATTD:근태 CHKPT:순회점검 RISK:위험성평가 TBM:TBM NEAR_MISS:아차사고',
  `LINK_SEQ` int NOT NULL COMMENT '도메인 내 확정 순번(다건)',
  `LINK_KEY_JSON` text COLLATE utf8mb4_unicode_ci COMMENT '연결 원본키 묶음(JSON 문자열; 예 {"chkptCd":"...","workDate":"..."} )',
  `SNAPSHOT_JSON` text COLLATE utf8mb4_unicode_ci COMMENT '확정 시점 조회값 고정(JSON 문자열; 사고 날짜·시각 기준 스냅샷)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'SYSTEM' COMMENT '입력자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`ACCT_ID`,`LINK_DOMAIN_CD`,`LINK_SEQ`),
  KEY `IX_TB_ACCT_LINK_DOMAIN` (`CMPNY_CD`,`SITE_CD`,`ACCT_ID`,`LINK_DOMAIN_CD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사고 연계 데이터 스냅샷';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_acct_link`
--

LOCK TABLES `tb_acct_link` WRITE;
/*!40000 ALTER TABLE `tb_acct_link` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_acct_link` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_aprv_line_preset`
--

LOCK TABLES `tb_aprv_line_preset` WRITE;
/*!40000 ALTER TABLE `tb_aprv_line_preset` DISABLE KEYS */;
INSERT INTO `tb_aprv_line_preset` VALUES ('001','P2026052300002','20260400010','프리셋1','N','Y','20260400010','2026-05-23 19:22:58',NULL,NULL),('001','P2026052300003','20260400010','프리셋2','Y','Y','20260400010','2026-05-23 19:23:15','20260400010','2026-05-23 19:23:25'),('001','P2026052300004','20260400013','프리셋1','Y','Y','20260400013','2026-05-23 21:30:03','20260400013','2026-05-31 20:27:19'),('001','P2026052500005','20260400012','프리셋1','Y','Y','20260400012','2026-05-25 20:38:50',NULL,NULL),('001','P2026052500006','20260400012','ㄴㄴㄴ','N','Y','20260400012','2026-05-25 20:39:01',NULL,NULL);
/*!40000 ALTER TABLE `tb_aprv_line_preset` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_aprv_line_preset_d`
--

LOCK TABLES `tb_aprv_line_preset_d` WRITE;
/*!40000 ALTER TABLE `tb_aprv_line_preset_d` DISABLE KEYS */;
INSERT INTO `tb_aprv_line_preset_d` VALUES ('001','P2026052300002',1,'20260400011','20260400010','2026-05-23 19:22:58',NULL,NULL),('001','P2026052300002',2,'20260400013','20260400010','2026-05-23 19:22:58',NULL,NULL),('001','P2026052300002',3,'20260400014','20260400010','2026-05-23 19:22:58',NULL,NULL),('001','P2026052300003',1,'20260400012','20260400010','2026-05-23 19:23:25',NULL,NULL),('001','P2026052300003',2,'20260400013','20260400010','2026-05-23 19:23:25',NULL,NULL),('001','P2026052300003',3,'20260400014','20260400010','2026-05-23 19:23:25',NULL,NULL),('001','P2026052300003',4,'20260400011','20260400010','2026-05-23 19:23:25',NULL,NULL),('001','P2026052300004',1,'20260400012','20260400013','2026-05-31 20:27:19',NULL,NULL),('001','P2026052300004',2,'20260400010','20260400013','2026-05-31 20:27:19',NULL,NULL),('001','P2026052300004',3,'20260400011','20260400013','2026-05-31 20:27:19',NULL,NULL),('001','P2026052300004',4,'20260400014','20260400013','2026-05-31 20:27:19',NULL,NULL),('001','P2026052500005',1,'20260400010','20260400012','2026-05-25 20:38:50',NULL,NULL),('001','P2026052500005',2,'20260400013','20260400012','2026-05-25 20:38:50',NULL,NULL),('001','P2026052500006',1,'20260400011','20260400012','2026-05-25 20:39:01',NULL,NULL),('001','P2026052500006',2,'20260400014','20260400012','2026-05-25 20:39:01',NULL,NULL);
/*!40000 ALTER TABLE `tb_aprv_line_preset_d` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_attd_close`
--

LOCK TABLES `tb_attd_close` WRITE;
/*!40000 ALTER TABLE `tb_attd_close` DISABLE KEYS */;
INSERT INTO `tb_attd_close` VALUES ('001','00001','*','Y','202604','OPEN','2026-05-25 15:45:22','20260400010','2026-05-25 15:45:32','20260400010',NULL,'20260400010','2026-05-25 15:45:22','20260400010','2026-05-25 15:45:32'),('001','00001','*','Y','202610','OPEN','2026-05-25 15:44:47','20260400010','2026-05-25 15:44:51','20260400010',NULL,'20260400010','2026-05-25 15:44:47','20260400010','2026-05-25 15:44:51');
/*!40000 ALTER TABLE `tb_attd_close` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_attd_close_hist`
--

LOCK TABLES `tb_attd_close_hist` WRITE;
/*!40000 ALTER TABLE `tb_attd_close_hist` DISABLE KEYS */;
INSERT INTO `tb_attd_close_hist` VALUES ('00001','001','00001','202610','*','Y','CLOSE','20260400010','2026-05-25 15:44:47',NULL,'20260400010','2026-05-25 15:44:47'),('00002','001','00001','202610','*','Y','UNCLOSE','20260400010','2026-05-25 15:44:51',NULL,'20260400010','2026-05-25 15:44:51'),('00003','001','00001','202604','*','Y','CLOSE','20260400010','2026-05-25 15:45:22',NULL,'20260400010','2026-05-25 15:45:22'),('00004','001','00001','202604','*','Y','UNCLOSE','20260400010','2026-05-25 15:45:32',NULL,'20260400010','2026-05-25 15:45:32');
/*!40000 ALTER TABLE `tb_attd_close_hist` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_attd_std_time_rule`
--

LOCK TABLES `tb_attd_std_time_rule` WRITE;
/*!40000 ALTER TABLE `tb_attd_std_time_rule` DISABLE KEYS */;
INSERT INTO `tb_attd_std_time_rule` VALUES ('001','01','05','20260400010','2026-05-06 21:21:05','20260400010','2026-05-06 21:21:13'),('001','02','05','20260400010','2026-05-06 21:21:06','20260400010','2026-05-06 21:21:13');
/*!40000 ALTER TABLE `tb_attd_std_time_rule` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_attd_std_time_rule_his`
--

LOCK TABLES `tb_attd_std_time_rule_his` WRITE;
/*!40000 ALTER TABLE `tb_attd_std_time_rule_his` DISABLE KEYS */;
INSERT INTO `tb_attd_std_time_rule_his` VALUES ('0','001','01','02','20260400010','2026-03-06 21:21:05'),('1','001','02','03','20260400010','2026-03-25 21:21:06'),('2','001','01','04','20260400010','2026-04-06 21:21:09'),('3','001','02','02','20260400010','2026-04-24 21:21:09'),('4','001','01','05','20260400010','2026-05-01 21:21:13'),('5','001','02','05','20260400010','2026-05-01 21:21:13');
/*!40000 ALTER TABLE `tb_attd_std_time_rule_his` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_audit_log`
--

LOCK TABLES `tb_audit_log` WRITE;
/*!40000 ALTER TABLE `tb_audit_log` DISABLE KEYS */;
INSERT INTO `tb_audit_log` VALUES ('A2026060600001','001','20260400013','01','NOTICE_FILE','N20260606001/005-20260606-00001','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400013','2026-06-06 18:03:23'),('A2026060600002','001','20260400013','01','NOTICE_FILE','N20260606001/005-20260606-00002','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400013','2026-06-06 18:03:24'),('A2026060600003','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-06 23:37:24'),('A2026060700004','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 00:30:35'),('A2026060700005','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 00:32:40'),('A2026060700006','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 21:05:15'),('A2026060700007','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 22:06:37'),('A2026060700008','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 22:06:52'),('A2026060700009','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 22:12:20'),('A2026060700010','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 22:12:35'),('A2026060700011','001','20260400010','01','USER_CREATE_TEMPLATE',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',NULL,'N','20260400010','2026-06-07 22:12:46');
/*!40000 ALTER TABLE `tb_audit_log` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_auth_token`
--

LOCK TABLES `tb_auth_token` WRITE;
/*!40000 ALTER TABLE `tb_auth_token` DISABLE KEYS */;
INSERT INTO `tb_auth_token` VALUES ('001','20260400001','00cc2d83607c49bf8e52e7b0a54760cf','WEB',NULL,'ltLShXsk_hEdJ-_ak_AIkcAHvPBnrJvW3Ye1b73Lexc','2026-04-13 19:50:17','2026-04-20 19:50:17','Y','2026-04-13 20:03:57',NULL,NULL,'20260400001','2026-04-13 19:50:17','20260400001','2026-04-13 20:03:57'),('001','20260400001','0144aa84483f42b18188336cccca8fce','WEB',NULL,'mxiu18cyF6lMaUXXbsvMm3fGquyyqhI20-JXy1LI03Q','2026-04-13 19:08:22','2026-04-20 19:08:22','Y','2026-04-13 19:09:03',NULL,NULL,'20260400001','2026-04-13 19:08:22','20260400001','2026-04-13 19:09:03'),('001','20260400001','02a2ee2812dd4ff8ac73b614a6b8bb49','WEB',NULL,'H-GJAhSG04-2z5O1GrAtx57Q3_woVecvJPLChKe_Fwc','2026-04-19 13:13:06','2026-04-26 13:13:06','Y','2026-04-19 14:13:40',NULL,NULL,'20260400001','2026-04-19 13:13:06','20260400001','2026-04-19 14:13:40'),('001','20260400001','0510318df44149379856a75fa2ad4aaf','WEB',NULL,'1OINp-nmd4zSheJN8qwOrnOCh-gixPal-FpTXWtY6rY','2026-04-17 22:29:16','2026-04-24 22:29:16','Y','2026-04-17 22:31:57',NULL,NULL,'20260400001','2026-04-17 22:29:16','20260400001','2026-04-17 22:31:57'),('001','20260400001','0511d1982dd94003b4a2d283d3224f0e','WEB',NULL,'93lAAUxlok2mzAZPHtVzrH3TpyMlEhm2R4KNGUfK7hc','2026-04-12 22:12:00','2026-04-19 22:12:00','Y','2026-04-13 18:26:33',NULL,NULL,'20260400001','2026-04-12 22:12:00','20260400001','2026-04-13 18:26:33'),('001','20260400001','07bc28c5ab1d4440946b7ec75f0daa73','WEB',NULL,'JSRkzNWTI4nvvK3TBDMieIPKZQ1fFm6reoppfl_jUGQ','2026-04-15 22:32:02','2026-04-22 22:32:02','Y','2026-04-15 22:32:36',NULL,NULL,'20260400001','2026-04-15 22:32:02','20260400001','2026-04-15 22:32:36'),('001','20260400001','08f156fc6ad6459e9d8ff45f10f6c3cc','WEB',NULL,'zw7AEudaNNEv0Jw-fhGCkXR6ynD4pUJis6vg-arChS4','2026-04-13 22:22:56','2026-04-20 22:22:56','Y','2026-04-14 19:37:18',NULL,NULL,'20260400001','2026-04-13 22:22:56','20260400001','2026-04-14 19:37:18'),('001','20260400001','0b05f3a63cd54e9b9912968b3f95cf58','WEB',NULL,'p6ZpujDmB-J4Gl76xqnu66DGZjMx5a0Mz6wGg3FX2Us','2026-04-25 19:27:49','2026-05-02 19:27:49','Y','2026-04-25 19:38:13',NULL,NULL,'20260400001','2026-04-25 19:27:49','20260400001','2026-04-25 19:38:13'),('001','20260400001','0c466e1542ca48e0a72767a73ac8a43c','WEB',NULL,'68i_fseGJFAYuxg5dLKLe0DNPl3xtLZlDwnImbQU1N8','2026-04-12 22:06:23','2026-04-19 22:06:23','Y','2026-04-12 22:09:24',NULL,NULL,'20260400001','2026-04-12 22:06:23','20260400001','2026-04-12 22:09:24'),('001','20260400001','0daa092ca61f4bb48377e3ea1bc5b342','WEB',NULL,'hlac_Ppz0TpiAccRRBt_j3Pf6UJrNSdGOmvixJbWNtA','2026-04-25 19:46:08','2026-05-02 19:46:08','Y','2026-04-25 19:47:59',NULL,NULL,'20260400001','2026-04-25 19:46:08','20260400001','2026-04-25 19:47:59'),('001','20260400001','10b233ea31d44da9ad3e0f51964b0d5b','WEB',NULL,'eAN72bGcSXCdayYUoUGMO7ZA_ooL9ER60-9U8X7NwCE','2026-04-14 19:37:18','2026-04-21 19:37:18','Y','2026-04-14 19:37:27',NULL,NULL,'20260400001','2026-04-14 19:37:18','20260400001','2026-04-14 19:37:27'),('001','20260400001','1244609adb4d44dd89cb71dbbb6c5f92','WEB',NULL,'S1L4SlDcBjnu-6nsW8Wh91VUQFRscUvWBdLn0863GwM','2026-04-18 19:28:55','2026-04-25 19:28:55','Y','2026-04-18 20:20:19',NULL,NULL,'20260400001','2026-04-18 19:28:55','20260400001','2026-04-18 20:20:19'),('001','20260400001','1735073f30ee4acc87ce3f09a27b3145','WEB',NULL,'IB1bVer_TGW8X7Ht0wHb8RO7Yvq7ygSxuPVf34KYZm0','2026-04-15 22:39:18','2026-04-22 22:39:18','Y','2026-04-15 22:39:52',NULL,NULL,'20260400001','2026-04-15 22:39:18','20260400001','2026-04-15 22:39:52'),('001','20260400001','1b5151543b0e4e308b3be4ad49219716','WEB',NULL,'7KfkqlqchcJQ97yeoyQtfSxq8-WxmexD49vgYRpS0vs','2026-04-13 21:05:04','2026-04-20 21:05:04','Y','2026-04-13 21:06:35',NULL,NULL,'20260400001','2026-04-13 21:05:04','20260400001','2026-04-13 21:06:35'),('001','20260400001','1d03021568024e9c88e4af445f97bce9','WEB',NULL,'RlXI4wHvtxnTnoY1GrWaG8g9-pLYhRYHW7BtyKEyNVU','2026-04-25 10:28:31','2026-05-02 10:28:31','Y','2026-04-25 18:41:21',NULL,NULL,'20260400001','2026-04-25 10:28:31','20260400001','2026-04-25 18:41:21'),('001','20260400001','1fcfa8e4f0ca4931b3cab6f07f2bfd75','WEB',NULL,'Li_7Kv7ps6B457PzlzEJCLlHWATKhzw7pMqWDs28Af0','2026-04-13 20:04:03','2026-04-20 20:04:03','Y','2026-04-13 20:22:11',NULL,NULL,'20260400001','2026-04-13 20:04:03','20260400001','2026-04-13 20:22:11'),('001','20260400001','1fdbf6828461462499a42fdd7f906b55','WEB',NULL,'MN3_pRlZ51OHhJD4Cdd8WfeP_WRGque2Xl4H-Nwcbxw','2026-04-25 19:48:03','2026-05-02 19:48:03','Y','2026-04-25 19:48:41',NULL,NULL,'20260400001','2026-04-25 19:48:03','20260400001','2026-04-25 19:48:41'),('001','20260400001','2471c7fdeda84fb9a04173100a4ff51d','WEB',NULL,'elT7frWIrGOYlfD8O8PqEziYqhheMBG2GEKqnmsyG90','2026-04-18 12:20:08','2026-04-25 12:20:08','Y','2026-04-18 19:28:55',NULL,NULL,'20260400001','2026-04-18 12:20:08','20260400001','2026-04-18 19:28:55'),('001','20260400001','24d58e83441b45df9524c7479e3568ec','WEB',NULL,'yhxFFLmQ05OWeV1k75eSqoY5Tmrh-HE_Qu4i4-teNEA','2026-04-13 19:50:11','2026-04-20 19:50:11','Y','2026-04-13 19:50:12',NULL,NULL,'20260400001','2026-04-13 19:50:11','20260400001','2026-04-13 19:50:12'),('001','20260400001','26b92926d1784952b38e25fe73413bb5','WEB',NULL,'P4ZLQUaNJv71yNQRqdrw3iVBZe1x7GRX6f2S9GciHjI','2026-04-25 19:02:07','2026-05-02 19:02:07','Y','2026-04-25 19:27:18',NULL,NULL,'20260400001','2026-04-25 19:02:07','20260400001','2026-04-25 19:27:18'),('001','20260400001','28722d4baae84d1d8887448033e11961','WEB',NULL,'wrokM01OCKRfe9yXNHxBUy_AWPggfFmhEucjsqICYEI','2026-04-12 22:01:34','2026-04-19 22:01:34','Y','2026-04-12 22:06:23',NULL,NULL,'20260400001','2026-04-12 22:01:34','20260400001','2026-04-12 22:06:23'),('001','20260400001','32f70a9cea254416a79d7d2ed4596107','WEB',NULL,'BroO4fT55416eyVcVq1SGYJj7ZMfFtLAzSsRUK3ZOJQ','2026-04-18 09:57:12','2026-04-25 09:57:12','Y','2026-04-18 10:27:31',NULL,NULL,'20260400001','2026-04-18 09:57:12','20260400001','2026-04-18 10:27:31'),('001','20260400001','337d567163d1481b99547fe1e7ec245e','WEB',NULL,'pIcg1B9sKnjKk6cu8rczI-LfZ-9HAfo4UxBflzd-XpI','2026-04-14 22:11:37','2026-04-21 22:11:37','Y','2026-04-14 22:21:34',NULL,NULL,'20260400001','2026-04-14 22:11:37','20260400001','2026-04-14 22:21:34'),('001','20260400001','3386b2d7b9b041d2925f9363723acdbf','WEB',NULL,'KqxNmXFkI-wxvfHCV81UEgmZtFcqZHiQxw1t2irPE4s','2026-04-20 20:37:04','2026-04-27 20:37:04','Y','2026-04-21 22:40:38',NULL,NULL,'20260400001','2026-04-20 20:37:04','20260400001','2026-04-21 22:40:38'),('001','20260400001','36eda93ba2c444919c11b8e2a61f5c9f','WEB',NULL,'j6yxovzHdRxtP4iDYI84rqdo6zqtcgISH9eq0tIaqZs','2026-04-14 20:33:27','2026-04-21 20:33:27','Y','2026-04-14 20:34:36',NULL,NULL,'20260400001','2026-04-14 20:33:27','20260400001','2026-04-14 20:34:36'),('001','20260400001','37d3bbbc08e34fd7bbbb7123b99d3edf','WEB',NULL,'iw73q6aVoJ77fUwNDZ4EPWNb0yHziSXNtO8jFKHL_Fw','2026-04-25 19:52:14','2026-05-02 19:52:14','Y','2026-04-25 19:52:41',NULL,NULL,'20260400001','2026-04-25 19:52:14','20260400001','2026-04-25 19:52:41'),('001','20260400001','38c05718cef64b83b34020b88e6617ed','WEB',NULL,'x2dQATEuRGUyT9TujNyoFqgQl0LYSALYzNJ10Nq0Hww','2026-04-25 18:41:25','2026-05-02 18:41:25','Y','2026-04-25 18:41:33',NULL,NULL,'20260400001','2026-04-25 18:41:25','20260400001','2026-04-25 18:41:33'),('001','20260400001','395cd7c7c0f7452a9b03d1299d7ca42d','WEB',NULL,'w78sbe7OqJafuMYX_Xm8U_E3Jqp34nT7y7D2XacHCXk','2026-04-13 18:26:33','2026-04-20 18:26:33','Y','2026-04-13 18:27:55',NULL,NULL,'20260400001','2026-04-13 18:26:33','20260400001','2026-04-13 18:27:55'),('001','20260400001','3a8110c1320743c4b109167daa70cfa6','WEB',NULL,'cKpQNrwLvUWWWh7TPxtAJ3vz7D24rTGTt99FXzPpNYI','2026-04-17 22:32:02','2026-04-24 22:32:02','Y','2026-04-18 09:57:12',NULL,NULL,'20260400001','2026-04-17 22:32:02','20260400001','2026-04-18 09:57:12'),('001','20260400001','3bc39dd50b884e939c72713ee2e25d83','WEB',NULL,'xQEtvhKK1gcGJRVVOHIncvcMdGwWapDfmDmKxLsRIrQ','2026-04-15 23:15:54','2026-04-22 23:15:54','Y','2026-04-15 23:16:03',NULL,NULL,'20260400001','2026-04-15 23:15:54','20260400001','2026-04-15 23:16:03'),('001','20260400001','4a23bae59ce64c6796f5ff5327df6b29','WEB',NULL,'T3q6YnmmWBdQo-V3JuXN5Wu8lxJAnqdKKvF6VZHkXqQ','2026-04-13 18:55:44','2026-04-20 18:55:44','Y','2026-04-13 19:02:06',NULL,NULL,'20260400001','2026-04-13 18:55:44','20260400001','2026-04-13 19:02:06'),('001','20260400001','4d72f49cdc2145559ee1a59bd28640de','WEB',NULL,'10YCp4kyVTmfkJcE9fXkesQEMUNS61RXzwjv2WMzXCU','2026-04-14 19:37:53','2026-04-21 19:37:53','Y','2026-04-14 20:25:30',NULL,NULL,'20260400001','2026-04-14 19:37:53','20260400001','2026-04-14 20:25:30'),('001','20260400001','5240d25d062e44c5bd68e4bcb3b1eec6','WEB',NULL,'XC-LhD-OPzUm6h5VoKC_16HmleVn1cHST1EwWgNgT1k','2026-04-13 18:54:14','2026-04-20 18:54:14','Y','2026-04-13 18:55:36',NULL,NULL,'20260400001','2026-04-13 18:54:14','20260400001','2026-04-13 18:55:36'),('001','20260400001','52716eaee9e14c638c44f085252f60ed','WEB',NULL,'seb5MALz0wCaJTG6_J8rewA_Fy-xwu08I_I98sAMp50','2026-04-18 20:21:19','2026-04-25 20:21:19','Y','2026-04-18 20:25:39',NULL,NULL,'20260400001','2026-04-18 20:21:19','20260400001','2026-04-18 20:25:39'),('001','20260400001','5354963779b44604931ca5aa72f6bee3','WEB',NULL,'useVVU0Ie7Gm5qzR6DrjzQ_7RzWZyrIyhJbMdwZ4wgY','2026-04-25 19:52:44','2026-05-02 19:52:44','Y','2026-04-26 14:25:03',NULL,NULL,'20260400001','2026-04-25 19:52:44','20260400001','2026-04-26 14:25:03'),('001','20260400001','56c145bfd8214ba8915a4ff7d532091a','WEB',NULL,'tvB40FObx2MD9A50aqYlGxrBSAnV7x9BVUIlB35PGqs','2026-04-13 21:03:12','2026-04-20 21:03:12','Y','2026-04-13 21:05:04',NULL,NULL,'20260400001','2026-04-13 21:03:12','20260400001','2026-04-13 21:05:04'),('001','20260400001','572e06392c514c1aaa808e72101a90cc','WEB',NULL,'S_k_R5CTzavKYe5Ngl55dRrwZp_4gvV1B55Aq3SwUbI','2026-04-23 19:09:28','2026-04-30 19:09:28','Y','2026-04-24 19:38:51',NULL,NULL,'20260400001','2026-04-23 19:09:28','20260400001','2026-04-24 19:38:51'),('001','20260400001','58a184cc852748198d9794ced3fedbf1','WEB',NULL,'nUtU-8WeffCC4N6HKbv-1QZLWOptuvr3TSKRuprXl30','2026-04-14 20:35:45','2026-04-21 20:35:45','Y','2026-04-14 20:36:01',NULL,NULL,'20260400001','2026-04-14 20:35:45','20260400001','2026-04-14 20:36:01'),('001','20260400001','597b00c027e74f758098e06260889fda','WEB',NULL,'TT5LfPmbvudzABEy-TVeNPD6Q2O_wsnh1KaiXPdxZ54','2026-04-16 21:42:07','2026-04-23 21:42:07','Y','2026-04-17 17:23:31',NULL,NULL,'20260400001','2026-04-16 21:42:07','20260400001','2026-04-17 17:23:31'),('001','20260400001','5fc310510444441c9f889297b5053dac','WEB',NULL,'gjjYcc2_czX1Md8lcGJSWA22SliKZYTfWet6pG5tTiU','2026-04-25 19:27:22','2026-05-02 19:27:22','Y','2026-04-25 19:27:45',NULL,NULL,'20260400001','2026-04-25 19:27:22','20260400001','2026-04-25 19:27:45'),('001','20260400001','6081caedeb50403bb996f01a65f0f855','WEB',NULL,'bdAZ9-dmXSXqPZc6muKAoYo9CmE89mSnjQrZZ7Brees','2026-04-21 22:40:38','2026-04-28 22:40:38','Y','2026-04-22 18:34:16',NULL,NULL,'20260400001','2026-04-21 22:40:38','20260400001','2026-04-22 18:34:16'),('001','20260400001','62f131ddf4734e5193e991b8e70d4063','WEB',NULL,'MRVSeD8nxel5JNKG5sUFbqeM8BIDhtzfRn56wZcUvtY','2026-04-14 21:07:26','2026-04-21 21:07:26','Y','2026-04-14 22:11:37',NULL,NULL,'20260400001','2026-04-14 21:07:26','20260400001','2026-04-14 22:11:37'),('001','20260400001','6858bad9764a4d5eb342a18d0ab8343d','WEB',NULL,'plBSfq0H_Zqwsk_kP4O2P6S8p4qIV9lu_iSWfAahnEw','2026-04-13 19:07:19','2026-04-20 19:07:19','Y','2026-04-13 19:08:15',NULL,NULL,'20260400001','2026-04-13 19:07:19','20260400001','2026-04-13 19:08:15'),('001','20260400001','6ca43ce8030a4711b46c4b090e10bdd0','WEB',NULL,'3tWmp77oYL4PnWQd7aGDLJsob9bvP0KgrawqwBhH6Qs','2026-04-15 22:28:41','2026-04-22 22:28:41','Y','2026-04-15 22:31:54',NULL,NULL,'20260400001','2026-04-15 22:28:41','20260400001','2026-04-15 22:31:54'),('001','20260400001','6d10601c82dc4e20a0694c3da1a1c931','WEB',NULL,'ElbL4VsrwMzyGMDxvdbcuIYVgGxtyoJE8Mvz5C0A-rI','2026-04-19 21:42:17','2026-04-26 21:42:17','Y','2026-04-20 20:37:04',NULL,NULL,'20260400001','2026-04-19 21:42:17','20260400001','2026-04-20 20:37:04'),('001','20260400001','71e0ec325e7040fe850e1dbc68bbcb64','WEB',NULL,'-_iVoWnCxyaSX1HS2SMF4DRMghmZ2Mx_HBuU3k9y_Ak','2026-04-15 20:16:28','2026-04-22 20:16:28','Y','2026-04-15 21:21:16',NULL,NULL,'20260400001','2026-04-15 20:16:28','20260400001','2026-04-15 21:21:16'),('001','20260400001','71ebe53ac2994bdab58939797a520c70','WEB',NULL,'IzXRO42mqsVW8sbFZLM9xZUl29hO2DaV0pYtdqr20JU','2026-04-15 22:33:11','2026-04-22 22:33:11','Y','2026-04-15 22:37:39',NULL,NULL,'20260400001','2026-04-15 22:33:11','20260400001','2026-04-15 22:37:39'),('001','20260400001','74f7340801d84246b74a337091a13c26','WEB',NULL,'7VRRINHToTGEnTzCipCsOHfWK4dnKFd5OJqJJfAxCO0','2026-04-19 15:56:38','2026-04-26 15:56:38','Y','2026-04-19 17:19:08',NULL,NULL,'20260400001','2026-04-19 15:56:38','20260400001','2026-04-19 17:19:08'),('001','20260400001','775df70ae64742bfa2c43a512735da5d','WEB',NULL,'_7x6uxDJxpCqlPVsQrwTtuF2MzTC1E6HJSWyn1f4A6E','2026-04-14 20:34:45','2026-04-21 20:34:45','Y','2026-04-14 20:34:58',NULL,NULL,'20260400001','2026-04-14 20:34:45','20260400001','2026-04-14 20:34:58'),('001','20260400001','7b37338919ad4cdba9dcc9e001955692','WEB',NULL,'ZjIBT1JqTxW7z93NYG3ELJpw5u4SEKhgIWS4qvDMRZs','2026-04-14 22:33:05','2026-04-21 22:33:05','Y','2026-04-15 20:16:07',NULL,NULL,'20260400001','2026-04-14 22:33:05','20260400001','2026-04-15 20:16:07'),('001','20260400001','7b96d967f21f4a37a775f03827cf944a','WEB',NULL,'StSfw0m62cJWAfDXgBS_OsAFXdAGyQRDFLhs5RWzAsY','2026-04-18 21:33:37','2026-04-25 21:33:37','Y','2026-04-19 11:10:19',NULL,NULL,'20260400001','2026-04-18 21:33:37','20260400001','2026-04-19 11:10:19'),('001','20260400001','7cb3a2422d264930b396fbcf9986271b','WEB',NULL,'I79oZN6x1Pvs0NHhIRKJtJS4N-THA0OTEtOaICUPr-E','2026-04-13 20:22:15','2026-04-20 20:22:15','Y','2026-04-13 20:22:36',NULL,NULL,'20260400001','2026-04-13 20:22:15','20260400001','2026-04-13 20:22:36'),('001','20260400001','7f36b25e8ee246438d4caff11cb44edc','WEB',NULL,'dLwPatM78yrDPvMzJPRhNmVwhYql4zjiOhcfcn2V1GA','2026-04-19 20:24:24','2026-04-26 20:24:24','Y','2026-04-19 21:40:47',NULL,NULL,'20260400001','2026-04-19 20:24:24','20260400001','2026-04-19 21:40:47'),('001','20260400001','81403552e28b49cd92995c5b00a26cf8','WEB',NULL,'moWO2W_phpCpNg8R0im9vXNa43bEndZJUFEDdd2X72k','2026-04-14 20:42:06','2026-04-21 20:42:06','Y','2026-04-14 20:42:13',NULL,NULL,'20260400001','2026-04-14 20:42:06','20260400001','2026-04-14 20:42:13'),('001','20260400001','87192d0906d849e38ede4621c01b4210','WEB',NULL,'oIvvWGTapDpiMF-iNznBcsl4uDTb70B5NP2lcBM6i_E','2026-04-13 21:01:41','2026-04-20 21:01:41','Y','2026-04-13 21:02:03',NULL,NULL,'20260400001','2026-04-13 21:01:41','20260400001','2026-04-13 21:02:03'),('001','20260400001','8b1cb30276b54a3da39967436a1dadab','WEB',NULL,'U0nRi4V8oVSTV82GknPs1W2Gw50aq8Hptacfulv1PKE','2026-04-17 17:23:31','2026-04-24 17:23:31','Y','2026-04-17 17:23:49',NULL,NULL,'20260400001','2026-04-17 17:23:31','20260400001','2026-04-17 17:23:49'),('001','20260400001','8eb9232a41754df1b043112fb8c8a436','WEB',NULL,'X9dEtuPExk_J4u8jvMqilnzf4nlw8IpqZsfUKi5dWsM','2026-04-15 22:39:02','2026-04-22 22:39:02','Y','2026-04-15 22:39:14',NULL,NULL,'20260400001','2026-04-15 22:39:02','20260400001','2026-04-15 22:39:14'),('001','20260400001','90baf596de3f49c19ad0e2f4239f3fcd','WEB',NULL,'IqP-kZvv4CUnI8oDw0d6gv3WJiPfZn8NuGLFBV4Vun4','2026-04-25 18:41:36','2026-05-02 18:41:36','Y','2026-04-25 19:02:02',NULL,NULL,'20260400001','2026-04-25 18:41:36','20260400001','2026-04-25 19:02:02'),('001','20260400001','91add3f28c4e4ce7bf1a105331106c87','WEB',NULL,'VHFprU5KyKugdswlV_RHH3nnMBOM47Ra8VPybmhWgAo','2026-04-17 17:23:57','2026-04-24 17:23:57','Y','2026-04-17 18:24:57',NULL,NULL,'20260400001','2026-04-17 17:23:57','20260400001','2026-04-17 18:24:57'),('001','20260400001','91fc310b478342c190a53ddb3963f93e','WEB',NULL,'GmVDdk4dTkzJcy0Pnn6F7n94upfgv788NeroPLg0Its','2026-04-13 21:06:35','2026-04-20 21:06:35','Y','2026-04-13 21:17:57',NULL,NULL,'20260400001','2026-04-13 21:06:35','20260400001','2026-04-13 21:17:57'),('001','20260400001','9243ebe11adf451f8ea0df5c18bce942','WEB',NULL,'Qz83hg0hsYxbeFcgaiPyTFMFceO44e7Rg93PS905HVU','2026-04-13 21:51:21','2026-04-20 21:51:21','Y','2026-04-13 22:22:52',NULL,NULL,'20260400001','2026-04-13 21:51:21','20260400001','2026-04-13 22:22:52'),('001','20260400001','930aec43743e4ca6b5840f62f0ad4aae','WEB',NULL,'jLW5AEpmUu-2RojxppodjJDPZW03bcb_RHmEBEmviGw','2026-04-17 18:24:57','2026-04-24 18:24:57','Y','2026-04-17 19:13:10',NULL,NULL,'20260400001','2026-04-17 18:24:57','20260400001','2026-04-17 19:13:10'),('001','20260400001','9523d29067f34b6eb692f6e96bb5c313','WEB',NULL,'F13-5kABJIEv4QA_fbS8w4TI6f_6lypQBYqZfxWBwLU','2026-04-18 20:25:56','2026-04-25 20:25:56','Y','2026-04-18 21:33:37',NULL,NULL,'20260400001','2026-04-18 20:25:56','20260400001','2026-04-18 21:33:37'),('001','20260400001','9704c4fa90444842ab46856e24203082','WEB',NULL,'JKeXj2O65uAATOzp5BDZ-xUC18GSO7kX4v2cg_pBWZc','2026-04-13 20:22:40','2026-04-20 20:22:40','Y','2026-04-13 20:39:48',NULL,NULL,'20260400001','2026-04-13 20:22:40','20260400001','2026-04-13 20:39:48'),('001','20260400001','985fed8ec9704a6586784755c47d1b4b','WEB',NULL,'B1YtYMRI7zmwuychqVphOIX2gYn2SIGR8VMWgD__QDU','2026-04-19 21:40:47','2026-04-26 21:40:47','Y','2026-04-19 21:42:11',NULL,NULL,'20260400001','2026-04-19 21:40:47','20260400001','2026-04-19 21:42:11'),('001','20260400001','9c1b5c4e031b4e59b07dbb5e84a7e544','WEB',NULL,'PwEhwTL7--X_RJpXuVWFHq27rEdwRdWwgHjsfe4qELo','2026-04-24 19:38:51','2026-05-01 19:38:51','Y','2026-04-25 10:28:31',NULL,NULL,'20260400001','2026-04-24 19:38:51','20260400001','2026-04-25 10:28:31'),('001','20260400001','a34093b853df44eb898b9150127af669','WEB',NULL,'hrKaEZRYwWvVXIqkEBQ887RCxF0ggMKQ27kq2GJqQ_E','2026-04-14 20:43:09','2026-04-21 20:43:09','Y','2026-04-14 20:43:43',NULL,NULL,'20260400001','2026-04-14 20:43:09','20260400001','2026-04-14 20:43:43'),('001','20260400001','aa98449da3e146ef88bf58782cb68bd9','WEB',NULL,'yJAOQa8Wd75Ljznn1t1wjcY8wS__BTEmTw2KYRPudbA','2026-04-22 18:34:16','2026-04-29 18:34:16','Y','2026-04-23 19:09:28',NULL,NULL,'20260400001','2026-04-22 18:34:16','20260400001','2026-04-23 19:09:28'),('001','20260400001','ac35bc1d46fb492d8dadbce12bfe4e3f','WEB',NULL,'btP5TCp1a7ehwz-2SXRBvDd3ZDncjTQq8h6WiO40pSk','2026-04-27 18:49:16','2026-05-04 18:49:16','Y','2026-04-28 18:36:01',NULL,NULL,'20260400001','2026-04-27 18:49:16','20260400001','2026-04-28 18:36:01'),('001','20260400001','accea0129bd94ba2ba0902d7168b5261','WEB',NULL,'-jEH6pR44mLwfoM7WrmhdJvM-KgN21RA4ort5U2pO4k','2026-04-15 23:16:43','2026-04-22 23:16:43','Y','2026-04-15 23:19:42',NULL,NULL,'20260400001','2026-04-15 23:16:43','20260400001','2026-04-15 23:19:42'),('001','20260400001','b08b5fcdff8a4523a477b69545395e77','WEB',NULL,'TGRkTtgaj4GzuebxeW1LqOY6sLus5kKJUjPmkw1kH0E','2026-04-15 23:20:14','2026-04-22 23:20:14','Y','2026-04-16 19:33:12',NULL,NULL,'20260400001','2026-04-15 23:20:14','20260400001','2026-04-16 19:33:12'),('001','20260400001','b28bb3f7fd8e436696c02ae41c41292c','WEB',NULL,'fhpFVdwXGueb0wHaGB2vijrc5Q8Ccr0itqhID0XNVfw','2026-04-25 19:48:45','2026-05-02 19:48:45','Y','2026-04-25 19:49:34',NULL,NULL,'20260400001','2026-04-25 19:48:45','20260400001','2026-04-25 19:49:34'),('001','20260400001','b72d325ba0714eb5afe30bac4728ea02','WEB',NULL,'Xfjp7C2z54EaB1Zsod_pqVD-ahPUa9XRVdBlaACgjJI','2026-04-14 20:43:43','2026-04-21 20:43:43','Y','2026-04-14 20:52:49',NULL,NULL,'20260400001','2026-04-14 20:43:43','20260400001','2026-04-14 20:52:49'),('001','20260400001','b83feba1181c433ba1abf628974bb77b','WEB',NULL,'FB7IXwQ7Vx1iDSfNwvPuEpxMnpbbjBqhcUnePJ2-Tws','2026-04-13 18:28:01','2026-04-20 18:28:01','Y','2026-04-13 18:54:14',NULL,NULL,'20260400001','2026-04-13 18:28:01','20260400001','2026-04-13 18:54:14'),('001','20260400001','b8e0dce075704030b30e2eb90a3cf373','WEB',NULL,'kDIMO1opRVCTEMk3IYn0-EpSmg7oyWlxguW1ZEi-HCw','2026-04-19 18:20:05','2026-04-26 18:20:05','Y','2026-04-19 20:24:24',NULL,NULL,'20260400001','2026-04-19 18:20:05','20260400001','2026-04-19 20:24:24'),('001','20260400001','bb8e99d16bd844b280d963061fb030eb','WEB',NULL,'e2vbsNmODvSZZYBITJlENRejb3hfPFAZUmBLlECXDHs','2026-04-19 14:13:40','2026-04-26 14:13:40','Y','2026-04-19 15:56:38',NULL,NULL,'20260400001','2026-04-19 14:13:40','20260400001','2026-04-19 15:56:38'),('001','20260400001','bc9312a087dd4523a6453b866a01b065','WEB',NULL,'A3UnAcikNtwVLKDOVWXbXSeF9zIit8fxVTP5d3Hi2so','2026-04-14 20:25:35','2026-04-21 20:25:35','Y','2026-04-14 20:33:07',NULL,NULL,'20260400001','2026-04-14 20:25:35','20260400001','2026-04-14 20:33:07'),('001','20260400001','bef02fd9cdf84dc4b54bcc114712cef1','WEB',NULL,'-tUNDzoVNqk-6M3aGo9SRwJ2Kwbb12Q644n0oJdltgM','2026-04-15 23:19:45','2026-04-22 23:19:45','Y','2026-04-15 23:19:52',NULL,NULL,'20260400001','2026-04-15 23:19:45','20260400001','2026-04-15 23:19:52'),('001','20260400001','c113f38300c14c40aa735075219b1620','WEB',NULL,'GJro1oPPPvXDJ8Q566PGUfl1doj-d1zo4Ghl1cPyLfs','2026-04-13 20:39:53','2026-04-20 20:39:53','Y','2026-04-13 21:01:41',NULL,NULL,'20260400001','2026-04-13 20:39:53','20260400001','2026-04-13 21:01:41'),('001','20260400001','c2c1104ca34e4cad9a64a76fcfc693f4','WEB',NULL,'WX0Pyr-Xtak60Y1Rfz__SmENxCWcYTchngembJ4e48U','2026-04-12 22:11:54','2026-04-19 22:11:54','Y','2026-04-12 22:11:55',NULL,NULL,'20260400001','2026-04-12 22:11:54','20260400001','2026-04-12 22:11:55'),('001','20260400001','c44625ed8d494edda8f4eedcfca6deac','WEB',NULL,'BT4Ch8XI6ehwjXT2Xg5b9o8llJ7BvIsXIB1Eb2wSrpQ','2026-04-12 22:10:06','2026-04-19 22:10:06','Y','2026-04-12 22:10:07',NULL,NULL,'20260400001','2026-04-12 22:10:06','20260400001','2026-04-12 22:10:07'),('001','20260400001','cce65825b0054246901a55b0e773675d','WEB',NULL,'4TCY6Af1dY48MUOzC0EXIhnhxlXw2_ubOOe-EBVGAX0','2026-04-15 20:16:07','2026-04-22 20:16:07','Y','2026-04-15 20:16:15',NULL,NULL,'20260400001','2026-04-15 20:16:07','20260400001','2026-04-15 20:16:15'),('001','20260400001','ccf184a8f6854e8797af463eb2995ed2','WEB',NULL,'t7IcRVwmTRyiW2gUnCBY34VhED3yVwyaQ1RNiEv8Wi8','2026-04-25 19:38:19','2026-05-02 19:38:19','Y','2026-04-25 19:40:30',NULL,NULL,'20260400001','2026-04-25 19:38:19','20260400001','2026-04-25 19:40:30'),('001','20260400001','ceaddc5ed1bd48ac8e7024f52e20afc3','WEB',NULL,'zLgy31eyZgM5hVtyd_CAk_D9p_WotOEvlesj71jz9cc','2026-04-26 14:25:03','2026-05-03 14:25:03','Y','2026-04-27 18:49:16',NULL,NULL,'20260400001','2026-04-26 14:25:03','20260400001','2026-04-27 18:49:16'),('001','20260400001','cf9a1326abf34b2492c96dccb42fc280','WEB',NULL,'OU7sCWt_b19TzkqNx2OHJ9ztF5C2BmcTY4-WLMVAxMg','2026-04-25 19:40:33','2026-05-02 19:40:33','Y','2026-04-25 19:45:05',NULL,NULL,'20260400001','2026-04-25 19:40:33','20260400001','2026-04-25 19:45:05'),('001','20260400001','d07276e4ca784e3da2301474eb376a92','WEB',NULL,'bew1U8asGxf4sl0M9yqNHchWPVhGSiXUOcg5a8dGO-U','2026-04-13 21:18:02','2026-04-20 21:18:02','Y','2026-04-13 21:51:17',NULL,NULL,'20260400001','2026-04-13 21:18:02','20260400001','2026-04-13 21:51:17'),('001','20260400001','d6e147fcedf74afe8bd6b94bd01369ab','WEB',NULL,'YxY2kbiEw7_60VpgGAz2Aff7oONy7jB067w9JyOKMPI','2026-04-19 11:10:19','2026-04-26 11:10:19','Y','2026-04-19 13:13:06',NULL,NULL,'20260400001','2026-04-19 11:10:19','20260400001','2026-04-19 13:13:06'),('001','20260400001','d817ede413174ddfacc76d88ef9a8fa6','WEB',NULL,'gSNZRKt9Jkj9QPnA1N5slgUkojB-uWleRtUW0DbR210','2026-04-25 19:50:07','2026-05-02 19:50:07','Y','2026-04-25 19:51:53',NULL,NULL,'20260400001','2026-04-25 19:50:07','20260400001','2026-04-25 19:51:53'),('001','20260400001','d97c0d95b3ae4395b98064da578b292d','WEB',NULL,'aq-W8qurs85fxbDo25iQWd7xSobSEXhT6rhgP6mYU8o','2026-04-15 22:43:39','2026-04-22 22:43:39','Y','2026-04-15 22:44:00',NULL,NULL,'20260400001','2026-04-15 22:43:39','20260400001','2026-04-15 22:44:00'),('001','20260400001','dc6e895094524983b16f227999c2221e','WEB',NULL,'A5L_4kA6IOsXcZS3qcg1TY_UsPmbkwOML6kzEzg6tnM','2026-04-14 19:37:33','2026-04-21 19:37:33','Y','2026-04-14 19:37:46',NULL,NULL,'20260400001','2026-04-14 19:37:33','20260400001','2026-04-14 19:37:46'),('001','20260400001','e0832c88ab694e77b0e2a33af61b7fb2','WEB',NULL,'CdYePHVJV9C8KFOWHa4KXc1xxiO4IjufgWNHUmCgQHg','2026-04-13 21:02:03','2026-04-20 21:02:03','Y','2026-04-13 21:03:12',NULL,NULL,'20260400001','2026-04-13 21:02:03','20260400001','2026-04-13 21:03:12'),('001','20260400001','e152c4d6f00a46278fcb940ec02b7cc0','WEB',NULL,'QudAbfswFGYQfotbERG6_xJgwwP8ZEUOoR0pQRPuNfY','2026-04-14 20:36:17','2026-04-21 20:36:17','Y','2026-04-14 20:42:01',NULL,NULL,'20260400001','2026-04-14 20:36:17','20260400001','2026-04-14 20:42:01'),('001','20260400001','e299f1c55f5847d9afc4b6e03ca52048','WEB',NULL,'oT47IjkYQYXSY87IzXtO2Gg9vUOZExZDJTj1dIu2V2U','2026-04-14 20:36:11','2026-04-21 20:36:11','Y','2026-04-14 20:36:12',NULL,NULL,'20260400001','2026-04-14 20:36:11','20260400001','2026-04-14 20:36:12'),('001','20260400001','eec539bfb7d144e1a837ed1dee02bbf4','WEB',NULL,'1ta0L5oddXxiEbyko0HXtE3-7dJalE_X0GGQSrGfsB4','2026-04-17 19:13:18','2026-04-24 19:13:18','Y','2026-04-17 22:29:11',NULL,NULL,'20260400001','2026-04-17 19:13:18','20260400001','2026-04-17 22:29:11'),('001','20260400001','ef02d024f5dc45dfadddcea7848f4492','WEB',NULL,'u8WF9KkFxFpog1UWVNGoejhWv1MAXcnf4kd2VddPepA','2026-04-25 19:49:38','2026-05-02 19:49:38','Y','2026-04-25 19:49:47',NULL,NULL,'20260400001','2026-04-25 19:49:38','20260400001','2026-04-25 19:49:47'),('001','20260400001','f0427f9265024ca9b142d8e0b64a9c21','WEB',NULL,'C4Y82lpuUz8DiSVUfQFmQpHnda7sAqFFEOYdfJ1KgAk','2026-04-25 19:45:10','2026-05-02 19:45:10','Y','2026-04-25 19:46:02',NULL,NULL,'20260400001','2026-04-25 19:45:10','20260400001','2026-04-25 19:46:02'),('001','20260400001','f6fc6f84ffc941489f11f406b868767b','WEB',NULL,'VJg-WXKM8vBPK7IAXhJvPMx4_AF19_zYjMIFuNWlZD0','2026-04-19 17:19:08','2026-04-26 17:19:08','Y','2026-04-19 18:20:05',NULL,NULL,'20260400001','2026-04-19 17:19:08','20260400001','2026-04-19 18:20:05'),('001','20260400001','f7d2f0166e4743b7b0beb5a411b35c6b','WEB',NULL,'-aC1HaeeDmVBZZZ52Uo_l0rweoygtj5Agf1o9jzNuao','2026-04-16 19:33:12','2026-04-23 19:33:12','Y','2026-04-16 21:42:02',NULL,NULL,'20260400001','2026-04-16 19:33:12','20260400001','2026-04-16 21:42:02'),('001','20260400001','faabe80aabfc4a6a9b0bb12750da769a','WEB',NULL,'yl0Q-joJIhhKaVZqJRbrb7tJUxN5gqfGc3tIWjqNWtk','2026-04-15 21:21:16','2026-04-22 21:21:16','Y','2026-04-15 22:28:36',NULL,NULL,'20260400001','2026-04-15 21:21:16','20260400001','2026-04-15 22:28:36'),('001','20260400001','fc29fbbd904e42aaa4412c1ba10a4860','WEB',NULL,'ZBj49ds_8ddATHGrD_jWRDw6qSBwnRvU0yWjsWTQoJs','2026-04-14 20:54:53','2026-04-21 20:54:53','Y','2026-04-14 21:06:15',NULL,NULL,'20260400001','2026-04-14 20:54:53','20260400001','2026-04-14 21:06:15'),('001','20260400001','ff0659cf2ce546ca9456c3928dd03d61','WEB',NULL,'rnlc-v6q26qPT4wwsUUTlxGCQq-fTPb0ToYoh4Jn864','2026-04-18 10:27:44','2026-04-25 10:27:44','Y','2026-04-18 12:20:08',NULL,NULL,'20260400001','2026-04-18 10:27:44','20260400001','2026-04-18 12:20:08'),('001','20260400002','337a26d870f749dbb1d2f9d4d09fde79','WEB',NULL,'KBmeWx-mjgzTj57TJM4a9DvHWJXbZtzIiN8UnuN5o0A','2026-04-14 20:43:01','2026-04-21 20:43:01','Y','2026-04-14 20:43:04',NULL,NULL,'20260400002','2026-04-14 20:43:01','20260400002','2026-04-14 20:43:04'),('001','20260400002','7bf676d04fd04940a636889e70fa99a1','WEB',NULL,'WEVnhbTFW5NDphTMpAWB8nRzNeM1QLcT9t0NqkFBRzA','2026-04-14 22:21:48','2026-04-21 22:21:48','N',NULL,NULL,NULL,'20260400002','2026-04-14 22:21:48',NULL,NULL),('001','20260400003','5e9789d7571b4af791dc0ef5a33755b7','WEB',NULL,'Pf4vy1vJ7pjROnCvDQFGuJfJzV00sa0oeEi0LxT8fJE','2026-04-15 22:32:48','2026-04-22 22:32:48','N',NULL,NULL,NULL,'20260400003','2026-04-15 22:32:48',NULL,NULL),('001','20260400004','0fa14c9bdaca40c1bdc6901c0d035ecf','WEB',NULL,'esHS5Gr_RfmFAkBi4l7VwgssGh3M62WazlDRru1XzV8','2026-04-15 22:37:45','2026-04-22 22:37:45','N',NULL,NULL,NULL,'20260400004','2026-04-15 22:37:45',NULL,NULL),('001','20260400005','31115905338d49959595219b8b72c5cf','WEB',NULL,'fLxr0sUXBTTx1EnhwCU5VEHWSkmKukTrXGV2C7yFTws','2026-04-15 23:19:57','2026-04-22 23:19:57','Y','2026-04-15 23:20:09',NULL,NULL,'20260400005','2026-04-15 23:19:57','20260400005','2026-04-15 23:20:09'),('001','20260400005','8cfa6f3073ce4841bdb4cda0926ba15f','WEB',NULL,'BnCYBo1xzFEvTi2wXHelsZs7wHyL2byosHMi5M93evE','2026-04-15 22:40:34','2026-04-22 22:40:34','Y','2026-04-15 22:41:59',NULL,NULL,'20260400005','2026-04-15 22:40:34','20260400005','2026-04-15 22:41:59'),('001','20260400005','b443d40ba0194d9595eba9da6843e321','WEB',NULL,'w2CECb3i3hGDKKnrjGiIVfScf-NAOEqbXQLOuMVJ_F4','2026-04-15 22:44:05','2026-04-22 22:44:05','Y','2026-04-15 23:12:06',NULL,NULL,'20260400005','2026-04-15 22:44:05','20260400005','2026-04-15 23:12:06'),('001','20260400006','df883c8ad8a543f3ad8762e3ae1ce5aa','WEB',NULL,'qXspQmX0m3gx6_q_czsvNAnqEmOqnizd9ZE14w2_zYQ','2026-04-15 22:42:44','2026-04-22 22:42:44','Y','2026-04-15 22:43:34',NULL,NULL,'20260400006','2026-04-15 22:42:44','20260400006','2026-04-15 22:43:34'),('001','20260400007','09bedb25fa4b4547b4cd7e2a8cdae5e2','WEB',NULL,'EnnIIe622SGz3b49BSwrzJa6EfGCfYXBl3jFoFJc9Uw','2026-04-22 21:04:30','2026-04-29 21:04:30','Y','2026-04-23 22:20:02',NULL,NULL,'20260400007','2026-04-22 21:04:30','20260400007','2026-04-23 22:20:02'),('001','20260400007','71af79a0dc414e23901f3797124d9ff3','WEB',NULL,'26HdvQ7zXiRe5w6u0VWRsIQwrsrlnB1DTC7ktRu1-bg','2026-04-23 22:20:02','2026-04-30 22:20:02','Y','2026-04-25 11:43:20',NULL,NULL,'20260400007','2026-04-23 22:20:02','20260400007','2026-04-25 11:43:20'),('001','20260400007','767d1c8f78aa415db3937a647f1688ac','WEB',NULL,'VH3BopdZQwA8yuyykbxcW_g8UrD3IpGyofugN7dj3uU','2026-04-25 11:43:20','2026-05-02 11:43:20','N',NULL,NULL,NULL,'20260400007','2026-04-25 11:43:20',NULL,NULL),('001','20260400007','9b22408a84234f5b992e57e0f088aa9e','WEB',NULL,'HToFTp3-3HjG3fSM8CyQSfQb2_2fL2-GHHMmbjTuFTE','2026-04-20 22:41:02','2026-04-27 22:41:02','Y','2026-04-21 22:44:17',NULL,NULL,'20260400007','2026-04-20 22:41:02','20260400007','2026-04-21 22:44:17'),('001','20260400007','a31a90efa93f4ad48627acbb534ae814','WEB',NULL,'mS8HceRvedRy5NztnGN4QzPFOXusH8EYMXaKkqFTl-8','2026-04-19 14:15:09','2026-04-26 14:15:09','Y','2026-04-20 22:41:02',NULL,NULL,'20260400007','2026-04-19 14:15:09','20260400007','2026-04-20 22:41:02'),('001','20260400007','a36c26f2f4fc42a0aae5f8fe38932275','WEB',NULL,'xUFQ7APbnDxPwz316IWV-OHVb4lZp7M9XyoVxWbW8nc','2026-04-16 21:44:12','2026-04-23 21:44:12','Y','2026-04-18 14:20:23',NULL,NULL,'20260400007','2026-04-16 21:44:12','20260400007','2026-04-18 14:20:23'),('001','20260400007','a631a808c9fe4accaa5ec8da8c249379','WEB',NULL,'BJ6ydy8BiZMli0Xv946PXLNR5bXrXshPazcBu4GCuB4','2026-04-21 22:44:17','2026-04-28 22:44:17','Y','2026-04-21 23:19:50',NULL,NULL,'20260400007','2026-04-21 22:44:17','20260400007','2026-04-21 23:19:50'),('001','20260400007','cb7c6db695a9417cb69a1cc84032e05a','WEB',NULL,'FUJ6zAk0tmM8kENlzgHSfWOV5Kbvkt69x5c9aOVbYyM','2026-04-19 12:39:35','2026-04-26 12:39:35','Y','2026-04-19 13:35:15',NULL,NULL,'20260400007','2026-04-19 12:39:35','20260400007','2026-04-19 13:35:15'),('001','20260400007','dfca69d503c645bc93aa27da9f604274','WEB',NULL,'qBRzR9Lh_Efxjox24Ssuth-ZD083f4X15p7PbMmHqT8','2026-04-18 14:20:23','2026-04-25 14:20:23','Y','2026-04-19 12:39:35',NULL,NULL,'20260400007','2026-04-18 14:20:23','20260400007','2026-04-19 12:39:35'),('001','20260400008','9bfb20708f264c1db7b9ba21ce47afb7','WEB',NULL,'O7PhLh35hX1xsGWOiDWe0T0QPf9ns4C29Cflde--ivk','2026-04-18 20:25:48','2026-04-25 20:25:48','Y','2026-04-18 20:25:50',NULL,NULL,'20260400008','2026-04-18 20:25:48','20260400008','2026-04-18 20:25:50'),('001','20260400009','14c87cb1dd434edfadad3fd5476cc84c','WEB',NULL,'-tO7gXpsEX9GPXnQ3_HnhTnMc5tPEzD8sB-QiJdFyao','2026-04-28 19:55:30','2026-05-05 19:55:30','Y','2026-04-28 19:55:31',NULL,NULL,'20260400009','2026-04-28 19:55:30','20260400009','2026-04-28 19:55:31'),('001','20260400009','632ccbbf8f4e4dfb90f3bcf79cb2b460','WEB',NULL,'2dxrRc5-jL_zlN3WuzvaJZBJi9DizcDCWVlrBemD3Gk','2026-04-28 19:54:21','2026-05-05 19:54:21','Y','2026-04-28 19:55:25',NULL,NULL,'20260400009','2026-04-28 19:54:21','20260400009','2026-04-28 19:55:25'),('001','20260400010','00a6eb1623fb4307a88c0f6e88f9f161','WEB',NULL,'BVvdEyGYrtAAskjW_1YgXWI5IDTXSCyiazWyIyTLSIA','2026-05-20 20:41:09','2026-05-22 20:41:09','Y','2026-05-20 21:02:44',NULL,NULL,'20260400010','2026-05-20 20:41:09','20260400010','2026-05-20 21:02:44'),('001','20260400010','02a87ee395e843e98d4f00386d0cb6fe','WEB',NULL,'Yck0c9ApiXJp3QQYC-BJPciFyYPnUKNSYpFCMSZEJ6w','2026-05-25 21:12:34','2026-05-27 21:12:34','Y','2026-05-25 21:22:27',NULL,NULL,'20260400010','2026-05-25 21:12:34','20260400010','2026-05-25 21:22:27'),('001','20260400010','0341614ab6704bfd99a7bee4413a9a25','WEB',NULL,'D3lDDRPD-P3zqAS4HoFzN4GjIJvjFp7AjoFeg0A0uAY','2026-05-21 16:15:16','2026-05-23 16:15:16','Y','2026-05-21 17:19:30',NULL,NULL,'20260400010','2026-05-21 16:15:16','20260400010','2026-05-21 17:19:30'),('001','20260400010','061644646dc14d1a89b9dc40a10e4f76','WEB',NULL,'zZqQM0TahCoYqQd99O9OwHbSfPLPoWqiSDUalgplXog','2026-06-03 21:08:32','2026-06-05 21:08:32','Y','2026-06-03 22:33:55',NULL,NULL,'20260400010','2026-06-03 21:08:32','20260400010','2026-06-03 22:33:55'),('001','20260400010','0a685e0ce2cd425d89eefe90f2b10721','APP',NULL,'Zns9EMyPPLYTaCdVGyAiwqyfPgIWcUcUghpe20iGmrs','2026-05-28 20:38:22','2026-05-30 20:38:22','Y','2026-05-28 20:38:25',NULL,NULL,'20260400010','2026-05-28 20:38:22','20260400010','2026-05-28 20:38:25'),('001','20260400010','0a938e70328d4deeb37f9cac662b75b9','WEB',NULL,'fcidGV5qGin6VRqltDGsGygFIx-l5kxlL-Q6sTksazk','2026-05-17 17:44:07','2026-05-19 16:42:26','Y','2026-05-17 19:44:28',NULL,NULL,'20260400010','2026-05-17 17:44:07','20260400010','2026-05-17 19:44:28'),('001','20260400010','0b444c33f4694311a4ea2743a2604a4c','WEB',NULL,'cYaP36KdI_tkDdFYSw5j7MwVPDeT-j-ZqoMcBCRIENk','2026-06-06 21:30:59','2026-06-08 21:30:59','Y','2026-06-06 22:32:47',NULL,NULL,'20260400010','2026-06-06 21:30:59','20260400010','2026-06-06 22:32:47'),('001','20260400010','0f8ba5cc9ae547659dbe0b0f451c9735','WEB',NULL,'KAaL5Gcc4O9SZNj9Tf2tjOdl8DElXuFf_t4CppdR5jk','2026-06-03 13:02:43','2026-06-05 13:02:43','Y','2026-06-03 13:50:55',NULL,NULL,'20260400010','2026-06-03 13:02:43','20260400010','2026-06-03 13:50:55'),('001','20260400010','0fd504adac984178949f7931da43c257','WEB',NULL,'FPjY6Uz9z5XG1zNhLHkRKyqYmbQ_iiM7yTu9PPlUHRE','2026-05-21 20:26:51','2026-05-23 20:26:51','Y','2026-05-21 21:22:34',NULL,NULL,'20260400010','2026-05-21 20:26:51','20260400010','2026-05-21 21:22:34'),('001','20260400010','107a70d03ca34875bfab2152e0c55039','WEB',NULL,'1lnTWj72leyRZ9s8GPONN6YKpkpvUYSxKJiPlyjfAHo','2026-05-16 21:55:01','2026-05-18 20:51:32','Y','2026-05-16 23:04:26',NULL,NULL,'20260400010','2026-05-16 21:55:01','20260400010','2026-05-16 23:04:26'),('001','20260400010','12d25ea1c7ff4e19abe988f9cd9306a5','WEB',NULL,'CjmcnIleynxVvba7Allok_xiUM_Qj72vaLPLXJJad1M','2026-05-15 19:32:16','2026-05-22 19:32:16','Y','2026-05-16 18:06:12',NULL,NULL,'20260400010','2026-05-15 19:32:16','20260400010','2026-05-16 18:06:12'),('001','20260400010','13b4f8597844483c962f4ddc09029d6a','WEB',NULL,'avtU3hiceYCKGoj2EK_o35doJb_1Bo5Ta9JpJbrPUwM','2026-06-01 19:57:32','2026-06-03 19:57:32','Y','2026-06-01 21:29:36',NULL,NULL,'20260400010','2026-06-01 19:57:32','20260400010','2026-06-01 21:29:36'),('001','20260400010','13db4807e23943c8b5da96f3c95b5f0c','APP',NULL,'mhvgvaG93nqMpJeWrGNBHDSz21hdYIrUMytG_kIhPnM','2026-05-29 20:27:35','2026-05-31 20:27:35','Y','2026-05-29 20:32:37',NULL,NULL,'20260400010','2026-05-29 20:27:35','20260400010','2026-05-29 20:32:37'),('001','20260400010','1536eab3378f486b972d23c3ccc5ba5a','WEB',NULL,'p9dkHqXroZ2LnkOdNQBb9mW3QGh0wloZoNt9-TCABJM','2026-05-16 18:06:12','2026-05-18 18:06:12','Y','2026-05-16 20:51:11',NULL,NULL,'20260400010','2026-05-16 18:06:12','20260400010','2026-05-16 20:51:11'),('001','20260400010','1af9e41ac917413ab73409b89b7ab706','WEB',NULL,'t-_D_cqNVhyeIsOx8ugMghI2CNZkB8IUTIf_g4E5Igw','2026-05-17 22:18:13','2026-05-19 21:12:40','Y','2026-05-18 19:04:07',NULL,NULL,'20260400010','2026-05-17 22:18:13','20260400010','2026-05-18 19:04:07'),('001','20260400010','1d8d1e6f9a60482b85c0d72ec4dd980e','WEB',NULL,'CFxJ4k0oW2g3i2MzdZhpl3ewjhucnYWY7YREiGu8Wb8','2026-05-25 21:22:27','2026-05-27 21:22:27','Y','2026-05-25 22:22:28',NULL,NULL,'20260400010','2026-05-25 21:22:27','20260400010','2026-05-25 22:22:28'),('001','20260400010','1fc2875470e84b9c8007844f7c97c383','WEB',NULL,'Ifkl-eXC3HauaKFHzFH1ZUzo0OMT6baYspJEJ3luXO0','2026-05-23 15:11:48','2026-05-25 15:11:48','Y','2026-05-23 15:49:33',NULL,NULL,'20260400010','2026-05-23 15:11:48','20260400010','2026-05-23 15:49:33'),('001','20260400010','20266c344562401bb638d8d4a38613c6','WEB',NULL,'QhFw3rDKyz8JU6Kjex8M9fV1zJwo_z3OjERmO_Gjgio','2026-05-14 20:53:16','2026-05-21 20:53:16','Y','2026-05-15 19:29:42',NULL,NULL,'20260400010','2026-05-14 20:53:16','20260400010','2026-05-15 19:29:42'),('001','20260400010','21c5bc9ef1b04a6b9e359c5176b9d325','WEB',NULL,'9f8o-z_XyvralcTajvbLs2l5dcQEMYy3QnhUZ2AH5is','2026-05-25 15:36:41','2026-05-27 09:07:43','Y','2026-05-25 18:30:17',NULL,NULL,'20260400010','2026-05-25 15:36:41','20260400010','2026-05-25 18:30:17'),('001','20260400010','2343715e0c69493b833b616b6daabca6','WEB',NULL,'TtasfEE8sQ59iPxKoxOU2thC3ciVUiFsJtYn1vFApFI','2026-05-31 13:34:51','2026-06-02 13:34:51','Y','2026-05-31 13:38:39',NULL,NULL,'20260400010','2026-05-31 13:34:51','20260400010','2026-05-31 13:38:39'),('001','20260400010','25581f6a66c145cb84cf63859f28d0f7','WEB',NULL,'vKBXU7rF1fESyJ16L4DfJmXWdetPazYjYxs7M7yVwe8','2026-05-01 08:17:58','2026-05-08 08:17:58','Y','2026-05-02 12:20:56',NULL,NULL,'20260400010','2026-05-01 08:17:58','20260400010','2026-05-02 12:20:56'),('001','20260400010','25afd3516620422fb51a310ad14bfa05','WEB',NULL,'ICuTUIzOWb9zpXk1FOUuamuTKgaQ-3HQlYRkA5rqLgU','2026-06-03 22:33:55','2026-06-05 22:33:55','Y','2026-06-04 18:45:02',NULL,NULL,'20260400010','2026-06-03 22:33:55','20260400010','2026-06-04 18:45:02'),('001','20260400010','26090a7a9741405e82ab7194f7ddb0c9','WEB',NULL,'bFHxyh8SfDBSwlAv3QtiQEfP-Yna_qq3n8NRTqcUrMo','2026-04-28 20:39:31','2026-05-05 20:39:31','Y','2026-04-28 20:40:06',NULL,NULL,'20260400010','2026-04-28 20:39:31','20260400010','2026-04-28 20:40:06'),('001','20260400010','267ff56f2ad24afba46449f8c1b9c1d8','WEB',NULL,'fk0XptMVPTjASlAhhThzJpxJE_T5KfAviiGKgvHx7vo','2026-05-25 18:30:17','2026-05-27 09:07:43','Y','2026-05-25 19:33:03',NULL,NULL,'20260400010','2026-05-25 18:30:17','20260400010','2026-05-25 19:33:03'),('001','20260400010','2873096ded1e4d0f92d832f165e3fb82','WEB',NULL,'vc2oUwbSXCWbv7H4Y0l_OtC6BHM0Ab4dmFRSw0IRfZ4','2026-06-07 22:06:34','2026-06-09 22:06:34','N',NULL,NULL,NULL,'20260400010','2026-06-07 22:06:34',NULL,NULL),('001','20260400010','287a6705f677417dadca9c8b0df0d31e','WEB',NULL,'SALtF97NkDaazMHlqQQtZI5uO5VDo5IohtqzsE-EIHA','2026-05-24 21:29:46','2026-05-26 16:31:55','Y','2026-05-24 22:31:23',NULL,NULL,'20260400010','2026-05-24 21:29:46','20260400010','2026-05-24 22:31:23'),('001','20260400010','28d08e2f818c4ace9f01f90023614112','WEB',NULL,'hHCnbwf1igbnLcuSXlqTjLpGoM9oZf8x7R3HufA4wzc','2026-05-29 18:35:44','2026-05-31 18:35:44','Y','2026-05-29 20:14:03',NULL,NULL,'20260400010','2026-05-29 18:35:44','20260400010','2026-05-29 20:14:03'),('001','20260400010','29e62b55087f4591bce1a087baef01dc','WEB',NULL,'-SB_xuXgWKrVdjpaPogqHBdouGaaiG8xKm7O-lkCDc4','2026-06-06 18:03:42','2026-06-08 18:03:42','Y','2026-06-06 21:30:59',NULL,NULL,'20260400010','2026-06-06 18:03:42','20260400010','2026-06-06 21:30:59'),('001','20260400010','2ae56b5c60e54f6cbc3b2fbbdca6cb66','WEB',NULL,'t0TGtxLKVc85ZNLtaUUNK4Pd2nFAetXFfMWvsQ0wK8o','2026-04-29 20:20:05','2026-05-06 20:20:05','Y','2026-04-29 20:57:35',NULL,NULL,'20260400010','2026-04-29 20:20:05','20260400010','2026-04-29 20:57:35'),('001','20260400010','2b39366b633d43d0ad614976f8d05e5a','WEB',NULL,'wAIlDu96K7jlxyeyAxEm_1x0c2u-OX4SuQmKQkS3SKM','2026-05-21 20:20:53','2026-05-23 17:32:32','Y','2026-05-21 20:25:45',NULL,NULL,'20260400010','2026-05-21 20:20:53','20260400010','2026-05-21 20:25:45'),('001','20260400010','2c3082623792402da95bed6b1016d9c0','WEB',NULL,'YzOcqvIE505K67JDaRjng9p-Q5UPIIdDoxHPLNsgk0U','2026-05-25 09:07:43','2026-05-27 09:07:43','Y','2026-05-25 10:09:27',NULL,NULL,'20260400010','2026-05-25 09:07:43','20260400010','2026-05-25 10:09:27'),('001','20260400010','2cfa50face614a4f9a665c30b17ee241','WEB',NULL,'ZUVE0AQU9rDMtgQpAqkWqEtx0IopinaLDQTpUXFWeMc','2026-05-28 18:29:12','2026-05-30 18:29:12','Y','2026-05-28 18:46:27',NULL,NULL,'20260400010','2026-05-28 18:29:12','20260400010','2026-05-28 18:46:27'),('001','20260400010','2ff789f2c6854e199fcc8610c9030759','WEB',NULL,'hc8PMiOE-OgdMtKd76zl0TvuEzwBQUYSdHBWOZQue4s','2026-05-02 12:20:56','2026-05-09 12:20:56','Y','2026-05-06 20:25:58',NULL,NULL,'20260400010','2026-05-02 12:20:56','20260400010','2026-05-06 20:25:58'),('001','20260400010','306a21e4805642d1bc78d91943303b89','WEB',NULL,'bYdZoH8pL0ngG4p7RSb2tP28IaGCbou_VSWq2HPFrrY','2026-05-17 21:12:40','2026-05-19 21:12:40','Y','2026-05-17 22:18:13',NULL,NULL,'20260400010','2026-05-17 21:12:40','20260400010','2026-05-17 22:18:13'),('001','20260400010','30d47bc65f8f4853ad92112dfaf271d4','WEB',NULL,'lF9VZ-FdJhfSHyl1thJyvMyb6zZfY9PhqCXf7Q-Ycrw','2026-05-21 16:15:07','2026-05-22 23:50:48','Y','2026-05-21 16:15:16',NULL,NULL,'20260400010','2026-05-21 16:15:07','20260400010','2026-05-21 16:15:16'),('001','20260400010','336aa4e5efe940238c222de00ddeb20a','WEB',NULL,'Y5pnkrovpTB7F3nJFVbu8VmaDGtVB0DmdzU8eLYosMc','2026-06-02 21:39:58','2026-06-04 21:39:58','Y','2026-06-02 23:11:16',NULL,NULL,'20260400010','2026-06-02 21:39:58','20260400010','2026-06-02 23:11:16'),('001','20260400010','33ea876d953c4b3b88f1b2cce24166dc','WEB',NULL,'hP_SZIzjTOXYHOmPY0wFNgsWW3BE8OGSrCQXul-XIA8','2026-05-23 15:49:33','2026-05-25 15:49:33','Y','2026-05-23 17:05:55',NULL,NULL,'20260400010','2026-05-23 15:49:33','20260400010','2026-05-23 17:05:55'),('001','20260400010','37033c9cae3b434ba8632a1c52bc11ad','WEB',NULL,'PXVKyqBnYbsHIHhYtPQh3fnXAEExUZRdtsjb5D0ce-0','2026-05-28 18:29:09','2026-05-29 21:58:13','Y','2026-05-28 18:29:12',NULL,NULL,'20260400010','2026-05-28 18:29:09','20260400010','2026-05-28 18:29:12'),('001','20260400010','3760d18f62bb460ca03bb9d7a1e33135','WEB',NULL,'xIczbLh7AqfE87Dop-aMNvOT2K_XvO4tDCTBAqIhfx8','2026-05-18 20:28:26','2026-05-20 19:06:03','Y','2026-05-18 21:21:23',NULL,NULL,'20260400010','2026-05-18 20:28:26','20260400010','2026-05-18 21:21:23'),('001','20260400010','3938b86b00b844a7ac1d4f7162f08bf1','WEB',NULL,'ersl72OOMc_IKqKo2vd4IGoNMrjZkOeADl-ItmPjEOc','2026-05-17 09:50:29','2026-05-18 20:51:32','Y','2026-05-17 09:50:33',NULL,NULL,'20260400010','2026-05-17 09:50:29','20260400010','2026-05-17 09:50:33'),('001','20260400010','3d04aac4109442708ab0c58bacdd2357','WEB',NULL,'pa63_cvXgGjnc4UF0iBeWxKm7jucxURYcIVnUfPgzyM','2026-06-01 21:29:36','2026-06-03 21:29:36','Y','2026-06-02 20:14:12',NULL,NULL,'20260400010','2026-06-01 21:29:36','20260400010','2026-06-02 20:14:12'),('001','20260400010','3eeafbdf39fe4850a41c3b15a0ccfc9c','WEB',NULL,'2sedhVHJXnVhGQ6hkMj7o1_7ij4WIFqghaPunVc7Ox8','2026-06-03 14:07:16','2026-06-05 14:07:16','Y','2026-06-03 21:08:02',NULL,NULL,'20260400010','2026-06-03 14:07:16','20260400010','2026-06-03 21:08:02'),('001','20260400010','3f0cc36ce1a14ee2bc405b4ec3579aad','WEB',NULL,'ky7x0kJQfW9HCm6JXKjoJS9U69F0YdyMpSlN1xPdp4g','2026-05-22 18:51:51','2026-05-23 21:22:34','Y','2026-05-22 18:51:55',NULL,NULL,'20260400010','2026-05-22 18:51:51','20260400010','2026-05-22 18:51:55'),('001','20260400010','3fc71740ba5345c49f35581707592caf','WEB',NULL,'yvThjC1AtFc3ZSkWHyyg01jGXatHe2wHN6LMFbwocWk','2026-05-06 20:25:58','2026-05-13 20:25:58','Y','2026-05-06 22:57:30',NULL,NULL,'20260400010','2026-05-06 20:25:58','20260400010','2026-05-06 22:57:30'),('001','20260400010','40e95eff963e4abb8aad58c7cfe1b5cb','WEB',NULL,'W1QulEJkHnbiKJO7NGUnb0bBodpCBymTNSnnJaXGMP0','2026-05-20 21:02:44','2026-05-22 21:02:44','Y','2026-05-20 22:00:12',NULL,NULL,'20260400010','2026-05-20 21:02:44','20260400010','2026-05-20 22:00:12'),('001','20260400010','41c99bd20df141098af7b5af2ed331a8','WEB',NULL,'0763MfvhF8G0UjwvvXOYAm8a3xranMRKnqLFBo0NzIM','2026-06-04 20:07:03','2026-06-06 20:07:03','Y','2026-06-04 20:38:00',NULL,NULL,'20260400010','2026-06-04 20:07:03','20260400010','2026-06-04 20:38:00'),('001','20260400010','427b237689ff49ba9a8799e611ddecc0','WEB',NULL,'izXfn8XYSb-Qw_BeVSRhCVpS_r4tm0iTFWvTu5MENMM','2026-05-06 22:57:30','2026-05-13 22:57:30','Y','2026-05-07 22:00:28',NULL,NULL,'20260400010','2026-05-06 22:57:30','20260400010','2026-05-07 22:00:28'),('001','20260400010','4509ed59671a44b79f045cb6d1350c86','WEB',NULL,'7J1ITN5vJE2yACGWq7yRsyaRXCK8_FbzaUyToNbb8Vk','2026-05-31 21:58:43','2026-06-02 21:58:43','Y','2026-06-01 19:57:32',NULL,NULL,'20260400010','2026-05-31 21:58:43','20260400010','2026-06-01 19:57:32'),('001','20260400010','459bf3e283ef4822824c815ba4809093','WEB',NULL,'c9NQjJ51uZIRRp4E2KbY-mzKNrK_GbQTZ7ZCvzzt3yM','2026-05-12 20:05:42','2026-05-19 20:05:42','Y','2026-05-12 21:17:51',NULL,NULL,'20260400010','2026-05-12 20:05:42','20260400010','2026-05-12 21:17:51'),('001','20260400010','478ab6799d2240ceb88f176de422f9f4','WEB',NULL,'aN_OeqfTrjdWP9Ph4PKz8j7yvaVX0C3bSgTD7EHx_k8','2026-05-31 19:40:02','2026-06-02 19:40:02','Y','2026-05-31 21:58:43',NULL,NULL,'20260400010','2026-05-31 19:40:02','20260400010','2026-05-31 21:58:43'),('001','20260400010','47dfbb32b3c543819c7f732bbd44879d','WEB',NULL,'Gjej8uEAFILtFNiSv142pF527rPYMeatqvFuCGPTDSc','2026-05-24 16:31:55','2026-05-26 16:31:55','Y','2026-05-24 17:35:11',NULL,NULL,'20260400010','2026-05-24 16:31:55','20260400010','2026-05-24 17:35:11'),('001','20260400010','47feb413d6d149439b719e46ea6e2389','WEB',NULL,'zGdb92sgQVWqp9JRCIwR8yg3kIBHKminsBn_YFe6lFo','2026-05-21 17:19:30','2026-05-23 16:15:16','Y','2026-05-21 17:32:32',NULL,NULL,'20260400010','2026-05-21 17:19:30','20260400010','2026-05-21 17:32:32'),('001','20260400010','4d17187abf5344af8d9532036101f03c','WEB',NULL,'Ix15WZtl_q7N3-xcXyRnYAtGw5Sas8OzKpCQvypzm98','2026-05-25 14:34:42','2026-05-27 09:07:43','Y','2026-05-25 15:36:41',NULL,NULL,'20260400010','2026-05-25 14:34:42','20260400010','2026-05-25 15:36:41'),('001','20260400010','4db2592eb1a441698808d19354d1fa22','WEB',NULL,'K1KdrxBFxBjC7ZP3UEQ4WgzUXp3_hJxeFYYy-KTuMbs','2026-05-12 22:32:42','2026-05-19 22:32:42','Y','2026-05-13 18:50:31',NULL,NULL,'20260400010','2026-05-12 22:32:42','20260400010','2026-05-13 18:50:31'),('001','20260400010','4f9eb04355924c07bd59b79f850b807c','WEB',NULL,'8V32LxXPG6cdtTdGiye5AaW0OM1IDdY8DZEli7sOivY','2026-06-06 01:21:17','2026-06-08 01:21:17','Y','2026-06-06 01:21:43',NULL,NULL,'20260400010','2026-06-06 01:21:17','20260400010','2026-06-06 01:21:43'),('001','20260400010','50294b507ee84c71b618203ebd137d08','WEB',NULL,'2ZkjwBDhtZYo_yvhJIOJVgEK6_hreY5jHf_hixHacJs','2026-05-07 22:13:54','2026-05-14 22:13:54','Y','2026-05-09 07:18:51',NULL,NULL,'20260400010','2026-05-07 22:13:54','20260400010','2026-05-09 07:18:51'),('001','20260400010','50dfc61ce664499c8a089839bec82ead','WEB',NULL,'u5wvWXCazBNVknfEli3KPnXevbDhojIPMVXSfGpJdXU','2026-05-23 14:30:45','2026-05-25 14:30:45','Y','2026-05-23 15:11:48',NULL,NULL,'20260400010','2026-05-23 14:30:45','20260400010','2026-05-23 15:11:48'),('001','20260400010','514af2e4cdc1439db8787198f18342df','WEB',NULL,'hY-INoH13F_V0VqSyY12AtLTDGirxhnhBOpuFYue4hM','2026-05-25 13:14:48','2026-05-27 09:07:43','Y','2026-05-25 14:34:42',NULL,NULL,'20260400010','2026-05-25 13:14:48','20260400010','2026-05-25 14:34:42'),('001','20260400010','51ee75e627264fd2bd37c182422464f1','WEB',NULL,'vnXVlJYEpFS2ERg0v0HnmE-Q3r1bUt0LpURT2oah4K8','2026-05-28 18:46:27','2026-05-30 18:46:27','Y','2026-05-28 19:48:25',NULL,NULL,'20260400010','2026-05-28 18:46:27','20260400010','2026-05-28 19:48:25'),('001','20260400010','53472339d9fe4204b841026d8481c83d','WEB',NULL,'Mnv2ddS_YvdCb7yHQC1Yx5OXRJp-qaGu3kirhtID-K4','2026-06-06 01:21:43','2026-06-08 01:21:43','Y','2026-06-06 01:21:52',NULL,NULL,'20260400010','2026-06-06 01:21:43','20260400010','2026-06-06 01:21:52'),('001','20260400010','547ff000b9b74a80b3cc41d20465550f','WEB',NULL,'PmauGrgf0fUUhX4wwrpCpcAOlJ7X2PhdHFxb-LntcbA','2026-05-19 22:15:49','2026-05-21 18:34:26','Y','2026-05-20 18:53:01',NULL,NULL,'20260400010','2026-05-19 22:15:49','20260400010','2026-05-20 18:53:01'),('001','20260400010','54facf5f07884c22b94347f9c8e727c6','WEB',NULL,'yaFNuqr2x-C9mx8addpcT-PItJPNFvefXmpQ1cb0CL4','2026-05-17 16:37:41','2026-05-19 16:37:41','Y','2026-05-17 16:41:53',NULL,NULL,'20260400010','2026-05-17 16:37:41','20260400010','2026-05-17 16:41:53'),('001','20260400010','55024b6c9fe648c1a3576ee2035b82ea','WEB',NULL,'26erUKEBR0uYLB5edzDAQ2VAhQxTkmJyw0UzuIiBlR8','2026-04-28 20:35:27','2026-05-05 20:35:27','Y','2026-04-28 20:37:26',NULL,NULL,'20260400010','2026-04-28 20:35:27','20260400010','2026-04-28 20:37:26'),('001','20260400010','552f56856fe34759b4a83e8f72861633','WEB',NULL,'xQkayNGs2p5Sfoi_JfC_lxsysKWryCeMCBSvQjo1L48','2026-05-10 13:45:55','2026-05-17 13:45:55','Y','2026-05-11 18:40:57',NULL,NULL,'20260400010','2026-05-10 13:45:55','20260400010','2026-05-11 18:40:57'),('001','20260400010','5542b78e77f947fdbde0deeb0bf6fa01','WEB',NULL,'YwKnoJIG6MRG03r74ninGcBdeIKpiOQXY9RuhQMOiyw','2026-05-23 18:32:03','2026-05-25 15:49:33','Y','2026-05-23 18:35:40',NULL,NULL,'20260400010','2026-05-23 18:32:03','20260400010','2026-05-23 18:35:40'),('001','20260400010','5766d4ca0e9345b18fecf9b10c5cba09','WEB',NULL,'2540_C0P9TC0BH_a0FoqeyWqOIseDFrbAII9lJ_HN-s','2026-05-27 20:01:34','2026-05-29 18:51:26','Y','2026-05-27 21:55:04',NULL,NULL,'20260400010','2026-05-27 20:01:34','20260400010','2026-05-27 21:55:04'),('001','20260400010','57b8262ee415492e96cee9822c6d8a9b','WEB',NULL,'VPhl40pNO0mD9lw4-_WJc6aycdIl1ygw-s1n6nMgS8I','2026-05-18 19:06:03','2026-05-20 19:06:03','Y','2026-05-18 20:28:26',NULL,NULL,'20260400010','2026-05-18 19:06:03','20260400010','2026-05-18 20:28:26'),('001','20260400010','59fa5ad4521049a59b3869804109dc16','WEB',NULL,'Kn-g9LvpDpHXUbA9ZnSI9jeD9i0L7wl55pELVHg2Q24','2026-04-29 21:02:12','2026-05-06 21:02:12','Y','2026-04-30 21:17:02',NULL,NULL,'20260400010','2026-04-29 21:02:12','20260400010','2026-04-30 21:17:02'),('001','20260400010','5a6129602db94e938765037eb9cdb6d0','WEB',NULL,'L4c2cTxNolIhm1DSA9zoZPAzICyktG58eIgtjhKSz-g','2026-06-06 17:19:20','2026-06-08 17:19:20','Y','2026-06-06 18:03:42',NULL,NULL,'20260400010','2026-06-06 17:19:20','20260400010','2026-06-06 18:03:42'),('001','20260400010','5ba18dbebbc449a59377bbf4c9b96efc','WEB',NULL,'N353FJYmXd3HbXXhbhw0GtqPCqf0RxKOEbofiul9YhA','2026-05-18 19:04:07','2026-05-19 21:12:40','Y','2026-05-18 19:06:03',NULL,NULL,'20260400010','2026-05-18 19:04:07','20260400010','2026-05-18 19:06:03'),('001','20260400010','5f70acee4e854837a630707015a15fab','WEB',NULL,'dxWpuAsCsVR6wJRnkeiSeXhZvQkI0s3UwuXKWctbKWM','2026-06-07 21:04:47','2026-06-09 21:04:47','Y','2026-06-07 22:06:34',NULL,NULL,'20260400010','2026-06-07 21:04:47','20260400010','2026-06-07 22:06:34'),('001','20260400010','600b55adbda042e18eac237e6bfe2a9b','WEB',NULL,'VG6AX62-yJzWc0OXKVxF0pIkvwF5DoZLazv8fsMf444','2026-05-11 18:40:57','2026-05-18 18:40:57','Y','2026-05-12 20:05:42',NULL,NULL,'20260400010','2026-05-11 18:40:57','20260400010','2026-05-12 20:05:42'),('001','20260400010','63442459a5a24e17ba3b929d57fc8799','WEB',NULL,'LahKIEOxxtBccPYpDE_-yTPguqPJBeJEuORh7nc94ug','2026-05-21 22:30:27','2026-05-23 21:22:34','Y','2026-05-22 18:51:51',NULL,NULL,'20260400010','2026-05-21 22:30:27','20260400010','2026-05-22 18:51:51'),('001','20260400010','6395f8a3a8934710820d4816cdb8647b','WEB',NULL,'BgQ6v_y0ZepqvAKv7sBYx50y5p2kA4u3rckpBJnSOFM','2026-04-30 22:05:52','2026-05-07 22:05:52','Y','2026-05-01 08:17:58',NULL,NULL,'20260400010','2026-04-30 22:05:52','20260400010','2026-05-01 08:17:58'),('001','20260400010','64150ec7eb0742888c6c4220a2545aea','WEB',NULL,'Q65TDEbm5jIlWfGBmwvgm3ubor_G9htF9S7bc0evZsg','2026-04-28 20:40:33','2026-05-05 20:40:33','Y','2026-04-28 20:43:03',NULL,NULL,'20260400010','2026-04-28 20:40:33','20260400010','2026-04-28 20:43:03'),('001','20260400010','641894ef883f4baaa7cfbf634db44e6f','APP',NULL,'QaS-TF4RSj3ygkoLzD73xyc0pgHBUWDuqUvtnGl19g4','2026-05-29 13:59:22','2026-05-31 13:59:22','Y','2026-05-29 13:59:25',NULL,NULL,'20260400010','2026-05-29 13:59:22','20260400010','2026-05-29 13:59:25'),('001','20260400010','6528839acabd4dfa991cd9c7ab3220c3','WEB',NULL,'UVo1f1TwWI8vEA1_BhAbkHCZivWtp7RPe25ANgx9ybw','2026-05-19 18:34:23','2026-05-20 21:21:23','Y','2026-05-19 18:34:26',NULL,NULL,'20260400010','2026-05-19 18:34:23','20260400010','2026-05-19 18:34:26'),('001','20260400010','6b8d892db8684ca2aebe2bc6f8c44281','APP',NULL,'JcKtVlnN_3XEWAtJx1hRhmePUzq_UMwnbpO4Dii9itk','2026-06-02 20:30:57','2026-06-04 20:30:57','Y','2026-06-03 14:01:20',NULL,NULL,'20260400010','2026-06-02 20:30:57','20260400010','2026-06-03 14:01:20'),('001','20260400010','6c9d69e2931c40d49f1da62647fe7859','WEB',NULL,'DufDyZkE7UJUmYuG6eqB1ht_yNMisQtuWq6TCDcyTgk','2026-05-24 10:01:10','2026-05-26 10:01:10','Y','2026-05-24 16:31:51',NULL,NULL,'20260400010','2026-05-24 10:01:10','20260400010','2026-05-24 16:31:51'),('001','20260400010','6d253861df364549a288bbf09706db94','WEB',NULL,'uCskwjJI3HOhxYtJymiY5WV9Z1Kc_vtY4DwpvsZ-biM','2026-05-18 21:21:23','2026-05-20 21:21:23','Y','2026-05-18 22:18:13',NULL,NULL,'20260400010','2026-05-18 21:21:23','20260400010','2026-05-18 22:18:13'),('001','20260400010','6d50c6fe227d4abdbf861a0897dcaca9','WEB',NULL,'zUGJcFGZD13IDhM-KD1AHQ6vhvb8KHERMrvTUohbmPY','2026-06-06 01:22:00','2026-06-08 01:22:00','Y','2026-06-06 14:51:23',NULL,NULL,'20260400010','2026-06-06 01:22:00','20260400010','2026-06-06 14:51:23'),('001','20260400010','6df0cb5a4b2848ea9552b4cb65f8793f','WEB',NULL,'k3gvFwTpGKSnmGT37PDaD1FBjuyc3NJqAnl7FWBaChg','2026-05-23 18:35:40','2026-05-25 18:35:40','Y','2026-05-23 20:40:33',NULL,NULL,'20260400010','2026-05-23 18:35:40','20260400010','2026-05-23 20:40:33'),('001','20260400010','6eb343cf23964247961b11e69316ef4a','WEB',NULL,'QCpdruaiHEpY_m-69Ut7sO6y3EEAciI0vpstB4P198A','2026-05-24 17:35:11','2026-05-26 16:31:55','Y','2026-05-24 20:09:01',NULL,NULL,'20260400010','2026-05-24 17:35:11','20260400010','2026-05-24 20:09:01'),('001','20260400010','6fb18255989a473b85040165fff2dc49','WEB',NULL,'j0KZy0df5U0VGN3yLXhWKmRE4OBSG1sV0CMNOgsj1rs','2026-05-25 22:22:28','2026-05-27 21:22:27','Y','2026-05-26 18:37:47',NULL,NULL,'20260400010','2026-05-25 22:22:28','20260400010','2026-05-26 18:37:47'),('001','20260400010','70958834b1d3405aa7c0f130bfbfd0cc','WEB',NULL,'tbcN4yqWd0C4lKAbVG1WEHibxvOqNv7I-bGWgd74EU8','2026-05-27 18:51:26','2026-05-29 18:51:26','Y','2026-05-27 20:01:34',NULL,NULL,'20260400010','2026-05-27 18:51:26','20260400010','2026-05-27 20:01:34'),('001','20260400010','7310e734b38941999a6802c1a24f5770','APP',NULL,'Q30s7aajj9Hvw-k95JmSBqOeTqJpUp_QQIIUhx3LvvM','2026-05-29 14:40:46','2026-05-31 14:40:46','Y','2026-05-29 20:27:35',NULL,NULL,'20260400010','2026-05-29 14:40:46','20260400010','2026-05-29 20:27:35'),('001','20260400010','73d99852f79b41f8a06b55b232d63b22','WEB',NULL,'-0BMdCDtPHK9nzjEhfrQKKELfIHWBHu7v2gaWPTY8k0','2026-05-26 18:38:42','2026-05-28 18:38:42','Y','2026-05-26 19:40:02',NULL,NULL,'20260400010','2026-05-26 18:38:42','20260400010','2026-05-26 19:40:02'),('001','20260400010','7475673740d542269732c429f8bc1265','WEB',NULL,'OMzePtlMFFDGDHnkpZPgtCP5weKDqjEeIVD7IeIN3OQ','2026-05-24 22:31:23','2026-05-26 16:31:55','Y','2026-05-24 23:33:20',NULL,NULL,'20260400010','2026-05-24 22:31:23','20260400010','2026-05-24 23:33:20'),('001','20260400010','74dac72a93764ef4b258269fe1863d06','WEB',NULL,'LJv4caJI2tlIsn3Vk6UpR3otHeQaG7F5yL957PvOGwg','2026-04-28 20:43:09','2026-05-05 20:43:09','Y','2026-04-28 20:44:19',NULL,NULL,'20260400010','2026-04-28 20:43:09','20260400010','2026-04-28 20:44:19'),('001','20260400010','754a69597cc640f7aa08fa0bbe361e93','APP',NULL,'l1coqOFFaO8ZIqVQNlcBB9YMO0NUfmZ2UZw4qBgdkZM','2026-05-29 21:28:13','2026-05-31 21:28:13','Y','2026-05-29 21:28:57',NULL,NULL,'20260400010','2026-05-29 21:28:13','20260400010','2026-05-29 21:28:57'),('001','20260400010','7850f17ac0ae4119b9a899ae290f5c17','WEB',NULL,'64uybwyDaRVPPi967Dj9hlm2M3v0wkHUx454K9J8IIY','2026-05-26 19:40:02','2026-05-28 18:38:42','Y','2026-05-26 21:56:34',NULL,NULL,'20260400010','2026-05-26 19:40:02','20260400010','2026-05-26 21:56:34'),('001','20260400010','79fc3d4ba3ea4c91a455390ed7b437d0','WEB',NULL,'Qx6t4_OQioC8UUyHNCy9MWwp2w7pROFsZtmZP29svpo','2026-05-07 22:00:28','2026-05-14 22:00:28','Y','2026-05-07 22:13:54',NULL,NULL,'20260400010','2026-05-07 22:00:28','20260400010','2026-05-07 22:13:54'),('001','20260400010','7bf1939ca20441cd9336beea92cf0e68','WEB',NULL,'OKqh_7JHWa8VyKU2ipB2QGJDo8N5yGznEBmSialgeDc','2026-04-28 20:40:10','2026-05-05 20:40:10','Y','2026-04-28 20:40:29',NULL,NULL,'20260400010','2026-04-28 20:40:10','20260400010','2026-04-28 20:40:29'),('001','20260400010','7f190950e1964e1f8a9ccd923ea11d8a','WEB',NULL,'KHj0dlsIToDNS0S4JoOwabrdDkLrOydxyjkQWhUtr1s','2026-05-24 16:31:51','2026-05-26 10:01:10','Y','2026-05-24 16:31:55',NULL,NULL,'20260400010','2026-05-24 16:31:51','20260400010','2026-05-24 16:31:55'),('001','20260400010','7f2ea48faa6142e9ad4177f64deed439','WEB',NULL,'xC1-Umm-0tpiz_e9dQtcR6rpAf4pKBdf6j4cdRfxWPc','2026-05-23 21:06:59','2026-05-25 21:06:59','Y','2026-05-23 21:34:17',NULL,NULL,'20260400010','2026-05-23 21:06:59','20260400010','2026-05-23 21:34:17'),('001','20260400010','80ece538c6c8448faf868e13a8f96a61','WEB',NULL,'YkbYWNAYKZhvXRYqTn4JJw3pnuFBPF1fkU4czwxX0JY','2026-04-29 20:57:46','2026-05-06 20:57:46','Y','2026-04-29 20:58:42',NULL,NULL,'20260400010','2026-04-29 20:57:46','20260400010','2026-04-29 20:58:42'),('001','20260400010','83a4e73993b04fbf9ba5930e8de41d22','WEB',NULL,'gVoDrYFXqUkECl0Cb3kP787dhac7r6Ddcz6tg8Wg9p4','2026-05-17 19:44:28','2026-05-19 16:42:26','Y','2026-05-17 21:12:40',NULL,NULL,'20260400010','2026-05-17 19:44:28','20260400010','2026-05-17 21:12:40'),('001','20260400010','844fe01f3c304fb4ba766216b7c8bcd7','WEB',NULL,'MtiNQis1zZ7OhOeJSqPS5iqF_eP1W2dhK-TfssAHRmc','2026-05-24 08:08:50','2026-05-25 21:34:17','Y','2026-05-24 08:08:53',NULL,NULL,'20260400010','2026-05-24 08:08:50','20260400010','2026-05-24 08:08:53'),('001','20260400010','84c773460af0427b8146a7edad5bc9c6','WEB',NULL,'p5da8XAeP45fuurHNsmizgPvq8v7Oqcyw4BgJrweWi8','2026-05-16 20:51:11','2026-05-18 20:51:11','Y','2026-05-16 20:51:32',NULL,NULL,'20260400010','2026-05-16 20:51:11','20260400010','2026-05-16 20:51:32'),('001','20260400010','8675a3c7df0a4735add9e1a9b84ba4a3','WEB',NULL,'ZNyewSyf-4wiUiCLrwrYhMHzOMEtA5SksmiW3xC_9b8','2026-05-21 17:32:32','2026-05-23 17:32:32','Y','2026-05-21 19:09:44',NULL,NULL,'20260400010','2026-05-21 17:32:32','20260400010','2026-05-21 19:09:44'),('001','20260400010','88220ce9c9ca471e83e0cda7f6267eb4','WEB',NULL,'f7pRm09KXXOhyBK4mIHdlLHxwcellD79SwhVOVMujNM','2026-05-30 17:54:20','2026-06-01 17:54:20','Y','2026-05-31 13:34:51',NULL,NULL,'20260400010','2026-05-30 17:54:20','20260400010','2026-05-31 13:34:51'),('001','20260400010','896c1d3b15b8480b8e3de9e1f820a657','WEB',NULL,'NTYLAEytTqcUckhvH3T2pREACqG23avA7frwP4WXcMs','2026-05-29 21:41:26','2026-05-31 21:41:26','Y','2026-05-30 17:54:20',NULL,NULL,'20260400010','2026-05-29 21:41:26','20260400010','2026-05-30 17:54:20'),('001','20260400010','8a4123cc57fe47bca8d325bd8de7ff43','WEB',NULL,'YU-iUSkV8UH9LxQX5CWO_fbGwE3hTLwva68Ox7mO9hE','2026-05-27 21:55:04','2026-05-29 21:55:04','Y','2026-05-27 21:58:13',NULL,NULL,'20260400010','2026-05-27 21:55:04','20260400010','2026-05-27 21:58:13'),('001','20260400010','8a611b09264e43f398f9299999173f12','APP',NULL,'QIWY-pj9-eASwWntzaBMsiAQCwkpo5RrkyHV14LTgu0','2026-06-03 14:01:20','2026-06-05 14:01:20','N',NULL,NULL,NULL,'20260400010','2026-06-03 14:01:20',NULL,NULL),('001','20260400010','8c479aecd09f4d5b9c5cdeff588cc0b5','APP',NULL,'Pujl-cbUl5oizQdmJor7zxq0e6qmSJfPIHSQZqH6FN8','2026-05-25 22:24:43','2026-05-27 22:24:43','N',NULL,NULL,NULL,'20260400010','2026-05-25 22:24:43',NULL,NULL),('001','20260400010','8dbfcaed50cc43bfb468e2cde29dba5b','WEB',NULL,'TGHDoS-_IDvbDyO13q5ojHOlfSXXSRTUukU-rufrHBE','2026-05-24 23:33:20','2026-05-26 16:31:55','Y','2026-05-25 09:07:30',NULL,NULL,'20260400010','2026-05-24 23:33:20','20260400010','2026-05-25 09:07:30'),('001','20260400010','8f5c6bcb3b6749c89335254604a85cdb','WEB',NULL,'NY-1ShhzVq1-BubgCyh4uUxQk2ZdngUoO044jXNtv58','2026-05-17 16:42:26','2026-05-19 16:42:26','Y','2026-05-17 17:44:07',NULL,NULL,'20260400010','2026-05-17 16:42:26','20260400010','2026-05-17 17:44:07'),('001','20260400010','926a77a90c8040468aa70ca21c37dd3a','WEB',NULL,'d7jkdclHaztCtwoDhOIGsb-HKTXZo8k49Hb1MwTDao0','2026-05-19 18:34:26','2026-05-21 18:34:26','Y','2026-05-19 20:05:31',NULL,NULL,'20260400010','2026-05-19 18:34:26','20260400010','2026-05-19 20:05:31'),('001','20260400010','9372ae6227794ad1b0d8abc1da582970','WEB',NULL,'MqXH4Rt2Ko1MHoq3JF8ugdgrA0uG_10VS2w9R3Bwvj8','2026-05-17 13:59:01','2026-05-19 09:50:33','Y','2026-05-17 16:37:33',NULL,NULL,'20260400010','2026-05-17 13:59:01','20260400010','2026-05-17 16:37:33'),('001','20260400010','93abc75ee9b2477388941c70493bb345','WEB',NULL,'LHqL1dbau6q-TQLcPbDM8D2kqhwxU3hhp9X8SglgERI','2026-05-18 22:23:43','2026-05-20 21:21:23','Y','2026-05-19 18:34:23',NULL,NULL,'20260400010','2026-05-18 22:23:43','20260400010','2026-05-19 18:34:23'),('001','20260400010','956c59719d4442ffa3a8b38654a5bec5','WEB',NULL,'vqctMnl7HgdP4M30EKZAu6wuXs51ZCiyMHnA0ws5LLA','2026-06-05 22:21:55','2026-06-07 22:21:55','Y','2026-06-06 00:17:48',NULL,NULL,'20260400010','2026-06-05 22:21:55','20260400010','2026-06-06 00:17:48'),('001','20260400010','9737c3961b7643ddb70db50a022faccc','WEB',NULL,'aku3HVquzsjIlgIOvx2HtFtomjKyxxs1rwOUqhysIiw','2026-06-06 23:34:31','2026-06-08 23:34:31','Y','2026-06-06 23:36:46',NULL,NULL,'20260400010','2026-06-06 23:34:31','20260400010','2026-06-06 23:36:46'),('001','20260400010','983e9e51bb11418382c60e26e7a067e5','WEB',NULL,'m-Sl5k_ykG3QhVVFy3Fy0HG_wNKvR-GglB5YDAqi_84','2026-06-06 16:14:02','2026-06-08 16:14:02','Y','2026-06-06 17:19:20',NULL,NULL,'20260400010','2026-06-06 16:14:02','20260400010','2026-06-06 17:19:20'),('001','20260400010','98c21eb49a8f4c59b65db5ddc3f7e86f','WEB',NULL,'XQj1LA4lEApnYNjNkk2ftg0DA7khA06-rYBa1ureWt8','2026-06-06 00:17:48','2026-06-08 00:17:48','Y','2026-06-06 00:19:37',NULL,NULL,'20260400010','2026-06-06 00:17:48','20260400010','2026-06-06 00:19:37'),('001','20260400010','9ac247a271ca4bd58e0203767097018d','WEB',NULL,'S_ztKKDngRGBA4kr_UGctLaQSPqGoGk47po5zhC3z6U','2026-06-03 21:08:02','2026-06-05 21:08:02','Y','2026-06-03 21:08:32',NULL,NULL,'20260400010','2026-06-03 21:08:02','20260400010','2026-06-03 21:08:32'),('001','20260400010','9c25d2ff0b5b4367b862e119697919a6','WEB',NULL,'pkI5iPaSe0C8Kd-KDZE6cFgruP0WBxPfy2lW7DDMJvE','2026-05-28 19:48:25','2026-05-30 18:46:27','Y','2026-05-28 20:38:25',NULL,NULL,'20260400010','2026-05-28 19:48:25','20260400010','2026-05-28 20:38:25'),('001','20260400010','9fad88e21b034e048c1782927640d9d1','WEB',NULL,'LaK9DRF-Rw8exF4NPs_mzeeDmOiKKwkr9nWvsfnXjIQ','2026-06-03 11:46:43','2026-06-05 11:46:43','Y','2026-06-03 13:02:43',NULL,NULL,'20260400010','2026-06-03 11:46:43','20260400010','2026-06-03 13:02:43'),('001','20260400010','a04999dfc0294d189477e668762dcbd1','WEB',NULL,'sjDlBQuCu4m41AUS1C3vVDwvtxtR_VT-8xjwtZA_rbc','2026-05-15 19:29:42','2026-05-22 19:29:42','Y','2026-05-15 19:32:12',NULL,NULL,'20260400010','2026-05-15 19:29:42','20260400010','2026-05-15 19:32:12'),('001','20260400010','a3f55cd57b0e4b1ba6be2f732ae18c38','WEB',NULL,'a6MKjsukXQghVS3b6pRaBnprE4dk2t2FnIjPguiEEHM','2026-05-25 19:33:03','2026-05-27 09:07:43','Y','2026-05-25 21:12:34',NULL,NULL,'20260400010','2026-05-25 19:33:03','20260400010','2026-05-25 21:12:34'),('001','20260400010','a67ad0037d5c4bb0bc009ca21cb28f7d','WEB',NULL,'Nqum7NZElyYGF_NDsk77wdDMMSCRnOnbTjltbk9IyiI','2026-05-21 19:09:44','2026-05-23 17:32:32','Y','2026-05-21 20:20:53',NULL,NULL,'20260400010','2026-05-21 19:09:44','20260400010','2026-05-21 20:20:53'),('001','20260400010','aa010205d10d4460904900416b5529f7','WEB',NULL,'S4EGQJQgGBLpfW5FM_TsOnTkyXqvd7LgwJU_9uzPF-c','2026-05-17 11:02:19','2026-05-19 09:50:33','Y','2026-05-17 12:19:12',NULL,NULL,'20260400010','2026-05-17 11:02:19','20260400010','2026-05-17 12:19:12'),('001','20260400010','ab83d4bb48224b2e9c57e782e084703c','APP',NULL,'RwJT-UIwwy2GTdYXek_ET8n3i6BU-J60lCnFtJURNFU','2026-05-28 20:38:36','2026-05-30 20:38:22','Y','2026-05-28 20:45:21',NULL,NULL,'20260400010','2026-05-28 20:38:36','20260400010','2026-05-28 20:45:21'),('001','20260400010','ab854ad8b19040ed866bdc63373ad69a','WEB',NULL,'kfixdk4j79BYL-2FbMr9e9f68k5LFM9_PtjNFScbu3w','2026-05-26 22:20:01','2026-05-28 18:38:42','Y','2026-05-27 18:51:18',NULL,NULL,'20260400010','2026-05-26 22:20:01','20260400010','2026-05-27 18:51:18'),('001','20260400010','abae0073d7884ac2b8a8197691b3535f','WEB',NULL,'ihRqC-TGd0fGCXS2hGqKqjF9DPCJ_1qil7XKi3_nkLE','2026-05-19 20:05:31','2026-05-21 18:34:26','Y','2026-05-19 22:15:49',NULL,NULL,'20260400010','2026-05-19 20:05:31','20260400010','2026-05-19 22:15:49'),('001','20260400010','ac366ba4898244a28488c44dc62bffc8','WEB',NULL,'PQDUzDLu9JbxydQPgVVqytLWMZLP6nRZPOhjTPzcyBk','2026-06-03 10:15:01','2026-06-05 10:15:01','Y','2026-06-03 11:46:43',NULL,NULL,'20260400010','2026-06-03 10:15:01','20260400010','2026-06-03 11:46:43'),('001','20260400010','ac46f5de41374aaf85714265625346b7','APP',NULL,'IewPFelSIm8boYmbAPatjjwHczyAT9RUEOVgf8LlqCI','2026-05-29 20:32:52','2026-05-31 20:32:52','Y','2026-05-29 21:21:19',NULL,NULL,'20260400010','2026-05-29 20:32:52','20260400010','2026-05-29 21:21:19'),('001','20260400010','acb7eb3be4144778a753217b881a81cd','WEB',NULL,'cTFj78IKjhwosnY1RyXC9BaBuRCWNwN3oI7jlFrvWjs','2026-06-07 12:06:44','2026-06-09 12:06:44','Y','2026-06-07 14:10:01',NULL,NULL,'20260400010','2026-06-07 12:06:44','20260400010','2026-06-07 14:10:01'),('001','20260400010','ace9207530ab4afc9327299333f226ec','WEB',NULL,'Fhm1cLYIOI89w_aPUnaUxq2XebcygsNZJ_HW00_161Y','2026-05-31 13:38:39','2026-06-02 13:38:39','Y','2026-05-31 19:40:02',NULL,NULL,'20260400010','2026-05-31 13:38:39','20260400010','2026-05-31 19:40:02'),('001','20260400010','ae121a95b74a4e77989b18aaef11e837','WEB',NULL,'e7hmnKivzxzN6R6ueMISUHpE7SwiX6A-_7pyuJbO33o','2026-05-26 18:37:47','2026-05-27 21:22:27','Y','2026-05-26 18:38:42',NULL,NULL,'20260400010','2026-05-26 18:37:47','20260400010','2026-05-26 18:38:42'),('001','20260400010','ae147bb770f64cbda5eafc98637273a9','WEB',NULL,'d6ftk5DCWS4SHUrW6hpzdICPOkucyLjBSIFj2VIFtL4','2026-06-02 20:14:12','2026-06-04 20:14:12','Y','2026-06-02 21:39:58',NULL,NULL,'20260400010','2026-06-02 20:14:12','20260400010','2026-06-02 21:39:58'),('001','20260400010','aed5bcee034840868d7775f354ccbe71','WEB',NULL,'p6WJq2Wc4LbKeczmKvUPFx51sPCIx6fWpWMnHByLDSU','2026-05-13 18:50:31','2026-05-20 18:50:31','Y','2026-05-13 22:44:58',NULL,NULL,'20260400010','2026-05-13 18:50:31','20260400010','2026-05-13 22:44:58'),('001','20260400010','af0c5311d8f04128935badc3dfadf70e','WEB',NULL,'7G0gIMf_iL0gOVO9uBfDN2EAFokHJfAm1EmHQI-oI-U','2026-06-04 20:38:00','2026-06-06 20:38:00','Y','2026-06-05 22:21:55',NULL,NULL,'20260400010','2026-06-04 20:38:00','20260400010','2026-06-05 22:21:55'),('001','20260400010','af2801de17744537b9d1e5cfdd2b4f38','WEB',NULL,'1tY9ZJLBW5qxDERSgdvU4zeq0WxNrkMuPVoVIaq_ObY','2026-05-09 07:18:51','2026-05-16 07:18:51','Y','2026-05-10 13:45:55',NULL,NULL,'20260400010','2026-05-09 07:18:51','20260400010','2026-05-10 13:45:55'),('001','20260400010','b0ecf7ffd7884dbdaf2a93c0369377dc','WEB',NULL,'1-B-zPcTIIjgkIoWESjI5g0_SxS-ogPS79MdXRRhyII','2026-05-23 22:40:04','2026-05-25 21:34:17','Y','2026-05-24 08:08:50',NULL,NULL,'20260400010','2026-05-23 22:40:04','20260400010','2026-05-24 08:08:50'),('001','20260400010','b2f1b53b1e7046a1aa605f5ee4d18aa6','WEB',NULL,'woqS7E4nyVpDM7ZhXdnquvE655T7YX_w95Bq4BcOUBk','2026-05-28 20:42:31','2026-05-30 20:42:31','Y','2026-05-28 20:45:21',NULL,NULL,'20260400010','2026-05-28 20:42:31','20260400010','2026-05-28 20:45:21'),('001','20260400010','b392437e7c1946a3b178c495d0fc7be0','WEB',NULL,'H0MH-CLguHxXzoHJFzs6dx_qbStPGFzI833FUm2ukas','2026-05-16 23:04:26','2026-05-18 20:51:32','Y','2026-05-17 09:50:29',NULL,NULL,'20260400010','2026-05-16 23:04:26','20260400010','2026-05-17 09:50:29'),('001','20260400010','b4ff07e92fcb4e0982b33600fcc4100e','APP',NULL,'LYOpLVMN0LnkBYfeBDB4nM4dfSPVT4pnQ-3TxMfK2yI','2026-05-28 20:38:28','2026-05-30 20:38:22','Y','2026-05-28 20:38:36',NULL,NULL,'20260400010','2026-05-28 20:38:28','20260400010','2026-05-28 20:38:36'),('001','20260400010','ba4afb8d0237471cab8f2801cf2163d7','WEB',NULL,'1ns-MBsDsNU8UAlpTxzaY8CGiP9mH1LcF6LTwgGMVNo','2026-05-13 22:45:02','2026-05-20 22:45:02','Y','2026-05-14 19:40:34',NULL,NULL,'20260400010','2026-05-13 22:45:02','20260400010','2026-05-14 19:40:34'),('001','20260400010','bad48e7b733c4549a3005e8e7386e2d0','WEB',NULL,'y-q5Q9TI32CgZFCUdaau57FNo1JS-9sH_4b760s4c40','2026-05-29 20:14:03','2026-05-31 20:14:03','Y','2026-05-29 20:32:37',NULL,NULL,'20260400010','2026-05-29 20:14:03','20260400010','2026-05-29 20:32:37'),('001','20260400010','bcda59f9f1f441fb8d7e93ca1170a05d','WEB',NULL,'xpvzK4KhYlBxkJFQhu9yI_sp_mapWpiH18i6Y3Kve_M','2026-05-25 10:09:27','2026-05-27 09:07:43','Y','2026-05-25 13:14:48',NULL,NULL,'20260400010','2026-05-25 10:09:27','20260400010','2026-05-25 13:14:48'),('001','20260400010','beffb110be1243a582f98646c87c095b','WEB',NULL,'m8oCtqik4Gr_craYfhRoZjD3HXtFaYX8XW_MPN8vEog','2026-06-06 01:21:52','2026-06-08 01:21:52','Y','2026-06-06 01:22:00',NULL,NULL,'20260400010','2026-06-06 01:21:52','20260400010','2026-06-06 01:22:00'),('001','20260400010','bf0c5be0d79548acb1eaebdfdf1e2990','WEB',NULL,'ZfJ3MAz5CevQambRWPgA9HVD5XSjs6YIEiaapO8t2_Q','2026-06-06 22:32:47','2026-06-08 22:32:47','Y','2026-06-06 23:34:31',NULL,NULL,'20260400010','2026-06-06 22:32:47','20260400010','2026-06-06 23:34:31'),('001','20260400010','bff7e98d93694e98a8a930e028fa0dab','WEB',NULL,'e7UBa574CPhLC9BC1uuAStE_uziRqFFztJZV1YgVMPk','2026-05-20 18:53:01','2026-05-22 18:53:01','Y','2026-05-20 20:27:01',NULL,NULL,'20260400010','2026-05-20 18:53:01','20260400010','2026-05-20 20:27:01'),('001','20260400010','c06a73cf56cf4627bca0e6a84d3f1f88','WEB',NULL,'gGY7K2qawvER-JyvwlefQTKlCYoaT38cDRVZwK0fi3k','2026-05-27 18:51:18','2026-05-28 18:38:42','Y','2026-05-27 18:51:26',NULL,NULL,'20260400010','2026-05-27 18:51:18','20260400010','2026-05-27 18:51:26'),('001','20260400010','c10c9adb99a843c5a6803e2292d99581','WEB',NULL,'TZMyHbPBtTEPVg4YTL850Z8lS6qsX1wUZ-TISnHj0Lc','2026-05-27 21:58:13','2026-05-29 21:58:13','Y','2026-05-28 18:29:09',NULL,NULL,'20260400010','2026-05-27 21:58:13','20260400010','2026-05-28 18:29:09'),('001','20260400010','c316aa17093e4eaabb100083084cda5d','WEB',NULL,'q2DKqq7oIFMivsZS4E-wH7ffGRZMV9cEBAmcCX1euFg','2026-05-20 22:00:12','2026-05-22 22:00:12','Y','2026-05-20 23:50:48',NULL,NULL,'20260400010','2026-05-20 22:00:12','20260400010','2026-05-20 23:50:48'),('001','20260400010','c38cccc707624bd884c665b8be89da44','WEB',NULL,'0ZpKtWm6ADVHpS3hdiJ9f7qvZVhnGJFHjrcx--UsIHE','2026-06-07 15:43:17','2026-06-09 15:43:17','Y','2026-06-07 21:04:47',NULL,NULL,'20260400010','2026-06-07 15:43:17','20260400010','2026-06-07 21:04:47'),('001','20260400010','c533c112c9b04701a50c204fb5fdf983','WEB',NULL,'3y53SrQhoYun7z8ARjukS8B9yhflsyj6Jm5PbPKqHck','2026-05-17 12:19:12','2026-05-19 09:50:33','Y','2026-05-17 13:59:01',NULL,NULL,'20260400010','2026-05-17 12:19:12','20260400010','2026-05-17 13:59:01'),('001','20260400010','c6515f17bf0f4ee398493e41668da6b5','WEB',NULL,'Q3Oase-aDEO1Gg2RPy9LkmIGD1wsnvhaYY0CIwgYX1k','2026-05-12 21:17:56','2026-05-19 21:17:56','Y','2026-05-12 21:19:06',NULL,NULL,'20260400010','2026-05-12 21:17:56','20260400010','2026-05-12 21:19:06'),('001','20260400010','c766edfd17fc449190a40b3ffb1264d3','WEB',NULL,'LRqmCylIYItaK6BMm1P3Z2ZSc_WkFPlslXMrmo-qClA','2026-05-14 19:40:34','2026-05-21 19:40:34','Y','2026-05-14 20:53:10',NULL,NULL,'20260400010','2026-05-14 19:40:34','20260400010','2026-05-14 20:53:10'),('001','20260400010','c987342269d949afb1a825e3c36b8551','WEB',NULL,'9UeSolIyS-IwLy_TTL060kz0zkxS4AB3sPuNcrllo4o','2026-04-28 20:44:25','2026-05-05 20:44:25','Y','2026-04-29 20:20:05',NULL,NULL,'20260400010','2026-04-28 20:44:25','20260400010','2026-04-29 20:20:05'),('001','20260400010','ca8c4379f1dc47708c661de13210b627','WEB',NULL,'OeNo8AqTa8w7p6ZdlDR_ynbhGMFAOAKP5Z-dRSzHPMY','2026-05-20 23:50:48','2026-05-22 23:50:48','Y','2026-05-21 16:15:07',NULL,NULL,'20260400010','2026-05-20 23:50:48','20260400010','2026-05-21 16:15:07'),('001','20260400010','cc164d09a47241c4bd2a294d94e6ea8d','WEB',NULL,'1q2nZUnBsiZCYubIioLe-ROPcvokSDQiOoDWJT3Rf7w','2026-05-24 08:08:53','2026-05-26 08:08:53','Y','2026-05-24 10:00:59',NULL,NULL,'20260400010','2026-05-24 08:08:53','20260400010','2026-05-24 10:00:59'),('001','20260400010','cd305f275e604e1cbd4ecc9ad6c01c6a','WEB',NULL,'S2fM2bBPbYzNOS20xYV3fsI16uSU7SY3UO7S0LCl8-4','2026-06-04 18:45:02','2026-06-06 18:45:02','Y','2026-06-04 20:07:03',NULL,NULL,'20260400010','2026-06-04 18:45:02','20260400010','2026-06-04 20:07:03'),('001','20260400010','cd69789e715f4e3abc738064b7eae038','WEB',NULL,'jBK8fFJ4XuNT2G0WbDbWQ58WypzqH7yU_CvJDdv2VG8','2026-05-23 21:34:17','2026-05-25 21:34:17','Y','2026-05-23 22:40:04',NULL,NULL,'20260400010','2026-05-23 21:34:17','20260400010','2026-05-23 22:40:04'),('001','20260400010','cf9c3a24649647b8ac525f43d50c5a11','APP',NULL,'nzkkVvFH-wF1jRGyF5FPgWZPTSPBToOdcJ9AMtuw6bk','2026-05-28 20:38:25','2026-05-30 20:38:22','Y','2026-05-28 20:38:28',NULL,NULL,'20260400010','2026-05-28 20:38:25','20260400010','2026-05-28 20:38:28'),('001','20260400010','d00e58cc9b7e445bbacd326548d571e6','WEB',NULL,'xnr874ybehrjFd3qybgT0fV8vbX8zl-w3ZSRzBS6EQM','2026-06-06 14:51:23','2026-06-08 14:51:23','Y','2026-06-06 16:14:02',NULL,NULL,'20260400010','2026-06-06 14:51:23','20260400010','2026-06-06 16:14:02'),('001','20260400010','d21f45d0fdef4732b8fad2d7ea48eaec','WEB',NULL,'3aLz2V4VowHpiZdIIw_qnmGZXhPpYD3FPJDSyWKi-8o','2026-04-30 21:17:02','2026-05-07 21:17:02','Y','2026-04-30 21:20:01',NULL,NULL,'20260400010','2026-04-30 21:17:02','20260400010','2026-04-30 21:20:01'),('001','20260400010','d472aa25db114426946d379d4c297372','WEB',NULL,'1iX6d0VxKmq2fNiUISUQgYMZjYaeZaqZqFbISQDLBF4','2026-04-30 22:04:11','2026-05-07 22:04:11','Y','2026-04-30 22:04:51',NULL,NULL,'20260400010','2026-04-30 22:04:11','20260400010','2026-04-30 22:04:51'),('001','20260400010','d4df78129f46412a97bef06b3cabc8c8','WEB',NULL,'Cfy8_eqQq-fXe12h12Nx3OW9WuVvmdlN_rtxjpH4LTQ','2026-05-22 18:51:55','2026-05-24 18:51:55','Y','2026-05-22 21:53:59',NULL,NULL,'20260400010','2026-05-22 18:51:55','20260400010','2026-05-22 21:53:59'),('001','20260400010','d6c03d3affb14173bcfc0e0f5b6e4081','WEB',NULL,'zSkFb-J2N2ECOj-AUfPam6puX_fzw_dOCX27AZnpGtA','2026-05-23 17:05:55','2026-05-25 15:49:33','Y','2026-05-23 18:32:03',NULL,NULL,'20260400010','2026-05-23 17:05:55','20260400010','2026-05-23 18:32:03'),('001','20260400010','d9c780185a464426b32dacac0b4a2407','WEB',NULL,'UKlH9QHzT9TDqBOA7T1H8jgid6mP6IIN4rDVZMah8hE','2026-05-22 21:53:59','2026-05-24 18:51:55','Y','2026-05-23 14:30:40',NULL,NULL,'20260400010','2026-05-22 21:53:59','20260400010','2026-05-23 14:30:40'),('001','20260400010','ddf16f6f6790420dbc4d4d88f66b25db','WEB',NULL,'sNGKF9OgAA_E9Xumb27odqF88fifeKdUZY0PsRtZK3A','2026-05-17 09:50:33','2026-05-19 09:50:33','Y','2026-05-17 11:02:19',NULL,NULL,'20260400010','2026-05-17 09:50:33','20260400010','2026-05-17 11:02:19'),('001','20260400010','e071038cea804853864b9522102baafe','WEB',NULL,'nM_MhZBIiRGN7wZf6JpOmDaWu7JSrPzSP8gnDnfbvuM','2026-05-20 20:27:01','2026-05-22 20:27:01','Y','2026-05-20 20:41:09',NULL,NULL,'20260400010','2026-05-20 20:27:01','20260400010','2026-05-20 20:41:09'),('001','20260400010','e08192fccee34f5e8f2fd9f17103b865','WEB',NULL,'SOrUupkk7oGzXsj6Y7OgkiZ8zDOLl1Zo17GFIqb7Xd4','2026-06-06 23:36:46','2026-06-08 23:36:46','Y','2026-06-07 12:06:44',NULL,NULL,'20260400010','2026-06-06 23:36:46','20260400010','2026-06-07 12:06:44'),('001','20260400010','e1ac03aac696432991c666ebd432ed96','WEB',NULL,'ZGvAfKFIuBPS3mgwh1XwRQI6VOcR4ipYBNvVYkVi7r0','2026-06-02 23:11:16','2026-06-04 23:11:16','Y','2026-06-03 10:15:01',NULL,NULL,'20260400010','2026-06-02 23:11:16','20260400010','2026-06-03 10:15:01'),('001','20260400010','e3875d8175224a888ac524421d981512','WEB',NULL,'jJ-K7xW3QmslaGAXKxHsbT6cMcRFZ0our3dtdL6O3eE','2026-05-29 13:53:36','2026-05-31 13:53:36','Y','2026-05-29 13:59:25',NULL,NULL,'20260400010','2026-05-29 13:53:36','20260400010','2026-05-29 13:59:25'),('001','20260400010','e53af56787fb41b99e5a8b2eb23074df','WEB',NULL,'vNSq2agqNQYw56w9Ea6oBMOrrgQ8EfjCS8JrOMZWz84','2026-05-21 21:22:34','2026-05-23 21:22:34','Y','2026-05-21 22:30:27',NULL,NULL,'20260400010','2026-05-21 21:22:34','20260400010','2026-05-21 22:30:27'),('001','20260400010','e619cea79687492b9fb03b1d9e1585d7','APP',NULL,'FV47Tm_P4LyAvsicwumZZtKJj_LvhBpHXdWJECu7ndk','2026-05-29 21:21:45','2026-05-31 21:21:45','Y','2026-05-29 21:28:08',NULL,NULL,'20260400010','2026-05-29 21:21:45','20260400010','2026-05-29 21:28:08'),('001','20260400010','e76e74d060694a01a7a7bdfe632f594b','WEB',NULL,'5nD73Q6XDJJsOOd2nEu29GQdzV-CYa1bBknK-0rX_Do','2026-05-25 09:07:30','2026-05-26 16:31:55','Y','2026-05-25 09:07:43',NULL,NULL,'20260400010','2026-05-25 09:07:30','20260400010','2026-05-25 09:07:43'),('001','20260400010','e8d4b5580ff84ac2892738930c4cee06','WEB',NULL,'H8zN-9qiavGUArAKa0SYSjTn_QG7NxC_hF-_8dRX3ss','2026-05-18 22:18:13','2026-05-20 21:21:23','Y','2026-05-18 22:23:43',NULL,NULL,'20260400010','2026-05-18 22:18:13','20260400010','2026-05-18 22:23:43'),('001','20260400010','e9722fe90ec94f9794c38f43c27a0eee','WEB',NULL,'FiSY2r8Nauj8WoIgHe6lK_PPCdcJkvaNDJ2Epq_nbIE','2026-06-07 14:10:01','2026-06-09 14:10:01','Y','2026-06-07 15:43:17',NULL,NULL,'20260400010','2026-06-07 14:10:01','20260400010','2026-06-07 15:43:17'),('001','20260400010','e9842416659944d0a402a84d987e5367','WEB',NULL,'yaNR6RKkcZ_qQXDyNkz_r2oaPFNs2A2v5EX-aludk6o','2026-05-17 16:37:33','2026-05-19 16:37:33','Y','2026-05-17 16:37:41',NULL,NULL,'20260400010','2026-05-17 16:37:33','20260400010','2026-05-17 16:37:41'),('001','20260400010','e9e13650728448ceb4b65ab7d91f15f4','WEB',NULL,'Z0xcau39Q9Hg3mohRyCc6pA6g_yAhz-2cxlRDpukGZU','2026-05-26 21:56:34','2026-05-28 18:38:42','Y','2026-05-26 22:20:01',NULL,NULL,'20260400010','2026-05-26 21:56:34','20260400010','2026-05-26 22:20:01'),('001','20260400010','ea0a5b580e484e5d97c24fbe32bc8c31','WEB',NULL,'0SbIfu_rowNWGN8_ZHE2woww0ic7kYY3LbxKpO7vUxs','2026-05-24 20:09:01','2026-05-26 16:31:55','Y','2026-05-24 21:29:46',NULL,NULL,'20260400010','2026-05-24 20:09:01','20260400010','2026-05-24 21:29:46'),('001','20260400010','eaa52d9aea0d4700b8f67505997d46d5','WEB',NULL,'w66gL-vto36njzzHX-2kfS3hTeQgSIEaa5vydo3mNIw','2026-05-17 16:41:53','2026-05-19 16:41:53','Y','2026-05-17 16:42:26',NULL,NULL,'20260400010','2026-05-17 16:41:53','20260400010','2026-05-17 16:42:26'),('001','20260400010','eb80fa853bd640c584951e5ac66560d6','WEB',NULL,'R0lgbC_rtY5dLCRtN9Cf5xfCPAUBSNCNqDlPRt-rXss','2026-05-24 10:00:59','2026-05-26 08:08:53','Y','2026-05-24 10:01:10',NULL,NULL,'20260400010','2026-05-24 10:00:59','20260400010','2026-05-24 10:01:10'),('001','20260400010','ed992b7fca31446aaeb2d1909255b512','WEB',NULL,'ekaYMJNG5G_RblbvmDXtKoio0SwapdFX_gjdfQAmIOQ','2026-06-06 00:19:37','2026-06-08 00:19:37','Y','2026-06-06 01:21:17',NULL,NULL,'20260400010','2026-06-06 00:19:37','20260400010','2026-06-06 01:21:17'),('001','20260400010','ee607c9042b74448b9fc28374e2ffd57','WEB',NULL,'GF8v-Az2RiBh3M-5MEW4UySGwUlYTfx9FCtvY5Ini9Q','2026-05-23 20:40:33','2026-05-25 18:35:40','Y','2026-05-23 21:06:59',NULL,NULL,'20260400010','2026-05-23 20:40:33','20260400010','2026-05-23 21:06:59'),('001','20260400010','f040a2d9229b4051836b97c8330541ef','WEB',NULL,'Ob7TeiaKKSv1yG7v9ENOU4fmBEpXShYpz6Olpk1anqQ','2026-05-12 21:19:11','2026-05-19 21:19:11','Y','2026-05-12 22:32:42',NULL,NULL,'20260400010','2026-05-12 21:19:11','20260400010','2026-05-12 22:32:42'),('001','20260400010','f1462867677546db931a6ae0d23c43c2','WEB',NULL,'2i0ZI5lL0ibitFNniIHh5zYQTipuGSB8P97NQczwUvk','2026-05-16 20:51:32','2026-05-18 20:51:32','Y','2026-05-16 21:55:01',NULL,NULL,'20260400010','2026-05-16 20:51:32','20260400010','2026-05-16 21:55:01'),('001','20260400010','f413c53eeb144597a63f13c962855cad','WEB',NULL,'UToTIiDblshQSYK-UxpIeMxS49X_Xnh3CchdpIeswnA','2026-05-23 14:30:40','2026-05-24 18:51:55','Y','2026-05-23 14:30:45',NULL,NULL,'20260400010','2026-05-23 14:30:40','20260400010','2026-05-23 14:30:45'),('001','20260400010','f5e16f72a16a45a9aa56b59804b15aa0','WEB',NULL,'y9vXw9cNCTcFSZX9QEJWxLsWfx7nkXZa9u9GcTbCaMY','2026-05-21 20:25:45','2026-05-23 20:25:45','Y','2026-05-21 20:26:51',NULL,NULL,'20260400010','2026-05-21 20:25:45','20260400010','2026-05-21 20:26:51'),('001','20260400010','f7cea28a55524d21ae2025f083c9c03a','WEB',NULL,'HhKaE-i9OE_9QgJJHpyShIocgewwOF1pFoR_9b9nkGs','2026-06-03 13:50:55','2026-06-05 13:50:55','Y','2026-06-03 14:07:16',NULL,NULL,'20260400010','2026-06-03 13:50:55','20260400010','2026-06-03 14:07:16'),('001','20260400010','f7d39e7a6a194040a690eb40cdb67df6','APP',NULL,'93YJ5VOqkYx7j7Qn0dWJdbEhuwmptsX2z5gehJkyJOA','2026-05-29 21:21:29','2026-05-31 21:21:29','Y','2026-05-29 21:21:37',NULL,NULL,'20260400010','2026-05-29 21:21:29','20260400010','2026-05-29 21:21:37'),('001','20260400010','f8eb6e1eb66e490d8f438c8005bf6d33','APP',NULL,'A6DTPsI60JPw3j7SK1tJCr8i6UPSb5jTfZbO6CYWN7E','2026-05-28 21:12:25','2026-05-30 21:12:25','Y','2026-05-28 21:49:35',NULL,NULL,'20260400010','2026-05-28 21:12:25','20260400010','2026-05-28 21:49:35'),('001','20260400012','02f54b6a587c43058e421a4cef479c01','WEB',NULL,'K8vK54FjE20otlC7axyf6BbtVcasb29pbKV1UGv8LcQ','2026-05-12 21:57:53','2026-05-19 21:57:53','Y','2026-05-14 20:53:25',NULL,NULL,'20260400012','2026-05-12 21:57:53','20260400012','2026-05-14 20:53:25'),('001','20260400012','043003cb1c974f669404522eaec607f5','WEB',NULL,'bGBUmUHUntWQ_V6EydC1_uHWkZO2Lw9FDP2n4uNBIcI','2026-05-23 21:49:54','2026-05-25 21:49:54','Y','2026-05-23 23:17:23',NULL,NULL,'20260400012','2026-05-23 21:49:54','20260400012','2026-05-23 23:17:23'),('001','20260400012','05de9075d0c749b3a396dc227b1e2a96','WEB',NULL,'AppG5l9HJni9mVb4vi6s1HjhAA0XvoqHOxANJW8UPV0','2026-05-24 18:01:03','2026-05-26 16:51:22','Y','2026-05-24 23:33:20',NULL,NULL,'20260400012','2026-05-24 18:01:03','20260400012','2026-05-24 23:33:20'),('001','20260400012','0abfed1b88d746efb7191828c61bf552','WEB',NULL,'PRelWukkq5nfNYUFMGk0a90tszFJNy-W5fr_hLVPOrw','2026-05-25 20:13:49','2026-05-27 20:13:49','Y','2026-05-25 22:16:04',NULL,NULL,'20260400012','2026-05-25 20:13:49','20260400012','2026-05-25 22:16:04'),('001','20260400012','1eb092f7d8c4456fb3759fe7e5ee0c35','WEB',NULL,'nIwgWw0WQY2GAjwDfqK1pi_amlaK7DH-GfEzMhQKc-c','2026-04-28 21:11:17','2026-05-05 21:11:17','Y','2026-04-28 21:12:41',NULL,NULL,'20260400012','2026-04-28 21:11:17','20260400012','2026-04-28 21:12:41'),('001','20260400012','283420fc0aef4ddface744f6409c0665','WEB',NULL,'m_lxLjawO_nvFycGQxTw-ymrLA4aAg071wkLeFkSOAI','2026-05-28 22:31:27','2026-05-30 20:41:47','Y','2026-05-29 22:07:55',NULL,NULL,'20260400012','2026-05-28 22:31:27','20260400012','2026-05-29 22:07:55'),('001','20260400012','2c750f5d7b3046819b3593273e6ace7a','WEB',NULL,'TZtDFCTUcXoRnLyvPPHGv5C9EyqjnDWWhqBhcR3n9SU','2026-05-26 22:23:07','2026-05-28 22:23:07','Y','2026-05-28 20:41:08',NULL,NULL,'20260400012','2026-05-26 22:23:07','20260400012','2026-05-28 20:41:08'),('001','20260400012','319b95fae1304953998f3f1a53bcfc56','WEB',NULL,'al-pKtBqxRrXl4uRfeocRr9zZzqL1J8nMvwk-SnOIXo','2026-05-28 20:41:47','2026-05-30 20:41:47','Y','2026-05-28 22:31:27',NULL,NULL,'20260400012','2026-05-28 20:41:47','20260400012','2026-05-28 22:31:27'),('001','20260400012','341502403eff4714b2168629990d3431','WEB',NULL,'JctMhDcxlftCjr2ltSI5yRftKBzfncTAfZgXpvgn55U','2026-05-26 22:22:56','2026-05-27 22:16:04','Y','2026-05-26 22:23:07',NULL,NULL,'20260400012','2026-05-26 22:22:56','20260400012','2026-05-26 22:23:07'),('001','20260400012','42f1be4766d042128ff89260a70aa45e','WEB',NULL,'giU3Ou_aQTQvhbRtqHNEUINoVZgollU0kpeXe7Aa9jo','2026-05-28 20:41:08','2026-05-28 22:23:07','Y','2026-05-28 20:41:47',NULL,NULL,'20260400012','2026-05-28 20:41:08','20260400012','2026-05-28 20:41:47'),('001','20260400012','472be2b48e5f457a80641a3cdf0f428c','WEB',NULL,'3t3JfqUdfUE34pTWZerl8Jy1ycESbu7K8MFERiHw-rs','2026-05-14 20:53:25','2026-05-21 20:53:25','Y','2026-05-20 21:59:23',NULL,NULL,'20260400012','2026-05-14 20:53:25','20260400012','2026-05-20 21:59:23'),('001','20260400012','4956d4621f174527b7c2929a9f00a528','WEB',NULL,'V_1TWpXa7UyAg3CyJ2GusbukpFFkrog0MYN6DCDIppQ','2026-05-24 23:33:20','2026-05-26 16:51:22','Y','2026-05-25 20:13:49',NULL,NULL,'20260400012','2026-05-24 23:33:20','20260400012','2026-05-25 20:13:49'),('001','20260400012','49b39df591714311bd8cb516d3482be2','WEB',NULL,'TmSUwsEOCP2uD0mK_QguOmdb2EHaTtZRoZkHmAuWXiY','2026-05-05 16:28:58','2026-05-12 16:28:58','Y','2026-05-10 21:22:35',NULL,NULL,'20260400012','2026-05-05 16:28:58','20260400012','2026-05-10 21:22:35'),('001','20260400012','4de50a895d994783869a42215497e0ee','WEB',NULL,'rscMXjxkz3E-4FXShMvb_Dczs5_STB5ktVtXuFm0FzE','2026-04-28 21:12:51','2026-05-05 21:12:51','Y','2026-05-05 16:28:58',NULL,NULL,'20260400012','2026-04-28 21:12:51','20260400012','2026-05-05 16:28:58'),('001','20260400012','57ba5fac2fa84760950b17ce1c7dd3c1','WEB',NULL,'Qkoe-Aedqo8E3eldHqGMBcToNjM7iEu-5CaEGMkkgUU','2026-05-24 16:51:22','2026-05-26 16:51:22','Y','2026-05-24 18:01:03',NULL,NULL,'20260400012','2026-05-24 16:51:22','20260400012','2026-05-24 18:01:03'),('001','20260400012','5c70cb9d97ed4bd291849bf8934c1bc4','WEB',NULL,'V73SshBT-LZsLGN7SCpA2uyAlHrlvmH9xHByix7dORE','2026-05-23 23:17:23','2026-05-25 21:49:54','Y','2026-05-24 16:51:22',NULL,NULL,'20260400012','2026-05-23 23:17:23','20260400012','2026-05-24 16:51:22'),('001','20260400012','8469ab07d88747e9b1d48bdaa42aaf8f','WEB',NULL,'kHj2JswQQDyGn_ny-8OSKvhprdyyo8xluf6KrAtftZA','2026-05-10 21:22:35','2026-05-17 21:22:35','Y','2026-05-12 21:57:53',NULL,NULL,'20260400012','2026-05-10 21:22:35','20260400012','2026-05-12 21:57:53'),('001','20260400012','9e0b504b11ff47678934ca12b73c4098','WEB',NULL,'XdK1Ca4d_K9loFb_Elcgq-2Fx2jCZbRoga8x3eHVJE8','2026-05-25 22:16:04','2026-05-27 22:16:04','Y','2026-05-26 22:22:56',NULL,NULL,'20260400012','2026-05-25 22:16:04','20260400012','2026-05-26 22:22:56'),('001','20260400012','be81ef7a16a04bd3acbfdfa269d364ad','WEB',NULL,'6wLsiynp-wxDRX2da0csQXFK51NdyPkoP4kI25HW-L8','2026-05-20 21:59:23','2026-05-22 21:59:23','N',NULL,NULL,NULL,'20260400012','2026-05-20 21:59:23',NULL,NULL),('001','20260400012','cc44d4ea63d24846bace02a0f1d3ff20','WEB',NULL,'YNcP8oMtKwWnXxAoSu8b3Jci1eyxQVLDIz_GMy0oGwk','2026-05-29 22:07:55','2026-05-31 22:07:55','N',NULL,NULL,NULL,'20260400012','2026-05-29 22:07:55',NULL,NULL),('001','20260400013','037c10bfc2e349ae983f551992b5324a','APP',NULL,'q6uyTEPZ1CWNczv0Hc9chSKUV05u5SApsxb6Ncl60fY','2026-06-03 21:37:07','2026-06-05 21:37:07','Y','2026-06-03 22:37:38',NULL,NULL,'20260400013','2026-06-03 21:37:07','20260400013','2026-06-03 22:37:38'),('001','20260400013','04ec9fdde8f246e78ec4444aa893e2a9','APP',NULL,'rvuPZRxjMLCmbZMIP0I7IrlJ0W51LBtzIDOvcDrP6Pw','2026-05-31 18:03:32','2026-06-02 18:03:32','Y','2026-05-31 18:11:28',NULL,NULL,'20260400013','2026-05-31 18:03:32','20260400013','2026-05-31 18:11:28'),('001','20260400013','11aa304579fe4c5f918a665ae5c612c3','APP',NULL,'9uWFc2_y6mE4cwhYUr-WKmQLC3qmWslDBxmIR0ltHt8','2026-06-01 21:38:05','2026-06-03 21:38:05','Y','2026-06-01 21:54:06',NULL,NULL,'20260400013','2026-06-01 21:38:05','20260400013','2026-06-01 21:54:06'),('001','20260400013','1da3347bd0c94b809656c45734c89552','APP',NULL,'ipwh9ezX_0S7GeKx1MuQRk-j6-l_Eq8aMgjT_6KvIvQ','2026-05-29 21:29:10','2026-05-31 21:29:10','Y','2026-05-29 21:36:41',NULL,NULL,'20260400013','2026-05-29 21:29:10','20260400013','2026-05-29 21:36:41'),('001','20260400013','1e041ce4cf864ee69c726b3efb6e494d','APP',NULL,'D9fA0GvqRqFTy1cHQh4kHWBT47_rZ3V-VhB8hAgOScc','2026-05-31 19:24:29','2026-06-02 19:24:29','Y','2026-05-31 19:35:56',NULL,NULL,'20260400013','2026-05-31 19:24:29','20260400013','2026-05-31 19:35:56'),('001','20260400013','1e81cda71aad406ca50114a465dcad10','APP',NULL,'-JaBxCg-ZraaNUGUytUH-Nsz_jbOHMKe14XnC9VVum4','2026-06-01 20:19:20','2026-06-03 20:19:20','Y','2026-06-01 20:29:12',NULL,NULL,'20260400013','2026-06-01 20:19:20','20260400013','2026-06-01 20:29:12'),('001','20260400013','214cb6e7c542458fb4f79943fb49149d','APP',NULL,'_ATAYnbNAeRiK_JnWblHv7i7rx07T7uBq5xNg6x_9pc','2026-05-31 20:29:56','2026-06-02 20:29:56','Y','2026-05-31 20:42:57',NULL,NULL,'20260400013','2026-05-31 20:29:56','20260400013','2026-05-31 20:42:57'),('001','20260400013','2362c1436f4a4389bc2eda2791c6e193','APP',NULL,'2NoQ5dkqbIWyPC2vm9UR9kqxvjCvhbbbu6g5rIB54lY','2026-05-31 19:00:51','2026-06-02 19:00:51','Y','2026-05-31 19:19:34',NULL,NULL,'20260400013','2026-05-31 19:00:51','20260400013','2026-05-31 19:19:34'),('001','20260400013','26b10222c15d42c49706bec6d3164a08','APP',NULL,'53a0b0nyRIVbehNMyhRGc5rO--nID8bvqKhrnUP3UVA','2026-05-30 13:13:03','2026-06-01 13:13:03','Y','2026-05-30 17:07:00',NULL,NULL,'20260400013','2026-05-30 13:13:03','20260400013','2026-05-30 17:07:00'),('001','20260400013','27e471bf23954607a10318e3c0bee5c4','APP',NULL,'VlNlTgAAN51ICBT_F_7biP0PaXg-CbDtuVzrM1_Y0Tc','2026-05-31 21:55:59','2026-06-02 21:55:59','Y','2026-05-31 22:52:29',NULL,NULL,'20260400013','2026-05-31 21:55:59','20260400013','2026-05-31 22:52:29'),('001','20260400013','29b2539659974b47a939c3841f3fb712','WEB',NULL,'g1266_soIJ8t-e6Oy4TiQUZYEtZs7fC_W-HBNIt9L3I','2026-06-06 23:36:42','2026-06-08 23:36:42','Y','2026-06-06 23:37:02',NULL,NULL,'20260400013','2026-06-06 23:36:42','20260400013','2026-06-06 23:37:02'),('001','20260400013','2e55dde3b9dc41e38e193d4926836ec7','APP',NULL,'8YOcF3CdoCJ3jbkyG9IsPvhGWgB50-aVAes5ZiodEF0','2026-06-01 19:55:44','2026-06-03 19:55:44','Y','2026-06-01 20:19:20',NULL,NULL,'20260400013','2026-06-01 19:55:44','20260400013','2026-06-01 20:19:20'),('001','20260400013','30397c6fc9dc499c8aca2cec9823f954','WEB',NULL,'CDyeN9zT6MSacUQMVG5t8dgboaJJB7tHopFOKvGVj1w','2026-05-23 21:29:47','2026-05-25 21:29:47','N',NULL,NULL,NULL,'20260400013','2026-05-23 21:29:47',NULL,NULL),('001','20260400013','3349180a95064b6486a5c63d5c2543f6','APP',NULL,'dozGgdWF8xcn5yl5HnNYcHt9B1idbM34zLEzMRftu-M','2026-06-03 19:19:57','2026-06-05 19:19:57','Y','2026-06-03 21:08:44',NULL,NULL,'20260400013','2026-06-03 19:19:57','20260400013','2026-06-03 21:08:44'),('001','20260400013','3555590a5b714ab8bcc4a494b514a191','APP',NULL,'CV2lU7qU5brRk3cgFOUqyYJPUTbwcsR7JP0jQ_bDBO8','2026-06-02 23:57:30','2026-06-04 23:57:30','Y','2026-06-03 00:11:21',NULL,NULL,'20260400013','2026-06-02 23:57:30','20260400013','2026-06-03 00:11:21'),('001','20260400013','3a1b35e2ba8a4d72a3582736cb778d58','APP',NULL,'owlkIH_QEjce5AMCdh5VvpPT4xUn83b4BG9077OTgTE','2026-05-31 20:59:06','2026-06-02 20:59:06','Y','2026-05-31 21:55:59',NULL,NULL,'20260400013','2026-05-31 20:59:06','20260400013','2026-05-31 21:55:59'),('001','20260400013','3a4909456162403eb88ea64b841faf3b','APP',NULL,'Lx-dn0x5mKMSgZYR59uM6UVZNqjwJ4CGg2lYwPL_EG0','2026-05-31 18:52:13','2026-06-02 18:52:13','Y','2026-05-31 18:58:55',NULL,NULL,'20260400013','2026-05-31 18:52:13','20260400013','2026-05-31 18:58:55'),('001','20260400013','3bb3c62e33634f61931a3313a9bbf8af','APP',NULL,'F9P0Bgx33Fwx8_Vn8KYcXZ_vcHc_lY2ssFr8CFO1Nd4','2026-06-03 13:07:29','2026-06-05 13:07:29','Y','2026-06-03 13:52:22',NULL,NULL,'20260400013','2026-06-03 13:07:29','20260400013','2026-06-03 13:52:22'),('001','20260400013','3cb0476a23e447d4a8bb87d4dbe0ae43','APP',NULL,'Xuis9Rp_WnEr0fEgr90OnXKSx_6gQW5-t0kodnFe69c','2026-06-03 22:37:38','2026-06-05 22:37:38','Y','2026-06-04 20:18:39',NULL,NULL,'20260400013','2026-06-03 22:37:38','20260400013','2026-06-04 20:18:39'),('001','20260400013','3ebdebc5ae2548b983bf85cbc62e8b1b','APP',NULL,'nMiRsky30WEX5jQRk_5d48dQKvenD71z3bHt4hzvxT0','2026-06-02 21:02:35','2026-06-04 21:02:35','Y','2026-06-02 21:11:51',NULL,NULL,'20260400013','2026-06-02 21:02:35','20260400013','2026-06-02 21:11:51'),('001','20260400013','43013e7b3bc64c5f8b48c76e536515e5','APP',NULL,'nDvjM7oH_Nv1jfX1BHdDxdLZFg7p1DLB9K2REDpMaIc','2026-06-03 10:15:45','2026-06-05 10:15:45','Y','2026-06-03 11:37:03',NULL,NULL,'20260400013','2026-06-03 10:15:45','20260400013','2026-06-03 11:37:03'),('001','20260400013','4d5631f25e744a55912bb12a7c8f9973','APP',NULL,'ROs-8GJEgzwS3tXoLu-uvLvNkr29EC5MMDbET6TWToc','2026-06-04 20:18:39','2026-06-06 20:18:39','N',NULL,NULL,NULL,'20260400013','2026-06-04 20:18:39',NULL,NULL),('001','20260400013','4fa57ca1dd7b4d11931e3c097ce9e2a0','APP',NULL,'36P_hG8TVhTaAQ3hVL1EmoK8Pm3tlPEFACqMWWusJSU','2026-06-01 18:20:23','2026-06-03 18:20:23','Y','2026-06-01 19:55:44',NULL,NULL,'20260400013','2026-06-01 18:20:23','20260400013','2026-06-01 19:55:44'),('001','20260400013','5273bc1902b949328382854be67afd25','APP',NULL,'kOxZsH1VUWL0gW43d8d2mE7MX-JuIdw8_f-ruaaJWps','2026-06-02 23:10:44','2026-06-04 23:10:44','Y','2026-06-02 23:57:30',NULL,NULL,'20260400013','2026-06-02 23:10:44','20260400013','2026-06-02 23:57:30'),('001','20260400013','547176bbe36a408099f9e5db6caedeed','APP',NULL,'K15_XKrjmrgXYitc8_D00FWZZJnZETJvtQKNCDksR-8','2026-06-03 13:52:22','2026-06-05 13:52:22','Y','2026-06-03 19:19:57',NULL,NULL,'20260400013','2026-06-03 13:52:22','20260400013','2026-06-03 19:19:57'),('001','20260400013','5fc0cc5e75ac45af9f709ebc23f946b1','APP',NULL,'e-bfqb06b4-WQbWPZP9A4iyMT55dFSskejPVgQIM9uI','2026-06-03 10:15:41','2026-06-05 10:15:41','Y','2026-06-03 10:15:45',NULL,NULL,'20260400013','2026-06-03 10:15:41','20260400013','2026-06-03 10:15:45'),('001','20260400013','6a21bc838fbc41fd8866a830d424594a','APP',NULL,'nmtf4uCjwU_Yuoy6ZX20xgaf5RzEkwdrhEQVp0uwnHE','2026-06-01 21:54:06','2026-06-03 21:54:06','Y','2026-06-02 20:55:11',NULL,NULL,'20260400013','2026-06-01 21:54:06','20260400013','2026-06-02 20:55:11'),('001','20260400013','7132f179768848a194258118d4d488e2','WEB',NULL,'32i9j9x-Jp3VfRdBksS3I1eG04k8smIJ_a2CyTV-trw','2026-06-06 18:03:17','2026-06-08 18:03:17','Y','2026-06-06 23:36:42',NULL,NULL,'20260400013','2026-06-06 18:03:17','20260400013','2026-06-06 23:36:42'),('001','20260400013','72476d10394d46b7b1c7db08455c0227','APP',NULL,'E70IheDAoak1tBQHnbwPzXLW-MsHdmftWcVdSb7YHRU','2026-05-31 13:54:27','2026-06-02 13:54:27','Y','2026-05-31 18:03:32',NULL,NULL,'20260400013','2026-05-31 13:54:27','20260400013','2026-05-31 18:03:32'),('001','20260400013','72e30131aa034a16b97cba28e369d5f8','APP',NULL,'zEDVipBPvpdIPSvBHC18KDU4Vi9yXl3foIxE-m5lfoE','2026-06-01 18:16:20','2026-06-03 18:16:20','Y','2026-06-01 18:20:23',NULL,NULL,'20260400013','2026-06-01 18:16:20','20260400013','2026-06-01 18:20:23'),('001','20260400013','774a2dca7ea34bafbda402336a450c6a','APP',NULL,'-SZjnQ2zsarm-GPWhY4P2JCGvcdJrdOUJC8D3KIPJ8A','2026-05-31 18:27:07','2026-06-02 18:27:07','Y','2026-05-31 18:34:00',NULL,NULL,'20260400013','2026-05-31 18:27:07','20260400013','2026-05-31 18:34:00'),('001','20260400013','7b6d386fe9dc46e6b6aae6877daa8915','APP',NULL,'b8qJkcvlE4UB3_wxih33IrzAa3vFrFlumSeMQTFzbdM','2026-05-31 18:11:28','2026-06-02 18:11:28','Y','2026-05-31 18:27:07',NULL,NULL,'20260400013','2026-05-31 18:11:28','20260400013','2026-05-31 18:27:07'),('001','20260400013','7e6f3a746be44275ae19a00bf7c7d3ee','APP',NULL,'oRQBXtEvHo0SSq8lAilXECuZBmNXrzN6WX5q1YHNjT0','2026-06-03 13:03:14','2026-06-05 13:03:14','Y','2026-06-03 13:07:29',NULL,NULL,'20260400013','2026-06-03 13:03:14','20260400013','2026-06-03 13:07:29'),('001','20260400013','7f3f8d584dc34df5938bc4eb3bad04ab','APP',NULL,'_1FDLW71VzR_pvPgEQGQaodghDb7EjvLLqWtyr9b5bk','2026-05-31 22:52:29','2026-06-02 22:52:29','Y','2026-06-01 18:16:20',NULL,NULL,'20260400013','2026-05-31 22:52:29','20260400013','2026-06-01 18:16:20'),('001','20260400013','8600b264fa2d4847baaacd984e4a9f1f','APP',NULL,'dmWThJ9I5EHwUXFn3IiHySyPyih5nwUZxDxsBDvTPp8','2026-06-03 00:11:21','2026-06-05 00:11:21','Y','2026-06-03 10:15:39',NULL,NULL,'20260400013','2026-06-03 00:11:21','20260400013','2026-06-03 10:15:39'),('001','20260400013','892ca5f23e00419da5a315a32fe75d15','APP',NULL,'x5qLkmXc704XUOnnXttYlsiwMHU-KYuhzaJUAWW9NBI','2026-05-31 18:58:55','2026-06-02 18:58:55','Y','2026-05-31 19:00:51',NULL,NULL,'20260400013','2026-05-31 18:58:55','20260400013','2026-05-31 19:00:51'),('001','20260400013','8d8e31801203468f90cf3d2c7551677e','APP',NULL,'ediXdaNw9hQVZyLROAD7Dr9t5J60kv2aCbMTFJwkdbg','2026-06-01 20:29:12','2026-06-03 20:29:12','Y','2026-06-01 20:44:49',NULL,NULL,'20260400013','2026-06-01 20:29:12','20260400013','2026-06-01 20:44:49'),('001','20260400013','93a5cb35586f466db040c807b926cc42','WEB',NULL,'QEdAzuHm3jSK5etClN1n9YTzaX9Lz9GHNJ2gBMCBcvs','2026-06-06 18:02:40','2026-06-08 18:02:40','Y','2026-06-06 18:03:17',NULL,NULL,'20260400013','2026-06-06 18:02:40','20260400013','2026-06-06 18:03:17'),('001','20260400013','94c0d0502b2445f58e59912fe3238006','APP',NULL,'xGIigA3pQ2HVrzW0RUq0-6l9gzv4D8cU6-LDk6HcXGQ','2026-06-02 21:53:04','2026-06-04 21:53:04','Y','2026-06-02 22:31:15',NULL,NULL,'20260400013','2026-06-02 21:53:04','20260400013','2026-06-02 22:31:15'),('001','20260400013','99b64f9ff8a941319d8c0ee5207b8b3a','APP',NULL,'kvlYUoQZqeHjmPFGsF0OzhTezSHSMtX-C_tpndDUQUs','2026-06-01 20:44:49','2026-06-03 20:44:49','Y','2026-06-01 21:36:07',NULL,NULL,'20260400013','2026-06-01 20:44:49','20260400013','2026-06-01 21:36:07'),('001','20260400013','9bcf582cc1504926babb9f75f776694b','APP',NULL,'6jo3PQm-KCDkUQUNNvmsLx-y_CXlRCFF00PGOucUuMM','2026-06-03 11:37:03','2026-06-05 11:37:03','Y','2026-06-03 13:03:14',NULL,NULL,'20260400013','2026-06-03 11:37:03','20260400013','2026-06-03 13:03:14'),('001','20260400013','a350049f1b3042fbb3be4a3119393b6c','APP',NULL,'nktrcs3OX6ii2mokCz6jBBtOw6KdD8UNvcgJ4-iIHs8','2026-05-29 23:10:42','2026-05-31 23:10:42','Y','2026-05-29 23:12:42',NULL,NULL,'20260400013','2026-05-29 23:10:42','20260400013','2026-05-29 23:12:42'),('001','20260400013','a551d886bbab412c8794af12d0a4e40a','APP',NULL,'hdHm5o6TtZcKt4i9hEKycinb_sx3OMybULHkJyY0MrY','2026-05-31 19:21:33','2026-06-02 19:21:33','Y','2026-05-31 19:24:29',NULL,NULL,'20260400013','2026-05-31 19:21:33','20260400013','2026-05-31 19:24:29'),('001','20260400013','a5e9087b97bc4cd9a390995e3d658584','APP',NULL,'_VgnXz3ux1k-UmMwYcTxTuzsjb0muDOIkQiESpQjH4M','2026-05-30 17:07:00','2026-06-01 17:07:00','Y','2026-05-31 13:54:27',NULL,NULL,'20260400013','2026-05-30 17:07:00','20260400013','2026-05-31 13:54:27'),('001','20260400013','a7dbc66458c24be7b3381c9fe5cb601f','APP',NULL,'gq5oxeNo55lsxCXBR_Nmz6Jb6OKbt328vUI7fE4UpHs','2026-05-31 19:35:56','2026-06-02 19:35:56','Y','2026-05-31 20:22:05',NULL,NULL,'20260400013','2026-05-31 19:35:56','20260400013','2026-05-31 20:22:05'),('001','20260400013','b49fed76934c46bd82b0b5c111aa8419','APP',NULL,'cZRrFyk0qBd07aun2ipyoGRN-Jc2KJX92V3vSiHrZjA','2026-06-02 22:31:15','2026-06-04 22:31:15','Y','2026-06-02 23:10:44',NULL,NULL,'20260400013','2026-06-02 22:31:15','20260400013','2026-06-02 23:10:44'),('001','20260400013','b87e0cf967ae432196d29c7294f81a73','APP',NULL,'aRzAhY2EN5VX1jmuwz78uqQNo-AtF-MpdL-4F9f2CDc','2026-06-02 21:11:51','2026-06-04 21:11:51','Y','2026-06-02 21:40:06',NULL,NULL,'20260400013','2026-06-02 21:11:51','20260400013','2026-06-02 21:40:06'),('001','20260400013','b8c4ba2542d2424093be4ba7bbb9ee8b','APP',NULL,'WJdgHbeZvu6fLuhuzfv9DvSCbQV19oSz99Q8HcXN_VM','2026-05-29 21:37:04','2026-05-31 21:37:04','Y','2026-05-29 21:37:07',NULL,NULL,'20260400013','2026-05-29 21:37:04','20260400013','2026-05-29 21:37:07'),('001','20260400013','c2eda1264c26456eb24fc664ab6bdc50','APP',NULL,'TGa3MnjzARd9DoIhjkB695nUijZscbMkwieJfN1q8qQ','2026-06-03 21:08:44','2026-06-05 21:08:44','Y','2026-06-03 21:09:37',NULL,NULL,'20260400013','2026-06-03 21:08:44','20260400013','2026-06-03 21:09:37'),('001','20260400013','c37a0182f71343299820f683ab48061f','APP',NULL,'blPhWlwOwIPTm8ykLdcdZfOKvl2zrofE46rDaHOVF8A','2026-06-01 21:36:07','2026-06-03 21:36:07','Y','2026-06-01 21:38:05',NULL,NULL,'20260400013','2026-06-01 21:36:07','20260400013','2026-06-01 21:38:05'),('001','20260400013','c691746897ed49f1bc59c5fa92ed9eaa','APP',NULL,'1NYy31FuXl_4omq-XMiiL6-IncteP8qinIk82pykj2Q','2026-06-02 21:40:06','2026-06-04 21:40:06','Y','2026-06-02 21:53:04',NULL,NULL,'20260400013','2026-06-02 21:40:06','20260400013','2026-06-02 21:53:04'),('001','20260400013','c7048cdffc714075a8929dc3aa128e40','APP',NULL,'wByCduZDT-kDf5J_Juzp3gyBu45dJT-wCMWvpGMo69Q','2026-05-31 20:42:57','2026-06-02 20:42:57','Y','2026-05-31 20:59:06',NULL,NULL,'20260400013','2026-05-31 20:42:57','20260400013','2026-05-31 20:59:06'),('001','20260400013','cb7eb2e72021451480777ef295277cf9','APP',NULL,'83oANqiuJxcI68cpoTpiaLrAEThoSmVbPs8oc7Uvxdw','2026-05-31 18:34:00','2026-06-02 18:34:00','Y','2026-05-31 18:39:35',NULL,NULL,'20260400013','2026-05-31 18:34:00','20260400013','2026-05-31 18:39:35'),('001','20260400013','d44ed8cef0744b2dad04649321abfd66','APP',NULL,'O-bkU8lXeXRi80qRCKJRkyIJqcRuEI43QZDf9V2QSVM','2026-05-29 21:37:30','2026-05-31 21:37:30','Y','2026-05-29 23:10:42',NULL,NULL,'20260400013','2026-05-29 21:37:30','20260400013','2026-05-29 23:10:42'),('001','20260400013','d65989e2bc4648fab987ab0ea4eb6b01','APP',NULL,'yJX_P6IDauO1_DZLpG9TAfyQ0M11ZA2bYRW0Z7J4XLs','2026-06-08 20:11:38','2026-06-10 20:11:38','N',NULL,NULL,NULL,'20260400013','2026-06-08 20:11:38',NULL,NULL),('001','20260400013','dbe008c5cd0b45d088c156ad37189588','APP',NULL,'_KFW9Kov5JZHUukG8QIiVc-hFnuA8es3n4iAxh0lvf4','2026-06-02 20:55:11','2026-06-04 20:55:11','Y','2026-06-02 21:02:35',NULL,NULL,'20260400013','2026-06-02 20:55:11','20260400013','2026-06-02 21:02:35'),('001','20260400013','e8f3c5faa1a043f0a13c8da7828b0ce7','APP',NULL,'w-yvKZwfdzmV17Q0Et45RoxalQb3LHKH8owSlzNCYWI','2026-05-31 19:19:34','2026-06-02 19:19:34','Y','2026-05-31 19:21:33',NULL,NULL,'20260400013','2026-05-31 19:19:34','20260400013','2026-05-31 19:21:33'),('001','20260400013','eac03bd0712e4911885ab4d8ac5007e6','WEB',NULL,'rPmIt4G6L5gnd6qvEinaBhHQBmB7CUksR0phCkS6exI','2026-06-06 17:52:59','2026-06-08 17:52:59','Y','2026-06-06 18:02:40',NULL,NULL,'20260400013','2026-06-06 17:52:59','20260400013','2026-06-06 18:02:40'),('001','20260400013','eee703410727460f8a18be45efd4a5fd','APP',NULL,'lX8ugI0i1h-Xkx2EMRze_Fc9WzcZWQMZX8gI5GyyR3Y','2026-05-31 18:39:35','2026-06-02 18:39:35','Y','2026-05-31 18:52:13',NULL,NULL,'20260400013','2026-05-31 18:39:35','20260400013','2026-05-31 18:52:13'),('001','20260400013','f780a8e2e25142a2a84c970bf09e1e50','APP',NULL,'-anYbx7p-KMWnwPcGIrtesmsLi6aw8fIT4zongFTIKs','2026-06-03 21:09:37','2026-06-05 21:09:37','Y','2026-06-03 21:37:07',NULL,NULL,'20260400013','2026-06-03 21:09:37','20260400013','2026-06-03 21:37:07'),('001','20260400013','f827facfb84b49b2b8a7333f86a73257','APP',NULL,'7ufW23ukR8KvDZ3OmI-BioR3D09mD6ozFd-rZ3erk28','2026-05-31 20:22:05','2026-06-02 20:22:05','Y','2026-05-31 20:29:56',NULL,NULL,'20260400013','2026-05-31 20:22:05','20260400013','2026-05-31 20:29:56'),('001','20260400013','fa5d4e3fb17742d1ae2622426d810280','WEB',NULL,'c2qHDnvNoL-lKwDkoKZgnU0iHWuO-dEm8fkAy5gzcA8','2026-06-06 23:37:02','2026-06-08 23:37:02','N',NULL,NULL,NULL,'20260400013','2026-06-06 23:37:02',NULL,NULL),('001','20260400013','fae7b4a0ad7d41c2967617a6475832db','APP',NULL,'hCTedXNdUiE81BiO69S2_VpH1rw00hjq1CHBajjxh4g','2026-05-30 10:40:28','2026-06-01 10:40:28','Y','2026-05-30 10:50:26',NULL,NULL,'20260400013','2026-05-30 10:40:28','20260400013','2026-05-30 10:50:26');
/*!40000 ALTER TABLE `tb_auth_token` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_baim_val_d`
--

LOCK TABLES `tb_baim_val_d` WRITE;
/*!40000 ALTER TABLE `tb_baim_val_d` DISABLE KEYS */;
INSERT INTO `tb_baim_val_d` VALUES ('001','COM001','00001','지게차',1,'Y',NULL,NULL,'ㅂㅈㄷㅂㅈㄷ','SYSTEM','2025-09-02 21:09:49','ADMIN','2025-09-07'),('001','COM001','00002','휴게실',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-09-02 21:10:20','ADMIN','2025-09-07'),('001','COM001','00003','제 1도크',5,'Y',NULL,NULL,'123','ADMIN','2025-09-04 21:38:52','ADMIN','2025-09-07'),('001','COM001','00004','상차지',3,'Y',NULL,NULL,'4324','ADMIN','2025-09-04 21:46:19','ADMIN','2025-09-07'),('001','COM001','00005','하차지',4,'Y',NULL,NULL,NULL,'ADMIN','2025-09-07 17:45:22','ADMIN','2025-09-07'),('001','COM001','00006','제 2도크',6,'Y',NULL,NULL,NULL,'ADMIN','2025-09-07 20:09:20','ADMIN','2025-09-07'),('001','COM002','00001','공정',1,'Y',NULL,NULL,NULL,'ADMIN','2026-01-24 17:58:07','ADMIN','2026-01-24'),('001','COM002','00002','설비/기계',2,'Y',NULL,NULL,NULL,'ADMIN','2026-01-24 17:58:07','ADMIN','2026-01-24'),('001','COM002','00003','작업',3,'Y',NULL,NULL,NULL,'ADMIN','2026-01-24 17:58:07','ADMIN','2026-01-24'),('001','COM002','00004','장소',4,'Y',NULL,NULL,NULL,'ADMIN','2026-01-24 17:58:07','ADMIN','2026-01-24'),('001','COM003','00001','안전교육자료',1,'Y',NULL,NULL,'TBM 안전교육자료','ADMIN','2026-01-24 17:58:37','ADMIN','2026-01-24'),('001','COM003','00002','사고사례자료',2,'Y',NULL,NULL,'TBM 사고사례 공유자료','ADMIN','2026-01-24 17:58:37','ADMIN','2026-01-24'),('001','COM003','00003','기타',3,'Y',NULL,NULL,'기타자료','ADMIN','2026-01-24 17:58:37','ADMIN','2026-01-24'),('001','COM004','00001','포스코',1,'Y',NULL,NULL,NULL,'ADMIN','2026-02-08 21:28:52','ADMIN','2026-02-08'),('001','COM004','00002','본부',2,'Y',NULL,NULL,NULL,'ADMIN','2026-02-08 21:29:37','ADMIN','2026-02-09'),('001','COM004','00003','팀',3,'Y',NULL,NULL,NULL,'ADMIN','2026-02-08 21:29:37','ADMIN','2026-02-09'),('001','COM004','00004','파트',4,'Y',NULL,NULL,NULL,'ADMIN','2026-02-08 21:29:37','ADMIN','2026-02-09'),('001','COM004','00005','본부',5,'N',NULL,NULL,NULL,'ADMIN','2026-02-08 21:29:37','ADMIN','2026-02-09'),('001','COM004','00006','팀',6,'N',NULL,NULL,NULL,'ADMIN','2026-02-08 21:29:37','ADMIN','2026-02-09'),('001','COM004','00007','파트',7,'N',NULL,NULL,NULL,'ADMIN','2026-02-08 21:29:37','ADMIN','2026-02-09'),('001','COM004','00008','사업장',8,'N',NULL,NULL,NULL,'ADMIN','2026-02-08 21:44:35','ADMIN','2026-02-09'),('001','COM005','00004','본부장',4,'Y',NULL,NULL,NULL,'ADMIN','2026-02-09 20:08:24','ADMIN','2026-04-12'),('001','COM005','00006','파트장',5,'Y',NULL,NULL,'123','ADMIN','2026-02-10 21:05:53','ADMIN','2026-04-12'),('001','COM005','00007','쫄병',5,'Y',NULL,NULL,NULL,'ADMIN','2026-03-17 21:09:04','ADMIN','2026-04-12'),('001','COM005','00008','팀장',4,'Y',NULL,NULL,NULL,'ADMIN','2026-03-17 22:05:01','ADMIN','2026-04-12'),('001','COM005','99999','일반사용자',999,'Y','system',NULL,'기본권한','ADMIN','2026-02-09 20:08:24','SOON','2026-03-24'),('001','COM005','hr','HR',2,'Y','system',NULL,NULL,'ADMIN','2026-02-09 20:08:24','SOON','2026-03-24'),('001','COM005','master','마스터관리자',1,'Y','system',NULL,'최고권한','ADMIN','2026-02-09 20:08:24','SOON','2026-03-24'),('001','COM005','safe','산업안전관리자',2,'Y','system',NULL,NULL,'ADMIN','2026-02-09 20:08:24','SOON','2026-03-24'),('001','COM005','system','시스템관리자',0,'Y','manage',NULL,'시스템관리권한','ADMIN','2026-02-09 20:08:24','ADMIN','2026-02-09'),('001','COM006','00001','1일',1,'Y',NULL,NULL,NULL,'ADMIN','2026-02-22 19:50:25','ADMIN','2026-02-22'),('001','COM006','00002','2일',2,'Y',NULL,NULL,NULL,'ADMIN','2026-02-22 19:50:25','ADMIN','2026-02-22'),('001','COM006','00003','3일',3,'Y',NULL,NULL,NULL,'ADMIN','2026-02-22 19:50:25','ADMIN','2026-02-22'),('001','COM006','00004','4일',4,'N',NULL,NULL,'TEST','ADMIN','2026-04-04 17:12:47','ADMIN','2026-04-04'),('001','COM007','00001','사원',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL),('001','COM007','00002','주임',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL),('001','COM007','00003','대리',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL),('001','COM007','00004','과장',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL),('001','COM007','00005','차장',5,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL),('001','COM007','00006','부장',6,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL),('001','COM007','00007','이사',7,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 10:21:21',NULL,NULL);
/*!40000 ALTER TABLE `tb_baim_val_d` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_baim_val_m`
--

LOCK TABLES `tb_baim_val_m` WRITE;
/*!40000 ALTER TABLE `tb_baim_val_m` DISABLE KEYS */;
INSERT INTO `tb_baim_val_m` VALUES ('001','COM001','일일점검 구분','Y',NULL,NULL,NULL,'SYSTEM','2025-09-02 21:09:32',NULL,NULL),('001','COM002','위험성평가 구분','Y',NULL,NULL,'위험성평가 공정구분','SYSTEM','2025-09-02 21:09:32',NULL,NULL),('001','COM003','TBM교육타입','Y',NULL,NULL,NULL,'SYSTEM','2025-09-02 21:09:32',NULL,NULL),('001','COM004','조직타입','Y',NULL,NULL,NULL,'SYSTEM','2025-09-02 21:09:32',NULL,NULL),('001','COM005','권한타입','Y',NULL,NULL,NULL,'SYSTEM','2025-09-02 21:09:32',NULL,NULL),('001','COM006','교대근무타입','Y',NULL,NULL,NULL,'SYSTEM','2025-09-02 21:09:32',NULL,NULL),('001','COM007','직급','Y',NULL,NULL,'사용자 직급 코드그룹 (prafta-019-B)','SYSTEM','2026-05-23 10:21:21',NULL,NULL);
/*!40000 ALTER TABLE `tb_baim_val_m` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_chkpt_defect_action`
--

DROP TABLE IF EXISTS `tb_chkpt_defect_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_chkpt_defect_action` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사업장코드',
  `CHKPT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '체크포인트 코드(점검대상)',
  `INSPECT_ITEM_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검항목코드',
  `WORK_DATE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '점검일자(YYYYMMDD) — 불량 발생일',
  `ACTION_DESC` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '조치 상세 내역(불량 처리 내용)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입력자(tb_user.USER_CD)',
  `INSERT_DATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자(tb_user.USER_CD)',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`CMPNY_CD`,`SITE_CD`,`CHKPT_CD`,`INSPECT_ITEM_CD`,`WORK_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='점검 불량 조치(개선) 내역';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_chkpt_defect_action`
--

LOCK TABLES `tb_chkpt_defect_action` WRITE;
/*!40000 ALTER TABLE `tb_chkpt_defect_action` DISABLE KEYS */;
INSERT INTO `tb_chkpt_defect_action` VALUES ('001','00001','000006','DCHK_1_00003','20260531','테스트','20260400010','2026-06-07 12:33:05',NULL,NULL);
/*!40000 ALTER TABLE `tb_chkpt_defect_action` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_chkpt_inspect_answer`
--

LOCK TABLES `tb_chkpt_inspect_answer` WRITE;
/*!40000 ALTER TABLE `tb_chkpt_inspect_answer` DISABLE KEYS */;
INSERT INTO `tb_chkpt_inspect_answer` VALUES ('001','00001','000001','DCHK_1_00001','20251120','N','불량 똥끄미 불량','001-20251120-00008','ADMIN','2025-11-20 22:24:42','ADMIN','2025-11-20 22:24:42'),('001','00001','000001','DCHK_1_00001','20251123','N','20251123\n불량메시지 테스트','001-20251123-00009','ADMIN','2025-11-23 20:12:43','ADMIN','2025-11-23 20:12:43'),('001','00001','000001','DCHK_1_00001','20251125','Y','','','ADMIN','2025-11-25 20:58:21','ADMIN','2025-11-25 20:58:21'),('001','00001','000001','DCHK_1_00002','20251120','Y','','','ADMIN','2025-11-20 22:24:42','ADMIN','2025-11-20 22:24:42'),('001','00001','000001','DCHK_1_00002','20251123','Y','','','ADMIN','2025-11-23 20:12:43','ADMIN','2025-11-23 20:12:43'),('001','00001','000001','DCHK_1_00002','20251125','Y','','','ADMIN','2025-11-25 20:58:21','ADMIN','2025-11-25 20:58:21'),('001','00001','000001','DCHK_1_00003','20251125','Y','','','ADMIN','2025-11-25 20:58:21','ADMIN','2025-11-25 20:58:21'),('001','00001','000006','DCHK_1_00001','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00002','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00003','20260531','N','테르트','001-20260531-00010','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00004','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00005','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00006','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00007','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00008','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00009','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00010','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00011','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00012','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00013','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00014','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00015','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00016','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00017','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00018','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00019','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00020','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00021','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00022','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00023','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00024','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00025','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00026','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00001','000006','DCHK_1_00027','20260531','Y','','','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','00002','000001','DCHK_1_00001','20251125','Y','','','ADMIN','2025-11-25 21:50:58','ADMIN','2025-11-25 21:50:58'),('001','00002','000001','DCHK_1_00002','20251125','Y','','','ADMIN','2025-11-25 21:50:58','ADMIN','2025-11-25 21:50:58'),('001','00002','000001','DCHK_1_00003','20251125','Y','','','ADMIN','2025-11-25 21:50:58','ADMIN','2025-11-25 21:50:58');
/*!40000 ALTER TABLE `tb_chkpt_inspect_answer` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_chkpt_inspect_item`
--

LOCK TABLES `tb_chkpt_inspect_item` WRITE;
/*!40000 ALTER TABLE `tb_chkpt_inspect_item` DISABLE KEYS */;
INSERT INTO `tb_chkpt_inspect_item` VALUES ('001','00001','DCHK_1_00001','지게차 비상등 점등상태 여부',1,'202509','Y','ADMIN','2025-09-14 12:12:12',NULL,NULL),('001','00001','DCHK_1_00002','타이어 펑크상태 확인',2,'202509','Y','ADMIN','2025-09-15 22:16:37',NULL,'2025-09-15 22:18:58'),('001','00001','DCHK_1_00003','지게차 시동걸림상태 확인',3,'202509','Y','ADMIN','2025-11-25 20:57:37','ADMIN','2025-11-25 20:57:37'),('001','00001','DCHK_1_00004','4444',4,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00005','5555',5,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00006','6666',6,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00007','7777',7,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00008','8888',8,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00009','9999',9,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00010','0000',10,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00011','1111',11,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00012','12121212',12,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00013','113131313',13,'202512','Y','ADMIN','2025-12-07 12:32:21','ADMIN','2025-12-07 12:32:21'),('001','00001','DCHK_1_00014','141414',14,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00015','1515151',15,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00016','1616161',16,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00017','1717171',17,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00018','18181',18,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00019','1919',19,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00020','20',20,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00021','21',21,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00022','22',22,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00023','23',23,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00024','24',24,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00025','25',25,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00026','26',26,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00001','DCHK_1_00027','27',27,'202512','Y','ADMIN','2025-12-07 12:32:54','ADMIN','2025-12-07 12:32:54'),('001','00002','DCHK_2_00001','ㅂㅈㄷㅂㅈㄷ',1,'202604','Y','ADMIN','2025-09-15 22:17:03','20260400001','2026-04-13 22:03:36'),('001','00002','DCHK_2_00002','ㅋㅌㅊㅋㅌㅊ',2,'202604','Y','20260400001','2026-04-13 21:56:58','20260400001','2026-04-13 21:56:58'),('001','00002','DCHK_2_00003','ㅁㄴㅇㅁㄴㅇ',3,'202604','Y','20260400001','2026-04-13 21:57:10','20260400001','2026-04-13 21:57:20'),('001','00002','DCHK_2_00004','123123',4,'202604','Y','20260400001','2026-04-13 22:03:46','20260400001','2026-04-13 22:03:46'),('001','00004','DCHK_4_00001','123123',1,'202604','Y','20260400001','2026-04-13 22:03:54','20260400001','2026-04-13 22:04:15'),('001','00004','DCHK_4_00002','ㅂㅈㄷㅂㅈㄷ',2,'202604','Y','20260400001','2026-04-13 22:04:01','20260400001','2026-04-13 22:13:09'),('001','00004','DCHK_4_00003','1111111',3,'202604','Y','20260400001','2026-04-13 22:04:15','20260400001','2026-04-13 22:04:15'),('001','00004','DCHK_4_00004','222222222222',4,'202604','Y','20260400001','2026-04-13 22:04:15','20260400001','2026-04-13 22:04:15'),('001','00004','DCHK_4_00005','12312',0,'202604','Y','20260400001','2026-04-13 22:07:40','20260400001','2026-04-13 22:07:40'),('001','00004','DCHK_4_00006','123',2,'202604','Y','20260400001','2026-04-13 22:13:09','20260400001','2026-04-13 22:13:09'),('001','00005','DCHK_5_00001','ㅂㅂㅂㅂㅂㅂ',1,'202604','Y','20260400001','2026-04-13 22:13:21','20260400001','2026-04-13 22:13:21'),('001','00005','DCHK_5_00002','ㅈㅈㅈㅈㅈㅈ',2,'202604','N','20260400001','2026-04-13 22:13:24','20260400001','2026-04-13 22:13:41'),('001','00005','DCHK_5_00003','ㄷㄷㄷㄷㄷㄷ',3,'202604','N','20260400001','2026-04-13 22:13:34','20260400001','2026-04-13 22:13:41'),('001','00006','DCHK_6_00001','도크 점검항목 테스트',1,'202604','N','ADMIN','2026-04-05 20:48:35','ADMIN','2026-04-05 20:48:52');
/*!40000 ALTER TABLE `tb_chkpt_inspect_item` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_chkpt_type_mgmt`
--

LOCK TABLES `tb_chkpt_type_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_chkpt_type_mgmt` DISABLE KEYS */;
INSERT INTO `tb_chkpt_type_mgmt` VALUES ('001','00001','00001','000006','중곡사업장_지게차','테스트11','20260400013','Y','20260400001','2026-04-13 21:56:08','20260400010','2026-05-31 19:45:10');
/*!40000 ALTER TABLE `tb_chkpt_type_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_cmm_seq`
--

LOCK TABLES `tb_cmm_seq` WRITE;
/*!40000 ALTER TABLE `tb_cmm_seq` DISABLE KEYS */;
INSERT INTO `tb_cmm_seq` VALUES ('001','APRV_PRESET_ID',6,99999),('001','ASSESSMENT',11,99999),('001','ATTD_CLOSE_HIST',4,99999),('001','ATTD_GPS_ID',2,99999),('001','ATTD_HIST_ID',87,99999),('001','ATTD_ID',69,99999),('001','ATTD_REQ_ID',28,99999),('001','AUDIT_LOG_ID',11,99999),('001','CHKPT_CD',6,999999),('001','COM001',6,99999),('001','COM001_00001',27,99999),('001','COM001_00002',1,99999),('001','COM001-00002',4,99999),('001','COM001-00004',6,99999),('001','COM001-00005',3,99999),('001','COM001-00006',1,99999),('001','COM002',4,99999),('001','COM003',3,99999),('001','COM004',8,99999),('001','COM005',8,99999),('001','COM006',4,99999),('001','DAILY_USER',7,99999),('001','DEVICE_LOGIN_NO',6,99999),('001','FILE_MGMT_CD-001',10,99999),('001','FILE_MGMT_CD-002',25,99999),('001','FILE_MGMT_CD-003',36,99999),('001','FILE_MGMT_CD-005',2,99999),('001','FILE_MGMT_CD-100',20,99999),('001','GPS_ID',4,99999),('001','HAZARD_CD',22,99999),('001','HIRE_HIST_ID',44,99999),('001','HOLIDAY_ID',222,99999),('001','HOLIDAY_RULE_ID',5,99999),('001','LEAVE_CD',21,99999),('001','LEAVE_GRANT_ID',243,99999),('001','LEAVE_POLICY_HIST',7,99999),('001','LEAVE_USE_ID',3,99999),('001','MTRL_CD',17,99999),('001','MTRL_ITEM_CD',47,99999),('001','NOTI_OUTBOX_ID',2,99999),('001','NOTICE_PIN_LOCK',2,99999),('001','OT_ID',21,99999),('001','RISK_TYPE_CD',25,99999),('001','SCH_CD-00001',7,99999),('001','SERVICE_CREDIT_ID',1,99999),('001','SHIFT_CD-00001',9,99999),('001','SITE_CD',8,99999),('001','TBM_SESSION_CD',2,99999),('001','TEAM_SHIFT_ID',20,99999),('001','USER_CD',22,99999),('001','USER_UPLOAD_JOB_ID',3,99999);
/*!40000 ALTER TABLE `tb_cmm_seq` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_cmpny`
--

LOCK TABLES `tb_cmpny` WRITE;
/*!40000 ALTER TABLE `tb_cmpny` DISABLE KEYS */;
INSERT INTO `tb_cmpny` VALUES ('001','JPC','123456789','서울시 광진구 중곡동 199-1','104동 601호','123-456','Y','Y','SYSTEM','2025-07-22 22:57:44','SYSTEM','2025-07-22 22:57:44');
/*!40000 ALTER TABLE `tb_cmpny` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_daily_link_mgmt`
--

LOCK TABLES `tb_daily_link_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_daily_link_mgmt` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_daily_link_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_daily_user`
--

LOCK TABLES `tb_daily_user` WRITE;
/*!40000 ALTER TABLE `tb_daily_user` DISABLE KEYS */;
INSERT INTO `tb_daily_user` VALUES ('001','00001','D2026042700006','D2026042700006','윤순기','$2a$12$DhXYPNYNMUYDnezgOZKvAOysJK9fFSCRVKCx9iFBpZoanV.iLN2.a','v1.AZgCVmvC6w1z_4qT-XpSBMIa0yNglHF6OvPBeNpfJe1V7BQ1tf8cmQ','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','5257','02','Y','01','20260427',NULL,0,'N',NULL,NULL,'20260400001','2026-04-27 22:31:43','20260400001','2026-04-27 22:31:43'),('001','00001','D2026051800007','D2026051800007','홍길동2','$2a$12$HPCGGnuwDdVyNCCCryilheB0GmvoBhS6FZsPC5BTO84.TifKzLXDK','v1.AaGJruM_UOLPzKuNeuXL0Xksc99jVzEmjxLLE4Uj5LfwrSaonE12Qg','np74ksQBo6BFhxY72VG0hFaCCIMRbEZeofAU8WSvfbE','5679','02','Y','01','20260518',NULL,0,'N',NULL,NULL,'20260400010','2026-05-18 21:02:26','20260400010','2026-05-18 21:02:26');
/*!40000 ALTER TABLE `tb_daily_user` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_daily_user_link_policy`
--

LOCK TABLES `tb_daily_user_link_policy` WRITE;
/*!40000 ALTER TABLE `tb_daily_user_link_policy` DISABLE KEYS */;
INSERT INTO `tb_daily_user_link_policy` VALUES ('001','00001','N',15,'ADMIN','2026-04-04 21:40:14','20260400001','2026-04-27 22:15:41'),('001','00002','Y',8,'ADMIN','2026-01-18 14:12:55','ADMIN','2026-01-21 18:52:37'),('001','00003','Y',10,'ADMIN','2026-01-18 14:12:55','ADMIN','2026-01-21 18:52:37'),('001','00004','N',20,'ADMIN','2026-01-19 21:09:59','ADMIN','2026-01-21 18:52:37');
/*!40000 ALTER TABLE `tb_daily_user_link_policy` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_daily_user_slot`
--

LOCK TABLES `tb_daily_user_slot` WRITE;
/*!40000 ALTER TABLE `tb_daily_user_slot` DISABLE KEYS */;
INSERT INTO `tb_daily_user_slot` VALUES ('001','00001','0','02','N','Y','D2026051800007','02','20260400001','2026-04-27 22:15:41','20260400010','2026-05-18 21:02:26'),('001','00001','1','02','N','Y','D2026042700006','02','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:31:43'),('001','00001','10','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','11','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','12','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','13','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','14','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','2','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','3','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','4','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','5','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','6','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','7','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','8','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41'),('001','00001','9','01','N','Y','','01','20260400001','2026-04-27 22:15:41','20260400001','2026-04-27 22:15:41');
/*!40000 ALTER TABLE `tb_daily_user_slot` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_daily_user_slot_his`
--

LOCK TABLES `tb_daily_user_slot_his` WRITE;
/*!40000 ALTER TABLE `tb_daily_user_slot_his` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_daily_user_slot_his` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_del_user`
--

LOCK TABLES `tb_del_user` WRITE;
/*!40000 ALTER TABLE `tb_del_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_del_user` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_file_info`
--

LOCK TABLES `tb_file_info` WRITE;
/*!40000 ALTER TABLE `tb_file_info` DISABLE KEYS */;
INSERT INTO `tb_file_info` VALUES ('001','001-20251120-00008',NULL,'001','\\uploads\\001\\20251120\\00001\\001','.jpg','ADMIN','2025-11-20 22:24:42','ADMIN','2025-11-20 22:24:42'),('001','001-20251123-00009',NULL,'001','\\uploads\\001\\20251123\\00001\\001','.jpg','ADMIN','2025-11-23 20:12:43','ADMIN','2025-11-23 20:12:43'),('001','001-20260531-00010','DCHK_1_00003_2026-05-31T104423826Z_image1322116029694342776.jpg','001','\\uploads\\001\\20260531\\00001\\001','.jpg','20260400013','2026-05-31 19:44:23','20260400013','2026-05-31 19:44:23'),('001','002-20251225-00005',NULL,'002','\\uploads\\001\\20251225\\00001\\002','.jpg','ADMIN','2025-12-25 15:09:25','ADMIN','2025-12-25 15:09:25'),('001','002-20251225-00006',NULL,'002','\\uploads\\001\\20251225\\00001\\002','.jpg','ADMIN','2025-12-25 15:09:53','ADMIN','2025-12-25 15:09:53'),('001','002-20251225-00007',NULL,'002','\\uploads\\001\\20251225\\00001\\002','.jpg','ADMIN','2025-12-25 15:11:10','ADMIN','2025-12-25 15:11:10'),('001','002-20251225-00008',NULL,'002','\\uploads\\001\\20251225\\00001\\002','.jpg','ADMIN','2025-12-25 15:12:29','ADMIN','2025-12-25 15:12:29'),('001','002-20251226-00009',NULL,'002','\\uploads\\001\\20251226\\00001\\002','.jpg','ADMIN','2025-12-26 16:32:26','ADMIN','2025-12-26 16:32:26'),('001','002-20251226-00010',NULL,'002','\\uploads\\001\\20251226\\00001\\002','.jpg','ADMIN','2025-12-26 16:39:31','ADMIN','2025-12-26 16:39:31'),('001','002-20251226-00011',NULL,'002','\\uploads\\001\\20251226\\00001\\002','.jpg','ADMIN','2025-12-26 16:40:00','ADMIN','2025-12-26 16:40:00'),('001','002-20251226-00012',NULL,'002','\\uploads\\001\\20251226\\00001\\002','.jpg','ADMIN','2025-12-26 16:40:11','ADMIN','2025-12-26 16:40:11'),('001','002-20251226-00013',NULL,'002','\\uploads\\001\\20251226\\00001\\002','.jpg','ADMIN','2025-12-26 16:41:54','ADMIN','2025-12-26 16:41:54'),('001','002-20260101-00018',NULL,'002','\\uploads\\001\\20260101\\00001\\002','.jpg','ADMIN','2026-01-01 14:43:41','ADMIN','2026-01-01 14:43:41'),('001','002-20260104-00019',NULL,'002','\\uploads\\001\\20260104\\00001\\002','.png','ADMIN','2026-01-04 21:24:32','ADMIN','2026-01-04 21:24:32'),('001','002-20260104-00020',NULL,'002','\\uploads\\001\\20260104\\00001\\002','.ico','ADMIN','2026-01-04 21:26:58','ADMIN','2026-01-04 21:26:58'),('001','002-20260104-00021',NULL,'002','\\uploads\\001\\20260104\\00001\\002','.ico','ADMIN','2026-01-04 21:30:47','ADMIN','2026-01-04 21:30:47'),('001','002-20260106-00022',NULL,'002','\\uploads\\001\\20260106\\00001\\002','.png','ADMIN','2026-01-06 21:47:20','ADMIN','2026-01-06 21:47:20'),('001','002-20260131-00023',NULL,'002','\\uploads\\001\\20260131\\003','.ico','ADMIN','2026-01-31 18:04:42','ADMIN','2026-01-31 18:04:42'),('001','002-20260413-00025','뮤즈텍.png','002','\\uploads\\001\\20260413\\00001\\002','.png','20260400001','2026-04-13 21:44:30','20260400001','2026-04-13 21:44:30'),('001','003-20260409-00030','KakaoTalk_20260211_180153358.jpg','003','\\uploads\\001\\20260409\\003','.jpg','ADMIN','2026-04-09 18:48:35','ADMIN','2026-04-09 18:48:35'),('001','003-20260409-00031','KakaoTalk_20250421_224522840.mp4','003','\\uploads\\001\\20260409\\003','.mp4','ADMIN','2026-04-09 18:48:35','ADMIN','2026-04-09 18:48:35'),('001','003-20260409-00032','KakaoTalk_20260211_180153358.jpg','003','\\uploads\\001\\20260409\\003','.jpg','ADMIN','2026-04-09 18:49:34','ADMIN','2026-04-09 18:49:34'),('001','003-20260409-00033','KakaoTalk_20260211_180153358.jpg','003','\\uploads\\001\\20260409\\003','.jpg','ADMIN','2026-04-09 18:49:34','ADMIN','2026-04-09 18:49:34'),('001','003-20260413-00034','KakaoTalk_20260211_180153358.jpg','003','\\uploads\\001\\20260413\\003','.jpg','20260400001','2026-04-13 21:36:10','20260400001','2026-04-13 21:36:10'),('001','003-20260413-00035','KakaoTalk_20250421_224522840.mp4','003','\\uploads\\001\\20260413\\003','.mp4','20260400001','2026-04-13 21:36:10','20260400001','2026-04-13 21:36:10'),('001','003-20260413-00036','뮤즈텍.png','003','\\uploads\\001\\20260413\\003','.png','20260400001','2026-04-13 21:37:33','20260400001','2026-04-13 21:37:33'),('001','005-20260606-00001','prafta_logo_640x160.png','005','\\uploads\\001\\20260606\\00001\\005','.png','20260400010','2026-06-06 17:52:49','20260400010','2026-06-06 17:52:49'),('001','005-20260606-00002','safenote_logo.png','005','\\uploads\\001\\20260606\\00001\\005','.png','20260400010','2026-06-06 17:52:49','20260400010','2026-06-06 17:52:49');
/*!40000 ALTER TABLE `tb_file_info` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_holiday`
--

LOCK TABLES `tb_holiday` WRITE;
/*!40000 ALTER TABLE `tb_holiday` DISABLE KEYS */;
INSERT INTO `tb_holiday` VALUES ('001','202600011','지정근무1','2026-02-23','02','Y','ADMIN','2026-02-26 21:18:41','ADMIN','2026-02-26 21:18:41'),('001','202600075','지정휴무1','2026-02-28','02','Y','ADMIN','2026-02-28 21:02:51','ADMIN','2026-02-28 21:02:51'),('001','202600181','1월1일','2026-01-01','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600182','설날','2026-02-16','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600183','설날','2026-02-17','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600184','설날','2026-02-18','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600185','삼일절','2026-03-01','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600186','대체공휴일(삼일절)','2026-03-02','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600187','어린이날','2026-05-05','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600188','부처님오신날','2026-05-24','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600189','대체공휴일(부처님오신날)','2026-05-25','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600190','전국동시지방선거','2026-06-03','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600191','현충일','2026-06-06','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600192','제헌절','2026-07-17','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600193','광복절','2026-08-15','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600194','대체공휴일(광복절)','2026-08-17','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600195','추석','2026-09-24','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600196','추석','2026-09-25','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600197','추석','2026-09-26','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600198','개천절','2026-10-03','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600199','대체공휴일(개천절)','2026-10-05','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600200','한글날','2026-10-09','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600201','기독탄신일','2026-12-25','01','Y','SYSTEM','2026-02-28 21:20:00','SYSTEM','2026-02-28 21:26:00'),('001','202600202','1월1일','2027-01-01','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600203','설날','2027-02-06','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600204','설날','2027-02-07','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600205','설날','2027-02-08','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600206','대체공휴일(설날)','2027-02-09','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600207','삼일절','2027-03-01','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600208','어린이날','2027-05-05','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600209','부처님오신날','2027-05-13','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600210','현충일','2027-06-06','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600211','제헌절','2027-07-17','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600212','광복절','2027-08-15','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600213','대체공휴일(광복절)','2027-08-16','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600214','추석','2027-09-14','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600215','추석','2027-09-15','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600216','추석','2027-09-16','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600217','개천절','2027-10-03','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600218','대체공휴일(개천절)','2027-10-04','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600219','한글날','2027-10-09','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600220','대체공휴일(한글날)','2027-10-11','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600221','기독탄신일','2027-12-25','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00'),('001','202600222','대체공휴일(기독탄신일)','2027-12-27','01','Y','SYSTEM','2026-02-28 21:21:00','SYSTEM','2026-02-28 21:24:00');
/*!40000 ALTER TABLE `tb_holiday` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_holiday_rule`
--

LOCK TABLES `tb_holiday_rule` WRITE;
/*!40000 ALTER TABLE `tb_holiday_rule` DISABLE KEYS */;
INSERT INTO `tb_holiday_rule` VALUES ('001','202600004','반복근무1','02','23','03','Y','ADMIN','2026-02-26 21:25:19','ADMIN','2026-02-26 21:25:19'),('001','202600005','휴무1','04','10','03','Y','ADMIN','2026-04-10 19:35:39','ADMIN','2026-04-10 19:35:39');
/*!40000 ALTER TABLE `tb_holiday_rule` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_leave_policy`
--

LOCK TABLES `tb_leave_policy` WRITE;
/*!40000 ALTER TABLE `tb_leave_policy` DISABLE KEYS */;
INSERT INTO `tb_leave_policy` VALUES (1,'001','CUSTOM','HIRE_DATE',NULL,NULL,'MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,12,'N','N','N','20260522','20260400010','2026-05-21 16:22:24','20260400010','2026-05-21 16:22:28'),(2,'001','CUSTOM','HIRE_DATE',NULL,NULL,'MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,24,'Y','N','N','20260522','20260400010','2026-05-21 16:22:28','20260400010','2026-05-24 18:22:40'),(3,'001','CUSTOM','FISCAL_YEAR','01','01','MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,24,'Y','N','N','20260525','20260400010','2026-05-24 18:22:40','20260400010','2026-05-24 18:23:24'),(4,'001','CUSTOM','HIRE_DATE',NULL,NULL,'MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,24,'Y','N','N','20260525','20260400010','2026-05-24 18:23:24','20260400010','2026-05-25 18:39:12'),(5,'001','CUSTOM','HIRE_DATE',NULL,NULL,'MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,12,'Y','N','N','20260526','20260400010','2026-05-25 18:39:12','20260400010','2026-05-25 19:52:35'),(6,'001','CUSTOM','HIRE_DATE',NULL,NULL,'MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,12,'Y','N','N','20260526','20260400010','2026-05-25 19:52:35','20260400010','2026-06-03 21:15:28'),(7,'001','CUSTOM','HIRE_DATE',NULL,NULL,'MONTHLY_ONLY','N','CEIL','LEGAL',3,2,25,12,'Y','N','Y','20260604','20260400010','2026-06-03 21:15:28','20260400010','2026-06-03 21:15:28');
/*!40000 ALTER TABLE `tb_leave_policy` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_leave_policy_history`
--

LOCK TABLES `tb_leave_policy_history` WRITE;
/*!40000 ALTER TABLE `tb_leave_policy_history` DISABLE KEYS */;
INSERT INTO `tb_leave_policy_history` VALUES ('00001','001',1,'CREATE',NULL,'{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"policySeq\": 1, \"allowHourly\": \"N\", \"allowFullDay\": \"Y\", \"allowHalfDay\": \"Y\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260522\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"allowQuarterDay\": \"Y\", \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"maxDailyRequest\": 3, \"axis7UsePromotion\": \"N\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','test','{\"axesChanged\": [\"AXIS1_GRANT_BASE\", \"AXIS3_FIRST_YEAR_METHOD\", \"AXIS5_TENURE_MODE\", \"AXIS5_MAX_DAYS\", \"AXIS6_VALIDITY_MONTHS\", \"AXIS7_USE_PROMOTION\"], \"previewedAt\": \"2026-05-21T16:22:24.1113759\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-05-21 16:22:24'),('00002','001',2,'UPDATE','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"policySeq\": 1, \"allowHourly\": null, \"allowFullDay\": null, \"allowHalfDay\": null, \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260522\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"allowQuarterDay\": null, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"maxDailyRequest\": null, \"axis7UsePromotion\": \"N\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"policySeq\": 2, \"allowHourly\": \"N\", \"allowFullDay\": \"Y\", \"allowHalfDay\": \"Y\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260522\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"allowQuarterDay\": \"Y\", \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"maxDailyRequest\": 3, \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 24, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','stes','{\"axesChanged\": [\"AXIS6_VALIDITY_MONTHS\", \"AXIS7_USE_PROMOTION\"], \"previewedAt\": \"2026-05-21T16:22:28.8765847\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-05-21 16:22:28'),('00003','001',3,'UPDATE','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 2, \"usageUnit\": null, \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260522\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 24, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 3, \"usageUnit\": \"FULL_DAY\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260525\", \"axis5Interval\": 2, \"axis1GrantBase\": \"FISCAL_YEAR\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": \"01\", \"axis2FiscalStartMm\": \"01\", \"axis6ValidityMonths\": 24, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','ㅅㄷㄴㅅ','{\"axesChanged\": [\"AXIS1_GRANT_BASE\", \"AXIS2_FISCAL_START\"], \"previewedAt\": \"2026-05-24T18:22:40.5881912\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-05-24 18:22:40'),('00004','001',4,'UPDATE','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 3, \"usageUnit\": null, \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260525\", \"axis5Interval\": 2, \"axis1GrantBase\": \"FISCAL_YEAR\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": \"01\", \"axis2FiscalStartMm\": \"01\", \"axis6ValidityMonths\": 24, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 4, \"usageUnit\": \"FULL_DAY\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260525\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 24, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','ㅂㅈㄷ','{\"axesChanged\": [\"AXIS1_GRANT_BASE\", \"AXIS2_FISCAL_START\"], \"previewedAt\": \"2026-05-24T18:23:24.9027877\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-05-24 18:23:24'),('00005','001',5,'UPDATE','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 4, \"usageUnit\": null, \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260525\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 24, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 5, \"usageUnit\": \"FULL_DAY\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260526\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','asd','{\"axesChanged\": [\"AXIS6_VALIDITY_MONTHS\"], \"previewedAt\": \"2026-05-25T18:39:12.147655\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-05-25 18:39:12'),('00006','001',6,'UPDATE','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 5, \"usageUnit\": null, \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260526\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 6, \"usageUnit\": \"FULL_DAY\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260526\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','ㅂㅈㄷㅂㅈㄷ','{\"axesChanged\": [], \"previewedAt\": \"2026-05-25T19:52:35.6637594\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-05-25 19:52:35'),('00007','001',7,'UPDATE','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 6, \"usageUnit\": null, \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260526\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','{\"useYn\": \"Y\", \"cmpnyCd\": \"001\", \"aprvUseYn\": \"N\", \"policySeq\": 7, \"usageUnit\": \"MIN_30\", \"axis5MaxDays\": 25, \"policyPreset\": \"CUSTOM\", \"applyFromDate\": \"20260604\", \"axis5Interval\": 2, \"axis1GrantBase\": \"HIRE_DATE\", \"axis5StartYear\": 3, \"axis3PregrantYn\": \"N\", \"axis5TenureMode\": \"LEGAL\", \"axis7UsePromotion\": \"Y\", \"axis2FiscalStartDd\": null, \"axis2FiscalStartMm\": null, \"axis6ValidityMonths\": 12, \"axis3FirstYearMethod\": \"MONTHLY_ONLY\", \"axis4ProrateRounding\": \"CEIL\"}','연차 부여 정책 수정','{\"axesChanged\": [], \"previewedAt\": \"2026-06-03T21:15:28.441368\", \"affectedUserCount\": 5, \"estimatedAdditionalDays\": 0}','20260400010','2026-06-03 21:15:28');
/*!40000 ALTER TABLE `tb_leave_policy_history` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_leave_refusal_log`
--

LOCK TABLES `tb_leave_refusal_log` WRITE;
/*!40000 ALTER TABLE `tb_leave_refusal_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_leave_refusal_log` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_leave_type_mgmt`
--

LOCK TABLES `tb_leave_type_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_leave_type_mgmt` DISABLE KEYS */;
INSERT INTO `tb_leave_type_mgmt` VALUES ('001','00013','LEAVE_SUMMER','하계휴가(4일)','01',NULL,'01','01','Y','N','하계휴가(4일)',4,'00','02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'ADMIN','2026-03-17 22:10:31','ADMIN','2026-03-17 22:10:31'),('001','00016','LEAVE_01','LEAVE_01','01',NULL,'01','01','Y','N','LEAVE_01',90,'00','01',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Y','Y','qweqwe','ADMIN','2026-03-18 20:18:12','ADMIN','2026-03-18 20:18:12'),('001','00017','LEAVE_ADMIN_AUTH','LEAVE_ADMIN_AUTH','02','01','01','01','Y','N','LEAVE_ADMIN_AUTH',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'01',2,NULL,'Y','Y','LEAVE_ADMIN_AUTH','ADMIN','2026-03-18 20:18:30','ADMIN','2026-03-18 20:18:30'),('001','00018','LEAVE_ADMIN_MANUAL','LEAVE_ADMIN_MANUAL','02','02','01','01','Y','N','LEAVE_ADMIN_MANUAL',NULL,NULL,NULL,NULL,NULL,1,'01',NULL,NULL,NULL,NULL,NULL,'Y','Y','LEAVE_ADMIN_MANUAL','ADMIN','2026-03-18 20:22:19','ADMIN','2026-03-18 20:22:19'),('001','SYS_ANNUAL','SYS_ANNUAL','연차','02','01','01','01','Y','Y',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'SYSTEM','2026-05-20 22:59:47',NULL,NULL),('001','SYS_BIRTHDAY','SYS_BIRTHDAY','생일 안식휴가','02','01','01','02','Y','Y',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'SYSTEM','2026-05-20 23:00:14',NULL,NULL),('001','SYS_MONTHLY','SYS_MONTHLY','월차','02','01','01','01','Y','Y',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'SYSTEM','2026-05-20 22:59:47',NULL,NULL),('001','SYS_PREGRANT','SYS_PREGRANT','일괄선부여 연차','02','01','01','01','Y','Y',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'SYSTEM','2026-05-20 23:00:14',NULL,NULL),('001','SYS_PROMOTION','SYS_PROMOTION','사용촉진 연차','02','01','01','01','Y','Y',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'SYSTEM','2026-05-20 22:59:47',NULL,NULL),('001','SYS_TENURE_BONUS','SYS_TENURE_BONUS','근속가산 연차','02','01','01','01','Y','Y',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'N','N',NULL,'SYSTEM','2026-05-20 22:59:47',NULL,NULL);
/*!40000 ALTER TABLE `tb_leave_type_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_leave_usage_policy`
--

LOCK TABLES `tb_leave_usage_policy` WRITE;
/*!40000 ALTER TABLE `tb_leave_usage_policy` DISABLE KEYS */;
INSERT INTO `tb_leave_usage_policy` VALUES (1,'001','Y','Y','N','N','N','FULL_DAY','20260400010','2026-05-21 16:22:24','20260400010','2026-05-21 16:22:24'),(2,'001','Y','Y','N','N','N','FULL_DAY','20260400010','2026-05-21 16:22:28','20260400010','2026-05-21 16:22:28'),(3,'001','Y','Y','N','N','N','FULL_DAY','20260400010','2026-05-24 18:22:40','20260400010','2026-05-24 18:22:40'),(4,'001','Y','Y','N','N','N','FULL_DAY','20260400010','2026-05-24 18:23:24','20260400010','2026-05-24 18:23:24'),(5,'001','Y','Y','N','N','N','FULL_DAY','20260400010','2026-05-25 18:39:12','20260400010','2026-05-25 18:39:12'),(6,'001','Y','Y','N','N','N','FULL_DAY','20260400010','2026-05-25 19:52:35','20260400010','2026-05-25 19:52:35'),(7,'001','Y','Y','N','N','N','MIN_30','20260400010','2026-06-03 21:15:28','20260400010','2026-06-03 21:15:28');
/*!40000 ALTER TABLE `tb_leave_usage_policy` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_near_miss`
--

LOCK TABLES `tb_near_miss` WRITE;
/*!40000 ALTER TABLE `tb_near_miss` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_near_miss` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_noti_outbox`
--

LOCK TABLES `tb_noti_outbox` WRITE;
/*!40000 ALTER TABLE `tb_noti_outbox` DISABLE KEYS */;
INSERT INTO `tb_noti_outbox` VALUES ('N2026052700001','001',NULL,'20260400012','LEAVE_GRANT_RECALLED','PUSH','부여 연차 회수 안내','관리자가 부여한 연차 1일이 회수되었습니다.','{\"reason\": \"ㅂㅈㄷ\", \"grantId\": \"G2026052700242\", \"leaveCd\": \"00018\", \"grantDays\": 1.0}','PENDING',NULL,0,NULL,'RECALL_G2026052700242','N','20260400010','2026-05-27 19:00:46',NULL,NULL),('N2026060300002','001','00001','20260400010','LEAVE_USED_NO_APRV','PUSH','[연차 사용 통보]','윤순기님이 2026-06-04 10:00~11:00 시간차 연차를 사용했습니다.','{\"type\": \"LEAVE_USED_NO_APRV\", \"leaveId\": \"LV2026060300003\", \"workYmd\": \"20260604\", \"applicantUserCd\": \"20260400013\"}','PENDING',NULL,0,NULL,'LV_USED_LV2026060300003_20260400010','N','20260400013','2026-06-03 22:39:42',NULL,NULL);
/*!40000 ALTER TABLE `tb_noti_outbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_notice`
--

DROP TABLE IF EXISTS `tb_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_notice` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `NOTICE_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공지ID (회사별 채번: N + YYYYMMDD + 3자리 SEQ)',
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제목',
  `CONTENT` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '내용(리치텍스트 가능)',
  `EDIT_PWD` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '수정 비밀번호 BCrypt 해시(평문 저장 금지)',
  `TARGET_SCOPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 스코프(컬럼 상수) ALL:전사 SITE:사업장 NODE:사업장+노드. 상세 대상은 tb_notice_target',
  `INCLUDE_DAILY_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '일용직 포함 여부 Y/N',
  `POPUP_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '로그인 시 팝업 여부 Y/N',
  `POPUP_FROM_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '팝업 시작일 YYYYMMDD (POPUP_YN=Y 시 필수)',
  `POPUP_TO_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '팝업 종료일 YYYYMMDD (POPUP_YN=Y 시 필수)',
  `PIN_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '상단 고정 여부 Y/N',
  `PIN_ORDER` int DEFAULT NULL COMMENT '고정 순번(1부터, PIN_YN=Y 시만). 서버 정규화(요청서 §5)',
  `DEL_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT '삭제 여부 Y/N (논리삭제)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자 USER_CD',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자 USER_CD',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시(사용자별 "수정됨" 뱃지 판정 기준, 요청서 §7)',
  PRIMARY KEY (`CMPNY_CD`,`NOTICE_ID`),
  KEY `IX_TB_NOTICE_LIST` (`CMPNY_CD`,`DEL_YN`,`PIN_YN`,`PIN_ORDER`),
  KEY `IX_TB_NOTICE_POPUP` (`CMPNY_CD`,`POPUP_YN`,`POPUP_FROM_YMD`,`POPUP_TO_YMD`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지사항 마스터';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_notice`
--

LOCK TABLES `tb_notice` WRITE;
/*!40000 ALTER TABLE `tb_notice` DISABLE KEYS */;
INSERT INTO `tb_notice` VALUES ('001','N20260606001','공지사항 테스트','공지사항 테스트\n\n2026-06-06','$2a$12$CQ1CWQG0lJarHYyvmYqQDuGJ1gx/38PXBwzLFc7gfAn6v4a30SkMm','NODE','Y','N','','','Y',1,'N','20260400010','2026-06-06 17:52:50','20260400010','2026-06-06 23:36:57');
/*!40000 ALTER TABLE `tb_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_notice_file`
--

DROP TABLE IF EXISTS `tb_notice_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_notice_file` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `NOTICE_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공지ID',
  `FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'tb_file_info 파일관리코드(FK)',
  `SORT_IDX` int NOT NULL DEFAULT '1' COMMENT '첨부 정렬순서',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`CMPNY_CD`,`NOTICE_ID`,`FILE_MGMT_CD`),
  KEY `IX_TB_NOTICE_FILE_NOTICE` (`CMPNY_CD`,`NOTICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지 첨부 매핑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_notice_file`
--

LOCK TABLES `tb_notice_file` WRITE;
/*!40000 ALTER TABLE `tb_notice_file` DISABLE KEYS */;
INSERT INTO `tb_notice_file` VALUES ('001','N20260606001','005-20260606-00001',1,'20260400010','2026-06-06 23:36:57'),('001','N20260606001','005-20260606-00002',2,'20260400010','2026-06-06 23:36:57');
/*!40000 ALTER TABLE `tb_notice_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_notice_target`
--

DROP TABLE IF EXISTS `tb_notice_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_notice_target` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `NOTICE_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공지ID',
  `TARGET_SEQ` int NOT NULL COMMENT '대상 순번(1부터)',
  `SITE_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '대상 사업장코드 (SITE/NODE 공통 필수)',
  `NODE_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '대상 노드코드 (NODE 일 때만, NULL=사업장 전체)',
  `INCLUDE_DESCENDANTS_YN` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y' COMMENT '하위(자손) 노드 포함 여부 Y/N (NODE 일 때만 의미)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (`CMPNY_CD`,`NOTICE_ID`,`TARGET_SEQ`),
  KEY `IX_TB_NOTICE_TARGET_MATCH` (`CMPNY_CD`,`SITE_CD`,`NODE_CD`),
  KEY `IX_TB_NOTICE_TARGET_NOTICE` (`CMPNY_CD`,`NOTICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지 대상 매핑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_notice_target`
--

LOCK TABLES `tb_notice_target` WRITE;
/*!40000 ALTER TABLE `tb_notice_target` DISABLE KEYS */;
INSERT INTO `tb_notice_target` VALUES ('001','N20260606001',1,'00001','n1','Y','20260400010','2026-06-06 23:36:57');
/*!40000 ALTER TABLE `tb_notice_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_notice_user_ack`
--

DROP TABLE IF EXISTS `tb_notice_user_ack`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_notice_user_ack` (
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사코드',
  `NOTICE_ID` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '공지ID',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자코드(정규/일용 공통)',
  `ACK_TYPE` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '확인 유형(컬럼 상수) CONFIRMED:영구확인 SNOOZED:한시숨김 READ:열람마킹(LAST_READ_DATE 갱신용 신규행 기본값, 확인/숨김 아님)',
  `SNOOZE_UNTIL_YMD` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '숨김 만료일 YYYYMMDD (ACK_TYPE=SNOOZED 시, 처리일+7일)',
  `LAST_READ_DATE` datetime DEFAULT NULL COMMENT '마지막 열람 일시("수정됨" 뱃지 판정, 요청서 §7)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`CMPNY_CD`,`NOTICE_ID`,`USER_CD`),
  KEY `IX_TB_NOTICE_ACK_USER` (`CMPNY_CD`,`USER_CD`,`NOTICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지 사용자 확인/숨김 이력';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_notice_user_ack`
--

LOCK TABLES `tb_notice_user_ack` WRITE;
/*!40000 ALTER TABLE `tb_notice_user_ack` DISABLE KEYS */;
INSERT INTO `tb_notice_user_ack` VALUES ('001','N20260606001','20260400010','SNOOZED','20260613','2026-06-06 23:37:13','20260400010','2026-06-06 18:03:42','20260400010','2026-06-06 23:37:13'),('001','N20260606001','20260400013','READ',NULL,'2026-06-06 23:36:42','20260400013','2026-06-06 17:52:59','20260400013','2026-06-06 23:36:42');
/*!40000 ALTER TABLE `tb_notice_user_ack` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_risk_assessment`
--

LOCK TABLES `tb_risk_assessment` WRITE;
/*!40000 ALTER TABLE `tb_risk_assessment` DISABLE KEYS */;
INSERT INTO `tb_risk_assessment` VALUES ('001','00001','00001','00025','00013','251200003','','003',3,4,'12',NULL,'002-20251226-00010','ADMIN','2025-12-26 16:39:31','20261113','테스트 임시조치 내용 테스트 임시조치 내용\r\n테스트 임시조치 내용\r\n테스트 임시조치 내용\r\n테스트 임시조치 내용\r\n테스트 임시조치 내용',2,3,'6','개선내용개선내용\r\n개선내용\r\n개선내용\r\n개선내용개선내용개선내용개선내용','002-20260106-00022','ADMIN','2026-01-06 21:47:20','ADMIN','2025-12-26 16:39:31','ADMIN','2026-01-06 21:47:20'),('001','00001','00001','00025','00013','251200004','','003',5,4,'20',NULL,'002-20251226-00011','ADMIN','2025-12-26 16:40:00','20260105','테스트',1,1,'1','ㅂㅈㄷㅂㅈㄷ','002-20260413-00025','20260400001','2026-04-13 21:44:30','ADMIN','2025-12-26 16:40:00','20260400001','2026-04-13 21:44:30'),('001','00001','00001','00025','self','251200005','직접입력 111','002',3,2,'6',NULL,'002-20251226-00012','ADMIN','2025-12-26 16:40:11','20260105','개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트개선 예정 텍스트 길이 테스트',0,0,'0','','','ADMIN','2026-01-06 21:48:00','ADMIN','2025-12-26 16:40:11','ADMIN','2026-01-06 21:48:00'),('001','00001','00001','00025','self','251200006','직접입력 111','004',4,2,'8',NULL,'002-20251226-00013','ADMIN','2025-12-26 16:41:54','20260105',NULL,0,0,'0','','','ADMIN','2026-01-05 21:46:11','ADMIN','2025-12-26 16:41:54','ADMIN','2026-01-05 21:46:11'),('001','00001','00001','00003','00003','260100010','','003',4,4,'16',NULL,'002-20260101-00018','ADMIN','2026-01-01 14:43:41','20260117',NULL,3,4,'12','ㅋㅌㅊㅋㅌㅊ1212','002-20260104-00021','ADMIN','2026-01-04 21:30:47','ADMIN','2026-01-01 14:43:41','ADMIN','2026-01-04 21:30:47'),('001','00001','00001','00003','00005','260100011','','003',3,4,'12','베베베베11','','ADMIN','2026-01-01 15:22:51','20260406','ㅂㅈㄷㅂㅈㄷ',1,1,'1','asdasd20260406','002-20260406-00024','ADMIN','2026-04-06 22:49:12','ADMIN','2026-01-01 15:22:51','ADMIN','2026-04-06 22:49:12');
/*!40000 ALTER TABLE `tb_risk_assessment` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_risk_site_hazard`
--

LOCK TABLES `tb_risk_site_hazard` WRITE;
/*!40000 ALTER TABLE `tb_risk_site_hazard` DISABLE KEYS */;
INSERT INTO `tb_risk_site_hazard` VALUES ('001','00001','00012','기계적요인 - 위험발생상황 1','','기계적요인 - 위험발생상황 1111','Y','ADMIN','2025-12-21 19:46:16','ADMIN','2025-12-21 19:46:16'),('001','00001','00014','기계적요인-위험발생상황2','','기계적요인-위험발생상황2222222222','Y','ADMIN','2026-04-06 20:10:54','ADMIN','2026-04-06 20:10:54'),('001','00001','00015','기계적요인-위험발생상황3','','기계적요인-위험발생상황3333333','Y','ADMIN','2026-04-06 20:11:03','ADMIN','2026-04-06 20:32:10'),('001','00001','00016','기계적요인-위험발생상황4','','기계적요인-위험발생상황444441','Y','ADMIN','2026-04-06 20:32:10','ADMIN','2026-04-06 20:58:25'),('001','00001','00019','테스트11','','22211','Y','ADMIN','2026-04-06 20:58:25','ADMIN','2026-04-06 20:58:25'),('001','00001','00022','테스트22','','121','Y','20260400001','2026-04-13 21:38:54','20260400001','2026-04-13 21:38:54'),('001','00002','00017','전기적요인-1','','전기적요인-111','Y','ADMIN','2026-04-06 20:33:14','ADMIN','2026-04-06 20:33:14'),('001','00003','00003','공정테스트-유해요인_1','','공정테스트-유해요인_11111','Y','ADMIN','2025-12-17 20:38:29','ADMIN','2025-12-17 20:38:29'),('001','00003','00004','공정테스트-유해요인_2','','공정테스트-유해요인_22222','Y','ADMIN','2025-12-17 20:38:29','ADMIN','2025-12-17 20:38:29'),('001','00003','00005','공정테스트-유해요인_3','','공정테스트-유해요인_33333','Y','ADMIN','2025-12-17 20:38:29','ADMIN','2025-12-17 20:38:29'),('001','00003','00006','공정테스트-유해요인_4','','공정테스트-유해요인_44444','Y','ADMIN','2025-12-17 20:38:29','ADMIN','2025-12-17 20:38:29'),('001','00005','00007','위험요인 구분 테스트_유해요인_1','','위험요인 구분 테스트_유해요인_1111111111','Y','ADMIN','2025-12-17 20:57:35','ADMIN','2025-12-17 20:57:35'),('001','00005','00008','위험요인 구분 테스트_유해요인_2','','위험요인 구분 테스트_유해요인_22222222222','Y','ADMIN','2025-12-17 20:57:35','ADMIN','2025-12-17 20:57:35'),('001','00006','00009','위험요인 구분 테스트_2_유해요인_11','','위험요인 구분 테스트_유해요인_111111111113333333','Y','ADMIN','2025-12-17 21:07:44','ADMIN','2025-12-17 22:29:27'),('001','00006','00010','위험요인 구분 테스트_2_유해요인_22','','위험요인 구분 테스트_2_유해요인_2222224444444','Y','ADMIN','2025-12-17 21:07:44','ADMIN','2025-12-17 22:29:27'),('001','00009','00011','중곡-위험요인구분_1_유해요인_1','00001','중곡-위험요인구분_1_유해요인_11111111111','Y','ADMIN','2025-12-17 22:21:23','ADMIN','2025-12-17 22:21:23'),('001','00025','00013','발생상황_1','00001','테스트 발생상황 비고 테스트','Y','ADMIN','2025-12-26 16:30:50','ADMIN','2025-12-26 16:30:50');
/*!40000 ALTER TABLE `tb_risk_site_hazard` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_risk_type`
--

LOCK TABLES `tb_risk_type` WRITE;
/*!40000 ALTER TABLE `tb_risk_type` DISABLE KEYS */;
INSERT INTO `tb_risk_type` VALUES ('001','00001','기계적 요인',NULL,'00001','Y',NULL,'ADMIN','2025-12-17 20:57:12','ADMIN','2025-12-17 21:07:11'),('001','00002','전기적 요인',NULL,'00001','Y',NULL,'ADMIN','2025-12-17 21:07:11','ADMIN','2025-12-18 20:43:08'),('001','00003','화학적 요인',NULL,'00001','Y',NULL,'ADMIN','2025-12-17 21:41:08','ADMIN','2025-12-17 22:40:57'),('001','00004','생물학적 요인',NULL,'00001','Y',NULL,'ADMIN','2025-12-17 22:44:32','ADMIN','2025-12-17 22:44:32'),('001','00005','작업특성 요인',NULL,'00001','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00006','작업환경 요인',NULL,'00001','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00007','기계적 요인',NULL,'00002','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00008','전기적 요인',NULL,'00002','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00009','화학적 요인',NULL,'00002','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00010','생물학적 요인',NULL,'00002','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00011','작업특성 요인',NULL,'00002','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00012','작업환경 요인',NULL,'00002','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00013','기계적 요인',NULL,'00003','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00014','전기적 요인',NULL,'00003','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00015','화학적 요인',NULL,'00003','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00016','생물학적 요인',NULL,'00003','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00017','작업특성 요인',NULL,'00003','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00018','작업환경 요인',NULL,'00003','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00019','기계적 요인',NULL,'00004','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00020','전기적 요인',NULL,'00004','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00021','화학적 요인',NULL,'00004','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00022','생물학적 요인',NULL,'00004','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00023','작업특성 요인',NULL,'00004','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00024','작업환경 요인',NULL,'00004','Y',NULL,'ADMIN','2025-12-17 22:47:50','ADMIN','2025-12-17 22:47:50'),('001','00025','테스트요인','00001','00001','Y',NULL,'ADMIN','2025-12-26 16:30:32','ADMIN','2025-12-26 16:30:32');
/*!40000 ALTER TABLE `tb_risk_type` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_sch_mgmt`
--

LOCK TABLES `tb_sch_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_sch_mgmt` DISABLE KEYS */;
INSERT INTO `tb_sch_mgmt` VALUES ('001','00001','00002','ST001','01',NULL,'20260417','0930','1800','90',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-02-20 23:00:42','20260400001','2026-04-13 21:18:37'),('001','00001','00003','ST002','01',NULL,'20260308','0700','1500','0',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-03-08 13:47:07','ADMIN','2026-03-08 13:47:07'),('001','00001','00004','ST003','02',NULL,'20260308','1500','2400','0',NULL,NULL,'0000','1800','',NULL,NULL,'Y','ADMIN','2026-03-08 14:19:21','ADMIN','2026-03-08 14:19:21'),('001','00001','00005','ST004','02',NULL,'20260517','0000','0700','0',NULL,NULL,'1300','1800','0',NULL,NULL,'Y','ADMIN','2026-03-08 14:19:50','20260400010','2026-05-16 21:02:58'),('001','00001','00006','ST005','02',NULL,'20260417','0200','1200','0',NULL,NULL,'1500','1800','0',NULL,NULL,'Y','20260400001','2026-04-17 23:13:47','20260400001','2026-04-17 23:13:47');
/*!40000 ALTER TABLE `tb_sch_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_sch_mgmt_hist`
--

LOCK TABLES `tb_sch_mgmt_hist` WRITE;
/*!40000 ALTER TABLE `tb_sch_mgmt_hist` DISABLE KEYS */;
INSERT INTO `tb_sch_mgmt_hist` VALUES ('001','00001',0,'00002','20260220','0900','1800','90',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-02-20 23:00:42'),('001','00001',0,'00003','20260308','0000','0430','0',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-03-08 13:47:07'),('001','00001',0,'00004','20260308','1500','2400','0',NULL,NULL,'0000','1800','',NULL,NULL,'Y','ADMIN','2026-03-08 14:19:21'),('001','00001',0,'00005','20260308','0000','0700','0',NULL,NULL,'0000','1800','',NULL,NULL,'Y','ADMIN','2026-03-08 14:19:50'),('001','00001',0,'00006','20260417','0200','1200','0',NULL,NULL,'1500','1800','0',NULL,NULL,'Y','20260400001','2026-04-17 23:13:47'),('001','00001',0,'00007','20260419','0100','0320','0',NULL,NULL,'0000','1800','',NULL,NULL,'Y','20260400001','2026-04-19 13:50:13'),('001','00001',1,'00002','20260220','0930','1800','90',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-02-20 23:01:14'),('001','00001',1,'00005','20260417','0000','0700','0',NULL,NULL,'1300','1800','0',NULL,NULL,'Y','20260400010','2026-05-16 21:02:58'),('001','00001',2,'00002','20260222','0930','1800','90',NULL,NULL,'','','',NULL,NULL,'N','ADMIN','2026-02-21 21:49:45'),('001','00001',3,'00002','20260301','0930','1800','90',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-02-28 21:48:28'),('001','00001',4,'00002','20260416','0930','1830','90',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-04-10 18:27:52'),('001','00001',5,'00002','20260417','0930','1800','90',NULL,NULL,'','','',NULL,NULL,'Y','ADMIN','2026-04-10 18:28:21'),('001','00001',6,'00002','20260417','0930','1800','90',NULL,NULL,'','','',NULL,NULL,'Y','20260400001','2026-04-13 21:18:37');
/*!40000 ALTER TABLE `tb_sch_mgmt_hist` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_shift_sch_assign_mgmt`
--

LOCK TABLES `tb_shift_sch_assign_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_shift_sch_assign_mgmt` DISABLE KEYS */;
INSERT INTO `tb_shift_sch_assign_mgmt` VALUES ('001','00001','00004',1,1,'Y','00003','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',1,2,'Y','00004','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',1,3,'Y','00005','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',1,4,'N',NULL,'ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',2,1,'N',NULL,'ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',2,2,'Y','00003','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',2,3,'Y','00004','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',2,4,'Y','00005','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',3,1,'Y','00005','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',3,2,'N',NULL,'ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',3,3,'Y','00003','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',3,4,'Y','00004','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',4,1,'Y','00004','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',4,2,'Y','00005','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',4,3,'N',NULL,'ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00004',4,4,'Y','00003','ADMIN','2026-03-08 21:26:02','ADMIN','2026-03-08 21:26:02'),('001','00001','00005',1,1,'Y','00003','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',1,2,'Y','00004','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',1,3,'Y','00005','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',1,4,'N',NULL,'ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',2,1,'N',NULL,'ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',2,2,'Y','00003','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',2,3,'Y','00004','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',2,4,'Y','00005','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',3,1,'Y','00005','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',3,2,'N',NULL,'ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',3,3,'Y','00003','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00005',3,4,'Y','00004','ADMIN','2026-03-09 21:09:02','ADMIN','2026-03-09 21:09:02'),('001','00001','00006',1,1,'Y','00003','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',1,2,'Y','00004','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',1,3,'Y','00005','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',1,4,'N',NULL,'ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',2,1,'N',NULL,'ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',2,2,'Y','00003','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',2,3,'Y','00004','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',2,4,'Y','00005','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',3,1,'Y','00005','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',3,2,'N',NULL,'ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',3,3,'Y','00003','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00006',3,4,'Y','00004','ADMIN','2026-03-09 21:09:08','ADMIN','2026-03-09 21:09:08'),('001','00001','00007',1,1,'Y','00003','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',1,2,'Y','00004','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',1,3,'Y','00005','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',1,4,'N',NULL,'ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',2,1,'N',NULL,'ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',2,2,'Y','00003','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',2,3,'Y','00004','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',2,4,'Y','00005','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',3,1,'Y','00005','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',3,2,'N',NULL,'ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',3,3,'Y','00003','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00007',3,4,'Y','00004','ADMIN','2026-03-19 22:16:16','ADMIN','2026-03-19 22:16:16'),('001','00001','00008',1,1,'N',NULL,'20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',1,2,'Y','00002','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',1,3,'Y','00003','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',1,4,'Y','00005','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',2,1,'Y','00005','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',2,2,'N',NULL,'20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',2,3,'Y','00002','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',2,4,'Y','00003','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',3,1,'Y','00003','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',3,2,'Y','00005','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',3,3,'N',NULL,'20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',3,4,'Y','00002','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',4,1,'Y','00002','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',4,2,'Y','00003','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',4,3,'Y','00005','20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00008',4,4,'N',NULL,'20260400001','2026-04-24 23:43:05','20260400001','2026-04-24 23:43:05'),('001','00001','00009',1,1,'N',NULL,'20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',1,2,'Y','00002','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',1,3,'Y','00003','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',1,4,'Y','00005','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',2,1,'Y','00005','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',2,2,'N',NULL,'20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',2,3,'Y','00002','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',2,4,'Y','00003','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',3,1,'Y','00003','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',3,2,'Y','00005','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',3,3,'N',NULL,'20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',3,4,'Y','00002','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',4,1,'Y','00002','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',4,2,'Y','00003','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',4,3,'Y','00005','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',4,4,'N',NULL,'20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32');
/*!40000 ALTER TABLE `tb_shift_sch_assign_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_shift_sch_mgmt`
--

LOCK TABLES `tb_shift_sch_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_shift_sch_mgmt` DISABLE KEYS */;
INSERT INTO `tb_shift_sch_mgmt` VALUES ('001','00001','00009','SHIFT_001',4,4,4,'Y','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32');
/*!40000 ALTER TABLE `tb_shift_sch_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_shift_sch_ptrn_mgmt`
--

LOCK TABLES `tb_shift_sch_ptrn_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_shift_sch_ptrn_mgmt` DISABLE KEYS */;
INSERT INTO `tb_shift_sch_ptrn_mgmt` VALUES ('001','00001','00009',1,'00002','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',2,'00003','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',3,'00005','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',4,'OFF','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32');
/*!40000 ALTER TABLE `tb_shift_sch_ptrn_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_shift_sch_team_meta_info`
--

LOCK TABLES `tb_shift_sch_team_meta_info` WRITE;
/*!40000 ALTER TABLE `tb_shift_sch_team_meta_info` DISABLE KEYS */;
INSERT INTO `tb_shift_sch_team_meta_info` VALUES ('001','00001','00009',1,'A','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',2,'B','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',3,'C','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32'),('001','00001','00009',4,'D','20260400010','2026-04-28 22:05:32','20260400010','2026-04-28 22:05:32');
/*!40000 ALTER TABLE `tb_shift_sch_team_meta_info` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_shift_sch_team_mgmt`
--

LOCK TABLES `tb_shift_sch_team_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_shift_sch_team_mgmt` DISABLE KEYS */;
INSERT INTO `tb_shift_sch_team_mgmt` VALUES ('001','00001','00009','20260500020','20260500020','20260501','20261031','20260400010','2026-05-10 17:46:31','20260400010','2026-05-10 17:46:31');
/*!40000 ALTER TABLE `tb_shift_sch_team_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_shift_sch_team_user`
--

LOCK TABLES `tb_shift_sch_team_user` WRITE;
/*!40000 ALTER TABLE `tb_shift_sch_team_user` DISABLE KEYS */;
INSERT INTO `tb_shift_sch_team_user` VALUES ('001','00001','00009','20260500020',1,'20260400010','N','20260400010','2026-05-10 17:46:31','20260400010','2026-05-10 17:46:31'),('001','00001','00009','20260500020',2,'20260400011','N','20260400010','2026-05-10 17:46:31','20260400010','2026-05-10 17:46:31'),('001','00001','00009','20260500020',3,'20260400012','N','20260400010','2026-05-10 17:46:31','20260400010','2026-05-10 17:46:31'),('001','00001','00009','20260500020',4,'20260400013','N','20260400010','2026-05-10 17:46:31','20260400010','2026-05-10 17:46:31');
/*!40000 ALTER TABLE `tb_shift_sch_team_user` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_site`
--

LOCK TABLES `tb_site` WRITE;
/*!40000 ALTER TABLE `tb_site` DISABLE KEYS */;
INSERT INTO `tb_site` VALUES ('001','00001','00001','중곡사업장','서울 광진구 중곡동 199-1','104-601호','04903','21150801',NULL,'Y','20260400010','0337353679',37.5701873,127.0834438,'200','ㅁㄴㅇㅁㄴ','SYSTEM','2025-08-03 14:20:19','20260400010','2026-06-01'),('001','00002','00002','월곡사업장','부산 부산진구 경마장로 2',NULL,NULL,'20251001',NULL,'Y',NULL,NULL,NULL,NULL,NULL,NULL,'SYSTEM','2025-08-03 14:20:25',NULL,NULL),('001','00003','00003','여수사업장','부산 부산진구 경마장로 2','asdasd','47203','20670801',NULL,'Y',NULL,'01077635257',NULL,NULL,'0','4154561212','SYSTEM','2025-08-03 14:20:32','ADMIN','2026-03-18'),('001','00004','00004','대전사업장','대전 대덕구 갈전도시고속도로 677','테스트1','34335','20250809',NULL,'Y',NULL,'01077635257',NULL,NULL,NULL,'테스트 대전사업장 생성','SYSTEM','2025-08-09 23:35:40',NULL,'2025-08-09'),('001','00005','00005','테스트사업장','서울 서초구 반포대로 142','','06595','20331008',NULL,'Y',NULL,'',NULL,NULL,'500','12121212','ADMIN','2026-03-18 22:29:52','ADMIN','2026-03-18'),('001','00006','00006','테스트사업장2','경기 성남시 분당구 경부고속도로 409','','13473','20280708',NULL,'Y',NULL,'',NULL,NULL,'','','ADMIN','2026-03-18 22:35:15','SOON','2026-03-23'),('001','00007','00007','테스트사업장3','서울 광진구 긴고랑로 2','','04913','20260318',NULL,'Y',NULL,'',NULL,NULL,'','','ADMIN','2026-03-18 22:35:43','ADMIN','2026-03-18'),('001','00008','testSite','TEST_SITE1212','서울 광진구 긴고랑로 3','','04912','20290405',NULL,'Y',NULL,'',NULL,NULL,'','','ADMIN','2026-04-05 18:34:07','ADMIN','2026-04-05');
/*!40000 ALTER TABLE `tb_site` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_site_node`
--

LOCK TABLES `tb_site_node` WRITE;
/*!40000 ALTER TABLE `tb_site_node` DISABLE KEYS */;
INSERT INTO `tb_site_node` VALUES ('001','00001','n1','중곡사업장','00001',NULL,'Y','20260400010',NULL,'20260400001','2026-04-13 20:25:57','20260400010','2026-06-01 20:29:42'),('001','00001','n2','1본부','00002','n1','N','20260400011',NULL,'20260400001','2026-04-13 20:30:17','20260400010','2026-05-02 12:47:31'),('001','00001','n3','2본부','00002','n1','N','20260400012',NULL,'20260400001','2026-04-13 20:30:17','20260400010','2026-05-02 12:47:27'),('001','00001','n4','1본부 1팀','00003','n2','N',NULL,NULL,'20260400001','2026-04-13 20:30:17','20260400001','2026-04-13 20:30:17'),('001','00001','n5','1본부 2팀','00003','n2','N',NULL,NULL,'20260400001','2026-04-13 20:30:17','20260400001','2026-04-13 20:30:17'),('001','00001','n6','2본부 1팀','00003','n3','N',NULL,NULL,'20260400001','2026-04-13 20:30:17','20260400001','2026-04-13 20:30:17'),('001','00002','n1','월곡사업장','00001',NULL,'N',NULL,NULL,'20260400001','2026-04-18 19:55:40','20260400001','2026-04-28 21:18:23'),('001','00002','n2','1본부','00002','n1','N',NULL,NULL,'20260400001','2026-04-18 19:55:22','20260400001','2026-04-18 19:55:22'),('001','00002','n3','2본부','00002','n1','N',NULL,NULL,'20260400001','2026-04-18 19:55:22','20260400001','2026-04-18 19:55:22'),('001','00002','n4','1본부 1팀','00003','n2','N',NULL,NULL,'20260400001','2026-04-18 19:55:22','20260400001','2026-04-18 19:55:22'),('001','00002','n5','1본부 2팀','00003','n2','N',NULL,NULL,'20260400001','2026-04-18 19:55:22','20260400001','2026-04-18 19:55:22'),('001','00002','n6','2본부 1팀','00003','n3','N',NULL,NULL,'20260400001','2026-04-18 19:55:22','20260400001','2026-04-18 19:55:22');
/*!40000 ALTER TABLE `tb_site_node` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_sms_auth_code`
--

LOCK TABLES `tb_sms_auth_code` WRITE;
/*!40000 ALTER TABLE `tb_sms_auth_code` DISABLE KEYS */;
INSERT INTO `tb_sms_auth_code` VALUES (176,'v1.AYsgo_hkO2w5YewsLF0mKQXfa0CIXt_Zmm8a1XQBdY7wKcLfz6cN1Q','u5wv821xWpgXFD_oJc-CAjzGOlNRQwB1fgyG9N48PfE','736672','2026-03-25 21:52:11','N','SYSTEM','2026-03-25 21:51:11',NULL,NULL),(177,'v1.AQEVdS3QdkaqBFvdSgiM485-q7wn2g791jIgxKB9M9L71_C6jw0s3Q','u5wv821xWpgXFD_oJc-CAjzGOlNRQwB1fgyG9N48PfE','771756','2026-03-25 22:32:08','N','SYSTEM','2026-03-25 22:31:08',NULL,NULL),(178,'v1.Afmp34-Q5cPugdtvyg3xhdtgY_6FQLMhtQ_ZPoPAptS084PByVuByg','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','802843','2026-03-25 22:51:50','Y','SYSTEM','2026-03-25 22:50:50','SYSTEM','2026-03-25 22:51:03'),(179,'v1.AZz-5PEqGDZU628Jcc8jweQgJBgIqJUuv3kJZ8lh1APEKSJ3_mpJxg','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','453699','2026-03-26 20:51:06','Y','SYSTEM','2026-03-26 20:50:06','SYSTEM','2026-03-26 20:50:12'),(180,'v1.AanpJxdUdNWRZfLJGGf6BfJDJb_AOsK8R0uueh1-kJ6HKZLhOztZ1A','Sic6ooGi4vQ9qd27_8MSZVWhDyEslobAXKQn4fPr-Ns','405863','2026-03-27 21:22:58','Y','SYSTEM','2026-03-27 21:21:58','SYSTEM','2026-03-27 21:22:01'),(181,'v1.AToMQHmhstLqVaCqxcDX1XcqOUKhBr7bNpUi-_EMy99tY0GvNBelvA','NlNBQcOqSjwtdCr5ZcWfPzFQUuksLYsiv1mhoVzi3WU','924668','2026-03-28 12:42:23','Y','SYSTEM','2026-03-28 12:41:23','SYSTEM','2026-03-28 12:41:26'),(182,'v1.AbTJlY5Z0zZMHftoQnOjkqG59pcOTVT0CNOWHTECS0a0VbU43Zp6_g','D-tDq0__0EHwV5WY4F87C3GmB6iCK8MWX7EAwaCQr24','281844','2026-03-28 22:15:44','Y','SYSTEM','2026-03-28 22:14:44','SYSTEM','2026-03-28 22:14:49'),(183,'v1.AV848yvsYrYi0Po_8IawapErNceM_e3vQHhUN9bzPXD0MHqP8wmg1w','D-tDq0__0EHwV5WY4F87C3GmB6iCK8MWX7EAwaCQr24','371776','2026-03-29 12:39:13','Y','SYSTEM','2026-03-29 12:38:13','SYSTEM','2026-03-29 12:38:20'),(184,'v1.Adv6haAvfIwkn-xg9f7pDrw7wRcI5-_RgqtAabEY_Ekb6ZQRehWcWw','sEFv-za8bhuzTdhjwfgHq-jJMK4js3FsaeIyXPj15XA','443876','2026-03-29 16:44:24','Y','SYSTEM','2026-03-29 16:43:24','SYSTEM','2026-03-29 16:43:28'),(185,'','','165252','2026-04-02 19:54:38','N','SYSTEM','2026-04-02 19:53:38',NULL,NULL),(186,'','','987882','2026-04-02 19:57:09','N','SYSTEM','2026-04-02 19:56:09',NULL,NULL),(187,'v1.AVN5O7OqjvpuHyN5SshFUv3P79ZSiQ3-nTKXugxHUsT1Dcktra12qw','u5wv821xWpgXFD_oJc-CAjzGOlNRQwB1fgyG9N48PfE','911750','2026-04-02 19:59:25','N','SYSTEM','2026-04-02 19:58:25',NULL,NULL),(188,'v1.AT-d0KQr_s_8X0rEvk8PGN9oeFqPS5tg0018fewbfDpHwyYTH4ZepQ','u5wv821xWpgXFD_oJc-CAjzGOlNRQwB1fgyG9N48PfE','952616','2026-04-02 20:00:55','N','SYSTEM','2026-04-02 19:59:55',NULL,NULL),(189,'v1.AVR9s1075Ivz3gGKOB37wxRPg7BOA-N75FCAtCwch-yJlKwHFDcUcg','u5wv821xWpgXFD_oJc-CAjzGOlNRQwB1fgyG9N48PfE','767536','2026-04-02 20:02:01','N','SYSTEM','2026-04-02 20:01:01',NULL,NULL),(190,'v1.AYMk-tjkzk4eeNoDNBREYmRODiX9_ll_oqSDYpreu5UbO3vvZsUnGA','u5wv821xWpgXFD_oJc-CAjzGOlNRQwB1fgyG9N48PfE','541259','2026-04-02 20:02:13','N','SYSTEM','2026-04-02 20:01:13',NULL,NULL),(191,'v1.AWobAsWcbdLbZTvjGig-l5NKd-AdSk7jjacjWy5cA03b02YedDJrxQ','JPnCL3w1_aC5iX-_VhT_klU1n7X0GJcJJWhDwhnFEHw','718037','2026-04-02 20:04:11','N','SYSTEM','2026-04-02 20:03:11',NULL,NULL),(192,'v1.AZ00ZrXaEb_0orsbG29wvjDqE9rD6DRSYptX-IDJWCyxZCdab7yo9Q','JPnCL3w1_aC5iX-_VhT_klU1n7X0GJcJJWhDwhnFEHw','175890','2026-04-02 20:04:50','N','SYSTEM','2026-04-02 20:03:50',NULL,NULL),(193,'v1.AdrDtU0NOy4dHA0YLOF1aGuwMVXtE2q9YhxgEewhrWR3pGv8KPFyzA','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','835022','2026-04-02 20:05:03','N','SYSTEM','2026-04-02 20:04:03',NULL,NULL),(194,'v1.AY7C-HSKykyo6vsSgw5jFYnjX03y4PE3giPsMYpEIVK58fOmVuxBkQ','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','355348','2026-04-12 21:44:16','N','SYSTEM','2026-04-12 21:43:16',NULL,NULL),(195,'v1.AfBTYcgYUkB-pN7K37bqNL6TPQf9uIW0e0JnuR5d_LaWJXH_tnSvxw','Sic6ooGi4vQ9qd27_8MSZVWhDyEslobAXKQn4fPr-Ns','517904','2026-04-14 20:43:29','N','SYSTEM','2026-04-14 20:42:29',NULL,NULL),(196,'v1.AQd8JRNS-FLyY6a0Cas5tx5yXKQTwZNab_GEsi5J_z06k9djfIi0iA','lpL1_b3DwIeQeYNXQAjUfapgKGAflNTeBSY0LKf40NI','668750','2026-04-14 20:54:10','N','SYSTEM','2026-04-14 20:53:10',NULL,NULL),(197,'v1.AdldQ0xH_mG3w82EjvmbWW65T-eJbEXjOOFNHcnl6Ja7rPkrNAMxrQ','N8KFfFZUDOoxPGVXNPMeiPlbwLpkI8sC-fqf7VbkPbs','836021','2026-04-14 21:07:41','N','SYSTEM','2026-04-14 21:06:41',NULL,NULL),(198,'v1.AVmbbmSNh-OsbOkrVtLKQWk3sEQQ3P9ilt81RiYwr9ycdLDsP6IsEA','Sic6ooGi4vQ9qd27_8MSZVWhDyEslobAXKQn4fPr-Ns','424532','2026-04-15 22:41:09','N','SYSTEM','2026-04-15 22:40:09',NULL,NULL),(199,'v1.ATgcP_iNuWgbFkPzFJz3WNW41GyJHxtDFcKyrNIR41v6JyudL1k1eg','NlNBQcOqSjwtdCr5ZcWfPzFQUuksLYsiv1mhoVzi3WU','171453','2026-04-15 22:43:19','N','SYSTEM','2026-04-15 22:42:19',NULL,NULL),(200,'v1.AQJCBDE9VAnwsIFqrgj_Tl2K0w1ILoEw9RpAR31fyMtButGU7AeQPA','N8KFfFZUDOoxPGVXNPMeiPlbwLpkI8sC-fqf7VbkPbs','793126','2026-04-15 23:17:20','N','SYSTEM','2026-04-15 23:16:20',NULL,NULL),(201,'v1.AXGzgRtH36f2jBz1UGE3LtwCe4TQ_OayvjEtxqP5IMzBkn4SpN2kdQ','D-tDq0__0EHwV5WY4F87C3GmB6iCK8MWX7EAwaCQr24','947865','2026-04-18 20:21:55','N','SYSTEM','2026-04-18 20:20:55',NULL,NULL),(202,'v1.ATlt8pmFq43vi5dfgwUULAo-_1V_MIAthZm0sI9HQIaCjmpqt3DYTQ','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','672614','2026-04-28 19:45:24','N','SYSTEM','2026-04-28 19:44:24',NULL,NULL),(203,'v1.AUUEpuUQKKezAyB2H7BtZV3aGEXE-V9xbda1nU9J3Z-peFwengup1w','UvCtleBuvDSqvjsMdYweLVsR_J4c1Ngx51rkTfbmwgA','565368','2026-04-28 19:48:51','N','SYSTEM','2026-04-28 19:47:51',NULL,NULL),(204,'v1.AUioUVKm3tpWrOU8CeU27ICFbkkfXLYfhwjy4A2qNXTJOXgnhokSIg','cG0lgINqcJGJB-q1azTxA7g7OHGtlLqKgnGPZKZdAdE','960473','2026-04-28 20:11:27','N','SYSTEM','2026-04-28 20:10:27',NULL,NULL),(205,'v1.ASCWoGmOSe99-dNaGICyQcuIc26I_VKygHsLN2X20mul-SnifbgcIA','cG0lgINqcJGJB-q1azTxA7g7OHGtlLqKgnGPZKZdAdE','497862','2026-04-28 20:18:51','N','SYSTEM','2026-04-28 20:17:51',NULL,NULL),(206,'v1.AQYX8954hve1oVu_gttOrL_PADQqbi-oVwLg1SrflIuYQRFdcrvphQ','cG0lgINqcJGJB-q1azTxA7g7OHGtlLqKgnGPZKZdAdE','149731','2026-04-28 20:19:26','N','SYSTEM','2026-04-28 20:18:26',NULL,NULL),(207,'v1.Af3Q2mJsPiGTDfT7qNUzKD-LtLVJdm8fBvKxMXY1o1v74yvfLucAvQ','cG0lgINqcJGJB-q1azTxA7g7OHGtlLqKgnGPZKZdAdE','120356','2026-04-28 20:21:20','N','SYSTEM','2026-04-28 20:20:20',NULL,NULL),(208,'v1.AYW0aDnxxRGkc89ZHuCbQEQbiMFJC8Hr_hjvVAQutx_MWmrbjBl1_g','cG0lgINqcJGJB-q1azTxA7g7OHGtlLqKgnGPZKZdAdE','100945','2026-04-28 20:36:01','N','SYSTEM','2026-04-28 20:35:01',NULL,NULL),(209,'v1.ATUG8-5f_H2T8WKiZj8jRc5OQOshbSxmVZD7IJzDH2q_Yf7XHdGZmg','0z0fIweuKRLSj3p88ggCdFcnkNxYbPxBgeb6sb3wU6I','431118','2026-04-28 20:38:52','N','SYSTEM','2026-04-28 20:37:52',NULL,NULL),(210,'v1.Abkcj_-Ti6Z_OEImEijvs_Qa4fdIAFdVhp9dguLH8ViHeXXEZG4H-Q','EacFZvOlndBG6wNgtY2sfrlISUV_zw5vEjSWISO8zFk','498760','2026-04-28 20:39:32','N','SYSTEM','2026-04-28 20:38:32',NULL,NULL),(211,'v1.AZGKRWWqkM2U7MvXg3dgs2Do8nYUq3QB8U4wN5jSwI0fkDmCRqpIqQ','-aGaJ59EIYAygR1VKLytM29dS0ePioJzLj4PkmGxPts','643302','2026-04-28 20:40:08','N','SYSTEM','2026-04-28 20:39:08',NULL,NULL),(212,'v1.AYUMyYhqOsdbYQYuK6lEZN-b3ROUiMTk8D89oVMm1wosdxqSL4lLIA','DzyE8bftQ5m0CRCPkbdjAJmB1gzELjmR8DylRli0Gkw','718118','2026-04-30 21:21:30','N','SYSTEM','2026-04-30 21:20:30',NULL,NULL),(213,'v1.AeGs_88Tlza1jPmoW3bESPfTqczMy_F6CMfOrE1nHF_Y6h6vSb2eDQ','my0zRMhP37cPPAvWBmJRmwZGazjY4REHx5doRAVz2nw','924921','2026-04-30 22:06:31','N','SYSTEM','2026-04-30 22:05:31',NULL,NULL),(214,'v1.AbzjiEljZf6LiEP__HPg7-qpI2DsxG_-MoN_-AqIVWj5tWv3f_RErQ','2OPDI3lfxVg9ANDC64mmf6WyZEd_gR1LsnAfPbd7Fcg','469868','2026-05-18 22:19:55','N','SYSTEM','2026-05-18 22:18:55',NULL,NULL),(215,'v1.ASWDZS54SpEVvgiQ9lafQfx_U_LNg4nzbIvWxI-1GiGd8Krqlj7aLg','Rywjkoxw06nue5avyFqFc7oFMR-5psin-iVpQXr6ze0','767981','2026-05-18 22:33:57','N','SYSTEM','2026-05-18 22:32:57',NULL,NULL),(216,'v1.AaGVIeZxdIfpTvah5awwq7CubkLPTq-5u_kdN7_NQsGbGdeOorG1Nw','-aGaJ59EIYAygR1VKLytM29dS0ePioJzLj4PkmGxPts','983306','2026-05-30 17:13:41','N','SYSTEM','2026-05-30 17:10:41',NULL,NULL),(217,'v1.AUkULfccWUOS1aTdV8PB2B4Kag-UfsljQU-UGqHUdm8P6LUfsW8pHQ','-aGaJ59EIYAygR1VKLytM29dS0ePioJzLj4PkmGxPts','794015','2026-05-31 20:29:17','Y','SYSTEM','2026-05-31 20:26:17','SYSTEM','2026-05-31 20:26:27'),(218,'v1.AcFZVa9BGhO_G8SpUWXhyRFyXayxW8uz8PnaJxQXIF8apsiummxqGg','EacFZvOlndBG6wNgtY2sfrlISUV_zw5vEjSWISO8zFk','960445','2026-06-03 13:40:27','N','SYSTEM','2026-06-03 13:39:27',NULL,NULL);
/*!40000 ALTER TABLE `tb_sms_auth_code` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_syst_auth_menu`
--

LOCK TABLES `tb_syst_auth_menu` WRITE;
/*!40000 ALTER TABLE `tb_syst_auth_menu` DISABLE KEYS */;
INSERT INTO `tb_syst_auth_menu` VALUES ('001','00001','ActInfoSrch','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Attd_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Attd_02','Y','N','N','N','N','N','ADMIN','2026-02-21 22:10:25','ADMIN','2026-02-21 22:10:25'),('001','00001','Attd_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-09 21:58:24','ADMIN','2026-03-09 21:58:24'),('001','00001','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00001','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','00001','Baim_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Baim_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','ChkLst_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','ChkLst_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','ChkLst_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','00001','JoinUser','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Risk_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Risk_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Risk_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Tbm_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','Tbm_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','TermsDetail','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','TermsInfo','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','User_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','User_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','User_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-12 21:05:34','ADMIN','2026-02-12 21:20:15'),('001','00001','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00004','ActInfoSrch','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_04','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_05','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_06','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00004','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','00004','Baim_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Baim_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Baim_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Baim_04','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Baim_05','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Baim_06','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','ChkLst_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','ChkLst_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','ChkLst_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','00004','JoinUser','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Risk_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Risk_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Risk_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Tbm_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','Tbm_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','TermsDetail','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','TermsInfo','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','User_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','User_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','User_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 18:41:06','20260400001','2026-04-25 18:41:06'),('001','00004','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00006','ActInfoSrch','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Attd_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:12'),('001','00006','Attd_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:12'),('001','00006','Attd_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:12'),('001','00006','Attd_04','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:12'),('001','00006','Attd_05','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:12'),('001','00006','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00006','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','00006','Baim_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:12'),('001','00006','Baim_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Baim_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Baim_04','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Baim_05','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Baim_06','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','ChkLst_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','ChkLst_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','ChkLst_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','00006','JoinUser','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Risk_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Risk_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Risk_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Tbm_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','Tbm_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','TermsDetail','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','TermsInfo','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','User_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','User_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','User_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 21:20:07','20260400001','2026-04-13 21:20:13'),('001','00006','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00008','ActInfoSrch','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Attd_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Attd_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Attd_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Attd_04','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 20:23:17','20260400001','2026-04-13 20:23:17'),('001','00008','Attd_05','Y','Y','Y','Y','Y','Y','20260400001','2026-04-13 20:23:17','20260400001','2026-04-13 20:23:17'),('001','00008','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','00008','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','00008','Baim_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Baim_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','ChkLst_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','ChkLst_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','ChkLst_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','00008','JoinUser','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Risk_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Risk_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Risk_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Tbm_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','Tbm_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','TermsDetail','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','TermsInfo','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','User_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','User_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','User_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-17 22:05:38','20260400001','2026-04-13 20:23:17'),('001','00008','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','99999','ActInfoSrch','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','Attd_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Attd_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Attd_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','99999','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','99999','Baim_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Baim_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','ChkLst_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','ChkLst_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:21','ADMIN','2026-04-02 20:33:43'),('001','99999','ChkLst_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','99999','JoinUser','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','Risk_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','Risk_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','Risk_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','Tbm_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','Tbm_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','TermsDetail','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','TermsInfo','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','User_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','User_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','User_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-23 21:52:22','ADMIN','2026-04-02 20:33:43'),('001','99999','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','hr','ActInfoSrch','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','Attd_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Attd_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Attd_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Attd_04','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','hr','Attd_05','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','hr','Attd_06','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','hr','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','hr','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','hr','Baim_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Baim_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:42','ADMIN','2026-03-19 21:29:42'),('001','hr','ChkLst_01','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','ChkLst_02','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','ChkLst_03','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','ChkLst_04','N','N','N','N','N','N','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','hr','JoinUser','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','Risk_01','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','Risk_02','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','Risk_03','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','Tbm_01','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','Tbm_02','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','TermsDetail','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','TermsInfo','N','N','N','N','N','N','20260400001','2026-04-25 19:50:39','20260400001','2026-04-25 19:50:39'),('001','hr','User_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:21','ADMIN','2026-03-19 21:30:21'),('001','hr','User_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:21','ADMIN','2026-03-19 21:30:21'),('001','hr','User_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:21','ADMIN','2026-03-19 21:30:21'),('001','hr','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','master','Acct_01','Y','Y','Y','Y','Y','Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','master','ActInfoSrch','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Attd_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Attd_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Attd_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Attd_04','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','master','Attd_05','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','master','Attd_06','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','master','Attd_07','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','master','Attd_08','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','master','Attd_09','Y','Y','Y','Y','Y','Y','ADMIN','2025-08-11 19:24:02','ADMIN','2026-03-19 21:29:20'),('001','master','Attd_10','Y','Y','Y','Y','Y','Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','master','Attd_11','Y','Y','Y','Y','Y','Y','SYSTEM','2026-05-27 20:59:40',NULL,NULL),('001','master','Attd_12','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','master','Baim_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Baim_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Baim_07','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','ChkLst_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','ChkLst_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','ChkLst_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','master','JoinUser','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','NearMiss_01','Y','Y','Y','Y','Y','Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','master','Notice_01','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-06 00:19:20',NULL,NULL),('001','master','Risk_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Risk_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Risk_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Tbm_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Tbm_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','Tbm_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','TermsDetail','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','TermsInfo','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','User_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','User_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','User_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:29:20','ADMIN','2026-03-19 21:29:20'),('001','master','User_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','safe','ActInfoSrch','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_01','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_02','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_03','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_04','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_05','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_06','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','safe','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','safe','Baim_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Baim_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','ChkLst_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','ChkLst_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','ChkLst_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','ChkLst_04','Y','Y','Y','Y','Y','Y','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','safe','JoinUser','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','Risk_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Risk_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Risk_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Tbm_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','Tbm_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','TermsDetail','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','TermsInfo','Y','Y','Y','Y','Y','Y','20260400001','2026-04-25 19:27:40','20260400001','2026-04-25 19:27:40'),('001','safe','User_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','User_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','User_03','Y','Y','Y','Y','Y','Y','ADMIN','2026-03-19 21:30:10','ADMIN','2026-03-19 21:30:10'),('001','safe','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','system','Attd_01','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_02','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_03','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_04','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_05','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_06','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_07','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Attd_10','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('001','system','Attd_11','Y','Y','N','N','N','N','SYSTEM','2026-05-27 20:59:40',NULL,NULL),('001','system','Attd_12','Y','Y','N','N','N','Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('001','system','Baim_01','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:23:57',NULL,NULL),('001','system','Baim_02','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-25 21:21:17',NULL,NULL),('001','system','Baim_03','Y','Y','Y','Y','Y','Y','ADMIN','2025-10-04 17:42:00','ADMIN','2025-10-06 22:55:25'),('001','system','Baim_04','Y','Y','Y','Y','Y','Y','ADMIN','2026-01-17 23:53:30','ADMIN','2026-01-17 23:53:30'),('001','system','Baim_05','Y','Y','Y','Y','Y','Y','ADMIN','2026-01-18 14:14:28','ADMIN','2026-01-18 14:14:28'),('001','system','Baim_06','Y','Y','Y','Y','Y','Y','ADMIN','2026-02-02 21:18:06','ADMIN','2026-02-03 20:25:28'),('001','system','ChkLst_01','Y','Y','Y','Y','Y','Y','ADMIN','2025-09-07 21:13:29','ADMIN','2025-09-07 21:13:29'),('001','system','ChkLst_02','Y','Y','Y','Y','Y','Y','ADMIN','2025-09-07 21:13:29','ADMIN','2025-09-07 21:13:29'),('001','system','ChkLst_03','Y','Y','N','N','N','N','ADMIN','2025-11-23 20:59:34','ADMIN','2025-11-23 21:09:50'),('001','system','ChkLst_04','Y','Y','N','N','N','N','SYSTEM','2026-06-07 12:05:48',NULL,NULL),('001','system','Risk_01','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','Risk_02','Y','N','N','N','N','N','ADMIN','2025-12-18 21:02:34','ADMIN','2025-12-18 21:02:34'),('001','system','Risk_03','Y','Y','Y','Y','Y','Y','ADMIN','2025-12-25 18:03:43','ADMIN','2026-01-14 21:11:40'),('001','system','Tbm_01','Y','Y','Y','Y','Y','Y','ADMIN','2026-01-14 21:11:40','ADMIN','2026-01-14 21:27:00'),('001','system','Tbm_02','Y','Y','Y','Y','Y','Y','ADMIN','2026-01-14 21:27:00','ADMIN','2026-01-14 21:27:00'),('001','system','User_01','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:00',NULL,NULL),('001','system','User_02','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','User_03','Y','Y','Y','Y','Y','Y','SYSTEM','2025-08-11 19:24:02',NULL,NULL),('001','system','User_04','Y','Y','N','N','N','N','SYSTEM','2026-05-23 14:25:52',NULL,NULL);
/*!40000 ALTER TABLE `tb_syst_auth_menu` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_syst_menu_d`
--

LOCK TABLES `tb_syst_menu_d` WRITE;
/*!40000 ALTER TABLE `tb_syst_menu_d` DISABLE KEYS */;
INSERT INTO `tb_syst_menu_d` VALUES ('Acct_01','acct','acct/Acct_01.vue','사고관리',1,NULL,'Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('ActInfoSrch','login','login/ActInfoSrch.vue','아이디/비밀번호 찾기',NULL,NULL,'Y','SYSTEM','2025-10-12 21:13:00',NULL,NULL),('Attd_01','attd','attd/Attd_01.vue','근무 타입 관리',1,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_02','attd','attd/Attd_02.vue','휴일 관리',2,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_03','attd','attd/Attd_03.vue','연차 타입 관리',3,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_04','attd','attd/Attd_04.vue','출퇴근 시간 표준화',4,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_05','attd','attd/Attd_05.vue','근무 계획 관리',5,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_06','attd','attd/Attd_06.vue','교대근무 팀 관리',6,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_07','attd','attd/Attd_07.vue','근무 관리',7,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_08','attd','attd/Attd_08.vue','근로자 근태 조회',8,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_09','attd','attd/Attd_09.vue','사용자 연차관리',9,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Attd_10','attd','attd/Attd_10.vue','요청 승인 관리',10,NULL,'Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('Attd_11','attd','attd/Attd_11.vue','월별 사용자 근태 판정',11,NULL,'Y','SYSTEM','2026-05-27 20:59:40',NULL,NULL),('Attd_12','attd','attd/Attd_12.vue','부정 출퇴근 의심 모니터링',12,NULL,'Y','SYSTEM','2026-06-03 21:04:11',NULL,NULL),('Baim_01','baim','baim/Baim_01.vue','사업장관리',1,'','Y','SYSTEM','2025-08-10 14:47:48',NULL,NULL),('Baim_02','baim','baim/Baim_02.vue','운영 기초정보 관리',2,NULL,'Y','SYSTEM','2025-08-25 12:43:49',NULL,NULL),('Baim_03','baim','baim/Baim_03.vue','이용약관',3,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Baim_04','baim','baim/Baim_04.vue','일일계정 발급 관리',5,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Baim_05','baim','baim/Baim_05.vue','계정슬롯 관리',6,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Baim_06','baim','baim/Baim_06.vue','조직관리',7,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Baim_07','baim','baim/Baim_07.vue','연차부여정책',4,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('ChkLst_01','chkLst','chkLst/ChkLst_01.vue','순회점검 대상 관리',1,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('ChkLst_02','chkLst','chkLst/ChkLst_02.vue','점검문항관리',2,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('ChkLst_03','chkLst','chkLst/ChkLst_03.vue','점검결과조회',3,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('ChkLst_04','chkLst','chkLst/ChkLst_04.vue','점검 불량 관리',4,NULL,'Y','SYSTEM','2026-06-07 12:05:46',NULL,NULL),('JoinUser','login','login/JoinUser.vue','회원가입',NULL,NULL,'Y','SYSTEM','2025-10-12 21:13:00',NULL,NULL),('NearMiss_01','nearMiss','nearMiss/NearMiss_01.vue','아차사고 관리',1,NULL,'Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL),('Notice_01','notice','notice/Notice_01.vue','공지사항 관리',1,NULL,'Y','SYSTEM','2026-06-06 00:19:20',NULL,NULL),('Risk_01','risk','risk/Risk_01.vue','유해/위험 구분 관리',1,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Risk_02','risk','risk/Risk_02.vue','위험성 평가 기준',2,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Risk_03','risk','risk/Risk_03.vue','위험성 평가 관리',4,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Tbm_01','tbm','tbm/Tbm_01.vue','TBM 교육자료 관리',1,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Tbm_02','tbm','tbm/Tbm_02.vue','TBM 교육 관리',2,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Tbm_03','tbm','tbm/Tbm_03.vue','TBM 진행관리',3,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('Tbm_04','tbm','tbm/Tbm_04.vue','TBM 이력관리',4,NULL,'Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('TermsDetail','login','login/TermsDetail.vue','이용약관 정보',NULL,NULL,'Y','SYSTEM','2025-10-12 21:13:00',NULL,NULL),('TermsInfo','login','login/TermsInfo.vue','이용약관 관리',NULL,NULL,'Y','SYSTEM','2025-10-12 21:13:00',NULL,NULL),('User_01','user','user/User_01.vue','사용자관리',1,'','Y','SYSTEM','2025-08-10 14:48:10',NULL,NULL),('User_02','user','user/User_02.vue','권한별 화면 제어',2,'','Y','SYSTEM','2025-08-10 14:48:19',NULL,NULL),('User_03','user','user/User_03.vue','사업장 권한 관리',3,'','Y','SYSTEM','2025-08-18 22:14:09',NULL,NULL),('User_04','user','user/User_04.vue','연차 결재라인 구성',4,NULL,'Y','SYSTEM','2026-05-23 14:25:52',NULL,NULL);
/*!40000 ALTER TABLE `tb_syst_menu_d` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_syst_menu_m`
--

LOCK TABLES `tb_syst_menu_m` WRITE;
/*!40000 ALTER TABLE `tb_syst_menu_m` DISABLE KEYS */;
INSERT INTO `tb_syst_menu_m` VALUES ('acct','001','사고관리',9,NULL,'Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL),('attd','001','근태관리',6,NULL,'Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL),('baim','001','기초정보관리',2,'','Y','SYSTEM','2025-08-17 21:55:40',NULL,NULL),('chkLst','001','순회점검관리',3,'','Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL),('login','002','로그인',1,NULL,'Y','SYSTEM','2025-10-12 21:09:00',NULL,NULL),('nearMiss','001','아차사고 관리',7,NULL,'Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL),('notice','001','공지사항',8,NULL,'Y','SYSTEM','2026-06-06 00:19:20',NULL,NULL),('risk','001','위험성평가',4,NULL,'Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL),('tbm','001','TBM관리',5,NULL,'Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL),('user','001','사용자관리',1,'','Y','SYSTEM','2025-08-17 21:56:06',NULL,NULL);
/*!40000 ALTER TABLE `tb_syst_menu_m` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_syst_val_d`
--

LOCK TABLES `tb_syst_val_d` WRITE;
/*!40000 ALTER TABLE `tb_syst_val_d` DISABLE KEYS */;
INSERT INTO `tb_syst_val_d` VALUES ('SYS001','100','도급사',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2025-06-30 21:41:49',NULL,NULL),('SYS001','200','수급사',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2025-06-30 21:42:05',NULL,NULL),('SYS001','300','주관사',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2025-06-30 21:42:10',NULL,NULL),('SYS003','N','미사용',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-06-30 21:43:12',NULL,NULL),('SYS003','Y','사용',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-06-30 21:43:03',NULL,NULL),('SYS004','100','남성',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-07-12 15:32:06',NULL,NULL),('SYS004','200','여성',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-07-12 15:32:13',NULL,NULL),('SYS005','100','정규직',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-07-12 17:38:35',NULL,NULL),('SYS005','200','단기직',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-07-12 17:38:40',NULL,NULL),('SYS006','N','미연동',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-08-03 14:16:20',NULL,NULL),('SYS006','Y','연동',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-08-03 14:16:13',NULL,NULL),('SYS007','001','WEB',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2025-08-17 21:51:56',NULL,NULL),('SYS007','002','APP',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2025-08-17 21:52:00',NULL,NULL),('SYS008','001','서비스이용약관',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS008','002','개인정보 처리방침',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS008','003','개인정보 수집 이용동의서',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS008','004','개인정보 제 3자 제공동의서',4,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS008','005','위치기반 서비스 이용약관 동의',5,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS009','N','불량',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS009','Y','양호',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS010','001','일일점검',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS010','002','위험성평가',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS010','003','TBM 교육자료',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS010','004','아차사고',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS010','005','공지첨부',5,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 00:19:20',NULL,NULL),('SYS011','001','검토요청',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS011','002','개선예정',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS011','003','개선완료',4,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS011','004','미처리대상',5,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS011','005','아차사고로 이관',6,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS012','01','상근직',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS012','02','단기직',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS013','01','활성화',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS013','02','잠김',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS013','03','탈퇴',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS013','04','인증대기',4,'Y',NULL,NULL,'관리자가 생성한 직후 휴대폰 본인인증 미완료 상태','SYSTEM','2026-05-28 21:56:59',NULL,NULL),('SYS014','01','직접가입',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-01-18 18:45:34',NULL,NULL),('SYS014','02','QR발급',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS015','01','비점유',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS015','02','점유중',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS016','01','관리자 점유해제',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS016','02','사용기간만료',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS017','01','고정',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS017','02','비고정',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS018','01','이미지',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS018','02','동영상',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS018','03','유튜브 URL',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS018','04','PDF',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS019','01','1구간',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS019','02','2구간',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS020','01','공휴일',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS020','02','지정휴무',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS020','03','반복휴무',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS021','01','사용자 신청',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS021','02','관리자 부여',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS022','01','자동부여',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS022','02','수동부여',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS023','01','유급',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS023','02','무급',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS024','01','법정',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS024','02','특별',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS025','00','1일',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 09:56:14',NULL,NULL),('SYS025','01','반차',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 09:56:14',NULL,NULL),('SYS025','02','시간차(2시간)',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 09:56:14',NULL,NULL),('SYS025','03','시간차(1시간)',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 09:56:14',NULL,NULL),('SYS025','04','시간차(30분)',5,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 09:56:14',NULL,NULL),('SYS026','01','설정안함',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS026','02','해당 년도 내',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS026','03','기간설정',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS027','01','입사일',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS027','02','생일',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS027','03','부여일지정',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS028','01','출근',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS028','02','퇴근',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS029','01','설정안함',1,'Y','0',NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS029','02','5분',2,'Y','5',NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS029','03','10분',3,'Y','10',NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS029','04','15분',4,'Y','15',NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS029','05','30분',5,'Y','30',NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS030','01','직접가입',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS030','02','QR가입',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS031','01','사용자등록',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS031','02','관리자생성',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS031','03','QR체크',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','01','근태 생성 요청',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','02','근태 수정 요청',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','03','초과근무 생성 요청',3,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','04','초과근무 수정 요청',4,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','05','연차 사용 요청',5,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','06','연차 수정 요청',6,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS032','07','관리자 반려',7,'Y',NULL,NULL,'prafta-009: 관리자의 근태/초과근무 요청 반려 처리 이력 구분 (TB_USER_ATTD_HIST.HIST_TYPE)','SYSTEM','2026-05-17 13:34:23',NULL,NULL),('SYS032','08','초과근무 승인',8,'Y',NULL,NULL,NULL,'PRAFTA-027','2026-05-25 10:00:50',NULL,NULL),('SYS032','09','초과근무 반려',9,'Y',NULL,NULL,NULL,'PRAFTA-027','2026-05-25 10:00:50',NULL,NULL),('SYS032','10','스케줄 수정 요청',10,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-02 21:16:55',NULL,NULL),('SYS033','01','신청',1,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS033','02','승인',2,'Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:25:00',NULL,NULL),('SYS033','03','반려',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-04-29 23:11:07',NULL,NULL),('SYS033','04','취소',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-04-29 23:11:07',NULL,NULL),('SYS034','01','Android',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-04-29 23:11:07',NULL,NULL),('SYS034','02','Ios',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-04-29 23:11:07',NULL,NULL),('SYS035','MANUAL_BONUS','포상 휴가',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS035','MANUAL_CONDOLENCE','경조사 휴가',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS035','MANUAL_LONG_SERVICE','장기근속 휴가',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS035','MANUAL_OTHER','기타 약정 휴가',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS035','STATUTORY_ANNUAL','법정 본연차',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS035','STATUTORY_MONTHLY','법정 월차',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS035','STATUTORY_TENURE_BONUS','법정 근속 가산',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS036','FISCAL_YEAR','회계연도 기준',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS036','HIRE_DATE','입사일 기준',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS037','MONTHLY_ONLY','월차만 부여',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS037','NEXT_YEAR_BULK','차년도 시점 일괄부여',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS037','PRORATE','회계연도 시점 비례부여',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS038','CEIL','올림 (근로자 유리)',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS038','FLOOR','내림 (회사 유리)',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS038','HALF_DAY','0.5일 단위 절사',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS038','ROUND','반올림 (표준)',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS039','KEEP_AND_APPLY_NEW','기존 유지 + 신규만 적용',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS039','KEEP_AND_BACKFILL','기존 유지 + 누락 소급',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS039','RESET_ALL','전체 재계산 (위험)',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS040','ACTIVE','사용중',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS040','CANCELED','취소됨',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS040','EXHAUSTED','소진완료',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS040','EXPIRED','만료',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS041','CONTRACT','계약직',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS041','DAILY','일용직',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS041','EXECUTIVE','임원',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS041','REGULAR','정규직',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','CONTRACT_TO_REGULAR','계약→정규 전환',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','EXPERIENCE_DIFF','이종업계 경력 인정',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','EXPERIENCE_SAME','동종업계 경력 인정',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','GROUP_MOVE','그룹사 이동',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','MA_TRANSFER','M&A 고용승계',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','OTHER','기타',NULL,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS043','01','자동 부여',1,'Y','AUTO',NULL,NULL,'SYSTEM','2026-05-21 18:42:47',NULL,NULL),('SYS043','02','관리자 수동 부여',2,'Y','ADMIN',NULL,NULL,'SYSTEM','2026-05-21 18:42:47',NULL,NULL),('SYS044','00','대기중',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 11:31:26',NULL,NULL),('SYS044','01','신청',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 11:31:26',NULL,NULL),('SYS044','02','승인',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 11:31:26',NULL,NULL),('SYS044','03','반려',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 11:31:26',NULL,NULL),('SYS045','LEAVE_APPROVAL_TURN','연차 결재 차례 도래(결재자)',4,'Y','PUSH',NULL,NULL,'SYSTEM','2026-06-04 00:40:14',NULL,NULL),('SYS045','LEAVE_GRANT_RECALLED','부여 연차 회수',1,'Y','PUSH',NULL,NULL,'SYSTEM','2026-05-27 18:59:37',NULL,NULL),('SYS045','LEAVE_REFUSAL_CHECKIN_ALERT','노무수령거부일 출근감지(관리자)',3,'Y','PUSH',NULL,NULL,'SYSTEM','2026-06-02 23:53:19',NULL,NULL),('SYS045','LEAVE_REFUSAL_NOTICE','노무수령거부 통지(근로자)',2,'Y','PUSH',NULL,NULL,'SYSTEM','2026-06-02 23:53:19',NULL,NULL),('SYS045','LEAVE_USED_NO_APRV','무결재 연차 사용 통보(노드 관리자)',5,'Y','PUSH',NULL,NULL,'SYSTEM','2026-06-04 00:40:14',NULL,NULL),('SYS045','NEAR_MISS_REPORTED','아차사고 보고',2,'Y','PUSH',NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS046','CANCELLED','취소',5,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS046','COMPLETED','교육종료',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36','SYSTEM','2026-06-07'),('SYS046','DRAFT','개설',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36','SYSTEM','2026-06-07'),('SYS046','IN_PROGRESS','교육시작',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36','SYSTEM','2026-06-07'),('SYS046','OPENED','교육준비',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36','SYSTEM','2026-06-07'),('SYS047','TBM','툴박스미팅',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS048','AUTO','자동',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS048','DISABLED','비활성',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS048','MANUAL','수동확인',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS049','PAUSED','정지',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS049','PLAYING','재생',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS050','DAILY','일용직',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS050','REGULAR','정규직',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS051','MANAGER_DIRECT','관리자직접입실',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-07 20:36:34','SYSTEM','2026-06-07'),('SYS051','MANAGER_QR_SCAN','관리자QR',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS051','SELF_DEVICE','본인디바이스',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS052','MANAGER_FORCED','관리자강제',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS052','MANAGER_QR_SCAN','관리자QR',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS052','SELF','본인',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS053','COMPLETED','이수',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS053','NOT_COMPLETED','미이수',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','BACKGROUND_IN','백그라운드진입',5,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','BACKGROUND_OUT','백그라운드복귀',6,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','END','종료',9,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','ENTER','입실',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','FORCED_END','강제종료',10,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','GPS_UPDATED','GPS갱신',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','NETWORK_LOST','네트워크끊김',7,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','SIGNATURE_STARTED','서명시작',8,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','SLIDE_CHANGED','슬라이드변경',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','START','교육시작',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS055','ENTRY','입실',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS055','EXIT','종료',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS060','01','다운로드',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-29 15:11:32',NULL,NULL),('SYS061','100','아차사고',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS061','200','경미사고',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS061','300','유해·위험요인발견',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS062','100','경미',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS062','200','중대',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS062','300','치명',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS063','100','접수',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS063','200','검토중',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS063','300','조치중',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS063','400','완료',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS063','900','반려',9,'Y',NULL,NULL,NULL,'SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS064','ADMIN_ALERTED','관리자 알림 발송됨',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-02 23:53:19',NULL,NULL),('SYS064','CHECKIN_DETECTED','대상일 출근 감지됨',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-02 23:53:19',NULL,NULL),('SYS064','NOTICED','통지 발송됨',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-02 23:53:19',NULL,NULL),('SYS065','100','중대재해',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS065','200','일반산재',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS065','300','신고제외',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS066','100','접수',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS066','200','처리중',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS066','300','종결',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS067','ATTD','근태',1,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS067','CHKPT','순회점검',2,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS067','NEAR_MISS','아차사고',5,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS067','RISK','위험성평가',3,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS067','TBM','TBM',4,'Y',NULL,NULL,NULL,'SYSTEM','2026-06-06 17:49:42',NULL,NULL);
/*!40000 ALTER TABLE `tb_syst_val_d` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_syst_val_m`
--

LOCK TABLES `tb_syst_val_m` WRITE;
/*!40000 ALTER TABLE `tb_syst_val_m` DISABLE KEYS */;
INSERT INTO `tb_syst_val_m` VALUES ('SYS001','회사타입','Y',NULL,NULL,NULL,'SYSTEM','2025-06-29 20:24:15',NULL,NULL),('SYS002','권한타입','N',NULL,NULL,NULL,'SYSTEM','2025-06-29 20:27:31',NULL,NULL),('SYS003','사용여부','Y',NULL,NULL,NULL,'SYSTEM','2025-06-30 21:22:59',NULL,NULL),('SYS004','성별타입','Y','',NULL,NULL,'SYSTEM','2025-07-12 15:31:38',NULL,NULL),('SYS005','근무형태','Y','',NULL,NULL,'SYSTEM','2025-07-12 17:38:24',NULL,NULL),('SYS006','연동상태','Y','',NULL,NULL,'SYSTEM','2025-08-03 14:16:00',NULL,NULL),('SYS007','화면사용처','Y',NULL,NULL,NULL,'SYSTEM','2025-08-17 21:51:32',NULL,NULL),('SYS008','이용약관','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS009','점검답변','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS010','파일타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS011','위험성평가진행단계','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS012','사용자구분','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS013','계정상태','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS014','계정 슬롯구분','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS015','계정 슬롯상태','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS016','점유해제유형','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS017','고정여부','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS018','교육자료항목타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS019','스케줄 구간수','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS020','휴일타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS021','연차타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS022','연차부여타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS023','연차유급구분','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS024','연차성격타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS025','연차사용단위','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS026','연차사용가능기간타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS027','연차자동부여타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS028','출퇴근 시간 표준화 종류','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS029','출퇴근 시간 표준화 타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS030','일일계정 가입경로','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS031','근태등록방법','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS032','근태요청구분','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS033','근태요청상태','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS034','모바일 디바이스 타입','Y',NULL,NULL,NULL,'SYSTEM','2025-10-04 17:24:00',NULL,NULL),('SYS035','연차 부여 분류','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS036','연차 정책 AXIS1','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS037','연차 정책 AXIS3','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS038','연차 정책 AXIS4','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS039','입사일 변경 처리 방식','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS040','연차 부여 상태','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS041','고용 형태','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS042','경력 인정 사유','Y',NULL,NULL,NULL,'SYSTEM','2026-05-20 21:49:06',NULL,NULL),('SYS043','연차 부여 방식','Y',NULL,NULL,'tb_user_leave_grant.GRANT_BY_TYPE 코드','SYSTEM','2026-05-21 18:42:47',NULL,NULL),('SYS044','결재 단계 상태','Y',NULL,NULL,NULL,'SYSTEM','2026-05-23 11:31:26',NULL,NULL),('SYS045','알림 유형','Y',NULL,NULL,'tb_noti_outbox.NOTI_TYPE 코드','SYSTEM','2026-05-27 18:59:36',NULL,NULL),('SYS046','TBM 세션 상태','Y',NULL,NULL,'tb_tbm_session.STATUS_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS047','TBM 교육 유형','Y',NULL,NULL,'tb_tbm_session.EDU_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS048','TBM GPS 검증유형','Y',NULL,NULL,'tb_tbm_session.GPS_VERIFY_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS049','TBM 동기화 상태','Y',NULL,NULL,'tb_tbm_session_state.SYNC_STATE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS050','TBM 출결 대상유형','Y',NULL,NULL,'tb_tbm_attendance.USER_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS051','TBM 입실 경로','Y',NULL,NULL,'tb_tbm_attendance.ENTRY_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS052','TBM 종료 경로','Y',NULL,NULL,'tb_tbm_attendance.EXIT_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS053','TBM 이수 상태','Y',NULL,NULL,'tb_tbm_attendance.COMPLETION_STATUS_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS054','TBM 출결 이벤트 유형','Y',NULL,NULL,'tb_tbm_attendance_event.EVENT_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS055','TBM 비번 유형','Y',NULL,NULL,'tb_tbm_pwd_fail.PWD_TYPE_CD 코드','SYSTEM','2026-05-27 21:49:36',NULL,NULL),('SYS060','감사 액션 유형','Y',NULL,NULL,'tb_audit_log.ACTION_TYPE 코드','SYSTEM','2026-05-29 15:11:30',NULL,NULL),('SYS061','사건유형','Y',NULL,NULL,'tb_near_miss.INCIDENT_TYPE_CD 코드','SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS062','잠재적 중대성','Y',NULL,NULL,'tb_near_miss.POTENTIAL_SEVERITY_CD 코드','SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS063','사건 처리상태','Y',NULL,NULL,'tb_near_miss.REPORT_STATUS_CD 코드','SYSTEM','2026-05-31 13:53:04',NULL,NULL),('SYS064','노무수령거부 이벤트 유형','Y',NULL,NULL,'tb_leave_refusal_log.EVENT_TYPE 코드','SYSTEM','2026-06-02 23:53:19',NULL,NULL),('SYS065','재해등급','Y',NULL,NULL,'tb_acct.ACCT_GRADE_CD 코드','SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS066','사고 처리상태','Y',NULL,NULL,'tb_acct.PROCESS_STATUS_CD 코드','SYSTEM','2026-06-06 17:49:42',NULL,NULL),('SYS067','사고 연계도메인 구분','Y',NULL,NULL,'tb_acct_link.LINK_DOMAIN_CD 코드','SYSTEM','2026-06-06 17:49:42',NULL,NULL);
/*!40000 ALTER TABLE `tb_syst_val_m` ENABLE KEYS */;
UNLOCK TABLES;

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
  `ENTRY_TYPE_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입실경로[SYS051] SELF_DEVICE:본인디바이스 MANAGER_QR_SCAN:관리자QR스캔 MANAGER_DIRECT:관리자직접입실(웹검색)',
  `ENTRY_BY_MANAGER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'QR 입실 처리 관리자 USER_CD',
  `ENTRY_AT` datetime DEFAULT NULL COMMENT '입실 시각',
  `ENTRY_GPS_LAT` decimal(10,7) DEFAULT NULL COMMENT '입실 위도',
  `ENTRY_GPS_LON` decimal(10,7) DEFAULT NULL COMMENT '입실 경도',
  `ENTRY_DISTANCE_M` int DEFAULT NULL COMMENT '입실 시 개설지점과의 거리(m)',
  `ENTRY_SIGN_FILE_MGMT_CD` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입실 서명 파일코드',
  `EXIT_TYPE_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료경로[SYS052] SELF:본인 MANAGER_QR_SCAN:관리자QR MANAGER_FORCED:관리자강제',
  `EXIT_BY_MANAGER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 처리 관리자 USER_CD',
  `EXIT_AT` datetime DEFAULT NULL COMMENT '종료 시각(NULL=미종료)',
  `APP_FOREGROUND_SEC` int DEFAULT NULL COMMENT '앱 포그라운드 누적초(SELF_DEVICE 종료 시 1회 수신, 대리/검색입실 NULL)',
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
-- Dumping data for table `tb_tbm_attendance`
--

LOCK TABLES `tb_tbm_attendance` WRITE;
/*!40000 ALTER TABLE `tb_tbm_attendance` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_tbm_attendance` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_attendance_event`
--

LOCK TABLES `tb_tbm_attendance_event` WRITE;
/*!40000 ALTER TABLE `tb_tbm_attendance_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_tbm_attendance_event` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_edu_mtrl`
--

LOCK TABLES `tb_tbm_edu_mtrl` WRITE;
/*!40000 ALTER TABLE `tb_tbm_edu_mtrl` DISABLE KEYS */;
INSERT INTO `tb_tbm_edu_mtrl` VALUES ('26041300017','001',NULL,'테스트','테스트','00002','Y','20260400001','2026-04-13 21:36:10','20260400001','2026-04-13 21:37:33');
/*!40000 ALTER TABLE `tb_tbm_edu_mtrl` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_edu_mtrl_item`
--

LOCK TABLES `tb_tbm_edu_mtrl_item` WRITE;
/*!40000 ALTER TABLE `tb_tbm_edu_mtrl_item` DISABLE KEYS */;
INSERT INTO `tb_tbm_edu_mtrl_item` VALUES ('26041300045','26041300017',2,'02','22','003-20260413-00035',NULL,NULL,NULL,'Y','20260400001','2026-04-13 21:36:10','20260400001','2026-04-13 21:36:10'),('26041300046','26041300017',3,'03','33',NULL,NULL,NULL,'https://github.com/y-Soong/safenote','Y','20260400001','2026-04-13 21:36:10','20260400001','2026-04-13 21:36:10'),('26041300047','26041300017',3,'01',NULL,'003-20260413-00036',NULL,NULL,NULL,'Y','20260400001','2026-04-13 21:37:33','20260400001','2026-04-13 21:37:33');
/*!40000 ALTER TABLE `tb_tbm_edu_mtrl_item` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_pwd_fail`
--

LOCK TABLES `tb_tbm_pwd_fail` WRITE;
/*!40000 ALTER TABLE `tb_tbm_pwd_fail` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_tbm_pwd_fail` ENABLE KEYS */;
UNLOCK TABLES;

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
  `STATUS_CD` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '세션상태[SYS046] DRAFT:개설 OPENED:교육준비 IN_PROGRESS:교육시작 COMPLETED:교육종료 CANCELLED:취소',
  `ENTRY_PWD` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '입실 비밀번호(랜덤6자리, 교육준비(OPENED) 전이 시 발급)',
  `EXIT_PWD` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '종료 비밀번호(랜덤6자리, 교육종료(COMPLETED) 전이 시 발급, 입실≠종료)',
  `MANAGER_USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '개설자 USER_CD',
  `MANAGER_GPS_LAT` decimal(10,7) DEFAULT NULL COMMENT '개설 위도(AUTO 모드 시)',
  `MANAGER_GPS_LON` decimal(10,7) DEFAULT NULL COMMENT '개설 경도(AUTO 모드 시)',
  `GPS_VERIFY_TYPE_CD` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO' COMMENT 'GPS검증유형[SYS048] AUTO:자동 MANUAL:수동확인 DISABLED:비활성',
  `GPS_VERIFY_RADIUS_M` int NOT NULL DEFAULT '100' COMMENT 'GPS 검증반경(m, 50~1000)',
  `GPS_MANUAL_CONFIRM_YN` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N' COMMENT 'MANUAL 모드 관리자 확인여부 Y:확인',
  `OPENED_AT` datetime DEFAULT NULL COMMENT '개설 시각',
  `PREP_START_AT` datetime DEFAULT NULL COMMENT '교육준비 타이머 기준시각(15분 자동 교육시작 기준, 수동연장 시 NOW() 리셋)',
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
-- Dumping data for table `tb_tbm_session`
--

LOCK TABLES `tb_tbm_session` WRITE;
/*!40000 ALTER TABLE `tb_tbm_session` DISABLE KEYS */;
INSERT INTO `tb_tbm_session` VALUES ('T2026060400001','001','00001','TBM','20260604 TBM 교육1','<h1><strong>테스트 TBM 교육</strong></h1><p><br></p><p><br></p><p>교육 테스트 자료로 올리는거고 </p><p><br></p><p>교육 내용은 가라 작성임</p>','RICH_HTML','OPENED','760253','004733','20260400010',37.5703470,127.0831908,'AUTO',100,'N','2026-06-04 20:42:56',NULL,NULL,NULL,NULL,NULL,'N','20260400010','2026-06-04 20:42:56','20260400010','2026-06-07 15:43:52'),('T2026060700002','001','00001','TBM','test 2026-06-07','<p>test111</p>','RICH_HTML','DRAFT',NULL,NULL,'20260400010',37.5702937,127.0831904,'AUTO',100,'N',NULL,NULL,NULL,NULL,NULL,NULL,'N','20260400010','2026-06-07 16:06:37','20260400010','2026-06-07 16:06:37');
/*!40000 ALTER TABLE `tb_tbm_session` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_session_content`
--

LOCK TABLES `tb_tbm_session_content` WRITE;
/*!40000 ALTER TABLE `tb_tbm_session_content` DISABLE KEYS */;
INSERT INTO `tb_tbm_session_content` VALUES ('001','T2026060400001','26041300017',0,NULL,'20260400010','2026-06-07 15:43:52'),('001','T2026060700002','26041300017',0,NULL,'20260400010','2026-06-07 16:06:37');
/*!40000 ALTER TABLE `tb_tbm_session_content` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_session_risk`
--

LOCK TABLES `tb_tbm_session_risk` WRITE;
/*!40000 ALTER TABLE `tb_tbm_session_risk` DISABLE KEYS */;
INSERT INTO `tb_tbm_session_risk` VALUES ('001','T2026060400001','00001','00001','251200004',0,'20260400010','2026-06-07 15:43:52'),('001','T2026060400001','00001','00001','251200005',1,'20260400010','2026-06-07 15:43:52'),('001','T2026060400001','00001','00001','251200006',2,'20260400010','2026-06-07 15:43:52'),('001','T2026060700002','00001','00001','260100010',0,'20260400010','2026-06-07 16:06:37'),('001','T2026060700002','00001','00001','260100011',1,'20260400010','2026-06-07 16:06:37');
/*!40000 ALTER TABLE `tb_tbm_session_risk` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_tbm_session_state`
--

LOCK TABLES `tb_tbm_session_state` WRITE;
/*!40000 ALTER TABLE `tb_tbm_session_state` DISABLE KEYS */;
INSERT INTO `tb_tbm_session_state` VALUES ('001','T2026060400001',NULL,NULL,0,'PAUSED','20260400010','20260400010','2026-06-04 20:42:56',NULL,NULL);
/*!40000 ALTER TABLE `tb_tbm_session_state` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_terms`
--

LOCK TABLES `tb_terms` WRITE;
/*!40000 ALTER TABLE `tb_terms` DISABLE KEYS */;
INSERT INTO `tb_terms` VALUES ('001','2','Y','<p>테스트 서비스 이용약관 2</p>','20260404','Y','테스트','ADMIN','2025-10-05 21:59:03','ADMIN','2026-04-04 20:24:36'),('002','1','Y','<h2>개인정보 처리방침</h2><p><br></p><p>2025-10-05 개인정보 처리방침 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p><br></p><p><br></p><p>끝</p><p><br></p><p><br></p>','20251005','Y','','ADMIN','2025-10-06 22:50:48','ADMIN','2025-10-06 22:50:48'),('003','1','Y','<h2>개인정보 수집 이용동의서</h2><p><br></p><p>2025-10-05 개인정보 수집 이용동의서 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p><br></p><p>끝</p><p><br></p>','20251005','Y','개인정보 수집 이용동의서 내용 테스트','ADMIN','2025-10-06 22:54:47','ADMIN','2025-10-06 22:54:47'),('004','1','Y','<h2>개인정보 제 3자 제공동의서</h2><p><br></p><p>2025-10-05 개인정보 제 3자 제공 동의서 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p><br></p><p>끝</p>','20251006','Y','개인정보 제 3자 제공동의서 내용 테스트','ADMIN','2025-10-06 22:56:53','ADMIN','2025-10-06 22:56:53'),('005','2','Y','<p>ㅁㄴㅇㅁㄴㅇㅁㄴ</p>','20260212','Y','123123','ADMIN','2025-10-06 22:58:28','ADMIN','2026-02-12 20:38:17');
/*!40000 ALTER TABLE `tb_terms` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_terms_id_version`
--

LOCK TABLES `tb_terms_id_version` WRITE;
/*!40000 ALTER TABLE `tb_terms_id_version` DISABLE KEYS */;
INSERT INTO `tb_terms_id_version` VALUES ('001','1','Y','<h2>서비스이용약관</h2><p><br></p><p>2025-10-05 서비스 이용약관 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트 </p><p><br></p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p>서비스 이용약관 내용 테스트</p><p><br></p><p><br></p><p>끝</p><p><br></p><p><br></p>','20251005','서비스 이용약관 비고 테스트','ADMIN','2025-10-05 21:59:03'),('001','2','Y','<p>테스트 서비스 이용약관 2</p>','20260404','테스트','ADMIN','2026-04-04 20:24:36'),('002','1','Y','<h2>개인정보 처리방침</h2><p><br></p><p>2025-10-05 개인정보 처리방침 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p>개인정보 처리방침 내용 테스트</p><p><br></p><p><br></p><p>끝</p><p><br></p><p><br></p>','20251005','','ADMIN','2025-10-06 22:50:48'),('003','1','Y','<h2>개인정보 수집 이용동의서</h2><p><br></p><p>2025-10-05 개인정보 수집 이용동의서 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p>개인정보 수집 이용동의서 내용 테스트</p><p><br></p><p>끝</p><p><br></p>','20251005','개인정보 수집 이용동의서 내용 테스트','ADMIN','2025-10-06 22:54:47'),('004','1','Y','<h2>개인정보 제 3자 제공동의서</h2><p><br></p><p>2025-10-05 개인정보 제 3자 제공 동의서 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p>개인정보 제 3자 제공동의서 내용 테스트</p><p><br></p><p>끝</p>','20251006','개인정보 제 3자 제공동의서 내용 테스트','ADMIN','2025-10-06 22:56:53'),('005','1','Y','<h2>위치기반 서비스 이용약관 동의서</h2><p><br></p><p>2025-10-05 위치기반 서비스 이용약관 동의서 초안</p><p>!@# (특수문자 테스트)</p><p><br></p><p>이후 길이 테스트</p><p><br></p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p>위치기반 서비스 이용약관 동의서 내용 테스트</p><p><br></p><p>끝</p>','20251006','위치기반 서비스 이용약관 동의서 내용 테스트','ADMIN','2025-10-06 22:58:28'),('005','2','Y','<p>ㅁㄴㅇㅁㄴㅇㅁㄴ</p>','20260212','123123','ADMIN','2026-02-12 20:38:17');
/*!40000 ALTER TABLE `tb_terms_id_version` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_terms_user_agr_mgmt`
--

LOCK TABLES `tb_terms_user_agr_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_terms_user_agr_mgmt` DISABLE KEYS */;
INSERT INTO `tb_terms_user_agr_mgmt` VALUES ('20260400001','001','2','Y','20260400001','2026-04-12 21:46:46'),('20260400001','002','1','Y','20260400001','2026-04-12 21:46:46'),('20260400001','003','1','Y','20260400001','2026-04-12 21:46:46'),('20260400001','004','1','Y','20260400001','2026-04-12 21:46:46'),('20260400001','005','2','Y','20260400001','2026-04-12 21:46:46'),('20260400002','001','2','Y','20260400002','2026-04-14 20:42:47'),('20260400002','002','1','Y','20260400002','2026-04-14 20:42:47'),('20260400002','003','1','Y','20260400002','2026-04-14 20:42:47'),('20260400002','004','1','Y','20260400002','2026-04-14 20:42:47'),('20260400002','005','2','Y','20260400002','2026-04-14 20:42:47'),('20260400003','001','2','Y','20260400003','2026-04-14 20:54:50'),('20260400003','002','1','Y','20260400003','2026-04-14 20:54:50'),('20260400003','003','1','Y','20260400003','2026-04-14 20:54:50'),('20260400003','004','1','Y','20260400003','2026-04-14 20:54:50'),('20260400003','005','2','Y','20260400003','2026-04-14 20:54:50'),('20260400004','001','2','Y','20260400004','2026-04-14 21:07:17'),('20260400004','002','1','Y','20260400004','2026-04-14 21:07:17'),('20260400004','003','1','Y','20260400004','2026-04-14 21:07:17'),('20260400004','004','1','Y','20260400004','2026-04-14 21:07:17'),('20260400004','005','2','Y','20260400004','2026-04-14 21:07:17'),('20260400005','001','2','Y','20260400005','2026-04-15 22:40:29'),('20260400005','002','1','Y','20260400005','2026-04-15 22:40:29'),('20260400005','003','1','Y','20260400005','2026-04-15 22:40:29'),('20260400005','004','1','Y','20260400005','2026-04-15 22:40:29'),('20260400005','005','2','Y','20260400005','2026-04-15 22:40:29'),('20260400006','001','2','Y','20260400006','2026-04-15 22:42:36'),('20260400006','002','1','Y','20260400006','2026-04-15 22:42:36'),('20260400006','003','1','Y','20260400006','2026-04-15 22:42:36'),('20260400006','004','1','Y','20260400006','2026-04-15 22:42:36'),('20260400006','005','2','Y','20260400006','2026-04-15 22:42:36'),('20260400007','001','2','Y','20260400007','2026-04-15 23:16:39'),('20260400007','002','1','Y','20260400007','2026-04-15 23:16:39'),('20260400007','003','1','Y','20260400007','2026-04-15 23:16:39'),('20260400007','004','1','Y','20260400007','2026-04-15 23:16:39'),('20260400007','005','2','Y','20260400007','2026-04-15 23:16:39'),('20260400008','001','2','Y','20260400008','2026-04-18 20:21:14'),('20260400008','002','1','Y','20260400008','2026-04-18 20:21:14'),('20260400008','003','1','Y','20260400008','2026-04-18 20:21:14'),('20260400008','004','1','Y','20260400008','2026-04-18 20:21:14'),('20260400008','005','2','Y','20260400008','2026-04-18 20:21:14'),('20260400009','001','2','Y','20260400009','2026-04-28 19:54:14'),('20260400009','002','1','Y','20260400009','2026-04-28 19:54:14'),('20260400009','003','1','Y','20260400009','2026-04-28 19:54:14'),('20260400009','004','1','Y','20260400009','2026-04-28 19:54:14'),('20260400009','005','2','Y','20260400009','2026-04-28 19:54:14'),('20260400010','001','2','Y','20260400010','2026-04-28 20:10:41'),('20260400010','002','1','Y','20260400010','2026-04-28 20:10:41'),('20260400010','003','1','Y','20260400010','2026-04-28 20:10:41'),('20260400010','004','1','Y','20260400010','2026-04-28 20:10:41'),('20260400010','005','2','Y','20260400010','2026-04-28 20:10:41'),('20260400011','001','2','Y','20260400011','2026-04-28 20:38:11'),('20260400011','002','1','Y','20260400011','2026-04-28 20:38:11'),('20260400011','003','1','Y','20260400011','2026-04-28 20:38:11'),('20260400011','004','1','Y','20260400011','2026-04-28 20:38:11'),('20260400011','005','2','Y','20260400011','2026-04-28 20:38:11'),('20260400012','001','2','Y','20260400012','2026-04-28 20:38:46'),('20260400012','002','1','Y','20260400012','2026-04-28 20:38:46'),('20260400012','003','1','Y','20260400012','2026-04-28 20:38:46'),('20260400012','004','1','Y','20260400012','2026-04-28 20:38:46'),('20260400012','005','2','Y','20260400012','2026-04-28 20:38:46'),('20260400013','001','2','Y','20260400013','2026-04-28 20:39:21'),('20260400013','002','1','Y','20260400013','2026-04-28 20:39:21'),('20260400013','003','1','Y','20260400013','2026-04-28 20:39:21'),('20260400013','004','1','Y','20260400013','2026-04-28 20:39:21'),('20260400013','005','2','Y','20260400013','2026-04-28 20:39:21'),('20260400014','001','2','Y','20260400014','2026-04-30 22:05:45'),('20260400014','002','1','Y','20260400014','2026-04-30 22:05:45'),('20260400014','003','1','Y','20260400014','2026-04-30 22:05:45'),('20260400014','004','1','Y','20260400014','2026-04-30 22:05:45'),('20260400014','005','2','Y','20260400014','2026-04-30 22:05:45');
/*!40000 ALTER TABLE `tb_terms_user_agr_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user`
--

LOCK TABLES `tb_user` WRITE;
/*!40000 ALTER TABLE `tb_user` DISABLE KEYS */;
INSERT INTO `tb_user` VALUES ('001','20260400010','ADMIN','시스템관리자','$2a$12$YuLJFYJAlBSVLvhzsGTvHO05Ut.me7pRPbf.bEHfr6RqwqI./JPmy','00001','n1','master',NULL,'v1.AZzUfhytVWgGWHRaFon7hT6lYBJCSOoX4l9nl3Ff8RkiAuCCou6HYA','cG0lgINqcJGJB-q1azTxA7g7OHGtlLqKgnGPZKZdAdE','5257','v1.AQzu6WsPZKqK7XRnSYHntX3o95zsgugue5GKFMEV0TRqhgNtMpGxdDDo','Xb9Mp6hPpNsQ5dQDHGE9Z6yTcXeMZm0Ge2y-Thi5Osg','test.com','v1.ARkm6xenzTefjIPsIQe0k45vh59JAmLlxyFVrVdrNEbbT_E',NULL,NULL,NULL,'100','Y','01','N',0,NULL,'2026-05-29 18:36:38',NULL,'2026-06-07 22:06:34','SYSTEM','2026-04-28 20:10:41','SYSTEM','2026-06-07 22:06:34'),('001','20260400011','WLSGML108','윤진희','$2a$12$oNDvaZ3gPPXbjqRT6Gm4luKU6yPtLuMOjspvwb1Fu8CZBs.z8lCG.','00001','n2','99999',NULL,'v1.AQ1qxWYqy3aszi5vTNhi49I6RwYod4OL7fgH8ee5zc6sX-cO1-3YIA','0z0fIweuKRLSj3p88ggCdFcnkNxYbPxBgeb6sb3wU6I','5258','v1.ASIERIxdLgTXSj6KB6fVJn1Kr-97NO_65vh8pYj8x-lpcN4bOaOWkshU','Xb9Mp6hPpNsQ5dQDHGE9Z6yTcXeMZm0Ge2y-Thi5Osg','test.com','v1.AXT3K6-LhBxNhBBSdTVIiXtR8jzJuqmx82oqo-38IWx-LvM','20240918',NULL,NULL,'100','Y','01','N',0,NULL,NULL,NULL,NULL,'SYSTEM','2026-04-28 20:38:11','20260400010','2026-05-24 18:14:22'),('001','20260400012','YJKIM','김여진','$2a$12$tnnkl3SDKbA4OUwNepiF3.EeRCUMkQp4SMouuIPa.AuB.aYSNdpe6','00001','n3','master',NULL,'v1.ATWzTAHVHNxbiOOEIzSMXhLty9DOW7FHFel4XEgT0UVoLksZbOSYlw','EacFZvOlndBG6wNgtY2sfrlISUV_zw5vEjSWISO8zFk','8295','v1.AbQKGV0TmS_aMnLBFSqHXswXPd2Yv7kshEWmuV0uYbSeoVWQnRt3wxg0','Xb9Mp6hPpNsQ5dQDHGE9Z6yTcXeMZm0Ge2y-Thi5Osg','test.com','v1.Ac7ac6F0FXg4KzRn878lRrmHvy2PFja7m6OSeKiJT19_9aY','20250725',NULL,NULL,'200','Y','01','N',1,NULL,'2026-05-29 22:08:44',NULL,'2026-05-29 22:07:55','SYSTEM','2026-04-28 20:38:46','SYSTEM','2026-06-03 13:50:46'),('001','20260400013','SOON','윤순기','$2a$12$ym1.0ElA0fkjLubMQiDcs.J38fM0kv/SM7nb5Lydr5DLInNVXRuyW','00001','n1','99999',NULL,'v1.AaJcR2RKnrARxlQmfUPr0MkVxtaA5RZxuDESOuDc_zjjRmrXtCazfg','-aGaJ59EIYAygR1VKLytM29dS0ePioJzLj4PkmGxPts','5258','v1.AZQCmzmCZjSgod-unLZdhRcL1VKA2EtMfbP-HKfLatLORw1ZyH1hQM_e','Xb9Mp6hPpNsQ5dQDHGE9Z6yTcXeMZm0Ge2y-Thi5Osg','test.com','v1.Ae4X_0ZmaBu4PyfMbtjCzJp0fSqXDcwIegOkspcWXyT0EnSlDg','20250602',NULL,NULL,'100','Y','01','N',0,NULL,NULL,NULL,'2026-06-08 20:11:38','SYSTEM','2026-04-28 20:39:21','SYSTEM','2026-06-08 20:11:38'),('001','20260400014','TEST01','테스터01','$2a$12$liZFlmJKetmozuiOXsmB3.f5Di14SoM8HwBMBSoX6wRDbR6a6oO.O','00001','','99999',NULL,'v1.AbwiE3HA0BodFntEddVdA9RKHuesHxSJYsN_ZxPsIJ01aip89cw3ug','my0zRMhP37cPPAvWBmJRmwZGazjY4REHx5doRAVz2nw','1112','v1.ATOHwe-ffn8hZ_ZGDvRyPBVnzBWJv4NddRNYHzNwmLQyavhxFBH9SGsT','Xb9Mp6hPpNsQ5dQDHGE9Z6yTcXeMZm0Ge2y-Thi5Osg','test.com','v1.ARQvJQhsIXXcnirlKy0eQ_O29OejFL2glY08RziZSA9Bzig',NULL,NULL,NULL,'100','Y','01','N',0,NULL,NULL,NULL,NULL,'SYSTEM','2026-04-30 22:05:45','SYSTEM','2026-04-30 22:05:45'),('001','20260600015','BOT01','BOT01','$2a$12$eShqJWwvef2gqD.soRn75.jvMW8TNqyZY3I1QsTpn2/5T/g3BhW9m','00001','n3','99999',NULL,'v1.AZ2h5Z2le1szSFC3oWi-b1dKJPxuJqCsGJG884Lil5-zhlmzOnaYow','hm4ThW1i4k4KrCY5YgfP8ixdeArqdnz5LjkoGf8yYg4','1112','v1.ARVaSiIXpDaSZP-ZeLyIuQMXQTg_cZexoraAIQM7MzHj5p9yG_DrPR281Lc','8mbxBVS0_KlESgquZu6oYcmNhQwvSMSL_DHplBEu6dc','test02.com','v1.Af02ia0PIiRjEjx4ABKaoWGVK9H11Bz_6vQ1vprl29mAQ6I',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:57','20260400010','2026-06-07 22:19:57'),('001','20260600016','BOT02','BOT02','$2a$12$1hRs4n.Ss8443RmrY/9SPu2foLdIHsaqlADd/VhjzNly0HbhcO5Gq','00001','n3','99999',NULL,'v1.AdoKuAJ1g-h1CWirLixjTLnpaH491tiBa2wBqwc0nLQpFb5Sgx7YLQ','aPuTCiek6gvYEJOMk5H4vkFRyF42SD_HxxL276P_ufo','1113','v1.AfDHA6W9TV_-WspD6PDcm61QlR0oRmJZmlRK1BfJkacN6Th1XUyPm6pWN4g','1VGDnecHkYBWyRI3iy912B9Qab1nhD9pMIv7EA076Mg','test03.com','v1.ARBr6_L16EJx9c1HeqCxqnDfOGCtc2gW3vAIdZGX3aUV5hg',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:57','20260400010','2026-06-07 22:19:57'),('001','20260600017','BOT03','BOT03','$2a$12$tWfCMfpyFG8t1RtIfa2LjeL0Q/en4zUsFlu7NK1vHYYkCWBbVsjSO','00001','n3','99999',NULL,'v1.Ae7-hZRWmPpeZrSCt-_iFkAPy7XmWlyzODTqEce_zkGq51YB4gZ_gg','FfUO3mKB4CPVSVh46qK5D_BvdOn8L8PCq9QivxF60vw','1114','v1.AWUqR4JillIy0plPEFR5OV1a-Va90J8xnzSnhxt7SS4E7g4UrR8Imalb7C4','i5LEGdwTYLh27SH1F_JSJrfSsqVwtGMwemy3g9tJ4YA','test04.com','v1.AX3UDDcGbpZ9_xLtv1e9ZoEhpWruUmjOeFc4BlnFL0z9ZCM',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600018','BOT04','BOT04','$2a$12$dmwtG6Goc.X28etQPfohuuDpVNTyinIgpUVn4PeVJzr9EF2VU5pn.','00001','n3','99999',NULL,'v1.AT0yaG7aobaV5le9DhjJLggGgBVcAe9bryiIkm9F5tR4okfGldsDYA','_3c7kuD79GPA6AoOi1Iv7NglRmJ0XZjF1pNzi_8B-E4','1115','v1.AeJ7zUkAL6zYNnsqU_3MZkRW7SqdlYQi8dQlM7Tbv2sxffBGFhJ9o_SdK-g','Z9uTwWc7ejRbEAgAWQrRfJl2N4ZvpZ558klw4fOWFDY','test05.com','v1.AcdDTqqfoCkHeEXHZ8coFifN2vjEcHOZ-pppjvBZgbuxeYE',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600019','BOT05','BOT05','$2a$12$l1gGmfwQck4z/yq7mebtx.3VZ87FNRicPFK5odqRcwdCxEuu/iikq','00001','n3','99999',NULL,'v1.AVeHw77b0RvZ91PrRRZKr5_Qc_wqbg2p4rurCQXkcZnyux9JYcpzjA','YdIo8TGRJ9AjX447tXJmTpbJKVWWZZbzyhdWzuokmpY','1116','v1.AZ0QX94iReqhQnYBd8J8XSQN3iPDhKBjQMmmWbAJToVRHaAGNh-K084c6t0','Ip1JQBY4TKgBmHyO3gMNlaywNgCxpVvjTJFNHqjdlKQ','test06.com','v1.AQq74_hZO9Z-lpBp1G8RGdwCp01bciUEI3NUDE5MuE0uqaE',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600020','BOT06','BOT06','$2a$12$lka7hjGGeJMspszOZVFjrOc2PVklxlg7hLHGUlTH4DcZN/94MQhMe','00001','n3','99999',NULL,'v1.AWaOSFoKzW26wEyki7APCfYwLDg6v42qRvWcIxNp_CufM6DVbNw5gQ','Tgqz-b3y8Cd22LvXh0qBhY9roMdaJbBNuuwYx2ttPPQ','1117','v1.AazdYyYwpOtYIcwC9Z-tpZ8Rrfe1kETn_f1woWVl2-oCFhIn6gBAjaY7thE','8GFqCShzS0pqwaitMSMvg_5wTJF1qszEU8xkh4l2yMc','test07.com','v1.Ae5J3LmhYQv7xzqbDBrM15s0_NHYiUfvlJ0KjW_coXggHkI',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600021','BOT07','BOT07','$2a$12$8HhKHOtL5x5elWQ5uehL4ehnB23yYvGLW0GTC8zAYWSyjNlUIsika','00001','n3','99999',NULL,'v1.ASHzmfF2VPGybAsrlq5yPBt0Jd6SLWxkAVIUuRbBzeYXV-c5Q3lOww','bmTBvFTJ61Qk3F4DxZkGK9K_z2NkR6Y4W7HCSrNWnWo','1118','v1.AS-SaLAHlESpTxg0wjYerqwP8g11stHcydKc21lIFgz6pGQyv3o-ZgFYzMI','EBvrb0EUcB2X2TbKzh2PYXOF6B2df12kSVLbHoKx244','test08.com','v1.AdMjOqappRHDxtgmRRXHWrSzU7XK2tP7BFQVlL7ReH90mo4',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:59','20260400010','2026-06-07 22:19:59'),('001','20260600022','BOT08','BOT08','$2a$12$AFZi7o0GpTzun3DuRS59Vu0/5v4O5V2tJ69ET5QiUIH9fkDj8twbq','00001','n3','99999',NULL,'v1.AfP64DVhT_-2fVjn1gkmosLOUz9c3rR6PqcdN0nQqlfOkAowAkfo','ORNLJ3fztV-hy20L-M6X8dHhdhdc_L3ZATBSTKH98a8','1119','v1.AUMXuaYH7IxyPSNYbzrS3UYV6GRmoY8CRbQMmvgGCfW3eVWZkLhA6FGM8nw','2RcY-2bdSTWUlc77dLXn8HocQJgAajnjRKKxIyNHBGw','test09.com','v1.ActRqTGMIM_1NT56BGI07po7bDE1jdZrnjUz66LWkCLyTSs',NULL,NULL,NULL,'M','Y','04','N',0,NULL,NULL,NULL,NULL,'20260400010','2026-06-07 22:19:59','20260400010','2026-06-07 22:19:59');
/*!40000 ALTER TABLE `tb_user` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_attd_gps`
--

LOCK TABLES `tb_user_attd_gps` WRITE;
/*!40000 ALTER TABLE `tb_user_attd_gps` DISABLE KEYS */;
INSERT INTO `tb_user_attd_gps` VALUES ('2026051700001','001','2026051700044','00001','20260400010','01',37.5012340,127.0398760,12.50,'20260509','091500','N','127.0.0.1',NULL,'P0101TST','2026-05-17 21:50:06'),('2026051700002','001','2026051700046','00001','20260400010','01',37.5665000,126.9780000,8.00,'20260514','090500','N','127.0.0.1',NULL,'P0101TST','2026-05-17 21:50:06'),('2026051700003','001','2026051700039','00001','20260400010','01',37.5012340,127.0398760,12.50,'20260509','091500','N','127.0.0.1',NULL,'P0101TST','2026-05-17 21:50:06'),('2026051700004','001','2026051700039','00001','20260400010','02',37.5665000,126.9780000,12.50,'20260509','091500','N','127.0.0.1',NULL,'P0101TST','2026-05-17 21:50:06'),('2026053100001','001','2026053100051','00001','20260400013','01',37.5702742,127.0835615,18.86,'20260531','193845','N','172.30.1.23','ㅎㅎㅎ','20260400013','2026-05-31 19:38:45'),('2026053100002','001','2026053100051','00001','20260400013','02',37.5702905,127.0835381,18.94,'20260531','215612','N','172.30.1.23','테스트','20260400013','2026-05-31 21:56:12');
/*!40000 ALTER TABLE `tb_user_attd_gps` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_attd_hist`
--

LOCK TABLES `tb_user_attd_hist` WRITE;
/*!40000 ALTER TABLE `tb_user_attd_hist` DISABLE KEYS */;
INSERT INTO `tb_user_attd_hist` VALUES ('2026051100038','001','2026051100030','00001','01','qwe','20260501','','','','','20260501','0900','20260501','1800','20260400010','2026-05-11 21:04:11'),('2026051100039','001','2026051100031','00001','01','asd','20260501','','','','','20260501','1900','20260501','2222','20260400010','2026-05-11 21:04:19'),('2026051100040','001','2026051100031','00001','03','zxc','20260501','20260501','1900','20260501','2222',NULL,NULL,NULL,NULL,'20260400010','2026-05-11 21:04:24'),('2026051100041','001','2026051100032','00001','01','uuuu','20260501','','','','','20260501','2222','20260501','2333','20260400010','2026-05-11 21:04:34'),('2026051200042','001','2026051100032','00001','01','사용자 요청 승인','20260501','20260501','2222','20260501','2333','20260501','1911','20260501','2022','20260400010','2026-05-12 21:17:05'),('2026051200043','001','2026051100032','00001','01','사용자 요청 승인','20260501','20260501','1911','20260501','2022','20260501','1911','20260501','2022','20260400010','2026-05-12 22:32:59'),('2026051200044','001','2026051200033','00001','01','사용자 요청 승인','20260514','','','','','20260514','0900','20260514','1800','20260400010','2026-05-12 22:33:59'),('2026051300045','001','2026051100032','00001','03','qwe','20260501','20260501','1911','20260501','2022',NULL,NULL,NULL,NULL,'20260400010','2026-05-13 20:05:50'),('2026051300046','001','2026051100030','00001','01','ryd','20260501','20260501','0900','20260501','1800','20260501','0900','20260501','2100','20260400010','2026-05-13 22:48:06'),('2026051300047','001','2026051300034','00001','01','qwe','20260505','','','','','20260505','0623','20260505','1800','20260400010','2026-05-13 22:50:28'),('2026051600048','001','2026051600035','00001','01','test','20260503','','','','','20260502','2355','20260503','0720','20260400010','2026-05-16 20:52:16'),('2026051600049','001','2026051600035','00001','01','trest2','20260503','20260502','2355','20260503','0720','20260502','2320','20260503','0850','20260400010','2026-05-16 20:52:53'),('2026051700050','001','2026051700036','00001','01','qweqwe','20260503','','','','','20260503','1212','20260503','1950','20260400010','2026-05-17 09:52:33'),('2026051700051','001','2026051700037','00001','01','test','20260502','','','','','20260502','0612','20260502','1932','20260400010','2026-05-17 17:30:03'),('2026051700052','001','P010ATTD0505001','00001','01','사용자 요청 승인','20260505','20260505','0900','20260505','1800','20260505','0830','20260505','1830','20260400010','2026-05-17 21:17:46'),('2026051700053','001','2026051700038','00001','07','테스트','20260507',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-05-17 21:29:59'),('2026051700054','001','2026051700039','00001','01','test','20260512','','','','','20260512','1859','20260512','2159','20260400010','2026-05-17 21:30:21'),('2026051700055','001','2026051700040','00001','01','사용자 요청 승인','20260512','','','','','20260512','1900','20260512','2200','20260400010','2026-05-17 21:30:37'),('2026052300056','001','2026052300047','00001','01','테스트','20260523','','','','','20260523','0900','20260523','1800','20260400012','2026-05-23 23:18:30'),('2026052400057','001','2026052400048','00001','01','ddd','20260515','','','','','20260515','0900','20260515','1800','20260400012','2026-05-24 16:56:20'),('2026052400058','001','2026052400049','00001','01','요청 승인 관리 승인','20260520',NULL,NULL,NULL,NULL,'20260520','0900','20260520','1800','20260400010','2026-05-24 22:42:11'),('2026052400059','001','2026052400049','00001','01','요청 승인 관리 승인','20260520',NULL,NULL,NULL,NULL,'20260520','0900','20260520','1800','20260400010','2026-05-24 22:56:10'),('2026052400060','001','2026052400049','00001','01','요청 승인 관리 승인','20260520',NULL,NULL,NULL,NULL,'20260520','0900','20260520','1800','20260400010','2026-05-24 23:08:21'),('2026052400061','001','2026052400050','00001','01','요청 승인 관리 승인','20261006',NULL,NULL,NULL,NULL,'20261006','0900','20261006','1800','20260400010','2026-05-24 23:40:45'),('2026052500062','001','2026050100901','00001','07','testtesttest','20260501',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-05-25 09:14:25'),('2026052500063','001','2026050100901','00001','01','요청 승인 관리 승인','20260501',NULL,NULL,NULL,NULL,'20260501','0900','20260501','1800','20260400010','2026-05-25 09:14:53'),('2026052500064','001','TEST_OT_ATT1','00001','09','123123','20260502',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-05-25 10:12:20'),('2026052500065','001','TEST_OT_ATT1','00001','08','마감 연장근무','20260502',NULL,NULL,NULL,NULL,'20260502','1830','20260502','2130','20260400010','2026-05-25 13:15:29'),('2026052500066','001','TEST_OT_ATT1','00001','08','마감 연장근무','20260502',NULL,NULL,NULL,NULL,'20260502','1830','20260502','2130','20260400010','2026-05-25 13:15:59'),('2026052500067','001','TEST_OT_ATT1','00001','09','ㅁㄴㅇㅁㄴㅇ','20260502',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-05-25 13:16:34'),('2026052500068','001','2026050100901','00001','01','요청 승인 관리 승인','20260501',NULL,NULL,NULL,NULL,'20260501','0900','20260501','1800','20260400010','2026-05-25 18:31:53'),('2026052500069','001','TEST_OT_ATT1','00001','08','마감 연장근무','20260502',NULL,NULL,NULL,NULL,'20260502','1830','20260502','2130','20260400010','2026-05-25 18:36:40'),('2026060200075','001','2026060200061','00001','01','사용자 요청 승인','20260602','20260602','2333','20260602','2333','20260601','2354','20260602','0712','20260400010','2026-06-02 23:39:39'),('2026060200076','001','2026060200062','00001','01','사용자 요청 승인','20260602','20260602','2333','20260602','2333','20260602','1233','20260602','1857','20260400010','2026-06-02 23:39:44'),('2026060300077','001','2026051700046','00001','07','qwe','20260514',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-06-03 11:47:06'),('2026060300078','001','2026051700045','00001','07','qwe','20260512',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-06-03 11:47:11'),('2026060300079','001','2026051700043','00001','07','asdasd','20260507',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-06-03 11:47:15'),('2026060300080','001','2026051700042','00001','07','zxczxc','20260505',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'20260400010','2026-06-03 11:47:19');
/*!40000 ALTER TABLE `tb_user_attd_hist` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_attd_mgmt`
--

LOCK TABLES `tb_user_attd_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_user_attd_mgmt` DISABLE KEYS */;
INSERT INTO `tb_user_attd_mgmt` VALUES ('2026050100901','001','00001','20260400013','20260501','n1',1,'20260501','0900','02',NULL,'20260501','1800','02',NULL,'N','20260400010','2026-05-25 09:14:53','20260400010','2026-05-25 18:31:53'),('2026051100030','001','00001','20260400010','20260501','n1',1,'20260501','0900','02',NULL,'20260501','2100','02',NULL,'N','20260400010','2026-05-11 21:04:11','20260400010','2026-05-13 22:48:06'),('2026051100031','001','00001','20260400010','20260501','n1',2,'20260501','1900','02',NULL,'20260501','2222','02',NULL,'Y','20260400010','2026-05-11 21:04:19','20260400010','2026-05-11 21:04:24'),('2026051100032','001','00001','20260400010','20260501','n1',2,'20260501','1911','02',NULL,'20260501','2022','02',NULL,'Y','20260400010','2026-05-11 21:04:34','20260400010','2026-05-13 20:05:50'),('2026051200033','001','00001','20260400013','20260514','n1',1,'20260514','0900','02',NULL,'20260514','1800','02',NULL,'N','20260400010','2026-05-12 22:33:59','20260400010','2026-05-12 22:33:59'),('2026051300034','001','00001','20260400013','20260505','n1',1,'20260505','0623','02',NULL,'20260505','1800','02',NULL,'N','20260400010','2026-05-13 22:50:28','20260400010','2026-05-13 22:50:28'),('2026051600035','001','00001','20260400010','20260503','n1',1,'20260502','2320','02',NULL,'20260503','0850','02',NULL,'N','20260400010','2026-05-16 20:52:16','20260400010','2026-05-16 20:52:53'),('2026051700036','001','00001','20260400010','20260503','n1',2,'20260503','1212','02',NULL,'20260503','1950','02',NULL,'N','20260400010','2026-05-17 09:52:33','20260400010','2026-05-17 09:52:33'),('2026051700037','001','00001','20260400010','20260502','n1',1,'20260502','0612','02',NULL,'20260502','1932','02',NULL,'N','20260400010','2026-05-17 17:30:03','20260400010','2026-05-17 17:30:03'),('2026051700039','001','00001','20260400010','20260512','n1',1,'20260512','1859','02',NULL,'20260512','2159','02',NULL,'N','20260400010','2026-05-17 21:30:21','20260400010','2026-05-17 21:30:21'),('2026051700040','001','00001','20260400010','20260512','n1',2,'20260512','1900','02',NULL,'20260512','2200','02',NULL,'N','20260400010','2026-05-17 21:30:37','20260400010','2026-05-17 21:30:37'),('2026051700042','001','00001','20260400010','20260505','n1',1,'20260505','0900','02',NULL,'20260505','1800','02',NULL,'N','P0101TST','2026-05-17 21:50:06',NULL,NULL),('2026051700044','001','00001','20260400010','20260509','n1',1,'20260509','0900','02',NULL,'20260509','1800','02',NULL,'N','P0101TST','2026-05-17 21:50:06',NULL,NULL),('2026051700046','001','00001','20260400010','20260514','n1',1,'20260514','0900','02',NULL,'20260514','1800','02',NULL,'N','P0101TST','2026-05-17 21:50:06',NULL,NULL),('2026052300047','001','00001','20260400012','20260523','n3',1,'20260523','0900','02',NULL,'20260523','1800','02',NULL,'N','20260400012','2026-05-23 23:18:30','20260400012','2026-05-23 23:18:30'),('2026052400048','001','00001','20260400012','20260515','n3',1,'20260515','0900','02',NULL,'20260515','1800','02',NULL,'N','20260400012','2026-05-24 16:56:20','20260400012','2026-05-24 16:56:20'),('2026052400049','001','00001','20260400013','20260520','n1',1,'20260520','0900','02',NULL,'20260520','1800','02',NULL,'N','20260400010','2026-05-24 22:42:11','20260400010','2026-05-24 23:08:21'),('2026052400050','001','00001','20260400013','20261006','n1',1,'20261006','0900','02',NULL,'20261006','1800','02',NULL,'N','20260400010','2026-05-24 23:40:45','20260400010','2026-05-24 23:40:45'),('2026053100051','001','00001','20260400013','20260531','n1',1,'20260531','1938','01',NULL,'20260531','2156','01',NULL,'N','20260400013','2026-05-31 19:38:45','20260400013','2026-05-31 21:56:12'),('2026060100055','001','00001','20260400013','20260601','n1',1,'20260601','2045','01',NULL,'20260601','2045','01',NULL,'N','20260400013','2026-06-01 20:45:06','20260400013','2026-06-01 20:45:08'),('2026060100056','001','00001','20260400013','20260601','n1',2,'20260601','2048','01',NULL,'20260601','2048','01',NULL,'N','20260400013','2026-06-01 20:48:11','20260400013','2026-06-01 20:48:27'),('2026060200061','001','00001','20260400013','20260602','n1',1,'20260601','2354','01',NULL,'20260602','0712','01',NULL,'N','20260400013','2026-06-02 23:33:36','20260400010','2026-06-02 23:39:39'),('2026060200062','001','00001','20260400013','20260602','n1',2,'20260602','1233','01',NULL,'20260602','1857','01',NULL,'N','20260400013','2026-06-02 23:33:47','20260400010','2026-06-02 23:39:44'),('2026060300067','001','00001','20260400013','20260603','n1',1,'20260603','2238','01','5d87af74336ab93f','20260603','2238','01','5d87af74336ab93f','N','20260400013','2026-06-03 22:38:00','20260400013','2026-06-03 22:38:02'),('2026060300068','001','00001','20260400013','20260603','n1',2,'20260603','2238','01','5d87af74336ab93f','20260603','2238','01','5d87af74336ab93f','N','20260400013','2026-06-03 22:38:04','20260400013','2026-06-03 22:38:10'),('2026060800069','001','00001','20260400013','20260608','n1',1,'20260608','2012','01','5d87af74336ab93f',NULL,NULL,NULL,NULL,'N','20260400013','2026-06-08 20:12:11',NULL,NULL),('TEST_OT_ATT1','001','00001','20260400013','20260502','n1',1,'20260502','0930','02',NULL,'20260502','2200','02',NULL,'N','TESTDATA','2026-05-25 18:32:22',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_attd_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_attd_req`
--

LOCK TABLES `tb_user_attd_req` WRITE;
/*!40000 ALTER TABLE `tb_user_attd_req` DISABLE KEYS */;
INSERT INTO `tb_user_attd_req` VALUES ('2026051700001','001','00001','20260400010','02','2026051700042','03','P0101TST case1 modify req','20260505','n1',1,'20260505','0830','20260505','1830',NULL,NULL,NULL,'20260400010','zxczxc','2026-06-03 11:47:19','N','P0101TST','2026-05-17 21:50:06','20260400010','2026-06-03 11:47:19'),('2026051700002','001','00001','20260400010','01','2026051700043','03','P0101TST case2 create req','20260507','n1',1,'20260507','0900','20260507','1800',NULL,NULL,NULL,'20260400010','asdasd','2026-06-03 11:47:15','N','P0101TST','2026-05-17 21:50:06','20260400010','2026-06-03 11:47:15'),('2026051700003','001','00001','20260400010','01','2026051700045','03','P0101TST case4 create req seg2','20260512','n1',2,'20260512','1900','20260512','2200',NULL,NULL,NULL,'20260400010','qwe','2026-06-03 11:47:11','N','P0101TST','2026-05-17 21:50:06','20260400010','2026-06-03 11:47:11'),('2026051700004','001','00001','20260400010','02','2026051700046','03','P0101TST case5 modify req outside','20260514','n1',1,'20260514','0800','20260514','1700',NULL,NULL,NULL,'20260400010','qwe','2026-06-03 11:47:06','N','P0101TST','2026-05-17 21:50:06','20260400010','2026-06-03 11:47:06'),('2026051700005','001','00001','20260400013','05','00016','03','TEST','20260523','n1',NULL,NULL,NULL,NULL,NULL,'00016',1.00000,NULL,'20260400010','asd','2026-06-03 11:47:32','N','SYSTEM','2026-05-17 21:50:06','20260400010','2026-06-03 11:47:32'),('2026060200011','001','00001','20260400013','02','2026060200061','02','테스트1','20260602','n1',1,'20260601','2354','20260602','0712',NULL,NULL,NULL,'20260400010','사용자 요청 승인','2026-06-02 23:39:39','N','20260400013','2026-06-02 23:34:22','20260400010','2026-06-02 23:39:39'),('2026060200012','001','00001','20260400013','02','2026060200062','02','테스트1','20260602','n1',2,'20260602','1233','20260602','1857',NULL,NULL,NULL,'20260400010','사용자 요청 승인','2026-06-02 23:39:44','N','20260400013','2026-06-02 23:34:22','20260400010','2026-06-02 23:39:44'),('2026060200013','001','00001','20260400013','10',NULL,'03','테스트1','20260604','n1',1,NULL,NULL,NULL,NULL,NULL,NULL,'00003','20260400010','ㅅㄷㄴㅅ','2026-06-02 23:57:10','N','20260400013','2026-06-02 23:42:53','20260400010','2026-06-02 23:57:10'),('2026060200014','001','00001','20260400013','10',NULL,'02','테스트11','20260604','n1',1,NULL,NULL,NULL,NULL,NULL,NULL,'00003','20260400010','SCHED_MODIFY_APPROVED','2026-06-03 00:02:51','N','20260400013','2026-06-02 23:57:48','20260400010','2026-06-03 00:02:51'),('2026060300027','001','00001','20260400013','05',NULL,'02','테스트','20260604',NULL,NULL,'20260604','1000','20260604','1100',NULL,0.12500,NULL,NULL,NULL,NULL,'N','20260400013','2026-06-03 22:39:41',NULL,NULL),('2026060300028','001','00001','20260400013','03',NULL,'01','ㅂㅂㅂ','20260603','n1',1,'20260603','2238','20260603','2259',NULL,NULL,NULL,NULL,NULL,NULL,'N','20260400013','2026-06-03 22:42:55',NULL,NULL),('TEST_AT_01','001','00001','20260400013','01','2026050100901','02','출근 기록 누락 보정','20260501','n1',1,'20260501','0900','20260501','1800',NULL,NULL,NULL,'20260400010','요청 승인 관리 승인','2026-05-25 18:31:53','N','TESTDATA','2026-05-25 18:30:47','20260400010','2026-05-25 18:31:53'),('TEST_LV_01','001','00001','20260400013','05',NULL,'02','연차 사용 신청','20261008','n1',NULL,'20261008',NULL,'20261008',NULL,'ANNUALdma ',1.00000,NULL,'20260400010','','2026-05-25 18:37:44','N','TESTDATA','2026-05-25 18:36:55','20260400010','2026-05-25 18:37:44'),('TEST_OT_01','001','00001','20260400013','03',NULL,'02','마감 연장근무','20260502','n1',NULL,'20260502','1830','20260502','2130',NULL,NULL,NULL,'20260400010','OT_APPROVED','2026-05-25 18:36:40','N','TESTDATA','2026-05-25 18:32:24','20260400010','2026-05-25 18:36:40'),('TEST_REQ_01','001','00001','20260400013','05',NULL,'01','개인 사유 연차','20260605','n1',NULL,'20260605',NULL,'20260605',NULL,'ANNUAL',1.00000,NULL,NULL,NULL,NULL,'N','TESTDATA','2026-05-24 23:19:39',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_attd_req` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_attd_req_approval`
--

LOCK TABLES `tb_user_attd_req_approval` WRITE;
/*!40000 ALTER TABLE `tb_user_attd_req_approval` DISABLE KEYS */;
INSERT INTO `tb_user_attd_req_approval` VALUES ('2026051700005',1,'001','20260400010','03','asd','2026-06-03 11:47:32','20260400013','2026-05-17 21:50:06','20260400010','2026-06-03 11:47:32'),('TEST_LV_01',1,'001','20260400010','02','','2026-05-25 18:37:44','TESTDATA','2026-05-25 18:36:57','20260400010','2026-05-25 18:37:44'),('TEST_REQ_01',1,'001','20260400012','01',NULL,NULL,'TESTDATA','2026-05-24 23:19:42',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_attd_req_approval` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_device`
--

LOCK TABLES `tb_user_device` WRITE;
/*!40000 ALTER TABLE `tb_user_device` DISABLE KEYS */;
INSERT INTO `tb_user_device` VALUES ('5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0',NULL,'2026-06-08 20:11:38','172.30.1.51','20260400013','2026-06-03 21:08:44','20260400013','2026-06-08 20:11:38','N');
/*!40000 ALTER TABLE `tb_user_device` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_device_login_hist`
--

LOCK TABLES `tb_user_device_login_hist` WRITE;
/*!40000 ALTER TABLE `tb_user_device_login_hist` DISABLE KEYS */;
INSERT INTO `tb_user_device_login_hist` VALUES ('20260600001','001','5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0','APP','172.30.1.76','2026-06-03 21:08:44','20260400013','2026-06-03 21:08:44'),('20260600002','001','5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0','APP','172.30.1.76','2026-06-03 21:09:37','20260400013','2026-06-03 21:09:37'),('20260600003','001','5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0','APP','172.30.1.76','2026-06-03 21:37:08','20260400013','2026-06-03 21:37:08'),('20260600004','001','5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0','APP','172.30.1.76','2026-06-03 22:37:38','20260400013','2026-06-03 22:37:38'),('20260600005','001','5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0','APP','172.30.1.68','2026-06-04 20:18:39','20260400013','2026-06-04 20:18:39'),('20260600006','001','5d87af74336ab93f','20260400013','ANDROID','SM-S938N','16','1.0.0','APP','172.30.1.51','2026-06-08 20:11:38','20260400013','2026-06-08 20:11:38');
/*!40000 ALTER TABLE `tb_user_device_login_hist` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_hire_date_history`
--

LOCK TABLES `tb_user_hire_date_history` WRITE;
/*!40000 ALTER TABLE `tb_user_hire_date_history` DISABLE KEYS */;
INSERT INTO `tb_user_hire_date_history` VALUES ('HH2026052300001','001','20260400013','20221115','20221115','test','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"부여 엔진 적용 후 산정\", \"scenarioLabel\": \"1년 이상 · 변동 없음\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 변동이 없습니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'Y','2026-05-24 16:32:41','20260400010','20260400010','2026-05-23 15:50:13'),('HH2026052300002','001','20260400012','20260102','20260102','test','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"부여 엔진 적용 후 산정\", \"scenarioLabel\": \"1년 미만 · 변동 없음\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 변동이 없습니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-23 22:43:39'),('HH2026052300003','001','20260400012','20260102','20250917','test','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"부여 엔진 적용 후 산정\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 107일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-23 22:44:37'),('HH2026052300004','001','20260400012','20250917','20250915','test','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"부여 엔진 적용 후 산정\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 2일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-23 22:45:32'),('HH2026052300005','001','20260400012','20250915','20240703','test','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"부여 엔진 적용 후 산정\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 439일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-23 22:46:03'),('HH2026052300006','001','20260400011','20250919','20250919','test','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"부여 엔진 적용 후 산정\", \"scenarioLabel\": \"1년 미만 · 변동 없음\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 변동이 없습니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'N',NULL,NULL,'20260400010','2026-05-23 22:46:21'),('HH2026052400007','001','20260400012','20240703','20260723','test','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-23\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 750일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"4일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-24 17:31:27'),('HH2026052400008','001','20260400012','20260723','20260724','test1','KEEP_AND_APPLY_NEW','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-24\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 1일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"4일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-24 17:36:07'),('HH2026052400009','001','20260400011','20250919','20240918','tesqweqweqw','KEEP_AND_APPLY_NEW','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-09-18\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"7일\", \"changeSummaryText\": \"입사일 366일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"8일\"}',NULL,NULL,NULL,'N',NULL,NULL,'20260400010','2026-05-24 18:14:22'),('HH2026052400010','001','20260400012','20260724','20250725','qweqweasdasdsa','KEEP_AND_APPLY_NEW','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-25\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"5일\", \"changeSummaryText\": \"입사일 364일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"4일\"}',NULL,NULL,NULL,'Y','2026-05-24 22:59:49','20260400010','20260400010','2026-05-24 18:14:53'),('HH2026052500011','001','20260400013','20221115','20220503','ㄷㅂㅈㄷㅅㄷㄴㅅㅅㄷㄴㄹㄴㅇ','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2027-05-03\", \"scenarioLabel\": \"1년 이상 · 입사일 과거로\", \"missingGrantText\": \"16일\", \"changeSummaryText\": \"입사일 196일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"16일\"}',NULL,NULL,NULL,'Y','2026-05-25 15:07:53','20260400010','20260400010','2026-05-25 15:07:34'),('HH2026052500012','001','20260400013','20220503','20250918','qwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-09-18\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 1234일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"32일\"}',NULL,NULL,NULL,'Y','2026-05-25 18:39:43','20260400010','20260400010','2026-05-25 18:39:32'),('HH2026052500013','001','20260400013','20250918','20250716','zxc','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-16\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 64일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"16일\"}',NULL,NULL,NULL,'Y','2026-05-25 18:42:44','20260400010','20260400010','2026-05-25 18:42:31'),('HH2026052500014','001','20260400013','20250716','20250918','ㅂㅈㄷ','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-09-18\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 64일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"12일\"}',NULL,NULL,NULL,'Y','2026-05-25 19:25:46','20260400010','20260400010','2026-05-25 19:25:06'),('HH2026052500015','001','20260400013','20250918','20250916','ㅂㅈㄷㅂㅈㄷ','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-09-16\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 2일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"12일\"}',NULL,NULL,NULL,'Y','2026-05-25 19:25:46','20260400010','20260400010','2026-05-25 19:25:36'),('HH2026052500016','001','20260400013','20250916','20230913','qweqwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-09-13\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 734일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"8일\"}',NULL,NULL,NULL,'Y','2026-05-25 19:56:48','20260400010','20260400010','2026-05-25 19:52:51'),('HH2026052500017','001','20260400013','20230913','20250717','qweqw','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-17\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 673일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"15일\"}',NULL,NULL,NULL,'Y','2026-05-25 20:05:17','20260400010','20260400010','2026-05-25 20:05:10'),('HH2026052500018','001','20260400013','20250717','20250514','sdfsdf','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2027-05-14\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 64일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 20:06:01','20260400010','20260400010','2026-05-25 20:05:52'),('HH2026052500019','001','20260400013','20250514','20230706','asdasd','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-06\", \"scenarioLabel\": \"1년 이상 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 678일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"28일\"}',NULL,NULL,NULL,'Y','2026-05-25 20:06:54','20260400010','20260400010','2026-05-25 20:06:37'),('HH2026052500020','001','20260400013','20230706','20251005','qweqwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-10-05\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 822일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"15일\"}',NULL,NULL,NULL,'Y','2026-05-25 20:09:28','20260400010','20260400010','2026-05-25 20:09:21'),('HH2026052500021','001','20260400013','20251005','20250502','dsfgfd','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2027-05-02\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 156일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"7일\"}',NULL,NULL,NULL,'Y','2026-05-25 20:11:13','20260400010','20260400010','2026-05-25 20:10:29'),('HH2026052500022','001','20260400013','20250502','20250717','fdsfs','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-17\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 76일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"26일\"}',NULL,NULL,NULL,'Y','2026-05-25 20:12:41','20260400010','20260400010','2026-05-25 20:11:50'),('HH2026052500023','001','20260400013','20250717','20250711','test','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 6일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 22:15:52','20260400010','20260400010','2026-05-25 22:15:43'),('HH2026052500024','001','20260400013','20250711','20250618','qwe','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-06-18\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 23일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 22:18:19','20260400010','20260400010','2026-05-25 22:17:23'),('HH2026052500025','001','20260400013','20250618','20250711','qwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 23일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 22:18:19','20260400010','20260400010','2026-05-25 22:18:09'),('HH2026052500026','001','20260400013','20250711','20250618','asdasd','KEEP_AND_APPLY_NEW','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-06-18\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 23일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 22:21:40','20260400010','20260400010','2026-05-25 22:19:00'),('HH2026052500027','001','20260400013','20250618','20250711','qwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 23일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 22:21:40','20260400010','20260400010','2026-05-25 22:21:34'),('HH2026052500028','001','20260400013','20250711','20250605','asdasd','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-06-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-25 23:07:00','20260400010','20260400010','2026-05-25 22:21:58'),('HH2026052600029','001','20260400013','20250605','20250711','qwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"11일\"}',NULL,NULL,NULL,'Y','2026-05-26 18:39:38','20260400010','20260400010','2026-05-26 18:39:29'),('HH2026052600030','001','20260400013','20250711','20250605','qwe','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-06-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-26 18:40:28','20260400010','20260400010','2026-05-26 18:40:21'),('HH2026052600031','001','20260400013','20250605','20250711','qwe','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"11일\"}',NULL,NULL,NULL,'Y','2026-05-26 18:41:05','20260400010','20260400010','2026-05-26 18:41:00'),('HH2026052600032','001','20260400013','20250711','20250605','zxczxc','KEEP_AND_APPLY_NEW','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-06-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-26 18:41:28','20260400010','20260400010','2026-05-26 18:41:23'),('HH2026052600033','001','20260400013','20250605','20250711','asdasd','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"11일\"}',NULL,NULL,NULL,'Y','2026-05-26 18:41:54','20260400010','20260400010','2026-05-26 18:41:47'),('HH2026052600034','001','20260400013','20250711','20250605','azczzxc','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-06-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-26 18:42:18','20260400010','20260400010','2026-05-26 18:42:09'),('HH2026052600035','001','20260400013','20250605','20250711','zxc','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 36일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"11일\"}',NULL,NULL,NULL,'Y','2026-05-26 19:40:18','20260400010','20260400010','2026-05-26 19:40:11'),('HH2026052600036','001','20260400013','20250711','20250505','qweqwe','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2027-05-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 67일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-26 19:40:42','20260400010','20260400010','2026-05-26 19:40:30'),('HH2026052600037','001','20260400013','20250505','20270711','asd','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 797일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"27일\"}',NULL,NULL,NULL,'Y','2026-05-26 22:09:03','20260400010','20260400010','2026-05-26 22:08:55'),('HH2026052600038','001','20260400013','20270711','20250711','zxczxc','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 730일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"0일\"}',NULL,NULL,NULL,'Y','2026-05-26 22:14:43','20260400010','20260400010','2026-05-26 22:09:29'),('HH2026052600039','001','20260400013','20250711','20250711','연차 부여 엔진 테스트','RESET_ALL',NULL,NULL,NULL,NULL,'Y','2026-05-26 22:14:43','20260400010','20260400013','2026-05-26 22:14:34'),('HH2026052600040','001','20260400013','20250711','20250505','ㅁㄴㅇㅁㄴㅇ','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2027-05-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"5일\", \"changeSummaryText\": \"입사일 67일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-26 22:20:41','20260400010','20260400010','2026-05-26 22:20:30'),('HH2026052600041','001','20260400013','20250505','20250711','ㅋㅌㅊ','RESET_ALL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-07-11\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 67일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"17일\"}',NULL,NULL,NULL,'Y','2026-05-26 22:21:19','20260400010','20260400010','2026-05-26 22:21:12'),('HH2026052600042','001','20260400013','20250711','20250505','ㅋㅌㅊㅋㅌㅊ','KEEP_AND_BACKFILL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2027-05-05\", \"scenarioLabel\": \"1년 미만 · 입사일 과거로\", \"missingGrantText\": \"5일\", \"changeSummaryText\": \"입사일 67일 앞당김 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"10일\"}',NULL,NULL,NULL,'Y','2026-05-26 22:32:00','20260400010','20260400010','2026-05-26 22:31:55'),('HH2026052700043','001','20260400013','20250505','20250815','test','MANUAL','{\"usedText\": \"0일\", \"approximated\": true, \"nextGrantText\": \"2026-08-15\", \"scenarioLabel\": \"1년 이상 · 입사일 미래로\", \"missingGrantText\": \"0일\", \"changeSummaryText\": \"입사일 102일 미룸 · 근속 기준일이 함께 이동합니다.\", \"existingGrantText\": \"17일\"}',17.0,17.0,NULL,'N',NULL,NULL,'20260400010','2026-05-27 22:03:33'),('HH2026052700044','001','20260400013','20250815','20250602','asdasd','MANUAL','[{\"action\": \"CANCELED\", \"recall\": \"1\", \"availTo\": \"20260605\", \"grantId\": \"G2026052500097\", \"usedDays\": \"0\", \"grantType\": \"STATUTORY_MONTHLY\", \"afterGrantDays\": \"1\", \"beforeGrantDays\": \"1\"}, {\"action\": \"CANCELED\", \"recall\": \"1\", \"availTo\": \"20260705\", \"grantId\": \"G2026052500098\", \"usedDays\": \"0\", \"grantType\": \"STATUTORY_MONTHLY\", \"afterGrantDays\": \"1\", \"beforeGrantDays\": \"1\"}, {\"action\": \"CANCELED\", \"recall\": \"1\", \"availTo\": \"20260811\", \"grantId\": \"G2026052600229\", \"usedDays\": \"0\", \"grantType\": \"STATUTORY_MONTHLY\", \"afterGrantDays\": \"1\", \"beforeGrantDays\": \"1\"}]',17.0,14.0,'qweqwe','N',NULL,NULL,'20260400010','2026-05-27 22:07:43');
/*!40000 ALTER TABLE `tb_user_hire_date_history` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_leave_grant`
--

LOCK TABLES `tb_user_leave_grant` WRITE;
/*!40000 ALTER TABLE `tb_user_leave_grant` DISABLE KEYS */;
INSERT INTO `tb_user_leave_grant` VALUES ('G2026052400025','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20250825','20270825','20260400012_202508_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400026','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20250925','20270925','20260400012_202509_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400027','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20251025','20271025','20260400012_202510_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400028','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20251125','20271125','20260400012_202511_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400029','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20251225','20271225','20260400012_202512_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400030','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20260125','20280125','20260400012_202601_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400031','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20260225','20280225','20260400012_202602_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400032','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20260325','20280325','20260400012_202603_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052400033','001','20260400012','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260524','20260425','20280425','20260400012_202604_STATUTORY_MONTHLY','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-24 23:00:33',NULL,NULL),('G2026052500034','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',15.0,0.00000,'정책 기준 연차 부여','01',6,'20260526','20260526','20270526','20260400013_2026_STATUTORY_ANNUAL','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 13:32:55','20260400010','2026-05-26 22:09:03'),('G2026052500035','001','20260400013','SYS_TENURE_BONUS','STATUTORY_TENURE_BONUS',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260525','20260525','20280525','20260400013_2026_STATUTORY_TENURE_BONUS','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 13:32:55','20260400010','2026-05-25 18:39:43'),('G2026052500036','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',15.0,0.00000,'정책 기준 연차 부여','01',4,'20260525','20250503','20270503','20260400013_2025_STATUTORY_ANNUAL','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 15:07:53','20260400010','2026-05-25 18:39:43'),('G2026052500037','001','20260400013','SYS_TENURE_BONUS','STATUTORY_TENURE_BONUS',1.0,0.00000,'정책 기준 연차 부여','01',4,'20260525','20250503','20270503','20260400013_2025_STATUTORY_TENURE_BONUS','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 15:07:53','20260400010','2026-05-25 18:39:43'),('G2026052500038','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251018','20261018','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500039','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251118','20261118','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500040','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251218','20261218','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500041','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260118','20270118','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500042','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260218','20270218','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500043','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260318','20270318','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500044','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260418','20270418','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500045','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260518','20270518','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500012','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:39:43','20260400010','2026-05-25 18:42:44'),('G2026052500046','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20251018','20261018','20260400013_202510_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500047','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20251118','20261118','20260400013_202511_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500048','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20251218','20261218','20260400013_202512_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500049','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20260118','20270118','20260400013_202601_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500050','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20260218','20270218','20260400013_202602_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500051','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20260318','20270318','20260400013_202603_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500052','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20260418','20270418','20260400013_202604_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500053','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20260518','20270518','20260400013_202605_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:40:01','20260400010','2026-05-25 18:42:44'),('G2026052500054','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20250816','20260816','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500055','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20250916','20260916','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500056','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251016','20261016','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500057','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251116','20261116','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500058','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251216','20261216','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500059','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260116','20270116','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500060','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260216','20270216','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500061','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260316','20270316','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500062','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260416','20270416','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500063','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260516','20270516','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500013','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:44','20260400010','2026-05-25 19:25:46'),('G2026052500064','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20250816','20260816','20260400013_202508_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:55','20260400010','2026-05-25 19:25:46'),('G2026052500065','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'정책 기준 연차 부여','01',5,'20260525','20250916','20260916','20260400013_202509_STATUTORY_MONTHLY','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 18:42:55','20260400010','2026-05-25 19:25:46'),('G2026052500066','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251016','20261016','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500067','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251116','20261116','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500068','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20251216','20261216','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500069','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260116','20270116','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500070','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260216','20270216','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500071','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260316','20270316','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500072','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260416','20270416','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500073','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',5,'20260525','20260516','20270516','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500015','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:25:46','20260400010','2026-05-25 19:56:48'),('G2026052500074','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',15.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260525','20270525','20260400013_2025_STATUTORY_ANNUAL_RHH2026052500016','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 19:56:48','20260400010','2026-05-25 20:05:17'),('G2026052500075','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250817','20260817','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500076','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250917','20260917','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500077','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251017','20261017','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500078','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251117','20261117','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500079','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251217','20261217','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500080','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260117','20270117','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500081','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260217','20270217','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500082','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260317','20270317','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500083','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260417','20270417','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500084','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260517','20270517','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500017','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:05:17','20260400010','2026-05-25 20:06:01'),('G2026052500085','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',15.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260525','20270525','20260400013_2026_STATUTORY_ANNUAL_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500086','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250614','20260614','20260400013_202506_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500087','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250714','20260714','20260400013_202507_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500088','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250814','20260814','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500089','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250914','20260914','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500090','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251014','20261014','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500091','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251114','20261114','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500092','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251214','20261214','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500093','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260114','20270114','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500094','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260214','20270214','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500095','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260314','20270314','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500096','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260414','20270414','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500018','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:01','20260400010','2026-05-25 20:06:54'),('G2026052500097','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'[입사일변경 회수 HH2026052700044] qweqwe','01',6,'20260526','20250605','20260605','20260400013_202506_STATUTORY_MONTHLY','CANCELED','N',NULL,'qweqwe','2026-05-27 22:07:43','20260400010','N','20260400010','2026-05-25 20:06:17','20260400010','2026-05-27 22:07:43'),('G2026052500098','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'[입사일변경 회수 HH2026052700044] qweqwe','01',6,'20260526','20250705','20260705','20260400013_202507_STATUTORY_MONTHLY','CANCELED','N',NULL,'qweqwe','2026-05-27 22:07:43','20260400010','N','20260400010','2026-05-25 20:06:17','20260400010','2026-05-27 22:07:43'),('G2026052500099','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',15.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260525','20270525','20260400013_2025_STATUTORY_ANNUAL_RHH2026052500019','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:06:54','20260400010','2026-05-25 20:09:28'),('G2026052500100','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251105','20261105','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500101','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251205','20261205','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500102','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260105','20270105','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500103','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260205','20270205','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500104','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260305','20270305','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500105','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260405','20270405','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500106','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260505','20270505','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500020','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:09:28','20260400010','2026-05-25 20:11:13'),('G2026052500107','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',15.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260525','20270525','20260400013_2026_STATUTORY_ANNUAL_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500108','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250602','20260602','20260400013_202506_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500109','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250702','20260702','20260400013_202507_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500110','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250802','20260802','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500111','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250902','20260902','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500112','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251002','20261002','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500113','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251102','20261102','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500114','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251202','20261202','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500115','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260102','20270102','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500116','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260202','20270202','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500117','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260302','20270302','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500118','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260402','20270402','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500021','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:11:13','20260400010','2026-05-25 20:12:41'),('G2026052500119','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250817','20260817','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500120','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250917','20260917','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500121','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251017','20261017','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500122','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251117','20261117','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500123','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251217','20261217','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500124','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260117','20270117','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500125','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260217','20270217','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500126','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260317','20270317','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500127','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260417','20270417','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500128','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260517','20270517','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500022','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 20:12:41','20260400010','2026-05-25 22:15:52'),('G2026052500129','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500130','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500131','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500132','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500133','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500134','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500135','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500136','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500137','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500138','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500023','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:15:52','20260400010','2026-05-25 22:18:19'),('G2026052500139','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500140','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500141','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500142','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500143','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500144','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500145','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500146','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500147','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500148','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500025','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:18:19','20260400010','2026-05-25 22:21:40'),('G2026052500149','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500150','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500151','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500152','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500153','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500154','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500155','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500156','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500157','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052500158','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260525','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052500027','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-25 22:21:40','20260400010','2026-05-26 18:39:38'),('G2026052600160','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600161','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600162','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600163','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600164','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600165','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600166','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600167','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600168','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600169','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600029','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:39:38','20260400010','2026-05-26 18:41:05'),('G2026052600171','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600172','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600173','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600174','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600175','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600176','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600177','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600178','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600179','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600180','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600031','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:05','20260400010','2026-05-26 18:41:54'),('G2026052600182','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600183','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600184','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600185','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600186','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600187','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600188','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600189','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600190','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600191','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600033','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:41:54','20260400010','2026-05-26 18:42:18'),('G2026052600192','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250705','20260705','20260400013_202507_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600193','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250805','20260805','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600194','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250905','20260905','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600195','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251005','20261005','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600196','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251105','20261105','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600197','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251205','20261205','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600198','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260105','20270105','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600199','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260205','20270205','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600200','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260305','20270305','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600201','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260405','20270405','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600202','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260505','20270505','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600034','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 18:42:18','20260400010','2026-05-26 19:40:18'),('G2026052600203','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600204','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600205','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600206','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600207','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600208','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600209','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600210','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600211','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600212','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600035','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 19:40:18','20260400010','2026-05-26 22:09:03'),('G2026052600216','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600217','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600218','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600219','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600220','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600221','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600222','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600223','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600224','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600225','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600039','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:14:43','20260400010','2026-05-26 22:21:19'),('G2026052600226','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',5.0,0.00000,'입사일 변경 보전(INSADAY_CHANGE_BACKFILL)','01',6,'20260526','20260526','20270505','20260400013_2026_STATUTORY_ANNUAL_BFHH2026052600040','CANCELED','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:20:41','20260400010','2026-05-26 22:21:19'),('G2026052600229','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'[입사일변경 회수 HH2026052700044] qweqwe','01',6,'20260526','20250811','20260811','20260400013_202508_STATUTORY_MONTHLY_RHH2026052600041','CANCELED','N',NULL,'qweqwe','2026-05-27 22:07:43','20260400010','N','20260400010','2026-05-26 22:21:19','20260400010','2026-05-27 22:07:43'),('G2026052600230','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.12500,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20250911','20260911','20260400013_202509_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19','20260400013','2026-06-03 22:39:42'),('G2026052600231','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251011','20261011','20260400013_202510_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600232','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251111','20261111','20260400013_202511_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600233','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20251211','20261211','20260400013_202512_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600234','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260111','20270111','20260400013_202601_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600235','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260211','20270211','20260400013_202602_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600236','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260311','20270311','20260400013_202603_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600237','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260411','20270411','20260400013_202604_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600238','001','20260400013','SYS_MONTHLY','STATUTORY_MONTHLY',1.0,0.00000,'입사일 변경(RESET_ALL) 재발급','01',6,'20260526','20260511','20270511','20260400013_202605_STATUTORY_MONTHLY_RHH2026052600041','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:21:19',NULL,NULL),('G2026052600239','001','20260400013','SYS_ANNUAL','STATUTORY_ANNUAL',5.0,0.00000,'입사일 변경 보전(INSADAY_CHANGE_BACKFILL)','01',6,'20260526','20260526','20270505','20260400013_2026_STATUTORY_ANNUAL_BFHH2026052600042','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-05-26 22:32:00','TEST_RESET','2026-06-01 20:44:56'),('G2026052700242','001','20260400012','00018','MANUAL_OTHER',1.0,0.00000,'test','02',NULL,'20260527','20260528','20270528','20260400012_20260527035903299_MANUAL','CANCELED','N',NULL,'ㅂㅈㄷ','2026-05-27 19:00:46','20260400010','N','20260400010','2026-05-27 18:52:07','20260400010','2026-05-27 19:00:46'),('G2026060300243','001','20260400013','00018','MANUAL_OTHER',2.0,0.00000,'테스ㅡ','02',NULL,'20260603','20260601','20270601','20260400013_20260603581475700_MANUAL','ACTIVE','N',NULL,NULL,NULL,NULL,'N','20260400010','2026-06-03 21:14:37',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_leave_grant` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_leave_use`
--

LOCK TABLES `tb_user_leave_use` WRITE;
/*!40000 ALTER TABLE `tb_user_leave_use` DISABLE KEYS */;
INSERT INTO `tb_user_leave_use` (`LEAVE_ID`, `CMPNY_CD`, `SITE_CD`, `USER_CD`, `LEAVE_CD`, `REQ_ID`, `GRANT_ID`, `START_DATE`, `START_TIME`, `END_DATE`, `END_TIME`, `USE_UNIT_TYPE`, `LEAVE_DAYS`, `LEAVE_MINUTES`, `LEAVE_REASON`, `EVIDENCE_FILE_ID`, `LEAVE_STATUS`, `CANCEL_REASON`, `CANCEL_DATE`, `DEL_YN`, `INSERT_NO`, `INSERT_DATE`, `UPDATE_NO`, `UPDATE_DATE`) VALUES ('LV2026060300003','001','00001','20260400013','SYS_MONTHLY','2026060300027','G2026052600230','20260604','1000','20260604','1100','04',0.12500,60,'테스트',NULL,'CONFIRMED',NULL,NULL,'N','20260400013','2026-06-03 22:39:42',NULL,NULL),('TEST_LU_01','001','00001','20260400013','SYS_ANNUAL','TEST_REQ_01','G2026052400016','20260605',NULL,'20260605',NULL,'00',1.00000,NULL,'개인 사유 연차',NULL,'CONFIRMED',NULL,NULL,'N','TESTDATA','2026-05-24 23:19:43',NULL,NULL),('TEST_LV_USE1','001','00001','20260400013','SYS_ANNUAL','TEST_LV_01','TEST_GRANT_01','20261008',NULL,'20261008',NULL,'00',1.00000,NULL,'연차 사용 신청',NULL,'CONFIRMED',NULL,NULL,'N','TESTDATA','2026-05-25 18:36:59',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_leave_use` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_overtime_mgmt`
--

LOCK TABLES `tb_user_overtime_mgmt` WRITE;
/*!40000 ALTER TABLE `tb_user_overtime_mgmt` DISABLE KEYS */;
INSERT INTO `tb_user_overtime_mgmt` VALUES ('2026051400001','001','00001','20260400010','2026051100030',NULL,'20260501','n1','20260501','1830','20260501','1900','20260501','1830',NULL,'20260501','1900',NULL,30,0,'COMPLETED','N','20260400010','2026-05-14 22:12:49',NULL,NULL),('2026051600011','001','00001','20260400010','2026051600035',NULL,'20260503','n1','20260502','2330','20260503','0000','20260502','2330',NULL,'20260503','0000',NULL,30,0,'COMPLETED','N','20260400010','2026-05-16 22:45:35',NULL,NULL),('2026051600012','001','00001','20260400010','2026051600035',NULL,'20260503','n1','20260503','0700','20260503','0850','20260503','0700',NULL,'20260503','0850',NULL,110,0,'COMPLETED','N','20260400010','2026-05-16 22:45:35',NULL,NULL),('2026051700013','001','00001','20260400010','2026051700036',NULL,'20260503','n1','20260503','1230','20260503','1300','20260503','1230',NULL,'20260503','1300',NULL,30,0,'COMPLETED','N','20260400010','2026-05-17 09:53:10',NULL,NULL),('2026051700014','001','00001','20260400010','2026051700036',NULL,'20260503','n1','20260503','1800','20260503','1930','20260503','1800',NULL,'20260503','1930',NULL,90,0,'COMPLETED','N','20260400010','2026-05-17 09:53:10',NULL,NULL),('2026051700015','001','00001','20260400010','2026051700037',NULL,'20260502','n1','20260502','0612','20260502','0700','20260502','0612',NULL,'20260502','0700',NULL,48,0,'COMPLETED','N','20260400010','2026-05-17 17:30:54',NULL,NULL),('2026051700016','001','00001','20260400010','2026051700037',NULL,'20260502','n1','20260502','1500','20260502','1920','20260502','1500',NULL,'20260502','1920',NULL,260,0,'COMPLETED','N','20260400010','2026-05-17 17:30:54',NULL,NULL),('2026052500021','001','00001','20260400013',NULL,'TEST_OT_01','20260502','n1','20260502','1830','20260502','2130','20260502','1830',NULL,'20260502','2130',NULL,180,0,'COMPLETED','N','20260400010','2026-05-25 18:36:40',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_overtime_mgmt` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_service_credit`
--

LOCK TABLES `tb_user_service_credit` WRITE;
/*!40000 ALTER TABLE `tb_user_service_credit` DISABLE KEYS */;
INSERT INTO `tb_user_service_credit` VALUES ('SC2026052300001','001','20260400013',50,'OTHER','테스트','N','20260400010','2026-05-23 21:07:48','20260400010','2026-05-24 16:32:12');
/*!40000 ALTER TABLE `tb_user_service_credit` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_site_auth`
--

LOCK TABLES `tb_user_site_auth` WRITE;
/*!40000 ALTER TABLE `tb_user_site_auth` DISABLE KEYS */;
INSERT INTO `tb_user_site_auth` VALUES ('001','20260400001','00001','Y','20260400001','2026-04-12 21:46:46','20260400001','2026-04-13 22:33:11'),('001','20260400001','00002','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:16:55'),('001','20260400001','00003','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:21:33'),('001','20260400001','00004','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:21:33'),('001','20260400001','00005','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:21:33'),('001','20260400001','00006','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:21:33'),('001','20260400001','00007','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:21:33'),('001','20260400001','00008','Y','20260400001','2026-04-13 20:03:54','20260400001','2026-04-13 20:21:33'),('001','20260400002','00001','Y','20260400002','2026-04-14 20:42:47','20260400002','2026-04-14 20:42:47'),('001','20260400003','00001','Y','20260400003','2026-04-14 20:54:50','20260400003','2026-04-14 20:54:50'),('001','20260400004','00001','Y','20260400004','2026-04-14 21:07:17','20260400004','2026-04-14 21:07:17'),('001','20260400005','00001','Y','20260400005','2026-04-15 22:40:29','20260400005','2026-04-15 22:40:29'),('001','20260400006','00001','Y','20260400006','2026-04-15 22:42:36','20260400006','2026-04-15 22:42:36'),('001','20260400007','00001','Y','20260400007','2026-04-15 23:16:39','20260400007','2026-04-15 23:16:39'),('001','20260400008','00002','Y','20260400008','2026-04-18 20:21:14','20260400008','2026-04-18 20:21:14'),('001','20260400009','00001','Y','20260400009','2026-04-28 19:54:14','20260400009','2026-04-28 19:54:14'),('001','20260400010','00001','Y','20260400010','2026-04-28 20:10:41','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00002','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00003','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00004','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00005','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00006','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00007','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400010','00008','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400011','00001','Y','20260400011','2026-04-28 20:38:11','20260400011','2026-04-28 20:38:11'),('001','20260400012','00001','Y','20260400012','2026-04-28 20:38:46','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00002','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00003','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00004','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00005','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00006','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00007','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400012','00008','Y','SYSTEM','2026-06-01 22:22:14','SYSTEM','2026-06-01 22:22:21'),('001','20260400013','00001','Y','20260400013','2026-04-28 20:39:21','20260400013','2026-04-28 20:39:21'),('001','20260400014','00001','Y','20260400014','2026-04-30 22:05:45','20260400014','2026-04-30 22:05:45'),('001','20260600015','00001','Y','20260400010','2026-06-07 22:19:57','20260400010','2026-06-07 22:19:57'),('001','20260600016','00001','Y','20260400010','2026-06-07 22:19:57','20260400010','2026-06-07 22:19:57'),('001','20260600017','00001','Y','20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600018','00001','Y','20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600019','00001','Y','20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600020','00001','Y','20260400010','2026-06-07 22:19:58','20260400010','2026-06-07 22:19:58'),('001','20260600021','00001','Y','20260400010','2026-06-07 22:19:59','20260400010','2026-06-07 22:19:59'),('001','20260600022','00001','Y','20260400010','2026-06-07 22:19:59','20260400010','2026-06-07 22:19:59'),('001','ADMIN','00002','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14'),('001','ADMIN','00003','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14'),('001','ADMIN','00004','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14'),('001','ADMIN','00005','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14'),('001','ADMIN','00006','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14'),('001','ADMIN','00007','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14'),('001','ADMIN','00008','Y','20260400001','2026-04-13 19:52:44','20260400001','2026-04-13 19:54:14');
/*!40000 ALTER TABLE `tb_user_site_auth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_user_upload_job`
--

DROP TABLE IF EXISTS `tb_user_upload_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_upload_job` (
  `JOB_ID` varchar(25) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '잡 ID (PK, 회사별 채번: U + YYYYMMDD + SEQ)',
  `CMPNY_CD` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '회사 코드',
  `USER_CD` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '잡 생성한 사용자 (작업 조회 권한 검증용)',
  `FILE_NAME` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '원본 파일명 (감사용)',
  `FILE_SIZE` bigint DEFAULT NULL COMMENT '파일 바이트 크기',
  `TOTAL_ROWS` int NOT NULL DEFAULT '0' COMMENT '파싱된 데이터 행 수',
  `PROCESSED_ROWS` int NOT NULL DEFAULT '0' COMMENT '처리 완료 행 수 (성공+실패)',
  `SUCCESS_COUNT` int NOT NULL DEFAULT '0' COMMENT '성공 행 수',
  `FAIL_COUNT` int NOT NULL DEFAULT '0' COMMENT '실패 행 수',
  `FAILS_JSON` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'AES-GCM 암호화된 실패 항목 JSON 배열(v1.*). 복호화 시 [{index,errorItem,errorCode,message,sourceRow}]. 레거시 행은 평문 json 가능(읽기 측 prefix 분기)',
  `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '잡 상태[SYS061] PENDING:대기 RUNNING:진행 SUCCESS:성공 PARTIAL:일부실패 FAILED:실패',
  `ERROR_MSG` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '치명 예외 사유 (FAILED 상태일 때)',
  `INSERT_NO` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록자',
  `INSERT_DATE` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  `UPDATE_NO` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '수정자',
  `UPDATE_DATE` datetime DEFAULT NULL COMMENT '수정 일시',
  PRIMARY KEY (`JOB_ID`),
  KEY `IX_USER_UPLOAD_JOB_USER` (`CMPNY_CD`,`USER_CD`,`INSERT_DATE`),
  KEY `IX_USER_UPLOAD_JOB_STATUS` (`CMPNY_CD`,`STATUS`,`INSERT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 일괄 생성 잡 (PRAFTA-037-F6 비동기)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_user_upload_job`
--

LOCK TABLES `tb_user_upload_job` WRITE;
/*!40000 ALTER TABLE `tb_user_upload_job` DISABLE KEYS */;
INSERT INTO `tb_user_upload_job` VALUES ('U2026060700002','001','20260400010','사용자생성양식.xlsx',14427,9,9,0,9,'v1.AdqPNe0RwPRP952txG1MI-aYL9GTfs-mS6QvZNRr5LqHugA-0q1GC0N4Fgn-texeqWkZiTxktbAIv7FittxaAJVYBiVdnc6rE5QOZ40IRTQ3Ivw70oPdjagmS3mbctLocXJ6YMcQOQmJjvm2yPZJ8vs1ArZjalgLMzD7DM51gvthD3VaObaxq7P3Wp9mnCWyEmfuU4UvShyN5k2Gc2QPSA_eerOnRm_FzahW_GaKFM09HFkucfSUu-7WqL8IV3V6uZ096fidwruia__Ppq7Z4AvB0GcwTSg1d4ZSzjO-W3cglqaJnp4DpAFoRbVRD0MOIx3jS-RKBb3nOIfEqBbRgohM6kxnCg6GelsoxXoUVtetf0uk5Bplk9fbQDerlUn-0f29DWcmJlW1YOKofODiEen1xBVeJflNdyxodWn6DdZtlgxKYirMfwBpfX_DCL1booZBIqBrIZ6MX_UGpc1w2JEIrnFgyIFrBXLxqtU08YcXrG_uuoqbAQtK93EWXIhrDByyfxccai37Kunogml1VgDHiVqb6QAShD8WCD7zOHqZcc2d_FlRN5D4SElIpSfIy1F7ybdtxn-RlmTRSmGcV7PHnIS8fl4yksibYOoJR_x0T0QxVIW1xxPOJ9alAGlvl0EAtclEnHhhQ8y4QWXNoMXj-emhBvzxnOgnOmCn8HRpD2xGvkpDKQmfaQ8KG9-X4uYToSA_dIp33-YMa2jutCb6HSj-_jwHD3c-qhhqhF1-0ADes73dhKrP1d6e6MD4sAkl4SbusOkhJIg6TZjn_1hrx4tDlVXQRZOel9wm3mIeHT4phKwuIf2M1vxQjN8lcYApvtGJqhFOB-tL03wx6Z1QOc6LLZSaNE0ftyWtP2_YGEk4nzRmpr_GSjX_pp7hPjr16w9y5NqxFHsD9L5p8d5Fxmsa2O1Q34pbIk31KUovuHEi2BG584aKIzYrrmRvS9rfDLOtTEx7RS7Wy5Rb1DCCH9Lff2FADz40-Xzl041y1PURqH-eaLTpEXW6KefqtysDdEehTwAd9ziT9wVfD7ByTh-pgGOwo8dQaIzoYttB6rCGAJ7CP-A2U5PHFVij2h9yOBBBP5vZli__ijy8jq-YU099o_fSUdVVA-1bgaWwMgeMq3qHA3ziMoBpRfc-sr50Liki4sknYonDtqLt1ScE88UE9sCqEf0CUMq2aCcI4-9xfO4ak2-m1ucHNZjtN06QoA283-UvYZG00tPbTRzJiImZywfUVQz5sKj2j0bS1jAW4spyrPYPLWKQ0WCItMVZnIEfaX2Dd3LPEtqWeGOx0P6jw8JBQHWN6E2OjWuvPmrp6zLTRup7XNZq_1yeozNhiM0rzua8gO8ZjuJmrrcgZvgMqoUnqJB_6UHbsXUWqk7Rg53VTbOxBWdF77805R8Z1EF5H2DCXP_2YLX2zQktK0xvg9xfAq5-xTLmh8jaX7JA87VA7ZS1W2ZRd1AYroUjiCP4aHOQ_2Z6w1LjAqu5u_YOv54gEbWMhz_RitdVvPb26RH94i8XiwdefYPj6TSXKFJ6djKVRKvrupnzHXtbMoXnAVeAhGtF-v0UzUoiL1GloFrXdJgSCcJ25bFHEmJwJhGAqRZ4sLW8VIw-hYGQ6EdzpKR3GBHqNBKElIMQ_I7bhpZoRnsetSUjAdJKvSNj1bblC2PgQTp6x3pGwTpBSItKBW0HofIV_bCubiwLiGdgERzG3cVtLKAZx2dsS0YTik_-l_punIJ8z8z6lhMgY6kStSvaCyg8nW-3_EV6bqnziY8iM6EwF74lJy5WbPYGdS8wm6HN0pP0kKdl45QvauQx77davE7aV9ufoltirLDq4LXvd-NAoDOq7bIIsxVaHUnma30x9shDMSPBzHJOUEuF2S1rcgHCnB-jEcOOgG6g24GllpoA3nd40pEhn43Yuw31e2BCq2laidJk6g1JiblaPw8H-TMXNzOczqXmOzynxJrey9Dy5npUzvKvrAMk_lO_3bW2hkdYZOZGL2Q1utUcwRFFL6FhQEr6AlXyvka4Vo86Z51cPrplYqmD_V4FF5lBOpdKvLpPHD4TuDDqlKzgbOpqLm2qZvhC1MKqU5SnRHGDkZcuK9FiBzQR9rEATZaVbejg09hnf567FFBTkONshl2-5N1rFqoZVmDK1dPxn_gWWzfycxYwj1ahMpGJEpMlVpcEjdIsdTkWcWrhsg3FG7pOOOSCS-h3DOjhrGhCSPA9IlsROln6mFrZC6kWztSmGu_VKsuem_n89QPqqdqZ13I1xHRVyBd0ldXMNAZCbtNNYGuN9vMpCE8MGDtdDM3Ky00mIGIVE6XefYXSbiCUTo2VCAAeeDpG4BwfDRpqTi4C9wMyWsxbdOX4G7Ui0o-X4jGERUSQXhDecuyjlpdKt0G6DeyKE36BL2WMkJ8vTytr4bEGE2ChusbaLzixGFYMnkCdhyUdZzt57zhMK_y61XdOCUSjY8f3U3pJb1k_2KHe_ugjlZD-XPuu2pEgy6CoZc5V5UqS4wLKeHFqgmsZnPhU-9k','PARTIAL',NULL,'20260400010','2026-06-07 22:19:14','20260400010','2026-06-07 22:19:14'),('U2026060700003','001','20260400010','사용자생성양식.xlsx',14437,9,9,8,1,'v1.AQ1KzjRbM7MuegAKpd69-WW470BCppC-USnZ5qxKZsYuCM9gZ5l5eY9S9TCt79NM1fHtzphmAoR7gGKDeKXcqyUbhj21jUUv5ngHy1XEm49FaM6XxXIkAXGd3PqIOQd8qHcl_0HpZ439y-qT_vEHdFXM1IMpHz4HIoO1orJzAINN0GuiImeAb3DIKog7pbqyqFj1cxx7zbAi32V1Ft1yTZU44GkWSei4hUKRSspG7gKK4AOTdjkf0lljFrKVwlU1zLgIS1gOTRfkQgG8jcLwFWxawonbIpzzOdpmTnL7CRoKmfpAHMzp0vA','PARTIAL',NULL,'20260400010','2026-06-07 22:19:57','20260400010','2026-06-07 22:19:59');
/*!40000 ALTER TABLE `tb_user_upload_job` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Dumping data for table `tb_user_work_plan`
--

LOCK TABLES `tb_user_work_plan` WRITE;
/*!40000 ALTER TABLE `tb_user_work_plan` DISABLE KEYS */;
INSERT INTO `tb_user_work_plan` VALUES ('001','00001','20260400010','20260401','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260402','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260403','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260404',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260405','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260406','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260407','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260408',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260409','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260410','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260411','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260412',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260413','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260414','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260415','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260416',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260417','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260418','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260419','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260420',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260421','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260422','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260423','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260424',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260425','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260426','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260427','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260428',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400010','20260429','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260430','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260501','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260502','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260503','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260504',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260505','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260506','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260507','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260508',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260509','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260510','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260511','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260512',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260513','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260514','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260515','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260516',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260517','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260518','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260519','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260520',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260521','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260522','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260523','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260524',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260525','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260526','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260527','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260528',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260529','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260530','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260531','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260601',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260602','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260603','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260604','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260605',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260606','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260607','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260608','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260609',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260610','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260611','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260612','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260613',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260614','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260615','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260616','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260617',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260618','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260619','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260620','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260621',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260622','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260623','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260624','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260625',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260626','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260627','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260628','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260629',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260630','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260701','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260702','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260703',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260704','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260705','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260706','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260707',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260708','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260709','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260710','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260711',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260712','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260713','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260714','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260715',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260716','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260717','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260718','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260719',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260720','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260721','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260722','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260723',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260724','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260725','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260726','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260727',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260728','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260729','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260730','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260731',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260801','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260802','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260803','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260804',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260805','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260806','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260807','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260808',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260809','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260810','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260811','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260812',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260813','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260814','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260815','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260816',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260817','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260818','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260819','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260820',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260821','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260822','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260823','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260824',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260825','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260826','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260827','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260828',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260829','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260830','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260831','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260901',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260902','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260903','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260904','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260905',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260906','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260907','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260908','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260909',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260910','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260911','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260912','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260913',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260914','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260915','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260916','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260917',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260918','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260919','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260920','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260921',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260922','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260923','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260924','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260925',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260926','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260927','00003','20260400010','2026-04-28 22:34:55','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260928','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20260929',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20260930','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261001','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261002','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261003',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261004','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261005','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261006','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261007',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261008','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261009','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261010','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261011',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261012','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261013','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261014','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261015',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261016','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261017','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261018','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261019',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261020','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261021','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261022','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261023',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261024','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261025','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261026','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261027',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261028','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261029','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261030','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261031',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400010','20261101','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261102',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261103','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261104','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261105','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261106',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261107','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261108','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261109','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261110',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261111','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261112','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261113','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261114',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261115','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261116','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261117','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261118',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261119','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261120','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261121','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261122',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261123','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261124','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261125','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261126',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261127','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261128','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261129','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261130',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261201','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261202','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261203','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261204',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261205','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261206','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261207','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261208',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261209','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261210','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261211','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261212',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261213','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261214','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261215','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261216',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261217','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261218','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261219','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261220',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261221','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261222','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261223','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261224',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261225','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261226','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261227','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:49'),('001','00001','20260400010','20261228',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400010','20261229','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:50'),('001','00001','20260400010','20261230','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:50'),('001','00001','20260400010','20261231','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-23 23:01:50'),('001','00001','20260400011','20260401',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260402','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260403','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260404','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260405',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260406','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260407','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260408','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260409',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260410','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260411','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260412','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260413',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260414','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260415','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260416','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260417',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260418','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260419','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260420','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260421',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260422','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260423','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260424','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260425',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260426','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260427','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260428','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260429',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260430','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400011','20260501',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260502','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260503','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260504','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260505',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260506','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260507','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260508','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260509',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260510','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260511','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260512','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260513',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260514','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260515','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260516','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260517',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260518','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260519','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260520','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260521',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260522','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260523','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260524','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260525',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260526','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260527','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260528','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260529',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260530','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260531','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260601','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260602',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260603','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260604','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260605','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260606',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260607','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260608','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260609','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260610',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260611','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260612','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260613','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260614',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260615','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260616','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260617','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260618',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260619','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260620','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260621','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260622',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260623','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260624','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260625','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260626',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260627','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260628','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260629','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260630',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260701','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260702','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260703','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260704',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260705','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260706','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260707','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260708',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260709','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260710','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260711','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260712',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260713','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260714','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260715','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260716',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260717','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260718','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260719','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260720',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260721','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260722','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260723','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260724',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260725','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260726','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260727','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260728',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260729','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260730','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260731','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260801',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260802','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260803','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260804','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260805',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260806','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260807','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260808','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260809',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260810','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260811','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260812','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260813',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260814','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260815','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260816','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260817',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260818','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260819','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260820','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260821',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260822','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260823','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260824','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260825',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260826','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260827','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260828','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260829',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260830','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260831','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260901','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260902',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260903','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260904','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260905','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260906',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260907','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260908','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260909','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260910',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260911','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260912','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260913','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260914',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260915','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260916','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260917','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260918',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260919','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260920','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260921','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260922',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260923','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260924','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260925','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260926',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260927','00002','20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260928','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260929','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20260930',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261001','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261002','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261003','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261004',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261005','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261006','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261007','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261008',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261009','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261010','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261011','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261012',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261013','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261014','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261015','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261016',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261017','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261018','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261019','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261020',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261021','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261022','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261023','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261024',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261025','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261026','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261027','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261028',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261029','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261030','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261031','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400011','20261101','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261102','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261103',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261104','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261105','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261106','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261107',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261108','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261109','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261110','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261111',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261112','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261113','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261114','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261115',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261116','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261117','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261118','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261119',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261120','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261121','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261122','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261123',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261124','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261125','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261126','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261127',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261128','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261129','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261130','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261201',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261202','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261203','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261204','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261205',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261206','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261207','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261208','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261209',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261210','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261211','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261212','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261213',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261214','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261215','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261216','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261217',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261218','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261219','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261220','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261221',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261222','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261223','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261224','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261225',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261226','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261227','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261228','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261229',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261230','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400011','20261231','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20260401','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260402',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260403','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260404','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260405','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260406',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260407','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260408','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260409','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260410',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260411','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260412','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260413','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260414',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260415','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260416','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260417','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260418',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260419','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260420','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260421','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260422',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260423','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260424','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260425','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260426',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260427','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260428','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260429','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260430',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400012','20260501','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260502',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260503','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260504','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260505','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260506',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260507','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260508','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260509','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260510',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260511','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260512','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260513','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260514',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260515','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260516','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260517','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260518',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260519','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260520','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260521','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260522',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260523','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260524','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260525','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260526',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260527','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260528','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260529','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260530',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260531','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260601','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260602','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260603',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260604','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260605','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260606','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260607',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260608','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260609','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260610','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260611',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260612','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260613','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260614','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260615',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260616','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260617','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260618','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260619',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260620','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260621','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260622','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260623',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260624','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260625','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260626','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260627',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260628','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260629','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260630','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260701',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260702','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260703','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260704','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260705',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260706','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260707','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260708','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260709',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260710','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260711','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260712','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260713',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260714','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260715','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260716','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260717',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260718','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260719','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260720','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260721',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260722','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260723','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260724','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260725',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260726','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260727','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260728','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260729',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260730','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260731','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260801','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260802',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260803','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260804','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260805','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260806',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260807','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260808','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260809','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260810',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260811','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260812','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260813','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260814',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260815','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260816','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260817','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260818',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260819','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260820','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260821','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260822',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260823','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260824','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260825','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260826',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260827','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260828','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260829','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260830',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260831','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260901','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260902','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260903',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260904','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260905','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260906','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260907',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260908','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260909','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260910','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260911',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260912','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260913','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260914','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260915',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260916','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260917','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260918','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260919',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260920','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260921','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260922','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260923',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260924','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260925','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260926','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260927',NULL,'20260400010','2026-04-28 22:34:07','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260928','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260929','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20260930','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261001',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261002','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261003','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261004','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261005',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261006','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261007','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261008','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261009',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261010','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261011','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261012','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261013',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261014','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261015','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261016','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261017',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261018','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261019','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261020','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261021',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261022','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261023','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261024','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261025',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261026','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261027','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261028','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261029',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261030','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261031','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400012','20261101','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261102','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261103','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261104',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261105','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261106','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261107','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261108',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261109','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261110','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261111','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261112',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261113','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261114','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261115','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261116',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261117','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261118','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261119','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261120',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261121','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261122','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261123','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261124',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261125','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261126','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261127','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261128',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261129','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261130','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261201','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261202',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261203','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261204','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261205','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261206',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261207','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261208','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261209','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261210',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261211','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261212','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261213','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261214',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261215','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261216','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261217','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261218',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261219','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261220','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261221','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261222',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261223','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261224','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261225','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261226',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261227','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261228','00003','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261229','00005','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261230',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400012','20261231','00002','20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20260401','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260402','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260403',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260404','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260405','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260406','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260407',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260408','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260409','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260410','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260411',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260412','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260413','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260414','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260415',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260416','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260417','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260418','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260419',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260420','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260421','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260422','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260423',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260424','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260425','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260426','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260427',NULL,'20260400010','2026-04-28 22:44:03','20260400010','2026-04-29 20:22:21'),('001','00001','20260400013','20260428','00002','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260429','00003','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260430','00005','20260400010','2026-04-28 22:44:03','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260501','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260502','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260503',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260504','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260505','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260506','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260507',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260508','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260509','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260510','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260511',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260512','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260513','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260514','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260515',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260516','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260517','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260518','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260519',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260520','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260521','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260522','00016','20260400010','2026-04-28 22:34:07','20260400010','2026-05-24 21:58:34'),('001','00001','20260400013','20260523',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260524','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260525','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260526','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260527',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260528','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260529','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260530','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260531',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260601','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260602','00005','20260400010','2026-04-28 22:27:15','20260400010','2026-06-02 20:55:49'),('001','00001','20260400013','20260603','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260604','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-03 00:02:51'),('001','00001','20260400013','20260605','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260606','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260607','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260608',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260609','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260610','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260611','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260612',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260613','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260614','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260615','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260616',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260617','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260618','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260619','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260620',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260621','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260622','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260623','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260624',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260625','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260626','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260627','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260628',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260629','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260630','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260701','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260702',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260703','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260704','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260705','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260706',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260707','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260708','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260709','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260710',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260711','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260712','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260713','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260714',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260715','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260716','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260717','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260718',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260719','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260720','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260721','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260722',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260723','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260724','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260725','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260726',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260727','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260728','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260729','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260730',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260731','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260801','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260802','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260803',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260804','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260805','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260806','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260807',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260808','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260809','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260810','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260811',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260812','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260813','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260814','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260815',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260816','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260817','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260818','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260819',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260820','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260821','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260822','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260823',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260824','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260825','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260826','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260827',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260828','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260829','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260830','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260831',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260901','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260902','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260903','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260904',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260905','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260906','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260907','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260908',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260909','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260910','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260911','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260912',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260913','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260914','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260915','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260916',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260917','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260918','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260919','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260920',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260921','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260922','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260923','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260924',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260925','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260926','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260927','00005','20260400010','2026-04-28 22:34:07','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260928',NULL,'20260400010','2026-04-28 22:27:15','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20260929','00002','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20260930','00003','20260400010','2026-04-28 22:27:15','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261001','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261002',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261003','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261004','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261005','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261006',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261007','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261008','SYS_ANNUAL','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261009','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261010',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261011','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261012','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261013','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261014',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261015','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261016','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261017','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261018',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261019','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261020','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261021','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261022',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261023','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261024','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261025','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261026',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261027','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:47'),('001','00001','20260400013','20261028','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261029','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261030',NULL,'20260400010','2026-04-29 20:22:21','20260400010','2026-05-10 17:46:31'),('001','00001','20260400013','20261031','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261101',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261102','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261103','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261104','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261105',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261106','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261107','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261108','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261109',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261110','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261111','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261112','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261113',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261114','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261115','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261116','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261117',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261118','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261119','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261120','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261121',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261122','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261123','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261124','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261125',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261126','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261127','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261128','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261129',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261130','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261201','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261202','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261203',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261204','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261205','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261206','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261207',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261208','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261209','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261210','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261211',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261212','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261213','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261214','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261215',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261216','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261217','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261218','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261219',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261220','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261221','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261222','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261223',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261224','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261225','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261226','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261227',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL),('001','00001','20260400013','20261228','00002','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261229','00003','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261230','00005','20260400010','2026-04-29 20:22:21','20260400010','2026-06-01 20:19:48'),('001','00001','20260400013','20261231',NULL,'20260400010','2026-04-29 20:22:21',NULL,NULL);
/*!40000 ALTER TABLE `tb_user_work_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'prafta'
--

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
CREATE DEFINER=`root`@`localhost` FUNCTION `FNC_CMM_INFO_SRCH`(
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
CREATE DEFINER=`root`@`localhost` FUNCTION `FNC_CMM_SEQ_NEXTVAL`(
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
CREATE DEFINER=`root`@`localhost` FUNCTION `FNC_STD_TIME`(
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
CREATE DEFINER=`root`@`localhost` FUNCTION `FN_DECRYPT`(p_cipher TEXT) RETURNS varchar(255) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
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
CREATE DEFINER=`root`@`localhost` FUNCTION `FN_ENCRYPT`(p_text VARCHAR(255)) RETURNS text CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
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
CREATE DEFINER=`dev_prafta`@`%` PROCEDURE `proc_encrypt_user`(
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
CREATE DEFINER=`root`@`localhost` PROCEDURE `PR_CMM_SEQ_NEXTVAL`(
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
CREATE DEFINER=`root`@`localhost` PROCEDURE `SP_INSERT_SITE`(
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

-- Dump completed on 2026-06-08 20:44:02
