package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.entity.BizChecklistEntity;
import com.iris.back.business.checklist.model.entity.BizChecklistItemEntity;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.mapper.BizPlanMapper;
import com.iris.back.business.plan.model.entity.BizPlanEntity;
import com.iris.back.business.plan.model.entity.BizPlanItemEntity;
import com.iris.back.business.project.mapper.BizProjectArchiveMapper;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.mapper.BizProjectTaskMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.dto.ProjectMemberDto;
import com.iris.back.business.project.model.dto.ProjectTaskDto;
import com.iris.back.business.project.model.dto.ProjectTaskWorkOrderDto;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.entity.BizProjectArchiveEntity;
import com.iris.back.business.project.model.entity.BizProjectEntity;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskWorkOrderEntity;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.model.request.ProjectTaskAssignRequest;
import com.iris.back.business.project.model.request.ProjectUpsertRequest;
import com.iris.back.business.project.model.request.ProjectWorkOrderCreateRequest;
import com.iris.back.business.project.model.request.ProjectWorkOrderRiskAcceptRequest;
import com.iris.back.business.project.model.request.ProjectWorkOrderReturnRequest;
import com.iris.back.business.project.model.request.ProjectWorkOrderReviewRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final BizProjectMapper projectMapper;
  private final BizProjectArchiveMapper projectArchiveMapper;
  private final BizProjectMemberMapper projectMemberMapper;
  private final BizProjectTaskMapper projectTaskMapper;
  private final BizProjectTaskWorkOrderMapper projectTaskWorkOrderMapper;
  private final BizProjectRectificationMapper projectRectificationMapper;
  private final BizChecklistMapper checklistMapper;
  private final BizChecklistItemMapper checklistItemMapper;
  private final BizPlanItemMapper planItemMapper;
  private final BizPlanMapper planMapper;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;
  private final OmsClient omsClient;
  private final ObjectMapper objectMapper;
  private final ProjectOperationLogService operationLogService;

  public ProjectService(
      BizProjectMapper projectMapper,
      BizProjectArchiveMapper projectArchiveMapper,
      BizProjectMemberMapper projectMemberMapper,
      BizProjectTaskMapper projectTaskMapper,
      BizProjectTaskWorkOrderMapper projectTaskWorkOrderMapper,
      BizProjectRectificationMapper projectRectificationMapper,
      BizChecklistMapper checklistMapper,
      BizChecklistItemMapper checklistItemMapper,
      BizPlanItemMapper planItemMapper,
      BizPlanMapper planMapper,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator,
      OmsClient omsClient,
      ObjectMapper objectMapper,
      ProjectOperationLogService operationLogService
  ) {
    this.projectMapper = projectMapper;
    this.projectArchiveMapper = projectArchiveMapper;
    this.projectMemberMapper = projectMemberMapper;
    this.projectTaskMapper = projectTaskMapper;
    this.projectTaskWorkOrderMapper = projectTaskWorkOrderMapper;
    this.projectRectificationMapper = projectRectificationMapper;
    this.checklistMapper = checklistMapper;
    this.checklistItemMapper = checklistItemMapper;
    this.planItemMapper = planItemMapper;
    this.planMapper = planMapper;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
    this.omsClient = omsClient;
    this.objectMapper = objectMapper;
    this.operationLogService = operationLogService;
  }

  public PageResponse<ProjectDto> list(ProjectListQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ProjectListQuery safeQuery = query == null
        ? new ProjectListQuery(null, null, null, null, null, null, 1L, 10L)
        : query;
    boolean superAdmin = isSuperAdmin(principal);
    Set<Long> visibleProjectIds = Set.of();
    if (!superAdmin) {
      List<BizProjectMemberEntity> myMemberships = projectMemberMapper.selectList(
          new LambdaQueryWrapper<BizProjectMemberEntity>()
              .eq(BizProjectMemberEntity::getTenantId, principal.tenantId())
              .eq(BizProjectMemberEntity::getPersonnelId, principal.userId())
      );
      visibleProjectIds = myMemberships.stream()
          .map(BizProjectMemberEntity::getProjectId)
          .collect(Collectors.toSet());
      if (visibleProjectIds.isEmpty()) {
        return PageResponse.of(0, normalizedPage(safeQuery.page()), normalizedPageSize(safeQuery.pageSize()), List.of());
      }
    }

    List<BizProjectEntity> projects = projectMapper.selectList(new LambdaQueryWrapper<BizProjectEntity>()
        .eq(BizProjectEntity::getTenantId, principal.tenantId())
        .orderByDesc(BizProjectEntity::getUpdatedAt)
        .orderByDesc(BizProjectEntity::getId));
    Set<Long> finalVisibleProjectIds = visibleProjectIds;
    List<BizProjectEntity> filteredProjects = projects.stream()
        .filter(project -> superAdmin || finalVisibleProjectIds.contains(project.getId()))
        .filter(project -> matches(project, safeQuery))
        .toList();
    Map<Long, List<BizProjectMemberEntity>> membersByProjectId = loadMembers(principal.tenantId(), filteredProjects);
    Map<Long, List<BizProjectTaskEntity>> tasksByProjectId = loadTasks(principal.tenantId(), filteredProjects);
    List<ProjectDto> filtered = filteredProjects.stream()
        .map(project -> toDto(
            project,
            membersByProjectId.getOrDefault(project.getId(), List.of()),
            tasksByProjectId.getOrDefault(project.getId(), List.of())
        ))
        .toList();

    long pageNo = normalizedPage(safeQuery.page());
    long pageSize = normalizedPageSize(safeQuery.pageSize());
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
    return PageResponse.of(filtered.size(), pageNo, pageSize, filtered.subList(fromIndex, toIndex));
  }

  public ProjectDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectEntity project = requireProject(parseId(id, "PROJECT_ID_INVALID"), principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureCanView(project, members, principal);
    List<BizProjectTaskEntity> tasks = listTasks(principal.tenantId(), project.getId());
    List<BizProjectTaskWorkOrderEntity> workOrders = listProjectWorkOrders(principal.tenantId(), project.getId());
    return toDto(project, members, tasks, workOrders);
  }

  @Transactional
  public ProjectDto start(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectEntity project = requireProject(parseId(id, "PROJECT_ID_INVALID"), principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureLeader(project, principal);
    if (!"not_started".equals(project.getStatus())) {
      throw new BusinessException("PROJECT_START_STATUS_INVALID", "PROJECT_START_STATUS_INVALID");
    }
    project.setStatus("in_progress");
    project.setActualStartedAt(LocalDateTime.now());
    project.setUpdatedBy(principal.userId());
    projectMapper.updateById(project);
    recordProjectOperation(principal, project.getId(), null, null, "启动项目", "项目进入执行中");
    return toDto(project, members, listTasks(principal.tenantId(), project.getId()));
  }

  @Transactional
  public ProjectDto complete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectEntity project = requireProject(parseId(id, "PROJECT_ID_INVALID"), principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureLeader(project, principal);
    if (!"in_progress".equals(project.getStatus())) {
      throw new BusinessException("PROJECT_COMPLETE_STATUS_INVALID", "PROJECT_COMPLETE_STATUS_INVALID");
    }
    List<BizProjectTaskEntity> tasks = listTasks(principal.tenantId(), project.getId());
    boolean everyTaskHandled = !tasks.isEmpty() && tasks.stream()
        .allMatch(task -> "passed".equals(task.getStatus()) || "nonconforming".equals(task.getStatus()));
    if (!everyTaskHandled) {
      throw new BusinessException("PROJECT_TASKS_NOT_HANDLED", "PROJECT_TASKS_NOT_HANDLED");
    }
    project.setStatus("completed");
    project.setEndDate(LocalDate.now());
    project.setUpdatedBy(principal.userId());
    projectMapper.updateById(project);
    recordProjectOperation(principal, project.getId(), null, null, "完成项目", "所有检查项已完成审核");
    return toDto(project, members, tasks);
  }

  @Transactional
  public ProjectDto archive(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectEntity project = requireProject(parseId(id, "PROJECT_ID_INVALID"), principal.tenantId());
    ensureLeader(project, principal);
    if (!"completed".equals(project.getStatus())) {
      throw new BusinessException("PROJECT_ARCHIVE_STATUS_INVALID", "PROJECT_ARCHIVE_STATUS_INVALID");
    }
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    List<BizProjectTaskEntity> tasks = listTasks(principal.tenantId(), project.getId());
    List<BizProjectTaskWorkOrderEntity> workOrders = listProjectWorkOrders(principal.tenantId(), project.getId());
    List<BizProjectRectificationEntity> rectifications = listProjectRectifications(principal.tenantId(), project.getId());
    syncProjectWorkOrdersBeforeArchive(workOrders, principal);

    LocalDateTime archivedAt = LocalDateTime.now();
    String snapshotJson = buildProjectArchiveSnapshot(project, members, tasks, workOrders, rectifications, principal, archivedAt);
    BizProjectArchiveEntity archive = existingProjectArchive(principal.tenantId(), project.getId());
    boolean create = archive == null;
    if (create) {
      archive = new BizProjectArchiveEntity();
      archive.setId(nextId(archive));
      archive.setTenantId(principal.tenantId());
      archive.setProjectId(project.getId());
      archive.setDeleted(0);
      archive.setVersion(0L);
      archive.setCreatedBy(principal.userId());
    }
    archive.setProjectCode(project.getProjectCode());
    archive.setProjectName(project.getProjectName());
    archive.setArchiveDate(archivedAt);
    archive.setArchivedBy(principal.userId());
    archive.setArchivedByName(principal.username());
    archive.setStatus("active");
    archive.setTaskCount(tasks.size());
    archive.setWorkOrderCount(workOrders.size());
    archive.setRectificationCount(rectifications.size());
    archive.setDocumentCount(countArchiveDocuments(workOrders));
    archive.setSnapshotVersion("v1");
    archive.setSnapshotJson(snapshotJson);
    archive.setUpdatedBy(principal.userId());
    if (create) {
      projectArchiveMapper.insert(archive);
    } else {
      projectArchiveMapper.updateById(archive);
    }

    project.setStatus("archived");
    project.setArchiveStatus("completed");
    project.setArchiveStartedAt(archivedAt);
    project.setArchiveCompletedAt(archivedAt);
    project.setArchiveError(null);
    project.setUpdatedBy(principal.userId());
    projectMapper.updateById(project);
    recordProjectOperation(principal, project.getId(), null, null, "归档项目", "归档前已同步 OMS 工单快照");
    return toDto(project, members, tasks);
  }

  private void syncProjectWorkOrdersBeforeArchive(
      List<BizProjectTaskWorkOrderEntity> workOrders,
      CurrentUserPrincipal principal
  ) {
    for (BizProjectTaskWorkOrderEntity workOrder : workOrders) {
      String omsWorkOrderId = normalizeRequiredText(workOrder.getOmsWorkOrderId(), "PROJECT_WORK_ORDER_OMS_ID_REQUIRED");
      // 归档是最终留痕动作，生成快照前必须主动拉取 OMS 最新详情、状态、日志和附件，确保档案固定的是归档时刻的最新材料。
      syncWorkOrderFromOms(workOrder, principal, omsWorkOrderId);
      projectTaskWorkOrderMapper.updateById(workOrder);
    }
  }

  @Transactional
  public ProjectDto update(String id, ProjectUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectEntity project = requireProject(parseId(id, "PROJECT_ID_INVALID"), principal.tenantId());
    ensureLeader(project, principal);
    if ("archived".equals(project.getStatus())) {
      throw new BusinessException("PROJECT_ARCHIVED_EDIT_FORBIDDEN", "PROJECT_ARCHIVED_EDIT_FORBIDDEN");
    }
    applyFields(project, request, false);
    project.setUpdatedBy(principal.userId());
    projectMapper.updateById(project);
    List<BizProjectMemberEntity> members = replaceMembers(project.getId(), principal, request.members());
    recordProjectOperation(principal, project.getId(), null, null, "更新项目", "更新项目基础信息和成员");
    return toDto(project, members, listTasks(principal.tenantId(), project.getId()));
  }

  @Transactional
  public void delete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectEntity project = requireProject(parseId(id, "PROJECT_ID_INVALID"), principal.tenantId());
    ensureLeader(project, principal);
    if (!"not_started".equals(project.getStatus())) {
      throw new BusinessException("PROJECT_DELETE_STATUS_INVALID", "PROJECT_DELETE_STATUS_INVALID");
    }
    projectTaskMapper.delete(new LambdaQueryWrapper<BizProjectTaskEntity>()
        .eq(BizProjectTaskEntity::getTenantId, principal.tenantId())
        .eq(BizProjectTaskEntity::getProjectId, project.getId()));
    projectMemberMapper.hardDeleteByProject(principal.tenantId(), project.getId());
    projectMapper.deleteById(project.getId());
    recordProjectOperation(principal, project.getId(), null, null, "删除项目", "删除未启动项目");
  }

  public List<ProjectTaskWorkOrderDto> listTaskWorkOrders(String projectId, String taskId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureTaskWorkOrderAccess(project, task, principal, members);
    Map<Long, String> employeeNoByPersonnelId = employeeNoByPersonnelId(members);
    return nullToList(projectTaskWorkOrderMapper.selectList(
        new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
            .eq(BizProjectTaskWorkOrderEntity::getTenantId, principal.tenantId())
            .eq(BizProjectTaskWorkOrderEntity::getProjectId, project.getId())
            .eq(BizProjectTaskWorkOrderEntity::getTaskId, task.getId())
            .orderByDesc(BizProjectTaskWorkOrderEntity::getUpdatedAt)
            .orderByDesc(BizProjectTaskWorkOrderEntity::getId)
    )).stream().map(workOrder -> toWorkOrderDto(workOrder, employeeNoByPersonnelId)).toList();
  }

  @Transactional
  public ProjectDto assignTasks(String projectId, ProjectTaskAssignRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    ensureLeader(project, principal);
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    BizProjectMemberEntity assignee = requireAssignableTaskMember(
        members,
        parseId(normalizeRequiredText(request.assigneeId(), "PROJECT_TASK_ASSIGNEE_ID_REQUIRED"),
            "PROJECT_TASK_ASSIGNEE_ID_INVALID")
    );
    List<Long> taskIds = parseIds(request.taskIds(), "PROJECT_TASK_ID_INVALID");
    Long contactId = parseNullableId(request.contactId(), "PROJECT_TASK_CONTACT_ID_INVALID");
    String contactName = trimToNull(request.contactName());
    String assigneeName = normalizeRequiredText(request.assigneeName(), "PROJECT_TASK_ASSIGNEE_NAME_REQUIRED");
    List<BizProjectTaskEntity> assignedTasks = taskIds.stream()
        .map(taskId -> {
          BizProjectTaskEntity task = requireTask(taskId, parsedProjectId, principal.tenantId());
          task.setAssigneeId(assignee.getPersonnelId());
          task.setAssigneeName(assigneeName);
          task.setContactId(contactId);
          task.setContactName(contactName);
          if (task.getIssuedAt() == null) {
            task.setIssuedAt(LocalDateTime.now());
          }
          task.setUpdatedBy(principal.userId());
          projectTaskMapper.updateById(task);
          return task;
        })
        .toList();
    List<BizProjectTaskEntity> tasks = listTasks(principal.tenantId(), project.getId());
    recordProjectOperation(
        principal,
        project.getId(),
        null,
        null,
        "设置检查项负责人",
        "检查项负责人：" + assigneeName + "，数量：" + assignedTasks.size()
    );
    return toDto(project, members, tasks.isEmpty() ? assignedTasks : tasks);
  }

  @Transactional
  public List<ProjectTaskWorkOrderDto> createWorkOrders(
      String projectId,
      String taskId,
      ProjectWorkOrderCreateRequest request
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    ensureProjectInProgress(project);
    ensureTaskWorkOrderAccess(project, task, principal, listMembers(principal.tenantId(), project.getId()));

    List<ProjectWorkOrderCreateRequest.HandlerRequest> handlers = request.handlers();
    LocalDate issuedDate = parseNullableDate(request.issuedAt(), "PROJECT_WORK_ORDER_ISSUED_AT_INVALID");
    LocalDateTime issuedAt = issuedDate == null ? task.getIssuedAt() : issuedDate.atStartOfDay();
    List<OmsClient.OmsCreateCommand> commands = handlers.stream()
        .map(handler -> {
          String handlerEmployeeNo = normalizeRequiredText(
              handler.handlerEmployeeNo(),
              "PROJECT_WORK_ORDER_HANDLER_EMPLOYEE_NO_REQUIRED"
          );
          Long localWorkOrderId = nextId(new BizProjectTaskWorkOrderEntity());
          return new OmsClient.OmsCreateCommand(
              handler.handlerId(),
              handlerEmployeeNo,
              handler.handlerName(),
              trimToNull(request.title()) == null ? task.getTaskName() : request.title().trim(),
              trimToNull(request.description()) == null ? task.getTaskDescription() : request.description().trim(),
              task.getId() + ":" + handlerEmployeeNo + ":" + localWorkOrderId,
              localWorkOrderId
          );
        })
        .toList();
    // 先真实创建 OMS 工单，再落本地记录；本地保存 OMS 返回的工单号用于后续详情、日志、退回。
    Map<String, OmsClient.OmsCreateResult> resultByHandlerId = omsClient.createWorkOrders(toTaskDto(task), commands)
        .stream()
        .collect(Collectors.toMap(OmsClient.OmsCreateResult::handlerId, Function.identity(), (left, right) -> left));
    Map<String, BizProjectTaskWorkOrderEntity> existingByKey = nullToList(
        projectTaskWorkOrderMapper.selectList(new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
            .eq(BizProjectTaskWorkOrderEntity::getTenantId, principal.tenantId())
            .eq(BizProjectTaskWorkOrderEntity::getTaskId, task.getId()))
    ).stream().collect(Collectors.toMap(
        BizProjectTaskWorkOrderEntity::getIdempotencyKey,
        Function.identity(),
        (left, right) -> left
    ));

    List<BizProjectTaskWorkOrderEntity> workOrders = commands.stream()
        .map(command -> saveWorkOrder(project, task, principal, command, issuedAt, resultByHandlerId, existingByKey))
        .toList();
    if (!"in_progress".equals(task.getStatus())) {
      task.setStatus("in_progress");
      task.setUpdatedBy(principal.userId());
      projectTaskMapper.updateById(task);
    }
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        null,
        "创建OMS工单",
        "创建 OMS 工单数量：" + workOrders.size()
    );
    return workOrders.stream().map(this::toWorkOrderDto).toList();
  }

  @Transactional
  public ProjectTaskWorkOrderDto reviewWorkOrder(
      String projectId,
      String taskId,
      String workOrderId,
      ProjectWorkOrderReviewRequest request
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    Long parsedWorkOrderId = parseId(workOrderId, "PROJECT_WORK_ORDER_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureTaskWorkOrderAccess(project, task, principal, members);
    BizProjectTaskWorkOrderEntity workOrder = requireWorkOrder(
        parsedWorkOrderId,
        parsedProjectId,
        parsedTaskId,
        principal.tenantId()
    );
    if (!isWorkOrderReviewable(workOrder)) {
      throw new BusinessException("PROJECT_WORK_ORDER_NOT_REVIEWABLE", "PROJECT_WORK_ORDER_NOT_REVIEWABLE");
    }
    if (Objects.equals(workOrder.getReviewLocked(), 1)) {
      throw new BusinessException("PROJECT_WORK_ORDER_REVIEW_LOCKED", "PROJECT_WORK_ORDER_REVIEW_LOCKED");
    }

    String reviewStatus = normalizeReviewStatus(request.reviewStatus());
    workOrder.setIrisReviewStatus(reviewStatus);
    workOrder.setIrisReviewOpinion(trimToNull(request.opinion()));
    workOrder.setIrisReviewedAt(LocalDateTime.now());
    workOrder.setIrisReviewedBy(principal.userId());
    workOrder.setReviewLocked(1);
    workOrder.setUpdatedBy(principal.userId());
    // 审核只记录结论；不符合项后续由负责人二选一处置：生成整改单或承担风险。
    projectTaskWorkOrderMapper.updateById(workOrder);
    updateTaskStatusAfterWorkOrderReview(task, principal);
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        workOrder.getId(),
        "审核工单",
        "审核结果：" + reviewStatus + "，审核意见：" + nonBlank(request.opinion(), null, "无")
    );
    return toWorkOrderDto(workOrder, employeeNoByPersonnelId(members));
  }

  @Transactional
  public ProjectTaskWorkOrderDto refreshWorkOrder(String projectId, String taskId, String workOrderId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    Long parsedWorkOrderId = parseId(workOrderId, "PROJECT_WORK_ORDER_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureTaskWorkOrderAccess(project, task, principal, members);
    BizProjectTaskWorkOrderEntity workOrder = requireWorkOrder(
        parsedWorkOrderId,
        parsedProjectId,
        parsedTaskId,
        principal.tenantId()
    );
    String omsWorkOrderId = normalizeRequiredText(workOrder.getOmsWorkOrderId(), "PROJECT_WORK_ORDER_OMS_ID_REQUIRED");
    syncWorkOrderFromOms(workOrder, principal, omsWorkOrderId);
    projectTaskWorkOrderMapper.updateById(workOrder);
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        workOrder.getId(),
        "同步OMS工单",
        "OMS 工单号：" + omsWorkOrderId + "，当前状态：" + nonBlank(workOrder.getOmsStatusName(), workOrder.getOmsStatus(), "未知")
    );
    return toWorkOrderDto(workOrder, employeeNoByPersonnelId(members));
  }

  @Transactional
  public RectificationDto createWorkOrderRectification(
      String projectId,
      String taskId,
      String workOrderId
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    Long parsedWorkOrderId = parseId(workOrderId, "PROJECT_WORK_ORDER_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureTaskWorkOrderAccess(project, task, principal, members);
    BizProjectTaskWorkOrderEntity workOrder = requireWorkOrder(
        parsedWorkOrderId,
        parsedProjectId,
        parsedTaskId,
        principal.tenantId()
    );
    ensureNonconformityCanCreateRectification(workOrder);

    BizProjectRectificationEntity rectification = createRectification(project, task, workOrder, principal);
    projectRectificationMapper.insert(rectification);
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        workOrder.getId(),
        "生成整改单",
        "整改单：" + rectification.getRectificationCode()
    );
    return toRectificationDto(rectification);
  }

  @Transactional
  public ProjectTaskWorkOrderDto acceptWorkOrderRisk(
      String projectId,
      String taskId,
      String workOrderId,
      ProjectWorkOrderRiskAcceptRequest request
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    Long parsedWorkOrderId = parseId(workOrderId, "PROJECT_WORK_ORDER_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureTaskWorkOrderAccess(project, task, principal, members);
    BizProjectTaskWorkOrderEntity workOrder = requireWorkOrder(
        parsedWorkOrderId,
        parsedProjectId,
        parsedTaskId,
        principal.tenantId()
    );
    ensureNonconformityPendingDisposition(workOrder);
    // 承担风险表示不再发起整改；如果来源不符合项工单已经生成过整改单，不能再切换为风险承担。
    if (hasRectificationsForSourceWorkOrder(workOrder.getId(), principal.tenantId())) {
      throw new BusinessException(
          "PROJECT_WORK_ORDER_NONCONFORMITY_DISPOSED",
          "该工单已生成整改单，不能再承担风险"
      );
    }

    workOrder.setNonconformityDisposition("risk_accepted");
    workOrder.setRiskAcceptanceReason(normalizeRequiredText(request.reason(), "PROJECT_WORK_ORDER_RISK_REASON_REQUIRED"));
    workOrder.setRiskAcceptedAt(LocalDateTime.now());
    workOrder.setRiskAcceptedBy(principal.userId());
    workOrder.setUpdatedBy(principal.userId());
    projectTaskWorkOrderMapper.updateById(workOrder);
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        workOrder.getId(),
        "承担风险",
        "风险承担原因：" + request.reason()
    );
    return toWorkOrderDto(workOrder, employeeNoByPersonnelId(members));
  }

  @Transactional
  public ProjectTaskWorkOrderDto returnWorkOrder(
      String projectId,
      String taskId,
      String workOrderId,
      ProjectWorkOrderReturnRequest request
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    Long parsedWorkOrderId = parseId(workOrderId, "PROJECT_WORK_ORDER_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    List<BizProjectMemberEntity> members = listMembers(principal.tenantId(), project.getId());
    ensureTaskWorkOrderAccess(project, task, principal, members);
    BizProjectTaskWorkOrderEntity workOrder = requireWorkOrder(
        parsedWorkOrderId,
        parsedProjectId,
        parsedTaskId,
        principal.tenantId()
    );
    ensureWorkOrderNotReviewed(workOrder);
    String omsWorkOrderId = normalizeRequiredText(workOrder.getOmsWorkOrderId(), "PROJECT_WORK_ORDER_OMS_ID_REQUIRED");
    String reason = normalizeRequiredText(request.reason(), "PROJECT_WORK_ORDER_RETURN_REASON_REQUIRED");
    // 退回只允许 OMS 已完成的工单；状态口径以 OMS 返回值为准，内控侧不再做状态码翻译。
    if (!isOmsCompleted(workOrder)) {
      throw new BusinessException("PROJECT_WORK_ORDER_NOT_COMPLETED", "PROJECT_WORK_ORDER_NOT_COMPLETED");
    }
    // 退回动作以 OMS 为准；OMS 调用失败时直接抛错，避免内控侧状态和 OMS 状态不一致。
    omsClient.returnWorkOrder(omsWorkOrderId, reason);

    // 退回后解除内控侧复核锁定，允许后续在 OMS 重新处理完成后再次复核。
    workOrder.setIrisReviewStatus("returned");
    workOrder.setIrisReviewOpinion(reason);
    workOrder.setIrisReviewedAt(LocalDateTime.now());
    workOrder.setIrisReviewedBy(principal.userId());
    workOrder.setReviewLocked(0);
    workOrder.setRectificationId(null);
    workOrder.setNonconformityDisposition(null);
    workOrder.setRiskAcceptanceReason(null);
    workOrder.setRiskAcceptedAt(null);
    workOrder.setRiskAcceptedBy(null);
    workOrder.setUpdatedBy(principal.userId());
    // 退回成功后立即同步一次 OMS 详情和日志，让前端看到 OMS 最新状态。
    syncWorkOrderFromOms(workOrder, principal, omsWorkOrderId);
    projectTaskWorkOrderMapper.updateById(workOrder);
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        workOrder.getId(),
        "退回工单",
        "退回原因：" + reason
    );
    return toWorkOrderDto(workOrder, employeeNoByPersonnelId(members));
  }

  private void syncWorkOrderFromOms(
      BizProjectTaskWorkOrderEntity workOrder,
      CurrentUserPrincipal principal,
      String omsWorkOrderId
  ) {
    // 本地只保存 OMS 快照，不在内控侧自行推导 OMS 状态，避免两套状态口径不一致。
    OmsClient.OmsWorkOrderSnapshot snapshot = omsClient.getWorkOrder(omsWorkOrderId);
    List<OmsClient.OmsWorkOrderLogSnapshot> logs = omsClient.getWorkOrderLogs(omsWorkOrderId);
    List<OmsClient.OmsAttachmentSnapshot> attachments = omsClient.getWorkOrderAttachments(omsWorkOrderId);

    workOrder.setOmsStatus(snapshot.omsStatus());
    workOrder.setOmsStatusName(snapshot.omsStatusName());
    workOrder.setOmsResultSummary(snapshot.resultSummary());
    workOrder.setOmsDetailPayload(snapshot.payload());
    workOrder.setOmsLogPayload(writeJson(logs));
    workOrder.setOmsAttachmentPayload(writeJson(attachments));
    workOrder.setSyncStatus("synced");
    workOrder.setLastSyncedAt(LocalDateTime.now());
    workOrder.setSyncError(null);
    workOrder.setUpdatedBy(principal.userId());
    if (snapshot.reviewable() && workOrder.getCompletedAt() == null) {
      workOrder.setCompletedAt(LocalDateTime.now());
    }
  }

  private void recordProjectOperation(
      CurrentUserPrincipal principal,
      Long projectId,
      Long taskId,
      Long workOrderId,
      String action,
      String remark
  ) {
    // 项目操作日志用于审计追溯，记录关键业务动作，不能绕过主业务事务单独成功。
    operationLogService.recordProjectLog(principal, projectId, taskId, workOrderId, action, remark);
  }

  @Transactional
  public void deleteWorkOrder(String projectId, String taskId, String workOrderId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    Long parsedWorkOrderId = parseId(workOrderId, "PROJECT_WORK_ORDER_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    ensureTaskWorkOrderAccess(project, task, principal, listMembers(principal.tenantId(), project.getId()));
    BizProjectTaskWorkOrderEntity workOrder = requireWorkOrder(
        parsedWorkOrderId,
        parsedProjectId,
        parsedTaskId,
        principal.tenantId()
    );
    ensureWorkOrderNotReviewed(workOrder);
    projectTaskWorkOrderMapper.delete(new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
        .eq(BizProjectTaskWorkOrderEntity::getTenantId, principal.tenantId())
        .eq(BizProjectTaskWorkOrderEntity::getProjectId, parsedProjectId)
        .eq(BizProjectTaskWorkOrderEntity::getTaskId, parsedTaskId)
        .eq(BizProjectTaskWorkOrderEntity::getId, workOrder.getId()));
    recordProjectOperation(
        principal,
        project.getId(),
        task.getId(),
        workOrder.getId(),
        "删除OMS工单记录",
        "删除待处理工单记录"
    );
  }

  @Transactional
  public ProjectDto create(ProjectUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensurePlanCanGenerateProject(principal.tenantId(), request);
    List<Long> checklistIds = parseIds(request.checklistIds(), "PROJECT_CHECKLIST_ID_INVALID");
    List<BizChecklistEntity> checklists = loadChecklists(principal.tenantId(), checklistIds);
    List<BizChecklistItemEntity> checklistItems = loadChecklistItems(principal.tenantId(), checklistIds);
    BizPlanEntity linkedPlan = loadProjectPlan(principal.tenantId(), request.planId());
    checklistItems = resolveChecklistItemsForProject(request, checklistItems, linkedPlan);
    if (checklistItems.isEmpty()) {
      throw new BusinessException("PROJECT_CHECKLIST_ITEMS_REQUIRED", "project requires checklist items");
    }

    BizProjectEntity project = new BizProjectEntity();
    project.setId(nextId(project));
    project.setTenantId(principal.tenantId());
    applyFields(project, request, true);
    project.setStatus("not_started");
    project.setArchiveStatus("none");
    project.setDeleted(0);
    project.setVersion(0L);
    project.setCreatedBy(principal.userId());
    project.setUpdatedBy(principal.userId());
    projectMapper.insert(project);

    List<BizProjectMemberEntity> members = createMembers(project.getId(), principal, request.members());
    List<BizProjectTaskEntity> tasks = createTasks(project.getId(), principal, checklists, checklistItems);
    linkPlanItemsToProject(project, principal);
    recordProjectOperation(principal, project.getId(), null, null, "创建项目", "创建项目：" + project.getProjectName());
    return toDto(project, members, tasks);
  }

  private void ensurePlanCanGenerateProject(Long tenantId, ProjectUpsertRequest request) {
    Long planId = parseNullableId(request.planId(), "PROJECT_PLAN_ID_INVALID");
    if (planId == null || !"plan".equalsIgnoreCase(trimToNull(request.source()))) {
      return;
    }
    Long existingCount = projectMapper.selectCount(new LambdaQueryWrapper<BizProjectEntity>()
        .eq(BizProjectEntity::getTenantId, tenantId)
        .eq(BizProjectEntity::getPlanId, planId));
    if (existingCount != null && existingCount > 0) {
      throw new BusinessException("PROJECT_PLAN_ALREADY_GENERATED", "PROJECT_PLAN_ALREADY_GENERATED");
    }
  }

  private List<BizProjectMemberEntity> replaceMembers(
      Long projectId,
      CurrentUserPrincipal principal,
      List<ProjectUpsertRequest.ProjectMemberRequest> requests
  ) {
    projectMemberMapper.hardDeleteByProject(principal.tenantId(), projectId);
    return createMembers(projectId, principal, requests);
  }

  private void applyFields(BizProjectEntity project, ProjectUpsertRequest request, boolean create) {
    String code = trimToNull(request.code());
    if (code != null) {
      project.setProjectCode(code);
    } else if (create || trimToNull(project.getProjectCode()) == null) {
      project.setProjectCode("PRJ-" + project.getId());
    }
    project.setProjectName(normalizeRequiredText(request.name(), "PROJECT_NAME_REQUIRED"));
    project.setSource(normalizeRequiredText(request.source(), "PROJECT_SOURCE_REQUIRED"));
    project.setPlanId(parseNullableId(request.planId(), "PROJECT_PLAN_ID_INVALID"));
    project.setPlanName(trimToNull(request.planName()));
    project.setDescription(trimToNull(request.description()));
    project.setStartDate(parseRequiredDate(request.startDate(), "PROJECT_START_DATE_INVALID"));
    project.setEndDate(parseNullableDate(request.endDate(), "PROJECT_END_DATE_INVALID"));
    project.setTagIds("");
    project.setTagNames("");
    project.setLeaderId(parseId(normalizeRequiredText(request.leaderId(), "PROJECT_LEADER_ID_REQUIRED"),
        "PROJECT_LEADER_ID_INVALID"));
    project.setLeaderName(normalizeRequiredText(request.leaderName(), "PROJECT_LEADER_NAME_REQUIRED"));
    project.setChecklistIds(joinCsv(request.checklistIds()));
  }

  private List<BizProjectMemberEntity> createMembers(
      Long projectId,
      CurrentUserPrincipal principal,
      List<ProjectUpsertRequest.ProjectMemberRequest> requests
  ) {
    if (requests == null || requests.isEmpty()) {
      return List.of();
    }
    return requests.stream()
        .map(request -> {
          BizProjectMemberEntity member = new BizProjectMemberEntity();
          member.setId(nextId(member));
          member.setTenantId(principal.tenantId());
          member.setProjectId(projectId);
          member.setPersonnelId(parseId(request.personnelId(), "PROJECT_MEMBER_ID_INVALID"));
          member.setPersonnelName(normalizeRequiredText(request.personnelName(), "PROJECT_MEMBER_NAME_REQUIRED"));
          member.setEmployeeNo(trimToNull(request.employeeNo()));
          member.setDepartment(trimToNull(request.department()));
          member.setRole(normalizeProjectMemberRole(request.role()));
          member.setDeleted(0);
          member.setVersion(0L);
          member.setCreatedBy(principal.userId());
          member.setUpdatedBy(principal.userId());
          projectMemberMapper.insert(member);
          return member;
        })
        .toList();
  }

  private List<BizProjectTaskEntity> createTasks(
      Long projectId,
      CurrentUserPrincipal principal,
      List<BizChecklistEntity> checklists,
      List<BizChecklistItemEntity> checklistItems
  ) {
    Map<Long, BizChecklistEntity> checklistById = checklists.stream()
        .collect(Collectors.toMap(BizChecklistEntity::getId, Function.identity(), (left, right) -> left));
    return checklistItems.stream()
        .sorted(Comparator.comparing(BizChecklistItemEntity::getChecklistId)
            .thenComparing(BizChecklistItemEntity::getId))
        .map(item -> {
          BizChecklistEntity checklist = checklistById.get(item.getChecklistId());
          BizProjectTaskEntity task = new BizProjectTaskEntity();
          task.setId(nextId(task));
          task.setTenantId(principal.tenantId());
          task.setProjectId(projectId);
          task.setChecklistId(item.getChecklistId());
          task.setChecklistName(checklist == null ? null : checklist.getChecklistName());
          task.setChecklistItemId(item.getId());
          task.setCheckContent(item.getContent());
          task.setCheckCriterion(item.getCriterion());
          task.setControlFrequency(item.getControlFrequency());
          task.setEvaluationType(item.getEvaluationType());
          task.setTaskName(item.getContent());
          task.setTaskDescription(item.getCriterion());
          task.setStatus("pending");
          task.setDeleted(0);
          task.setVersion(0L);
          task.setCreatedBy(principal.userId());
          task.setUpdatedBy(principal.userId());
          projectTaskMapper.insert(task);
          return task;
        })
        .toList();
  }

  private List<BizChecklistItemEntity> resolveChecklistItemsForProject(
      ProjectUpsertRequest request,
      List<BizChecklistItemEntity> checklistItems,
      BizPlanEntity linkedPlan
  ) {
    List<Long> selectedItemIds = parseOptionalIds(request.checklistItemIds(), "PROJECT_CHECKLIST_ITEM_ID_INVALID");
    if (!selectedItemIds.isEmpty()) {
      return selectChecklistItemsByIds(checklistItems, selectedItemIds);
    }

    String generationMode = normalizeChecklistGenerationMode(request.checklistGenerationMode(), linkedPlan);
    return switch (generationMode) {
      case "periodic" -> filterChecklistItemsByPlanCycle(
          checklistItems,
          linkedPlan == null ? null : linkedPlan.getCycle()
      );
      case "random" -> randomlySelectChecklistItems(checklistItems, request.randomCount());
      default -> checklistItems;
    };
  }

  private List<BizChecklistItemEntity> selectChecklistItemsByIds(
      List<BizChecklistItemEntity> checklistItems,
      List<Long> selectedItemIds
  ) {
    Map<Long, BizChecklistItemEntity> itemById = checklistItems.stream()
        .collect(Collectors.toMap(BizChecklistItemEntity::getId, Function.identity(), (left, right) -> left));
    List<BizChecklistItemEntity> selectedItems = selectedItemIds.stream()
        .map(itemById::get)
        .filter(Objects::nonNull)
        .toList();
    if (selectedItems.size() != selectedItemIds.size()) {
      throw new BusinessException("PROJECT_CHECKLIST_ITEM_ID_INVALID", "PROJECT_CHECKLIST_ITEM_ID_INVALID");
    }
    return selectedItems;
  }

  private String normalizeChecklistGenerationMode(String generationMode, BizPlanEntity linkedPlan) {
    String normalized = trimToNull(generationMode);
    if (normalized == null) {
      return linkedPlan == null ? "full" : "periodic";
    }
    normalized = normalized.toLowerCase(Locale.ROOT);
    if (!Set.of("full", "periodic", "random").contains(normalized)) {
      throw new BusinessException("PROJECT_CHECKLIST_GENERATION_MODE_INVALID", "PROJECT_CHECKLIST_GENERATION_MODE_INVALID");
    }
    return normalized;
  }

  private List<BizChecklistItemEntity> randomlySelectChecklistItems(
      List<BizChecklistItemEntity> checklistItems,
      Integer randomCount
  ) {
    int count = randomCount == null ? 0 : randomCount;
    if (count <= 0) {
      throw new BusinessException("PROJECT_CHECKLIST_RANDOM_COUNT_INVALID", "PROJECT_CHECKLIST_RANDOM_COUNT_INVALID");
    }
    if (count >= checklistItems.size()) {
      return checklistItems;
    }
    List<BizChecklistItemEntity> shuffled = checklistItems.stream().collect(Collectors.toList());
    Collections.shuffle(shuffled);
    return shuffled.stream().limit(count).toList();
  }

  private BizPlanEntity loadProjectPlan(Long tenantId, String planId) {
    Long parsedPlanId = parseNullableId(planId, "PROJECT_PLAN_ID_INVALID");
    if (parsedPlanId == null) {
      return null;
    }
    return planMapper.selectOne(new LambdaQueryWrapper<BizPlanEntity>()
        .eq(BizPlanEntity::getTenantId, tenantId)
        .eq(BizPlanEntity::getId, parsedPlanId));
  }

  private List<BizChecklistItemEntity> filterChecklistItemsByPlanCycle(
      List<BizChecklistItemEntity> checklistItems,
      String planCycle
  ) {
    Integer planRank = planCycleRank(planCycle);
    if (planRank == null) {
      return checklistItems;
    }
    return checklistItems.stream()
        .filter(item -> {
          Integer frequencyRank = controlFrequencyRank(item.getControlFrequency());
          return frequencyRank != null && frequencyRank <= planRank;
        })
        .toList();
  }

  private Integer planCycleRank(String planCycle) {
    return switch (normalizeFrequencyKey(planCycle)) {
      case "monthly", "月度" -> 4;
      case "quarterly", "季度" -> 5;
      case "half-yearly", "half_yearly", "半年度" -> 6;
      case "yearly", "年度" -> 7;
      default -> null;
    };
  }

  private Integer controlFrequencyRank(String controlFrequency) {
    return switch (normalizeFrequencyKey(controlFrequency)) {
      case "per_occurrence", "每次发生" -> 0;
      case "daily", "每日" -> 1;
      case "weekly", "每周" -> 2;
      case "monthly", "每月" -> 4;
      case "quarterly", "每季度" -> 5;
      case "half-yearly", "half_yearly", "每半年度" -> 6;
      case "yearly", "每年度" -> 7;
      default -> null;
    };
  }

  private String normalizeFrequencyKey(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private void linkPlanItemsToProject(BizProjectEntity project, CurrentUserPrincipal principal) {
    if (project.getPlanId() == null) {
      return;
    }

    BizPlanItemEntity planItem = new BizPlanItemEntity();
    planItem.setProjectId(String.valueOf(project.getId()));
    planItem.setUpdatedBy(principal.userId());
    planItemMapper.update(planItem, new LambdaUpdateWrapper<BizPlanItemEntity>()
        .eq(BizPlanItemEntity::getTenantId, principal.tenantId())
        .eq(BizPlanItemEntity::getPlanId, project.getPlanId()));
  }

  private List<BizChecklistEntity> loadChecklists(Long tenantId, List<Long> checklistIds) {
    return checklistMapper.selectList(new LambdaQueryWrapper<BizChecklistEntity>()
        .eq(BizChecklistEntity::getTenantId, tenantId)
        .in(BizChecklistEntity::getId, checklistIds));
  }

  private List<BizChecklistItemEntity> loadChecklistItems(Long tenantId, List<Long> checklistIds) {
    return checklistItemMapper.selectList(new LambdaQueryWrapper<BizChecklistItemEntity>()
        .eq(BizChecklistItemEntity::getTenantId, tenantId)
        .in(BizChecklistItemEntity::getChecklistId, checklistIds)
        .orderByAsc(BizChecklistItemEntity::getChecklistId)
        .orderByAsc(BizChecklistItemEntity::getSequenceNo)
        .orderByAsc(BizChecklistItemEntity::getId));
  }

  private Map<Long, List<BizProjectMemberEntity>> loadMembers(
      Long tenantId,
      List<BizProjectEntity> projects
  ) {
    if (projects.isEmpty()) {
      return Map.of();
    }
    List<BizProjectMemberEntity> members = projectMemberMapper.selectList(new LambdaQueryWrapper<BizProjectMemberEntity>()
            .eq(BizProjectMemberEntity::getTenantId, tenantId)
            .in(BizProjectMemberEntity::getProjectId, projects.stream().map(BizProjectEntity::getId).toList()));
    return nullToList(members)
        .stream()
        .collect(Collectors.groupingBy(BizProjectMemberEntity::getProjectId));
  }

  private List<BizProjectMemberEntity> listMembers(Long tenantId, Long projectId) {
    return nullToList(projectMemberMapper.selectList(new LambdaQueryWrapper<BizProjectMemberEntity>()
        .eq(BizProjectMemberEntity::getTenantId, tenantId)
        .eq(BizProjectMemberEntity::getProjectId, projectId)));
  }

  private Map<Long, List<BizProjectTaskEntity>> loadTasks(
      Long tenantId,
      List<BizProjectEntity> projects
  ) {
    if (projects.isEmpty()) {
      return Map.of();
    }
    List<BizProjectTaskEntity> tasks = projectTaskMapper.selectList(new LambdaQueryWrapper<BizProjectTaskEntity>()
            .eq(BizProjectTaskEntity::getTenantId, tenantId)
            .in(BizProjectTaskEntity::getProjectId, projects.stream().map(BizProjectEntity::getId).toList()));
    return nullToList(tasks)
        .stream()
        .collect(Collectors.groupingBy(BizProjectTaskEntity::getProjectId));
  }

  private List<BizProjectTaskEntity> listTasks(Long tenantId, Long projectId) {
    return nullToList(projectTaskMapper.selectList(new LambdaQueryWrapper<BizProjectTaskEntity>()
        .eq(BizProjectTaskEntity::getTenantId, tenantId)
        .eq(BizProjectTaskEntity::getProjectId, projectId)));
  }

  private List<BizProjectTaskWorkOrderEntity> listProjectWorkOrders(Long tenantId, Long projectId) {
    return nullToList(projectTaskWorkOrderMapper.selectList(
        new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
            .eq(BizProjectTaskWorkOrderEntity::getTenantId, tenantId)
            .eq(BizProjectTaskWorkOrderEntity::getProjectId, projectId)
            .orderByAsc(BizProjectTaskWorkOrderEntity::getTaskId)
            .orderByAsc(BizProjectTaskWorkOrderEntity::getId)));
  }

  private List<BizProjectRectificationEntity> listProjectRectifications(Long tenantId, Long projectId) {
    return nullToList(projectRectificationMapper.selectList(
        new LambdaQueryWrapper<BizProjectRectificationEntity>()
            .eq(BizProjectRectificationEntity::getTenantId, tenantId)
            .eq(BizProjectRectificationEntity::getProjectId, projectId)
            .orderByAsc(BizProjectRectificationEntity::getTaskId)
            .orderByAsc(BizProjectRectificationEntity::getId)));
  }

  private BizProjectArchiveEntity existingProjectArchive(Long tenantId, Long projectId) {
    return projectArchiveMapper.selectOne(new LambdaQueryWrapper<BizProjectArchiveEntity>()
        .eq(BizProjectArchiveEntity::getTenantId, tenantId)
        .eq(BizProjectArchiveEntity::getProjectId, projectId));
  }

  private String buildProjectArchiveSnapshot(
      BizProjectEntity project,
      List<BizProjectMemberEntity> members,
      List<BizProjectTaskEntity> tasks,
      List<BizProjectTaskWorkOrderEntity> workOrders,
      List<BizProjectRectificationEntity> rectifications,
      CurrentUserPrincipal principal,
      LocalDateTime archivedAt
  ) {
    // 项目档案是一条项目级冻结快照，不按检查项、工单或整改单拆分成多条档案，方便后续完整追溯归档时的项目状态。
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("snapshotVersion", "v1");
    snapshot.put("archivedAt", DateTimeFormatters.formatDateTime(archivedAt));
    snapshot.put("archivedBy", String.valueOf(principal.userId()));
    snapshot.put("archivedByName", principal.username());
    snapshot.put("project", projectSnapshot(project));
    snapshot.put("members", members.stream().map(this::memberSnapshot).toList());
    snapshot.put("tasks", tasks.stream().map(this::taskSnapshot).toList());
    snapshot.put("workOrders", workOrders.stream().map(this::workOrderSnapshot).toList());
    snapshot.put("rectifications", rectifications.stream().map(this::rectificationSnapshot).toList());
    return writeJson(snapshot);
  }

  private Map<String, Object> projectSnapshot(BizProjectEntity project) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", String.valueOf(project.getId()));
    snapshot.put("projectCode", project.getProjectCode());
    snapshot.put("projectName", project.getProjectName());
    snapshot.put("source", project.getSource());
    snapshot.put("planId", project.getPlanId() == null ? null : String.valueOf(project.getPlanId()));
    snapshot.put("planName", project.getPlanName());
    snapshot.put("description", project.getDescription());
    snapshot.put("startDate", project.getStartDate() == null ? null : project.getStartDate().toString());
    snapshot.put("endDate", project.getEndDate() == null ? null : project.getEndDate().toString());
    snapshot.put("status", project.getStatus());
    snapshot.put("tagIds", splitCsv(project.getTagIds()));
    snapshot.put("tagNames", splitCsv(project.getTagNames()));
    snapshot.put("leaderId", project.getLeaderId() == null ? null : String.valueOf(project.getLeaderId()));
    snapshot.put("leaderName", project.getLeaderName());
    snapshot.put("checklistIds", splitCsv(project.getChecklistIds()));
    snapshot.put("archiveStatus", project.getArchiveStatus());
    return snapshot;
  }

  private Map<String, Object> memberSnapshot(BizProjectMemberEntity member) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", String.valueOf(member.getId()));
    snapshot.put("personnelId", member.getPersonnelId() == null ? null : String.valueOf(member.getPersonnelId()));
    snapshot.put("personnelName", member.getPersonnelName());
    snapshot.put("employeeNo", member.getEmployeeNo());
    snapshot.put("department", member.getDepartment());
    snapshot.put("role", member.getRole());
    return snapshot;
  }

  private Map<String, Object> taskSnapshot(BizProjectTaskEntity task) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", String.valueOf(task.getId()));
    snapshot.put("projectId", String.valueOf(task.getProjectId()));
    snapshot.put("checklistId", task.getChecklistId() == null ? null : String.valueOf(task.getChecklistId()));
    snapshot.put("checklistName", task.getChecklistName());
    snapshot.put("checklistItemId", task.getChecklistItemId() == null ? null : String.valueOf(task.getChecklistItemId()));
    snapshot.put("checkContent", task.getCheckContent());
    snapshot.put("checkCriterion", task.getCheckCriterion());
    snapshot.put("controlFrequency", task.getControlFrequency());
    snapshot.put("evaluationType", task.getEvaluationType());
    snapshot.put("taskName", task.getTaskName());
    snapshot.put("taskDescription", task.getTaskDescription());
    snapshot.put("assigneeId", task.getAssigneeId() == null ? null : String.valueOf(task.getAssigneeId()));
    snapshot.put("assigneeName", task.getAssigneeName());
    snapshot.put("contactId", task.getContactId() == null ? null : String.valueOf(task.getContactId()));
    snapshot.put("contactName", task.getContactName());
    snapshot.put("status", task.getStatus());
    snapshot.put("issuedAt", DateTimeFormatters.formatDateTime(task.getIssuedAt()));
    snapshot.put("completedAt", DateTimeFormatters.formatDateTime(task.getCompletedAt()));
    return snapshot;
  }

  private Map<String, Object> workOrderSnapshot(BizProjectTaskWorkOrderEntity workOrder) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", String.valueOf(workOrder.getId()));
    snapshot.put("projectId", String.valueOf(workOrder.getProjectId()));
    snapshot.put("taskId", String.valueOf(workOrder.getTaskId()));
    snapshot.put("omsWorkOrderId", workOrder.getOmsWorkOrderId());
    snapshot.put("idempotencyKey", workOrder.getIdempotencyKey());
    snapshot.put("handlerId", workOrder.getHandlerId() == null ? null : String.valueOf(workOrder.getHandlerId()));
    snapshot.put("handlerEmployeeNo", workOrder.getHandlerEmployeeNo());
    snapshot.put("handlerName", workOrder.getHandlerName());
    snapshot.put("workOrderTitle", workOrder.getWorkOrderTitle());
    snapshot.put("workOrderDescription", workOrder.getWorkOrderDescription());
    snapshot.put("issuedAt", DateTimeFormatters.formatDateTime(workOrder.getIssuedAt()));
    snapshot.put("completedAt", DateTimeFormatters.formatDateTime(workOrder.getCompletedAt()));
    snapshot.put("omsStatus", workOrder.getOmsStatus());
    snapshot.put("omsStatusName", workOrder.getOmsStatusName());
    snapshot.put("omsResultSummary", workOrder.getOmsResultSummary());
    snapshot.put("omsDetailPayload", workOrder.getOmsDetailPayload());
    snapshot.put("omsLogPayload", workOrder.getOmsLogPayload());
    snapshot.put("omsAttachmentPayload", workOrder.getOmsAttachmentPayload());
    snapshot.put("syncStatus", workOrder.getSyncStatus());
    snapshot.put("lastSyncedAt", DateTimeFormatters.formatDateTime(workOrder.getLastSyncedAt()));
    snapshot.put("syncError", workOrder.getSyncError());
    snapshot.put("irisReviewStatus", workOrder.getIrisReviewStatus());
    snapshot.put("irisReviewOpinion", workOrder.getIrisReviewOpinion());
    snapshot.put("irisReviewedAt", DateTimeFormatters.formatDateTime(workOrder.getIrisReviewedAt()));
    snapshot.put("irisReviewedBy", workOrder.getIrisReviewedBy() == null ? null : String.valueOf(workOrder.getIrisReviewedBy()));
    snapshot.put("nonconformityDisposition", workOrder.getNonconformityDisposition());
    snapshot.put("riskAcceptanceReason", workOrder.getRiskAcceptanceReason());
    snapshot.put("riskAcceptedAt", DateTimeFormatters.formatDateTime(workOrder.getRiskAcceptedAt()));
    snapshot.put("riskAcceptedBy", workOrder.getRiskAcceptedBy() == null ? null : String.valueOf(workOrder.getRiskAcceptedBy()));
    return snapshot;
  }

  private Map<String, Object> rectificationSnapshot(BizProjectRectificationEntity rectification) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", String.valueOf(rectification.getId()));
    snapshot.put("rectificationCode", rectification.getRectificationCode());
    snapshot.put("title", rectification.getTitle());
    snapshot.put("description", rectification.getDescription());
    snapshot.put("taskName", rectification.getTaskName());
    snapshot.put("taskDescription", rectification.getTaskDescription());
    snapshot.put("projectId", rectification.getProjectId() == null ? null : String.valueOf(rectification.getProjectId()));
    snapshot.put("projectName", rectification.getProjectName());
    snapshot.put("taskId", rectification.getTaskId() == null ? null : String.valueOf(rectification.getTaskId()));
    snapshot.put("checklistItemId", rectification.getChecklistItemId() == null ? null : String.valueOf(rectification.getChecklistItemId()));
    snapshot.put("checkContent", rectification.getCheckContent());
    snapshot.put("sourceWorkOrderRecordId", rectification.getSourceWorkOrderRecordId() == null ? null : String.valueOf(rectification.getSourceWorkOrderRecordId()));
    snapshot.put("omsWorkOrderId", rectification.getOmsWorkOrderId());
    snapshot.put("assigneeId", rectification.getAssigneeId() == null ? null : String.valueOf(rectification.getAssigneeId()));
    snapshot.put("assigneeName", rectification.getAssigneeName());
    snapshot.put("contactId", rectification.getContactId() == null ? null : String.valueOf(rectification.getContactId()));
    snapshot.put("contactName", rectification.getContactName());
    snapshot.put("issuedAt", DateTimeFormatters.formatDateTime(rectification.getIssuedAt()));
    snapshot.put("deadline", DateTimeFormatters.formatDateTime(rectification.getDeadline()));
    snapshot.put("completedAt", DateTimeFormatters.formatDateTime(rectification.getCompletedAt()));
    snapshot.put("reviewResult", rectification.getReviewResult());
    snapshot.put("rectificationOmsWorkOrderId", rectification.getRectificationOmsWorkOrderId());
    snapshot.put("rectificationOmsStatus", rectification.getRectificationOmsStatus());
    snapshot.put("rectificationOmsStatusName", rectification.getRectificationOmsStatusName());
    snapshot.put("rectificationWorkOrderCreatedAt", DateTimeFormatters.formatDateTime(rectification.getRectificationWorkOrderCreatedAt()));
    snapshot.put("rectificationWorkOrderCompletedAt", DateTimeFormatters.formatDateTime(rectification.getRectificationWorkOrderCompletedAt()));
    snapshot.put("status", rectification.getStatus());
    snapshot.put("remark", rectification.getRemark());
    return snapshot;
  }

  private int countArchiveDocuments(List<BizProjectTaskWorkOrderEntity> workOrders) {
    return workOrders.stream()
        .mapToInt(workOrder -> countJsonArrayItems(workOrder.getOmsAttachmentPayload())
            + countOmsLogAttachmentItems(workOrder.getOmsLogPayload()))
        .sum();
  }

  private int countOmsLogAttachmentItems(String payload) {
    JsonNode logs = readJsonArrayOrEmpty(payload);
    int count = 0;
    for (JsonNode log : logs) {
      // OMS 日志附件存放在 RECORD_FJ 中，归档文件数量必须同步计入日志材料。
      String attachmentPayload = nonBlank(
          log.path("RECORD_FJ").asText(null),
          log.path("attachmentsPayload").asText(null),
          nonBlank(log.path("attachments").asText(null), log.path("attachmentPayload").asText(null), null)
      );
      count += countJsonArrayItems(attachmentPayload);
    }
    return count;
  }

  private int countJsonArrayItems(String payload) {
    return readJsonArrayOrEmpty(payload).size();
  }

  private JsonNode readJsonArrayOrEmpty(String payload) {
    String normalized = trimToNull(payload);
    if (normalized == null) {
      return objectMapper.createArrayNode();
    }
    try {
      JsonNode node = objectMapper.readTree(normalized);
      return node.isArray() ? node : objectMapper.createArrayNode();
    } catch (JsonProcessingException exception) {
      return objectMapper.createArrayNode();
    }
  }

  private boolean matches(BizProjectEntity project, ProjectListQuery query) {
    String keyword = trimToNull(query.keyword());
    String status = trimToNull(query.status());
    String tagId = trimToNull(query.tagId());
    String source = trimToNull(query.source());
    LocalDate startDate = parseFilterDate(query.startDate(), "PROJECT_QUERY_START_DATE_INVALID");
    LocalDate endDate = parseFilterDate(query.endDate(), "PROJECT_QUERY_END_DATE_INVALID");
    return (keyword == null
        || containsIgnoreCase(project.getProjectName(), keyword)
        || containsIgnoreCase(project.getProjectCode(), keyword)
        || containsIgnoreCase(project.getDescription(), keyword))
        && (status == null || status.equalsIgnoreCase(project.getStatus()))
        && (tagId == null || splitCsv(project.getTagIds()).contains(tagId))
        && (source == null || source.equalsIgnoreCase(project.getSource()))
        && (startDate == null || !project.getStartDate().isBefore(startDate))
        && (endDate == null || !project.getStartDate().isAfter(endDate));
  }

  private BizProjectEntity requireProject(Long id, Long tenantId) {
    BizProjectEntity project = projectMapper.selectById(id);
    if (project == null || !Objects.equals(project.getTenantId(), tenantId)) {
      throw new BusinessException("PROJECT_NOT_FOUND", "project not found: " + id);
    }
    return project;
  }

  private BizProjectTaskEntity requireTask(Long taskId, Long projectId, Long tenantId) {
    BizProjectTaskEntity task = projectTaskMapper.selectById(taskId);
    if (task == null
        || !Objects.equals(task.getTenantId(), tenantId)
        || !Objects.equals(task.getProjectId(), projectId)) {
      throw new BusinessException("PROJECT_TASK_NOT_FOUND", "project task not found: " + taskId);
    }
    return task;
  }

  private BizProjectTaskWorkOrderEntity requireWorkOrder(
      Long workOrderId,
      Long projectId,
      Long taskId,
      Long tenantId
  ) {
    BizProjectTaskWorkOrderEntity workOrder = projectTaskWorkOrderMapper.selectById(workOrderId);
    if (workOrder == null
        || Objects.equals(workOrder.getDeleted(), 1)
        || !Objects.equals(workOrder.getTenantId(), tenantId)
        || !Objects.equals(workOrder.getProjectId(), projectId)
        || !Objects.equals(workOrder.getTaskId(), taskId)) {
      throw new BusinessException("PROJECT_WORK_ORDER_NOT_FOUND", "project work order not found: " + workOrderId);
    }
    return workOrder;
  }

  private void ensureCanView(
      BizProjectEntity project,
      List<BizProjectMemberEntity> members,
      CurrentUserPrincipal principal
  ) {
    // 超级管理员需要具备租户内全局查看权限，不依赖项目成员关系。
    if (isSuperAdmin(principal)) {
      return;
    }
    boolean visible = Objects.equals(project.getLeaderId(), principal.userId())
        || members.stream().anyMatch(member -> Objects.equals(member.getPersonnelId(), principal.userId()));
    if (!visible) {
      throw new BusinessException("PROJECT_FORBIDDEN", "PROJECT_FORBIDDEN");
    }
  }

  private boolean isSuperAdmin(CurrentUserPrincipal principal) {
    return nullToList(principal.roles()).stream()
        .filter(Objects::nonNull)
        .map(role -> role.toUpperCase(Locale.ROOT))
        .anyMatch(role -> "PLATFORM_ADMIN".equals(role) || "SUPER_ADMIN".equals(role));
  }

  private void ensureLeader(BizProjectEntity project, CurrentUserPrincipal principal) {
    if (!Objects.equals(project.getLeaderId(), principal.userId())) {
      throw new BusinessException("PROJECT_LEADER_REQUIRED", "PROJECT_LEADER_REQUIRED");
    }
  }

  private void ensureProjectInProgress(BizProjectEntity project) {
    if (!"in_progress".equals(project.getStatus())) {
      throw new BusinessException("PROJECT_NOT_STARTED", "PROJECT_NOT_STARTED");
    }
  }

  private void ensureTaskWorkOrderAccess(
      BizProjectEntity project,
      BizProjectTaskEntity task,
      CurrentUserPrincipal principal,
      List<BizProjectMemberEntity> members
  ) {
    if (!canOperateTaskWorkOrder(project, task, principal, members)) {
      throw new BusinessException("PROJECT_TASK_ASSIGNEE_REQUIRED", "PROJECT_TASK_ASSIGNEE_REQUIRED");
    }
  }

  private boolean canOperateTaskWorkOrder(
      BizProjectEntity project,
      BizProjectTaskEntity task,
      CurrentUserPrincipal principal,
      List<BizProjectMemberEntity> members
  ) {
    if (Objects.equals(project.getLeaderId(), principal.userId())
        || Objects.equals(task.getAssigneeId(), principal.userId())) {
      return true;
    }
    return nullToList(members).stream()
        .filter(member -> isCurrentPrincipalMember(member, principal))
        .anyMatch(member -> "leader".equals(member.getRole()) || isTaskAssigneeMember(task, member));
  }

  private boolean isCurrentPrincipalMember(BizProjectMemberEntity member, CurrentUserPrincipal principal) {
    return Objects.equals(member.getPersonnelId(), principal.userId())
        || textEquals(member.getEmployeeNo(), principal.account())
        || textEquals(member.getPersonnelName(), principal.username());
  }

  private boolean isTaskAssigneeMember(BizProjectTaskEntity task, BizProjectMemberEntity member) {
    return Objects.equals(task.getAssigneeId(), member.getPersonnelId())
        || textEquals(task.getAssigneeName(), member.getPersonnelName());
  }

  private BizProjectMemberEntity requireAssignableTaskMember(
      List<BizProjectMemberEntity> members,
      Long assigneeId
  ) {
    return members.stream()
        .filter(member -> Objects.equals(member.getPersonnelId(), assigneeId))
        .filter(member -> "leader".equals(member.getRole()) || "auditor".equals(member.getRole()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(
            "PROJECT_TASK_ASSIGNEE_ROLE_INVALID",
            "PROJECT_TASK_ASSIGNEE_ROLE_INVALID"
        ));
  }

  private String normalizeProjectMemberRole(String role) {
    String normalized = normalizeRequiredText(role, "PROJECT_MEMBER_ROLE_REQUIRED");
    if (!Set.of("leader", "auditor", "observer").contains(normalized)) {
      throw new BusinessException("PROJECT_MEMBER_ROLE_INVALID", "PROJECT_MEMBER_ROLE_INVALID");
    }
    return normalized;
  }

  private BizProjectTaskWorkOrderEntity saveWorkOrder(
      BizProjectEntity project,
      BizProjectTaskEntity task,
      CurrentUserPrincipal principal,
      OmsClient.OmsCreateCommand command,
      LocalDateTime issuedAt,
      Map<String, OmsClient.OmsCreateResult> resultByHandlerId,
      Map<String, BizProjectTaskWorkOrderEntity> existingByKey
  ) {
    OmsClient.OmsCreateResult result = resultByHandlerId.get(command.handlerId());
    BizProjectTaskWorkOrderEntity workOrder = existingByKey.get(command.idempotencyKey());
    boolean create = workOrder == null;
    if (create) {
      workOrder = new BizProjectTaskWorkOrderEntity();
      workOrder.setId(command.localWorkOrderId());
      workOrder.setTenantId(principal.tenantId());
      workOrder.setProjectId(project.getId());
      workOrder.setTaskId(task.getId());
      workOrder.setDeleted(0);
      workOrder.setVersion(0L);
      workOrder.setCreatedBy(principal.userId());
    }
    workOrder.setIdempotencyKey(command.idempotencyKey());
    workOrder.setHandlerId(handlerRecordId(command));
    workOrder.setHandlerEmployeeNo(command.handlerEmployeeNo());
    workOrder.setHandlerName(command.handlerName());
    workOrder.setWorkOrderTitle(command.title());
    workOrder.setWorkOrderDescription(command.description());
    // 前端创建 OMS 工单时选择的下达时间要落到本地工单记录，列表和详情都从这里展示。
    workOrder.setIssuedAt(issuedAt);
    workOrder.setRequestPayload(command.toString());
    workOrder.setResponsePayload(result == null ? null : result.responsePayload());
    workOrder.setOmsWorkOrderId(result == null ? null : result.omsWorkOrderId());
    workOrder.setSyncStatus(result == null || trimToNull(result.error()) != null ? "failed" : "synced");
    workOrder.setSyncError(result == null ? "OMS_CREATE_NO_RESULT" : trimToNull(result.error()));
    workOrder.setOmsStatus(result == null ? null : result.status());
    workOrder.setOmsStatusName(result == null ? null : result.status());
    workOrder.setIrisReviewStatus("pending");
    workOrder.setReviewLocked(0);
    workOrder.setNonconformityDisposition(null);
    workOrder.setRiskAcceptanceReason(null);
    workOrder.setRiskAcceptedAt(null);
    workOrder.setRiskAcceptedBy(null);
    workOrder.setUpdatedBy(principal.userId());
    if (create) {
      projectTaskWorkOrderMapper.insert(workOrder);
    } else {
      projectTaskWorkOrderMapper.updateById(workOrder);
    }
    return workOrder;
  }

  private Long handlerRecordId(OmsClient.OmsCreateCommand command) {
    String handlerId = trimToNull(command.handlerId());
    if (handlerId != null) {
      try {
        return Long.valueOf(handlerId);
      } catch (NumberFormatException ignored) {
        // OMS userId can be a UUID-like value; keep the local numeric field stable with employee no.
      }
    }
    return parseId(command.handlerEmployeeNo(), "PROJECT_WORK_ORDER_HANDLER_ID_INVALID");
  }

  private BizProjectRectificationEntity createRectification(
      BizProjectEntity project,
      BizProjectTaskEntity task,
      BizProjectTaskWorkOrderEntity workOrder,
      CurrentUserPrincipal principal
  ) {
    Long rectificationId = nextId(new BizProjectRectificationEntity());
    BizProjectRectificationEntity rectification = new BizProjectRectificationEntity();
    rectification.setId(rectificationId);
    rectification.setTenantId(principal.tenantId());
    rectification.setRectificationCode("RECT-" + rectificationId);
    rectification.setTitle(nonBlank(workOrder.getWorkOrderTitle(), task.getTaskName(), "整改项-" + workOrder.getId()));
    rectification.setDescription(buildRectificationDescription(workOrder));
    rectification.setTaskName(task.getTaskName());
    rectification.setTaskDescription(task.getTaskDescription());
    rectification.setProjectId(project.getId());
    rectification.setProjectName(project.getProjectName());
    rectification.setTaskId(task.getId());
    rectification.setChecklistItemId(task.getChecklistItemId());
    rectification.setCheckContent(task.getCheckContent());
    rectification.setSourceWorkOrderRecordId(workOrder.getId());
    rectification.setOmsWorkOrderId(workOrder.getOmsWorkOrderId());
    rectification.setAssigneeId(task.getAssigneeId());
    rectification.setAssigneeName(task.getAssigneeName());
    rectification.setContactId(task.getContactId());
    rectification.setContactName(task.getContactName());
    rectification.setIssuedAt(LocalDateTime.now());
    rectification.setDeadline(LocalDateTime.now().plusDays(7));
    rectification.setStatus("pending");
    rectification.setDeleted(0);
    rectification.setVersion(0L);
    rectification.setCreatedBy(principal.userId());
    rectification.setUpdatedBy(principal.userId());
    return rectification;
  }

  private String buildRectificationDescription(
      BizProjectTaskWorkOrderEntity workOrder
  ) {
    String opinion = trimToNull(workOrder.getIrisReviewOpinion());
    String base = nonBlank(workOrder.getWorkOrderDescription(), workOrder.getOmsResultSummary(), "工单审核要求整改");
    return opinion == null ? base : base + "\n审核意见：" + opinion;
  }

  private void ensureNonconformityPendingDisposition(BizProjectTaskWorkOrderEntity workOrder) {
    if (!Objects.equals(workOrder.getReviewLocked(), 1)
        || !"rectification_required".equals(workOrder.getIrisReviewStatus())) {
      throw new BusinessException("PROJECT_WORK_ORDER_NOT_NONCONFORMING", "PROJECT_WORK_ORDER_NOT_NONCONFORMING");
    }
    // 整改单已经改为按来源工单一对多创建，旧的 rectificationId 只做历史展示，不能再作为是否已处置的依据。
    if (trimToNull(workOrder.getNonconformityDisposition()) != null) {
      throw new BusinessException(
          "PROJECT_WORK_ORDER_NONCONFORMITY_DISPOSED",
          "该工单已处置，不能重复承担风险"
      );
    }
  }

  private void ensureNonconformityCanCreateRectification(BizProjectTaskWorkOrderEntity workOrder) {
    if (!Objects.equals(workOrder.getReviewLocked(), 1)
        || !"rectification_required".equals(workOrder.getIrisReviewStatus())) {
      throw new BusinessException("PROJECT_WORK_ORDER_NOT_NONCONFORMING", "PROJECT_WORK_ORDER_NOT_NONCONFORMING");
    }
    // 一个不符合项工单可以拆出多个整改单；只有选择“承担风险”后才视为不再生成整改单。
    if ("risk_accepted".equals(trimToNull(workOrder.getNonconformityDisposition()))) {
      throw new BusinessException(
          "PROJECT_WORK_ORDER_NONCONFORMITY_DISPOSED",
          "该工单已承担风险，不能再生成整改单"
      );
    }
  }

  private boolean hasRectificationsForSourceWorkOrder(Long workOrderId, Long tenantId) {
    return !nullToList(projectRectificationMapper.selectList(
        new LambdaQueryWrapper<BizProjectRectificationEntity>()
            .eq(BizProjectRectificationEntity::getTenantId, tenantId)
            .eq(BizProjectRectificationEntity::getSourceWorkOrderRecordId, workOrderId)
    )).isEmpty();
  }

  private void ensureAllTaskWorkOrdersReviewed(Long projectId, Long taskId, Long tenantId) {
    List<BizProjectTaskWorkOrderEntity> workOrders = listTaskWorkOrderEntities(projectId, taskId, tenantId);
    // 不符合项处置必须等同一检查项下所有工单都审核完成，避免漏审工单被提前生成整改或承担风险。
    if (workOrders.isEmpty() || workOrders.stream().anyMatch(workOrder -> !isWorkOrderReviewed(workOrder))) {
      throw new BusinessException("PROJECT_WORK_ORDER_REVIEW_INCOMPLETE", "PROJECT_WORK_ORDER_REVIEW_INCOMPLETE");
    }
  }

  private List<BizProjectTaskWorkOrderEntity> listTaskWorkOrderEntities(Long projectId, Long taskId, Long tenantId) {
    return nullToList(projectTaskWorkOrderMapper.selectList(
        new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
            .eq(BizProjectTaskWorkOrderEntity::getTenantId, tenantId)
            .eq(BizProjectTaskWorkOrderEntity::getProjectId, projectId)
            .eq(BizProjectTaskWorkOrderEntity::getTaskId, taskId)
    ));
  }

  private boolean isWorkOrderReviewed(BizProjectTaskWorkOrderEntity workOrder) {
    return Objects.equals(workOrder.getReviewLocked(), 1)
        && Set.of("passed", "rectification_required").contains(workOrder.getIrisReviewStatus());
  }

  private void ensureWorkOrderNotReviewed(BizProjectTaskWorkOrderEntity workOrder) {
    // 工单审核后锁定业务操作，只保留详情、日志等只读查看能力。
    if (Objects.equals(workOrder.getReviewLocked(), 1)) {
      throw new BusinessException("PROJECT_WORK_ORDER_REVIEW_LOCKED", "PROJECT_WORK_ORDER_REVIEW_LOCKED");
    }
  }

  private void updateTaskStatusAfterWorkOrderReview(BizProjectTaskEntity task, CurrentUserPrincipal principal) {
    List<BizProjectTaskWorkOrderEntity> workOrders =
        listTaskWorkOrderEntities(task.getProjectId(), task.getId(), principal.tenantId());
    // 检查项结论必须等所有工单都审核完再计算，防止其中一个不符合项提前覆盖整体状态。
    if (workOrders.isEmpty() || workOrders.stream().anyMatch(workOrder -> !isWorkOrderReviewed(workOrder))) {
      return;
    }
    if (workOrders.stream().anyMatch(workOrder -> "rectification_required".equals(workOrder.getIrisReviewStatus()))) {
      task.setStatus("nonconforming");
      task.setCompletedAt(LocalDateTime.now());
    } else if (workOrders.stream().allMatch(workOrder -> "passed".equals(workOrder.getIrisReviewStatus()))) {
      task.setStatus("passed");
      task.setCompletedAt(LocalDateTime.now());
    } else {
      return;
    }
    task.setUpdatedBy(principal.userId());
    projectTaskMapper.updateById(task);
  }

  private String normalizeReviewStatus(String reviewStatus) {
    String normalized = normalizeRequiredText(reviewStatus, "PROJECT_WORK_ORDER_REVIEW_STATUS_REQUIRED");
    if (!Set.of("passed", "rectification_required").contains(normalized)) {
      throw new BusinessException("PROJECT_WORK_ORDER_REVIEW_STATUS_INVALID", "PROJECT_WORK_ORDER_REVIEW_STATUS_INVALID");
    }
    return normalized;
  }

  private ProjectDto toDto(
      BizProjectEntity project,
      List<BizProjectMemberEntity> members,
      List<BizProjectTaskEntity> tasks
  ) {
    return toDto(project, members, tasks, List.of());
  }

  private ProjectDto toDto(
      BizProjectEntity project,
      List<BizProjectMemberEntity> members,
      List<BizProjectTaskEntity> tasks,
      List<BizProjectTaskWorkOrderEntity> workOrders
  ) {
    int taskCount = tasks.size();
    long passedCount = tasks.stream().filter(task -> "passed".equals(task.getStatus())).count();
    long nonconformingCount = tasks.stream().filter(task -> "nonconforming".equals(task.getStatus())).count();
    int progress = taskCount == 0 ? 0 : (int) ((passedCount + nonconformingCount) * 100 / taskCount);
    Map<Long, List<BizProjectTaskWorkOrderEntity>> workOrdersByTaskId = nullToList(workOrders)
        .stream()
        .collect(Collectors.groupingBy(BizProjectTaskWorkOrderEntity::getTaskId));
    return new ProjectDto(
        String.valueOf(project.getId()),
        project.getProjectCode(),
        project.getProjectName(),
        project.getSource(),
        project.getPlanId() == null ? null : String.valueOf(project.getPlanId()),
        project.getPlanName(),
        project.getDescription(),
        project.getStartDate() == null ? null : project.getStartDate().toString(),
        project.getEndDate() == null ? null : project.getEndDate().toString(),
        DateTimeFormatters.formatDateTime(project.getActualStartedAt()),
        project.getStatus(),
        splitCsv(project.getTagIds()),
        splitCsv(project.getTagNames()),
        project.getLeaderId() == null ? null : String.valueOf(project.getLeaderId()),
        project.getLeaderName(),
        splitCsv(project.getChecklistIds()),
        project.getArchiveStatus(),
        DateTimeFormatters.formatDateTime(project.getArchiveStartedAt()),
        DateTimeFormatters.formatDateTime(project.getArchiveCompletedAt()),
        project.getArchiveError(),
        taskCount,
        (int) passedCount,
        (int) nonconformingCount,
        progress,
        members.stream().map(this::toMemberDto).toList(),
        tasks.stream().map(task -> toTaskDto(
            task,
            workOrdersByTaskId.getOrDefault(task.getId(), List.of())
        )).toList(),
        List.of("update", "delete", "start"),
        DateTimeFormatters.formatDateTime(project.getCreatedAt()),
        DateTimeFormatters.formatDateTime(project.getUpdatedAt())
    );
  }

  private ProjectMemberDto toMemberDto(BizProjectMemberEntity member) {
    return new ProjectMemberDto(
        String.valueOf(member.getId()),
        String.valueOf(member.getPersonnelId()),
        member.getPersonnelName(),
        member.getEmployeeNo(),
        member.getDepartment(),
        member.getRole()
    );
  }

  private ProjectTaskDto toTaskDto(BizProjectTaskEntity task) {
    return toTaskDto(task, List.of());
  }

  private ProjectTaskDto toTaskDto(
      BizProjectTaskEntity task,
      List<BizProjectTaskWorkOrderEntity> workOrders
  ) {
    List<ProjectTaskWorkOrderDto> workOrderDtos = nullToList(workOrders)
        .stream()
        .map(this::toWorkOrderDto)
        .toList();
    long passedWorkOrderCount = nullToList(workOrders)
        .stream()
        .filter(workOrder -> "passed".equals(workOrder.getIrisReviewStatus()))
        .count();
    long nonconformingWorkOrderCount = nullToList(workOrders)
        .stream()
        .filter(workOrder -> "rectification_required".equals(workOrder.getIrisReviewStatus()))
        .count();
    return new ProjectTaskDto(
        String.valueOf(task.getId()),
        String.valueOf(task.getProjectId()),
        String.valueOf(task.getChecklistId()),
        task.getChecklistName(),
        String.valueOf(task.getChecklistItemId()),
        task.getCheckContent(),
        task.getCheckCriterion(),
        task.getControlFrequency(),
        task.getEvaluationType(),
        task.getTaskName(),
        task.getTaskDescription(),
        task.getAssigneeId() == null ? null : String.valueOf(task.getAssigneeId()),
        task.getAssigneeName(),
        task.getContactId() == null ? null : String.valueOf(task.getContactId()),
        task.getContactName(),
        task.getStatus(),
        DateTimeFormatters.formatDateTime(task.getIssuedAt()),
        DateTimeFormatters.formatDateTime(task.getCompletedAt()),
        workOrderDtos.size(),
        (int) passedWorkOrderCount,
        (int) nonconformingWorkOrderCount,
        workOrderDtos,
        List.of("assign")
    );
  }

  private ProjectTaskWorkOrderDto toWorkOrderDto(BizProjectTaskWorkOrderEntity workOrder) {
    return toWorkOrderDto(workOrder, Map.of());
  }

  private RectificationDto toRectificationDto(BizProjectRectificationEntity rectification) {
    return new RectificationDto(
        String.valueOf(rectification.getId()),
        rectification.getRectificationCode(),
        rectification.getSourceWorkOrderRecordId() == null ? "manual" : "task",
        rectification.getTaskId() == null ? null : String.valueOf(rectification.getTaskId()),
        rectification.getTaskName(),
        rectification.getTaskDescription(),
        rectification.getProjectId() == null ? null : String.valueOf(rectification.getProjectId()),
        rectification.getProjectName(),
        rectification.getCheckContent(),
        rectification.getSourceWorkOrderRecordId() == null
            ? null
            : String.valueOf(rectification.getSourceWorkOrderRecordId()),
        rectification.getOmsWorkOrderId(),
        rectification.getTitle(),
        rectification.getDescription(),
        rectification.getAssigneeId() == null ? null : String.valueOf(rectification.getAssigneeId()),
        rectification.getAssigneeName(),
        rectification.getContactId() == null ? null : String.valueOf(rectification.getContactId()),
        rectification.getContactName(),
        rectification.getStatus(),
        DateTimeFormatters.formatDateTime(rectification.getIssuedAt()),
        DateTimeFormatters.formatDateTime(rectification.getDeadline()),
        DateTimeFormatters.formatDateTime(rectification.getCompletedAt()),
        rectification.getReviewResult(),
        rectification.getRectificationOmsWorkOrderId(),
        rectification.getRectificationOmsStatus(),
        rectification.getRectificationOmsStatusName(),
        DateTimeFormatters.formatDateTime(rectification.getRectificationWorkOrderCreatedAt()),
        DateTimeFormatters.formatDateTime(rectification.getRectificationWorkOrderCompletedAt()),
        null,
        null,
        null,
        List.of(),
        rectification.getRemark(),
        List.of(),
        DateTimeFormatters.formatDateTime(rectification.getCreatedAt()),
        DateTimeFormatters.formatDateTime(rectification.getUpdatedAt())
    );
  }

  private ProjectTaskWorkOrderDto toWorkOrderDto(
      BizProjectTaskWorkOrderEntity workOrder,
      Map<Long, String> employeeNoByPersonnelId
  ) {
    return new ProjectTaskWorkOrderDto(
        String.valueOf(workOrder.getId()),
        String.valueOf(workOrder.getProjectId()),
        String.valueOf(workOrder.getTaskId()),
        workOrder.getOmsWorkOrderId(),
        workOrder.getIdempotencyKey(),
        workOrder.getHandlerId() == null ? null : String.valueOf(workOrder.getHandlerId()),
        handlerEmployeeNo(workOrder, employeeNoByPersonnelId),
        workOrder.getHandlerName(),
        workOrder.getWorkOrderTitle(),
        workOrder.getWorkOrderDescription(),
        DateTimeFormatters.formatDateTime(workOrder.getIssuedAt()),
        DateTimeFormatters.formatDateTime(workOrder.getCompletedAt()),
        workOrder.getOmsStatus(),
        workOrder.getOmsStatusName(),
        workOrder.getOmsResultSummary(),
        workOrder.getOmsDetailPayload(),
        workOrder.getOmsLogPayload(),
        workOrder.getOmsAttachmentPayload(),
        workOrder.getSyncStatus(),
        DateTimeFormatters.formatDateTime(workOrder.getLastSyncedAt()),
        workOrder.getSyncError(),
        workOrder.getIrisReviewStatus(),
        workOrder.getIrisReviewOpinion(),
        DateTimeFormatters.formatDateTime(workOrder.getIrisReviewedAt()),
        workOrder.getIrisReviewedBy() == null ? null : String.valueOf(workOrder.getIrisReviewedBy()),
        workOrder.getRectificationId() == null ? null : String.valueOf(workOrder.getRectificationId()),
        workOrder.getNonconformityDisposition(),
        workOrder.getRiskAcceptanceReason(),
        DateTimeFormatters.formatDateTime(workOrder.getRiskAcceptedAt()),
        workOrder.getRiskAcceptedBy() == null ? null : String.valueOf(workOrder.getRiskAcceptedBy()),
        Objects.equals(workOrder.getReviewLocked(), 1),
        isWorkOrderReviewable(workOrder)
    );
  }

  private Map<Long, String> employeeNoByPersonnelId(List<BizProjectMemberEntity> members) {
    return nullToList(members).stream()
        .filter(member -> member.getPersonnelId() != null && trimToNull(member.getEmployeeNo()) != null)
        .collect(Collectors.toMap(
            BizProjectMemberEntity::getPersonnelId,
            member -> member.getEmployeeNo().trim(),
            (left, right) -> left
        ));
  }

  private String handlerEmployeeNo(
      BizProjectTaskWorkOrderEntity workOrder,
      Map<Long, String> employeeNoByPersonnelId
  ) {
    if (workOrder.getHandlerId() != null) {
      String employeeNo = employeeNoByPersonnelId.get(workOrder.getHandlerId());
      if (employeeNo != null) {
        return employeeNo;
      }
    }
    return workOrder.getHandlerEmployeeNo();
  }

  private boolean isOmsCompleted(BizProjectTaskWorkOrderEntity workOrder) {
    return isCompletedOmsStatusText(workOrder.getOmsStatusName())
        || isCompletedOmsStatusText(workOrder.getOmsStatus());
  }

  private boolean isCompletedOmsStatusText(String status) {
    String normalized = trimToNull(status);
    if (normalized == null) {
      return false;
    }
    // OMS 有些接口返回中文状态，有些接口只返回状态码；这里统一把完成态收口到退回校验。
    return "已完成".equals(normalized)
        || "20".equals(normalized)
        || "complete".equalsIgnoreCase(normalized)
        || "completed".equalsIgnoreCase(normalized);
  }

  private boolean isOmsReviewableStatus(BizProjectTaskWorkOrderEntity workOrder) {
    return isCompletedOmsStatusText(workOrder.getOmsStatusName())
        || isCompletedOmsStatusText(workOrder.getOmsStatus())
        || isArchivedOmsStatusText(workOrder.getOmsStatusName())
        || isArchivedOmsStatusText(workOrder.getOmsStatus());
  }

  private boolean isArchivedOmsStatusText(String status) {
    String normalized = trimToNull(status);
    if (normalized == null) {
      return false;
    }
    return "已归档".equals(normalized)
        || "30".equals(normalized)
        || "archived".equalsIgnoreCase(normalized);
  }

  private boolean isWorkOrderReviewable(BizProjectTaskWorkOrderEntity workOrder) {
    // 工单审核允许 OMS 已完成和已归档；退回仍只允许已完成，两个动作不要混用同一状态口径。
    return Objects.equals(workOrder.getReviewLocked(), 0)
        && isOmsReviewableStatus(workOrder);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("PROJECT_OMS_PAYLOAD_SERIALIZE_FAILED", "PROJECT_OMS_PAYLOAD_SERIALIZE_FAILED");
    }
  }

  private Long nextId(Object entity) {
    Object nextId = identifierGenerator.nextId(entity);
    if (nextId instanceof Number number) {
      return number.longValue();
    }
    return Long.valueOf(String.valueOf(nextId));
  }

  private List<Long> parseIds(List<String> ids, String code) {
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(code, code);
    }
    return ids.stream()
        .map(value -> parseId(value, code))
        .distinct()
        .toList();
  }

  private List<Long> parseOptionalIds(List<String> ids, String code) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ids.stream()
        .map(value -> parseId(value, code))
        .distinct()
        .toList();
  }

  private Long parseNullableId(String id, String code) {
    String normalized = trimToNull(id);
    return normalized == null ? null : parseId(normalized, code);
  }

  private Long parseId(String id, String code) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new BusinessException(code, code);
    }
  }

  private LocalDate parseRequiredDate(String date, String code) {
    String normalized = normalizeRequiredText(date, code);
    try {
      return LocalDate.parse(normalized);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(code, code);
    }
  }

  private LocalDate parseNullableDate(String date, String code) {
    String normalized = trimToNull(date);
    if (normalized == null) {
      return null;
    }
    try {
      return LocalDate.parse(normalized);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(code, code);
    }
  }

  private LocalDate parseFilterDate(String date, String code) {
    String normalized = trimToNull(date);
    if (normalized == null) {
      return null;
    }
    try {
      return LocalDate.parse(normalized);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(code, code);
    }
  }

  private String normalizeRequiredText(String value, String code) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new BusinessException(code, code);
    }
    return normalized;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String nonBlank(String first, String second, String fallback) {
    String normalizedFirst = trimToNull(first);
    if (normalizedFirst != null) {
      return normalizedFirst;
    }
    String normalizedSecond = trimToNull(second);
    return normalizedSecond == null ? fallback : normalizedSecond;
  }

  private boolean textEquals(String left, String right) {
    String normalizedLeft = trimToNull(left);
    String normalizedRight = trimToNull(right);
    return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
  }

  private String joinCsv(List<String> values) {
    if (values == null) {
      return "";
    }
    return values.stream()
        .map(this::trimToNull)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.joining(","));
  }

  private List<String> splitCsv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .toList();
  }

  private long normalizedPage(Long page) {
    return page == null || page < 1 ? 1 : page;
  }

  private long normalizedPageSize(Long pageSize) {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }
}
