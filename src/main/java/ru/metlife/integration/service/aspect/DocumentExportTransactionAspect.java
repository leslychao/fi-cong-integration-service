package ru.metlife.integration.service.aspect;


import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getFile;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class DocumentExportTransactionAspect {

  @Value(value = "${fi-cong-integration.docs-file-path}")
  private String docFilePath;

  @AfterThrowing("execution(* ru.metlife.integration.service.DocumentExportService.exportDocument(..))")
  public void rollbackDockFile() throws IOException {
    log.info("rollbackDockFile: Started rollback");
    File file = getFile(docFilePath);
    deleteQuietly(file);
    File backupFile = getFile(docFilePath + ".bkp");
    if (backupFile.exists()) {
      try (FileInputStream fileInputStream = new FileInputStream(
          backupFile); FileOutputStream fileOutputStream = new FileOutputStream(file);) {
        copy(fileInputStream, fileOutputStream);
      }
      log.info("rollbackDockFile: Successfully restored {} from backup file {}",
          file.getAbsolutePath(), backupFile.getAbsolutePath());
    } else {
      log.warn("rollbackDockFile: backup file does not exists {}", backupFile.getAbsolutePath());
    }
  }
}
