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
 * Calculator keypad — 5 rows:
 *
 *  Row 0 (shortcuts): [100g] [250g] [500g] [750g]  (or equivalent)
 *  Row 1: [7] [8] [9] [÷]
 *  Row 2: [4] [5] [6] [×]
 *  Row 3: [1] [2] [3] [−]
 *  Row 4: [⌫] [0] [.] [=]   ← ⌫ is here, NOT near Save Bill
 *  Row 5: [+] (full-width, taller — easy to hit for addition)
 *
 * The [+] is placed last and full-width so it is impossible to confuse
 * with the Save Bill button which lives in its own strip BELOW the keypad
 * with a gap between them.
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
    fun hapticConfirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // ── Quick shortcuts row ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickShortcuts.forEach { shortcut ->
                QuickShortcutButton(
                    text = shortcut,
                    onClick = { haptic(); onShortcutClicked(shortcut) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Row 1: 7 8 9 ÷ ──────────────────────────────────────────────────
        KeypadRow(
            col1 = KeyItem("7",  KeyType.DIGIT)    { onDigitClicked("7") },
            col2 = KeyItem("8",  KeyType.DIGIT)    { onDigitClicked("8") },
            col3 = KeyItem("9",  KeyType.DIGIT)    { onDigitClicked("9") },
            col4 = KeyItem("÷",  KeyType.OPERATOR) { onOperatorClicked("÷") },
            onHaptic = ::haptic
        )

        // ── Row 2: 4 5 6 × ──────────────────────────────────────────────────
        KeypadRow(
            col1 = KeyItem("4",  KeyType.DIGIT)    { onDigitClicked("4") },
            col2 = KeyItem("5",  KeyType.DIGIT)    { onDigitClicked("5") },
            col3 = KeyItem("6",  KeyType.DIGIT)    { onDigitClicked("6") },
            col4 = KeyItem("×",  KeyType.OPERATOR) { onOperatorClicked("×") },
            onHaptic = ::haptic
        )

        // ── Row 3: 1 2 3 − ──────────────────────────────────────────────────
        KeypadRow(
            col1 = KeyItem("1",  KeyType.DIGIT)    { onDigitClicked("1") },
            col2 = KeyItem("2",  KeyType.DIGIT)    { onDigitClicked("2") },
            col3 = KeyItem("3",  KeyType.DIGIT)    { onDigitClicked("3") },
            col4 = KeyItem("−",  KeyType.OPERATOR) { onOperatorClicked("−") },
            onHaptic = ::haptic
        )

        // ── Row 4: ⌫  0  .  = ───────────────────────────────────────────────
        // ⌫ is a KEYPAD key — thumb naturally reaches here, NOT the Save button
        // = is in this row, far from Save Bill (which is below the keypad)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ⌫ backspace key
            BackspaceKey(
                onClick = { haptic(); onBackspace() },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                item = KeyItem("0", KeyType.DIGIT) { onDigitClicked("0") },
                onHaptic = ::haptic,
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                item = KeyItem(".", KeyType.DIGIT) { onDecimalClicked() },
                onHaptic = ::haptic,
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                item = KeyItem("=", KeyType.EQUALS) { onEqualsClicked() },
                onHaptic = ::hapticConfirm,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Row 5: [+] full-width ────────────────────────────────────────────
        // Large comfortable target for the most-used operator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { haptic(); onOperatorClicked("+") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
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
            .height(52.dp)
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

/** Dedicated backspace key with the standard ⌫ icon */
@Composable
private fun BackspaceKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(52.dp)
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
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
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
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
