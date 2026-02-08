package com.synapx.acord.service;

import com.synapx.acord.model.ClaimFields;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FieldExtractorService {

    private static final List<String> FRAUD_KEYWORDS = List.of("fraud", "inconsistent", "staged");

    private static final Set<String> NEGATIVE_INJURY_VALUES = Set.of(
            "no", "none", "n/a", "na", "false", "0", "no injury", "no injuries", "not injured");

    private static final List<String> KNOWN_LABELS = List.of(
            "POLICY NUMBER",
            "DATE OF LOSS",
            "TIME OF LOSS",
            "LOCATION OF LOSS",
            "DESCRIPTION OF ACCIDENT",
            "ESTIMATE AMOUNT",
            "INSURED NAME",
            "DRIVER NAME",
            "OWNER NAME",
            "INJURED",
            "CLAIM TYPE");

    private static final Pattern DATE_PATTERN =
            Pattern.compile("\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12]\\d|3[01])[/-](\\d{2}|\\d{4})\\b");

    private static final Pattern TIME_PATTERN =
            Pattern.compile("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d(?:\\s?[APMapm]{2})?\\b|\\b(?:1[0-2]|0?[1-9])\\s?[APMapm]{2}\\b");

    private static final Pattern MONEY_PATTERN =
            Pattern.compile("[$]?\\s*\\d[\\d,]*(?:\\.\\d{1,2})?");

    public ClaimFields extractFields(String rawText) {
        ClaimFields fields = new ClaimFields();
        String text = normalizeText(rawText);
        if (!StringUtils.hasText(text)) {
            return fields;
        }

        fields.setPolicyNumber(extractLabeledValue(text, List.of("POLICY\\s*NUMBER", "POLICY\\s*NO(?:\\.|\\b)")));
        fields.setDateOfLoss(extractDate(text));
        fields.setTimeOfLoss(extractTime(text));
        fields.setLocationOfLoss(extractLocation(text));
        fields.setDescriptionOfAccident(extractDescription(text));
        fields.setEstimateAmount(extractEstimateAmount(text));
        fields.setInsuredName(extractLabeledValue(text, List.of("INSURED\\s*NAME", "NAME\\s*OF\\s*INSURED")));
        fields.setDriverName(extractLabeledValue(text, List.of("DRIVER\\s*NAME", "NAME\\s*OF\\s*DRIVER")));
        fields.setOwnerName(extractLabeledValue(text, List.of("OWNER\\s*NAME", "NAME\\s*OF\\s*OWNER")));
        fields.setInjuryPresent(detectInjury(text));
        fields.setFraudFlagPresent(detectFraud(fields.getDescriptionOfAccident()));
        return fields;
    }

    private String extractDate(String text) {
        String candidate = extractLabeledValue(text, List.of("DATE\\s*OF\\s*LOSS", "LOSS\\s*DATE"));
        String matchedDate = firstMatch(DATE_PATTERN, candidate);
        if (!StringUtils.hasText(matchedDate)) {
            matchedDate = firstMatch(DATE_PATTERN, text);
        }
        return normalizeDate(matchedDate);
    }

    private String extractTime(String text) {
        String candidate = extractLabeledValue(text, List.of("TIME\\s*OF\\s*LOSS", "LOSS\\s*TIME"));
        String matchedTime = firstMatch(TIME_PATTERN, candidate);
        if (!StringUtils.hasText(matchedTime)) {
            matchedTime = firstMatch(TIME_PATTERN, text);
        }
        return cleanValue(matchedTime);
    }

    private String extractLocation(String text) {
        String location = extractMultilineLabeledValue(
                text,
                List.of("LOCATION\\s*OF\\s*LOSS", "LOSS\\s*LOCATION"),
                List.of(
                        "DESCRIPTION\\s*OF\\s*ACCIDENT",
                        "ESTIMATE\\s*AMOUNT",
                        "DATE\\s*OF\\s*LOSS",
                        "TIME\\s*OF\\s*LOSS",
                        "INJURED",
                        "CLAIM\\s*TYPE",
                        "INSURED\\s*NAME",
                        "DRIVER\\s*NAME",
                        "OWNER\\s*NAME"));
        if (!StringUtils.hasText(location)) {
            location = extractLabeledValue(text, List.of("LOCATION\\s*OF\\s*LOSS", "LOSS\\s*LOCATION"));
        }
        if (!StringUtils.hasText(location)) {
            location = buildLocationFromAddressComponents(text);
        }
        return cleanValue(location);
    }

    private String extractDescription(String text) {
        String description = extractMultilineLabeledValue(
                text,
                List.of("DESCRIPTION\\s*OF\\s*ACCIDENT", "ACCIDENT\\s*DESCRIPTION"),
                List.of(
                        "ESTIMATE\\s*AMOUNT",
                        "INJURED",
                        "CLAIM\\s*TYPE",
                        "POLICY\\s*NUMBER",
                        "DATE\\s*OF\\s*LOSS",
                        "TIME\\s*OF\\s*LOSS",
                        "LOCATION\\s*OF\\s*LOSS",
                        "INSURED\\s*NAME",
                        "DRIVER\\s*NAME",
                        "OWNER\\s*NAME"));
        if (!StringUtils.hasText(description)) {
            description = extractLabeledValue(text, List.of("DESCRIPTION\\s*OF\\s*ACCIDENT", "ACCIDENT\\s*DESCRIPTION"));
        }
        return cleanValue(description);
    }

    private Integer extractEstimateAmount(String text) {
        String estimateCandidate = extractLabeledValue(text, List.of("ESTIMATE\\s*AMOUNT", "AMOUNT\\s*OF\\s*ESTIMATE"));
        Integer amount = parseFirstAmount(estimateCandidate);
        if (amount != null) {
            return amount;
        }

        Matcher matcher = Pattern.compile(
                "(?ims)\\bESTIMATE\\s*AMOUNT\\b\\s*[:#\\-]?\\s*([$]?\\s*\\d[\\d,]*(?:\\.\\d{1,2})?)")
                .matcher(text);
        if (matcher.find()) {
            amount = parseFirstAmount(matcher.group(1));
        }
        return amount;
    }

    private boolean detectInjury(String text) {
        String injuredValue = extractLabeledValue(text, List.of("INJURED\\s*PERSON", "INJURED", "INJURY\\s*INDICATOR"));
        boolean injuredSectionHasPerson =
                StringUtils.hasText(injuredValue) && !NEGATIVE_INJURY_VALUES.contains(normalizeToken(injuredValue));

        boolean claimTypeInjury = Pattern.compile("(?i)\\bCLAIM\\s*TYPE\\b\\s*[:#\\-]?\\s*INJURY\\b")
                .matcher(text)
                .find();

        return injuredSectionHasPerson || claimTypeInjury;
    }

    private boolean detectFraud(String description) {
        if (!StringUtils.hasText(description)) {
            return false;
        }
        String lower = description.toLowerCase(Locale.ROOT);
        return FRAUD_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String extractLabeledValue(String text, List<String> labelPatterns) {
        for (String label : labelPatterns) {
            String sameLine = extractWithPattern(
                    text,
                    "(?im)\\b" + label + "\\b\\s*[:#\\-]?\\s*([^\\r\\n]+)");
            if (StringUtils.hasText(sameLine)) {
                return sameLine;
            }

            String nextLine = extractWithPattern(
                    text,
                    "(?ims)\\b" + label + "\\b\\s*[:#\\-]?\\s*(?:\\r?\\n)+\\s*([^\\r\\n]+)");
            if (StringUtils.hasText(nextLine)) {
                return nextLine;
            }
        }
        return null;
    }

    private String extractMultilineLabeledValue(String text, List<String> labels, List<String> stopLabels) {
        String stopGroup = String.join("|", stopLabels);
        for (String label : labels) {
            Matcher matcher = Pattern.compile(
                    "(?ims)\\b" + label + "\\b\\s*[:#\\-]?\\s*(.+?)(?=\\n\\s*(?:" + stopGroup + ")\\b|\\z)")
                    .matcher(text);
            if (matcher.find()) {
                String collapsed = matcher.group(1).replaceAll("\\s*\\r?\\n\\s*", " ").trim();
                String cleaned = cleanValue(collapsed);
                if (StringUtils.hasText(cleaned)) {
                    return cleaned;
                }
            }
        }
        return null;
    }

    private String extractWithPattern(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return null;
    }

    private String cleanValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value
                .replaceAll("^[\\s:;\\-]+", "")
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = removeTrailingKnownLabels(cleaned);
        if (!StringUtils.hasText(cleaned) || isKnownLabel(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String removeTrailingKnownLabels(String value) {
        String cleaned = value;
        for (String knownLabel : KNOWN_LABELS) {
            String labelRegex = knownLabel.replace(" ", "\\s+");
            cleaned = cleaned.replaceAll("(?i)\\s{2,}" + labelRegex + "\\b.*$", "").trim();
        }
        return cleaned;
    }

    private boolean isKnownLabel(String value) {
        String normalized = value
                .replaceAll("[^A-Za-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return KNOWN_LABELS.contains(normalized);
    }

    private Integer parseFirstAmount(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        Matcher matcher = MONEY_PATTERN.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        String rawAmount = matcher.group().replace("$", "").replace(",", "").trim();
        if (!StringUtils.hasText(rawAmount)) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(rawAmount);
            return amount.setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstMatch(Pattern pattern, String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String normalizeDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(rawDate);
        if (!matcher.find()) {
            return null;
        }

        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        String rawYear = matcher.group(3);
        int year = Integer.parseInt(rawYear.length() == 2 ? "20" + rawYear : rawYear);
        return String.format(Locale.ROOT, "%02d/%02d/%04d", month, day, year);
    }

    private String buildLocationFromAddressComponents(String text) {
        String street = extractLabeledValue(text, List.of("STREET", "ADDRESS"));
        String city = extractLabeledValue(text, List.of("CITY"));
        String state = extractLabeledValue(text, List.of("STATE"));
        String zip = extractLabeledValue(text, List.of("ZIP", "ZIP\\s*CODE"));

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(street)) {
            parts.add(street);
        }
        String cityStateZip = java.util.stream.Stream.of(city, state, zip)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        if (StringUtils.hasText(cityStateZip)) {
            parts.add(cityStateZip);
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts);
    }

    private String normalizeToken(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }
}
