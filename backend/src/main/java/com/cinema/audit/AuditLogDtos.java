package com.cinema.audit;

import java.time.Instant;
import java.util.UUID;

public final class AuditLogDtos {
    private AuditLogDtos() {
    }

    public record AuditLogResponse(UUID id, UUID actorUserId, String actorRole, String action, String entityType,
            String entityId, String oldValue, String newValue, String ipAddress, Instant createdAt) {
        public static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(log.getId(), log.getActorUser() == null ? null : log.getActorUser().getId(),
                    log.getActorRole(), log.getAction(), log.getEntityType(), log.getEntityId(), log.getOldValue(),
                    log.getNewValue(), log.getIpAddress(), log.getCreatedAt());
        }
    }
}
