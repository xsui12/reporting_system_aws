package com.antra.evaluation.reporting_system.repo;

import com.antra.evaluation.reporting_system.pojo.report.PDFFile;

import java.util.List;
import java.util.Optional;

public interface DynamoDBRepository {

    Optional<PDFFile> getFileById(String id);

    PDFFile saveFile(PDFFile pdfFile);

    PDFFile deleteFile(String id);

    List<PDFFile> getFiles();

    String updateFile(String id, PDFFile pdfFile);


}
