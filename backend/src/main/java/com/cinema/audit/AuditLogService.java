package com.cinema.audit;

import com.cinema.auth.AuthUser;
import com.cinema.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogs;
    private final UserRepository users;

    public AuditLogService(AuditLogRepository auditLogs, UserRepository users) {
        this.auditLogs = auditLogs;
        this.users = users;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuthUser actor, String action, String entityType, String entityId, String oldValue, String newValue, String ipAddress) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            users.findById(actor.id()).ifPresent(log::setActorUser);
            log.setActorRole(actor.role().name());
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setIpAddress(ipAddress);
        auditLogs.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void system(String action, String entityType, String entityId, String newValue) {
        AuditLog log = new AuditLog();
        log.setActorRole("SYSTEM");
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setNewValue(newValue);
        auditLogs.save(log);
    }
}
