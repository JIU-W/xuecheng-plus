package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    /**
     * @param shardTotal 分片总数
     * @param shardIndex 分片序号
     * @param count      任务数
     * @return java.util.List<com.xuecheng.media.model.po.MediaProcess>
     * @description 根据分片参数获取待处理任务(保证多个执行器查询到的待处理视频记录不重复)
     * @author JIU-W
     */
    @Select("select * from media_process mp where mp.id % #{shardTotal} = #{shardIndex} " +
            "and (mp.status = '1' or mp.status = '3') " +
            "and mp.fail_count < 3 limit #{count}")
    List<MediaProcess> selectListByShardIndex(@Param("shardTotal") int shardTotal,
                                              @Param("shardIndex") int shardIndex,
                                              @Param("count") int count);


    /**
     * 开启一个任务(使用数据库的乐观锁)
     * @param id 任务id
     * @return 更新记录数
     */
    @Update("update media_process m set m.status = '4' where (m.status = '1' or m.status = '3') " +
            "and m.fail_count < 3 " +
            "and m.id = #{id}")
    int startTask(@Param("id") long id);


}
