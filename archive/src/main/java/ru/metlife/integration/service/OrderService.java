package ru.metlife.integration.service;

import java.io.Serializable;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.metlife.integration.dto.OrderDto;
import ru.metlife.integration.entity.OrderEntity;
import ru.metlife.integration.repository.OrderRepository;
import ru.metlife.integration.service.mapper.BeanMapper;

@Service
@Slf4j
public class OrderService extends AbstractCrudService<OrderDto, OrderEntity> {

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
}
