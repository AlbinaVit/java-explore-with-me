package ru.practicum.ewm.categories.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.categories.dto.CategoriesMapper;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.categories.dto.NewCategoryDto;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoriesRepository;
import ru.practicum.ewm.events.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriesServiceImpl implements CategoriesService {
    private final CategoriesRepository categoriesRepository;
    private final EventRepository eventRepository;
    private final CategoriesMapper categoriesMapper;

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        int offset = from > 0 ? from / size : 0;
        PageRequest page = PageRequest.of(offset, size);
        List<Category> categoriesList = categoriesRepository.findAll(page).getContent();
        log.info("Найдено {} категорий", categoriesList.size());
        return categoriesList.stream()
                .map(categoriesMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoriesId(Long catId) {
        Category category = getCategoriesIfExist(catId);
        log.info("Найдена категория: {}", category.getName());
        return categoriesMapper.toCategoryDto(category);
    }

    @Override
    public CategoryDto createCategories(NewCategoryDto newCategoryDto) {
        if (categoriesRepository.existsCategoriesByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с именем '" + newCategoryDto.getName() + "' уже существует");
        }
        Category category = categoriesRepository.save(categoriesMapper.toCategories(newCategoryDto));
        log.info("Создана новая категория: {}", category.getName());
        return categoriesMapper.toCategoryDto(category);
    }

    @Override
    public void deleteCategories(Long catId) {
        Category category = getCategoriesIfExist(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Невозможно удалить категорию - существуют связанные события");
        }

        categoriesRepository.deleteById(catId);
        log.info("Удалена категория с id: {}", catId);
    }

    @Override
    public CategoryDto updateCategories(CategoryDto categoryDto) {
        Category categories = getCategoriesIfExist(categoryDto.getId());

        if (categoriesRepository.existsCategoriesByNameAndIdNot(categoryDto.getName(), categoryDto.getId())) {
            throw new ConflictException("Категория с именем '" + categoryDto.getName() + "' уже существует");
        }

        categories.setName(categoryDto.getName());
        Category updatedCategory = categoriesRepository.save(categories);
        log.info("Обновлена категория с id: {}", categoryDto.getId());
        return categoriesMapper.toCategoryDto(updatedCategory);
    }

    private Category getCategoriesIfExist(Long catId) {
        return categoriesRepository.findById(catId).orElseThrow(
                () -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }

}
