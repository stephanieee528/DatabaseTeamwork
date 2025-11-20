package com.example.poverty.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "province")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Province {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long provinceId;
    @Column(nullable = false, length = 50)
    private String provinceName;

    // 明确添加 getter 方法，确保 Lombok 正常工作
    public Long getProvinceId() {
        return this.provinceId;
    }

    public String getProvinceName() {
        return this.provinceName;
    }

    public void setProvinceId(Long provinceId) {
        this.provinceId = provinceId;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }
}