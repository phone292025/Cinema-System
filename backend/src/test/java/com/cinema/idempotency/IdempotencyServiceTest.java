package com.cinema.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.cinema.common.ApiException;
import com.cinema.user.UserRepository;

import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {
    @Test
    void createsPendingRecordForFirstRequest() {
        IdempotencyKeyRepository keys = mock(IdempotencyKeyRepository.class);
        IdempotencyService service = new IdempotencyService(keys, mock(UserRepository.class));

        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.empty());

        IdempotencyService.CachedResponse response = service.checkOrCreate("key-1", "user-1", null, "hash-a");

        assertThat(response.hit()).isFalse();
        verify(keys).save(any(IdempotencyKey.class));
    }

    @Test
    void returnsCachedResponseForSameCompletedRequest() {
        IdempotencyKeyRepository keys = mock(IdempotencyKeyRepository.class);
        IdempotencyService service = new IdempotencyService(keys, mock(UserRepository.class));
        IdempotencyKey existing = new IdempotencyKey();
        existing.setRequestHash("hash-a");
        existing.setStatusCode(200);
        existing.setResponseBody("{\"ok\":true}");
        existing.setCompletedAt(Instant.now());

        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.of(existing));

        IdempotencyService.CachedResponse response = service.checkOrCreate("key-1", "user-1", null, "hash-a");

        assertThat(response.hit()).isTrue();
        assertThat(response.responseBody()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void rejectsSameKeyWithDifferentRequestHash() {
        IdempotencyKeyRepository keys = mock(IdempotencyKeyRepository.class);
        IdempotencyService service = new IdempotencyService(keys, mock(UserRepository.class));
        IdempotencyKey existing = new IdempotencyKey();
        existing.setRequestHash("hash-a");
        existing.setCompletedAt(Instant.now());

        when(keys.findByActorKeyAndKeyValue("user-1", "key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.checkOrCreate("key-1", "user-1", null, "hash-b"))
                .isInstanceOf(ApiException.class);
    }
}
