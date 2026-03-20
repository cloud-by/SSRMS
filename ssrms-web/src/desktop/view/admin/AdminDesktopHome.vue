<template>
  <div class="main">
    <!-- ============ 后台首页（Dashboard 整合版） ============ -->
    <div v-if="currentPage === 'admin-home'" class="dashboard-page">
      <!-- 顶部欢迎 + 今日概览 -->
      <div class="card welcome-card">
        <div class="welcome-left">
          <div class="welcome-title-row">
            <h2 class="welcome-title">自习室预约系统 · 管理后台</h2>
            <el-tag size="small" type="success">运行正常</el-tag>
          </div>
          <p class="welcome-subtitle">
            欢迎回来，管理员。同一时间可以在这里总览预约情况、签到率和异常提醒，快速进入各个管理模块。
          </p>

          <div class="welcome-meta">
            <span class="meta-item">今天：{{ todayText }}</span>
            <span class="meta-separator">·</span>
            <span class="meta-item">最近登录 IP：{{ lastLoginIp }}</span>
          </div>
        </div>

        <div class="welcome-right">
          <div class="metric-grid">
            <div class="metric-item">
              <div class="metric-label">今日预约总数</div>
              <div class="metric-value">{{ metrics.todayReservations }}</div>
              <div class="metric-desc">较昨日 {{ metrics.todayTrend }}%</div>
            </div>

            <div class="metric-item">
              <div class="metric-label">今日签到率</div>
              <div class="metric-value">
                {{ metrics.todaySignRate }}<span class="metric-unit">%</span>
              </div>
              <el-progress
                  :percentage="metrics.todaySignRate"
                  :stroke-width="6"
                  status="success"
                  class="metric-progress"
              />
            </div>

            <div class="metric-item">
              <div class="metric-label">当前开放自习室</div>
              <div class="metric-value">{{ metrics.openRooms }}</div>
              <div class="metric-desc">覆盖 {{ metrics.totalSeats }} 个座位</div>
            </div>

            <div class="metric-item">
              <div class="metric-label">待处理事项</div>
              <div class="metric-value warning">{{ metrics.pendingItems }}</div>
              <div class="metric-desc">投诉 / 异常 / 审核 等</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间两列：趋势预览 + 滚动公告 -->
      <div class="middle-row">
        <!-- 趋势预览 -->
        <div class="card trend-card">
          <div class="card-header-row">
            <div>
              <h3 class="card-title">本周运行概览</h3>
              <p class="card-subtitle">通过简单的进度条快速感知本周整体情况。</p>
            </div>
          </div>

          <div class="trend-list">
            <div class="trend-item" v-for="item in trendItems" :key="item.key">
              <div class="trend-item-header">
                <span class="trend-item-title">{{ item.title }}</span>
                <span class="trend-item-value">{{ item.value }}</span>
              </div>
              <el-progress
                  :percentage="item.percent"
                  :stroke-width="8"
                  :status="item.status || 'success'"
                  class="trend-progress"
              />
              <div class="trend-item-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>

        <!-- 系统公告（列表 + 管理） -->
        <div class="card notice-card">
          <div class="card-header-row">
            <div>
              <h3 class="card-title">系统公告管理</h3>
              <p class="card-subtitle">
                支持查看公告列表与详情；可新增、编辑（重新发布）与删除，学生端将同步更新。
              </p>
            </div>

            <div class="admin-notice-head-actions">
              <el-button size="small" @click="openAdminNoticeAll">查看全部</el-button>
              <el-button size="small" type="primary" plain @click="openAdminNoticeCreate">发布新公告</el-button>
            </div>
          </div>

          <div v-if="adminNoticeHomeLoading" class="admin-notice-loading">正在加载公告…</div>
          <div v-else-if="!adminHomeNotices.length" class="admin-notice-empty">暂无公告</div>

          <ul v-else class="admin-notice-home-list">
            <li
                v-for="item in adminHomeNotices"
                :key="item.id"
                class="admin-notice-home-item"
                @click="openAdminNoticeDetail(item, 'home')"
            >
              <div class="admin-notice-badges">
                <div class="admin-notice-pill admin-type" :class="'type-' + item.type">
                  <span class="admin-notice-icon">{{ noticeTypeIcon(item.type) }}</span>
                  <span>{{ noticeTypeText(item.type) }}</span>
                </div>

                <div class="admin-notice-pill admin-level" :class="'level-' + item.level">
                  {{ noticeLevelText(item.level) }}
                </div>

                <div v-if="Number(item.isTop) === 1" class="admin-notice-pill admin-top">置顶</div>
              </div>

              <div class="admin-notice-main">
                <div class="admin-notice-title" :title="item.title">{{ item.title }}</div>
                <div v-if="item.summary" class="admin-notice-snippet" :title="item.summary">{{ item.summary }}</div>
                <div class="admin-notice-meta">
                  <span>{{ formatNoticeTime(item.publishTime, true) }}</span>
                  <span class="admin-dot">·</span>
                  <span>{{ item.targetText || '全部用户' }}</span>
                </div>
              </div>

              <div class="admin-notice-right"><span class="admin-notice-arrow">›</span></div>
            </li>
          </ul>
        </div>

      </div>

      <!-- 补充一行：最近预约动态（整行 + 两列展示） -->
      <div class="dashboard-row">
        <div class="card dashboard-card dashboard-card-full">
          <h3 class="card-title">最近预约动态</h3>

          <div class="activity-two-col">
            <!-- 左列 -->
            <ul class="timeline-list activity-col">
              <li v-for="item in latestActivitiesLeft" :key="item.id" class="timeline-item">
                <div class="timeline-main">
                  <span class="timeline-text">{{ item.text }}</span>
                  <span class="timeline-tag" :class="'tag-' + item.type">
              {{ item.typeLabel }}
            </span>
                </div>
                <div class="timeline-time">{{ item.time }}</div>
              </li>
            </ul>

            <!-- 中间竖线 -->
            <div v-if="latestActivitiesRight.length" class="activity-divider"></div>

            <!-- 右列 -->
            <ul v-if="latestActivitiesRight.length" class="timeline-list activity-col">
              <li v-for="item in latestActivitiesRight" :key="item.id" class="timeline-item">
                <div class="timeline-main">
                  <span class="timeline-text">{{ item.text }}</span>
                  <span class="timeline-tag" :class="'tag-' + item.type">
              {{ item.typeLabel }}
            </span>
                </div>
                <div class="timeline-time">{{ item.time }}</div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 待办事项 / 快速入口 -->
      <div class="card todo-card">
        <div class="card-header-row">
          <div>
            <h3 class="card-title">待办事项</h3>
            <p class="card-subtitle">
              这里汇总了你近期需要处理的关键事项，比如投诉、异常记录和规则配置。
            </p>
          </div>
          <el-button size="small" @click="handleMarkAllDone">
            一键标记已读
          </el-button>
        </div>

        <el-table
            ref="todoTableRef"
            :data="todos"
            size="small"
            class="todo-table"
            border
            stripe
            table-layout="fixed"
            style="width: 100%"
        >
          <el-table-column prop="type" label="类型" min-width="110">
            <template #default="scope">
              <el-tag size="small" :type="getTodoTagType(scope.row.type)">
                {{ getTodoTypeText(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="content" label="内容" min-width="260" />

          <el-table-column prop="from" label="来源" min-width="120" />

          <el-table-column prop="time" label="创建时间" min-width="150" />

          <el-table-column prop="priority" label="优先级" min-width="80">
            <template #default="scope">
              <el-tag
                  size="small"
                  :type="scope.row.priority === 'high' ? 'danger' : (scope.row.priority === 'medium' ? 'warning' : 'info')"
                  effect="plain"
              >
                {{ getPriorityText(scope.row.priority) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" min-width="200">
            <template #default="scope">
              <div class="todo-actions">
                <el-button size="small" type="primary" link @click="handleTodoGo(scope.row)">
                  前往处理
                </el-button>
                <el-button size="small" type="success" link @click="handleTodoDone(scope.row)">
                  标记完成
                </el-button>
                <el-tooltip content="标记完成仅隐藏本机，不影响后端数据" placement="top">
                  <el-button size="small" type="info" link>详情</el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 管理端：公告管理中心 -->
      <el-dialog
          v-model="adminNoticeAllVisible"
          title="公告管理中心"
          width="980px"
          align-center
          append-to-body
          :lock-scroll="false"
          :z-index="5000"
      >
        <div class="admin-notice-toolbar">
          <div class="admin-notice-filters">
            <div class="f-item">
              <span class="f-label">类型</span>
              <el-select v-model="adminNoticeTypeFilter" :teleported="false" size="small" clearable placeholder="全部" style="width: 150px">

                <el-option v-for="opt in noticeTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />

              </el-select>
            </div>

            <div class="f-item">
              <span class="f-label">重要程度</span>
              <el-select v-model="adminNoticeLevelFilter" :teleported="false" size="small" clearable placeholder="全部" style="width: 150px">

                <el-option v-for="opt in noticeLevelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />

              </el-select>
            </div>

            <div class="f-item">
              <span class="f-label">面向对象</span>
              <el-select v-model="adminNoticeTargetFilter" :teleported="false" size="small" clearable placeholder="全部" style="width: 150px">

                <el-option v-for="opt in noticeTargetOptions" :key="opt.value" :label="opt.label" :value="opt.value" />

              </el-select>
            </div>

            <div class="f-item">
              <span class="f-label">关键词</span>
              <el-input v-model="adminNoticeKeyword" size="small" placeholder="标题/内容" style="width: 220px" clearable />
            </div>

            <div class="f-item">
              <el-checkbox v-model="adminNoticeIncludeExpired">包含过期/未生效</el-checkbox>
            </div>

            <div class="f-item">
              <el-button size="small" type="primary" plain @click="reloadAdminNoticePage(true)">查询</el-button>
              <el-button size="small" @click="resetAdminNoticeFilters">重置</el-button>
            </div>
          </div>

          <div class="admin-notice-toolbar-right">
            <div class="admin-notice-count">共 {{ adminNoticeTotalCount }} 条</div>
            <el-button size="small" type="primary" @click="openAdminNoticeCreate">发布新公告</el-button>
          </div>
        </div>

        <div v-if="adminNoticeListLoading" class="admin-notice-loading">正在加载公告…</div>
        <div v-else-if="!adminNoticePageList.length" class="admin-notice-empty">暂无公告</div>

        <ul v-else class="admin-notice-list">
          <li
              v-for="item in adminNoticePageList"
              :key="item.id"
              class="admin-notice-item"
              @click="openAdminNoticeDetail(item, 'list')"
          >
            <div class="admin-notice-badges">
              <div class="admin-notice-pill admin-type" :class="'type-' + item.type">
                <span class="admin-notice-icon">{{ noticeTypeIcon(item.type) }}</span>
                <span>{{ noticeTypeText(item.type) }}</span>
              </div>

              <div class="admin-notice-pill admin-level" :class="'level-' + item.level">
                {{ noticeLevelText(item.level) }}
              </div>

              <div v-if="Number(item.isTop) === 1" class="admin-notice-pill admin-top">置顶</div>
            </div>

            <div class="admin-notice-main">
              <div class="admin-notice-title" :title="item.title">{{ item.title }}</div>
              <div v-if="item.summary" class="admin-notice-snippet" :title="item.summary">{{ item.summary }}</div>
              <div class="admin-notice-meta">
                <span>{{ formatNoticeTime(item.publishTime, true) }}</span>
                <span class="admin-dot">·</span>
                <span>{{ item.targetText || '全部用户' }}</span>
              </div>
            </div>

            <div class="admin-notice-actions" @click.stop>
              <el-button size="small" type="primary" link @click.stop="openAdminNoticeEdit(item)">编辑</el-button>
              <el-button size="small" type="danger" link @click.stop="confirmAdminNoticeDelete(item)">删除</el-button>
            </div>
          </li>
        </ul>

        <div class="admin-notice-pagination">
          <el-pagination
              background
              layout="prev, pager, next"
              :page-size="adminNoticePageSize"
              :current-page="adminNoticePageNum"
              :total="adminNoticeTotalCount"
              @current-change="onAdminNoticePageChange"
          />
        </div>
      </el-dialog>

      <!-- 管理端：公告详情 -->
      <el-dialog
          v-model="adminNoticeDetailVisible"
          title="公告详情"
          width="860px"
          align-center
          append-to-body
          :lock-scroll="false"
          :z-index="5010"
      >
        <div v-if="adminNoticeDetail" class="admin-notice-detail-body">
          <div class="admin-notice-detail-actions">
            <el-button type="primary" link @click="backToAdminNoticeList">返回公告列表</el-button>

            <div class="admin-notice-detail-right">
              <el-button type="primary" plain size="small" @click="openAdminNoticeEdit(adminNoticeDetail)">编辑</el-button>
              <el-button type="danger" plain size="small" @click="confirmAdminNoticeDelete(adminNoticeDetail)">删除</el-button>
            </div>
          </div>

          <div class="admin-notice-detail-card">
            <div class="d-title">{{ adminNoticeDetail.title }}</div>

            <div class="d-tags">
        <span class="d-tag" :class="'type-' + adminNoticeDetail.type">
          {{ noticeTypeIcon(adminNoticeDetail.type) }} {{ noticeTypeText(adminNoticeDetail.type) }}
        </span>
              <span class="d-tag" :class="'level-' + adminNoticeDetail.level">
          {{ noticeLevelText(adminNoticeDetail.level) }}
        </span>
              <span v-if="Number(adminNoticeDetail.isTop) === 1" class="d-tag top">置顶</span>
            </div>

            <el-divider />

            <div class="d-info">
              <div class="d-info-item"><span class="k">发布时间</span><span class="v">{{ formatNoticeTime(adminNoticeDetail.publishTime, true) }}</span></div>
              <div class="d-info-item"><span class="k">公告类型</span><span class="v">{{ noticeTypeText(adminNoticeDetail.type) }}</span></div>
              <div class="d-info-item"><span class="k">重要程度</span><span class="v">{{ noticeLevelText(adminNoticeDetail.level) }}</span></div>
              <div class="d-info-item"><span class="k">面向对象</span><span class="v">{{ adminNoticeDetail.targetText || '全部用户' }}</span></div>
            </div>

            <el-divider />

            <div class="d-content">{{ adminNoticeDetail.content || '（无内容）' }}</div>
          </div>
        </div>
      </el-dialog>

      <!-- 管理端：发布/编辑公告（编辑=重新发布） -->
      <el-dialog
          v-model="adminNoticeEditVisible"
          :title="adminNoticeEditMode === 'create' ? '发布新公告' : '编辑公告（重新发布）'"
          width="860px"
          align-center
          append-to-body
          :lock-scroll="false"
          :z-index="5020"
      >
        <el-form :model="adminNoticeForm" label-width="90px" class="admin-notice-form">
          <el-form-item label="公告类型">
            <el-select v-model="adminNoticeForm.type" :teleported="false" placeholder="请选择" style="width: 240px">

              <el-option v-for="opt in noticeTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />

            </el-select>
          </el-form-item>

          <el-form-item label="重要程度">
            <el-select v-model="adminNoticeForm.level" :teleported="false" placeholder="请选择" style="width: 240px">

              <el-option v-for="opt in noticeLevelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />

            </el-select>
          </el-form-item>

          <el-form-item label="面向对象">
            <el-select v-model="adminNoticeForm.targetRole" :teleported="false" placeholder="请选择" style="width: 240px">

              <el-option v-for="opt in noticeTargetOptions" :key="opt.value" :label="opt.label" :value="opt.value" />

            </el-select>
          </el-form-item>

          <el-form-item label="对象说明">
            <el-input v-model="adminNoticeForm.targetText" placeholder="例如：全体学生 / 本部学生 / 研究生等（可选）" />
          </el-form-item>

          <el-form-item label="标题">
            <el-input v-model="adminNoticeForm.title" maxlength="80" show-word-limit />
          </el-form-item>

          <el-form-item label="内容">
            <el-input v-model="adminNoticeForm.content" type="textarea" :rows="8" maxlength="2000" show-word-limit />
          </el-form-item>

          <el-form-item label="置顶">
            <el-switch v-model="adminNoticeForm.isTop" />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="adminNoticeEditVisible = false">取消</el-button>
          <el-button type="primary" :loading="adminNoticeSubmitting" @click="submitAdminNotice">
            {{ adminNoticeEditMode === 'create' ? '发布' : '重新发布' }}
          </el-button>
        </template>
      </el-dialog>
    </div>

    <!-- ============ 预约管理（整合版） ============ -->
    <div v-else-if="currentPage === 'admin-reservations'" class="card reservation-card">
      <div class="card-header reservation-header">
        <div>
          <h2 class="page-title">预约管理</h2>
          <p class="page-subtitle">
            查看和管理学生预约记录，支持筛选、批量操作、补录签到与标记违约。
          </p>
        </div>
        <div class="reservation-header-right">
          <el-button type="primary" plain size="small" @click="handleReservationRefresh">
            刷新
          </el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-input
            v-model="reservationFilters.keyword"
            placeholder="按姓名 / 学号 / 预约编号搜索"
            clearable
            class="filter-item filter-input"
        >
          <template #prefix>
            <span class="input-prefix-icon">🔍</span>
          </template>
        </el-input>

        <el-select
            v-model="reservationFilters.roomId"
            placeholder="选择自习室"
            clearable
            class="filter-item filter-select"
        >
          <el-option v-for="room in reservationRoomOptions" :key="room.id" :label="room.label" :value="room.id" />
        </el-select>

        <el-date-picker
            v-model="reservationFilters.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            clearable
            class="filter-item filter-date"
        />

        <el-select
            v-model="reservationFilters.status"
            placeholder="预约状态"
            clearable
            class="filter-item filter-select"
        >
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <div class="filter-actions">
          <el-button type="primary" size="small" @click="handleReservationSearch">
            查询
          </el-button>
          <el-button size="small" @click="resetReservationFilters">
            重置
          </el-button>
        </div>
      </div>

      <div class="reservation-stats">
        <div class="stat-card">
          <div class="stat-label">当前记录</div>
          <div class="stat-value">{{ reservationStats.total }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已预约</div>
          <div class="stat-value">{{ reservationStats.booked }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已签到</div>
          <div class="stat-value">{{ reservationStats.signed }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已取消 / 违约</div>
          <div class="stat-value">
            {{ reservationStats.canceled }} / {{ reservationStats.violation }}
          </div>
        </div>
      </div>

      <div class="reservation-toolbar">
        <div class="toolbar-left">
          <el-button
              type="success"
              size="small"
              :disabled="!canBatchCheckin"
              @click="handleBatchSign"
          >
            批量补录签到
          </el-button>
          <el-button
              type="danger"
              size="small"
              plain
              :disabled="!canBatchCancel"
              @click="handleBatchCancel"
          >
            批量取消预约
          </el-button>
          <span v-if="reservationSelection.length" class="toolbar-tip">
            已选中 {{ reservationSelection.length }} 条记录
          </span>
        </div>
      </div>

      <el-table
          ref="reservationTableRef"
          :data="reservations"
          border
          stripe
          size="small"
          table-layout="fixed"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          class="reservation-table"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="code" label="预约编号" min-width="120" />

        <el-table-column prop="studentName" label="学生" min-width="110">
          <template #default="scope">
            <div class="student-cell">
              <div class="student-name">{{ scope.row.studentName }}</div>
              <div class="student-no">{{ scope.row.studentNo }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="room" label="自习室" min-width="130" />
        <el-table-column prop="seatNo" label="座位" min-width="80" />
        <el-table-column prop="date" label="日期" min-width="110" />
        <el-table-column prop="timeRange" label="时间段" min-width="120" />

        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="160">
          <template #default="scope">
            <span class="remark-text">{{ scope.row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="200">
          <template #default="scope">
            <div class="action-buttons">
              <!-- 1) 已预约：只能取消 + 详情 -->
              <template v-if="scope.row.status === 'reserved'">
                <el-button type="danger" link size="small" @click="handleCancel(scope.row)">
                  取消预约
                </el-button>
                <el-button type="info" link size="small" @click="handleViewDetail(scope.row)">
                  查看详情
                </el-button>
              </template>

              <!-- 2) 未签到 / 迟到：可补录签到 + 详情 -->
              <template v-else-if="scope.row.status === 'no_show' || scope.row.status === 'late'">
                <el-button type="primary" link size="small" @click="handleSign(scope.row)">
                  补录签到
                </el-button>
                <el-button type="info" link size="small" @click="handleViewDetail(scope.row)">
                  查看详情
                </el-button>
              </template>

              <!-- 3) 已签到：可标记违规 + 详情 -->
              <template v-else-if="scope.row.status === 'checked_in'">
                <el-button type="warning" link size="small" @click="handleMarkViolation(scope.row)">
                  标记违规
                </el-button>
                <el-button type="info" link size="small" @click="handleViewDetail(scope.row)">
                  查看详情
                </el-button>
              </template>

              <!-- 4) 已取消 / 逾期取消：仅详情 -->
              <template v-else>
                <el-button type="info" link size="small" @click="handleViewDetail(scope.row)">
                  查看详情
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="reservationTotal"
            :page-sizes="[8, 16, 24]"
            :page-size="reservationPageSize"
            :current-page="reservationPage"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- ============ 用户管理（整合版） ============ -->
    <div v-else-if="currentPage === 'admin-users'" class="card user-card">
      <div class="card-header user-header">
        <div>
          <h2 class="page-title">用户管理</h2>
          <p class="page-subtitle">
            查询学生基本信息、信用状态与违约记录，支持筛选、批量黑名单与预警管理。
          </p>
        </div>
        <div class="user-header-right">
          <el-button type="primary" plain size="small" @click="openCreateAdminDialog">
            新增管理员账号
          </el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-input
            v-model="userFilters.keyword"
            placeholder="按姓名 / 学号搜索"
            clearable
            class="filter-item filter-input"
        >
          <template #prefix>
            <span class="input-prefix-icon">🔍</span>
          </template>
        </el-input>

        <el-select
            v-model="userFilters.roleId"
            placeholder="用户角色"
            clearable
            class="filter-item filter-select"
        >
          <el-option
              v-for="role in userRoleOptions"
              :key="role.value"
              :label="role.label"
              :value="role.value"
          />
        </el-select>

        <el-select
            v-model="userFilters.status"
            placeholder="信用状态"
            clearable
            class="filter-item filter-select"
        >
          <el-option
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>

        <el-checkbox v-model="userFilters.onlyAbnormal" class="filter-item">
          仅查看预警 / 黑名单
        </el-checkbox>

        <div class="filter-actions">
          <el-button type="primary" size="small" @click="handleUserSearch">
            查询
          </el-button>
          <el-button size="small" @click="resetUserFilters">
            重置
          </el-button>
        </div>
      </div>

      <div class="reservation-stats user-stats">
        <div class="stat-card">
          <div class="stat-label">当前用户数</div>
          <div class="stat-value">{{ userStats.total }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">正常</div>
          <div class="stat-value">{{ userStats.normal }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">预警</div>
          <div class="stat-value">{{ userStats.warning }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">黑名单</div>
          <div class="stat-value">{{ userStats.blacklist }}</div>
        </div>
      </div>

      <div class="reservation-toolbar user-toolbar">
        <div class="toolbar-left">
          <template v-if="String(userFilters.roleId) !== '0'">
            <el-button
                type="warning"
                size="small"
                plain
                :disabled="!userSelection.length"
                @click="handleBatchLock"
            >
              批量加入黑名单
            </el-button>
            <el-button
                type="success"
                size="small"
                plain
                :disabled="!userSelection.length"
                @click="handleBatchUnlock"
            >
              批量解除黑名单
            </el-button>
            <span v-if="userSelection.length" class="toolbar-tip">
              已选中 {{ userSelection.length }} 位用户
            </span>
          </template>
        </div>
        <div class="toolbar-right">
          <span class="toolbar-tip">
            说明：预警状态由系统根据信用分自动判定（&lt;80 分为预警），黑名单仅管理员可手动设置/解除。
          </span>
        </div>
      </div>

      <el-table
          ref="userTableRef"
          v-loading="userLoading"
          :data="users"
          border
          stripe
          size="small"
          table-layout="fixed"
          style="width: 100%"
          @selection-change="handleUserSelectionChange"
          class="user-table"
      >
        <el-table-column type="selection" width="48" :selectable="userRowSelectable" />
        <el-table-column prop="studentNo" label="学号 / 工号" min-width="120" />

        <el-table-column prop="name" label="姓名" min-width="120">
          <template #default="scope">
            <div class="user-name-cell">
              <div class="user-name-main">
                {{ scope.row.name }}
                <span class="user-role-tag" v-if="Number(scope.row.roleId) === 1">用户</span>
                <span class="user-role-tag admin" v-else-if="Number(scope.row.roleId) === 0">管理员</span>
              </div>
              <div class="user-extra">{{ scope.row.college || '—' }} · {{ scope.row.gradeClass || '—' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="creditScore" label="信用分" min-width="90">
          <template #default="scope">
            <span
                :class="[
                'credit-score',
                scope.row.creditScore <= 60 ? 'credit-low' : '',
                scope.row.creditScore >= 95 ? 'credit-high' : ''
              ]"
            >
              {{ scope.row.creditScore }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="violationCount" label="违约次数" min-width="90" />

        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="scope">
            <el-tag :type="getUserStatusTagType(scope.row.status)" size="small">
              {{ getUserStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="lastLoginTime" label="最近登录" min-width="140" />

        <el-table-column label="联系方式" min-width="130">
          <template #default="scope">
            <span class="user-extra">{{ scope.row.phone || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="230">
          <template #default="scope">
            <div class="action-buttons">
              <template v-if="Number(scope.row.roleId) !== 0">
                <el-button type="primary" link size="small" @click="handleResetPassword(scope.row)">
                  重置密码
                </el-button>

                <el-button
                    v-if="Number(scope.row.status) !== 2"
                    type="danger"
                    link
                    size="small"
                    @click="handleLockUser(scope.row)"
                >
                  加入黑名单
                </el-button>

                <el-button
                    v-else
                    type="success"
                    link
                    size="small"
                    @click="handleUnlockUser(scope.row)"
                >
                  解除黑名单
                </el-button>
              </template>

              <el-button type="info" link size="small" @click="handleViewUserDetail(scope.row)">
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="userTotal"
            :page-sizes="[8, 16, 24]"
            :page-size="userPageSize"
            :current-page="userPage"
            @size-change="handleUserPageSizeChange"
            @current-change="handleUserPageChange"
        />
      </div>

      <!-- 用户详情（抽屉） -->
      <el-drawer
          v-model="userDetailVisible"
          :with-header="false"
          size="520px"
          append-to-body
          class="user-detail-drawer"
          @opened="handleUserDetailDrawerOpened"
      >
        <div class="user-detail" v-loading="userDetailLoading">
          <div class="ud-head">
            <div class="ud-avatar">{{ (userDetail.user?.name || 'U').slice(0, 1) }}</div>
            <div class="ud-meta">
              <div class="ud-name-line">
                <span class="ud-name">{{ userDetail.user?.name || '—' }}</span>
                <el-tag size="small" :type="Number(userDetail.user?.roleId) === 0 ? 'info' : 'primary'" class="ud-tag">
                  {{ Number(userDetail.user?.roleId) === 0 ? '管理员' : '用户' }}
                </el-tag>
                <el-tag size="small" :type="getUserStatusTagType(userDetail.user?.status)" class="ud-tag">
                  {{ getUserStatusText(userDetail.user?.status) }}
                </el-tag>
              </div>
              <div class="ud-sub">
                <span>账号：{{ userDetail.user?.account || '—' }}</span>
                <span class="dot">·</span>
                <span>学号/工号：{{ userDetail.user?.studentNo || '—' }}</span>
              </div>
            </div>
          </div>

          <div class="ud-stats">
            <div class="ud-stat">
              <div class="k">信用分</div>
              <div class="v">{{ userDetail.user?.creditScore ?? '—' }}</div>
            </div>
            <div class="ud-stat">
              <div class="k">违约次数</div>
              <div class="v">{{ userDetail.violationCount ?? 0 }}</div>
            </div>
          </div>

          <el-divider />

          <el-descriptions border size="small" :column="1" class="ud-desc">
            <el-descriptions-item label="学院">{{ userDetail.user?.college || '—' }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ userDetail.user?.gradeClass || '—' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userDetail.user?.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userDetail.user?.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">
              {{ userDetail.user?.lastLoginTime || '—' }}
              <span class="ud-muted" v-if="userDetail.user?.lastLoginIp">（{{ userDetail.user?.lastLoginIp }}）</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ userDetail.user?.createTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ userDetail.user?.profileRemark || '—' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <div class="ud-section">
            <div class="ud-section-title">最近违约记录</div>
            <el-table
                ref="userViolationTableRef"
                :data="userDetail.recentViolations || []"
                size="small"
                border
                table-layout="fixed"
                style="width: 100%"
                class="ud-table"
                empty-text="暂无违约记录"
            >
              <el-table-column prop="vDate" label="日期" min-width="110" />
              <el-table-column prop="vType" label="类型" min-width="90">
                <template #default="scope">
                  <el-tag size="small" :type="scope.row.vType === 'no_show' ? 'danger' : 'warning'">
                    {{ getViolationTypeText(scope.row.vType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="penaltyScore" label="扣分" min-width="70" />
              <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
              <el-table-column prop="createTime" label="记录时间" min-width="160" />
            </el-table>
          </div>
        </div>
      </el-drawer>

      <!-- 新增管理员账号（弹窗） -->
      <el-dialog
          v-model="createAdminVisible"
          title="新增管理员账号"
          width="420px"
          top="25vh"
          align-center
          :close-on-click-modal="false"
      >
        <el-form
            ref="createAdminFormRef"
            :model="createAdminForm"
            :rules="createAdminRules"
            label-width="70px"
        >
          <el-form-item label="账号" prop="account">
            <el-input v-model="createAdminForm.account" placeholder="用于登录" clearable />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="createAdminForm.name" placeholder="管理员姓名" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="createAdminForm.password" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="createAdminVisible = false">取消</el-button>
          <el-button type="primary" :loading="createAdminLoading" @click="submitCreateAdmin">创建</el-button>
        </template>
      </el-dialog>
    </div>


    <div v-if="currentPage === 'admin-complaints'" class="complaints-page">
      <div class="card complaints-head">
        <div class="complaints-head-left">
          <h2 class="page-title">投诉处理</h2>
          <p class="page-subtitle">查看学生评价/投诉/建议，回复并跟踪处理进度；必要时可关联处理预约。</p>
        </div>

        <div class="complaints-head-right">
          <div class="fb-stats">
            <div class="fb-stat">
              <div class="fb-stat-num">{{ fbAdminStats.pending }}</div>
              <div class="fb-stat-label">待处理</div>
            </div>
            <div class="fb-stat">
              <div class="fb-stat-num">{{ fbAdminStats.processing }}</div>
              <div class="fb-stat-label">处理中</div>
            </div>
            <div class="fb-stat">
              <div class="fb-stat-num">{{ fbAdminStats.resolved }}</div>
              <div class="fb-stat-label">已解决</div>
            </div>
          </div>

          <el-button size="small" @click="loadAdminFeedback(true)">刷新</el-button>
        </div>
      </div>

      <div class="card complaints-body">
        <div class="complaints-toolbar">
          <div class="tool-left">
            <el-checkbox v-model="fbAdminOnlyPending" @change="onFbAdminFilterChange">仅查看待处理</el-checkbox>

            <el-select
                v-model="fbAdminStatusFilter"
                :teleported="false"
                size="small"
                clearable
                placeholder="全部状态"
                style="width: 150px"
                @change="onFbAdminFilterChange"
            >
              <el-option label="全部状态" value="" />
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已解决" value="resolved" />
            </el-select>

            <el-select
                v-model="fbAdminCategoryFilter"
                :teleported="false"
                size="small"
                clearable
                placeholder="全部类型"
                style="width: 170px"
                @change="onFbAdminFilterChange"
            >
              <el-option label="全部类型" value="" />
              <el-option label="环境评价" value="env" />
              <el-option label="服务评价" value="service" />
              <el-option label="建议" value="suggestion" />
              <el-option label="投诉" value="complaint" />
              <el-option label="申诉" value="appeal" />
              <el-option label="其他" value="other" />
            </el-select>
          </div>

          <div class="tool-right">
            <el-input
                v-model="fbAdminKeyword"
                size="small"
                clearable
                placeholder="搜索：内容/姓名/学号/预约ID"
                style="width: 340px"
                @keyup.enter="loadAdminFeedback(true)"
            >
              <template #append>
                <el-button @click="loadAdminFeedback(true)">搜索</el-button>
              </template>
            </el-input>
          </div>
        </div>

        <el-table
            ref="fbAdminTableRef"
            :data="fbAdminList"
            v-loading="fbAdminLoading"
            border
            stripe
            size="small"
            table-layout="fixed"
            style="width: 100%"
            class="fb-admin-table"
        >
          <el-table-column prop="id" label="#" width="72" />

          <el-table-column label="类型" width="110">
            <template #default="scope">
              <el-tag size="small" :type="fbAdminCategoryTagType(scope.row.category)">
                {{ fbAdminCategoryText(scope.row.category) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="用户" min-width="160">
            <template #default="scope">
              <div class="fb-user">
                <div class="fb-user-name">{{ scope.row.userName || '-' }}</div>
                <div class="fb-user-sub">{{ scope.row.studentNo || '-' }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="关联预约" min-width="220">
            <template #default="scope">
              <div class="fb-rel">
                <div class="fb-rel-main">{{ scope.row.roomLabel || '-' }}</div>
                <div class="fb-rel-sub">{{ scope.row.reservationLabel || (scope.row.reservationId ? ('预约ID ' + scope.row.reservationId) : '未关联预约') }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="评分" width="90" align="center">
            <template #default="scope">
              <span v-if="scope.row.rating">{{ scope.row.rating }}</span>
              <span v-else class="fb-muted">-</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="110">
            <template #default="scope">
              <el-tag size="small" :type="fbAdminStatusTagType(scope.row.status)">
                {{ fbAdminStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="提交时间" width="160">
            <template #default="scope">{{ formatYmdhm(scope.row.createTime) }}</template>
          </el-table-column>

          <el-table-column label="更新时间" width="160">
            <template #default="scope">{{ formatYmdhm(scope.row.updateTime) }}</template>
          </el-table-column>

          <el-table-column label="操作" width="150">
            <template #default="scope">
              <div class="fb-row-actions">
                <el-button size="small" type="primary" plain @click="openFbAdminDetail(scope.row)">处理</el-button>
                <el-button size="small" @click="quickResolveFb(scope.row)">已解决</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="fb-admin-pagination">
          <el-pagination
              background
              layout="prev, pager, next, jumper"
              :current-page="fbAdminPageNum"
              :page-size="fbAdminPageSize"
              :total="fbAdminTotal"
              @current-change="fbAdminPageChange"
          />
        </div>
      </div>

      <el-drawer
          v-model="fbAdminDrawerVisible"
          size="520px"
          :with-header="false"
          append-to-body
          class="fb-admin-drawer"
          @opened="handleFbAdminDrawerOpened"
      >
        <div v-if="fbAdminDetail" class="fb-drawer">
          <div class="fb-drawer-head">
            <div>
              <div class="fb-drawer-title">反馈 #{{ fbAdminDetail.id }}</div>
              <div class="fb-drawer-sub">
                {{ fbAdminCategoryText(fbAdminDetail.category) }} · {{ fbAdminStatusText(fbAdminDetail.status) }}
              </div>
            </div>
            <el-button size="small" @click="fbAdminDrawerVisible = false">关闭</el-button>
          </div>

          <div class="fb-drawer-card">
            <div class="fb-kv">
              <div class="k">用户</div>
              <div class="v">{{ fbAdminDetail.userName || '-' }}（{{ fbAdminDetail.studentNo || '-' }}）</div>
            </div>
            <div class="fb-kv">
              <div class="k">自习室</div>
              <div class="v">{{ fbAdminDetail.roomLabel || '-' }}</div>
            </div>
            <div class="fb-kv">
              <div class="k">关联预约</div>
              <div class="v">{{ fbAdminDetail.reservationLabel || (fbAdminDetail.reservationId ? ('预约ID ' + fbAdminDetail.reservationId) : '未关联预约') }}</div>
            </div>
            <div class="fb-kv">
              <div class="k">评分</div>
              <div class="v">{{ fbAdminDetail.rating || '-' }}</div>
            </div>
            <div class="fb-kv">
              <div class="k">提交时间</div>
              <div class="v">{{ formatYmdhm(fbAdminDetail.createTime) }}</div>
            </div>

            <div class="fb-content">
              <div class="fb-content-title">反馈内容</div>
              <div class="fb-content-body">{{ fbAdminDetail.content || '（空）' }}</div>
            </div>

            <div v-if="fbAdminDetail.reply" class="fb-content">
              <div class="fb-content-title">已回复</div>
              <div class="fb-content-body">{{ fbAdminDetail.reply }}</div>
            </div>
          </div>

          <div class="fb-drawer-card">
            <div class="fb-form-title">处理与回复</div>

            <el-form :model="fbAdminHandleForm" label-width="86px" class="fb-handle-form">
              <el-form-item label="处理状态">
                <el-radio-group v-model="fbAdminHandleForm.status">
                  <el-radio label="pending">待处理</el-radio>
                  <el-radio label="processing">处理中</el-radio>
                  <el-radio label="resolved">已解决</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="回复内容">
                <el-input
                    v-model="fbAdminHandleForm.reply"
                    type="textarea"
                    :rows="5"
                    maxlength="800"
                    show-word-limit
                    placeholder="给学生的回复（可选，但建议在标记已解决时填写）"
                />
              </el-form-item>

              <el-form-item v-if="fbAdminDetail && fbAdminDetail.reservationId" label="关联预约">
                <el-checkbox v-model="fbAdminHandleForm.cancelReservation">
                  取消该预约（强制取消，仅对“已预约/待签到”有效）
                </el-checkbox>
              </el-form-item>

              <el-form-item>
                <div class="fb-handle-actions">
                  <el-button @click="fbAdminDrawerVisible = false">取消</el-button>
                  <el-button type="primary" :loading="fbAdminSubmitting" @click="submitFbAdminHandle">提交处理</el-button>
                </div>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-drawer>
    </div>
    <!-- ============ 日志统计（占位页，保留入口不丢功能） ============ -->

  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'AdminHome',
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],
  data () {
    return {
      /* timers */
      dashboardTimer: null,
      reservationTimer: null,
      /* Dashboard */
      autoRefresh: false,
      metrics: {
        todayReservations: 120,
        todayTrend: 12,
        todaySignRate: 82,
        openRooms: 12,
        totalSeats: 680,
        pendingItems: 5
      },
      trendItems: [
        {
          key: 'weekReservation',
          title: '本周预约完成率',
          value: '86%',
          percent: 86,
          status: 'success',
          desc: '预约完成率保持在较高水平，可继续观察周末波动。'
        },
        {
          key: 'weekCheckin',
          title: '本周平均签到率',
          value: '79%',
          percent: 79,
          status: 'success',
          desc: '建议针对低年级同学开展一次到馆提醒活动。'
        },
        {
          key: 'weekViolation',
          title: '本周违约率',
          value: '6%',
          percent: 6,
          status: 'warning',
          desc: '有 8 位同学出现多次违约，可结合信用分规则重点关注。'
        }
      ],

      /* Admin Announcement */
      adminNoticeHomeLoading: false,
      adminNoticeListLoading: false,
      adminHomeNotices: [],

      // 公告枚举（与 announcement 表字段含义保持一致）
      noticeTypeOptions: [
        { label: '规则', value: 'RULE' },
        { label: '调整', value: 'ADJUSTMENT' },
        { label: '突发', value: 'EMERGENCY' },
        { label: '维护', value: 'MAINTENANCE' },
        { label: '考试', value: 'EXAM' },
        { label: '其他', value: 'OTHER' }
      ],
      noticeLevelOptions: [
        { label: '重要', value: 'IMPORTANT' },
        { label: '提醒', value: 'WARNING' },
        { label: '通知', value: 'INFO' }
      ],
      noticeTargetOptions: [
        { label: '全体管理员', value: 0 },
        { label: '全体学生', value: 1 },
        { label: '全部用户', value: 2 }
      ],


      adminNoticeAllVisible: false,
      adminNoticeDetailVisible: false,
      adminNoticeDetail: null,
      adminNoticeDetailFrom: 'home',

      adminNoticeTypeFilter: '',
      adminNoticeLevelFilter: '',
      adminNoticeTargetFilter: null,
      adminNoticeKeyword: '',
      adminNoticeIncludeExpired: false,

      adminNoticePageNum: 1,
      adminNoticePageSize: 6,
      adminNoticeTotalCount: 0,
      adminNoticePageList: [],

      adminNoticeEditVisible: false,
      adminNoticeEditMode: 'create', // create | edit
      adminNoticeSubmitting: false,
      adminNoticeForm: {
        id: null,
        type: 'RULE',
        level: 'INFO',
        targetRole: 2, // 0=管理员 1=学生 2=全部用户
        targetText: '',
        title: '',
        content: '',
        isTop: false
      },

      /* Admin Feedback（投诉处理） */
      fbAdminLoading: false,
      fbAdminList: [],
      fbAdminPageNum: 1,
      fbAdminPageSize: 8,
      fbAdminTotal: 0,
      fbAdminStatusFilter: '',
      fbAdminCategoryFilter: '',
      fbAdminKeyword: '',
      fbAdminOnlyPending: true,
      fbAdminStats: { pending: 0, processing: 0, resolved: 0 },

      fbAdminDrawerVisible: false,
      fbAdminDetail: null,
      fbAdminSubmitting: false,
      fbAdminHandleForm: {
        status: 'processing',
        reply: '',
        cancelReservation: false
      },
      tableLayoutRafId: null,
      tableLayoutTimers: {},
      pendingTableLayoutRefs: [],
      // 待办事项（管理员首页：根据统计动态生成）
      todos: [],
      // 本地“标记完成”仅做隐藏，不影响后端数据（默认隐藏 6 小时）
      todoHideHours: 6,
      todoDismissed: {},

      latestActivities: [
        { id: 1, text: '张三 预约了 图书馆 401 · 10:00-12:00', type: 'book', typeLabel: '新预约', time: '09:15' },
        { id: 2, text: '李四 在 本部 · 3 楼 301 完成签到', type: 'sign', typeLabel: '已签到', time: '08:05' },
        { id: 3, text: '王五 取消了 明日的预约（原因：课程调整）', type: 'cancel', typeLabel: '已取消', time: '07:48' }
      ],

      /* Reservation Manage */
      reservationFilters: {
        keyword: '',
        roomId: null,
        date: '',
        status: ''
      },
      reservationRoomOptions: [],
      statusOptions: [
        { label: '已预约', value: 'reserved' },
        { label: '已签到', value: 'checked_in' },
        { label: '已取消', value: 'cancelled' },
        { label: '违约', value: 'violation' }
      ],
      reservationLoading: false,
      reservations: [],
      reservationTotal: 0,
      reservationStats: { total: 0, booked: 0, signed: 0, canceled: 0, violation: 0 },
      reservationSelection: [],
      reservationPage: 1,
      reservationPageSize: 8,

      /* User Manage */

      /* User Manage */
      userLoading: false,
      userFilters: {
        keyword: '',
        roleId: '',
        status: '',
        onlyAbnormal: false
      },
      userRoleOptions: [
        { label: '用户', value: 1 },
        { label: '管理员', value: 0 }
      ],
      userStatusOptions: [
        { label: '正常', value: 0 },
        { label: '预警', value: 1 },
        { label: '黑名单', value: 2 }
      ],
      users: [],
      userTotal: 0,
      userStats: { total: 0, normal: 0, warning: 0, blacklist: 0 },
      userSelection: [],
      userPage: 1,
      userPageSize: 8,

      userDetailVisible: false,
      userDetailLoading: false,
      userDetail: { user: null, violationCount: 0, recentViolations: [] },

      createAdminVisible: false,
      createAdminLoading: false,
      createAdminForm: { account: '', name: '', password: '' },
      createAdminRules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码至少 6 位', trigger: 'blur' }
        ]
      },


      /* Seat Manage */
      currentCampus: 'main',
      campusOptions: [
        { label: '本部校区', value: 'main' },
        { label: '东校区', value: 'east' }
      ],
      currentBuild: '',
      buildOptions: [
        { label: '3 号教学楼', value: '3F' },
        { label: '图书馆', value: 'LIB' }
      ],
      onlyShowOpen: true,

      roomKeyword: '',
      roomCapacityFilter: 'all',
      rooms: [
        {
          id: 1,
          name: '3 楼 301 自习室',
          campus: 'main',
          building: '3 号教学楼',
          buildingCode: '3F',
          floor: 3,
          capacity: 60,
          usedSeats: 32,
          status: 'open'
        },
        {
          id: 2,
          name: '3 楼 302 自习室',
          campus: 'main',
          building: '3 号教学楼',
          buildingCode: '3F',
          floor: 3,
          capacity: 40,
          usedSeats: 12,
          status: 'open'
        },
        {
          id: 3,
          name: '图书馆 401 自习区',
          campus: 'main',
          building: '图书馆',
          buildingCode: 'LIB',
          floor: 4,
          capacity: 90,
          usedSeats: 76,
          status: 'open'
        },
        {
          id: 4,
          name: '图书馆 501 研讨区',
          campus: 'main',
          building: '图书馆',
          buildingCode: 'LIB',
          floor: 5,
          capacity: 30,
          usedSeats: 5,
          status: 'closed'
        }
      ],
      selectedRoomId: 1,

      activeTab: 'layout',
      showSeatNo: true,
      showDemoOccupied: true,
      seatGrid: [],

      seatRule: {
        allowCrossTime: false,
        maxHoursPerOrder: 4,
        maxOrdersPerDay: 2,
        autoReleaseNoSign: true,
        releaseAfterMinutes: 20,
        violationStrategy: 'day-ban',
        description: '本自习室需保持安静，迟到超过 20 分钟系统将自动释放座位。'
      }
    }
  },
  computed: {
    todayText () {
      const d = new Date()
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },



    /* seat computed */
    filteredRooms () {
      return this.rooms.filter(room => {
        let ok = true
        if (this.currentCampus) ok = ok && room.campus === this.currentCampus
        if (this.currentBuild) ok = ok && room.buildingCode === this.currentBuild
        if (this.onlyShowOpen) ok = ok && room.status === 'open'
        if (this.roomKeyword && this.roomKeyword.trim()) {
          const k = this.roomKeyword.trim()
          ok = ok && (room.name.includes(k) || String(room.floor).includes(k) || room.building.includes(k))
        }
        if (this.roomCapacityFilter === 'small') ok = ok && room.capacity <= 40
        if (this.roomCapacityFilter === 'medium') ok = ok && room.capacity > 40 && room.capacity < 80
        if (this.roomCapacityFilter === 'large') ok = ok && room.capacity >= 80
        return ok
      })
    },
    selectedRoom () {
      const room = this.rooms.find(r => r.id === this.selectedRoomId)
      if (!room) return null
      return { ...room, statusModel: room.status === 'open' }
    },
    seatStats () {
      const stats = { free: 0, occupied: 0, disabled: 0, blockedArea: 1 }
      this.seatGrid.forEach(row => {
        row.seats.forEach(seat => {
          if (seat.status === 'free') stats.free++
          else if (seat.status === 'occupied') stats.occupied++
          else if (seat.status === 'disabled') stats.disabled++
        })
      })
      return stats
    },
    latestActivitiesLeft () {
      const mid = Math.ceil(this.latestActivities.length / 2)
      return this.latestActivities.slice(0, mid)
    },
    latestActivitiesRight () {
      const mid = Math.ceil(this.latestActivities.length / 2)
      return this.latestActivities.slice(mid)
    },
    lastLoginIp () {
      const s = localStorage.getItem('ssrmsUser') || sessionStorage.getItem('ssrmsUser')
      const u = s ? JSON.parse(s) : {}
      return u.lastLoginIp || '未知'
    }

    ,

    // 预约管理：批量操作可用性（严格遵守业务规则）
    canBatchCheckin () {
      return (this.reservationSelection || []).length > 0 &&
          (this.reservationSelection || []).every(r => r && (r.status === 'no_show' || r.status === 'late'))
    },
    canBatchCancel () {
      return (this.reservationSelection || []).length > 0 &&
          (this.reservationSelection || []).every(r => r && r.status === 'reserved')
    }},
  watch: {
    currentPage (val) {
      // 已移除：座位管理 / 日志统计
      if (val === 'admin-seats' || val === 'admin-logs') {
        this.emitChange('admin-home')
        return
      }
      if (val !== 'admin-home') this.stopDashboardTimer()
      else if (this.autoRefresh) this.startDashboardTimer()

      if (val === 'admin-home') {
        this.loadAdminNoticeHome()
        this.loadAdminDashboardHome()
        this.loadAdminHomeTodos()
        this.scheduleTableLayout('todoTableRef')
      }
      if (val === 'admin-reservations') {
        this.loadReservationRoomOptions()
        this.loadAdminReservations(true)
        this.scheduleTableLayout('reservationTableRef')
      }

      if (val === 'admin-users') {
        this.loadAdminUsers(true)
        this.scheduleTableLayout('userTableRef')
      }

      if (val === 'admin-complaints') {
        this.loadAdminFeedback(true)
        this.loadAdminFeedbackStats()
        this.scheduleTableLayout('fbAdminTableRef')
      }
    },
    autoRefresh (val) {
      if (val && this.currentPage === 'admin-home') this.startDashboardTimer()
      else this.stopDashboardTimer()
    },
  },
  created () {
    this.initSeatGrid()
    this.loadTodoDismissed()
    this.loadAdminNoticeHome()
    if (this.currentPage === 'admin-home') {
      this.loadAdminDashboardHome()
      this.loadAdminHomeTodos()
    }
    if (this.currentPage === 'admin-reservations') {
      this.loadReservationRoomOptions()
      this.loadAdminReservations(true)
    }
    if (this.currentPage === 'admin-users') {
      this.loadAdminUsers(true)
    }
  },
  beforeUnmount () {
    this.stopDashboardTimer()
    this.clearScheduledTableLayouts()
  },
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    },
    clearScheduledTableLayouts () {
      if (this.tableLayoutRafId && typeof cancelAnimationFrame === 'function') {
        cancelAnimationFrame(this.tableLayoutRafId)
      }
      this.tableLayoutRafId = null
      Object.values(this.tableLayoutTimers || {}).forEach((timer) => clearTimeout(timer))
      this.tableLayoutTimers = {}
      this.pendingTableLayoutRefs = []
    },
    flushTableLayouts () {
      const names = [...new Set(this.pendingTableLayoutRefs || [])]
      this.pendingTableLayoutRefs = []
      names.forEach((name) => {
        const ref = this.$refs[name]
        const target = Array.isArray(ref) ? ref[0] : ref
        if (target && typeof target.doLayout === 'function') {
          try {
            target.doLayout()
          } catch (e) {
            // ignore layout flush errors
          }
        }
      })
    },
    scheduleTableLayout (refNames) {
      const names = (Array.isArray(refNames) ? refNames : [refNames]).filter(Boolean)
      if (!names.length) return
      this.pendingTableLayoutRefs = [...new Set([...(this.pendingTableLayoutRefs || []), ...names])]
      this.$nextTick(() => {
        if (this.tableLayoutRafId && typeof cancelAnimationFrame === 'function') {
          cancelAnimationFrame(this.tableLayoutRafId)
        }
        if (typeof requestAnimationFrame === 'function') {
          this.tableLayoutRafId = requestAnimationFrame(() => {
            this.tableLayoutRafId = null
            this.flushTableLayouts()
          })
          return
        }
        if (this.tableLayoutTimers.fallback) clearTimeout(this.tableLayoutTimers.fallback)
        this.tableLayoutTimers.fallback = setTimeout(() => {
          delete this.tableLayoutTimers.fallback
          this.flushTableLayouts()
        }, 0)
      })
    },
    handleUserDetailDrawerOpened () {
      this.scheduleTableLayout('userViolationTableRef')
    },
    handleFbAdminDrawerOpened () {
      this.scheduleTableLayout('fbAdminTableRef')
    },

    /* ---------- dashboard timers ---------- */
    startDashboardTimer () {
      this.stopDashboardTimer()
      this.dashboardTimer = setInterval(() => {
        this.refreshDashboardMetrics()
      }, 8000)
    },
    stopDashboardTimer () {
      if (this.dashboardTimer) {
        clearInterval(this.dashboardTimer)
        this.dashboardTimer = null
      }
    },
    refreshDashboardMetrics () {
      // 接入后端：定时刷新管理员首页看板数据
      this.loadAdminDashboardHome(true)
    },

    async loadAdminDashboardHome (silent = false) {
      try {
        const res = await this.$axios.get('/dashboard/admin/home', { params: { recentLimit: 6 } })
        if (res && Number(res.code) === 200 && res.data) {
          const d = res.data || {}
          const m = d.metrics || {}

          // 顶部概览（三项接后端：今日预约总数 / 今日签到率 / 当前开放自习室）
          this.metrics.todayReservations = Number(m.todayReservations ?? 0)
          this.metrics.todayTrend = Number(m.todayTrend ?? 0)
          this.metrics.todaySignRate = Number(m.todaySignRate ?? 0)
          this.metrics.openRooms = Number(m.openRooms ?? 0)
          this.metrics.totalSeats = Number(m.totalSeats ?? 0)

          // 本周运行概览（三项 + 进度条）
          const w = d.weekly || {}
          const weekReservation = Number(w.weekReservationRate ?? 0)
          const weekCheckin = Number(w.weekAvgCheckinRate ?? 0)
          const weekViolation = Number(w.weekViolationRate ?? 0)

          const setTrend = (key, percent) => {
            const item = this.trendItems.find(x => x.key === key)
            if (!item) return
            const p = Math.max(0, Math.min(100, Number(percent ?? 0)))
            item.percent = p
            item.value = `${p}%`

            // status：违约率高一些就给 warning，其余默认 success
            if (key === 'weekViolation') {
              item.status = p >= 10 ? 'exception' : (p >= 5 ? 'warning' : 'success')
            } else {
              item.status = p >= 60 ? 'success' : (p >= 40 ? 'warning' : 'exception')
            }

            item.desc = this.buildTrendDesc(key, p)
          }

          setTrend('weekReservation', weekReservation)
          setTrend('weekCheckin', weekCheckin)
          setTrend('weekViolation', weekViolation)

          // 最近预约动态
          const acts = Array.isArray(d.latestActivities) ? d.latestActivities : []
          this.latestActivities = acts.map(it => ({
            id: it.id,
            text: it.text,
            type: it.type,
            typeLabel: it.typeLabel,
            time: it.time
          }))
        }
      } catch (e) {
        if (!silent) ElMessage.error('管理员首页看板数据加载失败')
        // silent 模式只在控制台记录
        console.error('loadAdminDashboardHome failed:', e)
      }
    },


    /* ---------- admin announcement ---------- */
    noticeTypeText (t) {
      if (t === 'RULE') return '规则'
      if (t === 'ADJUSTMENT') return '调整'
      if (t === 'EMERGENCY') return '突发'
      if (t === 'MAINTENANCE') return '维护'
      if (t === 'EXAM') return '考试'
      return '其他'
    },
    noticeTypeIcon (t) {
      if (t === 'RULE') return '📌'
      if (t === 'ADJUSTMENT') return '🧭'
      if (t === 'EMERGENCY') return '🚨'
      if (t === 'MAINTENANCE') return '🧰'
      if (t === 'EXAM') return '📝'
      return '📣'
    },
    noticeLevelText (l) {
      if (l === 'IMPORTANT') return '重要'
      if (l === 'WARNING') return '提醒'
      return '通知'
    },
    formatNoticeTime (timeStr, withTime) {
      if (!timeStr) return '—'
      const d = new Date(timeStr)
      if (Number.isNaN(d.getTime())) return String(timeStr)
      const pad = (n) => String(n).padStart(2, '0')
      const y = d.getFullYear()
      const m = pad(d.getMonth() + 1)
      const day = pad(d.getDate())
      if (!withTime) return `${y}-${m}-${day}`
      const hh = pad(d.getHours())
      const mm = pad(d.getMinutes())
      return `${y}-${m}-${day} ${hh}:${mm}`
    },
    makeNoticeSummary (content, maxLen = 42) {
      if (!content) return ''
      const s = String(content).replace(/\s+/g, ' ').trim()
      if (!s) return ''
      return s.length > maxLen ? s.slice(0, maxLen) + '…' : s
    },
    normalizeNoticeItem (item) {
      const it = item || {}
      return {
        ...it,
        summary: it.summary || this.makeNoticeSummary(it.content)
      }
    },
    getCurrentUser () {
      const s = localStorage.getItem('ssrmsUser') || sessionStorage.getItem('ssrmsUser')
      try { return s ? JSON.parse(s) : {} } catch (e) { return {} }
    },
    getCurrentUserId () {
      const u = this.getCurrentUser()
      const id = u && (u.id ?? u.userId)
      const n = Number(id)
      return Number.isFinite(n) ? n : null
    },

    async loadAdminNoticeHome (silent = false) {
      if (!silent) this.adminNoticeHomeLoading = true
      try {
        const res = await this.$axios.get('/announcement/admin/page', {
          params: { pageNum: 1, pageSize: 3, includeExpired: 0 }
        })
        if (res && Number(res.code) === 200) {
          const list = Array.isArray(res.data) ? res.data : []
          this.adminHomeNotices = list.map(this.normalizeNoticeItem)
        } else {
          this.adminHomeNotices = []
        }
      } catch (e) {
        this.adminHomeNotices = []
      } finally {
        if (!silent) this.adminNoticeHomeLoading = false
      }
    },

    openAdminNoticeAll () {
      this.adminNoticeAllVisible = true
      this.reloadAdminNoticePage(true)
    },
    async reloadAdminNoticePage (resetPage) {
      if (resetPage) this.adminNoticePageNum = 1
      this.adminNoticeListLoading = true
      try {
        const res = await this.$axios.get('/announcement/admin/page', {
          params: {
            pageNum: this.adminNoticePageNum,
            pageSize: this.adminNoticePageSize,
            type: this.adminNoticeTypeFilter || undefined,
            level: this.adminNoticeLevelFilter || undefined,
            targetRole: (this.adminNoticeTargetFilter === null || this.adminNoticeTargetFilter === '' || this.adminNoticeTargetFilter === undefined)
                ? undefined
                : Number(this.adminNoticeTargetFilter),
            keyword: this.adminNoticeKeyword || undefined,
            includeExpired: this.adminNoticeIncludeExpired ? 1 : 0
          }
        })
        if (res && Number(res.code) === 200) {
          const list = Array.isArray(res.data) ? res.data : []
          this.adminNoticePageList = list.map(this.normalizeNoticeItem)
          this.adminNoticeTotalCount = Number(res.total || 0)
        } else {
          this.adminNoticePageList = []
          this.adminNoticeTotalCount = 0
          ElMessage.error(res?.msg || '加载公告失败')
        }
      } catch (e) {
        this.adminNoticePageList = []
        this.adminNoticeTotalCount = 0
        ElMessage.error('加载公告失败，请检查后端服务或网络连接')
      } finally {
        this.adminNoticeListLoading = false
      }
    },
    onAdminNoticePageChange (p) {
      this.adminNoticePageNum = p
      this.reloadAdminNoticePage(false)
    },
    resetAdminNoticeFilters () {
      this.adminNoticeTypeFilter = ''
      this.adminNoticeLevelFilter = ''
      this.adminNoticeTargetFilter = null
      this.adminNoticeKeyword = ''
      this.adminNoticeIncludeExpired = false
      this.reloadAdminNoticePage(true)
    },

    async openAdminNoticeDetail (item, from) {
      this.adminNoticeDetailFrom = from || 'home'
      // 先展示（更丝滑），再尝试拉取最新详情
      this.adminNoticeDetail = this.normalizeNoticeItem(item)
      this.adminNoticeDetailVisible = true
      try {
        const res = await this.$axios.get('/announcement/admin/detail', { params: { id: item.id } })
        if (res && Number(res.code) === 200 && res.data) {
          this.adminNoticeDetail = this.normalizeNoticeItem(res.data)
        }
      } catch (e) {
        // 静默：已有列表数据可展示
      }
    },
    backToAdminNoticeList () {
      this.adminNoticeDetailVisible = false
      if (this.adminNoticeDetailFrom === 'list') {
        this.adminNoticeAllVisible = true
      }
    },

    openAdminNoticeCreate () {
      this.adminNoticeEditMode = 'create'
      this.adminNoticeForm = {
        id: null,
        type: 'RULE',
        level: 'INFO',
        targetRole: 2,
        targetText: '',
        title: '',
        content: '',
        isTop: false
      }
      this.adminNoticeEditVisible = true
    },
    openAdminNoticeEdit (item) {
      const it = item || {}
      this.adminNoticeEditMode = 'edit'
      this.adminNoticeForm = {
        id: it.id,
        type: it.type || 'RULE',
        level: it.level || 'INFO',
        targetRole: typeof it.targetRole === 'number' ? it.targetRole : Number(it.targetRole ?? 2),
        targetText: it.targetText || '',
        title: it.title || '',
        content: it.content || '',
        isTop: Number(it.isTop) === 1
      }
      this.adminNoticeEditVisible = true
    },

    async submitAdminNotice () {
      const f = this.adminNoticeForm || {}
      const title = String(f.title || '').trim()
      const content = String(f.content || '').trim()
      const type = String(f.type || '').trim()
      const level = String(f.level || '').trim()
      const targetRole = f.targetRole

      if (!title) return ElMessage.warning('请填写公告标题')
      if (!content) return ElMessage.warning('请填写公告内容')
      if (!type) return ElMessage.warning('请选择公告类型')
      if (!level) return ElMessage.warning('请选择重要程度')
      if (targetRole === null || targetRole === undefined || targetRole === '') return ElMessage.warning('请选择面向对象')

      const payload = {
        id: this.adminNoticeEditMode === 'edit' ? f.id : undefined,
        title,
        content,
        type,
        level,
        targetRole: Number(targetRole),
        targetText: String(f.targetText || '').trim(),
        isTop: f.isTop ? 1 : 0,
        createdBy: this.getCurrentUserId()
      }

      this.adminNoticeSubmitting = true
      try {
        const api = this.adminNoticeEditMode === 'create' ? '/announcement/admin/create' : '/announcement/admin/republish'
        const res = await this.$axios.post(api, payload)

        if (res && Number(res.code) === 200) {
          ElMessage.success(this.adminNoticeEditMode === 'create' ? '发布成功' : '重新发布成功')
          this.adminNoticeEditVisible = false
          this.adminNoticeDetailVisible = false

          // 刷新：主页 + 列表（如果打开）
          this.loadAdminNoticeHome(true)
          if (this.adminNoticeAllVisible) this.reloadAdminNoticePage(true)
        } else {
          ElMessage.error(res?.msg || '操作失败')
        }
      } catch (e) {
        ElMessage.error('操作失败，请检查后端服务或网络连接')
      } finally {
        this.adminNoticeSubmitting = false
      }
    },

    async confirmAdminNoticeDelete (item) {
      const it = item || {}
      try {
        await ElMessageBox.confirm(
            `确定删除公告「${it.title || '（无标题）'}」吗？删除后学生端也会同步更新。`,
            '删除确认',
            { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
        )
      } catch (e) {
        return
      }

      this.adminNoticeListLoading = true
      try {
        const res = await this.$axios.delete('/announcement/admin/delete', { params: { id: it.id } })
        if (res && Number(res.code) === 200) {
          ElMessage.success('删除成功')
          this.adminNoticeDetailVisible = false

          this.loadAdminNoticeHome(true)
          if (this.adminNoticeAllVisible) this.reloadAdminNoticePage(true)
        } else {
          ElMessage.error(res?.msg || '删除失败')
        }
      } catch (e) {
        ElMessage.error('删除失败，请检查后端服务或网络连接')
      } finally {
        this.adminNoticeListLoading = false
      }
    },

    getTodoTagType (type) {
      if (type === 'complaint') return 'danger'
      if (type === 'violation') return 'warning'
      if (type === 'rule') return 'success'
      return 'info'
    },
    getTodoTypeText (type) {
      if (type === 'complaint') return '投诉'
      if (type === 'violation') return '违约'
      if (type === 'rule') return '规则配置'
      if (type === 'system') return '系统'
      return '其他'
    },
    getPriorityText (p) {
      if (p === 'high') return '高'
      if (p === 'medium') return '中'
      return '低'
    },

    /* ---------- Admin Home: Todos（动态待办） ---------- */
    loadTodoDismissed () {
      // 仅用于管理员本机“隐藏待办”，不影响后端数据
      try {
        const raw = localStorage.getItem('ssrms_admin_todo_dismissed') || '{}'
        const obj = JSON.parse(raw)
        this.todoDismissed = (obj && typeof obj === 'object') ? obj : {}
        this.cleanTodoDismissed()
      } catch (e) {
        this.todoDismissed = {}
      }
    },
    saveTodoDismissed () {
      try {
        localStorage.setItem('ssrms_admin_todo_dismissed', JSON.stringify(this.todoDismissed || {}))
      } catch (e) {
        //
      }
    },
    cleanTodoDismissed () {
      const now = Date.now()
      const map = this.todoDismissed || {}
      let changed = false
      Object.keys(map).forEach(k => {
        if (!map[k] || Number(map[k]) <= now) {
          delete map[k]
          changed = true
        }
      })
      if (changed) this.saveTodoDismissed()
    },
    isTodoDismissed (id) {
      this.cleanTodoDismissed()
      const t = Number((this.todoDismissed || {})[id] || 0)
      return t > Date.now()
    },
    dismissTodo (id, hours) {
      const h = Number(hours ?? this.todoHideHours ?? 6)
      const ms = Math.max(0, h) * 60 * 60 * 1000
      this.todoDismissed = this.todoDismissed || {}
      this.todoDismissed[id] = Date.now() + ms
      this.saveTodoDismissed()
    },
    nowStr () {
      const d = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },

    async loadAdminHomeTodos () {
      // 管理员首页只需要“统计”就能生成待办：投诉处理 + 违约处理
      await Promise.all([
        this.loadAdminFeedbackStats(),
        this.loadAdminReservationStatsForHome()
      ])
      this.rebuildTodos()
      if (this.currentPage === 'admin-home') this.scheduleTableLayout('todoTableRef')
    },

    async loadAdminReservationStatsForHome () {
      // 轻量拉取：只用 stats，不需要 records
      try {
        const res = await this.$axios.get('/reservation/admin/page', { params: { page: 1, size: 1 } })
        if (res && Number(res.code) === 200) {
          const payload = res.data || {}
          this.reservationStats = payload.stats || this.reservationStats
        }
      } catch (e) {
        console.error(e)
      }
    },

    rebuildTodos () {
      const items = []
      const now = this.nowStr()

      // 1) 投诉/建议待处理（来自 feedback stats）
      const pending = Number(this.fbAdminStats?.pending || 0)
      const processing = Number(this.fbAdminStats?.processing || 0)
      const fbTotal = pending + processing
      if (fbTotal > 0) {
        items.push({
          id: 'todo_feedback',
          type: 'complaint',
          content: `有 ${fbTotal} 条反馈待处理（待处理 ${pending}，处理中 ${processing}）`,
          from: '投诉处理',
          time: now,
          priority: pending > 0 ? 'high' : 'medium'
        })
      }

      // 2) 违约记录待确认/处理（来自 reservation stats.violation）
      const vio = Number(this.reservationStats?.violation || 0)
      if (vio > 0) {
        items.push({
          id: 'todo_violation',
          type: 'violation',
          content: `有 ${vio} 条违约/未签到记录待处理（可补录签到或标记违约）`,
          from: '预约管理',
          time: now,
          priority: vio >= 5 ? 'high' : 'medium'
        })
      }

      // 过滤：本地标记完成（隐藏）
      this.todos = items.filter(it => !this.isTodoDismissed(it.id))
    },

    handleTodoGo (row) {
      // 快速入口：跳转到对应模块，并预填筛选条件
      if (row.type === 'complaint') {
        // 默认只看待处理：fbAdminOnlyPending 本来就是 true
        return this.emitChange('admin-complaints')
      }
      if (row.type === 'violation') {
        // 直接跳预约管理，并筛选“违约”
        this.reservationFilters.status = 'violation'
        return this.emitChange('admin-reservations')
      }
      return this.emitChange('admin-home')
    },
    handleTodoDone (row) {
      // 仅隐藏本机待办，不会修改后端数据
      this.dismissTodo(row.id)
      this.todos = this.todos.filter(t => t.id !== row.id)
    },
    handleMarkAllDone () {
      // 一键隐藏
      (this.todos || []).forEach(t => this.dismissTodo(t.id))
      this.todos = []
    },

    /* ---------- Reservation (admin) ---------- */
    async loadReservationRoomOptions () {
      try {
        const res = await this.$axios.get('/room/all')
        if (res && Number(res.code) === 200 && Array.isArray(res.data)) {
          this.reservationRoomOptions = res.data.map(r => ({
            id: r.id,
            label: `${r.campus} · ${r.building} ${r.roomName}`
          }))
        }
      } catch (e) {
        console.error('loadReservationRoomOptions failed:', e)
      }
    },

    async loadAdminReservations (resetPage = false) {
      if (resetPage) this.reservationPage = 1
      this.reservationLoading = true
      try {
        const keyword = (this.reservationFilters.keyword || '').trim()
        const params = {
          keyword: keyword || undefined,
          roomId: this.reservationFilters.roomId || undefined,
          date: this.reservationFilters.date || undefined,
          status: this.reservationFilters.status || undefined,
          page: this.reservationPage,
          size: this.reservationPageSize
        }

        const res = await this.$axios.get('/reservation/admin/page', { params })
        if (res && Number(res.code) === 200) {
          const payload = res.data || {}
          this.reservations = Array.isArray(payload.records) ? payload.records : []
          this.reservationStats = payload.stats || { total: 0, booked: 0, signed: 0, canceled: 0, violation: 0 }
          this.reservationTotal = Number(res.total ?? this.reservations.length)
          this.reservationSelection = []
        } else {
          ElMessage.error(res?.msg || '预约数据加载失败')
        }
      } catch (e) {
        console.error('loadAdminReservations failed:', e)
        ElMessage.error('预约数据加载失败，请检查后端服务或网络连接')
      } finally {
        this.reservationLoading = false
        if (this.currentPage === 'admin-reservations') this.scheduleTableLayout('reservationTableRef')
      }
    },

    handleReservationRefresh () {
      // 不重置分页，只做一次刷新
      this.loadAdminReservations(false)
    },

    handleReservationSearch () {
      // 查询：回到第一页
      this.loadAdminReservations(true)
    },

    resetReservationFilters () {
      this.reservationFilters = { keyword: '', roomId: null, date: '', status: '' }
      this.loadAdminReservations(true)
    },

    handleSelectionChange (val) {
      this.reservationSelection = val || []
    },

    handlePageChange (page) {
      this.reservationPage = page
      this.loadAdminReservations(false)
    },

    handlePageSizeChange (size) {
      this.reservationPageSize = size
      this.reservationPage = 1
      this.loadAdminReservations(false)
    },

    isCancelledStatus (status) {
      return status === 'cancelled' || status === 'cancel_overdue'
    },

    getStatusTagType (status) {
      if (status === 'reserved') return 'info'
      if (status === 'checked_in') return 'success'
      if (status === 'late') return 'warning'
      if (status === 'no_show') return 'danger'
      if (status === 'cancelled' || status === 'cancel_overdue') return 'warning'
      return ''
    },

    getStatusText (status) {
      if (status === 'reserved') return '已预约'
      if (status === 'checked_in') return '已签到'
      if (status === 'late') return '迟到'
      if (status === 'no_show') return '未签到'
      if (status === 'cancelled') return '已取消'
      if (status === 'cancel_overdue') return '逾期取消'
      return status
    },

    async handleSign (row) {
      const it = row || {}
      if (!it.id) return ElMessage.error('缺少预约 id')
      if (it.status !== 'no_show' && it.status !== 'late') {
        return ElMessage.error('仅支持对【未签到/迟到】记录补录签到')
      }

      try {
        await ElMessageBox.confirm(`确定为预约「${it.code || it.reservationNo || it.id}」补录签到吗？`, '确认补录签到', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post(`/reservation/admin/checkin/${it.id}`)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '补录签到成功')
          await this.loadAdminReservations(false)
        } else {
          ElMessage.error(res?.msg || '补录签到失败')
        }
      } catch (e) {
        ElMessage.error('补录签到失败，请检查后端服务或网络连接')
      }
    },

    async handleCancel (row) {
      const it = row || {}
      if (!it.id) return ElMessage.error('缺少预约 id')
      try {
        await ElMessageBox.confirm(`确定取消预约「${it.code || it.reservationNo || it.id}」吗？`, '确认取消预约', {
          type: 'warning',
          confirmButtonText: '取消预约',
          cancelButtonText: '返回'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post(`/reservation/admin/cancel/${it.id}`)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '取消成功')
          await this.loadAdminReservations(false)
        } else {
          ElMessage.error(res?.msg || '取消失败')
        }
      } catch (e) {
        ElMessage.error('取消失败，请检查后端服务或网络连接')
      }
    },

    async handleMarkViolation (row) {
      const it = row || {}
      if (!it.id) return ElMessage.error('缺少预约 id')
      try {
        await ElMessageBox.confirm(`确定将预约「${it.code || it.reservationNo || it.id}」标记为违约吗？`, '确认标记违约', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post(`/reservation/admin/markViolation/${it.id}`)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '标记违约成功')
          await this.loadAdminReservations(false)
        } else {
          ElMessage.error(res?.msg || '标记违约失败')
        }
      } catch (e) {
        ElMessage.error('标记违约失败，请检查后端服务或网络连接')
      }
    },

    handleViewDetail (row) {
      const it = row || {}
      const rmk = (it && (it['remark'] ?? it['note'] ?? it['comment'])) || '—'
      const lines = [
        `预约编号：${it.code || it.reservationNo || '—'}`,
        `学生：${it.studentName || '—'}（${it.studentNo || '—'}）`,
        `自习室：${it.room || '—'}`,
        `座位：${it.seatNo || '—'}`,
        `日期：${it.date || '—'}`,
        `时间段：${it.timeRange || '—'}`,
        `状态：${this.getStatusText(it.status)}`,
        `备注：${rmk}`
      ]

      ElMessageBox.alert(lines.join('<br/>'), '预约详情', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      })
    },

    async handleBatchSign () {
      const ids = (this.reservationSelection || []).map(x => x.id).filter(Boolean)
      if (!ids.length) return
      if (!this.canBatchCheckin) return ElMessage.error('请仅选择【未签到/迟到】记录进行批量补录签到')


      try {
        await ElMessageBox.confirm(`确定批量补录签到（${ids.length} 条）吗？`, '批量补录签到', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post('/reservation/admin/batchCheckin', { ids })
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '批量补录签到完成')
          await this.loadAdminReservations(false)
        } else {
          ElMessage.error(res?.msg || '批量补录签到失败')
        }
      } catch (e) {
        ElMessage.error('批量补录签到失败，请检查后端服务或网络连接')
      }
    },

    async handleBatchCancel () {
      const ids = (this.reservationSelection || []).map(x => x.id).filter(Boolean)
      if (!ids.length) return
      if (!this.canBatchCancel) return ElMessage.error('请仅选择【已预约】记录进行批量取消')


      try {
        await ElMessageBox.confirm(`确定批量取消预约（${ids.length} 条）吗？`, '批量取消预约', {
          type: 'warning',
          confirmButtonText: '取消预约',
          cancelButtonText: '返回'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post('/reservation/admin/batchCancel', { ids })
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '批量取消完成')
          await this.loadAdminReservations(false)
        } else {
          ElMessage.error(res?.msg || '批量取消失败')
        }
      } catch (e) {
        ElMessage.error('批量取消失败，请检查后端服务或网络连接')
      }
    },

    /* user methods */
    async loadAdminUsers (resetPage = false) {
      if (resetPage) this.userPage = 1

      const params = {
        page: this.userPage,
        size: this.userPageSize
      }
      const k = (this.userFilters.keyword || '').trim()
      if (k) params.keyword = k
      if (this.userFilters.roleId !== '' && this.userFilters.roleId !== null && this.userFilters.roleId !== undefined) {
        params.roleId = this.userFilters.roleId
      }
      if (this.userFilters.status !== '' && this.userFilters.status !== null && this.userFilters.status !== undefined) {
        params.status = this.userFilters.status
      }
      if (this.userFilters.onlyAbnormal) {
        params.onlyAbnormal = true
      }

      this.userLoading = true
      try {
        const res = await this.$axios.get('/user/admin/page', { params })
        if (res && Number(res.code) === 200) {
          const data = res.data || {}
          this.users = Array.isArray(data.records) ? data.records : []
          this.userStats = data.stats || { total: 0, normal: 0, warning: 0, blacklist: 0 }
          const total = (res.total ?? data.total)
          this.userTotal = Number.isFinite(Number(total)) ? Number(total) : this.users.length
        } else {
          ElMessage.error(res?.msg || '用户数据加载失败')
        }
      } catch (e) {
        ElMessage.error('用户数据加载失败，请检查后端服务或网络连接')
      } finally {
        this.userLoading = false
        if (this.currentPage === 'admin-users') this.scheduleTableLayout('userTableRef')
      }
    },

    handleUserSearch () {
      this.loadAdminUsers(true)
    },
    resetUserFilters () {
      this.userFilters = { keyword: '', roleId: '', status: '', onlyAbnormal: false }
      this.loadAdminUsers(true)
    },
    handleUserSelectionChange (val) {
      this.userSelection = val || []
    },
    handleUserPageChange (page) {
      this.userPage = page
      this.loadAdminUsers(false)
    },
    handleUserPageSizeChange (size) {
      this.userPageSize = size
      this.loadAdminUsers(true)
    },
    getUserStatusTagType (status) {
      const s = Number(status)
      if (s === 0) return 'success'
      if (s === 1) return 'warning'
      if (s === 2) return 'danger'
      return ''
    },
    getUserStatusText (status) {
      const s = Number(status)
      if (s === 0) return '正常'
      if (s === 1) return '预警'
      if (s === 2) return '黑名单'
      return String(status)
    },

    userRowSelectable (row) {
      // 管理员不允许加入/解除黑名单，也不参与批量操作
      return Number(row?.roleId) !== 0
    },

    getViolationTypeText (vType) {
      if (vType === 'no_show') return '未签到'
      if (vType === 'late') return '迟到'
      if (vType === 'cancel') return '取消'
      return vType || '—'
    },

    openCreateAdminDialog () {
      this.createAdminVisible = true
      this.$nextTick(() => {
        try {
          this.$refs.createAdminFormRef && this.$refs.createAdminFormRef.clearValidate && this.$refs.createAdminFormRef.clearValidate()
        } catch (e) {
          //
        }
      })
    },

    async submitCreateAdmin () {
      const formRef = this.$refs.createAdminFormRef
      if (!formRef) return
      try {
        await formRef.validate()
      } catch (e) {
        return
      }

      this.createAdminLoading = true
      try {
        const payload = {
          account: (this.createAdminForm.account || '').trim(),
          name: (this.createAdminForm.name || '').trim(),
          password: this.createAdminForm.password
        }
        const res = await this.$axios.post('/user/admin/createAdmin', payload)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '创建成功')
          this.createAdminVisible = false
          this.createAdminForm = { account: '', name: '', password: '' }
          await this.loadAdminUsers(false)
        } else {
          ElMessage.error(res?.msg || '创建失败')
        }
      } catch (e) {
        ElMessage.error('创建失败，请检查后端服务或网络连接')
      } finally {
        this.createAdminLoading = false
      }
    },
    async handleLockUser (row) {
      if (Number(row?.roleId) === 0) return ElMessage.warning('管理员账号不支持加入黑名单')
      const id = row && row.id
      if (!id) return ElMessage.error('缺少用户 id')
      try {
        await ElMessageBox.confirm(`确定将「${row.name || row.studentNo || id}」加入黑名单吗？`, '加入黑名单', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post(`/user/admin/blacklist/${id}`)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '已加入黑名单')
          await this.loadAdminUsers(false)
        } else {
          ElMessage.error(res?.msg || '操作失败')
        }
      } catch (e) {
        ElMessage.error('操作失败，请检查后端服务或网络连接')
      }
    },
    async handleUnlockUser (row) {
      if (Number(row?.roleId) === 0) return ElMessage.warning('管理员账号不支持解除黑名单')
      const id = row && row.id
      if (!id) return ElMessage.error('缺少用户 id')
      try {
        await ElMessageBox.confirm(`确定解除「${row.name || row.studentNo || id}」的黑名单吗？解除后将根据当前信用分自动判定是否为预警。`, '解除黑名单', {
          type: 'warning',
          confirmButtonText: '解除',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post(`/user/admin/unblacklist/${id}`)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '已解除黑名单')
          await this.loadAdminUsers(false)
        } else {
          ElMessage.error(res?.msg || '操作失败')
        }
      } catch (e) {
        ElMessage.error('操作失败，请检查后端服务或网络连接')
      }
    },
    async handleResetPassword (row) {
      if (Number(row?.roleId) === 0) return ElMessage.warning('管理员账号不支持重置密码')

      const id = row && row.id
      if (!id) return ElMessage.error('缺少用户 id')
      try {
        await ElMessageBox.confirm(`确定重置「${row.name || row.studentNo || id}」的密码吗？重置后默认密码为 123456。`, '重置密码', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post(`/user/admin/resetPassword/${id}`)
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '密码已重置为 123456')
        } else {
          ElMessage.error(res?.msg || '重置失败')
        }
      } catch (e) {
        ElMessage.error('重置失败，请检查后端服务或网络连接')
      }
    },
    async handleViewUserDetail (row) {
      const id = row && row.id
      if (!id) return ElMessage.error('缺少用户 id')
      this.userDetailVisible = true
      this.userDetailLoading = true
      this.userDetail = { user: null, violationCount: 0, recentViolations: [] }
      try {
        const res = await this.$axios.get(`/user/admin/detail/${id}`)
        if (res && Number(res.code) === 200) {
          const d = res.data || {}
          this.userDetail = {
            user: d.user || null,
            violationCount: d.violationCount || 0,
            recentViolations: d.recentViolations || []
          }
        } else {
          ElMessage.error(res?.msg || '获取详情失败')
        }
      } catch (e) {
        ElMessage.error('获取详情失败，请检查后端服务或网络连接')
      } finally {
        this.userDetailLoading = false
        this.scheduleTableLayout('userViolationTableRef')
      }
    },
    async handleBatchLock () {
      const ids = (this.userSelection || []).filter(x => Number(x?.roleId) !== 0).map(x => x.id).filter(Boolean)
      if (!ids.length) return
      try {
        await ElMessageBox.confirm(`确定批量加入黑名单（${ids.length} 人）吗？`, '批量加入黑名单', {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post('/user/admin/batchBlacklist', { ids })
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '批量加入黑名单完成')
          this.userSelection = []
          await this.loadAdminUsers(false)
        } else {
          ElMessage.error(res?.msg || '批量操作失败')
        }
      } catch (e) {
        ElMessage.error('批量操作失败，请检查后端服务或网络连接')
      }
    },
    async handleBatchUnlock () {
      const ids = (this.userSelection || []).filter(x => Number(x?.roleId) !== 0).map(x => x.id).filter(Boolean)
      if (!ids.length) return
      try {
        await ElMessageBox.confirm(`确定批量解除黑名单（${ids.length} 人）吗？解除后将根据当前信用分自动判定是否为预警。`, '批量解除黑名单', {
          type: 'warning',
          confirmButtonText: '解除',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }

      try {
        const res = await this.$axios.post('/user/admin/batchUnblacklist', { ids })
        if (res && Number(res.code) === 200) {
          ElMessage.success(res?.msg || '批量解除完成')
          this.userSelection = []
          await this.loadAdminUsers(false)
        } else {
          ElMessage.error(res?.msg || '批量操作失败')
        }
      } catch (e) {
        ElMessage.error('批量操作失败，请检查后端服务或网络连接')
      }
    },


    /* seat methods */
    initSeatGrid () {
      const rows = 6
      const cols = 8
      const grid = []
      let id = 1
      for (let r = 0; r < rows; r++) {
        const row = { rowIndex: r, seats: [] }
        for (let c = 0; c < cols; c++) {
          row.seats.push({
            id: id++,
            row: r,
            col: c,
            label: `${r + 1}-${c + 1}`,
            status: 'free',
            selected: false
          })
        }
        grid.push(row)
      }
      grid[0].seats[0].status = 'occupied'
      grid[0].seats[1].status = 'occupied'
      grid[2].seats[3].status = 'disabled'
      grid[3].seats[4].status = 'disabled'
      this.seatGrid = grid
    },
    handleSelectRoom (row) {
      this.selectedRoomId = row.id
    },
    toggleRoomOpen (row) {
      row.status = row.status === 'open' ? 'closed' : 'open'
    },
    handleEditRoom (row) {
      console.log('编辑自习室配置（弹窗预留）', row)
    },
    syncRoomStatus (val) {
      const room = this.rooms.find(r => r.id === this.selectedRoomId)
      if (room) room.status = val ? 'open' : 'closed'
    },
    seatCellClass (seat) {
      return [`seat-status-${seat.status}`, seat.selected ? 'seat-selected' : '']
    },
    handleSeatClick (seat) {
      seat.selected = !seat.selected
      if (this.showDemoOccupied && seat.status === 'free' && Math.random() < 0.05) {
        seat.status = 'occupied'
      }
    },
    handleBatchDisable () {
      this.seatGrid.forEach(row => {
        row.seats.forEach(seat => {
          if (seat.selected) {
            seat.status = 'disabled'
            seat.selected = false
          }
        })
      })
    },
    handleClearSelection () {
      this.seatGrid.forEach(row => {
        row.seats.forEach(seat => { seat.selected = false })
      })
    },

    /* ---------- admin feedback/complaints ---------- */
    onFbAdminFilterChange () {
      this.fbAdminPageNum = 1
      this.loadAdminFeedback(false)
      this.loadAdminFeedbackStats()
    },
    fbAdminPageChange (p) {
      this.fbAdminPageNum = Number(p || 1)
      this.loadAdminFeedback(false)
    },
    fbAdminStatusText (s) {
      const v = String(s || '')
      if (v === 'pending') return '待处理'
      if (v === 'processing') return '处理中'
      if (v === 'resolved') return '已解决'
      return v || '-'
    },
    fbAdminStatusTagType (s) {
      const v = String(s || '')
      if (v === 'pending') return 'warning'
      if (v === 'processing') return 'info'
      if (v === 'resolved') return 'success'
      return ''
    },
    fbAdminCategoryText (c) {
      const v = String(c || '')
      if (v === 'env') return '环境评价'
      if (v === 'service') return '服务评价'
      if (v === 'suggestion') return '建议'
      if (v === 'complaint') return '投诉'
      if (v === 'appeal') return '申诉'
      if (v === 'other') return '其他'
      return v || '-'
    },
    fbAdminCategoryTagType (c) {
      const v = String(c || '')
      if (v === 'complaint') return 'danger'
      if (v === 'appeal') return 'warning'
      if (v === 'suggestion') return 'success'
      if (v === 'env' || v === 'service') return 'info'
      return ''
    },
    formatYmdhm (dtStr) {
      if (!dtStr) return '-'
      const s = String(dtStr).replace('T', ' ')
      const parts = s.split(' ')
      const d = parts[0] || ''
      const t = (parts[1] || '').slice(0, 5)
      if (!d) return s
      const ds = d.split('-')
      if (ds.length >= 3) {
        const mmdd = `${String(ds[1]).padStart(2, '0')}-${String(ds[2]).padStart(2, '0')}`
        return t ? `${mmdd} ${t}` : mmdd
      }
      return s
    },
    async loadAdminFeedbackStats () {
      try {
        const res = await this.$axios.get('/feedback/admin/stats')
        if (res && res.code === 200) {
          const d = res.data || {}
          this.fbAdminStats = {
            pending: Number(d.pending || 0),
            processing: Number(d.processing || 0),
            resolved: Number(d.resolved || 0)
          }
          // 同步 Dashboard 待办数量（可选）
          this.metrics.pendingItems = this.fbAdminStats.pending + this.fbAdminStats.processing
        }
      } catch (e) {
        console.error(e)
      }
    },
    async loadAdminFeedback (resetPage = false) {
      if (resetPage) this.fbAdminPageNum = 1
      this.fbAdminLoading = true
      try {
        const params = {
          pageNum: this.fbAdminPageNum,
          pageSize: this.fbAdminPageSize
        }
        if (this.fbAdminOnlyPending) params.onlyPending = true
        if (this.fbAdminStatusFilter) params.status = this.fbAdminStatusFilter
        if (this.fbAdminCategoryFilter) params.category = this.fbAdminCategoryFilter
        if (this.fbAdminKeyword) params.keyword = this.fbAdminKeyword

        const res = await this.$axios.get('/feedback/admin/page', { params })
        if (res && res.code === 200) {
          this.fbAdminList = Array.isArray(res.data) ? res.data : []
          this.fbAdminTotal = Number(res.total || 0)
        } else {
          this.fbAdminList = []
          this.fbAdminTotal = 0
        }
      } catch (e) {
        console.error(e)
        this.fbAdminList = []
        this.fbAdminTotal = 0
      } finally {
        this.fbAdminLoading = false
        if (this.currentPage === 'admin-complaints') this.scheduleTableLayout('fbAdminTableRef')
      }
    },
    openFbAdminDetail (row) {
      this.fbAdminDetail = row ? { ...row } : null
      const currentStatus = String((row && row.status) || 'pending')
      this.fbAdminHandleForm = {
        status: currentStatus === 'resolved' ? 'resolved' : 'processing',
        reply: (row && row.reply) ? String(row.reply) : '',
        cancelReservation: false
      }
      this.fbAdminDrawerVisible = true
      this.scheduleTableLayout('fbAdminTableRef')
    },
    async quickResolveFb (row) {
      if (!row || !row.id) return
      try {
        await ElMessageBox.confirm('确认将该反馈标记为“已解决”？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (_) {
        return
      }

      this.fbAdminSubmitting = true
      try {
        const payload = {
          id: Number(row.id),
          status: 'resolved',
          reply: (row && row.reply) ? String(row.reply) : ''
        }
        const res = await this.$axios.post('/feedback/admin/handle', payload)
        if (res && res.code === 200) {
          ElMessage.success(res.msg || '已标记为已解决')
          this.loadAdminFeedback(false)
          this.loadAdminFeedbackStats()
        } else {
          ElMessage.error((res && res.msg) || '操作失败')
        }
      } catch (e) {
        console.error(e)
        ElMessage.error('请求失败')
      } finally {
        this.fbAdminSubmitting = false
      }
    },
    async submitFbAdminHandle () {
      if (!this.fbAdminDetail || !this.fbAdminDetail.id) return
      if (String(this.fbAdminHandleForm.status) === 'resolved' && !String(this.fbAdminHandleForm.reply || '').trim()) {
        try {
          await ElMessageBox.confirm('你将标记为“已解决”，但回复内容为空。继续提交？', '提示', {
            confirmButtonText: '继续',
            cancelButtonText: '返回填写',
            type: 'warning'
          })
        } catch (_) {
          return
        }
      }

      this.fbAdminSubmitting = true
      try {
        const payload = {
          id: Number(this.fbAdminDetail.id),
          status: this.fbAdminHandleForm.status,
          reply: this.fbAdminHandleForm.reply,
          cancelReservation: !!this.fbAdminHandleForm.cancelReservation
        }
        const res = await this.$axios.post('/feedback/admin/handle', payload)
        if (res && res.code === 200) {
          ElMessage.success(res.msg || '提交成功')
          this.fbAdminDrawerVisible = false
          this.fbAdminDetail = null
          this.loadAdminFeedback(false)
          this.loadAdminFeedbackStats()
        } else {
          ElMessage.error((res && res.msg) || '提交失败')
        }
      } catch (e) {
        console.error(e)
        ElMessage.error('请求失败')
      } finally {
        this.fbAdminSubmitting = false
      }
    },

    resetSeatRule () {
      this.seatRule = {
        allowCrossTime: false,
        maxHoursPerOrder: 4,
        maxOrdersPerDay: 2,
        autoReleaseNoSign: true,
        releaseAfterMinutes: 20,
        violationStrategy: 'day-ban',
        description: '本自习室需保持安静，迟到超过 20 分钟系统将自动释放座位。'
      }
    },

    buildTrendDesc (key, p) {
      // 兼容 0（可能是“暂无数据”，也可能是真的 0%，这里文案尽量中性）
      const pct = Number(p || 0)

      if (key === 'weekReservation') {
        if (pct === 0) return '本周暂无可统计的完成数据，或预约尚未产生完成记录。'
        if (pct >= 85) return '完成率很高，整体运行稳定。'
        if (pct >= 70) return '完成率良好，可关注高峰时段的波动。'
        if (pct >= 50) return '完成率一般，建议排查取消/未到的主要原因。'
        return '完成率偏低，建议加强提醒与规则引导。'
      }

      if (key === 'weekCheckin') {
        if (pct === 0) return '本周暂无可统计的签到数据，或签到记录尚未产生。'
        if (pct >= 85) return '签到率优秀，到馆秩序很不错。'
        if (pct >= 70) return '签到率正常，可在开场前推送提醒进一步提升。'
        if (pct >= 50) return '签到率偏低，建议强化到馆提醒与补签流程。'
        return '签到率较低，建议检查签到规则与现场执行情况。'
      }

      if (key === 'weekViolation') {
        if (pct === 0) return '本周暂无违约，秩序良好。'
        if (pct <= 3) return '违约率较低，保持现有提醒即可。'
        if (pct <= 8) return '违约率略高，可在高峰时段加强提醒。'
        if (pct <= 15) return '违约率偏高，建议结合信用分/黑名单规则重点处理。'
        return '违约率较高，建议重点排查未签到与迟到原因并加强管理。'
      }

      return ''
    }
  }
}
</script>

<style scoped>
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 通用卡片 */
.card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

/* ============ Dashboard styles ============ */
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
}

.welcome-left, .welcome-right { flex: 1.1; }

.welcome-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.welcome-subtitle {
  margin: 6px 0 8px;
  font-size: 13px;
  opacity: 0.95;
}

.welcome-meta {
  font-size: 12px;
  opacity: 0.95;
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-separator { opacity: 0.7; }

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-item {
  background: rgba(15, 23, 42, 0.16);
  border-radius: 12px;
  padding: 10px 12px;
}

.metric-label {
  font-size: 12px;
  opacity: 0.95;
}

.metric-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
}

.metric-value.warning { color: #fee2e2; }

.metric-unit { font-size: 12px; margin-left: 2px; }

.metric-desc {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.9;
}

.metric-progress { margin-top: 4px; }

.middle-row {
  display: flex;
  gap: 14px;
  align-items: stretch;
}

.trend-card { flex: 1.2; }
.notice-card { flex: 1; }

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.card-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.trend-extra {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-item {
  padding: 6px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.trend-item:last-child { border-bottom: none; }

.trend-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-item-title { font-size: 13px; color: #111827; }
.trend-item-value { font-size: 13px; font-weight: 600; color: #111827; }
.trend-progress { margin: 4px 0; }
.trend-item-desc { font-size: 12px; color: #6b7280; }

.notice-carousel { margin-top: 4px; }
.notice-item { display: flex; flex-direction: column; justify-content: center; }

.notice-tag-wrapper { display: flex; align-items: center; gap: 8px; }
.notice-time { font-size: 12px; color: #9ca3af; }

.notice-title { margin-top: 4px; font-size: 14px; font-weight: 600; color: #111827; }
.notice-desc { margin-top: 2px; font-size: 12px; color: #4b5563; }

.notice-footer {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.notice-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background-color: #22c55e;
}

.todo-table { margin-top: 4px; }
.dashboard-page,
.middle-row,
.welcome-left,
.welcome-right,
.card-header-row,
.card-header,
.filter-bar,
.filter-actions,
.reservation-header-right,
.user-header-right,
.reservation-toolbar,
.toolbar-left,
.toolbar-right,
.complaints-page,
.complaints-head-left,
.complaints-head-right,
.complaints-toolbar,
.tool-left,
.tool-right,
.fb-drawer,
.user-detail {
  min-width: 0;
}

.todo-table,
.reservation-table,
.user-table,
.fb-admin-table,
.ud-table {
  width: 100%;
}

.todo-table :deep(.el-scrollbar),
.reservation-table :deep(.el-scrollbar),
.user-table :deep(.el-scrollbar),
.fb-admin-table :deep(.el-scrollbar),
.ud-table :deep(.el-scrollbar) {
  width: auto;
}

.todo-table :deep(.el-table__body-wrapper),
.reservation-table :deep(.el-table__body-wrapper),
.user-table :deep(.el-table__body-wrapper),
.fb-admin-table :deep(.el-table__body-wrapper),
.ud-table :deep(.el-table__body-wrapper) {
  width: 100%;
}

.todo-table :deep(.cell),
.reservation-table :deep(.cell),
.user-table :deep(.cell),
.fb-admin-table :deep(.cell),
.ud-table :deep(.cell) {
  word-break: break-word;
}


.todo-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dashboard-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.dashboard-card { flex: 1; min-width: 280px; }

/* timeline */
.timeline-list { list-style: none; padding: 0; margin: 4px 0 0; }
.timeline-item { padding: 10px 0; border-bottom: 1px dashed #e5e7eb; }
.timeline-item:last-child { border-bottom: none; }

.timeline-main { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.timeline-text { font-size: 13px; color: #111827; }
.timeline-tag { font-size: 12px; padding: 2px 8px; border-radius: 999px; }
.tag-book { background-color: #eff6ff; color: #1d4ed8; }
.tag-sign { background-color: #ecfdf3; color: #15803d; }
.tag-cancel { background-color: #fef3c7; color: #b45309; }
.timeline-time { margin-top: 4px; font-size: 12px; color: #6b7280; }

/* quick actions */
.quick-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.quick-tip { margin-top: 8px; font-size: 12px; color: #6b7280; }

/* ============ Reservation/User shared styles ============ */
.reservation-card,
.user-card,
.complaints-head,
.complaints-body {
  border: 1px solid #e8eefc;
  border-radius: 24px;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
  min-width: 0;
}

.reservation-card,
.user-card {
  padding: 28px 28px 22px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef2ff;
}

.card-header .page-title {
  margin-bottom: 8px;
}

.card-header .page-subtitle {
  color: #64748b;
  line-height: 1.7;
}

.reservation-header-right,
.user-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f8fbff, #f8faff);
  border: 1px solid #e7eefb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.filter-item { min-width: 180px; }
.filter-input { flex: 2; }
.filter-select, .filter-date { flex: 1; }

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-prefix-icon { font-size: 14px; }

.reservation-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 10px 0 18px;
}

.stat-card {
  min-width: 140px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff, #f8fbff);
  border: 1px solid #e5ecfa;
  padding: 16px 16px 14px;
  box-shadow: 0 10px 24px rgba(148, 163, 184, 0.10);
}

.stat-label { font-size: 13px; color: #64748b; }
.stat-value { margin-top: 8px; font-size: 30px; font-weight: 700; color: #0f172a; line-height: 1; }

.reservation-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 16px;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #edf2fb;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-right { font-size: 12px; color: #64748b; }
.toolbar-tip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
  font-size: 12px;
}

.reservation-table,
.user-table,
.fb-admin-table {
  border-radius: 12px;
  overflow: visible;
  border: none;
  background: #fff;
  box-shadow: 0 0 0 1px #dfe7f5;
}

.reservation-table :deep(.el-table__inner-wrapper),
.user-table :deep(.el-table__inner-wrapper),
.fb-admin-table :deep(.el-table__inner-wrapper),
.ud-table :deep(.el-table__inner-wrapper) {
  border-radius: 10px;
  overflow: hidden;
}

.reservation-table :deep(.el-table__inner-wrapper::before),
.user-table :deep(.el-table__inner-wrapper::before),
.fb-admin-table :deep(.el-table__inner-wrapper::before),
.ud-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.reservation-table :deep(.el-table__header-wrapper th),
.user-table :deep(.el-table__header-wrapper th),
.fb-admin-table :deep(.el-table__header-wrapper th) {
  background: #f8fbff !important;
  color: #475569;
  font-weight: 700;
}

.reservation-table :deep(.el-table__row td),
.user-table :deep(.el-table__row td),
.fb-admin-table :deep(.el-table__row td) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.reservation-table :deep(.el-table__header-wrapper th:first-child),
.user-table :deep(.el-table__header-wrapper th:first-child),
.fb-admin-table :deep(.el-table__header-wrapper th:first-child) {
  border-top-left-radius: 14px;
}

.reservation-table :deep(.el-table__header-wrapper th:last-child),
.user-table :deep(.el-table__header-wrapper th:last-child),
.fb-admin-table :deep(.el-table__header-wrapper th:last-child) {
  border-top-right-radius: 14px;
}

.reservation-table :deep(.el-table__body-wrapper tr:last-child td:first-child),
.user-table :deep(.el-table__body-wrapper tr:last-child td:first-child),
.fb-admin-table :deep(.el-table__body-wrapper tr:last-child td:first-child) {
  border-bottom-left-radius: 14px;
}

.reservation-table :deep(.el-table__body-wrapper tr:last-child td:last-child),
.user-table :deep(.el-table__body-wrapper tr:last-child td:last-child),
.fb-admin-table :deep(.el-table__body-wrapper tr:last-child td:last-child) {
  border-bottom-right-radius: 14px;
}

.student-cell { display: flex; flex-direction: column; }
.student-name { font-size: 14px; color: #111827; font-weight: 700; }
.student-no { font-size: 12px; color: #6b7280; margin-top: 4px; }

.remark-text { font-size: 12px; color: #4b5563; }

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.el-dropdown-link { font-size: 13px; color: #2563eb; cursor: pointer; }
.disabled-text { font-size: 12px; color: #9ca3af; }

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* user special */
.user-name-cell { display: flex; flex-direction: column; }
.user-name-main { font-size: 13px; color: #111827; }

.user-role-tag {
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  background-color: #eff6ff;
  color: #1d4ed8;
}

.user-role-tag.teacher { background-color: #ecfdf3; color: #15803d; }
.user-role-tag.admin { background-color: #fef3c7; color: #b45309; }

.user-extra { font-size: 12px; color: #6b7280; }

.credit-score { font-size: 13px; font-weight: 600; color: #111827; }
.credit-low { color: #b91c1c; }
.credit-high { color: #15803d; }

/* ============ Seat Manage styles ============ */
.seat-manage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}


.seat-head-card {
  padding: 14px 18px;
}


.seat-head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.seat-head-left {
  flex: 1;
  min-width: 260px;
}

.seat-summary-box {
  border-radius: 16px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #eef6ff, #ffffff);
  border: 1px solid #e6eef9;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

.seat-summary-inline {
  width: 420px;
  padding: 12px 14px;
  margin-left: auto;
}
.summary-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.25);
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
}

.summary-main { display: flex; flex-direction: column; gap: 6px; }

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
}

.summary-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}


.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.summary-label {
  font-size: 11px;
  color: #64748b;
}

.summary-value {
  font-size: 13px;
  color: #0f172a;
  font-weight: 700;
  text-align: right;
  line-height: 1.2;
}

.mini-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #374151;
}

.mini-pill.on {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.seat-filter-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.seat-filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.seat-filter-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.seat-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.top-select { min-width: 180px; }

.seat-filter-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.seat-layout {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.seat-left-card {
  flex: 0 0 40%;
  max-width: 440px;
}

.left-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.left-title { margin: 0; font-size: 15px; font-weight: 700; color: #111827; }
.left-subtitle { margin: 4px 0 0; font-size: 12px; color: #6b7280; }

.left-filter {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 6px;
}

.capacity-radio { width: 100%; }

.room-list-wrap {
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: linear-gradient(135deg, #ffffff, #fbfdff);
  overflow: hidden;
}

.room-list {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.room-item {
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 12px 12px;
  background: #ffffff;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px 12px;
  cursor: pointer;
  transition: transform .12s ease, box-shadow .12s ease, border-color .12s ease;
}

.room-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
  border-color: #dbeafe;
}

.room-item.active {
  border-color: #c7d2fe;
  box-shadow: 0 12px 26px rgba(99, 102, 241, 0.10);
  background: linear-gradient(135deg, #ffffff, #eef2ff);
}

.room-item.closed {
  opacity: 0.88;
}

.room-item-main { min-width: 0; }
.room-item-title { font-size: 14px; font-weight: 700; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.room-item-sub { margin-top: 4px; font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.room-item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  white-space: nowrap;
}

.room-item-usage {
  font-size: 12px;
  color: #111827;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f3f4f6;
}

.room-item-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.room-empty {
  padding: 18px 10px;
  text-align: center;
  color: #6b7280;
  font-size: 13px;
}

.seat-right { flex: 1; }
.seat-right-card { width: 100%; }

.right-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
  gap: 16px;
}

.right-title { margin: 0; font-size: 16px; font-weight: 800; color: #111827; }
.right-subtitle { margin: 6px 0 0; font-size: 12px; color: #6b7280; }

.right-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.seat-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 10px 0 12px;
}

.seat-stats .stat-card {
  min-width: 0;
  padding: 10px 12px;
  background: linear-gradient(135deg, #ffffff, #f8fafc);
  border: 1px solid #eef2f7;
}

@media (max-width: 1100px) {
  .seat-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .seat-summary-inline {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 520px) {
  .seat-stats {
    grid-template-columns: 1fr;
  }
}

.seat-stats-compact { margin-bottom: 6px; }

.seat-tabs { margin-top: 4px; }

.layout-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 8px;
  flex-wrap: wrap;
  gap: 10px;
}

.layout-toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.layout-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.seat-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 4px 0 10px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  display: inline-block;
}

.legend-free { background-color: #60a5fa; }
.legend-occupied { background-color: #f87171; }
.legend-disabled { background-color: #9ca3af; }
.legend-selected { background-color: #a78bfa; }

.seat-grid {
  width: 100%;
  border-radius: 16px;
  background: linear-gradient(135deg, #f3f4f6, #fbfdff);
  padding: 12px;
  box-sizing: border-box;
  border: 1px solid #eef2f7;
}

.seat-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  gap: 10px;
}

.seat-row:last-child { margin-bottom: 0; }

.seat-row-label {
  width: 76px;
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
}

.seat-row-seats {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 8px;
}

.seat-cell {
  height: 34px;
  border-radius: 12px;
  box-sizing: border-box;
  cursor: pointer;
  position: relative;
  transition: transform .1s ease, box-shadow .12s ease, background-color .12s ease;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 0;
}

.seat-cell:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.seat-status-free { border-color: #dbeafe; background: #ffffff; }
.seat-status-occupied { border-color: #fecaca; background: #fff1f2; }
.seat-status-disabled { border-color: #e5e7eb; background: #f3f4f6; }

.seat-selected {
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.55);
  border-color: #c7d2fe;
  background: #eef2ff;
}

.seat-no {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  user-select: none;
}

.seat-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #6b7280;
}

.rule-form { margin-top: 6px; }

.item-desc {
  margin-left: 6px;
  font-size: 12px;
  color: #6b7280;
}

.inline-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.seat-empty-state {
  margin-top: 18px;
  border-radius: 16px;
  padding: 18px 16px;
  border: 1px dashed #e5e7eb;
  background: #fafafa;
}

.empty-title {
  font-size: 14px;
  font-weight: 800;
  color: #111827;
}

.empty-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}
/* simple page */
.simple-tip { margin-top: 12px; font-size: 13px; color: #4b5563; }

/* responsive */
@media (max-width: 1024px) {
  .welcome-card { flex-direction: column; }
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .middle-row { flex-direction: column; }
}

@media (max-width: 980px) {
  .seat-layout { flex-direction: column; }
  .seat-left-card { flex: 1; max-width: 100%; }
}

@media (max-width: 900px) {
  .card-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .reservation-header-right, .user-header-right { width: 100%; justify-content: flex-start; }
  .filter-bar { flex-direction: column; }
  .filter-item, .filter-input, .filter-select, .filter-date { width: 100%; }
  .reservation-toolbar { flex-direction: column; align-items: flex-start; }
  .table-footer { justify-content: center; }
}

@media (max-width: 780px) {
  .seat-top-card { flex-direction: column; align-items: flex-start; }
  .seat-top-right { flex-wrap: wrap; }
  .right-header { flex-direction: column; align-items: flex-start; }
  .layout-toolbar { flex-direction: column; align-items: flex-start; }
}

.dashboard-card-full { flex: 1 1 100%; }

.activity-two-col {
  display: flex;
  align-items: stretch;
  margin-top: 4px;
}

.activity-col { flex: 1; }

.activity-divider {
  width: 1px;
  background-color: #e5e7eb;
  margin: 0 14px;
}

@media (max-width: 960px) {
  .activity-two-col { flex-direction: column; }
  .activity-divider { display: none; }
}


/* -------------------- admin notice (announcement) -------------------- */
.admin-notice-head-actions { display: flex; gap: 10px; align-items: center; }

.admin-notice-loading,
.admin-notice-empty {
  padding: 14px 10px;
  color: #6b7280;
  font-size: 13px;
}

.admin-notice-home-list { list-style: none; padding: 0; margin: 8px 0 0; display: flex; flex-direction: column; gap: 10px; }
.admin-notice-home-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  cursor: pointer;
  transition: transform .12s ease, box-shadow .12s ease, background-color .12s ease;
  background: #fff;
}
.admin-notice-home-item:hover { transform: translateY(-1px); box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06); background: #fbfdff; }

.admin-notice-badges { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; min-width: 240px; }
.admin-notice-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid #e5e7eb;
  color: #374151;
  background: #ffffff;
}
.admin-notice-pill.admin-top { border-color: #fde68a; background: #fffbeb; color: #92400e; }
.admin-notice-icon { font-size: 13px; }

.admin-notice-pill.type-RULE { border-color: #dbeafe; background: #eff6ff; color: #1d4ed8; }
.admin-notice-pill.type-ADJUSTMENT { border-color: #e0f2fe; background: #f0f9ff; color: #0369a1; }
.admin-notice-pill.type-EMERGENCY { border-color: #fee2e2; background: #fff1f2; color: #b91c1c; }
.admin-notice-pill.type-MAINTENANCE { border-color: #ede9fe; background: #f5f3ff; color: #6d28d9; }
.admin-notice-pill.type-EXAM { border-color: #dcfce7; background: #f0fdf4; color: #15803d; }
.admin-notice-pill.type-OTHER { border-color: #e5e7eb; background: #f9fafb; color: #374151; }

.admin-notice-pill.level-IMPORTANT { border-color: #fecaca; background: #fff1f2; color: #b91c1c; }
.admin-notice-pill.level-WARNING { border-color: #fde68a; background: #fffbeb; color: #92400e; }
.admin-notice-pill.level-INFO { border-color: #e5e7eb; background: #f9fafb; color: #374151; }

.admin-notice-main { flex: 1; min-width: 0; }
.admin-notice-title { font-size: 14px; font-weight: 700; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.admin-notice-snippet { margin-top: 4px; font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.admin-notice-meta { margin-top: 6px; font-size: 12px; color: #9ca3af; display: flex; align-items: center; gap: 8px; }
.admin-dot { opacity: .8; }
.admin-notice-right { color: #9ca3af; font-size: 22px; padding-left: 8px; }

.admin-notice-toolbar { display: flex; justify-content: space-between; gap: 14px; margin-bottom: 12px; flex-wrap: wrap; }
.admin-notice-filters { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.f-item { display: inline-flex; align-items: center; gap: 8px; }
.f-label { color: #6b7280; font-size: 12px; }
.admin-notice-toolbar-right { display: flex; align-items: center; gap: 12px; }
.admin-notice-count { font-size: 12px; color: #6b7280; }

.admin-notice-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.admin-notice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  cursor: pointer;
  background: #fff;
}
.admin-notice-item:hover { background: #fbfdff; }

.admin-notice-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.admin-notice-pagination { margin-top: 14px; display: flex; justify-content: flex-end; }

.admin-notice-detail-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.admin-notice-detail-right { display: flex; gap: 10px; }

.admin-notice-detail-card {
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 14px 14px;
  background: #fff;
}

.d-title { font-size: 18px; font-weight: 800; color: #111827; line-height: 1.35; }
.d-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
.d-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid #e5e7eb;
  color: #374151;
  background: #fff;
}
.d-tag.top { border-color: #fde68a; background: #fffbeb; color: #92400e; }
.d-tag.type-RULE { border-color: #dbeafe; background: #eff6ff; color: #1d4ed8; }
.d-tag.type-ADJUSTMENT { border-color: #e0f2fe; background: #f0f9ff; color: #0369a1; }
.d-tag.type-EMERGENCY { border-color: #fee2e2; background: #fff1f2; color: #b91c1c; }
.d-tag.type-MAINTENANCE { border-color: #ede9fe; background: #f5f3ff; color: #6d28d9; }
.d-tag.type-EXAM { border-color: #dcfce7; background: #f0fdf4; color: #15803d; }
.d-tag.type-OTHER { border-color: #e5e7eb; background: #f9fafb; color: #374151; }
.d-tag.level-IMPORTANT { border-color: #fecaca; background: #fff1f2; color: #b91c1c; }
.d-tag.level-WARNING { border-color: #fde68a; background: #fffbeb; color: #92400e; }
.d-tag.level-INFO { border-color: #e5e7eb; background: #f9fafb; color: #374151; }

.d-info { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 16px; }
.d-info-item { display: flex; gap: 10px; font-size: 13px; }
.d-info-item .k { color: #6b7280; min-width: 64px; }
.d-info-item .v { color: #111827; font-weight: 600; }

.d-content { white-space: pre-wrap; font-size: 14px; line-height: 1.7; color: #111827; }

.admin-notice-form :deep(.el-form-item) { margin-bottom: 14px; }

@media (max-width: 980px) {
  .admin-notice-badges { min-width: 200px; }
  .d-info { grid-template-columns: 1fr; }
}



/* ====== Admin Complaints ====== */
.complaints-page{display:flex;flex-direction:column;gap:14px;}
.complaints-head{
  display:flex;
  align-items:flex-start;
  justify-content:space-between;
  gap:14px;
  padding:24px 28px;
  background: linear-gradient(135deg, #ffffff, #f8fbff);
}
.complaints-head-left .page-title{margin:0;}
.complaints-head-left .page-subtitle{margin-top:8px;color:#64748b;line-height:1.7;}

.complaints-head-right{display:flex;align-items:center;gap:12px;}
.fb-stats{display:flex;gap:10px;}
.fb-stat{
  min-width:88px;
  border:1px solid #e6edf9;
  background:linear-gradient(135deg, #ffffff, #f8fbff);
  border-radius:18px;
  padding:12px 14px;
  text-align:center;
  box-shadow: 0 10px 24px rgba(148, 163, 184, 0.10);
}
.fb-stat-num{font-size:28px;font-weight:800;color:#111827;line-height:1;}
.fb-stat-label{font-size:12px;color:#6b7280;margin-top:8px;}

.complaints-body{
  padding:24px 28px 22px;
  background:#ffffff;
  overflow:hidden;
}
.complaints-toolbar{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:12px;
  margin-bottom:16px;
  padding:16px 18px;
  border-radius:18px;
  background:linear-gradient(135deg, #f8fbff, #f8faff);
  border:1px solid #e7eefb;
}
.tool-left{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}
.tool-right{display:flex;align-items:center;gap:10px;}

.fb-admin-table :deep(.el-table__cell){vertical-align:top;}
.fb-admin-table :deep(.el-table__header-wrapper),
.fb-admin-table :deep(.el-table__body-wrapper),
.fb-admin-table :deep(.el-table__footer-wrapper) {
  width: 100% !important;
}
.fb-user-name{font-weight:700;color:#111827;line-height:1.2;}
.fb-user-sub{font-size:12px;color:#6b7280;margin-top:3px;}
.fb-rel-main{font-weight:700;color:#111827;line-height:1.2;}
.fb-rel-sub{font-size:12px;color:#6b7280;margin-top:3px;}
.fb-muted{color:#9ca3af;}
.fb-row-actions{display:flex;gap:8px;justify-content:flex-end;}

.fb-admin-pagination{margin-top:16px;display:flex;justify-content:flex-end;}

.fb-admin-drawer :deep(.el-drawer__body){padding:16px;}
.fb-drawer{display:flex;flex-direction:column;gap:12px;}
.fb-drawer-head{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;}
.fb-drawer-title{font-size:16px;font-weight:900;color:#111827;}
.fb-drawer-sub{font-size:12px;color:#6b7280;margin-top:4px;}

.fb-drawer-card{border:1px solid #eef2f7;border-radius:14px;background:#fff;padding:12px;}
.fb-kv{display:grid;grid-template-columns:76px 1fr;gap:10px;padding:6px 0;border-bottom:1px dashed #eef2f7;}
.fb-kv:last-child{border-bottom:none;}
.fb-kv .k{font-size:12px;color:#6b7280;}
.fb-kv .v{font-size:13px;color:#111827;font-weight:600;}

.fb-content{margin-top:10px;}
.fb-content-title{font-size:12px;color:#6b7280;margin-bottom:6px;}
.fb-content-body{white-space:pre-wrap;font-size:13px;line-height:1.7;color:#111827;}

.fb-form-title{font-size:14px;font-weight:900;color:#111827;margin-bottom:8px;}
.fb-handle-actions{display:flex;gap:10px;justify-content:flex-end;}

@media (max-width: 980px){
  .complaints-head{flex-direction:column;align-items:stretch;}
  .complaints-head-right{justify-content:space-between;}
  .fb-stats{flex:1;justify-content:space-between;}
  .reservation-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}


/* ===== 用户详情抽屉 / 新增管理员 ===== */
.user-detail-drawer :deep(.el-drawer__body){padding:16px;}
.user-detail{display:flex;flex-direction:column;gap:14px;}
.ud-head{display:flex;gap:12px;align-items:center;}
.ud-avatar{width:44px;height:44px;border-radius:14px;background:#eef2ff;color:#3730a3;display:flex;align-items:center;justify-content:center;font-weight:900;font-size:18px;flex-shrink:0;}
.ud-meta{flex:1;min-width:0;}
.ud-name-line{display:flex;align-items:center;gap:8px;flex-wrap:wrap;}
.ud-name{font-size:16px;font-weight:900;color:#111827;}
.ud-tag{border-radius:999px;}
.ud-sub{margin-top:4px;font-size:12px;color:#6b7280;display:flex;align-items:center;gap:6px;flex-wrap:wrap;}
.ud-sub .dot{color:#cbd5e1;}
.ud-stats{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
.ud-stat{border:1px solid #eef2f7;border-radius:14px;padding:10px 12px;background:#fff;}
.ud-stat .k{font-size:12px;color:#6b7280;}
.ud-stat .v{margin-top:4px;font-size:18px;font-weight:900;color:#111827;}
.ud-desc :deep(.el-descriptions__label){width:92px;color:#6b7280;font-size:12px;}
.ud-desc :deep(.el-descriptions__content){color:#111827;font-weight:600;font-size:13px;}
.ud-muted{color:#9ca3af;font-weight:500;}
.ud-section-title{font-size:13px;font-weight:900;color:#111827;margin-bottom:8px;}
.ud-table :deep(.el-table__cell){vertical-align:top;}

</style>
