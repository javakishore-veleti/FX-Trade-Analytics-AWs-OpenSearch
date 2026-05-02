package com.jk.fx.trade_mgmt.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "currency_pair",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_currency_pair_from_to",
        columnNames = {"from_currency", "to_currency"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency;

    @Column(nullable = false)
    private boolean active;
}
