package com.prafta.common.cmm.audit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.audit.vo.AuditLogInsertVO;

/**
 * 감사 로그 매퍼 (PRAFTA-037-F5).
 *
 * <p>채번: {@link #selectNextAuditId(String)} — 'A' + YYYYMMDD + 회사별 시퀀스.
 * 적재: {@link #insertAuditLog(AuditLogInsertVO)} — 단순 INSERT 1행.
 */
@Mapper
public interface AuditLogMapper {

    /**
     * 감사 로그 PK 채번. {@code 'A' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'AUDIT_LOG_ID')}.
     */
    String selectNextAuditId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 감사 로그 1건 INSERT.
     */
    void insertAuditLog(AuditLogInsertVO vo);
}
