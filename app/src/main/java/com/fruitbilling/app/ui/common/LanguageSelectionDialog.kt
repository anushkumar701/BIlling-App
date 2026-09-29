package com.fruitbilling.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fruitbilling.app.data.preferences.ShopPreferences
import com.fruitbilling.app.util.AppLocalization
import com.fruitbilling.app.util.CurrencyItem
import com.fruitbilling.app.util.LanguageItem
import com.fruitbilling.app.util.MoneyUtils

enum class LanguageDialogMode {
    BOTH,
    LANGUAGE_ONLY,
    CURRENCY_ONLY
}

/**
 * First-launch Language & Currency setup modal and in-app switcher.
 * Shown BEFORE Terms & Conditions so users read agreements and use the app in their native language & currency.
 * Also accessible from Menu to change language and currency anytime.
 */
@Composable
fun LanguageSelectionDialog(
    initialLanguage: String = "en",
    initialCurrencyCode: String = "INR",
    mode: LanguageDialogMode = LanguageDialogMode.BOTH,
    onConfirmed: (selectedLang: String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf(initialLanguage) }
    var selectedCurrency by remember {
        mutableStateOf(
            AppLocalization.SUPPORTED_CURRENCIES.find { it.code.equals(initialCurrencyCode, ignoreCase = true) }
                ?: AppLocalization.SUPPORTED_CURRENCIES[0]
        )
    }


    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(dismissOnBackPress = onDismiss != null, dismissOnClickOutside = onDismiss != null)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleText = when (mode) {
                    LanguageDialogMode.LANGUAGE_ONLY -> "🌐 Preferred Language"
                    LanguageDialogMode.CURRENCY_ONLY -> "💱 Store Currency"
                    LanguageDialogMode.BOTH -> "🌐 Choose Language & Currency"
                }
                val subtitleText = when (mode) {
                    LanguageDialogMode.LANGUAGE_ONLY -> "மொழியைத் தேர்ந்தெடுக்கவும் • भाषा चुनें"
                    LanguageDialogMode.CURRENCY_ONLY -> "நாணயத்தைத் தேர்ந்தெடுக்கவும் • मुद्रा चुनें"
                    LanguageDialogMode.BOTH -> "மொழியைத் தேர்ந்தெடுக்கவும் • भाषा चुनें"
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Language Section (Shown for BOTH or LANGUAGE_ONLY)
                    if (mode != LanguageDialogMode.CURRENCY_ONLY) {
                        Text(
                            text = "Primary Language / முதன்மை மொழி",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // English as Primary Option
                        val isEnglishSelected = selectedLang == "en"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLang = "en" },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEnglishSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            border = if (isEnglishSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🇬🇧", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "English",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = if (isEnglishSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Default / Primary",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Default application language",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (isEnglishSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Other Languages
                        Text(
                            text = "Other Languages / பிற மொழிகள்",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val otherLanguages = remember { AppLocalization.SUPPORTED_LANGUAGES.filter { it.code != "en" } }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            otherLanguages.chunked(2).forEach { rowLangs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowLangs.forEach { lang ->
                                        val isSelected = selectedLang == lang.code
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedLang = lang.code },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                }
                                            ),
                                            border = if (isSelected) {
                                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                            } else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = lang.flagEmoji, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = lang.nativeName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    )
                                                    Text(
                                                        text = lang.displayName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Currency Section (Shown for BOTH or CURRENCY_ONLY)
                    if (mode != LanguageDialogMode.LANGUAGE_ONLY) {
                        if (mode == LanguageDialogMode.BOTH) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                        Text(
                            text = "Store Currency / நாணயம்",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppLocalization.SUPPORTED_CURRENCIES.chunked(2).forEach { rowCurrs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowCurrs.forEach { curr ->
                                        val isSelected = selectedCurrency.code == curr.code
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedCurrency = curr },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                }
                                            ),
                                            border = if (isSelected) {
                                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                            } else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = curr.symbol,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${curr.code} (${curr.symbol})",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    )
                                                    Text(
                                                        text = curr.country,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        if (mode != LanguageDialogMode.CURRENCY_ONLY) {
                            ShopPreferences.setAppLanguage(context, selectedLang)
                            AppLocalization.updateAppLocale(context, selectedLang)
                        }
                        if (mode != LanguageDialogMode.LANGUAGE_ONLY) {
                            ShopPreferences.setCurrency(context, selectedCurrency.symbol, selectedCurrency.code)
                            MoneyUtils.setCurrency(selectedCurrency.symbol, selectedCurrency.code)
                        }
                        onConfirmed(selectedLang)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    val continueText = when (mode) {
                        LanguageDialogMode.LANGUAGE_ONLY -> when (selectedLang) {
                            "ta" -> "மொழியை அமை ✓"
                            "hi" -> "भाषा लागू करें ✓"
                            "ml" -> "ഭാഷ മാറ്റുക ✓"
                            "te" -> "భాషను మార్చండి ✓"
                            "es" -> "Aplicar Idioma ✓"
                            "ar" -> "تطبيق اللغة ✓"
                            "fr" -> "Appliquer la Langue ✓"
                            else -> "Apply Language ✓"
                        }
                        LanguageDialogMode.CURRENCY_ONLY -> "Apply Currency (${selectedCurrency.code}) ✓"
                        LanguageDialogMode.BOTH -> when (selectedLang) {
                            "ta" -> "தொடரவும் ➔"
                            "hi" -> "आगे बढ़ें ➔"
                            "ml" -> "തുടരുക ➔"
                            "te" -> "కొనసాగించండి ➔"
                            "es" -> "Continuar ➔"
                            "ar" -> "متابعة ➔"
                            "fr" -> "Continuer ➔"
                            else -> "Continue to Terms ➔"
                        }
                    }
                    Text(text = continueText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (onDismiss != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
