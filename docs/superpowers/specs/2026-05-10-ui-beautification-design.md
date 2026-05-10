# UI Beautification Design

**Date**: 2026-05-10  
**Status**: Approved → Implementation  
**Scope**: Theme system overhaul + key component visual enhancement, no business logic changes

## 1. Color System

### Light Theme
| Role | Value | Notes |
|------|-------|-------|
| primary | `#1a237e` | Unchanged |
| onPrimary | `#ffffff` | Unchanged |
| primaryContainer | `#d1d4ff` | New |
| onPrimaryContainer | `#00006e` | New |
| secondary | `#3949ab` | Changed from `#2196f3` (more mature) |
| secondaryContainer | `#dbe1ff` | New |
| tertiary | `#ff9800` | Unchanged |
| surface | `#fafafa` | From `#ffffff` (softer) |
| surfaceVariant | `#e7e0eb` | New |
| outline | `#7a757f` | New |
| background | `#fafafa` | Same as surface |

### Dark Theme (full redesign)
| Role | Value | Notes |
|------|-------|-------|
| primary | `#c1c6ff` | Lightened from `#1a237e` |
| onPrimary | `#00006e` | Dark text on light surface |
| primaryContainer | `#192292` | Deep blue container |
| secondary | `#b7c4ff` | Lightened |
| tertiary | `#ffb74d` | Softened |
| surface | `#121212` | Unchanged |
| surfaceVariant | `#47464f` | Card containers |
| background | `#121212` | Unchanged |

## 2. Shape System
| Level | Radius | Usage |
|-------|--------|-------|
| extraSmall | 4dp | Chips, small labels |
| small | 8dp | Buttons, snackbar |
| medium | 12dp | Cards, dialogs, sheets |
| large | 16dp | Large cards, modal |
| extraLarge | 24dp | Fullscreen dialogs |

## 3. Typography
Full 13-level Material 3 scale with adjusted sizes and weights for readability.

## 4. Component Enhancements

### ProjectCard
- Cover + info side-by-side layout
- Genre tag as AssistChip
- Date text demoted to bodySmall
- Press animation (scale)

### SettingsComponents
- Row-level dividers between items
- Chevron arrow on clickable items
- Themed switch color

### TopAppBar
- List pages: primary background (unchanged)
- Form pages: surface background + primary text + bottom divider
- Detail/sub pages: surfaceVariant background

### NavigationBar
- Indicator color on selected item
- Top divider line

### Page Transitions
- fadeIn/fadeOut on NavHost composable routes
