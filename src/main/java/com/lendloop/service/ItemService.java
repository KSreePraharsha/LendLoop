
package com.lendloop.service;

import com.lendloop.model.Item;
import com.lendloop.model.Student;
import com.lendloop.repository.ItemRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Add a new item
    public Item addItem(Item item, Student owner) {
        item.setOwner(owner);
        item.setAvailable(true);
        return itemRepository.save(item);
    }

    // Get all available items
    public List<Item> getAvailableItems() {
        return itemRepository.findByAvailableTrue();
    }

    // Get items listed by a particular student
    public List<Item> getMyItems(Student owner) {
        return itemRepository.findByOwner(owner);
    }

    // Search items by name
    public List<Item> searchItems(String keyword) {
    return itemRepository.findByTitleContainingIgnoreCase(keyword);
}

    // Filter items by category
    public List<Item> getItemsByCategory(String category) {
        return itemRepository.findByCategoryIgnoreCase(category);
    }
}
