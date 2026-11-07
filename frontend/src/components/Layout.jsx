import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { AppBar, Box, Button, Chip, Container, Stack, Toolbar, Typography } from '@mui/material';
import RestaurantIcon from '@mui/icons-material/Restaurant';
import { useAuth } from '../context/AuthContext';

const ENLACES = [
  { a: '/menu', texto: 'Menú' },
  { a: '/mis-pedidos', texto: 'Mis pedidos', roles: ['CLIENTE'] },
  { a: '/reservas', texto: 'Reservas', roles: ['CLIENTE', 'MESERO', 'ADMIN'] },
  { a: '/pedidos', texto: 'Pedidos', roles: ['MESERO', 'ADMIN'] },
  { a: '/caja', texto: 'Caja', roles: ['ADMIN'] },
  { a: '/usuarios', texto: 'Usuarios', roles: ['ADMIN'] },
];

export default function Layout() {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const visibles = ENLACES.filter((e) => !e.roles || (usuario && e.roles.includes(usuario.rol)));

  return (
    <Box sx={{ minHeight: '100vh' }}>
      <AppBar position="sticky" color="primary">
        <Toolbar sx={{ gap: 2, flexWrap: 'wrap' }}>
          <RestaurantIcon color="secondary" />
          <Typography variant="h6" sx={{ fontWeight: 700, mr: 2 }}>SIGR</Typography>
          <Stack direction="row" spacing={0.5} sx={{ flexGrow: 1, flexWrap: 'wrap' }}>
            {visibles.map((e) => (
              <Button key={e.a} component={NavLink} to={e.a} color="inherit"
                sx={{ '&.active': { bgcolor: 'rgba(255,255,255,0.15)' } }}>
                {e.texto}
              </Button>
            ))}
          </Stack>
          {usuario ? (
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="body2">{usuario.nombre}</Typography>
              <Chip size="small" label={usuario.rol} color="secondary" />
              <Button color="inherit" onClick={() => { cerrarSesion(); navigate('/login'); }}>Salir</Button>
            </Stack>
          ) : (
            <Button color="secondary" variant="contained" onClick={() => navigate('/login')}>Ingresar</Button>
          )}
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
