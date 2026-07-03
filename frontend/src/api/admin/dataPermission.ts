import { request } from '../client'

/**
 * 数据权限维度（与后端 DataScopeDimension 枚举一一对应）。
 * 使用 code 字符串而非数字，避免与历史 scope_type 数字歧义。
 */
export const DATA_SCOPE_DIMENSIONS = {
  ALL: 'ALL',
  CHANNEL: 'CHANNEL',
  REGION: 'REGION',
  UNIT: 'UNIT',
  BUSINESS_DOMAIN: 'BUSINESS_DOMAIN',
  POLICE_TYPE: 'POLICE_TYPE',
  SELF: 'SELF',
} as const

export type DataScopeDimensionCode =
  (typeof DATA_SCOPE_DIMENSIONS)[keyof typeof DATA_SCOPE_DIMENSIONS]

export const DATA_SCOPE_DIMENSION_LABEL: Record<DataScopeDimensionCode, string> = {
  ALL: '全部',
  CHANNEL: '渠道',
  REGION: '区域',
  UNIT: '单位',
  BUSINESS_DOMAIN: '业务域',
  POLICE_TYPE: '警种',
  SELF: '本人',
}

export interface DataPermissionVO {
  scopeType: DataScopeDimensionCode
  scopeTypeLabel: string
  scopeValues: string[]
}

export interface DataPermissionUpdateRequest {
  scopeType: DataScopeDimensionCode
  scopeValues: string[]
}

export function getUserDataPermissions(userId: number) {
  return request<DataPermissionVO[]>({
    url: `/admin/users/${userId}/data-permissions`,
    method: 'GET',
  })
}

export function updateUserDataPermissions(
  userId: number,
  data: DataPermissionUpdateRequest,
) {
  return request<void>({
    url: `/admin/users/${userId}/data-permissions`,
    method: 'PUT',
    data,
  })
}
