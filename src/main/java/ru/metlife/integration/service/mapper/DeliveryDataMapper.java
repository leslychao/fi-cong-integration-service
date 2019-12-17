package ru.metlife.integration.service.mapper;

import static java.util.Objects.isNull;

import org.springframework.stereotype.Component;
import ru.metlife.integration.dto.DeliveryDataDto;
import ru.metlife.integration.entity.DeliveryDataEntity;

@Component
public class DeliveryDataMapper implements BeanMapper<DeliveryDataDto, DeliveryDataEntity> {

  @Override
  public DeliveryDataEntity mapToEntity(DeliveryDataDto dto) {
    DeliveryDataEntity deliveryDataEntity = new DeliveryDataEntity();
    deliveryDataEntity.setId(dto.getId());
    deliveryDataEntity.setOrderId(dto.getOrderId());
    deliveryDataEntity.setDeliveryStatus(dto.getDeliveryStatus());
    deliveryDataEntity.setCreatedAt(dto.getCreatedAt());
    deliveryDataEntity.setPpNum(dto.getPpNum());
    return deliveryDataEntity;
  }

  @Override
  public DeliveryDataDto mapToDto(DeliveryDataEntity entity) {
    if (isNull(entity)) {
      return null;
    }
    DeliveryDataDto deliveryDataDto = new DeliveryDataDto();
    deliveryDataDto.setId(entity.getId());
    deliveryDataDto.setOrderId(entity.getOrderId());
    deliveryDataDto.setDeliveryStatus(entity.getDeliveryStatus());
    deliveryDataDto.setCreatedAt(entity.getCreatedAt());
    deliveryDataDto.setPpNum(entity.getPpNum());
    return deliveryDataDto;
  }

  @Override
  public DeliveryDataEntity updateEntityWithDto(DeliveryDataDto dto, DeliveryDataEntity entity) {
    return null;
  }
}
