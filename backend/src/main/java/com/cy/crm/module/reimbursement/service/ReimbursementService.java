package com.cy.crm.module.reimbursement.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.service.FileStorageService;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.reimbursement.converter.ReimbursementConverter;
import com.cy.crm.module.reimbursement.dto.ReimbursementApproveRequest;
import com.cy.crm.module.reimbursement.dto.ReimbursementRequest;
import com.cy.crm.module.reimbursement.entity.Reimbursement;
import com.cy.crm.module.reimbursement.entity.ReimbursementAttachment;
import com.cy.crm.module.reimbursement.mapper.ReimbursementAttachmentMapper;
import com.cy.crm.module.reimbursement.mapper.ReimbursementMapper;
import com.cy.crm.module.reimbursement.vo.ReimbursementAttachmentVO;
import com.cy.crm.module.reimbursement.vo.ReimbursementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReimbursementService extends ServiceImpl<ReimbursementMapper, Reimbursement> {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PAID = "PAID";

    private final ReimbursementMapper reimbursementMapper;
    private final ReimbursementAttachmentMapper attachmentMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final DictionaryService dictionaryService;
    private final FileStorageService fileStorageService;
    private final ReimbursementConverter converter;

    /**
     * 分页查询
     *
     * @param status      状态过滤
     * @param type        类型过滤
     * @param projectId   项目过滤
     * @param applicantId 申请人过滤
     * @param mine        是否只看自己（CHANNEL_BD 默认 true）
     */
    public Page<ReimbursementVO> pageReimbursements(Long current, Long size,
                                                   String status, String type,
                                                   Long projectId, Long applicantId,
                                                   boolean mine, Long currentUserId) {
        QueryWrapper<Reimbursement> wrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) wrapper.eq("status", status);
        if (type != null && !type.isEmpty()) wrapper.eq("type", type);
        if (projectId != null) wrapper.eq("project_id", projectId);
        if (applicantId != null) wrapper.eq("applicant_id", applicantId);
        if (mine) wrapper.eq("applicant_id", currentUserId);
        wrapper.orderByDesc("created_at");

        Page<Reimbursement> page = reimbursementMapper.selectPage(new Page<>(current, size), wrapper);
        Page<ReimbursementVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ReimbursementVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    public ReimbursementVO getReimbursement(Long id) {
        Reimbursement entity = reimbursementMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.reimbursementNotFound();
        }
        ReimbursementVO vo = toVO(entity);
        vo.setAttachments(listAttachments(id));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createReimbursement(ReimbursementRequest request, Long userId) {
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }
        Reimbursement entity = converter.requestToEntity(request);
        entity.setProjectNameSnapshot(project.getName());
        entity.setApplicantId(userId);
        entity.setApplicantNameSnapshot(user.getRealName());
        entity.setStatus(STATUS_DRAFT);
        entity.setCreatedBy(userId);
        reimbursementMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateReimbursement(Long id, ReimbursementRequest request, Long userId) {
        Reimbursement entity = mustGetOwnDraftOrRejected(id, userId);
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }
        converter.updateEntityFromRequest(request, entity);
        entity.setProjectNameSnapshot(project.getName());
        reimbursementMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReimbursement(Long id, Long userId) {
        Reimbursement entity = mustGetOwnDraftOrRejected(id, userId);
        // 同步删附件（软删）
        List<ReimbursementAttachment> attachments = attachmentMapper.selectList(
                new QueryWrapper<ReimbursementAttachment>().eq("reimbursement_id", id));
        for (ReimbursementAttachment att : attachments) {
            attachmentMapper.deleteById(att.getId());
        }
        reimbursementMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitReimbursement(Long id, Long userId) {
        Reimbursement entity = mustGetOwnDraftOrRejected(id, userId);
        entity.setStatus(STATUS_PENDING);
        // 重新提交时清掉旧的审批痕迹
        entity.setApproverId(null);
        entity.setApproverNameSnapshot(null);
        entity.setApprovedAt(null);
        entity.setApprovalComment(null);
        reimbursementMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveReimbursement(Long id, ReimbursementApproveRequest request, Long approverId, String approverName) {
        Reimbursement entity = reimbursementMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.reimbursementNotFound();
        }
        if (!STATUS_PENDING.equals(entity.getStatus())) {
            throw BusinessException.reimbursementStatusInvalid();
        }
        if (STATUS_APPROVED.equals(request.getResult())) {
            entity.setStatus(STATUS_APPROVED);
        } else if (STATUS_REJECTED.equals(request.getResult())) {
            entity.setStatus(STATUS_REJECTED);
        } else {
            throw BusinessException.paramError("审批结果必须为 APPROVED 或 REJECTED");
        }
        entity.setApproverId(approverId);
        entity.setApproverNameSnapshot(approverName);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovalComment(request.getComment());
        reimbursementMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long id, Long operatorId) {
        Reimbursement entity = reimbursementMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.reimbursementNotFound();
        }
        if (!STATUS_APPROVED.equals(entity.getStatus())) {
            throw BusinessException.reimbursementStatusInvalid();
        }
        entity.setStatus(STATUS_PAID);
        entity.setPaidAt(LocalDateTime.now());
        reimbursementMapper.updateById(entity);
        log.info("Reimbursement {} marked PAID by operator {}", id, operatorId);
    }

    // ========== 附件 ==========

    @Transactional(rollbackFor = Exception.class)
    public ReimbursementAttachmentVO uploadAttachment(Long reimbursementId, MultipartFile file, Long userId) {
        Reimbursement entity = reimbursementMapper.selectById(reimbursementId);
        if (entity == null) {
            throw BusinessException.reimbursementNotFound();
        }
        // 只有 DRAFT/REJECTED 状态下申请人本人可上传；其他状态由审批/财务端上传
        if (STATUS_DRAFT.equals(entity.getStatus()) || STATUS_REJECTED.equals(entity.getStatus())) {
            if (!userId.equals(entity.getApplicantId())) {
                throw BusinessException.reimbursementNotApplicant();
            }
        }
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        ReimbursementAttachment att = new ReimbursementAttachment();
        att.setReimbursementId(reimbursementId);
        att.setFileName(stored.originalName());
        att.setFilePath(stored.relativePath());
        att.setFileSize(stored.size());
        att.setContentType(stored.contentType());
        att.setUploadedBy(userId);
        attachmentMapper.insert(att);
        return converter.attachmentToVO(att);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long attachmentId, Long userId) {
        ReimbursementAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null) {
            throw BusinessException.reimbursementAttachmentNotFound();
        }
        Reimbursement entity = reimbursementMapper.selectById(att.getReimbursementId());
        if (entity == null) {
            throw BusinessException.reimbursementNotFound();
        }
        if (!userId.equals(entity.getApplicantId())) {
            throw BusinessException.reimbursementNotApplicant();
        }
        if (!(STATUS_DRAFT.equals(entity.getStatus()) || STATUS_REJECTED.equals(entity.getStatus()))) {
            throw BusinessException.reimbursementStatusInvalid();
        }
        attachmentMapper.deleteById(attachmentId);
    }

    public List<ReimbursementAttachmentVO> listAttachments(Long reimbursementId) {
        List<ReimbursementAttachment> list = attachmentMapper.selectList(
                new QueryWrapper<ReimbursementAttachment>().eq("reimbursement_id", reimbursementId));
        if (list.isEmpty()) return Collections.emptyList();
        return converter.attachmentsToVOs(list);
    }

    public ReimbursementAttachment getAttachmentEntity(Long attachmentId) {
        ReimbursementAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null) {
            throw BusinessException.reimbursementAttachmentNotFound();
        }
        return att;
    }

    // ========== 内部辅助 ==========

    private ReimbursementVO toVO(Reimbursement entity) {
        ReimbursementVO vo = converter.entityToVO(entity);
        vo.setTypeName(dictionaryService.getDictionaryName("reimbursement_type", entity.getType()));
        return vo;
    }

    private Reimbursement mustGetOwnDraftOrRejected(Long id, Long userId) {
        Reimbursement entity = reimbursementMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.reimbursementNotFound();
        }
        if (!userId.equals(entity.getApplicantId())) {
            throw BusinessException.reimbursementNotApplicant();
        }
        if (!(STATUS_DRAFT.equals(entity.getStatus()) || STATUS_REJECTED.equals(entity.getStatus()))) {
            throw BusinessException.reimbursementStatusInvalid();
        }
        return entity;
    }
}
