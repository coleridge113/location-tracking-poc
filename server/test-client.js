import { io } from 'socket.io-client';

const socket = io('http://localhost:3000', { transports: ['websocket'] });

socket.on('connect', () => console.log('connected', socket.id));
socket.on('route', data => console.log('route received', data.coordinates.length));
socket.on('point', data => console.log('point', data.seq, data.lng, data.lat));
socket.on('disconnect', () => console.log('disconnected'));
