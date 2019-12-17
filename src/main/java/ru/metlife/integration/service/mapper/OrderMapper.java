package ru.metlife.integration.service.mapper;

import static java.util.Objects.isNull;

import org.springframework.stereotype.Component;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.entity.OrderEntity;

@Component
public class OrderMapper implements BeanMapper<OrderDto, OrderEntity> {

  @Override
  public OrderEntity mapToEntity(OrderDto dto) {
    OrderEntity orderEntity = new OrderEntity();
    orderEntity.setOrderId(dto.getOrderId());
    orderEntity.setRecipient(dto.getRecipient());
    orderEntity.setChannel(dto.getChannel());
    orderEntity.setSender(dto.getSender());
    orderEntity.setStatus(dto.getStatus());
    orderEntity.setSubject(dto.getSubject());
    orderEntity.setCreatedAt(dto.getCreatedAt());
    orderEntity.setCreatedBy(dto.getCreatedBy());
    return orderEntity;
  }

  @Override
  public OrderDto mapToDto(OrderEntity entity) {
    if (isNull(entity)) {
      return null;
    }
    OrderDto orderDto = new OrderDto();
    orderDto.setOrderId(entity.getOrderId());
    orderDto.setRecipient(entity.getRecipient());
    orderDto.setChannel(entity.getChannel());
    orderDto.setSender(entity.getSender());
    orderDto.setStatus(entity.getStatus());
    orderDto.setSubject(entity.getSubject());
    orderDto.setCreatedAt(entity.getCreatedAt());
    orderDto.setCreatedBy(entity.getCreatedBy());
    orderDto.setDeliveryStatus(entity.getDeliveryStatus());
    return orderDto;
  }

  @Override
  public OrderEntity updateEntityWithDto(OrderDto dto, OrderEntity entity) {
    return null;
  }
}
