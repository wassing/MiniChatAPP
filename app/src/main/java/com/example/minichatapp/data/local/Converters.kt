package com.example.minichatapp.data.local

import androidx.room.TypeConverter
import com.example.minichatapp.domain.model.MessageType
import com.example.minichatapp.domain.model.MessageStatus

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = enumValueOf(value)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = enumValueOf(value)
}