import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '', // Load base URL from environment
  timeout: 5000,
})

export default request
