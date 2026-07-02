import { request } from '../client'

/** 审计日志实体 */
export interface AuditLog {
  id: number
  userId?: number
  username?: string
  /** 操作描述（如「创建客户」） */
  operation?: string
  /** 业务模块（如 customer / opportunity / project） */
  module?: string
  /** 被调用的方法签名 */
  method?: string
  /** 方法入参（JSON 字符串） */
  params?: string
  /** 客户端 IP */
  ip?: string
  /** 1=成功 0=失败 */
  status?: number
  /** 失败时的错误信息 */
  errorMsg?: string
  /** 执行耗时（毫秒） */
  executeTime?: number
  createdAt?: string
}

/** 分页结果 */
export interface AuditLogPage {
  records: AuditLog[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface AuditLogQuery {
  current?: number
  size?: number
  userId?: number
  module?: string
  operation?: string
  /** yyyy-MM-dd */
  startDate?: string
  /** yyyy-MM-dd */
  endDate?: string
}

export function pageAuditLogs(params: AuditLogQuery = {}) {
  return request<AuditLogPage>({
    url: '/admin/audit-logs',
    method: 'GET',
    params: {
      current: params.current ?? 1,
      size: params.size ?? 20,
      userId: params.userId,
      module: params.module,
      operation: params.operation,
      startDate: params.startDate,
      endDate: params.endDate,
    },
  })
}

export function getRecentAuditLogs(limit = 20) {
  return request<AuditLog[]>({
    url: '/admin/audit-logs/recent',
    method: 'GET',
    params: { limit },
  })
}

export function getUserOperationCount(
  userId: number,
  params: { startDate?: string; endDate?: string } = {},
) {
  return request<number>({
    url: `/admin/audit-logs/stats/user/${userId}`,
    method: 'GET',
    params,
  })
}
