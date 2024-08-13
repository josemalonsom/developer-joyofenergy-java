package uk.tw.energy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PricePlanServiceTest {

    private MeterReadingService meterReadingService;
    private PricePlanService pricePlanService;
    private PricePlan pricePlan;

    @BeforeEach
    public void setUp() {
        meterReadingService = new MeterReadingService(new HashMap<>());
        pricePlan = new PricePlan("price-plan-name", null, BigDecimal.valueOf(0.29), null);
        List<PricePlan> pricePlans = List.of(pricePlan);
        pricePlanService = new PricePlanService(pricePlans, meterReadingService);
    }

    @ParameterizedTest
    @MethodSource("costOfPastDaysDataProvider")
    public void givenSmartMeterIdShouldReturnConsumptionCostOfPastDays(int days, double expectedCost) {
        var readings = Arrays.asList(
            new ElectricityReading(Instant.parse("2024-10-19T09:25:00Z"), BigDecimal.valueOf(1.101)),
            new ElectricityReading(Instant.parse("2024-10-20T10:00:00Z"), BigDecimal.valueOf(0.994)),
            new ElectricityReading(Instant.parse("2024-10-21T16:58:00Z"), BigDecimal.valueOf(0.503)),
            new ElectricityReading(Instant.parse("2024-10-22T13:20:00Z"), BigDecimal.valueOf(1.065)),
            new ElectricityReading(Instant.parse("2024-10-23T10:40:00Z"), BigDecimal.valueOf(0.213)),
            new ElectricityReading(Instant.parse("2024-10-24T11:00:00Z"), BigDecimal.valueOf(0.24)),
            new ElectricityReading(Instant.parse("2024-10-25T15:28:00Z"), BigDecimal.valueOf(0.598)),
            new ElectricityReading(Instant.parse("2024-10-26T03:45:00Z"), BigDecimal.valueOf(0.001)),
            new ElectricityReading(Instant.parse("2024-10-26T09:26:00Z"), BigDecimal.valueOf(0.506)),
            new ElectricityReading(Instant.parse("2024-10-27T12:46:00Z"), BigDecimal.valueOf(1.011)),
            new ElectricityReading(Instant.parse("2024-10-28T15:14:00Z"), BigDecimal.valueOf(1.201)),
            new ElectricityReading(Instant.parse("2024-10-29T07:10:00Z"), BigDecimal.valueOf(0.009)),
            new ElectricityReading(Instant.parse("2024-10-30T09:54:00Z"), BigDecimal.valueOf(0.202))
        );

        meterReadingService.storeReadings("smart-meter-id", readings);

        BigDecimal result = pricePlanService.getConsumptionCostForPastDays("smart-meter-id", days, pricePlan.getPlanName());
        assertThat(result).isEqualTo(BigDecimal.valueOf(expectedCost).setScale(2, RoundingMode.HALF_UP));
    }

    private static Stream<Arguments> costOfPastDaysDataProvider() {
        return Stream.of(
                Arguments.of(12, 45.10),
                Arguments.of(7, 19.52)
        );
    }

    @Test
    public void givenNoReadingsShouldReturnZeroCost() {
        meterReadingService.storeReadings("smart-meter-id", List.of());

        BigDecimal result = pricePlanService.getConsumptionCostForPastDays("smart-meter-id", 1, pricePlan.getPlanName());
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }
}