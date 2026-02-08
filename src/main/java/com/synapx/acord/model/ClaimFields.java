package com.synapx.acord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ClaimFields {

    private String policyNumber;
    private String dateOfLoss;
    private String timeOfLoss;
    private String locationOfLoss;
    private String descriptionOfAccident;
    private Integer estimateAmount;
    private String insuredName;
    private String driverName;
    private String ownerName;
    private Boolean injuryPresent = Boolean.FALSE;
    private Boolean fraudFlagPresent = Boolean.FALSE;
}
