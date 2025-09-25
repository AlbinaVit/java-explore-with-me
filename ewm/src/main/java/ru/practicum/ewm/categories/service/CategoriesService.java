package ru.practicum.ewm.categories.service;

import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.categories.dto.NewCategoryDto;

import java.util.List;

public interface CategoriesService {

    List<CategoryDto> getCategories(Integer from, Integer size);

    CategoryDto getCategoriesId(Long catId);

    CategoryDto createCategories(NewCategoryDto newCategoryDto);

    void deleteCategories(Long catId);

    CategoryDto updateCategories(CategoryDto categoryDto);

}
