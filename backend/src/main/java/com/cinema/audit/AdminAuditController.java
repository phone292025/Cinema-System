package com.cinema.audit;

import java.util.List;

import com.cinema.audit.AuditLogDtos.AuditLogResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AuditLogRepository auditLogs;

    public AdminAuditController(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    List<AuditLogResponse> list() {
        return auditLogs.findTop100ByOrderByCreatedAtDesc().stream().map(AuditLogResponse::from).toList();
    }
}
