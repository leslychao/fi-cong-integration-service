package ru.metlife.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.metlife.integration.service.DocumentExportService;

@RestController
public class DocumentExportController {

  @Autowired
  private DocumentExportService documentExportService;

  @RequestMapping(value = "/exportDocument")
  public String exportDocument() {
    documentExportService.exportDocument();
    return "ok";
  }

  @RequestMapping(value = "/updateDeliveryStatus")
  public String updateDeliveryStatus() {
    documentExportService.updateDeliveryStatus();
    return "ok";
  }
}
