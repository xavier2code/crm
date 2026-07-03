package com.cy.crm.module.reimbursement.converter;

import com.cy.crm.module.reimbursement.dto.ReimbursementRequest;
import com.cy.crm.module.reimbursement.entity.Reimbursement;
import com.cy.crm.module.reimbursement.entity.ReimbursementAttachment;
import com.cy.crm.module.reimbursement.vo.ReimbursementAttachmentVO;
import com.cy.crm.module.reimbursement.vo.ReimbursementVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 报销对象映射
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface ReimbursementConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectNameSnapshot", ignore = true)
    @Mapping(target = "applicantId", ignore = true)
    @Mapping(target = "applicantNameSnapshot", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approverId", ignore = true)
    @Mapping(target = "approverNameSnapshot", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvalComment", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    Reimbursement requestToEntity(ReimbursementRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectNameSnapshot", ignore = true)
    @Mapping(target = "applicantId", ignore = true)
    @Mapping(target = "applicantNameSnapshot", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approverId", ignore = true)
    @Mapping(target = "approverNameSnapshot", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvalComment", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(ReimbursementRequest request, @MappingTarget Reimbursement reimbursement);

    @Mapping(target = "projectName", source = "projectNameSnapshot")
    @Mapping(target = "applicantName", source = "applicantNameSnapshot")
    @Mapping(target = "typeName", ignore = true)
    @Mapping(target = "approverName", source = "approverNameSnapshot")
    @Mapping(target = "attachments", ignore = true)
    ReimbursementVO entityToVO(Reimbursement reimbursement);

    @Mapping(target = "uploadedAt", source = "createdAt")
    ReimbursementAttachmentVO attachmentToVO(ReimbursementAttachment attachment);

    List<ReimbursementAttachmentVO> attachmentsToVOs(List<ReimbursementAttachment> attachments);
}
