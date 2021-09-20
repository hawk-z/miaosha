package com.peace.controller;

import com.peace.controller.BaseController;
import com.peace.error.BusinessException;
import com.peace.error.EMBusinessError;
import com.peace.response.CommonReturnType;
import com.peace.service.OrderService;
import com.peace.service.model.OrderModel;
import com.peace.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
@Controller("order")
@RequestMapping("/order")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/createorder",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="amount")Integer amount,
                                        @RequestParam(name="promoId",required = false)Integer promoId) throws BusinessException {
        //Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        //没次请求都要附上token值
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN,"用户未登陆，不能下单");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN,"用户未登陆，不能下单");
        }

//        if(isLogin == null || !isLogin.booleanValue()){
//            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN,"用户未登陆，不能下单");
//        }

//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        //  获取用户的登陆信息
        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId,amount);

        return CommonReturnType.create(null);
    }
}
