# PayShield AI — ML Architecture

## Overview

PayShield AI uses a Python-based machine learning service
to perform payment fraud detection.

## Architecture

Spring Boot
    |
    | REST
    v
Python ML Service
    |
    +---- XGBoost
    |
    +---- Isolation Forest
    |
    v
Fraud Prediction

## Models

### XGBoost

XGBoost is used as the supervised classification model.

It produces a probability representing the likelihood
that a transaction is fraudulent.

### Isolation Forest

Isolation Forest is used for unsupervised anomaly detection.

It identifies transactions that are unusual compared
with normal transaction behavior.

## Prediction Response

The ML service returns:

- XGBoost fraud probability
- Isolation Forest anomaly score
- Anomaly detection result

## Current Status

Phase 7:
ML service architecture and inference API implemented.

Phase 8:
PaySim dataset training and evaluation.

Phase 9:
MLflow experiment tracking and model management.