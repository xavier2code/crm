import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, message } from 'antd'

import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useDictStore } from '@/stores/dict'
import { login, fetchCurrentUser, fetchDictionaries } from '@/api/auth'
import type { DictionaryItem } from '@/stores/dict'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth, setUser } = useAuthStore()
  const [loading, setLoading] = useState(false)

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const result = await login(values)
      setAuth(result)
      useMenuStore.getState().setMenus(result.menuTree || [])

      const dicts = await fetchDictionaries()
      const dictMap: Record<string, DictionaryItem[]> = {}
      dicts.forEach((item) => {
        if (!dictMap[item.type]) dictMap[item.type] = []
        dictMap[item.type].push(item)
      })
      useDictStore.getState().setDicts(dictMap)

      const user = await fetchCurrentUser()
      setUser(user)
      message.success('登录成功')
      navigate('/dashboard')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card title="CRM 管理系统" style={{ width: 360 }}>
        <Form onFinish={handleLogin} autoComplete="off">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
