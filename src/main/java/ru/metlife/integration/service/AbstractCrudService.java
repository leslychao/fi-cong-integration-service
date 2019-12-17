package ru.metlife.integration.service;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import ru.metlife.integration.service.mapper.BeanMapper;

public abstract class AbstractCrudService<T, E> {

  @Transactional
  public T save(T dto) {
    requireNonNull(dto);
    E entity = mapToEntity(dto);
    entity = entityPreSaveAction(entity);
    E savedEntity = getRepository().save(entity);
    return mapToDto(savedEntity);
  }

  protected T mapToDto(E entity) {
    return getMapper().mapToDto(entity);
  }

  protected E entityPreSaveAction(E entity) {
    return entity;
  }

  protected E mapToEntity(T dto) {
    return getMapper().mapToEntity(dto);
  }

  protected abstract BeanMapper<T, E> getMapper();

  protected abstract <E, S extends Serializable> CrudRepository<E, S> getRepository();
}
