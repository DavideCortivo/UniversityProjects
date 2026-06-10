# Financial Risk Management: Market Risk Analysis
A Java-based quantitative framework designed to compute, compare, and visualize **Value at Risk (VaR)** and **Expected Shortfall (ES)** under a **1-day investment horizon**. Following the profit convention, the system analyzes a two-equity portfolio composed of **Newmont Goldcorp (NEM)** and **Eli Lilly (LLY)** using a **250-day rolling window**.

---

## Implemented Methodologies

1. **Historical Simulation (Non-Parametric):** Computes risk metrics directly from the empirical distribution of past portfolio returns, using a tail-adjustment term for exact mathematical consistency in ES.
2. **Parametric Normal Approach (Parametric):** Utilizes closed-form Gaussian solutions ($\text{VaR} = -\mu - z_{\alpha}\sigma$ and $\text{ES} = -\mu + \frac{\phi(z_{\alpha})}{\alpha}\sigma$) calibrated on rolling historical parameters.
3. **Monte Carlo Simulation (Stochastic):** Generates **100,000 random log-return scenarios** per window using independent seeds per asset to preserve their uncorrelated nature, aggregating them into a simulated portfolio before extracting empirical quantiles.

---

## Project Structure & Architecture

The project is structured under the package `it.univr.riskmanagement` and is divided into 5 specialized classes:

* **`DataCollectionAndPlotting.java`**: Handles Excel data ingestion via **Apache POI** and creates single/multiple overlaid time-series charts using **JFreeChart**.
* **`DataManagement.java`**: Manages asset prices, historical dates, Pearson correlation, and dynamically computes weighted portfolio returns based on capital allocation.
* **`MonteCarloSimulation.java`**: The core simulation engine that drives the 100,000 stochastic runs using the Apache Commons Math `JDKRandomGenerator`.
* **`RiskMeasures.java`**: Contains the pure mathematical logic (`static` methods) for all iterations. It features an optimized execution loop that computes both VaR and ES in a single pass to maximize performance.
* **`Tests.java`**: The main execution entry point. Includes an interactive console interface (`Scanner`) allowing users to toggle charts between **percentage** and **monetary** terms.
