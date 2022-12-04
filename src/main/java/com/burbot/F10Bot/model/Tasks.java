package com.burbot.F10Bot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@Entity(name="TASK_DATA")
public class Tasks {

    @Id
    private Long chatId;

    private Long nmbr;

    private String task;

    private String status;


}