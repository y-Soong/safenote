2026-05-14T21:23:51.513+09:00 DEBUG 2972 --- \[nio-8080-exec-4] .ApiPrefixConfig$ApiPrefixHandlerMapping : Mapped to com.prafta.web.attd.attd07.controller.Attd07Controller#updateUserOvertimeRequests(UpdateUserOvertimeRequestRequest, String)

2026-05-14T21:23:51.517+09:00  INFO 2972 --- \[nio-8080-exec-4] c.prafta.common.aop.log.LoggingAspect    : \[Before] Method: Attd07Controller.updateUserOvertimeRequests(..)

2026-05-14T21:23:51.520+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.s.jdbc.support.JdbcTransactionManager  : Creating new transaction with name \[com.prafta.web.attd.attd07.service.impl.Attd07ServiceImpl.updateUserOvertimeRequests]: PROPAGATION\_REQUIRED,ISOLATION\_DEFAULT

2026-05-14T21:23:51.520+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.s.jdbc.support.JdbcTransactionManager  : Acquired Connection \[HikariProxyConnection@1322956752 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@ee8df] for JDBC transaction

2026-05-14T21:23:51.520+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.s.jdbc.support.JdbcTransactionManager  : Switching JDBC Connection \[HikariProxyConnection@1322956752 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@ee8df] to manual commit

2026-05-14T21:23:51.521+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Creating a new SqlSession

2026-05-14T21:23:51.521+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Registering transaction synchronization for SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266]

2026-05-14T21:23:51.521+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.m.s.t.SpringManagedTransaction         : JDBC Connection \[HikariProxyConnection@1322956752 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@ee8df] will be managed by Spring

2026-05-14T21:23:51.521+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectUserExistInCmpnySite : ==>  Preparing: /\* Attd07Mapper.selectUserExistInCmpnySite - SEC-017 scope check \*/ SELECT COUNT(\*) FROM TB\_USER U WHERE U.CMPNY\_CD = ? AND U.SITE\_CD = ? AND U.USER\_CD = ? AND U.USE\_YN = 'Y' AND U.WITHDRAWAL\_DATE IS NULL

2026-05-14T21:23:51.522+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectUserExistInCmpnySite : ==> Parameters: 001(String), 00001(String), 20260400010(String)

2026-05-14T21:23:51.522+09:00  INFO 2972 --- \[nio-8080-exec-4] p6spy                                    : #1778761431522 | took 0ms | statement | connection 10| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul\&useUnicode=true\&characterEncoding=utf8\&connectionCollation=utf8mb4\_unicode\_ci

/\* Attd07Mapper.selectUserExistInCmpnySite - SEC-017 scope check \*/

&#x20;   SELECT COUNT(\*)

&#x20;   FROM TB\_USER U

&#x20;   WHERE U.CMPNY\_CD        = ?

&#x20;     AND U.SITE\_CD         = ?

&#x20;     AND U.USER\_CD         = ?

&#x20;     AND U.USE\_YN          = 'Y'

&#x20;     AND U.WITHDRAWAL\_DATE IS NULL

/\* Attd07Mapper.selectUserExistInCmpnySite - SEC-017 scope check \*/

&#x20;   SELECT COUNT(\*)

&#x20;   FROM TB\_USER U

&#x20;   WHERE U.CMPNY\_CD        = '001'

&#x20;     AND U.SITE\_CD         = '00001'

&#x20;     AND U.USER\_CD         = '20260400010'

&#x20;     AND U.USE\_YN          = 'Y'

&#x20;     AND U.WITHDRAWAL\_DATE IS NULL;

2026-05-14T21:23:51.523+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectUserExistInCmpnySite : <==      Total: 1

2026-05-14T21:23:51.523+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Releasing transactional SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266]

2026-05-14T21:23:51.523+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Fetched SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266] from current transaction

2026-05-14T21:23:51.523+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectAttdExistInScope     : ==>  Preparing: /\* Attd07Mapper.selectAttdExistInScope - SEC-017 scope check \*/ SELECT COUNT(\*) FROM TB\_USER\_ATTD\_MGMT M WHERE M.CMPNY\_CD = ? AND M.SITE\_CD = ? AND M.USER\_CD = ? AND M.ATTD\_ID = ? AND M.DEL\_YN = 'N'

2026-05-14T21:23:51.523+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectAttdExistInScope     : ==> Parameters: 001(String), 00001(String), 20260400010(String), 2026051100030(String)

2026-05-14T21:23:51.524+09:00  INFO 2972 --- \[nio-8080-exec-4] p6spy                                    : #1778761431524 | took 0ms | statement | connection 10| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul\&useUnicode=true\&characterEncoding=utf8\&connectionCollation=utf8mb4\_unicode\_ci

/\* Attd07Mapper.selectAttdExistInScope - SEC-017 scope check \*/

&#x20;   SELECT COUNT(\*)

&#x20;   FROM TB\_USER\_ATTD\_MGMT M

&#x20;   WHERE M.CMPNY\_CD = ?

&#x20;     AND M.SITE\_CD  = ?

&#x20;     AND M.USER\_CD  = ?

&#x20;     AND M.ATTD\_ID  = ?

&#x20;     AND M.DEL\_YN   = 'N'

/\* Attd07Mapper.selectAttdExistInScope - SEC-017 scope check \*/

&#x20;   SELECT COUNT(\*)

&#x20;   FROM TB\_USER\_ATTD\_MGMT M

&#x20;   WHERE M.CMPNY\_CD = '001'

&#x20;     AND M.SITE\_CD  = '00001'

&#x20;     AND M.USER\_CD  = '20260400010'

&#x20;     AND M.ATTD\_ID  = '2026051100030'

&#x20;     AND M.DEL\_YN   = 'N';

2026-05-14T21:23:51.524+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectAttdExistInScope     : <==      Total: 1

2026-05-14T21:23:51.524+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Releasing transactional SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266]

2026-05-14T21:23:51.527+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Fetched SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266] from current transaction

2026-05-14T21:23:51.527+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectAllowedWindow        : ==>  Preparing: /\* Attd07Mapper.selectAllowedWindow \*/ SELECT SCH.FST\_SCH\_STR\_TIME AS plan1Start , SCH.FST\_SCH\_END\_TIME AS plan1End , SCH.SEC\_SCH\_STR\_TIME AS plan2Start , SCH.SEC\_SCH\_END\_TIME AS plan2End /\* 1st attd row - standardized check-in / check-out \*/ , A1.CHECK\_IN\_DATE AS act1InDate , FNC\_STD\_TIME( A1.CHECK\_IN\_TIME , IFNULL( CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A1.CHECK\_IN\_DATE, NULL)) = 0 THEN NULL ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A1.CHECK\_IN\_DATE, NULL) END , 0) , 'IN' ) AS act1InStdTime , A1.CHECK\_OUT\_DATE AS act1OutDate , FNC\_STD\_TIME( A1.CHECK\_OUT\_TIME , IFNULL( CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A1.CHECK\_OUT\_DATE, NULL)) = 0 THEN NULL ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A1.CHECK\_OUT\_DATE, NULL) END , 0) , 'OUT' ) AS act1OutStdTime /\* 2nd attd row - standardized check-in / check-out \*/ , A2.CHECK\_IN\_DATE AS act2InDate , FNC\_STD\_TIME( A2.CHECK\_IN\_TIME , IFNULL( CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A2.CHECK\_IN\_DATE, NULL)) = 0 THEN NULL ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A2.CHECK\_IN\_DATE, NULL) END , 0) , 'IN' ) AS act2InStdTime , A2.CHECK\_OUT\_DATE AS act2OutDate , FNC\_STD\_TIME( A2.CHECK\_OUT\_TIME , IFNULL( CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A2.CHECK\_OUT\_DATE, NULL)) = 0 THEN NULL ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A2.CHECK\_OUT\_DATE, NULL) END , 0) , 'OUT' ) AS act2OutStdTime FROM TB\_USER\_WORK\_PLAN WP LEFT JOIN TB\_SCH\_MGMT SCH ON SCH.CMPNY\_CD = WP.CMPNY\_CD AND SCH.SITE\_CD = WP.SITE\_CD AND SCH.SCH\_CD = WP.WORK\_PLAN\_CD AND SCH.USE\_YN = 'Y' AND SCH.APPLY\_DATE <= WP.WORK\_YMD LEFT JOIN TB\_USER\_ATTD\_MGMT A1 ON A1.CMPNY\_CD = WP.CMPNY\_CD AND A1.SITE\_CD = WP.SITE\_CD AND A1.USER\_CD = WP.USER\_CD AND A1.WORK\_YMD = WP.WORK\_YMD AND A1.WORK\_SEQ = 1 AND A1.DEL\_YN = 'N' LEFT JOIN TB\_USER\_ATTD\_MGMT A2 ON A2.CMPNY\_CD = WP.CMPNY\_CD AND A2.SITE\_CD = WP.SITE\_CD AND A2.USER\_CD = WP.USER\_CD AND A2.WORK\_YMD = WP.WORK\_YMD AND A2.WORK\_SEQ = 2 AND A2.DEL\_YN = 'N' WHERE WP.CMPNY\_CD = ? AND WP.SITE\_CD = ? AND WP.USER\_CD = ? AND WP.WORK\_YMD = ? LIMIT 1

2026-05-14T21:23:51.528+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectAllowedWindow        : ==> Parameters: 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 001(String), 00001(String), 20260400010(String), 20260501(String)

2026-05-14T21:23:51.530+09:00  INFO 2972 --- \[nio-8080-exec-4] p6spy                                    : #1778761431530 | took 1ms | statement | connection 10| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul\&useUnicode=true\&characterEncoding=utf8\&connectionCollation=utf8mb4\_unicode\_ci

/\* Attd07Mapper.selectAllowedWindow \*/

&#x20;   SELECT

&#x20;         SCH.FST\_SCH\_STR\_TIME                                             AS plan1Start

&#x20;       , SCH.FST\_SCH\_END\_TIME                                             AS plan1End

&#x20;       , SCH.SEC\_SCH\_STR\_TIME                                             AS plan2Start

&#x20;       , SCH.SEC\_SCH\_END\_TIME                                             AS plan2End



&#x20;       /\* 1st attd row - standardized check-in / check-out \*/

&#x20;       , A1.CHECK\_IN\_DATE                                                 AS act1InDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A1.CHECK\_IN\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A1.CHECK\_IN\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A1.CHECK\_IN\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'IN'

&#x20;         )                                                                AS act1InStdTime

&#x20;       , A1.CHECK\_OUT\_DATE                                                AS act1OutDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A1.CHECK\_OUT\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A1.CHECK\_OUT\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A1.CHECK\_OUT\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'OUT'

&#x20;         )                                                                AS act1OutStdTime



&#x20;       /\* 2nd attd row - standardized check-in / check-out \*/

&#x20;       , A2.CHECK\_IN\_DATE                                                 AS act2InDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A2.CHECK\_IN\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A2.CHECK\_IN\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKIN', A2.CHECK\_IN\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'IN'

&#x20;         )                                                                AS act2InStdTime

&#x20;       , A2.CHECK\_OUT\_DATE                                                AS act2OutDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A2.CHECK\_OUT\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A2.CHECK\_OUT\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH(?, 'ATTD\_STD\_CHKOUT', A2.CHECK\_OUT\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'OUT'

&#x20;         )                                                                AS act2OutStdTime



&#x20;   FROM TB\_USER\_WORK\_PLAN WP



&#x20;   LEFT JOIN TB\_SCH\_MGMT SCH

&#x20;       ON  SCH.CMPNY\_CD   = WP.CMPNY\_CD

&#x20;       AND SCH.SITE\_CD    = WP.SITE\_CD

&#x20;       AND SCH.SCH\_CD     = WP.WORK\_PLAN\_CD

&#x20;       AND SCH.USE\_YN     = 'Y'

&#x20;       AND SCH.APPLY\_DATE   <=   WP.WORK\_YMD



&#x20;   LEFT JOIN TB\_USER\_ATTD\_MGMT A1

&#x20;       ON  A1.CMPNY\_CD = WP.CMPNY\_CD

&#x20;       AND A1.SITE\_CD  = WP.SITE\_CD

&#x20;       AND A1.USER\_CD  = WP.USER\_CD

&#x20;       AND A1.WORK\_YMD = WP.WORK\_YMD

&#x20;       AND A1.WORK\_SEQ = 1

&#x20;       AND A1.DEL\_YN   = 'N'



&#x20;   LEFT JOIN TB\_USER\_ATTD\_MGMT A2

&#x20;       ON  A2.CMPNY\_CD = WP.CMPNY\_CD

&#x20;       AND A2.SITE\_CD  = WP.SITE\_CD

&#x20;       AND A2.USER\_CD  = WP.USER\_CD

&#x20;       AND A2.WORK\_YMD = WP.WORK\_YMD

&#x20;       AND A2.WORK\_SEQ = 2

&#x20;       AND A2.DEL\_YN   = 'N'



&#x20;   WHERE WP.CMPNY\_CD = ?

&#x20;     AND WP.SITE\_CD  = ?

&#x20;     AND WP.USER\_CD  = ?

&#x20;     AND WP.WORK\_YMD = ?

&#x20;   LIMIT 1

/\* Attd07Mapper.selectAllowedWindow \*/

&#x20;   SELECT

&#x20;         SCH.FST\_SCH\_STR\_TIME                                             AS plan1Start

&#x20;       , SCH.FST\_SCH\_END\_TIME                                             AS plan1End

&#x20;       , SCH.SEC\_SCH\_STR\_TIME                                             AS plan2Start

&#x20;       , SCH.SEC\_SCH\_END\_TIME                                             AS plan2End



&#x20;       /\* 1st attd row - standardized check-in / check-out \*/

&#x20;       , A1.CHECK\_IN\_DATE                                                 AS act1InDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A1.CHECK\_IN\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKIN', A1.CHECK\_IN\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKIN', A1.CHECK\_IN\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'IN'

&#x20;         )                                                                AS act1InStdTime

&#x20;       , A1.CHECK\_OUT\_DATE                                                AS act1OutDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A1.CHECK\_OUT\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKOUT', A1.CHECK\_OUT\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKOUT', A1.CHECK\_OUT\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'OUT'

&#x20;         )                                                                AS act1OutStdTime



&#x20;       /\* 2nd attd row - standardized check-in / check-out \*/

&#x20;       , A2.CHECK\_IN\_DATE                                                 AS act2InDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A2.CHECK\_IN\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKIN', A2.CHECK\_IN\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKIN', A2.CHECK\_IN\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'IN'

&#x20;         )                                                                AS act2InStdTime

&#x20;       , A2.CHECK\_OUT\_DATE                                                AS act2OutDate

&#x20;       , FNC\_STD\_TIME(

&#x20;             A2.CHECK\_OUT\_TIME

&#x20;           , IFNULL(

&#x20;                 CASE WHEN CHAR\_LENGTH(FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKOUT', A2.CHECK\_OUT\_DATE, NULL)) = 0

&#x20;                      THEN NULL

&#x20;                      ELSE FNC\_CMM\_INFO\_SRCH('001', 'ATTD\_STD\_CHKOUT', A2.CHECK\_OUT\_DATE, NULL)

&#x20;                 END

&#x20;             , 0)

&#x20;           , 'OUT'

&#x20;         )                                                                AS act2OutStdTime



&#x20;   FROM TB\_USER\_WORK\_PLAN WP



&#x20;   LEFT JOIN TB\_SCH\_MGMT SCH

&#x20;       ON  SCH.CMPNY\_CD   = WP.CMPNY\_CD

&#x20;       AND SCH.SITE\_CD    = WP.SITE\_CD

&#x20;       AND SCH.SCH\_CD     = WP.WORK\_PLAN\_CD

&#x20;       AND SCH.USE\_YN     = 'Y'

&#x20;       AND SCH.APPLY\_DATE   <=   WP.WORK\_YMD



&#x20;   LEFT JOIN TB\_USER\_ATTD\_MGMT A1

&#x20;       ON  A1.CMPNY\_CD = WP.CMPNY\_CD

&#x20;       AND A1.SITE\_CD  = WP.SITE\_CD

&#x20;       AND A1.USER\_CD  = WP.USER\_CD

&#x20;       AND A1.WORK\_YMD = WP.WORK\_YMD

&#x20;       AND A1.WORK\_SEQ = 1

&#x20;       AND A1.DEL\_YN   = 'N'



&#x20;   LEFT JOIN TB\_USER\_ATTD\_MGMT A2

&#x20;       ON  A2.CMPNY\_CD = WP.CMPNY\_CD

&#x20;       AND A2.SITE\_CD  = WP.SITE\_CD

&#x20;       AND A2.USER\_CD  = WP.USER\_CD

&#x20;       AND A2.WORK\_YMD = WP.WORK\_YMD

&#x20;       AND A2.WORK\_SEQ = 2

&#x20;       AND A2.DEL\_YN   = 'N'



&#x20;   WHERE WP.CMPNY\_CD = '001'

&#x20;     AND WP.SITE\_CD  = '00001'

&#x20;     AND WP.USER\_CD  = '20260400010'

&#x20;     AND WP.WORK\_YMD = '20260501'

&#x20;   LIMIT 1;

2026-05-14T21:23:51.530+09:00 DEBUG 2972 --- \[nio-8080-exec-4] c.p.w.a.a.m.A.selectAllowedWindow        : <==      Total: 1

2026-05-14T21:23:51.530+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Releasing transactional SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266]

2026-05-14T21:23:51.531+09:00  WARN 2972 --- \[nio-8080-exec-4] c.p.w.a.a.s.impl.Attd07ServiceImpl       : OT register rejected - no standardized actual work segments. userCd=20260400010, workYmd=20260501

2026-05-14T21:23:51.532+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Transaction synchronization deregistering SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266]

2026-05-14T21:23:51.532+09:00 DEBUG 2972 --- \[nio-8080-exec-4] org.mybatis.spring.SqlSessionUtils       : Transaction synchronization closing SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@2ebc7266]

2026-05-14T21:23:51.532+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.s.jdbc.support.JdbcTransactionManager  : Initiating transaction rollback

2026-05-14T21:23:51.532+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.s.jdbc.support.JdbcTransactionManager  : Rolling back JDBC transaction on Connection \[HikariProxyConnection@1322956752 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@ee8df]

2026-05-14T21:23:51.533+09:00  INFO 2972 --- \[nio-8080-exec-4] p6spy                                    : #1778761431533 | took 0ms | rollback | connection 10| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul\&useUnicode=true\&characterEncoding=utf8\&connectionCollation=utf8mb4\_unicode\_ci



;

2026-05-14T21:23:51.533+09:00 DEBUG 2972 --- \[nio-8080-exec-4] o.s.jdbc.support.JdbcTransactionManager  : Releasing JDBC Connection \[HikariProxyConnection@1322956752 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@ee8df] after transaction

2026-05-14T21:23:51.535+09:00  WARN 2972 --- \[nio-8080-exec-4] .m.m.a.ExceptionHandlerExceptionResolver : Resolved \[com.prafta.common.exception.ApiException: 출퇴근 기록이 완료된 후에 초과근무를 등록할 수 있습니다.]



