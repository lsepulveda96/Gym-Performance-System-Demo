# Gym System - Feature Roadmap

## 🎯 Phase 1: Foundation (Complete ✅)
- [x] Monorepo structure (Shared, Server, ComposeApp)
- [x] Shared Domain Models & DTOs
- [x] Ktor Backend with JWT & Exposed
- [x] Compose Web Responsive Scaffold
- [x] Dynamic QR Code Logic (20s rotating)
- [x] Docker setup

## 🛠 Phase 2: Core Business Logic
- [ ] **Auth Implementation**: Real BCrypt password hashing and persistent storage.
- [ ] **Payments Integration**: Stripe or MercadoPago integration for subscriptions.
- [ ] **Real Admin CRUD**: Full UI for adding/editing members and plans.
- [ ] **Check-in History**: Persistence of attendance records in PostgreSQL.
- [ ] **Scanner Interface**: Use browser camera in Admin view to scan member QRs.

## ✨ Phase 3: Enhanced Member Experience
- [ ] **Workouts Module**: Digital workout routines for members.
- [ ] **Reservations**: Class booking system with capacity limits.
- [ ] **Push Notifications**: Firebase Cloud Messaging (FCM) integration for billing alerts.
- [ ] **Offline Mode**: Cache profile data for offline access.

## 📈 Phase 4: Analytics & Scaling
- [ ] **Reports Dashboard**: Member growth, churn, and revenue charts.
- [ ] **Multi-gym Support**: SaaS capability for multiple gym locations.
- [ ] **Automated Migrations**: Setup Liquibase or Flyway for DB versioning.
