package com.peace.service;

import com.peace.error.BusinessException;
import com.peace.service.model.UserModel;

public interface UserService {
    UserModel getUserById(Integer id);
    void register(UserModel userModel) throws BusinessException;
    UserModel validateLogin(String telphone,String encrptPasswd) throws BusinessException;
    UserModel getUserByIdInCache(Integer id);
}
