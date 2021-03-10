package com.antra.report.client.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.report.client.entity.ExcelReportEntity;
import com.antra.report.client.entity.PDFReportEntity;
import com.antra.report.client.entity.ReportRequestEntity;
import com.antra.report.client.entity.ReportStatus;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.EmailType;
import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.ExcelResponse;
import com.antra.report.client.pojo.reponse.PDFResponse;
import com.antra.report.client.pojo.reponse.ReportVO;
import com.antra.report.client.pojo.reponse.SqsResponse;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.repository.ReportRequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    @Value("${email}")
    String to;
    private final ReportRequestRepo reportRequestRepo;
    private final SNSService snsService;
    private final AmazonS3 s3Client;
    private final EmailService emailService;

    //TODO: Moving AmazonS3 to PDFService could not autowire AmazonS3
    public ReportServiceImpl(ReportRequestRepo reportRequestRepo, SNSService snsService, AmazonS3 s3Client, EmailService emailService) {
        this.reportRequestRepo = reportRequestRepo;
        this.snsService = snsService;
        this.s3Client = s3Client;
        this.emailService = emailService;
    }

    private ReportRequestEntity persistToLocal(ReportRequest request) {
        request.setReqId("Req-" + UUID.randomUUID().toString());

        ReportRequestEntity entity = new ReportRequestEntity();
        entity.setReqId(request.getReqId());
        entity.setSubmitter(request.getSubmitter());
        entity.setDescription(request.getDescription());
        entity.setCreatedTime(LocalDateTime.now());

        PDFReportEntity pdfReport = new PDFReportEntity();
        pdfReport.setRequest(entity);
        pdfReport.setStatus(ReportStatus.PENDING);
        pdfReport.setCreatedTime(LocalDateTime.now());
        entity.setPdfReport(pdfReport);     //To store report in DB

        ExcelReportEntity excelReport = new ExcelReportEntity();
        BeanUtils.copyProperties(pdfReport, excelReport);
        entity.setExcelReport(excelReport);

        return reportRequestRepo.save(entity);
    }

    private void sendDirectRequests(ReportRequest request) {
        Callable<Void> callExcel = () -> {sendExcelRequests(request); return null; };
        Callable<Void> callPDF = () -> {sendPDFRequests(request); return null; };
        List<Callable<Void>> taskList = new ArrayList<>();
        taskList.add(callExcel);
        taskList.add(callPDF);
        try {
            pool.invokeAll(taskList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void sendExcelRequests(ReportRequest request) {
        RestTemplate rs = new RestTemplate();
        ExcelResponse excelResponse = new ExcelResponse();
        try {
            excelResponse = rs.postForEntity("http://localhost:8888/excel", request, ExcelResponse.class).getBody();
        } catch(Exception e){
            log.error("Excel Generation Error (Sync) : e", e);
            excelResponse.setReqId(request.getReqId());
            excelResponse.setFailed(true);
        } finally {
            updateLocal(excelResponse);
        }
    }
    private void sendPDFRequests(ReportRequest request) {
        RestTemplate rs = new RestTemplate();
        PDFResponse pdfResponse = new PDFResponse();
        try {
            pdfResponse = rs.postForEntity("http://localhost:9999/pdf", request, PDFResponse.class).getBody();
        } catch(Exception e){
            log.error("PDF Generation Error (Sync) : e", e);
            pdfResponse.setReqId(request.getReqId());
            pdfResponse.setFailed(true);
        } finally {
            updateLocal(pdfResponse);
        }
    }

    private void updateLocal(ExcelResponse excelResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(excelResponse, response);
        updateAsyncExcelReport(response);
    }
    private void updateLocal(PDFResponse pdfResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(pdfResponse, response);
        updateAsyncPDFReport(response);
    }

    @Override
    public ReportVO generateReportsSync(ReportRequest request) {
        persistToLocal(request);
        sendDirectRequests(request);
        return new ReportVO(reportRequestRepo.findById(request.getReqId()).orElseThrow());
    }

    @Override
    @Transactional
    public ReportVO generateReportsAsync(ReportRequest request) {
        ReportRequestEntity entity = persistToLocal(request);
        snsService.sendReportNotification(request);
        log.info("Send SNS the message: {}", request);
        return new ReportVO(entity);
    }

    @Override
//    @Transactional // why this? email could fail
    public void updateAsyncPDFReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var pdfReport = entity.getPdfReport();
        pdfReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            pdfReport.setStatus(ReportStatus.FAILED);
        } else {
            pdfReport.setStatus(ReportStatus.COMPLETED);
            pdfReport.setFileId(response.getFileId());
            pdfReport.setFileLocation(response.getFileLocation());
            pdfReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);

        emailService.sendEmail(to, EmailType.SUCCESS, entity.getSubmitter());
    }

    @Override
//    @Transactional
    public void updateAsyncExcelReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var excelReport = entity.getExcelReport();
        excelReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            excelReport.setStatus(ReportStatus.FAILED);
        } else {
            excelReport.setStatus(ReportStatus.COMPLETED);
            excelReport.setFileId(response.getFileId());
            excelReport.setFileLocation(response.getFileLocation());
            excelReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        emailService.sendEmail(to, EmailType.SUCCESS, entity.getSubmitter());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVO> getReportList() {
        return reportRequestRepo.findAll().stream().map(ReportVO::new).collect(Collectors.toList());
    }

    @Override
    public InputStream getFileBodyByReqId(String reqId, FileType type) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        if (type == FileType.PDF) {//TODO:Redirect to localhost:9999/...
            String fileLocation = entity.getPdfReport().getFileLocation(); // this location is s3 "bucket/key"
            String bucket = fileLocation.split("/")[0];
            String key = fileLocation.split("/")[1];
            return s3Client.getObject(bucket, key).getObjectContent();
        } else if (type == FileType.EXCEL) {
            String fileId = entity.getExcelReport().getFileId();
//            String fileLocation = entity.getExcelReport().getFileLocation();
//            try {
//                return new FileInputStream(fileLocation);// this location is in local, definitely sucks
//            } catch (FileNotFoundException e) {
//                log.error("No file found", e);
//            }
            RestTemplate restTemplate = new RestTemplate();
//            InputStream is = restTemplate.execute(, HttpMethod.GET, null, ClientHttpResponse::getBody, fileId);
            ResponseEntity<Resource> exchange = restTemplate.exchange("http://localhost:8888/excel/{id}/content",
                    HttpMethod.GET, null, Resource.class, fileId);
            try {
                return exchange.getBody().getInputStream();
            } catch (IOException e) {
                log.error("Cannot download excel", e);
            }
        }
        return null;
    }

    @Override
    public void deleteReport(String reqId) throws FileNotFoundException {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        RestTemplate restTemplate = new RestTemplate();
        String id;
        if (entity == null){
            throw new FileNotFoundException();
        }
        var pdfReport = entity.getPdfReport();
        id = entity.getPdfReport().getFileId();
        restTemplate.delete("http://localhost:9999/pdf/{id}", id);
        pdfReport.setStatus(ReportStatus.DELETED);

        var excelReport = entity.getExcelReport();
        id = entity.getExcelReport().getFileId();
        restTemplate.delete("http://localhost:8888/excel/{id}", id);
        excelReport.setStatus(ReportStatus.DELETED);
    }

    @Override
    public ReportRequest findReportRequestByReqId(String reqId) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReqId(entity.getReqId());
        return reportRequest;
    }

}
