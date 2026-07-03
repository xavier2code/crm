package com.cy.crm.module.reimbursement.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.reimbursement.dto.ReimbursementApproveRequest;
import com.cy.crm.module.reimbursement.dto.ReimbursementRequest;
import com.cy.crm.module.reimbursement.entity.Reimbursement;
import com.cy.crm.module.reimbursement.entity.ReimbursementAttachment;
import com.cy.crm.module.reimbursement.mapper.ReimbursementAttachmentMapper;
import com.cy.crm.module.reimbursement.mapper.ReimbursementMapper;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 报销服务单测
 *
 * 覆盖：
 *  - 正常生命周期：DRAFT -> PENDING -> APPROVED -> PAID
 *  - 仅申请人本人可编辑/删除/提交（其他人抛 6015）
 *  - DRAFT/REJECTED 之外的状态不允许编辑/删除/提交（抛 6011）
 *  - 非 PENDING 不允许审批（抛 6011/6012）
 *  - 非 APPROVED 不允许付款（抛 6011）
 *  - ReimbursementAttachment 仅申请人本人 + DRAFT/REJECTED 可删（抛 6015/6011）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReimbursementServiceTest {

    @Autowired private ReimbursementService reimbursementService;
    @Autowired private ReimbursementMapper reimbursementMapper;
    @Autowired private ReimbursementAttachmentMapper attachmentMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private OpportunityMapper opportunityMapper;
    @Autowired private CustomerMapper customerMapper;
    @Autowired private UnitMapper unitMapper;
    @Autowired private UserMapper userMapper;

    private Long applicantId;
    private Long otherUserId;
    private Long approverId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        // 三种角色用户
        applicantId = createUser("applicant_user", "申请人");
        otherUserId = createUser("other_user", "其他人");
        approverId = createUser("approver_user", "审批人");

        // 准备一个项目（报销必填）
        Unit unit = new Unit();
        unit.setName("报销测试单位");
        unit.setRegion("华东");
        unit.setAdminLevel(1);
        unitMapper.insert(unit);

        Customer customer = new Customer();
        customer.setUnitId(unit.getId());
        customer.setPoliceType("公安");
        customer.setName("报销测试客户");
        customerMapper.insert(customer);

        Opportunity opportunity = new Opportunity();
        opportunity.setCustomerId(customer.getId());
        opportunity.setBusinessDomain("视频监控");
        opportunity.setProjectType(1);
        opportunity.setAmount(BigDecimal.valueOf(100000));
        opportunity.setStatus(1);
        opportunity.setSubmittedBy(applicantId);
        opportunityMapper.insert(opportunity);

        Project project = new Project();
        project.setOpportunityId(opportunity.getId());
        project.setName("报销测试项目");
        project.setAmount(BigDecimal.valueOf(100000));
        project.setStatus(1);
        projectMapper.insert(project);
        projectId = project.getId();

        // 鉴权用 ALL scope（测试不依赖数据权限）
        DataScope allScope = new DataScope();
        allScope.setAll(true);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                "test", Collections.emptyList(), applicantId,
                Collections.emptyList(), Collections.emptyList(), allScope);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Long createUser(String username, String realName) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("password");
        user.setRealName(realName);
        user.setCreatedBy(1L);
        userMapper.insert(user);
        return user.getId();
    }

    private ReimbursementRequest newRequest() {
        ReimbursementRequest req = new ReimbursementRequest();
        req.setProjectId(projectId);
        req.setType("TRAVEL");
        req.setTitle("北京差旅");
        req.setDescription("客户拜访");
        req.setAmount(new BigDecimal("1500.00"));
        req.setExpenseDate(LocalDate.of(2026, 6, 1));
        return req;
    }

    @Test
    public void shouldCreateReimbursementAsDraft() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        Reimbursement entity = reimbursementMapper.selectById(id);
        assertNotNull(entity);
        assertEquals(ReimbursementService.STATUS_DRAFT, entity.getStatus());
        assertEquals(applicantId, entity.getApplicantId());
        assertEquals("报销测试项目", entity.getProjectNameSnapshot());
    }

    @Test
    public void shouldThrowWhenProjectNotFound() {
        ReimbursementRequest req = newRequest();
        req.setProjectId(99999999L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.createReimbursement(req, applicantId));
        assertEquals(1002, ex.getCode());
    }

    @Test
    public void shouldThrowWhenUpdateByNonApplicant() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.updateReimbursement(id, newRequest(), otherUserId));
        assertEquals(6015, ex.getCode());
    }

    @Test
    public void shouldThrowWhenDeleteByNonApplicant() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.deleteReimbursement(id, otherUserId));
        assertEquals(6015, ex.getCode());
    }

    @Test
    public void shouldSubmitDraftToPending() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        reimbursementService.submitReimbursement(id, applicantId);
        Reimbursement entity = reimbursementMapper.selectById(id);
        assertEquals(ReimbursementService.STATUS_PENDING, entity.getStatus());
    }

    @Test
    public void shouldApprovePending() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        reimbursementService.submitReimbursement(id, applicantId);
        ReimbursementApproveRequest approve = new ReimbursementApproveRequest();
        approve.setResult("APPROVED");
        approve.setComment("通过");
        reimbursementService.approveReimbursement(id, approve, approverId, "审批人");
        Reimbursement entity = reimbursementMapper.selectById(id);
        assertEquals(ReimbursementService.STATUS_APPROVED, entity.getStatus());
        assertEquals(approverId, entity.getApproverId());
        assertEquals("审批人", entity.getApproverNameSnapshot());
    }

    @Test
    public void shouldThrowWhenApproveNonPending() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        // DRAFT 状态审批应失败
        ReimbursementApproveRequest approve = new ReimbursementApproveRequest();
        approve.setResult("APPROVED");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.approveReimbursement(id, approve, approverId, "审批人"));
        assertEquals(6011, ex.getCode());
    }

    @Test
    public void shouldMarkPaidAfterApproved() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        reimbursementService.submitReimbursement(id, applicantId);
        ReimbursementApproveRequest approve = new ReimbursementApproveRequest();
        approve.setResult("APPROVED");
        reimbursementService.approveReimbursement(id, approve, approverId, "审批人");
        reimbursementService.markPaid(id, approverId);
        Reimbursement entity = reimbursementMapper.selectById(id);
        assertEquals(ReimbursementService.STATUS_PAID, entity.getStatus());
        assertNotNull(entity.getPaidAt());
    }

    @Test
    public void shouldThrowWhenMarkPaidNonApproved() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        // DRAFT 不能直接付款
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.markPaid(id, approverId));
        assertEquals(6011, ex.getCode());
    }

    @Test
    public void shouldDeleteDraft() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        reimbursementService.deleteReimbursement(id, applicantId);
        assertNull(reimbursementMapper.selectById(id));
    }

    @Test
    public void shouldThrowWhenDeleteAttachmentByNonApplicant() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        ReimbursementAttachment att = new ReimbursementAttachment();
        att.setReimbursementId(id);
        att.setFileName("test.pdf");
        att.setFilePath("2026-06/dummy.pdf");
        att.setFileSize(1024L);
        att.setUploadedBy(applicantId);
        attachmentMapper.insert(att);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.deleteAttachment(att.getId(), otherUserId));
        assertEquals(6015, ex.getCode());
    }

    @Test
    public void shouldThrowWhenDeleteAttachmentOfPending() {
        Long id = reimbursementService.createReimbursement(newRequest(), applicantId);
        reimbursementService.submitReimbursement(id, applicantId);
        ReimbursementAttachment att = new ReimbursementAttachment();
        att.setReimbursementId(id);
        att.setFileName("test.pdf");
        att.setFilePath("2026-06/dummy.pdf");
        att.setFileSize(1024L);
        att.setUploadedBy(applicantId);
        attachmentMapper.insert(att);

        // PENDING 状态申请人本人也不能删附件
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reimbursementService.deleteAttachment(att.getId(), applicantId));
        assertEquals(6011, ex.getCode());
    }
}
