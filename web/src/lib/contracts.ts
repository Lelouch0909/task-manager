import { z } from 'zod'
export const userSchema = z.object({ id: z.string(), displayName: z.string(), email: z.string(), emailVerified: z.boolean(), createdAt: z.string() })
export const loginSchema = z.object({ accessToken: z.string(), expiresIn: z.number(), user: userSchema })
export const taskStatusSchema = z.enum(['TODO', 'IN_PROGRESS', 'DONE'])
export const taskSchema = z.object({
  id: z.string(), title: z.string(), description: z.string().nullable(), status: taskStatusSchema,
  createdAt: z.string(), updatedAt: z.string(),
})
export const taskPageSchema = z.object({
  items: z.array(taskSchema), page: z.number(), size: z.number(), totalElements: z.number(), totalPages: z.number(),
})
export type Task = z.infer<typeof taskSchema>
export type TaskStatus = z.infer<typeof taskStatusSchema>
export type TaskPage = z.infer<typeof taskPageSchema>
export const statusLabels: Record<TaskStatus, string> = { TODO: 'À faire', IN_PROGRESS: 'En cours', DONE: 'Terminée' }
