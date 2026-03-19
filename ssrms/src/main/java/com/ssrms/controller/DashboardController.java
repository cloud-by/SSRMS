package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.vo.MonthBriefVO;
import com.ssrms.controller.vo.TodayOverviewVO;
import com.ssrms.entity.Room;
import com.ssrms.mapper.ReservationMapper;
import com.ssrms.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private RoomService roomService;

    @GetMapping("/home")
    public Result home(@RequestParam Integer userId) {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Integer reserved = reservationMapper.countReservedNow(today, now);
        Integer inUse = reservationMapper.countInUseNow(today, now);

        reserved = (reserved == null) ? 0 : reserved;
        inUse = (inUse == null) ? 0 : inUse;

        int totalSeats = roomService.list().stream()
                .map(Room::getOpenSeats)
                .filter(x -> x != null && x > 0)
                .reduce(0, Integer::sum);

        int remaining = totalSeats - reserved - inUse;
        if (remaining < 0) remaining = 0;

        TodayOverviewVO todayOverview = new TodayOverviewVO(
                totalSeats, reserved, inUse, remaining
        );

        LocalDate monthStart = today.withDayOfMonth(1);
        // ✅ 关键：本月范围到当月最后一天，才能包含 12/21、12/24 这种未来预约
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        Long monthReserveCount = reservationMapper.countMonthReservations(userId, monthStart, monthEnd);
        Long studyMinutes = reservationMapper.sumMonthStudyMinutes(userId, monthStart, monthEnd);

        monthReserveCount = (monthReserveCount == null) ? 0L : monthReserveCount;
        studyMinutes = (studyMinutes == null) ? 0L : studyMinutes;

        // ✅ 最近一次到馆：也按本月范围统计
        LocalDateTime lastVisit = reservationMapper.selectLastVisitTime(userId, monthStart, monthEnd);

        monthReserveCount = (monthReserveCount == null) ? 0L : monthReserveCount;
        studyMinutes = (studyMinutes == null) ? 0L : studyMinutes;

        String lastVisitStr = null;
        if (lastVisit != null) {
            lastVisitStr = lastVisit.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        MonthBriefVO monthBrief = new MonthBriefVO(monthReserveCount, studyMinutes, lastVisitStr);

        Map<String, Object> data = new HashMap<>();
        data.put("todayOverview", todayOverview);
        data.put("monthBrief", monthBrief);

        return Result.success(data);
    }
}