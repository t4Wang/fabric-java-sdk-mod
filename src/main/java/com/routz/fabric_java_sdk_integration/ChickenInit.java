package com.routz.fabric_java_sdk_integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChickenInit {

    /**
     * 耳标编号
     */
    private String rfId;

    /**
     * 品种
     */
    private String breed;

    /**
     * 籍贯
     */
    private String nativePlace;

    /**
     * 出生日期
     */
    private LocalDateTime birthDt;


    /**
     * 戴脚环时间
     */
    private LocalDateTime wearFootRingDt;

    /**
     * 养殖场名称
     */
    private String farmName;

    /**
     * 出栏重量
     */
    private Double suchWeight;

    /**
     * 创建日期
     */
    private LocalDateTime createDt;

    /**
     * 唯一的编号
     */
    private String soleNum;
}
