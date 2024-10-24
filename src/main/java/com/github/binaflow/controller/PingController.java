package com.github.binaflow.controller;

import com.github.binaflow.annotation.Controller;
import com.github.binaflow.annotation.MessageMapping;
import com.github.binaflow.dto.Ping;
import com.github.binaflow.dto.Pong;

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
