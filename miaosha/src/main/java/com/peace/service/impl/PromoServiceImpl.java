package com.peace.service.impl;

import com.peace.dao.PromoDoMapper;
import com.peace.dataobject.PromoDo;
import com.peace.service.ItemService;
import com.peace.service.PromoService;
import com.peace.service.model.ItemModel;
import com.peace.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDoMapper promoDoMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀信息
        PromoDo promoDo = promoDoMapper.selectByItemId(itemId);

        //dataobj -> model
        PromoModel promoModel = convertFromDataObject(promoDo);
        if(promoModel == null){
            return null;
        }

        //判断当前时间是否是秒杀活动即将开始或者正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);  //表示还未开始
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);   //表示结束
        }else{
            promoModel.setStatus(2);
        }

        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        PromoDo promoDo = promoDoMapper.selectByPrimaryKey(promoId);
        if(promoDo.getItemId() == null || promoDo.getItemId().intValue() == 0){
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDo.getItemId());

        //将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(),itemModel.getStock());
    }

    private PromoModel convertFromDataObject(PromoDo promoDo){
        if(promoDo == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDo,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDo.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDo.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDo.getEndDate()));

        return promoModel;
    }
}
