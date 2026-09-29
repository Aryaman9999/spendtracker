package com.aryaman.spendtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_rules",
    indices = [
        Index(value = ["priority"])
    ]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val category: String,
    val priority: Int = 100, // lower number = checked first
    val isRegex: Boolean = false
)
