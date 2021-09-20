package com.peace.controller;

import com.peace.controller.viewobject.UserVO;
import com.peace.error.BusinessException;
import com.peace.error.EMBusinessError;
import com.peace.response.CommonReturnType;
import com.peace.service.UserService;
import com.peace.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//跨域请求问题
//将spring-boot-parent降低版本
//设置火狐浏览器禁用SameSite，网址如下
//https://blog.csdn.net/qq_36468169/article/details/108380226?utm_term=bycookiesdefaultsamesite%E8%AE%BE%E7%BD%AE&utm_medium=distribute.pc_aggpage_search_result.none-task-blog-2~all~sobaiduweb~default-1-108380226&spm=3001.4430
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
@Controller("user")
@RequestMapping("/user")
public class UserController extends BaseController{

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;


    //用户登陆接口
    @RequestMapping(value = "/login",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name="telphone") String telphone,
                                  @RequestParam(name="password") String password) throws Exception {
        if(org.apache.commons.lang3.StringUtils.isEmpty(telphone)||
                StringUtils.isEmpty(password)){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"用户名密码不能为空");
        }

        //用户登陆服务，检查用户登陆是否合法
        UserModel userModel = userService.validateLogin(telphone, this.EncodeByMd5(password));

        //将登陆凭证加入到用户登陆成功的session内
        // --->
        //修改为若用户登陆验证成功后将对应的登陆信息和登陆凭证一起存入redis中

        //生成登陆凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-","");

        //建立token和用户登陆态之间的联系
        redisTemplate.opsForValue().set(uuidToken,userModel);
        //1个小时后该用户的uuid在redis中过期，也就是一个小时候需要重新登陆
        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);

//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        //下发token
        return CommonReturnType.create(uuidToken);
    }


    //用户注册接口
    @RequestMapping(value = "/register",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name="telphone") String telphone,
                                     @RequestParam(name="optCode")String optCode,
                                     @RequestParam(name="name")String name,
                                     @RequestParam(name="gender")Integer gender,
                                     @RequestParam(name="age")Integer age,
                                     @RequestParam(name="password")String password) throws Exception {
        //验证手机号和对应的optCode相符合，下面getOtp存了(tel,opt)键值对
        String inSessionOptCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        if(!com.alibaba.druid.util.StringUtils.equals(optCode,inSessionOptCode)){
            throw new BusinessException(EMBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码错误");
        }
        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setTelphone(telphone);
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setEncrptPassword(this.EncodeByMd5(password));
        userModel.setRegisterMode("byphone");
        //System.out.println(MD5Encoder.encode(password.getBytes()));
        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    public String EncodeByMd5(String str) throws Exception{
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();

        //加密字符串
        String newStr = base64Encoder.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        return newStr;
    }

    @RequestMapping(value = "/getOpt",method = RequestMethod.POST,consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name="telphone") String telphone){

        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String optCode = String.valueOf(randomInt);

        //将opt验证码同用户对应手机号关联，使用httpsession的方式绑定它的手机号与OPTCODE
        httpServletRequest.getSession().setAttribute(telphone,optCode);

        //将opt验证码通过短信通道发送给用户
        System.out.println("telephone=" + telphone +" & optCode=" + optCode);

        return CommonReturnType.create(optCode);
    }

    @RequestMapping("/getUsr")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //若获取的对应用户信息不存在
        if(userModel == null){
            throw new BusinessException(EMBusinessError.USER_NOT_EXIST);
        }

        //将核心领域模型对象转换为可供用户使用的viewObject
        UserVO userVO = convertFromUserModel(userModel);

        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromUserModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }


}
