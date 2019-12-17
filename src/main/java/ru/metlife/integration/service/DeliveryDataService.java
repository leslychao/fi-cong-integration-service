package ru.metlife.integration.service;

import java.io.Serializable;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.metlife.integration.dto.DeliveryDataDto;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.entity.DeliveryDataEntity;
import ru.metlife.integration.repository.DeliveryDataRepository;
import ru.metlife.integration.service.mapper.BeanMapper;

@Service
public class DeliveryDataService extends AbstractCrudService<DeliveryDataDto, DeliveryDataEntity> {

  private DeliveryDataRepository deliveryDataRepository;
  private BeanMapper<DeliveryDataDto, DeliveryDataEntity> beanMapper;

  @Autowired
  public DeliveryDataService(DeliveryDataRepository deliveryDataRepository,
      @Qualifier("deliveryDataMapper") BeanMapper<DeliveryDataDto, DeliveryDataEntity> beanMapper) {
    this.deliveryDataRepository = deliveryDataRepository;
    this.beanMapper = beanMapper;
  }

  public void saveDeliveryData(DeliveryDataDto dto) {
    DeliveryDataDto deliveryDataDto = save(dto);
    dto.setId(deliveryDataDto.getId());
  }

  @Transactional(readOnly = true)
  public boolean existsDeliveryDataByPpNum(String ppNum) {
    return deliveryDataRepository.existsDeliveryDataByPpNum(ppNum);
  }

  @Transactional(readOnly = true)
  public DeliveryDataDto findByOrderId(String orderId) {
    return mapToDto(deliveryDataRepository.findByOrderId(orderId));
  }

  @Transactional
  public String updateStatus(String deliveryStatus, String orderId) {
    return deliveryDataRepository.updateStatus(deliveryStatus, orderId);
  }

  @Override
  protected DeliveryDataEntity entityPreSaveAction(DeliveryDataEntity entity) {
    entity.setId(UUID.randomUUID().toString());
    return super.entityPreSaveAction(entity);
  }

  @Override
  protected BeanMapper<DeliveryDataDto, DeliveryDataEntity> getMapper() {
    return beanMapper;
  }

  @Override
  protected CrudRepository<DeliveryDataEntity, ? extends Serializable> getRepository() {
    return deliveryDataRepository;
  }

  public DeliveryDataDto toDeliveryDataDto(OrderDto orderDto) {
    DeliveryDataDto deliveryDataDto = new DeliveryDataDto();
    deliveryDataDto.setPpNum(orderDto.getPpNum());
    deliveryDataDto.setOrderId(orderDto.getOrderId());
    deliveryDataDto.setCreatedAt(orderDto.getCreatedAt());
    deliveryDataDto.setDeliveryStatus(orderDto.getDeliveryStatus());
    return deliveryDataDto;
  }
}
