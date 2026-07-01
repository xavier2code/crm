import { Select } from 'antd'

import { useDictStore } from '@/stores/dict'

interface DictSelectProps {
  type: string
  value?: string | number
  onChange?: (value: string | number) => void
  placeholder?: string
  allowClear?: boolean
  disabled?: boolean
}

export function DictSelect({ type, value, onChange, placeholder, allowClear, disabled }: DictSelectProps) {
  const dicts = useDictStore((state) => state.getDict(type))

  return (
    <Select
      value={value}
      onChange={onChange}
      placeholder={placeholder || '请选择'}
      allowClear={allowClear}
      disabled={disabled}
      style={{ minWidth: 120 }}
    >
      {dicts.map((item) => (
        <Select.Option key={item.code} value={item.code}>
          {item.name}
        </Select.Option>
      ))}
    </Select>
  )
}
