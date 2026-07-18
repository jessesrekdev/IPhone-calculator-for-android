package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
