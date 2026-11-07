import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import RutaProtegida from './components/RutaProtegida';
import Login from './pages/Login';
import Registro from './pages/Registro';
import Menu from './pages/Menu';
import Pedidos from './pages/Pedidos';
import MisPedidos from './pages/MisPedidos';
import Reservas from './pages/Reservas';
import Caja from './pages/Caja';
import Usuarios from './pages/Usuarios';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/menu" replace />} />
        <Route path="/menu" element={<Menu />} />
        <Route path="/reservas" element={<RutaProtegida><Reservas /></RutaProtegida>} />
        <Route path="/mis-pedidos" element={<RutaProtegida roles={['CLIENTE']}><MisPedidos /></RutaProtegida>} />
        <Route path="/pedidos" element={<RutaProtegida roles={['MESERO', 'ADMIN']}><Pedidos /></RutaProtegida>} />
        <Route path="/caja" element={<RutaProtegida roles={['ADMIN']}><Caja /></RutaProtegida>} />
        <Route path="/usuarios" element={<RutaProtegida roles={['ADMIN']}><Usuarios /></RutaProtegida>} />
      </Route>
      <Route path="*" element={<Navigate to="/menu" replace />} />
    </Routes>
  );
}
