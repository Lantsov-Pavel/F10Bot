package com.burbot.F10Bot.service;

import com.burbot.F10Bot.config.BotConfig;
import com.burbot.F10Bot.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    TasksRepository tasksRepository;

    final BotConfig config;

    static final String HELP_TEXT = "It will check what useful you have done today. Every 2 hours it will check if " +
            "you have completed the tasks in order. You can write him 5 of your tasks, at the end of the week he" +
            " will send you statistics on how productive your week was. You can use this commands\n\n" +
            "/register You register to use the bot\n\n" +
            "/add You add your task\n\n" +
            "/edit You edit your task\n\n" +
            "/mytasks Display a list of your tasks\n\n" +
            "/mydata Display your data\n\n";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data source"));
        listOfCommands.add(new BotCommand("/deletedata", "delete your data source"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/add", "add your tasks"));
        listOfCommands.add(new BotCommand("/edit", "edit your tasks"));
        listOfCommands.add(new BotCommand("/mytasks", "show your tasks"));
        listOfCommands.add(new BotCommand("/register", "register your chatID and first name"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list$" + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.contains("/send")) {
                String textToSend = messageText.substring(messageText.indexOf(" "));
                Iterable<Users> users = usersRepository.findAll();
                for (Users user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
                if (messageText.contains("/add")){
                    String textTaskToAdd = messageText.substring(messageText.indexOf(" "));
                    addTask(chatId,textTaskToAdd);
                    sendMessage(chatId, "Success");

                }
            } else {
                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        helpButton(chatId);
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_TEXT);
                        break;
                    case "/add ":
                        sendMessage(chatId, "You can add your tasks for all day. You can add only 5 tasks, no more");
                        break;
                    case "/edit":
                        sendMessage(chatId, "Which task you want to edit?");
                        break;
                    case "/mytask":
                        sendMessage(chatId, "Your tasks: ");
                        break;
                    case "/comletetask":
                        sendMessage(chatId, "Which task you want to complete?");
                        break;
                    case "/mydata":
                        sendMessage(chatId, "Bot use your chatID " + chatId + " and your first name");
                        break;
                    case "/register":
                        registerUser(update.getMessage());
                        sendMessage(chatId, "Success");
                        break;
                    default:
                        sendMessage(chatId, "Sorry, command wasn't recognized. Burbot is useless.");
                        sendMessage(chatId, "You can find bot's command in Menu or in Help");
                        helpButton(chatId);
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callBackData.equals("HELP_BUTTON")) {
                executeEditMessage( chatId, messageId);
            }
        }
    }

    private void startCommandReceived(long chatId, String firstName) {
        String answer = "Hi " + firstName + " nice to meet you!";
        log.info("Replied to user: " + firstName);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void helpButton(long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Press help and you get information about this bot");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("HELP");
        inlineKeyboardButton.setCallbackData("HELP_BUTTON");
        rowInline.add(inlineKeyboardButton);
        rowsInline.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        msg.setReplyMarkup(inlineKeyboardMarkup);
        executeMessage(msg);
    }

    private void registerUser(Message msg) {
        if (usersRepository.findById(msg.getChatId()).isEmpty()) {
            Long chatId = msg.getChatId();
            var chat = msg.getChat();
            Users user = new Users();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            usersRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void executeEditMessage(long chatId, long messageId) {
        EditMessageText messageText = new EditMessageText();
        messageText.setChatId(String.valueOf(chatId));
        messageText.setText(HELP_TEXT);
        messageText.setMessageId((int) messageId);
        try {
            execute(messageText);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }

    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void addTask(long chatId, String taskToAdd){
        if (tasksRepository.findById(chatId).isEmpty()){
            Tasks tasks = new Tasks();
            tasks.setChatId(chatId);
            tasks.setStatus("N");
            tasks.setNmbr(1l);
            tasks.setTask(taskToAdd);
            tasksRepository.save(tasks);
        }

    }

    //@Scheduled(cron = "* * * * * *")
  //  private void sendMessage() {
      //  var messages = messageForUsersRepository.findAll();
       // var users = usersRepository.findAll();
       // for (MessagesForUsers messageForUsers : messages) {
         //   for (Users user : users) {
          //      sendMessage(user.getChatId(), messageForUsers.getMessage());
          //  }
       // }
   // }
}
