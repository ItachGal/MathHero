package io.github.galitach.mathhero.data.database

import androidx.room.TypeConverter
import io.github.galitach.mathhero.data.Operation

class Converters {
    @TypeConverter
    fun fromOperation(value: Operation): String = value.name

    @TypeConverter
    fun toOperation(value: String): Operation = Operation.valueOf(value)
}