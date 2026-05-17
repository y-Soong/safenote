package com.prafta.common.security.crypto.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.prafta.common.security.crypto.AesGcmCrypto;

import lombok.RequiredArgsConstructor;

/**
 * [목적]
 * MyBatis가 DB에서 문자열 컬럼을 읽어 Java 객체(UserInfo 등)에 매핑할 때,
 * AES-GCM으로 암호화된 값(v1.<payload>)은 자동으로 "복호화된 평문"으로 변환해 주는 TypeHandler.
 *
 * [왜 필요한가]
 * - 서비스/컨트롤러에서 리스트를 반복문으로 돌며 decrypt()를 호출하면 코드가 지저분해지고,
 *   특정 API에서 복호화 누락/중복 복호화 같은 실수가 발생하기 쉬움.
 * - 이 TypeHandler를 resultMap에 지정하면 "조회 시점"에 일괄 처리되어 일관성이 생김.
 *
 * [적용 범위]
 * - resultMap에서 MBL_NO_ENC / EMAIL_ENC / BIRTH_DT_ENC 같은 컬럼에만 지정해서 사용.
 *
 * [주의사항]
 * - 이 구현은 조회(get) 시 복호화가 목적이며, setNonNullParameter()는 기본 동작만 수행.
 *   (INSERT/UPDATE까지 자동 암호화를 원하면 Encrypt 전용 TypeHandler를 별도로 두는 것을 권장)
 * - v1. 포맷이 아닌 값은 복호화하지 않고 그대로 반환하여,
 *   평문 데이터 또는 마이그레이션 혼재 환경에서도 안전하게 동작하도록 함.
 */
@RequiredArgsConstructor
public class AesGcmDecryptTypeHandler extends BaseTypeHandler<String> {

    /**
     * 실제 AES-GCM 복호화를 수행하는 컴포넌트.
     * (키 관리/포맷 검증 포함)
     */
    private final AesGcmCrypto crypto;

    /**
     * PreparedStatement에 파라미터를 세팅할 때 호출됨.
     *
     * - 보통 SELECT 결과 매핑에서는 거의 사용되지 않음.
     * - 현재 구현은 "그대로 세팅"만 수행(암호화 안 함).
     * - INSERT/UPDATE에서 암호화를 자동 적용하려면 EncryptTypeHandler를 별도로 만들 것.
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter);
    }

    /**
     * ResultSet에서 컬럼명으로 값을 읽을 때 호출됨.
     * DB에서 읽은 문자열이 v1.<payload> 포맷이면 복호화하여 평문을 반환한다.
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decryptSafely(rs.getString(columnName));
    }

    /**
     * ResultSet에서 컬럼 인덱스로 값을 읽을 때 호출됨.
     * DB에서 읽은 문자열이 v1.<payload> 포맷이면 복호화하여 평문을 반환한다.
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decryptSafely(rs.getString(columnIndex));
    }

    /**
     * Stored Procedure 호출 등에서 CallableStatement로 값을 읽을 때 호출됨.
     * DB에서 읽은 문자열이 v1.<payload> 포맷이면 복호화하여 평문을 반환한다.
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decryptSafely(cs.getString(columnIndex));
    }

    /**
     * 복호화를 "안전하게" 수행하기 위한 가드 로직.
     *
     * - null/blank면 그대로 반환
     * - 암호문 포맷(v1.)이 아니면 그대로 반환 (평문/마이그레이션 데이터 혼재 대비)
     * - v1. 포맷이면 crypto.decrypt()로 복호화
     */
    private String decryptSafely(String v) {
        if (v == null || v.isBlank()) return v;

        // 암호 포맷(v1.)일 때만 복호화 시도 (평문/마이그레이션 데이터 혼재 대비)
        if (!v.startsWith("v1.")) return v;

        return crypto.decrypt(v);
    }
}