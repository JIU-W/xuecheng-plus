package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;

/**
 * @author JIU-W
 * @version 1.0
 * @description 我的课程表service接口
 * @date 2024-12-06
 */
public interface MyCourseTablesService {

    /**
     * @param userId   用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
     * @description 添加选课
     * @author Mr.M
     * @date 2022/10/24 17:33
     */
    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * @description 判断学习资格  获取学习资格
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @author Mr.M
     * @date 2022/10/3 7:37
     */
    XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    /**
     * @description 更新选课记录表，向我的课程表插入记录
     * @param chooseCourseId
     * @return
     */
    boolean saveChooseCourseSuccess(String chooseCourseId);

}
