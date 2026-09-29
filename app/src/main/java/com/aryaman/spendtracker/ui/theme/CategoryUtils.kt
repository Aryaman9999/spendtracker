package com.aryaman.spendtracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Semantic vector icon mapping for categories (replaces plain text letters).
 */
fun getCategoryIcon(category: String?): ImageVector {
    return when (category?.lowercase(Locale.ROOT)) {
        "personal spend", "personal" -> Icons.Default.Person
        "food & dining", "food" -> Icons.Default.Restaurant
        "groceries", "grocery" -> Icons.Default.ShoppingCart
        "travel & commute", "travel" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.LocalMall
        "bills & utilities", "bills" -> Icons.Default.FlashOn
        "entertainment" -> Icons.Default.Movie
        "health & medical", "health" -> Icons.Default.LocalHospital
        else -> Icons.Default.Category
    }
}

/**
 * Returns the first letter of the category as a fallback.
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
 * A modern squircle avatar displaying a crisp Material vector icon with tinted background.
 */
@Composable
fun CategoryAvatar(
    category: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val color = getCategoryColor(category)
    val icon = getCategoryIcon(category)
    val cornerRadius = (size.value * 0.3f).dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/**
 * A stylized circular or rounded avatar for merchants.
 */
@Composable
fun MerchantAvatar(
    merchantName: String?,
    isDebit: Boolean,
    category: String? = null,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val color = if (isDebit) RoseDebit else EmeraldSuccess
    val initial = merchantName?.trim()?.firstOrNull()?.uppercase() ?: "?"
    val cornerRadius = (size.value * 0.32f).dp

    // If category icon exists and is not generic, show category icon or clean initial
    val catIcon = category?.let { getCategoryIcon(it) }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (catIcon != null && catIcon != Icons.Default.Category) {
            Icon(
                imageVector = catIcon,
                contentDescription = merchantName,
                tint = color,
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            Text(
                text = initial,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}
