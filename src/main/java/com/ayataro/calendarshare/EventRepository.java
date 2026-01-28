package com.ayataro.calendarshare;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOwnerInAndStartTimeBetweenOrderByStartTimeAsc(
            Collection<User> owners,
            LocalDateTime from,
            LocalDateTime to
    );

}
