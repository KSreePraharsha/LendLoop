package com.lendloop.repository;

import com.lendloop.model.Item;
import com.lendloop.model.ItemOffer;
import com.lendloop.model.Student;
import com.lendloop.model.WantedItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ItemOfferRepository extends JpaRepository<ItemOffer, Long> {

    List<ItemOffer> findByWantedItemOrderByCreatedAtDesc(WantedItem wantedItem);

    List<ItemOffer> findByWantedItemAndStatus(WantedItem wantedItem, String status);

    List<ItemOffer> findByOfferedByOrderByCreatedAtDesc(Student offeredBy);

    boolean existsByWantedItemAndOfferedByAndStatusIn(
            WantedItem wantedItem,
            Student offeredBy,
            Collection<String> statuses
    );

    List<ItemOffer> findByOfferedItemAndStatus(Item offeredItem, String status);
}
