package com.example.orderservice.repository;

import com.example.orderservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemRepositor extends JpaRepository<Item, UUID> {
}
