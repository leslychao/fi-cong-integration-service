package ru.metlife.integration.service;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.metlife.integration.util.CommonUtils.andLogFilteredOutValues;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.metlife.integration.dto.DictionaryDto;
import ru.metlife.integration.dto.RecipientDto;
import ru.metlife.integration.service.xssf.DictionaryRowContentCallback;
import ru.metlife.integration.service.xssf.XlsService;
import ru.metlife.integration.service.xssf.XlsService.SheetData;
import ru.metlife.integration.util.CommonUtils;

@Service
@Slf4j
public class DictionaryService {

  @Value("${fi-cong-integration.doc-file-path}")
  private String docFilePath;

  private XlsService xlsService;

  @PostConstruct
  public void init() {
    xlsService = new XlsService(docFilePath);
  }

  List<DictionaryDto> toDictionary(XlsService.SheetData dictionarySheetData) {
    return Optional.ofNullable(dictionarySheetData.getData())
        .orElse(emptyList())
        .stream()
        .map(this::toDictionary)
        .collect(toList());
  }

  DictionaryDto toDictionary(Map<String, String> dictionaryFromXls) {
    String number = getStringCellValue(dictionaryFromXls, "№");
    String partner = getStringCellValue(dictionaryFromXls, "Партнер");
    String dealership = getStringCellValue(dictionaryFromXls, "Дилерский центр");
    String region = getStringCellValue(dictionaryFromXls, "Регион");
    String email = getStringCellValue(dictionaryFromXls, "e-mail");
    String emailCC = getStringCellValue(dictionaryFromXls, "e-mail копия");

    DictionaryDto dictionaryDto = new DictionaryDto();
    dictionaryDto.setNumber(number);
    dictionaryDto.setPartner(partner);
    dictionaryDto.setDealership(dealership);
    dictionaryDto.setRegion(region);
    dictionaryDto.setEmail(email);
    dictionaryDto.setEmailCC(emailCC);
    return dictionaryDto;
  }

  public SheetData processSheet() {
    return xlsService
        .processSheet("Справочник", 0, 3, new DictionaryRowContentCallback());
  }

  public List<RecipientDto> getRecipientsFromDictionary(SheetData dictionarySheetData, int hash) {
    List<RecipientDto> recipients = new ArrayList<>();
    try {
      DictionaryDto dictionaryDto = toDictionary(dictionarySheetData)
          .stream()
          .filter(
              d -> hash == getOrderIndependentHash(d.getRegion(), d.getPartner(), d.getDealership())
                  && isNotBlank(d.getEmail())
          )
          .findFirst()
          .orElse(null);
      if (!isNull(dictionaryDto)) {
        List<RecipientDto> mainRecipients = stream(dictionaryDto.getEmail().split("\\s*;\\s*"))
            .map(CommonUtils::formatContactString)
            .filter(
                andLogFilteredOutValues(CommonUtils::isEmailValid,
                    s -> log.warn("invalid e-mail {} № {}", s, dictionaryDto.getNumber())))
            .map(s -> new RecipientDto(s, dictionaryDto.getEmailCC(), true))
            .collect(toList());
        if (!mainRecipients.isEmpty()) {
          recipients.addAll(mainRecipients);
          stream(dictionaryDto.getEmailCC().split("\\s*;\\s*"))
              .map(CommonUtils::formatContactString)
              .filter(
                  andLogFilteredOutValues(CommonUtils::isEmailValid,
                      s -> log.warn("invalid e-mail копия {} № {}", s, dictionaryDto.getNumber())))
              .map(s -> new RecipientDto(s, dictionaryDto.getEmailCC()))
              .collect(toCollection(() -> recipients));
        }
      }
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    }
    return recipients;
  }

}
