package uk.tw.energy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/usage-costs")
public class UsageCostsController {

    public static final int DAYS = 7;
    private final AccountService accountService;
    private final PricePlanService pricePlanService;

    public UsageCostsController(AccountService accountService, PricePlanService pricePlanService) {
        this.accountService = accountService;
        this.pricePlanService = pricePlanService;
    }

    @GetMapping("/{smartMeterId}")
    public ResponseEntity<Map.Entry<String, BigDecimal>> getCostForLastWeek(@PathVariable String smartMeterId) {

        Optional<String> pricePlanId = Optional.ofNullable(accountService.getPricePlanIdForSmartMeterId(smartMeterId));

        if (pricePlanId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // We're assuming here we want the last 7 days of readings without taking into account the current time
        BigDecimal cost = pricePlanService.getConsumptionCostForPastDays(smartMeterId, DAYS, pricePlanId.get());

        return ResponseEntity.ok(Map.entry("cost", cost));
    }
}
