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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
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
            val hasAnyCoupon = state.activeCoupons.isNotEmpty() || state.usedCoupons.isNotEmpty()

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
                    CouponTopAppBar(onClearAllUsed = { viewModel.clearAllUsed() })
                    CouponFilterChips(
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) },
                        selectedRewardTypes = state.selectedRewardTypes,
                        onRewardTypeToggled = { viewModel.toggleRewardType(it) },
                        selectedSortOrder = state.selectedSortOrder,
                        onSortOrderSelected = { viewModel.setSortOrder(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val isFilteredEmpty = when (state.selectedFilter) {
                        CouponFilter.ALL -> state.activeCoupons.isEmpty() && state.usedCoupons.isEmpty()
                        CouponFilter.AVAILABLE -> state.activeCoupons.isEmpty()
                        CouponFilter.USED -> state.usedCoupons.isEmpty()
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
private fun CouponTopAppBar(onClearAllUsed: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("에르피스 도우미") },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "더보기")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("사용 기록 전체 초기화") },
                    onClick = {
                        menuExpanded = false
                        onClearAllUsed()
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponFilterChips(
    selectedFilter: CouponFilter,
    onFilterSelected: (CouponFilter) -> Unit,
    selectedRewardTypes: Set<RewardType>,
    onRewardTypeToggled: (RewardType?) -> Unit,
    selectedSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusFilters = listOf(
        CouponFilter.ALL to "전체",
        CouponFilter.AVAILABLE to "사용 가능",
        CouponFilter.USED to "사용 완료",
    )
    Column(modifier = modifier) {
        Text(
            text = "정렬",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOrder.entries.forEach { order ->
                val selected = selectedSortOrder == order
                FilterChip(
                    selected = selected,
                    onClick = { onSortOrderSelected(order) },
                    label = { Text(order.displayName) },
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "상태",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusFilters.forEach { (filter, label) ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(label) },
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "종류",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allSelected = selectedRewardTypes.isEmpty()
            FilterChip(
                selected = allSelected,
                onClick = { onRewardTypeToggled(null) },
                label = { Text("전체") },
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = allSelected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    selectedBorderWidth = 1.5.dp
                )
            )
            RewardType.entries.forEach { type ->
                val selected = type in selectedRewardTypes
                FilterChip(
                    selected = selected,
                    onClick = { onRewardTypeToggled(type) },
                    label = { Text(type.displayName) },
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
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
    val strikethrough = if (isAnyCodeUsed) TextDecoration.LineThrough else TextDecoration.None
    val contentColor = if (isAnyCodeUsed) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isAnyCodeUsed) 0.5f else 1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAnyCodeUsed) 0.dp else 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                if (coupon.isNew) {
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
                    val effectiveCopyAction = if (isAnyCodeUsed || isUsed) null else {
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
}

@Composable
private fun CouponCodeChip(
    code: String,
    isUsed: Boolean,
    onCopy: (() -> Unit)?,
    onToggleUsage: (() -> Unit)?
) {
    val isActive = onCopy != null && !isUsed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isActive) Modifier.clickable(onClick = onCopy!!) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
        shadowElevation = if (isActive) 4.dp else 0.dp,
        border = if (isActive)
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (onToggleUsage != null) {
                IconButton(
                    onClick = onToggleUsage,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isUsed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (isUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("coupon_code", text))
}
