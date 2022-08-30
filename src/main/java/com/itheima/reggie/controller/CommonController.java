package com.itheima.reggie.controller;


import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    @PostMapping("/upload")
    // the parameter name has to be the same with the name of the uploaded file!
    public R<String> upload(MultipartFile file) {
        log.info("上傳圖片！");
        // have to be restore to other places, otherwise, it will be removed;

        String originalFilename = file.getOriginalFilename();
        // always use UUID to generate a new name in order to prevent from the duplicated name!
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uid = UUID.randomUUID().toString();
        String newFileName = uid + suffix;
        File baseFile = new File(basePath);
        if (!baseFile.exists())  {
            baseFile.mkdirs();
        }
        try {
            file.transferTo(new File(basePath + newFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return R.success(newFileName);
    }

    /**
     * need response to bring the stream of the imag to the front end!
     * @param name
     * @param response
     */

    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        FileInputStream fileInputStream = null;
        ServletOutputStream outputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(basePath + name));
            outputStream = response.getOutputStream();
            //log.info("outputStream {}, inputStream {}", outputStream, fileInputStream);
            response.setContentType("image/jpeg");
            byte[] bytes = new byte[1024];
            int len = 0;
            while((len = fileInputStream.read(bytes))!= -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }
            fileInputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
