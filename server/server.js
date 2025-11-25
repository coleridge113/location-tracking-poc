import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import fs from 'fs';
import path from 'path';
import * as ably from './utils/ably.js'

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

io.on('connection', socket => {
    console.log('Client connected', socket.id);
    socket.emit('route', { type: 'route', coordinates: route });
    console.log(`Emitted full route (${route.length} points)`);

    let idx = 0;
    const interval = setInterval(() => {
        if (idx >= route.length) {
            clearInterval(interval);
            console.log('Finished streaming points');
            return;
        }
        const [lng, lat] = route[idx];
        const locationData = {
            type: 'point',
            seq: idx,
            lng,
            lat,
            ts: Date.now()
        } 

        ably.publishMessage(locationData)

        socket.emit('point', locationData);
        console.log(`Emitted point seq=${idx} lng=${lng} lat=${lat}`);
        idx++;
    }, 1000);

    socket.on('disconnect', () => {
        clearInterval(interval);
        console.log('Client disconnected', socket.id);
    });
});

app.get('/', (_req, res) => res.send('Route emitter running'));

httpServer.listen(3000, () => {
    console.log('Server listening on :3000');
});
