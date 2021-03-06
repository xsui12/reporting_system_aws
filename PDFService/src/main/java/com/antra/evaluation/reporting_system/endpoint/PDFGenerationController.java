package com.antra.evaluation.reporting_system.endpoint;

import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.api.PDFResponse;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.service.PDFService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PDFGenerationController {

    private static final Logger log = LoggerFactory.getLogger(PDFGenerationController.class);

    private static final String DOWNLOAD_API_URI = "/pdf/{id}/content";

    @Value("${client.localhost}")
    String localhost;

    PDFService pdfService;

    @Autowired
    public PDFGenerationController(PDFService pdfService) {
        this.pdfService = pdfService;
    }


    @PostMapping("/pdf")
    @ApiOperation("Generate PDF")
    public ResponseEntity<PDFResponse> createPDF(@RequestBody @Validated PDFRequest request) {
        log.info("Got request to generate PDF: {}", request);

        PDFResponse response = new PDFResponse();
        PDFFile file = null;
        response.setReqId(request.getReqId());

        try {
            file = pdfService.createPDF(request);
            response.setFileId(file.getFileId());
            response.setFileLocation(file.getFileLocation());
            response.setFileSize(file.getFileSize());

            log.info("Generated: {}", file);
        } catch (Exception e) {
            response.setFailed(true);
            log.error("Error in generating pdf", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String generateFileDownloadLink(String fileId) {
        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http").host(localhost).path(DOWNLOAD_API_URI)
                .buildAndExpand(fileId);
        return uriComponents.toUriString();
    }

    @GetMapping("/pdf")
    @ApiOperation("List all existing files")
    public ResponseEntity<List<PDFResponse>> listPDFs() {
        log.debug("Got Request to List All Files");
        List<PDFFile> fileList = pdfService.getPDFList();
        var responseList = fileList.stream().map(file -> {
            PDFResponse response = new PDFResponse();
            BeanUtils.copyProperties(file, response);
            response.setFileLocation(this.generateFileDownloadLink(file.getFileLocation()));
            return response;
        }).collect(Collectors.toList());
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }
    @GetMapping(DOWNLOAD_API_URI)
    @ApiOperation("Get pdf content")
    public void downloadpdf(@PathVariable String id, HttpServletResponse response) throws IOException {
        log.debug("Got Request to Download File:{}", id);
        InputStream fis = pdfService.getPDFBodyById(id);
        String fileName = id + ".pdf";
        response.setHeader("Content-Type", "application/.pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        FileCopyUtils.copy(fis, response.getOutputStream());
        log.debug("Downloaded File:{}", id);
    }

    @DeleteMapping("/pdf/{id}")
    @ApiOperation("Delete PDF")
    public ResponseEntity<PDFResponse> deletePDF(@PathVariable String id) throws FileNotFoundException {
        log.debug("Got Request to Delete File:{}", id);
        var response = new PDFResponse();
        PDFFile fileDeleted = pdfService.deleteFile(id);
        BeanUtils.copyProperties(fileDeleted, response);
        response.setFileLocation(fileDeleted.getFileLocation());
        log.debug("File Deleted");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/pdf/{id}")
    @ApiOperation("Update PDF")
    public ResponseEntity<PDFResponse> updateFile(@PathVariable String id, PDFRequest request) throws FileNotFoundException {
        log.debug("Got Request to Update File:{}", id);
        var response = new PDFResponse();
        PDFFile fileUpdated = pdfService.updateFile(id, request);
        BeanUtils.copyProperties(fileUpdated, response);
        response.setFileLocation(fileUpdated.getFileLocation());
        log.debug("File Updated");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    //@PatchMapping
}
