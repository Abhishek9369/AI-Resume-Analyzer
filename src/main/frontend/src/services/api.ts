import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'multipart/form-data',
  },
})

export interface UploadResponse {
  trackerId: string
  message: string
}

export interface ProcessStatusResponse {
  id: string
  status: string
  totalFiles: number
  processedFiles: number
  failedFiles: number
  startTime: string
  endTime?: string
  message?: string
}

export const uploadResumes = async (files: File[]): Promise<UploadResponse> => {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file)
  })

  const response = await api.post<UploadResponse>('/upload/resume', formData)
  return response.data
}

export const getProcessStatus = async (trackerId: string): Promise<ProcessStatusResponse> => {
  const response = await api.get<ProcessStatusResponse>(`/upload/status/${trackerId}`)
  return response.data
}

export default api
