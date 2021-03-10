package com.antra.evaluation.reporting_system.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.repo.DynamoDBRepository;
import com.antra.evaluation.reporting_system.repo.PDFRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PDFServiceImpl implements PDFService {

    private static final Logger log = LoggerFactory.getLogger(PDFServiceImpl.class);

    private final PDFRepository repository;
    //private final DynamoDBRepository repository;
    private final PDFGenerationServiceImpl generator;

    private final AmazonS3 s3Client;

    @Value("${s3.bucket}")
    private String s3Bucket;

    @Autowired
    public PDFServiceImpl(PDFRepository repository, PDFGenerationServiceImpl generator, AmazonS3 s3Client) {
        this.repository = repository;
        this.generator = generator;
        this.s3Client = s3Client;
    }

    @Override
    public PDFFile createPDF(final PDFRequest request) {
        PDFFile file = new PDFFile();
        file.setFileId("File-" + UUID.randomUUID().toString());
        file.setSubmitter(request.getSubmitter());
        file.setDescription(request.getDescription());
        file.setGeneratedTime(LocalDateTime.now());

        PDFFile generatedFile= generator.generate(request);

        File temp = new File(generatedFile.getFileLocation());
        log.debug("Upload temp file to s3 {}", generatedFile.getFileLocation());
        s3Client.putObject(s3Bucket,file.getFileId(),temp);
        log.debug("Uploaded");

        file.setFileLocation(String.join("/",s3Bucket,file.getFileId()));
        file.setFileSize(generatedFile.getFileSize());
        file.setFileName(generatedFile.getFileName());
        repository.save(file);

        log.debug("clear tem file {}", file.getFileLocation());
        if(temp.delete()){
            log.debug("cleared");
        }

        return file;
    }

    @Override
    public InputStream getPDFBodyById(String id) throws FileNotFoundException {
        Optional<PDFFile> fileInfo = repository.findById(id);
        return new FileInputStream(fileInfo.orElseThrow(FileNotFoundException::new).getFileLocation());
    }

    @Override
    public List<PDFFile> getPDFList() {
        return repository.findAll();
    }

    @Override
    public void deleteFile(String id) throws FileNotFoundException {

        Optional<PDFFile> pdfFile = repository.findById(id);
        String pdfLocation = pdfFile.orElseThrow(FileNotFoundException::new).getFileLocation();
        String bucket = pdfLocation.split("/")[0];
        String key = pdfLocation.split("/")[1];
        s3Client.deleteObject(bucket, key);
        repository.deleteById(id);
    }
}
