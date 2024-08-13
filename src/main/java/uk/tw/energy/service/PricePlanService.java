package uk.tw.energy.service;

import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(
            String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        return electricityReadings.map(readings -> pricePlans.stream()
                .collect(Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(readings, t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::reading)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::time))
                .get();

        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::time))
                .get();

        return BigDecimal.valueOf(Duration.between(first.time(), last.time()).getSeconds() / 3600.0);
    }

    /**
     * Calculate the cost of electricity readings for the past days
     *
     * <p>
     *     Assuming N electricity readings (er1, er2, ..., erN) are available for the smart meter, the cost is calculated
     *     as follows:
     *  <ul>
     *      <li>Average reading in KW = (er1.reading + er2.reading + ..... erN.Reading)/N</li>
     *      <li>Usage time in hours = Duration(D) in hours</li>
     *      <li>Energy consumed in kWh = average reading * usage time</li>
     *      <li>Cost = tariff unit prices * energy consumed</li>
     *  </ul>
     * </p>
     */
    public BigDecimal getConsumptionCostForPastDays(String smartMeterId, int days, String pricePlanId) {
        var pricePlan = getPricePlanOrThrowException(pricePlanId);

        List<ElectricityReading> electricityReadings = meterReadingService.getReadings(smartMeterId)
                .orElse(Collections.emptyList());

        if (electricityReadings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // We assume readings are sorted by time in ascending order
        LocalDate lastDateToReadExclusive = electricityReadings.getLast().time()
                .minus(Duration.ofDays(days))
                .atZone(ZoneId.systemDefault()).toLocalDate();

        int fromIndex = getIndexOfReadingAfterLastDateToRead(electricityReadings, lastDateToReadExclusive);
        List<ElectricityReading> readingsForPastDays = electricityReadings.subList(fromIndex, electricityReadings.size());

        BigDecimal average = calculateAverageReading(readingsForPastDays);
        BigDecimal usageTime = calculateTimeElapsed(readingsForPastDays);
        BigDecimal energyConsumed = average.multiply(usageTime);

        return pricePlan.getUnitRate().multiply(energyConsumed)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PricePlan getPricePlanOrThrowException(String pricePlanId) {
        return pricePlans.stream()
                .filter(plan -> plan.getPlanName().equals(pricePlanId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Price plan not found"));
    }

    private static int getIndexOfReadingAfterLastDateToRead(List<ElectricityReading> electricityReadings, LocalDate lastDateToReadExclusive) {
        int fromIndex = electricityReadings.size() - 1;

        for (int i = fromIndex; i >= 0; i--) {
            LocalDate readingDate = electricityReadings.get(i).time().atZone(ZoneId.systemDefault()).toLocalDate();

            boolean dateIsOutOfReadingBounds = readingDate.equals(lastDateToReadExclusive)
                    || readingDate.isBefore(lastDateToReadExclusive);
            if (dateIsOutOfReadingBounds) {
                break;
            }

            fromIndex = i;
        }

        return fromIndex;
    }
}
