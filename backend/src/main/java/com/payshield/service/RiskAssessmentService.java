package com.payshield.service;

import org.springframework.stereotype.Service;

/**
 * RiskAssessmentService — computes a composite risk score from three independent
 * signals and converts it into an actionable decision.
 *
 * Scoring weights
 * ───────────────
 *   XGBoost probability  60 %   (primary ML signal)
 *   Rule engine score    25 %   (deterministic heuristics)
 *   Isolation-forest     15 %   (unsupervised anomaly score, 0-100 range)
 *
 * Decision thresholds
 * ───────────────────
 *   BLOCK  → finalScore ≥ 75  AND  (xgboostPrediction=1  OR  isolationAnomaly=true)
 *              The hard double-confirmation requirement prevents false positives.
 *              Even a score of 90 will NOT block if both ML signals say it is clean.
 *
 *   REVIEW → finalScore ≥ 45  (one or more signals are suspicious — needs analyst)
 *
 *   ALLOW  → finalScore < 45  (all signals agree the transaction is low-risk)
 *
 * High-severity rule override
 * ───────────────────────────
 *   If any individually HIGH-severity rule fires, the score floor is raised to 55
 *   so that the transaction always goes to REVIEW (never silently ALLOW'd).
 */
@Service
public class RiskAssessmentService {

    // ── Score thresholds ──────────────────────────────────────────────────────

    /** Minimum finalScore to consider a transaction HIGH risk. */
    private static final double HIGH_RISK_THRESHOLD  = 75.0;

    /** Minimum finalScore to consider a transaction MEDIUM risk (→ REVIEW). */
    private static final double MEDIUM_RISK_THRESHOLD = 45.0;

    /**
     * When any high-severity rule fires, floor the score at this value so the
     * transaction is always sent to REVIEW even if ML scores are low.
     */
    private static final double HIGH_SEVERITY_RULE_FLOOR = 55.0;

    // ── Weight constants ──────────────────────────────────────────────────────

    private static final double XGBOOST_WEIGHT   = 0.60;
    private static final double RULE_WEIGHT       = 0.25;
    private static final double ISOLATION_WEIGHT  = 0.15;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calculates a composite 0-100 risk score.
     *
     * @param xgboostProbability     XGBoost fraud probability in [0.0, 1.0]
     * @param isolationForestScore   Isolation forest anomaly score in [0.0, 100.0]
     *                               (already normalised by the Python service)
     * @param ruleScore              Total risk points from the rule engine (0-N)
     * @param anyHighSeverityRuleTriggered  true if at least one HIGH-severity rule fired
     * @return composite risk score in [0.0, 100.0]
     */
    public double calculateRiskScore(
            double xgboostProbability,
            double isolationForestScore,
            int    ruleScore,
            boolean anyHighSeverityRuleTriggered
    ) {
        // Convert raw inputs to a 0-100 scale
        double xgboostRisk           = xgboostProbability * 100.0;
        double normalizedRuleScore   = Math.min(ruleScore, 100.0);
        double normalizedIsolation   = Math.min(Math.max(isolationForestScore, 0.0), 100.0);

        double score =
                (xgboostRisk         * XGBOOST_WEIGHT)
              + (normalizedRuleScore  * RULE_WEIGHT)
              + (normalizedIsolation  * ISOLATION_WEIGHT);

        score = Math.min(100.0, Math.max(0.0, score));

        // High-severity rule override — never silently approve a flagged tx
        if (anyHighSeverityRuleTriggered) {
            score = Math.max(score, HIGH_SEVERITY_RULE_FLOOR);
        }

        return score;
    }

    /**
     * Maps the composite score to a human-readable risk level string.
     */
    public String determineRiskLevel(double riskScore) {
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            return "HIGH";
        }
        if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * Converts a risk level into a fraud decision.
     *
     * This overload is a backwards-compatible helper that assumes the worst case
     * for BLOCKing (i.e., it always BLOCKs HIGH risk).  The preferred overload
     * below performs the double-confirmation check.
     */
    public String determineDecision(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH"   -> "BLOCK";
            case "MEDIUM" -> "REVIEW";
            default       -> "ALLOW";
        };
    }

    /**
     * Preferred overload — requires explicit ML confirmation before issuing BLOCK.
     *
     * BLOCK logic:
     *   finalScore ≥ HIGH_RISK_THRESHOLD  AND  (XGBoost predicted fraud  OR  anomaly detected)
     *
     * This prevents a transaction from being hard-blocked purely because of
     * correlated rule hits when the ML layer disagrees.
     *
     * @param riskLevel           output of {@link #determineRiskLevel(double)}
     * @param xgboostPrediction   raw XGBoost label (1 = fraud, 0 = legit)
     * @param isolationAnomaly    true when isolation-forest flags this as an anomaly
     */
    public String determineDecision(
            String  riskLevel,
            int     xgboostPrediction,
            boolean isolationAnomaly
    ) {
        return switch (riskLevel) {

            case "HIGH" -> {
                // Double-confirmation: at least ONE ML signal must agree
                boolean mlConfirmsBlock = (xgboostPrediction == 1) || isolationAnomaly;
                yield mlConfirmsBlock ? "BLOCK" : "REVIEW";
            }

            case "MEDIUM" -> "REVIEW";

            default -> "ALLOW";
        };
    }
}