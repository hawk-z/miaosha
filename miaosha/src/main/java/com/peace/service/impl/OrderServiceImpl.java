package com.peace.service.impl;

import com.peace.dao.OrderDoMapper;
import com.peace.dao.SequenceDoMapper;
import com.peace.dataobject.OrderDo;
import com.peace.dataobject.SequenceDo;
import com.peace.error.BusinessException;
import com.peace.error.EMBusinessError;
import com.peace.mq.MqProducer;
import com.peace.service.ItemService;
import com.peace.service.OrderService;
import com.peace.service.UserService;
import com.peace.service.model.ItemModel;
import com.peace.service.model.OrderModel;
import com.peace.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderDoMapper orderDoMapper ;
    @Autowired
    private SequenceDoMapper sequenceDoMapper;
    @Autowired
    private MqProducer mqProducer;

    @Transactional
    @Override
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount) throws BusinessException {
        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
        //ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);  //减少对数据库的依赖
        if(itemModel == null){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }
        //UserModel userModel = userService.getUserById(userId);
        UserModel userModel = userService.getUserByIdInCache(userId);  //减少对数据库的依赖
        if(userModel == null){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
        }

        if(amount <= 0 || amount > 99){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }

        //校验活动信息
        if(promoId != null){
            //(1)校验对应活动是否存在于这个商品
            if(promoId.intValue() != itemModel.getPromoModel().getId()){
                throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
            }else if(itemModel.getPromoModel().getStatus().intValue() != 2){
                throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"活动未开始");
            }
        }

        //2.落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(result == false){
            throw new BusinessException(EMBusinessError.STOCK_NOT_ENOUGH);
        }

        //3.订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setId(this.generateOrderNo());
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if(promoId != null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }

        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        //生成交易流水号，订单号

        OrderDo orderDo = convertFromOrderModel(orderModel);
        orderDoMapper.insertSelective(orderDo);

        //加上商品的销量
        itemService.increaseSales(itemId, amount);

        //4.返回前端
        return orderModel;
    }

    //
    //@Transactional(propagation = Propagation.REQUIRES_NEW)
    private String generateOrderNo(){
        StringBuilder stringBuilder = new StringBuilder();

        //前8位为时间信息
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        //中间六位为自增序列，表示当天内的订单数量
        int sequence = 0;
        SequenceDo sequenceDO = sequenceDoMapper.getSequenceByName("order_info");

        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDO);

        String sequenceStr = String.valueOf(sequence);
        //对不足6位的序列前面加0，这里未考虑超过6位的序列
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(sequenceStr);

        //最后两位为分库分表位，这里固定
        stringBuilder.append("00");

        return stringBuilder.toString();
    }


    private OrderDo convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderDo orderDo = new OrderDo();
        BeanUtils.copyProperties(orderModel,orderDo);
        orderDo.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        orderDo.setItemPrice(orderModel.getItemPrice().doubleValue());
        return orderDo;
    }
}
