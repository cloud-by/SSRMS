<template>
  <div class="main">
    <!-- 首页 -->
    <div v-if="currentPage === 'home'" class="home-wrapper">
      <div class="card home-card">
        <!-- 顶部：欢迎 + 右上角天气小卡片 -->
        <div class="home-top home-head">
          <div class="home-intro">
            <h2 class="page-title">{{ timeGreeting }}，欢迎使用自习室预约系统（学生端）</h2>
            <p class="page-subtitle">
              今天是 {{ todayStr }} · {{ weekDayStr }}。你可以在这里查看自习室开放情况、预约座位、查看自己的预约记录以及违规状态等。
            </p>
          </div>

          <!-- 右上角天气迷你卡片 -->
          <div v-if="weatherData" class="weather-mini">
            <div class="weather-main">
              <span class="weather-icon">{{ emojiWeather(weatherData.desc) }}</span>
              <span class="weather-temp">{{ weatherData.temp }}°C</span>
            </div>
            <div class="weather-desc">
              {{ weatherData.city }} · {{ weatherData.desc }}
            </div>
          </div>
        </div>

        <!-- 中部布局：按行排列 -->
        <div class="home-main-grid">
          <!-- 第一行：今日提示（整行，居中） -->
          <div class="home-row">
            <div class="quote-card" v-if="dailyQuote">
              <!-- 右上角刷新按钮 -->
              <button
                  type="button"
                  class="quote-refresh-btn"
                  title="换一句"
                  @click="refreshQuote"
              >
                ↻
              </button>

              <div class="quote-header">
                <span class="quote-icon-inline">💡</span>
                <span class="quote-label">今日提示</span>
              </div>

              <div class="quote-content">
                <div class="quote-text">{{ dailyQuote }}</div>
              </div>
            </div>
          </div>

          <div class="home-row-two">
            <div class="home-panel home-overview">
              <div class="home-panel-header">
                <div class="home-panel-title">今日自习室概况</div>
              </div>
              <div class="home-panel-body home-overview-body">
                <div class="home-overview-line">
                  <div class="home-panel-number">{{ todayOverview.totalSeats }} 个座位</div>
                  <div class="home-panel-desc">
                    已预约 {{ todayOverview.reservedCount }} · 正在使用 {{ todayOverview.inUseCount }} · 剩余 {{ todayOverview.remainingCount }}
                  </div>
                </div>
              </div>
            </div>

            <div class="month-report">
              <div class="report-title">本月学习简报</div>
              <div class="report-row">
                <span>本月累计预约</span>
                <span><strong>{{ monthBrief.monthReserveCount }}</strong> 次</span>
              </div>
              <div class="report-row">
                <span>本月累计自习时长</span>
                <span><strong>{{ monthStudyHours }}</strong> 小时</span>
              </div>
              <div class="report-row">
                <span>最近一次到馆</span>
                <span>{{ monthBrief.lastVisitTime || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 第三行：公告 / 通知（整行） -->
          <div class="home-row">
            <div class="home-panel notice-panel">
              <div class="home-panel-header">
                <div class="home-panel-title">
                  公告 / 通知
                  <span v-if="noticeTotalCount" class="notice-count">（{{ noticeTotalCount }}）</span>
                </div>
                <button type="button" class="notice-more-btn" @click="openNoticeAll">查看全部</button>
              </div>

              <div v-if="noticeLoading" class="notice-loading">正在加载公告…</div>

              <div v-else-if="!homeNotices.length" class="notice-empty">
                暂无公告
              </div>

              <ul v-else class="notice-list">
                <li
                    v-for="item in homeNotices"
                    :key="item.id"
                    class="notice-item"
                    @click="openNoticeDetail(item)"
                >
                  <div class="notice-badges">
                    <div v-if="Number(item.isTop) === 1" class="notice-pill notice-top">
                      <span class="notice-icon">📌</span>
                      <span>置顶</span>
                    </div>

                    <div class="notice-pill notice-type" :class="'type-' + item.type">
                      <span class="notice-icon">{{ noticeTypeIcon(item.type) }}</span>
                      <span>{{ noticeTypeText(item.type) }}</span>
                    </div>

                    <div class="notice-pill notice-level" :class="'level-' + item.level">
                      {{ noticeLevelText(item.level) }}
                    </div>
                  </div>

                  <div class="notice-main">
                    <div class="notice-title" :title="item.title">
                      {{ item.title }}
                    </div>

                    <div v-if="item.summary" class="notice-snippet" :title="item.summary">
                      {{ item.summary }}
                    </div>

                    <div class="notice-meta">
                      <span>{{ formatNoticeTime(item.publishTime) }}</span>
                      <span class="notice-dot">·</span>
                      <span>{{ item.targetText || '全体学生' }}</span>
                      <span v-if="item.roomHint" class="notice-dot">·</span>
                      <span v-if="item.roomHint">{{ item.roomHint }}</span>
                    </div>
                  </div>

                  <div class="notice-right">
                    <span v-if="isNoticeNew(item.id, item.publishTime)" class="notice-new">NEW</span>
                    <span class="notice-arrow">›</span>
                  </div>
                </li>
              </ul>
            </div>
          </div>




          <!-- 公告列表弹窗：查看全部（弹窗） -->
          <el-dialog
              v-model="noticeAllVisible"
              title="公告中心"
              width="920px"
              align-center
              append-to-body
              :z-index="4000"
              :lock-scroll="false"
              modal-class="notice-all-modal"
              class="notice-all-dialog"
          >
            <!-- 列表视图 -->
            <div>
              <div class="notice-all-head">
                <div class="notice-all-filter">
                  <span class="filter-label">类型</span>

                  <el-select
                      v-model="noticeTypeFilter"
                      size="small"
                      placeholder="全部"
                      clearable
                      :teleported="false"
                      class="notice-type-select"
                      popper-class="notice-type-popper"
                      style="width: 180px"
                      @change="onNoticeTypeChange"
                  >
                    <el-option label="全部" value="">
                      <span class="opt-row">
                        <span class="opt-ico">🗂️</span>
                        <span class="opt-text">全部</span>
                      </span>
                    </el-option>

                    <el-option
                        v-for="opt in noticeTypeOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                    >
                      <span class="opt-row">
                        <span class="opt-ico">{{ opt.icon }}</span>
                        <span class="opt-text">{{ opt.label }}</span>
                      </span>
                    </el-option>
                  </el-select>
                </div>

                <div class="notice-all-tip">
                  共 {{ noticeTotalCount }} 条
                </div>
              </div>

              <div v-if="noticeLoading" class="notice-loading">正在加载公告…</div>

              <div v-else-if="!noticePageList.length" class="notice-empty">
                暂无公告
              </div>

              <ul v-else class="notice-list notice-list-all">
                <li
                    v-for="item in noticePageList"
                    :key="item.id"
                    class="notice-item notice-item-click"
                    @click="openNoticeDetailFromAll(item)"
                >
                  <div class="notice-badges">
                    <div v-if="Number(item.isTop) === 1" class="notice-pill notice-top">
                      <span class="notice-icon">📌</span>
                      <span>置顶</span>
                    </div>

                    <div class="notice-pill notice-type" :class="'type-' + item.type">
                      <span class="notice-icon">{{ noticeTypeIcon(item.type) }}</span>
                      <span>{{ noticeTypeText(item.type) }}</span>
                    </div>
                    <div class="notice-pill notice-level" :class="'level-' + item.level">
                      {{ noticeLevelText(item.level) }}
                    </div>
                  </div>

                  <div class="notice-main">
                    <div class="notice-title" :title="item.title">
                      {{ item.title }}
                    </div>

                    <div v-if="item.summary" class="notice-snippet" :title="item.summary">
                      {{ item.summary }}
                    </div>

                    <div class="notice-meta">
                      <span>{{ formatNoticeTime(item.publishTime, true) }}</span>
                      <span class="notice-dot">·</span>
                      <span>{{ item.targetText || '全体学生' }}</span>
                    </div>
                  </div>

                  <div class="notice-right">
                    <span v-if="isNoticeNew(item.id)" class="notice-new">NEW</span>
                    <span class="notice-arrow">›</span>
                  </div>
                </li>
              </ul>

              <div class="notice-pagination">
                <el-pagination
                    background
                    layout="prev, pager, next"
                    :page-size="noticePageSize"
                    :current-page="noticePageNum"
                    :total="noticeTotalCount"
                    @current-change="onNoticePageChange"
                />
              </div>
            </div>



          </el-dialog>

          <!-- 公告详情弹窗（小弹窗） -->
          <el-dialog
              v-model="noticeDetailVisible"
              title="公告详情"
              width="820px"
              align-center
              append-to-body
              :z-index="4010"
              :lock-scroll="false"
              modal-class="notice-detail-modal"
              class="notice-detail-dialog"
          >
            <div v-if="noticeDetail" class="notice-detail-body">
              <div class="notice-detail-actions">
                <el-button
                    v-if="noticeDetailFrom === 'list'"
                    type="primary"
                    plain
                    size="small"
                    @click="backToNoticeAllList"
                >返回公告列表</el-button>

                <el-button
                    v-else
                    type="primary"
                    plain
                    size="small"
                    @click="openNoticeAllFromDetail"
                >查看全部公告</el-button>
              </div>

              <div class="notice-detail notice-detail-card">
                <div class="nd-head">
                  <div class="nd-title-row">
                    <div class="nd-title">{{ noticeDetail.title }}</div>
                    <span v-if="Number(noticeDetail.isTop) === 1" class="nd-top">置顶</span>
                  </div>

                  <div class="nd-tags">
                    <span class="nd-tag" :class="'type-' + noticeDetail.type">
                      {{ noticeTypeIcon(noticeDetail.type) }} {{ noticeTypeText(noticeDetail.type) }}
                    </span>
                    <span class="nd-tag" :class="'level-' + noticeDetail.level">
                      {{ noticeLevelText(noticeDetail.level) }}
                    </span>
                  </div>
                </div>

                <el-divider></el-divider>

                <div class="nd-info">
                  <div class="nd-info-item">
                    <div class="k">发布时间</div>
                    <div class="v">{{ formatNoticeTime(noticeDetail.publishTime, true) }}</div>
                  </div>
                  <div class="nd-info-item">
                    <div class="k">公告类型</div>
                    <div class="v">{{ noticeTypeText(noticeDetail.type) }}</div>
                  </div>
                  <div class="nd-info-item">
                    <div class="k">重要程度</div>
                    <div class="v">{{ noticeLevelText(noticeDetail.level) }}</div>
                  </div>
                  <div class="nd-info-item">
                    <div class="k">面向对象</div>
                    <div class="v">{{ noticeDetail.targetText || '全体学生' }}</div>
                  </div>
                </div>

                <el-divider></el-divider>

                <div class="nd-content">{{ noticeDetail.content || '（无内容）' }}</div>
              </div>
            </div>
          </el-dialog>
          <!-- 第四行：我的今日预约（整行） -->
          <div class="home-row">
            <div class="home-panel my-today-card">
              <div class="home-panel-header">
                <div class="home-panel-title">我的今日预约</div>
                <div
                    class="home-panel-subtitle"
                    v-if="todayAppointments.length"
                >
                  今日共 {{ todayAppointments.length }} 场自习安排
                </div>
                <div
                    class="home-panel-subtitle"
                    v-else
                >
                  今日暂无预约，可以安排一场高效自习～
                </div>
              </div>

              <div class="home-panel-body my-today-body">
                <!-- 有预约时，展示前 3 条 -->
                <template v-if="todayAppointments.length">
                  <div
                      v-for="(item, idx) in todayAppointments.slice(0, 3)"
                      :key="idx"
                      class="today-item"
                  >
                    <div class="today-left">
                      <div class="today-time">{{ formatTimeRange(item) }}</div>
                      <div class="today-room">
                        {{ item.roomFullName || item.roomName || '自习室' }}
                      </div>
                    </div>

                    <span class="badge today-badge" :class="statusClass(item.status)">
              {{ renderStatusText(item.status) }}
            </span>
                  </div>
                </template>

                <!-- 没预约时的文案 -->
                <div
                    v-else
                    class="today-empty"
                >
                  当前还没有预约，左侧导航栏中的“座位预约”可以快速创建新的预约。
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 我要预约页面（美化版） -->
    <div v-else-if="currentPage === 'user-reserve'">
      <div class="card reserve-card">
        <!-- 顶部：标题 + 当前选择概览 -->
        <div class="reserve-header-row">
          <div class="reserve-header-left">
            <h2 class="page-title">我要预约</h2>
            <p class="page-subtitle">
              选择校区、建筑、教室、日期与时间段，并选择座位号进行预约。
            </p>
          </div>

          <!-- 右侧：当前选择概要 -->
          <div class="reserve-summary-box">
            <div class="summary-title">当前选择</div>
            <div class="summary-main">
              <div class="summary-row">
                <span class="summary-label">场地</span>
                <span class="summary-value">
              {{ currentRoomFullName || '请选择场地' }}
            </span>
              </div>
              <div class="summary-row">
                <span class="summary-label">日期</span>
                <span class="summary-value">
              {{ currentDateLabel || '请选择日期' }}
            </span>
              </div>
              <div class="summary-row">
                <span class="summary-label">时间</span>
                <span class="summary-value" v-if="selectedTimeText">
              {{ selectedTimeText }}
            </span>
                <span class="summary-value summary-empty" v-else>尚未选择</span>
              </div>

              <div class="summary-row">
                <span class="summary-label">座位</span>
                <span class="summary-value" v-if="selectedSeatNo">
              {{ selectedSeatNo }}
            </span>
                <span class="summary-value summary-empty" v-else>尚未选择</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 场地（校区/建筑/教室） + 时间（开始/结束） -->
        <div class="reserve-toolbar">
          <div class="reserve-control short">
            <select class="reserve-select" v-model="selectedCampus" @change="onCampusChange">
              <option value="" disabled>选择校区</option>
              <option v-for="c in campusOptions" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>

          <div class="reserve-control short">
            <select
                class="reserve-select"
                v-model="selectedBuilding"
                :disabled="!selectedCampus"
                @change="onBuildingChange"
            >
              <option value="" disabled>选择建筑</option>
              <option v-for="b in buildingOptions" :key="b" :value="b">{{ b }}</option>
            </select>
          </div>

          <div class="reserve-control short">
            <select
                class="reserve-select"
                v-model.number="selectedRoomId"
                :disabled="!selectedBuilding"
                @change="onRoomChange"
            >
              <option :value="null" disabled>选择教室</option>
              <option v-for="r in roomOptions" :key="r.id" :value="r.id">{{ r.roomName }}</option>
            </select>
          </div>

          <div class="reserve-control long">
            <select class="reserve-select" v-model.number="startHour" @change="handleStartHourChange">
              <option :value="null" disabled>开始时间</option>
              <option v-for="h in startHourOptions" :key="h" :value="h">{{ pad2(h) }}:00</option>
            </select>
          </div>

          <div class="reserve-control long">
            <select
                class="reserve-select"
                v-model.number="endHour"
                :disabled="startHour == null"
                @change="handleEndHourChange"
            >
              <option :value="null" disabled>结束时间</option>
              <option v-for="h in endHourOptions" :key="h" :value="h">{{ pad2(h) }}:00</option>
            </select>
          </div>

          <div class="reserve-warning" v-if="rangeHasConflict">
            所选时间段包含不可预约时段
          </div>
        </div>

        <!-- 日期条 -->
        <div class="date-strip">
          <button
              class="date-arrow"
              type="button"
              @click="moveDates('prev')"
              :disabled="!canMovePrev"
          >
            ‹
          </button>

          <div class="date-list">
            <button
                v-for="d in visibleDates"
                :key="d.key"
                type="button"
                class="date-item"
                :class="{ active: currentDateIndex === d.index }"
                @click="selectDate(d.index)"
            >
              <div class="date-month">{{ d.monthLabel }}</div>
              <div class="date-day-row">
                <span class="date-day-number">{{ d.day }}</span>
                <span class="date-weekday">{{ d.weekday }}</span>
              </div>
              <div class="date-extra" v-if="d.isToday">今天</div>
              <div class="date-extra" v-else-if="d.isTomorrow">明天</div>
            </button>
          </div>

          <button
              class="date-arrow"
              type="button"
              @click="moveDates('next')"
              :disabled="!canMoveNext"
          >
            ›
          </button>
        </div>

        <!-- 座位号选择区域 -->
        <div class="slot-section">
          <div class="slot-header">
            <div class="slot-title-wrap">
              <div class="slot-title">选择座位号</div>
              <div class="slot-subtitle" v-if="currentRoomSeatSummary">{{ currentRoomSeatSummary }}</div>
            </div>
            <div class="slot-legend">
          <span class="legend-item">
            <span class="legend-dot legend-available"></span>可预约
          </span>
              <span class="legend-item">
            <span class="legend-dot legend-selected"></span>已选
          </span>
              <span class="legend-item">
            <span class="legend-dot legend-disabled"></span>不可预约
          </span>
            </div>
          </div>

          <div class="slot-grid" :style="seatGridStyle">
            <button
                v-for="seat in seatList"
                :key="seat"
                type="button"
                class="slot-item"
                :class="seatClass(seat)"
                :disabled="seatState(seat) === 'disabled'"
                @click="toggleSeat(seat)"
            >
              <span class="slot-time">{{ seat }}</span>
              <span class="slot-range">座位</span>
            </button>
          </div>
        </div>

        <!-- 已选时段 + 提交区域 -->
        <div class="reserve-bottom">
          <div class="selected-tags" v-if="tempReservations.length">
            <div
                class="selected-tag"
                v-for="(item, idx) in tempReservations"
                :key="item.key"
            >
              <span class="selected-tag-text">{{ item.label }}</span>
              <button
                  type="button"
                  class="selected-tag-close"
                  @click="removeTempReservation(idx)"
              >
                ×
              </button>
            </div>
          </div>


          <div class="reserve-actions">
            <button
                type="button"
                class="link-btn"
                :disabled="!canClearAll"
                @click="clearAllSelections"
            >
              清空选择
            </button>

            <button
                type="button"
                class="primary-btn reserve-btn"
                :disabled="!canAddTempReservation"
                @click="addTempReservation"
            >
              添加
            </button>

            <button
                type="button"
                class="primary-btn reserve-btn"
                :disabled="!canSubmitReservation"
                @click="submitReservations"
            >
              确认预约
            </button>
          </div>

          <p class="hint-text">
            先点击“添加”将当前选择加入下方列表，最后点击“确认预约”提交，并跳转到“我的预约”。
          </p>
        </div>
      </div>
    </div>

    <!-- 我的预约 -->
    <div v-else-if="currentPage === 'user-reservations'">
      <div class="card card-reservations">
        <h2 class="page-title">预约与签到签退</h2>
        <p class="page-subtitle">
          管理你的预约记录，支持签到、取消与签退；也可以通过筛选快速查看待签到或违约记录。
        </p>

        <!-- 预约概览：风格对齐你给的“图片二” -->
        <div class="res-overview">
          <div class="res-credit-ring-wrap">
            <div class="res-credit-ring" :class="creditLevelClass" title="信用分越高越好，低于 60 可能被列入黑名单">
              <div class="res-credit-ring-num">{{ currentCreditScore }}</div>
              <div class="res-credit-ring-label">信用分</div>
            </div>
          </div>

          <div class="res-overview-main">
            <div class="res-overview-top">
              <div class="res-overview-title">预约概览</div>
              <span class="res-overview-badge">{{ reservationStats.total }}条</span>
            </div>

            <div class="res-stat-grid">
              <div class="res-stat-card">
                <div class="res-stat-label">待签到</div>
                <div class="res-stat-value">{{ reservationStats.pending }}</div>
              </div>
              <div class="res-stat-card">
                <div class="res-stat-label">已签到</div>
                <div class="res-stat-value">{{ reservationStats.checkedIn }}</div>
              </div>
              <div class="res-stat-card">
                <div class="res-stat-label">迟到</div>
                <div class="res-stat-value danger">{{ reservationStats.late }}</div>
              </div>
              <div class="res-stat-card">
                <div class="res-stat-label">未签到</div>
                <div class="res-stat-value danger">{{ reservationStats.noShow }}</div>
              </div>
              <div class="res-stat-card">
                <div class="res-stat-label">已取消</div>
                <div class="res-stat-value">{{ reservationStats.cancelled }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="res-overview-tip" :class="creditLevelClass">
          <span class="res-tip-prefix">提示：</span>{{ reservationHintText }}
        </div>

        <div class="res-toolbar">
          <div class="res-toolbar-left">
            <button
                type="button"
                class="res-chip"
                :class="{ active: onlyViolation }"
                @click="onlyViolation = !onlyViolation"
            >
              仅查看违约记录
            </button>
            <button
                type="button"
                class="res-chip"
                :class="{ active: onlyPending }"
                @click="onlyPending = !onlyPending"
            >
              仅查看待签到记录
            </button>
          </div>

          <div class="res-toolbar-right">
            <span class="res-display-badge">当前展示 {{ pageSize }} 条</span>
          </div>
        </div>

        <div class="table-wrapper">
          <table class="table my-res-table">
            <thead>
            <tr>
              <th class="col-no">预约编号</th>
              <th class="col-campus">校区</th>
              <th class="col-building">建筑</th>
              <th class="col-room">自习室</th>
              <th class="col-date">日期</th>
              <th class="col-time">时间段</th>
              <th class="col-seat">座位号</th>
              <th class="col-status">状态</th>
              <th class="col-actions">操作</th>
            </tr>
            </thead>

            <tbody>
            <tr v-for="item in pagedReservations" :key="item.id">
              <td class="col-no">{{ item.reservationNo }}</td>
              <td class="col-campus">{{ item.campus || '-' }}</td>
              <td class="col-building">{{ item.building || '-' }}</td>
              <td class="col-room">{{ item.roomName || '-' }}</td>
              <td class="col-date">{{ item.date }}</td>
              <td class="col-time">{{ formatTimeRange(item) }}</td>
              <td class="col-seat">{{ item.seatNo || '-' }}</td>
              <td class="col-status">
        <span class="badge" :class="statusClass(item.status)">
          {{ renderStatusText(item.status) }}
        </span>
              </td>

              <!-- 关键：去掉 text-right，别把整列推到最右 -->
              <td class="col-actions">
                <div class="actions">
                  <template v-if="item.status === 'reserved'">
                    <button class="link-btn" type="button" @click="handleCheckIn(item)">签到</button>
                    <button class="link-btn link-danger" type="button" :disabled="!canCancel(item)" @click="handleCancel(item)">取消</button>
                  </template>

                  <button
                      class="link-btn"
                      v-else-if="item.status === 'checked_in' || item.status === 'late'"
                      type="button"
                      disabled
                  >已签到</button>

                  <button
                      class="link-btn"
                      v-else-if="item.status === 'no_show'"
                      type="button"
                      disabled
                  >已过期</button>

                  <button
                      class="link-btn"
                      v-else-if="item.status === 'cancelled' || item.status === 'cancel_overdue'"
                      type="button"
                      disabled
                  >不可操作</button>
                </div>
              </td>
            </tr>

            <tr v-if="!displayReservations.length">
              <td colspan="9" style="text-align: center; color: #9ca3af; padding: 16px 0;">
                暂无预约记录
              </td>
            </tr>
            </tbody>
          </table>
        </div>
        <!-- ⭐ 分页条：只有当总数 > 15 时才出现 -->
        <div class="pager" v-if="totalPages > 1">
          <!-- 上一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': reservationPageIndex <= 1 }"
              :disabled="reservationPageIndex <= 1"
              @click="gotoPrevPage"
          >
            上一页
          </button>

          <!-- 中间页码 -->
          <div class="pager-center">
            第 {{ reservationPageIndex }} / {{ totalPages }} 页
          </div>

          <!-- 下一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': reservationPageIndex >= totalPages }"
              :disabled="reservationPageIndex >= totalPages"
              @click="gotoNextPage"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 违规记录 -->
    <div v-else-if="currentPage === 'user-violations'">
      <div class="card violation-record-card">
        <h2 class="page-title">违规记录与信用分</h2>
        <p class="page-subtitle">
          信用分从 100 开始，未签到、迟到等行为会扣分。
        </p>

        <div class="credit-panel" :class="creditLevelClass">
          <div class="credit-panel-left">
            <div class="credit-ring" :class="creditLevelClass">
              <div class="credit-ring-num">{{ currentCreditScore }}</div>
              <div class="credit-ring-label">信用分</div>
            </div>
          </div>

          <div class="credit-panel-right">
            <div class="credit-head">
              <div class="credit-head-title">当前信用状态</div>
              <span class="credit-level-pill" :class="creditLevelClass">{{ creditLevelText }}</span>
            </div>

            <div class="credit-sub">
              信用分从 100 开始，迟到、未签到等行为会扣分。
            </div>

            <div class="credit-bar">
              <div class="credit-bar-inner" :class="creditLevelClass" :style="creditBarStyle"></div>
            </div>

            <div class="credit-note" :class="creditLevelClass">
              <span class="credit-note-strong">提示：</span>
              <span>{{ creditRiskTip }}</span>
            </div>
          </div>
        </div>

        <div class="table-wrapper">
          <table class="table violation-table">
            <thead>
            <tr>
              <th class="col-no">预约编号</th>
              <th class="col-campus">校区</th>
              <th class="col-building">建筑</th>
              <th class="col-room">自习室</th>
              <th class="col-date">日期</th>
              <th class="col-time">时间段</th>
              <th class="col-seat">座位号</th>
              <th class="col-vtype">违约类型</th>
              <th class="col-penalty">扣分</th>
              <th class="col-remark">备注</th>
            </tr>
            </thead>

            <tbody>
            <tr v-if="!pagedViolations.length">
              <td colspan="10" style="text-align: center; color: #999;">
                暂无违规记录
              </td>
            </tr>

            <tr v-for="item in pagedViolations" :key="(item.reservationNo || item.reservationId || item.id) + '-' + (item.violationType || '')">
              <td class="col-no">{{ item.reservationNo || item.reservationId }}</td>
              <td class="col-campus">{{ item.campus || '-' }}</td>
              <td class="col-building">{{ item.building || '-' }}</td>
              <td class="col-room">{{ item.roomName || '-' }}</td>
              <td class="col-date">{{ item.date }}</td>
              <td class="col-time">{{ formatTimeRange(item) || '-' }}</td>
              <td class="col-seat">{{ item.seatNo || '-' }}</td>
              <td class="col-vtype">
                <span class="violation-tag" :class="violationTypeClass(item.violationType)">
                  {{ item.violationType }}
                </span>
              </td>
              <td class="col-penalty">
                <span class="penalty-num">{{ item.penaltyScore }}</span>
              </td>
              <td class="col-remark">
                <span class="remark-text" :title="item.remark">{{ item.remark }}</span>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页器 -->
        <div class="pager" v-if="violationTotalPages > 1">
          <!-- 上一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': violationPageIndex <= 1 }"
              :disabled="violationPageIndex <= 1"
              @click="gotoPrevViolationPage"
          >
            上一页
          </button>

          <div class="pager-center">
            第 {{ violationPageIndex }} / {{ violationTotalPages }} 页
          </div>

          <!-- 下一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': violationPageIndex >= violationTotalPages }"
              :disabled="violationPageIndex >= violationTotalPages"
              @click="gotoNextViolationPage"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 评价与投诉 -->
    <div v-else-if="currentPage === 'user-feedback'">
      <div class="card">
        <div class="fb-head">
          <div>
            <h2 class="page-title">评价与反馈</h2>
            <p class="page-subtitle">
              你可以提交评价、建议、投诉或申诉。提交后会进入待处理队列，管理员处理后会在“我的反馈”中回复。
            </p>
          </div>
        </div>

        <el-tabs v-model="fbActiveTab" class="fb-tabs" @tab-click="onFbTabChange">
          <el-tab-pane label="提交反馈" name="submit">
            <div class="fb-submit-layout">
              <div class="fb-form-card">
                <el-form
                    ref="feedbackFormRef"
                    :model="feedbackForm"
                    :rules="feedbackRules"
                    label-width="92px"
                >
                  <el-form-item label="反馈类型" prop="category">
                    <el-select
                        v-model="feedbackForm.category"
                        placeholder="请选择"
                        filterable
                        style="width: 260px"
                        @change="onFeedbackCategoryChange"
                        :teleported="true"
                    >
                      <el-option
                          v-for="opt in fbCategoryOptions"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="关联预约">
                    <el-select
                        v-model="feedbackForm.reservationId"
                        placeholder="可选：选择相关预约（更方便定位）"
                        filterable
                        clearable
                        style="width: 520px"
                        :teleported="true"
                    >
                      <el-option
                          v-for="r in feedbackReservationOptions"
                          :key="r.id"
                          :label="fbReservationLabel(r)"
                          :value="r.id"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item v-if="fbNeedRating" label="满意度" prop="rating">
                    <div class="fb-rate-row">
                      <el-rate
                          v-model="feedbackForm.rating"
                          :max="5"
                          allow-half
                          show-score
                      />
                      <span class="fb-rate-hint">（1~5 分，可半星）</span>
                    </div>
                  </el-form-item>

                  <el-form-item label="内容描述" prop="content">
                    <el-input
                        v-model="feedbackForm.content"
                        type="textarea"
                        :rows="6"
                        maxlength="500"
                        show-word-limit
                        placeholder="请尽量描述清楚：时间、地点、问题现象/建议点、你希望的处理方式等。"
                    />
                  </el-form-item>

                  <div class="fb-actions">
                    <el-button
                        type="primary"
                        :loading="fbSubmitting"
                        @click="submitFeedback"
                    >提交</el-button>
                    <el-button
                        :disabled="fbSubmitting"
                        @click="resetFeedbackForm"
                    >清空</el-button>
                  </div>
                </el-form>
              </div>

              <div class="fb-tip-card">
                <div class="fb-tip-title">填写小贴士</div>
                <div class="fb-tip-sub">写得越具体，处理越快～</div>
                <ul class="fb-tip-list">
                  <li>投诉/申诉：建议包含时间、地点、涉及对象与现场情况。</li>
                  <li>建议：说明你希望增加/优化的功能点，越具体越好。</li>
                  <li>评价：可以对环境、卫生、秩序等打分并补充描述。</li>
                  <li>提交后可在“我的反馈”查看处理进度与管理员回复。</li>
                </ul>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的反馈" name="mine">
            <div class="fb-mine-toolbar">
              <div class="fb-mine-left">
                <span class="fb-filter-label">状态</span>
                <el-select
                    v-model="fbStatusFilter"
                    placeholder="全部"
                    size="small"
                    class="fb-filter"
                    :teleported="true"
                    @change="onFbFilterChange"
                >
                  <el-option label="全部" value="" />
                  <el-option label="待处理" value="pending" />
                  <el-option label="处理中" value="processing" />
                  <el-option label="已处理" value="resolved" />
                </el-select>
              </div>

              <div class="fb-mine-right">
                <el-button size="small" @click="reloadMyFeedback">刷新</el-button>
              </div>
            </div>

            <div v-loading="fbListLoading">
              <div v-if="fbList.length" class="fb-list">
                <div class="fb-item" v-for="item in fbList" :key="item.id">
                  <div class="fb-item-main">
                    <div class="fb-item-top">
                      <div class="fb-item-tags">
                        <span class="fb-tag fb-tag-type">{{ fbCategoryLabel(item.category) }}</span>
                        <span class="fb-tag" :class="fbStatusClass(item.status)">{{ fbStatusLabel(item.status) }}</span>
                        <span v-if="item.rating" class="fb-tag fb-tag-plain">评分 {{ item.rating }}/5</span>
                        <span v-if="item.reservationId" class="fb-tag fb-tag-plain">关联预约 {{ item.reservationId }}</span>
                      </div>
                      <div class="fb-item-time">{{ formatFbTime(item.createTime) }}</div>
                    </div>

                    <div class="fb-item-preview">{{ fbPreview(item.content) }}</div>

                    <div v-if="item.reply" class="fb-item-reply-preview">
                      <span class="label">管理员回复：</span>
                      <span class="txt">{{ fbPreview(item.reply, 120) }}</span>
                    </div>
                  </div>

                  <div class="fb-item-actions">
                    <el-button type="primary" link @click="openFeedbackDetail(item)">详情</el-button>
                  </div>
                </div>
              </div>

              <div v-else class="empty-state">
                暂无反馈记录
              </div>
            </div>

            <div class="pager" v-if="fbTotal > fbPageSize">
              <el-pagination
                  layout="prev, pager, next"
                  :current-page="fbPageNum"
                  :page-size="fbPageSize"
                  :total="fbTotal"
                  @current-change="onFbPageChange"
              />
            </div>
          </el-tab-pane>
        </el-tabs>

        <!-- 反馈详情 -->
        <el-dialog
            title="反馈详情"
            v-model="fbDetailVisible"
            width="720px"
            align-center
            append-to-body
            :close-on-click-modal="false"
        >
          <div class="fb-detail">
            <div class="fb-detail-title">
              <span class="fb-tag fb-tag-type">{{ fbCategoryLabel(fbDetail.category) }}</span>
              <span class="fb-tag" :class="fbStatusClass(fbDetail.status)">{{ fbStatusLabel(fbDetail.status) }}</span>
              <span v-if="fbDetail.rating" class="fb-tag fb-tag-plain">评分 {{ fbDetail.rating }}/5</span>
            </div>

            <div class="fb-detail-meta">
              <div>提交时间：{{ formatFbTime(fbDetail.createTime) }}</div>
              <div v-if="fbDetail.reservationId">关联预约：{{ fbDetail.reservationId }}</div>
            </div>

            <div class="fb-detail-block">
              <div class="t">内容</div>
              <div class="c">{{ fbDetail.content || '-' }}</div>
            </div>

            <div class="fb-detail-block" v-if="fbDetail.reply">
              <div class="t">管理员回复</div>
              <div class="c">{{ fbDetail.reply }}</div>
            </div>
          </div>

          <template #footer>
            <el-button @click="fbDetailVisible=false">关闭</el-button>
          </template>
        </el-dialog>
      </div>
    </div>

    <!-- 个人中心 -->
    <div v-else-if="currentPage === 'user-profile'">
      <div class="card full-page-card">
        <!-- 顶部用户信息区域 -->
        <div class="user-header-section">
          <div class="user-info-header">
            <div class="user-info-left">
              <div class="user-profile-info">
                <!-- 这里显示后端来的姓名，没有就先叫“同学” -->
                <h2 class="user-name">
                  {{ profileForm.name || '同学' }}
                </h2>
                <span class="user-role">学生</span>
              </div>
            </div>
            <div class="header-actions">
              <!-- 预留退出登录按钮 -->
              <!-- <button class="logout-btn">退出登录</button> -->
            </div>
          </div>
        </div>

        <!-- 个人信息表单 -->
        <div class="profile-content">
          <!-- 个人信息模块 -->
          <div class="form-section">
            <h3 class="section-title">个人信息</h3>

            <div class="form-group">
              <div class="form-grid">
                <!-- 左边：姓名 -->
                <div class="form-item">
                  <label>姓名:</label>
                  <input
                      type="text"
                      v-model="profileForm.name"
                  />
                </div>
                <!-- 右边：学号（student_no） -->
                <div class="form-item">
                  <label>学号:</label>
                  <input
                      type="text"
                      v-model="profileForm.studentNo"
                  />
                </div>
                <div class="form-item">
                  <label>学院:</label>
                  <select v-model="profileForm.college">
                    <option value="">请选择学院</option>
                    <option value="计算机学院">计算机学院</option>
                    <option value="信息工程学院">信息工程学院</option>
                    <option value="商学院">商学院</option>
                    <option value="法学院">法学院</option>
                  </select>
                </div>
                <div class="form-item">
                  <label>年级与班级:</label>
                  <input
                      type="text"
                      v-model="profileForm.gradeClass"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 联系方式模块 -->
          <div class="form-section">
            <h3 class="section-title">联系方式</h3>

            <div class="form-grid">
              <div class="form-item">
                <label>手机号:</label>
                <div class="input-with-action">
                  <input
                      type="text"
                      v-model="profileForm.phone"
                  />
                </div>
              </div>
              <div class="form-item">
                <label>邮箱:</label>
                <div class="input-with-action">
                  <input
                      type="email"
                      v-model="profileForm.email"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 偏好设置模块 -->
          <div class="form-section">
            <h3 class="section-title">偏好设置</h3>

            <div class="form-grid">
              <div class="form-item">
                <label>常用校区:</label>
                <div class="radio-group">
                  <label class="radio-option">
                    <input
                        type="radio"
                        value="本部校区"
                        v-model="profileForm.commonCampus"
                    />
                    <span class="radio-label">本部校区</span>
                  </label>
                  <label class="radio-option">
                    <input
                        type="radio"
                        value="东校区"
                        v-model="profileForm.commonCampus"
                    />
                    <span class="radio-label">东校区</span>
                  </label>
                  <label class="radio-option">
                    <input
                        type="radio"
                        value="梅山校区"
                        v-model="profileForm.commonCampus"
                    />
                    <span class="radio-label">梅山校区</span>
                  </label>
                </div>
              </div>
              <div class="form-item full-width">
                <label>备注信息:</label>
                <textarea
                    placeholder="例如：偏好图书馆、晚上时段较多、不擅长高数等"
                    v-model="profileForm.profileRemark"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="profile-actions">
            <button
                class="primary-btn"
                type="button"
                @click="handleProfileSave"
                :disabled="profileSaving"
            >
              {{ profileSaving ? '保存中…' : '保存信息' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <AiAssistantLite :user-id="currentUserId" />

  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AiAssistantLite from '../../components/AiAssistantLite.vue'

export default {
  name: 'AppHome',
  components: { AiAssistantLite },
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],

  beforeUnmount () {
    this.stopQuoteTimer()
  },

  data () {
    return {
      /* ---------------------------
       * 预约页：校区/建筑/教室 + 开始/结束时间 + 座位号
       * --------------------------- */
      // 房间全量（优先从后端拉；拉不到就用前端兜底生成，便于你先把页面跑通）
      allRooms: [],

      // 下拉选项
      campusOptions: [],
      buildingOptions: [],
      roomOptions: [],

      // 当前选中
      selectedCampus: '',
      selectedBuilding: '',
      selectedRoomId: null,

      // 开始/结束时间（小时）
      startHour: null,
      endHour: null,

      // 座位号（按当前房间动态生成，如 01-01）
      selectedSeatNo: '',
      disabledSeatNos: [],

      // 临时预约列表（可添加多条，最后统一提交）
      tempReservations: [],
      submittingReservations: false,

      // 日期相关
      dateList: [],
      visibleStart: 0,
      visibleCount: 7,
      currentDateIndex: 0,

      // 后端返回的“已满时段”（id 列表，例如 [8,9,14]）
      disabledSlotIds: [],

      // 当前登录用户 id
      currentUserId: null,

      // 我的预约列表
      myReservations: [],

      // 我的预约分页
      pageSize: 13,
      reservationPageIndex: 1,

      // 违规记录相关
      myViolations: [],
      violationPageIndex: 1,
      violationPageSize: 13,

      // 个人中心表单数据（模板里用的是这些字段名，所以这里不改字段名）
      profileForm: {
        name: '',
        account: '',
        studentNo: '',
        college: '',
        gradeClass: '',
        phone: '',
        email: '',
        commonCampus: '',
        profileRemark: ''
      },
      profileLoading: false,
      profileSaving: false,

      // 首页 - 天气、金句、快捷反馈
      weatherData: null,
      dailyQuote: '',
      dailyQuoteId: null,
      quoteTimer: null,
      openFeedback: false,

      // 评价与反馈（写入 feedback 表，管理员后续可查看处理）
      fbActiveTab: 'submit',
      fbSubmitting: false,
      fbCategoryOptions: [
        { label: '环境评价', value: 'env' },
        { label: '服务评价', value: 'service' },
        { label: '建议 / 功能优化', value: 'suggestion' },
        { label: '投诉', value: 'complaint' },
        { label: '申诉（预约/违规等）', value: 'appeal' },
        { label: '其他', value: 'other' }
      ],
      feedbackForm: {
        category: 'env',
        reservationId: '',
        rating: 5,
        content: ''
      },
      feedbackRules: {
        category: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
        rating: [{ required: true, message: '请给出评分', trigger: 'change' }],
        content: [
          { required: true, message: '请填写内容', trigger: 'blur' },
          { min: 5, message: '内容至少 5 个字', trigger: 'blur' }
        ]
      },

      fbStatusFilter: '',
      fbList: [],
      fbListLoading: false,
      fbPageNum: 1,
      fbPageSize: 6,
      fbTotal: 0,

      fbDetailVisible: false,
      fbDetail: {},

      // 首页 - 公告列表（从数据库加载）
      homeNotices: [],
      homeNoticeAllCache: [],

      // 公告枚举（与 announcement 表字段含义保持一致）
      noticeTypeOptions: [
        { label: '规则', value: 'RULE', icon: '📌' },
        { label: '调整', value: 'ADJUSTMENT', icon: '🕒' },
        { label: '突发', value: 'EMERGENCY', icon: '🚨' },
        { label: '维护', value: 'MAINTENANCE', icon: '🛠️' },
        { label: '考试', value: 'EXAM', icon: '📝' },
        { label: '其他', value: 'OTHER', icon: '📣' }
      ],

      noticeDetailVisible: false,
      noticeDetail: null,
      noticeAllVisible: false,
      noticeAllView: 'list',
      noticePageList: [],
      noticeAllList: [],
      noticeTotalCount: 0,
      noticePageNum: 1,
      noticePageSize: 6,
      noticeTypeFilter: '',
      noticeLoading: false,

      homeNoticeLimit: 3,
      noticeReadIds: [],
      onlyViolation: false,
      onlyPending: false,
      currentCreditScore: 100,

      todayOverview: {
        totalSeats: 15600,
        reservedCount: 0,
        inUseCount: 0,
        remainingCount: 15600
      },
      monthBrief: {
        monthReserveCount: 0,
        studyMinutes: 0,
        lastVisitTime: null
      },

    }
  },

  computed: {
    visibleDates () {
      return this.dateList
          .slice(this.visibleStart, this.visibleStart + this.visibleCount)
          .map((d, idx) => ({
            ...d,
            index: this.visibleStart + idx
          }))
    },
    canMovePrev () {
      return this.visibleStart > 0
    },
    canMoveNext () {
      return this.visibleStart + this.visibleCount < this.dateList.length
    },
    currentRoomId () {
      return this.selectedRoomId
    },
    currentRoomInfo () {
      return this.allRooms.find(r => Number(r.id) === Number(this.selectedRoomId)) || null
    },
    currentRoomFullName () {
      const room = this.currentRoomInfo
      if (!room) return ''
      return `${room.campus} · ${room.building} ${room.roomName}`
    },
    currentDateLabel () {
      const cur = this.dateList[this.currentDateIndex]
      return cur ? cur.fullLabel : ''
    },
    currentDateStr () {
      const cur = this.dateList[this.currentDateIndex]
      return cur ? cur.fullLabel : null
    },

    // 开始时间：08~22；结束时间：09~23（且必须 > startHour）
    startHourOptions () {
      const arr = []
      for (let h = 8; h <= 22; h++) arr.push(h)
      return arr
    },
    endHourOptions () {
      if (this.startHour == null) return []
      const arr = []
      const maxEnd = Math.min(23, this.startHour + 4)   // ✅最多4小时
      for (let h = this.startHour + 1; h <= maxEnd; h++) arr.push(h)
      return arr
    },
    selectedTimeText () {
      if (this.startHour == null || this.endHour == null) return ''
      return `${this.pad2(this.startHour)}:00 - ${this.pad2(this.endHour)}:00`
    },
    selectedSlotIdsFromRange () {
      if (this.startHour == null || this.endHour == null) return []
      const ids = []
      for (let h = this.startHour; h < this.endHour; h++) ids.push(h)
      return ids
    },
    rangeHasConflict () {
      if (!this.disabledSlotIds.length) return false
      const disabled = new Set(this.disabledSlotIds.map(x => Number(x)))
      return this.selectedSlotIdsFromRange.some(id => disabled.has(Number(id)))
    },
    seatColumns () {
      return this.currentRoomInfo?.building === '图书馆' ? 8 : 6
    },
    seatList () {
      const arr = []
      const totalSeats = Number(this.currentRoomInfo?.openSeats || this.currentRoomInfo?.totalSeats || 0)
      for (let i = 1; i <= totalSeats; i++) {
        const row = Math.floor((i - 1) / this.seatColumns) + 1
        const col = ((i - 1) % this.seatColumns) + 1
        arr.push(`${String(row).padStart(2, '0')}-${String(col).padStart(2, '0')}`)
      }
      return arr
    },
    seatGridStyle () {
      return {
        gridTemplateColumns: `repeat(${this.seatColumns}, minmax(0, 1fr))`
      }
    },
    currentRoomSeatSummary () {
      if (!this.currentRoomInfo) return ''
      const seats = this.currentRoomInfo.openSeats || this.currentRoomInfo.totalSeats || 0
      const layoutLabel = this.seatColumns === 8 ? '图书馆安静区布局' : '教学楼舒适布局'
      return `${seats} 座 · ${this.seatColumns} 列排布 · ${layoutLabel}`
    },
    hasAnySelection () {
      return this.startHour != null || this.endHour != null || !!this.selectedSeatNo
    },
    selectionTagText () {
      if (!this.currentDateLabel && !this.selectedTimeText && !this.selectedSeatNo) return ''
      const parts = []
      if (this.currentRoomFullName) parts.push(this.currentRoomFullName)
      if (this.currentDateLabel) parts.push(this.currentDateLabel)
      if (this.selectedTimeText) parts.push(this.selectedTimeText)
      if (this.selectedSeatNo) parts.push(`座位 ${this.selectedSeatNo}`)
      return parts.join(' · ')
    },
    canClearAll () {
      return this.tempReservations.length > 0
          || this.startHour != null
          || this.endHour != null
          || !!this.selectedSeatNo
    },

    currentTempKey () {
      if (!this.currentRoomId || !this.currentDateStr) return ''
      if (this.startHour == null || this.endHour == null) return ''
      if (!this.selectedSeatNo) return ''
      return [
        this.currentRoomId,
        this.currentDateStr,
        this.startHour,
        this.endHour,
        this.selectedSeatNo
      ].join('|')
    },

    isDuplicateTempReservation () {
      if (!this.currentTempKey) return false
      return this.tempReservations.some(x => x.key === this.currentTempKey)
    },

    canAddTempReservation () {
      return !!this.currentRoomId
          && !!this.currentDateStr
          && this.startHour != null
          && this.endHour != null
          && !!this.selectedSeatNo
          && !this.rangeHasConflict
          && !this.isDuplicateTempReservation
          && this.tempReservations.length < 4
          && this.getCurrentUserStatus() !== 2
    },

    canSubmitReservation () {
      return this.tempReservations.length > 0 && !this.submittingReservations && this.getCurrentUserStatus() !== 2
    },

    totalPages () {
      if (!this.displayReservations.length) return 0
      return Math.ceil(this.displayReservations.length / this.pageSize)
    },
    pagedReservations () {
      if (!this.displayReservations.length) return []
      const total = this.totalPages || 1
      const page = Math.min(this.reservationPageIndex, total)
      const start = (page - 1) * this.pageSize
      const end = start + this.pageSize
      return this.displayReservations.slice(start, end)
    },

    violationTotalPages () {
      if (!this.myViolations.length) return 0
      return Math.ceil(this.myViolations.length / this.violationPageSize)
    },
    pagedViolations () {
      if (!this.myViolations.length) return []
      const total = this.violationTotalPages || 1
      const page = Math.min(this.violationPageIndex, total)
      const start = (page - 1) * this.violationPageSize
      const end = start + this.violationPageSize
      return this.myViolations.slice(start, end)
    },

    todayStr () {
      const d = new Date()
      return `${d.getMonth() + 1} 月 ${d.getDate()} 日`
    },
    weekDayStr () {
      const list = ['日', '一', '二', '三', '四', '五', '六']
      return '星期' + list[new Date().getDay()]
    },
    timeGreeting () {
      const h = new Date().getHours()
      if (h < 11) return '上午好'
      if (h < 14) return '中午好'
      if (h < 19) return '下午好'
      return '晚上好'
    },

    todayAppointments () {
      const list = Array.isArray(this.myReservations) ? this.myReservations : []
      if (!list.length) return []

      const d = new Date()
      const todayStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      return list.filter(item => item.date === todayStr)
    },

    displayReservations () {
      const list = Array.isArray(this.myReservations) ? this.myReservations.slice() : []

      const byDateTimeAsc = (a, b) => {
        const da = a.date || ''
        const db = b.date || ''
        if (da !== db) return da.localeCompare(db)

        const ta = (a.startTime || '').slice(0, 8)
        const tb = (b.startTime || '').slice(0, 8)
        if (ta !== tb) return ta.localeCompare(tb)

        return String(a.reservationNo || '').localeCompare(String(b.reservationNo || ''))
      }

      const norm = s => String(s ?? '').trim().toLowerCase()
      const isPending = (x) => norm(x.status) === 'reserved'
      const isViolation = (x) => ['late', 'no_show'].includes(norm(x.status))

      if (this.onlyViolation) {
        return list.filter(isViolation).sort(byDateTimeAsc)
      }
      if (this.onlyPending) {
        return list.filter(isPending).sort(byDateTimeAsc)
      }

      // ✅ 不勾选：待签到(reserved) 优先，其次已签到/迟到，再未签到，最后取消类
      const rank = (st) => {
        if (st === 'reserved') return 0
        if (st === 'checked_in' || st === 'late') return 1
        if (st === 'no_show') return 2
        return 3
      }

      return list.sort((a, b) => {
        const ra = rank(a.status)
        const rb = rank(b.status)
        if (ra !== rb) return ra - rb
        return byDateTimeAsc(a, b)
      })
    },

    monthStudyHours () {
      const mins = Number(this.monthBrief.studyMinutes || 0)
      const hours = mins / 60
      return Number.isInteger(hours) ? String(hours) : hours.toFixed(1)
    },

    lastVisitText () {
      return this.formatLastVisit(this.monthBrief.lastVisitTime)
    },


    // 评价与反馈：只有“环境/服务”类需要打分
    fbNeedRating () {
      const c = (this.feedbackForm && this.feedbackForm.category) ? String(this.feedbackForm.category) : ''
      return ['env', 'service'].includes(c)
    },

    // 关联预约下拉：取最近一些记录（避免下拉太长）
    feedbackReservationOptions () {
      const list = Array.isArray(this.myReservations) ? this.myReservations.slice() : []
      const key = (x) => `${x.date || ''} ${String(x.startTime || '')}`
      list.sort((a, b) => (key(a) < key(b) ? 1 : -1))
      return list.slice(0, 50)
    },
    creditLevel () {
      const s = Number(this.currentCreditScore || 0)
      if (s >= 80) return 'good'
      if (s >= 60) return 'warn'
      return 'bad'
    },
    creditLevelText () {
      if (this.creditLevel === 'good') return '良好'
      if (this.creditLevel === 'warn') return '注意'
      return '风险'
    },
    creditLevelClass () {
      return `credit-${this.creditLevel}`
    },
    creditBarStyle () {
      const s = Math.max(0, Math.min(100, Number(this.currentCreditScore || 0)))
      return { width: `${s}%` }
    },
    creditRiskTip () {
      const s = Number(this.currentCreditScore || 0)
      if (s < 60) return '信用分低于 60 可能会被列入黑名单，一段时间内无法预约。'
      if (s < 80) return '信用分低于 80 会被列入预警名单；建议保持按时签到，避免迟到或未签到。'
      return '信用分低于 60 可能会被列入黑名单；继续保持按时签到。'
    },
    // 预约概览（用于“我的预约”页顶部统计卡片）
    reservationStats () {
      const list = Array.isArray(this.myReservations) ? this.myReservations : []
      const norm = (s) => String(s ?? '').trim().toLowerCase()
      const is = (st) => (x) => norm(x.status) === st
      const inSet = (arr) => (x) => arr.includes(norm(x.status))

      const count = (fn) => list.reduce((acc, x) => acc + (fn(x) ? 1 : 0), 0)

      return {
        total: list.length,
        pending: count(is('reserved')),
        checkedIn: count(is('checked_in')),
        late: count(is('late')),
        noShow: count(is('no_show')),
        cancelled: count(inSet(['cancelled', 'cancel_overdue']))
      }
    },
    reservationHintText () {
      const s = this.reservationStats
      if (s.pending > 0) return `你有 ${s.pending} 条待签到记录，建议按时到达并签到。`
      if (s.late > 0 || s.noShow > 0) return `你有 ${s.late + s.noShow} 条违约记录，建议按时签到，避免迟到或未签到。`
      return '保持良好习惯：按时到达并签到，结束后及时签退。'
    },


  },

  created () {
    // 初始化日期（首页也要用）
    this.initDates()

    // 先读 localStorage（兼容多种字段名）
    const u0 = this.getStoredUser()
    if (u0) {
      this.currentUserId = u0.userId ?? u0.id ?? null
      this.loadNoticeReadIds()
      this.profileForm = {
        name: u0.userName || u0.name || '',
        account: u0.accountNo || u0.account || '',
        studentNo: u0.accountNo || u0.studentNo || u0.account || '',
        college: u0.college || '',
        gradeClass: u0.gradeClass || '',
        phone: u0.phone || '',
        email: u0.email || '',
        commonCampus: u0.commonCampus || '',
        profileRemark: u0.profileRemark || ''
      }

      // 再去请求后端拿最新资料覆盖（不会影响模板字段名）
      this.loadUserProfile()
    }

    // ✅ 时段占用情况在 initReserveRooms() 里根据默认房间自动拉取

    // 首页也拉一次我的预约（不弹“未签到刷新”的提醒）
    this.loadMyReservations({ refreshNoShowNotify: false })

    this.loadWeather()
    this.loadQuoteFromDb()
    this.loadHomeDashboard()
    this.loadHomeNotices()
  },

  methods: {
    /* ---------------------------
     * 统一处理 Axios 返回结构
     * --------------------------- */
    normalizeBody (res) {
      // ✅ 你的 request.js 已经 return response.data
      // 所以这里拿到的 res 就是后端 JSON（Result）
      return res
    },
    normalizeData (res) {
      const body = this.normalizeBody(res)

      // ✅ 标准 Result：{code,msg,data,total}
      if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
        return body.data
      }

      // 兜底：如果以后某些接口真返回“裸数据”
      return body
    },
    isBizOk (resOrBody) {
      const body = this.normalizeBody(resOrBody)

      // ✅ 只认 Result.code===200 为成功
      if (!body || typeof body !== 'object' || !('code' in body)) return false
      return Number(body.code) === 200
    },
    getMsg (resOrBody, fallback = '') {
      const body = this.normalizeBody(resOrBody)
      if (!body || typeof body !== 'object') return fallback
      return body.msg || fallback
    },

    getUserStorage () {
      // 优先 localStorage，其次 sessionStorage；都没有就默认 localStorage
      if (localStorage.getItem('ssrmsUser')) return localStorage
      if (sessionStorage.getItem('ssrmsUser')) return sessionStorage
      return localStorage
    },

    getStoredUser () {
      const storage = this.getUserStorage()
      const raw = storage.getItem('ssrmsUser')
      if (!raw) return null
      try {
        return JSON.parse(raw)
      } catch (e) {
        console.error('解析 ssrmsUser 失败', e)
        return null
      }
    },

    setStoredUser (userObj) {
      const storage = this.getUserStorage()
      storage.setItem('ssrmsUser', JSON.stringify(userObj))
    },

    // 约定：0=正常，1=预警，2=黑名单（只有 2 才限制预约）
    getCurrentUserStatus () {
      const u = this.getStoredUser() || {}
      const raw = (u.blacklistFlag ?? u.blacklist_flag ?? u.status ?? 0)
      const s = Number(raw)
      return Number.isFinite(s) ? s : 0
    },

    ensureCurrentUserId () {
      if (this.currentUserId) return this.currentUserId
      const u = this.getStoredUser()
      if (u) {
        this.currentUserId = u.userId ?? u.id ?? null
      }
      return this.currentUserId
    },

    emitChange (page) {
      this.$emit('change-page', page)
    },

    /* ---------------------------
     * 预约页：房间选项初始化
     * --------------------------- */
    async initReserveRooms () {
      // ✅ 重新进入也刷新一次冲突数据（否则换了日期/教室再回来可能还是旧的）
      if (this.campusOptions && this.campusOptions.length) {
        await this.fetchSlotStatus()
        return
      }

      // 1) 先尝试从后端拉房间列表（你后端 RoomController 写好后，这里会自动生效）
      const rooms = await this.loadRoomsFromBackend().catch(() => null)

      // 2) 拉不到就用前端兜底生成（顺序与我们之前的批量插入保持一致，id 从 1 开始）
      this.allRooms = Array.isArray(rooms) && rooms.length ? rooms : this.buildFallbackRooms()

      // 3) 构建下拉选项，并给一个默认值
      this.campusOptions = Array.from(new Set(this.allRooms.map(r => r.campus)))
      this.selectedCampus = this.selectedCampus || (this.campusOptions[0] || '')
      this.onCampusChange()

      // 默认时间
      if (this.startHour == null) this.startHour = 8
      if (this.endHour == null) this.endHour = 9

      // ✅ 默认值就绪后，刷新一次冲突
      await this.fetchSlotStatus()
    },

    async loadRoomsFromBackend () {
      // 兼容你后端未来可能的不同接口命名，尽量“先能跑”
      const tries = [
        { url: '/room/list' },
        { url: '/room/all' },
      ]

      for (const t of tries) {
        try {
          const res = await this.$axios.get(t.url)
          const data = this.normalizeData(res)
          if (Array.isArray(data) && data.length) return data
        } catch (e) {
          // 继续尝试下一个
        }
      }
      return null
    },

    buildFallbackRooms () {
      const campuses = ['本部校区', '东校区', '梅山校区']
      const buildings = ['图书馆', '1号教学楼', '2号教学楼', '3号教学楼']
      const rooms = []

      // 101~105, 201~205 ... 501~505
      const roomNames = []
      for (let floor = 1; floor <= 5; floor++) {
        for (let no = 1; no <= 5; no++) {
          roomNames.push(`${floor}0${no}`)
        }
      }

      let id = 1
      for (const c of campuses) {
        for (const b of buildings) {
          for (const rn of roomNames) {
            rooms.push({
              id,
              campus: c,
              building: b,
              roomName: rn,
              totalSeats: b === '图书馆' ? 64 : 48,
              openSeats: b === '图书馆' ? 64 : 48,
              status: 'open'
            })
            id++
          }
        }
      }
      return rooms
    },

    onCampusChange () {
      // 重建 buildingOptions
      const bs = this.allRooms
          .filter(r => r.campus === this.selectedCampus)
          .map(r => r.building)
      this.buildingOptions = Array.from(new Set(bs))

      if (!this.buildingOptions.includes(this.selectedBuilding)) {
        this.selectedBuilding = this.buildingOptions[0] || ''
      }

      this.onBuildingChange()
    },

    onBuildingChange () {
      // 重建 roomOptions
      const list = this.allRooms
          .filter(r => r.campus === this.selectedCampus && r.building === this.selectedBuilding)
          .map(r => ({ id: r.id, roomName: r.roomName }))

      this.roomOptions = list

      const ids = list.map(x => Number(x.id))
      if (!ids.includes(Number(this.selectedRoomId))) {
        this.selectedRoomId = list.length ? Number(list[0].id) : null
      }

      // 房间切换后刷新占用时段 & 清空座位（座位占用后端还没接，这里先清空）
      this.selectedSeatNo = ''
      this.fetchSlotStatus()
    },

    onRoomChange () {
      this.selectedSeatNo = ''
      this.fetchSlotStatus()
    },

    pad2 (n) {
      return String(n).padStart(2, '0')
    },

    handleStartHourChange () {
      if (this.startHour == null) return
      const maxEnd = Math.min(23, this.startHour + 4)

      if (this.endHour == null || this.endHour <= this.startHour) this.endHour = this.startHour + 1
      if (this.endHour > maxEnd) this.endHour = maxEnd

      this.fetchSeatConflicts()
    },
    handleEndHourChange () {
      if (this.startHour == null || this.endHour == null) return
      const maxEnd = Math.min(23, this.startHour + 4)
      if (this.endHour <= this.startHour) this.endHour = this.startHour + 1
      if (this.endHour > maxEnd) this.endHour = maxEnd

      this.fetchSeatConflicts()
    },

    initDates () {
      const today = new Date()
      const list = []
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

      for (let i = 0; i < 14; i++) {
        const d = new Date(today)
        d.setDate(d.getDate() + i + 1)

        const month = d.getMonth() + 1
        const day = d.getDate()
        const fullLabel =
            `${d.getFullYear()}-${month.toString().padStart(2, '0')}-` +
            `${day.toString().padStart(2, '0')}`

        list.push({
          key: i,
          year: d.getFullYear(),
          month,
          day,
          weekday: weekdays[d.getDay()],
          monthLabel: `${d.getFullYear()}-${month.toString().padStart(2, '0')}`,
          isToday: false,
          isTomorrow: i === 0,
          fullLabel
        })
      }

      this.dateList = list
      this.visibleStart = 0
      this.currentDateIndex = 0
    },

    moveDates (direction) {
      if (direction === 'prev' && this.canMovePrev) this.visibleStart -= 1
      else if (direction === 'next' && this.canMoveNext) this.visibleStart += 1
    },

    async selectDate (index) {
      this.currentDateIndex = index
      await this.fetchSlotStatus()
    },

    async fetchSlotStatus () {
      this.disabledSlotIds = []
      // 同步刷新座位冲突（用于置灰不可预约座位）
      await this.fetchSeatConflicts()
    },

    /* ---------------------------
     * 预约页：座位选择
     * --------------------------- */
    seatState (seat) {
      if (this.disabledSeatNos.includes(seat)) return 'disabled'
      if (this.selectedSeatNo === seat) return 'selected'
      return 'available'
    },
    seatClass (seat) {
      const state = this.seatState(seat)
      return {
        'slot-available': state === 'available',
        'slot-disabled': state === 'disabled',
        'slot-selected': state === 'selected'
      }
    },
    toggleSeat (seat) {
      // 点到冲突座位：不选择，但提示冲突区间
      if (this.disabledSeatNos.includes(seat)) { return }

      // 可选座位：正常选中，并清空提示
      this.selectedSeatNo = (this.selectedSeatNo === seat) ? '' : seat
    },

    resetSelections () {
      // 只清空当前“输入框”选择，不影响已添加的临时列表
      this.startHour = null
      this.endHour = null
      this.selectedSeatNo = ''
    },

    clearAllSelections () {
      // 清空当前选择 + 临时列表
      this.resetSelections()
      this.tempReservations = []
    },

    addTempReservation () {
      if (!this.canAddTempReservation) return

      const room = this.allRooms.find(r => Number(r.id) == Number(this.currentRoomId))
      const labelParts = []
      if (room) labelParts.push(`${room.campus} · ${room.building} ${room.roomName}`)
      if (this.currentDateStr) labelParts.push(this.currentDateStr)
      if (this.selectedTimeText) labelParts.push(this.selectedTimeText)
      labelParts.push(`座位 ${this.selectedSeatNo}`)

      const item = {
        key: this.currentTempKey,
        roomId: this.currentRoomId,
        date: this.currentDateStr,
        startHour: this.startHour,
        endHour: this.endHour,
        seatNo: this.selectedSeatNo,
        label: labelParts.join(' · ')
      }

      this.tempReservations.push(item)
      // 添加后按钮会因“重复”而自动变灰（isDuplicateTempReservation）
    },

    removeTempReservation (idx) {
      if (idx == null) return
      this.tempReservations.splice(idx, 1)
    },

    async submitReservations () {
      if (this.getCurrentUserStatus() === 2) {
        alert('你已被列入黑名单，无法预约，请联系管理员')
        return
      }
      if (!this.canSubmitReservation) return
      if (!this.ensureCurrentUserId()) {
        alert('请先登录后再预约')
        return
      }

      const first = this.tempReservations[0]

      // 后端一次提交只能处理同一 room/date/time 的多个 seatNos
      const same = this.tempReservations.every(x =>
          Number(x.roomId) === Number(first.roomId) &&
          x.date === first.date &&
          Number(x.startHour) === Number(first.startHour) &&
          Number(x.endHour) === Number(first.endHour)
      )
      if (!same) {
        alert('请确保临时列表里的预约属于同一教室/日期/时间段（可多选座位），否则请分多次提交。')
        return
      }

      const payload = {
        userId: this.currentUserId,
        roomId: first.roomId,
        date: first.date,
        startTime: `${this.pad2(first.startHour)}:00`,
        endTime: `${this.pad2(first.endHour)}:00`,
        seatNos: this.tempReservations.map(x => String(x.seatNo))
      }

      this.submittingReservations = true
      try {
        const res = await this.$axios.post('/reservation/create', payload)
        const body = this.normalizeBody(res)

        if (!this.isBizOk(body)) {
          const msg = this.getMsg(body, '预约失败')
          if (msg && msg.includes('\n')) {
            this.conflictLines = msg.split('\n').slice(0, 4)
          }
          alert(msg)
          return
        }

        alert(this.getMsg(body, '预约成功'))
        this.clearAllSelections()
        this.emitChange('user-reservations')
      } catch (e) {
        console.error(e)
        alert('预约失败，服务器异常')
      } finally {
        this.submittingReservations = false
      }
    },

    async loadMyReservations (opts = {}) {
      const {
        refreshNoShowNotify = true // 是否提示“自动标记未签到”
      } = opts

      if (!this.ensureCurrentUserId()) return

      // 进入列表前，让后端刷新一次未签到状态（可选择是否弹提醒）
      await this.refreshNoShowStatus({ notify: refreshNoShowNotify })

      try {
        const res = await this.$axios.get('/reservation/my', {
          params: { userId: this.currentUserId }
        })

        const list = Array.isArray(this.normalizeData(res)) ? this.normalizeData(res).slice() : []

        // 日期升序，同一天按开始时间升序
        list.sort((a, b) => {
          const da = a.date || ''
          const db = b.date || ''
          if (da !== db) return da.localeCompare(db)
          const ta = a.startTime || ''
          const tb = b.startTime || ''
          return ta.localeCompare(tb)
        })

        this.myReservations = list
      } catch (e) {
        console.error(e)
        alert('加载预约列表失败')
      }
    },

    formatTimeRange (item) {
      const s = (item.startTime || '').slice(0, 5)
      const e = (item.endTime || '').slice(0, 5)
      return s && e ? `${s}-${e}` : ''
    },

    renderStatusText (status) {
      switch (status) {
        case 'reserved': return '待签到'
        case 'checked_in': return '已签到'
        case 'late': return '迟到'
        case 'no_show': return '未签到'
        case 'cancelled': return '已取消'
        case 'cancel_overdue': return '逾期取消'
        default: return status || ''
      }
    },

    statusClass (status) {
      return {
        'badge-pending': status === 'reserved',
        'badge-done': status === 'checked_in',
        'badge-late': status === 'late',
        'badge-missed': status === 'no_show',
        'badge-cancelled': status === 'cancelled',
        'badge-cancel-overdue': status === 'cancel_overdue'
      }
    },

    violationTypeClass (type) {
      const t = (type || '').toString()
      if (t.includes('未签到')) return 'vt-no-show'
      if (t.includes('迟到')) return 'vt-late'
      return 'vt-other'
    },


    gotoPrevPage () {
      if (this.reservationPageIndex > 1) this.reservationPageIndex--
    },
    gotoNextPage () {
      if (this.reservationPageIndex < this.totalPages) this.reservationPageIndex++
    },

    async handleCheckIn (item) {
      try {
        const res = await this.$axios.post(`/reservation/checkin/${item.id}`)
        const body = this.normalizeBody(res)

        const ok = this.isBizOk(body)
        alert(this.getMsg(body, ok ? '签到成功' : '签到失败'))

        // ✅ 不管成功失败，都刷新一次列表（避免“后端已更新但页面不变”）
        await this.loadMyReservations({ refreshNoShowNotify: false })
      } catch (e) {
        console.error(e)
        alert('签到失败，服务器异常')
      }
    },

    async handleCancel (item) {
      if (!this.canCancel(item)) {
        alert('距离开始不足 10 分钟，无法取消，请联系管理员处理')
        return
      }

      const ok = window.confirm(`确定要取消本次预约（${item.date} ${this.formatTimeRange(item)}）吗？`)
      if (!ok) return

      try {
        const res = await this.$axios.post(`/reservation/cancel/${item.id}`)
        const body = this.normalizeBody(res)

        if (!this.isBizOk(body)) {
          alert(this.getMsg(body, '取消失败'))
          return
        }

        alert(this.getMsg(body, '取消成功'))
        await this.loadMyReservations({ refreshNoShowNotify: false })
        await this.fetchSlotStatus()
      } catch (e) {
        console.error(e)
        alert('取消失败，服务器异常')
      }
    },

    canCancel (item) {
      if (item.status !== 'reserved') return false
      if (!item.date || !item.startTime) return false

      try {
        const startStr = item.date + 'T' + (item.startTime || '').slice(0, 8)
        const start = new Date(startStr)
        if (isNaN(start.getTime())) return true

        const now = new Date()
        const diffMs = start.getTime() - now.getTime()
        const tenMinutes = 10 * 60 * 1000
        return diffMs > tenMinutes
      } catch (e) {
        console.error(e)
        return true
      }
    },

    async refreshNoShowStatus (opts = {}) {
      const { notify = true } = opts
      if (!this.ensureCurrentUserId()) return

      try {
        const res = await this.$axios.post('/reservation/refreshNoShow', null, {
          params: { userId: this.currentUserId }
        })
        const data = this.normalizeData(res)
        const updated = typeof data === 'number' ? data : 0

        if (notify && updated > 0) {
          alert(`有 ${updated} 条已过期但未签到的预约，系统已自动标记为“未签到”，请留意信用分变化。`)
        }
      } catch (e) {
        console.error(e)
      }
    },

    async loadMyViolations () {
      if (!this.ensureCurrentUserId()) return

      try {
        // 这里默认不弹“刷新未签到”的提醒，避免用户一进页面就被打断
        await this.refreshNoShowStatus({ notify: false })

        const res = await this.$axios.get('/reservation/violations', {
          params: { userId: this.currentUserId }
        })

        const list = Array.isArray(this.normalizeData(res)) ? this.normalizeData(res).slice() : []
        this.myViolations = list
        this.violationPageIndex = 1
      } catch (e) {
        console.error(e)
        alert('加载违规记录失败')
      }

      await this.loadUserProfile()

    },

    gotoPrevViolationPage () {
      if (this.violationPageIndex > 1) this.violationPageIndex--
    },
    gotoNextViolationPage () {
      if (this.violationPageIndex < this.violationTotalPages) this.violationPageIndex++
    },

    async loadUserProfile () {
      if (!this.ensureCurrentUserId()) return

      this.profileLoading = true
      try {
        const res = await this.$axios.get('/user/profile', {
          params: { userId: this.currentUserId }
        })

        const u = this.normalizeData(res) || {}
        this.currentCreditScore = Number.isFinite(Number(u.creditScore)) ? Number(u.creditScore) : 100

        // 同步本地缓存，确保‘我的预约’等页面能显示最新信用分与状态
        const stored = this.getStoredUser() || {}
        const rawStatus = (u.blacklistFlag ?? u.blacklist_flag ?? u.status ?? stored.blacklistFlag ?? stored.blacklist_flag ?? stored.status ?? 0)
        const newStatus = Number(rawStatus)
        this.setStoredUser({
          ...stored,
          creditScore: this.currentCreditScore,
          blacklistFlag: Number.isFinite(newStatus) ? newStatus : (stored.blacklistFlag ?? 0)
        })

        this.profileForm = {
          name: u.userName || u.name || this.profileForm.name || '',
          account: u.accountNo || u.account || this.profileForm.account || '',
          studentNo: u.accountNo || u.studentNo || this.profileForm.studentNo || '',
          college: u.college || '',
          gradeClass: u.gradeClass || '',
          phone: u.phone || '',
          email: u.email || '',
          commonCampus: u.commonCampus || '',
          profileRemark: u.profileRemark || ''
        }
      } catch (e) {
        console.error('请求 /user/profile 失败', e)
      } finally {
        this.profileLoading = false
      }
    },

    async handleProfileSave () {
      if (!this.ensureCurrentUserId()) {
        alert('当前用户信息缺失，请重新登录后再试')
        return
      }

      this.profileSaving = true
      try {
        // 同时带上 userName/accountNo 等字段，兼容后端不同命名
        const payload = {
          userId: this.currentUserId,
          id: this.currentUserId,

          userName: this.profileForm.name,
          name: this.profileForm.name,

          accountNo: this.profileForm.studentNo || this.profileForm.account,
          account: this.profileForm.studentNo || this.profileForm.account,
          studentNo: this.profileForm.studentNo || this.profileForm.account,

          college: this.profileForm.college,
          gradeClass: this.profileForm.gradeClass,
          phone: this.profileForm.phone,
          email: this.profileForm.email,
          commonCampus: this.profileForm.commonCampus,
          profileRemark: this.profileForm.profileRemark
        }

        const res = await this.$axios.post('/user/profile', payload)
        const body = this.normalizeBody(res)

        if (!this.isBizOk(body)) {
          alert(this.getMsg(body, '保存失败'))
          return
        }

        alert(this.getMsg(body, '保存成功'))

        // 更新 localStorage
        const storage = this.getUserStorage()
        const raw = storage.getItem('ssrmsUser')
        if (raw) {
          try {
            const user = JSON.parse(raw)
            Object.assign(user, {
              name: this.profileForm.name,
              studentNo: this.profileForm.studentNo,
              college: this.profileForm.college,
              gradeClass: this.profileForm.gradeClass,
              phone: this.profileForm.phone,
              email: this.profileForm.email,
              age: this.profileForm.age,
              sex: this.profileForm.sex,
              commonCampus: this.profileForm.commonCampus,
              profileRemark: this.profileForm.profileRemark
            })
            storage.setItem('ssrmsUser', JSON.stringify(user))
          } catch (e) {
            console.error(e)
          }
        }
      } catch (e) {
        console.error('请求 /user/profile 失败', e)
        alert('保存失败，请稍后重试')
      } finally {
        this.profileSaving = false
      }
    },

    submitFB (score) {
      console.log('用户给了一个快捷评分：', score)
      this.openFeedback = false
    },

    codeToDesc (code) {
      const map = {
        0: '晴',
        1: '多云',
        2: '多云',
        3: '阴',
        45: '有雾',
        48: '雾，地面结冰',
        51: '毛毛雨',
        53: '小雨',
        55: '中雨',
        61: '小雨',
        63: '中雨',
        65: '大雨',
        71: '小雪',
        73: '中雪',
        75: '大雪',
        95: '雷阵雨',
        96: '雷阵雨伴有冰雹'
      }
      return map[code] || '多云'
    },

    async loadWeather () {
      try {
        const resp = await fetch(
            'https://api.open-meteo.com/v1/forecast' +
            '?latitude=29.88&longitude=121.55' +
            '&current_weather=true'
        )
        const json = await resp.json()
        const cw = (json && json.current_weather) || {}
        const temp = cw.temperature
        const windSpeed = cw.windspeed
        const code = cw.weathercode
        const desc = this.codeToDesc(code)

        this.weatherData = {
          city: '宁波',
          temp: temp != null ? temp : 18,
          desc: desc || '多云',
          wind: windSpeed != null ? `风速 ${windSpeed} km/h` : '微风'
        }
      } catch (e) {
        console.warn('请求天气失败，使用兜底文案', e)
        this.weatherData = {
          city: '宁波',
          temp: 18,
          desc: '多云',
          wind: '微风'
        }
      }
    },

    async loadQuoteFromDb () {
      try {
        const res = await this.$axios.get('/quote/random', {
          params: { excludeId: this.dailyQuoteId || undefined }
        })

        const body = this.normalizeBody(res)
        const data = this.normalizeData(res)

        // 这里即使后端不包 R，也能正常工作
        if (this.isBizOk(body) && data && data.content) {
          this.dailyQuote = data.content || ''
          this.dailyQuoteId = data.id || null
          return
        }

        // 后端返回成功但 content 为空的兜底
        this.dailyQuote = '今天也要稳住节奏，先做一小步。'
        this.dailyQuoteId = null
      } catch (e) {
        // ✅ 关键：把真正失败原因打出来（404 / 405 / CORS / 500 一眼就能看见）
        const status = e?.response?.status
        const url = e?.config?.url
        const msg = e?.message
        console.warn('[quote] load failed:', { status, url, msg, resp: e?.response?.data })

        this.dailyQuote = '网络开小差了，但你的自律别掉线。'
        this.dailyQuoteId = null
      }
    },

    startQuoteTimer () {
      this.stopQuoteTimer()
      this.quoteTimer = setInterval(() => {
        if (this.currentPage === 'home') {
          this.loadQuoteFromDb()
        }
      }, 20000)
    },

    stopQuoteTimer () {
      if (this.quoteTimer) {
        clearInterval(this.quoteTimer)
        this.quoteTimer = null
      }
    },

    async refreshQuote () {
      // 手动刷新：先换一句，再把 20s 计时从现在重新开始
      await this.loadQuoteFromDb()
      this.startQuoteTimer()
    },

    emojiWeather (desc) {
      if (!desc) return '⛅'
      if (desc.includes('雨')) return '🌧️'
      if (desc.includes('云')) return '⛅'
      if (desc.includes('晴')) return '☀️'
      if (desc.includes('雪')) return '❄️'
      return '⛅'
    },

    async fetchSeatConflicts () {
      this.disabledSeatNos = []

      if (!this.currentRoomId || !this.currentDateStr || this.startHour == null || this.endHour == null) return

      const startTime = `${this.pad2(this.startHour)}:00`
      const endTime = `${this.pad2(this.endHour)}:00`

      try {
        const res = await this.$axios.get('/reservation/seatConflicts', {
          params: { roomId: this.currentRoomId, date: this.currentDateStr, startTime, endTime }
        })

        const map = this.normalizeData(res) || {}
        this.disabledSeatNos = Object.keys(map)

        // 如果当前已选座位在新时间段下变成冲突：直接清掉选择
        if (this.selectedSeatNo && this.disabledSeatNos.includes(this.selectedSeatNo)) {
          this.selectedSeatNo = ''
        }
      } catch (e) {
        console.error(e)
      }
    },

    async loadHomeDashboard () {
      try {
        if (!this.currentUserId) return
        const res = await this.$axios.get('/dashboard/home', {
          params: { userId: this.currentUserId }
        })
        const data = this.normalizeData(res) || {}
        if (data.todayOverview) this.todayOverview = data.todayOverview
        if (data.monthBrief) this.monthBrief = data.monthBrief
      } catch (e) {
        console.error('loadHomeDashboard failed:', e)
      }
    },


    async loadHomeNotices () {
      this.noticeLoading = true
      try {
        // ✅ 首页只展示 3 条，但为了实现“NEW 优先 + 时间倒序”的规则
        // 这里需要先拉取更多数据作为候选池，再在前端统一排序后截取前 3 条展示
        const res = await this.$axios.get('/announcement/home', {
          params: { roleId: 1, limit: 50 }
        })

        const list = this.normalizeData(res) || []
        const mapped = (list || []).map(x => ({
          ...x,
          summary: this.makeNoticeSummary(x.content),
          roomHint: x.roomHint || ''
        }))

        const sorted = this.sortNotices(mapped)

        // ✅ 缓存：读/未读变化后，可以从候选池里“补位”，保证首页永远优先展示 NEW
        this.homeNoticeAllCache = sorted

        this.homeNotices = sorted.slice(0, this.homeNoticeLimit || 3)

        // ✅ 数量：至少用当前拉取到的数量；打开“公告中心”后会得到更准确的总数
        this.noticeTotalCount = Math.max(Number(this.noticeTotalCount || 0), sorted.length)
      } catch (e) {
        console.error('loadHomeNotices failed:', e)
        this.homeNotices = []
        this.homeNoticeAllCache = []
      } finally {
        this.noticeLoading = false
      }
    },


    async loadNoticePage () {
      this.noticeLoading = true
      try {
        // ✅ 公告量不大：一次性拉取，前端做“NEW优先 + 时间倒序”全局排序后再分页展示
        const res = await this.$axios.get('/announcement/page', {
          params: {
            roleId: 1,
            pageNum: 1,
            pageSize: 9999,
            type: this.noticeTypeFilter || undefined
          }
        })

        const body = this.normalizeBody(res)
        let list = []
        if (body && typeof body === 'object' && 'data' in body) {
          list = body.data || []
        } else {
          list = this.normalizeData(res) || []
        }

        const mapped = (list || []).map(x => ({
          ...x,
          summary: this.makeNoticeSummary(x.content)
        }))

        this.noticeAllList = this.sortNotices(mapped)
        this.noticeTotalCount = (this.noticeAllList || []).length

        this.rebuildNoticePageList()
      } catch (e) {
        console.error('loadNoticePage failed:', e)
        this.noticeAllList = []
        this.noticePageList = []
        this.noticeTotalCount = 0
      } finally {
        this.noticeLoading = false
      }
    },

    openNoticeDetail (item) {
      this.noticeDetail = item
      this.noticeDetailFrom = 'home'
      this.noticeDetailVisible = true

      this.markNoticeRead(item && item.id)
      this.afterNoticeRead()
    },

    openNoticeAll () {
      this.noticeAllVisible = true
      this.noticePageNum = 1
      this.noticeDetail = null
      this.loadNoticePage()
    },

    openNoticeDetailFromAll (item) {
      this.noticeDetail = item
      this.noticeDetailFrom = 'list'
      this.noticeDetailVisible = true
      this.noticeAllVisible = false

      this.markNoticeRead(item && item.id)
      this.afterNoticeRead()
    },

    backToNoticeAllList () {
      this.noticeDetailVisible = false
      this.noticeAllVisible = true
      this.rebuildNoticePageList()
    },

    onNoticePageChange (page) {
      this.noticePageNum = page
      this.rebuildNoticePageList()
    },

    onNoticeTypeChange () {
      this.noticePageNum = 1
      this.loadNoticePage()
    },


    noticeTypeText (type) {
      const map = {
        RULE: '规则',
        ADJUSTMENT: '调整',
        EMERGENCY: '突发',
        MAINTENANCE: '维护',
        EXAM: '考试',
        OTHER: '其他'
      }
      return map[type] || '其他'
    },

    noticeTypeIcon (type) {
      const map = {
        RULE: '📌',
        ADJUSTMENT: '🕒',
        EMERGENCY: '🚨',
        MAINTENANCE: '🛠️',
        EXAM: '📝',
        OTHER: '📣'
      }
      return map[type] || '📣'
    },

    noticeLevelText (level) {
      const map = {
        IMPORTANT: '重要',
        WARNING: '提醒',
        INFO: '通知'
      }
      return map[level] || '通知'
    },

    makeNoticeSummary (content) {
      if (!content) return ''
      const s = String(content).replace(/\s+/g, ' ').trim()
      if (!s) return ''
      return s.length > 60 ? (s.slice(0, 60) + '…') : s
    },

    /* ---------------------------
     * 评价与反馈（feedback）
     * --------------------------- */
    fbCategoryLabel (cat) {
      const c = String(cat || '')
      const hit = (this.fbCategoryOptions || []).find(x => x.value === c)
      return hit ? hit.label : (c || '其他')
    },
    fbStatusLabel (st) {
      const s = String(st || '')
      const map = {
        pending: '待处理',
        processing: '处理中',
        resolved: '已处理'
      }
      return map[s] || (s || '待处理')
    },
    fbStatusClass (st) {
      const s = String(st || '')
      if (s === 'resolved') return 'fb-tag-ok'
      if (s === 'processing') return 'fb-tag-warn'
      return 'fb-tag-pending'
    },
    fbPreview (txt, maxLen = 160) {
      if (!txt) return ''
      const s = String(txt).replace(/\s+/g, ' ').trim()
      if (!s) return ''
      return s.length > maxLen ? (s.slice(0, maxLen) + '…') : s
    },
    fbReservationLabel (r) {
      if (!r) return ''
      const room = r.roomLabel || r.roomName || `${r.campus || ''}${r.building ? ('·' + r.building) : ''}${r.roomName ? ('·' + r.roomName) : ''}`
      const d = r.date || ''
      const st = String(r.startTime || '').slice(0, 5)
      const et = String(r.endTime || '').slice(0, 5)
      const seat = r.seatNo ? ` · 座位 ${r.seatNo}` : ''
      return `${d} ${st}-${et} · ${room}${seat}`
    },
    formatFbTime (dtStr) {
      return this.formatNoticeTime(dtStr, true)
    },

    onFeedbackCategoryChange () {
      // 非评价类不强制评分
      if (!this.fbNeedRating) {
        this.feedbackForm.rating = null
        this.$nextTick(() => {

          try {
            const ref = this.$refs.feedbackFormRef
            if (ref && ref.clearValidate) ref.clearValidate(['rating'])
          } catch (e) {
            // 忽略清理校验的异常
          }

        })
      } else if (!this.feedbackForm.rating) {
        this.feedbackForm.rating = 5
      }
    },

    resetFeedbackForm () {
      this.feedbackForm = {
        category: 'env',
        reservationId: '',
        rating: 5,
        content: ''
      }
      this.$nextTick(() => {

        try {
          const ref = this.$refs.feedbackFormRef
          if (ref && ref.clearValidate) ref.clearValidate()
        } catch (e) {
          // 忽略清理校验的异常
        }

      })
    },

    async submitFeedback () {
      const userId = this.ensureCurrentUserId()
      if (!userId) {
        ElMessage.error('未获取到用户信息，请重新登录')
        return
      }

      // 先做表单校验
      try {
        if (this.fbNeedRating) {
          await this.$refs.feedbackFormRef.validate()
        } else {
          await this.$refs.feedbackFormRef.validateField(['category', 'content'])
        }
      } catch (e) {
        // validate 会抛错，直接结束
        return
      }

      const payload = {
        userId,
        category: this.feedbackForm.category,
        rating: this.fbNeedRating ? this.feedbackForm.rating : null,
        content: this.feedbackForm.content,
        reservationId: this.feedbackForm.reservationId ? Number(this.feedbackForm.reservationId) : null
      }

      this.fbSubmitting = true
      try {
        const res = await this.$axios.post('/feedback/submit', payload)
        if (this.isBizOk(res)) {
          ElMessage.success('提交成功，感谢你的反馈！')
          this.resetFeedbackForm()
          this.fbActiveTab = 'mine'
          this.fbPageNum = 1
          await this.reloadMyFeedback()
        } else {
          ElMessage.error(this.getMsg(res, '提交失败'))
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('提交失败，请检查网络或稍后重试')
      } finally {
        this.fbSubmitting = false
      }
    },

    onFbTabChange () {
      if (this.fbActiveTab === 'mine') {
        this.fbPageNum = 1
        this.reloadMyFeedback()
      }
    },
    onFbFilterChange () {
      this.fbPageNum = 1
      this.reloadMyFeedback()
    },
    onFbPageChange (p) {
      this.fbPageNum = Number(p || 1)
      this.reloadMyFeedback()
    },
    async reloadMyFeedback () {
      const userId = this.ensureCurrentUserId()
      if (!userId) return

      this.fbListLoading = true
      try {
        const params = {
          userId,
          pageNum: this.fbPageNum,
          pageSize: this.fbPageSize
        }
        if (this.fbStatusFilter) params.status = this.fbStatusFilter

        const res = await this.$axios.get('/feedback/my-page', { params })
        if (this.isBizOk(res)) {
          const list = this.normalizeData(res)
          this.fbList = Array.isArray(list) ? list : []
          this.fbTotal = Number(res.total || 0)
        } else {
          this.fbList = []
          this.fbTotal = 0
        }
      } catch (err) {
        console.error(err)
        this.fbList = []
        this.fbTotal = 0
      } finally {
        this.fbListLoading = false
      }
    },

    openFeedbackDetail (item) {
      this.fbDetail = item ? { ...item } : {}
      this.fbDetailVisible = true
    },

    formatNoticeTime (dtStr, withTime = false) {
      if (!dtStr) return '-'
      const parts = String(dtStr).replace('T', ' ').split(' ')
      const [d, t] = parts.length >= 2 ? parts : [parts[0], '']
      const ds = d.split('-')
      if (ds.length >= 3) {
        const mmdd = `${String(ds[1]).padStart(2, '0')}-${String(ds[2]).padStart(2, '0')}`
        if (!withTime) return mmdd
        if (!t) return mmdd
        const hhmm = t.slice(0, 5)
        return `${mmdd} ${hhmm}`
      }
      return String(dtStr)
    },
    noticeReadStorageKey () {
      return `ssrms_notice_read_${this.currentUserId || 'guest'}`
    },

    loadNoticeReadIds () {
      try {
        const raw = localStorage.getItem(this.noticeReadStorageKey())
        const arr = raw ? JSON.parse(raw) : []
        this.noticeReadIds = Array.isArray(arr) ? arr.map(x => Number(x)).filter(x => !Number.isNaN(x)) : []
      } catch (e) {
        this.noticeReadIds = []
      }
    },

    saveNoticeReadIds () {
      try {
        localStorage.setItem(this.noticeReadStorageKey(), JSON.stringify(this.noticeReadIds || []))
      } catch (e) {
        // ignore
      }
    },

    isNoticeRead (id) {
      if (id === null || id === undefined) return false
      const nid = Number(id)
      if (Number.isNaN(nid)) return false
      return (this.noticeReadIds || []).includes(nid)
    },

    markNoticeRead (id) {
      if (id === null || id === undefined) return
      const nid = Number(id)
      if (Number.isNaN(nid)) return
      if (!this.isNoticeRead(nid)) {
        this.noticeReadIds = [...(this.noticeReadIds || []), nid]
        this.saveNoticeReadIds()
      }
    },

    // NEW 规则：未读即 NEW（打开详情即会标记为已读并消失）
    isNoticeNew (id) {
      return !this.isNoticeRead(id)
    },

    noticeTimeValue (dtStr) {
      if (!dtStr) return 0
      const raw = String(dtStr).trim()
      if (/^\d+$/.test(raw)) return Number(raw)

      let s = raw
      if (/^\d{2}-\d{2}\s+\d{2}:\d{2}/.test(s)) {
        const y = new Date().getFullYear()
        s = `${y}-${s}`
      }

      const normalized = s.replace('T', ' ').replace(/-/g, '/')
      const ms = Date.parse(normalized)
      return Number.isNaN(ms) ? 0 : ms
    },

    sortNotices (list) {
      const arr = Array.isArray(list) ? list.slice() : []
      arr.sort((a, b) => {
        const aTop = Number(a && a.isTop) === 1 ? 1 : 0
        const bTop = Number(b && b.isTop) === 1 ? 1 : 0
        if (aTop !== bTop) return bTop - aTop

        const aNew = this.isNoticeNew(a && a.id) ? 1 : 0
        const bNew = this.isNoticeNew(b && b.id) ? 1 : 0
        if (aNew !== bNew) return bNew - aNew

        const at = this.noticeTimeValue(a && a.publishTime)
        const bt = this.noticeTimeValue(b && b.publishTime)
        if (at !== bt) return bt - at

        const aid = Number(a && a.id) || 0
        const bid = Number(b && b.id) || 0
        return bid - aid
      })
      return arr
    },

    rebuildNoticePageList () {
      const all = Array.isArray(this.noticeAllList) ? this.noticeAllList : []
      const size = Number(this.noticePageSize) || 6
      const page = Number(this.noticePageNum) || 1
      const start = (page - 1) * size
      this.noticePageList = all.slice(start, start + size)
    },


    afterNoticeRead () {
      // ✅ 首页：优先从“候选池”重新排序并截取前 3 条，保证 NEW 补位
      if (Array.isArray(this.homeNoticeAllCache) && this.homeNoticeAllCache.length) {
        this.homeNoticeAllCache = this.sortNotices(this.homeNoticeAllCache)
        this.homeNotices = this.homeNoticeAllCache.slice(0, this.homeNoticeLimit || 3)
      } else {
        this.homeNotices = this.sortNotices(this.homeNotices).slice(0, this.homeNoticeLimit || 3)
      }

      // ✅ 公告中心：如果已加载过全部列表，也同步重排
      if (Array.isArray(this.noticeAllList) && this.noticeAllList.length) {
        this.noticeAllList = this.sortNotices(this.noticeAllList)
        this.rebuildNoticePageList()
      }
    },


    openNoticeAllFromDetail () {
      this.noticeDetailVisible = false
      this.openNoticeAll()
    },


    formatLastVisit (dtStr) {
      if (!dtStr) return '-'

      // dtStr: "yyyy-MM-dd HH:mm"
      const parts = dtStr.replace('T', ' ').split(' ')
      if (parts.length < 2) return dtStr

      const [d, t] = parts
      const [y, m, day] = d.split('-').map(n => parseInt(n, 10))
      const [hh, mm] = t.split(':').map(n => parseInt(n, 10))

      const dt = new Date(y, (m || 1) - 1, day || 1, hh || 0, mm || 0)
      const now = new Date()

      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const startOfDt = new Date(dt.getFullYear(), dt.getMonth(), dt.getDate())
      const diffDays = Math.round((startOfToday - startOfDt) / (24 * 3600 * 1000))

      const timeText = `${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`

      if (diffDays === 0) return `今天 ${timeText}`
      if (diffDays === 1) return `昨天 ${timeText}`
      return `${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')} ${timeText}`
    },
  },

  watch: {
    currentPage: {
      immediate: true,
      handler (newVal) {
        if (newVal === 'home') {
          this.startQuoteTimer()
          this.loadHomeNotices()
        } else {
          this.stopQuoteTimer()
        }

        if (newVal === 'user-reservations') {
          this.loadMyReservations({ refreshNoShowNotify: true })
          // 让‘我的预约’页也能拿到最新信用分与状态
          this.loadUserProfile()
        }
        if (newVal === 'user-reserve') this.initReserveRooms()
        if (newVal === 'user-violations') this.loadMyViolations()
        if (newVal === 'user-profile') this.loadUserProfile()
        if (newVal === 'user-feedback') this.reloadMyFeedback()
      }
    },
    myReservations () {
      this.reservationPageIndex = 1
    },
    onlyViolation (v) {
      this.reservationPageIndex = 1
      if (v) this.onlyPending = false
    },
    onlyPending (v) {
      this.reservationPageIndex = 1
      if (v) this.onlyViolation = false
    }
  }
}
</script>

<style scoped>
.main {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* 通用卡片样式 */
.card {
  background-color: #ffffff;
  padding: 22px 24px;
  margin-bottom: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  border: 1px solid #e5e7eb;
}

.page-title {
  font-size: 24px;
  margin: 0 0 10px 0;
  color: #111827;
}

.page-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin-bottom: 16px;
}

/* 公共表格样式 */

.table-wrapper {
  margin-top: 12px;
  border-radius: 10px;
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid #e5e7eb;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table th,
.table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.table th {
  background-color: #f3f4f6;
  text-align: left;
  font-weight: 600;
  color: #374151;
}

/* 违规记录表格（v3：编号不改字体；全部单行省略号；压缩前面间距给备注留空间） */
.violation-record-card .violation-table {
  width: 100%;
  table-layout: fixed;
  --vio-col-pad-x: 10px; /* 更紧凑的列间距 */
}

.violation-record-card .violation-table th,
.violation-record-card .violation-table td {
  padding: 10px var(--vio-col-pad-x);
  vertical-align: middle;

  /* 单行显示：超出省略号（包含备注） */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 列宽（百分比，总和 100%，给“备注”更多空间） */
.violation-record-card .violation-table th.col-no,
.violation-record-card .violation-table td.col-no { width: 20%; }

.violation-record-card .violation-table th.col-campus,
.violation-record-card .violation-table td.col-campus { width: 7%; }

.violation-record-card .violation-table th.col-building,
.violation-record-card .violation-table td.col-building { width: 10%; }

.violation-record-card .violation-table th.col-room,
.violation-record-card .violation-table td.col-room { width: 6%; }

.violation-record-card .violation-table th.col-date,
.violation-record-card .violation-table td.col-date { width: 9%; }

.violation-record-card .violation-table th.col-time,
.violation-record-card .violation-table td.col-time { width: 10%; }

.violation-record-card .violation-table th.col-seat,
.violation-record-card .violation-table td.col-seat { width: 6%; text-align: center; }

.violation-record-card .violation-table th.col-vtype,
.violation-record-card .violation-table td.col-vtype { width: 8%; text-align: center; }

.violation-record-card .violation-table th.col-penalty,
.violation-record-card .violation-table td.col-penalty { width: 5%; text-align: center; }

.violation-record-card .violation-table th.col-remark,
.violation-record-card .violation-table td.col-remark { width: 19%; }

.violation-record-card .violation-table th.col-remark,
.violation-record-card .violation-table td.col-remark {
  padding-left: calc(var(--vio-col-pad-x) + 10px); /* 扣分→备注 之间更松一点 */
}

.violation-record-card .violation-table th.col-penalty,
.violation-record-card .violation-table td.col-penalty {
  padding-right: calc(var(--vio-col-pad-x) + 6px);
}

/* 备注文字也严格一行（防止 span 自己换行） */
.violation-record-card .violation-table td.col-remark .remark-text {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.35;
}

/* 细节：斑马纹 + hover */
.violation-record-card .violation-table tbody tr:nth-child(even) td {
  background: #fafafa;
}
.violation-record-card .violation-table tbody tr:hover td {
  background: #f5f7ff;
}

.table tr:nth-child(even) td {
  background-color: #f9fafb;
}

.text-right {
  text-align: right;
}

.link-danger {
  color: #dc2626;
}

/* 信用分 */

.credit-summary {
  margin-top: 6px;
  margin-bottom: 10px;
}

.credit-score {
  font-size: 15px;
  color: #111827;
}

.score-number {
  font-size: 20px;
  font-weight: 600;
  color: #16a34a;
}

/* 评价与反馈 */

.fb-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.fb-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.fb-submit-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
  align-items: start;
}

@media (max-width: 1080px) {
  .fb-submit-layout {
    grid-template-columns: 1fr;
  }
}

.fb-form-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 18px 18px 16px;
  background: #fff;
}

.fb-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-start;
  padding-left: 92px;
  margin-top: 6px;
}

.fb-rate-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fb-rate-hint {
  color: #6b7280;
  font-size: 13px;
}

.fb-tip-card {
  border: 1px dashed rgba(99, 102, 241, 0.45);
  background: rgba(99, 102, 241, 0.06);
  border-radius: 12px;
  padding: 16px;
}

.fb-tip-title {
  font-weight: 700;
  color: #111827;
  margin-bottom: 2px;
}

.fb-tip-sub {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 10px;
}

.fb-tip-list {
  margin: 0;
  padding-left: 18px;
  color: #374151;
  font-size: 13px;
  line-height: 1.7;
}

.fb-mine-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.fb-filter-label {
  color: #6b7280;
  font-size: 13px;
  margin-right: 8px;
}

.fb-filter {
  width: 140px;
}

.fb-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fb-item {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px 14px 12px;
  background: #fff;
}

.fb-item-main {
  flex: 1;
  min-width: 0;
}

.fb-item-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.fb-item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.fb-item-time {
  color: #9ca3af;
  font-size: 12px;
  white-space: nowrap;
}

.fb-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid transparent;
}

.fb-tag-type {
  background: rgba(59, 130, 246, 0.10);
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.18);
}

.fb-tag-plain {
  background: #f8fafc;
  color: #374151;
  border-color: #e5e7eb;
}

.fb-tag-pending {
  background: rgba(245, 158, 11, 0.10);
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.20);
}

.fb-tag-warn {
  background: rgba(99, 102, 241, 0.10);
  color: #4338ca;
  border-color: rgba(99, 102, 241, 0.18);
}

.fb-tag-ok {
  background: rgba(16, 185, 129, 0.10);
  color: #047857;
  border-color: rgba(16, 185, 129, 0.18);
}

.fb-item-preview {
  margin-top: 8px;
  color: #111827;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fb-item-reply-preview {
  margin-top: 8px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
  color: #374151;
  font-size: 13px;
  line-height: 1.6;
}

.fb-item-reply-preview .label {
  color: #6b7280;
  margin-right: 6px;
}

.fb-item-actions {
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.fb-detail-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.fb-detail-meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 14px;
}

.fb-detail-block {
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
  margin-top: 12px;
}

.fb-detail-block .t {
  font-weight: 700;
  color: #111827;
  margin-bottom: 6px;
}

.fb-detail-block .c {
  white-space: pre-wrap;
  color: #111827;
  line-height: 1.75;
}

.empty-state {
  padding: 26px 0;
  text-align: center;
  color: #9ca3af;
}

/* 顶部：标题 + 概要卡片 */
.reserve-header-row {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.reserve-header-left {
  flex: 1;
}

.reserve-summary-box {
  width: 260px;
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eef2ff, #f5f7ff);
  border: 1px solid #e0e7ff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  font-size: 13px;
}

.summary-title {
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.summary-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
}

.summary-label {
  color: #6b7280;
}

.summary-value {
  color: #111827;
  font-weight: 500;
  text-align: right;
}

.summary-empty {
  color: #9ca3af;
  font-weight: 400;
}

/* 场地 tabs + 开关 */
.reserve-toolbar {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}

.reserve-control {
  flex: 1;
  min-width: 140px;
}

.reserve-control.short {
  flex: 0.95;
  min-width: 140px;
}

.reserve-control.long {
  flex: 1.25;
  min-width: 170px;
}

@media (max-width: 1100px) {
  .reserve-toolbar {
    flex-wrap: wrap;
  }
  .reserve-control.short,
  .reserve-control.long {
    flex: 1;
    min-width: 160px;
  }
}

.reserve-select {
  width: 100%;
  height: 38px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background-color: #fff;
  padding: 0 12px;
  font-size: 13px;
  color: #111827;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.reserve-select:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(147, 197, 253, 0.35);
}

.reserve-select:disabled {
  background-color: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
}

.reserve-warning {
  font-size: 12px;
  color: #dc2626;
  white-space: nowrap;
}

.venue-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.venue-tab {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s ease;
}

.venue-tab:hover {
  border-color: #93c5fd;
  background-color: #eff6ff;
}

.venue-tab.active {
  border-color: #2563eb;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.25);
}

.reserve-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
}

.switch-label {
  white-space: nowrap;
}

/* 日期条 */
.date-strip {
  margin-top: 6px;
  padding: 10px 12px;
  border-radius: 14px;
  background-color: #f9fafb;
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.date-arrow {
  width: 32px;
  border-radius: 10px;
  border: none;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  font-size: 18px;
  color: #4b5563;
  cursor: pointer;
  flex-shrink: 0;
}

.date-arrow:disabled {
  opacity: 0.4;
  cursor: default;
  box-shadow: none;
}

.date-list {
  display: flex;
  gap: 8px;
  flex: 1;
}

.date-item {
  flex: 1;
  min-width: 90px;
  border-radius: 12px;
  border: 1px solid transparent;
  background-color: #fff;
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
  height: 80px;
}

.date-item:hover {
  border-color: #bfdbfe;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.date-item.active {
  border-color: #2563eb;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
}

.date-item.active .date-month,
.date-item.active .date-weekday,
.date-item.active .date-extra {
  color: #e0ecff;
}

.date-item.active .date-day-number {
  color: #fff;
}

.date-month {
  font-size: 11px;
  color: #6b7280;
}

.date-day-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 4px 0;
}

.date-day-number {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.date-weekday {
  font-size: 12px;
  color: #6b7280;
}

.date-extra {
  font-size: 11px;
  color: #2563eb;
}

/* 时间段区域 */

.slot-section {
  margin-top: 16px;
}

.slot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.slot-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.slot-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.slot-subtitle {
  font-size: 12px;
  color: #6b7280;
}

.slot-legend {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #6b7280;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.legend-available {
  background-color: #e5f3ff;
  border: 1px solid #93c5fd;
}

.legend-selected {
  background-color: #2563eb;
}

.legend-disabled {
  background-color: #e5e7eb;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.slot-item {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
  background-color: #f9fafb;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  flex-direction: column; /* 纵向排布子元素 */
  min-height: 72px;
  justify-content: center;
}

.slot-item:disabled {
  cursor: not-allowed;
}

.slot-time {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}

.slot-range {
  font-size: 10px;
  color: #6b7280;
}

/* 已选时段 + 提交区域 */

.reserve-bottom {
  margin-top: 16px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 12px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  background-color: #eff6ff;
  padding: 4px 8px 4px 10px;
  font-size: 11px;
  color: #1f2937;
}

.selected-tag-close {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 12px;
  color: #6b7280;
}

.selected-tag-close:hover {
  color: #111827;
}

.reserve-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.link-btn {
  border: none;
  background: none;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
}

.link-btn:hover {
  color: #111827;
  text-decoration: underline;
}

.link-btn:disabled {
  opacity: 0.4;
  cursor: default;
  text-decoration: none;
}

.reserve-btn {
  padding: 8px 20px;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 18px;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.3);
  transition: all 0.15s ease;
}

.primary-btn:disabled {
  opacity: 0.4;
  cursor: default;
  box-shadow: none;
}

.primary-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.38);
}

.pager {
  margin-top: 10px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: #4b5563;
}

.pager-center {
  min-width: 120px;
  text-align: center;
}

.pager-btn {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background-color: #ffffff;
  cursor: pointer;
  font-size: 13px;
  min-width: 72px;
}

.pager-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

/* 禁用状态的样式 */
.pager-btn-disabled,
.pager-btn:disabled {
  color: #9ca3af;
  border-color: #e5e7eb;
  background-color: #f9fafb;
  cursor: not-allowed;
}

/* 只调“我的预约”那一页的间距 */
.card-reservations .page-title {
  margin-bottom: 15px;   /* 标题到表格的上间距的一半来自这里 */
}

.card-reservations .table-wrapper {
  margin-top: 15px;      /* 标题到表格的另一半来自这里 */
  margin-bottom: 15px;   /* 表格到底下分页的间距的一半 */
}

.card-reservations .pager {
  margin-top: 15px;      /* 表格到底部分页的另一半 */
}

/* 外层卡片，让个人中心撑满右侧高度 */
.full-page-card {
  padding: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  /* 一般内容不多，也不需要滚动条了，可以改成 auto 或干脆删掉 */
  overflow-y: auto;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 顶部用户信息区域 */
.user-header-section {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  padding: 24px 32px;
  color: #ffffff;
  border-radius: 12px 12px 0 0;
}

.user-info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.user-info-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-profile-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: #ffffff;
}

.user-role {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 右上角操作（预留“退出登录”按钮之类） */
.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

/* 主内容区域 */
.profile-content {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #ffffff;
}

/* 表单模块（个人信息 / 联系方式 / 偏好设置 / 备注） */
.form-section {
  background: #ffffff;
  border-radius: 8px;
  padding: 0;
  border-bottom: none;
}

.profile-content .section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
}

.form-group {
  margin-top: 8px;
}

/* 两列栅格布局 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

/* 单个表单项 */
.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 文本输入框/下拉/文本域的通用样式，排除掉 radio */
.form-item input:not([type="radio"]),
.form-item select,
.form-item textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  color: #111827;
  background: #ffffff;
  transition: all 0.2s ease;
  outline: none;
  box-sizing: border-box;
}

/* 文本输入框获得焦点时的高亮（不作用于 radio） */
.form-item input:not([type="radio"]):focus,
.form-item select:focus,
.form-item textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-item input:read-only {
  background-color: #f9fafb;
  border-color: #e5e7eb;
  color: #6b7280;
  cursor: not-allowed;
}

/* 备注文本域占满一整行 */
.form-item.full-width {
  grid-column: 1 / -1;
}

/* 下拉框的小箭头（可以删掉这一段，也不影响功能） */
.form-item select {
  width: 100%;
  appearance: none;
  /* 这里原本有一长串 SVG 的 background-image，用来画下拉箭头。
     你可以先不写，有需要再补；不影响表单功能。 */
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
  padding-right: 36px;
  cursor: pointer;
}

/* 输入框 + 右侧“修改”按钮 */
.input-with-action {
  display: flex;
  gap: 8px;
  align-items: center;
}

.input-with-action input {
  flex: 1;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 2px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 0;
}

.radio-option input[type="radio"] {
  width: 16px;
  height: 16px;
  accent-color: #3b82f6;
}

.radio-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.radio-option input[type="radio"]:checked + .radio-label {
  color: #3b82f6;
}

/* 备注文本域 */
.form-item textarea {
  min-height: 100px;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
  padding: 12px;
}

/* 操作按钮区域（底部的 取消 / 保存 + 提示） */
.profile-actions {
  margin-top: 4px;
  padding-top: 0;
  border-top: none;
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 只在个人中心底部使用的按钮样式 */
.profile-actions .primary-btn {
  padding: 10px 32px;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.profile-actions .primary-btn:hover {
  background: #2563eb;
}

/* 响应式适配：窄屏时压缩边距 & 按钮纵向排列 */
@media (max-width: 768px) {
  .user-header-section {
    padding: 20px;
  }

  .profile-content {
    padding: 20px;
  }

  .user-info-header {
    flex-direction: column;
    gap: 20px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .profile-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .profile-actions .hint-text {
    text-align: center;
    margin-left: 0;
    margin-top: 8px;
    min-width: 100%;
  }
}

/* 首页外层容器，拉满右侧高度 */
.home-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 首页这张大卡片要撑满高度 */
.home-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

/* 首页上半部分：欢迎文案 */
.home-top {
  display: flex;
  align-items: stretch;
  gap: 24px;
  margin-bottom: 20px;
}

.home-top.home-head {
  position: relative;
}

.home-intro {
  flex: 1.3;
  padding-right: 150px;
}

.home-tagline {
  font-size: 13px;
  color: #4b5563;
  margin-top: 6px;
}

.home-tagline strong {
  color: #2563eb;
}

/* 今日概况卡片：统一为白色卡片背景 */
.home-panel {
  border-radius: 12px;
  padding: 16px 18px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  color: #111827;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.home-overview-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.home-overview-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.home-overview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.tag {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 999px;
  background-color: #eff6ff;
  color: #1d4ed8;
}

.tag-gray {
  background-color: #f3f4f6;
  color: #4b5563;
}

.tag-blue {
  background-color: #eff6ff;
  color: #2563eb;
}

.my-today-card .home-panel-body {
  padding-top: 8px;
}

.my-today-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.today-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.today-item:last-child {
  border-bottom: none;
}

.today-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.today-time {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}

.today-room {
  font-size: 12px;
  color: #6b7280;
}

.today-empty {
  font-size: 12px;
  color: #9ca3af;
  padding: 4px 0;
}

.notice-panel {
  margin-top: 4px;
}

.notice-more-btn {
  border: none;
  background: transparent;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
  padding: 2px 4px;
}

.notice-more-btn:hover {
  text-decoration: underline;
}

.notice-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.notice-item-click {
  cursor: pointer;
}

.notice-center-detail-top {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 12px;
}

.notice-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 13px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}


.notice-count {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  margin-left: 6px;
}

.notice-loading {
  font-size: 12px;
  color: #6b7280;
  padding: 10px 2px;
}

.notice-empty {
  font-size: 12px;
  color: #9ca3af;
  padding: 10px 2px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background-color .15s ease, border-color .15s ease, transform .15s ease;
}

.notice-item:hover {
  background-color: #f9fafb;
  border-color: #e5e7eb;
  transform: translateY(-1px);
}

.notice-badges {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 82px;
}

.notice-pill {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  line-height: 1;
}

.notice-icon {
  font-size: 12px;
}

.notice-top {
  background: #111827;
  color: #ffffff;
}

.notice-detail-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.notice-snippet {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-dot {
  margin: 0 6px;
  color: #d1d5db;
}

.notice-right {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 6px;
}

.notice-new {
  font-size: 11px;
  font-weight: 700;
  color: #dc2626;
  background: #fee2e2;
  border-radius: 999px;
  padding: 2px 8px;
}

.notice-arrow {
  color: #9ca3af;
  font-size: 18px;
  line-height: 1;
}

/* 类型配色 */
.notice-type.type-RULE { background: #eff6ff; color: #1d4ed8; }
.notice-type.type-ADJUSTMENT { background: #ecfeff; color: #0e7490; }
.notice-type.type-EMERGENCY { background: #fff1f2; color: #be123c; }
.notice-type.type-MAINTENANCE { background: #f5f3ff; color: #6d28d9; }
.notice-type.type-EXAM { background: #f0fdf4; color: #15803d; }
.notice-type.type-OTHER { background: #f3f4f6; color: #374151; }

/* 级别配色 */
.notice-level.level-IMPORTANT { background: #fef3c7; color: #92400e; }
.notice-level.level-WARNING { background: #ffedd5; color: #9a3412; }
.notice-level.level-INFO { background: #e0f2fe; color: #075985; }

/* 弹窗：公告详情 */
.notice-detail .nd-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 10px;
}

.notice-detail .nd-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notice-detail .nd-top {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #111827;
  color: #ffffff;
  flex: none;
}

.notice-detail .nd-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
  margin: 6px 0 4px;
}

.notice-detail .nd-info-item .k {
  font-size: 12px;
  color: #6b7280;
}

.notice-detail .nd-info-item .v {
  font-size: 13px;
  color: #111827;
  margin-top: 2px;
}

@media (max-width: 520px) {
  .notice-detail .nd-info {
    grid-template-columns: 1fr;
  }
}

.notice-detail .nd-tags {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.notice-detail .nd-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
}

.notice-detail .nd-tag.type-RULE { background: #eff6ff; color: #1d4ed8; }
.notice-detail .nd-tag.type-ADJUSTMENT { background: #ecfeff; color: #0e7490; }
.notice-detail .nd-tag.type-EMERGENCY { background: #fff1f2; color: #be123c; }
.notice-detail .nd-tag.type-MAINTENANCE { background: #f5f3ff; color: #6d28d9; }
.notice-detail .nd-tag.type-EXAM { background: #f0fdf4; color: #15803d; }
.notice-detail .nd-tag.type-OTHER { background: #f3f4f6; color: #374151; }

.notice-detail .nd-tag.level-IMPORTANT { background: #fef3c7; color: #92400e; }
.notice-detail .nd-tag.level-WARNING { background: #ffedd5; color: #9a3412; }
.notice-detail .nd-tag.level-INFO { background: #e0f2fe; color: #075985; }

.notice-detail .nd-meta {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 14px;
}

.notice-detail .nd-content {
  font-size: 14px;
  color: #111827;
  line-height: 1.7;
  white-space: pre-line;
  padding: 10px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

/* 全部公告弹窗 */
.notice-all-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.notice-all-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 12px;
  color: #6b7280;
}

.notice-all-tip {
  font-size: 12px;
  color: #6b7280;
}

.notice-list-all {
  max-height: 420px;
}

.notice-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}


.home-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.home-panel-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

.home-panel-number {
  font-size: 26px;
  font-weight: 600;
  margin: 6px 0 2px;
}

.home-panel-desc {
  font-size: 13px;
  opacity: 0.95;
}

.home-panel-footer {
  font-size: 12px;
  opacity: 0.88;
  margin-top: 8px;
}
.home-panel-footer span {
  font-weight: 500;
}

.weather-icon{font-size: 28px}
.weather-temp{font-size: 22px;font-weight: 600;margin-left:6px}
.weather-desc{font-size: 12px;color:#9ca3af}

/* 月报 + 信用环 横向一排 */
.month-report{
  flex: 1;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 8px rgba(15,23,42,.04);
}
.report-title {
  font-size: 15px;      /* ⭐ 跟 .home-panel-title 一致 */
  font-weight: 600;
  margin-bottom: 10px;
  color: #111827;
}
.report-row{
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin: 6px 0;
}
.report-row strong{color:#2563eb}

/* 10. 快捷反馈浮层 */
.feedback-float{
  position: fixed;
  right: 24px;
  bottom: 80px;
  z-index: 999;
}
.feedback-btn{
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(37,99,235,.35);
}
.feedback-panel{
  position: absolute;
  bottom: 56px;
  right: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
  box-shadow: 0 4px 12px rgba(15,23,42,.08);
  width: 140px;
}
.feedback-title{font-size: 12px;color:#6b7280;margin-bottom: 6px;text-align: center}
.feedback-emojis{display: flex;justify-content: space-around;font-size: 20px;cursor: pointer}

.weather-mini{
  position: absolute;
  top: 12px;
  right: 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
  box-shadow: 0 2px 6px rgba(15,23,42,.06);
  width: 130px;
  z-index: 1;
}
.weather-mini .weather-main{
  margin: 0 0 2px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.weather-mini .weather-icon{ font-size: 20px; }
.weather-mini .weather-temp{ font-size: 16px; font-weight: 600; margin-left: 6px; }
.weather-mini .weather-desc{ font-size: 11px; color: #6b7280; text-align: center; white-space: nowrap; }

/* 首页中部主布局：按行排列 */
.home-main-grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 10px;
}

.home-row {
  width: 100%;
}

/* 第二行：今日自习室概况 + 本月学习简报 */
.home-row-two {
  display: grid;
  grid-template-columns: 1.4fr 2fr;
  gap: 12px;
}

/* 让第二行两块卡片同高 */
.home-row-two .home-overview,
.home-row-two .month-report {
  height: 100%;
}

/* 窄屏下：概况和月报上下堆叠 */
@media (max-width: 1024px) {
  .home-row-two {
    grid-template-columns: 1fr;
  }
}

/* 今日提示整块卡片 */
.quote-card {
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: #f9fafb;
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.quote-refresh-btn {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.12);
}

.quote-refresh-btn:hover {
  background: #eff6ff;
  color: #2563eb;
}

.quote-content {
  max-width: 640px;
}

/* 上面一行：图标 + “今日提示” */
.quote-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 6px;
}

.quote-icon-inline {
  font-size: 24px;
}

.quote-label {
  font-size: 16px;
  color: #9ca3af;
}

.quote-text {
  font-size: 17px;           /* ⭐ 比之前更大一点 */
  color: #4b5563;
  line-height: 1.6;
}

.slot-item.slot-available {
  background-color: #f9fafb;
}

.slot-item.slot-selected {
  border-color: #2563eb;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #ffffff;
}

.slot-item.slot-selected .slot-range {
  color: #e0ecff;
}

.slot-item.slot-disabled {
  background-color: #f3f4f6;
  border-color: #e5e7eb;
  color: #9ca3af;
}

.slot-item.slot-disabled .slot-range {
  color: #9ca3af;
}

.badge.badge-pending {
  background-color: #fef3c7;
  color: #92400e;
}

.badge.badge-done {
  background-color: #dcfce7;
  color: #166534;
}

.badge.badge-late {
  background-color: #ffe4e6;
  color: #9f1239;
}

.badge.badge-missed {
  background-color: #fee2e2;
  color: #991b1b;
}

.badge.badge-cancelled {
  background-color: #e5e7eb;
  color: #374151;
}

.badge.badge-cancel-overdue {
  background-color: #e0e7ff;
  color: #3730a3;
}

.badge.today-badge{
  background-color: transparent !important;
}


/* ✅ 我的预约：状态只用“文字颜色”提示，不要底色 */
.my-res-table .badge{
  background-color: transparent !important;
  border: none !important;
  padding: 0 !important;
  border-radius: 0 !important;
  font-weight: 600;
}

.my-res-table .badge.badge-pending{ color: #d97706; }        /* 待签到：偏黄 */
.my-res-table .badge.badge-done{ color: #16a34a; }           /* 已签到：绿 */
.my-res-table .badge.badge-late{ color: #dc2626; }           /* 迟到：红 */
.my-res-table .badge.badge-missed{ color: #dc2626; }         /* 未签到：红 */
.my-res-table .badge.badge-cancelled{ color: #6b7280; }      /* 已取消：灰 */
.my-res-table .badge.badge-cancel-overdue{ color: #4f46e5; } /* 逾期取消：紫 */

.home-panel-header{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:12px;
}
.home-panel-body{
  margin-top: 8px;
}

.my-res-table { table-layout: fixed; }

.my-res-table th,
.my-res-table td {
  padding: 10px 8px;
}

.my-res-table th.col-no,      .my-res-table td.col-no      { width: 140px; padding-right: 4px; }
.my-res-table th.col-campus,  .my-res-table td.col-campus  { width: 80px;  padding-left: 4px;  }
.my-res-table th.col-building,.my-res-table td.col-building{ width: 100px; }
.my-res-table th.col-room,    .my-res-table td.col-room    { width: 80px;  }
.my-res-table th.col-date,    .my-res-table td.col-date    { width: 110px; }
.my-res-table th.col-time,    .my-res-table td.col-time    { width: 100px; }
.my-res-table th.col-seat,    .my-res-table td.col-seat    { width: 70px;  text-align: center; }
.my-res-table th.col-status,  .my-res-table td.col-status  { width: 90px;  text-align: center; }
.my-res-table th.col-actions, .my-res-table td.col-actions { width: 80px; text-align: left; }

.my-res-table td.col-no,
.my-res-table td.col-building {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.my-res-table td.col-actions .actions{
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-start;
}


.res-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 6px 0 10px;
}

.res-head-row .res-filter-row {
  margin: 0;
}

.res-credit-badge {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  user-select: none;
}

.res-credit-num {
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.res-credit-text {
  font-size: 12px;
  color: #6b7280;
}

.res-credit-badge.credit-good .res-credit-num {
  color: #16a34a;
}

.res-credit-badge.credit-warn .res-credit-num {
  color: #d97706;
}

.res-credit-badge.credit-bad .res-credit-num {
  color: #dc2626;
}

.res-filter-row{
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin: 6px 0 10px;
}

.res-filter{
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #6b7280;
  user-select: none;
}

.res-filter-checkbox{
  width: 14px;
  height: 14px;
  accent-color: #2563eb;
  cursor: pointer;
}

/* 公告中心：筛选条更像“工具栏” */
.notice-all-head{
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
}

.notice-type-select :deep(.el-input__wrapper){
  border-radius: 999px;
  box-shadow: none;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.notice-type-select :deep(.el-input__wrapper.is-focus){
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(147, 197, 253, 0.35);
}

.notice-type-select :deep(.el-input__inner){
  font-size: 12px;
}

/* 下拉面板里选项：图标 + 文本排版 */
.opt-row{
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.opt-ico{ font-size: 14px; }
.opt-text{ font-size: 12px; }

:deep(.notice-type-popper){
  z-index: 5000 !important;
}

/* 下拉 popper（你给了 popper-class 才能定向美化） */
:deep(.notice-type-popper .el-select-dropdown__item){
  border-radius: 8px;
  margin: 2px 6px;
}
:deep(.notice-type-popper .el-select-dropdown__item.selected){
  font-weight: 600;
}

/* 公告中心列表：让 9 条在全屏里更舒服 */
.notice-list-all{
  max-height: calc(100vh - 260px);
}




/* 违规记录表格字体与"我的预约"一致 */
.violation-record-card .table{
  font-size: 14px;
}
.violation-record-card .table thead th{
  font-size: 14px;
}



/* ===========================
   违规记录页：信用分面板（更接近“我的预约”的风格）
   =========================== */

.credit-panel{
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 16px;
  align-items: center;
  padding: 14px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f9fafb;
  margin-top: 10px;
  margin-bottom: 14px;
}

@media (max-width: 900px){
  .credit-panel{
    grid-template-columns: 1fr;
  }
}

.credit-panel-left{
  display: flex;
  justify-content: center;
}

.credit-ring{
  width: 116px;
  height: 116px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 6px 18px rgba(17, 24, 39, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.credit-ring-num{
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.credit-ring-label{
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.credit-panel-right{
  min-width: 0;
}

.credit-head{
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.credit-head-title{
  font-weight: 800;
  color: #111827;
}

.credit-level-pill{
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #374151;
  white-space: nowrap;
}

.credit-sub{
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.credit-bar{
  margin-top: 10px;
  height: 10px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.credit-bar-inner{
  height: 100%;
  border-radius: 999px;
  background: #9ca3af;
}

.credit-note{
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px dashed rgba(107, 114, 128, 0.35);
  background: rgba(107, 114, 128, 0.06);
  color: #374151;
  font-size: 13px;
  line-height: 1.45;
}

.credit-note-strong{
  font-weight: 800;
}

/* 分档颜色（只作用于文字/条，不给你整“血压飙升”的大红底） */
.credit-good .credit-ring-num{ color: #16a34a; }
.credit-warn .credit-ring-num{ color: #d97706; }
.credit-bad  .credit-ring-num{ color: #dc2626; }

.credit-good .credit-bar-inner{ background: #16a34a; }
.credit-warn .credit-bar-inner{ background: #d97706; }
.credit-bad  .credit-bar-inner{ background: #dc2626; }

.credit-good .credit-level-pill{ border-color: rgba(22, 163, 74, 0.35); color: #16a34a; }
.credit-warn .credit-level-pill{ border-color: rgba(217, 119, 6, 0.35); color: #d97706; }
.credit-bad  .credit-level-pill{ border-color: rgba(220, 38, 38, 0.35); color: #dc2626; }

.credit-good .credit-note{ border-color: rgba(22, 163, 74, 0.35); background: rgba(22, 163, 74, 0.06); }
.credit-warn .credit-note{ border-color: rgba(217, 119, 6, 0.35); background: rgba(217, 119, 6, 0.06); }
.credit-bad  .credit-note{ border-color: rgba(220, 38, 38, 0.35); background: rgba(220, 38, 38, 0.06); }

/* ===========================
   违规记录表：列宽、间距、标签
   =========================== */

.violation-table th.col-no,       .violation-table td.col-no{ width: 250px; padding-right: 4px; }
.violation-table th.col-campus,   .violation-table td.col-campus{ width: 120px; padding-left: 4px; }
.violation-table th.col-building, .violation-table td.col-building{ width: 120px; }
.violation-table th.col-room,     .violation-table td.col-room{ width: 120px; }
.violation-table th.col-date,     .violation-table td.col-date{ width: 150px; }
.violation-table th.col-time,     .violation-table td.col-time{ width: 150px; }
.violation-table th.col-seat,     .violation-table td.col-seat{ width: 120px; text-align: center; }
.violation-table th.col-vtype,    .violation-table td.col-vtype{ width: 150px; text-align: center; }
.violation-table th.col-penalty,  .violation-table td.col-penalty{ width: 120px; text-align: center; }
.violation-table th.col-remark,   .violation-table td.col-remark{
  min-width: 250px;
  padding-right: 14px;
  padding-left: 14px;
}

.violation-table td.col-no,
.violation-table td.col-building,
.violation-table td.col-remark{
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.violation-tag{
  font-weight: 800;
}

.violation-tag.vt-late{
  color: #d97706;
}

.violation-tag.vt-no-show{
  color: #dc2626;
}

.violation-tag.vt-other{
  color: #374151;
}

.penalty-num{
  font-weight: 800;
  color: #dc2626;
}

.remark-text{
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 扣分 ↔ 备注：增加两列之间的“空隙感”（不影响其它列） */
.violation-record-card .violation-table th.col-penalty,
.violation-record-card .violation-table td.col-penalty{
  padding-right: calc(var(--vio-col-pad-x) + 14px);
}

.violation-record-card .violation-table th.col-remark,
.violation-record-card .violation-table td.col-remark{
  padding-left: calc(var(--vio-col-pad-x) + 14px);
  padding-right: calc(var(--vio-col-pad-x) + 10px);
}



/* ===========================
   “我的预约”页（图片二风格）
   =========================== */
.card-reservations .page-subtitle{
  margin: 6px 0 12px;
  color: #6b7280;
  font-size: 13px;
}

.res-overview{
  display: flex;
  gap: 16px;
  align-items: stretch;
  padding: 12px;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  background: #ffffff;
}

.res-credit-ring-wrap{
  width: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.res-credit-ring{
  width: 92px;
  height: 92px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.res-credit-ring-num{
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  color: #111827;
}

.res-credit-ring-label{
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.res-credit-ring.credit-good .res-credit-ring-num{ color: #16a34a; }
.res-credit-ring.credit-warn .res-credit-ring-num{ color: #d97706; }
.res-credit-ring.credit-bad  .res-credit-ring-num{ color: #dc2626; }

.res-overview-main{
  flex: 1;
  min-width: 0;
}

.res-overview-top{
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 10px;
}

.res-overview-title{
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.res-overview-badge{
  font-size: 12px;
  color: #6b7280;
  padding: 2px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #ffffff;
}

.res-stat-grid{
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.res-stat-card{
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.res-stat-label{
  font-size: 12px;
  color: #6b7280;
}

.res-stat-value{
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.res-stat-value.danger{
  color: #dc2626;
}

.res-overview-tip{
  margin: 10px 0 12px;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid #e5e7eb;
  background: #fff7ed;
  color: #374151;
}

.res-overview-tip.credit-good{
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.res-overview-tip.credit-warn{
  background: #fff7ed;
  border-color: #fed7aa;
}

.res-overview-tip.credit-bad{
  background: #fef2f2;
  border-color: #fecaca;
}

.res-tip-prefix{
  font-weight: 700;
  margin-right: 6px;
}

.res-toolbar{
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  background: #f9fafb;
  margin-bottom: 10px;
}

.res-toolbar-left{
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.res-chip{
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
  transition: all .15s ease;
}

.res-chip:hover{
  border-color: #cbd5e1;
}

.res-chip.active{
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.res-display-badge{
  font-size: 12px;
  color: #6b7280;
  padding: 2px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #ffffff;
}


/* ===========================
   我的预约 & 违规记录：统一排版（不改功能）
   重点：字体 / 粗细 / 间距 / 列宽
   =========================== */

.card-reservations,
.violation-record-card{
  --u-title-size: 22px;
  --u-subtitle-size: 14px;
  --u-gap: 14px;

  --u-table-font: 13px;
  --u-table-pad-y: 10px;
  --u-table-pad-x: 12px;
}

/* 标题 / 副标题（两页一致） */
.card-reservations .page-title,
.violation-record-card .page-title{
  font-size: var(--u-title-size) !important;
  font-weight: 800 !important;
  margin: 0 0 8px 0 !important;
}

.card-reservations .page-subtitle,
.violation-record-card .page-subtitle{
  font-size: var(--u-subtitle-size) !important;
  margin: 0 0 var(--u-gap) 0 !important;
}

/* 表格区块间距（两页一致） */
.card-reservations .table-wrapper,
.violation-record-card .table-wrapper{
  margin-top: var(--u-gap) !important;
  margin-bottom: var(--u-gap) !important;
}

.card-reservations .pager,
.violation-record-card .pager{
  margin-top: var(--u-gap) !important;
}

/* 表格：字号 / 行高 / 列间距（两页一致） */
.card-reservations .table,
.violation-record-card .table{
  font-size: var(--u-table-font) !important;
}

.card-reservations .table th,
.card-reservations .table td,
.violation-record-card .table th,
.violation-record-card .table td{
  padding: var(--u-table-pad-y) var(--u-table-pad-x) !important;
}

/* “我的预约”表格：覆盖之前更窄的 padding / 4px 微调，避免列距不统一 */
.my-res-table th,
.my-res-table td{
  padding: var(--u-table-pad-y) var(--u-table-pad-x) !important;
}

.my-res-table th.col-no,      .my-res-table td.col-no,
.my-res-table th.col-campus,  .my-res-table td.col-campus{
  padding-left: var(--u-table-pad-x) !important;
  padding-right: var(--u-table-pad-x) !important;
}

/* “违规记录”表格：列距更紧凑，尽量保证宽屏下可一行展示 */
.violation-record-card .violation-table{
  --vio-col-pad-x: var(--u-table-pad-x) !important;
}

/* 扣分 ↔ 备注：两列之间留更明显的间距 */
.violation-record-card .violation-table th.col-penalty,
.violation-record-card .violation-table td.col-penalty{
  padding-right: calc(var(--u-table-pad-x) + 12px) !important;
}

.violation-record-card .violation-table th.col-remark,
.violation-record-card .violation-table td.col-remark{
  padding-left: calc(var(--u-table-pad-x) + 16px) !important;
  padding-right: var(--u-table-pad-x) !important;
}
/* 覆盖：违规记录列宽（更接近“我的预约”风格） */
.violation-table th.col-no,       .violation-table td.col-no{ width: 160px !important; }
.violation-table th.col-campus,   .violation-table td.col-campus{ width: 70px !important; }
.violation-table th.col-building, .violation-table td.col-building{ width: 90px !important; }
.violation-table th.col-room,     .violation-table td.col-room{ width: 70px !important; }
.violation-table th.col-date,     .violation-table td.col-date{ width: 100px !important; }
.violation-table th.col-time,     .violation-table td.col-time{ width: 100px !important; }
.violation-table th.col-seat,     .violation-table td.col-seat{ width: 60px !important; text-align: center !important; }
.violation-table th.col-vtype,    .violation-table td.col-vtype{ width: 90px !important; text-align: center !important; }
.violation-table th.col-penalty,  .violation-table td.col-penalty{ width: 60px !important; text-align: center !important; }
.violation-table th.col-remark,   .violation-table td.col-remark{ min-width: 160px !important; }

/* 取消 col-no/col-campus 里原本的 4px 微调，让列间距彻底统一 */
.violation-table th.col-no,      .violation-table td.col-no,
.violation-table th.col-campus,  .violation-table td.col-campus{
  padding-left: var(--u-table-pad-x) !important;
  padding-right: var(--u-table-pad-x) !important;
}

</style>