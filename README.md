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


## Images
<img width="562" height="1165" alt="image" src="https://github.com/user-attachments/assets/db41badd-1248-49ce-86a1-f8476fb02804" />

## 

<img width="562" height="1165" alt="image" src="https://github.com/user-attachments/assets/22aca06a-69c6-405f-814b-4d277c39c396" />

## 

<img width="561" height="1167" alt="image" src="https://github.com/user-attachments/assets/f4fe253c-29f6-4507-a9c4-078ed22bc4a8" />

## 

<img width="560" height="1166" alt="image" src="https://github.com/user-attachments/assets/dd1c05b2-595c-44f5-8dde-9ea05ec29eaa" />

## 

<img width="562" height="1168" alt="image" src="https://github.com/user-attachments/assets/e53a1e62-4aad-4f57-a221-1cd52af53128" />

## 

<img width="560" height="1166" alt="image" src="https://github.com/user-attachments/assets/12c169d5-4163-4468-8b52-fad82b08c985" />

## 

<img width="561" height="1171" alt="image" src="https://github.com/user-attachments/assets/7c85062b-66bb-42a2-9560-a0a310053b14" />

## 

<img width="1908" height="1259" alt="image" src="https://github.com/user-attachments/assets/5840e48d-da2e-4f4c-9c41-be65a96adba2" />

## 

<img width="1916" height="1265" alt="image" src="https://github.com/user-attachments/assets/7b648650-e6a6-4126-9952-3056fef7ff50" />

## 

<img width="1896" height="1264" alt="image" src="https://github.com/user-attachments/assets/f6d64e9c-a5e9-4640-b3c6-7275b61743bf" />

## 

<img width="1899" height="1245" alt="image" src="https://github.com/user-attachments/assets/9bd59f44-e5b8-4924-b7f5-86d373c77749" />

## 

<img width="1901" height="1218" alt="image" src="https://github.com/user-attachments/assets/48ca0c48-5321-4652-a138-98f18698e39a" />

## 

<img width="1898" height="1201" alt="image" src="https://github.com/user-attachments/assets/e03dad83-1a6d-4965-b50c-e2096acb2ea2" />

## 

<img width="1902" height="1220" alt="image" src="https://github.com/user-attachments/assets/08d06f4d-0906-4ade-9da5-356e48a46be3" />

## 

<img width="1906" height="1229" alt="image" src="https://github.com/user-attachments/assets/02142f6e-7741-47cd-b3e3-36643ae78311" />


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

