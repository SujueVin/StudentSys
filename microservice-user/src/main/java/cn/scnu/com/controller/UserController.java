package cn.scnu.com.controller;

import cn.scnu.com.pojo.Students;
import cn.scnu.com.pojo.Teachers;
import cn.scnu.com.pojo.Users;
import cn.scnu.com.service.UserService;
import cn.scnu.com.util.Result.Result;
import cn.scnu.com.utils.CookieUtils;
import cn.scnu.com.vo.changePwdVo;
import cn.scnu.com.vo.stuVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;

/**
 * @ClassName UserController
 * @Description
 * @date 2022/7/13 11:49
 * @Version 1.0
 * @Author HJW
 */
@RestController
@RequestMapping("user/manage")
@CrossOrigin
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 测试
     * @return 响应体
     */
    @RequestMapping("/test")
    public Result test(String major){
        Integer majorId = userService.getMajorId(major);
        return Result.success(majorId);
    }

    /**
     * 登录 并在cookie中存入 Ticket
     * @Param user 用户账号（学生学号）和密码
     * @return Result 用户角色（学生，老师，管理员）
     */
    @RequestMapping("login")
    public Result login(@RequestBody Users user, HttpServletRequest request, HttpServletResponse response){
        //调用业务确定合法并且存储数据
        String ticket = userService.login(user);
        //控制层将业务层存储登录成功的rediskey值
        if(!"".equals(ticket)&&ticket!=null){
            //登录成功则返回用户角色和ticket
            System.out.println(userService.queryUserRoleByAccount(user.getAccount())+"-"+ticket);
            return Result.success(userService.queryUserRoleByAccount(user.getAccount())+"-"+ticket);
        }else{
            return Result.error("登录失败");
        }
    }

    /**
     * 通过cookie查询正在登录的用户
     * @return 响应体（用户信息或者没有登录）
     */
    @RequestMapping("query/{ticket}")
    public Result getUser(@PathVariable String ticket){
        String userJson = userService.queryUserJson(ticket);
        if(userJson!=null&&!"".equals(userJson)){
            return Result.success(userJson);
        }else{
            return Result.error("您还没有登录");
        }
    }

    /**
     * 登出 把cookie中的Ticket清空
     * @return 响应体（是否成功）
     */
    @RequestMapping("logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response){

//        CookieUtils.deleteCookie(request, response,"Ticket");
        return Result.success("成功退出");
    }

    /**
     * 修改密码,登录状态下通过request后去到前端的cookie，只需要在请求体传信旧密码即可
     * @return 响应体（是否成功）
     */
    @RequestMapping("changePwd/{cookie}")
    public Result changePwd(@RequestBody changePwdVo vo, @PathVariable String cookie){
//        String cookie = CookieUtils.getCookieValue(request,"Ticket");
        if(cookie.equals("")||cookie == null){
             return Result.error("您还未登录！");
        }
        String account = cookie.split("_")[2];
        System.out.println(account);
        if(userService.changePwd(vo.getOldPwd(),vo.getNewPwd(), Integer.valueOf(account))){
            return Result.success("修改成功");
        }else{
            return Result.error("旧密码输入错误");
        }
    }

    /**
     * 获取学生个人信息，通过cookie
     * @return 响应体（学生详细信息）
     */
    @RequestMapping("getStuInfo/{cookie}")
    public Result getStuInfo(@PathVariable String cookie){
//        String cookie = CookieUtils.getCookieValue(request,"Ticket");
        if(cookie.equals("")||cookie == null){
            return Result.error("您还未登录！");
        }
        String account = cookie.split("_")[2];
        return Result.success(userService.queryStuDetailsByAccount(Integer.valueOf(account)));
    }

    /**
     * 学生 修改学生个人信息 学生只能修改description
     * @param description 学生个人描述
     * @return 响应体（成功修改）
     */
    @RequestMapping("stuChangeStuInfo/{cookie}")
    public Result changeStuInfoByStu(String description,@PathVariable String cookie){
//        String cookie = CookieUtils.getCookieValue(request,"Ticket");
        if(cookie.equals("")||cookie == null){
            return Result.error("您还未登录！");
        }
        String account = cookie.split("_")[2];
//        System.out.println(description);
        userService.changeStuInfoByStu(description, Integer.valueOf(account));
        return Result.success("修改成功");
    }

    /**
     * 管理员修改学生个人信息
     * @param stuVo 学生信息传输单元
     *              studentId(学号),name(),description(学生个人描述),major(学生专业名称)
     *              gradeId(年级),semester(学期),timeEnrollment(入学时间)
     * @return 响应体（修改成功）
     */
    @RequestMapping("adminChangeStuInfo")
    public Result changeStuInfoByAdmin(@RequestBody stuVo stuVo){
        String result = userService.changeStuInfoByAdmin(stuVo);
        System.out.println(stuVo);
        return Result.success(result);
    }

    /**
     * 管理员 批量导入学生信息，并生成账号
     * @param studentsJson 前端传来的学生信息（Json 对象数组）
     * @return 响应体（导入情况）
     */
    @RequestMapping("addStu")
    public Result addStu(@RequestBody JSONObject studentsJson){
        JSONArray jsonArray = studentsJson.getJSONArray("studentsList");
        List<stuVo> list = jsonArray.toJavaList(stuVo.class);
        String result = userService.insertStudent(list);

//        System.out.println("-----------------------------------");
//        for(int i=0;i<list.size();i++){
//            System.out.println(list.get(i));
//        }
//        System.out.println("-----------------------------------");

        return Result.success(result);
    }

    /**
     * 管理员批量导入老师信息
     * @param teachersJson 老师信息对象对象数组（JSON）
     * @return 导入情况
     */
    @RequestMapping("addTeacher")
    public Result addTea(@RequestBody JSONObject teachersJson){
        JSONArray jsonArray = teachersJson.getJSONArray("teacherList");
        List<Teachers> list = jsonArray.toJavaList(Teachers.class);
        String result = userService.insertTeacher(list);
        return Result.success(result);
    }
}