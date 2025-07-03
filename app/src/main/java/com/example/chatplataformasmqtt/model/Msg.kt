package com.example.chatplataformasmqtt.model

data class Msg(
    val texto: String,
    val epoch: Long,
    val fromMe: Boolean
)