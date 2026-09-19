import { configureStore } from '@reduxjs/toolkit'
import uiReducer from '../features/ui/uiSlice'
import notificationsReducer from '../features/notifications/notificationsSlice'
import authReducer from '../features/auth/authSlice'
import tasksReducer from '../features/tasks/tasksSlice'

export const store = configureStore({ reducer: { ui: uiReducer, notifications: notificationsReducer, auth: authReducer, tasks: tasksReducer } })

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
