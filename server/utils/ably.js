import 'dotenv/config';
import Ably from 'ably';

const client = new Ably.Realtime({ key: process.env.ABLY_API_KEY });
const channel = client.channels.get("ably-channel");

client.connection.on('connectionstate', (stateChange) => {
    console.log(
        '[Ably] connection state:',
        stateChange.previous, '->', stateChange.current,
        stateChange.reason ? `reason=${stateChange.reason.message}` : ''
    );
});

export function publishMessage(data) {
    channel.publish("ably-route", data);
}

export default client;
