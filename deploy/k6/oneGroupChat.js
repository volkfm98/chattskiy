import ws from 'k6/ws';
import encoding from 'k6/encoding';
import { sleep, check } from 'k6';

export const options = {
  // vus: 10,
  // duration: '30s',
  stages: [
    { duration: "100s", target: 1000 },
    { duration: "100s", target: 2000 },
    // { duration: "50s", target: 2500 },
    { duration: "2m", target: 2000 },
  ],
};

export default function() {
  const url = "ws://192.168.1.103:8080/chat"
  const user = "user";
  const password = "password";
  const sendMessageInterval = 1000
  const params = {
    headers: {
      Authorization: `Basic ${encoding.b64encode(user + ":" + password)}`,
    }
  };

  const messageFactory = function() {
    return {
      type:"MESSAGE",
      eventId: crypto.randomUUID(),
      chatId: "20d4e415-1919-46f2-b8ba-c426a37221a2",
      content: "Load-testing message"
    }
  }

  const res = ws.connect(url, params, function (socket) {
    socket.on("message", (message) => {
      const obj = JSON.parse(message);

      // console.log(`Got event with eventId:${obj.eventId}`)

      check(obj, {
        'is not ERROR': o => o.type != "ERROR"
      })
    });

    socket.on("error", (error) => {console.log(error)});

    socket.setInterval(() => {
      const message = messageFactory();
      // console.log(`Sending event with eventId:${message.eventId}`)
      socket.send(JSON.stringify(message))
    }, sendMessageInterval)
  });


  check(res, { 'status is 101': (r) => r && r.status === 101 });
}
