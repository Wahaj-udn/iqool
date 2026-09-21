package com.example.geminiapi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.geminiapi.ui.theme.Charcoal
import com.example.geminiapi.ui.theme.Lime
import java.util.Locale

/** A soft pastel version of [accent], laid over the current surface color. */
@Composable
fun tintOver(accent: Color, alpha: Float = 0.22f): Color = 
    accent.copy(alpha = alpha).compositeOver(MaterialTheme.colorScheme.surface)

/** Secondary text color that adapts to whatever card it sits on. */
@Composable
fun mutedContent(alpha: Float = 0.68f): Color = 
    LocalContentColor.current.copy(alpha = alpha)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Dark charcoal card – used for the most important content on a screen. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        contentPadding = contentPadding,
        content = content
    )
}

/** Lime card – used for calls to action and AI features. */
@Composable
fun LimeCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(
        modifier = modifier,
        containerColor = Lime,
        contentColor = Charcoal,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(text = title, modifier = modifier, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun ScreenSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(title)
        content()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleImageRes: Int? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (titleImageRes != null) {
                Image(
                    painter = painterResource(id = titleImageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .height(54.dp) // Professional font height (Increased by 50%)
                        .wrapContentWidth(), // Preserve original aspect ratio
                    alignment = Alignment.CenterStart
                )
            } else {
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun IconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    background: Color = Lime,
    tint: Color = Charcoal,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun Avatar(modifier: Modifier = Modifier, diameter: Dp = 48.dp) {
    Box(
        modifier = modifier.size(diameter).clip(CircleShape).background(Lime),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.Person,
            contentDescription = "Profile",
            tint = Charcoal,
            modifier = Modifier.size(diameter * 0.55f)
        )
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = contentColor, maxLines = 1)
    }
}

/** "68 bpm" – a big number with a small unit next to it. */
@Composable
fun ValueLine(value: String, unit: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = mutedContent(),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
    }
}

@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(LocalContentColor.current.copy(alpha = 0.08f)))
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showChevron: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconChip(
                icon = icon,
                background = LocalContentColor.current.copy(alpha = 0.08f),
                tint = LocalContentColor.current,
                size = 36.dp,
                iconSize = 18.dp,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = mutedContent())
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "›", style = MaterialTheme.typography.titleLarge, color = mutedContent())
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = 172.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(24.dp),
        color = tintOver(accent),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = icon,
                    background = accent,
                    tint = Charcoal,
                    size = 30.dp,
                    iconSize = 16.dp,
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

enum class AppButtonStyle {
    Primary, // Charcoal (lime in dark mode)
    Dark,    // Always charcoal
    Lime,    // Always lime
    Outline  // Outlined
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val buttonModifier = modifier.heightIn(min = 52.dp)
    val label: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
    }

    when (style) {
        AppButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            content = label
        )
        AppButtonStyle.Dark -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Charcoal, contentColor = Color.White),
            content = label
        )
        AppButtonStyle.Lime -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Charcoal),
            content = label
        )
        AppButtonStyle.Outline -> {
            val contentColor = LocalContentColor.current
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
                border = BorderStroke(1.5.dp, contentColor.copy(alpha = 0.25f)),
                content = label
            )
        }
    }
}

@Composable
fun AssessmentTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String? = null,
    isDecimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Lime,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = Lime,
            cursorColor = Lime
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun AssessmentChoice(
    label: String,
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = mutedContent())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val selected = option.lowercase() == current.lowercase()
                Surface(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) Lime else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) Charcoal else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = option.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentDropdown(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.uppercase(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = Lime
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.uppercase()) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
fun AssessmentSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, step: Float = 1f, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(if (step >= 1f) value.toInt().toString() else String.format(Locale.US, "%.1f", value), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = if (step >= 1f) (range.endInclusive - range.start).toInt() - 1 else 0,
            colors = SliderDefaults.colors(
                thumbColor = Lime,
                activeTrackColor = Lime,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun AssessmentSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Lime, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}
