package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.DictionaryConverter;
import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.entity.Dictionary;
import com.cy.crm.module.admin.mapper.DictionaryMapper;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.vo.DictionaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private DictionaryMapper dictionaryMapper;

    @Mock
    private UnitMapper unitMapper;

    @Mock
    private DictionaryConverter dictionaryConverter;

    @InjectMocks
    private DictionaryService dictionaryService;

    @Test
    void listByType_shouldReturnConvertedList() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setType("test_type");

        when(dictionaryMapper.selectByType("test_type")).thenReturn(List.of(dict));
        when(dictionaryConverter.entityToVO(dict)).thenReturn(new DictionaryVO());

        List<DictionaryVO> result = dictionaryService.listByType("test_type");

        assertEquals(1, result.size());
        verify(dictionaryMapper).selectByType("test_type");
    }

    @Test
    void create_shouldThrowWhenCodeDuplicate() {
        DictionaryRequest request = new DictionaryRequest();
        request.setType("test_type");
        request.setCode("EXISTING");

        when(dictionaryMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.create(request));
        assertEquals(3005, ex.getCode());
    }

    @Test
    void create_shouldInsertWhenCodeUnique() {
        DictionaryRequest request = new DictionaryRequest();
        request.setType("test_type");
        request.setCode("NEW");

        when(dictionaryMapper.selectCount(any())).thenReturn(0L);
        when(dictionaryConverter.requestToEntity(request)).thenReturn(new Dictionary());
        when(dictionaryMapper.insert(any(Dictionary.class))).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.create(request));
        verify(dictionaryMapper).insert(any(Dictionary.class));
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        DictionaryRequest request = new DictionaryRequest();
        request.setId(999L);

        when(dictionaryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.update(request));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void update_shouldUpdateWhenExists() {
        DictionaryRequest request = new DictionaryRequest();
        request.setId(1L);
        request.setName("新名称");

        Dictionary dict = new Dictionary();
        dict.setId(1L);

        when(dictionaryMapper.selectById(1L)).thenReturn(dict);
        doNothing().when(dictionaryConverter).updateEntityFromRequest(request, dict);
        when(dictionaryMapper.updateById(dict)).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.update(request));
        verify(dictionaryMapper).updateById(dict);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(dictionaryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.delete(999L));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void delete_shouldThrowWhenBuiltin() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setIsBuiltin(1);

        when(dictionaryMapper.selectById(1L)).thenReturn(dict);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.delete(1L));
        assertEquals(3009, ex.getCode());
    }

    @Test
    void delete_shouldSucceedWhenNormal() {
        Dictionary dict = new Dictionary();
        dict.setId(2L);
        dict.setIsBuiltin(0);

        when(dictionaryMapper.selectById(2L)).thenReturn(dict);
        when(dictionaryMapper.deleteById(2L)).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.delete(2L));
        verify(dictionaryMapper).deleteById(2L);
    }

    @Test
    void delete_shouldSucceedWhenIsBuiltinNull() {
        Dictionary dict = new Dictionary();
        dict.setId(3L);
        dict.setIsBuiltin(null);

        when(dictionaryMapper.selectById(3L)).thenReturn(dict);
        when(dictionaryMapper.deleteById(3L)).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.delete(3L));
        verify(dictionaryMapper).deleteById(3L);
    }

    @Test
    void allByTypes_shouldReturnGroupedMap() {
        Dictionary dict = new Dictionary();
        dict.setType("test_type");

        when(dictionaryMapper.selectTypes()).thenReturn(List.of("test_type"));
        when(dictionaryMapper.selectByType("test_type")).thenReturn(List.of(dict));
        when(dictionaryConverter.entityToVO(dict)).thenReturn(new DictionaryVO());

        Map<String, List<DictionaryVO>> result = dictionaryService.allByTypes();

        assertTrue(result.containsKey("test_type"));
        assertEquals(1, result.get("test_type").size());
    }
}
