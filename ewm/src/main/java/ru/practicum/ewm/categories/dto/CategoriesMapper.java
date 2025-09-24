package ru.practicum.ewm.categories.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.categories.model.Category;

@Component
@RequiredArgsConstructor
public class CategoriesMapper {
    public CategoryDto toCategoryDto(Category categories) {
        return CategoryDto.builder()
                .id(categories.getId())
                .name(categories.getName())
                .build();
    }

    public Category toCategories(NewCategoryDto newCategoryDto) {
        return Category.builder()
                .name(newCategoryDto.getName())
                .build();
    }

}
