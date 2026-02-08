package com.synapx.acord.controller;

import com.synapx.acord.exception.BadRequestException;
import com.synapx.acord.model.ClaimProcessingResponse;
import com.synapx.acord.model.ProcessTextRequest;
import com.synapx.acord.service.ClaimProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimProcessingController {

    private final ClaimProcessingService claimProcessingService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClaimProcessingResponse processPdf(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided. Use multipart field 'file' with a non-empty PDF.");
        }
        return claimProcessingService.processPdf(file);
    }

    @PostMapping(
            value = "/process-text",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClaimProcessingResponse processText(@RequestBody ProcessTextRequest request) {
        if (request == null || !StringUtils.hasText(request.getText())) {
            throw new BadRequestException("Text is empty. Provide JSON body in the form: {\"text\":\"...\"}.");
        }
        return claimProcessingService.processText(request.getText());
    }
}
