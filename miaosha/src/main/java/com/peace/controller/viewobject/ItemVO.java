package com.peace.controller.viewobject;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemVO {
    private Integer id;

    private String title;

    private BigDecimal price;

    private Integer stock;

    private String description;

    private Integer sales;

    private String imgUrl;

    //记录商品是否在秒杀活动中
    private Integer promoStatus;

    private BigDecimal promoPrice;

    private Integer promoId;

    private String startDate;
}
