/*
package ru.metlife.integration.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static ru.metlife.integration.util.CommonUtils.getOrderIndependentHash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.metlife.integration.dto.RecipientDto;

@RunWith(SpringJUnit4ClassRunner.class)
public class DictionaryServiceTest {

  @Mock
  private XlsService xlsService;

  @InjectMocks
  private DictionaryService dictionaryService;

  private SheetData prepareSheetData() {

    Map<String, Object> dictionaryWithValidEmails = new HashMap<>();
    dictionaryWithValidEmails.put("Партнер", "partner1");
    dictionaryWithValidEmails.put("Дилерский центр", "dealership1");
    dictionaryWithValidEmails.put("Регион", "region1");
    dictionaryWithValidEmails.put("e-mail", "test_test@test.com");
    dictionaryWithValidEmails.put("e-mail копия", " test1_test1@test.com; "
        + " te2st.te2st@test.com; ");

    Map<String, Object> dictionaryWithOneInvalidEmailCC = new HashMap<>();
    dictionaryWithOneInvalidEmailCC.put("Партнер", "partner2");
    dictionaryWithOneInvalidEmailCC.put("Дилерский центр", "dealership2");
    dictionaryWithOneInvalidEmailCC.put("Регион", "region2");
    dictionaryWithOneInvalidEmailCC.put("e-mail", "ivanov.ivan@test.com");
    dictionaryWithOneInvalidEmailCC.put("e-mail копия", "Ivanov1, Ivan1 <ivanov1_ivan1@test.com>; "
        + "Incorrect, Email <incorrect.email.test.com>; "
        + " Ivanov2, Ivan2<ivanov2.ivan2@test.com>; "
        + "ivanov3.ivan3@test.com ; ");

    Map<String, Object> dictionaryWithInvalidEmail = new HashMap<>();
    dictionaryWithInvalidEmail.put("Партнер", "partner3");
    dictionaryWithInvalidEmail.put("Дилерский центр", "dealership3");
    dictionaryWithInvalidEmail.put("Регион", "region3");
    dictionaryWithInvalidEmail.put("e-mail", "petr_petrov.test.com");
    dictionaryWithInvalidEmail.put("e-mail копия", " petr1_petrov1@test.com; "
        + " petr2.petrov2@test.com; ");

    Map<String, Object> dictionaryWithEmptyEmail = new HashMap<>();
    dictionaryWithEmptyEmail.put("Партнер", "partner4");
    dictionaryWithEmptyEmail.put("Дилерский центр", "dealership4");
    dictionaryWithEmptyEmail.put("Регион", "region4");
    dictionaryWithEmptyEmail.put("e-mail", "");
    dictionaryWithEmptyEmail.put("e-mail копия", " jon1_snow1@test.com; "
        + " jon2.snow2@test.com; ");

    SheetData sheetData = new SheetData();
    sheetData.setData(Arrays.asList(
        dictionaryWithValidEmails,
        dictionaryWithOneInvalidEmailCC));

    return sheetData;
  }

  @Before
  public void setup() {
    when(xlsService.processSheet("Справочник", 3))
        .thenReturn(prepareSheetData());
  }

  @Test
  public void testDictionaryWithValidEmails() {
    int hash = getOrderIndependentHash("partner1", "dealership1", "region1");
    List<RecipientDto> recipients = dictionaryService.getRecipientsFromDictionary(hash);
    assertThat(recipients, hasItems(
        hasProperty("email", is("test_test@test.com")),
        hasProperty("email", is("test1_test1@test.com")),
        hasProperty("email", is("te2st.te2st@test.com"))
    ));
  }

  @Test
  public void testDictionaryWithOneInvalidEmailCC() {
    int hash = getOrderIndependentHash("partner2", "dealership2", "region2");
    List<RecipientDto> recipients = dictionaryService.getRecipientsFromDictionary(hash);
    assertThat(recipients, hasItems(
        hasProperty("email", is("ivanov.ivan@test.com")),
        hasProperty("email", is("ivanov1_ivan1@test.com")),
        hasProperty("email", is("ivanov2.ivan2@test.com")),
        hasProperty("email", is("ivanov3.ivan3@test.com"))
    ));
    assertThat(recipients, not(hasItem(
        hasProperty("email", is("incorrect.email.test.com"))
    )));
  }

  @Test
  public void testDictionaryWithInvalidEmail() {
    int hash = getOrderIndependentHash("partner3", "dealership3", "region3");
    List<RecipientDto> recipients = dictionaryService.getRecipientsFromDictionary(hash);
    assertThat(recipients, not(hasItems(
        hasProperty("email", is("petr_petrov.test.com")),
        hasProperty("email", is("petr1_petrov1@test.com")),
        hasProperty("email", is("petr2.petrov2@test.com"))
    )));
    assertThat(recipients, hasSize(0));
  }

  @Test
  public void testDictionaryWithEmptyEmail() {
    int hash = getOrderIndependentHash("partner4", "dealership4", "region4");
    List<RecipientDto> recipients = dictionaryService.getRecipientsFromDictionary(hash);
    assertThat(recipients, not(hasItems(
        hasProperty("email", is("jon1_snow1@test.com")),
        hasProperty("email", is("jon2.snow2@test.com"))
    )));
    assertThat(recipients, hasSize(0));
  }
}*/
