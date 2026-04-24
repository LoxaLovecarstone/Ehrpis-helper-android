package com.loxa.ehrpishelper.presentation.coupon

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loxa.ehrpishelper.domain.model.Coupon

@Composable
fun CouponScreen(
    viewModel: CouponViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 거부해도 쿠폰 목록은 정상 동작 */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    when (val state = uiState) {
        is CouponUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is CouponUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is CouponUiState.Success -> {
            val hasAnyCoupon = state.activeCoupons.isNotEmpty()
                    || state.usedCoupons.isNotEmpty()
                    || state.expiredCoupons.isNotEmpty()

            if (!hasAnyCoupon) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("현재 유효한 쿠폰이 없습니다.")
                }
            } else {
                val copyAction = { code: String ->
                    copyToClipboard(context, code)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, "'$code' 복사됨", Toast.LENGTH_SHORT).show()
                    }
                }

                Column(Modifier.fillMaxSize()) {
                    CouponFilterChips(
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = viewModel::setFilter,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val isFilteredEmpty = when (state.selectedFilter) {
                        CouponFilter.ALL -> state.activeCoupons.isEmpty() && state.usedCoupons.isEmpty()
                        CouponFilter.AVAILABLE -> state.activeCoupons.isEmpty()
                        CouponFilter.USED -> state.usedCoupons.isEmpty()
                        CouponFilter.EXPIRED -> state.expiredCoupons.isEmpty()
                    }

                    if (isFilteredEmpty) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("해당하는 쿠폰이 없습니다.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { Spacer(Modifier.height(4.dp)) }

                            when (state.selectedFilter) {
                                CouponFilter.ALL -> {
                                    items(state.activeCoupons, key = { it.feedId }) { coupon ->
                                        CouponCard(
                                            coupon = coupon,
                                            usedCodes = state.usedCodes,
                                            onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                                            onCopy = copyAction
                                        )
                                    }
                                    if (state.usedCoupons.isNotEmpty()) {
                                        item { SectionDivider("사용한 쿠폰") }
                                        items(state.usedCoupons, key = { it.feedId }) { coupon ->
                                            CouponCard(
                                                coupon = coupon,
                                                usedCodes = state.usedCodes,
                                                onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                                                onCopy = copyAction
                                            )
                                        }
                                    }
                                }

                                CouponFilter.AVAILABLE -> {
                                    items(state.activeCoupons, key = { it.feedId }) { coupon ->
                                        CouponCard(
                                            coupon = coupon,
                                            usedCodes = state.usedCodes,
                                            onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                                            onCopy = copyAction
                                        )
                                    }
                                }

                                CouponFilter.USED -> {
                                    items(state.usedCoupons, key = { it.feedId }) { coupon ->
                                        CouponCard(
                                            coupon = coupon,
                                            usedCodes = state.usedCodes,
                                            onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                                            onCopy = copyAction
                                        )
                                    }
                                }

                                CouponFilter.EXPIRED -> {
                                    items(state.expiredCoupons, key = { it.feedId }) { coupon ->
                                        CouponCard(
                                            coupon = coupon,
                                            usedCodes = state.usedCodes,
                                            onToggleUsage = null,
                                            onCopy = null
                                        )
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponFilterChips(
    selectedFilter: CouponFilter,
    onFilterSelected: (CouponFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        CouponFilter.ALL to "전체",
        CouponFilter.AVAILABLE to "사용 가능",
        CouponFilter.USED to "사용완료",
        CouponFilter.EXPIRED to "만료",
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun CouponCard(
    coupon: Coupon,
    usedCodes: Set<String>,
    onToggleUsage: ((code: String, isCurrentlyUsed: Boolean) -> Unit)?,
    onCopy: ((String) -> Unit)?
) {
    val isAnyCodeUsed = coupon.codes.any { it in usedCodes }
    val isVisualDimmed = coupon.isExpired || isAnyCodeUsed
    val isVisualStrikethrough = coupon.isExpired || isAnyCodeUsed

    val strikethrough = if (isVisualStrikethrough) TextDecoration.LineThrough else TextDecoration.None
    val contentColor = if (isVisualDimmed) Color.Gray else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isVisualDimmed) 0.5f else 1f)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = coupon.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = contentColor,
                textDecoration = strikethrough,
                modifier = Modifier.weight(1f)
            )
            if (coupon.isNew && !coupon.isExpired) {
                Text(
                    text = "NEW",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (coupon.isExpired) {
                Text(text = "만료", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (coupon.expiryEnd.isEmpty()) "만료일 알 수 없음" else "~${coupon.expiryEnd}",
            fontSize = 12.sp,
            color = contentColor.copy(alpha = 0.7f),
            textDecoration = strikethrough
        )

        if (coupon.codes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            coupon.codes.forEach { code ->
                val isUsed = code in usedCodes
                val effectiveCopyAction = if (isVisualStrikethrough || isUsed) null else {
                    onCopy?.let { copyFunc -> { copyFunc(code) } }
                }
                CouponCodeChip(
                    code = code,
                    isUsed = isUsed,
                    onCopy = effectiveCopyAction,
                    onToggleUsage = onToggleUsage?.let { toggle -> { toggle(code, isUsed) } }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CouponCodeChip(
    code: String,
    isUsed: Boolean,
    onCopy: (() -> Unit)?,
    onToggleUsage: (() -> Unit)?
) {
    val isActive = onCopy != null && !isUsed

    val background = if (isUsed)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else
        Color.Transparent

    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
    val textColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = background, shape = RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .then(
                if (onCopy != null) Modifier.clickable(onClick = onCopy)
                else Modifier
            )
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = textColor,
            textDecoration = TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        if (onToggleUsage != null) {
            IconButton(
                onClick = onToggleUsage,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isUsed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (isUsed) "사용 완료" else "사용 전",
                    tint = if (isUsed) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("coupon_code", text))
}
