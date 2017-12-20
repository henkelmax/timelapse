package de.maxhenkel.timelapse.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.Update;
import java.util.List;

public abstract class TelegramBotBase implements UpdatesListener {

    protected TelegramBot bot;

    public TelegramBotBase(String apiKey){
        this.bot = new TelegramBot(apiKey);
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

    private void processUpdate(Update update) {
        onUpdate(update);

        if(update.message()!=null){
            String command=getCommand(update.message());
            if(command!=null){
                onCommand(command, update.message());
            }
        }
        if(update.callbackQuery()!=null){
            onCallbackQuery(update.callbackQuery());
        }
    }

    protected void onUpdate(Update update){

    }

    protected void onCommand(String command, Message message){

    }

    protected void onCallbackQuery(CallbackQuery callbackQuery){

    }

    protected String getCommand(Message message){
        if(message.entities()==null){
            return null;
        }

        for(MessageEntity entity:message.entities()){
            if(!entity.type().equals(MessageEntity.Type.bot_command)){
                continue;
            }
            String command=message.text().substring(entity.offset(), entity.offset()+entity.length());

            return command;
        }

        return null;
    }

    public void stop(){
        bot.removeGetUpdatesListener();
    }

}
