package com.aryaman.spendtracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Consistent category color mapping used across all screens.
 */
fun getCategoryColor(category: String?): Color {
    return when (category?.lowercase(Locale.ROOT)) {
        "personal spend", "personal" -> CategoryPersonal
        "food & dining", "food" -> CategoryFood
        "groceries", "grocery" -> CategoryGroceries
        "travel & commute", "travel" -> CategoryTravel
        "shopping" -> CategoryShopping
        "bills & utilities", "bills" -> CategoryBills
        "entertainment" -> CategoryEntertainment
        "health & medical", "health" -> CategoryHealth
        else -> CategoryOther
    }
}

/**
 * Returns the first letter of the category for use in circular avatars.
 */
fun getCategoryInitial(category: String?): String {
    return when (category?.lowercase(Locale.ROOT)) {
        "personal spend", "personal" -> "P"
        "food & dining", "food" -> "F"
        "groceries", "grocery" -> "G"
        "travel & commute", "travel" -> "T"
        "shopping" -> "S"
        "bills & utilities", "bills" -> "B"
        "entertainment" -> "E"
        "health & medical", "health" -> "H"
        else -> "?"
    }
}

/**
 * A colored circular avatar displaying the first letter of a category.
 * Replaces emojis for a clean, consistent look across devices and themes.
 */
@Composable
fun CategoryAvatar(
    category: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val color = getCategoryColor(category)
    val initial = getCategoryInitial(category)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

/**
 * A colored circular avatar displaying a merchant initial letter.
 */
@Composable
fun MerchantAvatar(
    merchantName: String?,
    isDebit: Boolean,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val color = if (isDebit) RoseDebit else EmeraldSuccess
    val initial = merchantName?.firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}
