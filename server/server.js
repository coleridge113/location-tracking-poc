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
    const filePath = path.resolve(process.cwd(), 'location_data.txt');
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

// Just log socket connections; they no longer control the loop
io.on('connection', socket => {
    console.log('Client connected', socket.id);
    socket.on('disconnect', () => {
        console.log('Client disconnected', socket.id);
    });
});

// Broadcast full route once at startup
const fullRoutePayload = { type: 'route', coordinates: route, ts: Date.now() };
io.emit('route', fullRoutePayload);
ably.publishMessage(fullRoutePayload);
pusher.publishMessage(fullRoutePayload);
console.log(`Broadcast full route (${route.length} points)`);

// Global interval that runs whether or not any clients are connected
let idx = 0;
setInterval(() => {
    if (idx >= route.length) {
        // stop or loop; here we just stop emitting
        return;
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
    pusher.publishMessage(locationData);

    console.log(`Broadcast point seq=${idx} lng=${lng} lat=${lat}`);
    idx++;
}, 1000);

app.get('/', (_req, res) => res.send('Route emitter running'));

httpServer.listen(3000, () => {
    console.log('Server listening on :3000');
});
