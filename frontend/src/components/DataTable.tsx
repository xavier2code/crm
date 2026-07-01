import { Table, type TableProps } from 'antd'

interface DataTableProps<T> extends Omit<TableProps<T>, 'pagination' | 'loading'> {
  data?: { records?: T[]; total?: number; current?: number; size?: number }
  loading?: boolean
  onPageChange?: (page: number, pageSize: number) => void
}

export function DataTable<T extends { id?: number | string }>({
  data,
  loading,
  onPageChange,
  rowKey = 'id',
  ...rest
}: DataTableProps<T>) {
  return (
    <Table
      rowKey={rowKey}
      loading={loading}
      dataSource={data?.records || []}
      pagination={{
        current: data?.current || 1,
        pageSize: data?.size || 20,
        total: data?.total || 0,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
        onChange: onPageChange,
      }}
      {...rest}
    />
  )
}
