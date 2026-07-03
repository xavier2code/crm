import { useMemo, useState } from 'react'
import { Select, type SelectProps } from 'antd'
import { useQuery } from '@tanstack/react-query'

import { getCustomers } from '@/api/customer'

interface CustomerSelectProps {
  value?: number
  onChange?: (value: number) => void
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
}

export function CustomerSelect({
  value,
  onChange,
  placeholder = '请选择客户',
  disabled,
  allowClear,
}: CustomerSelectProps) {
  const [keyword, setKeyword] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['customers', 'select', keyword],
    queryFn: () => getCustomers({ current: 1, size: 20, keyword }),
  })

  const options = useMemo(
    () =>
      (data?.records || []).map((c) => ({
        value: c.id as number,
        label: c.name,
      })),
    [data],
  )

  const handleSearch: SelectProps['onSearch'] = (v) => {
    setKeyword(v)
  }

  return (
    <Select
      showSearch
      value={value}
      onChange={onChange}
      onSearch={handleSearch}
      options={options}
      loading={isLoading}
      placeholder={placeholder}
      disabled={disabled}
      allowClear={allowClear}
      filterOption={false}
      style={{ width: '100%' }}
    />
  )
}
