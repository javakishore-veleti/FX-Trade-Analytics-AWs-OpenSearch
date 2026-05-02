package com.jk.fx.trade_mgmt.api;

import com.jk.fx.trade_mgmt.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService service;

    /** Legacy: emits a hardcoded demo trade. Kept for backward compatibility. */
    @PostMapping
    public String create() {
        service.process();
        return "sent";
    }

    @PostMapping("/place")
    public ResponseEntity<TradePlaceResponse> place(@RequestBody TradeRequest req) {
        TradePlaceResponse resp = service.place(req);
        return resp.isAccepted()
                ? ResponseEntity.status(HttpStatus.ACCEPTED).body(resp)
                : ResponseEntity.unprocessableEntity().body(resp);
    }
}
