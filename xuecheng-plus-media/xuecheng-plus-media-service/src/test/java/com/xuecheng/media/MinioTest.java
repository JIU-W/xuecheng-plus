package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;

/**
 * @author JIU-W
 * @version 1.0
 * @description 测试 minio
 * @date 2024-11-20
 */
public class MinioTest {

    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    //上传文件
    @Test
    public void upload() {
        //根据扩展名取出媒体资源类型(mimeType)
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")//确定桶
                    .filename("D://音频视频//1.mp4") //指定本地文件路径
                    //.object("test002.mp4")    //确定文件
                    .object("test1/001/test001.mp4")//添加子目录
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }

    //删除文件
    @Test
    public void delete() {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("testbucket")
                            .object("test002.mp4")
                            .build());
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    //查询文件(从minio中下载文件)
    @Test
    public void getFile() throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("test1/001/test001.mp4")
                .build();

        //查询远程服务获取到一个流对象
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        //指定输出流
        FileOutputStream outputStream =
                new FileOutputStream(new File("D:\\音频视频\\4.mp4"));

        IOUtils.copy(inputStream, outputStream);

        //校验文件的完整性对文件的内容进行md5
        String source_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:/音频视频/video_20240601_175922.mp4")));
        String local_md5 =
                DigestUtils.md5Hex(new FileInputStream(new File("D:\\音频视频\\4.mp4")));
        if (source_md5.equals(local_md5)) {
            System.out.println("下载成功");
        }


    }


}
