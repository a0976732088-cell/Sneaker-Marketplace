package com.project.sneakermarket.commerce.logistics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "cvs_stores",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_cvs_stores_store_id_chain",
                        columnNames = {"store_id", "chain"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CvsStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, length = 20)
    private String storeId;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "chain", nullable = false, length = 20)
    private String chain;

    @Column(name = "city", nullable = false, length = 20)
    private String city;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
}