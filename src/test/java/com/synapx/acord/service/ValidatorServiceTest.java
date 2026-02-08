package com.synapx.acord.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapx.acord.model.ClaimFields;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidatorServiceTest {

    private final ValidatorService validatorService = new ValidatorService();

    @Test
    void returnsAllMandatoryFieldsWhenMissing() {
        ClaimFields fields = new ClaimFields();

        List<String> missing = validatorService.findMissingMandatoryFields(fields);

        assertThat(missing).containsExactly(
                "policyNumber",
                "dateOfLoss",
                "locationOfLoss",
                "descriptionOfAccident",
                "estimateAmount");
    }

    @Test
    void returnsNoMissingWhenMandatoryFieldsArePresent() {
        ClaimFields fields = new ClaimFields();
        fields.setPolicyNumber("PL-12345");
        fields.setDateOfLoss("01/12/2026");
        fields.setLocationOfLoss("123 Main St, Austin, TX 78701");
        fields.setDescriptionOfAccident("Minor collision while reversing.");
        fields.setEstimateAmount(8900);

        List<String> missing = validatorService.findMissingMandatoryFields(fields);

        assertThat(missing).isEmpty();
    }
}
