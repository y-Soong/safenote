package com.prafta.common.cmm.audit.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
import com.prafta.common.cmm.audit.mapper.AuditLogMapper;
import com.prafta.common.cmm.audit.service.AuditLogService;
import com.prafta.common.cmm.audit.vo.AuditLogInsertVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 감사 로그 적재 구현 (PRAFTA-037-F5).
 *
 * <p>본업과 분리된 {@code REQUIRES_NEW} 트랜잭션으로 동작하며, 어떤 예외가 발생해도
 * 호출 측에 전파하지 않는다 (catch 후 {@code log.error} 만 — 본업 영향 0).
 * 적재 실패가 본업(다운로드/권한 변경 등) 응답을 막으면 안 된다는 정책 §11.3 의 요구.
 *
 * <p>User-Agent 는 컬럼 정의 상 ≤500자. 호출 측이 절단하지 않은 경우를 대비해 본 구현이
 * 한 번 더 안전하게 절단한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final int USER_AGENT_MAX_LEN = 500;

    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(AuditLogCommand command, AuditContext context) {

        // 필수값 누락은 감사 적재 자체가 의미 없으므로 본업을 막지 않고 log.warn 후 반환.
        if (command == null
                || isBlank(command.getCmpnyCd())
                || isBlank(command.getActionType())
                || isBlank(command.getResourceType())) {
            log.warn("감사 로그 적재 스킵 - 필수값 누락 (cmpnyCd/actionType/resourceType)");
            return;
        }

        try {
            String auditId = auditLogMapper.selectNextAuditId(command.getCmpnyCd());

            AuditLogInsertVO vo = new AuditLogInsertVO();
            vo.setAuditId(auditId);
            vo.setCmpnyCd(command.getCmpnyCd());
            vo.setUserCd(command.getUserCd());
            vo.setActionType(command.getActionType());
            vo.setResourceType(command.getResourceType());
            vo.setResourceKey(command.getResourceKey());
            vo.setDetailJson(command.getDetailJson());
            vo.setInsertNo(isBlank(command.getUserCd()) ? "SYSTEM" : command.getUserCd());

            if (context != null) {
                vo.setIpAddress(context.ipAddress());
                vo.setUserAgent(truncate(context.userAgent(), USER_AGENT_MAX_LEN));
            }

            auditLogMapper.insertAuditLog(vo);

            if (log.isDebugEnabled()) {
                log.debug("감사 로그 적재 - auditId={}, userCd={}, action={}, resource={}",
                        auditId, command.getUserCd(), command.getActionType(), command.getResourceType());
            }
        } catch (Exception e) {
            // 감사 적재 실패가 본업을 막지 않도록 catch 후 로그만 남기고 정상 return.
            log.error("감사 로그 적재 실패 - cmpnyCd={}, userCd={}, action={}, resource={}",
                    command.getCmpnyCd(), command.getUserCd(),
                    command.getActionType(), command.getResourceType(), e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
