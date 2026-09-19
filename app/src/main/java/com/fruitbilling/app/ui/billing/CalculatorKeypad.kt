package com.fruitbilling.app.ui.billing

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fruitbilling.app.data.model.ProductUnit

/**
 * Optimized Calculator Keypad for high-speed counter operations:
 *
 *  Shortcuts: 2 compact rows of up to 6 buttons each
 *    Row A: [100g] [200g] [250g] [300g] [400g] [500g]
 *    Row B: [600g] [700g] [750g] [800g] [900g] [1kg]
 *
 *  4-Row Keypad:
 *    Row 1: [7] [8] [9] [⌫]
 *    Row 2: [4] [5] [6] [×]
 *    Row 3: [1] [2] [3] [−]
 *    Row 4: [0] [.] [+] [=]
 *
 *  (Divide '÷' and redundant 5th full-width '+' row removed to maximize receipt list vertical height)
 */
@Composable
fun CalculatorKeypad(
    quickShortcuts: List<String>,
    @Suppress("UNUSED_PARAMETER") currentUnit: ProductUnit?,
    onShortcutClicked: (String) -> Unit,
    onDigitClicked: (String) -> Unit,
    onOperatorClicked: (String) -> Unit,
    onDecimalClicked: () -> Unit,
    onEqualsClicked: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    fun haptic() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    fun hapticConfirm() = view.performHapticFeedback(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Quick weight / piece shortcuts ──────────────────────────────────
        if (quickShortcuts.size <= 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickShortcuts.forEach { shortcut ->
                    QuickShortcutButton(
                        text = shortcut,
                        onClick = { haptic(); onShortcutClicked(shortcut) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            val row1 = quickShortcuts.take(6)
            val row2 = quickShortcuts.drop(6)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row1.forEach { shortcut ->
                    QuickShortcutButton(
                        text = shortcut,
                        onClick = { haptic(); onShortcutClicked(shortcut) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row2.forEach { shortcut ->
                    QuickShortcutButton(
                        text = shortcut,
                        onClick = { haptic(); onShortcutClicked(shortcut) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Row 1: 7 8 9 ⌫ ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(item = KeyItem("7", KeyType.DIGIT) { onDigitClicked("7") }, onHaptic = ::haptic, modifier = Modifier.weight(1f))
            KeypadButton(item = KeyItem("8", KeyType.DIGIT) { onDigitClicked("8") }, onHaptic = ::haptic, modifier = Modifier.weight(1f))
            KeypadButton(item = KeyItem("9", KeyType.DIGIT) { onDigitClicked("9") }, onHaptic = ::haptic, modifier = Modifier.weight(1f))
            BackspaceKey(onClick = { haptic(); onBackspace() }, modifier = Modifier.weight(1f))
        }

        // ── Row 2: 4 5 6 × ──────────────────────────────────────────────────
        KeypadRow(
            col1 = KeyItem("4", KeyType.DIGIT) { onDigitClicked("4") },
            col2 = KeyItem("5", KeyType.DIGIT) { onDigitClicked("5") },
            col3 = KeyItem("6", KeyType.DIGIT) { onDigitClicked("6") },
            col4 = KeyItem("×", KeyType.OPERATOR) { onOperatorClicked("×") },
            onHaptic = ::haptic
        )

        // ── Row 3: 1 2 3 − ──────────────────────────────────────────────────
        KeypadRow(
            col1 = KeyItem("1", KeyType.DIGIT) { onDigitClicked("1") },
            col2 = KeyItem("2", KeyType.DIGIT) { onDigitClicked("2") },
            col3 = KeyItem("3", KeyType.DIGIT) { onDigitClicked("3") },
            col4 = KeyItem("−", KeyType.OPERATOR) { onOperatorClicked("−") },
            onHaptic = ::haptic
        )

        // ── Row 4: 0 . + = ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(item = KeyItem("0", KeyType.DIGIT) { onDigitClicked("0") }, onHaptic = ::haptic, modifier = Modifier.weight(1f))
            KeypadButton(item = KeyItem(".", KeyType.DIGIT) { onDecimalClicked() }, onHaptic = ::haptic, modifier = Modifier.weight(1f))
            KeypadButton(item = KeyItem("+", KeyType.OPERATOR) { onOperatorClicked("+") }, onHaptic = ::haptic, modifier = Modifier.weight(1f))
            KeypadButton(item = KeyItem("=", KeyType.EQUALS) { onEqualsClicked() }, onHaptic = ::hapticConfirm, modifier = Modifier.weight(1f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

private enum class KeyType { DIGIT, OPERATOR, EQUALS }

private data class KeyItem(
    val label: String,
    val type: KeyType,
    val action: () -> Unit
)

@Composable
private fun KeypadRow(
    col1: KeyItem,
    col2: KeyItem,
    col3: KeyItem,
    col4: KeyItem,
    onHaptic: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KeypadButton(item = col1, onHaptic = onHaptic, modifier = Modifier.weight(1f))
        KeypadButton(item = col2, onHaptic = onHaptic, modifier = Modifier.weight(1f))
        KeypadButton(item = col3, onHaptic = onHaptic, modifier = Modifier.weight(1f))
        KeypadButton(item = col4, onHaptic = onHaptic, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KeypadButton(
    item: KeyItem,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (item.type) {
        KeyType.DIGIT    -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        KeyType.OPERATOR -> MaterialTheme.colorScheme.secondaryContainer
        KeyType.EQUALS   -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when (item.type) {
        KeyType.DIGIT    -> MaterialTheme.colorScheme.onSurfaceVariant
        KeyType.OPERATOR -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyType.EQUALS   -> MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onHaptic(); item.action() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.label,
            color = contentColor,
            fontSize = if (item.type == KeyType.DIGIT) 22.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Dedicated backspace key with standard ⌫ icon */
@Composable
private fun BackspaceKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun QuickShortcutButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
