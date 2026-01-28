package com.ayataro.calendarshare;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository repo;
    private final InviteService inviteService;

    public EventService(EventRepository repo, InviteService inviteService) {
        this.repo = repo;
        this.inviteService = inviteService;
    }

    public void create(User owner, String title, LocalDateTime start, LocalDateTime end) {
        Event e = new Event();
        e.setTitle(title);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setOwner(owner);
        repo.save(e);
    }

    public void deleteIfOwner(User me, Long id) {
        Event e = repo.findById(id).orElseThrow();
        if (e.getOwner().equals(me)) {
            repo.delete(e);
        }
    }

    public List<EventDTO> listVisibleEventsForDate(User me, LocalDate date) {
        List<User> owners = new ArrayList<>(inviteService.listAcceptedPartners(me));
        owners.add(me);

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        return repo.findByOwnerInAndStartTimeBetweenOrderByStartTimeAsc(owners, from, to)
                .stream()
                .map(e -> new EventDTO(
                        e.getId(),
                        e.getTitle(),
                        e.getStartTime(),
                        e.getEndTime(),
                        e.getOwner().getEmail(),
                        e.getOwner().getId().equals(me.getId())
                ))
                .toList();
    }
    public record DayCell(java.time.LocalDate date, boolean inMonth) {}
    public record MonthView(java.util.List<java.util.List<DayCell>> weeks,
                            java.util.Map<LocalDate, List<EventDTO>> eventsByDay) {}

    public MonthView buildMonthView(User me, java.time.YearMonth month) {
        java.util.List<User> owners = new ArrayList<>(inviteService.listAcceptedPartners(me));
        owners.add(me);

        java.time.LocalDate firstOfMonth = month.atDay(1);
        java.time.DayOfWeek firstDow = firstOfMonth.getDayOfWeek(); // MON..SUN

        // カレンダーを日曜始まりにする（Sun=0）
        int shift = firstDow.getValue() % 7; // Mon=1..Sun=7 → Sun=0
        java.time.LocalDate gridStart = firstOfMonth.minusDays(shift);

        java.time.LocalDate gridEndExclusive = gridStart.plusDays(42); // 6週固定
        java.time.LocalDateTime from = gridStart.atStartOfDay();
        java.time.LocalDateTime to = gridEndExclusive.atStartOfDay();

        var events = repo.findByOwnerInAndStartTimeBetweenOrderByStartTimeAsc(owners, from, to)
                .stream()
                .map(e -> new EventDTO(
                        e.getId(), e.getTitle(), e.getStartTime(), e.getEndTime(),
                        e.getOwner().getEmail(),
                        e.getOwner().getId().equals(me.getId())
                ))
                .toList();

        java.util.Map<java.time.LocalDate, java.util.List<EventDTO>> byDay = new java.util.HashMap<>();
        for (var e : events) {
            java.time.LocalDate d = e.startTime().toLocalDate();
            byDay.computeIfAbsent(d, k -> new java.util.ArrayList<>()).add(e);
        }

        java.util.List<java.util.List<DayCell>> weeks = new java.util.ArrayList<>();
        java.time.LocalDate cur = gridStart;
        for (int w = 0; w < 6; w++) {
            java.util.List<DayCell> row = new java.util.ArrayList<>();
            for (int i = 0; i < 7; i++) {
                row.add(new DayCell(cur, cur.getMonth().equals(month.getMonth())));
                cur = cur.plusDays(1);
            }
            weeks.add(row);
        }

        return new MonthView(weeks, byDay);
    }
}