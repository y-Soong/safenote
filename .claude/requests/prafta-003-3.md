2026-05-14T21:55:30.607+09:00 DEBUG 2972 --- \[nio-8080-exec-7] .ApiPrefixConfig$ApiPrefixHandlerMapping : Mapped to com.prafta.web.attd.attd07.controller.Attd07Controller#updateUserOvertimeRequests(UpdateUserOvertimeRequestRequest, String)

2026-05-14T21:55:30.613+09:00  WARN 2972 --- \[nio-8080-exec-7] .m.m.a.ExceptionHandlerExceptionResolver : Resolved \[com.prafta.common.exception.ApiException:   ūԴϴ.<EOL>ڿ ּ.]

2026-05-14T21:55:30.619+09:00 DEBUG 2972 --- \[nio-8080-exec-9] .ApiPrefixConfig$ApiPrefixHandlerMapping : Mapped to com.prafta.common.cmm.auth.controller.AuthController#refresh(RefreshRequest)

2026-05-14T21:55:30.619+09:00  INFO 2972 --- \[nio-8080-exec-9] c.prafta.common.aop.log.LoggingAspect    : \[Before] Method: AuthController.refresh(..)

2026-05-14T21:55:30.621+09:00 DEBUG 2972 --- \[nio-8080-exec-9] org.mybatis.spring.SqlSessionUtils       : Creating a new SqlSession

2026-05-14T21:55:30.621+09:00 DEBUG 2972 --- \[nio-8080-exec-9] org.mybatis.spring.SqlSessionUtils       : SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@6eab37ec] was not registered for synchronization because synchronization is not active

2026-05-14T21:55:30.621+09:00 DEBUG 2972 --- \[nio-8080-exec-9] o.s.jdbc.datasource.DataSourceUtils      : Fetching JDBC Connection from DataSource

2026-05-14T21:55:30.621+09:00 DEBUG 2972 --- \[nio-8080-exec-9] o.m.s.t.SpringManagedTransaction         : JDBC Connection \[HikariProxyConnection@526585434 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2d90846] will not be managed by Spring

2026-05-14T21:55:30.621+09:00 DEBUG 2972 --- \[nio-8080-exec-9] .c.c.a.m.A.selectValidByRefreshTokenHash : ==>  Preparing: /\* AuthMapper.selectValidByRefreshTokenHash \*/ SELECT CMPNY\_CD AS cmpnyCd , USER\_CD AS userCd , TOKEN\_ID AS tokenId , CLIENT\_TYPE AS clientType , DEVICE\_ID AS deviceId , REFRESH\_TOKEN\_HASH AS refreshTokenHash , ISSUED\_DTIME AS issuedDtime , EXPIRE\_DTIME AS expireDtime , REVOKED\_YN AS revokedYn , REVOKED\_DTIME AS revokedDtime , IP\_ADDR AS ipAddr , USER\_AGENT AS userAgent , INSERT\_NO AS insertNo , INSERT\_DATE AS insertDate , UPDATE\_NO AS updateNo , UPDATE\_DATE AS updateDate FROM TB\_AUTH\_TOKEN WHERE REFRESH\_TOKEN\_HASH = ? AND REVOKED\_YN = 'N' AND EXPIRE\_DTIME > NOW() LIMIT 1

2026-05-14T21:55:30.622+09:00 DEBUG 2972 --- \[nio-8080-exec-9] .c.c.a.m.A.selectValidByRefreshTokenHash : ==> Parameters: QhFw3rDKyz8JU6Kjex8M9fV1zJwo\_z3OjERmO\_Gjgio(String)

2026-05-14T21:55:30.623+09:00  INFO 2972 --- \[nio-8080-exec-9] p6spy                                    : #1778763330623 | took 0ms | statement | connection 20| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul\&useUnicode=true\&characterEncoding=utf8\&connectionCollation=utf8mb4\_unicode\_ci

/\* AuthMapper.selectValidByRefreshTokenHash \*/

&#x09;    SELECT

&#x09;		CMPNY\_CD            AS cmpnyCd

&#x09;		, USER\_CD             AS userCd

&#x09;		, TOKEN\_ID            AS tokenId

&#x09;		, CLIENT\_TYPE         AS clientType

&#x09;		, DEVICE\_ID           AS deviceId

&#x09;		, REFRESH\_TOKEN\_HASH  AS refreshTokenHash

&#x09;		, ISSUED\_DTIME        AS issuedDtime

&#x09;		, EXPIRE\_DTIME        AS expireDtime

&#x09;		, REVOKED\_YN          AS revokedYn

&#x09;		, REVOKED\_DTIME       AS revokedDtime

&#x09;		, IP\_ADDR             AS ipAddr

&#x09;		, USER\_AGENT          AS userAgent

&#x09;		, INSERT\_NO           AS insertNo

&#x09;		, INSERT\_DATE         AS insertDate

&#x09;		, UPDATE\_NO           AS updateNo

&#x09;		, UPDATE\_DATE         AS updateDate

&#x09;    FROM TB\_AUTH\_TOKEN

&#x09;    WHERE REFRESH\_TOKEN\_HASH = ?

&#x09;      AND REVOKED\_YN = 'N'

&#x09;      AND EXPIRE\_DTIME > NOW()

&#x09;    LIMIT 1

/\* AuthMapper.selectValidByRefreshTokenHash \*/

&#x09;    SELECT

&#x09;		CMPNY\_CD            AS cmpnyCd

&#x09;		, USER\_CD             AS userCd

&#x09;		, TOKEN\_ID            AS tokenId

&#x09;		, CLIENT\_TYPE         AS clientType

&#x09;		, DEVICE\_ID           AS deviceId

&#x09;		, REFRESH\_TOKEN\_HASH  AS refreshTokenHash

&#x09;		, ISSUED\_DTIME        AS issuedDtime

&#x09;		, EXPIRE\_DTIME        AS expireDtime

&#x09;		, REVOKED\_YN          AS revokedYn

&#x09;		, REVOKED\_DTIME       AS revokedDtime

&#x09;		, IP\_ADDR             AS ipAddr

&#x09;		, USER\_AGENT          AS userAgent

&#x09;		, INSERT\_NO           AS insertNo

&#x09;		, INSERT\_DATE         AS insertDate

&#x09;		, UPDATE\_NO           AS updateNo

&#x09;		, UPDATE\_DATE         AS updateDate

&#x09;    FROM TB\_AUTH\_TOKEN

&#x09;    WHERE REFRESH\_TOKEN\_HASH = 'QhFw3rDKyz8JU6Kjex8M9fV1zJwo\_z3OjERmO\_Gjgio'

&#x09;      AND REVOKED\_YN = 'N'

&#x09;      AND EXPIRE\_DTIME > NOW()

&#x09;    LIMIT 1;

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] .c.c.a.m.A.selectValidByRefreshTokenHash : <==      Total: 1

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] org.mybatis.spring.SqlSessionUtils       : Closing non transactional SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@6eab37ec]

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] org.mybatis.spring.SqlSessionUtils       : Creating a new SqlSession

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] org.mybatis.spring.SqlSessionUtils       : SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@4232d575] was not registered for synchronization because synchronization is not active

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] o.s.jdbc.datasource.DataSourceUtils      : Fetching JDBC Connection from DataSource

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] o.m.s.t.SpringManagedTransaction         : JDBC Connection \[HikariProxyConnection@1972477576 wrapping com.p6spy.engine.wrapper.ConnectionWrapper@2d90846] will not be managed by Spring

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] c.p.c.c.a.m.AuthMapper.selectUserForJwt  : ==>  Preparing: /\* AuthMapper.selectUserForJwt \*/ SELECT A.CMPNY\_CD AS cmpnyCd , A.USER\_CD AS userCd , A.USER\_ID AS userId , A.USER\_NM AS userNm , '' AS userPw , A.AUTH\_CD AS authCd , FNC\_CMM\_INFO\_SRCH( A.CMPNY\_CD , 'AUTH\_LEVEL' , A.AUTH\_CD , null ) AS authLevel , A.SITE\_CD AS siteCd , B.SITE\_NO AS siteNo , B.SITE\_NM AS siteNm , A.NODE\_CD AS nodeCd , C.NODE\_NM AS nodeNm , A.MBL\_NO\_ENC AS mblNoEnc , A.EMAIL\_ENC AS emailEnc , A.PWD\_LOCK\_YN AS pwdLockYn , A.PWD\_FAIL\_CNT AS pwdFailCnt , A.PWD\_LOCK\_EXPIRE\_DTIME AS pwdLockExpireDtime , A.PWD\_CHG\_DTIME AS pwdChgDtime , A.WITHDRAWAL\_DATE AS withdrawalDate , A.LAST\_LOGIN\_DTIME AS lastLoginDtime , A.INSERT\_NO AS insertNo , A.INSERT\_DATE AS insertDate , A.UPDATE\_NO AS updateNo , A.UPDATE\_DATE AS updateDate FROM TB\_USER A INNER JOIN TB\_SITE B ON ( A.CMPNY\_CD = B.CMPNY\_CD AND A.SITE\_CD = B.SITE\_CD ) LEFT OUTER JOIN TB\_SITE\_NODE C ON ( A.CMPNY\_CD = C.CMPNY\_CD AND A.SITE\_CD = C.SITE\_CD AND A.NODE\_CD = C.NODE\_CD ) WHERE 1=1 AND A.USER\_CD = ? AND A.USE\_YN = 'Y'

2026-05-14T21:55:30.623+09:00 DEBUG 2972 --- \[nio-8080-exec-9] c.p.c.c.a.m.AuthMapper.selectUserForJwt  : ==> Parameters: 20260400010(String)

2026-05-14T21:55:30.624+09:00  INFO 2972 --- \[nio-8080-exec-9] p6spy                                    : #1778763330624 | took 1ms | statement | connection 20| url jdbc:p6spy:mysql://localhost:3306/prafta?serverTimezone=Asia/Seoul\&useUnicode=true\&characterEncoding=utf8\&connectionCollation=utf8mb4\_unicode\_ci

/\* AuthMapper.selectUserForJwt \*/

&#x09;SELECT 

&#x09;	A.CMPNY\_CD			AS cmpnyCd

&#x09;	, A.USER\_CD			AS userCd

&#x09;	, A.USER\_ID			AS userId

&#x09;	, A.USER\_NM			AS userNm

&#x09;	, ''				AS userPw

&#x09;	, A.AUTH\_CD			AS authCd

&#x09;	, FNC\_CMM\_INFO\_SRCH(

&#x09;		A.CMPNY\_CD

&#x09;		, 'AUTH\_LEVEL'

&#x09;		, A.AUTH\_CD

&#x09;		, null

&#x09;	)					AS authLevel

&#x20;       , A.SITE\_CD			AS siteCd

&#x20;       , B.SITE\_NO			AS siteNo

&#x20;       , B.SITE\_NM			AS siteNm

&#x20;       , A.NODE\_CD			AS nodeCd

&#x20;       , C.NODE\_NM			AS nodeNm

&#x09;	, A.MBL\_NO\_ENC				AS mblNoEnc

&#x09;	, A.EMAIL\_ENC				AS emailEnc

&#x09;	, A.PWD\_LOCK\_YN				AS pwdLockYn

&#x09;	, A.PWD\_FAIL\_CNT			AS pwdFailCnt

&#x09;	, A.PWD\_LOCK\_EXPIRE\_DTIME	AS pwdLockExpireDtime

&#x09;	, A.PWD\_CHG\_DTIME			AS pwdChgDtime

&#x09;	, A.WITHDRAWAL\_DATE			AS withdrawalDate

&#x09;	, A.LAST\_LOGIN\_DTIME		AS lastLoginDtime

&#x09;	, A.INSERT\_NO				AS insertNo

&#x09;	, A.INSERT\_DATE		AS insertDate

&#x09;	, A.UPDATE\_NO		AS updateNo

&#x09;	, A.UPDATE\_DATE		AS updateDate

&#x09;FROM TB\_USER A

&#x20;        INNER JOIN TB\_SITE B

&#x09;		ON (

&#x09;			A.CMPNY\_CD = B.CMPNY\_CD

&#x09;		AND A.SITE\_CD = B.SITE\_CD

&#x20;           )

&#x20;        LEFT OUTER JOIN TB\_SITE\_NODE C

&#x20;        	ON (

&#x09;			A.CMPNY\_CD = C.CMPNY\_CD

&#x09;		AND A.SITE\_CD = C.SITE\_CD

&#x09;		AND A.NODE\_CD = C.NODE\_CD

&#x09;		)

&#x09;WHERE 1=1

&#x09;  AND A.USER\_CD = ?

&#x09;  AND A.USE\_YN = 'Y'

/\* AuthMapper.selectUserForJwt \*/

&#x09;SELECT 

&#x09;	A.CMPNY\_CD			AS cmpnyCd

&#x09;	, A.USER\_CD			AS userCd

&#x09;	, A.USER\_ID			AS userId

&#x09;	, A.USER\_NM			AS userNm

&#x09;	, ''				AS userPw

&#x09;	, A.AUTH\_CD			AS authCd

&#x09;	, FNC\_CMM\_INFO\_SRCH(

&#x09;		A.CMPNY\_CD

&#x09;		, 'AUTH\_LEVEL'

&#x09;		, A.AUTH\_CD

&#x09;		, null

&#x09;	)					AS authLevel

&#x20;       , A.SITE\_CD			AS siteCd

&#x20;       , B.SITE\_NO			AS siteNo

&#x20;       , B.SITE\_NM			AS siteNm

&#x20;       , A.NODE\_CD			AS nodeCd

&#x20;       , C.NODE\_NM			AS nodeNm

&#x09;	, A.MBL\_NO\_ENC				AS mblNoEnc

&#x09;	, A.EMAIL\_ENC				AS emailEnc

&#x09;	, A.PWD\_LOCK\_YN				AS pwdLockYn

&#x09;	, A.PWD\_FAIL\_CNT			AS pwdFailCnt

&#x09;	, A.PWD\_LOCK\_EXPIRE\_DTIME	AS pwdLockExpireDtime

&#x09;	, A.PWD\_CHG\_DTIME			AS pwdChgDtime

&#x09;	, A.WITHDRAWAL\_DATE			AS withdrawalDate

&#x09;	, A.LAST\_LOGIN\_DTIME		AS lastLoginDtime

&#x09;	, A.INSERT\_NO				AS insertNo

&#x09;	, A.INSERT\_DATE		AS insertDate

&#x09;	, A.UPDATE\_NO		AS updateNo

&#x09;	, A.UPDATE\_DATE		AS updateDate

&#x09;FROM TB\_USER A

&#x20;        INNER JOIN TB\_SITE B

&#x09;		ON (

&#x09;			A.CMPNY\_CD = B.CMPNY\_CD

&#x09;		AND A.SITE\_CD = B.SITE\_CD

&#x20;           )

&#x20;        LEFT OUTER JOIN TB\_SITE\_NODE C

&#x20;        	ON (

&#x09;			A.CMPNY\_CD = C.CMPNY\_CD

&#x09;		AND A.SITE\_CD = C.SITE\_CD

&#x09;		AND A.NODE\_CD = C.NODE\_CD

&#x09;		)

&#x09;WHERE 1=1

&#x09;  AND A.USER\_CD = '20260400010'

&#x09;  AND A.USE\_YN = 'Y';

2026-05-14T21:55:30.626+09:00 DEBUG 2972 --- \[nio-8080-exec-9] c.p.c.c.a.m.AuthMapper.selectUserForJwt  : <==      Total: 1

2026-05-14T21:55:30.626+09:00 DEBUG 2972 --- \[nio-8080-exec-9] org.mybatis.spring.SqlSessionUtils       : Closing non transactional SqlSession \[org.apache.ibatis.session.defaults.DefaultSqlSession@4232d575]

2026-05-14T21:55:30.627+09:00  INFO 2972 --- \[nio-8080-exec-9] c.prafta.common.aop.log.LoggingAspect    : \[After] Method: AuthController.refresh(..), Return: <200 OK OK,com.prafta.common.cmm.auth.dto.response.RefreshResponse@3dde9721,\[]>

2026-05-14T21:55:30.632+09:00 DEBUG 2972 --- \[io-8080-exec-10] .ApiPrefixConfig$ApiPrefixHandlerMapping : Mapped to com.prafta.web.attd.attd07.controller.Attd07Controller#updateUserOvertimeRequests(UpdateUserOvertimeRequestRequest, String)

2026-05-14T21:55:30.635+09:00  WARN 2972 --- \[io-8080-exec-10] .m.m.a.ExceptionHandlerExceptionResolver : Resolved \[org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument \[0] in public org.springframework.http.ResponseEntity<?> com.prafta.web.attd.attd07.controller.Attd07Controller.updateUserOvertimeRequests(com.prafta.web.attd.attd07.dto.request.UpdateUserOvertimeRequestRequest,java.lang.String) with 4 errors: \[Field error in object 'updateUserOvertimeRequestRequest' on field 'workYmd': rejected value \[null]; codes \[NotBlank.updateUserOvertimeRequestRequest.workYmd,NotBlank.workYmd,NotBlank.java.lang.String,NotBlank]; arguments \[org.springframework.context.support.DefaultMessageSourceResolvable: codes \[updateUserOvertimeRequestRequest.workYmd,workYmd]; arguments \[]; default message \[workYmd]]; default message \[공백일 수 없습니다]] \[Field error in object 'updateUserOvertimeRequestRequest' on field 'userCd': rejected value \[null]; codes \[NotBlank.updateUserOvertimeRequestRequest.userCd,NotBlank.userCd,NotBlank.java.lang.String,NotBlank]; arguments \[org.springframework.context.support.DefaultMessageSourceResolvable: codes \[updateUserOvertimeRequestRequest.userCd,userCd]; arguments \[]; default message \[userCd]]; default message \[공백일 수 없습니다]] \[Field error in object 'updateUserOvertimeRequestRequest' on field 'overtimes': rejected value \[null]; codes \[NotEmpty.updateUserOvertimeRequestRequest.overtimes,NotEmpty.overtimes,NotEmpty.java.util.List,NotEmpty]; arguments \[org.springframework.context.support.DefaultMessageSourceResolvable: codes \[updateUserOvertimeRequestRequest.overtimes,overtimes]; arguments \[]; default message \[overtimes]]; default message \[비어 있을 수 없습니다]] \[Field error in object 'updateUserOvertimeRequestRequest' on field 'siteCd': rejected value \[null]; codes \[NotBlank.updateUserOvertimeRequestRequest.siteCd,NotBlank.siteCd,NotBlank.java.lang.String,NotBlank]; arguments \[org.springframework.context.support.DefaultMessageSourceResolvable: codes \[updateUserOvertimeRequestRequest.siteCd,siteCd]; arguments \[]; default message \[siteCd]]; default message \[공백일 수 없습니다]] ]



