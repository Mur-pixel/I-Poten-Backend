package com.cygnus.ipoten.awsCost.controller;

import com.cygnus.ipoten.administer.service.AdministratorService;
import com.cygnus.ipoten.awsCost.entity.AwsDailyCost;
import com.cygnus.ipoten.awsCost.service.AwsCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/aws/cost")
@RequiredArgsConstructor
public class AwsCostController {
    private final AwsCostService awsCostService;
    private final AdministratorService administratorService;

    @GetMapping("/daily")
    public ResponseEntity<List<AwsDailyCost>> dailyCostList(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @CookieValue(name = "userToken", required = false) String userToken)
        {

        if (!end.isAfter(start))return ResponseEntity.badRequest().build();

        if(!administratorService.isAdminByUserToken(userToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<AwsDailyCost> result = awsCostService.getDailyTotalCost(start,end);


        return ResponseEntity.ok(result);
    }
}
