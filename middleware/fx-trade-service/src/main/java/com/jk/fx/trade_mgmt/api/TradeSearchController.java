package com.jk.fx.trade_mgmt.api;

import com.jk.fx.trade_mgmt.service.TradeSearchService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades/search")
@RequiredArgsConstructor
public class TradeSearchController {

    private final TradeSearchService service;

    /** Legacy: returns a count summary. Prefer GET /trades/search. */
    @GetMapping("/risk")
    public String searchByRisk(@RequestParam String risk) {
        return service.searchByRisk(risk);
    }

    /**
     * Single-region or multi-region search.
     *
     * <ul>
     *   <li>{@code regions=us-east-1,eu-west-2} — comma-separated; the service
     *       fans out across each, merges results, sorts by timestamp desc,
     *       truncates to {@code size}. Each result keeps its source region
     *       in the {@code region} field for the UI to display.</li>
     *   <li>{@code crossRegion=true} (with no {@code regions}) — fans out
     *       across every configured backend in {@code fx.opensearch.backends}.</li>
     *   <li>{@code region=us-east-1} (legacy single-region) — original behaviour.</li>
     * </ul>
     */
    @GetMapping
    public List<Map<String, Object>> search(
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String regions,
            @RequestParam(required = false, defaultValue = "false") boolean crossRegion,
            @RequestParam(defaultValue = "50") int size) {

        if (crossRegion || (regions != null && !regions.isBlank())) {
            List<String> list = (regions == null || regions.isBlank())
                    ? List.of()
                    : Arrays.stream(regions.split(",")).map(String::trim).toList();
            return service.searchMulti(risk, list, size);
        }
        return service.search(risk, region, size);
    }
}
