package com.peace.service.impl;

import com.peace.dao.ItemDoMapper;
import com.peace.dao.ItemStockDoMapper;
import com.peace.dataobject.ItemDo;
import com.peace.dataobject.ItemStockDo;
import com.peace.error.BusinessException;
import com.peace.error.EMBusinessError;
import com.peace.mq.MqProducer;
import com.peace.service.ItemService;
import com.peace.service.PromoService;
import com.peace.service.model.ItemModel;
import com.peace.service.model.PromoModel;
import com.peace.validator.ValidationResult;
import com.peace.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private ItemDoMapper itemDoMapper;
    @Autowired
    private ItemStockDoMapper itemStockDoMapper;
    @Autowired
    private PromoService promoService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MqProducer mqProducer;

    private ItemDo convertItemDoFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemDo itemDo = new ItemDo();
        BeanUtils.copyProperties(itemModel,itemDo);
        itemDo.setPrice(itemModel.getPrice().doubleValue());
        return itemDo;
    }

    private ItemStockDo convertItemStockDoFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemStockDo itemStockDo = new ItemStockDo();
        itemStockDo.setStock(itemModel.getStock());
        itemStockDo.setItemId(itemModel.getId());

        return itemStockDo;
    }

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult validate = validator.validate(itemModel);
        if(validate.isHasErrors()){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,validate.getErrMsg());
        }


        //转换itemmodel->dataobject
        ItemDo itemDo = this.convertItemDoFromItemModel(itemModel);

        //写入数据库

        itemDoMapper.insertSelective(itemDo);


        itemModel.setId(itemDo.getId());
        ItemStockDo stockDo = this.convertItemStockDoFromItemModel(itemModel);
        itemStockDoMapper.insertSelective(stockDo);


        //返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDo> itemDos = itemDoMapper.listItem();
        List<ItemModel> itemModelList = itemDos.stream().map(itemDo -> {
            ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());
            ItemModel itemModel = this.convertModelFromDataObject(itemDo, itemStockDo);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDo itemDo = itemDoMapper.selectByPrimaryKey(id);
        if(itemDo == null){
            return null;
        }
        //操作获得库存数量
        ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(id);

        ItemModel itemModel = this.convertModelFromDataObject(itemDo, itemStockDo);

        //获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());

        if(promoModel != null && promoModel.getStatus().intValue() != 3){
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        //int affectedRow = itemStockDoMapper.decreaseStock(itemId, amount);
        long res = redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue() * -1);
        System.out.println(redisTemplate.opsForValue().get("promo_item_stock_"+itemId));
        if(res >= 0) {

            boolean mqResult = mqProducer.asyncReduceStock(itemId,amount);
            if(!mqResult){
                redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue());
                return false;
            }
            return true;
        }  //更新库存成功
        else {
            redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue());
            return false;
        }               //更新库存失败
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemDoMapper.increaseSales(itemId,amount);
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if(itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id,itemModel);
            redisTemplate.expire("item_validate_" + id,10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    //将dataobject->model
    private ItemModel convertModelFromDataObject(ItemDo itemDo,ItemStockDo itemStockDo){
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDo,itemModel);
        itemModel.setPrice(new BigDecimal(itemDo.getPrice()));
        itemModel.setStock(itemStockDo.getStock());
        return itemModel;
    }
}
