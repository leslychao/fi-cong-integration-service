package ru.metlife.integration.service;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.entity.DataFiTimeFreezeEntity;
import ru.metlife.integration.repository.DataFiTimeFreezeRepository;
import ru.metlife.integration.service.mapper.BeanMapper;

@Service
@Slf4j
public class DataFiTimeFreezeService extends AbstractCrudService<OrderDto, DataFiTimeFreezeEntity> {

  private DataFiTimeFreezeRepository dataFiTimeFreezeRepository;
  private BeanMapper<OrderDto, DataFiTimeFreezeEntity> beanMapper;

  @Autowired
  public DataFiTimeFreezeService(DataFiTimeFreezeRepository dataFiTimeFreezeRepository,
      @Qualifier("dataFiTimeFreezeMapper") BeanMapper<OrderDto, DataFiTimeFreezeEntity> beanMapper) {
    this.dataFiTimeFreezeRepository = dataFiTimeFreezeRepository;
    this.beanMapper = beanMapper;
  }

  public void saveOrder(OrderDto dto) {
    OrderDto orderDto = save(dto);
    dto.setLetterId(orderDto.getLetterId());
  }

  @Override
  protected DataFiTimeFreezeEntity entityPreSaveAction(DataFiTimeFreezeEntity entity) {
    entity.setId(UUID.randomUUID().toString());
    return super.entityPreSaveAction(entity);
  }

  @Override
  protected BeanMapper<OrderDto, DataFiTimeFreezeEntity> getMapper() {
    return beanMapper;
  }

  @Override
  protected CrudRepository<DataFiTimeFreezeEntity, String> getRepository() {
    return dataFiTimeFreezeRepository;
  }

}
