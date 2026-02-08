package com.synapx.acord.service;

import com.synapx.acord.model.ClaimFields;
import com.synapx.acord.model.RouteDecision;
import com.synapx.acord.model.RouteType;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RoutingService {

    private static final List<String> INVESTIGATION_KEYWORDS = List.of("fraud", "inconsistent", "staged");

    /**
     * Routing priority order:
     * 1) INVESTIGATION_FLAG when description contains fraud/inconsistent/staged
     * 2) SPECIALIST_QUEUE when injury is indicated
     * 3) MANUAL_REVIEW when mandatory fields are missing
     * 4) FAST_TRACK when estimateAmount < 25000
     * 5) MANUAL_REVIEW as safe default
     */
    public RouteDecision determineRoute(ClaimFields fields, List<String> missingFields) {
        ClaimFields safeFields = fields == null ? new ClaimFields() : fields;
        String description = safeFields.getDescriptionOfAccident();
        String normalizedDescription = StringUtils.hasText(description)
                ? description.toLowerCase(Locale.ROOT)
                : "";

        boolean hasInvestigationKeyword = INVESTIGATION_KEYWORDS.stream().anyMatch(normalizedDescription::contains)
                || Boolean.TRUE.equals(safeFields.getFraudFlagPresent());
        if (hasInvestigationKeyword) {
            return new RouteDecision(
                    RouteType.INVESTIGATION_FLAG,
                    "Description contains investigation keyword (fraud, inconsistent, or staged).");
        }

        if (Boolean.TRUE.equals(safeFields.getInjuryPresent())) {
            return new RouteDecision(
                    RouteType.SPECIALIST_QUEUE,
                    "Injury is indicated in the claim details.");
        }

        if (missingFields != null && !missingFields.isEmpty()) {
            return new RouteDecision(
                    RouteType.MANUAL_REVIEW,
                    "Mandatory field(s) missing: " + String.join(", ", missingFields) + ".");
        }

        Integer estimateAmount = safeFields.getEstimateAmount();
        if (estimateAmount != null && estimateAmount < 25_000) {
            return new RouteDecision(
                    RouteType.FAST_TRACK,
                    "All mandatory fields are present and estimateAmount is below 25000.");
        }

        return new RouteDecision(
                RouteType.MANUAL_REVIEW,
                "Safe default route applied (high estimate amount or insufficient confidence for fast-track).");
    }
}
