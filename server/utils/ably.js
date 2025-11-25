import 'dotenv/config';
import Ably from 'ably';

const client = new Ably.Realtime({ key: process.env.ABLY_API_KEY });
const channel = client.channels.get("ably-channel");

export function publishMessage(data) {
    channel.publish("ably-route", data);
}

export default client;
