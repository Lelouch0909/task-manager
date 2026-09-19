import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

type ViewMode = 'list' | 'grid'
interface UiState { viewMode: ViewMode }
const initialState: UiState = { viewMode: 'list' }

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setViewMode(state, action: PayloadAction<ViewMode>) {
      state.viewMode = action.payload
    },
  },
})

export const { setViewMode } = uiSlice.actions
export default uiSlice.reducer
