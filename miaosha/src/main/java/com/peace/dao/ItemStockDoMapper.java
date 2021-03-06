package com.peace.dao;

import com.peace.dataobject.ItemStockDo;
import org.apache.ibatis.annotations.Param;

public interface ItemStockDoMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item_stock
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int deleteByPrimaryKey(Integer id);

    int decreaseStock(@Param("itemId") Integer itemId,@Param("amount") Integer amount);
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item_stock
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int insert(ItemStockDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item_stock
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int insertSelective(ItemStockDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item_stock
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    ItemStockDo selectByPrimaryKey(Integer id);
    ItemStockDo selectByItemId(Integer itemId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item_stock
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int updateByPrimaryKeySelective(ItemStockDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item_stock
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int updateByPrimaryKey(ItemStockDo record);
}