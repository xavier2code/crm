import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getCustomers,
  getCustomer,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  assignCustomer,
  addContact,
  updateContact,
  deleteContact,
} from '@/api/customer'

export function useCustomers(params: Parameters<typeof getCustomers>[0]) {
  return useQuery({
    queryKey: ['customers', params],
    queryFn: () => getCustomers(params),
  })
}

export function useCustomer(id: number | undefined) {
  return useQuery({
    queryKey: ['customer', id],
    queryFn: () => getCustomer(id!),
    enabled: !!id,
  })
}

export function useCreateCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createCustomer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
    },
  })
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateCustomer>[1] }) =>
      updateCustomer(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customer', variables.id] })
    },
  })
}

export function useDeleteCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteCustomer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
    },
  })
}

export function useAssignCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, userId }: { id: number; userId: number }) => assignCustomer(id, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customer', variables.id] })
    },
  })
}

export function useAddContact() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ customerId, data }: { customerId: number; data: Parameters<typeof addContact>[1] }) =>
      addContact(customerId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customer', variables.customerId] })
      queryClient.invalidateQueries({ queryKey: ['customers'] })
    },
  })
}

export function useUpdateContact() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateContact>[1] }) =>
      updateContact(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customer'] })
    },
  })
}

export function useDeleteContact() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteContact,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customer'] })
    },
  })
}
