import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getProjects,
  getProject,
  createProject,
  updateProject,
  updateProjectPNode,
  updateProjectStage6,
  updateProjectMilestone,
  saveProjectBiddingNode,
  saveProjectContractNode,
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

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createProject,
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
    },
  })
}

export function useUpdateProjectPNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, pNode }: { id: number; pNode: number }) => updateProjectPNode(id, pNode),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useUpdateProjectStage6() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, stage6 }: { id: number; stage6: number }) => updateProjectStage6(id, stage6),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useUpdateProjectMilestone() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateProjectMilestone>[1] }) =>
      updateProjectMilestone(id, data),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useSaveProjectBiddingNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof saveProjectBiddingNode>[1] }) =>
      saveProjectBiddingNode(id, data),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useSaveProjectContractNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof saveProjectContractNode>[1] }) =>
      saveProjectContractNode(id, data),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}
