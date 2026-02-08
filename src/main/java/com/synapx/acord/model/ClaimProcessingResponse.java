package com.synapx.acord.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimProcessingResponse {

    private ClaimFields extractedFields;
    private List<String> missingFields = new ArrayList<>();
    private RouteType recommendedRoute;
    private String reasoning;
}
