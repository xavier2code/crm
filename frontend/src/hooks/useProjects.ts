import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getProjects,
  getProject,
  getProjectProcess,
  createProject,
  createProjectFromOpportunity,
  updateProject,
  transitionProjectStatus,
  updateProjectPNode,
  updateProjectStage6,
  updateProjectMilestone,
  saveProjectBiddingNode,
  saveProjectContractNode,
  addProjectPaymentNode,
  updateProjectPaymentNode,
  deleteProjectPaymentNode,
  submitProjectScore,
  getProjectScoreDimensions,
} from '@/api/project'

export function useProjects(params: Parameters<typeof getProjects>[0]) {
  return useQuery({ queryKey: ['projects', params], queryFn: () => getProjects(params) })
}

export function useProject(id: number | undefined) {
  return useQuery({
    queryKey: ['project', id],
    queryFn: () => getProject(id!),
    enabled: !!id,
  })
}

export function useProjectProcess(id: number | undefined) {
  return useQuery({
    queryKey: ['project-process', id],
    queryFn: () => getProjectProcess(id!),
    enabled: !!id,
  })
}

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useCreateProjectFromOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ oppId, data }: { oppId: number; data: Parameters<typeof createProjectFromOpportunity>[1] }) =>
      createProjectFromOpportunity(oppId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useUpdateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateProject>[1] }) =>
      updateProject(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useTransitionProjectStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status, reason }: { id: number; status: number; reason: string }) =>
      transitionProjectStatus(id, status, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useUpdateProjectPNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, pNode }: { id: number; pNode: number }) => updateProjectPNode(id, pNode),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useUpdateProjectStage6() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, stage6 }: { id: number; stage6: string }) => updateProjectStage6(id, stage6),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useUpdateProjectMilestone() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateProjectMilestone>[1] }) =>
      updateProjectMilestone(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useSaveProjectBiddingNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof saveProjectBiddingNode>[1] }) =>
      saveProjectBiddingNode(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useSaveProjectContractNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof saveProjectContractNode>[1] }) =>
      saveProjectContractNode(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useAddProjectPaymentNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof addProjectPaymentNode>[1] }) =>
      addProjectPaymentNode(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.id] })
    },
  })
}

export function useUpdateProjectPaymentNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ nodeId, data }: { projectId: number; nodeId: number; data: Parameters<typeof updateProjectPaymentNode>[1] }) =>
      updateProjectPaymentNode(nodeId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.projectId] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.projectId] })
    },
  })
}

export function useDeleteProjectPaymentNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ nodeId }: { projectId: number; nodeId: number }) =>
      deleteProjectPaymentNode(nodeId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['project', variables.projectId] })
      queryClient.invalidateQueries({ queryKey: ['project-process', variables.projectId] })
    },
  })
}

export function useSubmitProjectScore() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: submitProjectScore,
    onSuccess: (_, variables) => {
      const projectId = variables.projectId
      queryClient.invalidateQueries({ queryKey: ['project', projectId] })
      queryClient.invalidateQueries({ queryKey: ['project-process', projectId] })
    },
  })
}

export function useProjectScoreDimensions() {
  return useQuery({
    queryKey: ['project-score-dimensions'],
    queryFn: () => getProjectScoreDimensions(),
  })
}
