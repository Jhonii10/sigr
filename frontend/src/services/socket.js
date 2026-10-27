import { Client } from '@stomp/stompjs';

/** Se suscribe a un tópico STOMP y devuelve una función para desconectarse. */
export function suscribir(topico, alRecibir) {
  const protocolo = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const client = new Client({
    brokerURL: `${protocolo}://${window.location.host}/api/ws`,
    reconnectDelay: 3000,
    onConnect: () => client.subscribe(topico, (msg) => alRecibir(JSON.parse(msg.body))),
  });
  client.activate();
  return () => client.deactivate();
}
