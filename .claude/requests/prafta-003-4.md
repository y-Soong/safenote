2026-05-14T22:07:37.709+09:00 DEBUG 2972 --- [nio-8080-exec-2] .ApiPrefixConfig$ApiPrefixHandlerMapping : Mapped to com.prafta.web.attd.attd07.controller.Attd07Controller#updateUserOvertimeRequests(UpdateUserOvertimeRequestRequest, String)
2026-05-14T22:07:37.713+09:00  INFO 2972 --- [nio-8080-exec-2] c.prafta.common.aop.log.LoggingAspect    : [Before] Method: Attd07Controller.updateUserOvertimeRequests(..)
2026-05-14T22:07:37.714+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.s.jdbc.support.JdbcTransactionManager  : Creating new transaction with name [com.prafta.web.attd.attd07.service.impl.Attd07ServiceImpl.updateUserOvertimeRequests]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.s.jdbc.support.JdbcTransactionManager  : Acquired Connection [HikariProxyConnection@1767303853 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2c45b096] for JDBC transaction
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.s.jdbc.support.JdbcTransactionManager  : Switching JDBC Connection [HikariProxyConnection@1767303853 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2c45b096] to manual commit
2026-05-14T22:07:37.715+09:00  WARN 2972 --- [nio-8080-exec-2] c.p.w.a.a.s.impl.Attd07ServiceImpl       : [OT-DIAG] enter cmpnyCd=001, siteCd=00001, userCd=20260400010, workYmd=20260501, attdId=2026051100030, reqId=null, gvUserCd=20260400010, gvAuthCd=master, otCount=1
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Creating a new SqlSession
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Registering transaction synchronization for SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c]
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.m.s.t.SpringManagedTransaction         : JDBC Connection [HikariProxyConnection@1767303853 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2c45b096] will be managed by Spring
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectUserExistInCmpnySite : ==>  Preparing: /* Attd07Mapper.selectUserExistInCmpnySite - SEC-017 scope check */ SELECT COUNT(*) FROM TB_USER U WHERE U.CMPNY_CD = ? AND U.SITE_CD = ? AND U.USER_CD = ? AND U.USE_YN = 'Y' AND U.WITHDRAWAL_DATE IS NULL
2026-05-14T22:07:37.715+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectUserExistInCmpnySite : ==> Parameters: 001(String), 00001(String), 20260400010(String)
2026-05-14T22:07:37.716+09:00  INFO 2972 --- [nio-8080-exec-2] p6spy                                    : #1778764057716 | took 0ms | statement | connection 30| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
/* Attd07Mapper.selectUserExistInCmpnySite - SEC-017 scope check */
    SELECT COUNT(*)
    FROM TB_USER U
    WHERE U.CMPNY_CD        = ?
      AND U.SITE_CD         = ?
      AND U.USER_CD         = ?
      AND U.USE_YN          = 'Y'
      AND U.WITHDRAWAL_DATE IS NULL
/* Attd07Mapper.selectUserExistInCmpnySite - SEC-017 scope check */
    SELECT COUNT(*)
    FROM TB_USER U
    WHERE U.CMPNY_CD        = '001'
      AND U.SITE_CD         = '00001'
      AND U.USER_CD         = '20260400010'
      AND U.USE_YN          = 'Y'
      AND U.WITHDRAWAL_DATE IS NULL;
2026-05-14T22:07:37.716+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectUserExistInCmpnySite : <==      Total: 1
2026-05-14T22:07:37.716+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c]
2026-05-14T22:07:37.716+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Fetched SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c] from current transaction
2026-05-14T22:07:37.717+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectAttdExistInScope     : ==>  Preparing: /* Attd07Mapper.selectAttdExistInScope - SEC-017 scope check */ SELECT COUNT(*) FROM TB_USER_ATTD_MGMT M WHERE M.CMPNY_CD = ? AND M.SITE_CD = ? AND M.USER_CD = ? AND M.ATTD_ID = ? AND M.DEL_YN = 'N'
2026-05-14T22:07:37.717+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectAttdExistInScope     : ==> Parameters: 001(String), 00001(String), 20260400010(String), 2026051100030(String)
2026-05-14T22:07:37.717+09:00  INFO 2972 --- [nio-8080-exec-2] p6spy                                    : #1778764057717 | took 0ms | statement | connection 30| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
/* Attd07Mapper.selectAttdExistInScope - SEC-017 scope check */
    SELECT COUNT(*)
    FROM TB_USER_ATTD_MGMT M
    WHERE M.CMPNY_CD = ?
      AND M.SITE_CD  = ?
      AND M.USER_CD  = ?
      AND M.ATTD_ID  = ?
      AND M.DEL_YN   = 'N'
/* Attd07Mapper.selectAttdExistInScope - SEC-017 scope check */
    SELECT COUNT(*)
    FROM TB_USER_ATTD_MGMT M
    WHERE M.CMPNY_CD = '001'
      AND M.SITE_CD  = '00001'
      AND M.USER_CD  = '20260400010'
      AND M.ATTD_ID  = '2026051100030'
      AND M.DEL_YN   = 'N';
2026-05-14T22:07:37.717+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectAttdExistInScope     : <==      Total: 1
2026-05-14T22:07:37.718+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c]
2026-05-14T22:07:37.718+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Fetched SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c] from current transaction
2026-05-14T22:07:37.718+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectAllowedWindow        : ==>  Preparing: /* Attd07Mapper.selectAllowedWindow */ SELECT SCH.FST_SCH_STR_TIME AS plan1Start , SCH.FST_SCH_END_TIME AS plan1End , SCH.SEC_SCH_STR_TIME AS plan2Start , SCH.SEC_SCH_END_TIME AS plan2End /* 1st attd row - standardized check-in / check-out */ , A1.CHECK_IN_DATE AS act1InDate , FNC_STD_TIME( A1.CHECK_IN_TIME , IFNULL( CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A1.CHECK_IN_DATE, NULL)) = 0 THEN NULL ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A1.CHECK_IN_DATE, NULL) END , 0) , 'IN' ) AS act1InStdTime , A1.CHECK_OUT_DATE AS act1OutDate , FNC_STD_TIME( A1.CHECK_OUT_TIME , IFNULL( CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A1.CHECK_OUT_DATE, NULL)) = 0 THEN NULL ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A1.CHECK_OUT_DATE, NULL) END , 0) , 'OUT' ) AS act1OutStdTime /* 2nd attd row - standardized check-in / check-out */ , A2.CHECK_IN_DATE AS act2InDate , FNC_STD_TIME( A2.CHECK_IN_TIME , IFNULL( CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A2.CHECK_IN_DATE, NULL)) = 0 THEN NULL ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A2.CHECK_IN_DATE, NULL) END , 0) , 'IN' ) AS act2InStdTime , A2.CHECK_OUT_DATE AS act2OutDate , FNC_STD_TIME( A2.CHECK_OUT_TIME , IFNULL( CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A2.CHECK_OUT_DATE, NULL)) = 0 THEN NULL ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A2.CHECK_OUT_DATE, NULL) END , 0) , 'OUT' ) AS act2OutStdTime FROM TB_USER_WORK_PLAN WP LEFT JOIN TB_SCH_MGMT SCH ON SCH.CMPNY_CD = WP.CMPNY_CD AND SCH.SITE_CD = WP.SITE_CD AND SCH.SCH_CD = WP.WORK_PLAN_CD AND SCH.USE_YN = 'Y' AND SCH.APPLY_DATE <= WP.WORK_YMD LEFT JOIN TB_USER_ATTD_MGMT A1 ON A1.CMPNY_CD = WP.CMPNY_CD AND A1.SITE_CD = WP.SITE_CD AND A1.USER_CD = WP.USER_CD AND A1.WORK_YMD = WP.WORK_YMD AND A1.WORK_SEQ = 1 AND A1.DEL_YN = 'N' LEFT JOIN TB_USER_ATTD_MGMT A2 ON A2.CMPNY_CD = WP.CMPNY_CD AND A2.SITE_CD = WP.SITE_CD AND A2.USER_CD = WP.USER_CD AND A2.WORK_YMD = WP.WORK_YMD AND A2.WORK_SEQ = 2 AND A2.DEL_YN = 'N' WHERE WP.CMPNY_CD = ? AND WP.SITE_CD = ? AND WP.USER_CD = ? AND WP.WORK_YMD = ? LIMIT 1
2026-05-14T22:07:37.718+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectAllowedWindow        : ==> Parameters: 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 00001(String), 20260400010(String), 20260501(String)
2026-05-14T22:07:37.720+09:00  INFO 2972 --- [nio-8080-exec-2] p6spy                                    : #1778764057720 | took 1ms | statement | connection 30| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
/* Attd07Mapper.selectAllowedWindow */
    SELECT
          SCH.FST_SCH_STR_TIME                                             AS plan1Start
        , SCH.FST_SCH_END_TIME                                             AS plan1End
        , SCH.SEC_SCH_STR_TIME                                             AS plan2Start
        , SCH.SEC_SCH_END_TIME                                             AS plan2End

        /* 1st attd row - standardized check-in / check-out */
        , A1.CHECK_IN_DATE                                                 AS act1InDate
        , FNC_STD_TIME(
              A1.CHECK_IN_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A1.CHECK_IN_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A1.CHECK_IN_DATE, NULL)
                  END
              , 0)
            , 'IN'
          )                                                                AS act1InStdTime
        , A1.CHECK_OUT_DATE                                                AS act1OutDate
        , FNC_STD_TIME(
              A1.CHECK_OUT_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A1.CHECK_OUT_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A1.CHECK_OUT_DATE, NULL)
                  END
              , 0)
            , 'OUT'
          )                                                                AS act1OutStdTime

        /* 2nd attd row - standardized check-in / check-out */
        , A2.CHECK_IN_DATE                                                 AS act2InDate
        , FNC_STD_TIME(
              A2.CHECK_IN_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A2.CHECK_IN_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKIN', A2.CHECK_IN_DATE, NULL)
                  END
              , 0)
            , 'IN'
          )                                                                AS act2InStdTime
        , A2.CHECK_OUT_DATE                                                AS act2OutDate
        , FNC_STD_TIME(
              A2.CHECK_OUT_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A2.CHECK_OUT_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH(?, 'ATTD_STD_CHKOUT', A2.CHECK_OUT_DATE, NULL)
                  END
              , 0)
            , 'OUT'
          )                                                                AS act2OutStdTime

    FROM TB_USER_WORK_PLAN WP

    LEFT JOIN TB_SCH_MGMT SCH
        ON  SCH.CMPNY_CD   = WP.CMPNY_CD
        AND SCH.SITE_CD    = WP.SITE_CD
        AND SCH.SCH_CD     = WP.WORK_PLAN_CD
        AND SCH.USE_YN     = 'Y'
        AND SCH.APPLY_DATE   <=   WP.WORK_YMD

    LEFT JOIN TB_USER_ATTD_MGMT A1
        ON  A1.CMPNY_CD = WP.CMPNY_CD
        AND A1.SITE_CD  = WP.SITE_CD
        AND A1.USER_CD  = WP.USER_CD
        AND A1.WORK_YMD = WP.WORK_YMD
        AND A1.WORK_SEQ = 1
        AND A1.DEL_YN   = 'N'

    LEFT JOIN TB_USER_ATTD_MGMT A2
        ON  A2.CMPNY_CD = WP.CMPNY_CD
        AND A2.SITE_CD  = WP.SITE_CD
        AND A2.USER_CD  = WP.USER_CD
        AND A2.WORK_YMD = WP.WORK_YMD
        AND A2.WORK_SEQ = 2
        AND A2.DEL_YN   = 'N'

    WHERE WP.CMPNY_CD = ?
      AND WP.SITE_CD  = ?
      AND WP.USER_CD  = ?
      AND WP.WORK_YMD = ?
    LIMIT 1
/* Attd07Mapper.selectAllowedWindow */
    SELECT
          SCH.FST_SCH_STR_TIME                                             AS plan1Start
        , SCH.FST_SCH_END_TIME                                             AS plan1End
        , SCH.SEC_SCH_STR_TIME                                             AS plan2Start
        , SCH.SEC_SCH_END_TIME                                             AS plan2End

        /* 1st attd row - standardized check-in / check-out */
        , A1.CHECK_IN_DATE                                                 AS act1InDate
        , FNC_STD_TIME(
              A1.CHECK_IN_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKIN', A1.CHECK_IN_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKIN', A1.CHECK_IN_DATE, NULL)
                  END
              , 0)
            , 'IN'
          )                                                                AS act1InStdTime
        , A1.CHECK_OUT_DATE                                                AS act1OutDate
        , FNC_STD_TIME(
              A1.CHECK_OUT_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKOUT', A1.CHECK_OUT_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKOUT', A1.CHECK_OUT_DATE, NULL)
                  END
              , 0)
            , 'OUT'
          )                                                                AS act1OutStdTime

        /* 2nd attd row - standardized check-in / check-out */
        , A2.CHECK_IN_DATE                                                 AS act2InDate
        , FNC_STD_TIME(
              A2.CHECK_IN_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKIN', A2.CHECK_IN_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKIN', A2.CHECK_IN_DATE, NULL)
                  END
              , 0)
            , 'IN'
          )                                                                AS act2InStdTime
        , A2.CHECK_OUT_DATE                                                AS act2OutDate
        , FNC_STD_TIME(
              A2.CHECK_OUT_TIME
            , IFNULL(
                  CASE WHEN CHAR_LENGTH(FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKOUT', A2.CHECK_OUT_DATE, NULL)) = 0
                       THEN NULL
                       ELSE FNC_CMM_INFO_SRCH('001', 'ATTD_STD_CHKOUT', A2.CHECK_OUT_DATE, NULL)
                  END
              , 0)
            , 'OUT'
          )                                                                AS act2OutStdTime

    FROM TB_USER_WORK_PLAN WP

    LEFT JOIN TB_SCH_MGMT SCH
        ON  SCH.CMPNY_CD   = WP.CMPNY_CD
        AND SCH.SITE_CD    = WP.SITE_CD
        AND SCH.SCH_CD     = WP.WORK_PLAN_CD
        AND SCH.USE_YN     = 'Y'
        AND SCH.APPLY_DATE   <=   WP.WORK_YMD

    LEFT JOIN TB_USER_ATTD_MGMT A1
        ON  A1.CMPNY_CD = WP.CMPNY_CD
        AND A1.SITE_CD  = WP.SITE_CD
        AND A1.USER_CD  = WP.USER_CD
        AND A1.WORK_YMD = WP.WORK_YMD
        AND A1.WORK_SEQ = 1
        AND A1.DEL_YN   = 'N'

    LEFT JOIN TB_USER_ATTD_MGMT A2
        ON  A2.CMPNY_CD = WP.CMPNY_CD
        AND A2.SITE_CD  = WP.SITE_CD
        AND A2.USER_CD  = WP.USER_CD
        AND A2.WORK_YMD = WP.WORK_YMD
        AND A2.WORK_SEQ = 2
        AND A2.DEL_YN   = 'N'

    WHERE WP.CMPNY_CD = '001'
      AND WP.SITE_CD  = '00001'
      AND WP.USER_CD  = '20260400010'
      AND WP.WORK_YMD = '20260501'
    LIMIT 1;
2026-05-14T22:07:37.720+09:00 DEBUG 2972 --- [nio-8080-exec-2] c.p.w.a.a.m.A.selectAllowedWindow        : <==      Total: 1
2026-05-14T22:07:37.720+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Releasing transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c]
2026-05-14T22:07:37.720+09:00  WARN 2972 --- [nio-8080-exec-2] c.p.w.a.a.s.impl.Attd07ServiceImpl       : [OT-DIAG] windows.plan1=(0930->1800), plan2=(->), act1=(null 20260501 ~ null 0900), act2=(null 20260501 ~ null 2100)
2026-05-14T22:07:37.720+09:00  WARN 2972 --- [nio-8080-exec-2] c.p.w.a.a.s.impl.Attd07ServiceImpl       : [OT-DIAG] schSegs=[[570,1080]], stdSegs=[]
2026-05-14T22:07:37.720+09:00  WARN 2972 --- [nio-8080-exec-2] c.p.w.a.a.s.impl.Attd07ServiceImpl       : OT register rejected - no standardized actual work segments. userCd=20260400010, workYmd=20260501
2026-05-14T22:07:37.721+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Transaction synchronization deregistering SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c]
2026-05-14T22:07:37.721+09:00 DEBUG 2972 --- [nio-8080-exec-2] org.mybatis.spring.SqlSessionUtils       : Transaction synchronization closing SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7f405b2c]
2026-05-14T22:07:37.721+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.s.jdbc.support.JdbcTransactionManager  : Initiating transaction rollback
2026-05-14T22:07:37.721+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.s.jdbc.support.JdbcTransactionManager  : Rolling back JDBC transaction on Connection [HikariProxyConnection@1767303853 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2c45b096]
2026-05-14T22:07:37.721+09:00  INFO 2972 --- [nio-8080-exec-2] p6spy                                    : #1778764057721 | took 0ms | rollback | connection 30| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci

;
2026-05-14T22:07:37.721+09:00 DEBUG 2972 --- [nio-8080-exec-2] o.s.jdbc.support.JdbcTransactionManager  : Releasing JDBC Connection [HikariProxyConnection@1767303853 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2c45b096] after transaction
2026-05-14T22:07:37.721+09:00  WARN 2972 --- [nio-8080-exec-2] c.p.c.exception.GlobalExceptionHandler   : [ApiException] code=ATTD_400_014 status=400 BAD_REQUEST resolvedMessage=출퇴근 기록이 완료된 후에 초과근무를 등록할 수 있습니다.
2026-05-14T22:07:37.722+09:00  WARN 2972 --- [nio-8080-exec-2] .m.m.a.ExceptionHandlerExceptionResolver : Resolved [com.prafta.common.exception.ApiException: 출퇴근 기록이 완료된 후에 초과근무를 등록할 수 있습니다.]
