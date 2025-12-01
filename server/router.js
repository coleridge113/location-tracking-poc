import express from 'express';
import Pusher from 'pusher';

const router = express.Router();

const pusher = new Pusher({
  appId: process.env.PUSHER_APP_ID,
  key: process.env.PUSHER_KEY,
  secret: process.env.PUSHER_SECRET,
  cluster: process.env.PUSHER_CLUSTER,
  useTLS: true,
});

router.post('/auth', express.json(), (req, res) => {
  const { socket_id, channel_name } = req.body || {};

  if (!socket_id || !channel_name) {
    return res.status(400).json({ error: 'socket_id and channel_name are required' });
  }

  const authResponse = pusher.authenticate(socket_id, channel_name);
  res.send(authResponse);
});

export default router;
