import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { Input, Spin } from 'antd'

import { fetchCaptcha } from '@/api/auth'

export interface CaptchaInputValue {
  captchaUuid: string
  captchaCode: string
}

interface CaptchaInputProps {
  value?: CaptchaInputValue
  onChange?: (value: CaptchaInputValue) => void
  disabled?: boolean
}

export interface CaptchaInputRef {
  refresh: () => void
}

const CaptchaInput = forwardRef<CaptchaInputRef, CaptchaInputProps>(
  ({ value, onChange, disabled }, ref) => {
    const [uuid, setUuid] = useState('')
    const [image, setImage] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(false)
    const mountedRef = useRef(true)
    const loadingRef = useRef(false)

    const refresh = useCallback(async () => {
      if (loadingRef.current) return
      loadingRef.current = true
      setLoading(true)
      setError(false)
      try {
        const res = await fetchCaptcha()
        if (!mountedRef.current) return
        setUuid(res.uuid)
        setImage(res.image)
        onChange?.({ captchaUuid: res.uuid, captchaCode: '' })
      } catch {
        if (mountedRef.current) {
          setError(true)
          setImage('')
          setUuid('')
        }
      } finally {
        if (mountedRef.current) setLoading(false)
        loadingRef.current = false
      }
    }, [onChange])

    useImperativeHandle(ref, () => ({ refresh }))

    useEffect(() => {
      refresh()
      return () => {
        mountedRef.current = false
      }
    }, [refresh])

    const handleCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange?.({ captchaUuid: uuid, captchaCode: e.target.value })
    }

    return (
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <Input
          value={value?.captchaCode || ''}
          onChange={handleCodeChange}
          placeholder="验证码"
          maxLength={6}
          disabled={disabled}
        />
        <div
          onClick={refresh}
          style={{
            width: 100,
            height: 32,
            flexShrink: 0,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#f5f5f5',
            border: '1px solid #d9d9d9',
            borderRadius: 6,
            overflow: 'hidden',
          }}
        >
          {loading ? (
            <Spin size="small" />
          ) : error ? (
            <span style={{ fontSize: 12, color: '#999' }}>加载失败，点击刷新</span>
          ) : (
            image && (
              <img
                src={image}
                alt="验证码"
                style={{ width: '100%', height: '100%', objectFit: 'contain' }}
              />
            )
          )}
        </div>
      </div>
    )
  }
)

CaptchaInput.displayName = 'CaptchaInput'

export default CaptchaInput
