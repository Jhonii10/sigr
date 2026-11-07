import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Menu from '../pages/Menu';

const auth = { usuario: null, rol: null };
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    usuario: auth.usuario,
    tieneRol: (...roles) => Boolean(auth.usuario && roles.includes(auth.usuario.rol)),
  }),
}));

const get = vi.fn();
vi.mock('../services/api', () => ({
  default: { get: (...a) => get(...a), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  mensajeError: (e) => e.message,
}));

const PLATOS = [
  { id: 1, categoriaId: 1, categoria: 'Entradas', nombre: 'Empanadas', descripcion: 'Tres', precio: 9000, disponible: true, imagenUrl: '/api/menu/imagenes/plato-1-x.jpg' },
  { id: 2, categoriaId: 1, categoria: 'Entradas', nombre: 'Patacón', descripcion: 'Con hogao', precio: 12000, disponible: true, imagenUrl: null },
];

const renderizar = () => render(<MemoryRouter><Menu /></MemoryRouter>);

describe('Menu', () => {
  beforeEach(() => {
    get.mockImplementation(async (url) => (url === '/menu/categorias'
      ? { data: [{ id: 1, nombre: 'Entradas' }] }
      : { data: PLATOS }));
  });

  it('muestra la foto del plato o un recuadro cuando no tiene', async () => {
    auth.usuario = null;
    renderizar();
    expect(await screen.findByAltText('Empanadas')).toHaveAttribute('src', '/api/menu/imagenes/plato-1-x.jpg');
    expect(screen.getByLabelText('Patacón sin foto')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Agregar/ })).not.toBeInTheDocument();
  });

  it('el cliente agrega platos a su pedido', async () => {
    auth.usuario = { nombre: 'Jhoni', rol: 'CLIENTE' };
    renderizar();
    const botones = await screen.findAllByRole('button', { name: /Agregar/ });
    fireEvent.click(botones[0]);
    fireEvent.click(botones[0]);
    fireEvent.click(screen.getByRole('button', { name: /Ver pedido/ }));
    await waitFor(() => expect(screen.getByText('Tu pedido')).toBeInTheDocument());
    expect(screen.getByText(/Total: .*18\.000/)).toBeInTheDocument();
  });
});
