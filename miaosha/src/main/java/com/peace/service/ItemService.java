package com.peace.service;

import com.peace.error.BusinessException;
import com.peace.service.model.ItemModel;

import java.util.List;

public interface ItemService {

    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);

    //库存扣减
    boolean decreaseStock(Integer itemId,Integer amount) throws BusinessException;

    //商品销量增加
    void increaseSales(Integer itemId,Integer amount);

    //验证item及promo model缓存模型
    ItemModel getItemByIdInCache(Integer id);
}
