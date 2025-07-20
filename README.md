# FOCX - Decentralized E-commerce Platform

A cutting-edge Android application for a decentralized e-commerce platform built on Solana blockchain, featuring Jetpack Compose UI and Clean Architecture.

## 🌟 Key Features

### Core E-commerce Functions
- **Product Management**: Create, update, and manage products with atomic operations
- **Merchant Registration**: Atomic merchant registration with Solana blockchain integration
- **Order Processing**: Complete order lifecycle management with escrow support
- **Payment System**: Multi-token payment support with USDC and custom tokens
- **Search & Discovery**: Advanced product search with keyword and price indexing

### Blockchain Integration
- **Solana Mobile Wallet Adapter**: Seamless wallet connectivity and transaction signing
- **Atomic Operations**: Single-transaction merchant registration and product creation
- **Program Derived Addresses (PDAs)**: Secure account management
- **Borsh Serialization**: Efficient data serialization for blockchain interactions
- **Real-time Data**: Live blockchain data synchronization

### Modern UI/UX
- **Dark Tech Theme**: Futuristic design with neon accents and sci-fi aesthetics
- **Material Design 3**: Modern UI components with custom tech styling
- **Responsive Layout**: Adaptive design for various screen sizes
- **Smooth Animations**: Lottie animations and custom transitions

## 🏗️ Tech Stack

### Frontend
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture + MVI Pattern
- **Dependency Injection**: Hilt
- **Navigation**: Compose Navigation
- **State Management**: ViewModel + StateFlow + SharedFlow
- **Image Loading**: Coil Compose
- **Animations**: Lottie Compose

### Blockchain & Backend
- **Blockchain**: Solana (Program ID: `H2ijJPLXRpj2Vw9mSPUSDU7tFZfqVSWkA5xZEkxdfin7`)
- **Wallet Integration**: Solana Mobile Wallet Adapter
- **Serialization**: Kotlinx Serialization + Borsh
- **Network**: Ktor Client + Retrofit
- **RPC**: Solana RPC with multiple drivers (Ktor, OkHttp)

### Data & Storage
- **Local Database**: Room
- **Preferences**: DataStore
- **Cryptography**: Bouncy Castle Provider

## 📁 Project Structure

```
app/src/main/java/com/focx/
├── core/
│   ├── constants/      # App constants and configuration
│   └── network/        # Network configuration
├── data/
│   ├── datasource/
│   │   ├── mock/       # Mock data sources for development
│   │   └── solana/     # Solana blockchain data sources
│   └── repository/     # Repository implementations
├── domain/
│   ├── entity/         # Domain entities (MerchantRegistration, Product, Order)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic use cases
├── presentation/
│   ├── intent/         # User intents (MVI)
│   ├── state/          # UI states
│   ├── ui/
│   │   ├── components/ # Reusable UI components
│   │   ├── screens/    # Screen composables
│   │   └── theme/      # Dark tech theme and styling
│   └── viewmodel/      # ViewModels
├── di/                 # Dependency injection modules
└── utils/              # Utility classes (Solana, Serialization)
```

### Key Components

- **SolanaMerchantDataSource**: Handles merchant registration and blockchain interactions
- **MerchantRegistrationSerialization**: Borsh serialization for blockchain data
- **RegisterMerchantUseCase**: Business logic for merchant registration
- **SolanaTokenUtils**: Utility functions for Solana operations
- **MobileWalletAdapter**: Wallet connectivity and transaction signing

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+
- Solana Mobile Wallet (Phantom, Solflare, etc.) for testing
- Java 11+

### Configuration
- **Program ID**: `H2ijJPLXRpj2Vw9mSPUSDU7tFZfqVSWkA5xZEkxdfin7`
- **Network**: Solana Devnet (configurable)
- **Wallet**: Requires Solana Mobile Wallet for blockchain interactions

## 🏛️ Architecture Overview

### Clean Architecture + MVI
The app follows Clean Architecture principles with MVI (Model-View-Intent) pattern for unidirectional data flow.

### Domain Layer
- **Entities**: Core business models (MerchantRegistration, Product, Order)
- **Use Cases**: Business logic operations (RegisterMerchantUseCase)
- **Repository Interfaces**: Data access contracts (IMerchantRepository)

### Data Layer
- **Solana Data Sources**: Blockchain interaction layer (SolanaMerchantDataSource)
- **Mock Data Sources**: Development and testing data
- **Repository Implementations**: Data access implementations
- **Serialization**: Borsh and Kotlinx serialization for blockchain data

### Presentation Layer
- **MVI Pattern**: Intent → ViewModel → State → Composable
- **Jetpack Compose**: Modern declarative UI with dark tech theme
- **State Management**: StateFlow and SharedFlow for reactive programming

## 🎨 UI Components & Features

### Core Screens
- **Buy/Index**: Product browsing with search and filtering
- **Sell**: Merchant dashboard with product and order management
- **Earn**: Insurance fund pool and staking features
- **Governance**: Proposal management and voting system
- **Profile**: Wallet management and user settings

### Custom Components
- **TechButton**: Futuristic buttons with neon glow effects
- **TechCard**: Product and content cards with sci-fi styling
- **TechBadge**: Status indicators with holographic effects
- **TechTextField**: Input fields with cyberpunk aesthetics
- **LoadingStates**: Matrix-style loading animations

### Blockchain Features
- **Wallet Connection**: Seamless Solana Mobile Wallet integration
- **Transaction Signing**: Secure transaction approval flow
- **Real-time Updates**: Live blockchain data synchronization
- **Atomic Operations**: Single-transaction complex operations

## 🛠️ Development

### Data Sources
The app uses a flexible data source architecture:
- **Mock Data Sources**: For rapid UI development and testing
- **Solana Data Sources**: For blockchain interactions and real data
- **Configurable**: Easy switching between mock and live data

### Key Development Features
- **Clean Architecture**: Separation of concerns with clear boundaries
- **Dependency Injection**: Hilt for modular and testable code
- **Reactive Programming**: Coroutines and Flow for async operations
- **Type Safety**: Kotlin's type system with sealed classes and data classes

### Blockchain Integration
- **Solana Program**: Custom smart contract for e-commerce operations
- **Atomic Instructions**: Efficient single-transaction operations
- **PDA Management**: Secure account derivation and management
- **Borsh Serialization**: Efficient data encoding for blockchain
