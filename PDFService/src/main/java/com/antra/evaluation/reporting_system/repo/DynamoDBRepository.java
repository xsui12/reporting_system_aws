package com.antra.evaluation.reporting_system.repo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//@Repository
public interface DynamoDBRepository extends CrudRepository<PDFFile, String> {

//    Optional<PDFFile> getFileByFileId(String id);
//
//    PDFFile saveFile(PDFFile pdfFile);
//
//    PDFFile deleteFile(String id);
//
//    List<PDFFile> getFiles();
//
//    void updateFile(String id, PDFFile pdfFile);

}
