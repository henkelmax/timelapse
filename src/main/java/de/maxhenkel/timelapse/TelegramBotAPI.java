package de.maxhenkel.timelapse;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.logging.Log;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TelegramBotAPI implements UpdatesListener{

    private TelegramBot bot;
    private TimelapseEngine timelapseEngine;

    public TelegramBotAPI(Configuration config, TimelapseEngine timelapseEngine){
        this.timelapseEngine=timelapseEngine;
        this.bot = new TelegramBot(config.getString("api_token", "373696179:AAEiNVKQHumH5Ld6pBt1TDwVJeP7T5pPAcw"));
        bot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> list) {
        for(Update u:list){
            processUpdate(u);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void processUpdate(Update update){
        if(update.message()!=null){
            processMessage(update.message());
        }
    }

    public void processMessage(Message message){
        if(message.text().startsWith("/image")) {
            sendImage(message.chat());
        }
    }

    public void sendImage(Chat chat){
        File image=timelapseEngine.getLastImageFile();

        if(image==null||!image.exists()){
            send(chat, "Momentan keine Bilder vorhanden");
            return;
        }

        Log.i("Sending image to " +chat.username());


        SendPhoto request = new SendPhoto(chat.id(), image).disableNotification(true);

        bot.execute(request, new Callback<SendPhoto, SendResponse>() {
            @Override
            public void onResponse(SendPhoto request, SendResponse response) {}

            @Override
            public void onFailure(SendPhoto request, IOException e) {
                if(Log.isDebug()){
                    e.printStackTrace();
                }
            }
        });
    }

    private void send(Chat chat, String message) {
        SendMessage request = new SendMessage(chat.id(), message).parseMode(ParseMode.HTML).disableNotification(true);

        bot.execute(request, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {}

            @Override
            public void onFailure(SendMessage request, IOException e) {}
        });
    }

    public void stop(){
        bot.removeGetUpdatesListener();
    }
}
