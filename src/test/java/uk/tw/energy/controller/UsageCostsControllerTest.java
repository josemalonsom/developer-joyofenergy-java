package uk.tw.energy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

public class UsageCostsControllerTest {

    private static final String SMART_METER_ID = "10101010";
    public static final String PRICE_PLAN_ID = "price-plan-id";

    private UsageCostsController usageCostsController;
    private final AccountService accountService = mock(AccountService.class);
    private final PricePlanService pricePlanService = mock(PricePlanService.class);

    @BeforeEach
    public void setUp() {
        this.usageCostsController = new UsageCostsController(accountService, pricePlanService);
    }

    @Test
    public void givenMeterIdSuppliedDoesNotHavePricePlanAttachedShouldReturnErrorResponse() {
        when(accountService.getPricePlanIdForSmartMeterId(SMART_METER_ID)).thenReturn(null);
        // TODO: What's the best error code to return here?
        // TODO: Message should be verified too
        assertThat(usageCostsController.getCostForLastWeek(SMART_METER_ID).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void givenMeterIdSuppliedReturnsTheCostForLastWeek() {
        when(accountService.getPricePlanIdForSmartMeterId(SMART_METER_ID)).thenReturn(PRICE_PLAN_ID);
        when(pricePlanService.getConsumptionCostForPastDays(SMART_METER_ID, 7, PRICE_PLAN_ID))
                .thenReturn(BigDecimal.valueOf(43.23));

        ResponseEntity<Map.Entry<String, BigDecimal>> result = usageCostsController.getCostForLastWeek(SMART_METER_ID);

        verify(pricePlanService).getConsumptionCostForPastDays(SMART_METER_ID, 7, PRICE_PLAN_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(result.getBody()).getKey()).isEqualTo("cost");
        assertThat(Objects.requireNonNull(result.getBody()).getValue()).isEqualTo(BigDecimal.valueOf(43.23));
    }
}
