package com.project.sneakermarket.calendar.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarEventDTOValidationTests {

    private static ValidatorFactory
            validatorFactory;

    private static Validator
            validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory =
                Validation
                        .buildDefaultValidatorFactory();

        validator =
                validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void emptyCoordinatesAreAllowed() {
        CalendarEventDTO request =
                new CalendarEventDTO();

        Set<ConstraintViolation<CalendarEventDTO>>
                violations =
                validator.validate(request);

        assertTrue(
                violations.isEmpty()
        );
    }

    @Test
    void validCoordinatePairIsAllowed() {
        CalendarEventDTO request =
                new CalendarEventDTO();

        request.setLatitude(
                new BigDecimal(
                        "25.051634"
                )
        );

        request.setLongitude(
                new BigDecimal(
                        "121.599389"
                )
        );

        Set<ConstraintViolation<CalendarEventDTO>>
                violations =
                validator.validate(request);

        assertTrue(
                violations.isEmpty()
        );
    }

    @Test
    void latitudeWithoutLongitudeIsRejected() {
        CalendarEventDTO request =
                new CalendarEventDTO();

        request.setLatitude(
                new BigDecimal(
                        "25.051634"
                )
        );

        Set<ConstraintViolation<CalendarEventDTO>>
                violations =
                validator.validate(request);

        assertTrue(
                hasMessage(
                        violations,
                        "緯度與經度必須同時填寫"
                )
        );
    }

    @Test
    void longitudeWithoutLatitudeIsRejected() {
        CalendarEventDTO request =
                new CalendarEventDTO();

        request.setLongitude(
                new BigDecimal(
                        "121.599389"
                )
        );

        Set<ConstraintViolation<CalendarEventDTO>>
                violations =
                validator.validate(request);

        assertTrue(
                hasMessage(
                        violations,
                        "緯度與經度必須同時填寫"
                )
        );
    }

    @Test
    void coordinatesOutsideValidRangeAreRejected() {
        CalendarEventDTO request =
                new CalendarEventDTO();

        request.setLatitude(
                new BigDecimal(
                        "91.000000"
                )
        );

        request.setLongitude(
                new BigDecimal(
                        "181.000000"
                )
        );

        Set<ConstraintViolation<CalendarEventDTO>>
                violations =
                validator.validate(request);

        assertTrue(
                hasMessage(
                        violations,
                        "緯度不可大於 90"
                )
        );

        assertTrue(
                hasMessage(
                        violations,
                        "經度不可大於 180"
                )
        );
    }

    @Test
    void venueAndAddressLengthLimitsAreEnforced() {
        CalendarEventDTO request =
                new CalendarEventDTO();

        request.setVenueName(
                "A".repeat(151)
        );

        request.setAddress(
                "B".repeat(301)
        );

        Set<ConstraintViolation<CalendarEventDTO>>
                violations =
                validator.validate(request);

        assertFalse(
                violations.isEmpty()
        );

        assertTrue(
                hasMessage(
                        violations,
                        "場地名稱最多 150 個字"
                )
        );

        assertTrue(
                hasMessage(
                        violations,
                        "活動地址最多 300 個字"
                )
        );
    }

    private boolean hasMessage(
            Set<ConstraintViolation<CalendarEventDTO>>
                    violations,
            String expectedMessage
    ) {
        return violations
                .stream()
                .anyMatch(violation ->
                        expectedMessage.equals(
                                violation.getMessage()
                        )
                );
    }
}