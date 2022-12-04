package com.burbot.F10Bot.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@Entity(name = "USERS_DATA")
public class Users {
    @Id
    private Long chatId;

    private String firstName;

    }

