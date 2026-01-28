package com.ayataro.calendarshare;

import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String ownerEmail,
        boolean canDelete
) {}