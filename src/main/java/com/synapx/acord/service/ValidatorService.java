package com.synapx.acord.service;

import com.synapx.acord.model.ClaimFields;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ValidatorService {

    public List<String> findMissingMandatoryFields(ClaimFields fields) {
        List<String> missing = new ArrayList<>();

        if (!StringUtils.hasText(fields.getPolicyNumber())) {
            missing.add("policyNumber");
        }
        if (!StringUtils.hasText(fields.getDateOfLoss())) {
            missing.add("dateOfLoss");
        }
        if (!StringUtils.hasText(fields.getLocationOfLoss())) {
            missing.add("locationOfLoss");
        }
        if (!StringUtils.hasText(fields.getDescriptionOfAccident())) {
            missing.add("descriptionOfAccident");
        }
        if (fields.getEstimateAmount() == null) {
            missing.add("estimateAmount");
        }
        return missing;
    }
}
