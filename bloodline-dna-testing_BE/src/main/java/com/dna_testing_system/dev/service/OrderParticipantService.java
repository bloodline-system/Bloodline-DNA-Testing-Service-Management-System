package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.ParticipantRequest;
import com.dna_testing_system.dev.dto.response.OrderParticipantResponse;

import java.util.List;

public interface OrderParticipantService {
    void createOrderParticipant(Long orderId, ParticipantRequest participantRequest);

    void deleteOrderParticipant(Long orderId);

    void updateOrderParticipant(Long orderId, Long participantId, ParticipantRequest participantRequest );

    List<OrderParticipantResponse> getAllParticipantsByOrderId(Long orderId);
}
