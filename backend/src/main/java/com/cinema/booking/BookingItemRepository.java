package com.cinema.booking;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
    @Query("""
            select m.title, count(bi)
            from BookingItem bi join bi.booking b join b.showtime s join s.movie m
            where b.status in :statuses
            group by m.id, m.title
            order by count(bi) desc, m.title
            """)
    List<Object[]> countSeatsByMovie(@Param("statuses") Collection<BookingStatus> statuses, Pageable pageable);
}
