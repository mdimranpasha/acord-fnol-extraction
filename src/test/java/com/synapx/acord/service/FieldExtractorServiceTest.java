package com.synapx.acord.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapx.acord.model.ClaimFields;
import org.junit.jupiter.api.Test;

class FieldExtractorServiceTest {

    private final FieldExtractorService fieldExtractorService = new FieldExtractorService();

    @Test
    void extractsFieldsFromLabeledTextWithLineBreaks() {
        String text = """
                POLICY NUMBER
                PL-998877
                DATE OF LOSS: 1/5/2026
                TIME OF LOSS
                14:45
                LOCATION OF LOSS
                123 Main St, Austin, TX 78701
                DESCRIPTION OF ACCIDENT
                Rear-end collision at a stoplight with minor bumper damage.
                ESTIMATE AMOUNT
                $12,450.00
                INSURED NAME: John Doe
                DRIVER NAME
                Jane Doe
                OWNER NAME: John Doe
                CLAIM TYPE: Injury
                """;

        ClaimFields fields = fieldExtractorService.extractFields(text);

        assertThat(fields.getPolicyNumber()).isEqualTo("PL-998877");
        assertThat(fields.getDateOfLoss()).isEqualTo("01/05/2026");
        assertThat(fields.getTimeOfLoss()).isEqualTo("14:45");
        assertThat(fields.getLocationOfLoss()).contains("123 Main St");
        assertThat(fields.getDescriptionOfAccident()).contains("Rear-end collision");
        assertThat(fields.getEstimateAmount()).isEqualTo(12450);
        assertThat(fields.getInsuredName()).isEqualTo("John Doe");
        assertThat(fields.getDriverName()).isEqualTo("Jane Doe");
        assertThat(fields.getOwnerName()).isEqualTo("John Doe");
        assertThat(fields.getInjuryPresent()).isTrue();
        assertThat(fields.getFraudFlagPresent()).isFalse();
    }
}
