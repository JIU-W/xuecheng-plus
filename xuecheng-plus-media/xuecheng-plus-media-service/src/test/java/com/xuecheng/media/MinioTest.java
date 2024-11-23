package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    //将分块文件上传至minio(保证每个分块文件的文件名不变)
    @Test
    public void uploadChunk() {
        String chunkFolderPath = "D:\\音频视频\\bigfile_test\\chunk\\";
        File chunkFolder = new File(chunkFolderPath);
        //分块文件(文件夹中所有文件的File对象数组)
        File[] files = chunkFolder.listFiles();
        //对file对象数组进行排序，使得相同的分块文件在本地和在minio上名称是相同的,
        //也就是确保本地分块文件上传到minio后名字不变。
        //那么才能确保后续在minio上合并文件时，合并分块的顺序是有序的。
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Integer.parseInt(f1.getName()) - Integer.parseInt(f2.getName());
            }
        });
        //将分块文件上传至minio
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String objectName = "chunk/" + i;
            try {
                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                        .bucket("testbucket")
                        .object(objectName)//对象名
                        .filename(file.getAbsolutePath())//本地文件路径
                        .build();
                minioClient.uploadObject(uploadObjectArgs);
                System.out.println("上传分块成功" + i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //将分块文件上传至minio(使用多线程并发上传)
    @Test
    public void uploadChunkConcurrently() {
        String chunkFolderPath = "D:\\音频视频\\bigfile_test\\chunk\\";
        File chunkFolder = new File(chunkFolderPath);
        File[] files = chunkFolder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("没有分块文件需要上传。");
            return;
        }
        //对file对象数组进行排序，使得相同的分块文件在本地和在minio上名称是相同的
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Integer.parseInt(f1.getName()) - Integer.parseInt(f2.getName());
            }
        });

        // 创建一个固定大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String objectName = "chunk/" + i;
            Runnable task = new UploadTask(file, objectName);
            executor.submit(task);
        }

        // 关闭线程池并等待所有任务完成
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 定义上传任务
    class UploadTask implements Runnable {
        private File file;
        private String objectName;

        public UploadTask(File file, String objectName) {
            this.file = file;
            this.objectName = objectName;
        }

        @Override
        public void run() {
            try {
                UploadObjectArgs args = UploadObjectArgs.builder()
                        .bucket("testbucket")
                        .object(objectName)
                        .filename(file.getAbsolutePath())
                        .build();
                minioClient.uploadObject(args);
                System.out.println("上传分块成功: " + objectName);
            } catch (Exception e) {
                e.printStackTrace();
                // 记录失败的上传任务，可以添加重试机制
            }
        }
    }


    //合并文件(minio文件系统要求分块文件最小5MB)
    @Test
    public void test_merge() throws Exception {
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(3)//分块数量为3
                .map(i -> ComposeSource.builder()
                        .bucket("testbucket")
                        .object("chunk/".concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")     //存储桶名称
                .object("merge01.mp4")    //合并后的文件名
                .sources(sources).build();       //源文件
        minioClient.composeObject(composeObjectArgs);
    }
    //Stream迭代(Stream的iterate方法)的时候就是按照从小到大的顺序读取的源文件的。

    //上传和合并的时候都保证排好序，那么小于5MB的文件一定是出现在最后一个分片文件的。
    //而minio允许：合并的时候最后一个文件可以小于5MB的。
    //在上传后检查一下minio中小于5M的文件的文件名的数字是不是最大的。


    //清除分块文件
    @Test
    public void test_removeObjects() {
        //合并分块完成将分块文件清除
        List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                .limit(3)
                .map(i -> new DeleteObject("chunk/".concat(Integer.toString(i))))
                .collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                .bucket("testbucket")
                .objects(deleteObjects)
                .build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        results.forEach(r -> {
            DeleteError deleteError = null;
            try {
                deleteError = r.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


}
