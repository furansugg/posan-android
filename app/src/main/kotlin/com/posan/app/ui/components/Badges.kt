package com.posan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.domain.model.TransactionStatus
import com.posan.app.domain.model.UserRole
import com.posan.app.ui.theme.CatBlue
import com.posan.app.ui.theme.CatGray
import com.posan.app.ui.theme.CatGreen
import com.posan.app.ui.theme.CatOrange
import com.posan.app.ui.theme.CatPurple
import com.posan.app.ui.theme.CatRed

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .border(width = 1.dp, color = color.copy(alpha = 0.35f), shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TransactionStatusBadge(status: String, modifier: Modifier = Modifier) {
    val s = TransactionStatus.fromName(status)
    val color = when (s) {
        TransactionStatus.PAID -> CatGreen
        TransactionStatus.VOID -> CatRed
    }
    StatusPill(label = s.displayName, color = color, modifier = modifier)
}

@Composable
fun PaymentMethodBadge(method: String, modifier: Modifier = Modifier) {
    val m = PaymentMethod.fromName(method)
    val color = when (m) {
        PaymentMethod.CASH -> CatGreen
        PaymentMethod.QRIS -> CatPurple
        PaymentMethod.CARD -> CatOrange
    }
    StatusPill(label = m.displayName, color = color, modifier = modifier)
}

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val r = UserRole.fromName(role)
    val color = when (r) {
        UserRole.ADMIN -> CatBlue
        UserRole.KASIR -> CatGray
    }
    StatusPill(label = r.displayName, color = color, modifier = modifier)
}
