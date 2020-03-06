package ru.metlife.integration.service.xssf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.metlife.integration.service.xssf.XlsService.SheetData;

public class TestApp {

  static Map<String, String> statusMap = new HashedMap<>();

  static {
    statusMap.put("LIQUIDATING", "В процессе ликвидации");
    statusMap.put("LIQUIDATED", "Ликвидирована");
    statusMap.put("ACTIVE", "Действующая");
  }

  static Dto createRequest(String searchName, String cladrId) {
    if (StringUtils.isEmpty(cladrId)) {
      return null;
    }
    if (StringUtils.isEmpty(searchName)) {
      return null;
    }
    try {

      DefaultHttpClient httpClient = new DefaultHttpClient();
      HttpPost postRequest = new HttpPost(
          "https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/party");

      StringEntity input = new StringEntity("{\n"
          + "    \"query\": \"" + searchName + "\", \n"
          + "    \"locations\": [{\"kladr_id\": \"" + cladrId + "\"}]\n"
          + "}", "UTF-8");
      input.setContentType("application/json");
      postRequest.setEntity(input);
      postRequest.setHeader("Content-Type", "application/json");
      postRequest.setHeader("Authorization", "Token 72251a27988c2cac9bfabb60364f32576233ec0d");
      postRequest.setHeader("Accept", "application/json");

      HttpResponse response = httpClient.execute(postRequest);

      BufferedReader br = new BufferedReader(
          new InputStreamReader((response.getEntity().getContent()), "UTF-8"));

      String output;
      System.out.println("Output from Server .... \n");
      StringBuilder stringBuilder = new StringBuilder();
      while ((output = br.readLine()) != null) {
        stringBuilder.append(output);
      }

      JSONObject jsonObject = new JSONObject(stringBuilder.toString());
      JSONArray jsonArray = null;
      try {
        jsonArray = jsonObject.getJSONArray("suggestions");
      } catch (JSONException e) {
        return null;
      }

      if (jsonArray.isEmpty()) {
        return null;
      }
      JSONObject obj = (JSONObject) jsonArray.get(0);
      Dto dto = new Dto();
      dto.actualName = obj.getString("value");
      String status = statusMap
          .get(obj.getJSONObject("data").getJSONObject("state").getString("status"));
      if (StringUtils.isEmpty(status)) {
        throw new RuntimeException("status is undefined = " + statusMap
            .get(obj.getJSONObject("data").getJSONObject("state").getString("status")));
      }
      dto.status = status;
      String inn = obj.getJSONObject("data").get("inn").toString();
      if ("null".equals(inn) || StringUtils.isEmpty(inn)) {
        dto.status = "Найдена без ИНН";
      } else {
        dto.inn = inn;
      }

      httpClient.getConnectionManager().shutdown();

      return dto;

    } catch (MalformedURLException e) {

      e.printStackTrace();

    } catch (IOException e) {

      e.printStackTrace();

    }
    return null;
  }

  static Map<String, String> prepareRegions() {
    Map<String, String> cladrIds = new HashMap<>();
    try {
      List<String> regions = IOUtils
          .readLines(new FileInputStream("/Users/vitaliikim/Downloads/change.log"), "UTF-8");
      for (String s : regions) {
        String[] array = s.split(":");
        cladrIds.put(array[0], array[1]);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return cladrIds;
  }

  public static void main(String[] args) {
    XlsService xlsService = new XlsService(
        "/Users/vitaliikim/Downloads/Спарк_без_ИНН_2020_02_21.xlsx");
    SheetData sheetData = xlsService.processSheet("регионы", 2, 3, new TestRowContentCallback());
    List<Map<String, String>> data = sheetData.getData();
    List<Dto> dtoList = new ArrayList<>();
    boolean test = true;
    if (test) {
      for (int i = 0; i < 4000; i++) {
        Map<String, String> map = data.get(i);
        String name = map.get("Наименование_полное");
        String searchName = name == null ? "" : name.replaceAll("\"", "");

        Dto dto = createRequest(searchName, map.get("cladrId"));
        if (dto == null) {
          dto = new Dto();
          dto.status = "Не найдена";
        }
        dto.name = name;
        dto.searchName = searchName;
        dto.rowNum = Integer.valueOf(map.get("rowNum"));

        dtoList.add(dto);
      }
      xlsService.openWorkbook(false);
      for (Dto dto : dtoList) {
        Map<String, String> toUpdate = new HashMap<>();
        toUpdate.put("Код_налогоплательщика", dto.inn);
        toUpdate.put("Статус", dto.status);
        xlsService.updateRow(toUpdate, dto.rowNum);
      }
      xlsService.saveWorkbook(true);
      xlsService.closeWorkbook();
    } else {
      for (Map<String, String> map : data) {
        String name = map.get("Наименование_полное");
        String searchName = name == null ? "" : name.replaceAll("\"", "");

        Dto dto = createRequest(searchName, map.get("cladrId"));
        if (dto == null) {
          dto = new Dto();
          dto.status = "Не найдена";
        }
        dto.name = name;
        dto.searchName = searchName;
        dto.rowNum = Integer.valueOf(map.get("rowNum"));

        dtoList.add(dto);
      }
      xlsService.openWorkbook(false);
      for (Dto dto : dtoList) {
        Map<String, String> toUpdate = new HashMap<>();
        toUpdate.put("Код_налогоплательщика", dto.inn);
        toUpdate.put("Статус", dto.status);
        xlsService.updateRow(toUpdate, dto.rowNum);
      }
      xlsService.saveWorkbook(true);
      xlsService.closeWorkbook();
    }
  }

  static class Dto {

    int rowNum;
    String name;
    String searchName;
    String actualName;
    String inn;
    String status;

    @Override
    public String toString() {
      return "name = " + name + "; status = " + status + "; inn = " + inn + "rowNum = " + rowNum;
    }
  }
}
