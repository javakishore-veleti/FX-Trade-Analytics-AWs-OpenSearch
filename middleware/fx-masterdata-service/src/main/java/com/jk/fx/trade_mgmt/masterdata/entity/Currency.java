package com.jk.fx.trade_mgmt.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "currency")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {

    @Id
    @Column(length = 3, nullable = false)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 80)
    private String country;

    @Column(nullable = false)
    private boolean active;
}
