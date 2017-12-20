package de.maxhenkel.timelapse.telegram;

import com.pengrad.telegrambot.Callback;
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
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class TelegramBotAPI  extends TelegramBotBase{

    private TimelapseEngine timelapseEngine;
    private SimpleDateFormat simpleDateFormat;
    private Database database;
    private int adminUserID;
    private long maxMessageDelay;
    private boolean privateMode;

    public TelegramBotAPI(Configuration config, TimelapseEngine timelapseEngine, boolean privateMode) throws SQLException {
        super(config.getString("api_token", ""));
        this.timelapseEngine = timelapseEngine;
        this.privateMode=privateMode;
        String sdf = config.getString("telegram_date_format", "dd.MM.yyyy HH:mm:ss");
        simpleDateFormat = new SimpleDateFormat(sdf);
        database = new Database(config);
        adminUserID = config.getInt("admin_user_id", 9181493);
        maxMessageDelay = config.getLong("max_message_delay", 60000);
    }

    @Override
    protected void onCommand(String command, Message message) {
        if ((System.currentTimeMillis() - (message.date() * 1000L)) > maxMessageDelay) {
            Log.d("Ignoring late messages");
            return;
        }

        if (command.equalsIgnoreCase("/image")||command.equalsIgnoreCase("/bild")) {
            sendImage(message);
        }else if(command.equalsIgnoreCase("/id")){
            send(message.chat().id(), "Ihre User ID ist '" +message.from().id() +"'");
        }else if(command.equalsIgnoreCase("/private")){
            if(isAdmin(message)){
                privateMode=true;
                send(message.chat().id(), "Private Mode aktiviert");
            }
        }else if(command.equalsIgnoreCase("/public")){
            if(isAdmin(message)){
                privateMode=false;
                send(message.chat().id(), "Private Mode deaktiviert");
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

    private void parseCallback(String uid, long senderChatID) throws SQLException{
        int userID = Integer.parseInt(uid.substring(1));

        if (uid.startsWith("w")) {
            if (database.isWhitelisted(userID)) {
                send(senderChatID, "Nutzer ist bereits auf der Whitelist");
                return;
            } else {
                database.addToWhitelist(userID, "");
                send(senderChatID, "Nutzer wurde zur Whitelist hinzugefügt");
                send(userID, "Du wurdest von einem Admin zur Whitelist hinzugefügt");
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

    public void sendImage(Message message) {
        User user=message.from();
        Chat chat=message.chat();
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

        if(privateMode){
            Log.i("Sending no image to " + (user.username() == null ? String.valueOf(user.id()) : user.username()) +" because private mode is activiated");
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

    private boolean isAdmin(User user){
        if(user==null){
            return false;
        }
        return user.id().intValue()==adminUserID;
    }

    private boolean isAdmin(Message message){
        if(message==null){
            return false;
        }

        return isAdmin(message.from());
    }

    @Override
    public void stop() {
        database.close();
    }
}
