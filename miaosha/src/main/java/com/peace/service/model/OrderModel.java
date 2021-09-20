package com.peace.service.model;

import lombok.Data;

import java.math.BigDecimal;

//用户下单的交易模型
@Data
public class OrderModel {
    //交易号是一串 比如2021070200011218可能代表2021年7月8号0001号的1218商品
    private String id;

    //购买的用户id
    private Integer userId;

    //购买的商品id
    private Integer itemId;

    //若非空，则表示是以秒杀商品的单价
    private Integer promoId;

    //购买商品的单价，若非空promoId，则表示是以秒杀商品的单价
    private BigDecimal itemPrice;

    //购买数量
    private Integer amount;

    //购买金额，若非空promoId，则表示是以秒杀商品的价格
    private BigDecimal orderPrice;
}
