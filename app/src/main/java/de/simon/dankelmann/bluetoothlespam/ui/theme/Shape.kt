package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Sharp, boxy corners for the terminal/CRT identity — panels read like console windows, not
// rounded Material cards. Kept just shy of 0dp on the larger roles so nested borders don't alias.
val SpecterShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(4.dp),
)
