package com.ayataro.calendarshare;

public record InviteDTO(
        Long id,
        String partnerEmail,
        String status,
        boolean canAccept

        
) {}