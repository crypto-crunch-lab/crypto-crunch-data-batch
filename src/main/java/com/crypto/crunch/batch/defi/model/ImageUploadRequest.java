package com.crypto.crunch.batch.defi.model;

import com.crypto.crunch.batch.defi.conf.ImageConf;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class ImageUploadRequest {
    private ImageConf.ImageUploadType uploadType;
    private MultipartFile multipartFile;
    private String url;
    private String dirName;
    private String fileName;
}
