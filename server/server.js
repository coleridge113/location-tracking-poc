import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import fs from 'fs';
import path from 'path';
import * as ably from './utils/ably.js';
import * as pusher from './utils/pusher.js';

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, { cors: { origin: "*" } });

function loadRoutePoints() {
    const filePath = path.resolve(process.cwd(), 'location_data.txt1');
    try {
        const raw = fs.readFileSync(filePath, 'utf-8');
        return raw
            .split(/\r?\n/)
            .map(l => l.trim())
            .filter(Boolean)
            .map(l => {
                const [latStr, lonStr] = l.split(',');
                const lat = parseFloat(latStr);
                const lng = parseFloat(lonStr);
                if (isNaN(lat) || isNaN(lng)) return null;
                return [lng, lat];
            })
            .filter(Boolean);
    } catch (e) {
        console.error('Failed to read location_data.txt:', e.message);
        return [];
    }
}

const route = loadRoutePoints();
console.log(`Loaded ${route.length} points from location_data.txt`);

// Track whether we are currently "online" for publishing
let publishing = true;

// Simple endpoint to toggle publishing from the terminal (or Postman)
app.post('/toggle-publishing', (_req, res) => {
    publishing = !publishing;
    console.log(`*** publishing = ${publishing} ***`);
    res.json({ publishing });
});

// Just log socket connections; they no longer control the loop
io.on('connection', socket => {
    console.log('Client connected', socket.id);
    socket.on('disconnect', () => {
        console.log('Client disconnected', socket.id);
    });
});

// Global interval that runs whether or not any clients are connected
let idx = 0;
setInterval(() => {
    // "network down" simulation: we stop publishing while keeping server & Zoom online
    if (!publishing) {
        return;
    }

    if (idx >= route.length) {
        return; // finished
    }

    const [lng, lat] = route[idx];
    const locationData = {
        type: 'point',
        seq: idx,
        lng,
        lat,
        ts: Date.now()
    };

    // 1) Socket.IO broadcast to any connected clients
    io.emit('point', locationData);

    // 2) Ably & Pusher publish (Android listens here)
    ably.publishMessage(locationData);
    // pusher.publishMessage(locationData);

    console.log(`Broadcast point seq=${idx} lng=${lng} lat=${lat}`);
    idx++;
}, 1000);

app.get('/', (_req, res) => res.send('Route emitter running'));

httpServer.listen(3000, () => {
    console.log('Server listening on :3000');
});
