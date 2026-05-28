# Kinetic Performance Ecosystem - AI Context

## Project Overview

Kinetic is a Kotlin Multiplatform fullstack gym management platform.

The project contains:

* Admin web dashboard
* Member mobile/web app
* QR access validation
* Membership management system

Main goal:
portfolio-quality SaaS demo deployable publicly.

---

# Tech Stack

## Frontend

* Kotlin Multiplatform
* Compose Multiplatform Web/WASM
* Responsive layouts
* Dark/light theme

## Backend

* Ktor REST API
* PostgreSQL
* Docker

## Hosting

* Netlify frontend deployment
* Railway planned for backend

---

# Architecture

## Modules

### composeApp

Frontend UI application.

Contains:

* admin dashboard
* member mobile UI
* responsive layouts
* QR scanner

### server

Ktor backend.

Contains:

* REST APIs
* authentication
* QR validation
* member management

### shared

Shared DTOs/models between frontend/backend.

---

# Current State

Implemented:

* responsive admin UI
* member mobile UI
* QR generation
* QR scanner
* demo deploy on Netlify
* dark/light theme
* mock/demo auth

Still pending:

* production auth
* real backend deployment
* payment integration

---

# Important Conventions

* Prefer small focused composables
* Avoid giant Screens.kt files
* Keep admin and member flows separated
* Use responsive layouts
* Avoid fixed heights in web layouts
* Use defensive localStorage handling
* Prefer scrollable layouts over clipped layouts

---

# AI Assistant Instructions

When modifying code:

* preserve responsive behavior
* avoid breaking WASM compatibility
* avoid introducing fixed-size layouts
* keep code understandable for a solo developer
* prioritize stability over overengineering

Do NOT:

* introduce unnecessary enterprise complexity
* create excessive abstraction
* split files excessively
* rewrite working code unnecessarily
