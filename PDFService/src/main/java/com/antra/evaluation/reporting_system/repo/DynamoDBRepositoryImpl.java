package com.antra.evaluation.reporting_system.repo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DynamoDBRepositoryImpl implements DynamoDBRepository{

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Override
    public Optional<PDFFile> getFileById(String id) {
        return Optional.ofNullable(dynamoDBMapper.load(PDFFile.class, id));
    }

    @Override
    public PDFFile saveFile(PDFFile pdfFile) {
        dynamoDBMapper.save(pdfFile);
        return pdfFile;
    }

    @Override
    public PDFFile deleteFile(String id) {
        PDFFile pdfFile = dynamoDBMapper.load(PDFFile.class, id);
        dynamoDBMapper.delete(pdfFile);
        return pdfFile;
    }

    @Override
    public List<PDFFile> getFiles() {
        return new ArrayList<>(dynamoDBMapper.scan(PDFFile.class, new DynamoDBScanExpression()));
    }

    @Override
    public String updateFile(String id, PDFFile pdfFile) {
        dynamoDBMapper.save(
                pdfFile,
                new DynamoDBSaveExpression().withExpectedEntry(
                        "id",
                        new ExpectedAttributeValue(new AttributeValue().withS(id)
                        ))
        );
        return id;
    }
}
