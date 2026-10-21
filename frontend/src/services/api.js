import axios from 'axios';

const CLAVE_TOKEN = 'sigr.token';

export const tokenStore = {
  get: () => sessionStorage.getItem(CLAVE_TOKEN),
  set: (t) => sessionStorage.setItem(CLAVE_TOKEN, t),
  clear: () => sessionStorage.removeItem(CLAVE_TOKEN),
};

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && tokenStore.get()) {
      tokenStore.clear();
      window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

/** Extrae un mensaje legible de un error del backend. */
export function mensajeError(error) {
  const data = error?.response?.data;
  if (data?.campos && Object.keys(data.campos).length) {
    return Object.entries(data.campos).map(([c, m]) => `${c}: ${m}`).join(' · ');
  }
  return data?.mensaje || error?.message || 'Ocurrió un error inesperado';
}

export default api;
