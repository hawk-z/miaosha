package com.peace.dao;

import com.peace.dataobject.ItemDo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ItemDoMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int increaseSales(@Param("id") Integer itemId, @Param("amount") Integer amount);

    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int insert(ItemDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int insertSelective(ItemDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */

    List<ItemDo> listItem();


    ItemDo selectByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int updateByPrimaryKeySelective(ItemDo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table item
     *
     * @mbg.generated Fri Jul 02 17:31:21 CST 2021
     */
    int updateByPrimaryKey(ItemDo record);
}