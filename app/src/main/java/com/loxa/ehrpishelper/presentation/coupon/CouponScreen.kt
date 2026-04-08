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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
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
                    || state.expiredCoupons.isNotEmpty()
                    || state.usedCoupons.isNotEmpty()

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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // 유효 미사용
                    items(state.activeCoupons, key = { it.feedId }) { coupon ->
                        // 뷰모델에서 실시간으로 변하는 usedCodes를 기준으로
                        // 이 카드 안의 코드들이 하나라도 체크되었는지 확인
                        val isAnyCheckedInSession = coupon.codes.any { it in state.usedCodes }

                        CouponCard(
                            coupon = coupon,
                            allUsed = isAnyCheckedInSession,
                            usedCodes = state.usedCodes,
                            onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                            onCopy = if (isAnyCheckedInSession) null else copyAction
                        )
                    }

                    // 만료 미사용
                    if (state.expiredCoupons.isNotEmpty()) {
                        item { SectionDivider("만료된 쿠폰") }
                        items(state.expiredCoupons, key = { it.feedId }) { coupon ->
                            CouponCard(
                                coupon = coupon,
                                allUsed = false,
                                usedCodes = state.usedCodes,
                                onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                                onCopy = null
                            )
                        }
                    }

                    // 사용 완료 (유효+만료 모두)
                    if (state.usedCoupons.isNotEmpty()) {
                        item { SectionDivider("사용한 쿠폰") }
                        items(state.usedCoupons, key = { it.feedId }) { coupon ->
                            CouponCard(
                                coupon = coupon,
                                allUsed = true,
                                usedCodes = state.usedCodes,
                                onToggleUsage = { code, isUsed -> viewModel.toggleUsage(code, isUsed) },
                                onCopy = null
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
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
    allUsed: Boolean, // 이 값은 초기 로딩 시 '사용한 쿠폰' 섹션에 있는지 여부
    usedCodes: Set<String>,
    onToggleUsage: (code: String, isCurrentlyUsed: Boolean) -> Unit,
    onCopy: ((String) -> Unit)?
) {
    // 1. 현재 이 카드의 모든 코드가 사용되었는지 실시간 확인 (UI 효과용)
    val isAnyCodeUsedInThisSession = coupon.codes.any { it in usedCodes }

    // 2. 카드 전체가 '흐려져야' 하는 조건: 만료되었거나 + (원래 사용됨 섹션이거나 OR 방금 체크했거나)
    val isVisualDimmed = coupon.isExpired || allUsed || isAnyCodeUsedInThisSession

    // 3. 제목에 '취소선'을 그어야 하는 조건: 원래 사용됨 섹션이거나 OR 방금 체크했거나
    val isVisualStrikethrough = allUsed || isAnyCodeUsedInThisSession

    val strikethrough = if (isVisualStrikethrough) TextDecoration.LineThrough else TextDecoration.None
    val contentColor = if (isVisualDimmed) Color.Gray else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isVisualDimmed) 0.5f else 1f) // 1 & 3번 요구사항: 카드 전체 흐려짐
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
                textDecoration = strikethrough, // 3번 요구사항: 제목 취소선
                modifier = Modifier.weight(1f)
            )
            if (coupon.isExpired) {
                Text(text = "만료", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "~${coupon.expiryEnd}",
            fontSize = 12.sp,
            color = contentColor.copy(alpha = 0.7f),
            textDecoration = strikethrough
        )

        if (coupon.codes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            coupon.codes.forEach { code ->
                val isUsed = code in usedCodes

                // 체크된 경우 null을 전달하여 복사를 막고,
                // 그렇지 않은 경우 onCopy 함수에 현재 code를 미리 바인딩해서 전달
                val effectiveCopyAction = if (isVisualStrikethrough || isUsed) null else {
                    // onCopy가 null이 아닐 때만 람다를 생성하여 전달
                    onCopy?.let { copyFunc -> { copyFunc(code) } }
                }

                CouponCodeChip(
                    code = code,
                    isUsed = isUsed,
                    onCopy = effectiveCopyAction, // 이제 () -> Unit 타입으로 일치함
                    onToggleUsage = { onToggleUsage(code, isUsed) }
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
    onToggleUsage: () -> Unit
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

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("coupon_code", text))
}
