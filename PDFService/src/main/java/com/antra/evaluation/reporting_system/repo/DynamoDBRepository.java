package com.antra.evaluation.reporting_system.repo;

import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DynamoDBRepository extends CrudRepository<PDFFile, String> {

    Optional<PDFFile> getFileByFileId(String id);

    PDFFile saveFile(PDFFile pdfFile);

    PDFFile deleteFile(String id);

    List<PDFFile> getFiles();

    void updateFile(String id, PDFFile pdfFile);

}
