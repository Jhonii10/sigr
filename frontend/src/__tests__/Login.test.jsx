import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Login from '../pages/Login';

const iniciarSesion = vi.fn();
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ iniciarSesion }) }));

const renderizar = () => render(<MemoryRouter><Login /></MemoryRouter>);

describe('Login', () => {
  beforeEach(() => {
    iniciarSesion.mockReset();
  });

  it('muestra el formulario de ingreso', () => {
    renderizar();
    expect(screen.getByText('Ingresar a SIGR')).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('muestra el mensaje del backend cuando las credenciales son inválidas', async () => {
    const error = Object.assign(new Error('401'), {
      response: { data: { mensaje: 'Credenciales inválidas o usuario inactivo' } },
    });
    iniciarSesion.mockImplementation(async () => { throw error; });
    renderizar();
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'x@test.com' } });
    fireEvent.change(screen.getByLabelText(/contraseña/i), { target: { value: 'malaclave' } });
    fireEvent.click(screen.getByRole('button', { name: 'Ingresar' }));
    await waitFor(() => expect(screen.getByText(/Credenciales inválidas/)).toBeInTheDocument());
    expect(iniciarSesion).toHaveBeenCalledWith('x@test.com', 'malaclave');
  });
});
