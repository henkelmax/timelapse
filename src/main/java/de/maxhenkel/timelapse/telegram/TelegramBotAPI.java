package de.maxhenkel.timelapse.telegram;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.logging.Log;
import de.maxhenkel.henkellib.time.TimeFormatter;
import de.maxhenkel.timelapse.Database;
import de.maxhenkel.timelapse.TimelapseEngine;
import org.json.JSONException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class TelegramBotAPI implements UpdatesListener {

    private TelegramBot bot;
    private TimelapseEngine timelapseEngine;
    private SimpleDateFormat simpleDateFormat;
    private Database database;
    private long adminChatID;
    private long maxMessageDelay;

    public TelegramBotAPI(Configuration config, TimelapseEngine timelapseEngine) throws SQLException {
        this.timelapseEngine = timelapseEngine;
        String sdf = config.getString("telegram_date_format", "dd.MM.yyyy HH:mm:ss");
        simpleDateFormat = new SimpleDateFormat(sdf);
        database = new Database(config);
        adminChatID = config.getLong("admin_chat_id", 9181493); //@get_id_bot
        maxMessageDelay = config.getLong("max_message_delay", 60000);
        this.bot = new TelegramBot(config.getString("api_token", "--APIKEY--"));
        bot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> list) {
        for (Update u : list) {
            try {
                processUpdate(u);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void processUpdate(Update update) {
        if (update.callbackQuery() != null) {
            processCallback(update.callbackQuery());
        } else if (update.message() != null) {
            processMessage(update.message());
        }
    }

    public void processMessage(Message message) {
        if (message.text() == null || message.chat() == null || message.from() == null) {
            return;
        }

        long date = message.date() * 1000L;
        if ((System.currentTimeMillis() - date) > maxMessageDelay) {
            Log.d("Ignoring late messages");
            return;
        }

        if (message.text().startsWith("/image")) {
            sendImage(message.chat(), message.from());
        }
    }

    public void processCallback(CallbackQuery callbackQuery) {
        if (callbackQuery.message() == null || callbackQuery.message().chat() == null) {
            return;
        }
        long id = callbackQuery.message().chat().id();
        if (id != adminChatID) {
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

    private void parseCallback(String uid, long senderChatID) throws SQLException, JSONException {
        int userID = Integer.parseInt(uid.substring(1));

        if (uid.startsWith("w")) {
            if (database.isWhitelisted(userID)) {
                send(senderChatID, "Nutzer ist bereits auf der Whitelist");
                return;
            } else {
                database.addToWhitelist(userID, "");
                send(senderChatID, "Nutzer wurde zur Whitelist hinzugefügt");
                //send(chatIDListed, "Du wurdest von einem Admin zur Whitelist hinzugefügt");
                Log.i("Added user '" + userID + "' to whitelist");
            }
        } else if (uid.startsWith("b")) {
            if (database.isWhitelisted(userID)) {
                send(senderChatID, "Nutzer ist auf der Whitelist");
                return;
            }else if (database.isBlacklisted(userID)) {
                send(senderChatID, "Nutzer ist bereits auf der Blacklist");
                return;
            } else {
                database.addToBlacklist(userID, "");
                send(senderChatID, "Nutzer wurde zur Blacklist hinzugefügt");
                Log.i("Added user '" + userID + "' to blacklist");
            }
        }
    }

    public void sendImage(Chat chat, User user) {
        if (user.isBot()) {
            return;
        }

        try {
            if (!chat.id().equals(adminChatID) && !database.isWhitelisted(user.id())) {
                Log.i("User " + (user.username() == null ? String.valueOf(user.id()) : user.username()) + " is not whitelisted");
                send(chat.id(), "Sie haben keine Berechtigung für diesen Befehl");
                if (!database.isBlacklisted(user.id())) {
                    sendAdminWhitelistRequest(chat.id(), user);
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

    private void sendAdminWhitelistRequest(long chatID, User user) {
        /*String whitelist=new JSONObject().put("type", "whitelist")
                .put("userid", user.id())
                .put("chatid", chatID)
                .put("username", user.username()).toString();
        String blacklist=new JSONObject().put("type", "blacklist")
                .put("userid", user.id())
                .put("chatid", chatID)
                .put("username", user.username()).toString();*/

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{
                        new InlineKeyboardButton("Whitelist").callbackData("w" + user.id()),
                        new InlineKeyboardButton("Blacklist").callbackData("b" + user.id())
                });

        String message = ("Anfrage von '" + user.username() + "' userid '" + user.id() + "'");

        send(adminChatID, message, inlineKeyboard);
    }

    public void stop() {
        database.close();
        bot.removeGetUpdatesListener();
    }
}
