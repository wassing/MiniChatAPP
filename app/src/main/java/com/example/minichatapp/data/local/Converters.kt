package com.example.minichatapp.data.local

import androidx.room.TypeConverter
import com.example.minichatapp.domain.model.MessageType
import com.example.minichatapp.domain.model.MessageStatus

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMessageType(value: String?): MessageType? {
        return value?.let { enumValueOf<MessageType>(it) }
    }

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMessageStatus(value: String?): MessageStatus? {
        return value?.let { enumValueOf<MessageStatus>(it) }
    }
}