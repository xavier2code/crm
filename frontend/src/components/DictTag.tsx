import { Tag } from 'antd'

import { useDictStore } from '@/stores/dict'

const colorMap: Record<string, Record<string, string>> = {
  opportunity_status: {
    '1': 'default',
    '2': 'processing',
    '3': 'success',
    '4': 'error',
  },
  project_status: {
    '1': 'processing',
    '2': 'success',
    '3': 'warning',
    '4': 'error',
  },
}

interface DictTagProps {
  type: string
  value?: string | number
}

export function DictTag({ type, value }: DictTagProps) {
  const name = useDictStore((state) => state.getDictName(type, String(value)))
  const color = colorMap[type]?.[String(value)]
  return <Tag color={color}>{name}</Tag>
}
