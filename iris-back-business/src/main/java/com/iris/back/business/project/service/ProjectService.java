package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.entity.BizChecklistEntity;
import com.iris.back.business.checklist.model.entity.BizChecklistItemEntity;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.model.entity.BizPlanItemEntity;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectTaskMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.dto.ProjectMemberDto;
import com.iris.back.business.project.model.dto.ProjectTaskDto;
import com.iris.back.business.project.model.dto.ProjectTaskWorkOrderDto;
import com.iris.back.business.project.model.entity.BizProjectEntity;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskWorkOrderEntity;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.model.request.ProjectTaskAssignRequest;
import com.iris.back.business.project.model.request.ProjectUpsertRequest;
import com.iris.back.business.project.model.request.ProjectWorkOrderCreateRequest;
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
  private final BizProjectMemberMapper projectMemberMapper;
  private final BizProjectTaskMapper projectTaskMapper;
  private final BizProjectTaskWorkOrderMapper projectTaskWorkOrderMapper;
  private final BizChecklistMapper checklistMapper;
  private final BizChecklistItemMapper checklistItemMapper;
  private final BizPlanItemMapper planItemMapper;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;
  private final OmsClient omsClient;
  private final ObjectMapper objectMapper;

  public ProjectService(
      BizProjectMapper projectMapper,
      BizProjectMemberMapper projectMemberMapper,
      BizProjectTaskMapper projectTaskMapper,
      BizProjectTaskWorkOrderMapper projectTaskWorkOrderMapper,
      BizChecklistMapper checklistMapper,
      BizChecklistItemMapper checklistItemMapper,
      BizPlanItemMapper planItemMapper,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator,
      OmsClient omsClient,
      ObjectMapper objectMapper
  ) {
    this.projectMapper = projectMapper;
    this.projectMemberMapper = projectMemberMapper;
    this.projectTaskMapper = projectTaskMapper;
    this.projectTaskWorkOrderMapper = projectTaskWorkOrderMapper;
    this.checklistMapper = checklistMapper;
    this.checklistItemMapper = checklistItemMapper;
    this.planItemMapper = planItemMapper;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
    this.omsClient = omsClient;
    this.objectMapper = objectMapper;
  }

  public PageResponse<ProjectDto> list(ProjectListQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ProjectListQuery safeQuery = query == null
        ? new ProjectListQuery(null, null, null, null, null, null, 1L, 10L)
        : query;
    List<BizProjectMemberEntity> myMemberships = projectMemberMapper.selectList(
        new LambdaQueryWrapper<BizProjectMemberEntity>()
            .eq(BizProjectMemberEntity::getTenantId, principal.tenantId())
            .eq(BizProjectMemberEntity::getPersonnelId, principal.userId())
    );
    Set<Long> visibleProjectIds = myMemberships.stream()
        .map(BizProjectMemberEntity::getProjectId)
        .collect(Collectors.toSet());
    if (visibleProjectIds.isEmpty()) {
      return PageResponse.of(0, normalizedPage(safeQuery.page()), normalizedPageSize(safeQuery.pageSize()), List.of());
    }

    List<BizProjectEntity> projects = projectMapper.selectList(new LambdaQueryWrapper<BizProjectEntity>()
        .eq(BizProjectEntity::getTenantId, principal.tenantId())
        .orderByDesc(BizProjectEntity::getUpdatedAt)
        .orderByDesc(BizProjectEntity::getId));
    List<BizProjectEntity> filteredProjects = projects.stream()
        .filter(project -> visibleProjectIds.contains(project.getId()))
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
    return toDto(project, members, listTasks(principal.tenantId(), project.getId()));
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
    project.setUpdatedBy(principal.userId());
    projectMapper.updateById(project);
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
    return toDto(project, members, tasks);
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
  }

  public List<ProjectTaskWorkOrderDto> listTaskWorkOrders(String projectId, String taskId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long parsedProjectId = parseId(projectId, "PROJECT_ID_INVALID");
    Long parsedTaskId = parseId(taskId, "PROJECT_TASK_ID_INVALID");
    BizProjectEntity project = requireProject(parsedProjectId, principal.tenantId());
    BizProjectTaskEntity task = requireTask(parsedTaskId, parsedProjectId, principal.tenantId());
    ensureTaskWorkOrderAccess(project, task, principal, listMembers(principal.tenantId(), project.getId()));
    return nullToList(projectTaskWorkOrderMapper.selectList(
        new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
            .eq(BizProjectTaskWorkOrderEntity::getTenantId, principal.tenantId())
            .eq(BizProjectTaskWorkOrderEntity::getProjectId, project.getId())
            .eq(BizProjectTaskWorkOrderEntity::getTaskId, task.getId())
            .orderByDesc(BizProjectTaskWorkOrderEntity::getUpdatedAt)
            .orderByDesc(BizProjectTaskWorkOrderEntity::getId)
    )).stream().map(this::toWorkOrderDto).toList();
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
    List<OmsClient.OmsCreateCommand> commands = handlers.stream()
        .map(handler -> new OmsClient.OmsCreateCommand(
            handler.handlerId(),
            handler.handlerName(),
            trimToNull(request.title()) == null ? task.getTaskName() : request.title().trim(),
            trimToNull(request.description()) == null ? task.getTaskDescription() : request.description().trim(),
            task.getId() + ":" + handler.handlerId()
        ))
        .toList();
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
        .map(command -> saveWorkOrder(project, task, principal, command, resultByHandlerId, existingByKey))
        .toList();
    if (!"in_progress".equals(task.getStatus())) {
      task.setStatus("in_progress");
      task.setUpdatedBy(principal.userId());
      projectTaskMapper.updateById(task);
    }
    return workOrders.stream().map(this::toWorkOrderDto).toList();
  }

  @Transactional
  public ProjectTaskWorkOrderDto refreshWorkOrder(String projectId, String taskId, String workOrderId) {
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
    String omsWorkOrderId = normalizeRequiredText(workOrder.getOmsWorkOrderId(), "PROJECT_WORK_ORDER_OMS_ID_REQUIRED");
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
    projectTaskWorkOrderMapper.updateById(workOrder);
    return toWorkOrderDto(workOrder);
  }

  @Transactional
  public ProjectDto create(ProjectUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    List<Long> checklistIds = parseIds(request.checklistIds(), "PROJECT_CHECKLIST_ID_INVALID");
    List<BizChecklistEntity> checklists = loadChecklists(principal.tenantId(), checklistIds);
    List<BizChecklistItemEntity> checklistItems = loadChecklistItems(principal.tenantId(), checklistIds);
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
    return toDto(project, members, tasks);
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
    boolean visible = Objects.equals(project.getLeaderId(), principal.userId())
        || members.stream().anyMatch(member -> Objects.equals(member.getPersonnelId(), principal.userId()));
    if (!visible) {
      throw new BusinessException("PROJECT_FORBIDDEN", "PROJECT_FORBIDDEN");
    }
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
      Map<String, OmsClient.OmsCreateResult> resultByHandlerId,
      Map<String, BizProjectTaskWorkOrderEntity> existingByKey
  ) {
    OmsClient.OmsCreateResult result = resultByHandlerId.get(command.handlerId());
    BizProjectTaskWorkOrderEntity workOrder = existingByKey.get(command.idempotencyKey());
    boolean create = workOrder == null;
    if (create) {
      workOrder = new BizProjectTaskWorkOrderEntity();
      workOrder.setId(nextId(workOrder));
      workOrder.setTenantId(principal.tenantId());
      workOrder.setProjectId(project.getId());
      workOrder.setTaskId(task.getId());
      workOrder.setIdempotencyKey(command.idempotencyKey());
      workOrder.setHandlerId(parseId(command.handlerId(), "PROJECT_WORK_ORDER_HANDLER_ID_INVALID"));
      workOrder.setHandlerName(command.handlerName());
      workOrder.setDeleted(0);
      workOrder.setVersion(0L);
      workOrder.setCreatedBy(principal.userId());
    }
    workOrder.setWorkOrderTitle(command.title());
    workOrder.setWorkOrderDescription(command.description());
    workOrder.setRequestPayload(command.toString());
    workOrder.setResponsePayload(result == null ? null : result.responsePayload());
    workOrder.setOmsWorkOrderId(result == null ? null : result.omsWorkOrderId());
    workOrder.setSyncStatus(result == null || trimToNull(result.error()) != null ? "failed" : "synced");
    workOrder.setSyncError(result == null ? "OMS_CREATE_NO_RESULT" : trimToNull(result.error()));
    workOrder.setOmsStatus(result == null ? null : result.status());
    workOrder.setOmsStatusName(result == null ? null : result.status());
    workOrder.setIrisReviewStatus("pending");
    workOrder.setReviewLocked(0);
    workOrder.setUpdatedBy(principal.userId());
    if (create) {
      projectTaskWorkOrderMapper.insert(workOrder);
    } else {
      projectTaskWorkOrderMapper.updateById(workOrder);
    }
    return workOrder;
  }

  private ProjectDto toDto(
      BizProjectEntity project,
      List<BizProjectMemberEntity> members,
      List<BizProjectTaskEntity> tasks
  ) {
    int taskCount = tasks.size();
    long passedCount = tasks.stream().filter(task -> "passed".equals(task.getStatus())).count();
    long nonconformingCount = tasks.stream().filter(task -> "nonconforming".equals(task.getStatus())).count();
    int progress = taskCount == 0 ? 0 : (int) ((passedCount + nonconformingCount) * 100 / taskCount);
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
        tasks.stream().map(this::toTaskDto).toList(),
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
        0,
        0,
        0,
        List.of(),
        List.of("assign")
    );
  }

  private ProjectTaskWorkOrderDto toWorkOrderDto(BizProjectTaskWorkOrderEntity workOrder) {
    return new ProjectTaskWorkOrderDto(
        String.valueOf(workOrder.getId()),
        String.valueOf(workOrder.getProjectId()),
        String.valueOf(workOrder.getTaskId()),
        workOrder.getOmsWorkOrderId(),
        workOrder.getIdempotencyKey(),
        workOrder.getHandlerId() == null ? null : String.valueOf(workOrder.getHandlerId()),
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
        Objects.equals(workOrder.getReviewLocked(), 1),
        isOmsReviewable(workOrder.getOmsStatus())
    );
  }

  private boolean isOmsReviewable(String omsStatus) {
    return "20".equals(omsStatus) || "25".equals(omsStatus) || "30".equals(omsStatus);
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
