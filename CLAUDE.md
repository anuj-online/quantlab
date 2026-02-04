# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QuantLab is a quantitative trading research platform with a React/TypeScript frontend and Spring Boot (Java 17) backend.

## Development Commands

### Frontend (quantlab-frontend/)
```bash
npm install              # Install dependencies
npm run dev             # Start development server (Vite, port 3000)
npm run build           # Build for production
npm run preview         # Preview production build
```

**Environment**: Set `GEMINI_API_KEY` in `quantlab-frontend/.env.local`

### Backend (quantlab-api/)
```bash
mvn spring-boot:run     # Run the Spring Boot application (port 8080)
mvn clean package       # Build JAR
mvn test                # Run tests
```

**Requirements**: Java 17, Maven, PostgreSQL (with Flyway migrations enabled)

## Architecture

### Monorepo Structure
```
quantlab/
├── quantlab-frontend/   # React + TypeScript + Vite
└── quantlab-api/        # Spring Boot 3.5.10 + Java 17
```

### Frontend Architecture
- **Routing**: HashRouter (routes defined in `App.tsx`)
- **State**: localStorage for persisting the active `strategyRunId` across sessions
- **API Layer**: `services/apiService.ts` currently returns mock data with intentional delays to simulate backend calls

**Key Pages:**
- `Dashboard` - Analytics summary, equity curve, performance metrics
- `StrategyConfig` - Configure and execute trading strategies
- `Signals` - Display BUY/SELL signals with entry/stop/target prices
- `PaperTrades` - Paper trading results with P&L calculations

**Market Support**: Both INDIA (INFY, RELIANCE, TCS) and US (AAPL, TSLA, MSFT) markets

### Backend Architecture
- **Framework**: Spring Boot 3.5.10 with standard layered architecture
- **Database**: PostgreSQL with Flyway migrations
- **Monitoring**: Spring Actuator + Prometheus metrics
- **Entry Point**: `com.quantlab.backend.QuantlabApiApplication`

### API Contract (Frontend to Backend)
The frontend expects these endpoints (see `services/apiService.ts`):

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/instruments?market=INDIA\|US` | Get available trading instruments |
| GET | `/api/strategies` | Get available strategies |
| POST | `/api/run-strategy` | Execute a strategy run |
| GET | `/api/signals/{runId}` | Get trading signals for a run |
| GET | `/api/paper-trades/{runId}` | Get paper trade results |
| GET | `/api/analytics/{runId}` | Get analytics summary |
| GET | `/api/equity-curve/{runId}` | Get equity curve data |

**Note**: The frontend uses mock data. When implementing the backend, replace the mock implementations in `apiService.ts` with actual API calls.

## Type Definitions
All shared TypeScript types are defined in `quantlab-frontend/types.ts`:
- `Instrument`, `Strategy`, `MarketType`
- `StrategyRunRequest`, `StrategyRunResponse`
- `Signal`, `PaperTrade`, `AnalyticsSummary`, `EquityCurvePoint`
