package ru.metlife.integration.service;

import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.springframework.util.DigestUtils.md5Digest;
import static ru.metlife.integration.util.CommonUtils.getStringCellValue;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.entity.OrderEntity;
import ru.metlife.integration.repository.OrderRepository;
import ru.metlife.integration.service.mapper.BeanMapper;
import ru.metlife.integration.service.xssf.XlsService.SheetData;

@Service
public class OrderService extends AbstractCrudService<OrderDto, OrderEntity> {

  private static final int FI_LETTER_STATUS = 17;

  @Value("${fi-cong-integration.sender-email}")
  private String sender;
  @Value("${fi-cong-integration.channel}")
  private String channel;

  private OrderRepository orderRepository;
  private BeanMapper<OrderDto, OrderEntity> beanMapper;

  @Autowired
  public OrderService(OrderRepository orderRepository,
      @Qualifier("orderMapper") BeanMapper<OrderDto, OrderEntity> beanMapper) {
    this.orderRepository = orderRepository;
    this.beanMapper = beanMapper;
  }

  public void saveOrder(OrderDto dto) {
    OrderDto orderDto = save(dto);
    dto.setOrderId(orderDto.getOrderId());
  }

  @Transactional(readOnly = true)
  public OrderDto findByOrderId(String orderId) {
    return mapToDto(orderRepository.findByOrderId(orderId));
  }

  @Override
  protected OrderEntity entityPreSaveAction(OrderEntity entity) {
    entity.setOrderId(UUID.randomUUID().toString());
    return super.entityPreSaveAction(entity);
  }

  @Override
  protected BeanMapper<OrderDto, OrderEntity> getMapper() {
    return beanMapper;
  }

  @Override
  protected CrudRepository<OrderEntity, ? extends Serializable> getRepository() {
    return orderRepository;
  }

  public List<OrderDto> toOrderDto(SheetData sheetData) {
    return ofNullable(sheetData.getData())
        .orElse(emptyList())
        .stream()
        .map(this::toOrderDto)
        .collect(toList());
  }

  public OrderDto toOrderDto(Map<String, String> orderFromXls) {
    String orderId = getStringCellValue(orderFromXls, "order_id", null);
    String deliveryStatus = getStringCellValue(orderFromXls, "delivery_status");
    String ppNum = getStringCellValue(orderFromXls, "№ п/п");
    String polNum = getStringCellValue(orderFromXls, "Номер сертификата");
    String clientFio = getStringCellValue(orderFromXls, "ФИО");
    String comments = getStringCellValue(orderFromXls, "Комментарии");
    String docType = getStringCellValue(orderFromXls, "Тип документа");
    String email = getStringCellValue(orderFromXls, "e-mail");
    String emailCC = getStringCellValue(orderFromXls, "e-mail копия");
    String dealership = getStringCellValue(orderFromXls, "Дилерский центр");
    String partner = getStringCellValue(orderFromXls, "Партнер");
    String region = getStringCellValue(orderFromXls, "Region");
    String rowNum = getStringCellValue(orderFromXls, "rowNum");

    OrderDto orderDto = new OrderDto();
    orderDto.setRowNum(Integer.valueOf(rowNum));
    orderDto.setPpNum(ppNum);
    orderDto.setOrderId(orderId);
    orderDto.setDeliveryStatus(deliveryStatus);
    orderDto.setRecipient(email);
    orderDto.setChannel(channel);
    orderDto.setSender(sender);
    String subject = of(dealership, region, polNum, clientFio, docType)
        .filter(StringUtils::isNotBlank)
        .collect(joining("_"));
    orderDto.setSubject(subject);
    orderDto.setStatus(FI_LETTER_STATUS);
    orderDto.setCreatedAt(new Date());
    orderDto.setCreatedBy(sender);
    orderDto.setClientFio(clientFio);
    orderDto.setPolNum(polNum);
    orderDto.setComment(comments);
    orderDto.setDocType(docType);
    orderDto.setEmailCC(emailCC);
    try {
      byte[] entryHash = md5Digest(join("_", subject, email).getBytes("UTF-8"));
      orderDto.setEntryHash(entryHash);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    orderDto.setDealership(dealership);
    orderDto.setPartner(partner);
    orderDto.setRegion(region);
    return orderDto;
  }
}
