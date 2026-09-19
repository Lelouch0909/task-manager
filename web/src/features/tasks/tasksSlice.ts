import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import type { TaskStatus } from '@/lib/contracts'
import { signedOut } from '@/features/auth/authSlice'
const initialState = { filter: 'ALL' as TaskStatus | 'ALL', search: '', page: 0 }
const slice = createSlice({
  name: 'tasks', initialState,
  reducers: {
    filterChanged(state, action: PayloadAction<typeof initialState.filter>) { state.filter = action.payload; state.page = 0 },
    searchChanged(state, action: PayloadAction<string>) { state.search = action.payload; state.page = 0 },
    pageChanged(state, action: PayloadAction<number>) { state.page = action.payload },
  },
  extraReducers: builder => { builder.addCase(signedOut, () => initialState) },
})
export const { filterChanged, searchChanged, pageChanged } = slice.actions
export default slice.reducer
