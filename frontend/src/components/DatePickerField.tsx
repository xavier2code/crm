import { DatePicker } from 'antd'
import type { Dayjs } from 'dayjs'

interface DatePickerFieldProps {
  value?: Dayjs | null
  onChange?: (value: Dayjs | null) => void
  placeholder?: string
  disabled?: boolean
}

export function DatePickerField({ value, onChange, placeholder, disabled }: DatePickerFieldProps) {
  return (
    <DatePicker
      value={value}
      onChange={onChange}
      placeholder={placeholder || '请选择日期'}
      disabled={disabled}
      style={{ width: '100%' }}
    />
  )
}
