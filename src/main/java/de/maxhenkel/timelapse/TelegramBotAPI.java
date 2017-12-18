package de.maxhenkel.timelapse;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.logging.Log;
import de.maxhenkel.henkellib.time.TimeFormatter;

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

    public TelegramBotAPI(Configuration config, TimelapseEngine timelapseEngine) throws SQLException {
        this.timelapseEngine = timelapseEngine;
        String sdf = config.getString("telegram_date_format", "dd.MM.yyyy HH:mm:ss");
        simpleDateFormat = new SimpleDateFormat(sdf);
        database = new Database(config);
        adminChatID = config.getLong("admin_chat_id", 9181493); //@get_id_bot
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
        if (update.message() != null) {
            processMessage(update.message());
        }
    }

    public void processMessage(Message message) {
        if (message.text() == null || message.chat() == null || message.from() == null) {
            return;
        }

        if (message.text().startsWith("/image")) {
            sendImage(message.chat(), message.from());
        } else if (message.text().startsWith("/whitelist_")) {
            processWhitelist(message.text(), message.chat());
        }
    }

    public void sendImage(Chat chat, User user) {
        if (user.isBot()) {
            return;
        }

        try {
            if (!chat.id().equals(adminChatID) && !database.isWhitelisted(user.id())) {
                Log.i("User " + (user.username() == null ? String.valueOf(user.id()) : user.username()) + " is not whitelisted");
                send(chat, "Sie haben keine Berechtigung für diesen Befehl");
                sendAdminWhitelistRequest(chat.id(), user);
                return;
            }
        } catch (SQLException e) {
            Log.e("Failed to check whitelist of user " + (user.username() == null ? String.valueOf(user.id()) : user.username()));
            e.printStackTrace();
            send(chat, "Es ist ein fehler bei der Anfrage aufgetreten");
            return;
        }

        byte[] image = timelapseEngine.getLastImage();

        if (image == null) {
            send(chat, "Momentan keine Bilder vorhanden");
            return;
        }

        Log.i("Sending image to " + (user.username() == null ? String.valueOf(user.id()) : user.username()));

        SendPhoto request = new SendPhoto(chat.id(), image).disableNotification(true);

        bot.execute(request, new Callback<SendPhoto, SendResponse>() {
            @Override
            public void onResponse(SendPhoto request, SendResponse response) {
                if (timelapseEngine.getLastImageTime() > 0) {
                    send(chat, "Bild vom " + TimeFormatter.format(simpleDateFormat, timelapseEngine.getLastImageTime()));
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

    private void send(Chat chat, String message) {
        send(chat.id(), message);
    }

    private void send(long chatid, String message) {
        SendMessage request = new SendMessage(chatid, message).parseMode(ParseMode.HTML).disableNotification(true);

        bot.execute(request, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
            }
        });
    }

    private void sendAdminWhitelistRequest(long chatid, User user) {
        if (user.username() != null) {
            send(adminChatID, "Anfrage von '" + user.username() + "' userid '" + user.id() + "' \n/whitelist_" + chatid);
        } else {
            send(adminChatID, "Anfrage von userid '" + user.id() + "' \n/whitelist_" + chatid);
        }
    }

    public void processWhitelist(String msg, Chat chat) {
        if (!chat.id().equals(adminChatID)) {
            send(chat, "Du bist nicht Max!");
            return;
        }

        String[] msgSplit = msg.split(" ");

        if (msgSplit.length <= 0) {
            return;
        }

        String id = msgSplit[0].replace("/whitelist_", "");
        int addID;
        try {
            addID = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            send(chat, "Fehlerhafte formatierung");
            return;
        }

        try {
            if (database.isWhitelisted(addID)) {
                send(chat, "Nutzer ist bereits auf der whitelist");
                return;
            } else {
                String comment = "";
                if (msgSplit.length >= 2) {
                    for (int i = 1; i < msgSplit.length; i++) {
                        comment += " " + msgSplit[i];
                    }
                }
                database.addToWhitelist(addID, comment);
                send(chat, "Nutzer wurde hinzugefügt");
                Log.i("Added user '" + addID + "' with comment '" + comment + "'");
            }
        } catch (SQLException e) {
            Log.e("Failed to add user to whitelist");
            e.printStackTrace();
            send(chat, "Datenbankfehler");
            return;
        }
    }

    public void stop() {
        database.close();
        bot.removeGetUpdatesListener();
    }
}
