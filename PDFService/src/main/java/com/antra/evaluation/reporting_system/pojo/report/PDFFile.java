package com.antra.evaluation.reporting_system.pojo.report;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.LocalDateTime;

@DynamoDBTable(tableName = "pdfFile")
//@Document
public class PDFFile {
    private String fileId;
    private String fileName;
    private String fileLocation;
    private String submitter;
    private Long fileSize;
    private String description;
    private LocalDateTime generatedTime;

    @DynamoDBHashKey(attributeName ="id")
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @DynamoDBAttribute
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @DynamoDBAttribute
    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    @DynamoDBAttribute
    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    @DynamoDBAttribute
    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    @DynamoDBAttribute
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDBAttribute
    public LocalDateTime getGeneratedTime() {
        return generatedTime;
    }

    public void setGeneratedTime(LocalDateTime generatedTime) {
        this.generatedTime = generatedTime;
    }

    @Override
    public String toString() {
        return "PDFFile{" +
                "id='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileLocation='" + fileLocation + '\'' +
                ", submitter='" + submitter + '\'' +
                ", fileSize=" + fileSize +
                ", description='" + description + '\'' +
                ", generatedTime=" + generatedTime +
                '}';
    }
}
