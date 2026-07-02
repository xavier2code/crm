import { useState } from 'react'
import { App, Button, Card, Form, Input, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { LockOutlined } from '@ant-design/icons'

import { useAuthStore } from '@/stores/auth'
import { request } from '@/api/client'

interface ChangePasswordValues {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * 修改密码页
 *
 * 业务依据：CRM-渠道版-开发文档.md §1.3
 *   - 用户首次登录强制改密
 *   - 忘记密码 → 管理员重置 → 重置后同样需要改密
 *
 * 触发链路：
 *   1) 登录响应里 userInfo.mustChangePassword = true
 *   2) axios 拦截器收到业务码 2007（密码已过期 / 需重置）
 */
export default function ChangePasswordPage() {
  const { user, clearAuth } = useAuthStore()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [form] = Form.useForm<ChangePasswordValues>()
  const [submitting, setSubmitting] = useState(false)

  const onFinish = async (values: ChangePasswordValues) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的新密码不一致')
      return
    }
    if (values.oldPassword === values.newPassword) {
      message.error('新密码不能与旧密码相同')
      return
    }
    setSubmitting(true)
    try {
      await request<void>({
        method: 'POST',
        url: '/auth/change-password',
        data: { oldPassword: values.oldPassword, newPassword: values.newPassword },
      })
      message.success('密码修改成功，请重新登录')
      clearAuth()
      navigate('/login', { replace: true })
    } catch (e) {
      console.error(e)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card
        title={
          <Space>
            <LockOutlined />
            修改密码
          </Space>
        }
        style={{ width: 460 }}
      >
        {user && (
          <div style={{ marginBottom: 16, color: '#666' }}>
            当前账号：<strong>{user.realName || user.username}</strong>
            <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
              {user.username} · 为了您的账号安全，请修改初始密码后再使用系统
            </div>
          </div>
        )}
        <Form<ChangePasswordValues>
          form={form}
          layout="vertical"
          onFinish={onFinish}
          requiredMark={false}
        >
          <Form.Item
            label="当前密码"
            name="oldPassword"
            rules={[{ required: true, message: '请输入当前密码' }]}
          >
            <Input.Password placeholder="请输入当前密码" autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            label="新密码"
            name="newPassword"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, max: 20, message: '密码长度应在 6-20 位之间' },
            ]}
          >
            <Input.Password placeholder="6-20 位" autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            label="确认新密码"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="再次输入新密码" autoComplete="new-password" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={submitting} block>
              提交并重新登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
