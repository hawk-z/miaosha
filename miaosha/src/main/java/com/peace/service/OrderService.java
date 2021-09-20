package com.peace.service;

import com.peace.error.BusinessException;
import com.peace.service.model.OrderModel;

public interface OrderService {
    OrderModel createOrder(Integer userId,Integer itemId,Integer promoId,Integer amount) throws BusinessException;
}
