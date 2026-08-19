package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// SocietySarv — Google-Quality Material 3 Color System
// ==========================================

// 1. Primary & Brand Colors (Deep Teal & Mint)
val GoogleTealPrimary = Color(0xFF00695C)        // Google Deep Teal
val GoogleTealDark = Color(0xFF004D40)           // Darker shade
val GoogleTealLight = Color(0xFF00796B)          // Active states & focus
val GoogleTealContainer = Color(0xFFE0F2F1)      // Mint chip badge & active nav pills
val GoogleOnTealContainer = Color(0xFF004D40)    // High-contrast dark teal text

// 2. Surface & Background Colors (Clean Light Canvas)
val GoogleSurfaceBg = Color(0xFFF8FAFC)          // Slate 50 Modern crisp off-white canvas
val GoogleSurfaceCard = Color(0xFFFFFFFF)        // Pure white elevated card
val GoogleSurfaceVariant = Color(0xFFF1F5F9)     // Slate 100 table header & input bg
val GoogleBorderLight = Color(0xFFE2E8F0)        // Slate 200 crisp dividers & strokes
val GoogleBorderFocus = Color(0xFF00796B)        // Focus outline

// 3. High-Contrast Typography Colors
val GoogleTextPrimary = Color(0xFF0F172A)        // Slate 900 High contrast headers
val GoogleTextSecondary = Color(0xFF475569)      // Slate 600 Subtitles & labels
val GoogleTextMuted = Color(0xFF94A3B8)          // Slate 400 Hints & placeholder

// 4. Semantic & Financial Status Colors
val GoogleSuccessGreen = Color(0xFF16A34A)       // Emerald Green (जमा / पावती)
val GoogleSuccessContainer = Color(0xFFDCFCE7)   // Light soft green badge
val GoogleDangerRed = Color(0xFFDC2626)          // Crimson Red (थकबाकी / खर्च)
val GoogleDangerContainer = Color(0xFFFEE2E2)    // Light soft red badge
val GoogleWarningAmber = Color(0xFFD97706)       // Warm Amber (प्रलंबित / दंड)
val GoogleWarningContainer = Color(0xFFFEF3C7)   // Light warm amber badge
val GoogleAccentBlue = Color(0xFF2563EB)         // Royal Blue (माहिती / गेट)
val GoogleAccentBlueContainer = Color(0xFFDBEAFE)// Light blue badge

// Backward Compatibility Aliases for components
val TealPrimary = GoogleTealPrimary
val TealOnPrimary = Color(0xFFFFFFFF)
val TealPrimaryContainer = GoogleTealContainer
val TealOnPrimaryContainer = GoogleOnTealContainer

val MintSecondary = GoogleTealLight
val MintOnSecondary = Color(0xFFFFFFFF)
val MintSecondaryContainer = Color(0xFFB2DFDB)
val MintOnSecondaryContainer = Color(0xFF00363A)

val GoldTertiary = GoogleWarningAmber
val GoldOnTertiary = Color(0xFFFFFFFF)
val GoldTertiaryContainer = GoogleWarningContainer
val GoldOnTertiaryContainer = Color(0xFF78350F)

val SlateBackground = GoogleSurfaceBg
val SlateOnBackground = GoogleTextPrimary
val SlateSurface = GoogleSurfaceCard
val SlateOnSurface = GoogleTextPrimary
val SlateSurfaceVariant = GoogleSurfaceVariant
val SlateOnSurfaceVariant = GoogleTextSecondary
val SlateOutline = Color(0xFFCBD5E1)
val SlateOutlineVariant = GoogleBorderLight

// Semantic Status Colors
val StatusPaid = GoogleSuccessGreen
val StatusPaidBg = GoogleSuccessContainer
val StatusPending = GoogleWarningAmber
val StatusPendingBg = GoogleWarningContainer
val StatusOverdue = GoogleDangerRed
val StatusOverdueBg = GoogleDangerContainer
val StatusInfo = GoogleAccentBlue
val StatusInfoBg = GoogleAccentBlueContainer

// Dark Palette
val DarkTealPrimary = Color(0xFF80CBC4)
val DarkTealOnPrimary = Color(0xFF003731)
val DarkTealPrimaryContainer = Color(0xFF004D40)
val DarkTealOnPrimaryContainer = Color(0xFFB2DFDB)

val DarkSlateBackground = Color(0xFF0F172A)
val DarkSlateOnBackground = Color(0xFFF8FAFC)
val DarkSlateSurface = Color(0xFF1E293B)
val DarkSlateOnSurface = Color(0xFFF8FAFC)
val DarkSlateSurfaceVariant = Color(0xFF334155)
val DarkSlateOnSurfaceVariant = Color(0xFF94A3B8)
