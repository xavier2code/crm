import { request } from './client'

export type ReimbursementStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'PAID'
export type ReimbursementType = 'TRAVEL' | 'ENTERTAIN'

export const REIMBURSEMENT_STATUS: Record<ReimbursementStatus, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  PENDING: { label: '待审批', color: 'blue' },
  APPROVED: { label: '已审批', color: 'green' },
  REJECTED: { label: '已驳回', color: 'red' },
  PAID: { label: '已付款', color: 'purple' },
}

export const REIMBURSEMENT_TYPE: Record<ReimbursementType, { label: string }> = {
  TRAVEL: { label: '差旅' },
  ENTERTAIN: { label: '招待' },
}

export interface ReimbursementAttachmentVO {
  id?: number
  fileName?: string
  filePath?: string
  fileSize?: number
  contentType?: string
  uploadedBy?: number
  uploadedAt?: string
}

export interface ReimbursementVO {
  id?: number
  projectId?: number
  projectName?: string
  applicantId?: number
  applicantName?: string
  type?: ReimbursementType
  typeName?: string
  title?: string
  description?: string
  amount?: number
  expenseDate?: string
  status?: ReimbursementStatus
  approverId?: number
  approverName?: string
  approvedAt?: string
  approvalComment?: string
  paidAt?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
  attachments?: ReimbursementAttachmentVO[]
}

export interface ReimbursementPage {
  records?: ReimbursementVO[]
  total?: number
  size?: number
  current?: number
}

export interface ReimbursementRequest {
  projectId: number
  type: ReimbursementType
  title: string
  description?: string
  amount: number
  expenseDate: string
}

export interface ReimbursementApproveRequest {
  result: 'APPROVED' | 'REJECTED'
  comment?: string
}

export function pageReimbursements(params: {
  status?: ReimbursementStatus
  type?: ReimbursementType
  projectId?: number
  applicantId?: number
  mine?: boolean
  current?: number
  size?: number
} = {}) {
  return request<ReimbursementPage>({
    url: '/reimbursements',
    method: 'GET',
    params: {
      status: params.status,
      type: params.type,
      projectId: params.projectId,
      applicantId: params.applicantId,
      mine: params.mine,
      current: params.current ?? 1,
      size: params.size ?? 10,
    },
  })
}

export function getReimbursement(id: number) {
  return request<ReimbursementVO>({ url: `/reimbursements/${id}`, method: 'GET' })
}

export function createReimbursement(data: ReimbursementRequest) {
  return request<number>({ url: '/reimbursements', method: 'POST', data })
}

export function updateReimbursement(id: number, data: ReimbursementRequest) {
  return request<void>({ url: `/reimbursements/${id}`, method: 'PUT', data })
}

export function deleteReimbursement(id: number) {
  return request<void>({ url: `/reimbursements/${id}`, method: 'DELETE' })
}

export function submitReimbursement(id: number) {
  return request<void>({ url: `/reimbursements/${id}/submit`, method: 'POST' })
}

export function approveReimbursement(id: number, data: ReimbursementApproveRequest) {
  return request<void>({ url: `/reimbursements/${id}/approve`, method: 'POST', data })
}

export function markPaid(id: number) {
  return request<void>({ url: `/reimbursements/${id}/pay`, method: 'POST' })
}

export function uploadAttachment(id: number, file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return request<ReimbursementAttachmentVO>({
    url: `/reimbursements/${id}/attachments`,
    method: 'POST',
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteAttachment(attachmentId: number) {
  return request<void>({
    url: `/reimbursements/attachments/${attachmentId}`,
    method: 'DELETE',
  })
}

export function downloadAttachmentUrl(attachmentId: number) {
  return `/api/reimbursements/attachments/${attachmentId}/download`
}
