import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Menu,
  message,
  Modal,
  Popconfirm,
  Row,
  Space,
  Table,
} from 'antd'
import type { MenuProps, TableColumnsType } from 'antd'

import {
  createDictionary,
  deleteDictionary,
  getDictionariesByType,
  updateDictionary,
} from '@/api/admin/dictionary'
import { fetchDictionaries } from '@/api/auth'
import type { components } from '@/types/api'

type DictionaryVO = components['schemas']['DictionaryVO']
type DictionaryRequest = components['schemas']['DictionaryRequest']

export default function DictionaryPage() {
  const [types, setTypes] = useState<string[]>([])
  const [selectedType, setSelectedType] = useState<string>('')
  const [items, setItems] = useState<DictionaryVO[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DictionaryVO | null>(null)
  const [form] = Form.useForm()

  useEffect(() => {
    fetchDictionaries()
      .then((data) => {
        const typeList = Object.keys(data)
        setTypes(typeList)
        if (typeList.length > 0) {
          setSelectedType((prev) => prev || typeList[0])
        }
      })
      .catch(() => {
        message.error('加载字典类型失败')
      })
  }, [])

  const refreshItems = useCallback(() => {
    if (!selectedType) return
    setLoading(true)
    getDictionariesByType(selectedType)
      .then((data) => setItems(data))
      .catch(() => {
        message.error('加载字典项失败')
      })
      .finally(() => setLoading(false))
  }, [selectedType])

  useEffect(() => {
    if (!selectedType) return
    refreshItems()
  }, [selectedType, refreshItems])

  const menuItems: MenuProps['items'] = useMemo(
    () =>
      types.map((t) => ({
        key: t,
        label: t,
      })),
    [types]
  )

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({ type: selectedType })
    setModalOpen(true)
  }

  const handleEdit = (record: DictionaryVO) => {
    setEditingItem(record)
    form.setFieldsValue({
      type: record.type,
      code: record.code,
      name: record.name,
      sort: record.sort,
      remark: record.remark,
    })
    setModalOpen(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteDictionary(id)
      message.success('删除成功')
      refreshItems()
    } catch {
      // request client handles error messages
    }
  }

  const handleModalOk = async () => {
    const values = await form.validateFields()
    try {
      if (editingItem) {
        await updateDictionary(editingItem.id as number, values as DictionaryRequest)
        message.success('更新成功')
      } else {
        await createDictionary(values as DictionaryRequest)
        message.success('创建成功')
      }
      setModalOpen(false)
      refreshItems()
    } catch {
      // request client handles error messages
    }
  }

  const columns: TableColumnsType<DictionaryVO> = [
    { title: '编码', dataIndex: 'code' },
    { title: '名称', dataIndex: 'name' },
    { title: '排序', dataIndex: 'sort', width: 80 },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除？"
            disabled={record.isBuiltin === 1}
            onConfirm={() => handleDelete(record.id as number)}
          >
            <Button type="link" danger size="small" disabled={record.isBuiltin === 1}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Row gutter={16}>
      <Col span={6}>
        <Card title="字典类型">
          <Menu
            mode="inline"
            selectedKeys={[selectedType]}
            items={menuItems}
            onClick={({ key }) => setSelectedType(key)}
          />
        </Card>
      </Col>
      <Col span={18}>
        <Card
          title={`${selectedType || ''} 字典项`}
          extra={
            <Button type="primary" onClick={handleAdd}>
              新增字典项
            </Button>
          }
        >
          <Table
            rowKey="id"
            columns={columns}
            dataSource={items}
            loading={loading}
            pagination={false}
          />
        </Card>
      </Col>

      <Modal
        title={editingItem ? '编辑字典项' : '新增字典项'}
        open={modalOpen}
        onOk={handleModalOk}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="type" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="编码"
            name="code"
            rules={[{ required: true, message: '请输入编码' }]}
          >
            <Input disabled={!!editingItem} />
          </Form.Item>
          <Form.Item
            label="名称"
            name="name"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item label="排序" name="sort" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Row>
  )
}
