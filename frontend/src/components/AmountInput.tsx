import { InputNumber } from 'antd'

interface AmountInputProps {
  value?: number
  onChange?: (value: number | null) => void
  placeholder?: string
  disabled?: boolean
  min?: number
  max?: number
}

export function AmountInput({ value, onChange, placeholder, disabled, min = 0, max }: AmountInputProps) {
  return (
    <InputNumber
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      disabled={disabled}
      min={min}
      max={max}
      precision={2}
      style={{ width: '100%' }}
      addonAfter="万元"
    />
  )
}
