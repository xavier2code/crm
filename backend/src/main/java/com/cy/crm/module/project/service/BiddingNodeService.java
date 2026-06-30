package com.cy.crm.module.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.project.converter.BiddingNodeConverter;
import com.cy.crm.module.project.dto.BiddingNodeRequest;
import com.cy.crm.module.project.entity.BiddingNode;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.BiddingNodeMapper;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiddingNodeService extends ServiceImpl<BiddingNodeMapper, BiddingNode> {

    private final BiddingNodeMapper biddingNodeMapper;
    private final ProjectMapper projectMapper;
    private final DictionaryService dictionaryService;
    private final NotificationService notificationService;
    private final BiddingNodeConverter biddingNodeConverter;

    // 采购方式常量
    public static final int PURCHASE_OPEN = 1;         // 公开招标
    public static final int PURCHASE_INVITE = 2;        // 邀请招标
    public static final int PURCHASE_COMPETITIVE = 3;  // 竞争性谈判
    public static final int PURCHASE_SINGLE = 4;       // 单一来源
    public static final int PURCHASE_DIRECT = 5;        // 直接采购

    /**
     * 获取项目招投标节点
     */
    public BiddingNode getByProjectId(Long projectId) {
        return biddingNodeMapper.selectOne(
                new QueryWrapper<BiddingNode>().eq("project_id", projectId)
        );
    }

    /**
     * 创建或更新招投标节点
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveBiddingNode(Long projectId, BiddingNodeRequest request) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }

        BiddingNode node = biddingNodeMapper.selectOne(
                new QueryWrapper<BiddingNode>().eq("project_id", projectId)
        );

        BiddingNode oldNode = null;
        if (node != null) {
            oldNode = cloneNode(node);
            biddingNodeConverter.updateEntityFromRequest(request, node);
            node.setUpdatedAt(java.time.LocalDateTime.now());
            biddingNodeMapper.updateById(node);
        } else {
            node = biddingNodeConverter.requestToEntity(request);
            node.setProjectId(projectId);
            node.setCreatedAt(java.time.LocalDateTime.now());
            node.setUpdatedAt(java.time.LocalDateTime.now());
            biddingNodeMapper.insert(node);
        }

        // 检测关键日期变更并发送通知
        detectAndNotifyKeyEvents(project, oldNode, node);

        return node.getId();
    }

    /**
     * 检测关键事件并发送通知
     */
    private void detectAndNotifyKeyEvents(Project project, BiddingNode oldNode, BiddingNode newNode) {
        if (oldNode == null) return;

        // 检测投标日期
        if (isDateChanged(oldNode.getBidDate(), newNode.getBidDate())) {
            notifyBiddingDate(project, newNode);
        }

        // 检测中标结果
        if (isDateChanged(oldNode.getBidResultStart(), newNode.getBidResultStart())) {
            notifyBidResult(project, newNode);
        }

        // 检测中标通知书收到
        if (isDateChanged(oldNode.getNoticeReceivedDate(), newNode.getNoticeReceivedDate())) {
            notifyNoticeReceived(project, newNode);
        }
    }

    private boolean isDateChanged(LocalDate oldValue, LocalDate newValue) {
        return (oldValue == null) && (newValue != null);
    }

    private void notifyBiddingDate(Project project, BiddingNode node) {
        if (project.getOwnerBdId() != null) {
            notificationService.createNotification(
                    project.getOwnerBdId(),
                    "投标日期提醒",
                    String.format("项目 %s 的投标日期为 %s，请做好准备。", project.getName(), node.getBidDate()),
                    "BIDDING_DATE",
                    project.getId()
            );
        }
    }

    private void notifyBidResult(Project project, BiddingNode node) {
        if (project.getOwnerBdId() != null) {
            notificationService.createNotification(
                    project.getOwnerBdId(),
                    "中标结果公布",
                    String.format("项目 %s 的中标结果已开始公布，请及时关注。", project.getName()),
                    "BID_RESULT",
                    project.getId()
            );
        }
    }

    private void notifyNoticeReceived(Project project, BiddingNode node) {
        if (project.getOwnerBdId() != null) {
            notificationService.createNotification(
                    project.getOwnerBdId(),
                    "中标通知书收到",
                    String.format("项目 %s 的中标通知书已于 %s 收到。", project.getName(), node.getNoticeReceivedDate()),
                    "NOTICE_RECEIVED",
                    project.getId()
            );
        }
    }

    private BiddingNode cloneNode(BiddingNode source) {
        BiddingNode target = new BiddingNode();
        target.setId(source.getId());
        target.setProjectId(source.getProjectId());
        target.setBiddingAgency(source.getBiddingAgency());
        target.setPurchaseMethod(source.getPurchaseMethod());
        target.setAnnouncementDate(source.getAnnouncementDate());
        target.setRegistrationStart(source.getRegistrationStart());
        target.setRegistrationEnd(source.getRegistrationEnd());
        target.setBidDate(source.getBidDate());
        target.setBidResultStart(source.getBidResultStart());
        target.setBidResultEnd(source.getBidResultEnd());
        target.setNoticeReceivedDate(source.getNoticeReceivedDate());
        target.setNoticeOriginalArchived(source.getNoticeOriginalArchived());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    public String getPurchaseMethodName(Integer method) {
        return switch (method) {
            case PURCHASE_OPEN -> "公开招标";
            case PURCHASE_INVITE -> "邀请招标";
            case PURCHASE_COMPETITIVE -> "竞争性谈判";
            case PURCHASE_SINGLE -> "单一来源";
            case PURCHASE_DIRECT -> "直接采购";
            default -> "未知";
        };
    }
}
