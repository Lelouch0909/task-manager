import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
export interface User { id: string; displayName: string; email: string; emailVerified: boolean; createdAt: string }
const initialState = { user: null as User | null, status: 'loading' as 'loading' | 'authenticated' | 'anonymous', error: null as string | null }
const slice = createSlice({
  name: 'auth', initialState,
  reducers: {
    signedIn(state, action: PayloadAction<User>) { state.user = action.payload; state.status = 'authenticated'; state.error = null },
    signedOut(state) { state.user = null; state.status = 'anonymous'; state.error = null },
    authFailed(state, action: PayloadAction<string>) { state.user = null; state.status = 'anonymous'; state.error = action.payload },
  },
})
export const { signedIn, signedOut, authFailed } = slice.actions
export default slice.reducer
