package ru.metlife.integration.service.mapper;

public interface BeanMapper<T, E> {

  E mapToEntity(T dto);

  T mapToDto(E entity);
}
