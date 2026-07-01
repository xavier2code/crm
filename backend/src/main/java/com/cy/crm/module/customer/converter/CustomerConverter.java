package com.cy.crm.module.customer.converter;

import com.cy.crm.module.customer.dto.ContactRequest;
import com.cy.crm.module.customer.dto.CustomerRequest;
import com.cy.crm.module.customer.entity.Contact;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.vo.ContactVO;
import com.cy.crm.module.customer.vo.CustomerVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

/**
 * 客户对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface CustomerConverter {

    /**
     * 请求 DTO -> 实体
     */
    Customer requestToEntity(CustomerRequest request);

    /**
     * 更新实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "ownerUserId", ignore = true)
    @Mapping(target = "region", ignore = true)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);

    /**
     * 联系人请求 -> 联系人实体
     */
    Contact contactRequestToEntity(ContactRequest request);

    /**
     * 更新联系人实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    void updateContactFromRequest(ContactRequest request, @MappingTarget Contact contact);

    /**
     * 实体 -> VO
     * unitName/policeTypeName/ownerUserName/contacts 由 Service 层设置
     */
    @Mapping(target = "unitName", ignore = true)
    @Mapping(target = "policeTypeName", ignore = true)
    @Mapping(target = "ownerUserName", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    CustomerVO entityToVO(Customer customer);

    /**
     * 联系人实体 -> VO
     * contactTypeName 由 Service 层设置
     */
    @Mapping(target = "contactTypeName", ignore = true)
    ContactVO contactEntityToVO(Contact contact);

    /**
     * 联系人列表映射
     */
    List<ContactVO> contactEntitiesToVOs(List<Contact> contacts);
}
