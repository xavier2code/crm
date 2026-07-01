import { DatePicker } from 'antd'
import type { Dayjs } from 'dayjs'

interface WeekPickerFieldProps {
  value?: Dayjs | null
  onChange?: (value: Dayjs | null) => void
  placeholder?: string
  disabled?: boolean
}

export function WeekPickerField({ value, onChange, placeholder, disabled }: WeekPickerFieldProps) {
  return (
    <DatePicker
      value={value}
      onChange={onChange}
      placeholder={placeholder || '请选择周'}
      disabled={disabled}
      style={{ width: '100%' }}
      picker="week"
    />
  )
}
