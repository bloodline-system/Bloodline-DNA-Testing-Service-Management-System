package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.ParticipantRequest;
import com.dna_testing_system.dev.dto.response.OrderParticipantResponse;
import com.dna_testing_system.dev.entity.OrderParticipant;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.mapper.ParticipantOrderMapper;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.ParticipantRepository;
import com.dna_testing_system.dev.service.OrderParticipantService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderParticipantServiceImpl implements OrderParticipantService {

    ParticipantRepository participantRepository;
    ParticipantOrderMapper participantOrderMapper;
    OrderServiceRepository orderServiceRepository;


    @Override
    public void createOrderParticipant(Long orderId, ParticipantRequest participantRequest) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        OrderParticipant orderParticipant = participantOrderMapper.toEntity(participantRequest);
        orderParticipant.setOrder(serviceOrder);
        participantRepository.save(orderParticipant);
    }

    @Override
    public void deleteOrderParticipant(Long orderId) {

    }

    @Override
    public void updateOrderParticipant(Long orderId, Long participantId, ParticipantRequest participantRequest) {

    }

    @Override
    public List<OrderParticipantResponse> getAllParticipantsByOrderId(Long orderId) {
        List<OrderParticipantResponse> responses = new ArrayList<>();
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        serviceOrder.getOrderParticipants().forEach(orderParticipant -> {
            OrderParticipantResponse response = participantOrderMapper.toDto(orderParticipant);
            if (response != null) {
                responses.add(response);
            }
            // You can add the response to a list if needed
        });
        return responses;
    }
}
