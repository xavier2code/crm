import { request } from '../client'

/** 权限维度（与后端 DataPermissionService 常量一一对应） */
export const SCOPE_TYPE = {
  BUSINESS_DOMAIN: 1,
  REGION: 2,
  CHANNEL: 3,
  POLICE_TYPE: 4,
} as const

export type ScopeType = (typeof SCOPE_TYPE)[keyof typeof SCOPE_TYPE]

export const SCOPE_TYPE_LABEL: Record<ScopeType, string> = {
  [SCOPE_TYPE.BUSINESS_DOMAIN]: '业务域',
  [SCOPE_TYPE.REGION]: '区域',
  [SCOPE_TYPE.CHANNEL]: '渠道',
  [SCOPE_TYPE.POLICE_TYPE]: '警种',
}

export interface DataPermissionVO {
  scopeType: ScopeType
  scopeValues: string[]
}

export interface DataPermissionUpdateRequest {
  scopeType: ScopeType
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
