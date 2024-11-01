package io.github.binaflow.controller;

import io.github.binaflow.annotation.Controller;
import io.github.binaflow.annotation.MessageMapping;
import io.github.binaflow.dto.Ping;
import io.github.binaflow.dto.Pong;

/**
 * Controller for handling Ping messages.
 */
@Controller
public class PingController {

    @MessageMapping
    public Pong pingHandler(Ping ping) {
        return Pong.newBuilder()
                .setMessageType("Pong")
                .setMessageId(ping.getMessageId())
                .build();
    }
}
