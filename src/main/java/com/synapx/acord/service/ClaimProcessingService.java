package com.synapx.acord.service;

import com.synapx.acord.exception.BadRequestException;
import com.synapx.acord.model.ClaimFields;
import com.synapx.acord.model.ClaimProcessingResponse;
import com.synapx.acord.model.RouteDecision;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClaimProcessingService {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final FieldExtractorService fieldExtractorService;
    private final ValidatorService validatorService;
    private final RoutingService routingService;

    public ClaimProcessingResponse processPdf(MultipartFile file) {
        String text = pdfTextExtractorService.extractText(file);
        return processText(text);
    }

    public ClaimProcessingResponse processText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BadRequestException("Text is empty. Provide a non-empty claim document text.");
        }

        ClaimFields fields = fieldExtractorService.extractFields(text);
        List<String> missingFields = validatorService.findMissingMandatoryFields(fields);
        RouteDecision routeDecision = routingService.determineRoute(fields, missingFields);

        return new ClaimProcessingResponse(
                fields,
                missingFields,
                routeDecision.route(),
                routeDecision.reasoning());
    }
}
