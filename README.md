# Modern Kotlin Fullstack SaaS

Modern gym management platform built with Kotlin Multiplatform.

## Features

* Admin web dashboard
* Member mobile/web experience
* QR access system
* Responsive UI
* Dark/light theme
* Kotlin fullstack architecture

## Tech Stack

* Kotlin Multiplatform
* Compose Multiplatform Web/WASM
* Ktor
* PostgreSQL
* Docker
* Netlify

Live Demo:
https://gym-performance-ecosystem.pages.dev/


## Architecture

### composeApp

Frontend application:

* Admin dashboard
* Member app
* Responsive UI
* QR scanner

### server

Ktor backend:

* REST APIs
* Authentication
* Membership management
* QR validation

### shared

Shared DTOs and models between frontend/backend.



## Current Features

### Admin

* Member management
* Membership tracking
* QR access validation
* Responsive dashboard

### Member

* QR access code
* Mobile-friendly UI
* Dark/light mode
* Profile access

### Technical

* Kotlin Multiplatform architecture
* Shared models
* Dockerized PostgreSQL
* WASM frontend deployment



## Demo Credentials

Admin:
admin@demo.com
1234

Member:
member@demo.com
1234



## Local Development

### Run Database

docker compose up -d db



### Run Backend

./gradlew :server:run



### Run Frontend

./gradlew :composeApp:jsBrowserDevelopmentRun

