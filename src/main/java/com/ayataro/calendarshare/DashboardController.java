package com.ayataro.calendarshare;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

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
    @GetMapping("/events/{id}/edit")
    public String editEventRow(@PathVariable Long id,
                               @RequestParam String date,
                               @RequestParam String ym,
                               Principal principal,
                               Model model) {

        var me = userService.currentUser(principal);

        var d = LocalDate.parse(date);
        var month = YearMonth.parse(ym);

        var dto = eventService.getVisibleEventDto(me, id); // 下で実装

        model.addAttribute("day", d);
        model.addAttribute("month", month);
        model.addAttribute("e", dto);

        return "fragments/event-row-edit";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }


    @GetMapping("/calendar/day")
    public String calendarDay(@RequestParam String date,
                              @RequestParam String ym,
                              Principal principal,
                              Model model) {

        var me = userService.currentUser(principal);
        var d = java.time.LocalDate.parse(date);
        var month = java.time.YearMonth.parse(ym);

        model.addAttribute("day", d);
        model.addAttribute("month", month);
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

    // --- actions ---
    @PostMapping("/events/day")
    public String createFromDay(@RequestParam String date,
                                @RequestParam String start,
                                @RequestParam String end,
                                @RequestParam String title,
                                @RequestParam(required=false) String location,
                                @RequestParam(required=false) String url,
                                @RequestParam(required=false) String description,
                                @RequestParam String ym,
                                Principal principal,
                                Model model) {

        var me = userService.currentUser(principal);

        var d = LocalDate.parse(date);
        var s = LocalTime.parse(start);
        var e = LocalTime.parse(end);

        eventService.create(me,
                title,
                LocalDateTime.of(d, s),
                LocalDateTime.of(d, e),
                location,
                url,
                description);

        var month = YearMonth.parse(ym);
        var vm = eventService.buildMonthView(me, month);

        model.addAttribute("month", month);
        model.addAttribute("weeks", vm.weeks());
        model.addAttribute("eventsByDay", vm.eventsByDay());

        return "fragments/calendar-month";
    }
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

    @PostMapping("/invites/{id}/reject")
    public String reject(@PathVariable Long id, Principal principal, Model model) {
        var me = userService.currentUser(principal);
        inviteService.reject(id, me);

        model.addAttribute("invites", inviteService.listInvitesForDashboard(me));
        return "fragments/invites";
    }

    // 拒否
    @DeleteMapping("/events/{id}/day")
    public String deleteEventFromDay(@PathVariable Long id,
                                     @RequestParam String ym,
                                     Principal principal,
                                     Model model,
                                     HttpServletResponse response) {

        var me = userService.currentUser(principal);
        eventService.deleteIfOwner(me, id);

        response.addHeader("HX-Trigger", "refresh-month");

        var d = LocalDate.now(); // モーダル再描画用（同じ日でもOK）
        model.addAttribute("day", d);
        model.addAttribute("month", YearMonth.parse(ym));
        model.addAttribute("events", eventService.listVisibleEventsForDate(me, d));

        return "fragments/calendar-day";
    }

    @DeleteMapping("/events/{id}")
    public String deleteFromModal(@PathVariable Long id,
                                  @RequestParam String date,
                                  @RequestParam String ym,
                                  Principal principal,
                                  Model model) {
        var me = userService.currentUser(principal);

        eventService.deleteIfOwner(me, id);

        var d = LocalDate.parse(date);
        var month = YearMonth.parse(ym);

        model.addAttribute("day", d);
        model.addAttribute("month", month);
        model.addAttribute("events", eventService.listVisibleEventsForDate(me, d));

        return "fragments/calendar-day :: modalBody";
    }


    // 更新
    @PutMapping("/events/{id}")
    public String updateEventRow(@PathVariable Long id,
                                 @RequestParam String date,
                                 @RequestParam String ym,
                                 @RequestParam String title,
                                 @RequestParam String start,
                                 @RequestParam String end,
                                 @RequestParam(required=false) String location,
                                 @RequestParam(required=false) String url,
                                 @RequestParam(required=false) String description,
                                 Principal principal,
                                 Model model) {

        var me = userService.currentUser(principal);

        var d = LocalDate.parse(date);
        var s = LocalTime.parse(start);
        var e = LocalTime.parse(end);

        eventService.updateIfOwner(me, id,
                title, LocalDateTime.of(d, s), LocalDateTime.of(d, e),
                location, url, description);

        // 更新後：その1行だけ表示に戻す
        var dto = eventService.getVisibleEventDto(me, id);
        model.addAttribute("day", d);
        model.addAttribute("month", YearMonth.parse(ym));
        model.addAttribute("e", dto);
        return "fragments/event-row-view";
    }

}