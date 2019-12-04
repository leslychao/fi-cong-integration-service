package ru.metlife.integration.service.mapper;

public interface BeanMapper<T, E> {

  E mapToEntity(T dto);

  E updateEntityWithDto(T dto, E entity);

  T mapToDto(E entity);
}
