package com.cinema.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.cinema.common.ApiException;
import com.cinema.common.SchedulerGuard;
import com.cinema.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class IdempotencyServiceTest {
    private final IdempotencyKeyRepository keys = mock(IdempotencyKeyRepository.class);
    private final IdempotencyService service = new IdempotencyService(keys, mock(UserRepository.class), mock(SchedulerGuard.class));

    @Test
    void createsPendingRecordForFirstRequest() {
        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.empty());

        IdempotencyService.CachedResponse response = service.checkOrCreate("key-1", "user-1", null, "hash-a");

        assertThat(response.hit()).isFalse();
        verify(keys).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    void concurrentFirstUseOfSameKeyBecomesConflict() {
        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.empty());
        when(keys.saveAndFlush(any(IdempotencyKey.class))).thenThrow(new DataIntegrityViolationException("uq_idempotency_actor_key"));

        assertThatThrownBy(() -> service.checkOrCreate("key-1", "user-1", null, "hash-a"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void returnsCachedResponseForSameCompletedRequest() {
        IdempotencyKey existing = new IdempotencyKey();
        existing.setRequestHash("hash-a");
        existing.setStatusCode(201);
        existing.setResponseBody("{\"ok\":true}");
        existing.setCompletedAt(Instant.now());

        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.of(existing));

        IdempotencyService.CachedResponse response = service.checkOrCreate("key-1", "user-1", null, "hash-a");

        assertThat(response.hit()).isTrue();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.responseBody()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void rejectsSameKeyWithDifferentRequestHash() {
        IdempotencyKey existing = new IdempotencyKey();
        existing.setRequestHash("hash-a");
        existing.setCompletedAt(Instant.now());

        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.checkOrCreate("key-1", "user-1", null, "hash-b"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsSameKeyWhileFirstRequestIsStillProcessing() {
        IdempotencyKey existing = new IdempotencyKey();
        existing.setRequestHash("hash-a");

        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.checkOrCreate("key-1", "user-1", null, "hash-a"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getMessage()).contains("still processing"));
    }

    @Test
    void cleanupPurgesAbandonedAndOldCompletedKeys() {
        Instant before = Instant.now();

        service.cleanupExpiredKeys();

        ArgumentCaptor<Instant> abandonedCutoff = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> completedCutoff = ArgumentCaptor.forClass(Instant.class);
        verify(keys).deleteAbandonedBefore(abandonedCutoff.capture());
        verify(keys).deleteCompletedBefore(completedCutoff.capture());
        assertThat(Duration.between(abandonedCutoff.getValue(), before)).isBetween(Duration.ofHours(1).minusSeconds(5), Duration.ofHours(1).plusSeconds(5));
        assertThat(Duration.between(completedCutoff.getValue(), before)).isBetween(Duration.ofHours(24).minusSeconds(5), Duration.ofHours(24).plusSeconds(5));
    }
}
