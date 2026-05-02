
package com.jk.fx.trade_mgmt.api;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.jk.fx.trade_mgmt.service.TradeService;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

  private final TradeService service;

  @PostMapping
  public String create() {
    service.process();
    return "sent";
  }
}
