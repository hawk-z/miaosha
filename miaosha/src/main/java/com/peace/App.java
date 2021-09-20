package com.peace;

/**
 * Hello world!
 *
 */

import com.peace.dao.UserDoMapper;
import com.peace.dataobject.UserDo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {"com.peace"})
@RestController
@MapperScan("com.peace.dao")
public class App 
{
    @Autowired
    private UserDoMapper userDoMapper;

    @RequestMapping("/")
    public String home(){
        UserDo userDo = userDoMapper.selectByPrimaryKey(1);
        if(userDo == null){
            return "用户对象不存在";
        }else{
            return userDo.getName();
        }
    }

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        SpringApplication.run(App.class,args);
    }
}
