package com.ssrms.controller.mobile;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ssrms.common.Result;
import com.ssrms.controller.vo.AdminDashboardHomeVO;
import com.ssrms.controller.vo.AnnouncementVO;
import com.ssrms.controller.vo.MonthBriefVO;
import com.ssrms.controller.vo.TodayOverviewVO;
import com.ssrms.entity.Announcement;
import com.ssrms.entity.Feedback;
import com.ssrms.entity.Reservation;
import com.ssrms.entity.Room;
import com.ssrms.entity.Seat;
import com.ssrms.entity.User;
import com.ssrms.mapper.ReservationMapper;
import com.ssrms.mapper.ViolationMapper;
import com.ssrms.service.AdminDashboardService;
import com.ssrms.service.AnnouncementService;
import com.ssrms.service.FeedbackService;
import com.ssrms.service.RoomService;
import com.ssrms.service.SeatService;
import com.ssrms.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mobile")
public class MobilePortalController {

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ViolationMapper violationMapper;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private FeedbackService feedbackService;

    @Resource
    private AdminDashboardService adminDashboardService;

    @GetMapping("/user/home")
    public Result userHome(@RequestParam Integer userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Integer reserved = reservationMapper.countReservedNow(today, now);
        Integer inUse = reservationMapper.countInUseNow(today, now);
        reserved = reserved == null ? 0 : reserved;
        inUse = inUse == null ? 0 : inUse;
        int totalSeats = roomService.list().stream()
                .map(Room::getOpenSeats)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int remaining = Math.max(0, totalSeats - reserved - inUse);
        TodayOverviewVO todayOverview = new TodayOverviewVO(totalSeats, reserved, inUse, remaining);

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        Long monthReserveCount = reservationMapper.countMonthReservations(userId, monthStart, monthEnd);
        Long studyMinutes = reservationMapper.sumMonthStudyMinutes(userId, monthStart, monthEnd);
        LocalDateTime lastVisit = reservationMapper.selectLastVisitTime(userId, monthStart, monthEnd);
        MonthBriefVO monthBrief = new MonthBriefVO(
                monthReserveCount == null ? 0L : monthReserveCount,
                studyMinutes == null ? 0L : studyMinutes,
                lastVisit == null ? null : lastVisit.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );

        Integer sum = violationMapper.sumUnhandledPenalty(userId);
        int penalty = sum == null ? 0 : sum;
        int credit = Math.max(0, 100 - penalty);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("account", user.getAccount());
        profile.put("name", user.getName());
        profile.put("studentNo", user.getStudentNo());
        profile.put("creditScore", credit);
        profile.put("blacklistFlag", user.getBlacklistFlag());
        profile.put("commonCampus", user.getCommonCampus());
        profile.put("lastLoginIp", user.getLastLoginIp());
        profile.put("lastLoginTime", user.getLastLoginTime());

        Reservation nextReservation = userServiceNextReservation(userId, today);
        Map<String, Object> nextReservationMap = nextReservation == null ? null : buildReservationCard(nextReservation);

        List<AnnouncementVO> notices = loadAnnouncements(1, 5);

        Map<String, Object> resp = new HashMap<>();
        resp.put("profile", profile);
        resp.put("todayOverview", todayOverview);
        resp.put("monthBrief", monthBrief);
        resp.put("nextReservation", nextReservationMap);
        resp.put("notices", notices);
        return Result.success(resp);
    }

    @GetMapping("/admin/workbench")
    public Result adminWorkbench(@RequestParam(defaultValue = "8") Integer recentLimit) {
        AdminDashboardHomeVO home = adminDashboardService.buildHome(Math.max(4, recentLimit));
        Map<String, Object> feedbackStats = new HashMap<>();
        feedbackStats.put("pending", feedbackService.count(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, "pending")));
        feedbackStats.put("processing", feedbackService.count(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, "processing")));
        feedbackStats.put("resolved", feedbackService.count(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, "resolved")));

        Map<String, Object> resp = new HashMap<>();
        resp.put("metrics", home == null ? null : home.getMetrics());
        resp.put("weekly", home == null ? null : home.getWeekly());
        resp.put("latestActivities", home == null ? Collections.emptyList() : home.getLatestActivities());
        resp.put("feedbackStats", feedbackStats);
        resp.put("notices", loadAnnouncements(0, 6));
        return Result.success(resp);
    }

    private Reservation userServiceNextReservation(Integer userId, LocalDate today) {
        List<Reservation> list = reservationMapper.selectList(
                Wrappers.<Reservation>lambdaQuery()
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, List.of("reserved", "checked_in", "late"))
                        .ge(Reservation::getDate, today)
                        .orderByAsc(Reservation::getDate)
                        .orderByAsc(Reservation::getStartTime)
                        .last("limit 1")
        );
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Object> buildReservationCard(Reservation reservation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", reservation.getId());
        map.put("reservationNo", reservation.getReservationNo());
        map.put("date", reservation.getDate());
        map.put("startTime", reservation.getStartTime());
        map.put("endTime", reservation.getEndTime());
        map.put("status", reservation.getStatus());

        if (reservation.getRoomId() != null) {
            Room room = roomService.getById(reservation.getRoomId());
            if (room != null) {
                map.put("roomName", room.getRoomName());
                map.put("campus", room.getCampus());
                map.put("building", room.getBuilding());
                map.put("roomLabel", buildRoomLabel(room));
            }
        }

        if (reservation.getSeatId() != null) {
            Seat seat = seatService.getById(reservation.getSeatId());
            if (seat != null) map.put("seatNo", seat.getSeatNo());
        }
        return map;
    }

    private List<AnnouncementVO> loadAnnouncements(Integer roleId, int limit) {
        int safeLimit = Math.max(1, Math.min(12, limit));
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> list = announcementService.list(
                Wrappers.<Announcement>lambdaQuery()
                        .eq(Announcement::getIsPublished, 1)
                        .and(w -> w.eq(Announcement::getTargetRole, 2).or().eq(Announcement::getTargetRole, roleId))
                        .le(Announcement::getPublishTime, now)
                        .and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now))
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getPublishTime)
                        .last("limit " + safeLimit)
        );
        if (list == null || list.isEmpty()) return Collections.emptyList();
        return list.stream().map(this::toAnnouncementVO).collect(Collectors.toList());
    }

    private AnnouncementVO toAnnouncementVO(Announcement a) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setType(a.getType());
        vo.setLevel(a.getLevel());
        vo.setTargetRole(a.getTargetRole());
        vo.setTargetText(a.getTargetText());
        vo.setIsTop(a.getIsTop());
        vo.setIsPublished(a.getIsPublished());
        vo.setPublishTime(a.getPublishTime());
        vo.setExpireTime(a.getExpireTime());
        vo.setCreatedBy(a.getCreatedBy());
        vo.setCreateTime(a.getCreateTime());
        vo.setUpdateTime(a.getUpdateTime());
        String content = a.getContent() == null ? "" : a.getContent().trim();
        vo.setSummary(content.length() > 72 ? content.substring(0, 72) + "…" : content);
        String roomHint = buildRoomHint(content);
        vo.setRoomHint(roomHint);
        return vo;
    }

    private String buildRoomLabel(Room room) {
        List<String> parts = new ArrayList<>();
        if (room.getCampus() != null && !room.getCampus().isBlank()) parts.add(room.getCampus().trim());
        if (room.getBuilding() != null && !room.getBuilding().isBlank()) parts.add(room.getBuilding().trim());
        if (room.getRoomName() != null && !room.getRoomName().isBlank()) parts.add(room.getRoomName().trim());
        return String.join(" · ", parts);
    }

    private String buildRoomHint(String content) {
        if (content == null || content.isBlank()) return null;
        List<Room> rooms = roomService.list(
                Wrappers.<Room>lambdaQuery().eq(Room::getStatus, "open")
        );
        if (rooms == null || rooms.isEmpty()) return null;
        return rooms.stream()
                .filter(Objects::nonNull)
                .map(Room::getRoomName)
                .filter(Objects::nonNull)
                .filter(name -> content.contains(name))
                .findFirst()
                .orElse(null);
    }
}