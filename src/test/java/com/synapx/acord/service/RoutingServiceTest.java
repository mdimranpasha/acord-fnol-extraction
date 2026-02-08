package com.synapx.acord.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapx.acord.model.ClaimFields;
import com.synapx.acord.model.RouteDecision;
import com.synapx.acord.model.RouteType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutingServiceTest {

    private final RoutingService routingService = new RoutingService();

    @Test
    void fraudKeywordHasHighestPriority() {
        ClaimFields fields = new ClaimFields();
        fields.setDescriptionOfAccident("Potential staged event with inconsistent details.");
        fields.setInjuryPresent(true);
        fields.setEstimateAmount(1000);

        RouteDecision decision = routingService.determineRoute(fields, List.of("policyNumber"));

        assertThat(decision.route()).isEqualTo(RouteType.INVESTIGATION_FLAG);
    }

    @Test
    void injuryHasPriorityOverMissingMandatoryFields() {
        ClaimFields fields = new ClaimFields();
        fields.setDescriptionOfAccident("Rear-end collision in city traffic.");
        fields.setInjuryPresent(true);
        fields.setEstimateAmount(1200);

        RouteDecision decision = routingService.determineRoute(fields, List.of("policyNumber"));

        assertThat(decision.route()).isEqualTo(RouteType.SPECIALIST_QUEUE);
    }

    @Test
    void missingMandatoryFieldsBeatFastTrack() {
        ClaimFields fields = new ClaimFields();
        fields.setDescriptionOfAccident("Low-speed parking lot accident.");
        fields.setInjuryPresent(false);
        fields.setEstimateAmount(5000);

        RouteDecision decision = routingService.determineRoute(fields, List.of("dateOfLoss"));

        assertThat(decision.route()).isEqualTo(RouteType.MANUAL_REVIEW);
    }

    @Test
    void fastTrackWhenCompleteAndUnderThreshold() {
        ClaimFields fields = new ClaimFields();
        fields.setDescriptionOfAccident("Minor bumper damage at stop sign.");
        fields.setInjuryPresent(false);
        fields.setEstimateAmount(20000);

        RouteDecision decision = routingService.determineRoute(fields, List.of());

        assertThat(decision.route()).isEqualTo(RouteType.FAST_TRACK);
    }
}
