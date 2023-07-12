package de.maxhenkel.timelapse.telegram;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.SendResponse;
import de.maxhenkel.timelapse.Database;
import de.maxhenkel.timelapse.Main;
import de.maxhenkel.timelapse.TimelapseEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class TelegramBotAPI extends TelegramBotBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private final TimelapseEngine timelapseEngine;
    private final SimpleDateFormat simpleDateFormat;
    private final Database database;
    private final long adminUserID;
    private final long maxMessageDelay;
    private boolean privateMode;

    public TelegramBotAPI(TimelapseEngine timelapseEngine, String databasePath, boolean privateMode) throws SQLException {
        super(Main.CONFIG.apiToken.get());
        this.timelapseEngine = timelapseEngine;
        this.privateMode = privateMode;
        simpleDateFormat = new SimpleDateFormat(Main.CONFIG.telegramDateFormat.get());
        database = new Database(databasePath);
        adminUserID = Main.CONFIG.adminUserId.get();
        maxMessageDelay = Main.CONFIG.maxMessageDelay.get();
    }

    @Override
    protected void onCommand(String command, String[] args, Message message) {
        if ((System.currentTimeMillis() - (message.date() * 1000L)) > maxMessageDelay) {
            LOGGER.debug("Ignoring old messages");
            return;
        }

        if (command.equalsIgnoreCase("/image")) {
            sendImage(message);
        } else if (command.equalsIgnoreCase("/id")) {
            send(message.chat().id(), "Your ID is '" + message.from().id() + "'");
        } else if (command.equalsIgnoreCase("/private")) {
            if (isAdmin(message)) {
                privateMode = true;
                send(message.chat().id(), "Private mode activated");
            }
        } else if (command.equalsIgnoreCase("/public")) {
            if (isAdmin(message)) {
                privateMode = false;
                send(message.chat().id(), "Private mode deactivated");
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
            send(id, "You are not an admin");
            return;
        }

        String message = callbackQuery.data();
        if (message == null) {
            return;
        }
        try {
            parseCallback(message, id);
        } catch (SQLException e) {
            LOGGER.error("Failed to process callback");
            e.printStackTrace();
            send(id, "Database error");
        } catch (Exception e) {
            LOGGER.error("Failed to process callback");
            e.printStackTrace();
        }
    }

    private void parseCallback(String uid, long senderChatID) throws SQLException {
        long userID = Long.parseLong(uid.substring(1));

        if (uid.startsWith("w")) {
            if (database.isWhitelisted(userID)) {
                send(senderChatID, "This user is already whitelisted");
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
                send(senderChatID, "This user is already whitelisted");
            } else if (database.isBlacklisted(userID)) {
                send(senderChatID, "This user is already blacklisted");
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

    private void addToDatabase(long userID, long senderID, String comment, boolean whitelist) {
        try {
            String listname = whitelist ? "whitelist" : "blacklist";

            if (whitelist) {
                database.addToWhitelist(userID, comment);
            } else {
                database.addToBlacklist(userID, comment);
            }
            send(senderID, "User was added to " + listname);
            if (whitelist) {
                send(userID, "You were added to the " + listname + " by an admin");
            }
            String name = comment.isEmpty() ? String.valueOf(userID) : comment;
            LOGGER.info("Added user " + name + " to " + listname.toLowerCase());
        } catch (SQLException e1) {
            e1.printStackTrace();
            send(senderID, "Couldn't add user to database (Database error)");
        }
    }

    private void remove(Message message, String[] args) {
        if (args.length <= 0) {
            send(message.chat().id(), "Insufficient arguments");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(args[0]);
        } catch (Exception e) {
            send(message.chat().id(), "Incorrect arguments");
            return;
        }

        try {
            Database.Entry blacklistEntry = database.getBlacklistEntry(id);


            if (blacklistEntry != null) {
                String name = blacklistEntry.getComment().isEmpty() ? String.valueOf(id) : blacklistEntry.getComment();
                database.removeFromBlacklist(id);
                send(message.chat().id(), name + " was removed from the blacklist");
                LOGGER.info("Removed User " + name + " from blacklist");
                return;
            }

            Database.Entry whitelistEntry = database.getWhitelistEntry(id);

            if (whitelistEntry != null) {
                String name = whitelistEntry.getComment().isEmpty() ? String.valueOf(id) : whitelistEntry.getComment();
                database.removeFromWhitelist(id);
                send(message.chat().id(), name + " was removed from the whitelist");
                LOGGER.info("Removed User " + name + " from whitelist");
                return;
            }

            send(message.chat().id(), "User not found");
        } catch (SQLException e) {
            send(message.chat().id(), "Database error");
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
                LOGGER.info("User " + getName(chat) + " is not whitelisted");
                send(chat.id(), "You don't have permission for this command");
                if (!database.isBlacklisted(user.id())) {
                    sendAdminWhitelistRequest(user);
                } else {
                    LOGGER.debug("User " + getName(chat) + " is blacklisted");
                }
                return;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check whitelist of user " + getName(chat));
            e.printStackTrace();
            send(chat.id(), "An error occurred processing this request");
            return;
        }

        byte[] image = timelapseEngine.getLastImage();

        if (image == null) {
            send(chat.id(), "Currently no images available");
            return;
        }

        if (privateMode) {
            LOGGER.info("Sending no image to " + getName(chat) + " because private mode is activiated");
            return;
        }

        LOGGER.info("Sending image to " + getName(chat));

        SendPhoto request = new SendPhoto(chat.id(), image).disableNotification(true);

        bot.execute(request, new Callback<SendPhoto, SendResponse>() {
            @Override
            public void onResponse(SendPhoto request, SendResponse response) {
                if (timelapseEngine.getLastImageTime() > 0) {
                    send(chat.id(), "Picture taken on " + Main.format(simpleDateFormat, timelapseEngine.getLastImageTime()));
                }
            }

            @Override
            public void onFailure(SendPhoto request, IOException e) {
                if (LOGGER.isDebugEnabled()) {
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
                new InlineKeyboardButton("Whitelist").callbackData("w" + user.id()),
                new InlineKeyboardButton("Blacklist").callbackData("b" + user.id()));

        String message = ("Request by '" + user.username() + "' userid '" + user.id() + "'");

        send(adminUserID, message, inlineKeyboard);
    }

    private void sendInfo(Message message) {
        try {
            StringBuilder sb = new StringBuilder();
            List<Database.Entry> whitelist = database.getWhitelistEntries();

            sb.append("<b>Whitelist</b>\n");
            for (Database.Entry entry : whitelist) {
                sb.append(entry.getId()).append(": ").append(entry.getComment()).append("\n");
            }

            List<Database.Entry> blacklist = database.getBlacklistEntries();

            sb.append("\n<b>Blacklist</b>\n");
            for (Database.Entry entry : blacklist) {
                sb.append(entry.getId()).append(": ").append(entry.getComment()).append("\n");
            }

            send(message.chat().id(), sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            send(message.chat().id(), "Database error");
        }
    }

    private boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return user.id() == adminUserID;
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
