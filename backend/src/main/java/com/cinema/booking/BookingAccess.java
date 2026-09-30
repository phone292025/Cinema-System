package com.cinema.booking;

import com.cinema.auth.AuthUser;
import com.cinema.common.ApiException;
import com.cinema.user.UserRole;

import org.springframework.http.HttpStatus;

public final class BookingAccess {
    private BookingAccess() {
    }

    public static void requireOwnerOrAdmin(AuthUser user, Booking booking) {
        if (!booking.getUser().getId().equals(user.id()) && user.role() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Booking does not belong to this user.");
        }
    }
}
