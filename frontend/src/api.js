import axios from 'axios'
// In the cloud set VITE_API_URL to your EC2 / load balancer URL
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api' })
export const setAuth = (u, p) => { api.defaults.auth = u ? { username: u, password: p } : undefined }
