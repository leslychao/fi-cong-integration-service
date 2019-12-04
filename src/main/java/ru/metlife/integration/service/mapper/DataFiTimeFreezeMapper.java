package ru.metlife.integration.service.mapper;

import org.springframework.stereotype.Component;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.entity.DataFiTimeFreezeEntity;

@Component
public class DataFiTimeFreezeMapper implements BeanMapper<OrderDto, DataFiTimeFreezeEntity> {

  @Override
  public DataFiTimeFreezeEntity mapToEntity(OrderDto dto) {
    DataFiTimeFreezeEntity dataFiTimeFreezeEntity = new DataFiTimeFreezeEntity();
    dataFiTimeFreezeEntity.setId(dto.getLetterId());
    dataFiTimeFreezeEntity.setClientFio(dto.getClientFio());
    dataFiTimeFreezeEntity.setPolNum(dto.getPolNum());
    dataFiTimeFreezeEntity.setDocType(dto.getDocType());
    dataFiTimeFreezeEntity.setComment(dto.getComment());
    dataFiTimeFreezeEntity.setEmail(dto.getRecipient());
    dataFiTimeFreezeEntity.setEmailCopy(dto.getEmailCC());
    dataFiTimeFreezeEntity.setCreatedAt(dto.getCreatedAt());
    dataFiTimeFreezeEntity.setEntryHash(dto.getEntryHash());
    dataFiTimeFreezeEntity.setOrderId(dto.getOrderId());
    return dataFiTimeFreezeEntity;
  }

  @Override
  public DataFiTimeFreezeEntity updateEntityWithDto(OrderDto dto, DataFiTimeFreezeEntity entity) {
    return null;
  }

  @Override
  public OrderDto mapToDto(DataFiTimeFreezeEntity entity) {
    OrderDto orderDto = new OrderDto();
    orderDto.setLetterId(entity.getId());
    orderDto.setClientFio(entity.getClientFio());
    orderDto.setPolNum(entity.getPolNum());
    orderDto.setDocType(entity.getDocType());
    orderDto.setComment(entity.getComment());
    orderDto.setRecipient(entity.getEmail());
    orderDto.setEmailCC(entity.getEmailCopy());
    orderDto.setCreatedAt(entity.getCreatedAt());
    orderDto.setEntryHash(entity.getEntryHash());
    orderDto.setOrderId(entity.getOrderId());
    return orderDto;
  }
}
