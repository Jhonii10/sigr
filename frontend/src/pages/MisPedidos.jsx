import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid, Stack, Typography,
} from '@mui/material';
import api, { mensajeError } from '../services/api';
import { suscribir } from '../services/socket';
import { ESTADOS_PEDIDO, formatoPesos } from '../utils/formato';

export default function MisPedidos() {
  const location = useLocation();
  const [pedidos, setPedidos] = useState([]);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState(Boolean(location.state?.enviado));

  const cargar = useCallback(() => {
    api.get('/pedidos/mios').then((r) => setPedidos(r.data)).catch((err) => setError(mensajeError(err)));
  }, []);

  useEffect(() => {
    cargar();
    // Actualiza el estado en vivo cuando la cocina o el mesero cambian alguno de mis pedidos.
    return suscribir('/topic/pedidos', (p) => {
      setPedidos((prev) => (prev.some((x) => x.id === p.id) ? prev.map((x) => (x.id === p.id ? p : x)) : prev));
    });
  }, [cargar]);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Mis pedidos</Typography>
        <Button variant="contained" color="secondary" component={RouterLink} to="/menu">Pedir algo más</Button>
      </Stack>
      {aviso && (
        <Alert severity="success" onClose={() => setAviso(false)} sx={{ mb: 2 }}>
          Pedido enviado. Aquí verás cómo avanza en la cocina.
        </Alert>
      )}
      {error && <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>{error}</Alert>}
      {pedidos.length === 0 && !error && (
        <Typography color="text.secondary">Todavía no has hecho pedidos. Elige tus platos en el menú.</Typography>
      )}
      <Grid container spacing={2}>
        {pedidos.map((p) => {
          const estado = ESTADOS_PEDIDO[p.estado];
          return (
            <Grid item xs={12} sm={6} md={4} key={p.id}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography fontWeight={700}>Mesa {p.mesa}</Typography>
                    <Chip size="small" color={estado.color} label={estado.etiqueta} />
                  </Stack>
                  <Typography variant="caption" color="text.secondary">
                    Pedido #{p.id} · {new Date(p.creadoEn).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' })}
                    {p.mesero ? ` · Atiende ${p.mesero}` : ''}
                  </Typography>
                  <Box sx={{ mt: 1 }}>
                    {p.detalles.map((d) => (
                      <Typography key={d.platoId} variant="body2">{d.cantidad} × {d.plato}</Typography>
                    ))}
                  </Box>
                  {p.nota && <Typography variant="body2" color="secondary">Nota: {p.nota}</Typography>}
                  <Typography fontWeight={600} sx={{ mt: 1 }}>{formatoPesos(p.total)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          );
        })}
      </Grid>
    </Box>
  );
}
