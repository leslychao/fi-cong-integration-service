package ru.metlife.integration.service;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.metlife.integration.dto.DeliveryDataDto;
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
  public DeliveryDataDto findByPpNum(String ppNum) {
    return mapToDto(deliveryDataRepository.findByPpNum(ppNum));
  }

  @Transactional(readOnly = true)
  public List<DeliveryDataDto> findByOrderId(List<String> orderId) {
    return deliveryDataRepository.findByOrderId(orderId).stream().map(this::mapToDto)
        .collect(toList());
  }

  @Transactional(readOnly = true)
  public boolean existsDeliveryDataByOrderIdAndPpNum(String orderId, String ppNum) {
    return deliveryDataRepository.existsDeliveryDataByOrderIdAndPpNum(orderId, ppNum);
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
}
