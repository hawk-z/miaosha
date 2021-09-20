package com.peace.service.impl;

import com.peace.dao.UserDoMapper;
import com.peace.dao.UserPasswordDoMapper;
import com.peace.dataobject.UserDo;
import com.peace.dataobject.UserPasswordDo;
import com.peace.error.BusinessException;
import com.peace.error.EMBusinessError;
import com.peace.service.UserService;
import com.peace.service.model.UserModel;
import com.peace.validator.ValidationResult;
import com.peace.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDoMapper userDoMapper;

    @Autowired
    private UserPasswordDoMapper userPasswordDoMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public UserModel getUserById(Integer id) {
        UserDo userDo = userDoMapper.selectByPrimaryKey(id);
        if(userDo == null){
            return null;
        }

        UserPasswordDo userPasswordDo = userPasswordDoMapper.selectByUserId(userDo.getId());

        return convertFromDataObject(userDo,userPasswordDo);
    }

    @Override
    @Transactional
    //Transactional的作用是为了防止出现用户信息表和密码表只有一个插入成功的场景
    public void register(UserModel userModel) throws BusinessException {
        if(userModel == null){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR);
        }
//        if(StringUtils.isEmpty(userModel.getName())
//                || userModel.getGender() == null
//                || userModel.getAge() == null
//                || userModel.getEncrptPassword() == null
//                || StringUtils.isEmpty(userModel.getTelphone())){
//            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR);
//        }
        ValidationResult result = validator.validate(userModel);
        if(result.isHasErrors()){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }

        //实现model->dataobject方法
        //insertSelective插入前会先进行判空，不为空才插入，为空则不插入，为数据库的默认值
        //insert如果传入的为null的话就把默认值覆盖掉了，就不好
        UserDo userDo = convertFromModel(userModel);
        try {
            userDoMapper.insertSelective(userDo);
        }catch (DuplicateKeyException ex){
            //System.out.println("===========");
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已被注册");
        }
        userModel.setId(userDo.getId());

        UserPasswordDo userPasswordDo = convertPasswordFromModel(userModel);

        userPasswordDoMapper.insertSelective(userPasswordDo);
        return;
    }

    @Override
    public UserModel validateLogin(String telphone, String encrptPasswd) throws BusinessException {
        UserDo userDo = userDoMapper.selectByTelphone(telphone);
        if(userDo == null){
            throw new BusinessException(EMBusinessError.UNKNOWN_ERROR,"用户名或密码错误");
        }
        UserPasswordDo userPasswordDo = userPasswordDoMapper.selectByUserId(userDo.getId());
        UserModel userModel = convertFromDataObject(userDo,userPasswordDo);

        // 入参校验
        if(!StringUtils.equals(encrptPasswd,userModel.getEncrptPassword())){
            throw new BusinessException(EMBusinessError.UNKNOWN_ERROR,"用户名或密码错误");
        }

        return userModel;
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_" + id);
        if(userModel == null){
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_validate_" + id,userModel);
            redisTemplate.expire("user_validate_" + id,10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    //----------------------------------------------------------------------------------------

    private UserPasswordDo convertPasswordFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserPasswordDo userPasswordDo = new UserPasswordDo();
        userPasswordDo.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDo.setUserId(userModel.getId());
        return userPasswordDo;
    }

    private UserDo convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserDo userDo = new UserDo();
        BeanUtils.copyProperties(userModel,userDo);
        return userDo;
    }

    //----------------------------------------------------------------------------------------

    private UserModel convertFromDataObject(UserDo userDo, UserPasswordDo userPasswordDo){
        if(userDo == null){
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDo,userModel);

        if(userPasswordDo != null){
            userModel.setEncrptPassword(userPasswordDo.getEncrptPassword());
        }
        return userModel;
    }
}
