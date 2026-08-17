import csv
import uuid
import psycopg2

user_counter = -1
chat_counter = -1 # we'll increment it back to 0 first

all_users = []
all_chats = []
all_chat_users = []

def generateUser():
    global user_counter
    user_counter += 1

    return {
        "id": str(uuid.uuid4()),
        "username": f"user{user_counter}",
        "password": "{noop}password",
        "role": "USER"
    }

def addAdmin():
    admin = {
        "id": str(uuid.uuid4()),
        "username": "admin",
        "password": "{noop}admin",
        "role": "ADMIN"
    }

    all_users.append(admin)

def generateChat():
    global chat_counter
    chat_counter += 1

    return {
        "id": str(uuid.uuid4()),
        "name": f"chat{chat_counter}"
    }

def addUsersToChat(users, chatId):
    return [{"chat_id": chatId, "user_id": user["id"]} for user in users]

def groupChat(count):
    users = [generateUser() for _ in range(count)]
    all_users.extend(users)

    chat = generateChat()
    all_chats.append(chat)

    chat_users = addUsersToChat(users, chat["id"])
    all_chat_users.extend(chat_users)

    return chat_users

def privateChat():
    return groupChat(2)

def writeToFile(dict, filename, fieldnames):
    with open(f"{filename}.csv", "w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames)
        writer.writerows(dict)

def dumpData():
    writeToFile(all_users, "csv/user", ["id", "username", "password", "role"])
    writeToFile(all_chats, "csv/chat", ["id", "name"])
    writeToFile(all_chat_users, "csv/chat_user", ["chat_id", "user_id"])

def insertUser(cursor, user):
    query = "INSERT INTO public.user (id, username, password, role) VALUES (%(id)s, %(username)s, %(password)s, %(role)s)"
    cursor.execute(query, user)

def insertChat(cursor, chat):
    query = "INSERT INTO public.chat (id, name) VALUES (%(id)s, %(name)s)"
    cursor.execute(query, chat)

def insertChatUser(cursor, user):
    query = "INSERT INTO public.chat_user (chat_id, user_id) VALUES (%(chat_id)s, %(user_id)s)"
    cursor.execute(query, user)

def insertUsers(cur, users):
    for user in users:
        insertUser(cur, user)


def insertChats(cur, chats):
    for chat in chats:
        insertChat(cur, chat)


def insertChatUsers(cur, chat_users):
    for chat_user in chat_users:
        insertChatUser(cur, chat_user)

def truncate(cur, table_name):
    cur.execute(f"TRUNCATE TABLE public.{table_name} CASCADE")

def insertIntoDB():
    conn = psycopg2.connect(
        host="192.168.1.101",
        database="chattskiy",
        user="postgres",
        password="passwd",
        port="5432"
    )

    cursor = conn.cursor()

    truncate(cursor, "chat_user")
    truncate(cursor, "user")
    truncate(cursor, "chat")

    insertUsers(cursor, all_users)
    insertChats(cursor, all_chats)
    insertChatUsers(cursor, all_chat_users)

    conn.commit()
    cursor.close()
    conn.close()

for i in range(5000):
    privateChat()

addAdmin()

dumpData()
insertIntoDB()