package com.ayataro.calendarshare;

import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class DashboardController {

    private final UserService userService;
    private final InviteService inviteService;
    private final EventService eventService;

    public DashboardController(UserService userService, InviteService inviteService, EventService eventService) {
        this.userService = userService;
        this.inviteService = inviteService;
        this.eventService = eventService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/calendar/day")
    public String calendarDay(@RequestParam(required = false) String date,
                              Principal principal,
                              Model model) {

        var me = userService.currentUser(principal);

        java.time.LocalDate d = (date == null || date.isBlank())
                ? java.time.LocalDate.now()
                : java.time.LocalDate.parse(date);

        model.addAttribute("day", d);
        model.addAttribute("events", eventService.listVisibleEventsForDate(me, d));
        return "fragments/calendar-day";
    }
    @GetMapping("/calendar/month")
    public String calendarMonth(@RequestParam(required = false) String ym,
                                Principal principal,
                                Model model) {
        var me = userService.currentUser(principal);

        java.time.YearMonth month = (ym == null || ym.isBlank())
                ? java.time.YearMonth.now()
                : java.time.YearMonth.parse(ym); // "2026-01" 形式

        var vm = eventService.buildMonthView(me,month);

        model.addAttribute("month", month);
        model.addAttribute("weeks", vm.weeks());      // List<List<DayCell>>
        model.addAttribute("eventsByDay", vm.eventsByDay()); // Map<LocalDate, List<EventDto>>
        return "fragments/calendar-month";
    }
    // --- fragments ---
    @GetMapping("/fragments/invites")
    public String invites(Model model, Principal principal) {
        var me = userService.currentUser(principal);
        model.addAttribute("invites", inviteService.listInvitesForDashboard(me));
        return "fragments/invites";
    }

    @GetMapping("/fragments/partners")
    public String partners(Model model, Principal principal) {
        var me = userService.currentUser(principal);
        model.addAttribute("partners", inviteService.listAcceptedPartners(me));
        return "fragments/partners";
    }

    @GetMapping("/fragments/events")
    public String events(Model model, Principal principal) {
        var me = userService.currentUser(principal);
        model.addAttribute("events", eventService.listVisibleEventsForDate(me, java.time.LocalDate.now()));
        return "fragments/events";
    }

    // --- actions ---
    @PostMapping("/invites")
    public String createInvite(@RequestParam String email, Principal principal, Model model) {
        var me = userService.currentUser(principal);
        inviteService.invite(me, email);

        // 更新対象のfragmentを返す
        model.addAttribute("invites", inviteService.listInvitesForDashboard(me));
        return "fragments/invites";
    }

    // 承認
    @PostMapping("/invites/{id}/accept")
    public String accept(@PathVariable Long id, Principal principal, Model model) {
        var me = userService.currentUser(principal);
        inviteService.accept(id, me);

        model.addAttribute("invites", inviteService.listInvitesForDashboard(me));
        return "fragments/invites";
    }

    // 拒否
    @PostMapping("/invites/{id}/reject")
    public String reject(@PathVariable Long id, Principal principal, Model model) {
        var me = userService.currentUser(principal);
        inviteService.reject(id, me);

        model.addAttribute("invites", inviteService.listInvitesForDashboard(me));
        return "fragments/invites";
    }

    @PostMapping("/events")
    public String createEvent(@RequestParam String title,
                              @RequestParam String start,
                              @RequestParam String end,
                              @RequestParam(required = false) String ym,
                              Principal principal,
                              Model model) {

        var me = userService.currentUser(principal);

        var st = java.time.LocalDateTime.parse(start);
        var en = java.time.LocalDateTime.parse(end);

        eventService.create(me, title, st, en);

        // どの月を再描画するか：送られてきた ym、なければ start の月
        java.time.YearMonth month = (ym != null && !ym.isBlank())
                ? java.time.YearMonth.parse(ym)
                : java.time.YearMonth.from(st);

        var vm = eventService.buildMonthView(me, month);
        model.addAttribute("month", month);
        model.addAttribute("weeks", vm.weeks());
        model.addAttribute("eventsByDay", vm.eventsByDay());

        return "fragments/calendar-month";
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@PathVariable Long id, Principal principal, Model model) {
        var me = userService.currentUser(principal);
        eventService.deleteIfOwner(me, id);

        model.addAttribute("events", eventService.listVisibleEventsForDate(me, java.time.LocalDate.now()));
        return "fragments/events";
    }

}