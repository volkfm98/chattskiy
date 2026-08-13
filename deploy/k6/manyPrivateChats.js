import ws from 'k6/ws';
import encoding from 'k6/encoding';
import exec from 'k6/execution';
import { open } from 'k6/experimental/fs';
import csv from 'k6/experimental/csv';
import { sleep, check } from 'k6';

export const options = {
  // vus: 10,
  // duration: '30s',
  stages: [
    { duration: "100s", target: 1000 },
    { duration: "100s", target: 2000 },
    { duration: "100s", target: 3000 },
    { duration: "2m", target: 3000 },
  ],
};

const user = await open('csv/user.csv');
const userRecords = await csv.parse(user, { delimiter: ',' });
const chat = await open('csv/chat.csv');
const chatRecords = await csv.parse(chat, { delimiter: ',' });
const chatUser = await open('csv/chat_user.csv');
const chatUserRecords = await csv.parse(chatUser, { delimiter: ',' });


export default function() {
  const iteration = exec.vu.idInTest;
  const url = "ws://192.168.1.101:8080/chat";
  const user = `user${iteration}`;
  const password = "password";
  const sendMessageInterval = 1000
  const params = {
    headers: {
      Authorization: `Basic ${encoding.b64encode(user + ":" + password)}`,
    }
  };

  // console.log(user);

  const messageFactory = function() {
    return {
      type:"MESSAGE",
      eventId: crypto.randomUUID(),
      chatId: chatRecords[iteration][0],
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
