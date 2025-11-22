package io.github.prinnyhu.wolbotnative.bot;

import io.github.prinnyhu.wolbotnative.util.WakeOnLanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Slf4j
@Component
public class WakeOnLanBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final String validUserName;
    private final String macAddress;
    private final String broadcastAddress;
    private final Integer broadcastPort;
    private final TelegramClient telegramClient;

    public WakeOnLanBot(@Value("${wol.bot.token}") String botToken,
                        @Value("${wol.valid.use.name}") String validUserName,
                        @Value("${wol.mac.address}") String macAddress,
                        @Value("${wol.broadcast.address}") String broadcastAddress,
                        @Value("${wol.broadcast.port}") Integer broadcastPort) {
        this.validUserName = validUserName;
        if (!StringUtils.hasText(validUserName)) {
            throw new IllegalArgumentException("validUserName is required");
        }
        this.macAddress = macAddress;
        if (!WakeOnLanUtils.checkMacAddress(macAddress)) {
            throw new IllegalArgumentException("invalid macAddress");
        }
        this.broadcastAddress = broadcastAddress;
        if (!WakeOnLanUtils.checkBroadcastAddress(broadcastAddress)) {
            throw new IllegalArgumentException("invalid broadcastAddress");
        }
        this.broadcastPort = broadcastPort;
        if (!WakeOnLanUtils.checkBroadcastPort(broadcastPort)) {
            throw new IllegalArgumentException("invalid broadcastPort");
        }
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        try {
            if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }
            if (!isValidUser(update.getMessage())) {
                return;
            }

            String text = update.getMessage().getText();
            if ("/wol".equals(text)) {
                WakeOnLanUtils.sendWolPacket(macAddress, broadcastAddress, broadcastPort);
                reply(update.getMessage().getChatId(), update.getMessage().getMessageId(), "done");
            } else {
                reply(update.getMessage().getChatId(), update.getMessage().getMessageId(), "dont support");
            }
        } catch (Exception e) {
            log.error("[WakeOnLanBot] consume error", e);
        }
    }

    private boolean isValidUser(Message message) {
        String userName = Optional.ofNullable(message)
                .map(Message::getFrom)
                .map(User::getUserName)
                .orElse(null);
        return validUserName.equals(userName);
    }

    private void reply(Long chatId, Integer messageId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .replyToMessageId(messageId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}
