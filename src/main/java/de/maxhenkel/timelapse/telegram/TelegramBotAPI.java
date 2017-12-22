package de.maxhenkel.timelapse.telegram;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.SendResponse;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.logging.Log;
import de.maxhenkel.henkellib.time.TimeFormatter;
import de.maxhenkel.timelapse.Database;
import de.maxhenkel.timelapse.TimelapseEngine;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class TelegramBotAPI extends TelegramBotBase {

    private TimelapseEngine timelapseEngine;
    private SimpleDateFormat simpleDateFormat;
    private Database database;
    private int adminUserID;
    private long maxMessageDelay;
    private boolean privateMode;

    public TelegramBotAPI(Configuration config, TimelapseEngine timelapseEngine, boolean privateMode) throws SQLException {
        super(config.getString("api_token", ""));
        this.timelapseEngine = timelapseEngine;
        this.privateMode = privateMode;
        String sdf = config.getString("telegram_date_format", "dd.MM.yyyy HH:mm:ss");
        simpleDateFormat = new SimpleDateFormat(sdf);
        database = new Database(config);
        adminUserID = config.getInt("admin_user_id", 9181493);
        maxMessageDelay = config.getLong("max_message_delay", 60000);
    }

    @Override
    protected void onCommand(String command, String[] args, Message message) {
        if ((System.currentTimeMillis() - (message.date() * 1000L)) > maxMessageDelay) {
            Log.d("Ignoring late messages");
            return;
        }

        if (command.equalsIgnoreCase("/image") || command.equalsIgnoreCase("/bild")) {
            sendImage(message);
        } else if (command.equalsIgnoreCase("/id")) {
            send(message.chat().id(), "Ihre User ID ist '" + message.from().id() + "'");
        } else if (command.equalsIgnoreCase("/private")) {
            if (isAdmin(message)) {
                privateMode = true;
                send(message.chat().id(), "Private Mode aktiviert");
            }
        } else if (command.equalsIgnoreCase("/public")) {
            if (isAdmin(message)) {
                privateMode = false;
                send(message.chat().id(), "Private Mode deaktiviert");
            }
        } else if (command.equalsIgnoreCase("/info")) {
            if (isAdmin(message)) {
                sendInfo(message);
            }
        } else if (command.equalsIgnoreCase("/remove")) {
            if (isAdmin(message)) {
                remove(message, args);
            }
        }
    }

    @Override
    protected void onCallbackQuery(CallbackQuery callbackQuery) {
        if (callbackQuery.message() == null || callbackQuery.message().chat() == null) {
            return;
        }
        long id = callbackQuery.message().chat().id();
        if (!isAdmin(callbackQuery.from())) {
            send(id, "Du bist kein Admin");
            return;
        }

        String message = callbackQuery.data();
        if (message == null) {
            return;
        }
        try {
            parseCallback(message, id);
        } catch (SQLException e) {
            Log.e("Failed to process callback");
            e.printStackTrace();
            send(id, "Datenbankfehler");
        } catch (Exception e) {
            Log.e("Failed to process callback");
            e.printStackTrace();
        }
    }

    private void parseCallback(String uid, long senderChatID) throws SQLException {
        int userID = Integer.parseInt(uid.substring(1));

        if (uid.startsWith("w")) {
            if (database.isWhitelisted(userID)) {
                send(senderChatID, "Nutzer ist bereits auf der Whitelist");
                return;
            } else {
                bot.execute(new GetChat(userID), new Callback<GetChat, GetChatResponse>() {
                    @Override
                    public void onResponse(GetChat request, GetChatResponse response) {
                        addToDatabase(userID, senderChatID, getName(response.chat()), true);
                    }

                    @Override
                    public void onFailure(GetChat request, IOException e) {
                        addToDatabase(userID, senderChatID, "", true);
                    }
                });
            }
        } else if (uid.startsWith("b")) {
            if (database.isWhitelisted(userID)) {
                send(senderChatID, "Nutzer ist auf der Whitelist");
                return;
            } else if (database.isBlacklisted(userID)) {
                send(senderChatID, "Nutzer ist bereits auf der Blacklist");
                return;
            } else {
                bot.execute(new GetChat(userID), new Callback<GetChat, GetChatResponse>() {
                    @Override
                    public void onResponse(GetChat request, GetChatResponse response) {
                        addToDatabase(userID, senderChatID, getName(response.chat()), false);
                    }

                    @Override
                    public void onFailure(GetChat request, IOException e) {
                        addToDatabase(userID, senderChatID, "", false);
                    }
                });
            }
        }
    }

    private String getName(Chat chat) {
        if (chat == null) {
            return "";
        } else if (chat.username() != null) {
            return "@" + chat.username();
        } else if (chat.firstName() != null) {
            String name = chat.firstName();
            if (chat.lastName() != null) {
                name += " " + chat.lastName();
            }
            return name;
        } else {
            return "";
        }
    }

    private void addToDatabase(int userID, long senderID, String comment, boolean whitelist) {
        try {
            String listname = whitelist ? "Whitelist" : "Blacklist";

            if (whitelist) {
                database.addToWhitelist(userID, comment);
            } else {
                database.addToBlacklist(userID, comment);
            }
            send(senderID, "Nutzer wurde zur " + listname + " hinzugefügt");
            if (whitelist) {
                send(userID, "Du wurdest von einem Admin zur " + listname + " hinzugefügt");
            }
            Log.i("Added user '" + userID + "' to " + listname.toLowerCase());
        } catch (SQLException e1) {
            e1.printStackTrace();
            send(senderID, "Nutzer konnte nicht zur Datenbank hinzugefügt werden (Datenbankfehler)");
        }
    }

    private void remove(Message message, String[] args) {
        if (args.length <= 0) {
            send(message.chat().id(), "Unzureichende Argumente");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(args[0]);
        } catch (Exception e) {
            send(message.chat().id(), "Fehlerhafte Argumente");
            return;
        }

        try {
            Database.Entry blacklistEntry = database.getBlacklistEntry(id);


            if (blacklistEntry != null) {
                String name = blacklistEntry.getComment().isEmpty() ? String.valueOf(id) : blacklistEntry.getComment();
                database.removeFromBlacklist(id);
                send(message.chat().id(), name + " wurde aus der Blacklist entfernt");
                return;
            }

            Database.Entry whitelistEntry = database.getWhitelistEntry(id);

            if (whitelistEntry != null) {
                String name = whitelistEntry.getComment().isEmpty() ? String.valueOf(id) : whitelistEntry.getComment();
                database.removeFromWhitelist(id);
                send(message.chat().id(), name + " wurde aus der Whitelist entfernt");
                return;
            }

            send(message.chat().id(), "Nutzer nicht gefunden");
        } catch (SQLException e) {
            send(message.chat().id(), "Datenbankfehler");
        }
    }

    public void sendImage(Message message) {
        User user = message.from();
        Chat chat = message.chat();
        if (user.isBot()) {
            return;
        }

        try {
            if (!isAdmin(message) && !database.isWhitelisted(user.id())) {
                Log.i("User " + (user.username() == null ? String.valueOf(user.id()) : user.username()) + " is not whitelisted");
                send(chat.id(), "Sie haben keine Berechtigung für diesen Befehl");
                if (!database.isBlacklisted(user.id())) {
                    sendAdminWhitelistRequest(user);
                } else {
                    Log.d("User " + (user.username() == null ? String.valueOf(user.id()) : user.username()) + " is blacklisted");
                }
                return;
            }
        } catch (SQLException e) {
            Log.e("Failed to check whitelist of user " + (user.username() == null ? String.valueOf(user.id()) : user.username()));
            e.printStackTrace();
            send(chat.id(), "Es ist ein fehler bei der Anfrage aufgetreten");
            return;
        }

        byte[] image = timelapseEngine.getLastImage();

        if (image == null) {
            send(chat.id(), "Momentan keine Bilder vorhanden");
            return;
        }

        if (privateMode) {
            Log.i("Sending no image to " + (user.username() == null ? String.valueOf(user.id()) : user.username()) + " because private mode is activiated");
            return;
        }

        Log.i("Sending image to " + (user.username() == null ? String.valueOf(user.id()) : user.username()));

        SendPhoto request = new SendPhoto(chat.id(), image).disableNotification(true);

        bot.execute(request, new Callback<SendPhoto, SendResponse>() {
            @Override
            public void onResponse(SendPhoto request, SendResponse response) {
                if (timelapseEngine.getLastImageTime() > 0) {
                    send(chat.id(), "Bild vom " + TimeFormatter.format(simpleDateFormat, timelapseEngine.getLastImageTime()));
                }
            }

            @Override
            public void onFailure(SendPhoto request, IOException e) {
                if (Log.isDebug()) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void send(long chatID, String message) {
        send(chatID, message, null);
    }

    private void send(long chatID, String message, Keyboard keyboard) {
        SendMessage request = new SendMessage(chatID, message).parseMode(ParseMode.HTML).disableNotification(true);

        if (keyboard != null) {
            request.replyMarkup(keyboard);
        }

        bot.execute(request, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
            }
        });
    }

    private void sendAdminWhitelistRequest(User user) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Whitelist").callbackData("w" + user.id()),
                        new InlineKeyboardButton("Blacklist").callbackData("b" + user.id())
                });

        String message = ("Anfrage von '" + user.username() + "' userid '" + user.id() + "'");

        send(adminUserID, message, inlineKeyboard);
    }

    private void sendInfo(Message message) {
        try {
            StringBuffer sb = new StringBuffer();
            List<Database.Entry> whitelist = database.getWhitelistEntries();

            sb.append("<b>Whitelist</b>");
            sb.append("\n");
            for (Database.Entry entry : whitelist) {
                sb.append(entry.getId() + ": " + entry.getComment());
                sb.append("\n");
            }

            List<Database.Entry> blacklist = database.getBlacklistEntries();

            sb.append("\n");
            sb.append("<b>Blacklist</b>");
            sb.append("\n");
            for (Database.Entry entry : blacklist) {
                sb.append(entry.getId() + ": " + entry.getComment());
                sb.append("\n");
            }

            send(message.chat().id(), sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            send(message.chat().id(), "Datenbankfehler");
        }
    }

    private boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return user.id().intValue() == adminUserID;
    }

    private boolean isAdmin(Message message) {
        if (message == null) {
            return false;
        }

        return isAdmin(message.from());
    }

    @Override
    public void stop() {
        database.close();
    }
}
