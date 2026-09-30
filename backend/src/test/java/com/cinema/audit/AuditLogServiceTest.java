package com.cinema.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditLogServiceTest {
    private final AuditLogRepository auditLogs = mock(AuditLogRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuditLogService service = new AuditLogService(auditLogs, users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminChangesRecordTheSignedInAdmin() {
        AuthUser admin = new AuthUser(UUID.randomUUID(), "Admin", "admin@cinema.test", UserRole.ADMIN);
        User adminRow = new User();
        adminRow.setId(admin.id());
        when(users.getReferenceById(admin.id())).thenReturn(adminRow);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

        service.recordAdmin("MOVIE_CREATED", "Movie", "m1", "QA Feature");

        AuditLog log = saved();
        assertThat(log.getActorUser()).isSameAs(adminRow);
        assertThat(log.getActorRole()).isEqualTo("ADMIN");
        assertThat(log.getAction()).isEqualTo("MOVIE_CREATED");
        assertThat(log.getNewValue()).isEqualTo("QA Feature");
    }

    @Test
    void adminChangesWithoutASignedInUserAreStillRecorded() {
        service.recordAdmin("CINEMA_CREATED", "Cinema", "c1", "QA Cinema");

        AuditLog log = saved();
        assertThat(log.getActorUser()).isNull();
        assertThat(log.getAction()).isEqualTo("CINEMA_CREATED");
    }

    private AuditLog saved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogs).save(captor.capture());
        return captor.getValue();
    }
}
