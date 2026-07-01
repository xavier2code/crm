package com.cy.crm.module.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.util.FieldMaskUtil;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.DataScopeValidator;
import com.cy.crm.security.SecurityContext;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.customer.converter.CustomerConverter;
import com.cy.crm.module.customer.dto.ContactRequest;
import com.cy.crm.module.customer.dto.CustomerRequest;
import com.cy.crm.module.customer.entity.Contact;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.ContactMapper;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.customer.vo.ContactVO;
import com.cy.crm.module.customer.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {

    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final UnitMapper unitMapper;
    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final CustomerConverter customerConverter;
    private final DataScopeValidator dataScopeValidator;

    public Page<CustomerVO> pageCustomers(Long current, Long size, String keyword, Long userId, List<Long> roleIds) {
        QueryWrapper<Customer> wrapper = new QueryWrapper<Customer>()
                .like(keyword != null, "name", keyword)
                .eq(hasOnlyBDRole(roleIds), "owner_user_id", userId)
                .orderByDesc("created_at");
        Page<Customer> page = customerMapper.selectPage(new Page<>(current, size), wrapper);
        Page<CustomerVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public CustomerVO getCustomerById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            return null;
        }

        // IDOR protection: validate access to this customer
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);

        return toVO(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCustomer(CustomerRequest request, Long userId) {
        Unit unit = unitMapper.selectById(request.getUnitId());
        if (unit == null) {
            throw BusinessException.unitNotFound();
        }

        QueryWrapper<Customer> wrapper = new QueryWrapper<Customer>()
                .eq("unit_id", request.getUnitId())
                .eq("police_type", request.getPoliceType());
        if (customerMapper.selectCount(wrapper) > 0) {
            throw BusinessException.customerExists();
        }

        Customer customer = customerConverter.requestToEntity(request);
        customer.setName(unit.getName() + "-" + request.getPoliceType());
        customer.setRegion(unit.getRegion());
        customer.setOwnerUserId(userId);
        customer.setCreatedBy(userId);
        try {
            customerMapper.insert(customer);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw BusinessException.customerExists();
        }

        if (request.getContacts() != null && !request.getContacts().isEmpty()) {
            saveContacts(customer.getId(), request.getContacts());
        }

        return customer.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw BusinessException.customerNotFound();
        }

        // IDOR protection: validate access to this customer
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);

        if (!customer.getPoliceType().equals(request.getPoliceType()) ||
                !customer.getUnitId().equals(request.getUnitId())) {
            QueryWrapper<Customer> wrapper = new QueryWrapper<Customer>()
                    .eq("unit_id", request.getUnitId())
                    .eq("police_type", request.getPoliceType())
                    .ne("id", id);
            if (customerMapper.selectCount(wrapper) > 0) {
                throw BusinessException.customerExists();
            }
        }

        Unit unit = unitMapper.selectById(request.getUnitId());
        if (unit == null) {
            throw BusinessException.unitNotFound();
        }

        customer.setName(unit.getName() + "-" + request.getPoliceType());
        customer.setUnitId(request.getUnitId());
        customer.setPoliceType(request.getPoliceType());
        customer.setCustomerLayer(request.getCustomerLayer());
        customer.setRegion(unit.getRegion());
        customerMapper.updateById(customer);

        if (request.getContacts() != null) {
            contactMapper.delete(new QueryWrapper<Contact>().eq("customer_id", id));
            saveContacts(id, request.getContacts());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            return;
        }

        // IDOR protection: validate access to this customer
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);

        customerMapper.deleteById(id);
        contactMapper.delete(new QueryWrapper<Contact>().eq("customer_id", id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignCustomer(Long customerId, Long userId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw BusinessException.customerNotFound();
        }

        // IDOR protection: validate access to this customer
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);

        customer.setOwnerUserId(userId);
        customerMapper.updateById(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addContact(Long customerId, ContactRequest request) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw BusinessException.customerNotFound();
        }

        // IDOR protection: validate access to this customer
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);

        if (request.getIsPrimary() != null && request.getIsPrimary() == 1) {
            Contact updateContact = new Contact();
            updateContact.setIsPrimary(0);
            contactMapper.update(updateContact,
                    new QueryWrapper<Contact>()
                            .eq("customer_id", customerId)
                            .eq("contact_type", request.getContactType()));
        }

        Contact contact = customerConverter.contactRequestToEntity(request);
        contact.setCustomerId(customerId);
        contactMapper.insert(contact);

        return contact.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateContact(Long id, ContactRequest request) {
        Contact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw BusinessException.contactNotFound();
        }

        if (request.getIsPrimary() != null && request.getIsPrimary() == 1) {
            Contact updateContact = new Contact();
            updateContact.setIsPrimary(0);
            contactMapper.update(updateContact,
                    new QueryWrapper<Contact>()
                            .eq("customer_id", contact.getCustomerId())
                            .eq("contact_type", request.getContactType())
                            .ne("id", id));
        }

        customerConverter.updateContactFromRequest(request, contact);
        contactMapper.updateById(contact);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteContact(Long id) {
        contactMapper.deleteById(id);
    }

    private void saveContacts(Long customerId, List<ContactRequest> contacts) {
        for (ContactRequest req : contacts) {
            Contact contact = customerConverter.contactRequestToEntity(req);
            contact.setCustomerId(customerId);
            contactMapper.insert(contact);
        }
    }

    private boolean hasOnlyBDRole(List<Long> roleIds) {
        return roleIds != null && roleIds.size() == 1 && roleIds.contains(4L);
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = customerConverter.entityToVO(customer);

        Unit unit = unitMapper.selectById(customer.getUnitId());
        if (unit != null) {
            vo.setUnitName(unit.getName());
        }

        vo.setPoliceTypeName(dictionaryService.getDictionaryName("police_type", customer.getPoliceType()));

        if (customer.getOwnerUserId() != null) {
            com.cy.crm.module.admin.entity.User owner = userService.getUserEntityById(customer.getOwnerUserId());
            vo.setOwnerUserName(owner != null ? owner.getRealName() : null);
        }

        List<Contact> contacts = contactMapper.selectList(
                new QueryWrapper<Contact>().eq("customer_id", customer.getId())
        );
        vo.setContacts(customerConverter.contactEntitiesToVOs(contacts));

        return vo;
    }

    private ContactVO toContactVO(Contact contact) {
        ContactVO vo = customerConverter.contactEntityToVO(contact);
        vo.setContactTypeName(dictionaryService.getDictionaryName("contact_type", String.valueOf(contact.getContactType())));
        // 字段级权限：手机号脱敏
        vo.setPhone(FieldMaskUtil.maskPhone(vo.getPhone()));
        return vo;
    }
}
