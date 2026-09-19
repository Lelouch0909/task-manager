import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

export interface NotificationItem {
  id: string
  taskId: string
  kind: string
  message: string
  createdAt: string
  readAt: string | null
}
export interface NotificationPage {
  items: NotificationItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  unreadCount: number
}
const initialState = {
  page: null as NotificationPage | null,
  connection: 'disconnected' as 'disconnected' | 'connecting' | 'connected',
  error: null as string | null,
  tasksRevision: 0,
}
const slice = createSlice({
  name: 'notifications', initialState,
  reducers: {
    received(state, action: PayloadAction<NotificationPage>) { state.page = action.payload; state.error = null },
    connectionChanged(state, action: PayloadAction<typeof initialState.connection>) { state.connection = action.payload },
    failed(state, action: PayloadAction<string>) { state.error = action.payload },
    tasksInvalidated(state) { state.tasksRevision++ },
    cleared: () => initialState,
  },
})
export const { received, connectionChanged, failed, tasksInvalidated, cleared } = slice.actions
export default slice.reducer
